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

package com.bytechef.automation.workflow.execution.web.rest;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.platform.workflow.execution.dto.FocusPathElementDTO;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionChildrenPageDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.web.rest.error.GlobalResponseEntityExceptionHandler;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ivica Cardic
 */
@Import(GlobalResponseEntityExceptionHandler.class)
@WebMvcTest(value = WorkflowExecutionApiController.class)
class WorkflowExecutionApiControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;

    @Test
    void getWorkflowExecutionPassesLazyFlag() throws Exception {
        mockMvc.perform(
            get("/internal/workflow-executions/5")
                .queryParam("lazy", "true")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(projectWorkflowExecutionFacade).getWorkflowExecution(5L, true);
    }

    @Test
    void getWorkflowExecutionIsNotLazyByDefault() throws Exception {
        mockMvc.perform(
            get("/internal/workflow-executions/5")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(projectWorkflowExecutionFacade).getWorkflowExecution(5L, false);
    }

    @Test
    void getWorkflowExecutionTaskExecutionChildrenUsesDefaults() throws Exception {
        when(projectWorkflowExecutionFacade.getWorkflowExecutionTaskExecutionChildren(5L, 9L, 0, 50))
            .thenReturn(new TaskExecutionChildrenPageDTO(List.of(), List.of(), 0, 50, 0, 120L, 3));

        mockMvc.perform(
            get("/internal/workflow-executions/5/task-executions/9/children")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(120))
            .andExpect(jsonPath("$.totalPages").value(3));

        verify(projectWorkflowExecutionFacade).getWorkflowExecutionTaskExecutionChildren(5L, 9L, 0, 50);
    }

    @Test
    void getWorkflowExecutionTaskExecutionChildrenRejectsOutOfRangePaging() throws Exception {
        List<Map<String, String>> invalidQueryParams = List.of(
            Map.of("pageNumber", "-1"), Map.of("pageSize", "0"), Map.of("pageSize", "201"));

        for (Map<String, String> invalidQueryParam : invalidQueryParams) {
            Map.Entry<String, String> entry = invalidQueryParam.entrySet()
                .iterator()
                .next();

            mockMvc.perform(
                get("/internal/workflow-executions/5/task-executions/9/children")
                    .queryParam(entry.getKey(), entry.getValue())
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }

        verify(projectWorkflowExecutionFacade, never())
            .getWorkflowExecutionTaskExecutionChildren(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void getWorkflowExecutionTaskExecutionChildrenReportsMissingTaskExecutionAsNotFound() throws Exception {
        when(projectWorkflowExecutionFacade.getWorkflowExecutionTaskExecutionChildren(5L, 9L, 0, 50))
            .thenThrow(new NoSuchElementException("Task execution 9 not found"));

        mockMvc.perform(
            get("/internal/workflow-executions/5/task-executions/9/children")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void getWorkflowExecutionTaskExecutionChildrenRejectsTaskExecutionOfAnotherWorkflowExecution() throws Exception {
        when(projectWorkflowExecutionFacade.getWorkflowExecutionTaskExecutionChildren(5L, 9L, 0, 50))
            .thenThrow(
                new IllegalArgumentException("Task execution 9 does not belong to workflow execution 5"));

        mockMvc.perform(
            get("/internal/workflow-executions/5/task-executions/9/children")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getWorkflowExecutionTaskExecutionChildrenSerializesPlainPage() throws Exception {
        Job childJob = new Job(50L);

        childJob.setParentTaskExecutionId(12L);
        childJob.setStatus(Job.Status.COMPLETED);

        List<TaskExecutionDTO> children = List.of(
            createTaskExecutionDTO(11L, 3, null), createTaskExecutionDTO(12L, 0, JobDTO.shallow(childJob)));

        when(projectWorkflowExecutionFacade.getWorkflowExecutionTaskExecutionChildren(5L, 9L, 0, 50))
            .thenReturn(new TaskExecutionChildrenPageDTO(children, List.of(), 0, 50, 2, 2L, 1));

        mockMvc.perform(
            get("/internal/workflow-executions/5/task-executions/9/children")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.children.length()").value(2))
            .andExpect(jsonPath("$.children[0].id").value(11))
            .andExpect(jsonPath("$.children[0].childCount").value(3))
            .andExpect(jsonPath("$.children[1].id").value(12))
            .andExpect(jsonPath("$.children[1].childJob.id").value(50))
            .andExpect(jsonPath("$.children[1].childJob.status").value("COMPLETED"))
            .andExpect(jsonPath("$.iterations").isArray())
            .andExpect(jsonPath("$.iterations").isEmpty());
    }

    @Test
    void getWorkflowExecutionTaskExecutionChildrenSerializesLoopPage() throws Exception {
        List<List<TaskExecutionDTO>> iterations = List.of(
            List.of(createTaskExecutionDTO(21L, 0, null), createTaskExecutionDTO(22L, 4, null)),
            List.of(createTaskExecutionDTO(23L, 0, null)));

        when(projectWorkflowExecutionFacade.getWorkflowExecutionTaskExecutionChildren(5L, 9L, 0, 50))
            .thenReturn(new TaskExecutionChildrenPageDTO(List.of(), iterations, 0, 50, 2, 2L, 1));

        mockMvc.perform(
            get("/internal/workflow-executions/5/task-executions/9/children")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.children").isEmpty())
            .andExpect(jsonPath("$.iterations.length()").value(2))
            .andExpect(jsonPath("$.iterations[0][0].id").value(21))
            .andExpect(jsonPath("$.iterations[0][1].id").value(22))
            .andExpect(jsonPath("$.iterations[0][1].childCount").value(4))
            .andExpect(jsonPath("$.iterations[1][0].id").value(23));
    }

    @Test
    void getWorkflowExecutionLazySerializesFocusPathAndTaskExecutionCount() throws Exception {
        Job job = new Job(5L);

        job.setStatus(Job.Status.FAILED);

        JobDTO jobDTO = new JobDTO(job, Map.of(), List.of(createTaskExecutionDTO(1L, 2, null)));

        List<FocusPathElementDTO> focusPath = List.of(
            new FocusPathElementDTO(1L, null, null), new FocusPathElementDTO(7L, 3, null));

        when(projectWorkflowExecutionFacade.getWorkflowExecution(5L, true))
            .thenReturn(new WorkflowExecutionDTO(5L, null, null, null, jobDTO, null, null, focusPath, 120L));

        mockMvc.perform(
            get("/internal/workflow-executions/5")
                .queryParam("lazy", "true")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskExecutionCount").value(120))
            .andExpect(jsonPath("$.focusPath.length()").value(2))
            .andExpect(jsonPath("$.focusPath[0].taskExecutionId").value("1"))
            .andExpect(jsonPath("$.focusPath[1].taskExecutionId").value("7"))
            .andExpect(jsonPath("$.focusPath[1].iterationIndex").value(3))
            .andExpect(jsonPath("$.job.taskExecutions[0].id").value(1))
            .andExpect(jsonPath("$.job.taskExecutions[0].childCount").value(2));
    }

    private static TaskExecutionDTO createTaskExecutionDTO(long id, int childCount, @Nullable JobDTO childJob) {
        return new TaskExecutionDTO(
            null, null, null, null, 0, null, id, null, 5L, null, null, 0, null, 9L, 0, 0, 0, null, 0, 0, null,
            TaskExecution.Status.COMPLETED, 0, "Title", "myComponent/v1/myAction", null, List.of(), List.of(),
            childJob, childCount);
    }

    @Configuration
    @ComponentScan(basePackages = {
        "com.bytechef.automation.configuration.web.rest.adapter",
        "com.bytechef.automation.workflow.execution.web.rest.adapter",
        "com.bytechef.automation.workflow.execution.web.rest.mapper",
        "com.bytechef.platform.configuration.web.rest.adapter",
        "com.bytechef.platform.workflow.execution.web.rest.adapter",
        "com.bytechef.platform.workflow.execution.web.rest.mapper",
        "com.bytechef.web.rest.mapper"
    })
    static class WorkflowExecutionApiControllerIntTestConfiguration {

        @Bean
        WorkflowExecutionApiController workflowExecutionApiController(
            ConversionService conversionService, ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade) {

            return new WorkflowExecutionApiController(conversionService, projectWorkflowExecutionFacade);
        }
    }
}
