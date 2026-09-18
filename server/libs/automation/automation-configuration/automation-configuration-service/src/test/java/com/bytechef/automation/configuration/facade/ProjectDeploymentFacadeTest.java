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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectDeploymentFacadeTest {

    private static final String MISSING_PROJECT_WORKFLOW_MESSAGE = "Project workflow not found";
    private static final String MISSING_WORKFLOW_ID = "wf-missing";
    private static final String UNARMABLE_WORKFLOW_ID = "wf-unarmable";
    private static final String MISSING_WORKFLOW_MESSAGE = "Workflow not found";
    private static final long PROJECT_DEPLOYMENT_ID = 1L;
    private static final String REQUIRED_INPUT_NAME = "apiKey";
    private static final String WORKFLOW_ID = "wf-1";

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ComponentConnectionFacade componentConnectionFacade;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private TagService tagService;

    @Mock
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private ProjectDeploymentFacadeImpl projectDeploymentFacade;

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

    @Test
    void testEnableArmsNoRowWhenALaterEnabledRowIsMissingARequiredInput() {
        stubWorkflowWithRequiredInput();

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(PROJECT_DEPLOYMENT_ID)).thenReturn(
            List.of(
                enabledRow(10L, Map.of(REQUIRED_INPUT_NAME, "secret")),
                enabledRow(11L, Map.of())));

        assertThatThrownBy(() -> createProjectDeploymentFacade().enableProjectDeployment(PROJECT_DEPLOYMENT_ID, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Missing required param: " + REQUIRED_INPUT_NAME);

        verify(triggerLifecycleFacade, never())
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
        verify(projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testEnableArmsEveryRowWhenEveryEnabledRowHasItsRequiredInputs() {
        stubTriggerArming(stubWorkflowWithRequiredInput());

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(PROJECT_DEPLOYMENT_ID)).thenReturn(
            List.of(enabledRow(10L, Map.of(REQUIRED_INPUT_NAME, "secret"))));

        createProjectDeploymentFacade().enableProjectDeployment(PROJECT_DEPLOYMENT_ID, true);

        verify(projectDeploymentService).updateEnabled(PROJECT_DEPLOYMENT_ID, true);
    }

    @Test
    void testEnableIgnoresADisabledRowMissingARequiredInput() {
        stubTriggerArming(stubWorkflowWithRequiredInput());

        ProjectDeploymentWorkflow disabledRow = enabledRow(11L, Map.of());

        disabledRow.setEnabled(false);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(PROJECT_DEPLOYMENT_ID)).thenReturn(
            List.of(enabledRow(10L, Map.of(REQUIRED_INPUT_NAME, "secret")), disabledRow));

        createProjectDeploymentFacade().enableProjectDeployment(PROJECT_DEPLOYMENT_ID, true);

        verify(triggerLifecycleFacade, times(1))
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
        verify(projectDeploymentService).updateEnabled(PROJECT_DEPLOYMENT_ID, true);
    }

    @Test
    void testCheckEnableThrowsForAnEnabledRowMissingARequiredInputAndArmsNothing() {
        stubWorkflowWithRequiredInput();

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(PROJECT_DEPLOYMENT_ID)).thenReturn(
            List.of(
                enabledRow(10L, Map.of(REQUIRED_INPUT_NAME, "secret")),
                enabledRow(11L, Map.of())));

        assertThatThrownBy(
            () -> createProjectDeploymentFacade().checkEnableProjectDeployment(PROJECT_DEPLOYMENT_ID, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing required param: " + REQUIRED_INPUT_NAME);

        verify(triggerLifecycleFacade, never())
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
        verify(projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testCheckEnableArmsNothingWhenEveryEnabledRowHasItsRequiredInputs() {
        stubWorkflowWithRequiredInput();

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(PROJECT_DEPLOYMENT_ID)).thenReturn(
            List.of(enabledRow(10L, Map.of(REQUIRED_INPUT_NAME, "secret"))));

        createProjectDeploymentFacade().checkEnableProjectDeployment(PROJECT_DEPLOYMENT_ID, true);

        verify(triggerLifecycleFacade, never())
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
        verify(projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testCheckEnableThrowsWhenALaterEnabledRowCannotBeArmedAndArmsNothing() {
        stubTriggerArming(stubWorkflowWithRequiredInput());

        ProjectDeploymentWorkflow unarmableRow = unarmableEnabledRow(11L);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(PROJECT_DEPLOYMENT_ID)).thenReturn(
            List.of(enabledRow(10L, Map.of(REQUIRED_INPUT_NAME, "secret")), unarmableRow));

        assertThatThrownBy(
            () -> createProjectDeploymentFacade().checkEnableProjectDeployment(PROJECT_DEPLOYMENT_ID, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MISSING_PROJECT_WORKFLOW_MESSAGE);

        verify(triggerLifecycleFacade, never())
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
        verify(projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testEnableResolvesEveryEnabledRowBeforeArmingAny() {
        stubTriggerArming(stubWorkflowWithRequiredInput());

        ProjectDeploymentWorkflow unarmableRow = unarmableEnabledRow(11L);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(PROJECT_DEPLOYMENT_ID)).thenReturn(
            List.of(enabledRow(10L, Map.of(REQUIRED_INPUT_NAME, "secret")), unarmableRow));

        assertThatThrownBy(() -> createProjectDeploymentFacade().enableProjectDeployment(PROJECT_DEPLOYMENT_ID, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(MISSING_PROJECT_WORKFLOW_MESSAGE);

        verify(triggerLifecycleFacade, never())
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
        verify(projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testCheckDisableValidatesNoInputsAndChangesNothing() {
        stubResolvableWorkflow();

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(PROJECT_DEPLOYMENT_ID)).thenReturn(
            List.of(enabledRow(11L, Map.of())));

        createProjectDeploymentFacade().checkEnableProjectDeployment(PROJECT_DEPLOYMENT_ID, false);

        verify(triggerLifecycleFacade, never()).executeTriggerDisable(any(), any(), any(), any(), any());
        verify(projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testCheckDisableThrowsWhenALaterEnabledRowCannotBeResolvedAndDisablesNothing() {
        stubResolvableWorkflow();

        ProjectDeploymentWorkflow unresolvableRow = unresolvableEnabledRow(11L);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(PROJECT_DEPLOYMENT_ID)).thenReturn(
            List.of(enabledRow(10L, Map.of()), unresolvableRow));

        assertThatThrownBy(
            () -> createProjectDeploymentFacade().checkEnableProjectDeployment(PROJECT_DEPLOYMENT_ID, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MISSING_WORKFLOW_MESSAGE);

        verify(triggerLifecycleFacade, never()).executeTriggerDisable(any(), any(), any(), any(), any());
        verify(projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testDisableResolvesEveryEnabledRowBeforeDisablingAny() {
        stubResolvableWorkflow();

        ProjectDeploymentWorkflow unresolvableRow = unresolvableEnabledRow(11L);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(PROJECT_DEPLOYMENT_ID)).thenReturn(
            List.of(enabledRow(10L, Map.of()), unresolvableRow));

        assertThatThrownBy(() -> createProjectDeploymentFacade().enableProjectDeployment(PROJECT_DEPLOYMENT_ID, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(MISSING_WORKFLOW_MESSAGE);

        verify(triggerLifecycleFacade, never()).executeTriggerDisable(any(), any(), any(), any(), any());
        verify(projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    private Workflow stubWorkflowWithRequiredInput() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(
            List.of(new Workflow.Input(REQUIRED_INPUT_NAME, "API key", "string", true, Map.of())));
        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);

        return workflow;
    }

    private void stubResolvableWorkflow() {
        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);

        stubTriggerResolution(workflow);
    }

    private void stubTriggerArming(Workflow workflow) {
        stubTriggerResolution(workflow);

        when(projectDeploymentService.getProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(mock(ProjectDeployment.class));
    }

    private void stubTriggerResolution(Workflow workflow) {
        WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);

        when(workflow.getId()).thenReturn(WORKFLOW_ID);
        when(workflowTrigger.getName()).thenReturn("trigger_1");
        when(workflowTrigger.getType()).thenReturn("schedule/v1/interval");
        doReturn(List.of(workflowTrigger)).when(workflow)
            .getExtensions(anyString(), any(), any());

        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getUuidAsString()).thenReturn("workflow-uuid");
        when(projectWorkflowService.getWorkflowProjectWorkflow(WORKFLOW_ID)).thenReturn(projectWorkflow);
    }

    private ProjectDeploymentWorkflow unarmableEnabledRow(long id) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = enabledRow(id, Map.of());

        projectDeploymentWorkflow.setWorkflowId(UNARMABLE_WORKFLOW_ID);

        Workflow workflow = mock(Workflow.class);
        WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);

        when(workflow.getId()).thenReturn(UNARMABLE_WORKFLOW_ID);
        when(workflow.getInputs()).thenReturn(List.of());
        doReturn(List.of(workflowTrigger)).when(workflow)
            .getExtensions(anyString(), any(), any());
        when(workflowService.getWorkflow(UNARMABLE_WORKFLOW_ID)).thenReturn(workflow);
        when(projectWorkflowService.getWorkflowProjectWorkflow(UNARMABLE_WORKFLOW_ID))
            .thenThrow(new IllegalArgumentException(MISSING_PROJECT_WORKFLOW_MESSAGE));

        return projectDeploymentWorkflow;
    }

    private ProjectDeploymentWorkflow unresolvableEnabledRow(long id) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = enabledRow(id, Map.of());

        projectDeploymentWorkflow.setWorkflowId(MISSING_WORKFLOW_ID);

        when(workflowService.getWorkflow(MISSING_WORKFLOW_ID))
            .thenThrow(new IllegalArgumentException(MISSING_WORKFLOW_MESSAGE));

        return projectDeploymentWorkflow;
    }

    private ProjectDeploymentFacadeImpl createProjectDeploymentFacade() {
        when(applicationProperties.getWebhookUrl()).thenReturn("http://localhost/webhooks/{id}");

        return new ProjectDeploymentFacadeImpl(
            null, null, null, null, null, null, null, projectDeploymentService, projectDeploymentWorkflowService, null,
            projectWorkflowService, null, null, null, triggerLifecycleFacade, applicationProperties,
            componentConnectionFacade, workflowService);
    }

    private static ProjectDeploymentWorkflow enabledRow(long id, Map<String, ?> inputs) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(id);
        projectDeploymentWorkflow.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);
        projectDeploymentWorkflow.setWorkflowId(WORKFLOW_ID);
        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setInputs(inputs);

        return projectDeploymentWorkflow;
    }
}
