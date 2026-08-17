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
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Reads {@code com.bytechef.automation.ai.agent.domain.AiAgent#getSettings()}' {@code builtInTools} block — the
 * per-agent on/off switches (plus one connection id) controlling which {@code aiAgentUtils} built-in tools
 * {@link AiAgentWorkflowGenerator} emits into the flat tools array. Shared between the generator and
 * {@code AiAgentFacadeImpl} (publish validation) so the two can never read a different default for the same key.
 *
 * <p>
 * Shape: {@code {builtInTools: {askUserQuestion: bool, autoMemory: bool, skillManagement: bool,
 * webSearch: bool, webSearchConnectionId?: number}}}. Absence of the {@code builtInTools} key, or of any individual key
 * inside it, means that key's documented default below applies — there is no separate "explicitly unset" state.
 *
 * @author Ivica Cardic
 */
public final class AiAgentSettings {

    public static final String BUILT_IN_TOOLS = "builtInTools";

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
     * {@code aiAgentUtils/v1/braveWebSearchTool} — default OFF (needs {@link #WEB_SEARCH_CONNECTION_ID} to publish).
     */
    public static final String WEB_SEARCH = "webSearch";

    /**
     * Design-time connection id for the {@code webSearch} built-in, feeding
     * {@link AiAgentWorkflowGenerator#buildConnectionRefs} the same way a {@code TOOL}/{@code APPROVAL_CHANNEL} row's
     * own {@code connectionId} does — the real connection still binds at deployment time via the generated node's
     * {@code connections} block; this id only lets the draft's test-chat panel resolve one. {@code null}/absent is
     * allowed at draft time but rejected at publish time when {@link #WEB_SEARCH} is on (see
     * {@code AiAgentErrorType#BUILT_IN_TOOL_CONNECTION_MISSING}).
     */
    public static final String WEB_SEARCH_CONNECTION_ID = "webSearchConnectionId";

    private AiAgentSettings() {
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
