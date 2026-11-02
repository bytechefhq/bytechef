/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.subflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
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
class RemoteChildJobPrincipalFactoryClientTest {

    private MockRestServiceServer mockRestServiceServer;
    private RemoteChildJobPrincipalFactoryClient remoteChildJobPrincipalFactoryClient;

    @BeforeEach
    void beforeEach() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://localhost");

        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        remoteChildJobPrincipalFactoryClient = new RemoteChildJobPrincipalFactoryClient(
            new LoadBalancedRestClient(restClientBuilder));
    }

    @Test
    void testCreatePrincipalLinkedJob() {
        mockRestServiceServer
            .expect(
                requestTo("http://configuration-app/remote/child-job-principal-factory/create-principal-linked-job"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(
                content().json(
                    "{\"referenceJobId\":7,\"jobParameters\":{\"workflowId\":\"subflow-uuid\",\"inputs\":{\"a\":1}}}",
                    JsonCompareMode.LENIENT))
            .andRespond(withSuccess("42", MediaType.APPLICATION_JSON));

        long jobId = remoteChildJobPrincipalFactoryClient.createPrincipalLinkedJob(
            7L, new JobParametersDTO("subflow-uuid", Map.of("a", 1)));

        assertThat(jobId).isEqualTo(42L);

        mockRestServiceServer.verify();
    }
}
