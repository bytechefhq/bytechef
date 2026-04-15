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

package com.bytechef.platform.connection.service;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ConnectionService {

    Connection create(Connection connection);

    Connection create(
        @Nullable AuthorizationType authorizationType, String componentName, int connectionVersion,
        int environmentId, String name, Map<String, Object> parameters, PlatformType type);

    void delete(long id);

    Connection getConnection(long id);

    List<Connection> getConnections(PlatformType type);

    List<Connection> getConnectionsByVisibility(ConnectionVisibility visibility, PlatformType type);

    List<Connection> getConnections(String componentName, int version, PlatformType type);

    List<Connection> getConnections(
        String componentName, Integer connectionVersion, Long tagId, Long environmentId, PlatformType type);

    List<Connection> getConnections(List<Long> connectionIds);

    Connection update(long id, List<Long> tagIds);

    Connection update(long id, String name, List<Long> tagIds, int version);

    Connection updateConnectionCredentialStatus(long connectionId, Connection.CredentialStatus status);

    Connection updateConnectionStatus(long connectionId, ConnectionStatus status);

    Connection updateCreatedBy(long id, String newCreatedBy);

    Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters);

    Connection updateVisibility(long id, ConnectionVisibility visibility);

    /**
     * Returns connections whose status is not {@link ConnectionStatus#ACTIVE}. {@link Connection#getStatus()} is total
     * (it throws {@link IllegalStateException} on a corrupted ordinal rather than returning null), so callers can rely
     * on each returned row having a usable status label. Returns an empty list for null/empty input or when all
     * connections are active. Callers use this to emit per-connection audit events before rejecting the operation.
     */
    List<Connection> getInactiveConnections(List<Long> connectionIds);

    void validateConnectionsActive(List<Long> connectionIds);
}
