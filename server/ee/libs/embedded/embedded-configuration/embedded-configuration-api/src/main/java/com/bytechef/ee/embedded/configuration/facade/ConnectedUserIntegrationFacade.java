/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionIntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserIntegrationFacade {

    IntegrationInstance createIntegrationInstance(
        String externalUserId, long integrationId, Map<String, Object> connectionParameters, Environment environment);

    void deleteIntegrationInstance(String externalUserId, long integrationInstanceId);

    ConnectedUserIntegrationDTO getConnectedUserIntegration(
        String externalUserId, long integrationId, boolean enabled, Environment environment);

    List<ConnectedUserIntegrationDTO> getConnectedUserIntegrations(
        String externalUserId, boolean enabled, Environment environment);

    List<ProvisionableIntegrationDTO> getProvisionableIntegrations(long connectedUserId);

    /**
     * Provisions an integration instance for a connected user and enables the given workflows.
     *
     * @param connectedUserId                    the connected user id
     * @param integrationInstanceConfigurationId the integration instance configuration to provision onto
     * @param connectionParameters               the connection parameters
     * @param workflows                          the workflows to enable with their inputs
     * @return the id of the new integration instance
     */
    long provisionIntegrationInstance(
        long connectedUserId, long integrationInstanceConfigurationId, Map<String, Object> connectionParameters,
        List<ProvisionIntegrationInstanceWorkflowDTO> workflows);
}
