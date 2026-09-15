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

package com.bytechef.platform.workflow.test.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on {@link WorkflowTestApiController} through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}, asserting each guard in
 * both directions and the exact check that reaches {@link PermissionService}. Starting a test executes the workflow's
 * components with real connections, so it needs the edit scope. Attaching to and stopping a test run name the
 * {@code TestJob} resource with the same scope: test jobs never reach the persistent job store, so a {@code Job} guard
 * could not find them and denied every non-admin.
 *
 * @author Ivica Cardic
 */
class WorkflowTestApiControllerAuthorizationTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final long JOB_ID = 7L;
    private static final Environment PRODUCTION = Environment.PRODUCTION;
    private static final String WORKFLOW_ID = "workflow-1";

    @ParameterizedTest(name = "{0} granted={1}")
    @MethodSource("guardCases")
    void testGuardDecidesOnTheExpectedPermissionCheck(GuardCase guardCase, boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);
        Function<PermissionService, Boolean> expectedCheck = guardCase.expectedCheck();

        when(expectedCheck.apply(permissionService)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, guardCase))
            .as("%s must %s when its permission check returns %s", guardCase, granted ? "allow" : "deny", granted)
            .isEqualTo(granted);

        expectedCheck.apply(verify(permissionService));
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testEveryGuardedMethodHasAnEvaluatedCase() {
        Set<String> guardedMethodNames = Arrays.stream(WorkflowTestApiController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .map(Method::getName)
            .collect(Collectors.toSet());

        Set<String> evaluatedMethodNames = guardCaseStream()
            .map(GuardCase::methodName)
            .collect(Collectors.toSet());

        assertThat(evaluatedMethodNames).isEqualTo(guardedMethodNames);
    }

    static Stream<Arguments> guardCases() {
        return guardCaseStream()
            .flatMap(guardCase -> Stream.of(Arguments.of(guardCase, false), Arguments.of(guardCase, true)));
    }

    private static Stream<GuardCase> guardCaseStream() {
        return Stream.of(
            new GuardCase(
                "startWorkflowTest", Map.of("id", WORKFLOW_ID, "environmentId", ENVIRONMENT_ID),
                permissionService -> permissionService.hasWorkflowScopeIfProjectWorkflow(
                    WORKFLOW_ID, "WORKFLOW_EDIT", PRODUCTION)),
            new GuardCase(
                "attachWorkflowTest", Map.of("jobId", JOB_ID),
                permissionService -> permissionService.hasResourceScope(JOB_ID, "TestJob", "WORKFLOW_EDIT")),
            new GuardCase(
                "stopWorkflowTest", Map.of("jobId", String.valueOf(JOB_ID)),
                permissionService -> permissionService.hasResourceScope(JOB_ID, "TestJob", "WORKFLOW_EDIT")),
            // A malformed id reaches the check as the -1 fallback rather than as the raw string.
            new GuardCase(
                "stopWorkflowTest", Map.of("jobId", "not-a-number"),
                permissionService -> permissionService.hasResourceScope(-1L, "TestJob", "WORKFLOW_EDIT")));
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, GuardCase guardCase) {
        Method method = findMethod(guardCase.methodName());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, toArguments(method, guardCase.argumentsByName()));

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method findMethod(String methodName) {
        List<Method> methods = Arrays.stream(WorkflowTestApiController.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic() && Modifier.isPublic(method.getModifiers()))
            .filter(method -> methodName.equals(method.getName()))
            .toList();

        assertThat(methods)
            .as("Expected exactly one public '%s' method, since a proxy only enforces a public guard", methodName)
            .hasSize(1);

        return methods.getFirst();
    }

    private static Object[] toArguments(Method method, Map<String, Object> argumentsByName) {
        Parameter[] parameters = method.getParameters();

        List<String> parameterNames = Arrays.stream(parameters)
            .map(Parameter::getName)
            .toList();

        assertThat(parameterNames)
            .as("%s must declare every parameter its guard is evaluated with", method.getName())
            .containsAll(argumentsByName.keySet());

        Object[] arguments = new Object[parameters.length];

        for (int index = 0; index < parameters.length; index++) {
            arguments[index] = argumentsByName.get(parameterNames.get(index));
        }

        return arguments;
    }

    private record GuardCase(
        String methodName, Map<String, Object> argumentsByName, Function<PermissionService, Boolean> expectedCheck) {

        @Override
        public String toString() {
            return methodName + argumentsByName;
        }
    }
}
