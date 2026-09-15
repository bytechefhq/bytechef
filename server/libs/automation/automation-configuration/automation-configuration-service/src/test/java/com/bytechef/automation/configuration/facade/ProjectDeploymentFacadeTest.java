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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectDeploymentFacadeTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private TagService tagService;

    @InjectMocks
    private ProjectDeploymentFacadeImpl projectDeploymentFacade;

    @Test
    void testUpdateProjectDeploymentWorkflowRefusesAConnectionTheWorkflowMayNotUse() {
        ProjectDeploymentWorkflow storedProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        storedProjectDeploymentWorkflow.setProjectDeploymentId(3L);
        storedProjectDeploymentWorkflow.setWorkflowId("workflow-1");

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(
            List.of(new ProjectDeploymentWorkflowConnection(9L, "connection", "node_1")));
        projectDeploymentWorkflow.setId(5L);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(5L))
            .thenReturn(storedProjectDeploymentWorkflow);
        when(projectDeploymentService.getProjectDeployment(3L))
            .thenReturn(projectDeployment);
        when(permissionService.canUseConnectionInWorkflow(9L, "workflow-1", Environment.PRODUCTION))
            .thenReturn(false);

        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow))
            .isInstanceOf(AccessDeniedException.class);

        verify(projectDeploymentWorkflowService, never()).update(any(ProjectDeploymentWorkflow.class));
    }

    @Test
    void testGetProjectDeploymentTags() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setTagIds(List.of(20L, 21L));

        when(projectDeploymentService.getProjectDeployments())
            .thenReturn(List.of(projectDeployment));
        when(tagService.getTags(List.of(20L, 21L))).thenReturn(List.of(new Tag("x"), new Tag("y")));

        List<Tag> tags = projectDeploymentFacade.getProjectDeploymentTags();

        assertThat(tags).hasSize(2);
    }

    @Test
    void testValidateInputsAcceptsNonStringValuesForRequiredInputs() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(
            new Workflow.Input("hourToRun", "Hour", "integer", true),
            new Workflow.Input("minutesToRun", "Minute", "integer", true),
            new Workflow.Input("serviceProviderEmail", "Email", "string", true)));

        Map<String, Object> inputs = Map.of("hourToRun", 11, "minutesToRun", 50, "serviceProviderEmail", "a@b.c");

        assertThatCode(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(inputs, workflow))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidateInputsRejectsMissingBlankAndNullRequiredValues() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(new Workflow.Input("name", "Name", "string", true)));

        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(Map.of(), workflow))
            .withMessageContaining("Missing required param: name");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(
                Map.of("name", "   "), workflow))
            .withMessageContaining("Missing required param: name");

        Map<String, Object> nullValueInputs = new HashMap<>();

        nullValueInputs.put("name", null);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(
                nullValueInputs, workflow))
            .withMessageContaining("Missing required param: name");
    }

    @Test
    void testValidateInputsIgnoresAbsentOptionalInputs() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(
            new Workflow.Input("destinationFolderName", "Folder", "string", false)));

        assertThatCode(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(Map.of(), workflow))
            .doesNotThrowAnyException();
    }
}
