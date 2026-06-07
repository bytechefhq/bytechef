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

package com.bytechef.automation.knowledgebase.dto;

import com.bytechef.platform.knowledgebase.domain.TombstoneStrategy;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Input DTO for creating a {@link com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource} bound to a Knowledge
 * Base. Consumed by
 * {@link com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseSourceFacade#create(Long, CreateKnowledgeBaseSourceInput)}
 * which in turn auto-generates and persists the source-bound workflow.
 *
 * <p>
 * The KB-Source surface uses the existing {@code KnowledgeBaseDocument} table as the destination — there is no KB-side
 * "entity" concept analogous to Context Store entities; the binding to a KB is via {@code knowledgeBaseId}.
 *
 * <p>
 * {@code sourceClusterElementName} is optional: when omitted, the facade auto-picks the first {@code ItemReader}
 * cluster element on the source component. An explicit name lets the caller pin a specific reader when the component
 * defines more than one.
 *
 * <p>
 * {@code parameters} carries the SOURCE cluster element's input parameters captured by the wizard (e.g., Airtable
 * {@code BASE_ID}/{@code TABLE_ID}, HubSpot {@code OBJECT_TYPE}). Threaded through to the auto-generated workflow's
 * SOURCE cluster element so the reader actually has the inputs it needs at sync time. May be {@code null} for readers
 * with no input properties.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record CreateKnowledgeBaseSourceInput(
    String name,
    String sourceComponentName,
    int sourceComponentVersion,
    @Nullable String sourceClusterElementName,
    @Nullable Long connectionId,
    Long knowledgeBaseId,
    String cadence,
    @Nullable Map<String, ?> parameters,
    @Nullable Map<String, ?> metadataFields,
    @Nullable String fullReplaceCadence,
    @Nullable TombstoneStrategy tombstoneStrategy) {

    public CreateKnowledgeBaseSourceInput {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(sourceComponentName, "sourceComponentName");
        Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId");
        Objects.requireNonNull(cadence, "cadence");
    }
}
