/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGate;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGateToolCallback;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalPolicy;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubToolCallbackWrappersTest {

    private final ToolCallback callback = callback();
    private final AiHubApprovalGate approvalGate = new AiHubApprovalGate(
        mock(AiHubToolApprovalPolicy.class), mock(AiHubToolApprovalService.class), mock(AiHubChatService.class));

    @Test
    void testGatedWrapPutsTheGateBetweenRehydrationAndTheNonEmptyGuard() {
        ToolCallback wrapped = AiHubToolCallbackWrappers.wrap(callback, null, approvalGate);

        assertThat(wrapped).isInstanceOf(AiHubApprovalGateToolCallback.class);
        assertThat(((AiHubApprovalGateToolCallback) wrapped).getDelegate()).isInstanceOf(NonEmptyToolCallback.class);
    }

    @Test
    void testTwoArgumentWrapAppliesNoGate() {
        ToolCallback wrapped = AiHubToolCallbackWrappers.wrap(callback, null);

        assertThat(wrapped).isInstanceOf(NonEmptyToolCallback.class);
        assertThat(((NonEmptyToolCallback) wrapped).getDelegate()).isSameAs(callback);
    }

    @Test
    void testRehydrationIsTheOutermostLayer() {
        ToolCallback wrapped = AiHubToolCallbackWrappers.wrap(
            callback, mock(SecurityContextRehydrator.class), approvalGate);

        assertThat(wrapped).isInstanceOf(RehydrateContextToolCallback.class);
    }

    @Test
    void testRewrappingDoesNotNestTheGate() {
        ToolCallback gated = approvalGate.wrap(callback);

        assertThat(approvalGate.wrap(gated)).isSameAs(gated);
    }

    private static ToolCallback callback() {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition toolDefinition = mock(ToolDefinition.class);

        when(toolDefinition.name()).thenReturn("deleteProject");
        when(callback.getToolDefinition()).thenReturn(toolDefinition);

        return callback;
    }
}
