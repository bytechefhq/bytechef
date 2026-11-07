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

package com.bytechef.platform.ai.guardrails;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
public class RedactingToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ObjectProvider<McpOutboundRedactorProvider> mcpOutboundRedactorProviderProvider;
    private final @Nullable Long workspaceId;
    private final String surface;

    private RedactingToolCallback(
        ToolCallback delegate, ObjectProvider<McpOutboundRedactorProvider> mcpOutboundRedactorProviderProvider,
        @Nullable Long workspaceId, String surface) {

        this.delegate = delegate;
        this.mcpOutboundRedactorProviderProvider = mcpOutboundRedactorProviderProvider;
        this.workspaceId = workspaceId;
        this.surface = surface;
    }

    public static ToolCallback wrap(
        ToolCallback delegate, ObjectProvider<McpOutboundRedactorProvider> mcpOutboundRedactorProviderProvider,
        @Nullable Long workspaceId, String surface) {

        return new RedactingToolCallback(delegate, mcpOutboundRedactorProviderProvider, workspaceId, surface);
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
        String result;

        try {
            result = delegate.call(toolInput);
        } catch (RuntimeException runtimeException) {
            throw redactFailure(runtimeException);
        }

        return redact(result);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String result;

        try {
            result = delegate.call(toolInput, toolContext);
        } catch (RuntimeException runtimeException) {
            throw redactFailure(runtimeException);
        }

        return redact(result);
    }

    private String redact(String result) {
        return McpOutboundRedaction.redact(result, mcpOutboundRedactorProviderProvider, workspaceId, surface);
    }

    private RuntimeException redactFailure(RuntimeException runtimeException) {
        String message = runtimeException.getMessage();

        if (message == null) {
            return runtimeException;
        }

        return new RedactedToolExecutionException(redact(message), runtimeException);
    }
}
