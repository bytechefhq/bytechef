/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectedUserConnectionReader {

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserService connectedUserService;
    private final ConnectionFacade connectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionReader(
        ConnectedUserConnectionMembership connectedUserConnectionMembership, ConnectedUserService connectedUserService,
        ConnectionFacade connectionFacade) {

        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserService = connectedUserService;
        this.connectionFacade = connectionFacade;
    }

    public List<ConnectionDTO> getConnections(
        long connectedUserId, @Nullable String componentName, List<Long> connectionIds) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Set<Long> entitledConnectionIds = connectedUserConnectionMembership.getConnectionIds(
            connectedUser.getId(), connectedUser.getEnvironment());

        List<Long> allowedConnectionIds = entitledConnectionIds.stream()
            .filter(connectionId -> connectionIds.isEmpty() || connectionIds.contains(connectionId))
            .toList();

        return connectionFacade.getConnections(new ArrayList<>(allowedConnectionIds), PlatformType.EMBEDDED)
            .stream()
            .filter(connectionDTO -> componentName == null || componentName.equals(connectionDTO.componentName()))
            .toList();
    }
}
