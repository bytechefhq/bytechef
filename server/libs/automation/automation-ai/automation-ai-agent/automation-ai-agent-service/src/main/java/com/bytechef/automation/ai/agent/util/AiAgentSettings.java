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

package com.bytechef.automation.ai.agent.util;

import com.bytechef.commons.util.MapUtils;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Reads {@code com.bytechef.automation.ai.agent.domain.AiAgent#getSettings()} — the {@code builtInTools} block (the
 * per-agent on/off switches, plus one connection id, controlling which {@code aiAgentUtils} built-in tools
 * {@link AiAgentWorkflowGenerator} emits into the flat tools array) and the top-level keys beside it. Shared between
 * the generator and {@code AiAgentFacadeImpl} (publish validation) so the two can never read a different default for
 * the same key.
 *
 * <p>
 * Shape: {@code {streamResponse?: bool, thinking?: bool, reasoningEffort?: "LOW"|"MEDIUM"|"HIGH",
 * maxToolCalls?: int, builtInTools: {askUserQuestion: bool, autoMemory: bool,
 * skillManagement: bool, webSearch: bool, webSearchProvider?: "BRAVE"|"NATIVE", webSearchConnectionId?: number}}}.
 * Absence of the {@code builtInTools} key, of any individual key inside it, or of a top-level key means that key's
 * documented default below applies — there is no separate "explicitly unset" state.
 *
 * @author Ivica Cardic
 */
public final class AiAgentSettings {

    public static final String BUILT_IN_TOOLS = "builtInTools";

    /**
     * Which {@code aiAgent} action the generated {@code aiAgent_1} node runs: {@code aiAgent/v1/streamChat} when on
     * (the default, and what every agent predating this key reads — the generator hardcoded it before), else
     * {@code aiAgent/v1/chat}. Top level rather than inside {@link #BUILT_IN_TOOLS} because it is not a tool: it picks
     * the agent's own action, the way the workflow editor's AI Agent panel does with its "Stream response" switch.
     *
     * <p>
     * Switching it does not change the node's output contract — {@code chat} carries a {@code response} property
     * {@code streamChat} lacks, but the generator emits no {@code response} parameter, so {@code ModelUtils.output}
     * still resolves to an unnamed {@code string()} and {@code __AGENT_OUTPUT__} stays the bare {@code ${aiAgent_1}}.
     */
    public static final String STREAM_RESPONSE = "streamResponse";

    /**
     * Extended reasoning, written onto the {@code MODEL} cluster element's parameters exactly like
     * {@link WebSearchProvider#NATIVE} web search is — it is a property of the model call, not a tool, so it sits
     * beside {@link #BUILT_IN_TOOLS} rather than inside it. Default OFF, which is also what every agent predating this
     * key reads.
     */
    public static final String THINKING = "thinking";

    /**
     * How hard the model thinks when {@link #THINKING} is on — {@code LOW}, {@code MEDIUM} (the default) or
     * {@code HIGH}. Unparseable values fall back to {@code MEDIUM} for the same reason {@link #WEB_SEARCH_PROVIDER}
     * falls back to {@code BRAVE}: the settings map is free-form JSON and a typo must not make an otherwise valid agent
     * ungeneratable.
     */
    public static final String REASONING_EFFORT = "reasoningEffort";

    /**
     * Caps the tool calls one agent run may make in total, written onto {@code aiAgent_1}'s own parameters. Absent
     * means the platform default applies ({@code DefaultToolCallingManager.DEFAULT_MAX_TOTAL_TOOL_CALLS}, currently
     * 150) — there is no "unlimited" value, because an agent that can loop forever is a billing incident, not a
     * feature.
     */
    public static final String MAX_TOOL_CALLS = "maxToolCalls";

