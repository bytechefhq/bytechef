/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(ObjectMapperSetupExtension.class)
class ContextStoreAgentToolCallbackTest {

    @Test
    void toolDefinitionExposesContextStoreAgent() {
        ContextStoreAgentToolCallback toolCallback =
            new ContextStoreAgentToolCallback(Mockito.mock(ChatClient.class));

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("context_store_agent");
    }

    @Test
    void blankRequestReturnsError() {
        ContextStoreAgentToolCallback toolCallback =
            new ContextStoreAgentToolCallback(Mockito.mock(ChatClient.class));

        String result = toolCallback.call("{\"request\": \"  \"}", null);

        assertThat(result).contains("request is required");
    }
}
