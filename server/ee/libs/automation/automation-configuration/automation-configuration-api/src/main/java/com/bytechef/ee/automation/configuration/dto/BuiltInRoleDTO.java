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
 * A built-in workspace role and everything it grants, so an operator can see what the fixed tiers already cover before
 * composing a custom role that duplicates one.
 *
 * <p>
 * Assembled from the {@code minimumRole} each module declares alongside its scopes, not from a list maintained
 * separately — the tiers a reader sees here are the tiers the authorization checks actually apply.
 *
 * @param name   the role ({@code VIEWER}, {@code EDITOR}, {@code ADMIN})
 * @param scopes every scope the role holds, inherited ones included, in the catalogue's order — the roles nest
 *               ({@code VIEWER ⊆ EDITOR ⊆ ADMIN}), so this is the full set rather than the tier's delta
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record BuiltInRoleDTO(String name, List<String> scopes) {

    public BuiltInRoleDTO {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(scopes, "scopes");

        scopes = List.copyOf(scopes);
    }
}
