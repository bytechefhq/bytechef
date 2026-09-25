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

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.commons.util.NumberUtils;
import com.bytechef.commons.util.StringUtils;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record SubAgentModelRequest(
    @Nullable Long environmentId, @Nullable String llmProvider, @Nullable String llmModel) {

    public static SubAgentModelRequest fromToolContext(Map<String, Object> toolContext) {
        return new SubAgentModelRequest(
            NumberUtils.asLong(toolContext.get(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY)),
            StringUtils.asString(toolContext.get(AgentToolInvocationContext.TOOL_CONTEXT_LLM_PROVIDER_KEY)),
            StringUtils.asString(toolContext.get(AgentToolInvocationContext.TOOL_CONTEXT_LLM_MODEL_KEY)));
    }
}
