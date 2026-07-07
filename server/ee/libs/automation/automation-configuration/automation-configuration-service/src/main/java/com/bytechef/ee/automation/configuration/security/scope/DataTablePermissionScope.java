/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The data-table permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum DataTablePermissionScope implements PermissionScopeType {

    DATA_TABLE_VIEW,
    DATA_TABLE_CREATE,
    DATA_TABLE_EDIT
}
