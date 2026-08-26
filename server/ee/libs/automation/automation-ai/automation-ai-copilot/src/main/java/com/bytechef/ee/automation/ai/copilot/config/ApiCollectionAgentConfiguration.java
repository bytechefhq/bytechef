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
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the API Collection Copilot panel source agents ({@code api_collection_ask}/ {@code api_collection_build})
 * and the {@link ApiCollectionToolCallbacksFactory} bean they, the AI Hub ASK/BUILD agents, and the management MCP
 * server (see {@link #apiCollectionFlatCrudMcpContributor}) all now consume. Lives in EE (not the CE
 * {@code CopilotConfiguration}, where the CE panel source agents live) because
 * {@link ApiCollectionToolCallbacksFactory} and the {@link ApiCollectionFacade} it wraps are EE.
 *
 * <p>
 * The former {@code api_collection_agent} delegate (dissolved ticket 732, Task 2 of the CRUD-delegate unwind) used to
 * build its own independent tool list wrapping the same three {@link ToolCallback} classes this factory's
 * {@link ApiCollectionToolCallbacksFactory#writeToolCallbacks()} returns — this factory is no longer merely additive to
 * it, it is the sole source for every surface.
 * </p>
 *
 * <p>
 * {@link ApiCollectionFacade} is optional — every bean here carries
 * {@code @ConditionalOnBean(ApiCollectionFacade.class)} so the whole slice skips silently when the api-platform feature
 * module is absent, instead of failing application startup with an unsatisfied dependency. The condition is repeated on
 * the factory bean and every agent/contributor bean (rather than only the factory) because a Spring {@code @Bean}
 * method with a required, unconditioned parameter fails context refresh if that parameter's bean was never registered —
 * every bean here that takes {@link ApiCollectionToolCallbacksFactory} as a hard constructor argument must carry the
 * same condition as the factory it depends on.
 * </p>
 *
 * <p>
 * Gated on {@code bytechef.ai.copilot.enabled} rather than an OR with {@code bytechef.ai.hub.enabled}, matching
 * {@code ProjectDeploymentAgentConfiguration} and {@code McpServerAgentConfiguration}. Since ticket 732 Task 2 this IS
 * a real coupling, not a historical accident to be revisited: the AI Hub ASK/BUILD agents resolve
 * {@link ApiCollectionToolCallbacksFactory} via {@code ObjectProvider} from {@code AiHubConfiguration} (mirroring how
 * {@code McpServerToolCallbacksFactory} is already consumed the same way for the MCP-server-CRUD flattening, ticket 732
 * Task 3), so a deployment with AI Hub enabled but Copilot disabled silently loses the three API-collection tools from
 * both hub surfaces — an accepted, pre-existing-pattern trade-off, not a regression this task introduces.
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

    /**
     * The one of the three flat API-collection CRUD tools that reads
     * {@code AutomationToolInvocationContext.workspaceId()} (see {@link #apiCollectionFlatCrudMcpContributor}'s
     * javadoc). {@code createApiCollection} resolve everything from an id already in their own input and need no
     * wrapping.
     */
    private static final Set<String> WORKSPACE_SCOPED_API_COLLECTION_TOOL_NAMES = Set.of("listApiCollections");

    /**
     * Flattens the API-collection CRUD tool set (ticket 732, Task 2 of the CRUD-delegate unwind) onto the management
     * MCP surface, replacing the dissolved {@code api_collection_agent} delegate's contribution — formerly a single
     * {@code SubAgentToolCallback} named {@code api_collection_agent}, contributed from the now-deleted
     * {@code ApiCollectionSubAgentMcpContributorConfiguration} in {@code automation-ai-tool}. This surface has no
     * schema-count pressure (MCP clients page {@code tools/list}), so all three of
     * {@link ApiCollectionToolCallbacksFactory#writeToolCallbacks()} are registered flat — {@code listApiCollections},
     * {@code createApiCollection}.
     *
     * <p>
     * Unlike the AI Hub chat surface, no {@code ToolContext} exists here to carry
     * {@code AutomationToolInvocationContext}'s workspace scope, so the one tool that reads it
     * ({@code listApiCollections}, see {@link #WORKSPACE_SCOPED_API_COLLECTION_TOOL_NAMES}) is wrapped in
     * {@link WorkspaceScopedFlatToolCallback}, which merges an optional {@code workspaceId}/{@code environment} into
     * its existing schema without discarding its other field. {@code createApiCollection} need no wrapping — they
     * resolve everything from an id already in their own input, a pre-existing property of these tool classes
     * unaffected by dissolving the delegate that used to wrap them (see this class's own javadoc).
     * </p>
     */
    @Bean
    @ConditionalOnBean(ApiCollectionFacade.class)
    McpServerToolCallbackContributor apiCollectionFlatCrudMcpContributor(
        ApiCollectionToolCallbacksFactory apiCollectionToolCallbacksFactory, WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : apiCollectionToolCallbacksFactory.writeToolCallbacks()) {
                String name = toolCallback.getToolDefinition()
                    .name();

                toolCallbacks.add(
                    WORKSPACE_SCOPED_API_COLLECTION_TOOL_NAMES.contains(name)
                        ? new WorkspaceScopedFlatToolCallback(toolCallback, workspaceService)
                        : toolCallback);
            }

            return toolCallbacks;
        };
    }
}
