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
 * Created integration workflow information record for API responses.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record CreatedIntegrationWorkflowInfo(
    @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the integration workflow") Long id,
    @JsonProperty("integration_id") @JsonPropertyDescription("The ID of the integration this workflow belongs to") long integrationId,
    @JsonProperty("integration_version") @JsonPropertyDescription("The version of the integration") int integrationVersion,
    @JsonProperty("workflow_id") @JsonPropertyDescription("The unique identifier of the workflow") String workflowId,
    @JsonProperty("workflow_uuid") @JsonPropertyDescription("The uuid of the workflow") String workflowUuid,
    @JsonProperty("created_date") @JsonPropertyDescription("When the workflow was created") Instant createdDate,
    @JsonProperty("last_modified_date") @JsonPropertyDescription("When the workflow was last modified") Instant lastModifiedDate) {
}
