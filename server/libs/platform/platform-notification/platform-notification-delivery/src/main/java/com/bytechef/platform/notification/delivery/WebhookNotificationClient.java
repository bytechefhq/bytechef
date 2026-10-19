/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.notification.delivery;

import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.commons.util.UrlValidatorUtils;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Ivica Cardic
 */
@Component
public class WebhookNotificationClient {

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RestTemplate restTemplate;

    public WebhookNotificationClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);

        this.restTemplate = new RestTemplate(requestFactory);
    }

    public record WebhookRetry(int maxRetries, Duration initialInterval, double multiplier) {

        public static WebhookRetry none() {
            return new WebhookRetry(0, Duration.ofSeconds(2), 2.0);
        }
    }

    public void deliver(WebhookDeliveryRequest webhookDeliveryRequest) {
        deliver(webhookDeliveryRequest, WebhookRetry.none());
    }

    public void deliver(WebhookDeliveryRequest webhookDeliveryRequest, WebhookRetry webhookRetry) {
        String url = webhookDeliveryRequest.url();

        try {
            UrlValidatorUtils.validate(url, Set.of());
        } catch (UrlValidationException urlValidationException) {
            throw new IllegalArgumentException(urlValidationException.getMessage(), urlValidationException);
        }

        long timestamp = System.currentTimeMillis();

        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-ByteChef-Event", webhookDeliveryRequest.eventType());
        httpHeaders.set("X-ByteChef-Timestamp", String.valueOf(timestamp));
        httpHeaders.set("X-ByteChef-Delivery", String.valueOf(UUID.randomUUID()));

        for (Map.Entry<String, String> header : webhookDeliveryRequest.headers()
            .entrySet()) {

            httpHeaders.set(header.getKey(), header.getValue());
        }

        String secret = webhookDeliveryRequest.secret();

        if (secret != null && !secret.isBlank()) {
            httpHeaders.set(
                "X-ByteChef-Signature",
                "t=" + timestamp + ",v1=" + sign(secret, timestamp + "." + webhookDeliveryRequest.payloadJson()));
        }

        post(url, new HttpEntity<>(webhookDeliveryRequest.payloadJson(), httpHeaders), webhookRetry);
    }

    public void deliverEvent(String url, Object payload, WebhookRetry webhookRetry) {
        post(url, new HttpEntity<>(payload), webhookRetry);
    }

    private void post(String url, HttpEntity<?> httpEntity, WebhookRetry webhookRetry) {
        try {
            if (webhookRetry.maxRetries() <= 0) {
                exchange(url, httpEntity);
            } else {
                RetryTemplate retryTemplate = new RetryTemplate(
                    RetryPolicy.builder()
                        .maxRetries(webhookRetry.maxRetries())
                        .delay(webhookRetry.initialInterval())
                        .multiplier(webhookRetry.multiplier())
                        .build());

                retryTemplate.execute(() -> {
                    exchange(url, httpEntity);

                    return null;
                });
            }
        } catch (RetryException retryException) {
            throw new WebhookDeliveryException("Webhook delivery to " + url + " failed after retries", retryException);
        } catch (RestClientResponseException restClientResponseException) {
            throw new WebhookDeliveryException(
                "Webhook delivery to " + url + " returned HTTP " + restClientResponseException.getStatusCode()
                    .value(),
                restClientResponseException.getStatusCode()
                    .value());
        } catch (org.springframework.web.client.RestClientException restClientException) {
            throw new WebhookDeliveryException("Failed to deliver webhook to " + url, restClientException);
        }
    }

    private void exchange(String url, HttpEntity<?> httpEntity) {
        restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);
    }

    static String sign(String secret, String signedContent) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);

            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));

            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to compute webhook signature", exception);
        }
    }
}
