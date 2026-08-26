/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SyncResult;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentPromoterTest {

    private static final UUID WORKFLOW_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final ProjectDeploymentPromoter promoter = new ProjectDeploymentPromoter(
        projectDeploymentFacade, projectDeploymentWorkflowService, projectService, projectWorkflowService,
        workflowService);

    @Test
    void testValidatePromotableRejectsUnpublishedProject() {
        Project project = new Project();

        when(projectService.getProject(1L)).thenReturn(project);

        assertThatThrownBy(() -> promoter.validatePromotable(1L, 1)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testValidatePromotableRejectsDraftVersion() {
        Project project = publishedProjectWithLastVersion(3);

        when(projectService.getProject(1L)).thenReturn(project);

        assertThatThrownBy(() -> promoter.validatePromotable(1L, 3)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testSyncOnNewTargetCopiesSourceBindingsThroughMappingsAndReportsUnresolved() {
        ProjectDeployment source = deployment(1L, 100L, 2, Environment.DEVELOPMENT);
        ProjectDeployment target = deployment(2L, 100L, 2, Environment.STAGING);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(100L, 2, "wf-v2", WORKFLOW_UUID);

        when(projectWorkflowService.getProjectWorkflows(100L, 2)).thenReturn(List.of(projectWorkflow));

        ProjectDeploymentWorkflow sourceProjectDeploymentWorkflow = projectDeploymentWorkflow(
            10L, 1L, "wf-v2", true, Map.of("k", "v"),
            List.of(
                new ProjectDeploymentWorkflowConnection(500L, "slack", "sendMessage_1"),
                new ProjectDeploymentWorkflowConnection(501L, "github", "createIssue_1")));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(1L))
            .thenReturn(List.of(sourceProjectDeploymentWorkflow));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(2L))
            .thenReturn(List.of())
            .thenReturn(List.of(projectDeploymentWorkflow(20L, 2L, "wf-v2", true, Map.of(), List.of())));

        SyncResult syncResult = promoter.sync(source, target, Map.of(500L, 600L), Map.of(), true);

        ArgumentCaptor<List<ProjectDeploymentWorkflow>> captor = ArgumentCaptor.captor();

        verify(projectDeploymentFacade).updateProjectDeployment(eq(target), captor.capture(), eq(List.of()));

        List<ProjectDeploymentWorkflow> syncedProjectDeploymentWorkflows = captor.getValue();

        ProjectDeploymentWorkflow synced = syncedProjectDeploymentWorkflows.getFirst();

        assertThat(synced.getWorkflowId()).isEqualTo("wf-v2");
        assertThat(synced.isEnabled()).isTrue();
        assertThat(synced.getInputs()).extractingByKey("k")
            .isEqualTo("v");
        assertThat(synced.getConnections())
            .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactly(600L);
        assertThat(syncResult.unresolvedConnectionIds()).containsExactly(501L);
        assertThat(syncResult.workflowIdMapping()).containsExactly(Map.entry(10L, 20L));
        assertThat(syncResult.warnings()).isEmpty();
        assertThat(target.getProjectVersion()).isEqualTo(2);
    }

    @Test
    void testSyncOnExistingTargetKeepsTargetBindingsInputsAndEnabled() {
        ProjectDeployment source = deployment(1L, 100L, 3, Environment.DEVELOPMENT);
        ProjectDeployment target = deployment(2L, 100L, 2, Environment.STAGING);

        when(projectWorkflowService.getProjectWorkflows(100L, 3))
            .thenReturn(List.of(new ProjectWorkflow(100L, 3, "wf-v3", WORKFLOW_UUID)));
        when(projectWorkflowService.getProjectWorkflows(100L, 2))
            .thenReturn(List.of(new ProjectWorkflow(100L, 2, "wf-v2", WORKFLOW_UUID)));

        ProjectDeploymentWorkflow sourceProjectDeploymentWorkflow = projectDeploymentWorkflow(
            10L, 1L, "wf-v3", true, Map.of("k", "source"),
            List.of(new ProjectDeploymentWorkflowConnection(500L, "slack", "sendMessage_1")));
        ProjectDeploymentWorkflow targetProjectDeploymentWorkflow = projectDeploymentWorkflow(
            20L, 2L, "wf-v2", false, Map.of("k", "target"),
            List.of(new ProjectDeploymentWorkflowConnection(650L, "slack", "sendMessage_1")));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(1L))
            .thenReturn(List.of(sourceProjectDeploymentWorkflow));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(2L))
            .thenReturn(List.of(targetProjectDeploymentWorkflow));

        SyncResult syncResult = promoter.sync(source, target, Map.of(), Map.of(500L, 600L), false);

        ArgumentCaptor<List<ProjectDeploymentWorkflow>> captor = ArgumentCaptor.captor();

        verify(projectDeploymentFacade).updateProjectDeployment(eq(target), captor.capture(), eq(List.of()));

        List<ProjectDeploymentWorkflow> syncedProjectDeploymentWorkflows = captor.getValue();

        ProjectDeploymentWorkflow synced = syncedProjectDeploymentWorkflows.getFirst();

        assertThat(synced.getWorkflowId()).isEqualTo("wf-v3");
        assertThat(synced.isEnabled()).isFalse();
        assertThat(synced.getInputs()).extractingByKey("k")
            .isEqualTo("target");
        assertThat(synced.getConnections())
            .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactly(650L);
        assertThat(syncResult.unresolvedConnectionIds()).isEmpty();
        assertThat(syncResult.warnings()).isEmpty();
        assertThat(target.getProjectVersion()).isEqualTo(3);
    }

    @Test
    void testSyncSkipsWorkflowsMissingFromTheNewVersionWithWarning() {
        ProjectDeployment source = deployment(1L, 100L, 3, Environment.DEVELOPMENT);
        ProjectDeployment target = deployment(2L, 100L, 2, Environment.STAGING);

        when(projectWorkflowService.getProjectWorkflows(100L, 3)).thenReturn(List.of());
        when(projectWorkflowService.getProjectWorkflows(100L, 2))
            .thenReturn(List.of(new ProjectWorkflow(100L, 2, "wf-removed", WORKFLOW_UUID)));

        ProjectDeploymentWorkflow sourceProjectDeploymentWorkflow = projectDeploymentWorkflow(
            10L, 1L, "wf-removed", true, Map.of(),
            List.of(new ProjectDeploymentWorkflowConnection(500L, "slack", "sendMessage_1")));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(1L))
            .thenReturn(List.of(sourceProjectDeploymentWorkflow));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(2L)).thenReturn(List.of());

        Workflow removedWorkflow = workflow("Removed");

        when(workflowService.getWorkflow("wf-removed")).thenReturn(removedWorkflow);

        SyncResult syncResult = promoter.sync(source, target, Map.of(500L, 600L), Map.of(), false);

        ArgumentCaptor<List<ProjectDeploymentWorkflow>> captor = ArgumentCaptor.captor();

        verify(projectDeploymentFacade).updateProjectDeployment(eq(target), captor.capture(), eq(List.of()));

        assertThat(captor.getValue()).isEmpty();
        assertThat(syncResult.warnings()).containsExactly("Workflow Removed no longer exists in version 3 and "
            + "was skipped");
        assertThat(syncResult.unresolvedConnectionIds()).isEmpty();
        assertThat(syncResult.workflowIdMapping()).isEmpty();
    }

    @Test
    void testExistingTargetBindingsCorrelatesByUuidNodeAndKey() {
        ProjectDeployment source = deployment(1L, 100L, 3, Environment.DEVELOPMENT);
        ProjectDeployment target = deployment(2L, 100L, 2, Environment.STAGING);

        when(projectWorkflowService.getProjectWorkflows(100L, 3))
            .thenReturn(List.of(new ProjectWorkflow(100L, 3, "wf-v3", WORKFLOW_UUID)));
        when(projectWorkflowService.getProjectWorkflows(100L, 2))
            .thenReturn(List.of(new ProjectWorkflow(100L, 2, "wf-v2", WORKFLOW_UUID)));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(1L))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(
                        10L, 1L, "wf-v3", true, Map.of(),
                        List.of(
                            new ProjectDeploymentWorkflowConnection(500L, "slack", "sendMessage_1"),
                            new ProjectDeploymentWorkflowConnection(501L, "slack", "sendMessage_2")))));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(2L))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(
                        20L, 2L, "wf-v2", true, Map.of(),
                        List.of(
                            new ProjectDeploymentWorkflowConnection(650L, "slack", "sendMessage_1"),
                            new ProjectDeploymentWorkflowConnection(651L, "slack", "sendMessage_9")))));

        Map<Long, Long> existingTargetBindings = promoter.existingTargetBindings(source, target);

        assertThat(existingTargetBindings).containsExactly(Map.entry(500L, 650L));
    }

    @Test
    void testCollectSourceBindingsIncludesWorkflowLabel() {
        ProjectDeployment source = deployment(1L, 100L, 2, Environment.DEVELOPMENT);

        when(projectWorkflowService.getProjectWorkflows(100L, 2))
            .thenReturn(List.of(new ProjectWorkflow(100L, 2, "wf-v2", WORKFLOW_UUID)));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(1L))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(
                        10L, 1L, "wf-v2", true, Map.of(),
                        List.of(new ProjectDeploymentWorkflowConnection(500L, "slack", "sendMessage_1")))));

        Workflow notifyWorkflow = workflow("Notify");

        when(workflowService.getWorkflow("wf-v2")).thenReturn(notifyWorkflow);

        List<SourceBinding> sourceBindings = promoter.collectSourceBindings(source);

        assertThat(sourceBindings).containsExactly(
            new SourceBinding(WORKFLOW_UUID.toString(), "Notify", "sendMessage_1", "slack", 500L));
    }

    @Test
    void testSyncResolvesExistingTargetBindingsPerNodeRatherThanPerConnection() {
        ProjectDeployment source = deployment(1L, 100L, 3, Environment.DEVELOPMENT);
        ProjectDeployment target = deployment(2L, 100L, 2, Environment.STAGING);

        when(projectWorkflowService.getProjectWorkflows(100L, 3))
            .thenReturn(List.of(new ProjectWorkflow(100L, 3, "wf-v3", WORKFLOW_UUID)));
        when(projectWorkflowService.getProjectWorkflows(100L, 2))
            .thenReturn(List.of(new ProjectWorkflow(100L, 2, "wf-v2", WORKFLOW_UUID)));

        // One source connection (500L) bound at TWO nodes, which the target wired to two DIFFERENT connections.
        // Collapsing the target's bindings to a per-source-connection map loses one of them and silently re-points
        // that node at the other node's connection.
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(1L))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(
                        10L, 1L, "wf-v3", true, Map.of(),
                        List.of(
                            new ProjectDeploymentWorkflowConnection(500L, "slack", "sendMessage_1"),
                            new ProjectDeploymentWorkflowConnection(500L, "slack", "sendMessage_2")))));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(2L))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(
                        20L, 2L, "wf-v2", true, Map.of(),
                        List.of(
                            new ProjectDeploymentWorkflowConnection(650L, "slack", "sendMessage_1"),
                            new ProjectDeploymentWorkflowConnection(651L, "slack", "sendMessage_2")))));

        SyncResult syncResult = promoter.sync(source, target, Map.of(), Map.of(), false);

        ArgumentCaptor<List<ProjectDeploymentWorkflow>> captor = ArgumentCaptor.captor();

        verify(projectDeploymentFacade).updateProjectDeployment(eq(target), captor.capture(), eq(List.of()));

        List<ProjectDeploymentWorkflow> syncedProjectDeploymentWorkflows = captor.getValue();

        ProjectDeploymentWorkflow synced = syncedProjectDeploymentWorkflows.getFirst();

        // Each node keeps the connection the TARGET environment wired at that node.
        assertThat(synced.getConnections())
            .extracting(
                ProjectDeploymentWorkflowConnection::getWorkflowNodeName,
                ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactlyInAnyOrder(tuple("sendMessage_1", 650L), tuple("sendMessage_2", 651L));
        assertThat(syncResult.unresolvedConnectionIds()).isEmpty();
    }

    /**
     * Pins the reason the per-node key is a record and not a joined string. Under the old
     * {@code workflowUuid + "|" + nodeName + "|" + key} scheme these two bindings produce the SAME key —
     * {@code <uuid>|a|b|c} — from {@code (node "a", key "b|c")} and {@code (node "a|b", key "c")}, so one node would be
     * resolved to the other's connection. Node names are user-editable and the platform's only node-name rule rejects a
     * {@code __} prefix, so a {@code |} is reachable, not theoretical.
     */
    @Test
    void testSyncResolvesNodesWhoseNamesWouldCollideUnderAJoinedStringKey() {
        ProjectDeployment source = deployment(1L, 100L, 3, Environment.DEVELOPMENT);
        ProjectDeployment target = deployment(2L, 100L, 2, Environment.STAGING);

        when(projectWorkflowService.getProjectWorkflows(100L, 3))
            .thenReturn(List.of(new ProjectWorkflow(100L, 3, "wf-v3", WORKFLOW_UUID)));
        when(projectWorkflowService.getProjectWorkflows(100L, 2))
            .thenReturn(List.of(new ProjectWorkflow(100L, 2, "wf-v2", WORKFLOW_UUID)));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(1L))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(
                        10L, 1L, "wf-v3", true, Map.of(),
                        List.of(
                            new ProjectDeploymentWorkflowConnection(500L, "b|c", "a"),
                            new ProjectDeploymentWorkflowConnection(501L, "c", "a|b")))));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(2L))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(
                        20L, 2L, "wf-v2", true, Map.of(),
                        List.of(
                            new ProjectDeploymentWorkflowConnection(650L, "b|c", "a"),
                            new ProjectDeploymentWorkflowConnection(651L, "c", "a|b")))));

        SyncResult syncResult = promoter.sync(source, target, Map.of(), Map.of(), false);

        ArgumentCaptor<List<ProjectDeploymentWorkflow>> captor = ArgumentCaptor.captor();

        verify(projectDeploymentFacade).updateProjectDeployment(eq(target), captor.capture(), eq(List.of()));

        List<ProjectDeploymentWorkflow> syncedProjectDeploymentWorkflows = captor.getValue();

        ProjectDeploymentWorkflow synced = syncedProjectDeploymentWorkflows.getFirst();

        assertThat(synced.getConnections())
            .extracting(
                ProjectDeploymentWorkflowConnection::getWorkflowNodeName,
                ProjectDeploymentWorkflowConnection::getWorkflowConnectionKey,
                ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactlyInAnyOrder(tuple("a", "b|c", 650L), tuple("a|b", "c", 651L));
        assertThat(syncResult.unresolvedConnectionIds()).isEmpty();
    }

    @Test
    void testSyncOnExistingTargetTakesSourceInputsAndEnabledForAWorkflowNewToTheTarget() {
        ProjectDeployment source = deployment(1L, 100L, 3, Environment.DEVELOPMENT);
        ProjectDeployment target = deployment(2L, 100L, 2, Environment.STAGING);

        UUID addedWorkflowUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // "wf-added-v3" is new in version 3: the target deployment has no row for its lineage uuid.
        when(projectWorkflowService.getProjectWorkflows(100L, 3))
            .thenReturn(
                List.of(
                    new ProjectWorkflow(100L, 3, "wf-v3", WORKFLOW_UUID),
                    new ProjectWorkflow(100L, 3, "wf-added-v3", addedWorkflowUuid)));
        when(projectWorkflowService.getProjectWorkflows(100L, 2))
            .thenReturn(List.of(new ProjectWorkflow(100L, 2, "wf-v2", WORKFLOW_UUID)));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(1L))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(
                        10L, 1L, "wf-v3", false, Map.of("k", "source"), List.of()),
                    projectDeploymentWorkflow(
                        11L, 1L, "wf-added-v3", true, Map.of("added", "fromSource"),
                        List.of(new ProjectDeploymentWorkflowConnection(500L, "slack", "sendMessage_1")))));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(2L))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(20L, 2L, "wf-v2", true, Map.of("k", "target"), List.of())));

        SyncResult syncResult = promoter.sync(source, target, Map.of(), Map.of(500L, 600L), false);

        ArgumentCaptor<List<ProjectDeploymentWorkflow>> captor = ArgumentCaptor.captor();

        verify(projectDeploymentFacade).updateProjectDeployment(eq(target), captor.capture(), eq(List.of()));

        ProjectDeploymentWorkflow addedWorkflow = captor.getValue()
            .stream()
            .filter(projectDeploymentWorkflow -> "wf-added-v3".equals(projectDeploymentWorkflow.getWorkflowId()))
            .findFirst()
            .orElseThrow();

        // No counterpart row in the target, so the source's configuration is adopted wholesale even on the update
        // path — the workflow that DOES have a counterpart still keeps the target's, proving the two are independent.
        assertThat(addedWorkflow.isEnabled()).isTrue();
        assertThat(addedWorkflow.getInputs()).extractingByKey("added")
            .isEqualTo("fromSource");
        assertThat(addedWorkflow.getConnections())
            .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactly(600L);

        ProjectDeploymentWorkflow existingWorkflow = captor.getValue()
            .stream()
            .filter(projectDeploymentWorkflow -> "wf-v3".equals(projectDeploymentWorkflow.getWorkflowId()))
            .findFirst()
            .orElseThrow();

        assertThat(existingWorkflow.isEnabled()).isTrue();
        assertThat(existingWorkflow.getInputs()).extractingByKey("k")
            .isEqualTo("target");
        assertThat(syncResult.warnings()).isEmpty();
    }

    /**
     * The single most important property of this class: every deployment-workflow write goes through
     * {@link ProjectDeploymentFacade}, because that is what performs the lineage-uuid pivot that keeps
     * {@code api_collection_endpoint} / {@code mcp_project_workflow} / {@code a2a_project_workflow} rows pointing at
     * live pdw ids. A direct write through {@link ProjectDeploymentWorkflowService} would bypass the pivot and strand
     * every mapping row, so the suite pins that none of its mutators is ever called.
     */
    @Test
    void testSyncNeverWritesProjectDeploymentWorkflowsBehindTheFacade() {
        ProjectDeployment source = deployment(1L, 100L, 3, Environment.DEVELOPMENT);
        ProjectDeployment target = deployment(2L, 100L, 2, Environment.STAGING);

        when(projectWorkflowService.getProjectWorkflows(100L, 3))
            .thenReturn(List.of(new ProjectWorkflow(100L, 3, "wf-v3", WORKFLOW_UUID)));
        when(projectWorkflowService.getProjectWorkflows(100L, 2))
            .thenReturn(List.of(new ProjectWorkflow(100L, 2, "wf-v2", WORKFLOW_UUID)));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(1L))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(
                        10L, 1L, "wf-v3", true, Map.of("k", "source"),
                        List.of(new ProjectDeploymentWorkflowConnection(500L, "slack", "sendMessage_1")))));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(2L))
            .thenReturn(
                List.of(
                    projectDeploymentWorkflow(
                        20L, 2L, "wf-v2", false, Map.of("k", "target"),
                        List.of(new ProjectDeploymentWorkflowConnection(650L, "slack", "sendMessage_1")))));

        promoter.sync(source, target, Map.of(), Map.of(), false);

        verify(projectDeploymentFacade).updateProjectDeployment(eq(target), anyList(), eq(List.of()));

        verify(projectDeploymentWorkflowService, never()).create(any(ProjectDeploymentWorkflow.class));
        verify(projectDeploymentWorkflowService, never()).create(anyList());
        verify(projectDeploymentWorkflowService, never()).update(any(ProjectDeploymentWorkflow.class));
        verify(projectDeploymentWorkflowService, never()).update(anyList());
        verify(projectDeploymentWorkflowService, never()).delete(anyLong());
        verify(projectDeploymentWorkflowService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    private static ProjectDeployment deployment(long id, long projectId, int projectVersion, Environment environment) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setEnvironment(environment);

        return projectDeployment;
    }

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(
        long id, long projectDeploymentId, String workflowId, boolean enabled, Map<String, ?> inputs,
        List<ProjectDeploymentWorkflowConnection> connections) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(id);
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(workflowId);
        projectDeploymentWorkflow.setEnabled(enabled);
        projectDeploymentWorkflow.setInputs(inputs);
        projectDeploymentWorkflow.setConnections(connections);

        return projectDeploymentWorkflow;
    }

    /**
     * A fresh {@link Project} starts on a single DRAFT version 1, and {@code publish} flips the current maximum version
     * to PUBLISHED while appending the next DRAFT — so publishing {@code lastVersion - 1} times leaves the project
     * published with {@code lastVersion} as its draft.
     */
    private static Project publishedProjectWithLastVersion(int lastVersion) {
        Project project = new Project();

        for (int version = 1; version < lastVersion; version++) {
            project.publish("v" + version);
        }

        return project;
    }

    private static Workflow workflow(String label) {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn(label);

        return workflow;
    }
}
