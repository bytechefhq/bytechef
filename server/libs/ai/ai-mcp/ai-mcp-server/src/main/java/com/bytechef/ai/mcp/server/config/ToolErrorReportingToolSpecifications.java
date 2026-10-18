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

package com.bytechef.ai.mcp.server.config;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.AbstractException;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.ToolMetadata;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
final class ToolErrorReportingToolSpecifications {

    private static final Logger log = LoggerFactory.getLogger(ToolErrorReportingToolSpecifications.class);

    private static final String ERROR_FIELD = "error";

    private ToolErrorReportingToolSpecifications() {
    }

    static List<McpServerFeatures.AsyncToolSpecification> toAsyncToolSpecifications(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(ToolErrorSanitizingToolCallback::new)
            .map(McpToolUtils::toAsyncToolSpecification)
            .map(ToolErrorReportingToolSpecifications::reportToolErrors)
            .toList();
    }

    private static McpServerFeatures.AsyncToolSpecification reportToolErrors(
        McpServerFeatures.AsyncToolSpecification toolSpecification) {

        return new McpServerFeatures.AsyncToolSpecification(
            toolSpecification.tool(),
            (exchange, callToolRequest) -> toolSpecification.callHandler()
                .apply(exchange, callToolRequest)
                .map(ToolErrorReportingToolSpecifications::markToolError));
    }

    private static McpSchema.CallToolResult markToolError(McpSchema.CallToolResult callToolResult) {
        if (Boolean.TRUE.equals(callToolResult.isError()) || !isToolErrorEnvelope(callToolResult)) {
            return callToolResult;
        }

        return new McpSchema.CallToolResult(
            callToolResult.content(), true, callToolResult.structuredContent(), callToolResult.meta());
    }

    private static boolean isToolErrorEnvelope(McpSchema.CallToolResult callToolResult) {
        List<McpSchema.Content> contents = callToolResult.content();

        if (contents == null || contents.size() != 1 ||
            !(contents.getFirst() instanceof McpSchema.TextContent textContent)) {

            return false;
        }

        String text = textContent.text();

        if (text == null || !text.startsWith("{") || !text.contains(ERROR_FIELD)) {
            return false;
        }

        JsonNode jsonNode;

        try {
            jsonNode = JsonUtils.readTree(text);
        } catch (JacksonException exception) {
            return false;
        }

        if (jsonNode == null || !jsonNode.isObject() || jsonNode.size() != 1) {
            return false;
        }

        JsonNode errorJsonNode = jsonNode.get(ERROR_FIELD);

        return errorJsonNode != null && errorJsonNode.isString();
    }

    private static @Nullable String findUserFacingMessage(Throwable toolError) {
        if (!(toolError instanceof IllegalArgumentException) && !(toolError instanceof AbstractException)) {
            return null;
        }

        String message = toolError.getMessage();

        if (message == null || message.isBlank()) {
            return null;
        }

        Throwable cause = toolError.getCause();

        if (cause != null && message.equals(cause.toString())) {
            return null;
        }

        return message;
    }

    private static Throwable unwrapToolExecutionException(Throwable throwable) {
        Throwable toolError = throwable;

        while (toolError instanceof ToolExecutionException && toolError.getCause() != null) {
            toolError = toolError.getCause();
        }

        return toolError;
    }

    private static final class ToolErrorSanitizingToolCallback implements ToolCallback {

        private final ToolCallback toolCallback;

        private ToolErrorSanitizingToolCallback(ToolCallback toolCallback) {
            this.toolCallback = toolCallback;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return toolCallback.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return toolCallback.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            try {
                return toolCallback.call(toolInput);
            } catch (RuntimeException exception) {
                throw toToolErrorException(exception);
            }
        }

        @Override
        public String call(String toolInput, @Nullable ToolContext toolContext) {
            try {
                return toolCallback.call(toolInput, toolContext);
            } catch (RuntimeException exception) {
                throw toToolErrorException(exception);
            }
        }

        private ToolErrorException toToolErrorException(RuntimeException exception) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();

            String toolName = toolDefinition.name();

            Throwable toolError = unwrapToolExecutionException(exception);

            String message = findUserFacingMessage(toolError);

            if (message == null) {
                log.warn("MCP tool {} failed", toolName, exception);

                Class<? extends Throwable> toolErrorClass = toolError.getClass();

                message = toolName + " failed (" + toolErrorClass.getSimpleName() + ")";
            } else if (log.isDebugEnabled()) {
                log.debug("MCP tool {} failed: {}", toolName, message);
            }

            return new ToolErrorException(message, exception);
        }
    }

    private static final class ToolErrorException extends RuntimeException {

        private ToolErrorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
