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

package com.bytechef.automation.datasync.dto;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Row of a Data Sync's version-history sheet: one {@code ProjectVersion} of its hidden backing project.
 *
 * @param version       the project version number
 * @param description   the description given at publish time, or {@code null} for a version never published (e.g. the
 *                      current draft)
 * @param publishedDate when this version was published, or {@code null} for a version never published
 * @param status        the version's {@code ProjectVersion.Status}, as a string
 *
 * @author Ivica Cardic
 */
public record DataSyncVersionDTO(
    int version, @Nullable String description, @Nullable Instant publishedDate, String status) {
}
