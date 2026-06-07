/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.mcp.tool.automation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.mcp.tool.spi.ToolCallbackContributor;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolCallbackContributorConfigurationTest {

    private final ToolCallbackContributorConfiguration configuration =
        new ToolCallbackContributorConfiguration();

    @Test
    void contributesAgentCallbacksWhenChatClientsPresent() {
        ToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), present(mock(ChatClient.class)), present(mock(ChatClient.class)),
            present(mock(ChatClient.class)), present(mock(ChatClient.class)), present(mock(ChatClient.class)),
            present(mock(ChatClient.class)));

        assertThat(contributor.getToolCallbacks()).hasSize(6);
    }

    @Test
    void contributesNothingWhenAllAbsent() {
        ToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(),
            emptyProvider());

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> present(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        doAnswer(invocation -> {
            Consumer<T> consumer = invocation.getArgument(0);

            consumer.accept(value);

            return null;
        }).when(provider)
            .ifAvailable(any());

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
