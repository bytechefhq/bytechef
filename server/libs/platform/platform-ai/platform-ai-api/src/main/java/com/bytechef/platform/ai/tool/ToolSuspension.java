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

package com.bytechef.platform.ai.tool;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.definition.ActionContextAware;
import org.jspecify.annotations.Nullable;

/**
 * The sentinel result a tool returns after it suspended the agent's action context, which tells the agent's tool
 * calling manager to end the turn and wait for the job to resume. {@link #isSuspendedToolResult(String)} also accepts
 * the JSON-quoted form, because Spring AI's {@code FunctionToolCallback} serializes a {@code String} result as JSON.
 *
 * @author Ivica Cardic
 */
public final class ToolSuspension {

    private static final String SUSPENDED_TOOL_RESULT = "__bytechef_tool_suspended__";
    private static final String JSON_SUSPENDED_TOOL_RESULT = "\"" + SUSPENDED_TOOL_RESULT + "\"";

    private ToolSuspension() {
    }

    public static String suspendedToolResult(ActionContext actionContext) {
        if (!(actionContext instanceof ActionContextAware actionContextAware) ||
            actionContextAware.getSuspend() == null) {

            throw new IllegalStateException(
                "A tool must suspend the agent's action context before it returns the suspended tool result.");
        }

        return SUSPENDED_TOOL_RESULT;
    }

    public static boolean isSuspendedToolResult(@Nullable String toolResult) {
        return SUSPENDED_TOOL_RESULT.equals(toolResult) || JSON_SUSPENDED_TOOL_RESULT.equals(toolResult);
    }
}
