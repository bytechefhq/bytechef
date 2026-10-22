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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionChildrenPageDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ivica Cardic
 */
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

    @Configuration
    @ComponentScan(basePackages = {
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
