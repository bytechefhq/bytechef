/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CodeWorkflowHostBridgeTest {

    @Test
    void testComponentExecuteParsesRequestAndSerializesResult() {
        RecordingTaskContext taskContext = new RecordingTaskContext(Map.of("y", 2));

        CodeWorkflowHostBridge codeWorkflowHostBridge = new CodeWorkflowHostBridge(taskContext);

        String resultJson = codeWorkflowHostBridge.componentExecute(
            "{\"componentName\": \"mock\", \"actionName\": \"doIt\", \"input\": {\"x\": 1}, " +
                "\"connectionName\": \"conn\"}");

        assertEquals("mock", taskContext.getComponentName());
        assertEquals("doIt", taskContext.getActionName());
        assertEquals(Map.of("x", 1), taskContext.getInput());
        assertEquals("conn", taskContext.getConnectionName());
        assertEquals("{\"y\":2}", resultJson);
    }

    @Test
    void testComponentExecuteDefaultsMissingInputAndConnection() {
        RecordingTaskContext taskContext = new RecordingTaskContext(null);

        CodeWorkflowHostBridge codeWorkflowHostBridge = new CodeWorkflowHostBridge(taskContext);

        String resultJson = codeWorkflowHostBridge.componentExecute(
            "{\"componentName\": \"mock\", \"actionName\": \"doIt\"}");

        assertEquals(Map.of(), taskContext.getInput());
        assertEquals(null, taskContext.getConnectionName());
        assertEquals("null", resultJson);
    }

    @Test
    void testComponentExecuteCarriesClusterElementsAcrossTheSandboxBoundary() {
        RecordingTaskContext taskContext = new RecordingTaskContext(null);

        CodeWorkflowHostBridge codeWorkflowHostBridge = new CodeWorkflowHostBridge(taskContext);

        codeWorkflowHostBridge.componentExecute(
            "{\"componentName\": \"aiAgent\", \"actionName\": \"chat\", \"clusterElements\": " +
                "{\"model\": {\"type\": \"openAi/v1/model\", \"connection\": \"openai-prod\"}}}");

        assertEquals(
            Map.of("model", Map.of("type", "openAi/v1/model", "connection", "openai-prod")),
            taskContext.getClusterElements());
    }

    @Test
    void testComponentExecuteWithoutTaskContextThrows() {
        CodeWorkflowHostBridge codeWorkflowHostBridge = new CodeWorkflowHostBridge(null);

        assertThrows(
            IllegalStateException.class,
            () -> codeWorkflowHostBridge.componentExecute("{\"componentName\": \"mock\", \"actionName\": \"doIt\"}"));
    }

    @Test
    void testConnectionParametersSerializesToJson() {
        RecordingTaskContext taskContext = new RecordingTaskContext(null, Map.of("region", "eu"));

        CodeWorkflowHostBridge codeWorkflowHostBridge = new CodeWorkflowHostBridge(taskContext);

        String parametersJson = codeWorkflowHostBridge.connection("slack-prod");

        assertEquals("slack-prod", taskContext.getConnectionName());
        assertEquals("{\"region\":\"eu\"}", parametersJson);
    }

    @Test
    void testConnectionParametersWithoutTaskContextThrows() {
        CodeWorkflowHostBridge codeWorkflowHostBridge = new CodeWorkflowHostBridge(null);

        assertThrows(IllegalStateException.class, () -> codeWorkflowHostBridge.connection("slack-prod"));
    }

    @Test
    void testLogDelegatesToTaskContext() {
        RecordingTaskContext taskContext = new RecordingTaskContext(null);

        CodeWorkflowHostBridge codeWorkflowHostBridge = new CodeWorkflowHostBridge(taskContext);

        codeWorkflowHostBridge.log("warn", "log message");

        assertEquals("WARN", taskContext.getLogLevel());
        assertEquals("log message", taskContext.getLogMessage());
    }

    @Test
    void testLogWithoutTaskContextThrows() {
        CodeWorkflowHostBridge codeWorkflowHostBridge = new CodeWorkflowHostBridge(null);

        assertThrows(IllegalStateException.class, () -> codeWorkflowHostBridge.log("warn", "log message"));
    }
}
