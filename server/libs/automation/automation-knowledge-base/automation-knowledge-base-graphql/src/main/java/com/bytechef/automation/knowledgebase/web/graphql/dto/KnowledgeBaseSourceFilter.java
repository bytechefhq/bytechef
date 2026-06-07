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

package com.bytechef.automation.knowledgebase.web.graphql.dto;

import org.jspecify.annotations.Nullable;

/**
 * Filter input for the {@code knowledgeBaseSources(workspaceId, filter)} GraphQL query. All fields are optional; null
 * means "no filtering on that dimension". When {@link #enabled()} is {@code true}, only enabled sources are returned;
 * when {@code false} or {@code null}, all sources are returned.
 *
 * @author Ivica Cardic
 */
public record KnowledgeBaseSourceFilter(@Nullable Boolean enabled) {
}
