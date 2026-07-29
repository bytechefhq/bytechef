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

/**
 * Integration workflow information record for API responses.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationWorkflowInfo(
    @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the workflow") String id,
    @JsonProperty("integration_workflow_id") @JsonPropertyDescription("The unique identifier of the integration workflow") Long integrationWorkflowId,
    @JsonProperty("workflow_uuid") @JsonPropertyDescription("The uuid of the workflow") String workflowUuid,
    @JsonProperty("name") @JsonPropertyDescription("The name of the workflow") String name,
    @JsonProperty("description") @JsonPropertyDescription("The description of the workflow") String description,
    @JsonProperty("definition") @JsonPropertyDescription("The definition of the workflow") String definition,
    @JsonProperty("version") @JsonPropertyDescription("The version of the workflow") int version,
    @JsonProperty("created_date") @JsonPropertyDescription("When the workflow was created") Instant createdDate,
    @JsonProperty("last_modified_date") @JsonPropertyDescription("When the workflow was last modified") Instant lastModifiedDate) {
}
