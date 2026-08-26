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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.workflow.test.facade.AiAgentTestFacade;
import com.bytechef.platform.workflow.test.web.rest.WorkflowScopeGateTestSupport.GateExpressionHandler;
import com.bytechef.platform.workflow.test.web.rest.WorkflowScopeGateTestSupport.GateRecorder;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Behavioral and reflection coverage for the {@code @PreAuthorize} gate closing the second half of the ticket-1051
 * hole: {@link AiAgentTestApiController#testAiAgent} was found ungated in review of the first fix (which only closed
 * {@link WorkflowTestApiController}) — any authenticated principal in the tenant could execute an AI agent node of any
 * workflow, using that workflow's stored credentials and tool access, and pick its own {@code environmentId}.
 *
 * <p>
 * Gated the same way as {@code WorkflowTestApiController#startWorkflowTest} —
 * {@code hasWorkflowScopeInEnvironment(#aiAgentTestRequest
 * .workflowId(), 'Workflow', 'WORKFLOW_EDIT')} — since this is the same operation (running a workflow node's code with
 * its stored credentials) reached through a different request shape (a JSON body instead of a path variable).
 *
 * <p>
 * {@code stopAiAgentTest} is intentionally NOT gated: its {@code testId} is a server-minted {@code UUID.randomUUID()}
 * with no owning workflow/workspace recorded anywhere resolvable, so there is nothing to key a permission check on. See
 * the Javadoc on {@link AiAgentTestApiController#stopAiAgentTest} and the named opt-out in
 * {@link WorkflowTestModuleAuthorizationCoverageTest} for the full rationale.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiAgentTestApiControllerAuthorizationTest.Config.class)
class AiAgentTestApiControllerAuthorizationTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    private static final String WORKFLOW_ID = "workflow-under-test";

    @Autowired
    private AiAgentTestApiController controller;

    @Autowired
    private GateRecorder gateRecorder;

    @BeforeEach
    void setUp() {
        gateRecorder.reset();

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testTestAiAgentDeniesCallerWithoutWorkflowEditScope() {
        gateRecorder.permit(false);

        assertThatThrownBy(() -> controller.testAiAgent(aiAgentTestRequest()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testTestAiAgentPermitsCallerWithWorkflowEditScope() {
        gateRecorder.permit(true);

        assertThatCode(() -> controller.testAiAgent(aiAgentTestRequest())).doesNotThrowAnyException();
    }

    @Test
    void testTestAiAgentExpressionRequiresWorkflowEdit() {
        Method match = null;

        for (Method candidate : AiAgentTestApiController.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals("testAiAgent") && candidate.isAnnotationPresent(PreAuthorize.class)) {

                match = candidate;

                break;
            }
        }

        assertThat(match)
            .as("@PreAuthorize-annotated method testAiAgent")
            .isNotNull();
        assertThat(match.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo("hasWorkflowScopeInEnvironment(#aiAgentTestRequest.workflowId(), 'WORKFLOW_EDIT', "
                + "#aiAgentTestRequest.environmentId())");
    }

    /**
     * The point of the gate: the environment it authorises must be the one the request will actually run in. Gating the
     * workflow alone unions the environments the caller can reach, which is what let a member who is editor in
     * Development run an agent node against Production by changing this field alone.
     */
    @Test
    void testTestAiAgentGatesTheEnvironmentIdFromTheRequestBody() {
        gateRecorder.permit(true);

        controller.testAiAgent(aiAgentTestRequest(2L));

        assertThat(gateRecorder.getCallCount()).isOne();
        assertThat(gateRecorder.getWorkflowId()).isEqualTo(WORKFLOW_ID);
        assertThat(gateRecorder.getScope()).isEqualTo("WORKFLOW_EDIT");
        assertThat(gateRecorder.getEnvironmentId()).isEqualTo(2L);
    }

    @Test
    void testTestAiAgentGatesEachRequestsOwnEnvironmentId() {
        gateRecorder.permit(true);

        controller.testAiAgent(aiAgentTestRequest(0L));

        assertThat(gateRecorder.getEnvironmentId()).isZero();

        controller.testAiAgent(aiAgentTestRequest(2L));

        assertThat(gateRecorder.getEnvironmentId()).isEqualTo(2L);
    }

    // -- Ticket 1051: the environment the agent actually RUNS in ---------------------------------------------------

    /**
     * Gating one environment and executing in another is the bug, so the environment reaching the facade must be the
     * effective one, not the request body's. An embedded connected user is confined to PRODUCTION while the embedded
     * client's default body carries {@code 0} (DEVELOPMENT) -- the exact pairing that produced the 403 when this was
     * implemented as a validation instead.
     *
     * <p>
     * The executor here runs the body synchronously but CLEARS the SecurityContext first, standing in for the worker
     * thread the context does not travel to. So this also pins that the resolution happens on the request thread: move
     * it inside the {@code runAsync} body and the context is empty there, the body's 0 wins, and this fails.
     */
    @Test
    void testTestAiAgentExecutesInTheConfinedPrincipalsEnvironment() {
        AiAgentTestFacade aiAgentTestFacade = mock(AiAgentTestFacade.class);

        AiAgentTestApiController unproxiedController = new AiAgentTestApiController(
            aiAgentTestFacade, workerThreadExecutor());

        SecurityContextHolder.getContext()
            .setAuthentication(
                new TestApiKeyAuthenticationToken(
                    PRODUCTION_ORDINAL, new User("connected-user-1", "", List.of())));

        unproxiedController.testAiAgent(aiAgentTestRequest(DEVELOPMENT_ORDINAL));

        verify(aiAgentTestFacade).executeAiAgentAction(
            eq(WORKFLOW_ID), eq("aiAgent_1"), eq(PRODUCTION_ORDINAL), eq("conversation-1"), eq("hello"), any());
    }

    /**
     * Containment: a session principal is not confined to an environment, so its request body is honoured unchanged.
     */
    @Test
    void testTestAiAgentExecutesInTheRequestedEnvironmentForASessionPrincipal() {
        AiAgentTestFacade aiAgentTestFacade = mock(AiAgentTestFacade.class);

        AiAgentTestApiController unproxiedController = new AiAgentTestApiController(
            aiAgentTestFacade, workerThreadExecutor());

        unproxiedController.testAiAgent(aiAgentTestRequest(DEVELOPMENT_ORDINAL));

        verify(aiAgentTestFacade).executeAiAgentAction(
            eq(WORKFLOW_ID), eq("aiAgent_1"), eq(DEVELOPMENT_ORDINAL), eq("conversation-1"), eq("hello"), any());
    }

    /**
     * Synchronous, so the assertions are deterministic without waiting on anything, but with the SecurityContext
     * cleared so it stands in for a real worker thread rather than quietly keeping the request's context alive.
     */
    private static TaskExecutor workerThreadExecutor() {
        return runnable -> {
            SecurityContextHolder.clearContext();

            runnable.run();
        };
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }

    private static AiAgentTestApiController.AiAgentTestRequest aiAgentTestRequest() {
        return new AiAgentTestApiController.AiAgentTestRequest(
            WORKFLOW_ID, "aiAgent_1", 1L, "conversation-1", "hello", List.of());
    }

    private static AiAgentTestApiController.AiAgentTestRequest aiAgentTestRequest(long environmentId) {
        return new AiAgentTestApiController.AiAgentTestRequest(
            WORKFLOW_ID, "aiAgent_1", environmentId, "conversation-1", "hello", List.of());
    }

    @SpringBootConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class Config {

        @Bean
        AiAgentTestApiController aiAgentTestApiController(
            AiAgentTestFacade aiAgentTestFacade,
            TaskExecutor taskExecutor) {

            return new AiAgentTestApiController(aiAgentTestFacade, taskExecutor);
        }

        @Bean
        AiAgentTestFacade aiAgentTestFacade() {
            return mock(AiAgentTestFacade.class);
        }

        @Bean
        TaskExecutor taskExecutor() {
            return mock(TaskExecutor.class);
        }

        @Bean
        PermissionEvaluator permissionEvaluator() {
            return mock(PermissionEvaluator.class);
        }

        @Bean
        GateRecorder gateRecorder() {
            return new GateRecorder();
        }

        @Bean
        MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            PermissionEvaluator permissionEvaluator, GateRecorder gateRecorder) {

            GateExpressionHandler handler = new GateExpressionHandler(gateRecorder);

            handler.setPermissionEvaluator(permissionEvaluator);

            return handler;
        }
    }
}
