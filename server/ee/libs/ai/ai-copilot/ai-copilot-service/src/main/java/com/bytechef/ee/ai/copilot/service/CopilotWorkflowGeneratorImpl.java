/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.service;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.bytechef.ee.ai.copilot.util.CopilotStateKeys;
import com.bytechef.ee.ai.copilot.util.Mode;
import com.bytechef.ee.ai.copilot.util.Source;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Synchronously drives the {@code workflow_editor_build} agent end-to-end so the embedded public API can return a fresh
 * workflow uuid after the agent has populated the workflow via its tools.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotWorkflowGeneratorImpl implements CopilotWorkflowGenerator {

    private static final Logger log = LoggerFactory.getLogger(CopilotWorkflowGeneratorImpl.class);

    private static final long DEFAULT_TIMEOUT_MINUTES = 10;

    private final Map<String, LocalAgent> localAgentMap;

    @SuppressFBWarnings("EI")
    public CopilotWorkflowGeneratorImpl(List<LocalAgent> localAgents) {
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent));
    }

    @Override
    public void generateWorkflow(
        String workflowId, String prompt, @Nullable String systemPrompt, Set<String> allowedComponentNames) {
        String agentId = (Source.WORKFLOW_EDITOR.name() + "_" + Mode.BUILD.name()).toLowerCase();

        LocalAgent localAgent = localAgentMap.get(agentId);

        if (localAgent == null) {
            throw new IllegalStateException("Workflow editor BUILD agent not available: " + agentId);
        }

        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", workflowId);
        stateMap.put("mode", Mode.BUILD.name());
        stateMap.put("autonomous", true);

        stateMap.put(CopilotStateKeys.STATE_TENANT_ID, TenantContext.getCurrentTenantId());

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication != null) {
            stateMap.put(CopilotStateKeys.STATE_AUTHENTICATION, authentication);
        }

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            stateMap.put(CopilotStateKeys.STATE_ADDITIONAL_SYSTEM_PROMPT, systemPrompt.strip());
        }

        if (allowedComponentNames != null && !allowedComponentNames.isEmpty()) {
            stateMap.put(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, allowedComponentNames);
        }

        State state = new State(stateMap);

        UserMessage userMessage = new UserMessage();

        UUID uuid = UUID.randomUUID();

        userMessage.setId(uuid.toString());

        userMessage.setContent(prompt);

        RunAgentParameters parameters = RunAgentParameters.builder()
            .threadId(uuid.toString())
            .runId(uuid.toString())
            .messages(List.<BaseMessage>of(userMessage))
            .state(state)
            .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        localAgent.runAgent(parameters, new AgentSubscriber() {

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                latch.countDown();
            }

            @Override
            public void onRunFailed(AgentSubscriberParams params, Throwable throwable) {
                errorRef.set(throwable);

                latch.countDown();
            }
        });

        try {
            if (!latch.await(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                throw new IllegalStateException(
                    "Workflow generation timed out after " + DEFAULT_TIMEOUT_MINUTES + " minutes for workflowId="
                        + workflowId);
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException("Workflow generation was interrupted for workflowId=" + workflowId,
                interruptedException);
        }

        Throwable error = errorRef.get();

        if (error != null) {
            log.error("Workflow generation failed for workflowId={}", workflowId, error);

            throw new IllegalStateException(
                "Workflow generation failed: " + error.getMessage(), error);
        }
    }
}
