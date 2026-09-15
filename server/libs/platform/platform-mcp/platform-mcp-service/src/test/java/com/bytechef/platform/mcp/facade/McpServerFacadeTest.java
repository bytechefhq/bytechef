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

package com.bytechef.platform.mcp.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class McpServerFacadeTest {

    private static final long MCP_COMPONENT_ID = 13L;
    private static final String MCP_COMPONENT_TYPE = "McpComponent";
    private static final long MCP_SERVER_ID = 11L;
    private static final String MCP_SERVER_TYPE = "McpServer";

    @Nested
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";

        private final PermissionService permissionService = mock(PermissionService.class);
        private final McpServerFacade securedMcpServerFacade = secure(
            new McpServerFacadeImpl(
                mock(McpComponentService.class, invocation -> {
                    throw new IllegalStateException(BODY_REACHED);
                }),
                mock(McpServerService.class, invocation -> {
                    throw new IllegalStateException(BODY_REACHED);
                }),
                mock(McpToolService.class, invocation -> {
                    throw new IllegalStateException(BODY_REACHED);
                }),
                mock(TagService.class, invocation -> {
                    throw new IllegalStateException(BODY_REACHED);
                })));

        @BeforeEach
        void beforeEach() {
            authenticate();
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @ParameterizedTest
        @MethodSource("guardCases")
        void testGuardDeniesWhenItsResourceScopeIsRefused(GuardCase guardCase) {
            assertThatThrownBy(() -> guardCase.guardedOperation()
                .invoke(securedMcpServerFacade))
                    .isInstanceOf(AccessDeniedException.class);

            verify(permissionService).hasResourceScope(
                guardCase.resourceId(), guardCase.resourceType(), guardCase.scope());
        }

        @ParameterizedTest
        @MethodSource("guardCases")
        void testGuardAllowsWhenItsResourceScopeIsGranted(GuardCase guardCase) {
            when(permissionService.hasResourceScope(
                guardCase.resourceId(), guardCase.resourceType(), guardCase.scope())).thenReturn(true);

            assertBodyReached(() -> guardCase.guardedOperation()
                .invoke(securedMcpServerFacade));
        }

        static Stream<Named<GuardCase>> guardCases() {
            return Stream.of(
                Named.of(
                    "create",
                    new GuardCase(
                        guardedFacade -> guardedFacade.create(mcpComponent(), List.of()), MCP_SERVER_ID,
                        MCP_SERVER_TYPE, "MCP_EDIT")),
                Named.of(
                    "deleteMcpComponent",
                    new GuardCase(
                        guardedFacade -> guardedFacade.deleteMcpComponent(MCP_COMPONENT_ID), MCP_COMPONENT_ID,
                        MCP_COMPONENT_TYPE, "MCP_EDIT")),
                Named.of(
                    "deleteMcpServer",
                    new GuardCase(
                        guardedFacade -> guardedFacade.deleteMcpServer(MCP_SERVER_ID), MCP_SERVER_ID,
                        MCP_SERVER_TYPE, "MCP_DELETE")),
                Named.of(
                    "update",
                    new GuardCase(
                        guardedFacade -> guardedFacade.update(mcpComponent(), List.of()), MCP_COMPONENT_ID,
                        MCP_COMPONENT_TYPE, "MCP_EDIT")),
                Named.of(
                    "updateMcpServerTags",
                    new GuardCase(
                        guardedFacade -> guardedFacade.updateMcpServerTags(MCP_SERVER_ID, List.of()), MCP_SERVER_ID,
                        MCP_SERVER_TYPE, "MCP_EDIT")));
        }

        @FunctionalInterface
        interface GuardedOperation {

            void invoke(McpServerFacade guardedFacade);
        }

        record GuardCase(GuardedOperation guardedOperation, long resourceId, String resourceType, String scope) {
        }

        private McpServerFacade secure(McpServerFacade targetService) {
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

            return (McpServerFacade) proxyFactory.getProxy();
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

    private static McpComponent mcpComponent() {
        McpComponent mcpComponent = new McpComponent("component", 1, MCP_SERVER_ID, null);

        mcpComponent.setId(MCP_COMPONENT_ID);

        return mcpComponent;
    }
}
