
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.commons.webclient;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Function;

public class AbstractWebClient {

    private final WebClient.Builder webClientBuilder;

    public AbstractWebClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Retryable
    public void delete(Function<UriBuilder, URI> uriFunction) {
        webClientBuilder
            .build()
            .delete()
            .uri(uriFunction)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @Retryable
    public void get(Function<UriBuilder, URI> uriFunction) {
        webClientBuilder
            .build()
            .get()
            .uri(uriFunction)
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @Retryable
    public <T> T get(Function<UriBuilder, URI> uriFunction, Class<T> responseClass) {
        return webClientBuilder
            .build()
            .get()
            .uri(uriFunction)
            .retrieve()
            .bodyToMono(responseClass)
            .block();
    }

    @Retryable
    public <T> T get(Function<UriBuilder, URI> uriFunction, ParameterizedTypeReference<T> responseTypeRef) {
        return webClientBuilder
            .build()
            .get()
            .uri(uriFunction)
            .retrieve()
            .bodyToMono(responseTypeRef)
            .block();
    }

    @Retryable
    public <T> Mono<T> getMono(Function<UriBuilder, URI> uriFunction, Class<T> responseClass) {
        return webClientBuilder
            .build()
            .get()
            .uri(uriFunction)
            .retrieve()
            .bodyToMono(responseClass);
    }

    @Retryable
    public <T> Mono<T> getMono(Function<UriBuilder, URI> uriFunction, ParameterizedTypeReference<T> responseTypeRef) {
        return webClientBuilder
            .build()
            .get()
            .uri(uriFunction)
            .retrieve()
            .bodyToMono(responseTypeRef);
    }

    @Retryable
    public void post(Function<UriBuilder, URI> uriFunction, Object bodyValue) {
        WebClient.RequestBodySpec requestBodySpec = webClientBuilder
            .build()
            .post()
            .uri(uriFunction);

        if (bodyValue != null) {
            requestBodySpec.bodyValue(bodyValue);
        }

        requestBodySpec.retrieve()
            .toBodilessEntity()
            .block();
    }

    @Retryable
    public <T> T post(Function<UriBuilder, URI> uriFunction, Object bodyValue, Class<T> responseClass) {
        WebClient.RequestBodySpec requestBodySpec = webClientBuilder
            .build()
            .post()
            .uri(uriFunction);

        if (bodyValue != null) {
            requestBodySpec.bodyValue(bodyValue);
        }

        return requestBodySpec.retrieve()
            .bodyToMono(responseClass)
            .block();
    }

    @Retryable
    public <T> T post(
        Function<UriBuilder, URI> uriFunction, Object bodyValue, ParameterizedTypeReference<T> responseTypeRef) {

        WebClient.RequestBodySpec requestBodySpec = webClientBuilder
            .build()
            .post()
            .uri(uriFunction);

        if (bodyValue != null) {
            requestBodySpec.bodyValue(bodyValue);
        }

        return requestBodySpec.retrieve()
            .bodyToMono(responseTypeRef)
            .block();
    }

    @Retryable
    public void put(Function<UriBuilder, URI> uriFunction, Object bodyValue) {
        WebClient.RequestBodySpec requestBodySpec = webClientBuilder
            .build()
            .put()
            .uri(uriFunction);

        if (bodyValue != null) {
            requestBodySpec.bodyValue(bodyValue);
        }

        requestBodySpec.retrieve()
            .toBodilessEntity()
            .block();
    }

    @Retryable
    public <T> T put(Function<UriBuilder, URI> uriFunction, Object bodyValue, Class<T> responseClass) {
        WebClient.RequestBodySpec requestBodySpec = webClientBuilder
            .build()
            .put()
            .uri(uriFunction);

        if (bodyValue != null) {
            requestBodySpec.bodyValue(bodyValue);
        }

        return requestBodySpec.retrieve()
            .bodyToMono(responseClass)
            .block();
    }

    @Retryable
    public <T> T put(
        Function<UriBuilder, URI> uriFunction, Object bodyValue, ParameterizedTypeReference<T> responseTypeRef) {

        WebClient.RequestBodySpec requestBodySpec = webClientBuilder
            .build()
            .put()
            .uri(uriFunction);

        if (bodyValue != null) {
            requestBodySpec.bodyValue(bodyValue);
        }

        return requestBodySpec.retrieve()
            .bodyToMono(responseTypeRef)
            .block();
    }
}
