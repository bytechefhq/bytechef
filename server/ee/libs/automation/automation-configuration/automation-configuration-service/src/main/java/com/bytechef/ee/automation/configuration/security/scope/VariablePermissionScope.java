/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The custom-variable permission scopes.
 *
 * @version ee
 */
public enum VariablePermissionScope implements PermissionScopeType {

    VARIABLE_VIEW,
    VARIABLE_MANAGE
}
