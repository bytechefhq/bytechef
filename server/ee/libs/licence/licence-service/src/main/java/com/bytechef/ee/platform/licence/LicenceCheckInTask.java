/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.licence.Licence;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Optional online check-in that flips the cached licence to INVALID when Keygen explicitly reports it revoked,
 * suspended, expired, or banned. Disabled by default; any network failure or non-parseable HTTP error is non-fatal —
 * the offline status is preserved so air-gapped deployments and transient auth blips are unaffected.
 *
 * <p>
 * Recovery: a subsequent check-in that returns an explicit ACTIVE status clears the invalid flag, allowing the instance
 * to recover without a restart or re-upload.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class LicenceCheckInTask {

    private static final Logger log = LoggerFactory.getLogger(LicenceCheckInTask.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Set<String> REVOKED_STATUSES = Set.of("SUSPENDED", "REVOKED", "EXPIRED", "BANNED");

    private final OfflineLicenceManager licenceManager;
    private final ApplicationProperties.Licence licenceProperties;
    private final HttpClient httpClient;

    @SuppressFBWarnings("EI2")
    public LicenceCheckInTask(
        OfflineLicenceManager licenceManager, ApplicationProperties.Licence licenceProperties,
        HttpClient httpClient) {

        this.licenceManager = licenceManager;
        this.licenceProperties = licenceProperties;
        this.httpClient = httpClient;
    }

    public void checkIn() {
        Optional<Licence> current = licenceManager.getLicence();

        if (current.isEmpty()) {
            return;
        }

        String accountId = licenceProperties.getAccountId();
        String licenceId = current.get()
            .id();

        if (accountId == null || accountId.isBlank() || licenceId == null || licenceId.isBlank()) {
            return;
        }

        try {
            String url = "https://api.keygen.sh/v1/accounts/%s/licenses/%s".formatted(accountId, licenceId);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {
                // Non-2xx without a parseable revoked status in the body is treated as a transient error.
                // A bare 403/404 (auth blip, path drift) must not permanently hard-block a valid instance.
                log.warn(
                    "Licence check-in returned HTTP {} — treating as transient, offline status preserved", statusCode);

                return;
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            JsonNode statusNode = root.path("data")
                .path("attributes")
                .path("status");

            String licenceStatus = statusNode.asString(null);

            if (licenceStatus != null && REVOKED_STATUSES.contains(licenceStatus)) {
                licenceManager.markInvalid();
            } else if ("ACTIVE".equalsIgnoreCase(licenceStatus)) {
                licenceManager.clearInvalid();
            }
        } catch (Exception exception) {
            log.warn("Licence check-in failed (offline status preserved): {}", exception.getMessage());
        }
    }
}
