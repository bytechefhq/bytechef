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
import com.bytechef.ai.copilot.agent.KnowledgeBaseSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.automation.ai.tool.ToolArtifactRecorder;
import com.bytechef.automation.ai.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
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
 * Registers the Knowledge Base Copilot panel source agents ({@code knowledge_base_ask}/{@code knowledge_base_build})
 * and the {@link KnowledgeBaseToolCallbacksFactory} bean consumed by the panel agents, the ai_hub agents (flat/catalog
 * — ticket 732, CRUD-delegate-unwind Task 6), and the management MCP server (flat). Lives in CE alongside
 * {@code CopilotConfiguration} because {@link KnowledgeBaseToolCallbacksFactory} and the knowledge-base
 * services/facades it wraps are CE; the optional {@code ToolArtifactRecorder} hook is a CE SPI that the EE AI Hub
 * recorder plugs into when present.
 *
 * <p>
 * The {@code knowledgeBaseAskSubAgentChatClient}/{@code knowledgeBaseBuildSubAgentChatClient} beans that used to live
 * here fed only the now-dissolved {@code knowledge_base_agent} delegate wrapper — the panel agent beans below have
 * always called {@link KnowledgeBaseToolCallbacksFactory}'s read/write lists directly, so removing the two subagent
 * beans leaves the panel untouched.
 * </p>
 *
 * <p>
 * Gated so the beans exist when either the Copilot panel or the AI Hub surface is enabled, since both consume these
 * agents — but only while the knowledge-base feature itself is on: every knowledge-base service and facade injected
 * here is registered only when {@code bytechef.ai.knowledge-base.enabled=true}, so activating without it fails server
 * startup. Both surfaces tolerate the absence of these beans ({@code ObjectProvider}/{@code List} injection).
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("(${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false})"
    + " and ${bytechef.ai.knowledge-base.enabled:false}")
public class KnowledgeBaseAgentConfiguration {

    @Value("classpath:prompt_knowledge_base_ask.txt")
    private Resource promptKnowledgeBaseAskResource;

    @Value("classpath:prompt_knowledge_base_build.txt")
    private Resource promptKnowledgeBaseBuildResource;

    private final State state = new State();

    @Bean
    KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory(
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade, KnowledgeBaseFacade knowledgeBaseFacade,
        KnowledgeBaseService knowledgeBaseService, KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        ObjectProvider<ToolArtifactRecorder> toolArtifactRecorderProvider) {

        return new KnowledgeBaseToolCallbacksFactory(
            workspaceKnowledgeBaseFacade, knowledgeBaseFacade, knowledgeBaseService, knowledgeBaseDocumentFacade,
            knowledgeBaseDocumentService,
            toolArtifactRecorderProvider.getIfAvailable());
    }

    @Bean
    KnowledgeBaseSpringAIAgent knowledgeBaseAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.KNOWLEDGE_BASE.name() + "_" + Mode.ASK.name();

        return KnowledgeBaseSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptKnowledgeBaseAskResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, knowledgeBaseToolCallbacksFactory.readToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    KnowledgeBaseSpringAIAgent knowledgeBaseBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.KNOWLEDGE_BASE.name() + "_" + Mode.BUILD.name();

        return KnowledgeBaseSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptKnowledgeBaseBuildResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, knowledgeBaseToolCallbacksFactory.writeToolCallbacks()))
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
                "Failed to read knowledge base prompt resource: " + resource.getDescription(), exception);
        }
    }
}
