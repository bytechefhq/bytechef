/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.constant.PlatformType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Behavioural and reflection coverage for the {@code @PreAuthorize} gates closing the missing-authorization hole on
 * {@link WebhookTriggerTestApiController}. Both endpoints previously carried no authorization at all, so any
 * authenticated principal in the tenant could mint a live webhook URL for any workflow (running
 * {@code executeWebhookEnable} against that workflow's test-configuration connection, whose callback then writes a test
 * output), or tear one down under somebody else.
 *
 * <p>
 * The denial tests prove the gate is reached before the shared {@code WebhookTriggerTestFacade} is touched at all. The
 * permit tests prove it is not a gate that denies everybody: with the check satisfied the endpoints run to completion
 * and reach the facade with the workflow id and the resolved environment.
 *
 * <p>
 * {@code isTenantAdmin()} is a SpEL function of {@code AutomationMethodSecurityExpressionRoot}, which lives in
 * {@code automation-configuration-service}; this is a REST module and must not depend on it, so the function is
 * re-declared here. What is pinned is therefore the wiring -- that the expression parses, resolves to a function of
 * this name and arity, and that a false answer denies before any work happens -- while the function's own semantics are
 * pinned beside {@code PermissionServiceImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WebhookTriggerTestApiControllerAuthorizationTest.Config.class)
class WebhookTriggerTestApiControllerAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "isTenantAdmin()";
    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final String WORKFLOW_ID = "workflow-1";

    @Autowired
    private WebhookTriggerTestApiController controller;

    @Autowired
    private GateRecorder gateRecorder;

    @Autowired
    private WebhookTriggerTestFacade webhookTriggerTestFacade;

    @BeforeEach
    void setUp() {
        reset(webhookTriggerTestFacade);

        gateRecorder.reset();

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                "user@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testStartWebhookTriggerTestDeniesCallerWhoIsNotTenantAdmin() {
        gateRecorder.permit(false);

        assertThatThrownBy(() -> controller.startWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(gateRecorder.getCallCount()).isEqualTo(1);

        verifyNoInteractions(webhookTriggerTestFacade);
    }

    @Test
    void testStartWebhookTriggerTestPermitsTenantAdmin() {
        gateRecorder.permit(true);

        when(webhookTriggerTestFacade.enableTrigger(anyString(), anyLong(), eq(PlatformType.EMBEDDED)))
            .thenReturn("https://example.org/webhook");

        controller.startWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL);

        assertThat(gateRecorder.getCallCount()).isEqualTo(1);

        verify(webhookTriggerTestFacade).enableTrigger(WORKFLOW_ID, DEVELOPMENT_ORDINAL, PlatformType.EMBEDDED);
    }

    @Test
    void testStopWebhookTriggerTestDeniesCallerWhoIsNotTenantAdmin() {
        gateRecorder.permit(false);

        assertThatThrownBy(() -> controller.stopWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(gateRecorder.getCallCount()).isEqualTo(1);

        verifyNoInteractions(webhookTriggerTestFacade);
    }

    @Test
    void testStopWebhookTriggerTestPermitsTenantAdmin() {
        gateRecorder.permit(true);

        controller.stopWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL);

        assertThat(gateRecorder.getCallCount()).isEqualTo(1);

        verify(webhookTriggerTestFacade).disableTrigger(WORKFLOW_ID, DEVELOPMENT_ORDINAL, PlatformType.EMBEDDED);
    }

    @Test
    void testStartWebhookTriggerTestRequiresTenantAdmin() {
        assertExpression("startWebhookTriggerTest");
    }

    @Test
    void testStopWebhookTriggerTestRequiresTenantAdmin() {
        assertExpression("stopWebhookTriggerTest");
    }

    /**
     * Pins the production expression, and names the one comment elsewhere in the tree that asserts what it is.
     * {@code WebhookTriggerTestApiFacadeImpl}'s class javadoc is ticket 1051's inventory of who reaches the shared
     * {@code WebhookTriggerTestFacade} and under what gate; it is what a future auditor reads instead of re-deriving
     * this, and it goes stale exactly when the annotation below changes. Nothing can assert a comment, so the failure
     * message carries the obligation.
     */
    private static void assertExpression(String methodName) {
        List<Method> methods = Arrays.stream(WebhookTriggerTestApiController.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(methods)
            .as("Expected exactly one non-synthetic '%s' on WebhookTriggerTestApiController", methodName)
            .hasSize(1);

        PreAuthorize preAuthorize = methods.get(0)
            .getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "@PreAuthorize on %s. If you are removing or changing it, also update the embedded bullet in "
                    + "WebhookTriggerTestApiFacadeImpl's class javadoc, which states this expression by name.",
                methodName)
            .isNotNull();
        assertThat(preAuthorize.value())
            .as(
                "@PreAuthorize expression on %s. WebhookTriggerTestApiFacadeImpl's class javadoc names this exact "
                    + "expression; change one and change the other.",
                methodName)
            .isEqualTo(ADMIN_EXPRESSION);
    }

    @SpringBootConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class Config {

        @Bean
        GateRecorder gateRecorder() {
            return new GateRecorder();
        }

        @Bean
        MethodSecurityExpressionHandler methodSecurityExpressionHandler(GateRecorder gateRecorder) {
            return new TenantAdminExpressionHandler(gateRecorder);
        }

        @Bean
        WebhookTriggerTestApiController webhookTriggerTestApiController(
            WebhookTriggerTestFacade webhookTriggerTestFacade) {

            return new WebhookTriggerTestApiController(webhookTriggerTestFacade);
        }

        @Bean
        WebhookTriggerTestFacade webhookTriggerTestFacade() {
            return mock(WebhookTriggerTestFacade.class);
        }
    }

    /**
     * Records whether the gate was consulted and what it answered, so a test can tell "denied by the gate" apart from
     * "the body happened not to run".
     */
    static final class GateRecorder {

        private boolean permit;
        private int callCount;

        void reset() {
            permit = false;
            callCount = 0;
        }

        void permit(boolean value) {
            permit = value;
        }

        int getCallCount() {
            return callCount;
        }

        private boolean record() {
            callCount++;

            return permit;
        }
    }

    private static final class TenantAdminExpressionHandler extends DefaultMethodSecurityExpressionHandler {

        private final GateRecorder gateRecorder;

        private TenantAdminExpressionHandler(GateRecorder gateRecorder) {
            this.gateRecorder = gateRecorder;
        }

        @Override
        public EvaluationContext createEvaluationContext(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {

            StandardEvaluationContext evaluationContext =
                (StandardEvaluationContext) super.createEvaluationContext(authentication, methodInvocation);

            TenantAdminExpressionRoot root = new TenantAdminExpressionRoot(
                authentication, methodInvocation, gateRecorder);

            root.setAuthorizationManagerFactory(getAuthorizationManagerFactory());
            root.setPermissionEvaluator(getPermissionEvaluator());
            root.setDefaultRolePrefix(getDefaultRolePrefix());

            evaluationContext.setRootObject(root);

            return evaluationContext;
        }
    }

    private static final class TenantAdminExpressionRoot extends SecurityExpressionRoot
        implements MethodSecurityExpressionOperations {

        private final GateRecorder gateRecorder;
        private final MethodInvocation methodInvocation;

        private Object filterObject;
        private Object returnObject;

        private TenantAdminExpressionRoot(
            Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation,
            GateRecorder gateRecorder) {

            super(authentication::get);

            this.gateRecorder = gateRecorder;
            this.methodInvocation = methodInvocation;
        }

        public boolean isTenantAdmin() {
            return gateRecorder.record();
        }

        @Override
        public Object getFilterObject() {
            return filterObject;
        }

        @Override
        public void setFilterObject(Object filterObject) {
            this.filterObject = filterObject;
        }

        @Override
        public Object getReturnObject() {
            return returnObject;
        }

        @Override
        public void setReturnObject(Object returnObject) {
            this.returnObject = returnObject;
        }

        @Override
        public Object getThis() {
            return methodInvocation.getThis();
        }
    }
}
