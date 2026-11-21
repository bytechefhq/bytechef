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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.configuration.exception.ProjectWorkflowErrorType;
import com.bytechef.exception.ExecutionException;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class EnvironmentScopedToolCallbackTest {

    private static final String SCHEMA =
        "{\"type\": \"object\", \"properties\": {\"workflowId\": {\"type\": \"string\"}}}";

    private final JsonMapper jsonMapper = new JsonMapper();
    private CapturingDelegate delegate;
    private EnvironmentScopedToolCallback toolCallback;

    @BeforeEach
    void beforeEach() {
        delegate = new CapturingDelegate();

        toolCallback = new EnvironmentScopedToolCallback(delegate);
    }

    @Test
    void testDefinitionAddsAnOptionalEnvironmentProperty() {
        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        JsonNode schemaNode = jsonMapper.readTree(toolDefinition.inputSchema());

        assertThat(toolDefinition.name()).isEqualTo("getWorkflow");
        assertThat(schemaNode.path("properties")
            .has("environment")).isTrue();
        assertThat(schemaNode.path("required")
            .toString()).doesNotContain("environment");
    }

    @Test
    void testForwardsDevelopmentWhenEnvironmentIsAbsent() {
        String result = toolCallback.call("{\"workflowId\":\"w\"}");

        assertThat(result).isEqualTo("done");
        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 0L);
        assertThat(delegate.capturedInput).isEqualTo("{\"workflowId\":\"w\"}");
    }

    @Test
    void testForwardsTheRequestedEnvironment() {
        toolCallback.call("{\"workflowId\":\"w\",\"environment\":\"STAGING\"}");

        assertThat(delegate.capturedContext)
            .containsEntry(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L)
            .containsEntry(AgentToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 1L);
        assertThat(delegate.capturedInput).isEqualTo("{\"workflowId\":\"w\"}");
    }

    @Test
    void testRejectsAnUnknownEnvironment() {
        String result = toolCallback.call("{\"workflowId\":\"w\",\"environment\":\"prod\"}");

        assertThat(result).contains("error");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testRejectsMalformedInputWithoutEchoingTheParserText() {
        String result = toolCallback.call("{\"workflowId\": secret-token}");

        assertThat(result).contains("Invalid input: not valid JSON")
            .doesNotContain("secret-token");
        assertThat(delegate.capturedContext).isNull();
    }

    @Test
    void testReturnsTheMessageOfAFailedDelegateCall() {
        delegate.failure = new ToolExecutionException(
            delegate.getToolDefinition(),
            new ExecutionException(
                "Nothing was saved: 1 validation error(s)", ProjectWorkflowErrorType.UPDATE_WORKFLOW));

        String result = toolCallback.call("{\"workflowId\":\"w\"}");

        assertThat(jsonMapper.readTree(result)
            .path("error")
            .asString()).isEqualTo("Nothing was saved: 1 validation error(s)");
    }

    @Test
    void testReducesAnUnexpectedDelegateFailureToItsClassName() {
        delegate.failure = new ToolExecutionException(
            delegate.getToolDefinition(), new IllegalStateException("secret"));

        String result = toolCallback.call("{\"workflowId\":\"w\"}");

        assertThat(result)
            .contains("IllegalStateException")
            .doesNotContain("secret");
    }

    @Test
    void testKeepsExistingContextEntries() {
        toolCallback.call("{\"workflowId\":\"w\"}", new ToolContext(Map.of("exchange", "x")));

        assertThat(delegate.capturedContext).containsEntry("exchange", "x");
    }

    private static final class CapturingDelegate implements ToolCallback {

        private Map<String, Object> capturedContext;
        private String capturedInput;
        private RuntimeException failure;

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("getWorkflow")
                .description("Get a workflow.")
                .inputSchema(SCHEMA)
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            capturedInput = toolInput;
            capturedContext = toolContext == null ? null : toolContext.getContext();

            if (failure != null) {
                throw failure;
            }

            return "done";
        }
    }
}
