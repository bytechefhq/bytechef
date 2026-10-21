/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import org.jspecify.annotations.Nullable;

/**
 * An API collection binding with what the collection is now. {@code available} is false when the collection no longer
 * resolves (missing, its deployment missing or disabled, or moved out of the app's workspace or environment); when the
 * collection itself is missing, {@code name} is {@code "#" + apiCollectionId} and the other collection fields are null
 * or 0.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppApiCollectionBindingDTO(
    long id, long appId, long apiCollectionId, String name, @Nullable String contextPath, int collectionVersion,
    long projectId, @Nullable String projectName, boolean available) {
}
