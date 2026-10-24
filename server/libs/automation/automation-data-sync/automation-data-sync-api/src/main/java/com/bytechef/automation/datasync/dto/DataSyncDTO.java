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

import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.domain.DataSyncElement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Read-model returned by {@link com.bytechef.automation.datasync.facade.DataSyncFacade} — the {@link DataSync} row plus
 * its {@link DataSyncElement} rows, and derived fields describing the draft &harr; published relationship of the Data
 * Sync's generated workflow within its project.
 *
 * @param dataSync             the Data Sync row
 * @param projectId            the project owning the Data Sync's generated workflow
 * @param elements             the Data Sync's SOURCE/DESTINATION/PROCESSOR rows
 * @param unpublishedChanges   {@code true} when the Data Sync's workflow is absent from the project's most recently
 *                             published version, or differs from it in the current draft
 * @param lastPublishedVersion the project's most recently published version number, or {@code 0} if that version does
 *                             not contain the Data Sync's workflow
 * @param publishedDate        when that published version was published, or {@code null} if it does not contain the
 *                             Data Sync's workflow
 * @param lastModifiedDate     when the Data Sync's draft workflow was last regenerated, falling back to the
 *                             {@code data_sync} row's own date
 * @param draftWorkflowId      the id of the Data Sync's workflow in the project's current draft version
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record DataSyncDTO(
    DataSync dataSync, long projectId, List<DataSyncElement> elements, boolean unpublishedChanges,
    int lastPublishedVersion, @Nullable Instant publishedDate, @Nullable Instant lastModifiedDate,
    String draftWorkflowId) {
}
