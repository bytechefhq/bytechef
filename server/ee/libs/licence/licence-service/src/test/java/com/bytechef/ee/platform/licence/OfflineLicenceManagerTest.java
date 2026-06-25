/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.licence.domain.LicenceEntity;
import com.bytechef.platform.licence.Licence;
import com.bytechef.platform.licence.LicenceException;
import com.bytechef.platform.licence.LicenceFeature;
import com.bytechef.platform.licence.LicenceStatus;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class OfflineLicenceManagerTest {

    private final LicenceTestFiles licenceTestFiles = new LicenceTestFiles();
    private final LicenceFileParser parser =
        new LicenceFileParser(new Ed25519Verifier(licenceTestFiles.publicKeyHex()));

    @Test
    void testUploadValidLicenceBecomesValid() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));

        byte[] file = licenceTestFiles.signLicence("lic_1", "2999-01-01T00:00:00.000Z", 100, "sso");

        Licence licence = manager.upload(file);

        assertThat(licence.allowedJobs()).isEqualTo(100L);
        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);
        assertThat(manager.isFeatureEnabled(LicenceFeature.SSO)).isTrue();
        assertThat(manager.isFeatureEnabled(LicenceFeature.AUDIT_LOG)).isFalse();
        assertThat(manager.getAllowedJobs()).isEqualTo(100L);
    }

    @Test
    void testExpiredWithinGraceIsGrace() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));
        // expired 5 days ago, grace 14 days -> GRACE
        manager.upload(licenceTestFiles.signLicence("lic_2", "2026-06-19T00:00:00.000Z", 100, "sso"));

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.GRACE);
        assertThat(manager.isFeatureEnabled(LicenceFeature.SSO)).isTrue();
    }

    @Test
    void testExpiredPastGraceIsExpired() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));
        // expired 30 days ago, past 14-day grace -> EXPIRED
        manager.upload(licenceTestFiles.signLicence("lic_3", "2026-05-25T00:00:00.000Z", 100, "sso"));

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.EXPIRED);
        assertThat(manager.isFeatureEnabled(LicenceFeature.SSO)).isFalse();
    }

    @Test
    void testMissingWhenNoLicence() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.MISSING);
        assertThat(manager.getAllowedJobs()).isEqualTo(-1L);
    }

    @Test
    void testCheckFeatureThrowsWhenNotEntitled() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));
        manager.upload(licenceTestFiles.signLicence("lic_4", "2999-01-01T00:00:00.000Z", 100, "sso"));

        assertThatThrownBy(() -> manager.checkFeature(LicenceFeature.AUDIT_LOG))
            .isInstanceOf(LicenceException.class);
    }

    @Test
    void testDeleteResetsToMissing() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));
        manager.upload(licenceTestFiles.signLicence("lic_5", "2999-01-01T00:00:00.000Z", 100, "sso"));

        manager.delete();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.MISSING);
    }

    @Test
    void testReloadValidPersistedLicenceIsValid() {
        InMemoryLicenceRepository repository = new InMemoryLicenceRepository();

        byte[] licenceFile = licenceTestFiles.signLicence("lic_reload_1", "2999-01-01T00:00:00.000Z", 50, "sso");

        LicenceEntity entity = new LicenceEntity();
        entity.setRawFile(new String(licenceFile, StandardCharsets.UTF_8));

        repository.save(entity);

        OfflineLicenceManager manager = new OfflineLicenceManager(
            parser, repository, Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC), 14);

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);
        assertThat(manager.isFeatureEnabled(LicenceFeature.SSO)).isTrue();
        assertThat(manager.getAllowedJobs()).isEqualTo(50L);
    }

    @Test
    void testReloadCorruptPersistedLicenceIsInvalid() {
        InMemoryLicenceRepository repository = new InMemoryLicenceRepository();

        LicenceEntity entity = new LicenceEntity();
        entity.setRawFile("not a licence");

        repository.save(entity);

        OfflineLicenceManager manager = new OfflineLicenceManager(
            parser, repository, Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC), 14);

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.INVALID);
    }

    @Test
    void testBootstrapFromInlineContentsSeedsRepository() {
        InMemoryLicenceRepository repository = new InMemoryLicenceRepository();
        OfflineLicenceManager manager = new OfflineLicenceManager(
            parser, repository, Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC), 14);

        String contents = new String(
            licenceTestFiles.signLicence("lic_boot", "2999-01-01T00:00:00.000Z", 50, "sso"),
            StandardCharsets.UTF_8);

        manager.bootstrap(null, contents);

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);
        assertThat(repository.count()).isEqualTo(1L);
    }

    @Test
    void testBootstrapNoOpWhenLicencePresent() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));
        manager.upload(licenceTestFiles.signLicence("lic_existing", "2999-01-01T00:00:00.000Z", 10, "sso"));

        manager.bootstrap(null, new String(
            licenceTestFiles.signLicence("lic_other", "2999-01-01T00:00:00.000Z", 999, "audit-log"),
            StandardCharsets.UTF_8));

        assertThat(manager.getAllowedJobs()).isEqualTo(10L);
    }

    @Test
    void testBootstrapNoOpWhenRepositoryAlreadyHasRow() {
        InMemoryLicenceRepository repository = new InMemoryLicenceRepository();

        byte[] originalFile = licenceTestFiles.signLicence("lic_pre", "2999-01-01T00:00:00.000Z", 42, "sso");
        LicenceEntity entity = new LicenceEntity();
        entity.setRawFile(new String(originalFile, StandardCharsets.UTF_8));
        repository.save(entity);

        OfflineLicenceManager manager = new OfflineLicenceManager(
            parser, repository, Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC), 14);

        String bootstrapContents = new String(
            licenceTestFiles.signLicence("lic_bootstrap", "2999-01-01T00:00:00.000Z", 999, "audit-log"),
            StandardCharsets.UTF_8);

        manager.bootstrap(null, bootstrapContents);

        assertThat(repository.count()).isEqualTo(1L);
        assertThat(manager.getAllowedJobs()).isEqualTo(42L);
    }

    private OfflineLicenceManager newManager(Instant now) {
        return new OfflineLicenceManager(
            parser, new InMemoryLicenceRepository(), Clock.fixed(now, ZoneOffset.UTC), 14);
    }
}
