/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationInstanceAdminFacade {

    void enableIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId, boolean enable);
}
