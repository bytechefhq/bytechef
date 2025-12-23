/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ConnectedUserConnectionFacadeImpl implements ConnectedUserConnectionFacade {

    private final ConnectedUserService connectedUserService;
    private final ConnectionFacade connectionFacade;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionFacadeImpl(
        ConnectedUserService connectedUserService, ConnectionFacade connectionFacade,
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        IntegrationInstanceService integrationInstanceService, ProjectWorkflowService projectWorkflowService) {

        this.connectedUserService = connectedUserService;
        this.connectionFacade = connectionFacade;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public long createConnectedUserProjectWorkflowConnection(
        long connectedUserId, String workflowUuid, ConnectionDTO connectionDTO) {

        long connectionId = connectionFacade.create(connectionDTO, PlatformType.EMBEDDED);

        ConnectedUserProject connectedUserProject = connectedUserProjectService.getConnectedUserConnectedUserProject(
            connectedUserId);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getLastProjectWorkflow(
            connectedUserProject.getProjectId(), workflowUuid);

        connectedUserProjectWorkflowService.addConnection(
            connectedUserProject.getId(), projectWorkflow.getId(), connectionId);

        return connectionId;
    }

    @Override
    public List<ConnectionDTO> getConnections(
        Long connectedUserId, String componentName, List<Long> connectionIds) {

        List<Long> allConnectionIds = new ArrayList<>();

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        allConnectionIds.addAll(
            integrationInstanceService
                .getIntegrationInstances(connectedUser.getId(), componentName, connectedUser.getEnvironment())
                .stream()
                .map(IntegrationInstance::getConnectionId)
                .toList());

        ConnectedUserProject connectedUserProject = connectedUserProjectService.getConnectUserProject(
            connectedUser.getExternalId(), connectedUser.getEnvironment());

        List<ConnectedUserProjectWorkflow> connectedUserProjectWorkflows = connectedUserProjectWorkflowService
            .getConnectedUserProjectWorkflows(connectedUserProject.getId());

        allConnectionIds.addAll(
            connectedUserProjectWorkflows.stream()
                .flatMap(connectedUserProjectWorkflow -> CollectionUtils.stream(
                    connectedUserProjectWorkflow.getConnections()))
                .map(ConnectedUserProjectWorkflowConnection::getConnectionId)
                .toList());

        List<ConnectionDTO> connectionDTOs = new ArrayList<>(
            connectionFacade.getConnections(allConnectionIds, PlatformType.EMBEDDED));

        connectionDTOs.addAll(
            connectionIds.stream()
                .map(connectionFacade::getConnection)
                .toList());

        return connectionDTOs
            .stream()
            .filter(connectionDTO -> componentName.equals(connectionDTO.componentName()))
            .toList();
    }
}
