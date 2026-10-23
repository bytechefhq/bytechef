/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ProvisionIntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@PreAuthorize("isTenantAdmin()")
public class IntegrationInstanceAdminFacadeImpl implements IntegrationInstanceAdminFacade {

    private final ConnectedUserIntegrationFacade connectedUserIntegrationFacade;
    private final IntegrationInstanceFacade integrationInstanceFacade;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceAdminFacadeImpl(
        ConnectedUserIntegrationFacade connectedUserIntegrationFacade,
        IntegrationInstanceFacade integrationInstanceFacade) {

        this.connectedUserIntegrationFacade = connectedUserIntegrationFacade;
        this.integrationInstanceFacade = integrationInstanceFacade;
    }

    @Override
    public void enableIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId, boolean enable) {
        integrationInstanceFacade.enableIntegrationInstanceWorkflow(integrationInstanceId, workflowId, enable);
    }

    @Override
    public List<ProvisionableIntegrationDTO> getProvisionableIntegrations(long connectedUserId) {
        return connectedUserIntegrationFacade.getProvisionableIntegrations(connectedUserId);
    }

    @Override
    public long provisionIntegrationInstance(
        long connectedUserId, long integrationInstanceConfigurationId, Map<String, Object> connectionParameters,
        List<ProvisionIntegrationInstanceWorkflowDTO> workflows) {

        return connectedUserIntegrationFacade.provisionIntegrationInstance(
            connectedUserId, integrationInstanceConfigurationId, connectionParameters, workflows);
    }
}
