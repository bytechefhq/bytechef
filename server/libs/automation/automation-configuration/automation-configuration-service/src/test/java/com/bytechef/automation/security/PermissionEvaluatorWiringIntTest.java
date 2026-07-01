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

package com.bytechef.automation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@SpringBootTest(classes = PermissionEvaluatorWiringIntTest.Config.class)
class PermissionEvaluatorWiringIntTest {

    @Autowired
    private GuardedService guardedService;

    @Autowired
    private PermissionService permissionService;

    @BeforeEach
    void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeniesWhenScopeMissing() {
        when(permissionService.hasResourceScope(1L, "Project", "PROJECT_DELETE")).thenReturn(false);

        assertThatThrownBy(() -> guardedService.deleteProject(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testAllowsWhenScopePresent() {
        when(permissionService.hasResourceScope(1L, "Project", "PROJECT_DELETE")).thenReturn(true);

        assertThatCode(() -> guardedService.deleteProject(1L)).doesNotThrowAnyException();
    }

    @Test
    void testResourceScopeTokenIsWired() {
        when(permissionService.hasResourceScope(3L, "Connection", "CONNECTION_DELETE")).thenReturn(false);

        assertThatThrownBy(() -> guardedService.deleteConnection(3L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testSkipContextBypassesDenial() throws Throwable {
        when(permissionService.hasResourceScope(1L, "Project", "PROJECT_DELETE")).thenReturn(false);

        Boolean result = AutomationAuthorizationContext.callSkippingChecks(() -> {
            guardedService.deleteProject(1L);

            return Boolean.TRUE;
        });

        assertThat(result).isTrue();
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import(GuardedService.class)
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }

    @Service
    static class GuardedService {

        @PreAuthorize("hasPermission(#projectId, 'Project', 'PROJECT_DELETE')")
        public void deleteProject(long projectId) {
        }

        @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_DELETE')")
        public void deleteConnection(long id) {
        }
    }
}
