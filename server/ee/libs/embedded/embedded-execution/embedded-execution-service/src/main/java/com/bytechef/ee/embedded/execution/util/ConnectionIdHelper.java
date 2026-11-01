/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.util;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectionIdHelper {

    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public ConnectionIdHelper(
        ConnectedUserService connectedUserService, IntegrationInstanceService integrationInstanceService) {

        this.connectedUserService = connectedUserService;
        this.integrationInstanceService = integrationInstanceService;
    }

    public ConnectedUserConnection getConnectedUserConnection(
        String externalUserId, String componentName, @Nullable Long instanceId, Environment environment) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        long connectedUserId = Objects.requireNonNull(connectedUser.getId());

        IntegrationInstance integrationInstance;

        if (instanceId == null) {
            integrationInstance = integrationInstanceService
                .fetchIntegrationInstance(connectedUserId, componentName, environment)
                .orElse(null);
        } else {
            integrationInstance = integrationInstanceService.getIntegrationInstance(instanceId);

            if (!Objects.equals(connectedUserId, integrationInstance.getConnectedUserId())) {
                throw new AccessDeniedException(
                    "Integration instance " + instanceId + " is not owned by the connected user");
            }
        }

        if (integrationInstance == null) {
            return new ConnectedUserConnection(connectedUserId, null, null);
        }

        return new ConnectedUserConnection(
            connectedUserId, integrationInstance.getId(), integrationInstance.getConnectionId());
    }

    public record ConnectedUserConnection(
        long connectedUserId, @Nullable Long integrationInstanceId, @Nullable Long connectionId) {
    }
}
