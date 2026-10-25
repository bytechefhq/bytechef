/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.facade;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RemoteJobFacadeClientTest {

    private MockRestServiceServer mockRestServiceServer;
    private RemoteJobFacadeClient remoteJobFacadeClient;

    @BeforeEach
    void beforeEach() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://localhost");

        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        remoteJobFacadeClient = new RemoteJobFacadeClient(new LoadBalancedRestClient(restClientBuilder));
    }

    @Test
    void testResumeClaimedJob() {
        mockRestServiceServer.expect(requestTo("http://execution-app/remote/job-facade/resume-claimed-job"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(
                content().json(
                    "{\"id\":7,\"taskExecutionId\":11,\"data\":{\"status\":\"COMPLETED\"}}", JsonCompareMode.STRICT))
            .andRespond(withNoContent());

        remoteJobFacadeClient.resumeClaimedJob(7L, 11L, Map.of("status", "COMPLETED"));

        mockRestServiceServer.verify();
    }

    @Test
    void testResumeClaimedJobWithoutTaskExecutionIdAndData() {
        mockRestServiceServer.expect(requestTo("http://execution-app/remote/job-facade/resume-claimed-job"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("{\"id\":7,\"taskExecutionId\":null,\"data\":null}", JsonCompareMode.STRICT))
            .andRespond(withNoContent());

        remoteJobFacadeClient.resumeClaimedJob(7L, null, null);

        mockRestServiceServer.verify();
    }
}
