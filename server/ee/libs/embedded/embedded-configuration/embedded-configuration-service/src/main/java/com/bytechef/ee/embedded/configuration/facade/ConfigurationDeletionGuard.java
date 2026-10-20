/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ConfigurationDeletionGuard {

    private static final String CONFIGURATION_TYPE = "IntegrationInstanceConfiguration";
    private static final String INSTANCE_TYPE = "IntegrationInstance";

    private ConfigurationDeletionGuard() {
    }

    static void assertMayDelete(
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations,
        IntegrationInstanceService integrationInstanceService, PermissionService permissionService) {

        for (IntegrationInstanceConfiguration integrationInstanceConfiguration : integrationInstanceConfigurations) {
            long configurationId = integrationInstanceConfiguration.getId();

            assertScope(permissionService, configurationId, CONFIGURATION_TYPE,
                "INTEGRATION_INSTANCE_CONFIGURATION_DELETE");

            List<IntegrationInstance> integrationInstances = integrationInstanceService.getIntegrationInstances(
                configurationId);

            for (IntegrationInstance integrationInstance : integrationInstances) {
                assertScope(permissionService, integrationInstance.getId(), INSTANCE_TYPE, "CONNECTED_USER_DELETE");

                if (integrationInstanceConfiguration.isEnabled() && integrationInstance.isEnabled()) {
                    assertScope(permissionService, integrationInstance.getId(), INSTANCE_TYPE, "CONNECTED_USER_MANAGE");
                }
            }
        }
    }

    private static void assertScope(PermissionService permissionService, long id, String type, String scope) {
        if (!permissionService.hasResourceScope(id, type, scope)) {
            throw new AccessDeniedException("Access Denied");
        }
    }
}
