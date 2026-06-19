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
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
 * Maps a connection id to its owning workspace (via the {@code workspace_connection} relation) and creator (the
 * relation row's {@code created_by} login resolved to a user id). EE authorizes by workspace scope; CE authorizes by
 * owner-isolation (creator), honoring CE's PRIVATE/creator-only connection model. Fails closed (returns
 * {@link ResourceOwner#unknown()}) when the connection is not mapped to any workspace.
 *
 * @author Ivica Cardic
 */
@Component
public class ConnectionOwnershipResolver implements ResourceOwnershipResolver {

    private final WorkspaceConnectionRepository workspaceConnectionRepository;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ConnectionOwnershipResolver(
        WorkspaceConnectionRepository workspaceConnectionRepository, UserService userService) {

        this.workspaceConnectionRepository = workspaceConnectionRepository;
        this.userService = userService;
    }

    @Override
    public String resourceType() {
        return "Connection";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return workspaceConnectionRepository.findByConnectionId(id)
            .map(workspaceConnection -> ResourceOwner.of(
                OptionalLong.of(workspaceConnection.getWorkspaceId()),
                resolveOwnerUserId(workspaceConnection.getCreatedBy())))
            .orElseGet(ResourceOwner::unknown);
    }

    private OptionalLong resolveOwnerUserId(String createdBy) {
        if (createdBy == null) {
            return OptionalLong.empty();
        }

        return userService.fetchUserByLogin(createdBy)
            .map(user -> OptionalLong.of(user.getId()))
            .orElse(OptionalLong.empty());
    }
}
