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
import java.util.List;

/**
 * Detailed project information record for API responses.
 *
 * @author Marko Kriskovic
 */
@SuppressFBWarnings("EI")
public record ProjectDetailInfo(
    @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the project") Long id,
    @JsonProperty("name") @JsonPropertyDescription("The name of the project") String name,
    @JsonProperty("description") @JsonPropertyDescription("The description of the project") String description,
    @JsonProperty("status") @JsonPropertyDescription("The current status of the project") String status,
    @JsonProperty("category_id") @JsonPropertyDescription("The category ID of the project") Long categoryId,
    @JsonProperty("workspace_id") @JsonPropertyDescription("The workspace ID of the project") Long workspaceId,
    @JsonProperty("tag_ids") @JsonPropertyDescription("The tag IDs associated with the project") List<Long> tagIds,
    @JsonProperty("versions") @JsonPropertyDescription("The versions of the project") List<ProjectVersionInfo> versions,
    @JsonProperty("created_date") @JsonPropertyDescription("When the project was created") Instant createdDate,
    @JsonProperty("last_modified_date") @JsonPropertyDescription("When the project was last modified") Instant lastModifiedDate,
    @JsonProperty("last_published_date") @JsonPropertyDescription("When the project was last published") Instant lastPublishedDate) {
}
