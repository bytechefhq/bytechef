/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record PortalAppDTO(
    long id, String name, @Nullable String description, @Nullable Long categoryId, @Nullable String categoryName,
    boolean hasIcon, boolean favorite, @Nullable Instant lastUsedDate, List<PortalAppInstanceDTO> instances) {

    public PortalAppDTO {
        instances = List.copyOf(instances);
    }
}
