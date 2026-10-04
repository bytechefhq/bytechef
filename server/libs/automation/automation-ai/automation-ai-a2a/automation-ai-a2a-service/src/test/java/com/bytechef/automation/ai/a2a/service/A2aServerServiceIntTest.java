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

package com.bytechef.automation.ai.a2a.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.config.A2aIntTestConfiguration;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.config.A2aMethodSecurityIntTestConfiguration;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class A2aServerServiceIntTest {

    @Nested
    @SpringBootTest(classes = A2aMethodSecurityIntTestConfiguration.class)
    class MethodSecurity {

        private static final String SECRET_KEY = "server-secret";

        @Autowired
        private A2aServerRepository a2aServerRepository;

        @Autowired
        private A2aServerService a2aServerService;

        @Autowired
        private PermissionService permissionService;

        private A2aServer a2aServer;

        @BeforeEach
        void beforeEach() {
            a2aServer = new A2aServer("agent", null, Environment.DEVELOPMENT);

            a2aServer.setId(1L);

            when(a2aServerRepository.findById(1L)).thenReturn(Optional.of(a2aServer));
            when(a2aServerRepository.findBySecretKey(SECRET_KEY)).thenReturn(Optional.of(a2aServer));
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();

            reset(a2aServerRepository, permissionService);
        }

        @Test
        void testNonAdminCannotCreateUpdateOrDeleteAServer() {
            authenticate("ROLE_USER");

            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> a2aServerService.create(new A2aServer("agent", "", Environment.DEVELOPMENT)));
            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> a2aServerService.update(a2aServer));
            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> a2aServerService.delete(1L));

            verify(a2aServerRepository, never()).save(any());
            verify(a2aServerRepository, never()).findById(anyLong());
            verify(a2aServerRepository, never()).deleteById(anyLong());
            verifyNoInteractions(permissionService);
        }

        @Test
        void testAdminCanDeleteAServer() {
            authenticate("ROLE_ADMIN");

            a2aServerService.delete(1L);

            verify(a2aServerRepository).deleteById(1L);
        }

        @Test
        void testSecretKeyIsDeniedWhenTheCallerIsNotATenantAdmin() {
            authenticate("ROLE_ADMIN");

            when(permissionService.isTenantAdmin()).thenReturn(false);

            assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> a2aServerService.getA2aServerSecretKey(1L));

            verify(permissionService).isTenantAdmin();
            verify(a2aServerRepository, never()).findById(anyLong());
        }

        @Test
        void testSecretKeyIsReturnedToATenantAdmin() {
            authenticate("ROLE_ADMIN");

            when(permissionService.isTenantAdmin()).thenReturn(true);

            assertThat(a2aServerService.getA2aServerSecretKey(1L)).isEqualTo(a2aServer.getSecretKey());

            verify(permissionService).isTenantAdmin();
        }

        @Test
        void testDataPlaneLookupsStayOpenToNonAdmins() {
            authenticate("ROLE_USER");

            assertThat(a2aServerService.getA2aServer(SECRET_KEY)).isSameAs(a2aServer);
            assertThat(a2aServerService.fetchA2aServer(SECRET_KEY)).contains(a2aServer);
            assertThat(a2aServerService.getA2aServer(1L)).isSameAs(a2aServer);

            verifyNoInteractions(permissionService);
        }

        private static void authenticate(String authority) {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "user", "n/a", List.of(new SimpleGrantedAuthority(authority))));
        }
    }

    @Nested
    @SpringBootTest(classes = A2aIntTestConfiguration.class)
    @Import(PostgreSQLContainerConfiguration.class)
    @A2aIntTestConfigurationSharedMocks
    class Persistence {

        @Autowired
        private A2aServerRepository a2aServerRepository;

        @Autowired
        private A2aServerService a2aServerService;

        @AfterEach
        void afterEach() {
            a2aServerRepository.deleteAll();
        }

        @Test
        void testCreatePersistsTheGivenFields() {
            A2aServer a2aServer = new A2aServer("agent", "answers questions", Environment.DEVELOPMENT);

            A2aServer createdA2aServer = a2aServerService.create(a2aServer);

            A2aServer persistedA2aServer = a2aServerRepository.findById(createdA2aServer.getId())
                .orElseThrow();

            assertThat(persistedA2aServer.getName()).isEqualTo("agent");
            assertThat(persistedA2aServer.getDescription()).isEqualTo("answers questions");
            assertThat(persistedA2aServer.getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
            assertThat(persistedA2aServer.getUuid()).isNotNull();
            assertThat(persistedA2aServer.getSecretKey()).isEqualTo(a2aServer.getSecretKey());
        }

        @Test
        void testCreateAssignsUuidWhenExplicitlyNull() {
            A2aServer a2aServer = new A2aServer("agent", null, Environment.DEVELOPMENT);

            a2aServer.setUuid(null);

            A2aServer createdA2aServer = a2aServerService.create(a2aServer);

            assertThat(createdA2aServer.getUuid()).isNotNull();
            assertThat(a2aServerRepository.findById(createdA2aServer.getId()))
                .hasValueSatisfying(
                    persistedA2aServer -> assertThat(persistedA2aServer.getUuid()).isEqualTo(
                        createdA2aServer.getUuid()));
        }

        @Test
        void testCreateRejectsAServerWithoutASecretKey() {
            assertThatNullPointerException()
                .isThrownBy(() -> a2aServerService.create(new A2aServer()))
                .withMessageContaining("secret key");

            assertThat(a2aServerRepository.count()).isZero();
        }

        @Test
        void testUpdateKeepsUuidAndSecretKey() {
            A2aServer currentA2aServer = a2aServerService.create(
                new A2aServer("agent", null, Environment.DEVELOPMENT));

            A2aServer incomingA2aServer = new A2aServer("agent2", null, Environment.DEVELOPMENT);

            incomingA2aServer.setId(currentA2aServer.getId());
            incomingA2aServer.setUuid(UUID.randomUUID());
            incomingA2aServer.setVersion(currentA2aServer.getVersion());

            a2aServerService.update(incomingA2aServer);

            A2aServer persistedA2aServer = a2aServerRepository.findById(currentA2aServer.getId())
                .orElseThrow();

            assertThat(persistedA2aServer.getName()).isEqualTo("agent2");
            assertThat(persistedA2aServer.getUuid()).isEqualTo(currentA2aServer.getUuid());
            assertThat(persistedA2aServer.getSecretKey()).isEqualTo(currentA2aServer.getSecretKey())
                .isNotEqualTo(incomingA2aServer.getSecretKey());
        }

        @Test
        void testUpdateKeepsTheEnvironment() {
            A2aServer currentA2aServer = a2aServerService.create(
                new A2aServer("agent", null, Environment.DEVELOPMENT));

            A2aServer incomingA2aServer = new A2aServer("agent", null, Environment.PRODUCTION);

            incomingA2aServer.setId(currentA2aServer.getId());
            incomingA2aServer.setVersion(currentA2aServer.getVersion());

            assertThat(a2aServerService.update(incomingA2aServer)
                .getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
            assertThat(a2aServerRepository.findById(currentA2aServer.getId()))
                .hasValueSatisfying(
                    persistedA2aServer -> assertThat(persistedA2aServer.getEnvironment()).isEqualTo(
                        Environment.DEVELOPMENT));
        }

        @Test
        void testFetchBySecretKeyFindsThePersistedServer() {
            A2aServer createdA2aServer = a2aServerService.create(
                new A2aServer("agent", null, Environment.DEVELOPMENT));

            assertThat(a2aServerService.fetchA2aServer(createdA2aServer.getSecretKey()))
                .hasValueSatisfying(
                    a2aServer -> assertThat(a2aServer.getId()).isEqualTo(createdA2aServer.getId()));
        }

        @Test
        void testFetchBySecretKeyIsEmptyForAnUnknownKey() {
            assertThat(a2aServerService.fetchA2aServer("unknown")).isEmpty();
            assertThatIllegalArgumentException().isThrownBy(() -> a2aServerService.getA2aServer("unknown"));
        }

        @Test
        void testGetA2aServerBySecretKeyDoesNotDiscloseTheKeyWhenMissing() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> a2aServerService.getA2aServer("server-secret"))
                .withMessageNotContaining("server-secret");
        }
    }
}
