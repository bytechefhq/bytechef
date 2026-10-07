/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.ReferenceResolution;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.RowSpec;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ConnectedUserReferenceDeploymentManagerTest {

    private static final String WORKFLOW_ID = "workflow-1";

    private final ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver =
        mock(ConnectedUserWorkflowConnectionResolver.class);
    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager =
        new ConnectedUserReferenceDeploymentManager(
            connectedUserWorkflowConnectionResolver, projectDeploymentFacade, projectDeploymentService,
            projectDeploymentWorkflowService, projectService, projectWorkflowService, workflowService);

    @Test
    void testResolveReferenceEnablesTheRowWhenConnectionsAndInputsAreComplete() {
        givenWorkflowInputs(new Workflow.Input("channel", "Channel", "string", true));
        givenResolvedConnections(new ResolvedWorkflowConnections(List.of(connection(7L)), List.of()));

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            101L, WORKFLOW_ID, true, Map.of(), List.of(), Map.of("channel", "general"));

        assertThat(resolution.rowSpec()
            .enabled()).isTrue();
        assertThat(resolution.rowSpec()
            .inputs()).isNull();
        assertThat(resolution.missingComponentName()).isNull();
        assertThat(resolution.missingInputName()).isNull();
    }

    @Test
    void testResolveReferenceKeepsTheRowDisabledWhenEnablingWasNotAskedFor() {
        givenWorkflowInputs();
        givenResolvedConnections(new ResolvedWorkflowConnections(List.of(connection(7L)), List.of()));

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            101L, WORKFLOW_ID, false, Map.of(), List.of(), Map.of());

        assertThat(resolution.rowSpec()
            .enabled()).isFalse();
    }

    @Test
    void testResolveReferenceKeepsTheRowDisabledWhenAConnectionIsMissing() {
        givenWorkflowInputs();
        givenResolvedConnections(new ResolvedWorkflowConnections(List.of(), List.of("slack")));

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            101L, WORKFLOW_ID, true, Map.of(), List.of(), Map.of());

        assertThat(resolution.rowSpec()
            .enabled()).isFalse();
        assertThat(resolution.missingComponentName()).isEqualTo("slack");
    }

    @Test
    void testResolveReferenceKeepsTheRowDisabledWhenARequiredInputIsMissing() {
        givenWorkflowInputs(new Workflow.Input("channel", "Channel", "string", true));
        givenResolvedConnections(new ResolvedWorkflowConnections(List.of(connection(7L)), List.of()));

        ReferenceResolution resolution = connectedUserReferenceDeploymentManager.resolveReference(
            101L, WORKFLOW_ID, true, Map.of(), List.of(), Map.of());

        assertThat(resolution.rowSpec()
            .enabled()).isFalse();
        assertThat(resolution.missingInputName()).isEqualTo("channel");
    }

    @Test
    void testFindMissingRequiredInputCountsABlankValueAsMissing() {
        givenWorkflowInputs(new Workflow.Input("channel", "Channel", "string", true));

        assertThat(connectedUserReferenceDeploymentManager.findMissingRequiredInput(
            WORKFLOW_ID, Map.of("channel", "  "))).isEqualTo("channel");
    }

    @Test
    void testFindMissingRequiredInputIgnoresOptionalInputs() {
        givenWorkflowInputs(
            new Workflow.Input("note", "Note", "string", false),
            new Workflow.Input("channel", "Channel", "string", true));

        Map<String, Object> inputs = new HashMap<>();

        inputs.put("channel", "general");
        inputs.put("note", null);

        assertThat(connectedUserReferenceDeploymentManager.findMissingRequiredInput(WORKFLOW_ID, inputs)).isNull();
    }

    @Test
    void testGetDeploymentNameIsDistinctPerEnvironment() {
        assertThat(connectedUserReferenceDeploymentManager.getDeploymentName("user-1", Environment.PRODUCTION))
            .isEqualTo("__EMBEDDED__user-1__PRODUCTION");
        assertThat(connectedUserReferenceDeploymentManager.getDeploymentName("user-1", Environment.DEVELOPMENT))
            .isEqualTo("__EMBEDDED__user-1__DEVELOPMENT");
    }

    @Test
    void testGetOrCreateDeploymentCreatesAnEnabledDeploymentAtTheLastPublishedVersion() {
        Project project = mock(Project.class);
        ProjectVersion projectVersion = mock(ProjectVersion.class);

        when(projectVersion.getVersion()).thenReturn(3);
        when(project.getLastPublishedProjectVersion()).thenReturn(projectVersion);
        when(projectService.getProject(500L)).thenReturn(project);
        when(projectDeploymentService.fetchProjectDeploymentByName(500L, "__EMBEDDED__user-1__PRODUCTION"))
            .thenReturn(Optional.empty());
        when(
            projectDeploymentFacade.createProjectDeployment(any(ProjectDeployment.class), eq(List.of()), eq(List.of())))
                .thenReturn(900L);

        long projectDeploymentId = connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            500L, "user-1", Environment.PRODUCTION);

        assertThat(projectDeploymentId).isEqualTo(900L);

        ArgumentCaptor<ProjectDeployment> projectDeploymentCaptor = ArgumentCaptor.forClass(ProjectDeployment.class);

        InOrder inOrder = inOrder(projectDeploymentFacade);

        inOrder.verify(projectDeploymentFacade)
            .createProjectDeployment(projectDeploymentCaptor.capture(), eq(List.of()), eq(List.of()));
        inOrder.verify(projectDeploymentFacade)
            .enableProjectDeployment(900L, true);

        ProjectDeployment projectDeployment = projectDeploymentCaptor.getValue();

        assertThat(projectDeployment.getProjectVersion()).isEqualTo(3);
        assertThat(projectDeployment.getProjectId()).isEqualTo(500L);
        assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(projectDeployment.getName()).isEqualTo("__EMBEDDED__user-1__PRODUCTION");
    }

    @Test
    void testGetOrCreateDeploymentReusesTheExistingDeployment() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(900L);

        when(projectDeploymentService.fetchProjectDeploymentByName(500L, "__EMBEDDED__user-1__PRODUCTION"))
            .thenReturn(Optional.of(projectDeployment));

        assertThat(connectedUserReferenceDeploymentManager.getOrCreateDeployment(
            500L, "user-1", Environment.PRODUCTION)).isEqualTo(900L);

        verify(projectDeploymentFacade, never())
            .createProjectDeployment(any(ProjectDeployment.class), eq(List.of()), eq(List.of()));
    }

    @Test
    void testGetLastPublishedVersionRejectsAnUnpublishedProject() {
        Project project = mock(Project.class);

        when(projectService.getProject(500L)).thenReturn(project);

        assertThatThrownBy(() -> connectedUserReferenceDeploymentManager.getLastPublishedVersion(500L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPutWorkflowsAtTheSameVersionRewritesAnEnabledRowAloneDisablingItFirst() {
        givenDeployment(1);
        givenWorkflowId(1, "uuid-1", "wf-1");

        ProjectDeploymentWorkflow existingRow = row(11L, "wf-1", true, Map.of("channel", "general"));
        ProjectDeploymentWorkflow disabledExistingRow = row(11L, "wf-1", false, Map.of("channel", "general"));

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(900L, "wf-1"))
            .thenReturn(Optional.of(existingRow));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(11L)).thenReturn(disabledExistingRow);

        RowSpec rowSpec = new RowSpec(
            new ResolvedWorkflowConnections(List.of(connection(7L)), List.of()), true, null);

        connectedUserReferenceDeploymentManager.putWorkflows(900L, 1, Map.of("uuid-1", rowSpec));

        InOrder inOrder = inOrder(projectDeploymentFacade, projectDeploymentWorkflowService);

        inOrder.verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(900L, "wf-1", false);

        ArgumentCaptor<ProjectDeploymentWorkflow> rowCaptor = ArgumentCaptor.forClass(ProjectDeploymentWorkflow.class);

        inOrder.verify(projectDeploymentWorkflowService)
            .update(rowCaptor.capture());
        inOrder.verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(900L, "wf-1", true);

        ProjectDeploymentWorkflow writtenRow = rowCaptor.getValue();

        assertThat(writtenRow).isSameAs(disabledExistingRow);
        assertThat(writtenRow.isEnabled()).isFalse();
        assertThat(writtenRow.getConnections()).containsExactly(connection(7L));
        assertThat(writtenRow.getInputs()).isEqualTo(Map.of("channel", "general"));

        verifyNoOtherRowIsTouched();
    }

    @Test
    void testPutWorkflowsAtTheSameVersionWritesADisabledRowWithoutTogglingTriggers() {
        givenDeployment(1);
        givenWorkflowId(1, "uuid-1", "wf-1");

        ProjectDeploymentWorkflow existingRow = row(11L, "wf-1", false, Map.of("channel", "general"));

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(900L, "wf-1"))
            .thenReturn(Optional.of(existingRow));

        RowSpec rowSpec = new RowSpec(
            new ResolvedWorkflowConnections(List.of(connection(7L)), List.of()), false, Map.of("channel", "random"));

        connectedUserReferenceDeploymentManager.putWorkflows(900L, 1, Map.of("uuid-1", rowSpec));

        verify(projectDeploymentWorkflowService).update(existingRow);

        assertThat(existingRow.isEnabled()).isFalse();
        assertThat(existingRow.getConnections()).containsExactly(connection(7L));
        assertThat(existingRow.getInputs()).isEqualTo(Map.of("channel", "random"));

        verify(projectDeploymentFacade, never())
            .enableProjectDeploymentWorkflow(anyLong(), anyString(), anyBoolean());
        verifyNoOtherRowIsTouched();
    }

    /**
     * Repeating an enable, or re-resolving to the same wiring, must not disable the row: disabling stops its running
     * jobs.
     */
    @Test
    void testPutWorkflowsAtTheSameVersionLeavesAnUnchangedRowAlone() {
        givenDeployment(1);
        givenWorkflowId(1, "uuid-1", "wf-1");

        ProjectDeploymentWorkflow existingRow = row(11L, "wf-1", true, Map.of("channel", "general"));

        existingRow.setConnections(List.of(connection(7L), connection(8L)));

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(900L, "wf-1"))
            .thenReturn(Optional.of(existingRow));

        RowSpec rowSpec = new RowSpec(
            new ResolvedWorkflowConnections(List.of(connection(8L), connection(7L)), List.of()), true,
            Map.of("channel", "general"));

        connectedUserReferenceDeploymentManager.putWorkflows(900L, 1, Map.of("uuid-1", rowSpec));

        verify(projectDeploymentFacade, never())
            .enableProjectDeploymentWorkflow(anyLong(), anyString(), anyBoolean());
        verify(projectDeploymentWorkflowService, never()).update(any(ProjectDeploymentWorkflow.class));
        verify(projectDeploymentWorkflowService, never()).create(any(ProjectDeploymentWorkflow.class));
        verifyNoOtherRowIsTouched();
    }

    @Test
    void testPutWorkflowsAtTheSameVersionCreatesANewRowDisabledThenEnablesIt() {
        givenDeployment(1);
        givenWorkflowId(1, "uuid-1", "wf-1");

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(900L, "wf-1"))
            .thenReturn(Optional.empty());

        RowSpec rowSpec = new RowSpec(
            new ResolvedWorkflowConnections(List.of(connection(7L)), List.of()), true, null);

        connectedUserReferenceDeploymentManager.putWorkflows(900L, 1, Map.of("uuid-1", rowSpec));

        ArgumentCaptor<ProjectDeploymentWorkflow> rowCaptor = ArgumentCaptor.forClass(ProjectDeploymentWorkflow.class);

        InOrder inOrder = inOrder(projectDeploymentFacade, projectDeploymentWorkflowService);

        inOrder.verify(projectDeploymentWorkflowService)
            .create(rowCaptor.capture());
        inOrder.verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(900L, "wf-1", true);

        ProjectDeploymentWorkflow createdRow = rowCaptor.getValue();

        assertThat(createdRow.isEnabled()).isFalse();
        assertThat(createdRow.getWorkflowId()).isEqualTo("wf-1");
        assertThat(createdRow.getProjectDeploymentId()).isEqualTo(900L);
        assertThat(createdRow.getConnections()).containsExactly(connection(7L));

        verify(projectDeploymentFacade, never())
            .enableProjectDeploymentWorkflow(900L, "wf-1", false);
        verifyNoOtherRowIsTouched();
    }

    @Test
    void testPutWorkflowsAtANewVersionWritesTheCompleteListAndDropsRowsNotPassed() {
        givenDeployment(1);

        ProjectDeploymentWorkflow otherRow = row(10L, "other-wf-1", true, Map.of());
        ProjectDeploymentWorkflow existingRow = row(11L, "wf-1", true, Map.of("channel", "general"));

        givenRows(Map.of("other-wf-1", "other-uuid", "wf-1", "uuid-1"), otherRow, existingRow);
        givenWorkflowId(2, "uuid-1", "wf-2");

        RowSpec rowSpec = new RowSpec(new ResolvedWorkflowConnections(List.of(), List.of()), false, null);

        connectedUserReferenceDeploymentManager.putWorkflows(900L, 2, Map.of("uuid-1", rowSpec));

        List<ProjectDeploymentWorkflow> writtenRows = captureWrittenRows(2);

        assertThat(writtenRows).singleElement()
            .satisfies(writtenRow -> {
                assertThat(writtenRow.getWorkflowId()).isEqualTo("wf-2");
                assertThat(writtenRow.getInputs()).isEqualTo(Map.of("channel", "general"));
            });

        verify(projectDeploymentWorkflowService, never()).update(any(ProjectDeploymentWorkflow.class));
        verify(projectDeploymentWorkflowService, never()).create(any(ProjectDeploymentWorkflow.class));
    }

    /**
     * {@code putWorkflow} would otherwise disable the row, save the incomplete inputs, then refuse to re-enable it with
     * a raw {@code IllegalArgumentException} from {@code ProjectDeploymentFacadeImpl}'s own input validation -- a
     * low-level exception the rest of the reference API never surfaces, on a row that is left disabled behind the
     * caller's back. Checking the missing input up front, before any write, keeps the row's old inputs and its enabled
     * state untouched.
     */
    @Test
    void testUpdateInputsRefusesAnEnabledRowMissingARequiredInput() {
        givenDeployment(1);
        givenWorkflowId(1, "uuid-1", WORKFLOW_ID);
        givenWorkflowInputs(new Workflow.Input("channel", "Channel", "string", true));

        ProjectDeploymentWorkflow existingRow = row(11L, WORKFLOW_ID, true, Map.of("channel", "general"));

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(900L, WORKFLOW_ID))
            .thenReturn(Optional.of(existingRow));

        assertThatThrownBy(() -> connectedUserReferenceDeploymentManager.updateInputs(900L, "uuid-1", Map.of()))
            .isInstanceOf(MissingInputException.class)
            .extracting("inputName")
            .isEqualTo("channel");

        verify(projectDeploymentWorkflowService, never()).update(any(ProjectDeploymentWorkflow.class));
        verify(projectDeploymentWorkflowService, never()).create(any(ProjectDeploymentWorkflow.class));
        verify(projectDeploymentFacade, never())
            .enableProjectDeploymentWorkflow(anyLong(), anyString(), anyBoolean());
    }

    @Test
    void testUpdateInputsSavesADisabledRowWithIncompleteInputs() {
        givenDeployment(1);
        givenWorkflowId(1, "uuid-1", "wf-1");

        ProjectDeploymentWorkflow existingRow = row(11L, "wf-1", false, Map.of("channel", "general"));

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(900L, "wf-1"))
            .thenReturn(Optional.of(existingRow));

        // A non-empty map: ProjectDeploymentWorkflow#setInputs silently ignores an empty one (see the class javadoc
        // on updateInputs), which would make this assertion pass regardless of whether the write actually happened.
        connectedUserReferenceDeploymentManager.updateInputs(900L, "uuid-1", Map.of("note", "partial"));

        verify(projectDeploymentWorkflowService).update(existingRow);

        assertThat(existingRow.getInputs()).isEqualTo(Map.of("note", "partial"));

        verify(projectDeploymentFacade, never())
            .enableProjectDeploymentWorkflow(anyLong(), anyString(), anyBoolean());
    }

    @Test
    void testRemoveWorkflowDisablesAndDeletesAnEnabledRowAloneKeepingTheDeployment() {
        givenDeployment(1);
        givenWorkflowId(1, "uuid-1", "wf-1");

        ProjectDeploymentWorkflow otherRow = row(10L, "other-wf-1", true, Map.of());
        ProjectDeploymentWorkflow existingRow = row(11L, "wf-1", true, Map.of());

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(900L, "wf-1"))
            .thenReturn(Optional.of(existingRow));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(900L)).thenReturn(List.of(otherRow));

        connectedUserReferenceDeploymentManager.removeWorkflow(900L, "uuid-1");

        InOrder inOrder = inOrder(projectDeploymentFacade, projectDeploymentWorkflowService);

        inOrder.verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(900L, "wf-1", false);
        inOrder.verify(projectDeploymentWorkflowService)
            .delete(11L);

        verify(projectDeploymentFacade, never()).enableProjectDeploymentWorkflow(900L, "other-wf-1", false);
        verify(projectDeploymentFacade, never()).deleteProjectDeployment(anyLong());
        verifyNoOtherRowIsTouched();
    }

    @Test
    void testRemoveWorkflowOfADisabledRowSkipsDisabling() {
        givenDeployment(1);
        givenWorkflowId(1, "uuid-1", "wf-1");

        ProjectDeploymentWorkflow existingRow = row(11L, "wf-1", false, Map.of());

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(900L, "wf-1"))
            .thenReturn(Optional.of(existingRow));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(900L))
            .thenReturn(List.of(row(10L, "other-wf-1", true, Map.of())));

        connectedUserReferenceDeploymentManager.removeWorkflow(900L, "uuid-1");

        verify(projectDeploymentWorkflowService).delete(11L);
        verify(projectDeploymentFacade, never())
            .enableProjectDeploymentWorkflow(anyLong(), anyString(), anyBoolean());
    }

    @Test
    void testRemoveWorkflowOfADeploymentThatNoLongerExistsDoesNothing() {
        when(projectDeploymentService.fetchProjectDeployment(900L)).thenReturn(Optional.empty());

        connectedUserReferenceDeploymentManager.removeWorkflow(900L, "uuid-1");

        verify(projectDeploymentService, never()).getProjectDeployment(anyLong());
        verify(projectDeploymentWorkflowService, never()).delete(anyLong());
        verify(projectDeploymentFacade, never()).deleteProjectDeployment(anyLong());
        verify(projectDeploymentFacade, never())
            .enableProjectDeploymentWorkflow(anyLong(), anyString(), anyBoolean());
    }

    @Test
    void testRemoveWorkflowDeletesTheDeploymentWithItsLastRow() {
        givenDeployment(1);
        givenWorkflowId(1, "uuid-1", "wf-1");

        ProjectDeploymentWorkflow existingRow = row(11L, "wf-1", true, Map.of());

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(900L, "wf-1"))
            .thenReturn(Optional.of(existingRow));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(900L)).thenReturn(List.of());

        connectedUserReferenceDeploymentManager.removeWorkflow(900L, "uuid-1");

        InOrder inOrder = inOrder(projectDeploymentFacade, projectDeploymentWorkflowService);

        inOrder.verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(900L, "wf-1", false);
        inOrder.verify(projectDeploymentWorkflowService)
            .delete(11L);
        inOrder.verify(projectDeploymentFacade)
            .deleteProjectDeployment(900L);

        verifyNoOtherRowIsTouched();
    }

    private List<ProjectDeploymentWorkflow> captureWrittenRows(int expectedProjectVersion) {
        ArgumentCaptor<ProjectDeployment> projectDeploymentCaptor = ArgumentCaptor.forClass(ProjectDeployment.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProjectDeploymentWorkflow>> rowsCaptor = ArgumentCaptor.forClass(List.class);

        verify(projectDeploymentFacade).updateProjectDeployment(
            projectDeploymentCaptor.capture(), rowsCaptor.capture(), eq(List.of()));

        ProjectDeployment projectDeployment = projectDeploymentCaptor.getValue();

        assertThat(projectDeployment.getProjectVersion()).isEqualTo(expectedProjectVersion);

        return rowsCaptor.getValue();
    }

    /**
     * The same-version paths never go through the full-list write, which is what would disable and re-enable every
     * other enabled row.
     */
    private void verifyNoOtherRowIsTouched() {
        verify(projectDeploymentFacade, never())
            .updateProjectDeployment(any(ProjectDeployment.class), anyList(), anyList());
    }

    private void givenDeployment(int projectVersion) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(900L);
        projectDeployment.setProjectId(500L);
        projectDeployment.setProjectVersion(projectVersion);

        when(projectDeploymentService.getProjectDeployment(900L)).thenReturn(projectDeployment);
        when(projectDeploymentService.fetchProjectDeployment(900L)).thenReturn(Optional.of(projectDeployment));
    }

    private void givenResolvedConnections(ResolvedWorkflowConnections resolvedWorkflowConnections) {
        when(connectedUserWorkflowConnectionResolver.resolve(WORKFLOW_ID, 101L, Map.of(), List.of()))
            .thenReturn(resolvedWorkflowConnections);
    }

    private void givenRows(Map<String, String> uuidsByWorkflowId, ProjectDeploymentWorkflow... rows) {
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(900L)).thenReturn(List.of(rows));

        for (Map.Entry<String, String> entry : uuidsByWorkflowId.entrySet()) {
            ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

            when(projectWorkflow.getUuidAsString()).thenReturn(entry.getValue());
            when(projectWorkflowService.getWorkflowProjectWorkflow(entry.getKey())).thenReturn(projectWorkflow);
        }
    }

    private void givenWorkflowId(int projectVersion, String workflowUuid, String workflowId) {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getWorkflowId()).thenReturn(workflowId);
        when(projectWorkflowService.fetchProjectWorkflow(500L, projectVersion, workflowUuid))
            .thenReturn(Optional.of(projectWorkflow));
    }

    private void givenWorkflowInputs(Workflow.Input... inputs) {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(inputs));
        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
    }

    private static ProjectDeploymentWorkflowConnection connection(long connectionId) {
        return new ProjectDeploymentWorkflowConnection(connectionId, "slack", "postMessage1");
    }

    private static ProjectDeploymentWorkflow row(
        long id, String workflowId, boolean enabled, Map<String, ?> inputs) {

        ProjectDeploymentWorkflow row = new ProjectDeploymentWorkflow();

        row.setEnabled(enabled);
        row.setId(id);
        row.setInputs(inputs);
        row.setProjectDeploymentId(900L);
        row.setWorkflowId(workflowId);

        return row;
    }
}
