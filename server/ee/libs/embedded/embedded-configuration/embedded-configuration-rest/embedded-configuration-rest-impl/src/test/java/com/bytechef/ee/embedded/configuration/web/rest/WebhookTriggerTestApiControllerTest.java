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

import com.bytechef.ee.embedded.configuration.web.rest.TenantAdminGateTestSupport.GateRecorder;
import com.bytechef.ee.embedded.configuration.web.rest.TenantAdminGateTestSupport.TenantAdminExpressionHandler;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Covers {@link WebhookTriggerTestApiController}: the {@code @PreAuthorize} gates on both endpoints, the
 * {@code PlatformType} they pass to {@code WebhookTriggerTestFacade}, and the environment they resolve for the calling
 * principal.
 *
 * <p>
 * Both endpoints previously carried no authorization at all, so any authenticated principal in the tenant could mint a
 * live webhook URL for any workflow (running {@code executeWebhookEnable} against that workflow's test-configuration
 * connection, whose callback then writes a test output), or tear one down under somebody else. The denial tests prove
 * the gate is reached before the shared {@code WebhookTriggerTestFacade} is touched at all. The permit tests prove it
 * is not a gate that denies everybody: with the check satisfied the endpoints run to completion and reach the facade
 * with the workflow id and the resolved environment. Every other test runs with the gate permitted.
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
@SpringBootTest(classes = WebhookTriggerTestApiControllerTest.Config.class)
class WebhookTriggerTestApiControllerTest {

    private static final String ADMIN_EXPRESSION = "isTenantAdmin()";
    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;
    private static final String TRIGGER_NAME = "trigger_1";
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
        gateRecorder.permit(true);

        authenticate(
            new UsernamePasswordAuthenticationToken(
                "user@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testStartWebhookTriggerTestDeniesCallerWhoIsNotTenantAdmin() {
        gateRecorder.permit(false);

        assertThatThrownBy(() -> controller.startWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL, TRIGGER_NAME))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(gateRecorder.getCallCount()).isEqualTo(1);

        verifyNoInteractions(webhookTriggerTestFacade);
    }

    @Test
    void testStartWebhookTriggerTestPermitsTenantAdmin() {
        gateRecorder.permit(true);

        when(
            webhookTriggerTestFacade.enableTrigger(anyString(), eq(TRIGGER_NAME), anyLong(), eq(PlatformType.EMBEDDED)))
                .thenReturn("https://example.org/webhook");

        controller.startWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL, TRIGGER_NAME);

        assertThat(gateRecorder.getCallCount()).isEqualTo(1);

