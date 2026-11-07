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

package com.bytechef.automation.configuration.web.graphql;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ProjectWorkflowGraphQlControllerErrorWorkflowTest {

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private ProjectWorkflowGraphQlController projectWorkflowGraphQlController;

    @Test
    void testUpdateProjectWorkflowErrorWorkflowDelegatesToTheFacade() {
        Boolean result = projectWorkflowGraphQlController.updateProjectWorkflowErrorWorkflow(1L, 10L, 99L, false);

        assertTrue(result);

        verify(projectWorkflowFacade).updateWorkflowErrorWorkflow(1L, 10L, 99L, false);
    }

    @Test
    void testUpdateProjectWorkflowErrorWorkflowDelegatesToTheFacadeWithExplicitNull() {
        Boolean result = projectWorkflowGraphQlController.updateProjectWorkflowErrorWorkflow(1L, 10L, null, true);

        assertTrue(result);

        verify(projectWorkflowFacade).updateWorkflowErrorWorkflow(1L, 10L, null, true);
    }

    @Test
    void testUpdateProjectWorkflowErrorWorkflowWrapsValidationFailureAsGraphQlBadRequest() {
        Mockito.doThrow(new IllegalArgumentException("A workflow cannot be its own error workflow"))
            .when(projectWorkflowFacade)
            .updateWorkflowErrorWorkflow(1L, 10L, 10L, false);

        GraphQlBadRequestException exception = Assertions.assertThrows(
            GraphQlBadRequestException.class,
            () -> projectWorkflowGraphQlController.updateProjectWorkflowErrorWorkflow(1L, 10L, 10L, false));

        Assertions.assertEquals("A workflow cannot be its own error workflow", exception.getMessage());
    }

    @Test
    void testEligibleErrorWorkflowsFiltersByTrigger() {
        ProjectWorkflow eligible = new ProjectWorkflow(1L);

        eligible.setWorkflowId("wf-1");

        ProjectWorkflow ineligible = new ProjectWorkflow(2L);

        ineligible.setWorkflowId("wf-2");

        Mockito.when(projectWorkflowService.getProjectWorkflows(1L, 3))
            .thenReturn(List.of(eligible, ineligible));

        Workflow errorHandlerWorkflow = new Workflow(
            "wf-1", "{\"triggers\":[{\"name\":\"t1\",\"type\":\"workflow/v1/newWorkflowError\"}],\"tasks\":[]}",
            Workflow.Format.JSON);

        Workflow plainWorkflow = new Workflow("wf-2", "{\"triggers\":[],\"tasks\":[]}", Workflow.Format.JSON);

        Mockito.when(workflowService.getWorkflow("wf-1"))
            .thenReturn(errorHandlerWorkflow);
        Mockito.when(workflowService.getWorkflow("wf-2"))
            .thenReturn(plainWorkflow);

        List<ProjectWorkflow> result = projectWorkflowGraphQlController.eligibleErrorWorkflows(1L, 3);

        Assertions.assertEquals(List.of(eligible), result);
    }
}
