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

import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * Resolves the ByteChef user behind an external identity, shared by every login path that maps a provider identity onto
 * an internal user so they cannot drift apart: OIDC (Google), plain OAuth2 (GitHub), and the enterprise SSO path.
 *
 * <p>
 * Its one behavioural job is to turn a refused provisioning attempt into an authentication failure. {@code UserService}
 * signals that with an {@link IllegalStateException}, which is not an {@code AuthenticationException} — left unwrapped
 * it escapes the filter chain, so the caller gets a 500 instead of a redirect back to the login page.
 *
 * @author Ivica Cardic
 */
public final class SocialUserResolver {

    private static final String ACCOUNT_NOT_PROVISIONED_ERROR_CODE = "account_not_provisioned";

    private SocialUserResolver() {
    }

    /**
     * Resolves a social-login identity: social login signs an invited user in rather than provisioning a new one, so
     * auto-provisioning is off and an unknown email is rejected. The two are coupled — the authority this path asks for
     * is {@code ROLE_ADMIN}, so provisioning here would let anyone holding an account at the configured provider, on
     * any email domain, self-escalate to instance admin. An email that already has a ByteChef account keeps the
     * authorities it already holds.
     */
    static User resolveInvitedUser(
        UserService userService, String email, String firstName, String lastName, String imageUrl, String authProvider,
        String providerId) {

        return resolveUser(
            userService, email, firstName, lastName, imageUrl, authProvider, providerId, false,
            AuthorityConstants.ADMIN);
    }

    /**
     * Resolves an identity under caller-chosen provisioning settings, for the SSO path where they come from the
     * tenant's {@code IdentityProvider} record. Both are passed through untouched — an administrator who turned
     * auto-provisioning off gets a rejected login rather than a provisioned account.
     */
    public static User resolveUser(
        UserService userService, String email, String firstName, String lastName, String imageUrl, String authProvider,
        String providerId, boolean autoProvision, String defaultAuthority) {

        try {
            return userService.findOrCreateSocialUser(
                email, firstName, lastName, imageUrl, authProvider, providerId, autoProvision, defaultAuthority);
        } catch (IllegalStateException illegalStateException) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(ACCOUNT_NOT_PROVISIONED_ERROR_CODE, "No ByteChef account exists for " + email, null),
                illegalStateException);
        }
    }
}
