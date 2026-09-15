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

package com.bytechef.platform.mcp.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.web.graphql.McpServerGraphQlController.McpServerInput;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@link PreAuthorize} expression on {@link McpServerGraphQlController#createMcpServer} through the
 * real {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. The mutation
 * creates an MCP server outside any workspace, so it must decide on the caller's {@code ROLE_ADMIN} authority alone;
 * workspace members create theirs through the workspace mutation, which requires {@code MCP_CREATE}.
 *
 * @author Ivica Cardic
 */
class McpServerGraphQlControllerTest {

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testCreateMcpServerRequiresTheAdminAuthority(boolean admin) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        assertThat(evaluateGuard(permissionService, admin))
            .as("createMcpServer must %s a caller %s ROLE_ADMIN", admin ? "allow" : "deny",
                admin ? "holding" : "without")
            .isEqualTo(admin);

        verifyNoInteractions(permissionService);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, boolean admin)
        throws NoSuchMethodException {

        Method method = McpServerGraphQlController.class.getMethod("createMcpServer", McpServerInput.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("createMcpServer must carry a @PreAuthorize guard")
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        List<GrantedAuthority> authorities = admin
            ? List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))
            : List.of(new SimpleGrantedAuthority(AuthorityConstants.USER));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", authorities);

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, new McpServerInput("server", PlatformType.AUTOMATION, 0L, true));

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }
}
