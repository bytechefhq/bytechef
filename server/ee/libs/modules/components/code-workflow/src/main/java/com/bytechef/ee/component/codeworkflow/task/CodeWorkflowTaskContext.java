/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.task;

import static com.bytechef.atlas.configuration.constant.WorkflowConstants.NAME;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.PARAMETERS;
import static com.bytechef.atlas.configuration.constant.WorkflowConstants.TYPE;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.polyglot.ComponentActionInvoker;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.workflow.definition.TaskContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowTaskContext implements TaskContext, ComponentActionInvoker {

    private static final String CONNECTION = "connection";

    private static final String TOOL_NAME = "toolName";

    private final ActionContext actionContext;
    private final ActionDefinitionService actionDefinitionService;
    private final Map<String, ? extends ComponentConnection> componentConnections;
    private final ComponentDefinitionService componentDefinitionService;
    private final @Nullable Long environmentId;
    private final Map<String, ?> input;
    private final List<String> concurrentTaskNames;
    private final Map<String, ?> parameters;

    @SuppressFBWarnings("EI")
    public CodeWorkflowTaskContext(
        ActionContext actionContext, ActionDefinitionService actionDefinitionService,
        Map<String, ? extends ComponentConnection> componentConnections,
        ComponentDefinitionService componentDefinitionService, Map<String, ?> input,
        List<String> concurrentTaskNames, Map<String, ?> parameters) {

        this.actionContext = actionContext;
        this.actionDefinitionService = actionDefinitionService;
        this.componentConnections = componentConnections;
        this.componentDefinitionService = componentDefinitionService;
        this.input = Collections.unmodifiableMap(new LinkedHashMap<>(input));
        this.concurrentTaskNames = List.copyOf(concurrentTaskNames);
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        this.environmentId = actionContext instanceof ActionContextAware actionContextAware
            ? actionContextAware.getEnvironmentId() : null;
    }

    @Override
    public Object component(
        String componentName, String actionName, Map<String, ?> input, String connectionName,
        @Nullable Map<String, ?> clusterElements) throws Exception {

        return invoke(componentName, actionName, input, connectionName, clusterElements);
    }

    @Override
    public Object invoke(
        String componentName, String actionName, Map<String, ?> input, @Nullable String connectionName,
        @Nullable Map<String, ?> clusterElements) throws Exception {

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            componentName, null);

        ComponentConnection componentConnection = resolveComponentConnection(connectionName);

        if (componentConnection == null && actionDefinitionService.actionDefinesConnection(
            componentDefinition.getName(), componentDefinition.getVersion(), actionName)) {

            throw new IllegalArgumentException(
                "Connection with name %s does not exist".formatted(connectionName));
        }

        Map<String, ComponentConnection> connections = new LinkedHashMap<>(componentConnections);

        Map<String, ?> extensions = composeExtensions(clusterElements, connections);

        return actionDefinitionService.executePerformForPolyglot(
            componentDefinition.getName(), componentDefinition.getVersion(), actionName, input, componentConnection,
            connections, extensions, environmentId, actionContext);
    }

    @Override
    public Map<String, ?> input() {
        return input;
    }

    @Override
    public Object input(String name) {
        if (!input.containsKey(name)) {
            if (concurrentTaskNames.contains(name)) {
                throw new IllegalArgumentException(
                    ("Task %s runs at the same time as this task, so its output is not available; move it before the "
                        + "group to read it here").formatted(name));
            }

            throw new IllegalArgumentException(
                "No workflow input or task output named %s; available: %s".formatted(name, input.keySet()));
        }

        return input.get(name);
    }

    @Override
    public Map<String, ?> parameters() {
        return parameters;
    }

    @Override
    public Map<String, ?> connection(String connectionName) {
        ComponentConnection componentConnection = resolveComponentConnection(connectionName);

        if (componentConnection == null) {
            throw new IllegalArgumentException(
                "Connection with name %s does not exist".formatted(connectionName));
        }

        return componentConnection.getParameters();
    }

    @Override
    public void log(LogLevel level, String message) {
        actionContext.log(log -> logAtLevel(log, level, message));
    }

    private Map<String, ?> composeExtensions(
        @Nullable Map<String, ?> clusterElements, Map<String, ComponentConnection> connections) {

        if (clusterElements == null || clusterElements.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> clusterElementEntries = new LinkedHashMap<>();

        for (Map.Entry<String, ?> entry : clusterElements.entrySet()) {
            String clusterElementType = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof List<?> list) {
                List<Map<String, ?>> elements = new ArrayList<>();

                for (Object element : list) {
                    elements.add(toClusterElement(clusterElementType, element, connections));
                }

                clusterElementEntries.put(clusterElementType, elements);
            } else {
                clusterElementEntries.put(
                    clusterElementType, toClusterElement(clusterElementType, value, connections));
            }
        }

        return Map.of(WorkflowExtConstants.CLUSTER_ELEMENTS, clusterElementEntries);
    }

    private Map<String, ?> toClusterElement(
        String clusterElementType, @Nullable Object element, Map<String, ComponentConnection> connections) {

        if (!(element instanceof Map<?, ?> elementMap)) {
            throw new IllegalArgumentException(
                "Cluster element %s must be an object, such as {type: \"openAi/v1/model\"}"
                    .formatted(clusterElementType));
        }

        if (!(elementMap.get(TYPE) instanceof String type) || type.isBlank()) {
            throw new IllegalArgumentException(
                "Cluster element %s requires a type, such as openAi/v1/model".formatted(clusterElementType));
        }

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        String name = elementMap.get(NAME) instanceof String string && !string.isBlank()
            ? string : workflowNodeType.operation();

        Map<String, Object> parameters = new LinkedHashMap<>();

        if (elementMap.get(PARAMETERS) instanceof Map<?, ?> declaredParameters) {
            for (Map.Entry<?, ?> parameterEntry : declaredParameters.entrySet()) {
                parameters.put(String.valueOf(parameterEntry.getKey()), parameterEntry.getValue());
            }
        }

        if (Objects.equals(clusterElementType, TOOLS.key()) && elementMap.get(NAME) instanceof String toolName) {
            parameters.putIfAbsent(TOOL_NAME, toolName);
        }

        if (elementMap.get(CONNECTION) instanceof String connectionName) {
            ComponentConnection componentConnection = resolveComponentConnection(connectionName);

            if (componentConnection == null) {
                throw new IllegalArgumentException(
                    "Connection with name %s does not exist".formatted(connectionName));
            }

            connections.put(name, componentConnection);
        }

        return Map.of(NAME, name, TYPE, type, PARAMETERS, parameters);
    }

    private @Nullable ComponentConnection resolveComponentConnection(@Nullable String connectionName) {
        if (connectionName == null) {
            return null;
        }

        return componentConnections.get(connectionName);
    }

    private void logAtLevel(Context.Log log, LogLevel level, String message) {
        switch (level) {
            case DEBUG -> log.debug(message);
            case WARN -> log.warn(message);
            case ERROR -> log.error(message);
            case TRACE -> log.trace(message);
            default -> log.info(message);
        }
    }
}
