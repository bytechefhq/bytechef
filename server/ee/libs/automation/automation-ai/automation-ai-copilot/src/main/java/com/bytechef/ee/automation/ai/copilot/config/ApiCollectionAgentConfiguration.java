/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.agent.SliceSpringAIAgent;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.automation.ai.tool.ApiCollectionToolCallbacksFactory;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")
public class ApiCollectionAgentConfiguration {

    @Value("classpath:prompt_api_collection_ask.txt")
    private Resource promptApiCollectionAskResource;

    @Value("classpath:prompt_api_collection_build.txt")
    private Resource promptApiCollectionBuildResource;

    private static final Set<String> WORKSPACE_SCOPED_API_COLLECTION_TOOL_NAMES = Set.of("listApiCollections");

    private final State state = new State();

    @Bean
    @ConditionalOnBean(ApiCollectionFacade.class)
    ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory(ApiCollectionFacade apiCollectionFacade) {
        return new ApiCollectionToolCallbacksFactory(apiCollectionFacade);
    }

    @Bean
    @ConditionalOnBean(ApiCollectionFacade.class)
    SliceSpringAIAgent apiCollectionAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return SliceSpringAIAgent.builder()
            .agentId(CopilotAgentType.API_COLLECTION_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptApiCollectionAskResource))
            .state(state)
            .toolCallbacks(askToolCallbacks(securityContextRehydrator, apiCollectionToolCallbacksFactory))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    @ConditionalOnBean(ApiCollectionFacade.class)
    SliceSpringAIAgent apiCollectionBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return SliceSpringAIAgent.builder()
            .agentId(CopilotAgentType.API_COLLECTION_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptApiCollectionBuildResource))
            .state(state)
            .toolCallbacks(buildToolCallbacks(securityContextRehydrator, apiCollectionToolCallbacksFactory))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    List<ToolCallback> askToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator,
        ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory) {

        return wrapToolCallbacks(securityContextRehydrator, apiCollectionToolCallbacksFactory.readToolCallbacks());
    }

    List<ToolCallback> buildToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator,
        ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory) {

        return wrapToolCallbacks(securityContextRehydrator, apiCollectionToolCallbacksFactory.writeToolCallbacks());
    }

    private List<ToolCallback> wrapToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator, List<ToolCallback> toolCallbacks) {

        List<ToolCallback> wrapped = new ArrayList<>(toolCallbacks.size());

        for (ToolCallback toolCallback : toolCallbacks) {
            wrapped.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
        }

        return wrapped;
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read api collection prompt resource: " + resource.getDescription(), exception);
        }
    }

    @Bean
    @ConditionalOnBean(ApiCollectionFacade.class)
    McpServerToolCallbackContributor apiCollectionFlatCrudMcpContributor(
        ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory,
        WorkspaceFacade workspaceFacade) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : apiCollectionToolCallbacksFactory.writeToolCallbacks()) {
                String name = toolCallback.getToolDefinition()
                    .name();

                toolCallbacks.add(
                    WORKSPACE_SCOPED_API_COLLECTION_TOOL_NAMES.contains(name)
                        ? new WorkspaceScopedFlatToolCallback(toolCallback, workspaceFacade)
                        : toolCallback);
            }

            return toolCallbacks;
        };
    }
}
