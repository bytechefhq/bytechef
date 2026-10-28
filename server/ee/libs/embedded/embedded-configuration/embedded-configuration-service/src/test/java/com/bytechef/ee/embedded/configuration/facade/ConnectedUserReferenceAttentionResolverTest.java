/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceAttentionResolver.ReferenceState;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserReferenceAttentionResolverTest {

    @Mock
    private ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;

    @Mock
    private ConnectedUserWorkflowValidator connectedUserWorkflowValidator;

    @Mock
    private WorkflowConnectionSlot workflowConnectionSlot;

    private ConnectedUserReferenceAttentionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ConnectedUserReferenceAttentionResolver(
            connectedUserReferenceDeploymentManager, connectedUserWorkflowValidator, workflowConnectionSlot);
    }

    @Test
    void testUnresolvableAutomationWorkflowProjectYieldsNoAttentionReasonButKeepsTheInputs() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setId(900L);
        projectDeployment.setProjectId(500L);
        projectDeployment.setProjectVersion(1);

        ProjectDeploymentWorkflow row = new ProjectDeploymentWorkflow();

        row.setConnections(List.of());
        row.setInputs(Map.of("channel", "#a"));

        when(connectedUserReferenceDeploymentManager.fetchDeployment(900L)).thenReturn(Optional.of(projectDeployment));
        when(connectedUserReferenceDeploymentManager.fetchWorkflowId(500L, 1, "automation-workflow-uuid"))
            .thenReturn(Optional.of("automation-workflow-1"));
        when(connectedUserReferenceDeploymentManager.fetchWorkflowRow(900L, "automation-workflow-1"))
            .thenReturn(Optional.of(row));
        stubAdministrativelyEnabled(true);

        when(connectedUserReferenceDeploymentManager.getReleasedVersion(500L, Environment.PRODUCTION))
            .thenThrow(new ConfigurationException("not released",
                ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED));

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(1L);
        reference.setConnectedUserProjectId(10L);
        reference.setAutomationWorkflowUuid("automation-workflow-uuid");
        reference.setProjectDeploymentId(900L);

        ReferenceState referenceState = resolver.resolve(reference);

        assertThat(referenceState.attentionReason()).isNull();
        assertThat(referenceState.inputs()).isEqualTo(Map.of("channel", "#a"));
    }

    @Test
    void testUpdatePendingWhenDeploymentVersionDiffersFromThePinnedVersion() {
        ProjectDeployment projectDeployment = stubReference(1);

        stubAdministrativelyEnabled(true);

        when(connectedUserReferenceDeploymentManager.getReleasedVersion(500L, Environment.PRODUCTION)).thenReturn(2);

        ReferenceState referenceState = resolver.resolve(newReference());

        assertThat(referenceState.attentionReason()).isEqualTo(ConnectedUserReferenceAttentionResolver.UPDATE_PENDING);
        assertThat(projectDeployment.getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testUpdatePendingWhenTheDeploymentIsAheadOfAnOlderPinnedVersion() {
        stubReference(2);
        stubAdministrativelyEnabled(true);

        when(connectedUserReferenceDeploymentManager.getReleasedVersion(500L, Environment.PRODUCTION)).thenReturn(1);

        ReferenceState referenceState = resolver.resolve(newReference());

        assertThat(referenceState.attentionReason()).isEqualTo(ConnectedUserReferenceAttentionResolver.UPDATE_PENDING);
    }

    @Test
    void testNoUpdatePendingWhenLastPublishedIsNewerButNotPinned() {
        stubReference(1);
        stubAdministrativelyEnabled(true);

        when(connectedUserReferenceDeploymentManager.getReleasedVersion(500L, Environment.PRODUCTION)).thenReturn(1);

        ReferenceState referenceState = resolver.resolve(newReference());

        assertThat(referenceState.attentionReason()).isNull();
    }

    @Test
    void testTurnedOffByAdministratorWhenConfigurationIsDisabled() {
        stubReference(1);
        stubAdministrativelyEnabled(false);

        ReferenceState referenceState = resolver.resolve(newReference());

        assertThat(referenceState.attentionReason())
            .isEqualTo(ConnectedUserReferenceAttentionResolver.TURNED_OFF_BY_ADMINISTRATOR);
    }

    @Test
    void testTurnedOffByAdministratorWinsOverUpdatePending() {
        stubReference(1);
        stubAdministrativelyEnabled(false);

        ReferenceState referenceState = resolver.resolve(newReference());

        assertThat(referenceState.attentionReason())
            .isEqualTo(ConnectedUserReferenceAttentionResolver.TURNED_OFF_BY_ADMINISTRATOR);
    }

    private void stubAdministrativelyEnabled(boolean administrativelyEnabled) {
        when(connectedUserReferenceDeploymentManager.isAdministrativelyEnabled(
            500L, Environment.PRODUCTION, "automation-workflow-uuid")).thenReturn(administrativelyEnabled);
    }

    private ProjectDeployment stubReference(int projectVersion) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setId(900L);
        projectDeployment.setProjectId(500L);
        projectDeployment.setProjectVersion(projectVersion);

        when(connectedUserReferenceDeploymentManager.fetchDeployment(900L)).thenReturn(Optional.of(projectDeployment));
        when(connectedUserReferenceDeploymentManager.fetchWorkflowId(500L, projectVersion, "automation-workflow-uuid"))
            .thenReturn(Optional.empty());

        return projectDeployment;
    }

    private static ConnectedUserProjectWorkflow newReference() {
        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(1L);
        reference.setConnectedUserProjectId(10L);
        reference.setAutomationWorkflowUuid("automation-workflow-uuid");
        reference.setProjectDeploymentId(900L);

        return reference;
    }
}
