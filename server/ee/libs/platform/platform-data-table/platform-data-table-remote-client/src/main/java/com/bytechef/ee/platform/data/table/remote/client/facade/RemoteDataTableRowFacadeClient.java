/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.facade;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.data.table.execution.facade.DataTableRowFacade;
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
public class RemoteDataTableRowFacadeClient implements DataTableRowFacade {

    @Override
    public int importCsv(String baseName, String csv, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow insertRow(String baseName, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(String baseName, long id, Map<String, Object> values, long environmentId) {
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
