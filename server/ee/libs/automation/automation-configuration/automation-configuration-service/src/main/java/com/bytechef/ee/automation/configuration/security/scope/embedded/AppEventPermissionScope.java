/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope.embedded;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The app event permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AppEventPermissionScope implements PermissionScopeType {

    APP_EVENT_VIEW,
    APP_EVENT_CREATE,
    APP_EVENT_EDIT,
    APP_EVENT_DELETE
}
