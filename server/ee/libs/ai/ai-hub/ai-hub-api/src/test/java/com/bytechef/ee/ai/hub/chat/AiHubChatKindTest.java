/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatKindTest {

    @Test
    void testOrdinalsMatchTheStoredValues() {
        assertThat(AiHubChatKind.STANDARD.ordinal()).isZero();
        assertThat(AiHubChatKind.WORKFLOW_CHAT.ordinal()).isEqualTo(1);
        assertThat(AiHubChatKind.values()).hasSize(2);
    }

    @Test
    void testOnlyWorkflowChatIsWebhookBridged() {
        assertThat(AiHubChatKind.WORKFLOW_CHAT.isWebhookBridged()).isTrue();
        assertThat(AiHubChatKind.STANDARD.isWebhookBridged()).isFalse();
    }

    @Test
    void testNewChatIsStandard() {
        assertThat(new AiHubChat(7L).getKind()).isEqualTo(AiHubChatKind.STANDARD);
    }

    @Test
    void testWorkflowColumnsRoundTrip() {
        AiHubChat chat = new AiHubChat(7L);

        chat.setKind(AiHubChatKind.WORKFLOW_CHAT);
        chat.setWorkflowExecutionId("tenant:uuid");
        chat.setProjectDeploymentId(9L);

        assertThat(chat.getKind()).isEqualTo(AiHubChatKind.WORKFLOW_CHAT);
        assertThat(chat.getWorkflowExecutionId()).isEqualTo("tenant:uuid");
        assertThat(chat.getProjectDeploymentId()).isEqualTo(9L);
    }
}
