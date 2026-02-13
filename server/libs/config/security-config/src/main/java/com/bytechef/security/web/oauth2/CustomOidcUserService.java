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
import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Custom OIDC user service that integrates OIDC provider users (Okta, Azure AD, Google Workspace) with the internal
 * ByteChef user model. Extracts standardized OIDC claims (email, given_name, family_name, picture, sub) and
 * finds/creates the corresponding internal user.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.security.social-login", name = "enabled", havingValue = "true")
public class CustomOidcUserService extends OidcUserService {

    private static final String SSO_PREFIX = "sso-";

    private final AuthorityService authorityService;
    private final IdentityProviderService identityProviderService;
    private final UserService userService;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public CustomOidcUserService(
        AuthorityService authorityService,
        ObjectProvider<IdentityProviderService> identityProviderServiceProvider,
        UserService userService) {

        this.authorityService = authorityService;
        this.identityProviderService = identityProviderServiceProvider.getIfAvailable();
        this.userService = userService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not available from OIDC provider");
        }

        String firstName = oidcUser.getGivenName();
        String lastName = oidcUser.getFamilyName();
        String imageUrl = oidcUser.getPicture();
        String providerId = oidcUser.getSubject();

        ClientRegistration clientRegistration = userRequest.getClientRegistration();

        String registrationId = clientRegistration.getRegistrationId();

        String authProvider = registrationId.startsWith(SSO_PREFIX) ? "SSO" : registrationId.toUpperCase();

        boolean autoProvision = true;
        String defaultAuthority = AuthorityConstants.ADMIN;

        if (registrationId.startsWith(SSO_PREFIX) && identityProviderService != null) {
            long identityProviderId = Long.parseLong(registrationId.substring(SSO_PREFIX.length()));

            IdentityProvider identityProvider = identityProviderService.getIdentityProvider(identityProviderId);

            autoProvision = identityProvider.isAutoProvision();
            defaultAuthority = identityProvider.getDefaultAuthority();
        }

        User user = userService.findOrCreateSocialUser(
            email, firstName, lastName, imageUrl, authProvider, providerId, autoProvision, defaultAuthority);

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
