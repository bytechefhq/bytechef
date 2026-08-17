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

package com.bytechef.automation.ai.agent.dto;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * One entry of an agent's version history — a {@code ProjectVersion} of the agent's hidden backing project, which is
 * what publishing an agent actually mints. Exposed rather than the raw {@code ProjectVersion} so the agent surface
 * never has to name the backing project, which is an implementation detail everywhere else.
 *
 * @param version       the version number
 * @param description   the publish description the user typed, or {@code null} if none was given
 * @param publishedDate when this version was published, {@code null} for the draft
 * @param status        {@code "PUBLISHED"} or {@code "DRAFT"} — the draft is the version publishing would mint next,
 *                      and is included so the history reads the same as a project's
 *
 * @author Ivica Cardic
 */
public record AiAgentVersionDTO(
    int version, @Nullable String description, @Nullable Instant publishedDate, String status) {
}
