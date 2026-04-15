/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.facade;

import com.bytechef.automation.configuration.dto.BulkPromoteResultDTO;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteWorkspaceConnectionFacadeClient implements WorkspaceConnectionFacade {

    @Override
    public long create(long workspaceId, com.bytechef.platform.connection.dto.ConnectionDTO connectionDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void demoteToPrivate(long workspaceId, long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnectConnection(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<com.bytechef.platform.connection.dto.ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, Long connectionEnvironment,
        Long tagId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionVisibility promoteToWorkspace(long workspaceId, long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revokeConnectionFromProject(long workspaceId, long connectionId, long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revokeSingleProjectShareAuditOnly(long connectionId, long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shareConnectionToProject(long workspaceId, long connectionId, long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setConnectionProjects(long workspaceId, long connectionId, List<Long> projectIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BulkPromoteResultDTO promoteAllPrivateToWorkspace(long workspaceId) {
        throw new UnsupportedOperationException();
    }
}
