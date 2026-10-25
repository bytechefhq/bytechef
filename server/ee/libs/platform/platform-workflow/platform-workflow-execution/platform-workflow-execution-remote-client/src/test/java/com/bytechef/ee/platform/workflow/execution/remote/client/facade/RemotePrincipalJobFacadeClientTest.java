/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.constant.PlatformType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RemotePrincipalJobFacadeClientTest {

    private MockRestServiceServer mockRestServiceServer;
    private RemotePrincipalJobFacadeClient remotePrincipalJobFacadeClient;

    @BeforeEach
    void beforeEach() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://localhost");

        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        remotePrincipalJobFacadeClient = new RemotePrincipalJobFacadeClient(
            new LoadBalancedRestClient(restClientBuilder));
    }

    @Test
    void testCreatePrincipalLinkedJobForPrincipal() {
        mockRestServiceServer
            .expect(
                requestTo(
                    "http://execution-app/remote/principal-job-facade/create-principal-linked-job-for-principal"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(
                content().json(
                    "{\"jobParameters\":{\"workflowId\":\"subflow-uuid\",\"inputs\":{\"a\":1}},\"jobPrincipalId\":5," +
                        "\"type\":\"AUTOMATION\"}",
                    JsonCompareMode.LENIENT))
            .andRespond(withSuccess("42", MediaType.APPLICATION_JSON));

        long jobId = remotePrincipalJobFacadeClient.createPrincipalLinkedJob(
            new JobParametersDTO("subflow-uuid", Map.of("a", 1)), 5L, PlatformType.AUTOMATION);

        assertThat(jobId).isEqualTo(42L);

        mockRestServiceServer.verify();
    }
}
