/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * The knowledge-base permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum KnowledgeBasePermissionScope implements PermissionScopeType {

    KNOWLEDGE_BASE_VIEW,
    KNOWLEDGE_BASE_CREATE,
    KNOWLEDGE_BASE_EDIT
}
