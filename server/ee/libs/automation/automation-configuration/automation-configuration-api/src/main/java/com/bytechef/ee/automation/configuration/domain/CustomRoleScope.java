/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.domain;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

/**
 * Value object backing the {@code custom_role_scope} child collection of {@link CustomRole}. Modeled as a record
 * because it has no identity of its own &mdash; equality is value-based via the single {@code scope} component, which
 * is exactly the semantics required for membership in a {@link java.util.Set}.
 *
 * <p>
 * The scope is stored as its plain {@code String} name. The set of valid names is no longer a hardcoded enum but is
 * assembled at runtime from the per-module {@code PermissionScopeProvider} SPI; persistence has always been by name, so
 * dropping the enum changes nothing on disk.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("custom_role_scope")
public record CustomRoleScope(@Column("scope") String scope) {

    public CustomRoleScope {
        Assert.hasText(scope, "'scope' must not be blank");
    }
}
