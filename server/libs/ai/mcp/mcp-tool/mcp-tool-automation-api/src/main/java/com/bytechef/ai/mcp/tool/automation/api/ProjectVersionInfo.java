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
 * Project version information record for API responses.
 *
 * @author Marko Kriskovic
 */
@SuppressFBWarnings("EI")
public record ProjectVersionInfo(
    @JsonProperty("version") @JsonPropertyDescription("The version number") int version,
    @JsonProperty("status") @JsonPropertyDescription("The status of the version") String status,
    @JsonProperty("description") @JsonPropertyDescription("The description of the version") String description,
    @JsonProperty("published_date") @JsonPropertyDescription("When the version was published") Instant publishedDate) {
}
