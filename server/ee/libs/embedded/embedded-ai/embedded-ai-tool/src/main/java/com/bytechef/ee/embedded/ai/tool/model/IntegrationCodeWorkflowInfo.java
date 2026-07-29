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

/**
 * Code workflow integration information record for API responses.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationCodeWorkflowInfo(
    @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the integration") Long id,
    @JsonProperty("component_name") @JsonPropertyDescription("The component name of the integration") String componentName,
    @JsonProperty("language") @JsonPropertyDescription("The language of the code workflow, or null if it could not be resolved") String language) {
}
