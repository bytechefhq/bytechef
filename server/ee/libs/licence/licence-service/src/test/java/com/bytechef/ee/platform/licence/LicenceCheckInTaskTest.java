/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.licence.LicenceStatus;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LicenceCheckInTask}.
 *
 * @version ee
 */
class LicenceCheckInTaskTest {

    private static final String ACCOUNT_ID = "test-account-123";
    private static final String LICENCE_ID = "lic_checkin_1";

    private final LicenceTestFiles licenceTestFiles = new LicenceTestFiles();
    private final LicenceFileParser parser =
        new LicenceFileParser(new Ed25519Verifier(licenceTestFiles.publicKeyHex()));

    @Test
    @SuppressWarnings("unchecked")
    void testRevokedResponseMarksInvalid() throws Exception {
        OfflineLicenceManager manager = newManagerWithFarFutureExpiry();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);

        doReturn(200).when(response)
            .statusCode();
        doReturn("{\"data\":{\"attributes\":{\"status\":\"REVOKED\"}}}").when(response)
            .body();
        doReturn(response).when(httpClient)
            .send(any(), any());

        ApplicationProperties.Licence properties = buildProperties();
        LicenceCheckInTask task = new LicenceCheckInTask(manager, properties, httpClient);

        task.checkIn();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.INVALID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testBare403LeavesStatusUnchanged() throws Exception {
        OfflineLicenceManager manager = newManagerWithFarFutureExpiry();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);

        doReturn(403).when(response)
            .statusCode();
        doReturn(response).when(httpClient)
            .send(any(), any());

        ApplicationProperties.Licence properties = buildProperties();
        LicenceCheckInTask task = new LicenceCheckInTask(manager, properties, httpClient);

        task.checkIn();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testBare404LeavesStatusUnchanged() throws Exception {
        OfflineLicenceManager manager = newManagerWithFarFutureExpiry();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);

        doReturn(404).when(response)
            .statusCode();
        doReturn(response).when(httpClient)
            .send(any(), any());

        ApplicationProperties.Licence properties = buildProperties();
        LicenceCheckInTask task = new LicenceCheckInTask(manager, properties, httpClient);

        task.checkIn();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testActiveResponseAfterRevokedClearsInvalid() throws Exception {
        OfflineLicenceManager manager = newManagerWithFarFutureExpiry();

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> revokedResponse = mock(HttpResponse.class);

        doReturn(200).when(revokedResponse)
            .statusCode();
        doReturn("{\"data\":{\"attributes\":{\"status\":\"REVOKED\"}}}").when(revokedResponse)
            .body();
        doReturn(revokedResponse).when(httpClient)
            .send(any(), any());

        ApplicationProperties.Licence properties = buildProperties();
        LicenceCheckInTask task = new LicenceCheckInTask(manager, properties, httpClient);

        task.checkIn();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.INVALID);

        HttpResponse<String> activeResponse = mock(HttpResponse.class);

        doReturn(200).when(activeResponse)
            .statusCode();
        doReturn("{\"data\":{\"attributes\":{\"status\":\"ACTIVE\"}}}").when(activeResponse)
            .body();
        doReturn(activeResponse).when(httpClient)
            .send(any(), any());

        task.checkIn();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testNetworkFailureLeavesStatusUnchanged() throws Exception {
        OfflineLicenceManager manager = newManagerWithFarFutureExpiry();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);

        HttpClient httpClient = mock(HttpClient.class);

        doThrow(new IOException("Connection refused")).when(httpClient)
            .send(any(), any());

        ApplicationProperties.Licence properties = buildProperties();
        LicenceCheckInTask task = new LicenceCheckInTask(manager, properties, httpClient);

        task.checkIn();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);
    }

    private OfflineLicenceManager newManagerWithFarFutureExpiry() {
        InMemoryLicenceRepository repository = new InMemoryLicenceRepository();
        OfflineLicenceManager manager = new OfflineLicenceManager(
            parser, repository, Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC), 14);

        byte[] licenceFile = licenceTestFiles.signLicence(LICENCE_ID, "2999-01-01T00:00:00.000Z", 100, "sso");
        manager.upload(licenceFile);

        return manager;
    }

    private static ApplicationProperties.Licence buildProperties() {
        ApplicationProperties.Licence properties = new ApplicationProperties.Licence();

        properties.setAccountId(ACCOUNT_ID);

        return properties;
    }
}
