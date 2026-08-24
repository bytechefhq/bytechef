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
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;

/**
 * Workspace-scoped connection CRUD. The base contract carries only edition-agnostic operations (create, register,
 * delete, disconnect, read); the EE-only visibility transitions (promote/demote/share/revoke) live on the EE
 * {@code com.bytechef.ee.automation.configuration.facade.WorkspaceConnectionFacade} sub-interface so no EE-licensed
 * visibility logic ships in the CE artifact.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceConnectionFacade {

    long create(long workspaceId, ConnectionDTO connectionDTO);

    List<Tag> getConnectionTags(long workspaceId);

    long registerExisting(
        long workspaceId, ConnectionDTO connectionDTO, CredentialStoreType storeType, String credentialRef);

    void delete(long connectionId);

    void disconnectConnection(long connectionId);

    ConnectionDTO getConnection(long connectionId);

    List<ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, Long environmentId, Long tagId);

    void update(long connectionId, String name, List<Tag> tags, int version);

    /**
     * Replaces the connection's authorization parameters wholesale, so a connection whose credentials stopped working
     * is reused rather than recreated. Guarded owner-or-admin, not by {@code CONNECTION_EDIT} — see the implementation
     * for why. {@code version} is checked against the stored connection so a concurrent edit fails cleanly instead of
     * overwriting a credential someone else just rotated.
     */
    void updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version);

    void updateTags(long connectionId, List<Tag> tags);
}
