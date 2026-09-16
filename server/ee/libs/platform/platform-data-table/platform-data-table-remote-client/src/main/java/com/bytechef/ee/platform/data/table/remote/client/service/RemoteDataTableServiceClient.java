/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteDataTableServiceClient implements DataTableService {

    @Override
    public void addColumn(long dataTableId, ColumnSpec columnSpec, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long createTable(
        @Nullable Long workspaceId, String name, @Nullable String description, List<ColumnSpec> columnSpecs,
        long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dropTable(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long duplicateTable(long dataTableId, String newName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DataTable> fetchDataTable(@Nullable Long workspaceId, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DataTableInfo> fetchDataTableInfo(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTable getDataTable(long dataTableId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTable> getWorkspaceDataTables(long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableInfo> listAllTables(long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableInfo> listTables(@Nullable Long workspaceId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeColumn(long dataTableId, String columnName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameColumn(long dataTableId, String fromColumnName, String toColumnName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameTable(long dataTableId, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDescription(long dataTableId, @Nullable String description) {
        throw new UnsupportedOperationException();
    }
}
