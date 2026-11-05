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
import java.util.List;

/**
 * @version ee
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationWorkflowSaveInfo(
    @JsonProperty("workflow_id") @JsonPropertyDescription("The unique identifier of the workflow, used as workflowId by the workflow tools") String workflowId,
    @JsonProperty("integration_workflow_id") @JsonPropertyDescription("The numeric identifier of the integration workflow") Long integrationWorkflowId,
    @JsonProperty("name") @JsonPropertyDescription("The name of the workflow") String name,
    @JsonProperty("version") @JsonPropertyDescription("The version of the saved workflow, to pass to the next updateWorkflow") int version,
    @JsonProperty("warnings") @JsonPropertyDescription("Validation warnings of the saved definition; they did not block the save") List<String> warnings) {
}
