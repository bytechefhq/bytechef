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
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Self-hosted licence checker that queries Keygen for the number of allowed task executions.
 *
 * Configuration (System properties or environment variables are supported): - licence.keygen.baseUrl (default:
 * https://api.keygen.sh/v1) - licence.keygen.accountId (required) - licence.keygen.licenseKey (required) -
 * licence.keygen.apiToken (optional, Bearer token) - licence.keygen.allowedTasksMetadataKey (default: allowedTasks)
 *
 * Returns -1 on any error or when configuration is incomplete.
 */
public class SelfHostedLicenceChecker implements LicenceChecker {

    private static final Logger log = LoggerFactory.getLogger(SelfHostedLicenceChecker.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public int getAllowedTasks() {
        String baseUrl = prop("licence.keygen.baseUrl", env("LICENCE_KEYGEN_BASEURL", "https://api.keygen.sh/v1"));
        String accountId = prop("licence.keygen.accountId", env("LICENCE_KEYGEN_ACCOUNTID", null));
        String licenseKey = prop("licence.keygen.licenseKey", env("LICENCE_KEYGEN_LICENSEKEY", null));
        String apiToken = prop("licence.keygen.apiToken", env("LICENCE_KEYGEN_APITOKEN", null));
        String metadataKey = prop("licence.keygen.allowedTasksMetadataKey",
            env("LICENCE_KEYGEN_ALLOWEDTASKS_METADATAKEY", "allowedTasks"));

        if (isBlank(accountId) || isBlank(licenseKey)) {
            log.debug("Keygen configuration incomplete (accountId or licenseKey missing); returning -1");
            return -1;
        }

        try {
            // Minimal GET to license resource; if the license key is sensitive, ensure proper usage.
            // This uses a straightforward endpoint shape and may need adjustment per actual Keygen setup.
            String url = String.format("%s/accounts/%s/licenses/%s", trimSlash(baseUrl), accountId, licenseKey);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

            if (!isBlank(apiToken)) {
                builder.header("Authorization", "Bearer " + apiToken);
            }

            builder.header("Accept", "application/json");

            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = MAPPER.readTree(response.body());
                JsonNode valNode = null;

                // Try data.attributes.metadata.allowedTasks
                if (root.has("data")) {
                    JsonNode data = root.get("data");
                    JsonNode attrs = data.get("attributes");
                    if (attrs != null) {
                        JsonNode meta = attrs.get("metadata");
                        if (meta != null) {
                            valNode = meta.get(metadataKey);
                        }
                    }
                }

                if (valNode != null && valNode.isTextual()) {
                    return parseIntOrDefault(valNode.asText(), -1);
                } else if (valNode != null && valNode.isNumber()) {
                    return valNode.asInt();
                }
            } else {
                log.warn("Keygen request failed: status={} body={}", response.statusCode(), truncate(response.body()));
            }
        } catch (IOException | InterruptedException e) {
            log.warn("Keygen request error: {}", e.toString());
            Thread.currentThread()
                .interrupt();
        } catch (Exception e) {
            log.warn("Keygen unexpected error: {}", e.toString());
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
