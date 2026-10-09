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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ProjectDeploymentFacadeTest {

    private static final long PROJECT_ID = 1L;
    private static final long ARMED_PROJECT_DEPLOYMENT_ID = 5L;
    private static final WorkflowNodeType WEBHOOK_TRIGGER_TYPE = new WorkflowNodeType("webhook", 1, "newEvent");

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private TagService tagService;

    @InjectMocks
    private ProjectDeploymentFacadeImpl projectDeploymentFacade;

    @AfterEach
    void afterEach() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
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

    @Test
    void testEnableDisarmsEarlierWorkflowsWhenALaterWorkflowFailsToArm() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID firstWorkflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");
        armingFixture.stubWorkflow("workflow-2", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(
                    armingFixture.projectDeploymentWorkflow("workflow-1"),
                    armingFixture.projectDeploymentWorkflow("workflow-2")));

        IllegalStateException armingFailure = new IllegalStateException("webhook registration refused");

        doReturn(null).doThrow(armingFailure)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true))
                .isSameAs(armingFailure);

        verify(armingFixture.triggerLifecycleFacade).executeTriggerDisable(
            eq("workflow-1"), argThat(workflowExecutionIdOf(firstWorkflowUuid, "trigger_1")),
            eq(WEBHOOK_TRIGGER_TYPE), eq(Map.of("event", "trigger_1")), isNull());
        verify(armingFixture.triggerLifecycleFacade, times(1))
            .executeTriggerDisable(any(), any(), any(), any(), any());
        verify(armingFixture.projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testEnableDisarmsEarlierTriggersOfTheSameWorkflowWhenALaterTriggerFailsToArm() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1", "trigger_2");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));

        IllegalStateException armingFailure = new IllegalStateException("schedule registration refused");

        doReturn(null).doThrow(armingFailure)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true))
                .isSameAs(armingFailure);

        verify(armingFixture.triggerLifecycleFacade).executeTriggerDisable(
            eq("workflow-1"), argThat(workflowExecutionIdOf(workflowUuid, "trigger_1")),
            eq(WEBHOOK_TRIGGER_TYPE), eq(Map.of("event", "trigger_1")), isNull());
        verify(armingFixture.triggerLifecycleFacade, times(1))
            .executeTriggerDisable(any(), any(), any(), any(), any());
        verify(armingFixture.projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testEnableKeepsDisarmingWhenADisarmFailsAndSuppressesItsFailure() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1", "trigger_2", "trigger_3");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));

        IllegalStateException armingFailure = new IllegalStateException("polling registration refused");
        IllegalStateException disarmingFailure = new IllegalStateException("webhook deregistration refused");

        doReturn(null).doReturn(null)
            .doThrow(armingFailure)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
        doThrow(disarmingFailure).doReturn(true)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerDisable(any(), any(), any(), any(), any());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true))
                .isSameAs(armingFailure);

        assertThat(armingFailure.getSuppressed()).containsExactly(disarmingFailure);

        verify(armingFixture.triggerLifecycleFacade).executeTriggerDisable(
            eq("workflow-1"), argThat(workflowExecutionIdOf(workflowUuid, "trigger_2")),
            eq(WEBHOOK_TRIGGER_TYPE), eq(Map.of("event", "trigger_2")), isNull());
        verify(armingFixture.triggerLifecycleFacade).executeTriggerDisable(
            eq("workflow-1"), argThat(workflowExecutionIdOf(workflowUuid, "trigger_1")),
            eq(WEBHOOK_TRIGGER_TYPE), eq(Map.of("event", "trigger_1")), isNull());
        verify(armingFixture.projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testEnableWorkflowDisarmsEarlierTriggersWhenALaterTriggerFailsToArm() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1", "trigger_2");

        ProjectDeploymentWorkflow projectDeploymentWorkflow = armingFixture.projectDeploymentWorkflow("workflow-1");

        projectDeploymentWorkflow.setId(30L);

        when(
            armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1"))
                    .thenReturn(projectDeploymentWorkflow);

        IllegalStateException armingFailure = new IllegalStateException("webhook registration refused");

        doReturn(null).doThrow(armingFailure)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(
                ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", true))
                    .isSameAs(armingFailure);

        verify(armingFixture.triggerLifecycleFacade).executeTriggerDisable(
            eq("workflow-1"), argThat(workflowExecutionIdOf(workflowUuid, "trigger_1")),
            eq(WEBHOOK_TRIGGER_TYPE), eq(Map.of("event", "trigger_1")), isNull());
        verify(armingFixture.triggerLifecycleFacade, times(1))
            .executeTriggerDisable(any(), any(), any(), any(), any());
        verify(armingFixture.projectDeploymentWorkflowService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testUpdateRestoresTheTriggerRegistrationsWhenALaterWorkflowFailsToArm() {
        ArmingFixture armingFixture = new ArmingFixture();

        UpgradedWorkflows upgradedWorkflows = armingFixture.stubUpgradedWorkflows();

        IllegalStateException armingFailure = new IllegalStateException("webhook registration refused");

        doReturn(null).doThrow(armingFailure)
            .doReturn(null)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.updateProjectDeployment(
                armingFixture.upgradedProjectDeployment(), upgradedWorkflows.projectDeploymentWorkflows(), List.of()))
                    .isSameAs(armingFailure);

        assertThat(armingFailure.getSuppressed()).isEmpty();

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-a-1", upgradedWorkflows.firstWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-a-2", upgradedWorkflows.firstWorkflowUuid());
        verifyTriggerDisable(inOrder, armingFixture, "workflow-b-1", upgradedWorkflows.secondWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-b-2", upgradedWorkflows.secondWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-b-1", upgradedWorkflows.secondWorkflowUuid());
        verifyTriggerDisable(inOrder, armingFixture, "workflow-a-2", upgradedWorkflows.firstWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-a-1", upgradedWorkflows.firstWorkflowUuid());
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
    }

    @Test
    void testUpdateDisarmsANewlyCreatedWorkflowWhenALaterWorkflowFailsToArm() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID firstWorkflowUuid = UUID.randomUUID();
        UUID secondWorkflowUuid = UUID.randomUUID();

        List<ProjectWorkflow> projectWorkflows = List.of(
            armingFixture.stubWorkflow("workflow-c", firstWorkflowUuid, 1, "trigger_1"),
            armingFixture.stubWorkflow("workflow-d", secondWorkflowUuid, 1, "trigger_1"));

        when(armingFixture.projectWorkflowService.getProjectWorkflows(PROJECT_ID)).thenReturn(projectWorkflows);
        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of());

        armingFixture.stubProjectDeploymentWorkflow(20L, "workflow-c");
        armingFixture.stubProjectDeploymentWorkflow(21L, "workflow-d");

        IllegalStateException armingFailure = new IllegalStateException("schedule registration refused");

        doReturn(null).doThrow(armingFailure)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.updateProjectDeployment(
                armingFixture.upgradedProjectDeployment(),
                List.of(
                    armingFixture.projectDeploymentWorkflow("workflow-c"),
                    armingFixture.projectDeploymentWorkflow("workflow-d")),
                List.of()))
                    .isSameAs(armingFailure);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-c", firstWorkflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-d", secondWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-c", firstWorkflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.projectDeploymentWorkflowService).updateEnabled(20L, true);
        verify(armingFixture.projectDeploymentWorkflowService, never()).updateEnabled(21L, true);
    }

    @Test
    void testUpdateKeepsRestoringWhenAnUndoStepFailsAndSuppressesItsFailure() {
        ArmingFixture armingFixture = new ArmingFixture();

        UpgradedWorkflows upgradedWorkflows = armingFixture.stubUpgradedWorkflows();

        IllegalStateException armingFailure = new IllegalStateException("polling registration refused");
        IllegalStateException undoFailure = new IllegalStateException("webhook deregistration refused");

        doReturn(null).doThrow(armingFailure)
            .doReturn(null)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
        doReturn(true).doReturn(true)
            .doThrow(undoFailure)
            .doReturn(true)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerDisable(any(), any(), any(), any(), any());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.updateProjectDeployment(
                armingFixture.upgradedProjectDeployment(), upgradedWorkflows.projectDeploymentWorkflows(), List.of()))
                    .isSameAs(armingFailure);

        assertThat(armingFailure.getSuppressed()).containsExactly(undoFailure);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-b-2", upgradedWorkflows.secondWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-b-1", upgradedWorkflows.secondWorkflowUuid());
        verifyTriggerDisable(inOrder, armingFixture, "workflow-a-2", upgradedWorkflows.firstWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-a-1", upgradedWorkflows.firstWorkflowUuid());
    }

    @Test
    void testUpdateMakesNoUndoCallsWhenEveryWorkflowArms() {
        ArmingFixture armingFixture = new ArmingFixture();

        UpgradedWorkflows upgradedWorkflows = armingFixture.stubUpgradedWorkflows();

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        armingProjectDeploymentFacade.updateProjectDeployment(
            armingFixture.upgradedProjectDeployment(), upgradedWorkflows.projectDeploymentWorkflows(), List.of());

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-a-1", upgradedWorkflows.firstWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-a-2", upgradedWorkflows.firstWorkflowUuid());
        verifyTriggerDisable(inOrder, armingFixture, "workflow-b-1", upgradedWorkflows.secondWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-b-2", upgradedWorkflows.secondWorkflowUuid());
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
    }

    @Test
    void testUpdateStopsNoRunningJobsWhenALaterWorkflowFailsToArm() {
        ArmingFixture armingFixture = new ArmingFixture();

        UpgradedWorkflows upgradedWorkflows = armingFixture.stubUpgradedWorkflows();

        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(100L)));

        IllegalStateException armingFailure = new IllegalStateException("webhook registration refused");

        doReturn(null).doThrow(armingFailure)
            .doReturn(null)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.updateProjectDeployment(
                armingFixture.upgradedProjectDeployment(), upgradedWorkflows.projectDeploymentWorkflows(), List.of()))
                    .isSameAs(armingFailure);

        verify(armingFixture.jobFacade, never()).stopJob(anyLong());
    }

    @Test
    void testUpdateStopsTheRunningJobsOfDisabledAndRemovedWorkflowsAfterEveryTriggerChange() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID upgradedWorkflowUuid = UUID.randomUUID();
        UUID removedWorkflowUuid = UUID.randomUUID();

        List<ProjectWorkflow> projectWorkflows = List.of(
            armingFixture.stubWorkflow("workflow-a-1", upgradedWorkflowUuid, 0, "trigger_1"),
            armingFixture.stubWorkflow("workflow-a-2", upgradedWorkflowUuid, 1, "trigger_1"),
            armingFixture.stubWorkflow("workflow-c-1", removedWorkflowUuid, 0, "trigger_1"));

        when(armingFixture.projectWorkflowService.getProjectWorkflows(PROJECT_ID)).thenReturn(projectWorkflows);
        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(
                    armingFixture.projectDeploymentWorkflow(10L, "workflow-a-1"),
                    armingFixture.projectDeploymentWorkflow(12L, "workflow-c-1")));

        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), eq(List.of("workflow-a-1")), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(100L)));
        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), eq(List.of("workflow-c-1")), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(102L)));

        armingFixture.stubProjectDeploymentWorkflow(10L, "workflow-a-1");
        armingFixture.stubProjectDeploymentWorkflow(10L, "workflow-a-2");
        armingFixture.stubProjectDeploymentWorkflow(12L, "workflow-c-1");

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        armingProjectDeploymentFacade.updateProjectDeployment(
            armingFixture.upgradedProjectDeployment(), List.of(armingFixture.projectDeploymentWorkflow("workflow-a-2")),
            List.of());

        InOrder inOrder = inOrder(
            armingFixture.triggerLifecycleFacade, armingFixture.principalJobService, armingFixture.jobFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-a-1", upgradedWorkflowUuid);
        verifyRunningJobsQueried(inOrder, armingFixture, "workflow-a-1");
        verifyTriggerEnable(inOrder, armingFixture, "workflow-a-2", upgradedWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-c-1", removedWorkflowUuid);
        verifyRunningJobsQueried(inOrder, armingFixture, "workflow-c-1");
        inOrder.verify(armingFixture.jobFacade)
            .stopJob(100L);
        inOrder.verify(armingFixture.jobFacade)
            .stopJob(102L);
        verifyNoMoreInteractions(
            armingFixture.triggerLifecycleFacade, armingFixture.principalJobService, armingFixture.jobFacade);
    }

    @Test
    void testDisableWorkflowReArmsEarlierTriggersWhenALaterTriggerFailsToDisarm() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1", "trigger_2");

        armingFixture.stubNoRunningJobs();
        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");

        IllegalStateException disarmingFailure = new IllegalStateException("webhook deregistration refused");

        doReturn(true).doThrow(disarmingFailure)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerDisable(any(), any(), any(), any(), any());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(
                ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", false))
                    .isSameAs(disarmingFailure);

        assertThat(disarmingFailure.getSuppressed()).isEmpty();

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_1");
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_2");
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_1");
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.principalJobService, never())
            .getJobIds(any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt());
        verify(armingFixture.projectDeploymentWorkflowService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testDisableWorkflowStopsItsRunningJobsAfterDisarmingEveryTrigger() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1", "trigger_2");

        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");

        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(100L)));

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", false);

        InOrder inOrder = inOrder(
            armingFixture.triggerLifecycleFacade, armingFixture.principalJobService,
            armingFixture.projectDeploymentWorkflowService, armingFixture.jobFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_1");
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_2");
        verifyRunningJobsQueried(inOrder, armingFixture, "workflow-1");
        inOrder.verify(armingFixture.projectDeploymentWorkflowService)
            .updateEnabled(30L, false);
        inOrder.verify(armingFixture.jobFacade)
            .stopJob(100L);
        verifyNoMoreInteractions(
            armingFixture.triggerLifecycleFacade, armingFixture.principalJobService, armingFixture.jobFacade);
    }

    @Test
    void testDisableReArmsEarlierWorkflowsWhenALaterWorkflowFailsToDisarm() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID firstWorkflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");
        UUID secondWorkflowUuid = armingFixture.stubWorkflow("workflow-2", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(
                    armingFixture.projectDeploymentWorkflow("workflow-1"),
                    armingFixture.projectDeploymentWorkflow("workflow-2")));

        IllegalStateException disarmingFailure = new IllegalStateException("schedule deregistration refused");

        doReturn(true).doThrow(disarmingFailure)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerDisable(any(), any(), any(), any(), any());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, false))
                .isSameAs(disarmingFailure);

        assertThat(disarmingFailure.getSuppressed()).isEmpty();

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testUpdateKeepingTheWorkflowIdStopsOnlyTheJobsRunningWhenItWasDisarmed() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = UUID.randomUUID();

        ProjectWorkflow projectWorkflow = armingFixture.stubWorkflow("workflow-1", workflowUuid, 0, "trigger_1");

        when(armingFixture.projectWorkflowService.getProjectWorkflows(PROJECT_ID))
            .thenReturn(List.of(projectWorkflow));
        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow(10L, "workflow-1")));

        armingFixture.stubProjectDeploymentWorkflow(10L, "workflow-1");

        AtomicBoolean reArmed = new AtomicBoolean();

        doAnswer(invocation -> {
            reArmed.set(true);

            return null;
        }).when(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                    .thenAnswer(invocation -> new PageImpl<>(reArmed.get() ? List.of(100L, 101L) : List.of(100L)));

        ProjectDeployment projectDeployment = projectDeployment(
            ARMED_PROJECT_DEPLOYMENT_ID, PROJECT_ID, Environment.DEVELOPMENT);

        projectDeployment.setEnabled(true);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        armingProjectDeploymentFacade.updateProjectDeployment(
            projectDeployment, List.of(armingFixture.projectDeploymentWorkflow("workflow-1")), List.of());

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade, armingFixture.jobFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid);
        inOrder.verify(armingFixture.jobFacade)
            .stopJob(100L);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade, armingFixture.jobFacade);
    }

    @Test
    void testDisableWorkflowKeepsStoppingItsRunningJobsWhenAStopFails() {
        ArmingFixture armingFixture = new ArmingFixture();

        armingFixture.stubWorkflow("workflow-1", "trigger_1");
        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");

        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(100L, 101L)));
        doThrow(new IllegalStateException("job already finished"))
            .when(armingFixture.jobFacade)
            .stopJob(100L);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatCode(
            () -> armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(
                ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", false))
                    .doesNotThrowAnyException();

        verify(armingFixture.jobFacade).stopJob(101L);
        verify(armingFixture.projectDeploymentWorkflowService).updateEnabled(30L, false);
        verify(armingFixture.triggerLifecycleFacade, times(1))
            .executeTriggerDisable(any(), any(), any(), any(), any());
        verify(armingFixture.triggerLifecycleFacade, never())
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void testUpdateKeepsStoppingRunningJobsWhenAStopFails() {
        ArmingFixture armingFixture = new ArmingFixture();

        armingFixture.stubUpgradedWorkflows();

        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), eq(List.of("workflow-a-1")), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(100L)));
        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), eq(List.of("workflow-b-1")), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(101L)));
        doThrow(new IllegalStateException("job already finished"))
            .when(armingFixture.jobFacade)
            .stopJob(100L);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatCode(
            () -> armingProjectDeploymentFacade.updateProjectDeployment(
                armingFixture.upgradedProjectDeployment(),
                List.of(
                    armingFixture.projectDeploymentWorkflow("workflow-a-2"),
                    armingFixture.projectDeploymentWorkflow("workflow-b-2")),
                List.of()))
                    .doesNotThrowAnyException();

        verify(armingFixture.jobFacade).stopJob(101L);
        verify(armingFixture.triggerLifecycleFacade, times(2))
            .executeTriggerDisable(any(), any(), any(), any(), any());
        verify(armingFixture.triggerLifecycleFacade, times(2))
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void testEnableDisarmsTheArmedTriggersWhenUpdateEnabledFails() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID firstWorkflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");
        UUID secondWorkflowUuid = armingFixture.stubWorkflow("workflow-2", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(
                    armingFixture.projectDeploymentWorkflow("workflow-1"),
                    armingFixture.projectDeploymentWorkflow("workflow-2")));

        IllegalStateException updateFailure = new IllegalStateException("row update refused");

        doThrow(updateFailure).when(armingFixture.projectDeploymentService)
            .updateEnabled(ARMED_PROJECT_DEPLOYMENT_ID, true);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true))
                .isSameAs(updateFailure);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
    }

    @Test
    void testDisableReArmsTheDisarmedTriggersWhenUpdateEnabledFails() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID firstWorkflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");
        UUID secondWorkflowUuid = armingFixture.stubWorkflow("workflow-2", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(
                    armingFixture.projectDeploymentWorkflow("workflow-1"),
                    armingFixture.projectDeploymentWorkflow("workflow-2")));

        IllegalStateException updateFailure = new IllegalStateException("row update refused");

        doThrow(updateFailure).when(armingFixture.projectDeploymentService)
            .updateEnabled(ARMED_PROJECT_DEPLOYMENT_ID, false);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, false))
                .isSameAs(updateFailure);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
    }

    @Test
    void testDisableDoesNotReArmATriggerThatCouldNotBeDisarmedWhenUpdateEnabledFails() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID firstWorkflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");
        UUID secondWorkflowUuid = armingFixture.stubWorkflow("workflow-2", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(
                    armingFixture.projectDeploymentWorkflow("workflow-1"),
                    armingFixture.projectDeploymentWorkflow("workflow-2")));

        doReturn(false).doReturn(true)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerDisable(any(), any(), any(), any(), any());

        IllegalStateException updateFailure = new IllegalStateException("row update refused");

        doThrow(updateFailure).when(armingFixture.projectDeploymentService)
            .updateEnabled(ARMED_PROJECT_DEPLOYMENT_ID, false);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, false))
                .isSameAs(updateFailure);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
    }

    @Test
    void testDisableWorkflowInATransactionDoesNotReArmATriggerThatCouldNotBeDisarmedWhenTheTransactionRollsBack() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1", "trigger_2");

        armingFixture.stubNoRunningJobs();
        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");

        doReturn(true).doReturn(false)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerDisable(any(), any(), any(), any(), any());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", false);

        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertThat(transactionSynchronizations).hasSize(1);

        TransactionSynchronization transactionSynchronization = transactionSynchronizations.getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_1");
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_2");
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_1");
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
    }

    @Test
    void testDisableWorkflowInATransactionReArmsItsTriggerInTheEnvironmentItWasDisarmedIn() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");

        armingFixture.stubNoRunningJobs();
        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", false);

        ProjectDeployment disarmedProjectDeployment = armingFixture.projectDeploymentService.getProjectDeployment(
            ARMED_PROJECT_DEPLOYMENT_ID);

        disarmedProjectDeployment.setEnvironment(Environment.PRODUCTION);

        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertThat(transactionSynchronizations).hasSize(1);

        TransactionSynchronization transactionSynchronization = transactionSynchronizations.getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
    }

    @Test
    void testEnableWorkflowDisarmsItsTriggersWhenUpdateEnabledFails() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1", "trigger_2");

        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");

        IllegalStateException updateFailure = new IllegalStateException("row update refused");

        doThrow(updateFailure).when(armingFixture.projectDeploymentWorkflowService)
            .updateEnabled(30L, true);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(
                ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", true))
                    .isSameAs(updateFailure);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_1");
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_2");
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_2");
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_1");
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
    }

    @Test
    void testDisableWorkflowReArmsItsTriggersAndStopsNoJobWhenUpdateEnabledFails() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1", "trigger_2");

        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");

        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(100L)));

        IllegalStateException updateFailure = new IllegalStateException("row update refused");

        doThrow(updateFailure).when(armingFixture.projectDeploymentWorkflowService)
            .updateEnabled(30L, false);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(
                ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", false))
                    .isSameAs(updateFailure);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_1");
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_2");
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_2");
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_1");
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.jobFacade, never()).stopJob(anyLong());
    }

    @Test
    void testDeleteReArmsTheDisarmedTriggersWhenALaterDeleteFails() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID firstWorkflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");
        UUID secondWorkflowUuid = armingFixture.stubWorkflow("workflow-2", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(
                    armingFixture.projectDeploymentWorkflow(40L, "workflow-1"),
                    armingFixture.projectDeploymentWorkflow(41L, "workflow-2")));

        IllegalStateException deleteFailure = new IllegalStateException("row delete refused");

        doThrow(deleteFailure).when(armingFixture.projectDeploymentService)
            .delete(ARMED_PROJECT_DEPLOYMENT_ID);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(() -> armingProjectDeploymentFacade.deleteProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID))
            .isSameAs(deleteFailure);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
    }

    @Test
    void testEnableInATransactionDisarmsTheArmedTriggersWhenTheTransactionRollsBack() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID firstWorkflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");
        UUID secondWorkflowUuid = armingFixture.stubWorkflow("workflow-2", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(
                    armingFixture.projectDeploymentWorkflow("workflow-1"),
                    armingFixture.projectDeploymentWorkflow("workflow-2")));

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true);

        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertThat(transactionSynchronizations).hasSize(1);

        TransactionSynchronization transactionSynchronization = transactionSynchronizations.getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.platformTransactionManager).getTransaction(
            argThat(transactionDefinition -> transactionDefinition
                .getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(armingFixture.jobFacade, never()).stopJob(anyLong());
    }

    @Test
    void testDisableInATransactionReArmsTheDisarmedTriggersWhenTheTransactionRollsBack() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID firstWorkflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");
        UUID secondWorkflowUuid = armingFixture.stubWorkflow("workflow-2", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(
                List.of(
                    armingFixture.projectDeploymentWorkflow("workflow-1"),
                    armingFixture.projectDeploymentWorkflow("workflow-2")));

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, false);

        verify(armingFixture.jobFacade, never()).stopJob(anyLong());

        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertThat(transactionSynchronizations).hasSize(1);

        TransactionSynchronization transactionSynchronization = transactionSynchronizations.getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-2", secondWorkflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.jobFacade, never()).stopJob(anyLong());
    }

    @Test
    void testDisableWorkflowInATransactionStopsItsRunningJobsOnlyAfterCommit() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1", "trigger_2");

        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");

        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(100L)));

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", false);

        verify(armingFixture.jobFacade, never()).stopJob(anyLong());

        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertThat(transactionSynchronizations).hasSize(1);

        TransactionSynchronization transactionSynchronization = transactionSynchronizations.getFirst();

        transactionSynchronization.afterCommit();
        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        verify(armingFixture.jobFacade).stopJob(100L);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_1");
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_2");
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade, armingFixture.jobFacade);
        verifyNoMoreInteractions(armingFixture.platformTransactionManager);
    }

    @Test
    void testUpdateInATransactionRestoresTheTriggerRegistrationsWhenTheTransactionRollsBack() {
        ArmingFixture armingFixture = new ArmingFixture();

        UpgradedWorkflows upgradedWorkflows = armingFixture.stubUpgradedWorkflows();

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.updateProjectDeployment(
            armingFixture.upgradedProjectDeployment(), upgradedWorkflows.projectDeploymentWorkflows(), List.of());

        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertThat(transactionSynchronizations).hasSize(1);

        TransactionSynchronization transactionSynchronization = transactionSynchronizations.getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-a-1", upgradedWorkflows.firstWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-a-2", upgradedWorkflows.firstWorkflowUuid());
        verifyTriggerDisable(inOrder, armingFixture, "workflow-b-1", upgradedWorkflows.secondWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-b-2", upgradedWorkflows.secondWorkflowUuid());
        verifyTriggerDisable(inOrder, armingFixture, "workflow-b-2", upgradedWorkflows.secondWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-b-1", upgradedWorkflows.secondWorkflowUuid());
        verifyTriggerDisable(inOrder, armingFixture, "workflow-a-2", upgradedWorkflows.firstWorkflowUuid());
        verifyTriggerEnable(inOrder, armingFixture, "workflow-a-1", upgradedWorkflows.firstWorkflowUuid());
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.jobFacade, never()).stopJob(anyLong());
    }

    @Test
    void testUnknownTransactionOutcomeNeitherUndoesTheTriggerChangesNorStopsJobs() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");

        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");

        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(100L)));

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", false);

        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertThat(transactionSynchronizations).hasSize(1);

        TransactionSynchronization transactionSynchronization = transactionSynchronizations.getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade, armingFixture.platformTransactionManager);
        verify(armingFixture.jobFacade, never()).stopJob(anyLong());
    }

    @Test
    void testFailedEnableInATransactionLeavesTheUndoToTheRollback() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID firstWorkflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));

        IllegalStateException updateFailure = new IllegalStateException("row update refused");

        doThrow(updateFailure).when(armingFixture.projectDeploymentService)
            .updateEnabled(ARMED_PROJECT_DEPLOYMENT_ID, true);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true))
                .isSameAs(updateFailure);

        verify(armingFixture.triggerLifecycleFacade, never()).executeTriggerDisable(any(), any(), any(), any(), any());

        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertThat(transactionSynchronizations).hasSize(1);

        TransactionSynchronization transactionSynchronization = transactionSynchronizations.getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", firstWorkflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.platformTransactionManager).getTransaction(
            argThat(transactionDefinition -> transactionDefinition
                .getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }

    @Test
    void testFailedDisableWorkflowInATransactionStopsNoJobsWhenTheTransactionCommits() {
        ArmingFixture armingFixture = new ArmingFixture();

        armingFixture.stubWorkflow("workflow-1", "trigger_1");
        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");

        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(100L)));

        IllegalStateException updateFailure = new IllegalStateException("row update refused");

        doThrow(updateFailure).when(armingFixture.projectDeploymentWorkflowService)
            .updateEnabled(30L, false);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(
                ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", false))
                    .isSameAs(updateFailure);

        TransactionSynchronization transactionSynchronization = TransactionSynchronizationManager.getSynchronizations()
            .getFirst();

        transactionSynchronization.afterCommit();
        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        verify(armingFixture.jobFacade, never()).stopJob(anyLong());
    }

    @Test
    void testRollbackUndoOfAnArmedWebhookTriggerUndoesTheEnableWithTheArmedOutputWithoutStoringItFirst() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));

        WebhookEnableOutput armedWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-in-the-rolled-back-transaction"), null);

        when(armingFixture.triggerStateService.fetchValue(argThat(workflowExecutionIdOf(workflowUuid, "trigger_1"))))
            .thenReturn(Optional.empty());
        armingFixture.stubTriggerEnableOutput(armedWebhookEnableOutput);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true);

        TransactionSynchronization transactionSynchronization = TransactionSynchronizationManager.getSynchronizations()
            .getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerEnableUndo(inOrder, armingFixture, workflowUuid, armedWebhookEnableOutput, null);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.triggerStateService, times(1)).fetchValue(any());
        verify(armingFixture.triggerStateService, never()).save(any(), any());
    }

    @Test
    void testRollbackUndoOfAnArmedWebhookTriggerPassesTheTriggerStateThatPrecededTheArm() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));

        WebhookEnableOutput previousWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-before-the-change"), null);
        WebhookEnableOutput armedWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-in-the-rolled-back-transaction"), null);

        when(armingFixture.triggerStateService.fetchValue(argThat(workflowExecutionIdOf(workflowUuid, "trigger_1"))))
            .thenReturn(Optional.of(previousWebhookEnableOutput));
        armingFixture.stubTriggerEnableOutput(armedWebhookEnableOutput);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true);

        TransactionSynchronization transactionSynchronization = TransactionSynchronizationManager.getSynchronizations()
            .getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerEnableUndo(
            inOrder, armingFixture, workflowUuid, armedWebhookEnableOutput, previousWebhookEnableOutput);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.triggerStateService, never()).save(any(), any());
    }

    @Test
    void testRollbackUndoOfAnArmedWebhookTriggerPassesATriggerStateThatPrecededTheArmReadAsAMap() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));

        Map<String, Object> previousTriggerState = Map.of(
            "parameters", Map.of("id", "subscription-armed-before-the-change"),
            "webhookExpirationDate", "2026-10-10T12:00:00Z");
        WebhookEnableOutput armedWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-in-the-rolled-back-transaction"), null);

        when(armingFixture.triggerStateService.fetchValue(argThat(workflowExecutionIdOf(workflowUuid, "trigger_1"))))
            .thenReturn(Optional.of(previousTriggerState));
        armingFixture.stubTriggerEnableOutput(armedWebhookEnableOutput);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true);

        TransactionSynchronization transactionSynchronization = TransactionSynchronizationManager.getSynchronizations()
            .getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerEnableUndo(inOrder, armingFixture, workflowUuid, armedWebhookEnableOutput, previousTriggerState);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.triggerStateService, never()).save(any(), any());
    }

    @Test
    void testRollbackUndoOfATriggerWhoseArmReturnedNoOutputIgnoresANonWebhookTriggerStateThatPrecededTheArm() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));

        Map<String, Object> pollingTriggerState = Map.of("lastPolledId", 7);

        when(armingFixture.triggerStateService.fetchValue(argThat(workflowExecutionIdOf(workflowUuid, "trigger_1"))))
            .thenReturn(Optional.of(pollingTriggerState));

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true);

        TransactionSynchronization transactionSynchronization = TransactionSynchronizationManager.getSynchronizations()
            .getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verify(armingFixture.triggerStateService, times(1)).fetchValue(any());
        verify(armingFixture.triggerStateService, never()).save(any(), any());
    }

    @Test
    void testRollbackUndoOfATriggerWhoseArmReturnedNoOutputIgnoresATriggerStateThatPrecededTheArmReadAsAMap() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));

        Map<String, Object> previousTriggerState = Map.of(
            "parameters", Map.of("id", "subscription-armed-before-the-change"),
            "webhookExpirationDate", "2026-10-10T12:00:00Z");

        when(armingFixture.triggerStateService.fetchValue(argThat(workflowExecutionIdOf(workflowUuid, "trigger_1"))))
            .thenReturn(Optional.of(previousTriggerState));

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true);

        TransactionSynchronization transactionSynchronization = TransactionSynchronizationManager.getSynchronizations()
            .getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verify(armingFixture.triggerStateService, times(1)).fetchValue(any());
        verify(armingFixture.triggerStateService, never()).save(any(), any());
    }

    @Test
    void testRollbackUndoOfATriggerWhoseArmReturnedNoOutputRestoresNoTriggerState() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));

        WebhookEnableOutput unrelatedWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-stored-by-someone-else"), null);

        when(armingFixture.triggerStateService.fetchValue(argThat(workflowExecutionIdOf(workflowUuid, "trigger_1"))))
            .thenReturn(Optional.empty(), Optional.of(unrelatedWebhookEnableOutput), Optional.empty());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true);

        TransactionSynchronization transactionSynchronization = TransactionSynchronizationManager.getSynchronizations()
            .getFirst();

        transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verify(armingFixture.triggerStateService, times(1)).fetchValue(any());
        verify(armingFixture.triggerStateService, never()).save(any(), any());
    }

    @Test
    void testEnableSuppressesTheFailureWhenTheUndoCannotRemoveAnArmedWebhookSubscription() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1", "trigger_2");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));

        WebhookEnableOutput previousWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-before-the-change"), null);
        WebhookEnableOutput armedWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-by-the-change"), null);
        IllegalStateException armingFailure = new IllegalStateException("webhook registration refused");

        when(armingFixture.triggerStateService.fetchValue(any()))
            .thenReturn(Optional.of(previousWebhookEnableOutput), Optional.empty());
        doReturn(armedWebhookEnableOutput).doThrow(armingFailure)
            .when(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong());
        when(
            armingFixture.triggerLifecycleFacade.executeTriggerEnableUndo(
                any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(false);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        assertThatThrownBy(
            () -> armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true))
                .isSameAs(armingFailure);

        assertThat(armingFailure.getSuppressed())
            .singleElement()
            .satisfies(suppressed -> assertThat(suppressed.getMessage())
                .contains("trigger_1", "workflow-1", "could not be disabled"));

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid, "trigger_2");
        verifyTriggerEnableUndo(
            inOrder, armingFixture, workflowUuid, armedWebhookEnableOutput, previousWebhookEnableOutput);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
        verify(armingFixture.triggerStateService, never()).save(any(), any());
    }

    @Test
    void testRollbackUndoLogsAWarningWhenItCannotRemoveAnArmedWebhookSubscription() {
        ArmingFixture armingFixture = new ArmingFixture();

        armingFixture.stubWorkflow("workflow-1", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));

        WebhookEnableOutput armedWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-in-the-rolled-back-transaction"), null);

        when(armingFixture.triggerStateService.fetchValue(any())).thenReturn(Optional.empty());
        armingFixture.stubTriggerEnableOutput(armedWebhookEnableOutput);
        when(
            armingFixture.triggerLifecycleFacade.executeTriggerEnableUndo(
                any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(false);

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true);

        TransactionSynchronization transactionSynchronization = TransactionSynchronizationManager.getSynchronizations()
            .getFirst();

        Logger logger = (Logger) LoggerFactory.getLogger(ProjectDeploymentFacadeImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        try {
            transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        } finally {
            logger.detachAppender(listAppender);
        }

        verify(armingFixture.triggerLifecycleFacade).executeTriggerEnableUndo(
            any(), any(), any(), any(), any(), eq(armedWebhookEnableOutput), isNull());
        verify(armingFixture.triggerLifecycleFacade, never()).executeTriggerDisable(any(), any(), any(), any(), any());
        verify(armingFixture.triggerStateService, never()).save(any(), any());

        assertThat(listAppender.list)
            .anySatisfy(loggingEvent -> {
                assertThat(loggingEvent.getLevel()).isEqualTo(Level.WARN);
                assertThat(loggingEvent.getFormattedMessage()).contains("trigger_1", "workflow-1");
                assertThat(loggingEvent.getThrowableProxy()
                    .getMessage()).contains("could not be disabled");
            });
    }

    @Test
    void testChangeThatJournalsNothingRegistersNoSynchronization() {
        ArmingFixture armingFixture = new ArmingFixture();

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of());

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true);

        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    void testTwoChangesInOneTransactionAreUndoneLatestFirstWhenTheTransactionRollsBack() {
        ArmingFixture armingFixture = new ArmingFixture();

        UUID workflowUuid = armingFixture.stubWorkflow("workflow-1", "trigger_1");

        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");
        armingFixture.stubNoRunningJobs();

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", false);
        armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", true);

        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertThat(transactionSynchronizations).hasSize(2);

        TransactionSynchronizationUtils.invokeAfterCompletion(
            transactionSynchronizations, TransactionSynchronization.STATUS_ROLLED_BACK);

        InOrder inOrder = inOrder(armingFixture.triggerLifecycleFacade);

        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerDisable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyTriggerEnable(inOrder, armingFixture, "workflow-1", workflowUuid);
        verifyNoMoreInteractions(armingFixture.triggerLifecycleFacade);
    }

    @Test
    void testRollbackUndoWhoseTransactionFailsIsLoggedAndNotThrown() {
        ArmingFixture armingFixture = new ArmingFixture();

        armingFixture.stubWorkflow("workflow-1", "trigger_1");

        when(armingFixture.projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
            .thenReturn(List.of(armingFixture.projectDeploymentWorkflow("workflow-1")));
        when(armingFixture.platformTransactionManager.getTransaction(any()))
            .thenThrow(new IllegalStateException("no connection"));

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        TransactionSynchronizationManager.initSynchronization();

        armingProjectDeploymentFacade.enableProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID, true);

        TransactionSynchronization transactionSynchronization = TransactionSynchronizationManager.getSynchronizations()
            .getFirst();

        assertThatCode(
            () -> transactionSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK))
                .doesNotThrowAnyException();
    }

    @Test
    void testDisableWorkflowWithoutATransactionStopsItsRunningJobsAtOnce() {
        ArmingFixture armingFixture = new ArmingFixture();

        armingFixture.stubWorkflow("workflow-1", "trigger_1");
        armingFixture.stubProjectDeploymentWorkflow(30L, "workflow-1");

        when(
            armingFixture.principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(100L)));

        ProjectDeploymentFacadeImpl armingProjectDeploymentFacade = armingFixture.projectDeploymentFacade();

        armingProjectDeploymentFacade.enableProjectDeploymentWorkflow(ARMED_PROJECT_DEPLOYMENT_ID, "workflow-1", false);

        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

        verify(armingFixture.jobFacade).stopJob(100L);
        verifyNoMoreInteractions(armingFixture.platformTransactionManager);
    }

    private static void verifyRunningJobsQueried(InOrder inOrder, ArmingFixture armingFixture, String workflowId) {
        inOrder.verify(armingFixture.principalJobService)
            .getJobIds(
                eq(Job.Status.STARTED), isNull(), isNull(), eq(List.of(ARMED_PROJECT_DEPLOYMENT_ID)),
                eq(PlatformType.AUTOMATION), eq(List.of(workflowId)), eq(false), eq(0));
    }

    private static void verifyTriggerDisable(
        InOrder inOrder, ArmingFixture armingFixture, String workflowId, UUID workflowUuid) {

        verifyTriggerDisable(inOrder, armingFixture, workflowId, workflowUuid, "trigger_1");
    }

    private static void verifyTriggerDisable(
        InOrder inOrder, ArmingFixture armingFixture, String workflowId, UUID workflowUuid, String triggerName) {

        inOrder.verify(armingFixture.triggerLifecycleFacade)
            .executeTriggerDisable(
                eq(workflowId), argThat(workflowExecutionIdOf(workflowUuid, triggerName)), eq(WEBHOOK_TRIGGER_TYPE),
                eq(Map.of("event", triggerName)), isNull());
    }

    private static void verifyTriggerEnableUndo(
        InOrder inOrder, ArmingFixture armingFixture, UUID workflowUuid, WebhookEnableOutput armedTriggerState,
        @Nullable Object previousTriggerState) {

        inOrder.verify(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnableUndo(
                eq("workflow-1"), argThat(workflowExecutionIdOf(workflowUuid, "trigger_1")), eq(WEBHOOK_TRIGGER_TYPE),
                eq(Map.of("event", "trigger_1")), isNull(), eq(armedTriggerState), eq(previousTriggerState));
    }

    private static void verifyTriggerEnable(
        InOrder inOrder, ArmingFixture armingFixture, String workflowId, UUID workflowUuid) {

        verifyTriggerEnable(inOrder, armingFixture, workflowId, workflowUuid, "trigger_1");
    }

    private static void verifyTriggerEnable(
        InOrder inOrder, ArmingFixture armingFixture, String workflowId, UUID workflowUuid, String triggerName) {

        String webhookUrl = "http://localhost/webhooks/" + WorkflowExecutionId.of(
            PlatformType.AUTOMATION, ARMED_PROJECT_DEPLOYMENT_ID, workflowUuid.toString(), triggerName);

        inOrder.verify(armingFixture.triggerLifecycleFacade)
            .executeTriggerEnable(
                eq(workflowId), argThat(workflowExecutionIdOf(workflowUuid, triggerName)), eq(WEBHOOK_TRIGGER_TYPE),
                eq(Map.of("event", triggerName)), isNull(), eq(webhookUrl),
                eq((long) Environment.DEVELOPMENT.ordinal()));
    }

    private record UpgradedWorkflows(
        UUID firstWorkflowUuid, UUID secondWorkflowUuid, List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {
    }

    private static ArgumentMatcher<WorkflowExecutionId> workflowExecutionIdOf(
        UUID workflowUuid, String triggerName) {

        String expectedWorkflowExecutionId = String.valueOf(
            WorkflowExecutionId.of(
                PlatformType.AUTOMATION, ARMED_PROJECT_DEPLOYMENT_ID, workflowUuid.toString(), triggerName));

        return workflowExecutionId -> workflowExecutionId != null
            && expectedWorkflowExecutionId.equals(workflowExecutionId.toString());
    }

    private final class ArmingFixture {

        private final ComponentConnectionFacade componentConnectionFacade = mock(ComponentConnectionFacade.class);
        private final Evaluator evaluator = mock(Evaluator.class);
        private final JobFacade jobFacade = mock(JobFacade.class);
        private final PlatformTransactionManager platformTransactionManager = mock(PlatformTransactionManager.class);
        private final PrincipalJobService principalJobService = mock(PrincipalJobService.class);
        private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
        private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
            mock(ProjectDeploymentWorkflowService.class);
        private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        private final TriggerLifecycleFacade triggerLifecycleFacade = mock(TriggerLifecycleFacade.class);
        private final TriggerStateService triggerStateService = mock(TriggerStateService.class);
        private final WorkflowService workflowService = mock(WorkflowService.class);

        private ArmingFixture() {
            ProjectDeployment projectDeployment = projectDeployment(
                ARMED_PROJECT_DEPLOYMENT_ID, PROJECT_ID, Environment.DEVELOPMENT);

            projectDeployment.setEnabled(true);

            lenient()
                .when(projectDeploymentService.getProjectDeployment(ARMED_PROJECT_DEPLOYMENT_ID))
                .thenReturn(projectDeployment);
            lenient()
                .when(evaluator.evaluate(anyMap(), anyMap(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0));
            lenient()
                .when(triggerLifecycleFacade.executeTriggerDisable(any(), any(), any(), any(), any()))
                .thenReturn(true);
            lenient()
                .when(
                    triggerLifecycleFacade.executeTriggerEnableUndo(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);
        }

        private ProjectDeploymentFacadeImpl projectDeploymentFacade() {
            when(applicationProperties.getWebhookUrl()).thenReturn("http://localhost/webhooks/{id}");

            return new ProjectDeploymentFacadeImpl(
                null, evaluator, null, null, principalJobService, jobFacade, null, projectDeploymentService,
                projectDeploymentWorkflowService, null, projectWorkflowService, null, null, null,
                triggerLifecycleFacade, applicationProperties, componentConnectionFacade, workflowService,
                platformTransactionManager, triggerStateService);
        }

        private ProjectDeploymentWorkflow projectDeploymentWorkflow(String workflowId) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setEnabled(true);
            projectDeploymentWorkflow.setInputs(Map.of());
            projectDeploymentWorkflow.setProjectDeploymentId(ARMED_PROJECT_DEPLOYMENT_ID);
            projectDeploymentWorkflow.setWorkflowId(workflowId);

            return projectDeploymentWorkflow;
        }

        private ProjectDeployment upgradedProjectDeployment() {
            ProjectDeployment projectDeployment = projectDeployment(
                ARMED_PROJECT_DEPLOYMENT_ID, PROJECT_ID, Environment.DEVELOPMENT);

            projectDeployment.setEnabled(true);
            projectDeployment.setProjectVersion(1);

            return projectDeployment;
        }

        private UpgradedWorkflows stubUpgradedWorkflows() {
            UUID firstWorkflowUuid = UUID.randomUUID();
            UUID secondWorkflowUuid = UUID.randomUUID();

            List<ProjectWorkflow> projectWorkflows = List.of(
                stubWorkflow("workflow-a-1", firstWorkflowUuid, 0, "trigger_1"),
                stubWorkflow("workflow-a-2", firstWorkflowUuid, 1, "trigger_1"),
                stubWorkflow("workflow-b-1", secondWorkflowUuid, 0, "trigger_1"),
                stubWorkflow("workflow-b-2", secondWorkflowUuid, 1, "trigger_1"));

            when(projectWorkflowService.getProjectWorkflows(PROJECT_ID)).thenReturn(projectWorkflows);
            when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(ARMED_PROJECT_DEPLOYMENT_ID))
                .thenReturn(
                    List.of(
                        projectDeploymentWorkflow(10L, "workflow-a-1"),
                        projectDeploymentWorkflow(11L, "workflow-b-1")));
            stubNoRunningJobs();

            stubProjectDeploymentWorkflow(10L, "workflow-a-1");
            stubProjectDeploymentWorkflow(10L, "workflow-a-2");
            stubProjectDeploymentWorkflow(11L, "workflow-b-1");
            stubProjectDeploymentWorkflow(11L, "workflow-b-2");

            return new UpgradedWorkflows(
                firstWorkflowUuid, secondWorkflowUuid,
                List.of(projectDeploymentWorkflow("workflow-a-2"), projectDeploymentWorkflow("workflow-b-2")));
        }

        private void stubNoRunningJobs() {
            lenient()
                .when(
                    principalJobService.getJobIds(
                        any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                .thenReturn(Page.empty());
        }

        private void stubProjectDeploymentWorkflow(long id, String workflowId) {
            when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(ARMED_PROJECT_DEPLOYMENT_ID, workflowId))
                .thenReturn(projectDeploymentWorkflow(id, workflowId));
        }

        private ProjectDeploymentWorkflow projectDeploymentWorkflow(long id, String workflowId) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflow(workflowId);

            projectDeploymentWorkflow.setId(id);

            return projectDeploymentWorkflow;
        }

        private void stubTriggerEnableOutput(WebhookEnableOutput webhookEnableOutput) {
            when(triggerLifecycleFacade.executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong()))
                .thenReturn(webhookEnableOutput);
        }

        private UUID stubWorkflow(String workflowId, String... triggerNames) {
            UUID workflowUuid = UUID.randomUUID();

            stubWorkflow(workflowId, workflowUuid, 1, triggerNames);

            return workflowUuid;
        }

        private ProjectWorkflow stubWorkflow(
            String workflowId, UUID workflowUuid, int projectVersion, String... triggerNames) {

            Workflow workflow = mock(Workflow.class);

            List<WorkflowTrigger> workflowTriggers = Arrays.stream(triggerNames)
                .map(triggerName -> new WorkflowTrigger(
                    Map.of(
                        "name", triggerName, "type", "webhook/v1/newEvent", "parameters",
                        Map.of("event", triggerName))))
                .toList();

            when(workflow.getId()).thenReturn(workflowId);
            when(workflow.getExtensions(WorkflowExtConstants.TRIGGERS, WorkflowTrigger.class, List.of()))
                .thenReturn(workflowTriggers);
            when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);

            ProjectWorkflow projectWorkflow = new ProjectWorkflow(
                PROJECT_ID, projectVersion, workflowId, workflowUuid);

            when(projectWorkflowService.getWorkflowProjectWorkflow(workflowId)).thenReturn(projectWorkflow);

            return projectWorkflow;
        }
    }

    private static ProjectDeployment projectDeployment(long id, long projectId, Environment environment) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setEnvironment(environment);
        projectDeployment.setProjectId(projectId);

        return projectDeployment;
    }
}
