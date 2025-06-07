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
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
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
            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(instanceId);

            connectionId = integrationInstance.getConnectionId();
        }

        return connectionId;
    }
}
