/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import org.jspecify.annotations.Nullable;

/**
 * An API collection an app can be bound to. {@code available} is false while the collection's deployment is disabled.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppBindableApiCollectionDTO(
    long apiCollectionId, String name, @Nullable String contextPath, int collectionVersion, long projectId,
    @Nullable String projectName, boolean available) {
}
