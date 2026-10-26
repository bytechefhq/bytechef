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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.util.SerializationUtils;

/**
 * @author Ivica Cardic
 */
class CustomOidcUserTest {

    @Test
    void testSerializationRoundTrip() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        Instant issuedAt = Instant.parse("2026-10-26T08:00:00Z");

        OidcIdToken oidcIdToken = new OidcIdToken(
            "token-value", issuedAt, issuedAt.plusSeconds(3600), Map.of("sub", "subject-1", "email", "u@x.com"));

        OidcUserInfo oidcUserInfo = new OidcUserInfo(Map.of("sub", "subject-1", "name", "User"));

        CustomOidcUser customOidcUser = new CustomOidcUser(
            "user@localhost.com", authorities, new DefaultOidcUser(authorities, oidcIdToken, oidcUserInfo));

        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
            customOidcUser, authorities, "google");

        OAuth2AuthenticationToken deserializedAuthenticationToken =
            (OAuth2AuthenticationToken) SerializationUtils.deserialize(
                SerializationUtils.serialize(authenticationToken));

        assertThat(deserializedAuthenticationToken).isNotNull();

        CustomOidcUser deserializedCustomOidcUser = (CustomOidcUser) deserializedAuthenticationToken.getPrincipal();

        assertThat(deserializedCustomOidcUser.getName()).isEqualTo("user@localhost.com");
        assertThat(deserializedCustomOidcUser.getAuthorities()).isEqualTo(customOidcUser.getAuthorities());
        assertThat(deserializedCustomOidcUser.getClaims()).isEqualTo(customOidcUser.getClaims());
        assertThat(deserializedCustomOidcUser.getAttributes()).isEqualTo(customOidcUser.getAttributes());
        assertThat(deserializedCustomOidcUser.getIdToken()
            .getTokenValue()).isEqualTo("token-value");
        assertThat(deserializedCustomOidcUser.getUserInfo()
            .getFullName()).isEqualTo("User");
    }
}
