/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceAttentionResolver.ReferenceState;
import com.bytechef.platform.configuration.domain.ComponentConnection;
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
    private WorkflowConnectionSlots workflowConnectionSlots;

    private ConnectedUserReferenceAttentionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ConnectedUserReferenceAttentionResolver(
            connectedUserReferenceDeploymentManager, workflowConnectionSlots);
    }

    @Test
    void testDanglingReferenceHasNoInputsAndNoAttentionReason() {
        ConnectedUserProjectWorkflow reference = reference();

        reference.setDangling(true);

        ReferenceState referenceState = resolver.resolve(reference);

        assertThat(referenceState.inputs()).isEmpty();
        assertThat(referenceState.attentionReason()).isNull();

        verifyNoInteractions(connectedUserReferenceDeploymentManager);
    }

    @Test
    void testDeploymentBehindPublishedVersionIsUpdatePendingAndKeepsItsInputs() {
        stubDeploymentWithRow(1, List.of(), Map.of("channel", "#a"));
        when(connectedUserReferenceDeploymentManager.getLastPublishedVersion(500L)).thenReturn(2);

        ReferenceState referenceState = resolver.resolve(reference());

        assertThat(referenceState.attentionReason()).isEqualTo("UPDATE_PENDING");
        assertThat(referenceState.inputs()).isEqualTo(Map.of("channel", "#a"));
    }

    @Test
    void testRequiredSlotWithoutConnectionIsMissingConnection() {
        stubCurrentDeploymentWithRow(List.of(), Map.of());
        when(workflowConnectionSlots.getSlots("automation-workflow-1"))
            .thenReturn(List.of(new ComponentConnection("slack", 1, "postMessage1", "slack", true)));

        assertThat(resolver.resolve(reference())
            .attentionReason()).isEqualTo("MISSING_CONNECTION:slack");
    }

    @Test
    void testMissingRequiredInputIsInputRequired() {
        stubCurrentDeploymentWithRow(
            List.of(new ProjectDeploymentWorkflowConnection(11L, "slack", "postMessage1")), Map.of());
        when(workflowConnectionSlots.getSlots("automation-workflow-1"))
            .thenReturn(List.of(new ComponentConnection("slack", 1, "postMessage1", "slack", true)));
        when(connectedUserReferenceDeploymentManager.findMissingRequiredInput("automation-workflow-1", Map.of()))
            .thenReturn("channel");

        assertThat(resolver.resolve(reference())
            .attentionReason()).isEqualTo("INPUT_REQUIRED:channel");
    }

    @Test
    void testUnresolvableAutomationWorkflowProjectYieldsNoAttentionReasonButKeepsTheInputs() {
        stubDeploymentWithRow(1, List.of(), Map.of("channel", "#a"));
        when(connectedUserReferenceDeploymentManager.getLastPublishedVersion(500L))
            .thenThrow(new IllegalArgumentException("not published"));

        ReferenceState referenceState = resolver.resolve(reference());

        assertThat(referenceState.attentionReason()).isNull();
        assertThat(referenceState.inputs()).isEqualTo(Map.of("channel", "#a"));
    }

    @Test
    void testMissingDeploymentHasNoInputsAndNoAttentionReason() {
        when(connectedUserReferenceDeploymentManager.fetchDeployment(900L)).thenReturn(Optional.empty());

        ReferenceState referenceState = resolver.resolve(reference());

        assertThat(referenceState.inputs()).isEmpty();
        assertThat(referenceState.attentionReason()).isNull();
    }

    @Test
    void testCompleteReferenceHasNoAttentionReason() {
        stubCurrentDeploymentWithRow(
            List.of(new ProjectDeploymentWorkflowConnection(11L, "slack", "postMessage1")), Map.of("channel", "#a"));
        when(workflowConnectionSlots.getSlots("automation-workflow-1"))
            .thenReturn(List.of(new ComponentConnection("slack", 1, "postMessage1", "slack", true)));

        ReferenceState referenceState = resolver.resolve(reference());

        assertThat(referenceState.attentionReason()).isNull();
        assertThat(referenceState.inputs()).isEqualTo(Map.of("channel", "#a"));
    }

    private static ConnectedUserProjectWorkflow reference() {
        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(1L);
        reference.setConnectedUserProjectId(10L);
        reference.setAutomationWorkflowUuid("automation-workflow-uuid");
        reference.setProjectDeploymentId(900L);

        return reference;
    }

    private void stubCurrentDeploymentWithRow(
        List<ProjectDeploymentWorkflowConnection> connections, Map<String, ?> inputs) {

        stubDeploymentWithRow(2, connections, inputs);
        when(connectedUserReferenceDeploymentManager.getLastPublishedVersion(500L)).thenReturn(2);
    }

    private void stubDeploymentWithRow(
        int version, List<ProjectDeploymentWorkflowConnection> connections, Map<String, ?> inputs) {

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(900L);
        projectDeployment.setProjectId(500L);
        projectDeployment.setProjectVersion(version);

        when(connectedUserReferenceDeploymentManager.fetchDeployment(900L)).thenReturn(Optional.of(projectDeployment));
        when(connectedUserReferenceDeploymentManager.fetchWorkflowId(500L, version, "automation-workflow-uuid"))
            .thenReturn(Optional.of("automation-workflow-1"));

        ProjectDeploymentWorkflow row = new ProjectDeploymentWorkflow();

        row.setConnections(connections);
        row.setInputs(inputs);

        when(connectedUserReferenceDeploymentManager.fetchWorkflowRow(900L, "automation-workflow-1"))
            .thenReturn(Optional.of(row));
    }
}
