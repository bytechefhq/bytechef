/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.eval.web.graphql;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expression on every AI agent eval GraphQL mutation through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}, asserting each guard in
 * both directions and the exact check that reaches {@link PermissionService}. Evals belong to an AI Agent node of a
 * workflow and a run executes the agent's model with the workflow's test connections, so every change needs the
 * workflow edit scope, keyed on the workflow the eval belongs to. A stand-in for {@code aiAgentEvalWorkflowResolver}
 * answers only for the resource type and id the guard is expected to ask about, so a guard naming the wrong one checks
 * a workflow the permission mock does not know.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiAgentEvalGraphQlControllerAuthorizationTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final long RESOURCE_ID = 21L;
    private static final String UNKNOWN_WORKFLOW_ID = "unknown-workflow";
    private static final String WORKFLOW_ID = "workflow-1";

    @ParameterizedTest(name = "{0} granted={1}")
    @MethodSource("guardCases")
    void testGuardDecidesOnTheExpectedPermissionCheck(GuardCase guardCase, boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasWorkflowScopeIfProjectWorkflow(
            WORKFLOW_ID, "WORKFLOW_EDIT", guardCase.expectedEnvironment())).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, guardCase))
            .as("%s must %s when its permission check returns %s", guardCase, granted ? "allow" : "deny", granted)
            .isEqualTo(granted);

        verify(permissionService).hasWorkflowScopeIfProjectWorkflow(
            WORKFLOW_ID, "WORKFLOW_EDIT", guardCase.expectedEnvironment());
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testEveryMutationHasAnEvaluatedCase() {
        Set<String> guardedMethodNames = Arrays.stream(AiAgentEvalGraphQlController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(MutationMapping.class) ||
                method.isAnnotationPresent(PreAuthorize.class))
            .map(Method::getName)
            .collect(Collectors.toSet());

        Set<String> evaluatedMethodNames = guardCaseStream()
            .map(GuardCase::methodName)
            .collect(Collectors.toSet());

        assertThat(guardedMethodNames).hasSize(17);
        assertThat(evaluatedMethodNames).isEqualTo(guardedMethodNames);
    }

    @Test
    void testReadsCarryNoGuard() {
        List<Method> queryMethods = Arrays.stream(AiAgentEvalGraphQlController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(QueryMapping.class))
            .toList();

        assertThat(queryMethods).isNotEmpty();

        for (Method method : queryMethods) {
            assertThat(method.getAnnotation(PreAuthorize.class))
                .as("%s", method.getName())
                .isNull();
        }
    }

    static Stream<Arguments> guardCases() {
        return guardCaseStream()
            .flatMap(guardCase -> Stream.of(Arguments.of(guardCase, false), Arguments.of(guardCase, true)));
    }

    private static Stream<GuardCase> guardCaseStream() {
        return Stream.of(
            directCase("createAiAgentJudge"),
            resolvedCase("updateAiAgentJudge", "id", "AiAgentJudge"),
            resolvedCase("deleteAiAgentJudge", "id", "AiAgentJudge"),
            directCase("createAiAgentEvalTest"),
            resolvedCase("updateAiAgentEvalTest", "id", "AiAgentEvalTest"),
            resolvedCase("deleteAiAgentEvalTest", "id", "AiAgentEvalTest"),
            resolvedCase("createAiAgentEvalScenario", "agentEvalTestId", "AiAgentEvalTest"),
            resolvedCase("updateAiAgentEvalScenario", "id", "AiAgentEvalScenario"),
            resolvedCase("deleteAiAgentEvalScenario", "id", "AiAgentEvalScenario"),
            resolvedCase("createAiAgentScenarioJudge", "agentEvalScenarioId", "AiAgentEvalScenario"),
            resolvedCase("updateAiAgentScenarioJudge", "id", "AiAgentScenarioJudge"),
            resolvedCase("deleteAiAgentScenarioJudge", "id", "AiAgentScenarioJudge"),
            resolvedCase("createAiAgentScenarioToolSimulation", "agentEvalScenarioId", "AiAgentEvalScenario"),
            resolvedCase("updateAiAgentScenarioToolSimulation", "id", "AiAgentScenarioToolSimulation"),
            resolvedCase("deleteAiAgentScenarioToolSimulation", "id", "AiAgentScenarioToolSimulation"),
            // A run executes in the environment the caller names, not in Development.
            new GuardCase(
                "startAiAgentEvalRun",
                Map.of("agentEvalTestId", RESOURCE_ID, "name", "run", "environmentId", ENVIRONMENT_ID),
                "AiAgentEvalTest", Environment.PRODUCTION),
            // Cancelling a run checks the environment the run was started in, read off the run itself.
            new GuardCase("cancelAiAgentEvalRun", Map.of("id", RESOURCE_ID), "AiAgentEvalRun", Environment.PRODUCTION));
    }

    private static GuardCase directCase(String methodName) {
        return new GuardCase(
            methodName, Map.of("workflowId", WORKFLOW_ID, "workflowNodeName", "agent_1", "name", "judge"), null,
            Environment.DEVELOPMENT);
    }

    private static GuardCase resolvedCase(String methodName, String idParameterName, String resourceType) {
        return new GuardCase(methodName, Map.of(idParameterName, RESOURCE_ID), resourceType, Environment.DEVELOPMENT);
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

        StandardEvaluationContext evaluationContext = (StandardEvaluationContext) expressionHandler
            .createEvaluationContext(() -> authentication, methodInvocation);

        WorkflowResolverStandIn workflowResolver = new WorkflowResolverStandIn(guardCase.resolvedResourceType());

        evaluationContext.setBeanResolver((EvaluationContext context, String beanName) -> {
            if (!"aiAgentEvalWorkflowResolver".equals(beanName)) {
                throw new AccessException("Unknown bean " + beanName);
            }

            return workflowResolver;
        });

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method findMethod(String methodName) {
        List<Method> methods = Arrays.stream(AiAgentEvalGraphQlController.class.getDeclaredMethods())
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
        String methodName, Map<String, Object> argumentsByName, @Nullable String resolvedResourceType,
        Environment expectedEnvironment) {

        @Override
        public String toString() {
            return methodName;
        }
    }

    /**
     * Answers like {@code AiAgentEvalWorkflowResolver}, but only for the expected resource type and id.
     */
    public static final class WorkflowResolverStandIn {

        private final @Nullable String expectedResourceType;

        WorkflowResolverStandIn(@Nullable String expectedResourceType) {
            this.expectedResourceType = expectedResourceType;
        }

        public String getWorkflowId(String resourceType, long id) {
            return isExpected(resourceType, id) ? WORKFLOW_ID : UNKNOWN_WORKFLOW_ID;
        }

        public @Nullable Long getEnvironmentId(String resourceType, long id) {
            return isExpected(resourceType, id) ? ENVIRONMENT_ID : null;
        }

        private boolean isExpected(String resourceType, long id) {
            return resourceType.equals(expectedResourceType) && id == RESOURCE_ID;
        }
    }
}
