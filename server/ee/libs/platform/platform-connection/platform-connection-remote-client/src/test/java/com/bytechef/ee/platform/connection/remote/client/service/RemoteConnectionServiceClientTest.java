/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RemoteConnectionServiceClientTest {

    private static final String GET_CONNECTION_URL =
        "http://connection-app/remote/connection-service/get-connection/7";

    private MockRestServiceServer mockRestServiceServer;
    private RemoteConnectionServiceClient remoteConnectionServiceClient;

    @BeforeEach
    void beforeEach() {
        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://localhost");

        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        remoteConnectionServiceClient = new RemoteConnectionServiceClient(
            new LoadBalancedRestClient(restClientBuilder));
    }

    @Test
    void testFetchConnectionReturnsTheConnection() {
        mockRestServiceServer.expect(requestTo(GET_CONNECTION_URL))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"id\":7}", MediaType.APPLICATION_JSON));

        Optional<Connection> connection = remoteConnectionServiceClient.fetchConnection(7L);

        assertThat(connection).hasValueSatisfying(value -> assertThat(value.getId()).isEqualTo(7L));

        mockRestServiceServer.verify();
    }

    @Test
    void testFetchConnectionReturnsEmptyWhenTheConnectionDoesNotExist() {
        mockRestServiceServer.expect(requestTo(GET_CONNECTION_URL))
            .andRespond(withResourceNotFound());

        assertThat(remoteConnectionServiceClient.fetchConnection(7L)).isEmpty();

        mockRestServiceServer.verify();
    }

    @Test
    void testFetchConnectionPropagatesAServerError() {
        mockRestServiceServer.expect(requestTo(GET_CONNECTION_URL))
            .andRespond(withServerError());

        assertThatThrownBy(() -> remoteConnectionServiceClient.fetchConnection(7L))
            .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void testGetConnectionsSendsThePlatformType() {
        mockRestServiceServer
            .expect(requestTo("http://connection-app/remote/connection-service/get-connections/EMBEDDED"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(remoteConnectionServiceClient.getConnections(PlatformType.EMBEDDED)).isEmpty();

        mockRestServiceServer.verify();
    }
}
