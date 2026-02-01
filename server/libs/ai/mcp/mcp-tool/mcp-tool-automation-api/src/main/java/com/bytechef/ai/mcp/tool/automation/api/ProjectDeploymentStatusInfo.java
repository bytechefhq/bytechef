/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.mcp.tool.automation.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;

/**
 * Project deployment status information record for API responses.
 *
 * @author Marko Kriskovic
 */
@SuppressFBWarnings("EI")
public record ProjectDeploymentStatusInfo(
    @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the deployment") Long id,
    @JsonProperty("environment") @JsonPropertyDescription("The deployment environment") String environment,
    @JsonProperty("enabled") @JsonPropertyDescription("Whether the deployment is enabled") boolean enabled,
    @JsonProperty("is_deployment_enabled") @JsonPropertyDescription("Whether the deployment is currently enabled") boolean isDeploymentEnabled,
    @JsonProperty("created_date") @JsonPropertyDescription("When the deployment was created") Instant createdDate,
    @JsonProperty("last_modified_date") @JsonPropertyDescription("When the deployment was last modified") Instant lastModifiedDate) {
}
