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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps a connection to its owning workspace through the {@code workspace_connection} relation. Fails closed when the
 * connection is assigned to no workspace.
 *
 * @author Ivica Cardic
 */
@Component
public class ConnectionOwnershipResolver implements ResourceOwnershipResolver {

    private final WorkspaceConnectionRepository workspaceConnectionRepository;

    @SuppressFBWarnings("EI")
    public ConnectionOwnershipResolver(WorkspaceConnectionRepository workspaceConnectionRepository) {
        this.workspaceConnectionRepository = workspaceConnectionRepository;
    }

    @Override
    public String resourceType() {
        return "Connection";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return workspaceConnectionRepository.findByConnectionId(id)
            .map(workspaceConnection -> ResourceOwner.ofWorkspace(workspaceConnection.getWorkspaceId()))
            .orElseGet(ResourceOwner::unknown);
    }
}
