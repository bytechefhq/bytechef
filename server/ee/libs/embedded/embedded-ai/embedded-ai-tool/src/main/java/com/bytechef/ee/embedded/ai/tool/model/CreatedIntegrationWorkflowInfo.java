/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Instant;

/**
 * @version ee
 * @author Ivica Cardic
 */
public record CreatedIntegrationWorkflowInfo(
    @JsonProperty("workflow_id") @JsonPropertyDescription("The unique identifier of the workflow, used as workflowId by the workflow tools") String workflowId,
    @JsonProperty("integration_workflow_id") @JsonPropertyDescription("The numeric identifier of the integration workflow") Long integrationWorkflowId,
    @JsonProperty("integration_id") @JsonPropertyDescription("The ID of the integration this workflow belongs to") long integrationId,
    @JsonProperty("integration_version") @JsonPropertyDescription("The version of the integration") int integrationVersion,
    @JsonProperty("workflow_uuid") @JsonPropertyDescription("The uuid of the workflow") String workflowUuid,
    @JsonProperty("created_date") @JsonPropertyDescription("When the workflow was created") Instant createdDate,
    @JsonProperty("last_modified_date") @JsonPropertyDescription("When the workflow was last modified") Instant lastModifiedDate) {
}
