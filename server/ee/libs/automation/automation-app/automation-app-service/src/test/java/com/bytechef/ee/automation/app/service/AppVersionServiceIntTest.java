/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppVersion;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.ee.automation.app.repository.AppVersionRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppVersionServiceIntTest {

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppService appService;

    @MockitoSpyBean
    private AppVersionRepository appVersionRepository;

    @Autowired
    private AppVersionService appVersionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long appId;

    @BeforeEach
    void beforeEach() {
        appId = createApp("crm");
    }

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void testCreateStoresTheVersion() {
        AppVersion created = appVersionService.create(newAppVersion(appId, "1.0", "abc123"));

        AppVersion stored = appVersionService.getAppVersion(created.getId());

        assertThat(stored.getAppId()).isEqualTo(appId);
        assertThat(stored.getName()).isEqualTo("1.0");
        assertThat(stored.getGitRef()).isEqualTo("main");
        assertThat(stored.getGitCommitSha()).isEqualTo("abc123");
        assertThat(stored.getDescription()).isEqualTo("First release");
        assertThat(stored.getCreatedBy()).isEqualTo("system");
        assertThat(stored.getCreatedDate()).isNotNull();
    }

    @Test
    void testCreateRefusesTakenNameOfTheSameApp() {
        appVersionService.create(newAppVersion(appId, "1.0", "abc123"));

        assertThatThrownBy(() -> appVersionService.create(newAppVersion(appId, "1.0", "def456")))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_VERSION_NAME_ALREADY_EXISTS.getErrorKey()));

        long otherAppId = createApp("erp");

        assertThat(appVersionService.create(newAppVersion(otherAppId, "1.0", "abc123"))
            .getId()).isNotNull();
    }

    @Test
    void testCreateTranslatesTheNameConstraintViolationToNameAlreadyExists() {
        appVersionService.create(newAppVersion(appId, "1.0", "abc123"));

        doReturn(Optional.empty()).when(appVersionRepository)
            .findByAppIdAndName(anyLong(), anyString());

        assertThatThrownBy(() -> appVersionService.create(newAppVersion(appId, "1.0", "def456")))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_VERSION_NAME_ALREADY_EXISTS.getErrorKey()));
    }

    @Test
    void testCreateRefusesBlankAndTooLongName() {
        assertThatThrownBy(() -> appVersionService.create(newAppVersion(appId, " ", null)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> appVersionService.create(newAppVersion(appId, "v".repeat(257), null)))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_FIELD_TOO_LONG.getErrorKey()));
    }

    @Test
    void testCreateRefusesATooLongDescriptionAndAcceptsTheLimit() {
        AppVersion atLimit = newAppVersion(appId, "limit", null);

        atLimit.setDescription("d".repeat(4096));

        assertThat(appVersionService.create(atLimit)
            .getId()).isNotNull();

        AppVersion tooLong = newAppVersion(appId, "too-long", null);

        tooLong.setDescription("d".repeat(4097));

        assertThatThrownBy(() -> appVersionService.create(tooLong))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_FIELD_TOO_LONG.getErrorKey()));
    }

    @Test
    void testFetchAppVersionFindsByAppAndName() {
        AppVersion created = appVersionService.create(newAppVersion(appId, "1.0", "abc123"));

        assertThat(appVersionService.fetchAppVersion(appId, "1.0")).get()
            .extracting(AppVersion::getId)
            .isEqualTo(created.getId());
        assertThat(appVersionService.fetchAppVersion(appId, "2.0")).isEmpty();
        assertThat(appVersionService.fetchAppVersion(createApp("erp"), "1.0")).isEmpty();
    }

    @Test
    void testGetAppVersionOfUnknownIdIsNotFound() {
        assertThatThrownBy(() -> appVersionService.getAppVersion(Long.MAX_VALUE))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_VERSION_NOT_FOUND.getErrorKey()));
    }

    @Test
    void testGetAppVersionsIsNewestFirstAndByIds() {
        AppVersion first = appVersionService.create(newAppVersion(appId, "1.0", null));
        AppVersion second = appVersionService.create(newAppVersion(appId, "1.1", null));

        appVersionService.create(newAppVersion(createApp("erp"), "9.9", null));

        assertThat(appVersionService.getAppVersions(appId, 50))
            .extracting(AppVersion::getName)
            .containsExactly("1.1", "1.0");
        assertThat(appVersionService.getAppVersionsByIds(List.of(first.getId(), second.getId())))
            .extracting(AppVersion::getName)
            .containsExactlyInAnyOrder("1.0", "1.1");
        assertThat(appVersionService.getAppVersionsByIds(List.of())).isEmpty();
    }

    @Test
    void testGetAppVersionsReturnsOnlyTheNewestLimitOfThem() {
        appVersionService.create(newAppVersion(appId, "1.0", null));
        appVersionService.create(newAppVersion(appId, "1.1", null));
        appVersionService.create(newAppVersion(appId, "1.2", null));

        appVersionService.create(newAppVersion(createApp("erp"), "9.9", null));

        assertThat(appVersionService.getAppVersions(appId, 2))
            .extracting(AppVersion::getName)
            .containsExactly("1.2", "1.1");
    }

    @Test
    void testDeletingAppDeletesItsVersions() {
        appVersionService.create(newAppVersion(appId, "1.0", null));

        appService.delete(appId);

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM app_version WHERE app_id = ?", Long.class, appId)).isZero();
    }

    private long createApp(String name) {
        App app = new App();

        app.setName(name);
        app.setWorkspaceId(1L);
        app.setOwnerUserId(1L);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        return appService.create(app)
            .getId();
    }

    private static AppVersion newAppVersion(long appId, String name, String gitCommitSha) {
        AppVersion appVersion = new AppVersion();

        appVersion.setAppId(appId);
        appVersion.setName(name);
        appVersion.setGitRef("main");
        appVersion.setGitCommitSha(gitCommitSha);
        appVersion.setDescription("First release");

        return appVersion;
    }
}
