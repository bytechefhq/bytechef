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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@link PreAuthorize} expressions on {@link ManagementMcpServerGraphQlController} through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. The management MCP
 * server URL carries the tenant-wide secret key, so reading and rotating it must decide on the caller's
 * {@code ROLE_ADMIN} authority alone.
 *
 * @author Ivica Cardic
 */
class ManagementMcpServerGraphQlControllerTest {

    static Stream<Arguments> managementMcpServerUrlOperations() {
        return Stream.of("managementMcpServerUrl", "updateManagementMcpServerUrl")
            .flatMap(methodName -> Stream.of(Arguments.of(methodName, false), Arguments.of(methodName, true)));
    }

    @ParameterizedTest(name = "{0} admin={1}")
    @MethodSource("managementMcpServerUrlOperations")
    void testManagementMcpServerUrlRequiresTheAdminAuthority(String methodName, boolean admin) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        Method method = ManagementMcpServerGraphQlController.class.getMethod(methodName);

        assertThat(evaluateGuard(permissionService, method, admin))
            .as("%s must %s a caller %s ROLE_ADMIN", methodName, admin ? "allow" : "deny",
                admin ? "holding" : "without")
            .isEqualTo(admin);

        verifyNoInteractions(permissionService);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method, boolean admin) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        List<GrantedAuthority> authorities = admin
            ? List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))
            : List.of(new SimpleGrantedAuthority(AuthorityConstants.USER));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", authorities);

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(new Object(), method);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }
}
