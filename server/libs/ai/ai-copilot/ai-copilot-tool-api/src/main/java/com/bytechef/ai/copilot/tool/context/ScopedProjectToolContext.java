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

package com.bytechef.ai.copilot.tool.context;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * The single project an agent run is confined to.
 *
 * @author Ivica Cardic
 */
public final class ScopedProjectToolContext {

    public static final String TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY = "bytechef.agentTool.scopedProjectId";

    private ScopedProjectToolContext() {
    }

    public static @Nullable Long fetchScopedProjectId(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();

        if (context == null) {
            return null;
        }

        return context.get(TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY) instanceof Number number ? number.longValue() : null;
    }

    public static boolean isConfined(@Nullable ToolContext toolContext) {
        if (fetchScopedProjectId(toolContext) != null) {
            return true;
        }

        AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

        return invocationContext != null && invocationContext.skipAutomationAuthorization();
    }
}
