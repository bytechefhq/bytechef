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

    public void recordToolCallId(long approvalId, String toolCallId) {
        approvalService.recordToolCallId(approvalId, toolCallId);
    }

    public ToolCallback wrap(ToolCallback callback) {
        if (callback instanceof AiHubApprovalGateToolCallback) {
            return callback;
        }

        return new AiHubApprovalGateToolCallback(callback, policy, approvalService, chatService);
    }
}
