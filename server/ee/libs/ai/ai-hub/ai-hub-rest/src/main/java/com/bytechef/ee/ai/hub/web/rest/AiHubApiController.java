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
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.ai.hub.agent.AiHubChatStreamer;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.security.WorkspaceAccessGuard;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Metrics;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AG-UI dispatch entry point for the {@code ai_hub} agent. Resolves the LLM-backed AI Hub routing agent (build vs ask
 * variant based on {@link Mode}) and runs it through the AG-UI service.
 *
 * <p>
 * The {@code ai_hub} agent is the only AG-UI-dispatched agent that is bound to a {@link AiHubTask} — its
 * {@code threadId} keys a row in {@code ai_hub_task} and every tool callback invoked from the agent records its work
 * against that row's {@code id}. Keeping the dispatcher in the {@code automation-ai-hub-rest} module means CC owns
 * end-to-end the path that creates, mutates, and reads its own task surface.
 * </p>
 *
 * <p>
 * Spring's {@code RequestMappingHandlerMapping} resolves the literal path {@code /ai/chat/ai_hub} ahead of any generic
 * {@code /ai/chat/{agentId}} mapping that may exist elsewhere, so dispatchers can co-exist without endpoint collisions.
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
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@PreAuthorize("isAuthenticated()")
public class AiHubApiController {

    private static final Logger log = LoggerFactory.getLogger(AiHubApiController.class);

