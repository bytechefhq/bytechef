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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.workflow.test.dto.WorkflowTestExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
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
    void executeSyncNoTriggersMergesInputsAndExecutesJob() {
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
        when(jobSyncExecutor.awaitJob(anyLong(), any(Boolean.class), any())).thenReturn(job);
        when(job.getId()).thenReturn(1L);

        // When
        Map<String, Object> inputs = new HashMap<>();

        inputs.put("inKey", "inVal");

        WorkflowTestExecutionDTO result = testWorkflowExecutor.executeSync(WORKFLOW_ID, inputs, ENVIRONMENT_ID);

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
    void executeSyncWithTriggerEmptyInputsBuildsTriggerDTOAndFlattensSampleForNonBatch() {
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
        when(jobSyncExecutor.awaitJob(anyLong(), any(Boolean.class), any())).thenReturn(job);
        when(job.getId()).thenReturn(1L);

        // When inputs are empty, facade should build inputs from trigger sample
        WorkflowTestExecutionDTO result = testWorkflowExecutor.executeSync(WORKFLOW_ID, Map.of(), ENVIRONMENT_ID);

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
    void executeAsyncSuccessfulExecutionFlow() throws Exception {
        // Given a workflow without triggers
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);
        when(workflow.getExtensions(anyString(), any(), anyList())).thenReturn(Collections.emptyList());

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration(
            ENVIRONMENT_ID, Map.of(), WORKFLOW_ID, List.of());

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(WORKFLOW_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(workflowTestConfiguration));

        Job job = mock(Job.class);

        when(jobSyncExecutor.startJob(any(JobParametersDTO.class))).thenReturn(1L);
        when(jobSyncExecutor.awaitJob(anyLong(), any(Boolean.class), any())).thenReturn(job);
        when(job.getId()).thenReturn(1L);

        AutoCloseable mockHandle = mock(AutoCloseable.class);

        when(jobSyncExecutor.addJobStatusListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addTaskStartedListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addTaskExecutionCompleteListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addErrorListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addSseStreamBridge(anyLong(), any())).thenReturn(mockHandle);

        String[] afterStartKey = new String[1];
        String[] afterFutureKey = new String[1];
        String[] whenCompleteKey = new String[1];
        SseStreamBridge[] sseStreamBridge = new SseStreamBridge[1];

        // When
        testWorkflowExecutor.executeAsync(
            WORKFLOW_ID,
            Map.of("key", "value"),
            ENVIRONMENT_ID,
            key -> afterStartKey[0] = key,
            key -> {
                sseStreamBridge[0] = mock(SseStreamBridge.class);

                return sseStreamBridge[0];
            },
            (key, future) -> {
                afterFutureKey[0] = key;

                future.join();
            },
            key -> whenCompleteKey[0] = key);

        // Then verify callbacks were invoked with the same key
        assertThat(afterStartKey[0]).isNotNull()
            .isEqualTo(afterFutureKey[0])
            .isEqualTo(whenCompleteKey[0]);

        verify(jobSyncExecutor).startJob(any(JobParametersDTO.class));
        verify(sseStreamBridge[0], times(2)).onEvent(any(Map.class));
        verify(jobSyncExecutor).addJobStatusListener(anyLong(), any());
        verify(mockHandle, times(5)).close();
    }

    @Test
    @SuppressWarnings("rawtypes")
    @SuppressFBWarnings("RV")
    void executeAsyncGenericExceptionSendsErrorEvent() throws Exception {
        // Given a workflow without triggers
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);
        when(workflow.getExtensions(anyString(), any(), anyList())).thenReturn(Collections.emptyList());

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration(
            ENVIRONMENT_ID, Map.of(), WORKFLOW_ID, List.of());

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(WORKFLOW_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(workflowTestConfiguration));

        when(jobSyncExecutor.startJob(any(JobParametersDTO.class))).thenReturn(1L);

        when(jobSyncExecutor.awaitJob(anyLong(), any(Boolean.class), any()))
            .thenThrow(new RuntimeException("Execution failed"));

        AutoCloseable mockHandle = mock(AutoCloseable.class);

        when(jobSyncExecutor.addJobStatusListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addTaskStartedListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addTaskExecutionCompleteListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addErrorListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addSseStreamBridge(anyLong(), any())).thenReturn(mockHandle);

        SseStreamBridge mockSseStreamBridge = mock(SseStreamBridge.class);

        String[] whenCompleteKey = new String[1];
        CountDownLatch latch = new CountDownLatch(1);

        // When
        testWorkflowExecutor.executeAsync(
            WORKFLOW_ID,
            Map.of(),
            ENVIRONMENT_ID,
            key -> {},
            key -> mockSseStreamBridge,
            (key, future) -> {
                // Do not join - let it run asynchronously
            },
            key -> {
                whenCompleteKey[0] = key;
                latch.countDown();
            });

        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);

        // Then verify error event contains the exception message (start event + error event = 2)
        ArgumentCaptor<Map> eventCaptor = ArgumentCaptor.forClass(Map.class);

        verify(mockSseStreamBridge, times(2)).onEvent(eventCaptor.capture());

        List<Map> events = eventCaptor.getAllValues();

        Map errorEvent = events.stream()
            .filter(event -> "error".equals(event.get("event")))
            .findFirst()
            .orElse(null);

        assertThat(errorEvent).isNotNull();
        assertThat(errorEvent.get("payload")).asString()
            .contains("Execution failed");

        assertThat(whenCompleteKey[0]).isNotNull();

        verify(jobSyncExecutor, never()).stopJob(anyLong());
    }

    @Test
    void executeAsyncCallbackExecutionOrder() {
        // Given a workflow without triggers
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);
        when(workflow.getExtensions(anyString(), any(), anyList())).thenReturn(Collections.emptyList());

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration(
            ENVIRONMENT_ID, Map.of(), WORKFLOW_ID, List.of());

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(WORKFLOW_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(workflowTestConfiguration));

        Job job = mock(Job.class);

        when(jobSyncExecutor.startJob(any(JobParametersDTO.class))).thenReturn(1L);
        when(jobSyncExecutor.awaitJob(anyLong(), any(Boolean.class), any())).thenReturn(job);
        when(job.getId()).thenReturn(1L);

        AutoCloseable mockHandle = mock(AutoCloseable.class);

        when(jobSyncExecutor.addJobStatusListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addTaskStartedListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addTaskExecutionCompleteListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addErrorListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addSseStreamBridge(anyLong(), any())).thenReturn(mockHandle);

        List<String> callbackOrder = new java.util.ArrayList<>();

        // When
        testWorkflowExecutor.executeAsync(
            WORKFLOW_ID,
            Map.of(),
            ENVIRONMENT_ID,
            key -> callbackOrder.add("afterStart"),
            key -> {
                callbackOrder.add("sseStreamBridgeFactory");

                return mock(SseStreamBridge.class);
            },
            (key, future) -> {
                callbackOrder.add("afterFuture");

                future.join();

                callbackOrder.add("afterFutureComplete");
            },
            key -> callbackOrder.add("whenComplete"));

        // Then verify callback execution order
        // Note: whenComplete fires after afterFutureComplete because future.join() blocks until completion
        assertThat(callbackOrder).containsExactly(
            "afterStart",
            "sseStreamBridgeFactory",
            "afterFuture",
            "afterFutureComplete",
            "whenComplete");
    }

    @Test
    void executeAsyncListenerRegistrationAndUnregistration() throws Exception {
        // Given a workflow without triggers
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);
        when(workflow.getExtensions(anyString(), any(), anyList())).thenReturn(Collections.emptyList());

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration(
            ENVIRONMENT_ID, Map.of(), WORKFLOW_ID, List.of());

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(WORKFLOW_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(workflowTestConfiguration));

        Job job = mock(Job.class);

        when(jobSyncExecutor.startJob(any(JobParametersDTO.class))).thenReturn(1L);
        when(jobSyncExecutor.awaitJob(anyLong(), any(Boolean.class), any())).thenReturn(job);
        when(job.getId()).thenReturn(1L);

        AutoCloseable mockHandle = mock(AutoCloseable.class);

        when(jobSyncExecutor.addJobStatusListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addTaskStartedListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addTaskExecutionCompleteListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addErrorListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addSseStreamBridge(anyLong(), any())).thenReturn(mockHandle);

        // When
        testWorkflowExecutor.executeAsync(
            WORKFLOW_ID,
            Map.of(),
            ENVIRONMENT_ID,
            key -> {},
            key -> mock(SseStreamBridge.class),
            (key, future) -> future.join(),
            key -> {});

        verify(jobSyncExecutor).addJobStatusListener(anyLong(), any());
        verify(jobSyncExecutor).addTaskStartedListener(anyLong(), any());
        verify(jobSyncExecutor).addTaskExecutionCompleteListener(anyLong(), any());
        verify(jobSyncExecutor).addErrorListener(anyLong(), any());
        verify(jobSyncExecutor).addSseStreamBridge(anyLong(), any());
        verify(mockHandle, times(5)).close();
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void executeAsyncSendsStartAndResultEvents() throws Exception {
        // Given a workflow without triggers
        when(workflowService.getWorkflow(anyString())).thenReturn(workflow);
        when(workflow.getExtensions(anyString(), any(), anyList())).thenReturn(Collections.emptyList());

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration(
            ENVIRONMENT_ID, Map.of(), WORKFLOW_ID, List.of());

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(WORKFLOW_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(workflowTestConfiguration));

        Job job = mock(Job.class);

        when(jobSyncExecutor.startJob(any(JobParametersDTO.class))).thenReturn(1L);
        when(jobSyncExecutor.awaitJob(anyLong(), any(Boolean.class), any())).thenReturn(job);
        when(job.getId()).thenReturn(1L);

        AutoCloseable mockHandle = mock(AutoCloseable.class);

        when(jobSyncExecutor.addJobStatusListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addTaskStartedListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addTaskExecutionCompleteListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addErrorListener(anyLong(), any())).thenReturn(mockHandle);
        when(jobSyncExecutor.addSseStreamBridge(anyLong(), any())).thenReturn(mockHandle);

        SseStreamBridge mockSseStreamBridge = mock(SseStreamBridge.class);

        // When
        testWorkflowExecutor.executeAsync(
            WORKFLOW_ID,
            Map.of(),
            ENVIRONMENT_ID,
            key -> {},
            key -> mockSseStreamBridge,
            (key, future) -> future.join(),
            key -> {});

        // Then verify SSE events were sent in order
        ArgumentCaptor<Map> eventCaptor = ArgumentCaptor.forClass(Map.class);

        verify(mockSseStreamBridge, org.mockito.Mockito.atLeastOnce()).onEvent(eventCaptor.capture());

        List<Map> events = eventCaptor.getAllValues();

        Map startEvent = events.stream()
            .filter(event -> "start".equals(event.get("event")))
            .findFirst()
            .orElse(null);

        assertThat(startEvent).isNotNull();

        Object payload = startEvent.get("payload");

        assertThat(payload).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) payload).get("jobId")).isEqualTo("1");

        Map resultEvent = events.stream()
            .filter(event -> "result".equals(event.get("event")))
            .findFirst()
            .orElse(null);

        assertThat(resultEvent).isNotNull();
        assertThat(resultEvent.get("payload")).isInstanceOf(WorkflowTestExecutionDTO.class);
    }

    @Test
    void testStop() {
        testWorkflowExecutor.stop(555L);

        verify(jobSyncExecutor).stopJob(555L);
    }
}
