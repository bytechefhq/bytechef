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
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
 * Owning coordinates of a connection for the {@code 'Connection'} token, which the four connection-sharing mutations on
 * the EE {@code WorkspaceConnectionFacadeImpl} name through {@code isResourceOwner} and {@code hasResourceRole}. Both
 * of those deny unconditionally when the resolver registry has no entry for the type, so without this class every one
 * of those mutations was a tenant-admin-only lockout — including for the connection's own owner, whom the first
 * disjunct exists specifically to admit.
 * <p>
 * Populates <em>both</em> coordinates, because the two disjuncts ask different questions:
 * <ul>
 * <li>{@code workspaceId} comes from the {@code workspace_connection} join row, since a connection carries no workspace
 * column of its own. It is what {@code hasResourceRole} resolves before asking for the caller's workspace-wide ADMIN
 * role. A connection with no join row — an embedded or organization-level connection — resolves to empty and is
 * therefore refused by these workspace mutations, which is correct: the facade would reject it as not belonging to the
 * workspace anyway.</li>
 * <li>{@code ownerUserId} comes from {@code connection.created_by}, mapped through the login to a user id because
 * {@code isResourceOwner} compares numeric ids. {@code created_by} is already the system's definition of a connection's
 * owner: {@code ResourceVisibilityResolverImpl} admits a PRIVATE connection by comparing it against the current login,
 * and reassignment rewrites exactly that column. Deriving ownership from anywhere else would let the sharing gate and
 * the visibility filter disagree about who owns a row.</li>
 * </ul>
 * Registering it also makes {@link ConnectionEnvironmentResolver} reachable, since an environment resolver is only
 * consulted after an ownership resolver has produced a workspace.
 * <p>
 * Reads the repository and the connection service rather than the {@code @PreAuthorize}-guarded facades, to avoid
 * recursing back into the check being evaluated. Fails closed — {@code ResourceOwner.unknown()} — for a non-numeric id,
 * a missing connection, a connection in no workspace, and a {@code created_by} login that matches no user.
 *
 * @author Ivica Cardic
 */
@Component
public class ConnectionOwnershipResolver implements ResourceOwnershipResolver {

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
        return ResourceOwner.of(fetchWorkspaceId(id), fetchOwnerUserId(id));
    }

    private OptionalLong fetchOwnerUserId(long id) {
        return connectionService.fetchConnection(id)
            .map(Connection::getCreatedBy)
            .flatMap(userService::fetchUserByLogin)
            .map(User::getId)
            .map(OptionalLong::of)
            .orElseGet(OptionalLong::empty);
    }

    private OptionalLong fetchWorkspaceId(long id) {
        return workspaceConnectionRepository.findByConnectionId(id)
            .map(WorkspaceConnection::getWorkspaceId)
            .map(OptionalLong::of)
            .orElseGet(OptionalLong::empty);
    }
}
