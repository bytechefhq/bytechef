/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;

/**
 * Detailed integration information record for API responses. The embedded mirror of the automation
 * {@code ProjectDetailInfo}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationDetailInfo(
    @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the integration") Long id,
    @JsonProperty("name") @JsonPropertyDescription("The name of the integration") String name,
    @JsonProperty("description") @JsonPropertyDescription("The description of the integration") String description,
    @JsonProperty("component_name") @JsonPropertyDescription("The name of the component the integration wraps") String componentName,
    @JsonProperty("component_version") @JsonPropertyDescription("The version of the component the integration wraps") int componentVersion,
    @JsonProperty("status") @JsonPropertyDescription("The publication status of the integration (DRAFT or PUBLISHED)") String status,
    @JsonProperty("multiple_instances") @JsonPropertyDescription("Whether the integration allows multiple connected instances") boolean multipleInstances,
    @JsonProperty("integration_workflow_ids") @JsonPropertyDescription("The ids of the workflows belonging to the integration") List<Long> integrationWorkflowIds,
    @JsonProperty("created_date") @JsonPropertyDescription("When the integration was created") Instant createdDate,
    @JsonProperty("last_modified_date") @JsonPropertyDescription("When the integration was last modified") Instant lastModifiedDate,
    @JsonProperty("last_published_date") @JsonPropertyDescription("When the integration was last published") Instant lastPublishedDate) {
}
