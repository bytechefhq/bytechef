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

package com.bytechef.automation.ai.tool;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
public record AutomationToolInvocationContext(@Nullable Long workspaceId, @Nullable Long environmentId) {

    public static final String TOOL_CONTEXT_WORKSPACE_ID_KEY = "bytechef.automationTool.workspaceId";
    public static final String TOOL_CONTEXT_ENVIRONMENT_ID_KEY = "bytechef.automationTool.environmentId";

    public static @Nullable AutomationToolInvocationContext fromToolContext(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> map = toolContext.getContext();

        if (map == null || map.isEmpty()) {
            return null;
        }

        Long workspaceId = asLong(map.get(TOOL_CONTEXT_WORKSPACE_ID_KEY));
        Long environmentId = asLong(map.get(TOOL_CONTEXT_ENVIRONMENT_ID_KEY));

        if (workspaceId == null && environmentId == null) {
            return null;
        }

        return new AutomationToolInvocationContext(workspaceId, environmentId);
    }

    public static int resolveEnvironmentOrDefault(@Nullable AutomationToolInvocationContext context) {
        Long environmentId = context == null ? null : context.environmentId();

        if (environmentId == null) {
            return 0;
        }

        return environmentId.intValue();
    }

    private static @Nullable Long asLong(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }

        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }
}
