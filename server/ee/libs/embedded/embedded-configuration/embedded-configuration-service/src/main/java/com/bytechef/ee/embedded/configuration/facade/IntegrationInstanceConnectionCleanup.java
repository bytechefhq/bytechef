/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationInstanceConnectionCleanup {

    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectionFacade connectionFacade;
    private final ConnectionService connectionService;
    private final IntegrationInstanceService integrationInstanceService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConnectionCleanup(
        ConnectedUserConnectionService connectedUserConnectionService, ConnectionFacade connectionFacade,
        ConnectionService connectionService, IntegrationInstanceService integrationInstanceService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectionFacade = connectionFacade;
        this.connectionService = connectionService;
        this.integrationInstanceService = integrationInstanceService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    public void deleteOrKeepForConnectedUser(long connectedUserId, long connectionId) {
        if (connectedUserConnectionService.existsByConnectionId(connectionId) ||
            integrationInstanceService.existsByConnectionId(connectionId)) {

            return;
        }

        if (connectionFacade.isConnectionUsed(connectionId) ||
            projectDeploymentWorkflowService.isConnectionUsed(connectionId)) {

            connectedUserConnectionService.create(connectedUserId, connectionId);
        } else {
            connectionService.delete(connectionId);
        }
    }
}
