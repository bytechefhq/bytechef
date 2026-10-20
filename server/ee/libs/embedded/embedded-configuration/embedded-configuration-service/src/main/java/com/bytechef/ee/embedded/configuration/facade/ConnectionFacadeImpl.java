/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.audit.ConnectionAuditEvents;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
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
@Service("com.bytechef.ee.embedded.configuration.facade.ConnectionFacade")
@ConditionalOnEEVersion
public class ConnectionAdminFacadeImpl implements ConnectionAdminFacade {

    private static final String CONNECTION_EDIT = "CONNECTION_EDIT";
    private static final String CONNECTION_TYPE = "Connection";

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final com.bytechef.platform.connection.facade.ConnectionFacade connectionFacade;
    private final ConnectionService connectionService;
    private final PermissionService permissionService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public ConnectionFacadeImpl(
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserConnectionService connectedUserConnectionService, ConnectionFacade connectionFacade,
        ConnectionService connectionService, @Lazy PermissionService permissionService,
        TagService tagService) {

        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectionFacade = connectionFacade;
        this.connectionService = connectionService;
        this.permissionService = permissionService;
        this.tagService = tagService;
    }

    @Override
    @Transactional
    @Audited(event = ConnectionAuditEvents.CONNECTION_CREATED, data = {
        @AuditData(key = "connectionId", value = "#result"),
        @AuditData(key = "name", value = "#connectionDTO.name"),
        @AuditData(key = "componentName", value = "#connectionDTO.componentName"),
        @AuditData(key = "shared", value = "#shared")
    })
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
    @Audited(event = ConnectionAuditEvents.CONNECTION_DELETED, data = {
        @AuditData(key = "connectionId", value = "#id")
    })
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_DELETE')")
    public void deleteConnection(long id) {
        requireAdminConnection(id);

        connectedUserConnectionService.deleteByConnectionId(id);

        connectionFacade.delete(id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_VIEW')")
    public ConnectionDTO getConnection(long id) {
        requireAdminConnection(id);

        ConnectionDTO connectionDTO = connectionFacade.getConnection(id);

        return permissionService.hasResourceScope(id, CONNECTION_TYPE, CONNECTION_EDIT)
            ? connectionDTO : connectionDTO.withoutCredentials();
    }

    @Override
    @PreAuthorize("hasEmbeddedScopeInEnvironmentId('CONNECTION_VIEW', #environmentId)")
    public List<ConnectionDTO> getConnections(
        @Nullable String componentName, @Nullable Integer connectionVersion, @Nullable Long environmentId,
        @Nullable Long tagId) {

        List<ConnectionDTO> connectionDTOs = connectionFacade.getConnections(
            componentName, connectionVersion, List.of(), tagId, environmentId, PlatformType.EMBEDDED);

        Set<Long> connectionIds = connectionDTOs.stream()
            .map(ConnectionDTO::id)
            .collect(Collectors.toSet());

        Set<Long> connectedUserOwnedConnectionIds =
            connectedUserConnectionMembership.getConnectedUserOwnedConnectionIds(connectionIds);

        Map<Integer, Boolean> canEditByEnvironmentId = new HashMap<>();
        List<ConnectionDTO> redactedConnectionDTOs = new ArrayList<>();

        for (ConnectionDTO connectionDTO : connectionDTOs) {
            if (connectedUserOwnedConnectionIds.contains(connectionDTO.id())) {
                continue;
            }

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
    public List<Tag> getConnectionTags() {
        List<Connection> connections = connectionService.getConnections(PlatformType.EMBEDDED);

        Set<Long> connectionIds = connections.stream()
            .map(Connection::getId)
            .collect(Collectors.toSet());

        Set<Long> connectedUserOwnedConnectionIds =
            connectedUserConnectionMembership.getConnectedUserOwnedConnectionIds(connectionIds);

        List<Long> tagIds = connections.stream()
            .filter(connection -> !connectedUserOwnedConnectionIds.contains(connection.getId()))
            .flatMap(connection -> CollectionUtils.stream(connection.getTagIds()))
            .distinct()
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('CONNECTION_VIEW')")
    public Set<Long> getSharedConnectionIds() {
        return connectedUserConnectionService.getSharedConnectionIds();
    }

    @Override
    @Transactional
    @Audited(event = ConnectionAuditEvents.CONNECTION_UPDATED, data = {
        @AuditData(key = "connectionId", value = "#id"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "shared", value = "#shared")
    })
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_EDIT')")
    public void updateConnection(long id, String name, List<Tag> tags, @Nullable Boolean shared, int version) {
        requireAdminConnection(id);

        connectionFacade.update(id, name, tags, version);

        if (shared != null) {
            connectedUserConnectionService.updateShared(id, shared);
        }
    }

    @Override
    @Transactional
    @Audited(event = ConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED, data = {
        @AuditData(key = "connectionId", value = "#id")
    })
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_EDIT')")
    public void updateConnectionCredentials(long id, Map<String, ?> parameters, int version) {
        requireAdminConnection(id);

        connectionFacade.updateCredentials(id, parameters, version);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_EDIT')")
    public void updateConnectionTags(long id, List<Tag> tags) {
        requireAdminConnection(id);

        connectionFacade.update(id, tags);
    }

    private void requireAdminConnection(long id) {
        boolean embedded = connectionService.fetchConnection(id)
            .map(Connection::getType)
            .filter(type -> type == PlatformType.EMBEDDED)
            .isPresent();

        if (!embedded || connectedUserConnectionMembership.isOwnedByConnectedUser(id)) {
            throw new NoSuchElementException("Connection id=%s not found".formatted(id));
        }
    }
}
