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

package com.bytechef.component.ai.agent.utils.cluster.subagent;

import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentExecutor;
import org.springaicommunity.agent.common.task.subagent.TaskCall;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * Runs a builder-defined subagent. Its system prompt, its tools and its model all come from its own SUBAGENT cluster
 * element, so a subagent can call nothing the builder did not attach beneath it — not even a tool attached to a sibling
 * subagent.
 *
 * <p>
 * A subagent that declares no Model of its own falls back to the one attached to the Task Tool, so a builder who wants
 * every subagent on the same model configures it once. Declaring a Model beneath a subagent overrides that for this
 * subagent alone, which is the point: a cheap model can triage while an expensive one reasons.
 *
 * @author Ivica Cardic
 */
public class ByteChefSubagentExecutor implements SubagentExecutor {

    private final @Nullable ChatModel defaultChatModel;
    private final Function<ClusterElement, @Nullable ChatModel> chatModelResolver;
    private final Function<ClusterElement, List<ToolCallback>> toolCallbackResolver;

    public ByteChefSubagentExecutor(
        @Nullable ChatModel defaultChatModel, Function<ClusterElement, List<ToolCallback>> toolCallbackResolver) {

        this(defaultChatModel, clusterElement -> null, toolCallbackResolver);
    }

    public ByteChefSubagentExecutor(
        @Nullable ChatModel defaultChatModel, Function<ClusterElement, @Nullable ChatModel> chatModelResolver,
        Function<ClusterElement, List<ToolCallback>> toolCallbackResolver) {

        this.defaultChatModel = defaultChatModel;
        this.chatModelResolver = chatModelResolver;
        this.toolCallbackResolver = toolCallbackResolver;
    }

    @Override
    public String execute(TaskCall taskCall, SubagentDefinition subagentDefinition) {
        if (!(subagentDefinition instanceof ByteChefSubagentDefinition byteChefSubagentDefinition)) {
            throw new IllegalArgumentException(
                "Unsupported subagent definition: " + subagentDefinition.getClass());
        }

        ChatModel chatModel = resolveChatModel(byteChefSubagentDefinition);

        if (chatModel == null) {
            throw new IllegalStateException(
                "Subagent '%s' has no model. Attach a Model to the subagent, or to the Task Tool to cover every "
                    .formatted(byteChefSubagentDefinition.getName()) + "subagent that declares none.");
        }

        List<ToolCallback> toolCallbacks = resolveToolCallbacks(byteChefSubagentDefinition);

        ChatClient chatClient = ChatClient.builder(chatModel)
            .defaultToolCallbacks(toolCallbacks)
            .build();

        return chatClient.prompt()
            .system(byteChefSubagentDefinition.getInstructions())
            .user(taskCall.prompt())
            .call()
            .content();
    }

    @Override
    public String getKind() {
        return ByteChefSubagentDefinition.KIND;
    }

    /**
     * Package-private so a test can pin the per-subagent override directly: the subagent's own Model wins, and the Task
     * Tool's is only consulted when the subagent declares none.
     */
    @Nullable
    ChatModel resolveChatModel(ByteChefSubagentDefinition subagentDefinition) {
        ChatModel chatModel = chatModelResolver.apply(subagentDefinition.getClusterElement());

        return chatModel == null ? defaultChatModel : chatModel;
    }

    /**
     * Package-private so a test can pin the scoping guarantee directly: tools are resolved from this subagent's own
     * cluster element and from nothing else.
     */
    List<ToolCallback> resolveToolCallbacks(ByteChefSubagentDefinition subagentDefinition) {
        return toolCallbackResolver.apply(subagentDefinition.getClusterElement());
    }
}
