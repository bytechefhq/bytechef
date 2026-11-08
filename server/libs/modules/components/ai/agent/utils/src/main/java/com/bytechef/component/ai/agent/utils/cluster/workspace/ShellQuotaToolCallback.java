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

package com.bytechef.component.ai.agent.utils.cluster.workspace;

import com.bytechef.commons.util.JsonUtils;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @author Ivica Cardic
 */
public class ShellQuotaToolCallback implements ToolCallback {

    static final String BASH_TOOL_NAME = "Bash";

    private static final Pattern CLEANUP_COMMAND_PATTERN = Pattern.compile(
        "^\\s*(rm|rmdir|truncate)\\s.*|^\\s*find\\s.*\\s-delete(\\s.*)?$", Pattern.DOTALL);
    private static final String COMMAND = "command";

    private final ToolCallback delegate;
    private final AgentWorkspaceQuota quota;

    public ShellQuotaToolCallback(ToolCallback delegate, AgentWorkspaceQuota quota) {
        this.delegate = delegate;
        this.quota = quota;
    }

    public static ToolCallback wrap(ToolCallback toolCallback, AgentWorkspaceQuota quota) {
        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        if (!quota.isLimited() || !BASH_TOOL_NAME.equals(toolDefinition.name())) {
            return toolCallback;
        }

        return new ShellQuotaToolCallback(toolCallback, quota);
    }

    @Override
    public String call(String toolInput) {
        return callWithinQuota(toolInput, () -> delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        return callWithinQuota(toolInput, () -> delegate.call(toolInput, toolContext));
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    private String callWithinQuota(String toolInput, Supplier<String> command) {
        long usedBytes = quota.getUsedBytes();

        if (usedBytes > quota.maxBytes() && !isCleanupCommand(toolInput)) {
            return quota.getRefusedMessage(usedBytes);
        }

        String result = command.get();

        long usedBytesAfterCommand = quota.getUsedBytes();

        if (usedBytesAfterCommand <= quota.maxBytes()) {
            return result;
        }

        return (result == null ? "" : result + "\n\n") + quota.getOverLimitMessage(usedBytesAfterCommand);
    }

    private static boolean isCleanupCommand(String toolInput) {
        Map<String, ?> arguments = JsonUtils.readMap(toolInput);

        if (!(arguments.get(COMMAND) instanceof String command)) {
            return false;
        }

        return CLEANUP_COMMAND_PATTERN.matcher(command)
            .matches();
    }
}
