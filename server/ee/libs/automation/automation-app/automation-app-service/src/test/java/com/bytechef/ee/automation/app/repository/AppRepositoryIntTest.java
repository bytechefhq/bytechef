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
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
class AppRepositoryIntTest {

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void savesAndReadsBackEnumsAndPortalColumns() {
        App app = newApp("crm-dashboard");

        app.setStatus(AppStatus.ACTIVE);
        app.setPortalDiscoverable(true);
        app.setIcon(new FileEntry("icon.png", "base64:///app_icons/icon.png"));

        App saved = appRepository.save(app);

        App found = appRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(found.getStatus()).isEqualTo(AppStatus.ACTIVE);
        assertThat(found.getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
        assertThat(found.isPortalDiscoverable()).isTrue();
        assertThat(found.getIcon()).isEqualTo(new FileEntry("icon.png", "base64:///app_icons/icon.png"));
        assertThat(found.getCategoryId()).isNull();
        assertThat(found.getWorkspaceId()).isEqualTo(1L);
    }

    @Test
    void savesACategoryThatExists() {
        Long categoryId = jdbcTemplate.queryForObject(
            """
                INSERT INTO category (name, description, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES ('Sales', null, now(), 'test', now(), 'test', 0)
                RETURNING id
                """,
            Long.class);

        try {
            App app = newApp("crm-dashboard");

            app.setCategoryId(categoryId);

            App saved = appRepository.save(app);

            assertThat(appRepository.findById(saved.getId())
                .orElseThrow()
                .getCategoryId()).isEqualTo(categoryId);

            App withMissingCategory = newApp("support-portal");

            withMissingCategory.setCategoryId(Objects.requireNonNull(categoryId) + 1000);

            assertThatThrownBy(() -> appRepository.save(withMissingCategory))
                .hasStackTraceContaining("fk_app_category");
        } finally {
            appRepository.deleteAll();

            jdbcTemplate.update("DELETE FROM category WHERE id = ?", categoryId);
        }
    }

    @Test
    void findsByWorkspaceAndOrdersByName() {
        appRepository.save(newApp("zeta"));
        appRepository.save(newApp("alpha"));

        App otherWorkspace = newApp("beta");

        otherWorkspace.setWorkspaceId(2L);

        appRepository.save(otherWorkspace);

        List<App> apps = appRepository.findAllByWorkspaceIdOrderByName(1L);

        assertThat(apps).extracting(App::getName)
            .containsExactly("alpha", "zeta");
    }

    @Test
    void findsByUuidAndByWorkspaceAndName() {
        App saved = appRepository.save(newApp("crm-dashboard"));

        assertThat(appRepository.findByUuid(saved.getUuid())).get()
            .extracting(App::getId)
            .isEqualTo(saved.getId());
        assertThat(appRepository.findByUuid(UUID.randomUUID()
            .toString())).isEmpty();
        assertThat(appRepository.findByWorkspaceIdAndName(1L, "crm-dashboard")).get()
            .extracting(App::getId)
            .isEqualTo(saved.getId());
        assertThat(appRepository.findByWorkspaceIdAndName(2L, "crm-dashboard")).isEmpty();
    }

    @Test
    void rejectsDuplicateNameInSameWorkspace() {
        appRepository.save(newApp("crm-dashboard"));

        App duplicate = newApp("crm-dashboard");

        assertThatThrownBy(() -> appRepository.save(duplicate))
            .hasStackTraceContaining("uk_app_workspace_name");

        App otherWorkspace = newApp("crm-dashboard");

        otherWorkspace.setWorkspaceId(2L);

        assertThat(appRepository.save(otherWorkspace)
            .getId()).isNotNull();
    }

    static App newApp(String name) {
        App app = new App();

        app.setUuid(UUID.randomUUID()
            .toString());
        app.setName(name);
        app.setWorkspaceId(1L);
        app.setOwnerUserId(1L);
        app.setStatus(AppStatus.DRAFT);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        return app;
    }
}
