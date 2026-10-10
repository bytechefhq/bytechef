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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies that {@link WorkspaceFacadeImpl#getUserWorkspaces(long)} reads the persisted workspaces through the real
 * {@link com.bytechef.automation.configuration.service.WorkspaceService} and filters them by {@link PermissionService}:
 * tenant admins see every workspace, while a regular user only sees the workspaces they hold a role in.
 * {@link PermissionService} stays a mock because the workspace role it answers is an Enterprise Edition concept the CE
 * implementation does not model.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class WorkspaceFacadeIntTest {

    private static final long USER_ID = 31L;

    @MockitoBean
    private PermissionService permissionService;

    @Autowired
    private WorkspaceFacade workspaceFacade;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace1;
    private Workspace workspace2;
    private Workspace workspace3;

    @BeforeEach
    void beforeEach() {
        workspace1 = workspaceRepository.save(new Workspace("Workspace 1"));
        workspace2 = workspaceRepository.save(new Workspace("Workspace 2"));
        workspace3 = workspaceRepository.save(new Workspace("Workspace 3"));
    }

    @AfterEach
    void afterEach() {
        workspaceRepository.deleteAllById(List.of(workspace1.getId(), workspace2.getId(), workspace3.getId()));
    }

    @Test
    void testGetUserWorkspacesReturnsAllForTenantAdmin() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        List<Workspace> workspaces = workspaceFacade.getUserWorkspaces(USER_ID);

        assertThat(workspaces)
            .extracting(Workspace::getId)
            .containsAll(List.of(workspace1.getId(), workspace2.getId(), workspace3.getId()))
            .hasSize((int) workspaceRepository.count());

        verify(permissionService, never()).getMyWorkspaceRole(anyLong());
    }

    @Test
    void testGetUserWorkspacesFiltersToMembershipForNonAdmin() {
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.getMyWorkspaceRole(workspace1.getId())).thenReturn("VIEWER");
        when(permissionService.getMyWorkspaceRole(workspace2.getId())).thenReturn(null);
        when(permissionService.getMyWorkspaceRole(workspace3.getId())).thenReturn("EDITOR");

        List<Workspace> workspaces = workspaceFacade.getUserWorkspaces(USER_ID);

        assertThat(workspaces)
            .extracting(Workspace::getId)
            .containsExactlyInAnyOrder(workspace1.getId(), workspace3.getId());
    }

    @Test
    void testGetMyWorkspacesFiltersToMembershipForNonAdmin() {
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.getMyWorkspaceRole(workspace1.getId())).thenReturn("VIEWER");
        when(permissionService.getMyWorkspaceRole(workspace3.getId())).thenReturn("EDITOR");

        List<Workspace> workspaces = workspaceFacade.getMyWorkspaces();

        assertThat(workspaces)
            .extracting(Workspace::getId)
            .containsExactlyInAnyOrder(workspace1.getId(), workspace3.getId());
    }

    @Test
    void testGetUserWorkspacesEmptyWhenNonAdminHasNoMemberships() {
        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.getMyWorkspaceRole(anyLong())).thenReturn(null);

        assertThat(workspaceFacade.getUserWorkspaces(USER_ID)).isEmpty();
    }

    @Nested
    @ContextConfiguration(classes = PreAuthorizeEnforcement.MethodSecurityConfiguration.class)
    class PreAuthorizeEnforcement {

        @BeforeEach
        void beforeEach() {
            reset(permissionService);

            authenticate(AuthorityConstants.USER);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testGetUserWorkspacesIsGated() {
            assertDenied(() -> workspaceFacade.getUserWorkspaces(USER_ID));

            when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);

            assertNotDenied(() -> workspaceFacade.getUserWorkspaces(USER_ID));

            reset(permissionService);

            when(permissionService.isTenantAdmin()).thenReturn(true);

            assertNotDenied(() -> workspaceFacade.getUserWorkspaces(USER_ID));
        }

        @Test
        void testGetMyWorkspacesIsGatedOnAuthentication() {
            assertNotDenied(() -> workspaceFacade.getMyWorkspaces());

            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> workspaceFacade.getMyWorkspaces())
                .isInstanceOfAny(AccessDeniedException.class, AuthenticationCredentialsNotFoundException.class);
        }

        private static void assertDenied(ThrowingCallable call) {
            assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
        }

        private static void assertNotDenied(ThrowingCallable call) {
            Throwable throwable = catchThrowable(call);

            assertThat(throwable)
                .as("Expected the gate to let the call through, but it was denied: %s", throwable)
                .satisfiesAnyOf(
                    actual -> assertThat(actual).isNull(),
                    actual -> assertThat(actual).isNotInstanceOf(AccessDeniedException.class));
        }

        private static void authenticate(String authority) {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "user", "n/a", List.of(new SimpleGrantedAuthority(authority))));
        }

        @EnableMethodSecurity(proxyTargetClass = true)
        @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
        static class MethodSecurityConfiguration {
        }
    }
}
