/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.service.WorkspaceMembershipAssigner;
import com.bytechef.platform.user.service.WorkspaceMembershipAssigner.WorkspaceAssignment;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit test for the {@link UserManagementFacadeImpl} invite orchestration: the email-collision checks it owns, and
 * workspace assignment through the {@link WorkspaceMembershipAssigner} seam.
 *
 * <p>
 * Provisioning and the claim-link mail moved to {@code UserInvitationService} so both invite surfaces share one
 * implementation, and are covered by that service's own test. The password-validation tests that used to live here are
 * gone with the {@code password} argument — an administrator no longer supplies one.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class UserManagementFacadeImplTest {

    private static final String EMAIL = "newuser@example.com";
    private static final String ROLE = "ROLE_USER";

    private AuthorityService authorityService;
    private TenantService tenantService;
    private UserInvitationService userInvitationService;
    private UserService userService;
    private WorkspaceMembershipAssigner workspaceMembershipAssigner;
    private UserManagementFacadeImpl userManagementFacade;

    @BeforeEach
    void setUp() {
        authorityService = Mockito.mock(AuthorityService.class);
        tenantService = Mockito.mock(TenantService.class);
        userInvitationService = Mockito.mock(UserInvitationService.class);
        userService = Mockito.mock(UserService.class);
        workspaceMembershipAssigner = Mockito.mock(WorkspaceMembershipAssigner.class);

        userManagementFacade = new UserManagementFacadeImpl(
            authorityService, tenantService, userInvitationService, userService,
            objectProviderOf(workspaceMembershipAssigner));
    }

    @Test
    void testInviteUserDelegatesProvisioning() {
        givenInvitableUser();

        userManagementFacade.inviteUser(EMAIL, ROLE, List.of());

        verify(userInvitationService).inviteUser(EMAIL, ROLE);
    }

    @Test
    void testInviteUserAssignsRequestedWorkspaces() {
        User user = givenInvitableUser();

        user.setId(11L);

        List<WorkspaceAssignment> assignments = List.of(
            new WorkspaceAssignment(1L, "EDITOR"), new WorkspaceAssignment(2L, "VIEWER"));

        userManagementFacade.inviteUser(EMAIL, ROLE, assignments);

        verify(workspaceMembershipAssigner).assign(11L, assignments);
    }

    /**
     * Pins D2/D7: an invite carrying no workspaces leaves the invitee in none. This is how a second tenant admin is
     * provisioned, and a later change that "helpfully" auto-joins a default workspace must fail here.
     */
    @Test
    void testInviteUserWithoutWorkspacesAssignsNone() {
        givenInvitableUser();

        userManagementFacade.inviteUser(EMAIL, ROLE, List.of());

        verifyNoInteractions(workspaceMembershipAssigner);
    }

    @Test
    void testInviteUserWithWorkspacesRejectedWhenNoAssignerRegistered() {
        givenInvitableUser();

        userManagementFacade = new UserManagementFacadeImpl(
            authorityService, tenantService, userInvitationService, userService, objectProviderOf(null));

        assertThatThrownBy(
            () -> userManagementFacade.inviteUser(EMAIL, ROLE, List.of(new WorkspaceAssignment(1L, "EDITOR"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no workspace support");
    }

    @Test
    void testInviteUserWithExistingEmailNeverProvisions() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userService.fetchUserByEmail("existing@example.com"))
            .thenReturn(Optional.of(createUser("existing", "existing@example.com")));

        assertThatThrownBy(() -> userManagementFacade.inviteUser("existing@example.com", ROLE, List.of()))
            .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userInvitationService, never()).inviteUser(anyString(), anyString());
    }

    @Test
    void testInviteUserWithEmailUsedInAnotherTenantNeverProvisions() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.tenantIdsByUserEmailExist(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userManagementFacade.inviteUser(EMAIL, ROLE, List.of()))
            .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userInvitationService, never()).inviteUser(anyString(), anyString());
    }

    private User givenInvitableUser() {
        User user = createUser("newuser", EMAIL);

        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userService.fetchUserByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userInvitationService.inviteUser(EMAIL, ROLE)).thenReturn(user);

        return user;
    }

    private User createUser(String login, String email) {
        User user = new User();

        user.setLogin(login);
        user.setEmail(email);

        return user;
    }

    /**
     * Minimal {@link ObjectProvider} over a single optional bean. Spring's own test doubles pull in a container; only
     * {@code getIfAvailable} is exercised here.
     */
    private static ObjectProvider<WorkspaceMembershipAssigner> objectProviderOf(
        WorkspaceMembershipAssigner workspaceMembershipAssigner) {

        @SuppressWarnings("unchecked")
        ObjectProvider<WorkspaceMembershipAssigner> objectProvider = Mockito.mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(workspaceMembershipAssigner);

        return objectProvider;
    }
}
