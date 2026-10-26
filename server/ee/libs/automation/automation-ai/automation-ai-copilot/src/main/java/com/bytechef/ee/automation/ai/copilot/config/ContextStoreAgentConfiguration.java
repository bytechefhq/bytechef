/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.ContextStoreSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.ee.automation.ai.tool.contextstore.ContextStoreToolCallbacksFactory;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreFacade;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreQueryService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSemanticSearchService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} and ${bytechef.context-store.enabled:false}")
public class ContextStoreAgentConfiguration {

    @Value("classpath:prompt_context_store_ask.txt")
    private Resource promptContextStoreAskResource;

    @Value("classpath:prompt_context_store_build.txt")
    private Resource promptContextStoreBuildResource;

    private final State state = new State();

    @Bean
    ContextStoreToolCallbacksFactory contextStoreToolCallbacksFactory(
        WorkspaceContextStoreSourceService workspaceContextStoreSourceService,
        ContextStoreQueryService contextStoreQueryService,
        ContextStoreSourceFacade contextStoreSourceFacade,
        ContextStoreFacade contextStoreFacade,
        ObjectProvider<ContextStoreSemanticSearchService> contextStoreSemanticSearchServiceProvider,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return new ContextStoreToolCallbacksFactory(
            workspaceContextStoreSourceService, contextStoreQueryService, contextStoreSourceFacade,
            contextStoreFacade, contextStoreSemanticSearchServiceProvider.getIfAvailable(),
            clusterElementDefinitionService);
    }

    @Bean
    ContextStoreSpringAIAgent contextStoreAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        ContextStoreToolCallbacksFactory contextStoreToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.CONTEXT_STORE.name() + "_" + Mode.ASK.name();

        return ContextStoreSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptContextStoreAskResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, contextStoreToolCallbacksFactory.readToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ContextStoreSpringAIAgent contextStoreBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        ContextStoreToolCallbacksFactory contextStoreToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.CONTEXT_STORE.name() + "_" + Mode.BUILD.name();

        return ContextStoreSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptContextStoreBuildResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, contextStoreToolCallbacksFactory.writeToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
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
                "Failed to read context store prompt resource: " + resource.getDescription(), exception);
        }
    }
}
