/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.remote.client.service;

import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.service.WorkspaceDataTableService;
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
public class RemoteWorkspaceDataTableServiceClient implements WorkspaceDataTableService {

    @Override
    public void assignDataTableToWorkspace(Long dataTableId, Long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<WorkspaceDataTable> getWorkspaceDataTables(Long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDataTableFromWorkspace(Long dataTableId) {
        throw new UnsupportedOperationException();
    }
}
