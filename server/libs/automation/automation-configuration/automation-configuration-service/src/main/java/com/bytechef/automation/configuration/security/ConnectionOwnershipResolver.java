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

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.OptionalLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ConnectionOwnershipResolver implements ResourceOwnershipResolver {

    private static final Logger log = LoggerFactory.getLogger(ConnectionOwnershipResolver.class);

    private final ConnectionService connectionService;
    private final UserService userService;
    private final WorkspaceConnectionRepository workspaceConnectionRepository;

    @SuppressFBWarnings("EI")
    public ConnectionOwnershipResolver(
        ConnectionService connectionService, UserService userService,
        WorkspaceConnectionRepository workspaceConnectionRepository) {

        this.connectionService = connectionService;
        this.userService = userService;
        this.workspaceConnectionRepository = workspaceConnectionRepository;
    }

    @Override
    public String resourceType() {
        return "Connection";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        Optional<Connection> connectionOptional = connectionService.fetchConnection(id);

        if (connectionOptional.isEmpty()) {
            return ResourceOwner.unknown();
        }

        Connection connection = connectionOptional.get();

        if (connection.getType() != PlatformType.AUTOMATION) {
            return ResourceOwner.unknown();
        }

        return ResourceOwner.of(fetchWorkspaceId(id), fetchOwnerUserId(connection));
    }

    private OptionalLong fetchOwnerUserId(Connection connection) {
        return Optional.ofNullable(connection.getCreatedBy())
            .flatMap(this::fetchUserIdByLogin)
            .map(OptionalLong::of)
            .orElseGet(OptionalLong::empty);
    }

    private Optional<Long> fetchUserIdByLogin(String login) {
        try {
            return userService.fetchUserByLogin(login)
                .map(User::getId);
        } catch (UnsupportedOperationException exception) {
            log.warn("Cannot resolve the owner of a connection, fetchUserByLogin is not supported", exception);

            return Optional.empty();
        }
    }

    private OptionalLong fetchWorkspaceId(long connectionId) {
        return workspaceConnectionRepository.findByConnectionId(connectionId)
            .map(WorkspaceConnection::getWorkspaceId)
            .map(OptionalLong::of)
            .orElseGet(OptionalLong::empty);
    }
}