    private final Map<String, LocalAgent> localAgentMap;
    private final AiHubChatStreamer chatStreamer;
    private final InFlightAiHubRunRegistry inFlightRunRegistry;
    private final AiHubTaskService taskService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AiHubApiController(
        AiHubChatStreamer chatStreamer, InFlightAiHubRunRegistry inFlightRunRegistry,
        List<LocalAgent> localAgents, AiHubTaskService taskService,
        UserService userService, WorkspaceFacade workspaceFacade) {

        this.chatStreamer = chatStreamer;
        this.inFlightRunRegistry = inFlightRunRegistry;
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent));
        this.taskService = taskService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @Validated
    @PostMapping(value = "/ai/chat/ai_hub")
    public SseEmitter chat(@NonNull @RequestBody() AgUiParameters agUiParameters) {
        long userId = userService.getCurrentUser()
            .getId();

        long workspaceId = enforceWorkspaceAccess(agUiParameters, userId);

        String verifiedThreadId = enforceThreadOwnership(agUiParameters, userId, workspaceId);

        injectAuthenticatedContext(agUiParameters, userId, workspaceId, verifiedThreadId);

        Mode mode = resolveMode(agUiParameters);

        String resolvedAgentId = mode == Mode.BUILD ? "ai_hub_build" : "ai_hub_ask";

        LocalAgent localAgent = localAgentMap.get(resolvedAgentId);

        if (localAgent == null) {
            log.warn(
                "ai_hub agent variant '{}' is not registered (mode={}); known agents: {}",
                resolvedAgentId, mode, localAgentMap.keySet());

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown agentId");
        }

        return chatStreamer.runAgent(localAgent, agUiParameters, verifiedThreadId);
    }

    /**
     * Resume / mirror an existing in-flight run for {@code threadId}. The client invokes this on mount when its probe
     * reports that a run is in flight — instead of reposting the user's message (which would start a duplicate agent
     * invocation), it opens an EventSource here and gets the buffered prefix + live tail.
     *
     * <p>
     * Auth gate matches the POST path: the resolved {@link AiHubTask} must belong to the authenticated user. 404 is
     * returned when no run is registered for the thread — the client treats that as "fell off the bus" and renders
     * history-only without a running pulse.
     * </p>
     */
    @GetMapping(value = "/ai/chat/ai_hub/{threadId}/attach")
    public SseEmitter attach(@PathVariable("threadId") String threadId) {
        long userId = userService.getCurrentUser()
            .getId();

        AiHubTask task = enforceThreadOwnershipForAttach(threadId, userId);

        Optional<SseEmitter> emitterOptional = chatStreamer.attachToRun(threadId);

        if (emitterOptional.isEmpty()) {
            // Either the run completed before the client mounted, or it never existed. Either way, an attach with
            // no run to attach to is best surfaced as 404 so the client can fall back to history-only rendering.
            Metrics.counter(
                "bytechef.ai_hub.chat.attach_miss_total",
                "taskId", String.valueOf(task.getId()))
                .increment();

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No in-flight run for thread");
        }

        Metrics.counter("bytechef.ai_hub.chat.attach_hit_total")
            .increment();

        return emitterOptional.get();
    }

    /**
     * Reports which of the supplied {@code threadIds} currently have in-flight runs. Used by the sidebar on mount /
     * environment switch to paint a "running" pulse on each task that's actively streaming, not just the focused one.
     * Filtering against a caller-supplied id set scopes the answer to threads the user actually owns — without it, an
     * enumeration of every in-flight thread would leak cross-workspace state.
     */
    @GetMapping(value = "/ai/chat/ai_hub/in-flight")
    public Map<String, Boolean> inFlightStatus(@RequestParam("threadIds") List<String> threadIds) {
        long userId = userService.getCurrentUser()
            .getId();

        // Build the per-thread answer in one pass: each id is either (a) unknown to the registry → false, (b)
        // owned by someone other than the caller → false (silently — same shape as not-in-flight), or (c) the
        // caller's own and in flight → true. The owner-mismatch case is treated as not-in-flight rather than
        // 403 so a malformed sidebar id list doesn't break the whole probe.
        Set<String> inFlight = new HashSet<>(inFlightRunRegistry.getInFlightThreadIds());

        return threadIds.stream()
            .collect(
                Collectors.toMap(threadId -> threadId, threadId -> isOwnedInFlightThread(threadId, userId, inFlight)));
    }

    private boolean isOwnedInFlightThread(String threadId, long userId, Collection<String> inFlightThreadIds) {
        if (!inFlightThreadIds.contains(threadId)) {
            return false;
        }

        Optional<AiHubTask> task = taskService.findByThreadId(threadId);

        return task.isPresent() && task.get()
            .getUserId() == userId;
    }

    private AiHubTask enforceThreadOwnershipForAttach(String threadId, long userId) {
        if (threadId == null || threadId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing threadId");
        }

        Optional<AiHubTask> task = taskService.findByThreadId(threadId);

        if (task.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown thread");
        }

        AiHubTask row = task.get();

        if (row.getUserId() != userId) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "AiHubTask is not accessible to the current user");
        }

        return row;
    }

    /**
     * Reads the client-supplied {@code workspaceId} from the request state and enforces that the authenticated user is
     * a member of that workspace. Without this gate a malicious client could submit
     * {@code state.workspaceId=&lt;victim_ws&gt;} and have CC tool callbacks operate against a workspace they have no
     * access to.
     */
    private long enforceWorkspaceAccess(AgUiParameters agUiParameters, long userId) {
        Long requestedWorkspaceId = readLong(agUiParameters, AiHubStateKeys.WORKSPACE_ID);

        if (requestedWorkspaceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing workspaceId in request state");
        }

        if (!WorkspaceAccessGuard.isMember(workspaceFacade, userId, requestedWorkspaceId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Workspace is not accessible to the current user");
        }

        return requestedWorkspaceId;
    }

    /**
     * Verifies that the resolved {@link AiHubTask} belongs to the authenticated user AND lives in the requested
     * workspace. Returns the verified thread id, or {@code null} when none was supplied or no task exists yet (first
     * turn).
     */
    private String enforceThreadOwnership(AgUiParameters agUiParameters, long userId, long workspaceId) {
        String threadId = agUiParameters.getThreadId();

        if (threadId == null || threadId.isBlank()) {
            threadId = readString(agUiParameters, AiHubStateKeys.THREAD_ID);
        }

        if (threadId == null || threadId.isBlank()) {
            return null;
        }

        Optional<AiHubTask> task = taskService.findByThreadId(threadId);

        if (task.isEmpty()) {
            Metrics.counter("bytechef.ai_hub.chat.threadid_unknown_total")
                .increment();

            return null;
        }

        AiHubTask row = task.get();

        if (row.getUserId() != userId || taskService.getWorkspaceId(row.getId()) != workspaceId) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "AiHubTask is not accessible to the current user");
        }

        return threadId;
    }

    /**
     * Writes the verified identity values into reserved keys on the AG-UI state. The agent's
     * {@code buildInvocationContext} reads from these server-controlled keys (not from the original request fields), so
     * the {@code AiHubToolInvocationContext} carried into tool callbacks is constructed from authenticated-session data
     * — not from user-controlled request body.
     */
    private void injectAuthenticatedContext(
        AgUiParameters agUiParameters, long userId, long workspaceId, String verifiedThreadId) {

        State state = agUiParameters.getState();

        if (state == null) {
            state = new State();
            agUiParameters.setState(state);
        }

        state.set(AiHubStateKeys.AUTHENTICATED_USER_ID, userId);
        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, workspaceId);

        // Defensively overwrite the unverified key paths with verified values so a future regression that reads
        // state.workspaceId or state.userId still gets server-controlled data.
        state.set(AiHubStateKeys.WORKSPACE_ID, workspaceId);
        state.set(AiHubStateKeys.USER_ID, userId);

        if (verifiedThreadId != null) {
            state.set(AiHubStateKeys.VERIFIED_THREAD_ID, verifiedThreadId);
            state.set(AiHubStateKeys.THREAD_ID, verifiedThreadId);
        }

        Long rawEnvironmentId = readLong(agUiParameters, AiHubStateKeys.ENVIRONMENT_ID);

        long environmentId =
            rawEnvironmentId != null && rawEnvironmentId >= 0 && rawEnvironmentId < Environment.values().length
                ? rawEnvironmentId
                : 0L;

        state.set(AiHubStateKeys.VERIFIED_ENVIRONMENT_ID, environmentId);
        state.set(AiHubStateKeys.ENVIRONMENT_ID, environmentId);

        state.set(AiHubStateKeys.VERIFIED_TENANT_ID, TenantContext.getCurrentTenantId());
    }

    /**
     * Resolves the {@link Mode} from the AG-UI request state. Missing/blank state defaults to {@link Mode#ASK}; an
     * unrecognised mode string is rejected with 400 so a stale client surfaces a "please refresh" toast instead of
     * silently degrading to ASK.
     */
    private Mode resolveMode(AgUiParameters agUiParameters) {
        State state = agUiParameters.getState();

        if (state == null) {
            return Mode.ASK;
        }

        Map<String, Object> stateMap = state.getState();

        if (stateMap == null) {
            return Mode.ASK;
        }

        Object rawMode = stateMap.get("mode");

        if (!(rawMode instanceof String stringMode) || stringMode.isBlank()) {
            return Mode.ASK;
        }

        try {
            return Mode.valueOf(stringMode);
        } catch (IllegalArgumentException exception) {
            log.warn("Unknown mode '{}' on ai_hub request; rejecting with 400", stringMode);

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Unknown mode: " + stringMode, exception);
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException exception) {
        HttpStatusCode statusCode = exception.getStatusCode();
        String reason = exception.getReason();

        if (reason == null) {
            return ResponseEntity.status(statusCode)
                .build();
        }

        return ResponseEntity.status(statusCode)
            .body(Map.of("error", reason));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException exception) {
        Metrics.counter(
            "bytechef.ai_hub.chat.unexpected_failure_total",
            "exception", exception.getClass()
                .getSimpleName())
            .increment();

        log.error("Unexpected runtime exception in AiHubApiController: {}", exception.toString(), exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "An unexpected error occurred while dispatching the agent"));
    }

    private static Long readLong(AgUiParameters agUiParameters, String key) {
        Object raw = readRaw(agUiParameters, key);

        if (raw instanceof Number numberValue) {
            return numberValue.longValue();
        }

        if (raw instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException exception) {
                log.warn("Malformed numeric value for state key '{}': '{}' is not parseable as Long",
                    key, stringValue);

                return null;
            }
        }

        return null;
    }

    private static String readString(AgUiParameters agUiParameters, String key) {
        Object raw = readRaw(agUiParameters, key);

        return raw == null ? null : raw.toString();
    }

    private static Object readRaw(AgUiParameters agUiParameters, String key) {
        State state = agUiParameters.getState();

        if (state == null) {
            return null;
        }

        Map<String, Object> stateMap = state.getState();

        return stateMap == null ? null : stateMap.get(key);
    }
}
