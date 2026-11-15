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

package com.bytechef.platform.data.table.configuration.service;

import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.domain.DataTableLinkColumn;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface DataTableService {

    void addColumn(long dataTableId, ColumnSpec columnSpec, long environmentId);

    long createTable(
        @Nullable Long workspaceId, String name, @Nullable String description, List<ColumnSpec> columnSpecs,
        long environmentId);

    boolean dropTable(long dataTableId, long environmentId);

    long duplicateTable(long dataTableId, String newName, long environmentId);

    Optional<DataTable> fetchDataTable(@Nullable Long workspaceId, String name);

    Optional<DataTableInfo> fetchDataTableInfo(long dataTableId, long environmentId);

    DataTable getDataTable(long dataTableId);

    List<DataTable> getWorkspaceDataTables(long workspaceId);

    List<DataTableLinkColumn> getLinksTo(long targetDataTableId, long environmentId);

    List<DataTableInfo> listAllTables(long environmentId);

    List<DataTableInfo> listTables(@Nullable Long workspaceId, long environmentId);

    void removeColumn(long dataTableId, String columnName, long environmentId);

    void renameColumn(long dataTableId, String fromColumnName, String toColumnName, long environmentId);

    void renameTable(long dataTableId, String newName);

    void updateDescription(long dataTableId, @Nullable String description);
}
