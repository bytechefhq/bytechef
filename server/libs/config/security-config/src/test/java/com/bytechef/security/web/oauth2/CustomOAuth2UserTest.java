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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.util.SerializationUtils;

/**
 * @author Ivica Cardic
 */
class CustomOAuth2UserTest {

    @Test
    void testSerializationRoundTrip() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        CustomOAuth2User customOAuth2User = new CustomOAuth2User(
            "user@localhost.com", authorities, Map.of("login", "octocat", "id", 42));

        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
            customOAuth2User, authorities, "github");

        OAuth2AuthenticationToken deserializedAuthenticationToken =
            (OAuth2AuthenticationToken) SerializationUtils.deserialize(
                SerializationUtils.serialize(authenticationToken));

        assertThat(deserializedAuthenticationToken).isNotNull();

        CustomOAuth2User deserializedCustomOAuth2User =
            (CustomOAuth2User) deserializedAuthenticationToken.getPrincipal();

        assertThat(deserializedCustomOAuth2User.getName()).isEqualTo("user@localhost.com");
        assertThat(deserializedCustomOAuth2User.getAuthorities()).isEqualTo(customOAuth2User.getAuthorities());
        assertThat(deserializedCustomOAuth2User.getAttributes()).isEqualTo(customOAuth2User.getAttributes());
    }
}
