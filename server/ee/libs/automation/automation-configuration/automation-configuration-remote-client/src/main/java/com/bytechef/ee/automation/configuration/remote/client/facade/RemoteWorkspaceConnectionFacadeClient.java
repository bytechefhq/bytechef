/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.facade;

import com.bytechef.automation.configuration.dto.ConnectionWorkflowDTO;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.platform.tag.domain.Tag;
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
public class RemoteWorkspaceConnectionFacadeClient implements WorkspaceConnectionFacade {

    @Override
    public long create(long workspaceId, ConnectionDTO connectionDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long registerExisting(
        long workspaceId, ConnectionDTO connectionDTO, CredentialStoreType storeType, String credentialRef) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnectConnection(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnectConnection(
        long connectionId, ConnectionWorkflowDTO.Usage usage, long id) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionDTO getConnection(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, Long connectionEnvironment,
        Long tagId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tag> getConnectionTags(long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectionWorkflowDTO> getConnectionWorkflows(
        long connectionId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void update(long connectionId, String name, List<Tag> tags, int version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(
        long connectionId, String name, Map<String, ?> connectionParameters, List<Tag> tags, int version) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateConnectionTags(long connectionId, List<Tag> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version) {
        throw new UnsupportedOperationException();
    }
}
