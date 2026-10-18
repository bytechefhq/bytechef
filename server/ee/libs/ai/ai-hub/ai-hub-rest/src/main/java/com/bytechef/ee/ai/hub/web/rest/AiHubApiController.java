/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.rest;

import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.agent.AiHubChatStreamer;
import com.bytechef.ee.ai.hub.agent.AiHubRunState;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry.InFlightRun;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatAccessPolicy;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AG-UI dispatch entry point for the AI Hub chat. A turn is checked in this order: workspace role for the requested
 * mode, the chat behind the thread id (created on the first turn, otherwise owner or tenant admin and the same
 * workspace), the one-turn-per-chat gate, then pending approvals are superseded, the verified identity is written into
 * the run state, and the agent of the mode runs.
 *
 * <p>
 * The literal path {@code /ai/chat/ai_hub} resolves ahead of the Copilot's generic {@code /ai/chat/{agentId}}.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@ConditionalOnAiHub
@PreAuthorize("isAuthenticated()")
public class AiHubApiController {

    private static final Logger log = LoggerFactory.getLogger(AiHubApiController.class);

    private static final String UNKNOWN_THREAD_REASON = "Unknown thread";

    private final AiHubChatAccessPolicy accessPolicy;
    private final AiHubChatService chatService;
    private final AiHubChatStreamer chatStreamer;
    private final InFlightAiHubRunRegistry inFlightRunRegistry;
    private final Map<String, LocalAgent> localAgentMap;
    private final ObjectProvider<PermissionService> permissionServiceProvider;
    private final AiHubToolApprovalService toolApprovalService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public AiHubApiController(
        AiHubChatStreamer chatStreamer, InFlightAiHubRunRegistry inFlightRunRegistry, List<LocalAgent> localAgents,
        AiHubChatService chatService, AiHubChatAccessPolicy accessPolicy, UserService userService,
        ObjectProvider<PermissionService> permissionServiceProvider, AiHubToolApprovalService toolApprovalService) {

        this.accessPolicy = accessPolicy;
        this.chatService = chatService;
        this.chatStreamer = chatStreamer;
        this.inFlightRunRegistry = inFlightRunRegistry;
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent, (first, second) -> first));
        this.permissionServiceProvider = permissionServiceProvider;
        this.toolApprovalService = toolApprovalService;
        this.userService = userService;
    }

    @Validated
    @PostMapping(value = "/ai/chat/ai_hub")
    public SseEmitter chat(@NonNull @RequestBody() AgUiParameters agUiParameters) {
        long userId = userService.getCurrentUser()
            .getId();

        Mode mode = resolveMode(agUiParameters);

        long workspaceId = enforceWorkspaceRole(agUiParameters, mode);

        LocalAgent localAgent = resolveAgent(mode);

        String threadId = resolveThreadId(agUiParameters);

        long environmentId = AiHubRunState.clampEnvironmentId(readLong(agUiParameters, AiHubStateKeys.ENVIRONMENT_ID));

        AiHubChat chat = resolveChat(threadId, userId, workspaceId, environmentId);

        InFlightRun run = inFlightRunRegistry.tryStart(threadId, agUiParameters.getRunId())
            .orElseThrow(TurnInFlightException::new);

        try {
            toolApprovalService.supersedePending(Objects.requireNonNull(chat.getId()));

            prepareParameters(agUiParameters, userId, workspaceId, threadId, environmentId, chat.getUserId());

            return chatStreamer.stream(localAgent, agUiParameters, run);
        } catch (RuntimeException exception) {
            inFlightRunRegistry.cancel(threadId);

            throw exception;
        }
    }

    /**
     * Resumes an existing run for {@code threadId}: the client opens this on mount when a run may still be streaming
     * and gets the buffered prefix plus the live tail. A thread the caller cannot view, or whose workspace the caller
     * can no longer read, answers exactly as an unknown one, and a thread with no registered run answers 404 so the
     * client renders history only.
     */
    @GetMapping(value = "/ai/chat/ai_hub/{threadId}/attach")
    public SseEmitter attach(@PathVariable("threadId") String threadId) {
        long userId = userService.getCurrentUser()
            .getId();

        if (threadId == null || threadId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing threadId");
        }

        AiHubChat chat = chatService.findByThreadIdViewable(threadId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, UNKNOWN_THREAD_REASON));

        if (!canReadWorkspace(chat)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, UNKNOWN_THREAD_REASON);
        }

        return chatStreamer.attach(threadId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No in-flight run for thread"));
    }

    @ExceptionHandler(TurnInFlightException.class)
    public ResponseEntity<Map<String, Object>> handleTurnInFlight(TurnInFlightException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("error", "TURN_IN_FLIGHT"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException exception) {
        HttpStatusCode statusCode = exception.getStatusCode();
        String reason = exception.getReason();

        if (reason == null) {
            HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());

            reason = httpStatus == null ? String.valueOf(statusCode.value()) : httpStatus.getReasonPhrase();
        }

        return ResponseEntity.status(statusCode)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("error", reason));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("error", "Access denied"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException exception) {
        log.error("Unexpected runtime exception in AiHubApiController: {}", exception.toString(), exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("error", "An unexpected error occurred while dispatching the agent"));
    }

    /**
     * Reads the requested workspace from the run state and requires the caller to hold the mode's minimum workspace
     * role: VIEWER for ASK, EDITOR for BUILD. Fails closed when no permission service is available.
     */
    private long enforceWorkspaceRole(AgUiParameters agUiParameters, Mode mode) {
        Long workspaceId = readLong(agUiParameters, AiHubStateKeys.WORKSPACE_ID);

        if (workspaceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing workspaceId in request state");
        }

        PermissionService permissionService = permissionServiceProvider.getIfAvailable();

        String minimumRole = switch (mode) {
            case ASK -> "VIEWER";
            case BUILD -> "EDITOR";
        };

        if (permissionService == null || !permissionService.hasWorkspaceRole(workspaceId, minimumRole)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Workspace is not accessible to the current user");
        }

        return workspaceId;
    }

    /**
     * Whether the caller may still read the chat's workspace: the VIEWER role, plus the {@code WORKFLOW_VIEW} scope for
     * a webhook-bridged chat, the same rules a turn on the chat checks.
     */
    private boolean canReadWorkspace(AiHubChat chat) {
        PermissionService permissionService = permissionServiceProvider.getIfAvailable();
        Long workspaceId = chat.getWorkspaceId();

        if (permissionService == null || workspaceId == null) {
            return false;
        }

        if (!permissionService.hasWorkspaceRole(workspaceId, "VIEWER")) {
            return false;
        }

        AiHubChatKind kind = chat.getKind();

        return !kind.isWebhookBridged() || permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_VIEW");
    }

    private LocalAgent resolveAgent(Mode mode) {
        String agentId = switch (mode) {
            case ASK -> "ai_hub_ask";
            case BUILD -> "ai_hub_build";
        };

        LocalAgent localAgent = localAgentMap.get(agentId);

        if (localAgent == null) {
            log.warn("AI Hub agent '{}' is not registered (mode={})", agentId, mode);

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown agentId");
        }

        return localAgent;
    }

    /**
     * The request's {@code threadId}, falling back to {@code state.threadId}. Required: session memory, the run
     * registry and the approval gate are all keyed on it.
     */
    private static String resolveThreadId(AgUiParameters agUiParameters) {
        String threadId = agUiParameters.getThreadId();

        if (threadId == null || threadId.isBlank()) {
            threadId = readString(agUiParameters, AiHubStateKeys.THREAD_ID);
        }

        if (threadId == null || threadId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing threadId");
        }

        return threadId;
    }

    /**
     * Returns the chat behind {@code threadId}, creating it on the thread's first turn, so a chat row always exists
     * before the agent runs and the approval gate can resolve it. An existing chat must be one the caller may
     * participate in and must live in the requested workspace.
     */
    private AiHubChat resolveChat(String threadId, long userId, long workspaceId, long environmentId) {
        Optional<AiHubChat> chatOptional = chatService.findByThreadId(threadId);

        if (chatOptional.isPresent()) {
            AiHubChat chat = chatOptional.get();

            if (!accessPolicy.canParticipate(chat, userId) || !Objects.equals(chat.getWorkspaceId(), workspaceId)) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "AiHubChat is not accessible to the current user");
            }

            return chat;
        }

        try {
            return chatService.create(workspaceId, userId, Math.toIntExact(environmentId), threadId);
        } catch (ConflictException exception) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "AiHubChat is not accessible to the current user", exception);
        }
    }

    /**
     * Pins the request to the verified thread and identity before the agent sees it: the AG-UI thread id (which keys
     * session memory) becomes the verified thread id, the reserved state keys are overwritten with server-side values,
     * and client-supplied AG-UI tools are dropped.
     */
    private static void prepareParameters(
        AgUiParameters agUiParameters, long userId, long workspaceId, String threadId, long environmentId,
        long ownerUserId) {

        agUiParameters.setThreadId(threadId);
        agUiParameters.setTools(List.of());

        State state = agUiParameters.getState();

        if (state == null) {
            state = new State();

            agUiParameters.setState(state);
        }

        AiHubRunState.inject(
            state, userId, workspaceId, threadId, environmentId, TenantContext.getCurrentTenantId(), ownerUserId);
    }

    /**
     * Resolves the {@link Mode} from the run state. Missing or blank defaults to {@link Mode#ASK}; an unrecognised
     * value is a 400 so a stale client surfaces an error instead of silently running ASK.
     */
    private static Mode resolveMode(AgUiParameters agUiParameters) {
        Object rawMode = readRaw(agUiParameters, "mode");

        if (!(rawMode instanceof String stringMode) || stringMode.isBlank()) {
            return Mode.ASK;
        }

        try {
            return Mode.valueOf(stringMode);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown mode: " + stringMode, exception);
        }
    }

    private static @Nullable Long readLong(AgUiParameters agUiParameters, String key) {
        Object raw = readRaw(agUiParameters, key);

        if (raw instanceof Number numberValue) {
            return numberValue.longValue();
        }

        if (raw instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException exception) {
                log.warn("Malformed numeric value for state key '{}': '{}'", key, stringValue);

                return null;
            }
        }

        return null;
    }

    private static @Nullable String readString(AgUiParameters agUiParameters, String key) {
        Object raw = readRaw(agUiParameters, key);

        return raw == null ? null : raw.toString();
    }

    private static @Nullable Object readRaw(AgUiParameters agUiParameters, String key) {
        State state = agUiParameters.getState();

        if (state == null) {
            return null;
        }

        Map<String, Object> stateMap = state.getState();

        return stateMap == null ? null : stateMap.get(key);
    }
}
