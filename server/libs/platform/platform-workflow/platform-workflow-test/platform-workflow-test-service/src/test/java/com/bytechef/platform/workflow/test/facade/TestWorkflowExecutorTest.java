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

package com.bytechef.platform.workflow.test.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.workflow.test.dto.WorkflowTestExecutionDTO;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class TestWorkflowExecutorTest {

    private static final String WORKFLOW_ID = "wf-1";
    private static final long ENVIRONMENT_ID = 10L;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @Mock
    private JobSyncExecutor jobSyncExecutor;

    @Mock
    private Workflow workflow;

    private TestWorkflowExecutorImpl testWorkflowExecutor;

    @BeforeEach
    void beforeEach() {
        testWorkflowExecutor = new TestWorkflowExecutorImpl(
            componentDefinitionService, mock(com.bytechef.atlas.execution.service.ContextService.class),
            mock(com.bytechef.evaluator.Evaluator.class), jobSyncExecutor,
            mock(com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService.class),
            mock(com.bytechef.atlas.execution.service.TaskExecutionService.class),
            mock(com.bytechef.atlas.file.storage.TaskFileStorage.class), workflowService, workflowNodeOutputFacade,
            workflowTestConfigurationService);
    }

    @Test
    void executeNoTriggersMergesInputsAndExecutesJob() {
        // Given a workflow without triggers
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);
        when(workflow.getExtensions(anyString(), any(), anyList())).thenReturn(Collections.emptyList());

        // And test configuration with inputs and one connection mapping
        Map<String, Object> cfgInputs = Map.of("cfgKey", "cfgVal");

        List<WorkflowTestConfigurationConnection> connections = List.of(
            new WorkflowTestConfigurationConnection(42L, "connKey", "nodeA"));

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration(
            ENVIRONMENT_ID, cfgInputs, WORKFLOW_ID, connections);

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(WORKFLOW_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(workflowTestConfiguration));

        // And a job is executed
        Job job = mock(Job.class);

        when(jobSyncExecutor.startJob(any(JobParametersDTO.class))).thenReturn(1L);
        when(jobSyncExecutor.awaitJob(anyLong(), any(Boolean.class))).thenReturn(job);
        when(job.getId()).thenReturn(1L);

        // When
        Map<String, Object> inputs = new HashMap<>();

        inputs.put("inKey", "inVal");

        WorkflowTestExecutionDTO result = testWorkflowExecutor.execute(WORKFLOW_ID, inputs, ENVIRONMENT_ID);

        // Then the job was executed with merged inputs and connection metadata
        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(jobSyncExecutor).startJob(captor.capture());

        JobParametersDTO jobParametersDTO = captor.getValue();

        assertThat(jobParametersDTO.getWorkflowId()).isEqualTo(WORKFLOW_ID);
        assertThat(jobParametersDTO.getInputs()).containsEntry("inKey", "inVal")
            .containsEntry("cfgKey", "cfgVal");

        Map<String, Object> metadata = jobParametersDTO.getMetadata();

        Object connectionsMeta = metadata.get(MetadataConstants.CONNECTION_IDS);

        assertThat(connectionsMeta).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Long>> metaMap = (Map<String, Map<String, Long>>) connectionsMeta;

        assertThat(metaMap).containsKey("nodeA");
        assertThat(metaMap.get("nodeA")).containsEntry("connKey", 42L);
        assertThat(result).isNotNull();
        assertThat(result.triggerExecution()).isNull();
    }

    @Test
    void executeWithTriggerEmptyInputsBuildsTriggerDTOAndFlattensSampleForNonBatch() {
        // Given a workflow with one trigger
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

        WorkflowTrigger trigger = mock(WorkflowTrigger.class);

        when(trigger.getName()).thenReturn("myTrigger");
        when(trigger.getType()).thenReturn("compX/v1/start");
        when(workflow.getExtensions(anyString(), any(), anyList())).thenReturn(List.of(trigger));

        // And a sample output list (non-batch trigger should flatten to the first element)
        List<Map<String, Object>> sampleOutput = List.of(Map.of("x", 1), Map.of("y", 2));

        WorkflowNodeOutputDTO workflowNodeOutputDTO = new WorkflowNodeOutputDTO(
            null, null, new OutputResponse(null, sampleOutput, null), null, false, null, "myTrigger");

        when(workflowNodeOutputFacade.getWorkflowNodeOutput(WORKFLOW_ID, "myTrigger", ENVIRONMENT_ID))
            .thenReturn(workflowNodeOutputDTO);

        // And component with the corresponding trigger definition (non-batch)
        ComponentDefinition component = mock(ComponentDefinition.class);
        TriggerDefinition triggerDef = mock(TriggerDefinition.class);

        when(triggerDef.getName()).thenReturn("start");
        when(triggerDef.isBatch()).thenReturn(false);
        when(component.getTriggers()).thenReturn(List.of(triggerDef));
        when(component.getTitle()).thenReturn("Comp Title");
        when(component.getIcon()).thenReturn("icon");

        WorkflowNodeType type = WorkflowNodeType.ofType("compX/v1/start");

        when(componentDefinitionService.getComponentDefinition(type.name(), type.version())).thenReturn(component);

        // And test configuration with some default inputs
        Map<String, Object> cfgInputs = Map.of("foo", "bar");

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration(
            ENVIRONMENT_ID, cfgInputs, WORKFLOW_ID, List.of());

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(WORKFLOW_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(workflowTestConfiguration));

        // And a job execution result
        Job job = mock(Job.class);

        when(jobSyncExecutor.startJob(any(JobParametersDTO.class))).thenReturn(1L);
        when(jobSyncExecutor.awaitJob(anyLong(), any(Boolean.class))).thenReturn(job);
        when(job.getId()).thenReturn(1L);

        // When inputs are empty, facade should build inputs from trigger sample
        WorkflowTestExecutionDTO result = testWorkflowExecutor.execute(WORKFLOW_ID, Map.of(), ENVIRONMENT_ID);

        // Then JobParameters contain the flattened sample under trigger name and cfg inputs
        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(jobSyncExecutor).startJob(captor.capture());

        JobParametersDTO jobParametersDTO = captor.getValue();

        Map<String, Object> inputs = jobParametersDTO.getInputs();

        assertThat(inputs).containsEntry("foo", "bar");
        assertThat(inputs).containsKey("myTrigger");
        assertThat(inputs.get("myTrigger")).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) inputs.get("myTrigger")).get("x")).isEqualTo(1);
        assertThat(result.triggerExecution()).isNotNull();
    }

    @Test
    void testStartMergesInputsAndReturnsJobId() {
        // Given a workflow without triggers and test config inputs
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);
        when(workflow.getExtensions(anyString(), any(), anyList())).thenReturn(Collections.emptyList());

        Map<String, Object> cfgInputs = Map.of("cfg", 1);

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration(
            ENVIRONMENT_ID, cfgInputs, WORKFLOW_ID, List.of());

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(WORKFLOW_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(workflowTestConfiguration));

        when(jobSyncExecutor.startJob(any(JobParametersDTO.class))).thenReturn(99L);

        long jobId = testWorkflowExecutor.start(WORKFLOW_ID, Map.of("in", 2), ENVIRONMENT_ID);

        assertThat(jobId).isEqualTo(99L);

        ArgumentCaptor<JobParametersDTO> captor = ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(jobSyncExecutor).startJob(captor.capture());

        JobParametersDTO params = captor.getValue();

        Map<String, Object> inputs = params.getInputs();

        assertThat(inputs.get("in")).isEqualTo(2);
        assertThat(inputs.get("cfg")).isEqualTo(1);
    }

    @Test
    void testAwaitExecutionReturnsJobAndNullTrigger() {
        Job job = mock(Job.class);

        when(jobSyncExecutor.awaitJob(anyLong(), any(Boolean.class))).thenReturn(job);
        when(job.getId()).thenReturn(123L);

        WorkflowTestExecutionDTO res = testWorkflowExecutor.awaitExecution(123L);

        assertThat(res.job()).isNotNull();
        assertThat(res.triggerExecution()).isNull();
    }

    @Test
    void testStopDelegates() {
        testWorkflowExecutor.stop(555L);
        verify(jobSyncExecutor).stopJob(555L);
    }

    @Test
    void testListenerRegistrationMethodsDelegate() {
        AutoCloseable jc = () -> {};
        AutoCloseable tc = () -> {};
        AutoCloseable ec = () -> {};

        when(jobSyncExecutor.addJobStatusListener(anyLong(), any())).thenReturn(jc);
        when(jobSyncExecutor.addTaskStartedListener(anyLong(), any())).thenReturn(tc);
        when(jobSyncExecutor.addTaskExecutionCompleteListener(anyLong(), any())).thenReturn(tc);
        when(jobSyncExecutor.addErrorListener(anyLong(), any())).thenReturn(ec);

        assertThat(testWorkflowExecutor.addJobStatusListener(1L, event -> {})).isSameAs(jc);
        assertThat(testWorkflowExecutor.addTaskStartedListener(1L, event -> {})).isSameAs(tc);
        assertThat(testWorkflowExecutor.addTaskExecutionCompleteListener(1L, event -> {})).isSameAs(tc);
        assertThat(testWorkflowExecutor.addErrorListener(1L, event -> {})).isSameAs(ec);
    }
}
