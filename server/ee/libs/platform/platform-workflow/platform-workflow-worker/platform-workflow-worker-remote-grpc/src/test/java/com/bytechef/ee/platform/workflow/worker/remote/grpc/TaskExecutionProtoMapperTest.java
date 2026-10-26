/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.worker.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TaskExecutionProtoMapperTest {

    private final TaskExecutionProtoMapper taskExecutionProtoMapper = new TaskExecutionProtoMapper(
        JsonMapper.builder()
            .build());

    @Test
    void testTaskExecutionRoundTrip() {
        TaskExecution taskExecution = taskExecution();

        assertThat(taskExecutionProtoMapper.toTaskExecution(taskExecutionProtoMapper.toProto(taskExecution)))
            .usingRecursiveComparison()
            .isEqualTo(taskExecution);
    }

    @Test
    void testSavedTaskExecutionRoundTrip() {
        TaskExecution taskExecution = taskExecution();

        ReflectionTestUtils.setField(taskExecution, "createdBy", "admin@localhost.com");
        ReflectionTestUtils.setField(taskExecution, "createdDate", Instant.parse("2026-10-26T09:59:00Z"));
        ReflectionTestUtils.setField(taskExecution, "lastModifiedBy", "user@localhost.com");
        ReflectionTestUtils.setField(taskExecution, "lastModifiedDate", Instant.parse("2026-10-26T10:00:01Z"));

        assertThat(taskExecutionProtoMapper.toTaskExecution(taskExecutionProtoMapper.toProto(taskExecution)))
            .usingRecursiveComparison()
            .isEqualTo(taskExecution);
    }

    @Test
    void testBareTaskExecutionRoundTrip() {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setWorkflowTask(new WorkflowTask(Map.of("name", "a", "type", "var/v1/set")));

        assertThat(taskExecutionProtoMapper.toTaskExecution(taskExecutionProtoMapper.toProto(taskExecution)))
            .usingRecursiveComparison()
            .isEqualTo(taskExecution);
    }

    static TaskExecution taskExecution() {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setEndDate(Instant.parse("2026-10-26T10:00:05Z"));
        taskExecution.setError(new ExecutionError("Bad input", List.of("line 1", "line 2")));
        taskExecution.setExecutionTime(5000);
        taskExecution.setId(1L);
        taskExecution.setJobId(2L);
        taskExecution.setMaxRetries(3);
        taskExecution.setMetadata(Map.of("tenant", "public"));
        taskExecution.setOutput(new FileEntry("output.json", "base64:///abc"));
        taskExecution.setParentId(4L);
        taskExecution.setPriority(5);
        taskExecution.setProgress(50);
        taskExecution.setRetryAttempts(1);
        taskExecution.setRetryDelay("1s");
        taskExecution.setRetryDelayFactor(2);
        taskExecution.setStartDate(Instant.parse("2026-10-26T10:00:00Z"));
        taskExecution.setStatus(TaskExecution.Status.STARTED);
        taskExecution.setTaskNumber(6);
        taskExecution.setWorkflowTask(
            new WorkflowTask(
                Map.of(
                    "name", "randomHelper_1", "type", "randomHelper/v1/randomFloat", "parameters",
                    Map.of("from", 1, "to", 5, "label", "x"))));

        return taskExecution;
    }
}
