/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import java.util.List;
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
class RemoteConnectedUserWorkflowCopyResolverClientTest {

    private static final String WORKFLOW_COPY_UUIDS_URL =
        "http://configuration-app/remote/connected-user-workflow-copy-resolver/get-workflow-copy-uuids/template-uuid";

    private MockRestServiceServer mockRestServiceServer;
    private RemoteConnectedUserWorkflowCopyResolverClient remoteConnectedUserWorkflowCopyResolverClient;

    @BeforeEach
    void beforeEach() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://localhost");

        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        remoteConnectedUserWorkflowCopyResolverClient = new RemoteConnectedUserWorkflowCopyResolverClient(
            new LoadBalancedRestClient(restClientBuilder));
    }

    @Test
    void testGetWorkflowCopyUuids() {
        mockRestServiceServer.expect(requestTo(WORKFLOW_COPY_UUIDS_URL))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(TenantConstants.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId()))
            .andRespond(withSuccess("[\"copy-uuid-1\",\"copy-uuid-2\"]", MediaType.APPLICATION_JSON));

        List<String> workflowCopyUuids = remoteConnectedUserWorkflowCopyResolverClient.getWorkflowCopyUuids(
            "template-uuid");

        assertThat(workflowCopyUuids).containsExactly("copy-uuid-1", "copy-uuid-2");

        mockRestServiceServer.verify();
    }

    @Test
    void testGetWorkflowCopyUuidsReturnsEmptyList() {
        mockRestServiceServer.expect(requestTo(WORKFLOW_COPY_UUIDS_URL))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(remoteConnectedUserWorkflowCopyResolverClient.getWorkflowCopyUuids("template-uuid")).isEmpty();

        mockRestServiceServer.verify();
    }
}
