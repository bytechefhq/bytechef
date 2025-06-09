/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceDTO;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationInstanceFacade {

    void enableIntegrationInstance(long integrationInstanceId, boolean enable);

    void enableIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId, boolean enable);

    void enableIntegrationInstanceWorkflowTriggers(long integrationInstanceId, boolean enable);

    long enableIntegrationInstanceWorkflowTriggers(
        long integrationInstanceId, String workflowId, boolean enable);

    IntegrationInstanceDTO getIntegrationInstance(long id);

    void updateIntegrationInstanceWorkflow(
        long integrationInstanceId, String workflowId, Map<String, Object> inputs);
}
