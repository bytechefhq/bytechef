/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ProvisionIntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationInstanceAdminFacade {

    void enableIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId, boolean enable);

    List<ProvisionableIntegrationDTO> getProvisionableIntegrations(long connectedUserId);

    long provisionIntegrationInstance(
        long connectedUserId, long integrationInstanceConfigurationId, Map<String, Object> connectionParameters,
        List<ProvisionIntegrationInstanceWorkflowDTO> workflows);
}
