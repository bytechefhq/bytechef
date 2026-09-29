/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentications;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
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

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionFacade connectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionFacadeImpl(
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserConnectionService connectedUserConnectionService, ConnectedUserService connectedUserService,
        ConnectionFacade connectionFacade) {

        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectedUserService = connectedUserService;
        this.connectionFacade = connectionFacade;
    }

    @Override
    public long createConnectedUserConnection(
        long connectedUserId, @Nullable Long requestedEnvironmentId, ConnectionDTO connectionDTO) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        validateConnectedUserAccess(connectedUser);

        long environmentId = ConnectedUserAuthentications.fetchCurrent()
            .map(ConnectedUserAuthentication::environmentId)
            .orElse(connectedUser.getEnvironmentId());

        if (requestedEnvironmentId != null && requestedEnvironmentId != environmentId) {
            throw new AccessDeniedException(
                "A connected user's connection belongs to the connected user's environment " + environmentId);
        }

        ConnectionDTO environmentConnectionDTO = ConnectionDTO.builder(connectionDTO)
            .environmentId(Math.toIntExact(environmentId))
            .build();

        long connectionId = connectionFacade.create(environmentConnectionDTO, PlatformType.EMBEDDED);

        connectedUserConnectionService.create(connectedUserId, connectionId);

        return connectionId;
    }

    @Override
    public List<ConnectionDTO> getConnectedUserConnections(long connectedUserId, @Nullable String componentName) {
        validateConnectedUserAccess(connectedUserService.getConnectedUser(connectedUserId));

        return getConnections(connectedUserId, componentName);
    }

    @Override
    public List<ConnectionDTO> getConnections(Long connectedUserId, @Nullable String componentName) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Set<Long> ownedConnectionIds = connectedUserConnectionMembership.getOwnedConnectionIds(
            connectedUser.getId(), connectedUser.getEnvironment());

        return connectionFacade.getConnections(new ArrayList<>(ownedConnectionIds), PlatformType.EMBEDDED)
            .stream()
            .filter(connectionDTO -> componentName == null || componentName.equals(connectionDTO.componentName()))
            .toList();
    }

    private void validateConnectedUserAccess(ConnectedUser connectedUser) {
        if (SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)) {
            return;
        }

        Optional<ConnectedUserAuthentication> connectedUserAuthentication = ConnectedUserAuthentications
            .fetchCurrent();

        boolean connectedUserMatches = connectedUserAuthentication
            .filter(
                authentication -> Objects.equals(authentication.externalUserId(), connectedUser.getExternalId()) &&
                    authentication.environmentId() == connectedUser.getEnvironmentId())
            .isPresent();

        if (!connectedUserMatches) {
            throw new AccessDeniedException(
                "Connected user " + connectedUser.getId() + " does not belong to the current user");
        }
    }
}
