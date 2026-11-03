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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

/**
 * @author Ivica Cardic
 */
@Component
public class WebhookNotificationClient {

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int MAX_BODY_BYTES = 4000;
    private static final int MAX_BODY_CHARACTERS = 1000;

    private final RestTemplate restTemplate;

    public WebhookNotificationClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);

        this.restTemplate = new RestTemplate(requestFactory);

        this.restTemplate.setErrorHandler(new ResponseErrorHandler() {

            @Override
            public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
                HttpStatusCode statusCode = clientHttpResponse.getStatusCode();

                return !statusCode.is2xxSuccessful();
            }

            @Override
            public void handleError(URI requestUri, HttpMethod httpMethod, ClientHttpResponse clientHttpResponse)
                throws IOException {

                HttpStatusCode statusCode = clientHttpResponse.getStatusCode();

                String responseBody = readBody(clientHttpResponse);

                throw new RestClientResponseException(
                    "HTTP " + statusCode.value(), statusCode, clientHttpResponse.getStatusText(),
                    clientHttpResponse.getHeaders(), responseBody == null ? null : responseBody.getBytes(
                        StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8);
            }
        });
    }

    public WebhookResponse post(WebhookDelivery webhookDelivery, WebhookPayload webhookPayload) {
        URI uri = toUri(restTemplate, webhookDelivery.url());
        String host = uri.getHost();
        String payloadJson = webhookPayload.json();

        HttpHeaders httpHeaders = new HttpHeaders();

        for (Map.Entry<String, String> header : webhookDelivery.headers()
            .entrySet()) {

            if (!WebhookReservedHeaders.isReserved(header.getKey())) {
                httpHeaders.set(header.getKey(), header.getValue());
            }
        }

        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        Instant suppliedTimestamp = webhookDelivery.timestamp();
        String suppliedDeliveryId = webhookDelivery.deliveryId();

        long timestamp = suppliedTimestamp == null ? System.currentTimeMillis() : suppliedTimestamp.toEpochMilli();

        if (webhookDelivery.eventType() != null) {
            httpHeaders.set("X-ByteChef-Event", webhookDelivery.eventType());
            httpHeaders.set("X-ByteChef-Timestamp", String.valueOf(timestamp));
            httpHeaders.set("X-ByteChef-Delivery",
                suppliedDeliveryId == null ? String.valueOf(UUID.randomUUID()) : suppliedDeliveryId);
        }

        String secret = webhookDelivery.secret();

        if (secret != null && !secret.isBlank()) {
            httpHeaders.set(
                "X-ByteChef-Signature", "t=" + timestamp + ",v1=" + sign(secret, timestamp + "." + payloadJson));
        }

        try {
            RequestCallback requestCallback = clientHttpRequest -> {
                clientHttpRequest.getHeaders()
                    .putAll(httpHeaders);

                clientHttpRequest.getBody()
                    .write(payloadJson.getBytes(StandardCharsets.UTF_8));
            };
            ResponseExtractor<WebhookResponse> responseExtractor = clientHttpResponse -> new WebhookResponse(
                clientHttpResponse.getStatusCode()
                    .value(),
                readBody(clientHttpResponse));

            WebhookResponse webhookResponse = restTemplate.execute(
                uri, HttpMethod.POST, requestCallback, responseExtractor);

            if (webhookResponse == null) {
                throw new WebhookDeliveryException("Failed to deliver webhook to " + host, null, null);
            }

            return webhookResponse;
        } catch (RestClientResponseException restClientResponseException) {
            int statusCode = restClientResponseException.getStatusCode()
                .value();

            String responseBody = restClientResponseException.getResponseBodyAsByteArray().length == 0
                ? null : restClientResponseException.getResponseBodyAsString(StandardCharsets.UTF_8);

            throw new WebhookDeliveryException(
                "Webhook delivery to " + host + " returned HTTP " + statusCode, statusCode,
                restClientResponseException, responseBody);
        } catch (RestClientException restClientException) {
            throw new WebhookDeliveryException("Failed to deliver webhook to " + host, null, restClientException);
        }
    }

    private static @Nullable String readBody(ClientHttpResponse clientHttpResponse) throws IOException {
        byte[] bodyBytes;

        try (InputStream inputStream = clientHttpResponse.getBody()) {
            bodyBytes = inputStream.readNBytes(MAX_BODY_BYTES);
        }

        if (bodyBytes.length == 0) {
            return null;
        }

        String body = new String(bodyBytes, StandardCharsets.UTF_8);

        if (body.length() <= MAX_BODY_CHARACTERS) {
            return body;
        }

        int end = MAX_BODY_CHARACTERS;

        if (Character.isHighSurrogate(body.charAt(end - 1))) {
            end--;
        }

        return body.substring(0, end) + "…";
    }

    private static URI toUri(RestTemplate restTemplate, String url) {
        UriTemplateHandler uriTemplateHandler = restTemplate.getUriTemplateHandler();

        URI uri;

        try {
            uri = uriTemplateHandler.expand(url);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new InvalidWebhookUrlException("Webhook URL is not a valid URI", exception);
        }

        String scheme = uri.getScheme();

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new InvalidWebhookUrlException("Webhook URL must use http or https", null);
        }

        if (uri.getHost() == null) {
            throw new InvalidWebhookUrlException("Webhook URL has no host", null);
        }

        return uri;
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

    /**
     * @param statusCode the HTTP status of the receiver
     * @param body       the response body, capped at 1000 characters, or {@code null} when empty
     */
    public record WebhookResponse(int statusCode, @Nullable String body) {
    }
}
