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

package com.bytechef.automation.data.table.configuration.facade;

import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService.Webhook;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

/**
 * Coordinates the platform {@code DataTableService} with the workspace relation, exposing the workspace-scoped
 * create/list operations the web layer needs.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceDataTableFacade {

    void addColumn(long dataTableId, ColumnSpec columnSpec, long environmentId);

    void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long workspaceId, long environmentId);

    void dropTable(long dataTableId, long environmentId);

    void duplicateTable(long dataTableId, String newBaseName, long environmentId);

    List<Tag> getDataTableTags(long workspaceId);

    List<DataTableInfo> listTables(long workspaceId, long environmentId);

    void removeColumn(long dataTableId, String columnName, long environmentId);

    void renameColumn(long dataTableId, String fromColumnName, String newName, long environmentId);

    void renameTable(long dataTableId, String newBaseName, long environmentId);

    List<DataTableRow> listRows(long dataTableId, int limit, int offset, long environmentId);

    DataTableRow insertRow(long dataTableId, Map<String, Object> values, long environmentId);

    DataTableRow updateRow(long dataTableId, long rowId, Map<String, Object> values, long environmentId);

    boolean deleteRow(long dataTableId, long rowId, long environmentId);

    String exportCsv(long dataTableId, long environmentId);

    int importCsv(long dataTableId, String csv, long environmentId);

    void updateTags(long dataTableId, List<Tag> tags);

    List<Webhook> listWebhooks(long dataTableId, long environmentId);

    DataTableStorageUsage getStorageUsage();

    /**
     * The metadata of a single data table.
     */
    DataTableInfo getTable(long dataTableId, long environmentId);

    /**
     * Updates a data table's description.
     */
    void updateDescription(long dataTableId, @Nullable String description);

    /**
     * Filtered, sorted and paged rows of a data table.
     */
    Page<DataTableRow> listRows(
        long dataTableId, List<RowFilter> rowFilters, List<RowSort> rowSorts, int pageNumber, int pageSize,
        long environmentId);

    /**
     * A single row by its id.
     */
    DataTableRow getRow(long dataTableId, long rowId, long environmentId);

    /**
     * A single row by its caller-supplied external id, empty when none matches.
     */
    Optional<DataTableRow> fetchRowByExternalId(long dataTableId, String externalId, long environmentId);

    /**
     * Inserts a row with an optional caller-supplied external id.
     */
    DataTableRow insertRow(
        long dataTableId, Map<String, Object> values, @Nullable String externalId, long environmentId);

    /**
     * Updates a row by id, optionally patching its external id.
     */
    DataTableRow updateRow(
        long dataTableId, long rowId, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        long environmentId);

    /**
     * Inserts or updates the row keyed by the given external id.
     */
    UpsertResult upsertRow(long dataTableId, String externalId, Map<String, Object> values, long environmentId);

    /**
     * Deletes the row keyed by the given external id; false when none matches.
     */
    boolean deleteRowByExternalId(long dataTableId, String externalId, long environmentId);

    /**
     * Inserts (or upserts) a batch of rows in one transaction.
     */
    List<DataTableRow> insertRows(
        long dataTableId, List<NewRow> newRows, CreateStrategy createStrategy, long environmentId);

    /**
     * Deletes the rows among the given ids; returns the ids that were actually deleted.
     */
    List<Long> deleteRows(long dataTableId, List<Long> rowIds, long environmentId);

    /**
     * Deletes every row of a data table; returns the count.
     */
    long clearRows(long dataTableId, long environmentId);

    /**
     * The tags assigned to each of the workspace's data tables, keyed by data table id.
     */
    Map<Long, List<Tag>> getTagsByTableId(long workspaceId);

    /**
     * The workspace a data table belongs to.
     */
    long getWorkspaceId(long dataTableId);
}
