/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.licence.config.LicenceIntTestConfiguration;
import com.bytechef.ee.platform.licence.repository.LicenceRepository;
import com.bytechef.platform.licence.LicenceStatus;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Clock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test: verifies that {@link OfflineLicenceManager} persists a licence to a real PostgreSQL database via
 * {@link LicenceRepository} and that a freshly-constructed manager reloads the persisted licence as
 * {@link LicenceStatus#VALID}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = LicenceIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, OfflineLicenceManagerPersistenceIntTest.TestLicenceBeans.class
})
public class OfflineLicenceManagerPersistenceIntTest {

    /**
     * Shared test keypair used by both the {@link Ed25519Verifier} bean and the test method. Static so the same keypair
     * is available during Spring context construction (for the bean definitions) and during test execution (for signing
     * the licence file).
     */
    private static final LicenceTestFiles LICENCE_TEST_FILES = new LicenceTestFiles();

    @Autowired
    private LicenceFileParser licenceFileParser;

    @Autowired
    private LicenceRepository licenceRepository;

    @Autowired
    private OfflineLicenceManager offlineLicenceManager;

    @AfterEach
    public void afterEach() {
        licenceRepository.deleteAll();
    }

    @Test
    public void testUploadPersistsAndReloads() {
        byte[] licenceFileBytes = LICENCE_TEST_FILES.signLicence("lic_int", "2999-01-01T00:00:00.000Z", 100, "sso");

        offlineLicenceManager.upload(licenceFileBytes);

        assertThat(licenceRepository.count()).isEqualTo(1L);

        OfflineLicenceManager reloadedManager = new OfflineLicenceManager(
            licenceFileParser, licenceRepository, Clock.systemUTC(), 14);

        assertThat(reloadedManager.getStatus()).isEqualTo(LicenceStatus.VALID);
    }

    /**
     * Provides the licence beans wired to the test keypair so that test-signed licence files pass Ed25519 verification.
     *
     * @version ee
     */
    @TestConfiguration
    static class TestLicenceBeans {

        @Bean
        Ed25519Verifier ed25519Verifier() {
            return new Ed25519Verifier(LICENCE_TEST_FILES.publicKeyHex());
        }

        @Bean
        LicenceFileParser licenceFileParser(Ed25519Verifier ed25519Verifier) {
            return new LicenceFileParser(ed25519Verifier);
        }

        @Bean
        OfflineLicenceManager offlineLicenceManager(
            LicenceFileParser licenceFileParser, LicenceRepository licenceRepository) {

            return new OfflineLicenceManager(licenceFileParser, licenceRepository, Clock.systemUTC(), 14);
        }
    }
}
