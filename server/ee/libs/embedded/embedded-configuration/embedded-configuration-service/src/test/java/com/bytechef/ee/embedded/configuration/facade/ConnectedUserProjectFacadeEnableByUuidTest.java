/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that {@link ConnectedUserProjectFacadeImpl#enableProjectWorkflow(String, String, boolean, Long)} -- the
 * String-uuid overload the public REST enable/disable endpoints route through -- branches on whether {@code
 * workflowUuid} matches one of the caller's automation-bridge reference rows the same way the {@code long}-typed
 * overload already does on a known row (see {@link ConnectedUserProjectFacadeReferenceRowTest}). Before this fix, the
 * String overload always resolved {@code workflowUuid} against the caller's own copy project only, so a reference uuid
 * never matched and surfaced a false WORKFLOW_NOT_FOUND instead of enabling/disabling the reference.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserProjectFacadeEnableByUuidTest {

    private static final String EXTERNAL_USER_ID = "ext-1";

    @Mock
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Mock
    private ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;

    @Mock
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Mock
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Test
    void testEnableProjectWorkflowOnReferenceUuidDelegatesToReferenceFacadeWithoutResolvingProjectDeployment() {
        ConnectedUserProjectFacadeImpl facade = facade();

        ConnectedUserProject connectedUserProject = new ConnectedUserProject(100L, 10L, 1L, 0);

        Mockito.when(
            connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
                EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(connectedUserProject);
        Mockito.when(
            connectedUserProjectWorkflowRepository.findByConnectedUserProjectIdAndCatalogWorkflowUuid(
                10L, "catalog-uuid-1"))
            .thenReturn(Optional.of(referenceRow()));

        facade.enableProjectWorkflow(EXTERNAL_USER_ID, "catalog-uuid-1", true, null);

        Mockito.verify(connectedUserCodeWorkflowReferenceFacade)
            .enableReference(EXTERNAL_USER_ID, "catalog-uuid-1", true, Environment.PRODUCTION);
        Mockito.verifyNoInteractions(projectDeploymentService);
        Mockito.verifyNoInteractions(projectDeploymentFacade);
        Mockito.verifyNoInteractions(projectWorkflowService);
    }

    @Test
    void testDisableProjectWorkflowOnReferenceUuidDelegatesToReferenceFacade() {
        ConnectedUserProjectFacadeImpl facade = facade();

        ConnectedUserProject connectedUserProject = new ConnectedUserProject(100L, 10L, 1L, 0);

        Mockito.when(
            connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
                EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(connectedUserProject);
        Mockito.when(
            connectedUserProjectWorkflowRepository.findByConnectedUserProjectIdAndCatalogWorkflowUuid(
                10L, "catalog-uuid-1"))
            .thenReturn(Optional.of(referenceRow()));

        facade.enableProjectWorkflow(EXTERNAL_USER_ID, "catalog-uuid-1", false, null);

        Mockito.verify(connectedUserCodeWorkflowReferenceFacade)
            .enableReference(EXTERNAL_USER_ID, "catalog-uuid-1", false, Environment.PRODUCTION);
    }

    /**
     * Regression pin: a copy-mode uuid (no matching reference row) must keep going through the pre-existing
     * project-deployment resolution, unchanged by the new reference branch.
     */
    @Test
    void testEnableProjectWorkflowOnCopyModeUuidKeepsResolvingThroughTheProjectDeployment() {
        ConnectedUserProjectFacadeImpl facade = facade();

        ConnectedUserProject connectedUserProject = new ConnectedUserProject(100L, 10L, 1L, 0);

        Mockito.when(
            connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
                EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(connectedUserProject);
        Mockito.when(
            connectedUserProjectWorkflowRepository.findByConnectedUserProjectIdAndCatalogWorkflowUuid(
                10L, "copy-uuid-1"))
            .thenReturn(Optional.empty());
        Mockito.when(projectDeploymentService.getProjectDeploymentId(1L, Environment.PRODUCTION))
            .thenReturn(500L);
        Mockito.when(projectWorkflowService.fetchProjectWorkflowWorkflowId(500L, "copy-uuid-1"))
            .thenReturn(Optional.of("copy-wf-1"));

        facade.enableProjectWorkflow(EXTERNAL_USER_ID, "copy-uuid-1", true, null);

        Mockito.verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(1L, "copy-wf-1", true, Environment.PRODUCTION);
        Mockito.verifyNoInteractions(connectedUserCodeWorkflowReferenceFacade);
    }

    private static ConnectedUserProjectWorkflow referenceRow() {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setId(1L);
        connectedUserProjectWorkflow.setConnectedUserProjectId(10L);
        connectedUserProjectWorkflow.setCatalogWorkflowUuid("catalog-uuid-1");

        return connectedUserProjectWorkflow;
    }

    private ConnectedUserProjectFacadeImpl facade() {
        return new ConnectedUserProjectFacadeImpl(
            null, null, null, connectedUserCodeWorkflowReferenceFacade, connectedUserProjectWorkflowManager,
            connectedUserProjectWorkflowRepository, null, null, null, null, null, null, null, null, null,
            projectDeploymentFacade, projectDeploymentService, null, null, null, null, projectWorkflowService, null,
            null, null, null);
    }
}
