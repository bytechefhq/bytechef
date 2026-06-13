/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static com.bytechef.ee.platform.connection.audit.ConnectionAuditEvent.CONNECTION_CREATED;
import static com.bytechef.ee.platform.connection.audit.ConnectionAuditEvent.CONNECTION_DELETED;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.connection.audit.AuditConnection;
import com.bytechef.ee.platform.connection.audit.AuditConnection.AuditData;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
@ConditionalOnCoordinator
public class OrganizationConnectionFacadeImpl implements OrganizationConnectionFacade {

    private final ConnectionFacade connectionFacade;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public OrganizationConnectionFacadeImpl(ConnectionFacade connectionFacade, ConnectionService connectionService) {
        this.connectionFacade = connectionFacade;
        this.connectionService = connectionService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    @Transactional
    @AuditConnection(
        event = CONNECTION_CREATED, connectionId = "#result",
        data = @AuditData(key = "visibility", value = "'ORGANIZATION'"))
    public long create(ConnectionDTO connectionDTO) {
        long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

        connectionService.updateVisibility(connectionId, ResourceVisibility.ORGANIZATION);

        return connectionId;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    @Transactional
    @AuditConnection(event = CONNECTION_DELETED, connectionId = "#connectionId")
    public void delete(long connectionId) {
        Connection connection = connectionService.getConnection(connectionId);

        validateOrganizationVisibility(connectionId, connection);

        connectionFacade.delete(connectionId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getOrganizationConnections(Long environmentId) {
        List<Connection> connections = connectionService.getConnectionsByVisibility(
            ResourceVisibility.ORGANIZATION, PlatformType.AUTOMATION)
            .stream()
            .filter(
                connection -> environmentId == null || connection.getEnvironmentId() == environmentId.intValue())
            .toList();

        if (connections.isEmpty()) {
            return List.of();
        }

        // Reuse already-fetched entities — avoids a second SQL round-trip through
        // connectionFacade.getConnections(ids, type) which re-queries the same connections.
        return connectionFacade.toConnectionDTOs(connections);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    @Transactional
    public ConnectionDTO update(long connectionId, String name, List<Long> tagIds, int version) {
        Connection connection = connectionService.getConnection(connectionId);

        validateOrganizationVisibility(connectionId, connection);

        List<Tag> tags = tagIds.stream()
            .map(Tag::new)
            .toList();

        connectionFacade.update(connectionId, name, tags, version);

        return connectionFacade.getConnection(connectionId);
    }

    private void validateOrganizationVisibility(long connectionId, Connection connection) {
        if (connection.getVisibility() != ResourceVisibility.ORGANIZATION) {
            throw new ConfigurationException(
                "Connection id=%s is not an organization connection".formatted(connectionId),
                ConnectionErrorType.INVALID_CONNECTION);
        }
    }
}
