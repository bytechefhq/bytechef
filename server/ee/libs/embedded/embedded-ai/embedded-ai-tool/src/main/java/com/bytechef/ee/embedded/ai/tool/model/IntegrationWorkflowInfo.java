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
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 * @author Ivica Cardic
 */
public record IntegrationWorkflowInfo(
    @JsonProperty("workflow_id") @JsonPropertyDescription("The unique identifier of the workflow, used as workflowId by the workflow tools") String workflowId,
    @JsonProperty("integration_workflow_id") @JsonPropertyDescription("The numeric identifier of the integration workflow, as listed in an integration's integration_workflow_ids") Long integrationWorkflowId,
    @JsonProperty("workflow_uuid") @JsonPropertyDescription("The uuid of the workflow") String workflowUuid,
    @JsonProperty("name") @JsonPropertyDescription("The name of the workflow") String name,
    @JsonProperty("description") @JsonPropertyDescription("The description of the workflow") @Nullable String description,
    @JsonProperty("definition") @JsonPropertyDescription("The definition of the workflow") String definition,
    @JsonProperty("version") @JsonPropertyDescription("The version of the workflow, pass it to updateWorkflow") int version,
    @JsonProperty("created_date") @JsonPropertyDescription("When the workflow was created") Instant createdDate,
    @JsonProperty("last_modified_date") @JsonPropertyDescription("When the workflow was last modified") Instant lastModifiedDate) {
}
