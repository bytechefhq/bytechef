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

package com.bytechef.platform.connection.facade;

import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface ConnectionFacade {
    long create(ConnectionDTO connectionDTO, PlatformType type);

    void delete(Long id);

    Integer executeConnectionRefresh(Long connectionId);

    ConnectionDTO getConnection(Long id);

    boolean isConnectionUsed(long id);

    List<ConnectionDTO> getConnections(List<Long> connectionIds, PlatformType type);

    List<ConnectionDTO> getConnections(
        String componentName, Integer connectionVersion, List<Long> connectionIds, Long tagId, Long environmentId,
        PlatformType type);

    List<ConnectionDTO> toConnectionDTOs(List<Connection> connections);

    List<Tag> getConnectionTags(PlatformType type);

    void update(long id, List<Tag> tags);

    void update(long id, String name, List<Tag> tags, int version);

    /**
     * Updates a connection's name and tags and replaces the values of its connection properties.
     *
     * @param id                   the id of the connection
     * @param name                 the new name
     * @param connectionParameters the values of the connection properties declared by the connection definition; a
     *                             property left out is cleared, and a key that is not a connection property is ignored
     * @param tags                 the new tags
     * @param version              the connection version the caller read
     */
    void update(long id, String name, Map<String, ?> connectionParameters, List<Tag> tags, int version);

    void replaceAuthorizationParameters(long id, Map<String, ?> parameters);

    /**
     * Replaces a connection's authorization parameters, rejected when {@code version} is stale.
     */
    void replaceAuthorizationParameters(long id, Map<String, ?> parameters, int version);

    void updateCredentials(long id, Map<String, ?> parameters, int version);
}
