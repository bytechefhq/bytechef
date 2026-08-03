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
 * @author Ivica Cardic
 */
public class ClusterElementSubagentExecutor implements SubagentExecutor {

    private final @Nullable ChatModel defaultChatModel;
    private final Function<ClusterElement, @Nullable ChatModel> chatModelResolver;
    private final Function<ClusterElement, List<ToolCallback>> toolCallbackResolver;

    public ClusterElementSubagentExecutor(
        @Nullable ChatModel defaultChatModel, Function<ClusterElement, List<ToolCallback>> toolCallbackResolver) {

        this(defaultChatModel, clusterElement -> null, toolCallbackResolver);
    }

    public ClusterElementSubagentExecutor(
        @Nullable ChatModel defaultChatModel, Function<ClusterElement, @Nullable ChatModel> chatModelResolver,
        Function<ClusterElement, List<ToolCallback>> toolCallbackResolver) {

        this.defaultChatModel = defaultChatModel;
        this.chatModelResolver = chatModelResolver;
        this.toolCallbackResolver = toolCallbackResolver;
    }

    @Override
    public String execute(TaskCall taskCall, SubagentDefinition subagentDefinition) {
        if (!(subagentDefinition instanceof ClusterElementSubagentDefinition clusterElementSubagentDefinition)) {
            throw new IllegalArgumentException(
                "Unsupported subagent definition: " + subagentDefinition.getClass());
        }

        ChatModel chatModel = resolveChatModel(clusterElementSubagentDefinition);

        if (chatModel == null) {
            throw new IllegalStateException(
                "Subagent '%s' has no model. Attach a Model to the subagent, or to the Task Tool to cover every "
                    .formatted(clusterElementSubagentDefinition.getName()) + "subagent that declares none.");
        }

        List<ToolCallback> toolCallbacks = resolveToolCallbacks(clusterElementSubagentDefinition);

        ChatClient chatClient = ChatClient.builder(chatModel)
            .defaultToolCallbacks(toolCallbacks)
            .build();

        return chatClient.prompt()
            .system(clusterElementSubagentDefinition.getInstructions())
            .user(taskCall.prompt())
            .call()
            .content();
    }

    @Override
    public String getKind() {
        return ClusterElementSubagentDefinition.KIND;
    }

    @Nullable
    ChatModel resolveChatModel(ClusterElementSubagentDefinition subagentDefinition) {
        ChatModel chatModel = chatModelResolver.apply(subagentDefinition.getClusterElement());

        return chatModel == null ? defaultChatModel : chatModel;
    }

    List<ToolCallback> resolveToolCallbacks(ClusterElementSubagentDefinition subagentDefinition) {
        return toolCallbackResolver.apply(subagentDefinition.getClusterElement());
    }
}
