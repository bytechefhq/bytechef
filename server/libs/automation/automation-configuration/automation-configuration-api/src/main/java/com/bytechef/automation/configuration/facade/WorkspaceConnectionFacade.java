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

import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface WorkspaceConnectionFacade {

    long create(long workspaceId, ConnectionDTO connectionDTO);

    void delete(long connectionId);

    void disconnectConnection(long connectionId);

    ConnectionDTO getConnection(long connectionId);

    List<ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, Long environmentId, Long tagId);

    void update(long connectionId, String name, List<Tag> tags, int version);

    void update(long connectionId, String name, Map<String, ?> connectionParameters, List<Tag> tags, int version);

    void updateConnectionTags(long connectionId, List<Tag> tags);

    /**
     * Replaces the authorization parameters of an existing connection wholesale.
     *
     * @param connectionId the id of the connection
     * @param parameters   the new authorization parameters; a parameter that is not submitted is cleared
     * @param version      the connection version the caller read, checked against the stored one
     */
    void updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version);
}
