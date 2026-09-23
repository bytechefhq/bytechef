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

package com.bytechef.platform.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.web.rest.model.WorkflowTestConfigurationModel;
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
import org.junit.jupiter.api.BeforeAll;
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
 * Evaluates the real {@code @PreAuthorize} expression on every workflow test configuration mutation through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}, asserting each guard in
 * both directions and the exact check that reaches {@link PermissionService}. A test configuration picks the
 * connections a test run executes with, so changing it is editing the workflow.
 *
 * @author Ivica Cardic
 */
class WorkflowTestConfigurationApiControllerAuthorizationTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final Environment PRODUCTION = Environment.PRODUCTION;
    private static final String WORKFLOW_ID = "workflow-1";

    // The first guard evaluation in a test JVM loads, and under coverage instruments, the Spring Security and SpEL
    // class graph. That one-time cost belongs under the longer @BeforeAll limit, not the per-test one.
    @BeforeAll
    static void warmUpGuardEvaluation() {
        evaluateGuard(
            mock(PermissionService.class), guardCaseStream().findFirst()
                .orElseThrow());
    }

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
        Set<String> guardedMethodNames = Arrays
            .stream(WorkflowTestConfigurationApiController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .map(Method::getName)
            .collect(Collectors.toSet());

        Set<String> evaluatedMethodNames = guardCaseStream()
            .map(GuardCase::methodName)
            .collect(Collectors.toSet());

        assertThat(evaluatedMethodNames).isEqualTo(guardedMethodNames);
    }

    @Test
    void testTestConfigurationReadsCarryNoGuard() throws NoSuchMethodException {
        Method configurationMethod = WorkflowTestConfigurationApiController.class.getMethod(
            "getWorkflowTestConfiguration", String.class, Long.class);
        Method connectionsMethod = WorkflowTestConfigurationApiController.class.getMethod(
            "getWorkflowTestConfigurationConnections", String.class, String.class, Long.class);

        assertThat(configurationMethod.getAnnotation(PreAuthorize.class)).isNull();
        assertThat(connectionsMethod.getAnnotation(PreAuthorize.class)).isNull();
    }

    static Stream<Arguments> guardCases() {
        return guardCaseStream()
            .flatMap(guardCase -> Stream.of(Arguments.of(guardCase, false), Arguments.of(guardCase, true)));
    }

    private static Stream<GuardCase> guardCaseStream() {
        WorkflowTestConfigurationModel workflowTestConfigurationModel = new WorkflowTestConfigurationModel()
            .environmentId(ENVIRONMENT_ID);

        return Stream.of(
            new GuardCase(
                "saveWorkflowTestConfiguration",
                Map.of("workflowId", WORKFLOW_ID, "workflowTestConfigurationModel", workflowTestConfigurationModel),
                permissionService -> permissionService.hasWorkflowScopeIfProjectWorkflow(
                    WORKFLOW_ID, "WORKFLOW_EDIT", PRODUCTION)),
            // Without a body the environment is absent, and the check falls back to Development.
            new GuardCase(
                "saveWorkflowTestConfiguration", Map.of("workflowId", WORKFLOW_ID),
                permissionService -> permissionService.hasWorkflowScopeIfProjectWorkflow(
                    WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT)),
            new GuardCase(
                "deleteWorkflowTestConfigurationConnection",
                Map.of(
                    "workflowId", WORKFLOW_ID, "workflowNodeName", "node_1", "workflowConnectionKey", "key",
                    "environmentId", ENVIRONMENT_ID),
                permissionService -> permissionService.hasWorkflowScopeIfProjectWorkflow(
                    WORKFLOW_ID, "WORKFLOW_EDIT", PRODUCTION)),
            new GuardCase(
                "saveWorkflowTestConfigurationInputs",
                Map.of("workflowId", WORKFLOW_ID, "environmentId", ENVIRONMENT_ID),
                permissionService -> permissionService.hasWorkflowScopeIfProjectWorkflow(
                    WORKFLOW_ID, "WORKFLOW_EDIT", PRODUCTION)));
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
        List<Method> methods = Arrays.stream(WorkflowTestConfigurationApiController.class.getDeclaredMethods())
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
            return methodName + argumentsByName.keySet();
        }
    }
}
