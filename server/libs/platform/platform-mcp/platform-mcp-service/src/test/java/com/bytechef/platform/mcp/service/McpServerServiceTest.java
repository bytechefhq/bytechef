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

package com.bytechef.platform.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class McpServerServiceTest {

    private static final long MCP_SERVER_ID = 11L;
    private static final String MCP_SERVER_TYPE = "McpServer";

    private final McpServerRepository mcpServerRepository = mock(McpServerRepository.class);

    private final McpServerServiceImpl mcpServerService = new McpServerServiceImpl(mcpServerRepository);

    @Test
    void testGetEnabledMcpServersReturnsOnlyEnabledServersOfTheRequestedType() {
        McpServer enabledEmbeddedServer = new McpServer(
            "enabled-embedded", PlatformType.EMBEDDED, Environment.DEVELOPMENT, true);
        McpServer disabledEmbeddedServer = new McpServer(
            "disabled-embedded", PlatformType.EMBEDDED, Environment.DEVELOPMENT, false);
        McpServer enabledAutomationServer = new McpServer(
            "enabled-automation", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);

        when(mcpServerRepository.findAll())
            .thenReturn(List.of(enabledEmbeddedServer, disabledEmbeddedServer, enabledAutomationServer));

        List<McpServer> enabledEmbeddedServers = mcpServerService.getEnabledMcpServers(PlatformType.EMBEDDED);

        assertThat(enabledEmbeddedServers).containsExactly(enabledEmbeddedServer);
    }

    @Nested
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";

        private final PermissionService permissionService = mock(PermissionService.class);
        private final McpServerService securedMcpServerService = secure(
            new McpServerServiceImpl(
                mock(McpServerRepository.class, invocation -> {
                    throw new IllegalStateException(BODY_REACHED);
                })));

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @ParameterizedTest
        @MethodSource("resourceGuardCases")
        void testResourceGuardDeniesWhenItsScopeIsRefused(ResourceGuardCase resourceGuardCase) {
            authenticate();

            assertThatThrownBy(() -> resourceGuardCase.guardedOperation()
                .invoke(securedMcpServerService))
                    .isInstanceOf(AccessDeniedException.class);

            verify(permissionService).hasResourceScope(MCP_SERVER_ID, MCP_SERVER_TYPE, resourceGuardCase.scope());
        }

        @ParameterizedTest
        @MethodSource("resourceGuardCases")
        void testResourceGuardAllowsWhenItsScopeIsGranted(ResourceGuardCase resourceGuardCase) {
            authenticate();

            when(permissionService.hasResourceScope(MCP_SERVER_ID, MCP_SERVER_TYPE, resourceGuardCase.scope()))
                .thenReturn(true);

            assertBodyReached(() -> resourceGuardCase.guardedOperation()
                .invoke(securedMcpServerService));
        }

        @Test
        void testListingEveryServerOfATypeDeniesACallerWithoutTheAdminAuthority() {
            authenticate(new SimpleGrantedAuthority(AuthorityConstants.USER));

            assertThatThrownBy(
                () -> securedMcpServerService.getMcpServers(PlatformType.AUTOMATION, null))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(permissionService);
        }

        @Test
        void testListingEveryServerOfATypeAllowsACallerHoldingTheAdminAuthority() {
            authenticate(new SimpleGrantedAuthority(AuthorityConstants.ADMIN));

            assertBodyReached(() -> securedMcpServerService.getMcpServers(PlatformType.AUTOMATION, null));

            verifyNoInteractions(permissionService);
        }

        static Stream<Named<ResourceGuardCase>> resourceGuardCases() {
            return Stream.of(
                Named.of(
                    "getMcpServer",
                    new ResourceGuardCase(
                        guardedService -> guardedService.getMcpServer(MCP_SERVER_ID), "MCP_VIEW")),
                Named.of(
                    "update",
                    new ResourceGuardCase(
                        guardedService -> guardedService.update(MCP_SERVER_ID, "name", Boolean.TRUE), "MCP_EDIT")),
                Named.of(
                    "update of the whole server",
                    new ResourceGuardCase(
                        guardedService -> guardedService.update(createMcpServer()), "MCP_EDIT")));
        }

        @FunctionalInterface
        interface GuardedOperation {

            void invoke(McpServerService guardedService);
        }

        record ResourceGuardCase(GuardedOperation guardedOperation, String scope) {
        }

        private McpServerService secure(McpServerService targetService) {
            AutomationMethodSecurityExpressionHandler expressionHandler =
                new AutomationMethodSecurityExpressionHandler(permissionService);

            expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));
            expressionHandler.setRoleHierarchy(
                RoleHierarchyImpl.withDefaultRolePrefix()
                    .role("ADMIN")
                    .implies("USER")
                    .build());

            PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager =
                new PreAuthorizeAuthorizationManager();

            preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

            ProxyFactory proxyFactory = new ProxyFactory(targetService);

            proxyFactory.addAdvisor(
                AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

            return (McpServerService) proxyFactory.getProxy();
        }

        private static void authenticate(GrantedAuthority... grantedAuthorities) {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "credentials", List.of(grantedAuthorities)));

            SecurityContextHolder.setContext(securityContext);
        }

        private static void assertBodyReached(ThrowingCallable throwingCallable) {
            assertThatThrownBy(throwingCallable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        }
    }

    private static McpServer createMcpServer() {
        McpServer mcpServer = new McpServer();

        mcpServer.setId(MCP_SERVER_ID);

        return mcpServer;
    }
}
