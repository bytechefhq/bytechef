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
 * @author Ivica Cardic
 */
public final class AiAgentSettings {

    public static final String BUILT_IN_TOOLS = "builtInTools";

    public static final String STREAM_RESPONSE = "streamResponse";

    public static final String THINKING = "thinking";

    public static final String REASONING_EFFORT = "reasoningEffort";

    public static final String MAX_TOOL_CALLS = "maxToolCalls";

    public static final Set<String> THINKING_MODEL_PROVIDERS = Set.of(
        "anthropic", "azureOpenAi", "gemini", "groq", "mistral", "nvidia", "ollama", "openAi", "perplexity");

    public static final String ASK_USER_QUESTION = "askUserQuestion";

    public static final String AUTO_MEMORY = "autoMemory";

    public static final String SKILL_MANAGEMENT = "skillManagement";

    public static final String WEB_SEARCH = "webSearch";

    public static final String WEB_SEARCH_PROVIDER = "webSearchProvider";

    public static final String WEB_SEARCH_CONNECTION_ID = "webSearchConnectionId";

    public static final Set<String> NATIVE_WEB_SEARCH_MODEL_PROVIDERS = Set.of("anthropic");

    public enum ReasoningEffort {

        LOW, MEDIUM, HIGH;

        public String toParameterValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum WebSearchProvider {

        BRAVE, FIRECRAWL, NATIVE;

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

    public static boolean isConnectionBackedWebSearchEnabled(Map<String, ?> settings) {
        WebSearchProvider webSearchProvider = getWebSearchProvider(settings);

        return isWebSearchEnabled(settings) && webSearchProvider.isConnectionBacked();
    }

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
