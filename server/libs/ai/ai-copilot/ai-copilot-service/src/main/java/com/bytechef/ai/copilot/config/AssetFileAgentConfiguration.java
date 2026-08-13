/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.ManagerSliceSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.automation.ai.tool.AssetFileToolCallbacksFactory;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.automation.ai.tool.ToolArtifactRecorder;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
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
 * Registers the asset-file Copilot panel source agents ({@code asset_file_ask}/{@code asset_file_build}) and the
 * asset-file subagent {@link ChatClient} beans consumed by the AI Hub root agents and the management MCP server. Lives
 * in CE because {@link AssetFileToolCallbacksFactory} and {@link AssetFileFacade} are CE; the optional
 * {@link ToolArtifactRecorder} hook is a CE SPI that the EE AI Hub recorder plugs into when present.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")
public class AssetFileAgentConfiguration {

    @Value("classpath:prompt_asset_file_ask.txt")
    private Resource promptAssetFileAskResource;

    @Value("classpath:prompt_asset_file_build.txt")
    private Resource promptAssetFileBuildResource;

    private final State state = new State();

    @Bean
    AssetFileToolCallbacksFactory assetFileToolCallbacksFactory(
        AssetFileFacade assetFileFacade, ObjectProvider<ToolArtifactRecorder> toolArtifactRecorderProvider) {

        return new AssetFileToolCallbacksFactory(
            assetFileFacade, toolArtifactRecorderProvider.getIfAvailable());
    }

    @Bean
    ManagerSliceSpringAIAgent assetFileAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, AssetFileToolCallbacksFactory assetFileToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.ASSET_FILE.name() + "_" + Mode.ASK.name();

        return ManagerSliceSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptAssetFileAskResource))
            .state(state)
            .toolCallbacks(askToolCallbacks(securityContextRehydrator, assetFileToolCallbacksFactory))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .sourceOrdinal(AutomationToolInvocationContext.SOURCE_ORDINAL_FILES)
            .build();
    }

    @Bean
    ManagerSliceSpringAIAgent assetFileBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, AssetFileToolCallbacksFactory assetFileToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.ASSET_FILE.name() + "_" + Mode.BUILD.name();

        return ManagerSliceSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptAssetFileBuildResource))
            .state(state)
            .toolCallbacks(buildToolCallbacks(securityContextRehydrator, assetFileToolCallbacksFactory))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .sourceOrdinal(AutomationToolInvocationContext.SOURCE_ORDINAL_FILES)
            .build();
    }

    @Bean
    ChatClient assetFileAskSubAgentChatClient(
        ChatModel chatModel, AssetFileToolCallbacksFactory assetFileToolCallbacksFactory) {

        return ChatClient.builder(chatModel)
            .defaultSystem(readPrompt(promptAssetFileAskResource))
            .defaultToolCallbacks(assetFileToolCallbacksFactory.readToolCallbacks())
            .build();
    }

    @Bean
    ChatClient assetFileBuildSubAgentChatClient(
        ChatModel chatModel, AssetFileToolCallbacksFactory assetFileToolCallbacksFactory) {

        return ChatClient.builder(chatModel)
            .defaultSystem(readPrompt(promptAssetFileBuildResource))
            .defaultToolCallbacks(assetFileToolCallbacksFactory.writeToolCallbacks())
            .build();
    }

    /**
     * Package-private so {@code AssetFileAgentConfigurationTest} can assert on the resolved tool names directly —
     * {@link ManagerSliceSpringAIAgent} does not expose its wrapped {@link ToolCallback} list.
     */
    List<ToolCallback> askToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator,
        AssetFileToolCallbacksFactory assetFileToolCallbacksFactory) {

        return wrapToolCallbacks(securityContextRehydrator, assetFileToolCallbacksFactory.readToolCallbacks());
    }

    /**
     * Package-private so {@code AssetFileAgentConfigurationTest} can assert on the resolved tool names directly —
     * {@link ManagerSliceSpringAIAgent} does not expose its wrapped {@link ToolCallback} list.
     */
    List<ToolCallback> buildToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator,
        AssetFileToolCallbacksFactory assetFileToolCallbacksFactory) {

        return wrapToolCallbacks(securityContextRehydrator, assetFileToolCallbacksFactory.writeToolCallbacks());
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
                "Failed to read asset file prompt resource: " + resource.getDescription(), exception);
        }
    }
}
