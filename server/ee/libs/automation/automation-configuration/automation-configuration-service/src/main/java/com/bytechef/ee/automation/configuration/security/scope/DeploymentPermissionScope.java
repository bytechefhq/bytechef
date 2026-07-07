/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The deployment-domain permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum DeploymentPermissionScope implements PermissionScopeType {

    DEPLOYMENT_VIEW,
    DEPLOYMENT_PUSH,
    DEPLOYMENT_PULL,
    DEPLOYMENT_EDIT
}
