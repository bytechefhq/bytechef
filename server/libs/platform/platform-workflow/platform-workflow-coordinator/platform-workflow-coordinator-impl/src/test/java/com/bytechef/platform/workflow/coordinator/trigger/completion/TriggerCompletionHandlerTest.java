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

package com.bytechef.platform.workflow.coordinator.trigger.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.trigger.jobparameter.TriggerJobParameterContributor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TriggerCompletionHandler}, split into two groups:
 *
 * <ol>
 * <li>{@code handle(...)} dispatch paths — how many jobs the handler spawns, and what trigger output value each one
 * carries, across the null-output, non-batch-collection (fan-out), non-batch-scalar, batch-collection,
 * batch-empty-collection, and batch-scalar cases. The surrounding side-effects (trigger-status persistence, event
 * publishing) are out of scope here and exercised by the end-to-end IntTest; the {@code workflowService} lookup in the
 * metadata-enrichment path is deliberately stubbed to fail so it falls back to the un-enriched metadata.</li>
 * <li>{@link TriggerCompletionHandler#assembleJobParameters} merge — extracted into a package-private static helper so
 * it can be unit-tested as a pure function of its inputs. Five scenarios matter: no source contributes (empty result),
 * static trigger block only, contributor only, both merged (later layer wins on collision), and a misbehaving
 * contributor that throws (skipped without blocking the dispatch).</li>
 * </ol>
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    ObjectMapperSetupExtension.class, MockitoExtension.class
})
class TriggerCompletionHandlerTest {

    private static final String TRIGGER_NAME = "scheduledIncrementalSync";
    private static final WorkflowExecutionId WORKFLOW_EXECUTION_ID =
        WorkflowExecutionId.of(PlatformType.AUTOMATION, 1L, "workflow-uuid", TRIGGER_NAME);

    @Mock
    private FileEntry fileEntry;

    @Mock
    private JobPrincipalAccessor jobPrincipalAccessor;

    @Mock
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Mock
    private PrincipalJobFacade principalJobFacade;

    @Mock
    private TriggerExecution triggerExecution;

    @Mock
    private TriggerExecutionService triggerExecutionService;

    @Mock
    private TriggerFileStorage triggerFileStorage;

    @Mock
    private TriggerStateService triggerStateService;

    @Mock
    private WorkflowService workflowService;

    private TriggerCompletionHandler triggerCompletionHandler;

    @BeforeEach
    void setUp() {
        triggerCompletionHandler = new TriggerCompletionHandler(
            jobPrincipalAccessorRegistry, principalJobFacade, List.of(), triggerExecutionService, triggerFileStorage,
            triggerStateService, workflowService);
    }

    @Test
    void testHandleNullOutputCreatesSingleJobWithEmptyMap() {
        stubCommon();
        stubName();
        when(triggerExecution.getOutput()).thenReturn(null);
        stubCreateJob();

        triggerCompletionHandler.handle(triggerExecution);

        List<JobParametersDTO> jobParametersDTOs = captureCreatedJobs(1);

        assertEquals(Map.of(), jobParametersDTOs.get(0)
            .getInputs()
            .get(TRIGGER_NAME));
    }

    @Test
    void testHandleNonBatchCollectionFansOutJobPerElement() {
        stubCommon();
        stubName();
        stubOutput(List.of("first", "second"));
        when(triggerExecution.isBatch()).thenReturn(false);
        stubCreateJob();

        triggerCompletionHandler.handle(triggerExecution);

        List<JobParametersDTO> jobParametersDTOs = captureCreatedJobs(2);

        assertEquals("first", triggerValue(jobParametersDTOs.get(0)));
        assertEquals("second", triggerValue(jobParametersDTOs.get(1)));
    }

    @Test
    void testHandleNonBatchNonCollectionCreatesSingleJob() {
        stubCommon();
        stubName();
        stubOutput("singleValue");
        when(triggerExecution.isBatch()).thenReturn(false);
        stubCreateJob();

        triggerCompletionHandler.handle(triggerExecution);

        List<JobParametersDTO> jobParametersDTOs = captureCreatedJobs(1);

        assertEquals("singleValue", triggerValue(jobParametersDTOs.get(0)));
        assertEquals("existingValue", jobParametersDTOs.get(0)
            .getInputs()
            .get("existingKey"));
    }

    @Test
    void testHandleBatchCollectionCreatesSingleJobWithWholeCollection() {
        List<String> output = List.of("first", "second");

        stubCommon();
        stubName();
        stubOutput(output);
        when(triggerExecution.isBatch()).thenReturn(true);
        stubCreateJob();

        triggerCompletionHandler.handle(triggerExecution);

        List<JobParametersDTO> jobParametersDTOs = captureCreatedJobs(1);

        assertEquals(output, triggerValue(jobParametersDTOs.get(0)));
    }

