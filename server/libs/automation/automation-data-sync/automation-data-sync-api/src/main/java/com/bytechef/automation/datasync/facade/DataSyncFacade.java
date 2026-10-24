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

package com.bytechef.automation.datasync.facade;

import com.bytechef.automation.datasync.domain.DataSync.TriggerType;
import com.bytechef.automation.datasync.domain.DataSyncElement;
import com.bytechef.automation.datasync.domain.DataSyncElement.Kind;
import com.bytechef.automation.datasync.dto.DataSyncDTO;
import com.bytechef.automation.datasync.dto.DataSyncDeploymentDTO;
import com.bytechef.automation.datasync.dto.DataSyncVersionDTO;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The HTTP surface of Data Sync, and the owner of its authorization: every method carries a {@code @PreAuthorize} on
 * the implementation. Every mutation regenerates the Data Sync's generated workflow in its project's draft version
 * before returning. A Data Sync has no publish of its own: it is published with its project.
 *
 * @author Ivica Cardic
 */
public interface DataSyncFacade {

    DataSyncDTO createDataSync(String title, @Nullable String description, long workspaceId, @Nullable Long projectId);

    DataSyncDTO updateDataSync(long id, @Nullable String title, @Nullable String description);

    /**
     * Deletes the Data Sync and its generated workflow in every version of its project, never the project itself.
     * Refused while that workflow is enabled in any deployment of the project.
     */
    void deleteDataSync(long id);

    /**
     * Deletes the Data Sync rows of a project that is about to be deleted. The project's workflows and deployments are
     * left to the project delete itself.
     */
    void deleteProjectDataSyncs(long projectId);

    DataSyncDTO getDataSync(long id);

    List<DataSyncDTO> getDataSyncs(long workspaceId);

    void updateDataSyncTrigger(long id, TriggerType triggerType, @Nullable Map<String, ?> triggerParameters);

    /**
     * Upserts the element of {@code kind}. Replacing the {@code SOURCE} or {@code DESTINATION} component also deletes
     * the {@code PROCESSOR} row, because a field mapping over a different side is meaningless.
     */
    DataSyncElement setDataSyncElement(
        long id, Kind kind, String componentName, int componentVersion, String operationName,
        @Nullable Map<String, ?> parameters, @Nullable Long connectionId);

    /** Whole-map replace of the element's parameters and connection; the per-field autosave target. */
    void updateDataSyncElement(long elementId, @Nullable Map<String, ?> parameters, @Nullable Long connectionId);

    /**
     * Validates every Data Sync of the project and regenerates its workflow in the project's draft version, in place,
     * so the version about to be published snapshots the Data Syncs' current rows. Throws when a Data Sync is not
     * publishable, naming it, which aborts the project publish.
     */
    void prepareProjectPublish(long projectId);

    List<DataSyncVersionDTO> getDataSyncVersions(long id);

    List<DataSyncDeploymentDTO> getDataSyncDeployments(long workspaceId);

    /** Starts one run of the deployed workflow, for either trigger type. Returns the job id. */
    long runDataSyncDeployment(long id, long projectDeploymentId);

    /**
     * Copies every Data Sync of the source project into the target project, both in {@code workspaceId}: new rows with
     * new names, new generated workflows in the target's draft version and freshly synced test connections. Everything
     * stays in the workspace, so each element keeps its connection.
     */
    List<DataSyncDTO> copyProjectDataSyncs(long sourceProjectId, long targetProjectId, long workspaceId);

    /**
     * Exports the Data Sync as a pretty-printed, versioned JSON document ({@code exportVersion} 1) carrying its name,
     * title, description, trigger and elements (in {@code SOURCE}, {@code DESTINATION}, {@code PROCESSOR} order), with
     * enums written by name. Connections are never exported: a connection id means nothing in another workspace. The
     * generated workflow is not exported either; import regenerates it.
     */
    String exportDataSync(long id);

    /**
     * Recreates a Data Sync from an {@link #exportDataSync export document}, through the same path as
     * {@link #createDataSync}: a {@code null} {@code projectId} creates a project named after the title. The exported
     * name seeds the new name. Every element is created without a connection, so a Data Sync whose components need one
     * blocks its project's publish until one is picked.
     */
    DataSyncDTO importDataSync(long workspaceId, String json, @Nullable Long projectId);

    /**
     * Applies an {@link #exportDataSync export document} to an existing Data Sync: title, description and trigger are
     * replaced and each element is upserted by kind. An element whose component, version and operation are unchanged
     * keeps its connection, a changed one loses it, and a kind absent from the document is deleted.
     */
    DataSyncDTO updateDataSyncFromExport(long id, String json);
}
