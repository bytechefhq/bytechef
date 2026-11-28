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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ErrorType;
import com.bytechef.exception.ExecutionException;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;

/**
 * @author Ivica Cardic
 */
class ToolErrorReportingToolSpecificationsSanitizationTest {

    private static final ErrorType ERROR_TYPE = new ErrorType() {

        @Override
        public Class<?> getErrorClass() {
            return ToolErrorReportingToolSpecificationsSanitizationTest.class;
        }

        @Override
        public int getErrorKey() {
            return 100;
        }
    };

    private static final ToolDefinition TOOL_DEFINITION = ToolDefinition.builder()
        .name("updateWorkflow")
        .description("updateWorkflow")
        .inputSchema("{\"type\":\"object\",\"properties\":{}}")
        .build();

    @Test
    void testExecutionExceptionMessageReachesTheClient() {
        assertToolError(new ExecutionException("Workflow 7 not found", ERROR_TYPE), "Workflow 7 not found");
    }

    @Test
    void testConfigurationExceptionMessageReachesTheClient() {
        assertToolError(
            new ConfigurationException("Invalid workflow definition", ERROR_TYPE), "Invalid workflow definition");
    }

    @Test
    void testIllegalArgumentExceptionMessageReachesTheClient() {
        assertToolError(new IllegalArgumentException("projectId is required"), "projectId is required");
    }

    @Test
    void testNullPointerExceptionIsReducedToItsClassName() {
        assertToolError(new NullPointerException("secret"), "updateWorkflow failed (NullPointerException)");
    }

    @Test
    void testIllegalStateExceptionIsReducedToItsClassName() {
        assertToolError(new IllegalStateException("secret"), "updateWorkflow failed (IllegalStateException)");
    }

    @Test
    void testDataAccessLikeExceptionIsReducedToItsClassName() {
        assertToolError(
            new BadSqlGrammarException("SQL [select * from workflow where secret = 'token'] failed"),
            "updateWorkflow failed (BadSqlGrammarException)");
    }

    @Test
    void testAbstractExceptionCarryingOnlyItsCauseIsReducedToItsClassName() {
        assertToolError(
            new ExecutionException(new IllegalStateException("secret"), ERROR_TYPE),
            "updateWorkflow failed (ExecutionException)");
    }

    @Test
    void testIllegalArgumentExceptionCarryingOnlyItsCauseIsReducedToItsClassName() {
        assertToolError(
            new IllegalArgumentException(new IllegalStateException("secret")),
            "updateWorkflow failed (IllegalArgumentException)");
    }

    @Test
    void testToolExecutionExceptionWrappingAnExecutionExceptionKeepsItsMessage() {
        assertToolError(
            new ToolExecutionException(TOOL_DEFINITION, new ExecutionException("Workflow 7 not found", ERROR_TYPE)),
            "Workflow 7 not found");
    }

    @Test
    void testToolExecutionExceptionWrappingAnIllegalArgumentExceptionKeepsItsMessage() {
        assertToolError(
            new ToolExecutionException(TOOL_DEFINITION, new IllegalArgumentException("projectId is required")),
            "projectId is required");
    }

    @Test
    void testToolExecutionExceptionWrappingANullPointerExceptionIsReducedToItsClassName() {
        assertToolError(
            new ToolExecutionException(TOOL_DEFINITION, new NullPointerException("secret")),
            "updateWorkflow failed (NullPointerException)");
    }

    @Test
    void testToolExecutionExceptionWrappingAnIllegalStateExceptionIsReducedToItsClassName() {
        assertToolError(
            new ToolExecutionException(TOOL_DEFINITION, new IllegalStateException("secret")),
            "updateWorkflow failed (IllegalStateException)");
    }

    @Test
    void testToolExecutionExceptionWrappingADataAccessLikeExceptionIsReducedToItsClassName() {
        assertToolError(
            new ToolExecutionException(
                TOOL_DEFINITION, new BadSqlGrammarException("SQL [select * from workflow where secret = 'token']")),
            "updateWorkflow failed (BadSqlGrammarException)");
    }

    private static void assertToolError(RuntimeException exception, String expectedText) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition()).thenReturn(TOOL_DEFINITION);
        when(toolCallback.call(ArgumentMatchers.anyString())).thenThrow(exception);
        when(toolCallback.call(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenThrow(exception);

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications =
            ToolErrorReportingToolSpecifications.toAsyncToolSpecifications(List.of(toolCallback));

        McpServerFeatures.AsyncToolSpecification toolSpecification = toolSpecifications.getFirst();

        McpSchema.CallToolRequest callToolRequest = McpSchema.CallToolRequest.builder()
            .name("updateWorkflow")
            .arguments(Map.of())
            .build();

        McpSchema.CallToolResult callToolResult = toolSpecification.callHandler()
            .apply(mock(McpAsyncServerExchange.class), callToolRequest)
            .block();

        assertThat(callToolResult.isError()).isTrue();

        McpSchema.TextContent textContent = (McpSchema.TextContent) callToolResult.content()
            .getFirst();

        assertThat(textContent.text()).isEqualTo(expectedText);
    }

    private static final class BadSqlGrammarException extends RuntimeException {

        private BadSqlGrammarException(String message) {
            super(message);
        }
    }
}