    /**
     * Model providers whose {@code model} cluster element declares {@code LLMConstants.THINKING_PROPERTY}. Every other
     * provider would silently ignore the setting, so selecting it against one of those is rejected at publish time —
     * the same contract, and the same reason, as {@link #NATIVE_WEB_SEARCH_MODEL_PROVIDERS}. Add a provider here in the
     * same commit that wires its model element's {@code thinking} property.
     */
    public static final Set<String> THINKING_MODEL_PROVIDERS = Set.of("anthropic", "openAi");

    /** {@code aiAgentUtils/v1/askUserQuestionTool} — default ON. */
    public static final String ASK_USER_QUESTION = "askUserQuestion";

    /** {@code aiAgentUtils/v1/autoMemoryTool} — default ON. Replaces the retired {@code AUTO_MEMORY} element kind. */
    public static final String AUTO_MEMORY = "autoMemory";

    /**
     * The five {@code AiSkill} management actions as TOOL entries ({@code aiAgentUtils/v1/createAiSkill},
     * {@code updateAiSkill}, {@code deleteAiSkill}, {@code appendFilesToAiSkill}, {@code removeFileFromAiSkill}) —
     * default ON.
     */
    public static final String SKILL_MANAGEMENT = "skillManagement";

    /**
     * Web search — default OFF. What being on emits depends on {@link #WEB_SEARCH_PROVIDER}: {@code BRAVE} adds the
     * {@code brave/v1/webSearch} tool element and {@code FIRECRAWL} the {@code firecrawl/v1/search} one (both need
     * {@link #WEB_SEARCH_CONNECTION_ID} to publish); {@code NATIVE} adds no tool at all and switches the model's own
     * provider-side search on instead.
     */
    public static final String WEB_SEARCH = "webSearch";

    /**
     * Which web search the {@link #WEB_SEARCH} built-in uses — {@code BRAVE} (the default, and the value every agent
     * predating this key reads), {@code FIRECRAWL} or {@code NATIVE}. Unparseable values fall back to {@code BRAVE}
     * rather than failing: the settings map is free-form JSON, and a typo must not make an otherwise valid agent
     * ungeneratable.
     */
    public static final String WEB_SEARCH_PROVIDER = "webSearchProvider";

    /**
     * Design-time connection id for the {@code webSearch} built-in, feeding
     * {@link AiAgentWorkflowGenerator#buildConnectionRefs} the same way a {@code TOOL}/{@code APPROVAL_CHANNEL} row's
     * own {@code connectionId} does — the real connection still binds at deployment time via the generated node's
     * {@code connections} block; this id only lets the draft's test-chat panel resolve one. {@code null}/absent is
     * allowed at draft time but rejected at publish time when {@link #WEB_SEARCH} is on (see
     * {@code AiAgentErrorType#BUILT_IN_TOOL_CONNECTION_MISSING}).
     */
    public static final String WEB_SEARCH_CONNECTION_ID = "webSearchConnectionId";

    /**
     * Model providers whose {@code model} cluster element implements {@link WebSearchProvider#NATIVE} — i.e. those
     * whose Spring AI chat options expose a provider-side web search server tool. Only anthropic does today
     * ({@code AnthropicChatOptions.webSearchTool}); Spring AI 2.0.1 ships no equivalent for openai and the rest, so
     * selecting {@code NATIVE} against one of those is rejected at publish time rather than silently searching nothing.
     * Add a provider here in the same commit that wires its model element's {@code webSearch} property.
     */
    public static final Set<String> NATIVE_WEB_SEARCH_MODEL_PROVIDERS = Set.of("anthropic");

    /**
     * How hard the model thinks when {@link #THINKING} is on. Deliberately an effort word rather than a token budget:
     * only Anthropic expresses the setting as a budget and every other provider takes an effort string, so each
     * provider's model element maps these three values onto its own knob.
     */
    public enum ReasoningEffort {

        LOW, MEDIUM, HIGH;

