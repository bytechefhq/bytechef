/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The Data Sync permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum DataSyncPermissionScope implements PermissionScopeType {

    DATA_SYNC_VIEW,
    DATA_SYNC_CREATE,
    DATA_SYNC_EDIT,
    DATA_SYNC_DELETE,
    DATA_SYNC_PUBLISH
}
