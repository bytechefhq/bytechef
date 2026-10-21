/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppFavorite;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Objects;
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
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppFavoriteServiceIntTest {

    @Autowired
    private AppFavoriteService appFavoriteService;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppService appService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long appId;
    private long otherAppId;
    private long otherUserId;
    private long userId;

    @BeforeEach
    void beforeEach() {
        userId = insertUser("member@example.com");
        otherUserId = insertUser("other@example.com");

        appId = createApp("crm");
        otherAppId = createApp("hr");
    }

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM \"user\" WHERE login IN ('member@example.com', 'other@example.com')");
    }

    @Test
    void testAddFavoriteIsIdempotent() {
        appFavoriteService.addFavorite(userId, appId);
        appFavoriteService.addFavorite(userId, appId);

        List<AppFavorite> favorites = appFavoriteService.getFavorites(userId, List.of(appId, otherAppId));

        assertThat(favorites).extracting(AppFavorite::appId)
            .containsExactly(appId);
        assertThat(favorites.getFirst()
            .createdDate()).isNotNull();
    }

    @Test
    void testFavoritesArePerUser() {
        appFavoriteService.addFavorite(userId, appId);
        appFavoriteService.addFavorite(otherUserId, otherAppId);

        assertThat(appFavoriteService.getFavorites(userId, List.of(appId, otherAppId))).extracting(AppFavorite::appId)
            .containsExactly(appId);
        assertThat(appFavoriteService.getFavorites(otherUserId, List.of(appId, otherAppId)))
            .extracting(AppFavorite::appId)
            .containsExactly(otherAppId);
    }

    @Test
    void testGetFavoritesReturnsOnlyTheAskedApps() {
        appFavoriteService.addFavorite(userId, appId);
        appFavoriteService.addFavorite(userId, otherAppId);

        assertThat(appFavoriteService.getFavorites(userId, List.of(otherAppId))).extracting(AppFavorite::appId)
            .containsExactly(otherAppId);
        assertThat(appFavoriteService.getFavorites(userId, List.of())).isEmpty();
    }

    @Test
    void testRemoveFavorite() {
        appFavoriteService.addFavorite(userId, appId);

        appFavoriteService.removeFavorite(userId, appId);
        appFavoriteService.removeFavorite(userId, otherAppId);

        assertThat(appFavoriteService.getFavorites(userId, List.of(appId))).isEmpty();
    }

    @Test
    void testDeleteFavoritesDeletesOnlyTheUsersRowsOfTheGivenApps() {
        appFavoriteService.addFavorite(userId, appId);
        appFavoriteService.addFavorite(userId, otherAppId);
        appFavoriteService.addFavorite(otherUserId, appId);

        appFavoriteService.deleteFavorites(userId, List.of(appId));
        appFavoriteService.deleteFavorites(userId, List.of());

        assertThat(appFavoriteService.getFavorites(userId, List.of(appId, otherAppId))).extracting(AppFavorite::appId)
            .containsExactly(otherAppId);
        assertThat(appFavoriteService.getFavorites(otherUserId, List.of(appId))).extracting(AppFavorite::appId)
            .containsExactly(appId);
    }

    @Test
    void testDeletingTheAppDeletesItsFavorites() {
        appFavoriteService.addFavorite(userId, appId);

        appService.delete(appId);

        assertThat(countFavorites(appId)).isZero();
    }

    @Test
    void testDeletingTheUserDeletesTheirFavorites() {
        appFavoriteService.addFavorite(userId, appId);

        jdbcTemplate.update("DELETE FROM \"user\" WHERE id = ?", userId);

        assertThat(countFavorites(appId)).isZero();
    }

    private long countFavorites(long favoriteAppId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM app_favorite WHERE app_id = ?", Long.class, favoriteAppId);

        return Objects.requireNonNull(count);
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

    private long insertUser(String login) {
        Long id = jdbcTemplate.queryForObject(
            """
                INSERT INTO "user" (login, password_hash, activated, created_by, created_date, last_modified_date, uuid)
                     VALUES (?, 'hash', true, 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, gen_random_uuid())
                  RETURNING id
                """,
            Long.class, login);

        return Objects.requireNonNull(id);
    }
}
