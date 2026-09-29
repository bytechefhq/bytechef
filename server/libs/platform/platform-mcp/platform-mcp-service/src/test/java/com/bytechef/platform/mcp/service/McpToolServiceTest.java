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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
class McpToolServiceTest {

    private static final long MCP_TOOL_ID = 21L;
    private static final String MCP_TOOL_TYPE = "McpTool";

    private final McpToolRepository mcpToolRepository = mock(McpToolRepository.class);

    private final McpToolServiceImpl mcpToolService = new McpToolServiceImpl(mcpToolRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetMcpToolsRefusesAConnectedUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of("external-user"));

        assertThatThrownBy(mcpToolService::getMcpTools)
            .isInstanceOf(AccessDeniedException.class);

        verify(mcpToolRepository, never()).findAll();
    }

    @Test
    void testGetMcpToolsListsEveryToolForAPlatformUser() {
        McpTool mcpTool = new McpTool("sendMessage", Map.of(), 3L);

        SecurityContextHolder.getContext()
            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated("admin", null, List.of()));

        when(mcpToolRepository.findAll()).thenReturn(List.of(mcpTool));

        assertThat(mcpToolService.getMcpTools()).containsExactly(mcpTool);
    }

    @Nested
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";

        private final PermissionService permissionService = mock(PermissionService.class);
        private final McpToolService securedMcpToolService = secure(
            new McpToolServiceImpl(
                mock(McpToolRepository.class, invocation -> {
                    throw new IllegalStateException(BODY_REACHED);
                })));

        @BeforeEach
        void beforeEach() {
            authenticate();
        }

        @Test
        void testDeleteDeniesWhenTheMcpEditAndMcpDeleteScopesAreBothRefused() {
            assertThatThrownBy(() -> securedMcpToolService.delete(createMcpTool()))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionService).hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_EDIT");
            verify(permissionService).hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_DELETE");
        }

        @Test
        void testDeleteAllowsWhenTheMcpEditScopeIsGranted() {
            when(permissionService.hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_EDIT")).thenReturn(true);

            assertBodyReached(() -> securedMcpToolService.delete(createMcpTool()));
        }

        @Test
        void testDeleteAllowsWhenOnlyTheMcpDeleteScopeIsGranted() {
            when(permissionService.hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_DELETE")).thenReturn(true);

            assertBodyReached(() -> securedMcpToolService.delete(createMcpTool()));

            verify(permissionService).hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_EDIT");
        }

        private static McpTool createMcpTool() {
            McpTool mcpTool = new McpTool();

            mcpTool.setId(MCP_TOOL_ID);

            return mcpTool;
        }

        private McpToolService secure(McpToolService targetService) {
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

            return (McpToolService) proxyFactory.getProxy();
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
}
