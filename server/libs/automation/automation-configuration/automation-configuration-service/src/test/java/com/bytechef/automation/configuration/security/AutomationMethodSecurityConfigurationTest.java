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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AutomationMethodSecurityConfigurationTest {

    private final RoleHierarchy roleHierarchy = AutomationMethodSecurityConfiguration.roleHierarchy();

    @Test
    void testAdminReachesUserAuthority() {
        Collection<? extends GrantedAuthority> reachable = roleHierarchy.getReachableGrantedAuthorities(
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(reachable)
            .extracting(GrantedAuthority::getAuthority)
            .contains("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void testUserDoesNotReachAdminAuthority() {
        Collection<? extends GrantedAuthority> reachable = roleHierarchy.getReachableGrantedAuthorities(
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(reachable)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
    }
}
