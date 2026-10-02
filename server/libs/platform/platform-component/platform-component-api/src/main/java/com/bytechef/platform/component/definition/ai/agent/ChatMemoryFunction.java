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

package com.bytechef.platform.component.definition.ai.agent;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.Ordered;

/**
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface ChatMemoryFunction {

    ClusterElementType CHAT_MEMORY = new ClusterElementType("CHAT_MEMORY", "chatMemory", "Memory");

    int TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER = Ordered.HIGHEST_PRECEDENCE + 400;

    Result apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception;

    default Result apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections, Context context) throws Exception {

        return apply(inputParameters, connectionParameters, extensions, componentConnections);
    }

    /**
     * What a memory type contributes to an agent run.
     *
     * @param advisor                        the advisor that reads and writes the conversation
     * @param conversationHistoryReader      reads the active conversation history, e.g. for guardrails, never
     *                                       {@code null}
     * @param toolCallbacks                  extra tools the memory type offers the model, never {@code null}
     * @param supportsToolMessagePersistence {@code true} when the advisor is ordered at
     *                                       {@link ChatMemoryFunction#TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER} and
     *                                       persists the tool messages itself, so the agent disables the tool-calling
     *                                       advisor's own conversation history
     */
    @SuppressFBWarnings({
        "EI", "EI2"
    })
    record Result(
        BaseAdvisor advisor, ConversationHistoryReader conversationHistoryReader, List<ToolCallback> toolCallbacks,
        boolean supportsToolMessagePersistence) {

        public Result {
            Objects.requireNonNull(advisor, "advisor");
            Objects.requireNonNull(conversationHistoryReader, "conversationHistoryReader");
            Objects.requireNonNull(toolCallbacks, "toolCallbacks");

            if (supportsToolMessagePersistence && advisor.getOrder() != TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER) {
                throw new IllegalArgumentException(
                    "An advisor that persists tool messages must have order " +
                        TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER + " but has order " + advisor.getOrder());
            }

            toolCallbacks = List.copyOf(toolCallbacks);
        }

        public static Result of(BaseAdvisor advisor, @Nullable ChatMemory chatMemory) {
            return new Result(advisor, toConversationHistoryReader(chatMemory), List.of(), false);
        }

        public static Result persistingToolMessages(
            BaseAdvisor advisor, ConversationHistoryReader conversationHistoryReader,
            List<ToolCallback> toolCallbacks) {

            return new Result(advisor, conversationHistoryReader, toolCallbacks, true);
        }

        private static ConversationHistoryReader toConversationHistoryReader(@Nullable ChatMemory chatMemory) {
            if (chatMemory == null) {
                return conversationId -> List.of();
            }

            return chatMemory::get;
        }
    }
}
