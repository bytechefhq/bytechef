/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RemoteTriggerLifecycleFacadeClientTest {

    private static final String TRIGGER_LIFECYCLE_FACADE = "http://execution-app/remote/trigger-lifecycle-facade";

    private MockRestServiceServer server;
    private RemoteTriggerLifecycleFacadeClient remoteTriggerLifecycleFacadeClient;
    private WorkflowExecutionId workflowExecutionId;

    @BeforeEach
    void beforeEach() {
        TenantContext.setCurrentTenantId("public");

        RestClient.Builder builder = RestClient.builder()
            .baseUrl("http://execution-app");

        server = MockRestServiceServer.bindTo(builder)
            .build();
        remoteTriggerLifecycleFacadeClient = new RemoteTriggerLifecycleFacadeClient(
            new LoadBalancedRestClient(builder));
        workflowExecutionId = WorkflowExecutionId.of(PlatformType.AUTOMATION, 7L, "workflow-uuid", "trigger_1");
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testExecuteTriggerDisablePostsToTheDisableRouteWithoutAConnection() {
        server.expect(requestTo(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-disable"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.workflowId").value("workflow-1"))
            .andExpect(jsonPath("$.workflowExecutionId").value(workflowExecutionId.toString()))
            .andExpect(jsonPath("$.triggerWorkflowNodeType.name").value("webhook"))
            .andExpect(jsonPath("$.triggerParameters.path").value("/hook"))
            .andExpect(jsonPath("$.connectionId").isEmpty())
            .andRespond(withSuccess("true", MediaType.APPLICATION_JSON));

        boolean disabled = remoteTriggerLifecycleFacadeClient.executeTriggerDisable(
            "workflow-1", workflowExecutionId, WorkflowNodeType.ofType("webhook/v1/trigger"), Map.of("path", "/hook"),
            null);

        assertThat(disabled).isTrue();

        server.verify();
    }

    @Test
    void testExecuteTriggerDisableReportsATriggerTheExecutionAppCouldNotDisable() {
        server.expect(requestTo(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-disable"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("false", MediaType.APPLICATION_JSON));

        boolean disabled = remoteTriggerLifecycleFacadeClient.executeTriggerDisable(
            "workflow-1", workflowExecutionId, WorkflowNodeType.ofType("webhook/v1/trigger"), Map.of(), 42L);

        assertThat(disabled).isFalse();

        server.verify();
    }

    @Test
    void testExecuteTriggerEnableUndoPostsTheEnableOutputAndThePreviousStateToTheUndoRoute() {
        server.expect(requestTo(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-enable-undo"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.workflowId").value("workflow-1"))
            .andExpect(jsonPath("$.workflowExecutionId").value(workflowExecutionId.toString()))
            .andExpect(jsonPath("$.triggerWorkflowNodeType.name").value("webhook"))
            .andExpect(jsonPath("$.triggerParameters.path").value("/hook"))
            .andExpect(jsonPath("$.connectionId").value(42))
            .andExpect(jsonPath("$.enableOutput.parameters.id").value("subscription-armed-by-the-enable"))
            .andExpect(jsonPath("$.enableOutput.webhookExpirationDate").value("2026-12-01T00:00:00Z"))
            .andExpect(jsonPath("$.previousTriggerState.parameters.id").value("subscription-armed-before-the-enable"))
            .andRespond(withSuccess("true", MediaType.APPLICATION_JSON));

        boolean undone = remoteTriggerLifecycleFacadeClient.executeTriggerEnableUndo(
            "workflow-1", workflowExecutionId, WorkflowNodeType.ofType("webhook/v1/trigger"), Map.of("path", "/hook"),
            42L,
            new WebhookEnableOutput(
                Map.of("id", "subscription-armed-by-the-enable"), Instant.parse("2026-12-01T00:00:00Z")),
            new WebhookEnableOutput(Map.of("id", "subscription-armed-before-the-enable"), null));

        assertThat(undone).isTrue();

        server.verify();
    }

    @Test
    void testExecuteTriggerEnableUndoPostsNoConnectionAndNoPreviousState() {
        server.expect(requestTo(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-enable-undo"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.connectionId").isEmpty())
            .andExpect(jsonPath("$.previousTriggerState").isEmpty())
            .andRespond(withSuccess("false", MediaType.APPLICATION_JSON));

        boolean undone = remoteTriggerLifecycleFacadeClient.executeTriggerEnableUndo(
            "workflow-1", workflowExecutionId, WorkflowNodeType.ofType("webhook/v1/trigger"), Map.of(), null,
            new WebhookEnableOutput(Map.of("id", "subscription-armed-by-the-enable"), null), null);

        assertThat(undone).isFalse();

        server.verify();
    }

    @Test
    void testExecuteTriggerEnableUndoReportsAnEmptyResponseAsNotUndone() {
        server.expect(requestTo(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-enable-undo"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess());

        boolean undone = remoteTriggerLifecycleFacadeClient.executeTriggerEnableUndo(
            "workflow-1", workflowExecutionId, WorkflowNodeType.ofType("webhook/v1/trigger"), Map.of(), 42L,
            new WebhookEnableOutput(Map.of("id", "subscription-armed-by-the-enable"), null), null);

        assertThat(undone).isFalse();

        server.verify();
    }

    @Test
    void testExecuteTriggerEnablePostsToTheEnableRouteWithItsConnection() {
        server.expect(requestTo(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-enable"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.workflowExecutionId").value(workflowExecutionId.toString()))
            .andExpect(jsonPath("$.connectionId").value(42))
            .andExpect(jsonPath("$.webhookUrl").value("https://hooks.example/1"))
            .andExpect(jsonPath("$.environmentId").value(1))
            .andRespond(withSuccess());

        WebhookEnableOutput enableOutput = remoteTriggerLifecycleFacadeClient.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WorkflowNodeType.ofType("webhook/v1/trigger"), Map.of(), 42L,
            "https://hooks.example/1", 1L);

        assertThat(enableOutput).isNull();

        server.verify();
    }

    @Test
    void testExecuteTriggerEnableReadsTheWebhookEnableOutputFromTheResponseBody() {
        server.expect(requestTo(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-enable"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """
                        {"parameters":{"id":"subscription-1","secret":"s3cr3t"},
                        "webhookExpirationDate":"2026-12-01T00:00:00Z"}""",
                    MediaType.APPLICATION_JSON));

        WebhookEnableOutput enableOutput = remoteTriggerLifecycleFacadeClient.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WorkflowNodeType.ofType("webhook/v1/trigger"), Map.of(), 42L,
            "https://hooks.example/1", 1L);

        assertThat(enableOutput).isEqualTo(
            new WebhookEnableOutput(
                Map.of("id", "subscription-1", "secret", "s3cr3t"), Instant.parse("2026-12-01T00:00:00Z")));

        server.verify();
    }
}
