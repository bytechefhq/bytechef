/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashSet;
import java.util.List;
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
public class ConnectedUserConnectionMembership {

    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionMembership(
        ConnectedUserConnectionService connectedUserConnectionService,
        IntegrationInstanceService integrationInstanceService) {

        this.connectedUserConnectionService = connectedUserConnectionService;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Transactional(readOnly = true)
    public Set<Long> getOwnedConnectionIds(long connectedUserId, Environment environment) {
        Set<Long> connectionIds = new LinkedHashSet<>();

        List<IntegrationInstance> integrationInstances =
            integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId, environment);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            connectionIds.add(integrationInstance.getConnectionId());
        }

        connectionIds.addAll(connectedUserConnectionService.getConnectionIds(connectedUserId));

        return connectionIds;
    }
}
