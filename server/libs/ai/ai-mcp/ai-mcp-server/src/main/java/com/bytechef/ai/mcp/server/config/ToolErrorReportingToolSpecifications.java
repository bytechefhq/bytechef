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
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * Converts management tool callbacks into MCP tool specifications whose results carry {@code isError: true} when the
 * callback answered with the {@code {"error": "<message>"}} envelope that {@code ToolErrors} produces.
 *
 * @author Ivica Cardic
 */
final class ToolErrorReportingToolSpecifications {

    private static final String ERROR_FIELD = "error";

    private ToolErrorReportingToolSpecifications() {
    }

    static List<McpServerFeatures.AsyncToolSpecification> toAsyncToolSpecifications(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
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
}
