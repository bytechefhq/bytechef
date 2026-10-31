/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope.embedded;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The integration instance configuration permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum IntegrationInstanceConfigurationPermissionScope implements PermissionScopeType {

    INTEGRATION_INSTANCE_CONFIGURATION_VIEW,
    INTEGRATION_INSTANCE_CONFIGURATION_CREATE,
    INTEGRATION_INSTANCE_CONFIGURATION_EDIT,
    INTEGRATION_INSTANCE_CONFIGURATION_DELETE
}
