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

package com.bytechef.ai.mcp.server.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    ManagementMcpServerGraphQlController.class,
    ManagementMcpServerGraphQlControllerIntTest.MethodSecurityConfiguration.class
})
@GraphQlTest(
    controllers = ManagementMcpServerGraphQlController.class,
    properties = "spring.graphql.schema.locations=classpath*:/graphql/")
class ManagementMcpServerGraphQlControllerIntTest {

    private static final String BODY_REACHED = "body reached";

    @MockitoBean
    private ApplicationProperties applicationProperties;

    @Autowired
    private ManagementMcpServerGraphQlController managementMcpServerGraphQlController;

    @Autowired
    private PermissionService permissionService;

    @MockitoBean
    private PropertyService propertyService;

    @BeforeEach
    void beforeEach() {
        IllegalStateException bodyReachedException = new IllegalStateException(BODY_REACHED);

        doThrow(bodyReachedException).when(propertyService)
            .fetchProperty(anyString(), any(), isNull());
        doThrow(bodyReachedException).when(propertyService)
            .save(anyString(), anyMap(), any(), isNull());
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("managementMcpServerUrlOperations")
    void testManagementMcpServerUrlDeniesACallerWithoutTheAdminAuthority(GuardedOperation guardedOperation) {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> guardedOperation.invoke(managementMcpServerGraphQlController))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(permissionService);
    }

    @ParameterizedTest
    @MethodSource("managementMcpServerUrlOperations")
    void testManagementMcpServerUrlAllowsACallerHoldingTheAdminAuthority(GuardedOperation guardedOperation) {
        authenticate(AuthorityConstants.ADMIN);

        assertThatThrownBy(() -> guardedOperation.invoke(managementMcpServerGraphQlController))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);

        verifyNoInteractions(permissionService);
    }

    static Stream<Named<GuardedOperation>> managementMcpServerUrlOperations() {
        return Stream.of(
            Named.of(
                "managementMcpServerUrl",
                (GuardedOperation) ManagementMcpServerGraphQlController::managementMcpServerUrl),
            Named.of(
                "updateManagementMcpServerUrl",
                (GuardedOperation) ManagementMcpServerGraphQlController::updateManagementMcpServerUrl));
    }

    private static void authenticate(String authority) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "alice", "credentials", List.of(new SimpleGrantedAuthority(authority))));

        SecurityContextHolder.setContext(securityContext);
    }

    @FunctionalInterface
    interface GuardedOperation {

        void invoke(ManagementMcpServerGraphQlController guardedController);
    }

    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean
        PermissionService permissionService() {
            return mock(PermissionService.class, MockReset.withSettings(MockReset.AFTER));
        }
    }
}
