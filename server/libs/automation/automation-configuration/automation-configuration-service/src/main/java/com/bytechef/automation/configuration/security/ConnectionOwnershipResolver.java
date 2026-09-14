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
import java.util.Optional;
import java.util.OptionalLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Owning coordinates of a connection for the {@code 'Connection'} token, which the connection-sharing gates on the EE
 * {@code WorkspaceConnectionFacadeImpl} and the CE/EE {@code delete}, {@code update} and {@code updateTags} gates name
 * through {@code isResourceOwner} and {@code hasResourceRole}, and which {@code hasPermission(..., 'Connection', ...)}
 * reaches through {@code hasResourceScope}. Both deny unconditionally when the resolver registry has no entry for the
 * type, so without this class every one of those gates was a tenant-admin-only lockout — including for the connection's
 * own owner, whom the owner disjunct exists specifically to admit.
 * <p>
 * Populates <em>both</em> coordinates, because the two disjuncts of those gates ask different questions:
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
 * recursing back into the check being evaluated. Fails closed rather than throwing, but per coordinate rather than
 * wholesale: a non-numeric id is the only case that yields {@code ResourceOwner.unknown()}, and it does so through the
 * SPI's default overload. A missing connection, a {@code created_by} that is null or matches no user, and a user lookup
 * this deployment does not implement each leave {@code ownerUserId} empty with {@code workspaceId} intact; a connection
 * in no workspace leaves {@code workspaceId} empty with {@code ownerUserId} intact. The distinction matters: an empty
 * coordinate denies only the disjunct that reads it, so a partially populated owner still lets the other disjunct admit
 * a caller it is entitled to admit.
 *
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
        return ResourceOwner.of(fetchWorkspaceId(id), fetchOwnerUserId(id));
    }

    private OptionalLong fetchOwnerUserId(long id) {
        return connectionService.fetchConnection(id)
            .map(Connection::getCreatedBy)
            .flatMap(this::fetchUserIdByLogin)
            .map(OptionalLong::of)
            .orElseGet(OptionalLong::empty);
    }

    /**
     * The one lookup on this path that a deployment may not implement: {@code configuration-app} injects
     * {@code RemoteUserServiceClient}, whose {@code fetchUserByLogin} is a stub that throws. An ownership resolver is
     * called from inside an authorization check, so letting that escape turns a decision into a 500 — and takes the
     * workspace-ADMIN branch of the sharing gates with it, since {@code hasResourceRole} reaches this same
     * {@code resolveOwner}. "Owner unknown" is what the SPI's fail-closed contract asks for, and it denies rather than
     * admits.
     * <p>
     * Only {@code UnsupportedOperationException} is caught, and only around this call: it is the signal every remote
     * client stub in this tree raises for a method it does not carry. A wider catch here would swallow the genuine
     * programming errors and infrastructure failures that must surface, and silently deny where the cause was a bug.
     * The exception is logged with its stack trace because the stub is not its only possible source — a real
     * {@code UserServiceImpl} could raise the same type from deeper in its own call graph, and then every connection
     * silently loses its owner with the message as the only, wrong, account of why.
     */
    private Optional<Long> fetchUserIdByLogin(String login) {
        try {
            return userService.fetchUserByLogin(login)
                .map(User::getId);
        } catch (UnsupportedOperationException exception) {
            log.warn(
                "Cannot resolve the owner of a connection: fetchUserByLogin threw UnsupportedOperationException, " +
                    "as this deployment's remote UserService stub does; see the trace for whether that is the " +
                    "actual cause. Ownership is unanswerable either way, so owner-or-admin checks fall back to " +
                    "the workspace role.",
                exception);

            return Optional.empty();
        }
    }

    private OptionalLong fetchWorkspaceId(long id) {
        return workspaceConnectionRepository.findByConnectionId(id)
            .map(WorkspaceConnection::getWorkspaceId)
            .map(OptionalLong::of)
            .orElseGet(OptionalLong::empty);
    }
}