    @Test
    void testHandleBatchEmptyCollectionCreatesNoJob() {
        stubCommon();
        stubOutput(List.of());
        when(triggerExecution.isBatch()).thenReturn(true);

        triggerCompletionHandler.handle(triggerExecution);

        verify(principalJobFacade, never()).createJob(any(), eq(1L), eq(PlatformType.AUTOMATION));
    }

    @Test
    void testHandleBatchNonCollectionCreatesNoJob() {
        stubCommon();
        stubOutput("singleValue");
        when(triggerExecution.isBatch()).thenReturn(true);

        triggerCompletionHandler.handle(triggerExecution);

        verify(principalJobFacade, never()).createJob(any(), eq(1L), eq(PlatformType.AUTOMATION));
    }

    @Test
    void testAssembleJobParametersReturnsEmptyWhenNoSourceContributes() {
        Map<String, Object> result = TriggerCompletionHandler.assembleJobParameters(
            triggerWithoutJobParameters(), Map.of(), null, List.of());

        // Pre-17b shape: trigger has no jobParameters block, no contributors registered. Empty result tells the
        // caller to leave the principal metadataMap untouched — the spawned job carries no __jobParameters
        // entry and the dataStream reader falls back to its baked task parameters.
        assertThat(result).isEmpty();
    }

    @Test
    void testAssembleJobParametersFromTriggerStaticBlock() {
        Map<String, Object> result = TriggerCompletionHandler.assembleJobParameters(
            triggerWithJobParameters(Map.of("datastream.mode", "PARTIAL")), Map.of(), null, List.of());

        // The trigger JSON carries jobParameters.datastream.mode = PARTIAL (set by the dual-trigger workflow
        // generators from commit 4). Without any contributors, the static block is the entire result.
        assertThat(result).hasSize(1);
        assertThat(result).containsEntry("datastream.mode", "PARTIAL");
    }

    @Test
    void testAssembleJobParametersFromContributorOnly() {
        TriggerJobParameterContributor contributor = (metadata, executionId) -> Map.of(
            "datastream.since", 1700000000000L);

        Map<String, Object> result = TriggerCompletionHandler.assembleJobParameters(
            triggerWithoutJobParameters(), Map.of(), null, List.of(contributor));

        assertThat(result).hasSize(1);
        assertThat(result).containsEntry("datastream.since", 1700000000000L);
    }

    @Test
    void testAssembleJobParametersMergesTriggerBlockAndContributors() {
        // Contributor adds datastream.since to the trigger's static datastream.mode entry. Disjoint keys —
        // both end up in the final map.
        TriggerJobParameterContributor contributor = (metadata, executionId) -> Map.of(
            "datastream.since", 1700000000000L);

        Map<String, Object> result = TriggerCompletionHandler.assembleJobParameters(
            triggerWithJobParameters(Map.of("datastream.mode", "PARTIAL")), Map.of(), null, List.of(contributor));

        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("datastream.mode", "PARTIAL");
        assertThat(result).containsEntry("datastream.since", 1700000000000L);
    }

    @Test
    void testAssembleJobParametersContributorWinsOnKeyCollision() {
        // Adversarial contributor overrides the trigger's PARTIAL with FULL_REPLACE. Contributors layer on top
        // of the static block (later wins) — documented in JobMetadataKeys / TriggerJobParameterContributor
        // Javadoc. In practice contributors should pick disjoint key prefixes; this test pins the merge order.
        TriggerJobParameterContributor contributor = (metadata, executionId) -> Map.of(
            "datastream.mode", "FULL_REPLACE");

        Map<String, Object> result = TriggerCompletionHandler.assembleJobParameters(
            triggerWithJobParameters(Map.of("datastream.mode", "PARTIAL")), Map.of(), null, List.of(contributor));

        assertThat(result).hasSize(1);
        assertThat(result).containsEntry("datastream.mode", "FULL_REPLACE");
    }

    @Test
    void testAssembleJobParametersSkipsMisbehavingContributor() {
        TriggerJobParameterContributor throwingContributor = (metadata, executionId) -> {
            throw new RuntimeException("contributor exploded");
        };

        TriggerJobParameterContributor sensibleContributor = (metadata, executionId) -> Map.of(
            "datastream.since", 1700000000000L);

        Map<String, Object> result = TriggerCompletionHandler.assembleJobParameters(
            triggerWithJobParameters(Map.of("datastream.mode", "PARTIAL")), Map.of(), null,
            List.of(throwingContributor, sensibleContributor));

        // The throwing contributor is skipped (its exception is logged but swallowed); the sensible one still
        // contributes; the trigger's static block is preserved. The SPI contract says contributors MUST NOT
        // throw, but defending against the bad case keeps one misbehaving impl from breaking the platform.
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("datastream.mode", "PARTIAL");
        assertThat(result).containsEntry("datastream.since", 1700000000000L);
    }