        public String toParameterValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Where the {@link #WEB_SEARCH} built-in gets its results.
     *
     * <ul>
     * <li>{@link #BRAVE} — the {@code brave} component's own {@code webSearch} tool cluster element, carrying a Brave
     * connection.</li>
     * <li>{@link #FIRECRAWL} — the {@code firecrawl} component's own {@code search} tool cluster element, carrying a
     * Firecrawl connection. Same shape as {@code BRAVE}, a different component.</li>
     * <li>{@link #NATIVE} — the model provider's built-in search, switched on through the {@code model} cluster
     * element. No tool element and no connection of any kind.</li>
     * </ul>
     */
    public enum WebSearchProvider {

        BRAVE, FIRECRAWL, NATIVE;

        /**
         * Whether this provider is a tool element of its own, and therefore needs {@link #WEB_SEARCH_CONNECTION_ID}.
         * True for everything except {@link #NATIVE}, which runs inside the model call.
         */
        public boolean isConnectionBacked() {
            return this != NATIVE;
        }
    }

    private AiAgentSettings() {
    }

    public static boolean isStreamResponseEnabled(Map<String, ?> settings) {
        return MapUtils.getBoolean(settings, STREAM_RESPONSE, true);
    }

    public static boolean isThinkingEnabled(Map<String, ?> settings) {
        return MapUtils.getBoolean(settings, THINKING, false);
    }

    public static ReasoningEffort getReasoningEffort(Map<String, ?> settings) {
        String value = MapUtils.getString(settings, REASONING_EFFORT);

        if (value == null) {
            return ReasoningEffort.MEDIUM;
        }

        try {
            return ReasoningEffort.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ReasoningEffort.MEDIUM;
        }
    }

    public static @Nullable Integer getMaxToolCalls(Map<String, ?> settings) {
        return MapUtils.getInteger(settings, MAX_TOOL_CALLS);
    }

    public static boolean isAskUserQuestionEnabled(Map<String, ?> settings) {
        return isEnabled(settings, ASK_USER_QUESTION, true);
    }

    public static boolean isAutoMemoryEnabled(Map<String, ?> settings) {
        return isEnabled(settings, AUTO_MEMORY, true);
    }

    public static boolean isSkillManagementEnabled(Map<String, ?> settings) {
        return isEnabled(settings, SKILL_MANAGEMENT, true);
    }

    public static boolean isWebSearchEnabled(Map<String, ?> settings) {
        return isEnabled(settings, WEB_SEARCH, false);
    }

    public static WebSearchProvider getWebSearchProvider(Map<String, ?> settings) {
        String value = MapUtils.getString(builtInTools(settings), WEB_SEARCH_PROVIDER);

        if (value == null) {
            return WebSearchProvider.BRAVE;
        }

        try {
            return WebSearchProvider.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return WebSearchProvider.BRAVE;
        }
    }

    /**
     * Whether web search is on AND resolves to a tool element of its own ({@code brave}/{@code firecrawl}) — the
     * combination that needs a connection, both when generating connection refs and when validating a publish.
     */
    public static boolean isConnectionBackedWebSearchEnabled(Map<String, ?> settings) {
        WebSearchProvider webSearchProvider = getWebSearchProvider(settings);

        return isWebSearchEnabled(settings) && webSearchProvider.isConnectionBacked();
    }

    /**
     * Whether web search is on AND resolves to the model provider's own search — no tool element, no connection.
     */
    public static boolean isNativeWebSearchEnabled(Map<String, ?> settings) {
        return isWebSearchEnabled(settings) && getWebSearchProvider(settings) == WebSearchProvider.NATIVE;
    }

    public static @Nullable Long getWebSearchConnectionId(Map<String, ?> settings) {
        return MapUtils.getLong(builtInTools(settings), WEB_SEARCH_CONNECTION_ID);
    }

    private static boolean isEnabled(Map<String, ?> settings, String key, boolean defaultValue) {
        return MapUtils.getBoolean(builtInTools(settings), key, defaultValue);
    }

    private static Map<String, ?> builtInTools(Map<String, ?> settings) {
        return MapUtils.getMap(settings, BUILT_IN_TOOLS, Map.of());
    }
}
