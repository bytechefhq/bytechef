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

package com.bytechef.security.web.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * @author Ivica Cardic
 */
class SocialUserResolverTest {

    private static final String EMAIL = "someone@example.com";

    @Test
    void testResolveInvitedUserDisablesAutoProvisioning() {
        UserService userService = mock(UserService.class);

        when(
            userService.findOrCreateSocialUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(),
                anyString()))
                    .thenReturn(new User());

        SocialUserResolver.resolveInvitedUser(
            userService, EMAIL, "Some", "One", "https://example.com/avatar.png", "GOOGLE", "provider-id");

        ArgumentCaptor<Boolean> autoProvisionCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> defaultAuthorityCaptor = ArgumentCaptor.forClass(String.class);

        verify(userService).findOrCreateSocialUser(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            autoProvisionCaptor.capture(), defaultAuthorityCaptor.capture());

        assertThat(autoProvisionCaptor.getValue())
            .as("auto-provisioning must stay off: this path asks for %s, so provisioning through social login would "
                + "grant instance admin to any account at the configured provider", AuthorityConstants.ADMIN)
            .isFalse();

        assertThat(defaultAuthorityCaptor.getValue())
            .as("unused while auto-provisioning is off, but pinned so that turning provisioning on here has to be a "
                + "reviewed change rather than a silent grant of %s", AuthorityConstants.ADMIN)
            .isEqualTo(AuthorityConstants.ADMIN);
    }

    @Test
    void testResolveUserPassesProvisioningSettingsThrough() {
        UserService userService = mock(UserService.class);

        when(
            userService.findOrCreateSocialUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(),
                anyString()))
                    .thenReturn(new User());

        SocialUserResolver.resolveUser(
            userService, EMAIL, "Some", "One", "https://example.com/avatar.png", "SSO", "provider-id", true,
            AuthorityConstants.USER);

        ArgumentCaptor<Boolean> autoProvisionCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> defaultAuthorityCaptor = ArgumentCaptor.forClass(String.class);

        verify(userService).findOrCreateSocialUser(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            autoProvisionCaptor.capture(), defaultAuthorityCaptor.capture());

        // The enterprise SSO path reads both from the tenant's IdentityProvider record; overriding either here would
        // silently ignore what the administrator configured.
        assertThat(autoProvisionCaptor.getValue())
            .as("the caller's auto-provisioning setting must reach the service unchanged")
            .isTrue();

        assertThat(defaultAuthorityCaptor.getValue())
            .as("the caller's default authority must reach the service unchanged")
            .isEqualTo(AuthorityConstants.USER);
    }

    @Test
    void testResolveInvitedUserRejectsUnknownEmail() {
        UserService userService = mock(UserService.class);

        IllegalStateException illegalStateException = new IllegalStateException("Auto-provisioning is disabled");

        when(
            userService.findOrCreateSocialUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(),
                anyString()))
                    .thenThrow(illegalStateException);

        // The failure has to arrive as an AuthenticationException, or OAuth2AuthenticationFailureHandler never sees it
        // and the unknown-account case surfaces as a 500 instead of a redirect back to the login page.
        assertThatThrownBy(
            () -> SocialUserResolver.resolveInvitedUser(
                userService, EMAIL, "Some", "One", "https://example.com/avatar.png", "GOOGLE", "provider-id"))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .hasCause(illegalStateException);
    }
}
