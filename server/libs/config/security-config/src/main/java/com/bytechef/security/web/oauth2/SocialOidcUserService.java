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

import com.bytechef.security.web.oauth2.SocialUserProvisioner.ProvisionedUser;
import com.bytechef.security.web.oauth2.SocialUserProvisioner.SocialIdentity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * OIDC user service for social login (Google): maps an OIDC provider user to an internal ByteChef user through
 * {@link SocialUserProvisioner}. External-IdP (SSO) provisioning that reads {@code IdentityProvider} records is an
 * enterprise capability and lives in the EE {@code CustomOidcUserService}.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.security.social-login", name = "enabled", havingValue = "true")
public class SocialOidcUserService extends OidcUserService {

    static final String EMAIL_NOT_VERIFIED_ERROR_CODE = "email_not_verified";

    private final SocialUserProvisioner socialUserProvisioner;

    @SuppressFBWarnings("EI")
    public SocialOidcUserService(SocialUserProvisioner socialUserProvisioner) {
        this.socialUserProvisioner = socialUserProvisioner;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        if (oidcUser == null) {
            throw new OAuth2AuthenticationException("No user returned by OIDC provider");
        }

        return toCustomOidcUser(userRequest, oidcUser);
    }

    CustomOidcUser toCustomOidcUser(OidcUserRequest userRequest, OidcUser oidcUser) {
        ProvisionedUser provisionedUser = socialUserProvisioner.provision(toSocialIdentity(userRequest, oidcUser));

        return new CustomOidcUser(provisionedUser.login(), provisionedUser.authorities(), oidcUser);
    }

    /**
     * Turns an OIDC social identity into a {@link SocialIdentity}, rejecting one without a verified email: sign-in
     * links to an existing account by email, so an unverified address would hand that account to whoever claimed it.
     */
    public static SocialIdentity toSocialIdentity(OidcUserRequest userRequest, OidcUser oidcUser) {
        String email = oidcUser.getEmail();

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not available from OIDC provider");
        }

        if (Boolean.FALSE.equals(oidcUser.getEmailVerified())) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(EMAIL_NOT_VERIFIED_ERROR_CODE, "Email " + email + " is not verified", null));
        }

        String registrationId = userRequest.getClientRegistration()
            .getRegistrationId();

        return new SocialIdentity(
            registrationId.toUpperCase(Locale.ROOT), oidcUser.getSubject(), email, oidcUser.getGivenName(),
            oidcUser.getFamilyName(), oidcUser.getPicture());
    }
}
