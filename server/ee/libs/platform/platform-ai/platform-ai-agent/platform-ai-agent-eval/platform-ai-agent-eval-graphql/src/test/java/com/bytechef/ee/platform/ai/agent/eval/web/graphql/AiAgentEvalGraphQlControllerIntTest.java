/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.eval.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.platform.ai.agent.eval.facade.AiAgentEvalRunFacade;
import com.bytechef.ee.platform.ai.agent.eval.file.storage.AiAgentEvalFileStorage;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalResultService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalRunService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalScenarioService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentEvalTestService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentJudgeService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentJudgeVerdictService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentScenarioJudgeService;
import com.bytechef.ee.platform.ai.agent.eval.service.AiAgentScenarioToolSimulationService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.execution.SecurityDataFetcherExceptionResolver;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

/**
 * Executes every AI agent eval GraphQL operation through the real schema with Spring Security's method interceptor
 * enforcing {@link AiAgentEvalGraphQlController}'s real {@code @PreAuthorize} guards through the production
 * {@link AutomationMethodSecurityConfiguration}, asserting each mutation guard in both directions and the exact check
 * that reaches {@link PermissionService}. Evals belong to an AI Agent node of a workflow and a run executes the agent's
 * model with the workflow's test connections, so every change needs the workflow edit scope, keyed on the workflow the
 * eval belongs to. The {@code aiAgentEvalWorkflowResolver} bean is a stand-in that answers only for the resource type
 * and id the guard is expected to ask about, so a guard naming the wrong one checks a workflow the permission mock does
 * not know. Every collaborator throws on any call, so an allowed operation proves it entered the controller method.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AiAgentEvalGraphQlControllerIntTest.Config.class)
@GraphQlTest(
    controllers = AiAgentEvalGraphQlController.class,
    properties = "spring.graphql.schema.locations=classpath*:/graphql/")
class AiAgentEvalGraphQlControllerIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long ENVIRONMENT_ID = 2L;
    private static final long RESOURCE_ID = 21L;
    private static final String UNKNOWN_WORKFLOW_ID = "unknown-workflow";
    private static final String WORKFLOW_ID = "workflow-1";

    private static final Map<String, String> QUERY_DOCUMENTS = Map.of(
        "aiAgentEvalResult", "query { aiAgentEvalResult(id: \"21\") { id } }",
        "aiAgentEvalResultTranscript", "query { aiAgentEvalResultTranscript(id: \"21\") }",
        "aiAgentEvalRun", "query { aiAgentEvalRun(id: \"21\") { id } }",
        "aiAgentEvalRuns", "query { aiAgentEvalRuns(agentEvalTestId: \"21\") { id } }",
        "aiAgentEvalTest", "query { aiAgentEvalTest(id: \"21\") { id } }",
        "aiAgentEvalTests",
        "query { aiAgentEvalTests(workflowId: \"workflow-1\", workflowNodeName: \"agent_1\") { id } }",
        "aiAgentJudges", "query { aiAgentJudges(workflowId: \"workflow-1\", workflowNodeName: \"agent_1\") { id } }");

    @Autowired
    private List<Object> collaborators;

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkflowResolverStandIn workflowResolverStandIn;

    static Stream<Arguments> guardCases() {
        return guardCaseStream()
            .flatMap(guardCase -> Stream.of(Arguments.of(guardCase, false), Arguments.of(guardCase, true)));
    }

    @BeforeEach
    void beforeEach() {
        reset(permissionService);

        clearInvocations(collaborators.toArray());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "{0} granted={1}")
    @MethodSource("guardCases")
    void testGuardDecidesOnTheExpectedPermissionCheck(GuardCase guardCase, boolean granted) {
        workflowResolverStandIn.setExpectedResourceType(guardCase.resolvedResourceType());

        when(permissionService.hasWorkflowScopeIfProjectWorkflow(
            WORKFLOW_ID, "WORKFLOW_EDIT", guardCase.expectedEnvironment())).thenReturn(granted);

        List<ResponseError> responseErrors = execute(guardCase.document());

        if (granted) {
            assertThat(responseErrors)
                .as("%s must allow when its permission check grants", guardCase)
                .singleElement()
                .extracting(ResponseError::getErrorType)
                .isEqualTo(ErrorType.INTERNAL_ERROR);

            assertThat(countCollaboratorInvocations())
                .as("%s must reach its controller method", guardCase)
                .isPositive();
        } else {
            assertThat(responseErrors)
                .as("%s must deny when its permission check refuses", guardCase)
                .singleElement()
                .extracting(ResponseError::getErrorType)
                .isEqualTo(ErrorType.FORBIDDEN);

            verifyNoInteractions(collaborators.toArray());
        }

        verify(permissionService).hasWorkflowScopeIfProjectWorkflow(
            WORKFLOW_ID, "WORKFLOW_EDIT", guardCase.expectedEnvironment());
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testEveryMutationHasAnEvaluatedCase() {
        Set<String> mutationNames = mappedMethodNames(MutationMapping.class);

        Set<String> evaluatedMethodNames = guardCaseStream()
            .map(GuardCase::methodName)
            .collect(Collectors.toSet());

        assertThat(mutationNames).hasSize(17);
        assertThat(evaluatedMethodNames).isEqualTo(mutationNames);
    }

    @Test
    void testReadsCarryNoGuard() {
        assertThat(mappedMethodNames(QueryMapping.class)).isEqualTo(QUERY_DOCUMENTS.keySet());

        for (Map.Entry<String, String> entry : QUERY_DOCUMENTS.entrySet()) {
            clearInvocations(collaborators.toArray());

            assertThat(execute(entry.getValue()))
                .as("%s must be readable without any permission", entry.getKey())
                .singleElement()
                .extracting(ResponseError::getErrorType)
                .isEqualTo(ErrorType.INTERNAL_ERROR);

            assertThat(countCollaboratorInvocations())
                .as("%s must reach its controller method", entry.getKey())
                .isPositive();
        }

        verifyNoInteractions(permissionService);
    }

    private int countCollaboratorInvocations() {
        return collaborators.stream()
            .mapToInt(collaborator -> mockingDetails(collaborator).getInvocations()
                .size())
            .sum();
    }

    private List<ResponseError> execute(String document) {
        return graphQlTester.document(document)
            .execute()
            .returnResponse()
            .getErrors();
    }

    private static Set<String> mappedMethodNames(Class<? extends java.lang.annotation.Annotation> annotationType) {
        return Arrays.stream(AiAgentEvalGraphQlController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(annotationType))
            .map(Method::getName)
            .collect(Collectors.toSet());
    }

    private static Stream<GuardCase> guardCaseStream() {
        return Stream.of(
            directCase(
                "createAiAgentJudge",
                "mutation { createAiAgentJudge(workflowId: \"workflow-1\", workflowNodeName: \"agent_1\", " +
                    "name: \"judge\", type: LLM_RULE, configuration: {}) { id } }"),
            resolvedCase(
                "updateAiAgentJudge", "mutation { updateAiAgentJudge(id: \"21\") { id } }", "AiAgentJudge"),
            resolvedCase("deleteAiAgentJudge", "mutation { deleteAiAgentJudge(id: \"21\") }", "AiAgentJudge"),
            directCase(
                "createAiAgentEvalTest",
                "mutation { createAiAgentEvalTest(workflowId: \"workflow-1\", workflowNodeName: \"agent_1\", " +
                    "name: \"test\") { id } }"),
            resolvedCase(
                "updateAiAgentEvalTest", "mutation { updateAiAgentEvalTest(id: \"21\") { id } }", "AiAgentEvalTest"),
            resolvedCase("deleteAiAgentEvalTest", "mutation { deleteAiAgentEvalTest(id: \"21\") }", "AiAgentEvalTest"),
            resolvedCase(
                "createAiAgentEvalScenario",
                "mutation { createAiAgentEvalScenario(agentEvalTestId: \"21\", name: \"scenario\", " +
                    "type: SINGLE_TURN) { id } }",
                "AiAgentEvalTest"),
            resolvedCase(
                "updateAiAgentEvalScenario", "mutation { updateAiAgentEvalScenario(id: \"21\") { id } }",
                "AiAgentEvalScenario"),
            resolvedCase(
                "deleteAiAgentEvalScenario", "mutation { deleteAiAgentEvalScenario(id: \"21\") }",
                "AiAgentEvalScenario"),
            resolvedCase(
                "createAiAgentScenarioJudge",
                "mutation { createAiAgentScenarioJudge(agentEvalScenarioId: \"21\", name: \"judge\", " +
                    "type: LLM_RULE, configuration: {}) { id } }",
                "AiAgentEvalScenario"),
            resolvedCase(
                "updateAiAgentScenarioJudge", "mutation { updateAiAgentScenarioJudge(id: \"21\") { id } }",
                "AiAgentScenarioJudge"),
            resolvedCase(
                "deleteAiAgentScenarioJudge", "mutation { deleteAiAgentScenarioJudge(id: \"21\") }",
                "AiAgentScenarioJudge"),
            resolvedCase(
                "createAiAgentScenarioToolSimulation",
                "mutation { createAiAgentScenarioToolSimulation(agentEvalScenarioId: \"21\", toolName: \"tool\", " +
                    "responsePrompt: \"prompt\") { id } }",
                "AiAgentEvalScenario"),
            resolvedCase(
                "updateAiAgentScenarioToolSimulation",
                "mutation { updateAiAgentScenarioToolSimulation(id: \"21\") { id } }",
                "AiAgentScenarioToolSimulation"),
            resolvedCase(
                "deleteAiAgentScenarioToolSimulation",
                "mutation { deleteAiAgentScenarioToolSimulation(id: \"21\") }", "AiAgentScenarioToolSimulation"),
            new GuardCase(
                "startAiAgentEvalRun",
                "mutation { startAiAgentEvalRun(agentEvalTestId: \"21\", name: \"run\", environmentId: \"2\") " +
                    "{ id } }",
                "AiAgentEvalTest", Environment.PRODUCTION),
            new GuardCase(
                "cancelAiAgentEvalRun", "mutation { cancelAiAgentEvalRun(id: \"21\") { id } }", "AiAgentEvalRun",
                Environment.PRODUCTION));
    }

    private static GuardCase directCase(String methodName, String document) {
        return new GuardCase(methodName, document, null, Environment.DEVELOPMENT);
    }

    private static GuardCase resolvedCase(String methodName, String document, String resourceType) {
        return new GuardCase(methodName, document, resourceType, Environment.DEVELOPMENT);
    }

    private record GuardCase(
        String methodName, String document, @Nullable String resolvedResourceType, Environment expectedEnvironment) {

        @Override
        public String toString() {
            return methodName;
        }
    }

    /**
     * Answers like {@code AiAgentEvalWorkflowResolver}, but only for the expected resource type and id.
     */
    public static final class WorkflowResolverStandIn {

        private @Nullable String expectedResourceType;

        public String getWorkflowId(String resourceType, long id) {
            return isExpected(resourceType, id) ? WORKFLOW_ID : UNKNOWN_WORKFLOW_ID;
        }

        public @Nullable Long getEnvironmentId(String resourceType, long id) {
            return isExpected(resourceType, id) ? ENVIRONMENT_ID : null;
        }

        void setExpectedResourceType(@Nullable String expectedResourceType) {
            this.expectedResourceType = expectedResourceType;
        }

        private boolean isExpected(String resourceType, long id) {
            return resourceType.equals(expectedResourceType) && id == RESOURCE_ID;
        }
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration({
        AopAutoConfiguration.class, AutomationMethodSecurityConfiguration.class
    })
    @Import(AiAgentEvalGraphQlController.class)
    static class Config {

        @Bean
        AiAgentEvalFileStorage aiAgentEvalFileStorage() {
            return mock(AiAgentEvalFileStorage.class, bodyReachedAnswer());
        }

        @Bean
        AiAgentEvalResultService aiAgentEvalResultService() {
            return mock(AiAgentEvalResultService.class, bodyReachedAnswer());
        }

        @Bean
        AiAgentEvalRunFacade aiAgentEvalRunFacade() {
            return mock(AiAgentEvalRunFacade.class, bodyReachedAnswer());
        }

        @Bean
        AiAgentEvalRunService aiAgentEvalRunService() {
            return mock(AiAgentEvalRunService.class, bodyReachedAnswer());
        }

        @Bean
        AiAgentEvalScenarioService aiAgentEvalScenarioService() {
            return mock(AiAgentEvalScenarioService.class, bodyReachedAnswer());
        }

        @Bean
        AiAgentEvalTestService aiAgentEvalTestService() {
            return mock(AiAgentEvalTestService.class, bodyReachedAnswer());
        }

        @Bean
        WorkflowResolverStandIn aiAgentEvalWorkflowResolver() {
            return new WorkflowResolverStandIn();
        }

        @Bean
        AiAgentJudgeService aiAgentJudgeService() {
            return mock(AiAgentJudgeService.class, bodyReachedAnswer());
        }

        @Bean
        AiAgentJudgeVerdictService aiAgentJudgeVerdictService() {
            return mock(AiAgentJudgeVerdictService.class, bodyReachedAnswer());
        }

        @Bean
        AiAgentScenarioJudgeService aiAgentScenarioJudgeService() {
            return mock(AiAgentScenarioJudgeService.class, bodyReachedAnswer());
        }

        @Bean
        AiAgentScenarioToolSimulationService aiAgentScenarioToolSimulationService() {
            return mock(AiAgentScenarioToolSimulationService.class, bodyReachedAnswer());
        }

        @Bean
        List<Object> collaborators(
            AiAgentEvalFileStorage aiAgentEvalFileStorage, AiAgentEvalResultService aiAgentEvalResultService,
            AiAgentEvalRunFacade aiAgentEvalRunFacade, AiAgentEvalRunService aiAgentEvalRunService,
            AiAgentEvalScenarioService aiAgentEvalScenarioService, AiAgentEvalTestService aiAgentEvalTestService,
            AiAgentJudgeService aiAgentJudgeService, AiAgentJudgeVerdictService aiAgentJudgeVerdictService,
            AiAgentScenarioJudgeService aiAgentScenarioJudgeService,
            AiAgentScenarioToolSimulationService aiAgentScenarioToolSimulationService) {

            return List.of(
                aiAgentEvalFileStorage, aiAgentEvalResultService, aiAgentEvalRunFacade, aiAgentEvalRunService,
                aiAgentEvalScenarioService, aiAgentEvalTestService, aiAgentJudgeService, aiAgentJudgeVerdictService,
                aiAgentScenarioJudgeService, aiAgentScenarioToolSimulationService);
        }

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        RuntimeWiringConfigurer scalarRuntimeWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar())
                .scalar(GraphQLScalarTypes.mapScalar());
        }

        @Bean
        SecurityDataFetcherExceptionResolver securityDataFetcherExceptionResolver() {
            return new SecurityDataFetcherExceptionResolver();
        }

        private static Answer<Object> bodyReachedAnswer() {
            return invocation -> {
                if (invocation.getMethod()
                    .getDeclaringClass() == Object.class) {

                    return Answers.RETURNS_DEFAULTS.answer(invocation);
                }

                throw new IllegalStateException(BODY_REACHED);
            };
        }
    }
}
