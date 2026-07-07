/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The project-lifecycle permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ProjectPermissionScope implements PermissionScopeType {

    PROJECT_CREATE,
    PROJECT_SETTINGS,
    PROJECT_DELETE
}
