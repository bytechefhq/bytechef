/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.ee.automation.app.util.AppSecretKeys;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppServiceIntTest {

    @MockitoSpyBean
    private AppRepository appRepository;

    @Autowired
    private AppService appService;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void createReturnsTheKeyOnceAndStoresOnlyItsHash() {
        AppService.CreatedApp created = create("crm");

        App stored = appRepository.findById(created.app()
            .getId())
            .orElseThrow();

        assertThat(created.secretKey()).isNotBlank();
        assertThat(stored.getSecretKeyHash()).isEqualTo(AppSecretKeys.hash(created.secretKey()));
        assertThat(stored.getSecretKeyHash()).isNotEqualTo(created.secretKey());
        assertThat(stored.getStatus()).isEqualTo(AppStatus.ENABLED);
        assertThat(stored.getUuid()).isNotBlank();
        assertThat(appService.fetchAppBySecretKey(created.secretKey())).isPresent();
    }

    @Test
    void duplicateNameInSameWorkspaceAndEnvironmentIsRejected() {
        create("crm");

        assertThatThrownBy(() -> create("crm")).hasRootCauseInstanceOf(ConfigurationException.class);
    }

    @Test
    void rotateInvalidatesOldHashOnly() {
        AppService.CreatedApp created = create("crm");

        AppService.RotatedKey rotated = TenantContext.callWithTenantId("public", () -> appService.rotateSecretKey(
            created.app()
                .getId()));

        assertThat(appService.fetchAppBySecretKey(created.secretKey())).isEmpty();
        assertThat(appService.fetchAppBySecretKey(rotated.secretKey())).isPresent();
        assertThat(rotated.secretKey()).isNotEqualTo(created.secretKey());
    }

    @Test
    void touchLastUsedWritesOncePerMinute() {
        AppService.CreatedApp created = create("crm");
        long id = created.app()
            .getId();

        appService.touchSecretKeyLastUsedDate(id);

        Instant first = appRepository.findById(id)
            .orElseThrow()
            .getSecretKeyLastUsedDate();

        appService.touchSecretKeyLastUsedDate(id);

        Instant second = appRepository.findById(id)
            .orElseThrow()
            .getSecretKeyLastUsedDate();

        assertThat(first).isNotNull();
        assertThat(second).isEqualTo(first);
    }

    @Test
    void updateStatusAndUpdateChangeOnlyTheirFields() {
        AppService.CreatedApp created = create("crm");
        long id = created.app()
            .getId();

        App disabled = appService.updateStatus(id, AppStatus.DISABLED);

        assertThat(disabled.getStatus()).isEqualTo(AppStatus.DISABLED);

        App updated = appService.update(
            id, "crm-renamed", "desc", AppHostingTarget.VERCEL, null, null, ResourceVisibility.PRIVATE);

        assertThat(updated.getName()).isEqualTo("crm-renamed");
        assertThat(updated.getStatus()).isEqualTo(AppStatus.DISABLED);
        assertThat(updated.getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
        assertThat(appService.getApps(1L, Environment.PRODUCTION)).hasSize(1);
    }

    @Test
    void updateWithNullFieldsKeepsStoredValuesAndEmptyTextClears() {
        long id = create("crm").app()
            .getId();
        Instant deployed = Instant.parse("2026-10-01T00:00:00Z");

        appService.update(
            id, null, "desc", AppHostingTarget.SELF_HOSTED, "https://x.test", deployed, ResourceVisibility.PRIVATE);

        App kept = appService.update(id, null, null, null, null, null, null);

        assertThat(kept.getName()).isEqualTo("crm");
        assertThat(kept.getDescription()).isEqualTo("desc");
        assertThat(kept.getHostingTarget()).isEqualTo(AppHostingTarget.SELF_HOSTED);
        assertThat(kept.getUrl()).isEqualTo("https://x.test");
        assertThat(kept.getLastDeployedDate()).isEqualTo(deployed);
        assertThat(kept.getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);

        App cleared = appService.update(id, null, "", null, "", null, null);

        assertThat(cleared.getDescription()).isNull();
        assertThat(cleared.getUrl()).isNull();
        assertThat(cleared.getLastDeployedDate()).isEqualTo(deployed);
    }

    @Test
    void renameToAnExistingNameIsRejectedAndRenameToOwnNameSucceeds() {
        AppService.CreatedApp first = create("crm");
        AppService.CreatedApp second = create("billing");

        long secondId = second.app()
            .getId();

        assertThatThrownBy(() -> appService.update(secondId, "crm", null, null, null, null, null))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_NAME_ALREADY_EXISTS.getErrorKey());

        assertThat(appService.update(
            first.app()
                .getId(),
            "crm", "still crm", null, null, null, null)
            .getDescription()).isEqualTo("still crm");

        assertThatThrownBy(() -> appService.update(
            first.app()
                .getId(),
            " ", null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void touchLastUsedSwallowsRepositoryFailures() {
        long id = create("crm").app()
            .getId();

        doThrow(new QueryTimeoutException("database unavailable")).when(appRepository)
            .touchSecretKeyLastUsedDate(eq(id), any(Instant.class), any(Instant.class));

        assertThatCode(() -> appService.touchSecretKeyLastUsedDate(id)).doesNotThrowAnyException();

        verify(appRepository).touchSecretKeyLastUsedDate(eq(id), any(Instant.class), any(Instant.class));

        assertThat(appRepository.findById(id)
            .orElseThrow()
            .getSecretKeyLastUsedDate()).isNull();
    }

    @Test
    void createRejectsANameLongerThanTheColumn() {
        assertThatThrownBy(() -> create("x".repeat(257)))
            .cause()
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_FIELD_TOO_LONG.getErrorKey()));

        assertThat(appRepository.count()).isZero();
    }

    @Test
    void updateRejectsAUrlLongerThanTheColumn() {
        long id = create("crm").app()
            .getId();
        String url = "https://x.test/" + "a".repeat(2048);

        assertThatThrownBy(() -> appService.update(id, null, null, null, url, null, null))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_FIELD_TOO_LONG.getErrorKey()));

        assertThat(appRepository.findById(id)
            .orElseThrow()
            .getUrl()).isNull();
    }

    @Test
    void createTranslatesOnlyTheNameConstraintViolationToNameAlreadyExists() {
        create("crm");

        doReturn(Optional.empty()).when(appRepository)
            .findByWorkspaceIdAndEnvironmentAndName(anyLong(), anyInt(), anyString());

        assertThatThrownBy(() -> create("crm"))
            .cause()
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_NAME_ALREADY_EXISTS.getErrorKey()))
            .hasCauseInstanceOf(DataIntegrityViolationException.class);

        assertThat(appRepository.count()).isEqualTo(1);
    }

    @Test
    void createRethrowsAnyOtherConstraintViolation() {
        App app = newApp("crm");

        app.setOwnerUserId(null);

        assertThatThrownBy(() -> TenantContext.callWithTenantId("public", () -> appService.create(app)))
            .cause()
            .isInstanceOf(DataIntegrityViolationException.class)
            .isNotInstanceOf(ConfigurationException.class);

        assertThat(appRepository.count()).isZero();
    }

    @Test
    void getAppFailsWithConfigurationExceptionWhenMissing() {
        assertThatThrownBy(() -> appService.getApp(-1L)).isInstanceOf(ConfigurationException.class);
    }

    private AppService.CreatedApp create(String name) {
        return TenantContext.callWithTenantId("public", () -> appService.create(newApp(name)));
    }

    private static App newApp(String name) {
        App app = new App();

        app.setName(name);
        app.setWorkspaceId(1L);
        app.setEnvironment(Environment.PRODUCTION);
        app.setOwnerUserId(1L);
        app.setHostingTarget(AppHostingTarget.VERCEL);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        return app;
    }
}
