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
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM category WHERE name = 'Sales'");
    }

    @Test
    void testCreateStartsTheAppAsDraftWithAUuid() {
        App created = appService.create(newApp("crm"));

        App stored = appRepository.findById(created.getId())
            .orElseThrow();

        assertThat(stored.getStatus()).isEqualTo(AppStatus.DRAFT);
        assertThat(stored.getUuid()).isNotBlank();
        assertThat(stored.getWorkspaceId()).isEqualTo(1L);
        assertThat(stored.isPortalDiscoverable()).isFalse();
    }

    @Test
    void testDuplicateNameInSameWorkspaceIsRejected() {
        appService.create(newApp("crm"));

        App other = newApp("crm");

        other.setWorkspaceId(2L);

        assertThat(appService.create(other)
            .getId()).isNotNull();
        assertThatThrownBy(() -> appService.create(newApp("crm")))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_NAME_ALREADY_EXISTS.getErrorKey()));
    }

    @Test
    void testUpdateStatusAndUpdateChangeOnlyTheirFields() {
        long id = appService.create(newApp("crm"))
            .getId();

        App suspended = appService.updateStatus(id, AppStatus.SUSPENDED);

        assertThat(suspended.getStatus()).isEqualTo(AppStatus.SUSPENDED);

        App updated = appService.update(id, "crm-renamed", "desc");

        assertThat(updated.getName()).isEqualTo("crm-renamed");
        assertThat(updated.getStatus()).isEqualTo(AppStatus.SUSPENDED);
        assertThat(updated.getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
        assertThat(appService.getApps(1L)).hasSize(1);
    }

    @Test
    void testUpdateVisibilityChangesOnlyTheVisibility() {
        long id = appService.create(newApp("crm"))
            .getId();

        App updated = appService.updateVisibility(id, ResourceVisibility.PRIVATE);

        assertThat(updated.getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
        assertThat(updated.getName()).isEqualTo("crm");
        assertThat(appService.getApp(id)
            .getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
    }

    @Test
    void testUpdateWithNullFieldsKeepsStoredValuesAndEmptyTextClears() {
        long id = appService.create(newApp("crm"))
            .getId();

        appService.update(id, null, "desc");

        App kept = appService.update(id, null, null);

        assertThat(kept.getName()).isEqualTo("crm");
        assertThat(kept.getDescription()).isEqualTo("desc");

        App cleared = appService.update(id, null, "");

        assertThat(cleared.getDescription()).isNull();
    }

    @Test
    void testRenameToAnExistingNameIsRejectedAndRenameToOwnNameSucceeds() {
        long firstId = appService.create(newApp("crm"))
            .getId();
        long secondId = appService.create(newApp("billing"))
            .getId();

        assertThatThrownBy(() -> appService.update(secondId, "crm", null))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_NAME_ALREADY_EXISTS.getErrorKey()));

        assertThat(appService.update(firstId, "crm", "still crm")
            .getDescription()).isEqualTo("still crm");

        assertThatThrownBy(() -> appService.update(firstId, " ", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateRejectsANameLongerThanTheColumn() {
        assertThatThrownBy(() -> appService.create(newApp("x".repeat(257))))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_FIELD_TOO_LONG.getErrorKey()));

        assertThat(appRepository.count()).isZero();
    }

    @Test
    void testCreateTranslatesOnlyTheNameConstraintViolationToNameAlreadyExists() {
        appService.create(newApp("crm"));

        doReturn(Optional.empty()).when(appRepository)
            .findByWorkspaceIdAndName(anyLong(), anyString());

        assertThatThrownBy(() -> appService.create(newApp("crm")))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_NAME_ALREADY_EXISTS.getErrorKey()))
            .hasCauseInstanceOf(DataIntegrityViolationException.class);

        assertThat(appRepository.count()).isEqualTo(1);
    }

    @Test
    void testCreateRethrowsAnyOtherConstraintViolation() {
        App app = newApp("crm");

        app.setCategoryId(-1L);

        assertThatThrownBy(() -> appService.create(app))
            .isInstanceOf(DataIntegrityViolationException.class)
            .isNotInstanceOf(ConfigurationException.class);

        assertThat(appRepository.count()).isZero();
    }

    @Test
    void testGetAppFailsWithConfigurationExceptionWhenMissing() {
        assertThatThrownBy(() -> appService.getApp(-1L)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testUpdateIconStoresAndClearsTheFileEntry() {
        long id = appService.create(newApp("crm"))
            .getId();
        FileEntry icon = new FileEntry("icon.png", "base64:///app_icons/icon.png");

        assertThat(appService.updateIcon(id, icon)
            .getIcon()).isEqualTo(icon);
        assertThat(appRepository.findById(id)
            .orElseThrow()
            .getIcon()).isEqualTo(icon);

        appService.updateIcon(id, null);

        assertThat(appRepository.findById(id)
            .orElseThrow()
            .getIcon()).isNull();
    }

    @Test
    void testUpdateCategoryStoresAndClearsTheCategory() {
        long id = appService.create(newApp("crm"))
            .getId();

        jdbcTemplate.update(
            "INSERT INTO category (name, created_date, created_by, last_modified_date, last_modified_by, version) " +
                "VALUES ('Sales', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin', 0)");

        Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM category WHERE name = 'Sales'", Long.class);

        assertThat(appService.updateCategory(id, categoryId)
            .getCategoryId()).isEqualTo(categoryId);
        assertThat(appService.getApp(id)
            .getCategoryId()).isEqualTo(categoryId);

        appService.updateCategory(id, null);

        assertThat(appService.getApp(id)
            .getCategoryId()).isNull();
    }

    @Test
    void testUpdatePortalDiscoverableChangesOnlyThatFlag() {
        long id = appService.create(newApp("crm"))
            .getId();

        App updated = appService.updatePortalDiscoverable(id, true);

        assertThat(updated.isPortalDiscoverable()).isTrue();
        assertThat(updated.getName()).isEqualTo("crm");
        assertThat(updated.getStatus()).isEqualTo(AppStatus.DRAFT);
        assertThat(appService.getApp(id)
            .isPortalDiscoverable()).isTrue();

        appService.updatePortalDiscoverable(id, false);

        assertThat(appService.getApp(id)
            .isPortalDiscoverable()).isFalse();
    }

    private static App newApp(String name) {
        App app = new App();

        app.setName(name);
        app.setWorkspaceId(1L);
        app.setOwnerUserId(1L);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        return app;
    }
}
