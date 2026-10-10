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

package com.bytechef.platform.security.web.mcp.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @author Ivica Cardic
 */
class McpJwtIdentityTest {

    @Test
    void testRejectsNullTenantId() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new McpJwtIdentity(null, "user@customer.test", List.of()));
    }

    @Test
    void testRejectsNullLogin() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new McpJwtIdentity("acme", null, List.of()));
    }

    @Test
    void testRejectsNullAuthorities() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new McpJwtIdentity("acme", "user@customer.test", null));
    }

    @Test
    void testCopiesAuthoritiesDefensively() {
        List<GrantedAuthority> authorities = new ArrayList<>(List.of(new SimpleGrantedAuthority("ROLE_USER")));

        McpJwtIdentity mcpJwtIdentity = new McpJwtIdentity("acme", "user@customer.test", authorities);

        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        List<GrantedAuthority> identityAuthorities = mcpJwtIdentity.authorities();
        SimpleGrantedAuthority adminAuthority = new SimpleGrantedAuthority("ROLE_ADMIN");

        assertThat(identityAuthorities).containsExactly(new SimpleGrantedAuthority("ROLE_USER"));
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> identityAuthorities.add(adminAuthority));
    }
}
