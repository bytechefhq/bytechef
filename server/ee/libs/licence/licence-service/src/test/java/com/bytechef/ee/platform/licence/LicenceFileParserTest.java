/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.licence.Licence;
import com.bytechef.platform.licence.LicenceException;
import com.bytechef.platform.licence.LicenceFeature;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class LicenceFileParserTest {

    private final LicenceTestFiles licenceTestFiles = new LicenceTestFiles();
    private final LicenceFileParser parser =
        new LicenceFileParser(new Ed25519Verifier(licenceTestFiles.publicKeyHex()));

    @Test
    void testParseValidLicence() {
        String dataset = """
            {"data":{"id":"lic_123","attributes":{"expiry":"2999-01-01T00:00:00.000Z",
            "created":"2026-01-01T00:00:00.000Z","metadata":{"allowedJobs":1000,
            "features":["sso","audit-log","unknown-feature"],"holderName":"Acme",
            "holderEmail":"ops@acme.example","maxUsers":25}}}}""";

        byte[] file = licenceTestFiles.signDataset(dataset);

        Licence licence = parser.parse(file);

        assertThat(licence.id()).isEqualTo("lic_123");
        assertThat(licence.allowedJobs()).isEqualTo(1000L);
        assertThat(licence.holderName()).isEqualTo("Acme");
        assertThat(licence.maxUsers()).isEqualTo(25);
        assertThat(licence.features()).containsExactlyInAnyOrder(LicenceFeature.SSO, LicenceFeature.AUDIT_LOG);
    }

    @Test
    void testParseAllowedJobsAbsentDefaultsUnlimited() {
        String dataset = """
            {"data":{"id":"lic_1","attributes":{"expiry":"2999-01-01T00:00:00.000Z",
            "created":"2026-01-01T00:00:00.000Z","metadata":{"features":[]}}}}""";

        Licence licence = parser.parse(licenceTestFiles.signDataset(dataset));

        assertThat(licence.allowedJobs()).isEqualTo(-1L);
    }

    @Test
    void testParseRejectsTamperedSignature() {
        String dataset = "{\"data\":{\"id\":\"x\",\"attributes\":{\"expiry\":\"2999-01-01T00:00:00.000Z\"," +
            "\"created\":\"2026-01-01T00:00:00.000Z\",\"metadata\":{}}}}";

        byte[] file = licenceTestFiles.signDataset(dataset);
        // flip a byte inside the armored payload
        file[file.length / 2] ^= 0x01;

        assertThatThrownBy(() -> parser.parse(file)).isInstanceOf(LicenceException.class);
    }

    @Test
    void testParseRejectsInvalidSignatureWithValidEnvelope() {
        String dataset = """
            {"data":{"id":"sig_test","attributes":{"expiry":"2999-01-01T00:00:00.000Z",
            "created":"2026-01-01T00:00:00.000Z","metadata":{}}}}""";

        byte[] file = licenceTestFiles.signDatasetWithCorruptedSignature(dataset);

        assertThatThrownBy(() -> parser.parse(file)).isInstanceOf(LicenceException.class);
    }

    @Test
    void testParseAllowedJobsNumericString() {
        String dataset = """
            {"data":{"id":"lic_str","attributes":{"expiry":"2999-01-01T00:00:00.000Z",
            "created":"2026-01-01T00:00:00.000Z","metadata":{"allowedJobs":"500"}}}}""";

        Licence licence = parser.parse(licenceTestFiles.signDataset(dataset));

        assertThat(licence.allowedJobs()).isEqualTo(500L);
    }
}