        verify(webhookTriggerTestFacade).enableTrigger(WORKFLOW_ID, TRIGGER_NAME, DEVELOPMENT_ORDINAL,
            PlatformType.EMBEDDED);
    }

    @Test
    void testStopWebhookTriggerTestDeniesCallerWhoIsNotTenantAdmin() {
        gateRecorder.permit(false);

        assertThatThrownBy(() -> controller.stopWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL, TRIGGER_NAME))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(gateRecorder.getCallCount()).isEqualTo(1);

        verifyNoInteractions(webhookTriggerTestFacade);
    }

    @Test
    void testStopWebhookTriggerTestPermitsTenantAdmin() {
        gateRecorder.permit(true);

        controller.stopWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL, TRIGGER_NAME);

        assertThat(gateRecorder.getCallCount()).isEqualTo(1);

        verify(webhookTriggerTestFacade).disableTrigger(WORKFLOW_ID, TRIGGER_NAME, DEVELOPMENT_ORDINAL,
            PlatformType.EMBEDDED);
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
     * {@code PlatformType.EMBEDDED} is hardcoded by both endpoints and is NOT a formality: it selects
     * {@code IntegrationJobPrincipalAccessor}, so {@code WebhookTriggerTestFacadeImpl#executeTrigger} resolves the
     * workflow via {@code IntegrationWorkflowServiceImpl#getWorkflowIntegrationWorkflow}, which is
     * {@code findByWorkflowId(workflowId).orElseThrow(...)}. A workflow with no {@code integration_workflow} row fails
     * the FIRST statement of that method with {@code IllegalArgumentException("Workflow not found for id: ...")}.
     *
     * <p>
     * The practical consequence, and the reason this is pinned on its own rather than left as an {@code eq(...)}
     * matcher inside the environment tests below: only a page editing an INTEGRATION workflow may call these endpoints.
     * That is {@code Integration.tsx} alone. {@code WorkflowBuilder.tsx} and {@code AutomationWorkflow.tsx} live under
     * {@code client/src/ee/pages/embedded} too but edit PROJECT workflows, which have no {@code integration_workflow}
     * row; pointing them here 400s on every call, and they use the automation endpoint instead. Changing this constant,
     * or repointing another page at this controller, means revisiting that.
     *
     * <p>
     * Note what this test can and cannot do. The facade is a mock, so the throw described above is NOT exercised here
     * -- only the constant is. That is precisely the gap that once let a bad repoint through review: an
     * {@code eq(PlatformType.EMBEDDED)} matcher asserts the argument while saying nothing about what it implies. The
     * comment carries the obligation the assertion cannot.
     */
    @Test
    void testStartWebhookTriggerTestIsTypedEmbeddedWhichConstrainsItsCallers() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(
            webhookTriggerTestFacade.enableTrigger(anyString(), eq(TRIGGER_NAME), anyLong(), eq(PlatformType.EMBEDDED)))
                .thenReturn("https://example.org/webhook");

        controller.startWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL, TRIGGER_NAME);

        ArgumentCaptor<PlatformType> platformTypeCaptor = ArgumentCaptor.forClass(PlatformType.class);

        verify(webhookTriggerTestFacade).enableTrigger(eq(WORKFLOW_ID), eq(TRIGGER_NAME), anyLong(),
            platformTypeCaptor.capture());

        assertThat(platformTypeCaptor.getValue())
            .as(
                "startWebhookTriggerTest must pass PlatformType.EMBEDDED, which routes through "
                    + "IntegrationJobPrincipalAccessor and so requires an integration_workflow row. Only a page "
                    + "editing an integration workflow (Integration.tsx) may call this endpoint; a project workflow "
                    + "400s. See this method's javadoc before changing it.")
            .isEqualTo(PlatformType.EMBEDDED);
    }

    /**
     * The {@code stopWebhookTriggerTest} half of
     * {@link #testStartWebhookTriggerTestIsTypedEmbeddedWhichConstrainsItsCallers()}. Both endpoints resolve the
     * workflow the same way, so a page that 400s on start 400s on stop.
     */
    @Test
    void testStopWebhookTriggerTestIsTypedEmbeddedWhichConstrainsItsCallers() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        controller.stopWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL, TRIGGER_NAME);

        ArgumentCaptor<PlatformType> platformTypeCaptor = ArgumentCaptor.forClass(PlatformType.class);

        verify(webhookTriggerTestFacade).disableTrigger(eq(WORKFLOW_ID), eq(TRIGGER_NAME), anyLong(),
            platformTypeCaptor.capture());

        assertThat(platformTypeCaptor.getValue())
            .as(
                "stopWebhookTriggerTest must pass PlatformType.EMBEDDED. See "
                    + "testStartWebhookTriggerTestIsTypedEmbeddedWhichConstrainsItsCallers.")
            .isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testStartWebhookTriggerTestUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(
            webhookTriggerTestFacade.enableTrigger(anyString(), eq(TRIGGER_NAME), anyLong(), eq(PlatformType.EMBEDDED)))
                .thenReturn("https://example.org/webhook");

        controller.startWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL, TRIGGER_NAME);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).enableTrigger(
            eq(WORKFLOW_ID), eq(TRIGGER_NAME), environmentIdCaptor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(environmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testStartWebhookTriggerTestHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(
            webhookTriggerTestFacade.enableTrigger(anyString(), eq(TRIGGER_NAME), anyLong(), eq(PlatformType.EMBEDDED)))
                .thenReturn("https://example.org/webhook");

        controller.startWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL, TRIGGER_NAME);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).enableTrigger(
            eq(WORKFLOW_ID), eq(TRIGGER_NAME), environmentIdCaptor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(environmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
    }

    @Test
    void testStopWebhookTriggerTestUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        controller.stopWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL, TRIGGER_NAME);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).disableTrigger(
            eq(WORKFLOW_ID), eq(TRIGGER_NAME), environmentIdCaptor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(environmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testStopWebhookTriggerTestHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        controller.stopWebhookTriggerTest(WORKFLOW_ID, DEVELOPMENT_ORDINAL, TRIGGER_NAME);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).disableTrigger(
            eq(WORKFLOW_ID), eq(TRIGGER_NAME), environmentIdCaptor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(environmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
    }

    /**
     * Pins the production expression. The shared {@code WebhookTriggerTestFacade} carries no gate of its own, so this
     * controller-level annotation is the only authorization on the embedded webhook-test path.
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
                "@PreAuthorize on %s. WebhookTriggerTestFacade is not gated, so without it any authenticated "
                    + "principal in the tenant can start or stop a webhook trigger test for any workflow.",
                methodName)
            .isNotNull();
        assertThat(preAuthorize.value())
            .as("@PreAuthorize expression on %s", methodName)
            .isEqualTo(ADMIN_EXPRESSION);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static User user() {
        return new User("connected-user-1", "", List.of());
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

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }
}
