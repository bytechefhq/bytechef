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

import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Service for managing data table row operations (CRUD and CSV import/export).
 *
 * <p>
 * This service handles all data manipulation operations on dynamic data tables, while the structure (DDL) operations
 * are managed by {@link com.bytechef.platform.data.table.configuration.service.DataTableService}.
 * </p>
 *
 * <p>
 * Every method names its table with a {@link DataTableRef} rather than with a base name, so the table a statement
 * reaches cannot differ from the one resolution picked: there is no base name here for the service to address a table
 * with on its own. Get a ref from {@code DataTableService.fetchDataTableResolution}; see {@link DataTableRef} for the
 * one other, deliberately narrow, way to build one.
 * </p>
 *
 * <p>
 * Reads and writes are scoped differently, and the difference is deliberate: a read admits the run's own rows and the
 * ones belonging to nobody, a write matches the run's alone. A vendor-seeded reference row is every account's to read
 * and nobody's to change.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface DataTableRowService {

    /**
     * Deletes a row by id.
     *
     * @param dataTableRef the resolved physical table
     * @param id           the row id
     * @return true if a row was deleted, false if no row with that id exists
     */
    boolean deleteRow(DataTableRef dataTableRef, long id);

    /**
     * Gets a single row by its id.
     *
     * @param dataTableRef the resolved physical table
     * @param id           the row id
     * @return the row if found, null otherwise
     */
    DataTableRow getRow(DataTableRef dataTableRef, long id);

    /**
     * Gets a single row by its external id.
     *
     * @param dataTableRef the resolved physical table
     * @param externalId   the caller-supplied external id
     * @return the row if found, empty otherwise
     */
    Optional<DataTableRow> fetchRowByExternalId(DataTableRef dataTableRef, String externalId);

    /**
     * Inserts a row with provided values. Returns the created row including generated id.
     *
     * @param dataTableRef the resolved physical table
     * @param values       column name to value map
     * @return the created row with generated id
     */
    DataTableRow insertRow(DataTableRef dataTableRef, Map<String, Object> values);

    /**
     * Inserts a row with provided values and an optional external id. Returns the created row including generated id.
     *
     * @param dataTableRef the resolved physical table
     * @param values       column name to value map
     * @param externalId   the caller-supplied external id, or null to leave it unset
     * @return the created row with generated id
     */
    DataTableRow insertRow(DataTableRef dataTableRef, Map<String, Object> values, @Nullable String externalId);

    /**
     * Lists rows of a dynamic table with pagination.
     *
     * @param dataTableRef the resolved physical table
     * @param limit        maximum number of rows to return
     * @param offset       number of rows to skip
     * @return list of rows with their data
     */
    List<DataTableRow> listRows(DataTableRef dataTableRef, int limit, int offset);

    /**
     * Filtered and sorted form. These filters narrow within what the run may already see, and are ANDed onto the row
     * owner predicate rather than replacing it -- a workflow can ask for less than its own rows and never for more. See
     * {@link RowFilter} and {@link RowSort}.
     */
    List<DataTableRow> listRows(
        DataTableRef dataTableRef, int limit, int offset, List<RowFilter> rowFilters, List<RowSort> rowSorts);

    /**
     * Exports the entire table (excluding the primary key column 'id' in the header) as CSV text. The first row is a
     * header with column names in their physical order.
     *
     * @param dataTableRef the resolved physical table
     * @return CSV text representation of all rows
     */
    String exportCsv(DataTableRef dataTableRef);

    /**
     * Imports CSV text into the table. The CSV must contain a header row with column names matching existing columns
     * (case-insensitive). The reserved column 'id' is ignored if present; a header naming 'external_id' sets each row's
     * external id rather than a column value. Any other header that names no column of the table is rejected, throwing
     * {@code DataTableException} with error type
     * {@link com.bytechef.platform.data.table.configuration.exception.DataTableErrorType#CSV_INVALID}.
     *
     * @param dataTableRef the resolved physical table
     * @param csv          CSV text with header row
     * @return the number of rows inserted
     * @throws DataTableException when a header names no column of the table
     */
    int importCsv(DataTableRef dataTableRef, String csv);

    /**
     * Updates a row by its stable id. Returns the updated row.
     *
     * @param dataTableRef the resolved physical table
     * @param id           the row id
     * @param values       column name to value map (only provided columns will be updated)
     * @return the updated row
     */
    DataTableRow updateRow(DataTableRef dataTableRef, long id, Map<String, Object> values);

    /**
     * Updates a row by its stable id, optionally patching its external id. Returns the updated row.
     *
     * @param dataTableRef    the resolved physical table
     * @param id              the row id
     * @param values          column name to value map (only provided columns will be updated)
     * @param externalIdPatch the requested change to the row's external id, or null to leave it untouched
     * @return the updated row
     */
    DataTableRow updateRow(
        DataTableRef dataTableRef, long id, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch);

    /**
     * Inserts the row keyed by {@code externalId} or merges {@code values} into the row that already carries it, in one
     * statement. The conflict target is the ownership index, so the owner the ref carries is part of the key: an
     * account can never upsert onto another account's row. Only the supplied columns are written on the update path.
     *
     * @param dataTableRef the resolved physical table
     * @param externalId   the caller-supplied external id to key the upsert on
     * @param values       column name to value map (only provided columns will be written)
     * @return the row the upsert left behind and whether it had to create it
     */
    UpsertResult upsertRow(DataTableRef dataTableRef, String externalId, Map<String, Object> values);

    /**
     * Counts the rows a run may read that also satisfy {@code rowFilters}, using the same filter fragment as the
     * filtered {@link #listRows}, so a page's {@code totalElements} agrees with its content.
     *
     * @param dataTableRef the resolved physical table
     * @param rowFilters   the filters to apply, ANDed onto the row owner predicate
     * @return the matching row count
     */
    long countRows(DataTableRef dataTableRef, List<RowFilter> rowFilters);

    /**
     * Deletes the rows among {@code ids} the ref may write; returns the ids that were actually deleted.
     *
     * @param dataTableRef the resolved physical table
     * @param ids          the candidate row ids
     * @return the ids that were actually deleted
     */
    List<Long> deleteRows(DataTableRef dataTableRef, List<Long> ids);

    /**
     * Deletes every row the ref may write; returns the count.
     *
     * @param dataTableRef the resolved physical table
     * @return the number of rows deleted
     */
    long clearRows(DataTableRef dataTableRef);

    /**
     * Inserts (or, under {@link CreateStrategy#UPSERT}, upserts) every row in one transaction: one failure rolls back
     * all of them. UPSERT requires an external id on every row, throwing {@code DataTableException} with error type
     * {@link com.bytechef.platform.data.table.configuration.exception.DataTableErrorType#ROW_EXTERNAL_ID_REQUIRED} when
     * a row is missing one.
     *
     * @param dataTableRef   the resolved physical table
     * @param newRows        the rows to create
     * @param createStrategy whether to plainly insert or upsert on external id
     * @return the created (or upserted) rows
     * @throws DataTableException when {@code createStrategy} is {@link CreateStrategy#UPSERT} and a row has no external
     *                            id
     */
    List<DataTableRow> insertRows(DataTableRef dataTableRef, List<NewRow> newRows, CreateStrategy createStrategy);
}
