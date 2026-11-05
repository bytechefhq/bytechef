/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope.embedded;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The integration permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum IntegrationPermissionScope implements PermissionScopeType {

    INTEGRATION_CREATE,
    INTEGRATION_PUBLISH,
    INTEGRATION_SETTINGS,
    INTEGRATION_DELETE
}
