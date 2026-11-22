/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static com.bytechef.workflow.definition.WorkflowDsl.componentTask;
import static com.bytechef.workflow.definition.WorkflowDsl.parallel;
import static com.bytechef.workflow.definition.WorkflowDsl.task;
import static com.bytechef.workflow.definition.WorkflowDsl.workflow;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.workflow.definition.ComponentTaskDefinition;
import com.bytechef.workflow.definition.CompositeTaskDefinition;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowTaskDefinition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationImplicitConnectionsTest {

    @Test
    void testImplicitConnectionsLeaveComponentTasksUnchanged() {
        ComponentTaskDefinition topLevelComponentTask = componentTask("respond", "apiPlatform/v1/responseToAPIRequest")
            .parameters(Map.of("response", Map.of("statusCode", 200)));
        ComponentTaskDefinition nestedComponentTask = componentTask("log", "logger/v1/info");

        WorkflowDefinition workflowDefinition = workflow("my-workflow")
            .tasks(
                task("fetch"),
                parallel("enrich").tasks(task("customer"), nestedComponentTask),
                topLevelComponentTask);

        List<WorkflowDefinition> workflowDefinitions = IntegrationImplicitConnections.addTo(
            List.of(workflowDefinition), "test-component", 1);

        WorkflowDefinition implicitWorkflowDefinition = workflowDefinitions.getFirst();

        List<? extends WorkflowTaskDefinition> tasks = implicitWorkflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition performTask = (TaskDefinition) tasks.getFirst();

        assertThat(performTask.getConnections()).hasValueSatisfying(connections -> assertThat(connections).hasSize(1));

        CompositeTaskDefinition parallelTask = (CompositeTaskDefinition) tasks.get(1);

        List<? extends WorkflowTaskDefinition> nestedTasks = parallelTask.getTasks();

        TaskDefinition nestedPerformTask = (TaskDefinition) nestedTasks.getFirst();

        assertThat(nestedPerformTask.getConnections())
            .hasValueSatisfying(connections -> assertThat(connections).hasSize(1));
        assertThat(nestedTasks.get(1)).isSameAs(nestedComponentTask);
        assertThat(tasks.get(2)).isSameAs(topLevelComponentTask);
    }
}
