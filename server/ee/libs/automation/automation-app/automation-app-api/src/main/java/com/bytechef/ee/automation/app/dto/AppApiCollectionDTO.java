/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * An API collection bound to the calling app, with the endpoints it can call. When the collection no longer resolves,
 * {@code available} is false and {@code endpoints} is empty; when the collection itself is missing, {@code name} is
 * {@code "#" + apiCollectionId}, {@code contextPath} is null and {@code collectionVersion} is 0.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AppApiCollectionDTO(
    long apiCollectionId, String name, @Nullable String contextPath, int collectionVersion, boolean available,
    List<AppApiEndpointDTO> endpoints) {
}
