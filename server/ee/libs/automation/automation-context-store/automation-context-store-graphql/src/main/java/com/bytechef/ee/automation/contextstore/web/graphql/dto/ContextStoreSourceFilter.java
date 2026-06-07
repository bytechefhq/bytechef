/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.web.graphql.dto;

import org.jspecify.annotations.Nullable;

/**
 * Filter input for the {@code contextStoreSources(workspaceId, filter)} GraphQL query. All fields are optional; null
 * means "no filtering on that dimension". When {@link #enabled()} is {@code true}, only enabled sources are returned;
 * when {@code false} or {@code null}, all sources are returned.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record ContextStoreSourceFilter(@Nullable Boolean enabled) {
}
