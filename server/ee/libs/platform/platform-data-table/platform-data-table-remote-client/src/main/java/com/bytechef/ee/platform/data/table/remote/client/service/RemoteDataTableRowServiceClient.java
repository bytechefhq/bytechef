/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.util.List;
import java.util.Map;
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
public class RemoteDataTableRowServiceClient implements DataTableRowService {
    @Override
    public List<DataTableRow> listRows(long dataTableId, int limit, int offset, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> findRows(
        long dataTableId, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int limit,
        int offset, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow insertRow(long dataTableId, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> insertRows(long dataTableId, List<Map<String, Object>> rowValues, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(long dataTableId, long id, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> updateRows(
        long dataTableId, DataTableRowConditions conditions, Map<String, Object> values, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteRow(long dataTableId, long id, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> deleteAllRows(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> deleteRows(
        long dataTableId, DataTableRowConditions conditions, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow getRow(long dataTableId, long id, long environmentId) {
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
    public long countRows(long dataTableId, DataTableRowConditions conditions, long environmentId) {
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
    public List<DataTableRow> insertRows(
        long dataTableId, List<NewRow> newRows, CreateStrategy createStrategy, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(
        long dataTableId, long id, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public UpsertResult upsertRow(long dataTableId, String externalId, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }
}
