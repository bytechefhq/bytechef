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
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
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

    long clearRows(long dataTableId, long environmentId);

    void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long workspaceId, long environmentId);

    boolean deleteRow(long dataTableId, long rowId, long environmentId);

    boolean deleteRowByExternalId(long dataTableId, String externalId, long environmentId);

    List<Long> deleteRows(long dataTableId, List<Long> rowIds, long environmentId);

    void dropTable(long dataTableId, long environmentId);

    void duplicateTable(long tableId, String newBaseName, long environmentId);

    String exportCsv(long dataTableId, long environmentId);

    Optional<DataTableRow> fetchRowByExternalId(long dataTableId, String externalId, long environmentId);

    DataTableRow getRow(long dataTableId, long rowId, long environmentId);

    DataTableInfo getTable(long dataTableId, long environmentId);

    Map<Long, List<Tag>> getTagsByTableId(long workspaceId);

    long getWorkspaceId(long dataTableId);

    int importCsv(long dataTableId, String csv, long environmentId);

    DataTableRow insertRow(
        long dataTableId, Map<String, Object> values, @Nullable String externalId, long environmentId);

    List<DataTableRow> insertRows(
        long dataTableId, List<NewRow> newRows, CreateStrategy createStrategy, long environmentId);

    Page<DataTableRow> listRows(
        long dataTableId, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int pageNumber,
        int pageSize, long environmentId);

    List<DataTableInfo> listTables(long workspaceId, long environmentId);

    void removeColumn(long dataTableId, String columnName, long environmentId);

    void renameColumn(long dataTableId, String fromColumnName, String toColumnName, long environmentId);

    void updateDescription(long dataTableId, @Nullable String description);

    DataTableRow updateRow(
        long dataTableId, long rowId, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        long environmentId);

    void updateTags(long dataTableId, List<Tag> tags);

    UpsertResult upsertRow(long dataTableId, String externalId, Map<String, Object> values, long environmentId);
}
