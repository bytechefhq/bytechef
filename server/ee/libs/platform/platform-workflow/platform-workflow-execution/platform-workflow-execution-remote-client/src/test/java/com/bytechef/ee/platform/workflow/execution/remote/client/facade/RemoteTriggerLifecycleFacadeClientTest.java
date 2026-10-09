/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
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
            .andRespond(withSuccess());

        remoteTriggerLifecycleFacadeClient.executeTriggerDisable(
            "workflow-1", workflowExecutionId, WorkflowNodeType.ofType("webhook/v1/trigger"), Map.of("path", "/hook"),
            null);

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

        remoteTriggerLifecycleFacadeClient.executeTriggerEnable(
            "workflow-1", workflowExecutionId, WorkflowNodeType.ofType("webhook/v1/trigger"), Map.of(), 42L,
            "https://hooks.example/1", 1L);

        server.verify();
    }
}
