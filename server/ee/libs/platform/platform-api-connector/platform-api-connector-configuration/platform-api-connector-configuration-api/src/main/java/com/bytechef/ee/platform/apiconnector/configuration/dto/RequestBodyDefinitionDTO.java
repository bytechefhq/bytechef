/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.dto;

/**
 * DTO for API request body definition.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record RequestBodyDefinitionDTO(
    String contentType,
    String description,
    Boolean required,
    String schema) {
}
