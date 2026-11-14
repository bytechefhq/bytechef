/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.dto.CallableAutomationDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserMcpAutomationResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteConnectedUserMcpAutomationResolverClient implements ConnectedUserMcpAutomationResolver {

    @Override
    public Optional<CallableAutomationDTO> fetchCallableAutomation(
        long connectedUserId, long automationProjectConfigurationId, String workflowUuid) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> fetchConnectedUserProjectWorkflowId(
        long connectedUserId, long automationProjectConfigurationId, String workflowUuid) {

        throw new UnsupportedOperationException();
    }
}
