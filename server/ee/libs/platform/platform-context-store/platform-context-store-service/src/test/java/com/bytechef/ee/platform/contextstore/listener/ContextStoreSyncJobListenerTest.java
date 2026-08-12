/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.ee.platform.contextstore.service.ContextStoreRecordIndexService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreRecordService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;

/**
 * Unit tests for {@link ContextStoreSyncJobListener}. Verifies the FULL_REPLACE sweep purges the sidecar index rows of
 * tombstoned records (best-effort — a purge failure must not block the READY status flip) and that PARTIAL mode skips
 * both the tombstone sweep and the purge.
 *
 * @author Ivica Cardic
 * @version ee
 */
class ContextStoreSyncJobListenerTest {

    private static final Long SOURCE_ID = 100L;
    private static final long JOB_EXECUTION_ID = 999L;

    private ContextStoreRecordIndexService contextStoreRecordIndexService;
    private ContextStoreRecordService contextStoreRecordService;
    private ContextStoreSourceService contextStoreSourceService;
    private ContextStoreSyncJobListener listener;

    @BeforeEach
    void setUp() {
        contextStoreRecordIndexService = mock(ContextStoreRecordIndexService.class);
        contextStoreRecordService = mock(ContextStoreRecordService.class);
        contextStoreSourceService = mock(ContextStoreSourceService.class);

        listener = new ContextStoreSyncJobListener(
            contextStoreRecordIndexService, contextStoreRecordService, contextStoreSourceService);
    }

    @Test
    void testAfterJobOnCompletedFullReplacePurgesTombstonedIndexRows() {
        JobExecution jobExecution = newJobExecution(
            BatchStatus.COMPLETED, jobParametersWithDestination(newContextStoreDestination("FULL_REPLACE")));

        jobExecution.addStepExecution(newStepExecution(jobExecution, List.of("1", "2")));

        listener.afterJob(jobExecution);

        verify(contextStoreRecordService, times(1)).tombstoneUnseen(eq(SOURCE_ID), anyCollection(), any());
        verify(contextStoreRecordIndexService, times(1)).deleteAllForTombstonedRecords(SOURCE_ID);
        verify(contextStoreSourceService, times(1))
            .updateStatus(eq(SOURCE_ID), eq(ContextStoreSourceStatus.READY), any(), eq(JOB_EXECUTION_ID));
    }

    @Test
    void testAfterJobOnCompletedFullReplacePurgeFailureStillFlipsToReady() {
        when(contextStoreRecordIndexService.deleteAllForTombstonedRecords(SOURCE_ID))
            .thenThrow(new IllegalStateException("purge failed"));

        JobExecution jobExecution = newJobExecution(
            BatchStatus.COMPLETED, jobParametersWithDestination(newContextStoreDestination("FULL_REPLACE")));

        jobExecution.addStepExecution(newStepExecution(jobExecution, List.of("1")));

        listener.afterJob(jobExecution);

        verify(contextStoreSourceService, times(1))
            .updateStatus(eq(SOURCE_ID), eq(ContextStoreSourceStatus.READY), any(), eq(JOB_EXECUTION_ID));
    }

    @Test
    void testAfterJobOnCompletedPartialSkipsTombstoneSweepAndIndexPurge() {
        JobExecution jobExecution = newJobExecution(
            BatchStatus.COMPLETED, jobParametersWithDestination(newContextStoreDestination("PARTIAL")));

        listener.afterJob(jobExecution);

        verify(contextStoreRecordService, never()).tombstoneUnseen(anyLong(), anyCollection(), any());
        verify(contextStoreRecordIndexService, never()).deleteAllForTombstonedRecords(anyLong());
        verify(contextStoreSourceService, times(1))
            .updateLastSyncMetadata(eq(SOURCE_ID), any(), eq(JOB_EXECUTION_ID));
    }

    private static Map<String, Object> newContextStoreDestination(String mode) {
        Map<String, Object> destination = new HashMap<>();

        destination.put("componentName", "contextStore");
        destination.put("clusterElementName", "writeToReplica");

        Map<String, Object> inputParameters = new HashMap<>();

        inputParameters.put("sourceId", SOURCE_ID);
        inputParameters.put("mode", mode);

        destination.put("inputParameters", inputParameters);

        return destination;
    }

    private static JobParameters jobParametersWithDestination(Map<String, Object> destination) {
        Set<JobParameter<?>> parameters = new HashSet<>();

        parameters.add(new JobParameter<>("DESTINATION", destination, Map.class));

        return new JobParameters(parameters);
    }

    private static JobExecution newJobExecution(BatchStatus status, JobParameters jobParameters) {
        JobExecution jobExecution = new JobExecution(
            JOB_EXECUTION_ID, new JobInstance(1L, "dataStreamJob"), jobParameters);

        jobExecution.setStatus(status);

        return jobExecution;
    }

    private static StepExecution newStepExecution(JobExecution jobExecution, List<String> seenIds) {
        StepExecution stepExecution = new StepExecution("step1", jobExecution);

        ExecutionContext executionContext = new ExecutionContext();

        executionContext.put(ContextStoreExecutionContextKeys.SEEN_IDS_KEY, new ArrayList<>(seenIds));

        stepExecution.setExecutionContext(executionContext);

        return stepExecution;
    }
}
