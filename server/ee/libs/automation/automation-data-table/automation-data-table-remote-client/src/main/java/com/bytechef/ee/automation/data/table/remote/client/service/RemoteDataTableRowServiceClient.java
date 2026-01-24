/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.remote.client.service;

import com.bytechef.automation.data.table.execution.domain.DataTableRow;
import com.bytechef.automation.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Map;
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
    public DataTableRow insertRow(String baseName, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(String baseName, long id, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteRow(String baseName, long id, long environmentId) {
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
    public void importCsv(String baseName, String csv, long environmentId) {
        throw new UnsupportedOperationException();
    }
}
