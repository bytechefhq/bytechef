/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.client;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AbstractRestClient {

    private final RestClient restClient;

    @Value("${bytechef.internal.service-token:}")
    private String serviceToken;

    public AbstractRestClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    Consumer<HttpHeaders> headers() {
        return httpHeaders -> {
            httpHeaders.set(TenantConstants.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());
            httpHeaders.set(TenantConstants.INTERNAL_SERVICE_TOKEN, serviceToken);
        };
    }

    @Retryable
    public void delete(Function<UriBuilder, URI> uriFunction) {
        restClient
            .delete()
            .uri(uriFunction)
            .headers(headers())
            .retrieve()
            .toBodilessEntity();
    }

    @Retryable
    public void get(Function<UriBuilder, URI> uriFunction) {
        restClient
            .get()
            .uri(uriFunction)
            .headers(headers())
            .retrieve()
            .toBodilessEntity();
    }

    @Retryable
    public <T> T get(Function<UriBuilder, URI> uriFunction, Class<T> responseClass) {
        return restClient
            .get()
            .uri(uriFunction)
            .headers(headers())
            .retrieve()
            .body(responseClass);
    }

    @Retryable
    public <T> T get(Function<UriBuilder, URI> uriFunction, ParameterizedTypeReference<T> responseTypeRef) {
        return restClient
            .get()
            .uri(uriFunction)
            .headers(headers())
            .retrieve()
            .body(responseTypeRef);
    }

    @Retryable
    public void post(Function<UriBuilder, URI> uriFunction, Object bodyValue) {
        RestClient.RequestBodySpec requestBodySpec = restClient
            .post()
            .uri(uriFunction)
            .headers(headers());

        if (bodyValue != null) {
            requestBodySpec.body(bodyValue);
        }

        requestBodySpec.retrieve()
            .toBodilessEntity();
    }

    @Retryable
    public <T> T post(Function<UriBuilder, URI> uriFunction, Object bodyValue, Class<T> responseClass) {
        RestClient.RequestBodySpec requestBodySpec = restClient
            .post()
            .uri(uriFunction)
            .headers(headers());

        if (bodyValue != null) {
            requestBodySpec.body(bodyValue);
        }

        return requestBodySpec.retrieve()
            .body(responseClass);
    }

    @Retryable
    public <T> T post(
        Function<UriBuilder, URI> uriFunction, Object bodyValue, ParameterizedTypeReference<T> responseTypeRef) {

        RestClient.RequestBodySpec requestBodySpec = restClient
            .post()
            .uri(uriFunction)
            .headers(headers());

        if (bodyValue != null) {
            requestBodySpec.body(bodyValue);
        }

        return requestBodySpec.retrieve()
            .body(responseTypeRef);
    }

    @Retryable
    public void put(Function<UriBuilder, URI> uriFunction, Object bodyValue) {
        RestClient.RequestBodySpec requestBodySpec = restClient
            .put()
            .uri(uriFunction)
            .headers(headers());

        if (bodyValue != null) {
            requestBodySpec.body(bodyValue);
        }

        requestBodySpec.retrieve()
            .toBodilessEntity();
    }

    @Retryable
    public <T> T put(Function<UriBuilder, URI> uriFunction, Object bodyValue, Class<T> responseClass) {
        RestClient.RequestBodySpec requestBodySpec = restClient
            .put()
            .uri(uriFunction)
            .headers(headers());

        if (bodyValue != null) {
            requestBodySpec.body(bodyValue);
        }

        return requestBodySpec.retrieve()
            .body(responseClass);
    }

    @Retryable
    public <T> T put(
        Function<UriBuilder, URI> uriFunction, Object bodyValue, ParameterizedTypeReference<T> responseTypeRef) {

        RestClient.RequestBodySpec requestBodySpec = restClient
            .put()
            .uri(uriFunction)
            .headers(headers());

        if (bodyValue != null) {
            requestBodySpec.body(bodyValue);
        }

        return requestBodySpec.retrieve()
            .body(responseTypeRef);
    }
}
