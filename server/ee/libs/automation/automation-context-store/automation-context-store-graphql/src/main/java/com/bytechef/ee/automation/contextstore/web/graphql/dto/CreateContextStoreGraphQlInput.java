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
 * GraphQL input for {@code createContextStore}. Environment is supplied separately on the mutation argument list rather
 * than nested here, matching the {@code createKnowledgeBase} pattern in the KB GraphQL surface.
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record CreateContextStoreGraphQlInput(
    String name, @Nullable String description, @Nullable List<Long> tagIds) {
}
