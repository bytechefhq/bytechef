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

package com.bytechef.component.ai.llm.tool;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.Context;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @author Ivica Cardic
 */
public class ToolCallLoggingToolCallback implements ToolCallback {

    private final Context context;
    private final ToolCallback delegate;

    @SuppressFBWarnings("EI2")
    public ToolCallLoggingToolCallback(ToolCallback delegate, Context context) {
        this.context = context;
        this.delegate = delegate;
    }

    public static List<ToolCallback> wrap(List<ToolCallback> toolCallbacks, Context context) {
        return toolCallbacks.stream()
            .map(toolCallback -> (ToolCallback) new ToolCallLoggingToolCallback(toolCallback, context))
            .toList();
    }

    public static void logCall(
        Context context, String toolName, @Nullable Object inputs, @Nullable String output) {

        Map<String, @Nullable Object> toolExecutionLogEntry = new LinkedHashMap<>();

        toolExecutionLogEntry.put("inputs", inputs);
        toolExecutionLogEntry.put("output", output);
        toolExecutionLogEntry.put("toolName", toolName);

        context.log(log -> log.info(JsonUtils.write(toolExecutionLogEntry)));
    }

    public static void logFailure(
        Context context, String toolName, @Nullable Object inputs, RuntimeException exception) {

        Map<String, @Nullable Object> toolFailureLogEntry = new LinkedHashMap<>();

        toolFailureLogEntry.put("error", exception.getMessage());
        toolFailureLogEntry.put("inputs", inputs);
        toolFailureLogEntry.put("toolName", toolName);

        context.log(log -> log.error(JsonUtils.write(toolFailureLogEntry), exception));
    }

    @Override
    public String call(String toolInput) {
        return logAndCall(toolInput, () -> delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        return logAndCall(toolInput, () -> delegate.call(toolInput, toolContext));
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    private static Object parseInputs(String toolInput) {
        try {
            return JsonUtils.read(toolInput);
        } catch (RuntimeException exception) {
            return Map.of("rawInput", toolInput);
        }
    }

    private String logAndCall(String toolInput, Supplier<String> call) {
        ToolDefinition toolDefinition = delegate.getToolDefinition();

        String result;

        try {
            result = call.get();
        } catch (RuntimeException exception) {
            logFailure(context, toolDefinition.name(), parseInputs(toolInput), exception);

            throw exception;
        }

        logCall(context, toolDefinition.name(), parseInputs(toolInput), result);

        return result;
    }
}
