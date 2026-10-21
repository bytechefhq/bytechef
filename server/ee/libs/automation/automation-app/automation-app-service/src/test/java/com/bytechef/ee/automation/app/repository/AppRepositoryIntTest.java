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
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppRepositoryIntTest {

    @Autowired
    private AppRepository appRepository;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void savesAndReadsBackEnumsAndKeyColumns() {
        App app = newApp("crm-dashboard", "a".repeat(64));

        App saved = appRepository.save(app);

        App found = appRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(found.getStatus()).isEqualTo(AppStatus.ENABLED);
        assertThat(found.getHostingTarget()).isEqualTo(AppHostingTarget.VERCEL);
        assertThat(found.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(found.getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
        assertThat(found.getSecretKeyHash()).isEqualTo("a".repeat(64));
        assertThat(appRepository.findBySecretKeyHash("a".repeat(64))).isPresent();
    }

    @Test
    void touchSecretKeyLastUsedDateHonoursCutoffAndSkipsVersion() {
        App saved = appRepository.save(newApp("crm-dashboard", "b".repeat(64)));
        Instant now = Instant.now()
            .truncatedTo(ChronoUnit.MILLIS);

        int first = appRepository.touchSecretKeyLastUsedDate(saved.getId(), now, now.minusSeconds(60));
        int second = appRepository.touchSecretKeyLastUsedDate(
            saved.getId(), now.plusSeconds(1), now.plusSeconds(1)
                .minusSeconds(60));

        App found = appRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
        assertThat(found.getSecretKeyLastUsedDate()).isEqualTo(now);
        assertThat(found.getVersion()).isEqualTo(saved.getVersion());
    }

    @Test
    void findsByWorkspaceEnvironmentAndOrdersByName() {
        appRepository.save(newApp("zeta", "c".repeat(64)));
        appRepository.save(newApp("alpha", "d".repeat(64)));

        App otherWorkspace = newApp("beta", "e".repeat(64));

        otherWorkspace.setWorkspaceId(2L);

        appRepository.save(otherWorkspace);

        App otherEnvironment = newApp("gamma", "f".repeat(64));

        otherEnvironment.setEnvironment(Environment.DEVELOPMENT);

        appRepository.save(otherEnvironment);

        List<App> apps = appRepository.findAllByWorkspaceIdAndEnvironmentOrderByName(
            1L, Environment.PRODUCTION.ordinal());

        assertThat(apps).extracting(App::getName)
            .containsExactly("alpha", "zeta");
    }

    @Test
    void findsByUuidAndByWorkspaceEnvironmentAndName() {
        App saved = appRepository.save(newApp("crm-dashboard", "1".repeat(64)));

        assertThat(appRepository.findByUuid(saved.getUuid())).get()
            .extracting(App::getId)
            .isEqualTo(saved.getId());
        assertThat(appRepository.findByUuid(UUID.randomUUID()
            .toString())).isEmpty();
        assertThat(appRepository.findByWorkspaceIdAndEnvironmentAndName(
            1L, Environment.PRODUCTION.ordinal(), "crm-dashboard")).get()
                .extracting(App::getId)
                .isEqualTo(saved.getId());
        assertThat(appRepository.findByWorkspaceIdAndEnvironmentAndName(
            1L, Environment.STAGING.ordinal(), "crm-dashboard")).isEmpty();
        assertThat(appRepository.findByWorkspaceIdAndEnvironmentAndName(
            2L, Environment.PRODUCTION.ordinal(), "crm-dashboard")).isEmpty();
    }

    @Test
    void rejectsDuplicateNameInSameWorkspaceAndEnvironment() {
        appRepository.save(newApp("crm-dashboard", "2".repeat(64)));

        App duplicate = newApp("crm-dashboard", "3".repeat(64));

        assertThatThrownBy(() -> appRepository.save(duplicate))
            .hasStackTraceContaining("uk_app_workspace_environment_name");

        App otherEnvironment = newApp("crm-dashboard", "4".repeat(64));

        otherEnvironment.setEnvironment(Environment.STAGING);

        assertThat(appRepository.save(otherEnvironment)
            .getId()).isNotNull();
    }

    static App newApp(String name, String hash) {
        App app = new App();

        app.setUuid(UUID.randomUUID()
            .toString());
        app.setName(name);
        app.setWorkspaceId(1L);
        app.setEnvironment(Environment.PRODUCTION);
        app.setOwnerUserId(1L);
        app.setStatus(AppStatus.ENABLED);
        app.setVisibility(ResourceVisibility.WORKSPACE);
        app.setHostingTarget(AppHostingTarget.VERCEL);
        app.setSecretKeyHash(hash);
        app.setSecretKeyHint(hash.substring(0, 8));

        return app;
    }
}
