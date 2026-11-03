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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Ivica Cardic
 */
class ProjectDeploymentJobPrincipalAuthenticationResolverTest {

    private static final long PROJECT_DEPLOYMENT_ID = 11L;

    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final UserService userService = mock(UserService.class);

    private final ProjectDeploymentJobPrincipalAuthenticationResolver resolver =
        new ProjectDeploymentJobPrincipalAuthenticationResolver(
            authorityService, projectDeploymentService, userService);

    @Test
    void testGetType() {
        assertThat(resolver.getType()).isEqualTo(PlatformType.AUTOMATION);
    }

    @Test
    void testFetchAuthenticationReturnsTheDeploymentCreatorWithTheirOwnAuthorities() {
        stubProjectDeploymentCreatedBy("editor");

        when(userService.fetchUserByLogin("editor")).thenReturn(Optional.of(user("editor", true, List.of(1L, 2L))));
        when(authorityService.fetchAuthority(1L)).thenReturn(Optional.of(authority("ROLE_USER")));
        when(authorityService.fetchAuthority(2L)).thenReturn(Optional.empty());

        Authentication authentication = resolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID)
            .orElseThrow();

        assertThat(authentication.getName()).isEqualTo("editor");
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(
            authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList()).containsExactly("ROLE_USER");
    }

    @Test
    void testFetchAuthenticationIsEmptyForAnInactiveCreator() {
        stubProjectDeploymentCreatedBy("editor");

        when(userService.fetchUserByLogin("editor")).thenReturn(Optional.of(user("editor", false, List.of())));

        assertThat(resolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID)).isEmpty();
    }

    @Test
    void testFetchAuthenticationIsEmptyWhenTheCreatorIsNotAUser() {
        stubProjectDeploymentCreatedBy("system");

        when(userService.fetchUserByLogin("system")).thenReturn(Optional.empty());

        assertThat(resolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID)).isEmpty();
    }

    @Test
    void testFetchAuthenticationIsEmptyForAMissingDeployment() {
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID)).thenReturn(Optional.empty());

        assertThat(resolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID)).isEmpty();
    }

    private void stubProjectDeploymentCreatedBy(String createdBy) {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);

        when(projectDeployment.getCreatedBy()).thenReturn(createdBy);
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(projectDeployment));
    }

    private static Authority authority(String name) {
        Authority authority = new Authority();

        authority.setName(name);

        return authority;
    }

    private static User user(String login, boolean activated, List<Long> authorityIds) {
        User user = new User();

        user.setActivated(activated);
        user.setAuthorityIds(authorityIds);
        user.setLogin(login);

        return user;
    }
}
