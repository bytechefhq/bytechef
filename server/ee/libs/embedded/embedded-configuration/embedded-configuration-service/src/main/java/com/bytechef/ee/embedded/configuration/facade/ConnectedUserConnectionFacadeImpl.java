/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
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

    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionFacade connectionFacade;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionFacadeImpl(
        ConnectedUserConnectionService connectedUserConnectionService, ConnectedUserService connectedUserService,
        ConnectionFacade connectionFacade, IntegrationInstanceService integrationInstanceService) {

        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectedUserService = connectedUserService;
        this.connectionFacade = connectionFacade;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    public long createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO) {
        long connectionId = connectionFacade.create(connectionDTO, PlatformType.EMBEDDED);

        connectedUserConnectionService.create(connectedUserId, connectionId);

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

        allConnectionIds.addAll(connectedUserConnectionService.getConnectionIds(connectedUser.getId()));

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
