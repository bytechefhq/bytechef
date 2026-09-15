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

package com.bytechef.platform.configuration.web.graphql;

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
 * Evaluates the real {@code @PreAuthorize} expressions on the workflow test GraphQL mutations through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}, asserting each guard in
 * both directions and the exact check that reaches {@link PermissionService}: saving a test configuration connection,
 * saving a cluster element test output, and running a node or cluster element script. The two script mutations execute
 * code server-side with the workflow's connections.
 *
 * @author Ivica Cardic
 */
class WorkflowTestGraphQlControllerAuthorizationTest {

    private static final long CONNECTION_ID = 5L;
    private static final long ENVIRONMENT_ID = 2L;
    private static final List<Class<?>> GUARDED_CONTROLLER_CLASSES = List.of(
        WorkflowNodeScriptGraphQlController.class, WorkflowNodeTestOutputGraphQlController.class,
        WorkflowTestConfigurationGraphQlController.class);
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
        Set<String> guardedMethodNames = GUARDED_CONTROLLER_CLASSES.stream()
            .flatMap(controllerClass -> Arrays.stream(controllerClass.getDeclaredMethods()))
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .map(method -> method.getDeclaringClass()
                .getSimpleName() + "#" + method.getName())
            .collect(Collectors.toSet());

        Set<String> evaluatedMethodNames = guardCaseStream()
            .map(GuardCase::toString)
            .collect(Collectors.toSet());

        assertThat(evaluatedMethodNames).isEqualTo(guardedMethodNames);
    }

    @Test
    void testScriptInputReadsCarryNoGuard() throws NoSuchMethodException {
        Method clusterElementMethod = WorkflowNodeScriptGraphQlController.class.getDeclaredMethod(
            "clusterElementScriptInput", String.class, String.class, String.class, String.class, Long.class);
        Method workflowNodeMethod = WorkflowNodeScriptGraphQlController.class.getDeclaredMethod(
            "workflowNodeScriptInput", String.class, String.class, Long.class);

        assertThat(clusterElementMethod.getAnnotation(PreAuthorize.class)).isNull();
        assertThat(workflowNodeMethod.getAnnotation(PreAuthorize.class)).isNull();
    }

    static Stream<Arguments> guardCases() {
        return guardCaseStream()
            .flatMap(guardCase -> Stream.of(Arguments.of(guardCase, false), Arguments.of(guardCase, true)));
    }

    private static Stream<GuardCase> guardCaseStream() {
        return Stream.of(
            new GuardCase(
                WorkflowTestConfigurationGraphQlController.class, "saveClusterElementTestConfigurationConnection",
                Map.of("workflowId", WORKFLOW_ID, "connectionId", CONNECTION_ID, "environmentId", ENVIRONMENT_ID)),
            new GuardCase(
                WorkflowTestConfigurationGraphQlController.class, "saveWorkflowTestConfigurationConnection",
                Map.of("workflowId", WORKFLOW_ID, "connectionId", CONNECTION_ID, "environmentId", ENVIRONMENT_ID)),
            new GuardCase(
                WorkflowNodeTestOutputGraphQlController.class, "saveClusterElementTestOutput",
                Map.of("workflowId", WORKFLOW_ID, "environmentId", ENVIRONMENT_ID)),
            new GuardCase(
                WorkflowNodeScriptGraphQlController.class, "testClusterElementScript",
                Map.of("workflowId", WORKFLOW_ID, "environmentId", ENVIRONMENT_ID)),
            new GuardCase(
                WorkflowNodeScriptGraphQlController.class, "testWorkflowNodeScript",
                Map.of("workflowId", WORKFLOW_ID, "environmentId", ENVIRONMENT_ID)));
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, GuardCase guardCase) {
        Method method = findMethod(guardCase.controllerClass(), guardCase.methodName());

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

    private static Method findMethod(Class<?> controllerClass, String methodName) {
        List<Method> methods = Arrays.stream(controllerClass.getDeclaredMethods())
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

    private record GuardCase(Class<?> controllerClass, String methodName, Map<String, Object> argumentsByName) {

        Function<PermissionService, Boolean> expectedCheck() {
            return permissionService -> permissionService.hasWorkflowScopeIfProjectWorkflow(
                WORKFLOW_ID, "WORKFLOW_EDIT", PRODUCTION);
        }

        @Override
        public String toString() {
            return controllerClass.getSimpleName() + "#" + methodName;
        }
    }
}
