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
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
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
     * Deletes every row of the table in one transaction. One deleted event holding every deleted row is published;
     * webhooks are called once the transaction commits. When the table is empty, nothing is published and an empty list
     * is returned.
     *
     * @param baseName      the logical table base name
     * @param environmentId the environment ID
     * @return the deleted rows ordered by id
     */
    List<DataTableRow> deleteAllRows(String baseName, long environmentId);

    /**
     * Deletes every row that satisfies the given conditions in one transaction. One deleted event holding every deleted
     * row is published; webhooks are called once the transaction commits. When no row matches, nothing is deleted and
     * an empty list is returned.
     *
     * @param baseName      the logical table base name
     * @param conditions    the conditions selecting the rows to delete; must contain at least one condition
     * @param environmentId the environment ID
     * @return the deleted rows ordered by id
     * @throws IllegalArgumentException if the conditions contain no condition, a filter names an unknown column, has a
     *                                  value that is not valid for the column type, or uses a text operation on a
     *                                  non-text column
     */
    List<DataTableRow> deleteRows(String baseName, DataTableRowConditions conditions, long environmentId);

    /**
     * Gets a single row by its id.
     *
     * @param baseName      the logical table base name
     * @param id            the row id
     * @param environmentId the environment ID
     * @return the row if found, null otherwise
     */
    @Nullable
    DataTableRow getRow(String baseName, long id, long environmentId);

    /**
     * Inserts a row with provided values. Returns the created row including generated id.
     *
     * @param baseName      the logical table base name
     * @param values        column name to value map; an id key and keys naming no column are ignored, a null value
     *                      stores null and a blank value stores an empty string in a text column and null otherwise
     * @param environmentId the environment ID
     * @return the created row with generated id
     * @throws IllegalArgumentException if a value is not valid for the column type, or there are keys other than id and
     *                                  none of them names a column
     */
    DataTableRow insertRow(String baseName, Map<String, Object> values, long environmentId);

    /**
     * Inserts rows in one transaction: a row that is not valid stops the insert and no row is inserted. One created
     * event holding every inserted row is published; webhooks are called once the transaction commits.
     *
     * @param baseName      the logical table base name
     * @param rowValues     one column name to value map per row, with the same rules as {@link #insertRow}
     * @param environmentId the environment ID
     * @return the created rows with generated ids, in the given order
     * @throws IllegalArgumentException if a value is not valid for the column type; the message names the row
     */
    List<DataTableRow> insertRows(String baseName, List<Map<String, Object>> rowValues, long environmentId);

    /**
     * Lists rows of a dynamic table ordered by id ascending, with pagination.
     *
     * @param baseName      the logical table base name
     * @param limit         maximum number of rows to return; must not be negative
     * @param offset        number of rows to skip; must not be negative
     * @param environmentId the environment ID
     * @return list of rows with their data
     * @throws IllegalArgumentException if limit or offset is negative
     */
    List<DataTableRow> listRows(String baseName, int limit, int offset, long environmentId);

    /**
     * Lists rows of a dynamic table that satisfy the given conditions, ordered by the given sort, with pagination.
     *
     * @param baseName      the logical table base name
     * @param conditions    the conditions a row must satisfy; {@link DataTableRowConditions#matchAll()} returns every
     *                      row
     * @param sort          the sort order, or null to order by id ascending
     * @param limit         maximum number of rows to return; must not be negative
     * @param offset        number of rows to skip; must not be negative
     * @param environmentId the environment ID
     * @return list of matching rows with their data
     * @throws IllegalArgumentException if limit or offset is negative, the sort names an unknown column, or a filter
     *                                  names an unknown column, has a value that is not valid for the column type, or
     *                                  uses a text operation on a non-text column
     */
    List<DataTableRow> findRows(
        String baseName, DataTableRowConditions conditions, @Nullable DataTableRowSort sort, int limit,
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
     * (case-insensitive). Unknown columns are ignored. The 'id' column, if present, is ignored. The import runs in one
     * transaction: a cell that is not valid for its column type stops the import and no row is imported.
     *
     * @param baseName      the logical table base name
     * @param csv           CSV text with header row
     * @param environmentId the environment ID
     * @throws IllegalArgumentException if the header matches no column of the table, or a cell is not valid for its
     *                                  column type; the message names the CSV line
     */
    void importCsv(String baseName, String csv, long environmentId);

    /**
     * Updates a row by its stable id. Returns the updated row.
     *
     * @param baseName      the logical table base name
     * @param id            the row id
     * @param values        column name to value map (only provided columns will be updated); an id key and keys naming
     *                      no column are ignored, a null value sets the column to null and a blank value clears it,
     *                      leaving an empty string in a text column and null otherwise
     * @param environmentId the environment ID
     * @return the updated row
     * @throws IllegalArgumentException if a value is not valid for the column type, there are keys other than id and
     *                                  none of them names a column, or no row with that id exists
     */
    DataTableRow updateRow(String baseName, long id, Map<String, Object> values, long environmentId);

    /**
     * Updates every row that satisfies the given conditions in one transaction. One updated event holding every updated
     * row is published; webhooks are called once the transaction commits. When no row matches, nothing is updated and
     * an empty list is returned.
     *
     * @param baseName      the logical table base name
     * @param conditions    the conditions selecting the rows to update; must contain at least one condition
     * @param values        column name to value map; at least one column is required, and values must not be null or
     *                      blank
     * @param environmentId the environment ID
     * @return the updated rows ordered by id
     * @throws IllegalArgumentException if the conditions contain no condition, no column value is given, a filter is
     *                                  invalid, or a value names id or an unknown column, is null or blank, or is not
     *                                  valid for the column type
     */
    List<DataTableRow> updateRows(
        String baseName, DataTableRowConditions conditions, Map<String, Object> values, long environmentId);
}
