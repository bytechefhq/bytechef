
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public <T> T get(Function<UriBuilder, URI> uriFunction, Class<T> elementClass) {
        return webClientBuilder
            .build()
            .get()
            .uri(uriFunction)
            .retrieve()
            .bodyToMono(elementClass)
            .block();
    }

    @Retryable
    public <T> T get(Function<UriBuilder, URI> uriFunction, ParameterizedTypeReference<T> elementTypeRef) {
        return webClientBuilder
            .build()
            .get()
            .uri(uriFunction)
            .retrieve()
            .bodyToMono(elementTypeRef)
            .block();
    }

    @Retryable
    public <T> Mono<T> getMono(Function<UriBuilder, URI> uriFunction, Class<T> elementClass) {
        return webClientBuilder
            .build()
            .get()
            .uri(uriFunction)
            .retrieve()
            .bodyToMono(elementClass);
    }

    @Retryable
    public <T> Mono<T> getMono(Function<UriBuilder, URI> uriFunction, ParameterizedTypeReference<T> elementTypeRef) {
        return webClientBuilder
            .build()
            .get()
            .uri(uriFunction)
            .retrieve()
            .bodyToMono(elementTypeRef);
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
    public <T> T post(Function<UriBuilder, URI> uriFunction, Object bodyValue, Class<T> elementClass) {
        WebClient.RequestBodySpec requestBodySpec = webClientBuilder
            .build()
            .post()
            .uri(uriFunction);

        if (bodyValue != null) {
            requestBodySpec.bodyValue(bodyValue);
        }

        return requestBodySpec.retrieve()
            .bodyToMono(elementClass)
            .block();
    }

    @Retryable
    public <T> T post(
        Function<UriBuilder, URI> uriFunction, Object bodyValue, ParameterizedTypeReference<T> elementTypeRef) {

        WebClient.RequestBodySpec requestBodySpec = webClientBuilder
            .build()
            .post()
            .uri(uriFunction);

        if (bodyValue != null) {
            requestBodySpec.bodyValue(bodyValue);
        }

        return requestBodySpec.retrieve()
            .bodyToMono(elementTypeRef)
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
    public <T> T put(Function<UriBuilder, URI> uriFunction, Object bodyValue, Class<T> elementClass) {
        WebClient.RequestBodySpec requestBodySpec = webClientBuilder
            .build()
            .put()
            .uri(uriFunction);

        if (bodyValue != null) {
            requestBodySpec.bodyValue(bodyValue);
        }

        return requestBodySpec.retrieve()
            .bodyToMono(elementClass)
            .block();
    }

    @Retryable
    public <T> T put(
        Function<UriBuilder, URI> uriFunction, Object bodyValue, ParameterizedTypeReference<T> elementTypeRef) {

        WebClient.RequestBodySpec requestBodySpec = webClientBuilder
            .build()
            .put()
            .uri(uriFunction);

        if (bodyValue != null) {
            requestBodySpec.bodyValue(bodyValue);
        }

        return requestBodySpec.retrieve()
            .bodyToMono(elementTypeRef)
            .block();
    }
}
