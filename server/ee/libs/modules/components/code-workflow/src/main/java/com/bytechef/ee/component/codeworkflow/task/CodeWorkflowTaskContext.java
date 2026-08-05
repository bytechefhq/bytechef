/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.task;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.polyglot.ComponentActionInvoker;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.workflow.definition.TaskContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import java.util.Map;
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
 * Also implements {@link ComponentActionInvoker} so the same resolution/dispatch logic can back a polyglot
 * {@code context.component} proxy chain once the loader engines learn to hand this context to guest (JavaScript,
 * Python, Ruby) code workflow tasks.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowTaskContext implements TaskContext, ComponentActionInvoker {

    private final ActionContext actionContext;
    private final ActionDefinitionService actionDefinitionService;
    private final Map<String, ? extends ComponentConnection> componentConnections;
    private final ComponentDefinitionService componentDefinitionService;
    private final @Nullable Long environmentId;

    @SuppressFBWarnings("EI")
    public CodeWorkflowTaskContext(
        ActionContext actionContext, ActionDefinitionService actionDefinitionService,
        Map<String, ? extends ComponentConnection> componentConnections,
        ComponentDefinitionService componentDefinitionService) {

        this.actionContext = actionContext;
        this.actionDefinitionService = actionDefinitionService;
        this.componentConnections = componentConnections;
        this.componentDefinitionService = componentDefinitionService;
        this.environmentId = actionContext instanceof ActionContextAware actionContextAware
            ? actionContextAware.getEnvironmentId() : null;
    }

    @Override
    public Object component(String componentName, String actionName, Map<String, ?> input, String connectionName)
        throws Exception {

        return invoke(componentName, actionName, input, connectionName);
    }

    @Override
    public Object invoke(String componentName, String actionName, Map<String, ?> input, String connectionName)
        throws Exception {

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            componentName, null);

        ComponentConnection componentConnection = resolveComponentConnection(connectionName);

        if (componentConnection == null && actionDefinitionService.actionDefinesConnection(
            componentDefinition.getName(), componentDefinition.getVersion(), actionName)) {

            throw new IllegalArgumentException(
                "Connection with name %s does not exist".formatted(connectionName));
        }

        return actionDefinitionService.executePerformForPolyglot(
            componentDefinition.getName(), componentDefinition.getVersion(), actionName, input, componentConnection,
            environmentId, actionContext);
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
    public void log(String level, String message) {
        actionContext.log(log -> logAtLevel(log, level, message));
    }

    private @Nullable ComponentConnection resolveComponentConnection(@Nullable String connectionName) {
        if (connectionName == null) {
            return null;
        }

        return componentConnections.get(connectionName);
    }

    private void logAtLevel(Context.Log log, @Nullable String level, String message) {
        String normalizedLevel = level == null ? "" : level.toLowerCase(Locale.ROOT);

        switch (normalizedLevel) {
            case "debug" -> log.debug(message);
            case "warn" -> log.warn(message);
            case "error" -> log.error(message);
            case "trace" -> log.trace(message);
            default -> log.info(message);
        }
    }
}
