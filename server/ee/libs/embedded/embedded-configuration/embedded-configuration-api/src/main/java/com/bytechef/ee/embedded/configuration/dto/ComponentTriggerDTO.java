/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ComponentTriggerDTO(
    String name, String title, @Nullable String description, String type, String inputSchema,
    @Nullable String outputSchema, boolean hasDynamicOutput) {
}
