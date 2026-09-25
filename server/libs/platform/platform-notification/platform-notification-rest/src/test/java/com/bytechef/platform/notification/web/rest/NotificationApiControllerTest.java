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

package com.bytechef.platform.notification.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
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
 * Evaluates the real {@link PreAuthorize} expressions on the {@link NotificationApiController} writes through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. Notifications are
 * configured for the whole tenant, so each write must decide on the caller's {@code ROLE_ADMIN} authority alone.
 *
 * @author Ivica Cardic
 */
class NotificationApiControllerTest {

    static Stream<Arguments> notificationWrites() {
        return Stream.of("createNotification", "deleteNotification", "updateNotification")
            .flatMap(methodName -> Stream.of(Arguments.of(methodName, false), Arguments.of(methodName, true)));
    }

    @ParameterizedTest(name = "{0} admin={1}")
    @MethodSource("notificationWrites")
    void testNotificationWriteRequiresTheAdminAuthority(String methodName, boolean admin) {
        PermissionService permissionService = mock(PermissionService.class);

        assertThat(evaluateGuard(permissionService, findMethod(methodName), admin))
            .as("%s must %s a caller %s ROLE_ADMIN", methodName, admin ? "allow" : "deny",
                admin ? "holding" : "without")
            .isEqualTo(admin);

        verifyNoInteractions(permissionService);
    }

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

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, new Object[method.getParameterCount()]);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method findMethod(String methodName) {
        List<Method> methods = Arrays.stream(NotificationApiController.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic() && Modifier.isPublic(method.getModifiers()))
            .filter(method -> methodName.equals(method.getName()))
            .toList();

        assertThat(methods)
            .as("Expected exactly one public '%s' method on NotificationApiController", methodName)
            .hasSize(1);

        return methods.getFirst();
    }
}
