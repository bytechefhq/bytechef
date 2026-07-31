/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayModelService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PromptBasedModerationClassifierTest {

    private final AiGatewayChatModelFactory chatModelFactory = mock(AiGatewayChatModelFactory.class);
    private final AiGatewayModelService modelService = mock(AiGatewayModelService.class);
    private final AiGatewayProviderService providerService = mock(AiGatewayProviderService.class);
    private final ChatModel chatModel = mock(ChatModel.class);

    private final PromptBasedModerationClassifier classifier = new PromptBasedModerationClassifier(
        chatModelFactory, modelService, providerService, "moderation-model-1");

    @BeforeEach
    void beforeEach() {
        AiGatewayModel model = mock(AiGatewayModel.class);

        when(model.getProviderId()).thenReturn(5L);
        when(modelService.findByModelIdentifier("moderation-model-1")).thenReturn(Optional.of(model));
        when(providerService.getProvider(5L)).thenReturn(mock(AiGatewayProvider.class));
        when(chatModelFactory.getChatModel(org.mockito.ArgumentMatchers.any())).thenReturn(chatModel);
    }

    @Test
    void testUnsafeVerdictFlagsContent() {
        when(chatModel.call(anyString())).thenReturn("UNSAFE");

        assertThat(classifier.isFlagged("harmful text")).isTrue();
    }

    @Test
    void testSafeVerdictDoesNotFlagContent() {
        when(chatModel.call(anyString())).thenReturn("SAFE");

        assertThat(classifier.isFlagged("hello world")).isFalse();
    }

    @Test
    void testClassificationErrorFailsOpen() {
        when(chatModel.call(anyString())).thenThrow(new RuntimeException("provider down"));

        assertThat(classifier.isFlagged("anything")).isFalse();
    }

    @Test
    void testUnknownModerationModelFailsOpen() {
        when(modelService.findByModelIdentifier("moderation-model-1")).thenReturn(Optional.empty());
        when(providerService.getProvider(anyLong())).thenReturn(mock(AiGatewayProvider.class));

        assertThat(classifier.isFlagged("anything")).isFalse();
    }

    @Test
    void testBlankContentIsNotFlagged() {
        assertThat(classifier.isFlagged("  ")).isFalse();
        assertThat(classifier.isFlagged(null)).isFalse();
    }
}
