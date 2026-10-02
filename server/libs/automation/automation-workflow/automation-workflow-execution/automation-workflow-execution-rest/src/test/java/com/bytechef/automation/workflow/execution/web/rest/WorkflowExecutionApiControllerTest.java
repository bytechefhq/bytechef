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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

/**
 * @author Ivica Cardic
 */
class WorkflowExecutionApiControllerTest {

    private final ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade =
        mock(ProjectWorkflowExecutionFacade.class);
    private final WorkflowExecutionApiController workflowExecutionApiController = new WorkflowExecutionApiController(
        mock(ConversionService.class), projectWorkflowExecutionFacade);

    @Test
    void testGetWorkflowExecutionsPagePassesTheProjectVersionToTheFacade() {
        Page<WorkflowExecutionDTO> workflowExecutionPage = Page.empty();

        when(projectWorkflowExecutionFacade.getWorkflowExecutions(
            null, null, null, null, null, null, 9L, "workflow-1", 2, 1L, 0))
                .thenReturn(workflowExecutionPage);

        ResponseEntity<Page> responseEntity = workflowExecutionApiController.getWorkflowExecutionsPage(
            1L, null, null, null, null, null, null, 9L, "workflow-1", 2, 0);

        assertSame(workflowExecutionPage, responseEntity.getBody());

        verify(projectWorkflowExecutionFacade).getWorkflowExecutions(
            null, null, null, null, null, null, 9L, "workflow-1", 2, 1L, 0);
    }
}
