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

package com.bytechef.automation.data.table.web.graphql;

import static com.bytechef.commons.util.EncodingUtils.urlDecodeBase64FromString;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.data.table.configuration.domain.DataTableInfo;
import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.domain.ColumnSpec;
import com.bytechef.automation.data.table.domain.ColumnType;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing mutations for dynamic table DDL (configuration) operations.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class DataTableGraphQlController {

    private final DataTableService dataTableService;
    private final EnvironmentService environmentService;

    public DataTableGraphQlController(DataTableService dataTableService, EnvironmentService environmentService) {
        this.dataTableService = dataTableService;
        this.environmentService = environmentService;
    }

    @MutationMapping
    public boolean createDataTable(@Argument CreateDataTableInput input) {
        Long environmentId = input.environmentId();

        Environment environment = environmentService.getEnvironment(environmentId);

        List<ColumnSpec> columnSpecs = input.columns()
            .stream()
            .map(ColumnInput::toSpec)
            .toList();

        dataTableService.createTable(
            input.baseName(), input.description(), columnSpecs, input.workspaceId(), environment.ordinal());

        return true;
    }

    @MutationMapping
    public boolean addDataTableColumn(@Argument AddColumnInput input) {
        Long environmentId = input.environmentId();

        Environment environment = environmentService.getEnvironment(environmentId);
        String baseName = dataTableService.getBaseNameById(input.tableId());

        ColumnInput columnInput = input.column();

        dataTableService.addColumn(baseName, columnInput.toSpec(), environment.ordinal());

        return true;
    }

    @QueryMapping
    public List<DataTable> dataTables(@Argument Long environmentId, @Argument Long workspaceId) {
        Environment environment = environmentService.getEnvironment(environmentId);

        List<DataTableInfo> dataTableInfos = dataTableService.listTables(workspaceId, environment.ordinal());

        return dataTableInfos.stream()
            .map(info -> {
                Instant instant = info.lastModifiedDate();
                List<DataTableColumn> dataTableColumns = info.columns()
                    .stream()
                    .map(columnSpec -> new DataTableColumn(
                        EncodingUtils.urlEncodeBase64ToString(columnSpec.name()), columnSpec.name(), columnSpec.type()))
                    .toList();

                return new DataTable(
                    info.id(), info.baseName(), info.description(), dataTableColumns,
                    instant != null ? instant.toEpochMilli() : null);
            })
            .toList();
    }

    @MutationMapping
    public boolean dropDataTable(@Argument RemoveTableInput input) {
        Long environmentId = input.environmentId();

        Environment environment = environmentService.getEnvironment(environmentId);
        String baseName = dataTableService.getBaseNameById(input.tableId());

        dataTableService.dropTable(baseName, environment.ordinal());

        return true;
    }

    @MutationMapping
    public boolean duplicateDataTable(@Argument DuplicateDataTableInput input) {
        Long environmentId = input.environmentId();

        Environment environment = environmentService.getEnvironment(environmentId);
        String baseName = dataTableService.getBaseNameById(input.tableId());

        dataTableService.duplicateTable(baseName, input.newBaseName(), environment.ordinal());

        return true;
    }

    @MutationMapping
    public boolean removeDataTableColumn(@Argument RemoveColumnInput input) {
        Long environmentId = input.environmentId();

        Environment environment = environmentService.getEnvironment(environmentId);
        String baseName = dataTableService.getBaseNameById(input.tableId());
        String columnName = new String(urlDecodeBase64FromString(input.columnId()), UTF_8);

        dataTableService.removeColumn(baseName, columnName, environment.ordinal());

        return true;
    }

    @MutationMapping
    public boolean renameDataTableColumn(@Argument RenameColumnInput input) {
        Long environmentId = input.environmentId();

        Environment environment = environmentService.getEnvironment(environmentId);
        String baseName = dataTableService.getBaseNameById(input.tableId());
        String fromColumnName = new String(urlDecodeBase64FromString(input.columnId()), UTF_8);

        dataTableService.renameColumn(baseName, fromColumnName, input.newName(), environment.ordinal());

        return true;
    }

    @MutationMapping
    public boolean renameDataTable(@Argument RenameDataTableInput input) {
        Long environmentId = input.environmentId();

        Environment environment = environmentService.getEnvironment(environmentId);
        String baseName = dataTableService.getBaseNameById(input.tableId());

        dataTableService.renameTable(baseName, input.newBaseName(), environment.ordinal());

        return true;
    }

    @SuppressFBWarnings("EI")
    public record CreateDataTableInput(Long environmentId, String baseName, String description,
        List<ColumnInput> columns, Long workspaceId) {
    }

    @SuppressFBWarnings("EI")
    public record AddColumnInput(Long environmentId, Long tableId, ColumnInput column) {
    }

    public record RemoveColumnInput(Long environmentId, Long tableId, String columnId) {
    }

    public record RenameColumnInput(Long environmentId, Long tableId, String columnId, String newName) {
    }

    public record RemoveTableInput(Long environmentId, Long tableId) {
    }

    public record RenameDataTableInput(Long environmentId, Long tableId, String newBaseName) {
    }

    public record DuplicateDataTableInput(Long environmentId, Long tableId, String newBaseName) {
    }

    public record ColumnInput(String name, ColumnType type) {
        public ColumnSpec toSpec() {
            return new ColumnSpec(name, type);
        }
    }

    // GraphQL output records
    public record DataTable(Long id, String baseName, String description, List<DataTableColumn> columns,
        Long lastModifiedDate) {
    }

    public record DataTableColumn(String id, String name, ColumnType type) {
    }
}
