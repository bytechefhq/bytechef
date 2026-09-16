/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.remote.client.facade;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService.Webhook;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteWorkspaceDataTableFacadeClient implements WorkspaceDataTableFacade {

    @Override
    public void addColumn(long dataTableId, ColumnSpec columnSpec, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long workspaceId, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void dropTable(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void duplicateTable(long dataTableId, String newBaseName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableInfo> listTables(long workspaceId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeColumn(long dataTableId, String columnName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameColumn(long dataTableId, String fromColumnName, String newName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameTable(long dataTableId, String newBaseName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> listRows(long dataTableId, int limit, int offset, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow insertRow(long dataTableId, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(long dataTableId, long rowId, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteRow(long dataTableId, long rowId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tag> getDataTableTags(long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String exportCsv(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int importCsv(long dataTableId, String csv, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTags(long dataTableId, List<Tag> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Webhook> listWebhooks(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableStorageUsage getStorageUsage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableInfo getTable(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDescription(long dataTableId, @Nullable String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<DataTableRow> listRows(
        long dataTableId, List<RowFilter> rowFilters, List<RowSort> rowSorts, int pageNumber, int pageSize,
        long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow getRow(long dataTableId, long rowId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DataTableRow> fetchRowByExternalId(long dataTableId, String externalId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow insertRow(
        long dataTableId, Map<String, Object> values, @Nullable String externalId, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(
        long dataTableId, long rowId, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public UpsertResult upsertRow(
        long dataTableId, String externalId, Map<String, Object> values, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteRowByExternalId(long dataTableId, String externalId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> insertRows(
        long dataTableId, List<NewRow> newRows, CreateStrategy createStrategy, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> deleteRows(long dataTableId, List<Long> rowIds, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long clearRows(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, List<Tag>> getTagsByTableId(long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getWorkspaceId(long dataTableId) {
        throw new UnsupportedOperationException();
    }
}
