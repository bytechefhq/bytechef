/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
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
 * Implementation of {@link UserManagementFacade}. Delegates to the shared user-management services and carries the
 * {@code ADMIN} authorization guard so it is enforced for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class UserManagementFacadeImpl implements UserManagementFacade {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AuthorityService authorityService;
    private final TenantService tenantService;
    private final UserInvitationService userInvitationService;
    private final UserService userService;
    private final ObjectProvider<WorkspaceMembershipAssigner> workspaceMembershipAssignerProvider;

    @SuppressFBWarnings("EI")
    UserManagementFacadeImpl(
        AuthorityService authorityService, TenantService tenantService, UserInvitationService userInvitationService,
        UserService userService, ObjectProvider<WorkspaceMembershipAssigner> workspaceMembershipAssignerProvider) {

        this.authorityService = authorityService;
        this.tenantService = tenantService;
        this.userInvitationService = userInvitationService;
        this.userService = userService;
        this.workspaceMembershipAssignerProvider = workspaceMembershipAssignerProvider;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteUser(String login) {
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

        User user = userInvitationService.inviteUser(email, role);

        assignWorkspaces(user, workspaces);
    }

    /**
     * Places the invitee into the requested workspaces through the {@link WorkspaceMembershipAssigner} SPI. Workspace
     * membership lives in the automation modules, which depend on this one, so the call has to invert through an
     * interface rather than a direct dependency.
     *
     * <p>
     * Runs inside {@code inviteUser}'s transaction, so a failed placement rolls the provisioned account back with it
     * rather than leaving a user half-placed. Absent implementation means the deployment has no workspace concept
     * (embedded), where an invitation provisions the account and assigns nothing.
     */
    private void assignWorkspaces(User user, List<WorkspaceAssignment> workspaces) {
        if (workspaces.isEmpty()) {
            return;
        }

        WorkspaceMembershipAssigner workspaceMembershipAssigner = workspaceMembershipAssignerProvider.getIfAvailable();

        if (workspaceMembershipAssigner == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Workspaces cannot be assigned: this deployment has no workspace support");
        }

        workspaceMembershipAssigner.assign(user.getId(), workspaces);
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
            .orElseThrow();

        user.setAuthorities(Set.of(authority));

        User updatedUser = userService.update(new AdminUserDTO(user, allAuthorities))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return new UserWithAuthorities(updatedUser, allAuthorities);
    }
}
