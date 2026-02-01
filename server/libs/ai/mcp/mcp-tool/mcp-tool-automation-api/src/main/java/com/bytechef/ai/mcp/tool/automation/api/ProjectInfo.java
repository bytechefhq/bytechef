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
 * Project information record for API responses.
 *
 * @author Marko Kriskovic
 */
@SuppressFBWarnings("EI")
public record ProjectInfo(
    @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the project") Long id,
    @JsonProperty("name") @JsonPropertyDescription("The name of the project") String name,
    @JsonProperty("description") @JsonPropertyDescription("The description of the project") String description,
    @JsonProperty("status") @JsonPropertyDescription("The current status of the project") String status,
    @JsonProperty("created_date") @JsonPropertyDescription("When the project was created") Instant createdDate,
    @JsonProperty("last_modified_date") @JsonPropertyDescription("When the project was last modified") Instant lastModifiedDate) {
}
