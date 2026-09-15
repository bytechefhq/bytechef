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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.test.web.rest.AiAgentTestApiController.AiAgentTestRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expression on {@link AiAgentTestApiController#testAiAgent} through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. Testing an AI Agent runs
 * the workflow's AI Agent node with the workflow's test connections, so it needs the same edit scope in the requested
 * environment as starting a workflow test.
 *
 * @author Ivica Cardic
 */
class AiAgentTestApiControllerTest {

    private static final String WORKFLOW_ID = "workflow-1";

    @ParameterizedTest(name = "granted={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testTestAiAgentRequiresWorkflowEditInTheRequestedEnvironment(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .thenReturn(granted);

        assertThat(evaluateGuard(permissionService, createRequest(Environment.PRODUCTION.ordinal())))
            .as("testAiAgent must %s when WORKFLOW_EDIT in PRODUCTION returns %s", granted ? "allow" : "deny",
                granted)
            .isEqualTo(granted);

        verify(permissionService).hasWorkflowScopeIfProjectWorkflow(
            WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testTestAiAgentDeniesAnEnvironmentOutsideTheEnum() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        assertThat(evaluateGuard(permissionService, createRequest(Environment.values().length)))
            .as("testAiAgent must deny an environment id that names no environment")
            .isFalse();

        verify(permissionService, never()).hasWorkflowScopeIfProjectWorkflow(anyString(), anyString(), any());
    }

    private static AiAgentTestRequest createRequest(long environmentId) {
        return new AiAgentTestRequest(WORKFLOW_ID, "aiAgent_1", environmentId, "conversation-1", "hello", null);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, AiAgentTestRequest aiAgentTestRequest)
        throws NoSuchMethodException {

        Method method = AiAgentTestApiController.class.getMethod("testAiAgent", AiAgentTestRequest.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("testAiAgent must carry a @PreAuthorize guard")
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, aiAgentTestRequest);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }
}
