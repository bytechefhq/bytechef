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

import com.bytechef.security.web.oauth2.SocialUserProvisioner.SocialIdentity;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * @author Ivica Cardic
 */
class SocialOidcUserServiceTest {

    @Test
    void testToSocialIdentityMapsGoogleClaims() {
        OidcIdToken idToken = idToken(Map.of(
            "email", "someone@example.com", "email_verified", true, "given_name", "Some", "family_name", "One",
            "picture", "https://example.com/p.png"));

        SocialIdentity socialIdentity = SocialOidcUserService.toSocialIdentity(userRequest(idToken), oidcUser(idToken));

        assertThat(socialIdentity).isEqualTo(
            new SocialIdentity(
                "GOOGLE", "google-subject", "someone@example.com", "Some", "One", "https://example.com/p.png"));
    }

    @Test
    void testToSocialIdentityRejectsUnverifiedEmail() {
        OidcIdToken idToken = idToken(Map.of("email", "victim@example.com", "email_verified", false));

        assertThatThrownBy(() -> SocialOidcUserService.toSocialIdentity(userRequest(idToken), oidcUser(idToken)))
            .isInstanceOf(OAuth2AuthenticationException.class)
            .extracting(exception -> ((OAuth2AuthenticationException) exception).getError()
                .getErrorCode())
            .isEqualTo(SocialOidcUserService.EMAIL_NOT_VERIFIED_ERROR_CODE);
    }

    @Test
    void testToSocialIdentityRejectsMissingEmail() {
        OidcIdToken idToken = idToken(Map.of("name", "No Email"));

        assertThatThrownBy(() -> SocialOidcUserService.toSocialIdentity(userRequest(idToken), oidcUser(idToken)))
            .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private static OidcIdToken idToken(Map<String, Object> claims) {
        Map<String, Object> allClaims = new HashMap<>(claims);

        allClaims.put("sub", "google-subject");

        return new OidcIdToken("token", Instant.now(), Instant.now()
            .plusSeconds(60), allClaims);
    }

    private static OidcUser oidcUser(OidcIdToken idToken) {
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken);
    }

    private static OidcUserRequest userRequest(OidcIdToken idToken) {
        return new OidcUserRequest(
            CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .build(),
            new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access", Instant.now(), Instant.now()
                .plusSeconds(60)),
            idToken);
    }
}
