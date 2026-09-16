/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.BatchRowModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.BatchRowsRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.BatchRowsResponseModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.ClearRowsResponseModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.CreateRowRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.CreateStrategyModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableRowModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DeleteRowsResponseModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.ImportRowsResponseModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.UpdateRowRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.UpsertRowRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public row endpoints of the data table API. Every operation resolves the public name to the guarded id and delegates
 * to {@link WorkspaceDataTableFacade}; no authorization lives here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.data.table.public_.web.rest.DataTableRowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class DataTableRowApiController extends AbstractDataTableApiController implements DataTableRowApi {

    static final int DEFAULT_PAGE_SIZE = 50;
    static final int MAX_BATCH_SIZE = 1000;
    static final int MAX_PAGE_SIZE = 500;

    private final WorkspaceDataTableFacade workspaceDataTableFacade;
    private final DataTableApiSupport support;

    @SuppressFBWarnings("EI2")
    public DataTableRowApiController(WorkspaceDataTableFacade workspaceDataTableFacade, DataTableApiSupport support) {
        this.workspaceDataTableFacade = workspaceDataTableFacade;
        this.support = support;
    }

    @Override
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public ResponseEntity<Page> listRows(
        String name, @Nullable EnvironmentModel xEnvironment, @Nullable List<String> filter,
        @Nullable List<String> sort, @Nullable Integer pageNumber, @Nullable Integer pageSize) {

        Resolved resolved = resolve(name, xEnvironment);
        Set<String> queryableColumns = queryableColumns(resolved.dataTableInfo());

        List<RowFilter> rowFilters = RowQueryParser.parseFilters(filter, queryableColumns);
        List<RowSort> rowSorts = RowQueryParser.parseSorts(sort, queryableColumns);

        int resolvedPageNumber = pageNumber == null ? 0 : Math.max(0, pageNumber);
        int resolvedPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);

        Page<DataTableRow> page = workspaceDataTableFacade.listRows(
            resolved.dataTableId(), rowFilters, rowSorts, resolvedPageNumber, resolvedPageSize,
            resolved.environmentId());

        return ResponseEntity.ok(page.map(DataTableModelMapper::toModel));
    }

    @Override
    public ResponseEntity<DataTableRowModel> createRow(
        String name, CreateRowRequestModel createRowRequestModel, @Nullable EnvironmentModel xEnvironment) {

        Resolved resolved = resolve(name, xEnvironment);

        RowValuesValidator.validate(createRowRequestModel.getValues(), resolved.dataTableInfo()
            .columns());

        String externalId = createRowRequestModel.getExternalId();

        if (externalId != null) {
            RowValuesValidator.validateExternalId(externalId);
        }

        DataTableRow dataTableRow = workspaceDataTableFacade.insertRow(
            resolved.dataTableId(), createRowRequestModel.getValues(), externalId, resolved.environmentId());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(DataTableModelMapper.toModel(dataTableRow));
    }

    @Override
    public ResponseEntity<DataTableRowModel> getRow(String name, Long id, @Nullable EnvironmentModel xEnvironment) {
        long dataTableId = support.resolveTableId(name);

        DataTableRow dataTableRow =
            workspaceDataTableFacade.getRow(dataTableId, id, support.environmentId(xEnvironment));

        return ResponseEntity.ok(DataTableModelMapper.toModel(dataTableRow));
    }

    @Override
    public ResponseEntity<DataTableRowModel> updateRow(
        String name, Long id, UpdateRowRequestModel updateRowRequestModel, @Nullable EnvironmentModel xEnvironment) {

        Resolved resolved = resolve(name, xEnvironment);
        Map<String, Object> values = updateRowRequestModel.getValues() == null
            ? Map.of() : updateRowRequestModel.getValues();

        RowValuesValidator.validate(values, resolved.dataTableInfo()
            .columns());

        ExternalIdPatch externalIdPatch = null;

        JsonNullable<String> externalId = updateRowRequestModel.getExternalId();

        if (externalId != null && externalId.isPresent()) {
            String externalIdValue = externalId.get();

            if (externalIdValue != null) {
                RowValuesValidator.validateExternalId(externalIdValue);
            }

            externalIdPatch = new ExternalIdPatch(externalIdValue);
        }

        DataTableRow dataTableRow = workspaceDataTableFacade.updateRow(
            resolved.dataTableId(), id, values, externalIdPatch, resolved.environmentId());

        return ResponseEntity.ok(DataTableModelMapper.toModel(dataTableRow));
    }

    @Override
    public ResponseEntity<Void> deleteRow(String name, Long id, @Nullable EnvironmentModel xEnvironment) {
        boolean deleted = workspaceDataTableFacade.deleteRow(
            support.resolveTableId(name), id, support.environmentId(xEnvironment));

        if (!deleted) {
            throw new DataTableException("Row not found: id=" + id, DataTableErrorType.ROW_NOT_FOUND);
        }

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<DataTableRowModel> getRowByExternalId(
        String name, String externalId, @Nullable EnvironmentModel xEnvironment) {

        DataTableRow dataTableRow = workspaceDataTableFacade
            .fetchRowByExternalId(support.resolveTableId(name), externalId, support.environmentId(xEnvironment))
            .orElseThrow(() -> new DataTableException(
                "Row not found: externalId=" + externalId, DataTableErrorType.ROW_NOT_FOUND));

        return ResponseEntity.ok(DataTableModelMapper.toModel(dataTableRow));
    }

    @Override
    public ResponseEntity<DataTableRowModel> upsertRowByExternalId(
        String name, String externalId, UpsertRowRequestModel upsertRowRequestModel,
        @Nullable EnvironmentModel xEnvironment) {

        Resolved resolved = resolve(name, xEnvironment);

        RowValuesValidator.validateExternalId(externalId);
        RowValuesValidator.validate(upsertRowRequestModel.getValues(), resolved.dataTableInfo()
            .columns());

        UpsertResult upsertResult = workspaceDataTableFacade.upsertRow(
            resolved.dataTableId(), externalId, upsertRowRequestModel.getValues(), resolved.environmentId());

        return ResponseEntity.status(upsertResult.created() ? HttpStatus.CREATED : HttpStatus.OK)
            .body(DataTableModelMapper.toModel(upsertResult.row()));
    }

    @Override
    public ResponseEntity<Void> deleteRowByExternalId(
        String name, String externalId, @Nullable EnvironmentModel xEnvironment) {

        boolean deleted = workspaceDataTableFacade.deleteRowByExternalId(
            support.resolveTableId(name), externalId, support.environmentId(xEnvironment));

        if (!deleted) {
            throw new DataTableException("Row not found: externalId=" + externalId, DataTableErrorType.ROW_NOT_FOUND);
        }

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<BatchRowsResponseModel> batchRows(
        String name, BatchRowsRequestModel batchRowsRequestModel, @Nullable EnvironmentModel xEnvironment) {

        Resolved resolved = resolve(name, xEnvironment);
        List<BatchRowModel> batchRowModels = batchRowsRequestModel.getRows();

        if (batchRowModels == null || batchRowModels.size() > MAX_BATCH_SIZE) {
            throw new DataTableException(
                "A batch holds at most " + MAX_BATCH_SIZE + " rows", DataTableErrorType.BATCH_TOO_LARGE);
        }

        List<NewRow> newRows = new ArrayList<>(batchRowModels.size());

        for (BatchRowModel batchRowModel : batchRowModels) {
            RowValuesValidator.validate(batchRowModel.getValues(), resolved.dataTableInfo()
                .columns());

            String externalId = batchRowModel.getExternalId();

            if (externalId != null) {
                RowValuesValidator.validateExternalId(externalId);
            }

            newRows.add(new NewRow(batchRowModel.getValues(), externalId));
        }

        CreateStrategyModel createStrategyModel = batchRowsRequestModel.getCreateStrategy();
        CreateStrategy createStrategy = createStrategyModel == null
            ? CreateStrategy.INSERT
            : CreateStrategy.valueOf(createStrategyModel.name());

        List<DataTableRow> dataTableRows = workspaceDataTableFacade.insertRows(
            resolved.dataTableId(), newRows, createStrategy, resolved.environmentId());

        return ResponseEntity.ok(
            new BatchRowsResponseModel().rows(
                dataTableRows.stream()
                    .map(DataTableModelMapper::toModel)
                    .toList()));
    }

    @Override
    public ResponseEntity<DeleteRowsResponseModel> deleteRows(
        String name, List<Long> ids, @Nullable EnvironmentModel xEnvironment) {

        if (ids == null || ids.isEmpty()) {
            throw new DataTableException("ids is required", DataTableErrorType.ROW_VALUE_INVALID);
        }

        if (ids.size() > MAX_BATCH_SIZE) {
            throw new DataTableException(
                "At most " + MAX_BATCH_SIZE + " ids per request", DataTableErrorType.BATCH_TOO_LARGE);
        }

        List<Long> deletedIds = workspaceDataTableFacade.deleteRows(
            support.resolveTableId(name), ids, support.environmentId(xEnvironment));

        return ResponseEntity.ok(
            new DeleteRowsResponseModel().deletedCount(deletedIds.size())
                .deletedIds(deletedIds));
    }

    @Override
    public ResponseEntity<ClearRowsResponseModel> clearRows(String name, @Nullable EnvironmentModel xEnvironment) {
        long deletedCount = workspaceDataTableFacade.clearRows(
            support.resolveTableId(name), support.environmentId(xEnvironment));

        return ResponseEntity.ok(new ClearRowsResponseModel().deletedCount(deletedCount));
    }

    @Override
    public ResponseEntity<ImportRowsResponseModel> importRows(
        String name, String body, @Nullable EnvironmentModel xEnvironment) {

        int importedCount = workspaceDataTableFacade.importCsv(
            support.resolveTableId(name), body, support.environmentId(xEnvironment));

        return ResponseEntity.ok(new ImportRowsResponseModel().importedCount(importedCount));
    }

    @Override
    public ResponseEntity<String> exportRows(String name, @Nullable EnvironmentModel xEnvironment) {
        String csv = workspaceDataTableFacade.exportCsv(
            support.resolveTableId(name), support.environmentId(xEnvironment));

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + ".csv\"")
            .body(csv);
    }

    private Resolved resolve(String name, @Nullable EnvironmentModel xEnvironment) {
        long dataTableId = support.resolveTableId(name);
        long environmentId = support.environmentId(xEnvironment);

        return new Resolved(
            dataTableId, environmentId, workspaceDataTableFacade.getTable(dataTableId, environmentId));
    }

    private static Set<String> queryableColumns(DataTableInfo dataTableInfo) {
        Set<String> columns = new HashSet<>();

        columns.add(ReservedColumns.ID);
        columns.add(ReservedColumns.EXTERNAL_ID);

        for (ColumnSpec columnSpec : dataTableInfo.columns()) {
            columns.add(columnSpec.name()
                .toLowerCase(Locale.ROOT));
        }

        return columns;
    }

    private record Resolved(long dataTableId, long environmentId, DataTableInfo dataTableInfo) {
    }
}
