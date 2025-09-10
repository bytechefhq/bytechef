/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserIntegrationInstanceFacade {

    void disableIntegrationInstanceWorkflow(String externalUserId, long id, String workflowUuid);

    void enableIntegrationInstanceWorkflow(String externalUserId, long id, String workflowUuid);

    void updateIntegrationInstanceWorkflow(
        String externalUserId, long id, String workflowUuid, Map<String, Object> inputs);
}
