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
 * Registers the Context Store Copilot panel source agents ({@code context_store_ask}/{@code context_store_build}) and
 * the {@link ContextStoreToolCallbacksFactory} bean they (and the AI Hub's flat context-store registrations, see
 * {@code AiHubConfiguration#contextStoreFlatCrudToolCallbacks}/{@code #contextStoreCatalogToolCallbacks}) consume.
 * These beans live in EE (not the CE {@code CopilotConfiguration}, where the other Copilot panel source agents live)
 * because {@link ContextStoreToolCallbacksFactory} and the context-store services/facades it wraps are EE.
 *
 * <p>
 * The two subagent {@code ChatClient} beans that used to live here ({@code contextStoreAskSubAgentChatClient} /
 * {@code contextStoreBuildSubAgentChatClient}) are gone (ticket 732, CRUD-delegate-unwind Task 7) — they backed only
 * the now-dissolved {@code context_store_agent} delegate (both the AI Hub chat surface's registration and the
 * management MCP surface's), which is replaced by flat/catalog tool-callback registrations straight off
 * {@link ContextStoreToolCallbacksFactory}. The panel agents below never consumed those beans — they already call the
 * factory's {@code readToolCallbacks()}/{@code writeToolCallbacks()} directly — so they are unaffected.
 * </p>
 *
 * <p>
 * Gated so the beans exist when either the Copilot panel or the AI Hub surface is enabled, since both consume these
 * agents — but only while the context-store feature itself is on: every context-store service and facade injected here
 * is registered only when {@code bytechef.context-store.enabled=true}, so activating without it fails server startup.
 * {@code @ConditionalOnEEVersion} is part of that same gate: the authorization-enforcing
 * {@link ContextStoreSourceFacade} the tool factory now needs is itself EE-version-gated (as is
 * {@code ContextStoreSourceGraphQlController}, the other consumer), so activating on a non-{@code ee} edition would
 * fail startup on an unsatisfied dependency. Both surfaces tolerate the absence of these beans
 * ({@code ObjectProvider}/{@code List} injection).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnExpression("(${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false})"
    + " and ${bytechef.context-store.enabled:false}")
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
