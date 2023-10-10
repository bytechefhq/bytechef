
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
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
