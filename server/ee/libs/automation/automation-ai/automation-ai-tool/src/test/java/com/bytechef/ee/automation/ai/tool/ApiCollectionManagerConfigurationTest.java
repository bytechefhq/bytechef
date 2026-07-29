/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Covers the api_collection_manager subagent configuration: ChatClient construction succeeds against mocks and the
 * delegate ToolCallback carries the agent-type key the ai_hub BUILD prompt references.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiCollectionManagerConfigurationTest {

    @Test
    void testApiCollectionManagerChatClientIsBuilt() {
        Resource promptResource = new ByteArrayResource(
            "You are the api_collection_manager subagent.".getBytes(StandardCharsets.UTF_8), "test prompt resource");

        ApiCollectionManagerConfiguration configuration = new ApiCollectionManagerConfiguration();

        assertThatNoException().isThrownBy(
            () -> configuration.apiCollectionManagerChatClient(
                mock(ApiCollectionFacade.class), mock(ChatModel.class), promptResource));
    }

    @Test
    void testApiCollectionManagerToolCallbackIsNamedCorrectly() {
        ToolCallback toolCallback = ApiCollectionManagerConfiguration.createApiCollectionManagerToolCallback(
            mock(ChatClient.class));

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("api_collection_manager");
    }
}