    @Test
    void testAssembleJobParametersHandlesNullTrigger() {
        // The handler's enrichMetadata path passes null when WorkflowTrigger.of(triggerName, workflow) returns
        // null (no matching trigger by name). Helper must not NPE — only contributors run in that case.
        TriggerJobParameterContributor contributor = (metadata, executionId) -> Map.of(
            "datastream.since", 1700000000000L);

        Map<String, Object> result = TriggerCompletionHandler.assembleJobParameters(
            null, Map.of(), null, List.of(contributor));

        assertThat(result).hasSize(1);
        assertThat(result).containsEntry("datastream.since", 1700000000000L);
    }

    @Test
    void testAssembleJobParametersPassesWorkflowMetadataToContributors() {
        // The contributor receives the workflow's static metadata block — that's what carries
        // knowledgeBaseSourceId / contextStoreSourceId, the discriminator keys the real contributors look at.
        Map<String, Object> workflowMetadata = Map.of("knowledgeBaseSourceId", 42L);

        TriggerJobParameterContributor recording = new TriggerJobParameterContributor() {

            @Override
            public Map<String, ?> contribute(Map<String, ?> metadata, WorkflowExecutionId workflowExecutionId) {
                // Echo the metadata back so the test can verify it was threaded through.
                return Map.of("seen-id", metadata.get("knowledgeBaseSourceId"));
            }
        };

        Map<String, Object> result = TriggerCompletionHandler.assembleJobParameters(
            triggerWithoutJobParameters(), workflowMetadata, null, List.of(recording));

        assertThat(result).containsEntry("seen-id", 42L);
    }

    private List<JobParametersDTO> captureCreatedJobs(int expectedCount) {
        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(principalJobFacade, times(expectedCount))
            .createJob(captor.capture(), eq(1L), eq(PlatformType.AUTOMATION));

        return captor.getAllValues();
    }

    private void stubCommon() {
        when(triggerExecution.getId()).thenReturn(1L);
        when(triggerExecution.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        doReturn(Map.of("existingKey", "existingValue"))
            .when(jobPrincipalAccessor)
            .getInputMap(1L, "workflow-uuid");
        when(jobPrincipalAccessor.getWorkflowId(1L, "workflow-uuid"))
            .thenReturn("workflow-id");
        doReturn(Map.of("metaKey", "metaValue"))
            .when(jobPrincipalAccessor)
            .getMetadataMap(1L);

        // The metadata-enrichment path is orthogonal to the dispatch branching under test; forcing the workflow
        // lookup to fail exercises the handler's documented fallback (un-enriched metadata) without a Workflow fixture.
        when(workflowService.getWorkflow("workflow-id"))
            .thenThrow(new RuntimeException("workflow loading not under test"));
    }

    private void stubCreateJob() {
        when(principalJobFacade.createJob(any(), eq(1L), eq(PlatformType.AUTOMATION)))
            .thenReturn(5L);
    }

    private void stubName() {
        when(triggerExecution.getName()).thenReturn(TRIGGER_NAME);
    }

    private void stubOutput(Object output) {
        when(triggerExecution.getOutput()).thenReturn(fileEntry);
        when(triggerFileStorage.readTriggerExecutionOutput(fileEntry)).thenReturn(output);
    }

    /**
     * Constructs a {@link WorkflowTrigger} via its {@code Map}-source ctor — bypasses the workflow mapper that the
     * {@code Workflow} domain ctor depends on, which would otherwise require the platform's reserved-word registration
     * and full Spring boot-up.
     */
    private static WorkflowTrigger triggerWithJobParameters(Map<String, Object> jobParameters) {
        return new WorkflowTrigger(Map.of(
            "name", TRIGGER_NAME,
            "type", "schedule/v1/cron",
            "jobParameters", jobParameters));
    }

    private static WorkflowTrigger triggerWithoutJobParameters() {
        return new WorkflowTrigger(Map.of(
            "name", TRIGGER_NAME,
            "type", "schedule/v1/cron"));
    }

    private static Object triggerValue(JobParametersDTO jobParametersDTO) {
        return jobParametersDTO.getInputs()
            .get(TRIGGER_NAME);
    }
}
