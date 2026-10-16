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
class RemoteConnectedUserProjectDeploymentResolverClientTest {

    private static final String PROJECT_DEPLOYMENT_IDS_URL =
        "http://configuration-app/remote/connected-user-project-deployment-resolver/get-project-deployment-ids/7";

    private MockRestServiceServer mockRestServiceServer;
    private RemoteConnectedUserProjectDeploymentResolverClient remoteConnectedUserProjectDeploymentResolverClient;

    @BeforeEach
    void beforeEach() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://localhost");

        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        remoteConnectedUserProjectDeploymentResolverClient = new RemoteConnectedUserProjectDeploymentResolverClient(
            new LoadBalancedRestClient(restClientBuilder));
    }

    @Test
    void testGetProjectDeploymentIds() {
        mockRestServiceServer.expect(requestTo(PROJECT_DEPLOYMENT_IDS_URL))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(TenantConstants.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId()))
            .andRespond(withSuccess("[3,5,11]", MediaType.APPLICATION_JSON));

        List<Long> projectDeploymentIds =
            remoteConnectedUserProjectDeploymentResolverClient.getProjectDeploymentIds(7L);

        assertThat(projectDeploymentIds).containsExactly(3L, 5L, 11L);

        mockRestServiceServer.verify();
    }

    @Test
    void testGetProjectDeploymentIdsReturnsEmptyList() {
        mockRestServiceServer.expect(requestTo(PROJECT_DEPLOYMENT_IDS_URL))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(remoteConnectedUserProjectDeploymentResolverClient.getProjectDeploymentIds(7L)).isEmpty();

        mockRestServiceServer.verify();
    }
}
