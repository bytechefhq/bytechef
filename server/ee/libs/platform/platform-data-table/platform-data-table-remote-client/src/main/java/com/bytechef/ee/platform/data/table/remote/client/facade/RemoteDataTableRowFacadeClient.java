/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.facade;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.facade.DataTableRowFacade;
import java.util.Map;
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
    public void importCsv(String baseName, String csv, long environmentId) {
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
}
