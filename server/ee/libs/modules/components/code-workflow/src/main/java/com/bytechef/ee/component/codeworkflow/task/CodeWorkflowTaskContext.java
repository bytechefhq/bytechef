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
 * Host-side {@link TaskContext} handed to a code workflow task's
 * {@link com.bytechef.workflow.definition.TaskDefinition.PerformFunction} at perform time.
 *
 * <p>
 * {@link #component(String, String, Map, String)} resolves the named connection from the perform action's forwarded
 * {@code componentConnections} map — the connections wired to this task, keyed by the name the task declared in its
 * source (see {@code CodeWorkflowComponentConnectionFactory}). A name absent from that map, on an action whose
 * component requires a connection, fails with an {@link IllegalArgumentException} naming the connection. Dispatch goes
 * through {@link ActionDefinitionService#executePerformForPolyglot}. {@link #log(String, String)} delegates to the
 * forwarded {@link ActionContext#log}.
 *
 * <p>
 * {@link #input()} exposes the job context snapshot the engine evaluated into the task's {@code input} parameter — the
 * workflow's inputs plus every prior task's output, keyed by name. It is read from the evaluated parameters rather than
 * from expressions in the source because a task name that is not a valid identifier (a hyphen, say) cannot be written
 * as a {@code ${...}} reference at all.
 *
 * <p>
 * Also implements {@link ComponentActionInvoker} so the same resolution/dispatch logic can back a polyglot
 * {@code context.component} proxy chain once the loader engines learn to hand this context to guest (JavaScript,
 * Python, Ruby) code workflow tasks.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowTaskContext implements TaskContext, ComponentActionInvoker {

    /**
     * The element member naming one of the task's declared connections — this call surface's own vocabulary, not a
     * workflow definition key, since nothing about an element is ever written into the definition.
     */
    private static final String CONNECTION = "connection";

    /**
     * Mirrors {@code com.bytechef.platform.ai.tool.constant.ToolConstants#TOOL_NAME}; kept as a literal so a code
     * workflow task does not drag the AI tool module onto its classpath for one key.
     */
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

        // The task's whole wiring goes along, keyed by the names it declared, so an action whose perform takes
        // several connections finds them where it looks. Each cluster element adds its own entry, since the agent
        // looks an element's connection up by the element's name rather than by the name the task declared.
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
            // A name the deploy path recorded as concurrent is not a typo but a timing mistake, so say which one it
            // is rather than leaving the author reading a list of available names for a name that is spelled right.
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

    /**
     * Turns the elements the call site composed into the {@code extensions.clusterElements} map the platform already
     * speaks — the same shape a visual agent node carries — adding each element's connection to {@code connections}
     * under the element's own name, which is where the agent looks for it.
     */
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

        // Nothing here was checked at save time, so each failure names what is wrong and what a right one looks like.
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

        // A hand-written tool list would otherwise leave the model with the platform's generated names, which say
        // nothing about why this tool is in this agent's list.
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
