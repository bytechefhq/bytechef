/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
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
class AtlasExecutionProtoMapperTest {

    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper = new AtlasExecutionProtoMapper(
        JsonMapper.builder()
            .build());

    @Test
    void testJobRoundTrip() {
        Job job = job(7L);

        Job readJob = atlasExecutionProtoMapper.toJob(atlasExecutionProtoMapper.toProto(job));

        assertThat(readJob).usingRecursiveComparison()
            .isEqualTo(job);
    }

    @Test
    void testJobKeepsNumberTypes() {
        Job readJob = atlasExecutionProtoMapper.toJob(atlasExecutionProtoMapper.toProto(job(7L)));

        assertThat(readJob.getInputs()
            .get("id")).isEqualTo(9_007_199_254_740_993L);
        assertThat(readJob.getInputs()
            .get("ratio")).isEqualTo(5.0);
    }

    @Test
    void testJobParametersRoundTrip() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO(
            "workflow-1", 3L, Map.of("id", 9_007_199_254_740_993L), "Label", 4, List.of(webhook()),
            Map.of("source", "test"));

        assertThat(atlasExecutionProtoMapper.toJobParametersDTO(atlasExecutionProtoMapper.toProto(jobParametersDTO)))
            .usingRecursiveComparison()
            .isEqualTo(jobParametersDTO);
    }

    @Test
    void testFileEntryRoundTrip() {
        FileEntry fileEntry = new FileEntry("output", "json", "application/json", "base64:///abc");

        assertThat(atlasExecutionProtoMapper.toFileEntry(atlasExecutionProtoMapper.toProto(fileEntry)))
            .usingRecursiveComparison()
            .isEqualTo(fileEntry);
    }

    static Job job(long id) {
        Job job = new Job();

        job.setCurrentTask(2);
        job.setEndDate(Instant.parse("2026-10-26T12:00:00Z"));
        job.setError(new ExecutionError("Bad input", List.of("line 1")));
        job.setId(id);
        job.setInputs(Map.of("id", 9_007_199_254_740_993L, "ratio", 5.0));
        job.setLabel("Live Workflow");
        job.setMetadata(Map.of("source", "test"));
        job.setOutputs(new FileEntry("outputs.json", "base64:///abc"));
        job.setParentTaskExecutionId(3L);
        job.setPriority(4);
        job.setStartDate(Instant.parse("2026-10-26T11:59:00Z"));
        job.setStatus(Job.Status.COMPLETED);
        job.setWebhooks(List.of(webhook()));
        job.setWorkflowId("workflow-1");

        ReflectionTestUtils.setField(job, "createdBy", "admin@localhost.com");
        ReflectionTestUtils.setField(job, "createdDate", Instant.parse("2026-10-26T10:00:00Z"));
        ReflectionTestUtils.setField(job, "lastModifiedBy", "user@localhost.com");
        ReflectionTestUtils.setField(job, "lastModifiedDate", Instant.parse("2026-10-26T11:00:00Z"));
        ReflectionTestUtils.setField(job, "version", 5);

        return job;
    }

    private static Job.Webhook webhook() {
        return new Job.Webhook("JOB_STATUS", "https://example.com/hook", new Job.Retry(1000, 30000, 5, 2));
    }
}
