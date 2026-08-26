/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * The {@code isTenantAdmin()} gate both endpoints now carry is environment-agnostic, so the environmentId reaching
 * WebhookTriggerTestFacade.enableTrigger/disableTrigger must still be resolved to the caller's own: it mints or tears
 * down a live webhook URL in whatever environment it names. These tests pin that execution side; the gate itself is
 * pinned by {@link WebhookTriggerTestApiControllerAuthorizationTest}. The controller is constructed directly here, so
 * no proxy intercepts and the gate does not run.
 *
 * <p>
 * They also pin the {@link PlatformType} the controller passes, which is a CONSTRAINT ON WHICH CLIENT PAGES MAY CALL
 * THESE ENDPOINTS rather than an incidental argument -- see
 * {@link #testStartWebhookTriggerTestIsTypedEmbeddedWhichConstrainsItsCallers()}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WebhookTriggerTestApiControllerTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    private WebhookTriggerTestApiController controller;
    private WebhookTriggerTestFacade webhookTriggerTestFacade;

    @BeforeEach
    void setUp() {
        webhookTriggerTestFacade = mock(WebhookTriggerTestFacade.class);
        controller = new WebhookTriggerTestApiController(webhookTriggerTestFacade);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
     * matcher inside the environment tests above: only a page editing an INTEGRATION workflow may call these endpoints.
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

        when(webhookTriggerTestFacade.enableTrigger(anyString(), anyLong(), eq(PlatformType.EMBEDDED)))
            .thenReturn("https://example.org/webhook");

        controller.startWebhookTriggerTest("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<PlatformType> platformTypeCaptor = ArgumentCaptor.forClass(PlatformType.class);

        verify(webhookTriggerTestFacade).enableTrigger(eq("workflow-1"), anyLong(), platformTypeCaptor.capture());

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

        controller.stopWebhookTriggerTest("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<PlatformType> platformTypeCaptor = ArgumentCaptor.forClass(PlatformType.class);

        verify(webhookTriggerTestFacade).disableTrigger(eq("workflow-1"), anyLong(), platformTypeCaptor.capture());

        assertThat(platformTypeCaptor.getValue())
            .as(
                "stopWebhookTriggerTest must pass PlatformType.EMBEDDED. See "
                    + "testStartWebhookTriggerTestIsTypedEmbeddedWhichConstrainsItsCallers.")
            .isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testStartWebhookTriggerTestUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(webhookTriggerTestFacade.enableTrigger(anyString(), anyLong(), eq(PlatformType.EMBEDDED)))
            .thenReturn("https://example.org/webhook");

        controller.startWebhookTriggerTest("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).enableTrigger(
            eq("workflow-1"), environmentIdCaptor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(environmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testStartWebhookTriggerTestHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(webhookTriggerTestFacade.enableTrigger(anyString(), anyLong(), eq(PlatformType.EMBEDDED)))
            .thenReturn("https://example.org/webhook");

        controller.startWebhookTriggerTest("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).enableTrigger(
            eq("workflow-1"), environmentIdCaptor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(environmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
    }

    @Test
    void testStopWebhookTriggerTestUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        controller.stopWebhookTriggerTest("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).disableTrigger(
            eq("workflow-1"), environmentIdCaptor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(environmentIdCaptor.getValue()).isEqualTo(PRODUCTION_ORDINAL);
    }

    @Test
    void testStopWebhookTriggerTestHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        controller.stopWebhookTriggerTest("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).disableTrigger(
            eq("workflow-1"), environmentIdCaptor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(environmentIdCaptor.getValue()).isEqualTo(DEVELOPMENT_ORDINAL);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static User user() {
        return new User("connected-user-1", "", List.of());
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }
}
