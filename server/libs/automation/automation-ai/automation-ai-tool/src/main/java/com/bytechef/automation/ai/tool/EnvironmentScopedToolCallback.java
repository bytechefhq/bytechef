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

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Ivica Cardic
 */
public class EnvironmentScopedToolCallback implements ToolCallback {

    private static final String ENVIRONMENT_FIELD = "environment";

    private final ToolCallback delegate;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public EnvironmentScopedToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        ToolDefinition delegateToolDefinition = delegate.getToolDefinition();

        String description = delegateToolDefinition.description() +
            " Supply environment (DEVELOPMENT, STAGING, or PRODUCTION) to target another environment.";

        return ToolDefinition.builder()
            .name(delegateToolDefinition.name())
            .description(description)
            .inputSchema(mergeSchema(delegateToolDefinition.inputSchema()))
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            ObjectNode inputNode = readInputObject(toolInput);

            if (inputNode == null) {
                return ToolErrors.toolError("Invalid tool input: expected a JSON object");
            }

            JsonNode environmentNode = inputNode.remove(ENVIRONMENT_FIELD);

            String requestedEnvironment = isPresent(environmentNode) ? environmentNode.asString() : null;

            long environmentId;

            try {
                environmentId = AutomationToolInvocationContext.resolveEnvironmentId(
                    requestedEnvironment, toolContext);
            } catch (UnknownEnvironmentException exception) {
                return ToolErrors.toolError(exception.getMessage());
            }

            Map<String, Object> forwardedContext = AutomationToolInvocationContext.withEnvironment(
                toolContext, environmentId);

            return delegate.call(JsonUtils.write(inputNode), new ToolContext(forwardedContext));
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (RuntimeException exception) {
            ToolDefinition toolDefinition = delegate.getToolDefinition();

            return DelegateToolErrors.toToolError(
                EnvironmentScopedToolCallback.class, toolDefinition.name(), exception);
        }
    }

    private @Nullable ObjectNode readInputObject(@Nullable String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return JsonUtils.createObjectNode();
        }

        JsonNode node = JsonUtils.readTree(toolInput);

        return node.isObject() ? (ObjectNode) node : null;
    }

    private static boolean isPresent(@Nullable JsonNode fieldNode) {
        if (fieldNode == null || fieldNode.isNull() || fieldNode.isMissingNode()) {
            return false;
        }

        return !fieldNode.isString() || !fieldNode.asString()
            .isBlank();
    }

    private String mergeSchema(String delegateInputSchema) {
        JsonNode schemaNode = JsonUtils.readTree(delegateInputSchema);

        if (!schemaNode.isObject()) {
            return delegateInputSchema;
        }

        ObjectNode schemaObjectNode = (ObjectNode) schemaNode;
        JsonNode propertiesNode = schemaObjectNode.get("properties");

        ObjectNode propertiesObjectNode = propertiesNode != null && propertiesNode.isObject()
            ? (ObjectNode) propertiesNode
            : schemaObjectNode.putObject("properties");

        propertiesObjectNode.putObject(ENVIRONMENT_FIELD)
            .put("type", "string")
            .put("description",
                "Target environment: DEVELOPMENT, STAGING, or PRODUCTION. Optional - defaults to DEVELOPMENT.");

        return JsonUtils.write(schemaObjectNode);
    }
}
