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
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * OIDC user service for social login (Google, GitHub, ...): maps an OIDC provider user to an internal ByteChef user
 * with default provisioning ({@code autoProvision=true}, {@code ROLE_ADMIN}). External-IdP (SSO) provisioning that
 * reads {@code IdentityProvider} records is an enterprise capability and lives in the EE {@code CustomOidcUserService}.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.security.social-login", name = "enabled", havingValue = "true")
public class SocialOidcUserService extends OidcUserService {

    private final AuthorityService authorityService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public SocialOidcUserService(AuthorityService authorityService, UserService userService) {
        this.authorityService = authorityService;
        this.userService = userService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        if (oidcUser == null) {
            throw new OAuth2AuthenticationException("No user returned by OIDC provider");
        }

        String email = oidcUser.getEmail();

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not available from OIDC provider");
        }

        String registrationId = userRequest.getClientRegistration()
            .getRegistrationId();

        User user = userService.findOrCreateSocialUser(
            email, oidcUser.getGivenName(), oidcUser.getFamilyName(), oidcUser.getPicture(),
            registrationId.toUpperCase(), oidcUser.getSubject(), true, AuthorityConstants.ADMIN);

        List<SimpleGrantedAuthority> grantedAuthorities = user.getAuthorityIds()
            .stream()
            .map(authorityService::fetchAuthority)
            .map(Optional::get)
            .map(Authority::getName)
            .map(SimpleGrantedAuthority::new)
            .toList();

        return new CustomOidcUser(user.getLogin(), grantedAuthorities, oidcUser);
    }
}
