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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SuppressWarnings("unchecked")
class ErrorWorkflowPayloadFactoryTest {

    @Test
    void testPayloadFieldNamesArePinned() {
        Job job = new Job();

        job.setId(11L);
        job.setStatus(Job.Status.FAILED);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of("name", "task1", "type", "logger/v1/info")))
            .build();

        taskExecution.setError(new ExecutionError("boom", List.of("frame-1")));

        Map<String, Object> payload = new ErrorWorkflowPayloadFactory("https://app.example.com")
            .build(job, taskExecution, context());

        Map<String, Object> execution = (Map<String, Object>) payload.get("execution");
        Map<String, Object> error = (Map<String, Object>) execution.get("error");
        Map<String, Object> workflow = (Map<String, Object>) payload.get("workflow");

        Assertions.assertEquals(
            Set.of("jobId", "url", "error", "lastTaskExecuted", "autoRecoveryAttempts"), execution.keySet());
        Assertions.assertEquals(Set.of("message", "stackTrace"), error.keySet());
        Assertions.assertEquals(
            Set.of("projectId", "projectWorkflowId", "workflowId", "label"), workflow.keySet());
        Assertions.assertEquals(Set.of("execution", "workflow", "environment"), payload.keySet());
        Assertions.assertEquals("boom", error.get("message"));
        Assertions.assertEquals("https://app.example.com/automation/executions/11", execution.get("url"));
    }

    @Test
    void testLastTaskExecutedIsNullWhenNoTaskRan() {
        Job job = new Job();

        job.setId(11L);

        Map<String, Object> payload = new ErrorWorkflowPayloadFactory("https://app.example.com")
            .build(job, null, context());

        Map<String, Object> execution = (Map<String, Object>) payload.get("execution");

        Assertions.assertNull(execution.get("lastTaskExecuted"));
    }

    @Test
    void testPayloadCarriesNoTaskInputsOrOutputs() {
        Job job = new Job();

        job.setId(11L);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(
                Map.of(
                    "name", "task1", "type", "logger/v1/info", "parameters",
                    Map.of("apiKey", "secret-input"))))
            .build();

        taskExecution.setOutput(new FileEntry("secret-output", "file:/tmp/secret-output"));

        String rendered = new ErrorWorkflowPayloadFactory("https://app.example.com")
            .build(job, taskExecution, context())
            .toString();

        Assertions.assertFalse(rendered.contains("secret-output"));
        Assertions.assertFalse(rendered.contains("secret-input"));
    }

    @Test
    void testPayloadHasDefaultErrorMessageWhenNoErrorIsSet() {
        Job job = new Job();

        job.setId(11L);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of("name", "task1", "type", "logger/v1/info")))
            .build();

        Map<String, Object> payload = new ErrorWorkflowPayloadFactory("https://app.example.com")
            .build(job, taskExecution, context());

        Map<String, Object> execution = (Map<String, Object>) payload.get("execution");
        Map<String, Object> error = (Map<String, Object>) execution.get("error");

        Assertions.assertEquals("Workflow run failed", error.get("message"));
        Assertions.assertNull(error.get("stackTrace"));
    }

    @Test
    void testUrlIsNullWhenPublicUrlIsNull() {
        Job job = new Job();

        job.setId(11L);

        Map<String, Object> payload = new ErrorWorkflowPayloadFactory(null)
            .build(job, null, context());

        Map<String, Object> execution = (Map<String, Object>) payload.get("execution");

        // The "url" key must stay present -- it's a pinned part of the payload's public contract -- but its value
        // must be null, never the literal string "null" that plain concatenation would have produced.
        Assertions.assertTrue(execution.containsKey("url"));
        Assertions.assertNull(execution.get("url"));
    }

    @Test
    void testUrlIsNullWhenPublicUrlIsBlank() {
        Job job = new Job();

        job.setId(11L);

        Map<String, Object> payload = new ErrorWorkflowPayloadFactory("   ")
            .build(job, null, context());

        Map<String, Object> execution = (Map<String, Object>) payload.get("execution");

        Assertions.assertTrue(execution.containsKey("url"));
        Assertions.assertNull(execution.get("url"));
    }

    private static ErrorWorkflowPayloadFactory.ErrorWorkflowContext context() {
        return new ErrorWorkflowPayloadFactory.ErrorWorkflowContext(
            1L, 2L, "wf-1", "My Workflow", "PRODUCTION");
    }
}
