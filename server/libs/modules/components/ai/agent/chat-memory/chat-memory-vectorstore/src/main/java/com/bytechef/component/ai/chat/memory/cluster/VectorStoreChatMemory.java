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

package com.bytechef.component.ai.chat.memory.cluster;

import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;
import static com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction.VECTOR_STORE;

import com.bytechef.component.ai.chat.memory.util.VectorStoreChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor.Builder;

/**
 * @author Ivica Cardic
 */
public class VectorStoreChatMemory {

    private static final String CHAT_MEMORY_RETRIEVE_SIZE = "chatMemoryRetrieveSize";

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public static ClusterElementDefinition<ChatMemoryFunction> of(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return new VectorStoreChatMemory(clusterElementDefinitionService).build();
    }

    private VectorStoreChatMemory(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    private ClusterElementDefinition<ChatMemoryFunction> build() {
        return ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("Vector Store Chat Memory")
            .description(
                "Memory is retrieved from a VectorStore and added as prior messages in the conversation. This " +
                    "only works for text based exchanges with the models, not multi-modal exchanges.")
            .type(CHAT_MEMORY)
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .options(VectorStoreChatMemoryUtils.getClusterElementFirstMessages())
                    .required(true),
                ComponentDsl.integer(CHAT_MEMORY_RETRIEVE_SIZE)
                    .label("Chat Memory Retrieve Size")
                    .description("The number of messages to retrieve from the vector store.")
                    .defaultValue(20))
            .object(() -> this::apply);
    }

    protected ChatMemoryFunction.Result apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(VECTOR_STORE);

        VectorStoreFunction vectorStoreFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        Map<String, ?> componentConnectionConnectionParameters = componentConnection.getParameters();

        Builder builder = VectorStoreChatMemoryAdvisor
            .builder(
                vectorStoreFunction.apply(
                    ParametersFactory.create(clusterElement.getParameters()),
                    ParametersFactory.create(componentConnectionConnectionParameters),
                    ParametersFactory.create(clusterElement.getExtensions()), componentConnections))
            .defaultTopK(
                inputParameters.getInteger(CHAT_MEMORY_RETRIEVE_SIZE, 20));

        return new ChatMemoryFunction.Result(new ToolCallSafeVectorStoreChatMemoryAdvisor(builder.build()), null);
    }

    /**
     * Guards {@link VectorStoreChatMemoryAdvisor} against the second iteration of a {@code ToolCallAdvisor} loop. In
     * that iteration the prompt has only a {@code SystemMessage} and a {@code ToolResponseMessage} — no real user
     * message — so {@code getUserMessage().getText()} is blank. Passing a blank string to the embedding API causes a
     * 400 error. When there is no real user message we skip the vector-store lookup and pass the request through
     * unchanged; the {@code after()} delegation is preserved so the final assistant reply is still stored.
     */
    private static class ToolCallSafeVectorStoreChatMemoryAdvisor implements BaseChatMemoryAdvisor {

        private final VectorStoreChatMemoryAdvisor delegate;

        ToolCallSafeVectorStoreChatMemoryAdvisor(VectorStoreChatMemoryAdvisor delegate) {
            this.delegate = delegate;
        }

        @Override
        public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
            String userText = Objects.requireNonNullElse(request.prompt()
                .getUserMessage()
                .getText(), "");

            if (userText.isBlank()) {
                return request;
            }

            return delegate.before(request, advisorChain);
        }

        @Override
        public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
            return delegate.after(response, advisorChain);
        }

        @Override
        public int getOrder() {
            return delegate.getOrder();
        }
    }
}
