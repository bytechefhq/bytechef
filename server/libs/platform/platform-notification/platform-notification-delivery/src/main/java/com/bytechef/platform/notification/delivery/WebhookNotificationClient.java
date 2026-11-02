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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;
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

                throw new RestClientResponseException(
                    "HTTP " + statusCode.value(), statusCode, clientHttpResponse.getStatusText(),
                    clientHttpResponse.getHeaders(), null, null);
            }
        });
    }

    public void post(WebhookDelivery webhookDelivery, WebhookPayload webhookPayload) {
        URI uri = toUri(restTemplate, webhookDelivery.url());
        String host = uri.getHost();
        String payloadJson = webhookPayload.json();

        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        long timestamp = System.currentTimeMillis();

        if (webhookDelivery.eventType() != null) {
            httpHeaders.set("X-ByteChef-Event", webhookDelivery.eventType());
            httpHeaders.set("X-ByteChef-Timestamp", String.valueOf(timestamp));
            httpHeaders.set("X-ByteChef-Delivery", String.valueOf(UUID.randomUUID()));
        }

        for (Map.Entry<String, String> header : webhookDelivery.headers()
            .entrySet()) {

            httpHeaders.set(header.getKey(), header.getValue());
        }

        String secret = webhookDelivery.secret();

        if (secret != null && !secret.isBlank()) {
            httpHeaders.set(
                "X-ByteChef-Signature", "t=" + timestamp + ",v1=" + sign(secret, timestamp + "." + payloadJson));
        }

        try {
            restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(payloadJson, httpHeaders), String.class);
        } catch (RestClientResponseException restClientResponseException) {
            int statusCode = restClientResponseException.getStatusCode()
                .value();

            throw new WebhookDeliveryException(
                "Webhook delivery to " + host + " returned HTTP " + statusCode, statusCode,
                restClientResponseException);
        } catch (RestClientException restClientException) {
            throw new WebhookDeliveryException("Failed to deliver webhook to " + host, null, restClientException);
        }
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
}
