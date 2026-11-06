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
    public List<DataTableRow> listRows(String baseName, int limit, int offset, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> findRows(
        String baseName, DataTableRowConditions conditions, @Nullable DataTableRowSort sort, int limit,
        int offset, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow insertRow(String baseName, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> insertRows(String baseName, List<Map<String, Object>> rowValues, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(String baseName, long id, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> updateRows(
        String baseName, DataTableRowConditions conditions, Map<String, Object> values, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteRow(String baseName, long id, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> deleteAllRows(String baseName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> deleteRows(
        String baseName, DataTableRowConditions conditions, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow getRow(String baseName, long id, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String exportCsv(String baseName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int importCsv(String baseName, String csv, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long countRows(String baseName, DataTableRowConditions conditions, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DataTableRow> fetchRowByExternalId(String baseName, String externalId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow insertRow(
        String baseName, Map<String, Object> values, @Nullable String externalId, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> insertRows(
        String baseName, List<NewRow> newRows, CreateStrategy createStrategy, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> queryRows(
        String baseName, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int limit, int offset,
        long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(
        String baseName, long id, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public UpsertResult upsertRow(String baseName, String externalId, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }
}
