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

package com.bytechef.platform.data.table.execution.service;

import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Service for managing data table row operations (CRUD and CSV import/export).
 *
 * <p>
 * This service handles all data manipulation operations on dynamic data tables, while the structure (DDL) operations
 * are managed by {@link com.bytechef.platform.data.table.configuration.service.DataTableService}.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface DataTableRowService {

    /**
     * Deletes a row by id.
     *
     * @param baseName      the logical table base name
     * @param id            the row id
     * @param environmentId the environment ID
     * @return true if a row was deleted, false if no row with that id exists
     */
    boolean deleteRow(String baseName, long id, long environmentId);

    /**
     * Deletes every row that satisfies the given conditions in one transaction, publishing a deleted event per row.
     *
     * @param baseName      the logical table base name
     * @param conditions    OR groups of AND conditions, as in {@link #findRows}; at least one non-empty group is
     *                      required
     * @param environmentId the environment ID
     * @return the deleted rows ordered by id
     * @throws IllegalArgumentException if no non-empty condition group is given, a filter names an unknown column, has
     *                                  a value that is not valid for the column type, or uses a text operation on a
     *                                  non-text column
     */
    List<DataTableRow> deleteRows(String baseName, List<List<DataTableRowFilter>> conditions, long environmentId);

    /**
     * Gets a single row by its id.
     *
     * @param baseName      the logical table base name
     * @param id            the row id
     * @param environmentId the environment ID
     * @return the row if found, null otherwise
     */
    DataTableRow getRow(String baseName, long id, long environmentId);

    /**
     * Inserts a row with provided values. Returns the created row including generated id.
     *
     * @param baseName      the logical table base name
     * @param values        column name to value map; an id key is ignored and a null or blank value leaves the column
     *                      empty
     * @param environmentId the environment ID
     * @return the created row with generated id
     * @throws IllegalArgumentException if a value names an unknown column or is not valid for the column type
     */
    DataTableRow insertRow(String baseName, Map<String, Object> values, long environmentId);

    /**
     * Lists rows of a dynamic table with pagination.
     *
     * @param baseName      the logical table base name
     * @param limit         maximum number of rows to return
     * @param offset        number of rows to skip
     * @param environmentId the environment ID
     * @return list of rows with their data
     */
    List<DataTableRow> listRows(String baseName, int limit, int offset, long environmentId);

    /**
     * Lists rows of a dynamic table that satisfy the given conditions, ordered by the given sort, with pagination.
     *
     * @param baseName      the logical table base name
     * @param conditions    OR groups of AND conditions: a row matches when it satisfies every filter of at least one
     *                      group; empty groups are ignored, and no groups match every row
     * @param sort          the sort order, or null to order by id ascending
     * @param limit         maximum number of rows to return; must not be negative
     * @param offset        number of rows to skip; must not be negative
     * @param environmentId the environment ID
     * @return list of matching rows with their data
     * @throws IllegalArgumentException if limit or offset is negative, or a filter names an unknown column, has a value
     *                                  that is not valid for the column type, or uses a text operation on a non-text
     *                                  column
     */
    List<DataTableRow> findRows(
        String baseName, List<List<DataTableRowFilter>> conditions, @Nullable DataTableRowSort sort, int limit,
        int offset, long environmentId);

    /**
     * Exports the entire table (excluding the primary key column 'id' in the header) as CSV text. The first row is a
     * header with column names in their physical order.
     *
     * @param baseName      the logical table base name
     * @param environmentId the environment ID
     * @return CSV text representation of all rows
     */
    String exportCsv(String baseName, long environmentId);

    /**
     * Imports CSV text into the table. The CSV must contain a header row with column names matching existing columns
     * (case-insensitive). Unknown columns are ignored. The 'id' column, if present, is ignored. A cell that is not
     * valid for its column type stops the import; rows before it stay imported.
     *
     * @param baseName      the logical table base name
     * @param csv           CSV text with header row
     * @param environmentId the environment ID
     */
    void importCsv(String baseName, String csv, long environmentId);

    /**
     * Updates a row by its stable id. Returns the updated row.
     *
     * @param baseName      the logical table base name
     * @param id            the row id
     * @param values        column name to value map (only provided columns will be updated); an id key is ignored and a
     *                      null or blank value clears the column
     * @param environmentId the environment ID
     * @return the updated row
     * @throws IllegalArgumentException if a value names an unknown column or is not valid for the column type
     */
    DataTableRow updateRow(String baseName, long id, Map<String, Object> values, long environmentId);

    /**
     * Updates every row that satisfies the given conditions in one transaction, publishing an updated event per row.
     *
     * @param baseName      the logical table base name
     * @param conditions    OR groups of AND conditions, as in {@link #findRows}; at least one non-empty group is
     *                      required
     * @param values        column name to value map; at least one column is required, and values must not be null or
     *                      blank
     * @param environmentId the environment ID
     * @return the updated rows ordered by id
     * @throws IllegalArgumentException if no non-empty condition group or no column value is given, a filter is
     *                                  invalid, or a value names id or an unknown column, is null or blank, or is not
     *                                  valid for the column type
     */
    List<DataTableRow> updateRows(
        String baseName, List<List<DataTableRowFilter>> conditions, Map<String, Object> values, long environmentId);
}
