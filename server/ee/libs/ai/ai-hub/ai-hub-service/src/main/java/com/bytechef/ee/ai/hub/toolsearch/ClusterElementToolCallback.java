/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister.CopilotConnection;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister.CopilotConnectionRequest;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MemoizationUtils;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ClusterElementToolCallback implements ToolCallback {

    static final String EMPTY_INPUT_SCHEMA = "{\"type\":\"object\",\"properties\":{}}";

    private static final Logger log = LoggerFactory.getLogger(ClusterElementToolCallback.class);

    private final String toolName;
    private final String description;
    private final String componentName;
    private final int componentVersion;
    private final String clusterElementName;
    private final Supplier<String> inputSchemaSupplier;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final CopilotConnectionLister connectionLister;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ClusterElementToolCallback(
        String toolName, String description, String componentName, int componentVersion, String clusterElementName,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ConnectionDefinitionService connectionDefinitionService, CopilotConnectionLister connectionLister) {

        this.toolName = toolName;
        this.description = description;
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.clusterElementName = clusterElementName;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionLister = connectionLister;
        this.inputSchemaSupplier = MemoizationUtils.memoize(
            () -> generateInputSchema(
                clusterElementDefinitionService, toolName, componentName, componentVersion, clusterElementName));
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(toolName)
            .description(description)
            .inputSchema(inputSchemaSupplier.get())
            .build();
    }

    public String getToolName() {
        return toolName;
    }

    public String getComponentName() {
        return componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public String getClusterElementName() {
        return clusterElementName;
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            Map<String, Object> inputParameters = toolInput == null || toolInput.isBlank()
                ? Map.of()
                : JsonUtils.read(toolInput, Map.class);

            AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null ||
                invocationContext.environmentId() == null) {

                return ToolErrors.toolError(
                    "This chat has no workspace and environment, so " + toolName + " cannot run.");
            }

            ConnectionResolution connectionResolution = resolveConnection(invocationContext);

            if (connectionResolution.error() != null) {
                return connectionResolution.error();
            }

            Object result = clusterElementDefinitionFacade.executeTool(
                componentName, componentVersion, clusterElementName, inputParameters,
                connectionResolution.connectionId());

            return JsonUtils.write(result);
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (IllegalArgumentException exception) {
            return ToolErrors.toolError(String.valueOf(exception.getMessage()));
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(ClusterElementToolCallback.class, toolName, exception);
        }
    }

    private ConnectionResolution resolveConnection(AgentToolInvocationContext invocationContext) {
        if (!definesConnection()) {
            return new ConnectionResolution(null, null);
        }

        CopilotConnectionRequest request = invocationContext.toConnectionRequest(componentName, null);

        List<CopilotConnection> connections = connectionLister.listConnections(request);

        if (connections.isEmpty()) {
            Map<String, Object> envelope = new LinkedHashMap<>();

            envelope.put("connectionRequired", true);
            envelope.put("componentName", componentName);
            envelope.put("componentVersion", componentVersion);
            envelope.put(
                "message",
                "No connection for component '" + componentName + "' in this workspace and environment. " +
                    "Ask the user to connect this component before calling " + toolName + ".");

            return new ConnectionResolution(null, JsonUtils.write(envelope));
        }

        if (connections.size() > 1) {
            Map<String, Object> envelope = new LinkedHashMap<>();

            envelope.put("connectionAmbiguous", true);
            envelope.put("componentName", componentName);
            envelope.put(
                "candidates",
                connections.stream()
                    .map(connection -> Map.<String, Object>of("id", connection.id(), "name", connection.name()))
                    .toList());
            envelope.put(
                "message",
                "Multiple connections found for '" + componentName + "'. " + toolName + " cannot choose one; ask " +
                    "the user which connection to use.");

            return new ConnectionResolution(null, JsonUtils.write(envelope));
        }

        CopilotConnection connection = connections.getFirst();

        return new ConnectionResolution(connection.id(), null);
    }

    private boolean definesConnection() {
        try {
            connectionDefinitionService.getConnectionDefinition(componentName, componentVersion);

            return true;
        } catch (NoSuchElementException exception) {
            return false;
        }
    }

    private static String generateInputSchema(
        ClusterElementDefinitionService clusterElementDefinitionService, String toolName, String componentName,
        int componentVersion, String clusterElementName) {

        try {
            ClusterElementDefinition clusterElementDefinition =
                clusterElementDefinitionService.getClusterElementDefinition(
                    componentName, componentVersion, clusterElementName);

            return JsonSchemaGeneratorUtils.generateInputSchema(clusterElementDefinition.getProperties());
        } catch (RuntimeException exception) {
            log.warn(
                "Could not generate the input schema of tool {} ({} v{}), so it is offered with an empty schema",
                toolName, componentName, componentVersion, exception);

            return EMPTY_INPUT_SCHEMA;
        }
    }

    private record ConnectionResolution(@Nullable Long connectionId, @Nullable String error) {
    }
}
