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

package com.bytechef.ai.copilot.tool.catalog;

import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * Describes one intelligent delegate tool — an LLM-backed subagent exposed as a {@link ToolCallback} — for registration
 * with the {@link IntelligentToolCatalog}.
 *
 * @author Ivica Cardic
 */
public interface IntelligentToolDefinition {

    /**
     * The tool name, e.g. {@code "buildWorkflow"}.
     */
    String name();

    /**
     * The key this delegate is registered under in {@code AgentTypeRegistry}, used to key its per-conversation session
     * memory.
     *
     * <p>
     * MUST be a key registered with {@code com.bytechef.ai.agent.tool.AgentTypeRegistry} — an unregistered key would
     * leave a per-conversation session memory row that a task delete's registry-driven purge could never reconstruct
     * and clean up. This is a raw {@link String} rather than a typed {@code AgentType}/{@code CopilotAgentType}
     * deliberately: {@code ai-api} is an {@code implementation}-only dependency of {@code ai-copilot-tool}, so this
     * interface cannot expose it on its public surface. {@code IntelligentToolSurfaceParityTest} (ai-hub-service)
     * enforces the registration invariant for every contributed definition.
     * </p>
     */
    String agentTypeKey();

    /**
     * Which Copilot panels get this delegate. The AI Hub and the management MCP surface always get every definition
     * regardless of this scope.
     */
    Set<IntelligentToolScope> panelScopes();

    /**
     * The raw {@link IntelligentToolChatClientFactory} for the given variant, or {@code null} when this delegate is not
     * offered in that variant (e.g. the converter delegate has no ASK variant). The catalog skips this definition for a
     * variant with a {@code null} factory.
     *
     * <p>
     * Unlike a plain {@link java.util.function.Supplier}, the returned factory is invoked per delegation, not once per
     * registration: every delegate call resolves its {@code ChatClient} afresh, so a definition MUST honour a non-null
     * {@code chatModel} argument by rebuilding its client over that model with the same system prompt and tools it
     * would otherwise use.
     * </p>
     */
    @Nullable
    IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant);

    /**
     * Builds the {@link ToolCallback} over the given (already surface-decorated)
     * {@link IntelligentToolChatClientFactory}.
     */
    ToolCallback create(IntelligentToolChatClientFactory chatClientFactory);
}
