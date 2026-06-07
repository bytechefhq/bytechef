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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.automation.ai.tool.model.WorkflowExecutionDetailInfo;
import com.bytechef.automation.ai.tool.model.WorkflowExecutionSummary;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.error.ExecutionError;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowExecutionToolsTest {

    private final ProjectWorkflowExecutionFacade facade = mock(ProjectWorkflowExecutionFacade.class);
    private final WorkflowExecutionTools workflowExecutionTools = new WorkflowExecutionTools(facade);

    @Test
    void testGetWorkflowExecutionMapsTaskFailure() {
        Job job = new Job();
        job.setId(7L);
        job.setStatus(Job.Status.FAILED);
        job.setWorkflowId("wf-1");

        TaskExecution taskExecution = TaskExecution.builder()
            .id(11L)
            .workflowTask(new WorkflowTask(Map.of("name", "getUser", "type", "httpClient/v1/get")))
            .status(TaskExecution.Status.FAILED)
            .error(new ExecutionError("boom", List.of("at x", "at y")))
            .build();

        TaskExecutionDTO taskExecutionDTO = new TaskExecutionDTO(
            taskExecution, "Get user", "icon", Map.of("url", "https://x"), null, null);

        JobDTO jobDTO = new JobDTO(job, Map.of(), List.of(taskExecutionDTO));

        Project project = new Project();
        project.setName("Demo");
        project.setWorkspaceId(42L);

        Workflow workflow = new Workflow("{\"label\": \"My Workflow\", \"tasks\": []}", Format.JSON);

        WorkflowExecutionDTO dto = new WorkflowExecutionDTO(7L, project, null, jobDTO, workflow, null);

        when(facade.getWorkflowExecution(7L)).thenReturn(dto);

        ToolContext toolContext = new ToolContext(Map.of(WorkflowExecutionToolContextKeys.WORKSPACE_ID, 42L));

        WorkflowExecutionDetailInfo result = workflowExecutionTools.getWorkflowExecution(7L, toolContext);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.workflowLabel()).isEqualTo("My Workflow");
        assertThat(result.taskExecutions()).hasSize(1);
        assertThat(result.taskExecutions()
            .get(0)
            .error()).isEqualTo("boom");
        assertThat(result.taskExecutions()
            .get(0)
            .type()).isEqualTo("httpClient/v1/get");
    }

    @Test
    void testGetWorkflowExecutionDeniesCrossWorkspaceAccess() {
        Job job = new Job();
        job.setId(7L);
        job.setStatus(Job.Status.COMPLETED);

        JobDTO jobDTO = new JobDTO(job, Map.of(), List.of());

        Project project = new Project();
        project.setName("Other workspace");
        project.setWorkspaceId(99L);

        WorkflowExecutionDTO dto = new WorkflowExecutionDTO(7L, project, null, jobDTO, null, null);

        when(facade.getWorkflowExecution(7L)).thenReturn(dto);

        // Caller is scoped to workspace 42 but the execution belongs to workspace 99.
        ToolContext toolContext = new ToolContext(Map.of(WorkflowExecutionToolContextKeys.WORKSPACE_ID, 42L));

        assertThatThrownBy(() -> workflowExecutionTools.getWorkflowExecution(7L, toolContext))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void testListWorkflowExecutionsScopesByWorkspaceFromToolContext() {
        Job job = new Job();
        job.setId(7L);
        job.setStatus(Job.Status.COMPLETED);

        JobDTO jobDTO = new JobDTO(job, Map.of(), List.of());

        Project project = new Project();
        project.setName("Demo");

        Workflow workflow = new Workflow("{\"label\": \"My Workflow\", \"tasks\": []}", Format.JSON);

        WorkflowExecutionDTO dto = new WorkflowExecutionDTO(7L, project, null, jobDTO, workflow, null);

        Page<WorkflowExecutionDTO> page = new PageImpl<>(List.of(dto));

        when(
            facade.getWorkflowExecutions(
                eq(false), eq(2L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(42L), anyInt()))
                    .thenReturn(page);

        ToolContext toolContext = new ToolContext(
            Map.of(
                WorkflowExecutionToolContextKeys.WORKSPACE_ID, 42L,
                WorkflowExecutionToolContextKeys.ENVIRONMENT_ID, 2L));

        List<WorkflowExecutionSummary> result =
            workflowExecutionTools.listWorkflowExecutions(null, null, toolContext);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .id()).isEqualTo(7L);
        assertThat(result.get(0)
            .status()).isEqualTo("COMPLETED");
    }

    @Test
    void testListWorkflowExecutionsReturnsEmptyWhenWorkspaceMissing() {
        ToolContext toolContext = new ToolContext(Map.of());

        List<WorkflowExecutionSummary> result =
            workflowExecutionTools.listWorkflowExecutions(null, null, toolContext);

        assertThat(result).isEmpty();
    }
}
