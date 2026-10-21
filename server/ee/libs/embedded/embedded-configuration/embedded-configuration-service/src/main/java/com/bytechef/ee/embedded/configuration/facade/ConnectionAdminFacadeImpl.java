/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ConnectionAdminFacadeImpl implements ConnectionAdminFacade {

    private static final String CONNECTION_EDIT = "CONNECTION_EDIT";
    private static final String CONNECTION_TYPE = "Connection";

    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectionFacade connectionFacade;
    private final ConnectionService connectionService;
    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    public ConnectionAdminFacadeImpl(
        ConnectedUserConnectionService connectedUserConnectionService, ConnectionFacade connectionFacade,
        ConnectionService connectionService, @Lazy PermissionService permissionService) {

        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectionFacade = connectionFacade;
        this.connectionService = connectionService;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional
    @PreAuthorize("hasEmbeddedScopeInEnvironmentId('CONNECTION_CREATE', #connectionDTO.environmentId)")
    public long createConnection(ConnectionDTO connectionDTO, boolean shared) {
        long connectionId = connectionFacade.create(connectionDTO, PlatformType.EMBEDDED);

        if (shared) {
            connectedUserConnectionService.updateShared(connectionId, true);
        }

        return connectionId;
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_DELETE')")
    public void deleteConnection(long id) {
        requireEmbedded(id);

        connectionFacade.delete(id, () -> connectedUserConnectionService.deleteByConnectionId(id));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_VIEW')")
    public ConnectionDTO getConnection(long id) {
        requireEmbedded(id);

        ConnectionDTO connectionDTO = connectionFacade.getConnection(id);

        return permissionService.hasResourceScope(id, CONNECTION_TYPE, CONNECTION_EDIT)
            ? connectionDTO : connectionDTO.withoutCredentials();
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('CONNECTION_VIEW')")
    public List<Tag> getConnectionTags() {
        return connectionFacade.getConnectionTags(PlatformType.EMBEDDED);
    }

    @Override
    @PreAuthorize("hasEmbeddedScopeInEnvironmentId('CONNECTION_VIEW', #environmentId)")
    public List<ConnectionDTO> getConnections(
        @Nullable String componentName, @Nullable Integer connectionVersion, @Nullable Long environmentId,
        @Nullable Long tagId) {

        List<ConnectionDTO> connectionDTOs = connectionFacade.getConnections(
            componentName, connectionVersion, List.of(), tagId, environmentId, PlatformType.EMBEDDED);

        Map<Integer, Boolean> canEditByEnvironmentId = new HashMap<>();
        List<ConnectionDTO> redactedConnectionDTOs = new ArrayList<>();

        for (ConnectionDTO connectionDTO : connectionDTOs) {
            boolean canEdit = canEditByEnvironmentId.computeIfAbsent(
                connectionDTO.environmentId(),
                connectionEnvironmentId -> permissionService.hasResourceScope(
                    connectionDTO.id(), CONNECTION_TYPE, CONNECTION_EDIT));

            redactedConnectionDTOs.add(canEdit ? connectionDTO : connectionDTO.withoutCredentials());
        }

        return redactedConnectionDTOs;
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('CONNECTION_VIEW')")
    public Set<Long> getSharedConnectionIds() {
        return connectedUserConnectionService.getSharedConnectionIds();
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_EDIT')")
    public void updateConnection(long id, String name, List<Tag> tags, @Nullable Boolean shared, int version) {
        requireEmbedded(id);

        connectionFacade.update(id, name, tags, version);

        if (shared != null) {
            connectedUserConnectionService.updateShared(id, shared);
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_EDIT')")
    public void updateConnectionTags(long id, List<Tag> tags) {
        requireEmbedded(id);

        connectionFacade.update(id, tags);
    }

    private void requireEmbedded(long id) {
        boolean embedded = connectionService.fetchConnection(id)
            .map(Connection::getType)
            .filter(type -> type == PlatformType.EMBEDDED)
            .isPresent();

        if (!embedded) {
            throw new NoSuchElementException("Connection id=%s not found".formatted(id));
        }
    }
}
