/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.facade;

import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_CREATED;
import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_DELETED;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.audit.AuditConnection;
import com.bytechef.platform.connection.audit.AuditConnection.AuditData;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@ConditionalOnCoordinator
public class OrganizationConnectionFacadeImpl implements OrganizationConnectionFacade {

    private final ConnectionFacade connectionFacade;
    private final ConnectionService connectionService;
    private final boolean eeEdition;

    @SuppressFBWarnings("EI")
    public OrganizationConnectionFacadeImpl(
        ConnectionFacade connectionFacade, ConnectionService connectionService,
        @Value("${bytechef.edition:CE}") String edition) {

        this.connectionFacade = connectionFacade;
        this.connectionService = connectionService;
        this.eeEdition = "EE".equalsIgnoreCase(edition);
    }

    @Override
    @Transactional
    @AuditConnection(
        event = CONNECTION_CREATED, connectionId = "#result",
        data = @AuditData(key = "visibility", value = "'ORGANIZATION'"))
    public long create(ConnectionDTO connectionDTO) {
        validateEeEdition();

        long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

        connectionService.updateVisibility(connectionId, ConnectionVisibility.ORGANIZATION);

        return connectionId;
    }

    @Override
    @Transactional
    @AuditConnection(event = CONNECTION_DELETED, connectionId = "#connectionId")
    public void delete(long connectionId) {
        validateEeEdition();

        Connection connection = connectionService.getConnection(connectionId);

        validateOrganizationVisibility(connectionId, connection);

        connectionFacade.delete(connectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getOrganizationConnections(Long environmentId) {
        if (!eeEdition) {
            return List.of();
        }

        List<Connection> connections = connectionService.getConnectionsByVisibility(
            ConnectionVisibility.ORGANIZATION, PlatformType.AUTOMATION)
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
    @Transactional
    public ConnectionDTO update(long connectionId, String name, List<Long> tagIds, int version) {
        validateEeEdition();

        Connection connection = connectionService.getConnection(connectionId);

        validateOrganizationVisibility(connectionId, connection);

        List<Tag> tags = tagIds.stream()
            .map(Tag::new)
            .toList();

        connectionFacade.update(connectionId, name, tags, version);

        return connectionFacade.getConnection(connectionId);
    }

    private void validateEeEdition() {
        // Organization visibility is EE-only. The UI hides these surfaces on CE but a hand-crafted
        // GraphQL request would otherwise reach updateVisibility(ORGANIZATION) and bypass the
        // CE-forces-PRIVATE guard in ConnectionFacadeImpl.create.
        if (!eeEdition) {
            throw new ConfigurationException(
                "Organization-scoped connections are only available on Enterprise Edition",
                ConnectionErrorType.INVALID_CONNECTION);
        }
    }

    private void validateOrganizationVisibility(long connectionId, Connection connection) {
        if (connection.getVisibility() != ConnectionVisibility.ORGANIZATION) {
            throw new ConfigurationException(
                "Connection id=%s is not an organization connection".formatted(connectionId),
                ConnectionErrorType.INVALID_CONNECTION);
        }
    }
}
