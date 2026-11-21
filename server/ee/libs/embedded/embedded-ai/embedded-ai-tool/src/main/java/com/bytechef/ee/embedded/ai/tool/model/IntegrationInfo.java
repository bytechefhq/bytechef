/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool.model;

import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 * @author Ivica Cardic
 */
public record IntegrationInfo(
    @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the integration") Long id,
    @JsonProperty("name") @JsonPropertyDescription("The name of the integration") String name,
    @JsonProperty("description") @JsonPropertyDescription("The description of the integration") @Nullable String description,
    @JsonProperty("component_name") @JsonPropertyDescription("The name of the component the integration wraps") String componentName,
    @JsonProperty("status") @JsonPropertyDescription("The publication status of the integration (DRAFT or PUBLISHED)") Status status,
    @JsonProperty("created_date") @JsonPropertyDescription("When the integration was created") Instant createdDate,
    @JsonProperty("last_modified_date") @JsonPropertyDescription("When the integration was last modified") Instant lastModifiedDate) {
}
