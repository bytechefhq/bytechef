/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import com.bytechef.workflow.definition.TaskContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Map;
import org.graalvm.polyglot.HostAccess;

/**
 * Host callback object placed into the polyglot bindings for the GraalVM Espresso guest JVM. The
 * {@code @HostAccess.Export} methods below are the ONLY host surface reachable from sandboxed code workflow code — both
 * exchange plain JSON strings so the interop surface stays minimal. The guest-side counterpart is
 * {@code com.bytechef.workflow.guest.GuestTaskContext}, whose {@code HostBridge} view must match these methods.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class CodeWorkflowHostBridge {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    private final TaskContext taskContext;

    CodeWorkflowHostBridge(TaskContext taskContext) {
        this.taskContext = taskContext;
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public String componentExecute(String requestJson) {
        if (taskContext == null) {
            throw new IllegalStateException("A TaskContext is not available");
        }

        try {
            Map<String, ?> request = OBJECT_MAPPER.readValue(requestJson, Map.class);

            Map<String, ?> input = (Map<String, ?>) request.get("input");

            Object result = taskContext.component(
                (String) request.get("componentName"), (String) request.get("actionName"),
                input == null ? Map.of() : input, (String) request.get("connectionName"));

            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @HostAccess.Export
    public String connection(String connectionName) {
        if (taskContext == null) {
            throw new IllegalStateException("A TaskContext is not available");
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(taskContext.connection(connectionName));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @HostAccess.Export
    public void log(String level, String message) {
        if (taskContext == null) {
            throw new IllegalStateException("A TaskContext is not available");
        }

        taskContext.log(level, message);
    }
}
