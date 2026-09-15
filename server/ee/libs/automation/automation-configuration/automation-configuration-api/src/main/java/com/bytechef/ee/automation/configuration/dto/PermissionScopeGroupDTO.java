/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.dto;

import java.util.List;
import java.util.Objects;

/**
 * The permission scopes one module owns, so a role editor can present them under the module's heading rather than as
 * one undifferentiated list.
 *
 * <p>
 * The grouping is derived from the {@code PermissionScopeProvider} SPI itself — a module's scopes are the constants of
 * the enum it declares — so a module contributing a new scope enum gets its own group with no change here and none in
 * the client.
 *
 * @param name   the module, as a scope-style name (e.g. {@code WORKSPACE}, {@code API_KEY}), so a client labels it with
 *               the same title-casing it already applies to scope names
 * @param scopes the module's scope names, in the order the enum declares them — read before write before delete, which
 *               is how the permissions actually escalate
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record PermissionScopeGroupDTO(String name, List<String> scopes) {

    public PermissionScopeGroupDTO {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(scopes, "scopes");

        scopes = List.copyOf(scopes);
    }
}
