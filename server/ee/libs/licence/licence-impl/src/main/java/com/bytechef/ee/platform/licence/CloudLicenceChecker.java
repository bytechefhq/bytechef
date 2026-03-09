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

package com.bytechef.ee.platform.licence;

import com.bytechef.platform.licence.LicenceChecker;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Cloud licence checker that queries Stripe for the number of allowed task executions.
 *
 * Configuration (System properties or environment variables are supported): - licence.stripe.apiKey (required, secret
 * key) - licence.stripe.subscriptionId (optional) - licence.stripe.customerId (optional, used if subscriptionId is not
 * provided) - licence.stripe.allowedTasksMetadataKey (default: allowedTasks) - licence.stripe.baseUrl (default:
 * https://api.stripe.com)
 *
 * The checker reads the metadata key from the selected Stripe object (subscription or customer). Returns -1 on any
 * error or when configuration is incomplete.
 */
public class CloudLicenceChecker implements LicenceChecker {

    private static final Logger log = LoggerFactory.getLogger(CloudLicenceChecker.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public int getAllowedTasks() {
        String apiKey = prop("licence.stripe.apiKey", env("LICENCE_STRIPE_APIKEY", null));
        String subscriptionId = prop("licence.stripe.subscriptionId", env("LICENCE_STRIPE_SUBSCRIPTIONID", null));
        String customerId = prop("licence.stripe.customerId", env("LICENCE_STRIPE_CUSTOMERID", null));
        String metadataKey = prop("licence.stripe.allowedTasksMetadataKey",
            env("LICENCE_STRIPE_ALLOWEDTASKS_METADATAKEY", "allowedTasks"));
        String baseUrl = prop("licence.stripe.baseUrl", env("LICENCE_STRIPE_BASEURL", "https://api.stripe.com"));

        if (isBlank(apiKey)) {
            log.debug("Stripe API key missing; returning -1");
            return -1;
        }

        String path = null;
        if (!isBlank(subscriptionId)) {
            path = "/v1/subscriptions/" + subscriptionId;
        } else if (!isBlank(customerId)) {
            path = "/v1/customers/" + customerId;
        } else {
            log.debug("Neither subscriptionId nor customerId provided; returning -1");
            return -1;
        }

        try {
            String url = trimSlash(baseUrl) + path;

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey);

            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = MAPPER.readTree(response.body());
                JsonNode metadata = root.get("metadata");
                if (metadata != null) {
                    JsonNode valNode = metadata.get(metadataKey);
                    if (valNode != null && valNode.isTextual()) {
                        return parseIntOrDefault(valNode.asText(), -1);
                    } else if (valNode != null && valNode.isNumber()) {
                        return valNode.asInt();
                    }
                }
            } else {
                log.warn("Stripe request failed: status={} body={}", response.statusCode(), truncate(response.body()));
            }
        } catch (Exception e) {
            log.warn("Stripe request error: {}", e.toString());
        }

        return -1;
    }

    private static String prop(String key, String def) {
        return System.getProperty(key, def);
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v != null ? v : def;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignored) {
            return def;
        }
    }

    private static String trimSlash(String url) {
        if (url == null)
            return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String truncate(String s) {
        if (s == null)
            return null;
        return s.length() > 500 ? s.substring(0, 500) + "…" : s;
    }
}
