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

package com.bytechef.task.dispatcher.callaiagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource.ResolvedAiAgent;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class CallAiAgentTaskDispatcherTest {

    private static final String AGENT_UUID = "agent-uuid-1";
    private static final String AGENT_WORKFLOW_UUID = "agent-workflow-uuid";
    private static final String RESOLVED_WORKFLOW_ID = "resolved-workflow-id";

    @Mock
    private CallableAiAgentDataSource callableAiAgentDataSource;

    @Mock
    private ChildJobPrincipalFactory childJobPrincipalFactory;

    @Mock
    private JobService jobService;

    @Mock
    private SubflowResolver subflowResolver;

    @Test
    void testDispatchResolvesTheAgentThenItsWorkflowAndLaunchesWithMessageAndConversationId() {
        TaskExecution taskExecution = createTaskExecution(
            Map.of("agentUuid", AGENT_UUID, "message", "hello", "conversationId", "thread-7"));

        stubParentJob(Map.of());

        when(callableAiAgentDataSource.resolveAgent(AGENT_UUID, false))
            .thenReturn(new ResolvedAiAgent(AGENT_WORKFLOW_UUID, "Triage Agent", null));
        when(subflowResolver.resolveSubflow(AGENT_WORKFLOW_UUID, WorkflowConstants.NEW_WORKFLOW_CALL, false))
            .thenReturn(new Subflow(RESOLVED_WORKFLOW_ID, "workflowCall_1"));

        newDispatcher(callableAiAgentDataSource).dispatch(taskExecution);

        ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
            ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(childJobPrincipalFactory).createChildJob(anyLong(), jobParametersDTOArgumentCaptor.capture());

        JobParametersDTO jobParametersDTO = jobParametersDTOArgumentCaptor.getValue();

        assertThat(jobParametersDTO.getWorkflowId()).isEqualTo(RESOLVED_WORKFLOW_ID);
        assertThat(jobParametersDTO.getParentTaskExecutionId()).isEqualTo(10L);
        assertThat(jobParametersDTO.getInputs())
            .containsEntry("workflowCall_1", Map.of("message", "hello", "conversationId", "thread-7"))
            .containsEntry(JobInputConstants.TRIGGER_NAME_INPUT, "workflowCall_1");
    }

    @Test
    void testDispatchOmitsAnAbsentConversationId() {
        TaskExecution taskExecution = createTaskExecution(Map.of("agentUuid", AGENT_UUID, "message", "hello"));

        stubParentJob(Map.of());

        when(callableAiAgentDataSource.resolveAgent(AGENT_UUID, false))
            .thenReturn(new ResolvedAiAgent(AGENT_WORKFLOW_UUID, "Triage Agent", null));
        when(subflowResolver.resolveSubflow(AGENT_WORKFLOW_UUID, WorkflowConstants.NEW_WORKFLOW_CALL, false))
            .thenReturn(new Subflow(RESOLVED_WORKFLOW_ID, "workflowCall_1"));

        newDispatcher(callableAiAgentDataSource).dispatch(taskExecution);

        ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
            ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(childJobPrincipalFactory).createChildJob(anyLong(), jobParametersDTOArgumentCaptor.capture());

        assertThat(jobParametersDTOArgumentCaptor.getValue()
            .getInputs()).containsEntry("workflowCall_1", Map.of("message", "hello"));
    }

    @Test
    void testDispatchPassesTheEditorEnvironmentFlagThroughBothResolutions() {
        TaskExecution taskExecution = createTaskExecution(Map.of("agentUuid", AGENT_UUID, "message", "hello"));

        stubParentJob(Map.of(MetadataConstants.EDITOR_ENVIRONMENT, true));

        when(callableAiAgentDataSource.resolveAgent(AGENT_UUID, true))
            .thenReturn(new ResolvedAiAgent(AGENT_WORKFLOW_UUID, "Triage Agent", null));
        when(subflowResolver.resolveSubflow(AGENT_WORKFLOW_UUID, WorkflowConstants.NEW_WORKFLOW_CALL, true))
            .thenReturn(new Subflow(RESOLVED_WORKFLOW_ID, "workflowCall_1"));

        newDispatcher(callableAiAgentDataSource).dispatch(taskExecution);

        verify(callableAiAgentDataSource).resolveAgent(AGENT_UUID, true);
        verify(subflowResolver).resolveSubflow(AGENT_WORKFLOW_UUID, WorkflowConstants.NEW_WORKFLOW_CALL, true);
    }

    @Test
    void testDispatchFailsTheTaskWhenTheAgentCannotBeResolved() {
        TaskExecution taskExecution = createTaskExecution(Map.of("agentUuid", AGENT_UUID, "message", "hello"));

        stubParentJob(Map.of());

        when(callableAiAgentDataSource.resolveAgent(AGENT_UUID, false))
            .thenThrow(new IllegalArgumentException("Agent agent-uuid-1 has no published version"));

        assertThatThrownBy(() -> newDispatcher(callableAiAgentDataSource).dispatch(taskExecution))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no published version");

        verify(childJobPrincipalFactory, never()).createChildJob(anyLong(), any());
    }

    @Test
    void testDispatchFailsClearlyWhenAgentsAreNotAvailableInThisDeployment() {
        TaskExecution taskExecution = createTaskExecution(Map.of("agentUuid", AGENT_UUID, "message", "hello"));

        assertThatThrownBy(() -> newDispatcher(null).dispatch(taskExecution))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not available");

        verify(childJobPrincipalFactory, never()).createChildJob(anyLong(), any());
    }

    @Test
    void testResolveMatchesOnlyItsOwnType() {
        CallAiAgentTaskDispatcher dispatcher = newDispatcher(callableAiAgentDataSource);

        assertThat(dispatcher.resolve(taskOfType("callAiAgent/v1"))).isSameAs(dispatcher);
        assertThat(dispatcher.resolve(taskOfType("subflow/v1"))).isNull();
    }

    private CallAiAgentTaskDispatcher newDispatcher(CallableAiAgentDataSource dataSource) {
        return new CallAiAgentTaskDispatcher(childJobPrincipalFactory, dataSource, jobService, subflowResolver);
    }

    private void stubParentJob(Map<String, ?> metadata) {
        Job job = new Job();

        job.setId(1L);
        job.setMetadata(metadata);

        when(jobService.getJob(1L)).thenReturn(job);
    }

    private static TaskExecution createTaskExecution(Map<String, ?> parameters) {
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(
                new WorkflowTask(
                    Map.of("name", "callAgent", "type", "callAiAgent/v1", "parameters", parameters)))
            .build();

        taskExecution.setId(10L);
        taskExecution.setJobId(1L);

        return taskExecution;
    }

    private static Task taskOfType(String type) {
        return new WorkflowTask(Map.of("name", "task", "type", type));
    }
}
