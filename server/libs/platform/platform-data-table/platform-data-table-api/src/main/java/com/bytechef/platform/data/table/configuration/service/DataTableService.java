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
import com.bytechef.platform.data.table.domain.ColumnSpec;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Unified service for managing dynamic data tables and querying their metadata.
 *
 * <p>
 * A table is addressed by its registry id. A name is unique only within a workspace, so the one name lookup,
 * {@link #fetchDataTable(Long, String)}, always takes the workspace the name belongs to. Each environment holds its own
 * physical instance of a registered table.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface DataTableService {

    /**
     * Adds a column to the table's physical instance in the given environment.
     */
    void addColumn(long dataTableId, ColumnSpec columnSpec, long environmentId);

    /**
     * Creates the environment's physical table for {@code (workspaceId, name)}, registering the name first when the
     * workspace does not hold it yet, and returns the table's id.
     */
    long createTable(
        @Nullable Long workspaceId, String name, @Nullable String description, List<ColumnSpec> columnSpecs,
        long environmentId);

    /**
     * Drops the table's physical instance in the given environment and deletes the registry row once no environment
     * holds one.
     */
    void dropTable(long dataTableId, long environmentId);

    /**
     * Copies the table's columns and rows in the given environment into a new table of the source's workspace and
     * returns the copy's id.
     */
    long duplicateTable(long dataTableId, String newName, long environmentId);

    /**
     * The registry row {@code name} names within the workspace, the only lookup by name.
     */
    Optional<DataTable> fetchDataTable(@Nullable Long workspaceId, String name);

    /**
     * The table's registry metadata and user columns, present only when its physical table exists in the environment.
     */
    Optional<DataTableInfo> fetchDataTableInfo(long dataTableId, long environmentId);

    /**
     * The registry row with the given id, failing with {@code DATA_TABLE_NOT_FOUND} when there is none.
     */
    DataTable getDataTable(long dataTableId);

    /**
     * Every registry row of the workspace, ordered by name.
     */
    List<DataTable> getWorkspaceDataTables(long workspaceId);

    /**
     * Every table of every workspace whose physical table exists in the environment.
     */
    List<DataTableInfo> listAllTables(long environmentId);

    /**
     * The workspace's tables whose physical table exists in the environment, ordered by name.
     */
    List<DataTableInfo> listTables(@Nullable Long workspaceId, long environmentId);

    /**
     * Removes a column from the table's physical instance in the given environment.
     */
    void removeColumn(long dataTableId, String columnName, long environmentId);

    /**
     * Renames a column of the table's physical instance in the given environment.
     */
    void renameColumn(long dataTableId, String fromColumnName, String toColumnName, long environmentId);

    /**
     * Renames the table within its workspace; every environment keeps reaching it because the physical name holds the
     * id.
     */
    void renameTable(long dataTableId, String newName);

    /**
     * Writes the registry description, which is shared by every environment.
     */
    void updateDescription(long dataTableId, @Nullable String description);
}
