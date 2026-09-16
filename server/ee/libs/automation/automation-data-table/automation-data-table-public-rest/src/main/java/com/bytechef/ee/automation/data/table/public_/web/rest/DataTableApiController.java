/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.CreateColumnRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.CreateDataTableRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.RenameColumnRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.UpdateDataTableRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public DDL endpoints of the data table API. Every operation resolves the public name to the guarded id and delegates
 * to {@link WorkspaceDataTableFacade}; no authorization lives here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.data.table.public_.web.rest.DataTableApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class DataTableApiController extends AbstractDataTableApiController implements DataTableApi {

    private final WorkspaceDataTableFacade workspaceDataTableFacade;
    private final DataTableApiSupport support;

    @SuppressFBWarnings("EI2")
    public DataTableApiController(WorkspaceDataTableFacade workspaceDataTableFacade, DataTableApiSupport support) {
        this.workspaceDataTableFacade = workspaceDataTableFacade;
        this.support = support;
    }

    @Override
    public ResponseEntity<List<DataTableModel>> listDataTables(
        Long workspaceId, @Nullable EnvironmentModel xEnvironment, @Nullable String tag) {

        long environmentId = support.environmentId(xEnvironment);

        Map<Long, List<Tag>> tagsByTableId = workspaceDataTableFacade.getTagsByTableId(workspaceId);

        List<DataTableModel> dataTableModels = workspaceDataTableFacade.listTables(workspaceId, environmentId)
            .stream()
            .map(dataTableInfo -> DataTableModelMapper.toModel(
                dataTableInfo, tagsByTableId.getOrDefault(dataTableInfo.id(), List.of())))
            .filter(dataTableModel -> tag == null || dataTableModel.getTags()
                .contains(tag))
            .toList();

        return ResponseEntity.ok(dataTableModels);
    }

    @Override
    public ResponseEntity<DataTableModel> createDataTable(
        Long workspaceId, CreateDataTableRequestModel request, @Nullable EnvironmentModel xEnvironment) {

        DataTableApiSupport.validateTableName(request.getName());

        List<ColumnSpec> columnSpecs = request.getColumns()
            .stream()
            .map(DataTableModelMapper::toColumnSpec)
            .toList();
        long environmentId = support.environmentId(xEnvironment);

        workspaceDataTableFacade.createTable(
            request.getName(), request.getDescription(), columnSpecs, workspaceId, environmentId);

        long dataTableId = support.resolveTableId(request.getName());

        if (request.getTags() != null && !request.getTags()
            .isEmpty()) {

            workspaceDataTableFacade.updateTags(dataTableId, toTags(request.getTags()));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(table(dataTableId, workspaceId, environmentId));
    }

    @Override
    public ResponseEntity<DataTableModel> getDataTable(String name, @Nullable EnvironmentModel xEnvironment) {
        long dataTableId = support.resolveTableId(name);

        return ResponseEntity.ok(table(dataTableId, null, support.environmentId(xEnvironment)));
    }

    /**
     * Description and tags are each patched only when the caller sent them. Tags are three-state -- absent, explicit
     * null, or a list -- so the last tag on a table can be removed, which a "present and non-empty" guard cannot
     * express.
     */
    @Override
    public ResponseEntity<DataTableModel> updateDataTable(
        String name, UpdateDataTableRequestModel request, @Nullable EnvironmentModel xEnvironment) {

        long dataTableId = support.resolveTableId(name);

        if (request.getDescription() != null) {
            workspaceDataTableFacade.updateDescription(dataTableId, request.getDescription());
        }

        // Three-state, exactly as updateRow treats externalId: absent leaves the tags alone, an explicit null clears
        // them, and a list replaces them. Guarding on "non-empty" instead would make a table's last tag unremovable.
        JsonNullable<List<String>> tags = request.getTags();

        if (tags != null && tags.isPresent()) {
            List<String> tagNames = tags.get();

            workspaceDataTableFacade.updateTags(dataTableId, tagNames == null ? List.of() : toTags(tagNames));
        }

        return ResponseEntity.ok(table(dataTableId, null, support.environmentId(xEnvironment)));
    }

    @Override
    public ResponseEntity<Void> deleteDataTable(String name, @Nullable EnvironmentModel xEnvironment) {
        workspaceDataTableFacade.dropTable(support.resolveTableId(name), support.environmentId(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<DataTableModel> createColumn(
        String name, CreateColumnRequestModel request, @Nullable EnvironmentModel xEnvironment) {

        DataTableApiSupport.validateColumnName(request.getName());

        long dataTableId = support.resolveTableId(name);
        long environmentId = support.environmentId(xEnvironment);

        workspaceDataTableFacade.addColumn(
            dataTableId, new ColumnSpec(
                request.getName(), ColumnType.valueOf(request.getType()
                    .name())),
            environmentId);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(table(dataTableId, null, environmentId));
    }

    @Override
    public ResponseEntity<Void> deleteColumn(String name, String column, @Nullable EnvironmentModel xEnvironment) {
        DataTableApiSupport.validateColumnName(column);

        workspaceDataTableFacade.removeColumn(
            support.resolveTableId(name), column, support.environmentId(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<DataTableModel> renameColumn(
        String name, String column, RenameColumnRequestModel request, @Nullable EnvironmentModel xEnvironment) {

        DataTableApiSupport.validateColumnName(column);
        DataTableApiSupport.validateColumnName(request.getNewName());

        long dataTableId = support.resolveTableId(name);
        long environmentId = support.environmentId(xEnvironment);

        workspaceDataTableFacade.renameColumn(dataTableId, column, request.getNewName(), environmentId);

        return ResponseEntity.ok(table(dataTableId, null, environmentId));
    }

    /**
     * Tags live on the registry row, keyed by workspace; when the caller did not name the workspace (item URLs) the
     * table's own workspace is looked up through the facade.
     */
    private DataTableModel table(long dataTableId, @Nullable Long workspaceId, long environmentId) {
        DataTableInfo dataTableInfo = workspaceDataTableFacade.getTable(dataTableId, environmentId);

        long resolvedWorkspaceId = workspaceId != null
            ? workspaceId : workspaceDataTableFacade.getWorkspaceId(dataTableId);

        List<Tag> tags = workspaceDataTableFacade.getTagsByTableId(resolvedWorkspaceId)
            .getOrDefault(dataTableId, List.of());

        return DataTableModelMapper.toModel(dataTableInfo, tags);
    }

    private static List<Tag> toTags(List<String> names) {
        return names.stream()
            .map(String::trim)
            .filter(tagName -> !tagName.isEmpty())
            .distinct()
            .map(Tag::new)
            .toList();
    }
}
