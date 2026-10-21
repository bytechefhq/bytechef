/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.ee.embedded.configuration.repository.ConnectedUserConnectionRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.security.ConnectionOwnershipExemption;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentications;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectedUserConnectionOwnershipExemption implements ConnectionOwnershipExemption {

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserConnectionRepository connectedUserConnectionRepository;
    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceRepository integrationInstanceRepository;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionOwnershipExemption(
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserConnectionRepository connectedUserConnectionRepository,
        ConnectedUserService connectedUserService, IntegrationInstanceRepository integrationInstanceRepository) {

        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserConnectionRepository = connectedUserConnectionRepository;
        this.connectedUserService = connectedUserService;
        this.integrationInstanceRepository = integrationInstanceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isConnectedUserOwned(long connectionId) {
        return connectedUserConnectionRepository.existsByConnectionId(connectionId) ||
            integrationInstanceRepository.existsByConnectionId(connectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwnedByCurrentConnectedUser(long connectionId) {
        return ConnectedUserAuthentications.fetchCurrent()
            .map(connectedUserAuthentication -> isOwnedBy(connectedUserAuthentication, connectionId))
            .orElse(false);
    }

    private boolean isOwnedBy(ConnectedUserAuthentication connectedUserAuthentication, long connectionId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            connectedUserAuthentication.connectedUserId());

        Set<Long> ownedConnectionIds = connectedUserConnectionMembership.getOwnedConnectionIds(
            connectedUser.getId(), connectedUser.getEnvironment());

        return ownedConnectionIds.contains(connectionId);
    }
}
