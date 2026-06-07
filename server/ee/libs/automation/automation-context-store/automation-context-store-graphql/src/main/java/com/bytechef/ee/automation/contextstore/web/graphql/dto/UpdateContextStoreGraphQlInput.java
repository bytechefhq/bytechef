/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.web.graphql.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * GraphQL input for {@code updateContextStore}. Carries {@code version} for optimistic concurrency. Environment is
 * immutable post-creation and not part of this input.
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record UpdateContextStoreGraphQlInput(
    String name, @Nullable String description, @Nullable List<Long> tagIds, int version) {
}
