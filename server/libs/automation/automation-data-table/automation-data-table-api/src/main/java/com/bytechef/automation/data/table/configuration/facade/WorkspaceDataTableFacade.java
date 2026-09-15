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
 * Coordinates the platform data table services with the workspace relation, exposing the workspace-scoped and per-table
 * operations the web layer needs. Every operation is authorized here.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceDataTableFacade {

    void addColumn(long tableId, ColumnSpec columnSpec, long environmentId);

    void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long workspaceId, long environmentId);

    boolean deleteRow(long tableId, long rowId, long environmentId);

    void dropTable(long tableId, long environmentId);

    void duplicateTable(long tableId, String newBaseName, long environmentId);

    String exportCsv(long tableId, long environmentId);

    Map<Long, List<Tag>> getTagsByTableId(long workspaceId, long environmentId);

    void importCsv(long tableId, String csv, long environmentId);

    DataTableRow insertRow(long tableId, Map<String, Object> values, long environmentId);

    List<DataTableRow> listRows(long tableId, int limit, int offset, long environmentId);

    List<DataTableInfo> listTables(long workspaceId, long environmentId);

    List<Webhook> listWebhooks(long tableId, long environmentId);

    void removeColumn(long tableId, String columnName, long environmentId);

    void renameColumn(long tableId, String fromColumnName, String toColumnName, long environmentId);

    void renameTable(long tableId, String newBaseName, long environmentId);

    DataTableRow updateRow(long tableId, long rowId, Map<String, Object> values, long environmentId);

    void updateTags(long tableId, List<Tag> tags);
}
