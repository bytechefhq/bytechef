/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.audit.ConnectionAuditEvents;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
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
@PreAuthorize("isTenantAdmin()")
public class ConnectionAdminFacadeImpl implements ConnectionAdminFacade {

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectionFacade connectionFacade;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public ConnectionAdminFacadeImpl(
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserConnectionService connectedUserConnectionService, ConnectionFacade connectionFacade,
        ConnectionService connectionService) {

        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectionFacade = connectionFacade;
        this.connectionService = connectionService;
    }

    @Override
    @Transactional
    @Audited(event = ConnectionAuditEvents.CONNECTION_CREATED, data = {
        @AuditData(key = "connectionId", value = "#result"),
        @AuditData(key = "name", value = "#connectionDTO.name"),
        @AuditData(key = "componentName", value = "#connectionDTO.componentName"),
        @AuditData(key = "shared", value = "#shared")
    })
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
    public void deleteConnection(long id) {
        requireAdminConnection(id);

        connectedUserConnectionService.deleteByConnectionId(id);

        connectionFacade.delete(id);
    }

    @Override
    public ConnectionDTO getConnection(long id) {
        requireAdminConnection(id);

        return connectionFacade.getConnection(id);
    }

    @Override
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

        return connectionDTOs.stream()
            .filter(connectionDTO -> !connectedUserOwnedConnectionIds.contains(connectionDTO.id()))
            .toList();
    }

    @Override
    public List<Tag> getConnectionTags() {
        Map<Long, Tag> tagsById = new LinkedHashMap<>();

        for (ConnectionDTO connectionDTO : getConnections(null, null, null, null)) {
            for (Tag tag : connectionDTO.tags()) {
                tagsById.putIfAbsent(tag.getId(), tag);
            }
        }

        List<Tag> tags = new ArrayList<>(tagsById.values());

        tags.sort(Comparator.comparing(Tag::getName));

        return tags;
    }

    @Override
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
    public void updateConnection(long id, String name, List<Tag> tags, @Nullable Boolean shared, int version) {
        requireAdminConnection(id);

        connectionFacade.update(id, name, tags, version);

        if (shared != null) {
            connectedUserConnectionService.updateShared(id, shared);
        }
    }

    @Override
    @Transactional
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
