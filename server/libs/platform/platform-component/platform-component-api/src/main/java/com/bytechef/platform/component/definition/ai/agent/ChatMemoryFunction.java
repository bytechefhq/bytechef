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
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
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

    /**
     *
     */
    ClusterElementType CHAT_MEMORY = new ClusterElementType("CHAT_MEMORY", "chatMemory", "Memory");

    /**
     * Advisor order that places a chat-memory advisor <b>inside</b> the {@code ToolCallingAdvisor} loop, so it records
     * the full per-iteration tool request/response transcript rather than only the final user/assistant exchange. It
     * must stay strictly greater than {@code ToolCallingAdvisor.DEFAULT_ORDER}
     * ({@code Ordered.HIGHEST_PRECEDENCE + 300}) — Spring AI runs advisors ordered after the tool advisor on every loop
     * iteration. Only memory types that can safely persist tool messages (see {@code Result}'s
     * {@code supportsToolMessagePersistence}) should build their advisor with this order; the default
     * {@code Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER} ({@code HIGHEST_PRECEDENCE + 200}) keeps the advisor
     * upstream (outside) the loop.
     */
    int TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER = Ordered.HIGHEST_PRECEDENCE + 400;

    /**
     * @param inputParameters
     * @param connectionParameters
     * @param extensions
     * @param componentConnections
     * @return
     * @throws Exception
     */
    Result apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception;

    /**
     * Holds the advisor and the backing {@link ChatMemory} together so callers can read conversation history without
     * building a second memory instance. {@code chatMemory} is {@code null} for advisor types (e.g. VectorStore) that
     * do not expose a standard {@link ChatMemory}.
     *
     * <p>
     * {@code supportsToolMessagePersistence} declares whether this memory type can safely persist the full tool
     * request/response transcript when its advisor runs inside the {@code ToolCallingAdvisor} loop. When {@code true},
     * the component MUST build its advisor with {@link #TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER} so it sits inside the
     * loop, and the agent disables the tool advisor's own in-loop history to avoid double-writing the transcript. It
     * defaults to {@code false} — the safe, outside-the-loop behavior — for every memory type that does not opt in.
     * </p>
     */
    @SuppressFBWarnings({
        "EI", "EI2"
    })
    record Result(
        BaseAdvisor advisor, @Nullable ChatMemory chatMemory, @Nullable ToolCallback[] toolCallbacks,
        boolean supportsToolMessagePersistence) {

        public Result(BaseAdvisor advisor, @Nullable ChatMemory chatMemory) {
            this(advisor, chatMemory, null, false);
        }

        public Result(BaseAdvisor advisor, @Nullable ChatMemory chatMemory, boolean supportsToolMessagePersistence) {
            this(advisor, chatMemory, null, supportsToolMessagePersistence);
        }

        public Result(BaseAdvisor advisor, @Nullable ChatMemory chatMemory, @Nullable ToolCallback[] toolCallbacks) {
            this(advisor, chatMemory, toolCallbacks, false);
        }
    }
}
