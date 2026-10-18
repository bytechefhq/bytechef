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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @author Ivica Cardic
 */
class CustomOAuth2UserTest {

    @Test
    void testKeepsNullAttributesSuchAsAPrivateGitHubEmail() {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put("email", null);
        attributes.put("id", 42);

        CustomOAuth2User customOAuth2User = new CustomOAuth2User(
            "octocat@example.com", List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes);

        assertThat(customOAuth2User.getName()).isEqualTo("octocat@example.com");
        assertThat(customOAuth2User.getAttributes()).containsEntry("email", null)
            .containsEntry("id", 42);
    }
}
