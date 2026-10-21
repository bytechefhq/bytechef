/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppInstanceRepositoryIntTest {

    @Autowired
    private AppInstanceRepository appInstanceRepository;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private App app;

    @BeforeEach
    void beforeEach() {
        app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard"));
    }

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void testSavesAndReadsBackEnumsKeyAndConfigurationColumns() {
        AppInstance appInstance = newAppInstance(app.getId(), "Croatia", "a".repeat(64));

        appInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);
        appInstance.setRegisteredClientId("client-1");
        appInstance.setConfiguration(Map.of("region", "hr"));
        appInstance.setDeploymentConnectionId(7L);
        appInstance.setDeploymentSettings(Map.of("project", "crm"));

        AppInstance saved = appInstanceRepository.save(appInstance);

        jdbcTemplate.update(
            "INSERT INTO app_version (id, app_id, name, created_by, created_date) VALUES (9, ?, '1.0', 'system', " +
                "CURRENT_TIMESTAMP)",
            app.getId());
        jdbcTemplate.update(
            "INSERT INTO app_deployment (id, app_instance_id, app_version_id, provider, status, created_by, " +
                "created_date) VALUES (9, ?, 9, 1, 2, 'system', CURRENT_TIMESTAMP)",
            saved.getId());

        saved.setCurrentAppDeploymentId(9L);

        saved = appInstanceRepository.save(saved);

        AppInstance found = appInstanceRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(found.getAppId()).isEqualTo(app.getId());
        assertThat(found.getWorkspaceId()).isEqualTo(1L);
        assertThat(found.getOwnerUserId()).isEqualTo(5L);
        assertThat(found.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(found.getStatus()).isEqualTo(AppInstanceStatus.ACTIVE);
        assertThat(found.getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
        assertThat(found.getHostingTarget()).isEqualTo(AppHostingTarget.VERCEL);
        assertThat(found.getUserAuthMode()).isEqualTo(AppUserAuthMode.REQUIRED);
        assertThat(found.getRegisteredClientId()).isEqualTo("client-1");
        assertThat(found.getConfiguration()).isEqualTo(Map.of("region", "hr"));
        assertThat(found.getDeploymentConnectionId()).isEqualTo(7L);
        assertThat(found.getDeploymentSettings()).isEqualTo(Map.of("project", "crm"));
        assertThat(found.getCurrentAppDeploymentId()).isEqualTo(9L);
        assertThat(found.getSecretKeyHash()).isEqualTo("a".repeat(64));
        assertThat(appInstanceRepository.findBySecretKeyHash("a".repeat(64))).get()
            .extracting(AppInstance::getId)
            .isEqualTo(saved.getId());
        assertThat(appInstanceRepository.findBySecretKeyHash("b".repeat(64))).isEmpty();
    }

    @Test
    void testRowInsertedWithoutModeIsNone() {
        String uuid = String.valueOf(UUID.randomUUID());

        jdbcTemplate.update(
            """
                INSERT INTO app_instance (uuid, app_id, workspace_id, owner_user_id, environment, name, hosting_target,
                    secret_key_hash, secret_key_hint, created_by, created_date, last_modified_by, last_modified_date)
                VALUES (?, ?, 1, 5, 0, 'Raw', 0, ?, 'hint', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP)
                """,
            uuid, app.getId(), "c".repeat(64));

        AppInstance found = appInstanceRepository.findByUuid(uuid)
            .orElseThrow();

        assertThat(found.getUserAuthMode()).isEqualTo(AppUserAuthMode.NONE);
        assertThat(found.getRegisteredClientId()).isNull();
    }

    @Test
    void testFindsInstancesOfAppOrderedById() {
        AppInstance first = appInstanceRepository.save(newAppInstance(app.getId(), "Zagreb", "c".repeat(64)));
        AppInstance second = appInstanceRepository.save(newAppInstance(app.getId(), "Berlin", "d".repeat(64)));

        App otherApp = appRepository.save(AppRepositoryIntTest.newApp("support-portal"));

        appInstanceRepository.save(newAppInstance(otherApp.getId(), "Vienna", "e".repeat(64)));

        assertThat(appInstanceRepository.findAllByAppIdOrderById(app.getId()))
            .extracting(AppInstance::getId)
            .containsExactly(first.getId(), second.getId());
    }

    @Test
    void testFindsByAppEnvironmentAndName() {
        AppInstance saved = appInstanceRepository.save(newAppInstance(app.getId(), "Croatia", "f".repeat(64)));

        assertThat(
            appInstanceRepository.findByAppIdAndEnvironmentAndName(
                app.getId(), Environment.PRODUCTION.ordinal(), "Croatia"))
                    .get()
                    .extracting(AppInstance::getId)
                    .isEqualTo(saved.getId());
        assertThat(
            appInstanceRepository.findByAppIdAndEnvironmentAndName(
                app.getId(), Environment.STAGING.ordinal(), "Croatia"))
                    .isEmpty();
    }

    @Test
    void testRejectsDuplicateNameInSameAppAndEnvironment() {
        appInstanceRepository.save(newAppInstance(app.getId(), "Croatia", "1".repeat(64)));

        AppInstance duplicate = newAppInstance(app.getId(), "Croatia", "2".repeat(64));

        assertThatThrownBy(() -> appInstanceRepository.save(duplicate))
            .hasStackTraceContaining("uk_app_instance_app_environment_name");

        AppInstance otherEnvironment = newAppInstance(app.getId(), "Croatia", "3".repeat(64));

        otherEnvironment.setEnvironment(Environment.STAGING);

        assertThat(appInstanceRepository.save(otherEnvironment)
            .getId()).isNotNull();
    }

    @Test
    void testTouchSecretKeyLastUsedDateHonoursCutoffAndSkipsVersion() {
        AppInstance saved = appInstanceRepository.save(newAppInstance(app.getId(), "Croatia", "4".repeat(64)));
        Instant now = Instant.now()
            .truncatedTo(ChronoUnit.MILLIS);

        int first = appInstanceRepository.touchSecretKeyLastUsedDate(saved.getId(), now, now.minusSeconds(60));
        int second = appInstanceRepository.touchSecretKeyLastUsedDate(
            saved.getId(), now.plusSeconds(1), now.plusSeconds(1)
                .minusSeconds(60));

        AppInstance found = appInstanceRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
        assertThat(found.getSecretKeyLastUsedDate()).isEqualTo(now);
        assertThat(found.getVersion()).isEqualTo(saved.getVersion());
    }

    @Test
    void testDeletingTheAppCascadesToItsInstances() {
        AppInstance saved = appInstanceRepository.save(newAppInstance(app.getId(), "Croatia", "5".repeat(64)));

        appRepository.deleteById(app.getId());

        assertThat(appInstanceRepository.findById(saved.getId())).isEmpty();
    }

    static AppInstance newAppInstance(long appId, String name, String hash) {
        AppInstance appInstance = new AppInstance();

        appInstance.setUuid(UUID.randomUUID()
            .toString());
        appInstance.setAppId(appId);
        appInstance.setName(name);
        appInstance.setWorkspaceId(1L);
        appInstance.setOwnerUserId(5L);
        appInstance.setEnvironment(Environment.PRODUCTION);
        appInstance.setStatus(AppInstanceStatus.ACTIVE);
        appInstance.setHostingTarget(AppHostingTarget.VERCEL);
        appInstance.setSecretKeyHash(hash);
        appInstance.setSecretKeyHint(hash.substring(0, 8));

        return appInstance;
    }
}
