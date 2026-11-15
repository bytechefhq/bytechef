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
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
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

    private final EnvironmentService environmentService;
    private final WorkspaceDataTableFacade workspaceDataTableFacade;

    public DataTableGraphQlController(
        EnvironmentService environmentService, WorkspaceDataTableFacade workspaceDataTableFacade) {

        this.environmentService = environmentService;
        this.workspaceDataTableFacade = workspaceDataTableFacade;
    }

    @MutationMapping
    public boolean createDataTable(@Argument CreateDataTableInput input) {
        Long environmentId = input.environmentId();

        Environment environment = environmentService.getEnvironment(environmentId);

        List<ColumnSpec> columnSpecs = input.columns()
            .stream()
            .map(ColumnInput::toSpec)
            .toList();

        workspaceDataTableFacade.createTable(
            input.baseName(), input.description(), columnSpecs, input.workspaceId(), environment.ordinal());

        return true;
    }

    @MutationMapping
    public boolean addDataTableColumn(@Argument AddColumnInput input) {
        Environment environment = environmentService.getEnvironment(input.environmentId());

        workspaceDataTableFacade.addColumn(input.tableId(), input.column()
            .toSpec(), environment.ordinal());

        return true;
    }

    @QueryMapping
    public List<DataTable> dataTables(@Argument Long environmentId, @Argument Long workspaceId) {
        Environment environment = environmentService.getEnvironment(environmentId);

        List<DataTableInfo> dataTableInfos = workspaceDataTableFacade.listTables(workspaceId, environment.ordinal());

        Map<Long, String> tableNames = new HashMap<>();

        for (DataTableInfo dataTableInfo : dataTableInfos) {
            tableNames.put(dataTableInfo.id(), dataTableInfo.name());
        }

        return dataTableInfos.stream()
            .map(info -> {
                Instant instant = info.lastModifiedDate();
                List<DataTableColumn> dataTableColumns = info.columns()
                    .stream()
                    .map(columnSpec -> new DataTableColumn(
                        EncodingUtils.urlEncodeBase64ToString(columnSpec.name()), columnSpec.name(), columnSpec.type(),
                        toLink(columnSpec.link(), tableNames, environment.ordinal())))
                    .toList();

                return new DataTable(
                    info.id(), info.name(), info.description(), dataTableColumns,
                    instant != null ? instant.toEpochMilli() : null);
            })
            .toList();
    }

    private DataTableColumnLink toLink(@Nullable LinkSpec linkSpec, Map<Long, String> tableNames, long environmentId) {
        if (linkSpec == null) {
            return null;
        }

        String targetDataTableName = tableNames.computeIfAbsent(
            linkSpec.targetDataTableId(),
            targetDataTableId -> workspaceDataTableFacade.getTable(targetDataTableId, environmentId)
                .name());

        return new DataTableColumnLink(
            linkSpec.targetDataTableId(), targetDataTableName, linkSpec.onDelete(), linkSpec.displayColumnName());
    }

    @QueryMapping
    public DataTableStorageUsage dataTableStorageUsage() {
        return workspaceDataTableFacade.getStorageUsage();
    }

    @MutationMapping
    public boolean dropDataTable(@Argument RemoveTableInput input) {
        Environment environment = environmentService.getEnvironment(input.environmentId());

        workspaceDataTableFacade.dropTable(input.tableId(), environment.ordinal());

        return true;
    }

    @MutationMapping
    public boolean duplicateDataTable(@Argument DuplicateDataTableInput input) {
        Environment environment = environmentService.getEnvironment(input.environmentId());

        workspaceDataTableFacade.duplicateTable(input.tableId(), input.newBaseName(), environment.ordinal());

        return true;
    }

    @MutationMapping
    public boolean removeDataTableColumn(@Argument RemoveColumnInput input) {
        Environment environment = environmentService.getEnvironment(input.environmentId());
        String columnName = new String(urlDecodeBase64FromString(input.columnId()), UTF_8);

        workspaceDataTableFacade.removeColumn(input.tableId(), columnName, environment.ordinal());

        return true;
    }

    @MutationMapping
    public boolean renameDataTableColumn(@Argument RenameColumnInput input) {
        Environment environment = environmentService.getEnvironment(input.environmentId());
        String fromColumnName = new String(urlDecodeBase64FromString(input.columnId()), UTF_8);

        workspaceDataTableFacade.renameColumn(input.tableId(), fromColumnName, input.newName(), environment.ordinal());

        return true;
    }

    @MutationMapping
    public boolean renameDataTable(@Argument RenameDataTableInput input) {
        workspaceDataTableFacade.renameTable(input.tableId(), input.newBaseName());

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

    public record ColumnInput(String name, ColumnType type, @Nullable ColumnLinkInput link) {
        public ColumnSpec toSpec() {
            if (link == null) {
                return new ColumnSpec(name, type);
            }

            return new ColumnSpec(
                name, type, new LinkSpec(link.targetDataTableId(), link.onDelete(), link.displayColumnName()));
        }
    }

    public record ColumnLinkInput(Long targetDataTableId, LinkDeleteRule onDelete, @Nullable String displayColumnName) {
    }

    // GraphQL output records
    public record DataTable(Long id, String baseName, String description, List<DataTableColumn> columns,
        Long lastModifiedDate) {
    }

    public record DataTableColumn(String id, String name, ColumnType type, @Nullable DataTableColumnLink link) {
    }

    public record DataTableColumnLink(
        Long targetDataTableId, String targetDataTableName, LinkDeleteRule onDelete,
        @Nullable String displayColumnName) {
    }
}
