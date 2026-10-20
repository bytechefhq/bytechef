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

package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Maps a connection to its owning workspace through the {@code workspace_connection} relation. Fails closed when the
 * connection is assigned to no workspace or is not an automation connection.
 *
 * @author Ivica Cardic
 */
@Component
public class ConnectionOwnershipResolver implements ResourceOwnershipResolver {

    private final ConnectionService connectionService;
    private final WorkspaceConnectionRepository workspaceConnectionRepository;

    @SuppressFBWarnings("EI")
    public ConnectionOwnershipResolver(
        ConnectionService connectionService, WorkspaceConnectionRepository workspaceConnectionRepository) {

        this.connectionService = connectionService;
        this.workspaceConnectionRepository = workspaceConnectionRepository;
    }

    @Override
    public String resourceType() {
        return "Connection";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        Optional<Connection> connection = connectionService.fetchConnection(id);

        if (connection.isEmpty() || connection.get()
            .getType() != PlatformType.AUTOMATION) {

            return ResourceOwner.unknown();
        }

        return workspaceConnectionRepository.findByConnectionId(id)
            .map(workspaceConnection -> ResourceOwner.ofWorkspace(workspaceConnection.getWorkspaceId()))
            .orElseGet(ResourceOwner::unknown);
    }
}
