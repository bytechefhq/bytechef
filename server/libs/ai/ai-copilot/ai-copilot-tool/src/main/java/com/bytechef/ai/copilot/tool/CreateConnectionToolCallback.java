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

package com.bytechef.ai.copilot.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.util.ComponentSlugUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * Signaling-only Spring AI {@link ToolCallback} that instructs the chat client to open a "Connect &lt;Component&gt;"
 * dialog inline in the chat thread. The server returns a marker payload with {@code kind: "create-connection"} that the
 * client subscriber recognises and renders as a button. No connection is created server-side; the client drives OAuth,
 * credential entry, and persistence through the existing {@code ConnectionDialog}. Companion to
 * {@link SelectConnectionToolCallback}, which is reserved for the "pick an existing connection" intent — this one is
 * reserved for the "create a new connection" intent.
 *
 * @author Ivica Cardic
 */
public class CreateConnectionToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CreateConnectionToolCallback.class);

    private static final String DESCRIPTION = """
        Request the user to create a connection for a specific component — for example Slack,
        Gmail, HubSpot, or any other integration. Call this when the user says "connect Slack",
        "add my Gmail account", "hook up HubSpot", etc. The client renders a "Connect
        <ComponentLabel>" button inline in the chat; clicking it opens the ConnectionDialog
        prefilled with the component. Do NOT try to create connections via any other tool —
        the UI handles credentials, OAuth, and persistence.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "componentName": {
                        "type": "string",
                        "description": "Lowercase slug of the component to connect — e.g. \\"slack\\", \\"gmail\\", \\"hubspot\\""
                    },
                    "suggestedName": {
                        "type": "string",
                        "description": "Optional suggested display name for the new connection"
                    }
                },
                "required": ["componentName"]
            }""";

    private final ComponentDefinitionService componentDefinitionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateConnectionToolCallback(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("createConnection")
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            CreateConnectionInput input = JsonUtils.read(toolInput, CreateConnectionInput.class);

            String componentName = input.componentName();

            if (componentName == null || componentName.isBlank()) {
                return toolError("componentName is required and must not be blank");
            }

            Optional<ComponentDefinition> componentDefinition = componentDefinitionService.fetchComponentDefinition(
                componentName, null);

            String resolvedFromComponentName = null;

            if (componentDefinition.isEmpty()) {
                String resolvedComponentName = ComponentSlugUtils.resolveSingleMatch(
                    componentName, componentDefinitionService);

                if (resolvedComponentName == null) {
                    return toolError(ComponentSlugUtils.unknownComponentMessage(componentName,
                        componentDefinitionService));
                }

                resolvedFromComponentName = componentName;
                componentName = resolvedComponentName;
                componentDefinition = componentDefinitionService.fetchComponentDefinition(componentName, null);

                if (componentDefinition.isEmpty()) {
                    return toolError(
                        ComponentSlugUtils.unknownComponentMessage(componentName, componentDefinitionService));
                }
            }

            String componentLabel = resolveComponentLabel(componentDefinition.get(), componentName);

            return JsonUtils.write(
                new CreateConnectionOutput(
                    "create-connection", componentName, componentLabel, resolvedFromComponentName,
                    input.suggestedName()));
        } catch (JacksonException exception) {
            log.warn(
                "createConnection rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                CreateConnectionToolCallback.class, "createConnection", exception);
        }
    }

    private static String resolveComponentLabel(ComponentDefinition componentDefinition, String componentName) {
        String title = componentDefinition.getTitle();

        return (title == null || title.isBlank()) ? componentName : title;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record CreateConnectionInput(String componentName, @Nullable String suggestedName) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateConnectionOutput(String kind, String componentName, String componentLabel,
        @Nullable String resolvedFromComponentName, @Nullable String suggestedName) {
    }
}
