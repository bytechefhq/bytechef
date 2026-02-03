/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.dto;

/**
 * DTO for API response definition.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ResponseDefinitionDTO(
    String statusCode,
    String description,
    String contentType,
    String schema) {
}
