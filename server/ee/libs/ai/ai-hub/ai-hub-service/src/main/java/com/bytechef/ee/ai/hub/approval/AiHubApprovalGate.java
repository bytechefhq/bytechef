/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Spring bean facade over {@link AiHubApprovalGateToolCallback}: wraps a {@link ToolCallback} with the tool approval
 * gate. Registered as a bean (rather than constructing {@link AiHubApprovalGateToolCallback} directly at each call
 * site) so {@link com.bytechef.ee.ai.hub.agent.AiHubToolCallbackWrappers} and its callers can depend on one
 * collaborator that is simply absent when the AI Hub is not enabled.
 *
 * <p>
 * Every deployment running the hub gates the built-in tool names ({@link AiHubToolApprovalDefaults#DEFAULT_TOOL_NAMES})
 * plus four destructive-verb prefixes: a destructive tool call waits for a person by default rather than by opt-in. A
 * workspace lifts a default with an {@code EXEMPT} rule.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubApprovalGate {

    private final AiHubToolApprovalPolicy policy;
    private final AiHubToolApprovalService approvalService;
    private final AiHubChatService chatService;

    @SuppressFBWarnings("EI")
    public AiHubApprovalGate(
        AiHubToolApprovalPolicy policy, AiHubToolApprovalService approvalService, AiHubChatService chatService) {

        this.policy = policy;
        this.approvalService = approvalService;
        this.chatService = chatService;
    }

    /**
     * Wraps {@code callback} with the tool approval gate. Returns {@code callback} unchanged when it is already
     * wrapped, so re-wrapping at a second registration site doesn't nest the gate twice.
     */
    public ToolCallback wrap(ToolCallback callback) {
        if (callback instanceof AiHubApprovalGateToolCallback) {
            return callback;
        }

        return new AiHubApprovalGateToolCallback(callback, policy, approvalService, chatService);
    }
}
