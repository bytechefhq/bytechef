/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.ai.agent.tool.ToolErrors;
import com.bytechef.ee.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * Spring AI {@link ToolCallback} that lists the workspace's existing connections for a given component, scoped to the
 * current workspace and environment. Used by the LLM before {@code createConnection} so it can offer the user an
 * existing connection instead of forcing them through the create dialog when one is already there.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ListConnectionsForComponentToolCallback implements ToolCallback {

    static final String TOOL_NAME = "listConnectionsForComponent";

    private static final Logger log = LoggerFactory.getLogger(ListConnectionsForComponentToolCallback.class);

    private static final String DESCRIPTION = """
        List existing workspace connections for a component, scoped to the current environment. Use before
        createConnection so the user can pick an existing connection rather than re-entering credentials. Returns
        componentName, connectionVersion (the connection definition's version, NOT the component version), and
        a connections array of {id, name, environmentId, active}.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "componentName": {"type": "string"},
                "componentVersion": {"type": "integer", "description": "Defaults to 1"}
            },
            "required": ["componentName"]
        }""";

    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;
    private final PropertyOptionsResolver resolver;
    private final ToolStateVisibilityMetrics metrics;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListConnectionsForComponentToolCallback(
        ComponentDefinitionService componentDefinitionService, ConnectionDefinitionService connectionDefinitionService,
        WorkspaceConnectionFacade workspaceConnectionFacade, PropertyOptionsResolver resolver,
        ToolStateVisibilityMetrics metrics) {

        this.componentDefinitionService = componentDefinitionService;
        this.connectionDefinitionService = connectionDefinitionService;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
        this.resolver = resolver;
        this.metrics = metrics;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
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
            ListConnectionsForComponentInput input = JsonUtils.read(toolInput, ListConnectionsForComponentInput.class);

            String componentName = input.componentName();

            if (componentName == null || componentName.isBlank()) {
                return toolError("componentName is required and must not be blank");
            }

            AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null) {
                return toolError("Workspace context unavailable — open this chat from the AI Hub.");
            }

            Optional<ComponentDefinition> componentDefinitionOptional =
                componentDefinitionService.fetchComponentDefinition(componentName, null);

            if (componentDefinitionOptional.isEmpty()) {
                metrics.recordStateVisibility(TOOL_NAME, "unknown_component");

                return toolError(
                    ComponentSlugSuggestions.unknownComponentMessage(componentName, componentDefinitionService));
            }

            int componentVersion = input.componentVersion() == null ? 1 : input.componentVersion();

            ConnectionDefinition connectionDefinition;

            try {
                connectionDefinition = connectionDefinitionService.getConnectionDefinition(
                    componentName, componentVersion);
            } catch (RuntimeException exception) {
                log.warn(
                    "Failed to resolve connection definition for {} v{}; treating as no connections available. Reason: {}",
                    componentName, componentVersion, exception.getMessage());

                return JsonUtils.write(buildEmptyEnvelope(componentName));
            }

            int connectionVersion = connectionDefinition.getVersion();

            long environmentId = invocationContext.resolveEnvironmentOrDefault();

            List<ConnectionDTO> connectionDTOs = resolver.withUserSecurityContext(
                invocationContext.userId(),
                () -> workspaceConnectionFacade.getConnections(
                    invocationContext.workspaceId(), componentName, connectionVersion, environmentId, null));

            List<Map<String, Object>> connections = new ArrayList<>(connectionDTOs.size());

            for (ConnectionDTO connectionDTO : connectionDTOs) {
                Map<String, Object> row = new LinkedHashMap<>();

                row.put("id", connectionDTO.id());
                row.put("name", connectionDTO.name());
                row.put("environmentId", connectionDTO.environmentId());
//                row.put("visibility", connectionDTO.visibility()
//                    .name());
                row.put("active", connectionDTO.active());

                connections.add(row);
            }

            Map<String, Object> envelope = new LinkedHashMap<>();

            envelope.put("componentName", componentName);
            envelope.put("connectionVersion", connectionVersion);
            envelope.put("connections", connections);

            metrics.recordStateVisibility(TOOL_NAME, connections.isEmpty() ? "empty" : "success");

            return JsonUtils.write(envelope);
        } catch (JacksonException exception) {
            log.warn(
                "listConnectionsForComponent rejected malformed tool input: {} — first 200 chars: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            metrics.recordStateVisibility(TOOL_NAME, "error");

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            metrics.recordStateVisibility(TOOL_NAME, "error");

            return ToolErrors.runtimeFailure(
                ListConnectionsForComponentToolCallback.class, TOOL_NAME, exception);
        }
    }

    private static Map<String, Object> buildEmptyEnvelope(String componentName) {
        Map<String, Object> envelope = new LinkedHashMap<>();

        envelope.put("componentName", componentName);
        envelope.put("connections", List.of());

        return envelope;
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record ListConnectionsForComponentInput(String componentName, @Nullable Integer componentVersion) {
    }
}
