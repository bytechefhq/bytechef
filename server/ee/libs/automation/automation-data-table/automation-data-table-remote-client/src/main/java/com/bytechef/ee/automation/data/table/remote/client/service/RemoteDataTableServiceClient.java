/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.remote.client.service;

import com.bytechef.automation.data.table.configuration.domain.DataTableInfo;
import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.domain.ColumnSpec;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
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
    public void createTable(String baseName, List<ColumnSpec> columnSpecs, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createTable(String baseName, String description, List<ColumnSpec> columnSpecs, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addColumn(String baseName, ColumnSpec columnSpec, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeColumn(String baseName, String columnName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameColumn(String baseName, String fromColumnName, String toColumnName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dropTable(String baseName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableInfo> listTables(long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableInfo> listTables(long workspaceId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBaseNameById(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getIdByBaseName(String baseName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameTable(String fromBaseName, String toBaseName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void duplicateTable(String fromBaseName, String toBaseName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createTable(long workspaceId, String baseName, List<ColumnSpec> columnSpecs, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createTable(
        String baseName, String description, List<ColumnSpec> columnSpecs, long workspaceId, long environmentId) {
        throw new UnsupportedOperationException();
    }
}
