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

package com.bytechef.automation.data.table.configuration.service;

import com.bytechef.automation.data.table.configuration.domain.DataTableInfo;
import com.bytechef.automation.data.table.domain.ColumnSpec;
import java.util.List;

/**
 * Unified service for managing dynamic data tables and querying their metadata.
 *
 * <p>
 * Physical table names are constructed internally using the pattern: <code>dt_&lt;envIndex&gt;_&lt;baseName&gt;</code>,
 * where envIndex is mapped as DEVELOPMENT=0, STAGING=1, PRODUCTION=2.
 * </p>
 *
 * <p>
 * Callers must pass only the logical base table name (without any <code>dt_</code> prefix). Inputs starting with
 * <code>dt_</code> will be rejected.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface DataTableService {

    /**
     * Adds a new column to an existing dynamic data table in the specified environment.
     *
     * @param baseName      The logical base name of the table to which the column will be added. The name must not
     *                      include a "dt_" prefix.
     * @param columnSpec    The specification of the column to be added, including the column name and type.
     * @param environmentId The target environment ID.
     */
    void addColumn(String baseName, ColumnSpec columnSpec, long environmentId);

    /**
     * Creates a new dynamic data table in the specified environment with the given base name and column specifications.
     * The physical table name is derived internally based on the environment and base name.
     *
     * @param baseName      The logical base name of the table. The name must not include a "dt_" prefix, as it will be
     *                      automatically added.
     * @param columnSpecs   A list of column specifications defining the structure of the table, including column names
     *                      and types. This list must be non-null and non-empty.
     * @param environmentId The target environment where the table should be created (e.g., DEVELOPMENT, STAGING,
     *                      PRODUCTION).
     */
    void createTable(String baseName, List<ColumnSpec> columnSpecs, long environmentId);

    /**
     * Creates a new dynamic data table in the specified environment with the given base name, description, and column
     * specifications. The physical table name is derived internally based on the environment and base name.
     *
     * @param baseName      The logical base name of the table. The name must not include a "dt_" prefix, as it will be
     *                      automatically added.
     * @param description   A description for the table to provide additional metadata about its purpose. Must not be
     *                      null or empty.
     * @param columnSpecs   A list of column specifications defining the structure of the table, including column names
     *                      and types. This list must be non-null and non-empty.
     * @param environmentId The target environment where the table should be created (e.g., DEVELOPMENT, STAGING,
     *                      PRODUCTION).
     */
    void createTable(String baseName, String description, List<ColumnSpec> columnSpecs, long environmentId);

    /**
     * Creates a new dynamic data table in the specified environment and workspace, with the given base name and column
     * specifications. The physical table name is derived internally based on the environment, workspace ID, and base
     * name.
     *
     * @param workspaceId   The identifier of the workspace within which the table should be created. Must be a valid
     *                      non-negative ID.
     * @param baseName      The logical base name of the table. The name must not include a "dt_" prefix, as it will be
     *                      automatically added.
     * @param columnSpecs   A list of column specifications defining the structure of the table, including column names
     *                      and types. This list must be non-null and non-empty.
     * @param environmentId The target environment where the table should be created (e.g., DEVELOPMENT, STAGING,
     *                      PRODUCTION).
     */
    void createTable(long workspaceId, String baseName, List<ColumnSpec> columnSpecs, long environmentId);

    /**
     * Creates a new table in the specified workspace with the given parameters.
     *
     * @param baseName      The base name of the table to be created.
     * @param description   A description of the table to provide additional context.
     * @param columnSpecs   A list of column specifications that define the structure of the table.
     * @param workspaceId   The unique identifier of the workspace where the table will be added.
     * @param environmentUd The environment in which the table will be created.
     */
    void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long workspaceId, long environmentUd);

    /**
     * Deletes a dynamic data table in the specified environment with the given base name. This action is irreversible
     * and will permanently remove the table and its associated data.
     *
     * @param baseName      The logical base name of the table to be deleted. The name must not include a "dt_" prefix,
     *                      as it is added internally.
     * @param environmentId The target environmentID.
     */
    void dropTable(String baseName, long environmentId);

    /**
     * Duplicates an existing dynamic data table in the specified environment. The new table will have a different base
     * name while preserving the structure
     */
    void duplicateTable(String fromBaseName, String toBaseName, long environmentId);

    /**
     * Retrieves the base name of a dynamic data table by its unique identifier.
     *
     * @param id The unique identifier of the data table.
     * @return The base name of the data table corresponding to the given identifier. If no table is found for the
     *         provided ID, the method may return null or an empty string.
     */
    String getBaseNameById(long id);

    /**
     * Retrieves the unique identifier of a dynamic data table based on its base name.
     *
     * @param baseName The logical base name of the data table. The base name should not include a "dt_" prefix, as it
     *                 is internally managed by the service. Must not be null or empty.
     * @return The unique identifier of the data table corresponding to the given base name. If no table is found with
     *         the provided base name, the method may return -1 or a similar default value.
     */
    long getIdByBaseName(String baseName);

    /**
     * Retrieves a list of metadata for all dynamic data tables available in the specified environment.
     *
     * @param environmentId The target environment ID.
     * @return A list of {@code DataTableInfo} objects representing the metadata of the tables in the environment. If no
     *         tables exist, an empty list is returned.
     */
    List<DataTableInfo> listTables(long environmentId);

    /**
     * Retrieves a list of metadata for all dynamic data tables available in the specified environment and workspace.
     *
     * @param workspaceId   The identifier of the workspace within which to list the tables. Must be a valid
     *                      non-negative ID.
     * @param environmentId The target environment ID.
     * @return A list of {@code DataTableInfo} objects representing the metadata of the tables in the specified
     *         environment and workspace. If no tables exist, an empty list is returned.
     */
    List<DataTableInfo> listTables(long workspaceId, long environmentId);

    /**
     * Removes a column from an existing dynamic data table in the specified environment.
     *
     * @param baseName      The logical base name of the table from which the column will be removed. The name must not
     *                      include a "dt_" prefix.
     * @param columnName    The name of the column to be removed. This must match the existing column name in the table.
     * @param environmentId The target environment ID.
     */
    void removeColumn(String baseName, String columnName, long environmentId);

    /**
     * Renames a column in an existing dynamic data table in the specified environment.
     *
     * @param baseName       The logical base name of the table containing the column to be renamed. The name must not
     *                       include a "dt_" prefix.
     * @param fromColumnName The current name of the column to be renamed. This must match the existing column name in
     *                       the table.
     * @param toColumnName   The new name for the column. This name must not conflict with any existing columns in the
     *                       table.
     * @param environmentId  The target environment ID.
     */
    void renameColumn(String baseName, String fromColumnName, String toColumnName, long environmentId);

    /**
     * Renames an existing dynamic data table in the specified environment from one base name to another. This method
     * updates the logical base name of the table, which will also be reflected in its physical table representation.
     *
     * @param fromBaseName  The current logical base name of the table. This name must not include a "dt_" prefix and
     *                      must match an existing table.
     * @param toBaseName    The new logical base name for the table. This name must not include a "dt_" prefix and must
     *                      not conflict with any existing table's base name in the same environment .
     * @param environmentId The target environment ID.
     */
    void renameTable(String fromBaseName, String toBaseName, long environmentId);
}
