/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry.InFlightRun;
import com.bytechef.ee.ai.hub.approval.AiHubContinuationStarter;
import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.tool.AiHubAgentType;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Starts the continuation turn that reports a resolved tool approval back into its chat. The run goes through the same
 * {@link InFlightAiHubRunRegistry#tryStart} gate as a user's turn, so it never overlaps one; the client follows it
 * through the attach endpoint. When a run holds the thread, the continuation is deferred until that run ends
 * ({@link InFlightAiHubRunRegistry#runWhenReleased}) and then starts on the worker executor in the same tenant and
 * security context. It is dropped when the chat is deleted, when the holding run never reports that it stopped, or on
 * restart (the queue is in memory).
 *
 * <p>
 * The run state's {@code AUTHENTICATED_USER_ID} is the approval's requester, not the resolver: the continuation is
 * still driven by the requester's instruction and can call further tools. The chat owner travels as
 * {@code VERIFIED_OWNER_USER_ID}.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubContinuationStarterImpl implements AiHubContinuationStarter {

    private static final Logger log = LoggerFactory.getLogger(AiHubContinuationStarterImpl.class);

    private final AiHubChatService chatService;
    private final AiHubChatStreamer chatStreamer;
    private final InFlightAiHubRunRegistry inFlightRunRegistry;
    private final Map<String, LocalAgent> localAgentMap;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubContinuationStarterImpl(
        AiHubChatStreamer chatStreamer, InFlightAiHubRunRegistry inFlightRunRegistry, List<LocalAgent> localAgents,
        AiHubChatService chatService) {

        this.chatService = chatService;
        this.chatStreamer = chatStreamer;
        this.inFlightRunRegistry = inFlightRunRegistry;
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent, (first, second) -> first));
    }

    @Override
    public Optional<String> startContinuation(
        AiHubToolApproval approval, AiHubChat chat, long workspaceId, String message) {

        Mode mode = Mode.BUILD.name()
            .equals(approval.getMode()) ? Mode.BUILD : Mode.ASK;

        LocalAgent localAgent = localAgentMap.get(agentId(mode));

        if (localAgent == null) {
            return Optional.empty();
        }

        String threadId = chat.getThreadId();
        String runId = UUID.randomUUID()
            .toString();

        Optional<InFlightRun> runOptional = inFlightRunRegistry.tryStart(threadId, runId);

        if (runOptional.isEmpty()) {
            deferContinuation(approval, chat, workspaceId, message);

            return Optional.empty();
        }

        UserMessage userMessage = new UserMessage();

        userMessage.setId(UUID.randomUUID()
            .toString());
        userMessage.setContent(message);

        State state = new State();

        state.set("mode", mode.name());

        if (approval.getLlmProvider() != null) {
            state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, approval.getLlmProvider());
        }

        if (approval.getLlmModel() != null) {
            state.set(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY, approval.getLlmModel());
        }

        AiHubRunState.inject(
            state, approval.getRequestedByUserId(), workspaceId, threadId,
            AiHubRunState.clampEnvironmentId((long) approval.getEnvironment()), TenantContext.getCurrentTenantId(),
            chat.getUserId());

        AgUiParameters agUiParameters = new AgUiParameters();

        agUiParameters.setThreadId(threadId);
        agUiParameters.setRunId(runId);
        agUiParameters.setMessages(List.of(userMessage));
        agUiParameters.setState(state);

        try {
            chatStreamer.start(localAgent, agUiParameters, runOptional.get());
        } catch (RuntimeException exception) {
            runOptional.get()
                .complete();

            throw exception;
        }

        return Optional.of(runId);
    }

    private void deferContinuation(AiHubToolApproval approval, AiHubChat chat, long workspaceId, String message) {
        String tenantId = TenantContext.getCurrentTenantId();

        SecurityContext currentSecurityContext = SecurityContextHolder.getContext();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(currentSecurityContext.getAuthentication());

        Runnable deferredContinuation = () -> TenantContext.runWithTenantId(
            tenantId, () -> startDeferredContinuation(approval, chat, workspaceId, message));

        inFlightRunRegistry.runWhenReleased(
            chat.getThreadId(), new DelegatingSecurityContextRunnable(deferredContinuation, securityContext));
    }

    private void startDeferredContinuation(
        AiHubToolApproval approval, AiHubChat chat, long workspaceId, String message) {

        Optional<AiHubChat> chatOptional = chatService.findByThreadId(chat.getThreadId());

        if (chatOptional.isEmpty()) {
            log.info(
                "Dropping the deferred continuation of approval id={}: chat thread={} no longer exists",
                approval.getId(), chat.getThreadId());

            return;
        }

        startContinuation(approval, chatOptional.get(), workspaceId, message);
    }

    private static String agentId(Mode mode) {
        return switch (mode) {
            case ASK -> AiHubAgentType.AI_HUB_ASK.key();
            case BUILD -> AiHubAgentType.AI_HUB_BUILD.key();
        };
    }
}
