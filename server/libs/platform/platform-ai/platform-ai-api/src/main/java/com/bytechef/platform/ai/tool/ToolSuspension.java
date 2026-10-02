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
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The result a tool returns after it suspended the agent's action context, which tells the agent's tool calling manager
 * to end the turn and wait for the job to resume. Each suspension gets its own random result, stored in the suspend's
 * continue parameters under {@link #SUSPENDED_TOOL_RESULT}, so a tool output that merely looks like it cannot pass for
 * a suspension. {@link #isSuspendedToolResult(String, Suspend)} also accepts the JSON-quoted form, because Spring AI's
 * {@code FunctionToolCallback} serializes a {@code String} result as JSON.
 *
 * @author Ivica Cardic
 */
public final class ToolSuspension {

    public static final String SUSPENDED_TOOL_RESULT = "__bytechef_suspended_tool_result__";

    private static final String SUSPENDED_TOOL_RESULT_PREFIX = "__bytechef_tool_suspended__:";

    private ToolSuspension() {
    }

    /**
     * Records a new suspended tool result in the suspend of the given action context and returns it.
     */
    public static String suspendedToolResult(ActionContext actionContext) {
        Suspend suspend = actionContext instanceof ActionContextAware actionContextAware
            ? actionContextAware.getSuspend() : null;

        if (suspend == null) {
            throw new IllegalStateException(
                "A tool must suspend the agent's action context before it returns the suspended tool result.");
        }

        String suspendedToolResult = SUSPENDED_TOOL_RESULT_PREFIX + UUID.randomUUID();

        Map<String, Object> continueParameters = new HashMap<>(suspend.continueParameters());

        continueParameters.put(SUSPENDED_TOOL_RESULT, suspendedToolResult);

        actionContext.suspend(new Suspend(continueParameters, suspend.expiresAt()));

        return suspendedToolResult;
    }

    public static @Nullable String getSuspendedToolResult(@Nullable Suspend suspend) {
        if (suspend == null) {
            return null;
        }

        Map<String, ?> continueParameters = suspend.continueParameters();

        return continueParameters.get(SUSPENDED_TOOL_RESULT) instanceof String suspendedToolResult
            ? suspendedToolResult : null;
    }

    public static boolean isSuspendedToolResult(@Nullable String toolResult, @Nullable Suspend suspend) {
        return isSuspendedToolResult(toolResult, getSuspendedToolResult(suspend));
    }

    public static boolean isSuspendedToolResult(@Nullable String toolResult, @Nullable String suspendedToolResult) {
        if (toolResult == null || suspendedToolResult == null) {
            return false;
        }

        return suspendedToolResult.equals(toolResult) || ("\"" + suspendedToolResult + "\"").equals(toolResult);
    }
}
