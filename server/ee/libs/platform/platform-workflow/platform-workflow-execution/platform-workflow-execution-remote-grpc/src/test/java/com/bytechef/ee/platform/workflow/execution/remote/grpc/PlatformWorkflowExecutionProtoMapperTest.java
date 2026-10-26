/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class PlatformWorkflowExecutionProtoMapperTest {

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper =
        new PlatformWorkflowExecutionProtoMapper(
            JsonMapper.builder()
                .build());

    @Test
    void testTriggerExecutionRoundTrip() {
        TriggerExecution triggerExecution = triggerExecution();

        TriggerExecution readTriggerExecution = platformWorkflowExecutionProtoMapper.toTriggerExecution(
            platformWorkflowExecutionProtoMapper.toProto(triggerExecution));

        assertThat(readTriggerExecution).usingRecursiveComparison()
            .ignoringFields("workflowExecutionId", "workflowTrigger")
            .isEqualTo(triggerExecution);
        assertThat(String.valueOf(readTriggerExecution.getWorkflowExecutionId()))
            .isEqualTo(String.valueOf(triggerExecution.getWorkflowExecutionId()));
        assertThat(readTriggerExecution.getWorkflowTrigger()
            .toMap()).isEqualTo(triggerExecution.getWorkflowTrigger()
                .toMap());
        assertThat(readTriggerExecution.getState()).isEqualTo(9_007_199_254_740_993L);
    }

    static TriggerExecution triggerExecution() {
        TriggerExecution triggerExecution = TriggerExecution.builder()
            .endDate(Instant.parse("2026-10-26T12:00:00Z"))
            .error(new ExecutionError("Bad input", List.of("line 1")))
            .id(5L)
            .maxRetries(3)
            .metadata(Map.of("source", "test"))
            .output(new FileEntry("output.json", "base64:///abc"))
            .priority(4)
            .retryAttempts(1)
            .retryDelay("2s")
            .retryDelayFactor(2)
            .startDate(Instant.parse("2026-10-26T11:59:00Z"))
            .status(TriggerExecution.Status.COMPLETED)
            .workflowExecutionId(
                WorkflowExecutionId.of(PlatformType.AUTOMATION, 1051L, "workflow-uuid", "trigger_1"))
            .workflowTrigger(
                new WorkflowTrigger(
                    Map.of(
                        "name", "trigger_1", "type", "webhook/v1/awaitWorkflowAndRespond",
                        "parameters", Map.of("count", 5))))
            .build();

        triggerExecution.setBatch(true);
        triggerExecution.setExecutionTime(250L);
        triggerExecution.setJobIds(List.of(7L, 8L));
        triggerExecution.setState(9_007_199_254_740_993L);

        ReflectionTestUtils.setField(triggerExecution, "createdBy", "admin@localhost.com");
        ReflectionTestUtils.setField(triggerExecution, "createdDate", Instant.parse("2026-10-26T10:00:00Z"));
        ReflectionTestUtils.setField(triggerExecution, "lastModifiedBy", "user@localhost.com");
        ReflectionTestUtils.setField(triggerExecution, "lastModifiedDate", Instant.parse("2026-10-26T11:00:00Z"));

        return triggerExecution;
    }
}
