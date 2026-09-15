/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The execution-domain permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ExecutionPermissionScope implements PermissionScopeType {

    EXECUTION_VIEW,
    EXECUTION_DELETE
}
