/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.agent.ManagerSliceSpringAIAgent;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ee.automation.ai.tool.ApiCollectionToolCallbacksFactory;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ApiCollectionAgentConfigurationTest {

    private final ApiCollectionAgentConfiguration configuration = newConfiguration();

    private final SecurityContextRehydrator securityContextRehydrator = mock(SecurityContextRehydrator.class);

    private final ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory =
        new ApiCollectionToolCallbacksFactory(mock(ApiCollectionFacade.class));

    @Test
    void testAskAgentUsesReadToolsAndBuildAgentUsesWriteTools() throws AGUIException {
        ManagerSliceSpringAIAgent askAgent = configuration.apiCollectionAskSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), apiCollectionToolCallbacksFactory,
            securityContextRehydrator, emptyProvider());

        ManagerSliceSpringAIAgent buildAgent = configuration.apiCollectionBuildSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), apiCollectionToolCallbacksFactory,
            securityContextRehydrator, emptyProvider());

        assertThat(askAgent.getAgentId()).isEqualTo("api_collection_ask");
        assertThat(buildAgent.getAgentId()).isEqualTo("api_collection_build");

        List<String> buildToolNames = toolNames(
            configuration.buildToolCallbacks(securityContextRehydrator, apiCollectionToolCallbacksFactory));

        assertThat(buildToolNames).containsExactlyInAnyOrder(
            "listApiCollections", "createApiCollection", "cloneApiCollection");
    }

    @Test
    void testAskAgentToolsAreReadOnly() {
        List<String> askToolNames = toolNames(
            configuration.askToolCallbacks(securityContextRehydrator, apiCollectionToolCallbacksFactory));

        assertThat(askToolNames).containsExactly("listApiCollections");
    }

    private static ApiCollectionAgentConfiguration newConfiguration() {
        ApiCollectionAgentConfiguration configuration = new ApiCollectionAgentConfiguration();

        setResourceField(configuration, "promptApiCollectionAskResource", "prompt_api_collection_ask.txt");
        setResourceField(configuration, "promptApiCollectionBuildResource", "prompt_api_collection_build.txt");

        return configuration;
    }

    private static void setResourceField(
        ApiCollectionAgentConfiguration configuration, String fieldName, String classpathResource) {

        try {
            Field field = ApiCollectionAgentConfiguration.class.getDeclaredField(fieldName);

            field.setAccessible(true);
            field.set(configuration, new ClassPathResource(classpathResource));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
