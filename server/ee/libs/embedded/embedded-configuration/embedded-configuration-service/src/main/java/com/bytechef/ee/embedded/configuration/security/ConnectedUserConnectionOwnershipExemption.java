/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.ee.embedded.configuration.repository.ConnectedUserConnectionRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.security.ConnectionOwnershipExemption;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    private final ConnectedUserConnectionRepository connectedUserConnectionRepository;
    private final IntegrationInstanceRepository integrationInstanceRepository;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionOwnershipExemption(
        ConnectedUserConnectionRepository connectedUserConnectionRepository,
        IntegrationInstanceRepository integrationInstanceRepository) {

        this.connectedUserConnectionRepository = connectedUserConnectionRepository;
        this.integrationInstanceRepository = integrationInstanceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isConnectedUserOwned(long connectionId) {
        return connectedUserConnectionRepository.existsByConnectionId(connectionId) ||
            integrationInstanceRepository.existsByConnectionId(connectionId);
    }
}
