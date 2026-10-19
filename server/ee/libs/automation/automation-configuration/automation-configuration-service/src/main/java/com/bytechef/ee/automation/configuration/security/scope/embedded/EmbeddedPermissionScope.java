/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope.embedded;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The embedded workspace permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum EmbeddedPermissionScope implements PermissionScopeType {

    EMBEDDED_VIEW,
    EMBEDDED_MEMBER_MANAGE,
    EMBEDDED_SETTINGS_MANAGE
}
