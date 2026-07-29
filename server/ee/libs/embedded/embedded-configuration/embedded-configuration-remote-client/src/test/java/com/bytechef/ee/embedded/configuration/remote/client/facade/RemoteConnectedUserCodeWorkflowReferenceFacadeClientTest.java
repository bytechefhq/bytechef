/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RemoteConnectedUserCodeWorkflowReferenceFacadeClientTest {

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId("public");
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testGetConnectedUserWorkflowsHitsTheRemoteRestPath() {
        RestClient.Builder builder = RestClient.builder()
            .baseUrl("http://configuration-app");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder)
            .build();

        server.expect(
            requestTo("http://configuration-app/remote/connected-user-code-workflow-reference-facade"
                + "/get-connected-user-workflows/42"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        RemoteConnectedUserCodeWorkflowReferenceFacadeClient client =
            new RemoteConnectedUserCodeWorkflowReferenceFacadeClient(new LoadBalancedRestClient(builder));

        List<ConnectedUserProjectWorkflow> result = client.getConnectedUserWorkflows(42L);

        assertThat(result).isEmpty();

        server.verify();
    }

    @Test
    void testGetOrCreateReferenceTranslates409IntoMissingConnectionException() {
        RestClient.Builder builder = RestClient.builder()
            .baseUrl("http://configuration-app");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder)
            .build();

        server.expect(
            requestTo("http://configuration-app/remote/connected-user-code-workflow-reference-facade"
                + "/get-or-create-reference?externalUserId=ext-1&catalogWorkflowUuid=uuid-1&environment=PRODUCTION"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"missingConnectionComponentName\":\"slack\"}"));

        RemoteConnectedUserCodeWorkflowReferenceFacadeClient client =
            new RemoteConnectedUserCodeWorkflowReferenceFacadeClient(new LoadBalancedRestClient(builder));

        assertThatThrownBy(() -> client.getOrCreateReference("ext-1", "uuid-1", Environment.PRODUCTION))
            .isInstanceOf(MissingConnectionException.class)
            .hasFieldOrPropertyWithValue("componentName", "slack");
    }

    @Test
    void testGetOrCreateReferenceDoesNotTranslateOtherErrorStatuses() {
        RestClient.Builder builder = RestClient.builder()
            .baseUrl("http://configuration-app");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder)
            .build();

        server.expect(
            requestTo("http://configuration-app/remote/connected-user-code-workflow-reference-facade"
                + "/get-or-create-reference?externalUserId=ext-1&catalogWorkflowUuid=uuid-1&environment=PRODUCTION"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        RemoteConnectedUserCodeWorkflowReferenceFacadeClient client =
            new RemoteConnectedUserCodeWorkflowReferenceFacadeClient(new LoadBalancedRestClient(builder));

        assertThatThrownBy(() -> client.getOrCreateReference("ext-1", "uuid-1", Environment.PRODUCTION))
            .isInstanceOf(HttpServerErrorException.class)
            .isNotInstanceOf(MissingConnectionException.class);
    }

    @Test
    void testGetConnectedUserWorkflowsDeserializesPopulatedElements() {
        RestClient.Builder builder = RestClient.builder()
            .baseUrl("http://configuration-app");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder)
            .build();

        String responseBody = "[{"
            + "\"id\":1,"
            + "\"connectedUserProjectId\":10,"
            + "\"projectWorkflowId\":20,"
            + "\"workflowVersion\":1,"
            + "\"catalogWorkflowUuid\":\"cat-uuid-1\","
            + "\"copiedFromWorkflowUuid\":\"copied-uuid-1\","
            + "\"projectDeploymentId\":30,"
            + "\"enabled\":true,"
            + "\"dangling\":false,"
            + "\"danglingReason\":null,"
            + "\"createdBy\":\"user1\","
            + "\"createdDate\":\"2025-01-01T10:00:00Z\","
            + "\"lastModifiedBy\":\"user2\","
            + "\"lastModifiedDate\":\"2025-01-02T10:00:00Z\","
            + "\"version\":2"
            + "}]";

        server.expect(
            requestTo("http://configuration-app/remote/connected-user-code-workflow-reference-facade"
                + "/get-connected-user-workflows/42"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        RemoteConnectedUserCodeWorkflowReferenceFacadeClient client =
            new RemoteConnectedUserCodeWorkflowReferenceFacadeClient(new LoadBalancedRestClient(builder));

        List<ConnectedUserProjectWorkflow> result = client.getConnectedUserWorkflows(42L);

        assertThat(result).hasSize(1);
        ConnectedUserProjectWorkflow workflow = result.get(0);
        assertThat(workflow.getId()).isEqualTo(1L);
        assertThat(workflow.getConnectedUserProjectId()).isEqualTo(10L);
        assertThat(workflow.getProjectWorkflowId()).isEqualTo(20L);
        assertThat(workflow.getWorkflowVersion()).isEqualTo(1);
        assertThat(workflow.getCatalogWorkflowUuid()).isEqualTo("cat-uuid-1");
        assertThat(workflow.getCopiedFromWorkflowUuid()).isEqualTo("copied-uuid-1");
        assertThat(workflow.getProjectDeploymentId()).isEqualTo(30L);
        assertThat(workflow.isEnabled()).isTrue();
        assertThat(workflow.isDangling()).isFalse();
        assertThat(workflow.getDanglingReason()).isNull();
        assertThat(workflow.getVersion()).isEqualTo(2);

        server.verify();
    }

    @Test
    void testGetOrCreateReferenceDeserializesPopulatedBody() {
        RestClient.Builder builder = RestClient.builder()
            .baseUrl("http://configuration-app");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder)
            .build();

        String responseBody = "{"
            + "\"id\":5,"
            + "\"connectedUserProjectId\":15,"
            + "\"projectWorkflowId\":25,"
            + "\"workflowVersion\":3,"
            + "\"catalogWorkflowUuid\":\"cat-uuid-2\","
            + "\"copiedFromWorkflowUuid\":null,"
            + "\"projectDeploymentId\":35,"
            + "\"enabled\":false,"
            + "\"dangling\":true,"
            + "\"danglingReason\":\"workflow deleted\","
            + "\"createdBy\":\"system\","
            + "\"createdDate\":\"2025-02-01T10:00:00Z\","
            + "\"lastModifiedBy\":\"admin\","
            + "\"lastModifiedDate\":\"2025-02-02T10:00:00Z\","
            + "\"version\":3"
            + "}";

        server.expect(
            requestTo("http://configuration-app/remote/connected-user-code-workflow-reference-facade"
                + "/get-or-create-reference?externalUserId=ext-2&catalogWorkflowUuid=uuid-2&environment=STAGING"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        RemoteConnectedUserCodeWorkflowReferenceFacadeClient client =
            new RemoteConnectedUserCodeWorkflowReferenceFacadeClient(new LoadBalancedRestClient(builder));

        ConnectedUserProjectWorkflow result = client.getOrCreateReference("ext-2", "uuid-2", Environment.STAGING);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getConnectedUserProjectId()).isEqualTo(15L);
        assertThat(result.getProjectWorkflowId()).isEqualTo(25L);
        assertThat(result.getWorkflowVersion()).isEqualTo(3);
        assertThat(result.getCatalogWorkflowUuid()).isEqualTo("cat-uuid-2");
        assertThat(result.getCopiedFromWorkflowUuid()).isNull();
        assertThat(result.getProjectDeploymentId()).isEqualTo(35L);
        assertThat(result.isEnabled()).isFalse();
        assertThat(result.isDangling()).isTrue();
        assertThat(result.getDanglingReason()).isEqualTo("workflow deleted");
        assertThat(result.getVersion()).isEqualTo(3);

        server.verify();
    }
}
