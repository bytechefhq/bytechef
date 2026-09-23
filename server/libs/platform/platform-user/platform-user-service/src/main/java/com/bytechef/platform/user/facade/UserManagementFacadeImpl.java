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

package com.bytechef.platform.user.facade;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.service.ApiKeyRevoker;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.service.WorkspaceMembershipAssigner;
import com.bytechef.platform.user.service.WorkspaceMembershipAssigner.WorkspaceAssignment;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnCoordinator
class UserManagementFacadeImpl implements UserManagementFacade {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ObjectProvider<ApiKeyRevoker> apiKeyRevokerProvider;
    private final AuthorityService authorityService;
    private final TenantService tenantService;
    private final UserInvitationService userInvitationService;
    private final UserService userService;
    private final ObjectProvider<WorkspaceMembershipAssigner> workspaceMembershipAssignerProvider;

    @SuppressFBWarnings("EI")
    UserManagementFacadeImpl(
        ObjectProvider<ApiKeyRevoker> apiKeyRevokerProvider, AuthorityService authorityService,
        TenantService tenantService, UserInvitationService userInvitationService, UserService userService,
        ObjectProvider<WorkspaceMembershipAssigner> workspaceMembershipAssignerProvider) {

        this.apiKeyRevokerProvider = apiKeyRevokerProvider;
        this.authorityService = authorityService;
        this.tenantService = tenantService;
        this.userInvitationService = userInvitationService;
        this.userService = userService;
        this.workspaceMembershipAssignerProvider = workspaceMembershipAssignerProvider;
    }

    /**
     * An API key authenticates as the user who owns it, so the keys go with the account rather than outliving it. Not
     * only a security property: {@code api_key.user_id} is NOT NULL under {@code fk_api_key_user}, so removing the user
     * first fails on the constraint instead of orphaning the key.
     *
     * <p>
     * Orchestrated here rather than inside {@code UserService}, which has no business calling another service.
     * Transactional so a failed revocation rolls the account back with it, leaving neither half done.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteUser(String login) {
        userService.fetchUserByLogin(login)
            .map(User::getId)
            .ifPresent(this::revokeApiKeys);

        userService.delete(login);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void inviteUser(String email, String role, List<WorkspaceAssignment> workspaces) {
        if (tenantService.isMultiTenantEnabled() && tenantService.tenantIdsByUserEmailExist(email)) {
            throw new EmailAlreadyUsedException();
        } else {
            Optional<User> existingUser = userService.fetchUserByEmail(email);

            if (existingUser.isPresent()) {
                throw new EmailAlreadyUsedException();
            }
        }

        // Every reason the placement could be refused is asked before anything is provisioned. Rolling back afterwards
        // is not enough: the claim-link mail is dispatched from inside this transaction, so a rejection here used to
        // take the user row and its reset_key away from a link that had already left for the invitee's mailbox.
        WorkspaceMembershipAssigner workspaceMembershipAssigner = resolveWorkspaceMembershipAssigner(workspaces);

        if (workspaceMembershipAssigner != null) {
            workspaceMembershipAssigner.validateAssignments(workspaces);
        }

        User user = userInvitationService.inviteUser(email, role);

        if (workspaceMembershipAssigner != null) {
            workspaceMembershipAssigner.assign(user.getId(), workspaces);
        }
    }

    /**
     * The {@link WorkspaceMembershipAssigner} that will place the invitee, or {@code null} when there is nothing to
     * place. Workspace membership lives in the automation modules, which depend on this one, so the call has to invert
     * through an SPI rather than a direct dependency.
     *
     * <p>
     * An empty request needs no implementation at all — provisioning an account with no workspace is legitimate, and is
     * how a second tenant admin is created. A non-empty request with no implementation registered means the deployment
     * has no workspace concept (embedded), which is a caller error rather than something to silently drop.
     */
    private WorkspaceMembershipAssigner resolveWorkspaceMembershipAssigner(List<WorkspaceAssignment> workspaces) {
        if (workspaces.isEmpty()) {
            return null;
        }

        WorkspaceMembershipAssigner workspaceMembershipAssigner = workspaceMembershipAssignerProvider.getIfAvailable();

        if (workspaceMembershipAssigner == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Workspaces cannot be assigned: this deployment has no workspace support");
        }

        return workspaceMembershipAssigner;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Optional<UserWithAuthorities> fetchUser(String login) {
        List<Authority> authorities = authorityService.getAuthorities();

        return userService.fetchUserByLogin(login)
            .map(user -> new UserWithAuthorities(user, authorities));
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public UsersWithAuthorities getUsers(Integer pageNumber, Integer pageSize) {
        List<Authority> authorities = authorityService.getAuthorities();

        int page = pageNumber != null ? pageNumber : 0;
        int size = pageSize != null ? pageSize : DEFAULT_PAGE_SIZE;

        Page<User> usersPage = userService.getAllManagedUsers(PageRequest.of(page, size));

        return new UsersWithAuthorities(usersPage, authorities);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public UserWithAuthorities updateUserRole(String login, String role) {
        List<Authority> allAuthorities = authorityService.getAuthorities();
        Optional<User> userOptional = userService.fetchUserByLogin(login);

        if (userOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        User user = userOptional.get();

        Authority authority = allAuthorities.stream()
            .filter(curAuthority -> Objects.equals(curAuthority.getName(), role))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role));

        user.setAuthorities(Set.of(authority));

        User updatedUser = userService.update(new AdminUserDTO(user, allAuthorities))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return new UserWithAuthorities(updatedUser, allAuthorities);
    }

    /**
     * Deployments carrying no security module register no revoker, and have no keys to revoke either.
     */
    private void revokeApiKeys(long userId) {
        ApiKeyRevoker apiKeyRevoker = apiKeyRevokerProvider.getIfAvailable();

        if (apiKeyRevoker == null) {
            return;
        }

        apiKeyRevoker.revokeAll(userId);
    }
}
