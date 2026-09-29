/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ConnectedUserProjectWorkflowManagerTest {

    private static final String DEFINITION = """
        {
            "label": "Workflow",
            "triggers": [],
            "tasks": [
                {
                    "name": "slack_1",
                    "type": "slack/v1/sendMessage"
                }
            ]
        }
        """;

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership =
        mock(ConnectedUserConnectionMembership.class);
    private final ConnectedUserProjectService connectedUserProjectService = mock(ConnectedUserProjectService.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final ProjectWorkflowFacade projectWorkflowFacade = mock(ProjectWorkflowFacade.class);
    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade =
        mock(WorkflowTestConfigurationFacade.class);

    private ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;

    @BeforeEach
    void beforeEach() {
        connectedUserProjectWorkflowManager = new ConnectedUserProjectWorkflowManager(
            connectedUserConnectionMembership, connectedUserProjectService,
            mock(ConnectedUserProjectWorkflowService.class), mock(ConnectedUserService.class), connectionService,
            mock(ProjectService.class), projectWorkflowFacade, mock(ProjectWorkflowService.class),
            mock(WorkflowService.class), workflowTestConfigurationFacade);

        when(connectedUserProjectService.fetchConnectUserProject("external-user-1", Environment.DEVELOPMENT))
            .thenReturn(Optional.of(new ConnectedUserProject(1L, 3L, 4L, 0)));

        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getId()).thenReturn(5L);
        when(projectWorkflow.getWorkflowId()).thenReturn("workflow-1");
        when(projectWorkflow.getUuidAsString()).thenReturn("uuid-1");

        when(projectWorkflowFacade.addWorkflow(4L, DEFINITION)).thenReturn(projectWorkflow);

        lenient().when(connectionService.getConnections(PlatformType.EMBEDDED))
            .thenReturn(List.of(getConnection(99L)));
    }

    @Test
    void testCreateProjectWorkflowBindsTheConnectedUsersOwnConnectionFirst() {
        Set<Long> ownedConnectionIds = new LinkedHashSet<>(List.of(20L, 30L));

        when(connectedUserConnectionMembership.getOwnedConnectionIds(1L, Environment.DEVELOPMENT))
            .thenReturn(ownedConnectionIds);
        when(connectionService.getConnections(List.of(20L, 30L)))
            .thenReturn(List.of(getConnection(30L), getConnection(20L)));

        connectedUserProjectWorkflowManager.createProjectWorkflow(
            "external-user-1", DEFINITION, Environment.DEVELOPMENT);

        verify(workflowTestConfigurationFacade).saveWorkflowTestConfigurationConnection(
            "workflow-1", "slack_1", "slack", 20L, Environment.DEVELOPMENT.ordinal());
    }

    @Test
    void testCreateProjectWorkflowNeverBindsAnotherConnectedUsersConnection() {
        when(connectedUserConnectionMembership.getOwnedConnectionIds(1L, Environment.DEVELOPMENT))
            .thenReturn(Set.of());

        connectedUserProjectWorkflowManager.createProjectWorkflow(
            "external-user-1", DEFINITION, Environment.DEVELOPMENT);

        verify(workflowTestConfigurationFacade, never()).saveWorkflowTestConfigurationConnection(
            anyString(), anyString(), anyString(), anyLong(), anyLong());
    }

    private static Connection getConnection(long id) {
        Connection connection = Connection.builder()
            .componentName("slack")
            .name("Slack " + id)
            .type(PlatformType.EMBEDDED)
            .build();

        connection.setId(id);

        return connection;
    }
}
