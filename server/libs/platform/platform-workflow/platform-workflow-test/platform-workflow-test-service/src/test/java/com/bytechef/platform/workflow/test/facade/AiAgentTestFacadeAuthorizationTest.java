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

package com.bytechef.platform.workflow.test.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Calls {@link AiAgentTestFacadeImpl#executeAiAgentAction} through Spring Security's real {@code @PreAuthorize}
 * interceptor, backed by the real {@link AutomationMethodSecurityExpressionHandler}. Testing an AI agent runs the
 * workflow's agent with its connections, so it needs WORKFLOW_EDIT in the environment the test runs in.
 *
 * @author Ivica Cardic
 */
class AiAgentTestFacadeAuthorizationTest {

    private static final String BODY_REACHED = "body reached";
    private static final String WORKFLOW_ID = "workflow-1";

    private final PermissionService permissionService = mock(PermissionService.class);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testExecuteAiAgentActionDeniesWhenTheWorkflowEditScopeInTheRequestedEnvironmentIsRefused() {
        AiAgentTestFacade aiAgentTestFacade = createSecuredFacade();

        assertThatThrownBy(() -> execute(aiAgentTestFacade, Environment.PRODUCTION.ordinal()))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkflowScopeIfProjectWorkflow(
            WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION);
    }

    @Test
    void testExecuteAiAgentActionAllowsWhenTheWorkflowEditScopeInTheRequestedEnvironmentIsGranted() {
        when(permissionService.hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .thenReturn(true);

        AiAgentTestFacade aiAgentTestFacade = createSecuredFacade();

        assertThatThrownBy(() -> execute(aiAgentTestFacade, Environment.PRODUCTION.ordinal()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    @Test
    void testExecuteAiAgentActionDeniesAnEnvironmentOutsideTheEnum() {
        when(permissionService.hasWorkflowScopeIfProjectWorkflow(anyString(), anyString(), any())).thenReturn(true);

        AiAgentTestFacade aiAgentTestFacade = createSecuredFacade();

        assertThatThrownBy(() -> execute(aiAgentTestFacade, Environment.values().length))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService, never()).hasWorkflowScopeIfProjectWorkflow(anyString(), anyString(), any());
    }

    private static Object execute(AiAgentTestFacade aiAgentTestFacade, long environmentId) {
        return aiAgentTestFacade.executeAiAgentAction(
            WORKFLOW_ID, "aiAgent_1", environmentId, "conversation-1", "hello", List.of());
    }

    private AiAgentTestFacade createSecuredFacade() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

        SecurityContextHolder.setContext(securityContext);

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        AiAgentTestFacadeImpl target = mock(
            AiAgentTestFacadeImpl.class, invocation -> {
                throw new IllegalStateException(BODY_REACHED);
            });

        ProxyFactory proxyFactory = new ProxyFactory(target);

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return (AiAgentTestFacade) proxyFactory.getProxy();
    }
}
