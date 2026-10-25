/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
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
    public List<DataTableRow> listRows(DataTableRef dataTableRef, int limit, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> listRows(
        DataTableRef dataTableRef, int limit, int offset, List<RowFilter> rowFilters, List<RowSort> rowSorts) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow insertRow(DataTableRef dataTableRef, Map<String, Object> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow insertRow(DataTableRef dataTableRef, Map<String, Object> values, @Nullable String externalId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(DataTableRef dataTableRef, long id, Map<String, Object> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(
        DataTableRef dataTableRef, long id, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteRow(DataTableRef dataTableRef, long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow getRow(DataTableRef dataTableRef, long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DataTableRow> fetchRowByExternalId(DataTableRef dataTableRef, String externalId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String exportCsv(DataTableRef dataTableRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int importCsv(DataTableRef dataTableRef, String csv) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UpsertResult upsertRow(DataTableRef dataTableRef, String externalId, Map<String, Object> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long countRows(DataTableRef dataTableRef, List<RowFilter> rowFilters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> deleteRows(DataTableRef dataTableRef, List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long clearRows(DataTableRef dataTableRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow>
        insertRows(DataTableRef dataTableRef, List<NewRow> newRows, CreateStrategy createStrategy) {
        throw new UnsupportedOperationException();
    }
}
