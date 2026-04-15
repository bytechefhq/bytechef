/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.domain;

import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

/**
 * Value object backing the {@code custom_role_scope} child collection of {@link CustomRole}. Modeled as a record
 * because it has no identity of its own &mdash; equality is value-based via the single {@code scope} component, which
 * is exactly the semantics required for membership in a {@link java.util.Set}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("custom_role_scope")
public record CustomRoleScope(@Column("scope") PermissionScope scope) {

    public CustomRoleScope {
        Assert.notNull(scope, "'scope' must not be null");
    }
}
