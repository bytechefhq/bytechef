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
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
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
    public boolean deleteRow(long dataTableId, long rowId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean dropTable(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void duplicateTable(long tableId, String newBaseName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getDataTableId(String baseName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable DataTableRow getRow(long dataTableId, long rowId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow insertRow(long dataTableId, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> listRows(long dataTableId, int limit, int offset, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableInfo> listTables(long workspaceId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(long dataTableId, long rowId, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }
}
