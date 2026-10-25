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
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;

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

    void importCsv(long dataTableId, String csv, long environmentId);

    void updateTags(long dataTableId, List<Tag> tags);

    List<Webhook> listWebhooks(long dataTableId, long environmentId);
}
