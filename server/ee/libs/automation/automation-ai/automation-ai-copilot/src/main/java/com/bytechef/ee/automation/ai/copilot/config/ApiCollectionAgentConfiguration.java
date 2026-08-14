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
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.ee.automation.ai.tool.ApiCollectionToolCallbacksFactory;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the API Collection Copilot panel source agents ({@code api_collection_ask}/ {@code api_collection_build}).
 * Lives in EE (not the CE {@code CopilotConfiguration}, where the CE panel source agents live) because
 * {@link ApiCollectionToolCallbacksFactory} and the {@link ApiCollectionFacade} it wraps are EE.
 *
 * <p>
 * This configuration is purely additive: it does not touch {@code ApiCollectionSubAgentConfiguration}'s existing
 * {@code apiCollectionAgentChatClient} bean or {@code createApiCollectionAgentToolCallback} factory methods, which back
 * the {@code api_collection_agent} subagent consumed by AI Hub and the management MCP server. Those keep building their
 * own tool list independently of {@link ApiCollectionToolCallbacksFactory}.
 * </p>
 *
 * <p>
 * {@link ApiCollectionFacade} is optional — mirroring {@code ApiCollectionSubAgentConfiguration}'s
 * {@code apiCollectionAgentChatClient} bean, every bean here carries
 * {@code @ConditionalOnBean(ApiCollectionFacade.class)} so the whole slice skips silently when the api-platform feature
 * module is absent, instead of failing application startup with an unsatisfied dependency. The condition is repeated on
 * the factory bean and both agent beans (rather than only the factory) because a Spring {@code @Bean} method with a
 * required, unconditioned parameter fails context refresh if that parameter's bean was never registered — the ASK/BUILD
 * agent beans both take {@link ApiCollectionToolCallbacksFactory} as a hard constructor argument, so they must carry
 * the same condition as the factory they depend on.
 * </p>
 *
 * <p>
 * Gated on {@code bytechef.ai.copilot.enabled} rather than an OR with {@code bytechef.ai.hub.enabled}, matching
 * {@code ProjectDeploymentAgentConfiguration} and {@code McpServerAgentConfiguration}: an OR-gate would let this
 * configuration register with {@code hub.enabled=true, copilot.enabled=false}, when nothing in the AI Hub surface
 * consumes these panel-agent beans.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class ApiCollectionAgentConfiguration {

    @Value("classpath:prompt_api_collection_ask.txt")
    private Resource promptApiCollectionAskResource;

    @Value("classpath:prompt_api_collection_build.txt")
    private Resource promptApiCollectionBuildResource;

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

        String name = Source.API_COLLECTION.name() + "_" + Mode.ASK.name();

        return SliceSpringAIAgent.builder()
            .agentId(name.toLowerCase())
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

        String name = Source.API_COLLECTION.name() + "_" + Mode.BUILD.name();

        return SliceSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptApiCollectionBuildResource))
            .state(state)
            .toolCallbacks(buildToolCallbacks(securityContextRehydrator, apiCollectionToolCallbacksFactory))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    /**
     * Package-private so {@code ApiCollectionAgentConfigurationTest} can assert on the resolved tool names directly —
     * {@link SliceSpringAIAgent} does not expose its wrapped {@link ToolCallback} list.
     */
    List<ToolCallback> askToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator,
        ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory) {

        return wrapToolCallbacks(securityContextRehydrator, apiCollectionToolCallbacksFactory.readToolCallbacks());
    }

    /**
     * Package-private so {@code ApiCollectionAgentConfigurationTest} can assert on the resolved tool names directly —
     * {@link SliceSpringAIAgent} does not expose its wrapped {@link ToolCallback} list.
     */
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
}
