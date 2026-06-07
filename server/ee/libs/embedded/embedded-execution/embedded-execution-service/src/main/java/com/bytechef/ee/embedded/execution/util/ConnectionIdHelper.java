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

    public Long getConnectionId(String externalUserid, String componentName, Long instanceId, Environment environment) {
        Long connectionId;

        if (instanceId == null) {
            ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserid, environment);

            connectionId = integrationInstanceService
                .fetchIntegrationInstance(connectedUser.getId(), componentName, environment)
                .map(IntegrationInstance::getConnectionId)
                .orElse(null);
        } else {
            ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserid, environment);

            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(instanceId);

            Long connectedUserId = connectedUser.getId();

            if (!connectedUserId.equals(integrationInstance.getConnectedUserId())) {
                throw new AccessDeniedException(
                    "Integration instance " + instanceId + " is not owned by the connected user");
            }

            connectionId = integrationInstance.getConnectionId();
        }

        return connectionId;
    }
}
