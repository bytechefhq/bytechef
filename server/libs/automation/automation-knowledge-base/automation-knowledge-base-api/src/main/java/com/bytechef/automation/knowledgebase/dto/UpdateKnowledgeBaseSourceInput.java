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
import org.jspecify.annotations.Nullable;

/**
 * Input DTO for updating a {@link com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource}. All fields are
 * optional; null means "leave unchanged". Consumed by
 * {@link com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseSourceFacade#update(Long, Long, UpdateKnowledgeBaseSourceInput)}.
 *
 * <p>
 * A {@code cadence} change triggers a targeted mutation of the workflow's trigger cron parameter; the rest of the
 * workflow definition is left untouched. An {@code enabled} change toggles the corresponding {@code
 * ProjectDeploymentWorkflow} (which controls trigger activation in the scheduler).
 *
 * <p>
 * {@code metadataFields} updates the metadata-tag whitelist on the source. Use {@code Optional.empty()} semantics via a
 * sentinel — since records can't represent "set to null" vs "leave unchanged" with a plain {@code Map} field, this
 * record reserves {@code null} for "leave unchanged" and an empty {@code Map} ({@code {}}) for "clear the whitelist
 * (revert to full-flatten MVP behavior)".
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record UpdateKnowledgeBaseSourceInput(
    @Nullable String name,
    @Nullable String cadence,
    @Nullable Boolean enabled,
    @Nullable Map<String, ?> metadataFields,
    @Nullable String fullReplaceCadence,
    @Nullable TombstoneStrategy tombstoneStrategy) {
}
