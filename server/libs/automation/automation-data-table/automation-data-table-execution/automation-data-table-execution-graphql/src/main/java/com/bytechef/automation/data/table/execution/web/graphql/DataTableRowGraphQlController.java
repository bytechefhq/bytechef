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

package com.bytechef.automation.data.table.execution.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.execution.domain.DataTableRow;
import com.bytechef.automation.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing queries and mutations for data table row operations (DML).
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class DataTableRowGraphQlController {

    private final DataTableRowService dataTableRowService;
    private final DataTableService dataTableService;

    public DataTableRowGraphQlController(
        DataTableRowService dataTableRowService, DataTableService dataTableService) {

        this.dataTableRowService = dataTableRowService;
        this.dataTableService = dataTableService;
    }

    @QueryMapping
    public List<Row> dataTableRows(@Argument Long environmentId, @Argument Long tableId) {
        String baseName = dataTableService.getBaseNameById(tableId);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            baseName, Integer.MAX_VALUE, 0, environmentId);

        return dataTableRows.stream()
            .map(dataTableRow -> new Row(dataTableRow.id(), dataTableRow.values()))
            .toList();
    }

    @QueryMapping
    public RowPage dataTableRowsPage(
        @Argument Long environmentId, @Argument Long tableId, @Argument Integer limit, @Argument Integer offset) {

        String baseName = dataTableService.getBaseNameById(tableId);
        int pageSize = (limit == null || limit <= 0) ? 100 : limit;
        int pageOffset = (offset == null || offset < 0) ? 0 : offset;

        List<DataTableRow> fetched = dataTableRowService.listRows(
            baseName, pageSize + 1, pageOffset, environmentId);

        boolean hasMore = fetched.size() > pageSize;

        List<DataTableRow> pageItems = hasMore ? fetched.subList(0, pageSize) : fetched;

        List<Row> items = pageItems.stream()
            .map(dataTableRow -> new Row(dataTableRow.id(), dataTableRow.values()))
            .toList();

        Integer nextOffset = hasMore ? pageOffset + pageSize : null;

        return new RowPage(items, hasMore, nextOffset);
    }

    @MutationMapping
    public Row insertDataTableRow(@Argument InsertRowInput input) {
        Long environmentId = input.environmentId();
        String baseName = dataTableService.getBaseNameById(input.tableId());

        DataTableRow dataTableRow = dataTableRowService.insertRow(
            baseName, input.values(), environmentId);

        return new Row(dataTableRow.id(), dataTableRow.values());
    }

    @MutationMapping
    public Row updateDataTableRow(@Argument UpdateRowInput input) {
        Long environmentId = input.environmentId();
        String baseName = dataTableService.getBaseNameById(input.tableId());

        DataTableRow row = dataTableRowService.updateRow(
            baseName, input.id(), input.values(), environmentId);

        return new Row(row.id(), row.values());
    }

    @MutationMapping
    public boolean deleteDataTableRow(@Argument DeleteRowInput input) {
        Long environmentId = input.environmentId();
        String baseName = dataTableService.getBaseNameById(input.tableId());

        return dataTableRowService.deleteRow(baseName, input.id(), environmentId);
    }

    @QueryMapping
    public String exportDataTableCsv(@Argument Long environmentId, @Argument Long tableId) {
        String baseName = dataTableService.getBaseNameById(tableId);

        return dataTableRowService.exportCsv(baseName, environmentId);
    }

    @MutationMapping
    public boolean importDataTableCsv(@Argument ImportCsvInput input) {
        Long environmentId = input.environmentId();

        String baseName = dataTableService.getBaseNameById(input.tableId());

        dataTableRowService.importCsv(baseName, input.csv(), environmentId);

        return true;
    }

    public record Row(Long id, Map<String, Object> values) {
    }

    public record RowPage(List<Row> items, boolean hasMore, Integer nextOffset) {
    }

    @SuppressFBWarnings("EI")
    public record InsertRowInput(Long environmentId, Long tableId, Map<String, Object> values) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateRowInput(Long environmentId, Long tableId, Long id, Map<String, Object> values) {
    }

    public record DeleteRowInput(Long environmentId, Long tableId, Long id) {
    }

    public record ImportCsvInput(Long environmentId, Long tableId, String csv) {
    }
}
