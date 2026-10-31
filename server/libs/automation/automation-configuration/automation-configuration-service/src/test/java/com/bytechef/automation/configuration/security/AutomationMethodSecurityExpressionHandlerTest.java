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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import java.lang.reflect.Method;
import java.util.List;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.ReflectionUtils;

/**
 * @author Ivica Cardic
 */
class AutomationMethodSecurityExpressionHandlerTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

    @Test
    void testResolvesContributorMethodByBareName() {
        EvaluationContext evaluationContext = createEvaluationContext(new FirstContributor());

        assertThat(evaluate("isFirst()", evaluationContext)).isTrue();
        assertThat(evaluate("isFirstWithId(7)", evaluationContext)).isTrue();
        assertThat(evaluate("isFirstWithId(8)", evaluationContext)).isFalse();
        assertThat(evaluate("isTenantAdmin() or isFirst()", evaluationContext)).isTrue();
    }

    @Test
    void testResolvesContributorMethodOnRepeatedEvaluation() {
        Expression expression = spelExpressionParser.parseExpression("isFirstWithId(7)");

        assertThat(expression.getValue(createEvaluationContext(new FirstContributor()), Boolean.class)).isTrue();
        assertThat(expression.getValue(createEvaluationContext(new FirstContributor()), Boolean.class)).isTrue();
    }

    @Test
    void testDoesNotResolveContributorMethodOnNonRootTarget() {
        EvaluationContext evaluationContext = createEvaluationContext(new FirstContributor());

        assertThatThrownBy(() -> evaluate("'text'.isFirst()", evaluationContext))
            .isInstanceOf(SpelEvaluationException.class);
    }

    @Test
    void testRejectsContributorThatRedefinesRootMethod() {
        assertThatThrownBy(() -> createEvaluationContext(new TenantAdminContributor()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("isTenantAdmin");
    }

    @Test
    void testRejectsContributorsThatDefineTheSameMethod() {
        assertThatThrownBy(() -> createEvaluationContext(new FirstContributor(), new SecondContributor()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("isFirst");
    }

    private EvaluationContext createEvaluationContext(MethodSecurityExpressionContributor... contributors) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        for (int i = 0; i < contributors.length; i++) {
            beanFactory.addBean("contributor" + i, contributors[i]);
        }

        ObjectProvider<MethodSecurityExpressionContributor> contributorProvider =
            beanFactory.getBeanProvider(MethodSecurityExpressionContributor.class);

        AutomationMethodSecurityExpressionHandler handler = new AutomationMethodSecurityExpressionHandler(
            permissionService, contributorProvider);

        return handler.createEvaluationContext(
            () -> new UsernamePasswordAuthenticationToken(
                "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))),
            createMethodInvocation());
    }

    private Boolean evaluate(String expression, EvaluationContext evaluationContext) {
        return spelExpressionParser.parseExpression(expression)
            .getValue(evaluationContext, Boolean.class);
    }

    private static MethodInvocation createMethodInvocation() {
        Method method = ReflectionUtils.findMethod(Object.class, "toString");
        MethodInvocation methodInvocation = mock(MethodInvocation.class);

        when(methodInvocation.getMethod())
            .thenReturn(method);
        when(methodInvocation.getArguments())
            .thenReturn(new Object[0]);
        when(methodInvocation.getThis())
            .thenReturn(new Object());

        return methodInvocation;
    }

    static class FirstContributor implements MethodSecurityExpressionContributor {

        public boolean isFirst() {
            return true;
        }

        public boolean isFirstWithId(long id) {
            return id == 7;
        }
    }

    static class SecondContributor implements MethodSecurityExpressionContributor {

        public boolean isFirst() {
            return false;
        }
    }

    static class TenantAdminContributor implements MethodSecurityExpressionContributor {

        public boolean isTenantAdmin() {
            return true;
        }
    }
}
