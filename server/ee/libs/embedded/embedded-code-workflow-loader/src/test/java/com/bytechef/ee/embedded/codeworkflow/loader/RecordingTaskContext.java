/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import com.bytechef.workflow.definition.TaskContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

/**
 * Test double recording the arguments a task perform passes through the {@link TaskContext} seam.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
class RecordingTaskContext implements TaskContext {

    private final Object componentResult;
    private final Map<String, ?> connectionParameters;
    private final Map<String, ?> taskInput;

    private String componentName;
    private String actionName;
    private Map<String, ?> input;
    private String connectionName;
    private Map<String, ?> clusterElements;
    private String logLevel;
    private String logMessage;

    RecordingTaskContext(Object componentResult) {
        this(componentResult, Map.of());
    }

    RecordingTaskContext(Object componentResult, Map<String, ?> connectionParameters) {
        this(componentResult, connectionParameters, Map.of());
    }

    RecordingTaskContext(Object componentResult, Map<String, ?> connectionParameters, Map<String, ?> taskInput) {
        this.componentResult = componentResult;
        this.connectionParameters = connectionParameters;
        this.taskInput = taskInput;
    }

    @Override
    public Object component(
        String componentName, String actionName, Map<String, ?> input, String connectionName,
        Map<String, ?> clusterElements) {

        this.componentName = componentName;
        this.actionName = actionName;
        this.input = input;
        this.connectionName = connectionName;
        this.clusterElements = clusterElements;

        return componentResult;
    }

    @Override
    public Map<String, ?> input() {
        return taskInput;
    }

    @Override
    public Object input(String name) {
        if (!taskInput.containsKey(name)) {
            throw new IllegalArgumentException("No workflow input or task output named " + name);
        }

        return taskInput.get(name);
    }

    @Override
    public Map<String, ?> parameters() {
        return Map.of();
    }

    @Override
    public Map<String, ?> connection(String connectionName) {
        this.connectionName = connectionName;

        return connectionParameters;
    }

    @Override
    public void log(LogLevel level, String message) {
        this.logLevel = level.name();
        this.logMessage = message;
    }

    String getComponentName() {
        return componentName;
    }

    String getActionName() {
        return actionName;
    }

    Map<String, ?> getInput() {
        return input;
    }

    String getConnectionName() {
        return connectionName;
    }

    Map<String, ?> getClusterElements() {
        return clusterElements;
    }

    String getLogLevel() {
        return logLevel;
    }

    String getLogMessage() {
        return logMessage;
    }
}
