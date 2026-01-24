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

package com.bytechef.automation.data.table.execution.service;

import com.bytechef.automation.data.table.execution.domain.DataTableRow;
import java.util.List;
import java.util.Map;

/**
 * Service for managing data table row operations (CRUD and CSV import/export).
 *
 * <p>
 * This service handles all data manipulation operations on dynamic data tables, while the structure (DDL) operations
 * are managed by {@link com.bytechef.automation.data.table.configuration.service.DataTableService}.
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
     * @param values        column name to value map
     * @param environmentId the environment ID
     * @return the created row with generated id
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
     * (case-insensitive). Unknown columns are ignored. The 'id' column, if present, is ignored.
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
     * @param values        column name to value map (only provided columns will be updated)
     * @param environmentId the environment ID
     * @return the updated row
     */
    DataTableRow updateRow(String baseName, long id, Map<String, Object> values, long environmentId);
}
