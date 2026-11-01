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

package com.bytechef.ai.mcp.server.tool;

import com.bytechef.automation.ai.tool.WorkspaceScopeResolver;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @author Ivica Cardic
 */
public class ToolInvocationRecordingToolCallback implements ToolCallback {

    private static final String ENVIRONMENT_FIELD = "environment";
    private static final String ERROR_FIELD = "error";
    private static final String WORKSPACE_ID_FIELD = "workspaceId";

    private final ToolCallback delegate;
    private final ToolExecutionKind kind;
    private final ToolExecutionRecorder toolExecutionRecorder;
    private final WorkspaceScopeResolver workspaceScopeResolver;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ToolInvocationRecordingToolCallback(
        ToolCallback delegate, ToolExecutionKind kind, ToolExecutionRecorder toolExecutionRecorder,
        WorkspaceScopeResolver workspaceScopeResolver) {

        this.delegate = delegate;
        this.kind = kind;
        this.toolExecutionRecorder = toolExecutionRecorder;
        this.workspaceScopeResolver = workspaceScopeResolver;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        ToolDefinition toolDefinition = delegate.getToolDefinition();

        ToolExecutionEvent.Builder builder = ToolExecutionEvent.builder(
            PlatformType.AUTOMATION, ToolExecutionSurface.MCP_MANAGEMENT, kind, toolDefinition.name());

        if (resolveWorkspaceScope(toolInput) instanceof WorkspaceScopeResolver.Resolved resolved) {
            builder
                .workspaceId(resolved.workspaceId())
                .environment(Math.toIntExact(resolved.environmentId()));
        }

        return toolExecutionRecorder.record(
            builder, () -> delegate.call(toolInput, toolContext),
            ToolInvocationRecordingToolCallback::resolveOutcome);
    }

    private WorkspaceScopeResolver.@Nullable WorkspaceScope resolveWorkspaceScope(@Nullable String toolInput) {
        try {
            Map<String, ?> input = readObject(toolInput);

            return workspaceScopeResolver.resolve(
                toLong(input.get(WORKSPACE_ID_FIELD)), toEnvironment(input.get(ENVIRONMENT_FIELD)));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static ToolExecutionOutcome resolveOutcome(@Nullable String result) {
        if (result == null) {
            return ToolExecutionOutcome.SUCCESS;
        }

        String trimmedResult = result.strip();

        if (!trimmedResult.startsWith("{") || !trimmedResult.contains("\"" + ERROR_FIELD + "\"")) {
            return ToolExecutionOutcome.SUCCESS;
        }

        try {
            Map<String, ?> resultMap = JsonUtils.readMap(trimmedResult);

            return resultMap.containsKey(ERROR_FIELD) ? ToolExecutionOutcome.ERROR : ToolExecutionOutcome.SUCCESS;
        } catch (RuntimeException exception) {
            return ToolExecutionOutcome.SUCCESS;
        }
    }

    private static Map<String, ?> readObject(@Nullable String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return Map.of();
        }

        return JsonUtils.readMap(toolInput);
    }

    private static @Nullable String toEnvironment(@Nullable Object value) {
        if (value instanceof String environment && !environment.isBlank()) {
            return environment;
        }

        return null;
    }

    private static @Nullable Long toLong(@Nullable Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string.strip());
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }
}
