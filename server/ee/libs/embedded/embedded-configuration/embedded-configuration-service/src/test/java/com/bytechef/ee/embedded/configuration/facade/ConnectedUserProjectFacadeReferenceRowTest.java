/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that {@link ConnectedUserProjectFacadeImpl#deleteProjectWorkflow(long)} and
 * {@link ConnectedUserProjectFacadeImpl#enableProjectWorkflow(long, boolean)} branch to
 * {@link ConnectedUserCodeWorkflowReferenceFacade} for a reference row instead of resolving it through
 * {@link ProjectWorkflowService#getProjectWorkflow(long)} -- a reference row's {@code projectWorkflowId} is null
 * ({@code catalogWorkflowUuid} is set instead), so passing it straight into that {@code long}-typed overload would NPE
 * on auto-unboxing.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserProjectFacadeReferenceRowTest {

    @Mock
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Mock
    private ConnectedUserProjectService connectUserProjectService;

    @Mock
    private ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;

    @Mock
    private ConnectedUserService connectedUserService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    private ConnectedUserProjectFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new ConnectedUserProjectFacadeImpl(
            null, null, connectUserProjectService, connectedUserCodeWorkflowReferenceFacade, null, null,
            connectedUserProjectWorkflowService, connectedUserService, null, null, null, null, null, null, null, null,
            null, null, null, null, null, projectWorkflowService, null, null, null, null, null);
    }

    @Test
    void testDeleteProjectWorkflowOnReferenceRowDelegatesToReferenceFacadeWithoutResolvingProjectWorkflow() {
        ConnectedUserProjectWorkflow referenceRow = referenceRow();

        Mockito.when(connectedUserProjectWorkflowService.getConnectedUserProjectWorkflow(1L))
            .thenReturn(referenceRow);

        ConnectedUserProject connectedUserProject = connectedUserProject();

        Mockito.when(connectUserProjectService.getConnectedUserProject(10L))
            .thenReturn(connectedUserProject);

        ConnectedUser connectedUser = connectedUser();

        Mockito.when(connectedUserService.getConnectedUser(100L))
            .thenReturn(connectedUser);

        facade.deleteProjectWorkflow(1L);

        Mockito.verify(connectedUserCodeWorkflowReferenceFacade)
            .deleteReference("ext-1", "catalog-uuid", Environment.PRODUCTION);
        Mockito.verifyNoInteractions(projectWorkflowService);
    }

    @Test
    void testEnableProjectWorkflowOnReferenceRowDelegatesToReferenceFacadeWithoutResolvingProjectWorkflow() {
        ConnectedUserProjectWorkflow referenceRow = referenceRow();

        Mockito.when(connectedUserProjectWorkflowService.getConnectedUserProjectWorkflow(1L))
            .thenReturn(referenceRow);

        ConnectedUserProject connectedUserProject = connectedUserProject();

        Mockito.when(connectUserProjectService.getConnectedUserProject(10L))
            .thenReturn(connectedUserProject);

        ConnectedUser connectedUser = connectedUser();

        Mockito.when(connectedUserService.getConnectedUser(100L))
            .thenReturn(connectedUser);

        facade.enableProjectWorkflow(1L, false);

        Mockito.verify(connectedUserCodeWorkflowReferenceFacade)
            .enableReference("ext-1", "catalog-uuid", false, Environment.PRODUCTION);
        Mockito.verifyNoInteractions(projectWorkflowService);
    }

    private static ConnectedUserProjectWorkflow referenceRow() {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setId(1L);
        connectedUserProjectWorkflow.setConnectedUserProjectId(10L);
        connectedUserProjectWorkflow.setCatalogWorkflowUuid("catalog-uuid");

        return connectedUserProjectWorkflow;
    }

    private static ConnectedUserProject connectedUserProject() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(10L);
        connectedUserProject.setConnectedUserId(100L);

        return connectedUserProject;
    }

    private static ConnectedUser connectedUser() {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setExternalId("ext-1");
        connectedUser.setEnvironment(Environment.PRODUCTION);

        return connectedUser;
    }
}
