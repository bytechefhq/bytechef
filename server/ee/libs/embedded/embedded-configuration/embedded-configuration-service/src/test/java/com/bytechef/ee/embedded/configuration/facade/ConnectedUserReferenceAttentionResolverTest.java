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
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceAttentionResolver.ReferenceState;
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
    private WorkflowConnectionSlot workflowConnectionSlot;

    private ConnectedUserReferenceAttentionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ConnectedUserReferenceAttentionResolver(
            connectedUserReferenceDeploymentManager, workflowConnectionSlot);
    }

    @Test
    void testUnresolvableAutomationWorkflowProjectYieldsNoAttentionReasonButKeepsTheInputs() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

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
        when(connectedUserReferenceDeploymentManager.getLastPublishedVersion(500L))
            .thenThrow(new IllegalArgumentException("not published"));

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(1L);
        reference.setConnectedUserProjectId(10L);
        reference.setAutomationWorkflowUuid("automation-workflow-uuid");
        reference.setProjectDeploymentId(900L);

        ReferenceState referenceState = resolver.resolve(reference);

        assertThat(referenceState.attentionReason()).isNull();
        assertThat(referenceState.inputs()).isEqualTo(Map.of("channel", "#a"));
    }
}
