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

package com.bytechef.platform.knowledgebase.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseSourceSyncJobListenerTest {

    private static final Long SOURCE_ID = 200L;
    private static final long JOB_EXECUTION_ID = 999L;
    private static final long JOB_EXECUTION_ID_BOXED = JOB_EXECUTION_ID;
    private static final String SEEN_RECORD_IDS_KEY = "knowledgeBaseSource.seenRecordIds";

    private KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private KnowledgeBaseSourceService knowledgeBaseSourceService;
    private KnowledgeBaseSourceSyncJobListener listener;

    @BeforeEach
    void setUp() {
        knowledgeBaseDocumentFacade = mock(KnowledgeBaseDocumentFacade.class);
        knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);
        knowledgeBaseSourceService = mock(KnowledgeBaseSourceService.class);

        listener = new KnowledgeBaseSourceSyncJobListener(
            knowledgeBaseDocumentFacade, knowledgeBaseDocumentService, knowledgeBaseSourceService);
    }

    @Test
    void testAfterJobOnCompletedFullReplaceTombstonesUnseenAndFlipsToReady() {
        Map<String, Object> destination = newKnowledgeBaseDestination("FULL_REPLACE");

        JobExecution jobExecution = newJobExecution(BatchStatus.COMPLETED, jobParametersWithDestination(destination));

        StepExecution stepExecution = newStepExecution(jobExecution, List.of("rec1", "rec2", "rec3"));

        jobExecution.addStepExecution(stepExecution);

        listener.afterJob(jobExecution);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> seenIdsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Instant> deletedAtCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(knowledgeBaseDocumentService, times(1))
            .tombstoneUnseen(eq(SOURCE_ID), seenIdsCaptor.capture(), deletedAtCaptor.capture());

        assertThat(seenIdsCaptor.getValue()).containsExactlyInAnyOrder("rec1", "rec2", "rec3");
        assertThat(deletedAtCaptor.getValue()).isNotNull();

        ArgumentCaptor<Instant> statusInstantCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(knowledgeBaseSourceService, times(1))
            .updateStatus(eq(SOURCE_ID), eq(KnowledgeBaseSourceStatus.READY), statusInstantCaptor.capture(),
                eq(JOB_EXECUTION_ID_BOXED));

        assertThat(statusInstantCaptor.getValue()).isEqualTo(deletedAtCaptor.getValue());

        verify(knowledgeBaseDocumentFacade, times(1)).sweepTombstonedDocumentChunks(SOURCE_ID);
        verify(knowledgeBaseSourceService, never()).updateLastSyncMetadata(anyLong(), any(), any());
    }

    @Test
    void testAfterJobOnCompletedFullReplaceSweepFailureStillFlipsToReady() {
        Map<String, Object> destination = newKnowledgeBaseDestination("FULL_REPLACE");

        JobExecution jobExecution = newJobExecution(BatchStatus.COMPLETED, jobParametersWithDestination(destination));

        jobExecution.addStepExecution(newStepExecution(jobExecution, List.of("rec1")));

        when(knowledgeBaseDocumentFacade.sweepTombstonedDocumentChunks(SOURCE_ID))
            .thenThrow(new RuntimeException("vector store unavailable"));

        listener.afterJob(jobExecution);

        verify(knowledgeBaseDocumentService, times(1)).tombstoneUnseen(eq(SOURCE_ID), any(), any(Instant.class));
        verify(knowledgeBaseSourceService, times(1))
            .updateStatus(eq(SOURCE_ID), eq(KnowledgeBaseSourceStatus.READY), any(Instant.class),
                eq(JOB_EXECUTION_ID_BOXED));
    }

    @Test
    void testAfterJobOnCompletedPartialSkipsTombstoneSweep() {
        Map<String, Object> destination = newKnowledgeBaseDestination("PARTIAL");

        JobExecution jobExecution = newJobExecution(BatchStatus.COMPLETED, jobParametersWithDestination(destination));

        StepExecution stepExecution = newStepExecution(jobExecution, List.of("rec1", "rec2"));

        jobExecution.addStepExecution(stepExecution);

        listener.afterJob(jobExecution);

        verify(knowledgeBaseDocumentService, never()).tombstoneUnseen(anyLong(), any(), any());
        verify(knowledgeBaseDocumentFacade, never()).sweepTombstonedDocumentChunks(anyLong());
        verify(knowledgeBaseSourceService, never())
            .updateStatus(anyLong(), any(KnowledgeBaseSourceStatus.class), any(), any());

        verify(knowledgeBaseSourceService, times(1))
            .updateLastSyncMetadata(eq(SOURCE_ID), any(Instant.class), eq(JOB_EXECUTION_ID_BOXED));
    }

    @Test
    void testAfterJobOnCompletedPartialPreservesBuildingPreviewStatus() {
        Map<String, Object> destination = newKnowledgeBaseDestination("PARTIAL");

        JobExecution jobExecution = newJobExecution(BatchStatus.COMPLETED, jobParametersWithDestination(destination));

        listener.afterJob(jobExecution);

        verify(knowledgeBaseSourceService, never())
            .updateStatus(anyLong(), any(KnowledgeBaseSourceStatus.class), any(), any());
        verify(knowledgeBaseDocumentService, never()).tombstoneUnseen(anyLong(), any(), any());
    }

    @Test
    void testAfterJobModeDefaultsToFullReplaceWhenAbsent() {
        Map<String, Object> destination = newKnowledgeBaseDestination(null);

        JobExecution jobExecution = newJobExecution(BatchStatus.COMPLETED, jobParametersWithDestination(destination));

        StepExecution stepExecution = newStepExecution(jobExecution, List.of("rec1"));

        jobExecution.addStepExecution(stepExecution);

        listener.afterJob(jobExecution);

        verify(knowledgeBaseDocumentService, times(1)).tombstoneUnseen(eq(SOURCE_ID), any(), any(Instant.class));
        verify(knowledgeBaseSourceService, times(1))
            .updateStatus(eq(SOURCE_ID), eq(KnowledgeBaseSourceStatus.READY), any(Instant.class),
                eq(JOB_EXECUTION_ID_BOXED));
    }

    @Test
    void testAfterJobOnFailedPreservesReadyStatus() {
        Map<String, Object> destination = newKnowledgeBaseDestination("FULL_REPLACE");

        JobExecution jobExecution = newJobExecution(BatchStatus.FAILED, jobParametersWithDestination(destination));

        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setStatus(KnowledgeBaseSourceStatus.READY);

        when(knowledgeBaseSourceService.fetch(SOURCE_ID)).thenReturn(Optional.of(source));

        listener.afterJob(jobExecution);

        verify(knowledgeBaseSourceService, never())
            .updateStatus(anyLong(), any(KnowledgeBaseSourceStatus.class), any(), any());
        verify(knowledgeBaseSourceService, times(1))
            .updateLastSyncMetadata(eq(SOURCE_ID), isNull(), eq(JOB_EXECUTION_ID_BOXED));
        verify(knowledgeBaseDocumentService, never()).tombstoneUnseen(anyLong(), any(), any());
        verify(knowledgeBaseDocumentFacade, never()).sweepTombstonedDocumentChunks(anyLong());
    }

    @Test
    void testAfterJobOnFailedDowngradesNonReadyToFailed() {
        Map<String, Object> destination = newKnowledgeBaseDestination("FULL_REPLACE");

        JobExecution jobExecution = newJobExecution(BatchStatus.FAILED, jobParametersWithDestination(destination));

        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setStatus(KnowledgeBaseSourceStatus.BUILDING_PREVIEW);

        when(knowledgeBaseSourceService.fetch(SOURCE_ID)).thenReturn(Optional.of(source));

        listener.afterJob(jobExecution);

        verify(knowledgeBaseDocumentService, never()).tombstoneUnseen(anyLong(), any(), any());
        verify(knowledgeBaseSourceService, times(1))
            .updateStatus(eq(SOURCE_ID), eq(KnowledgeBaseSourceStatus.FAILED), isNull(), eq(JOB_EXECUTION_ID_BOXED));
    }

    @Test
    void testAfterJobShortCircuitsForNonKnowledgeBaseDestination() {
        Map<String, Object> destination = new HashMap<>();

        destination.put("componentName", "contextStore");
        destination.put("clusterElementName", "writeToReplica");
        destination.put("inputParameters", Map.of("sourceId", SOURCE_ID));

        JobExecution jobExecution = newJobExecution(BatchStatus.COMPLETED, jobParametersWithDestination(destination));

        StepExecution stepExecution = newStepExecution(jobExecution, List.of("rec1"));

        jobExecution.addStepExecution(stepExecution);

        listener.afterJob(jobExecution);

        verify(knowledgeBaseDocumentService, never()).tombstoneUnseen(anyLong(), any(), any());
        verify(knowledgeBaseSourceService, never())
            .updateStatus(anyLong(), any(KnowledgeBaseSourceStatus.class), any(), any());
        verify(knowledgeBaseSourceService, never()).updateLastSyncMetadata(anyLong(), any(), any());
    }

    @Test
    void testAfterJobAggregatesSeenIdsFromMultipleSteps() {
        Map<String, Object> destination = newKnowledgeBaseDestination("FULL_REPLACE");

        JobExecution jobExecution = newJobExecution(BatchStatus.COMPLETED, jobParametersWithDestination(destination));

        jobExecution.addStepExecution(newStepExecution(jobExecution, List.of("rec1", "rec2")));
        jobExecution.addStepExecution(newStepExecution(jobExecution, List.of("rec2", "rec3")));

        listener.afterJob(jobExecution);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> seenIdsCaptor = ArgumentCaptor.forClass(Set.class);

        verify(knowledgeBaseDocumentService, times(1))
            .tombstoneUnseen(eq(SOURCE_ID), seenIdsCaptor.capture(), any(Instant.class));

        assertThat(new HashSet<>(seenIdsCaptor.getValue())).containsExactlyInAnyOrder("rec1", "rec2", "rec3");
    }

    @Test
    void testAfterJobMissingSourceLogsAndShortCircuits() {
        Map<String, Object> destination = newKnowledgeBaseDestination("FULL_REPLACE");

        JobExecution jobExecution = newJobExecution(BatchStatus.FAILED, jobParametersWithDestination(destination));

        when(knowledgeBaseSourceService.fetch(SOURCE_ID)).thenReturn(Optional.empty());

        listener.afterJob(jobExecution);

        verify(knowledgeBaseSourceService, never())
            .updateStatus(anyLong(), any(KnowledgeBaseSourceStatus.class), any(), any());
        verify(knowledgeBaseSourceService, never()).updateLastSyncMetadata(anyLong(), any(), any());
    }

    private static Map<String, Object> newKnowledgeBaseDestination(String mode) {
        Map<String, Object> destination = new HashMap<>();

        destination.put("componentName", "knowledgeBase");
        destination.put("clusterElementName", "writeAsDocument");

        Map<String, Object> inputParameters = new HashMap<>();

        inputParameters.put("sourceId", SOURCE_ID);

        if (mode != null) {
            inputParameters.put("mode", mode);
        }

        destination.put("inputParameters", inputParameters);

        return destination;
    }

    private static JobParameters jobParametersWithDestination(Map<String, Object> destination) {
        Set<JobParameter<?>> parameters = new HashSet<>();

        parameters.add(new JobParameter<>("DESTINATION", destination, Map.class));

        return new JobParameters(parameters);
    }

    private static JobExecution newJobExecution(BatchStatus status, JobParameters jobParameters) {
        JobInstance jobInstance = new JobInstance(1L, "dataStreamJob");

        JobExecution jobExecution = new JobExecution(JOB_EXECUTION_ID, jobInstance, jobParameters);

        jobExecution.setStatus(status);

        return jobExecution;
    }

    private static StepExecution newStepExecution(JobExecution jobExecution, List<String> seenRecordIds) {
        StepExecution stepExecution = new StepExecution("step1", jobExecution);

        ExecutionContext executionContext = new ExecutionContext();

        executionContext.put(SEEN_RECORD_IDS_KEY, new ArrayList<>(seenRecordIds));

        stepExecution.setExecutionContext(executionContext);

        return stepExecution;
    }
}
