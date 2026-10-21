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
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceUsage;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class AppInstanceUsageServiceIntTest {

    @Autowired
    private AppInstanceService appInstanceService;

    @Autowired
    private AppInstanceUsageService appInstanceUsageService;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppService appService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long appInstanceId;
    private long otherAppInstanceId;
    private long otherUserId;
    private long userId;

    @BeforeEach
    void beforeEach() {
        userId = insertUser("member@example.com");
        otherUserId = insertUser("other@example.com");

        App app = new App();

        app.setName("crm");
        app.setWorkspaceId(1L);
        app.setOwnerUserId(1L);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        long appId = appService.create(app)
            .getId();

        appInstanceId = createAppInstance(appId, "Croatia");
        otherAppInstanceId = createAppInstance(appId, "Germany");
    }

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM \"user\" WHERE login IN ('member@example.com', 'other@example.com')");
    }

    @Test
    void testRecordLaunchIncrementsTheCountAndMovesTheLastUsedDate() {
        appInstanceUsageService.recordLaunch(userId, appInstanceId);

        AppInstanceUsage firstLaunch = getUsage(userId, appInstanceId);

        setLastUsedDate(userId, appInstanceId, firstLaunch.lastUsedDate()
            .minus(Duration.ofMinutes(1)));

        appInstanceUsageService.recordLaunch(userId, appInstanceId);

        AppInstanceUsage secondLaunch = getUsage(userId, appInstanceId);

        assertThat(firstLaunch.useCount()).isEqualTo(1);
        assertThat(secondLaunch.useCount()).isEqualTo(2);
        assertThat(secondLaunch.lastUsedDate()).isAfterOrEqualTo(firstLaunch.lastUsedDate());
    }

    @Test
    void testTouchCreatesARowWithoutCountingALaunch() {
        appInstanceUsageService.touchLastUsedDate(userId, appInstanceId);

        AppInstanceUsage usage = getUsage(userId, appInstanceId);

        assertThat(usage.useCount()).isZero();
        assertThat(usage.lastUsedDate()).isNotNull();
    }

    @Test
    void testTouchWithinTheThrottleIntervalDoesNotMoveTheLastUsedDate() {
        appInstanceUsageService.recordLaunch(userId, appInstanceId);

        Instant now = Instant.now();

        Instant recent = now.truncatedTo(ChronoUnit.MILLIS)
            .minus(Duration.ofMinutes(5));

        setLastUsedDate(userId, appInstanceId, recent);

        appInstanceUsageService.touchLastUsedDate(userId, appInstanceId);

        AppInstanceUsage usage = getUsage(userId, appInstanceId);

        assertThat(usage.lastUsedDate()).isEqualTo(recent);
        assertThat(usage.useCount()).isEqualTo(1);
    }

    @Test
    void testTouchAfterTheThrottleIntervalMovesTheLastUsedDateAndKeepsTheCount() {
        appInstanceUsageService.recordLaunch(userId, appInstanceId);

        Instant stale = Instant.now()
            .minus(AppInstanceUsageServiceImpl.TOUCH_THROTTLE)
            .minus(Duration.ofMinutes(1));

        setLastUsedDate(userId, appInstanceId, stale);

        appInstanceUsageService.touchLastUsedDate(userId, appInstanceId);

        AppInstanceUsage usage = getUsage(userId, appInstanceId);

        assertThat(usage.lastUsedDate()).isAfter(stale.plus(AppInstanceUsageServiceImpl.TOUCH_THROTTLE));
        assertThat(usage.useCount()).isEqualTo(1);
    }

    @Test
    void testUsageIsPerUserAndPerInstance() {
        appInstanceUsageService.recordLaunch(userId, appInstanceId);
        appInstanceUsageService.recordLaunch(otherUserId, otherAppInstanceId);

        assertThat(appInstanceUsageService.getUsages(userId, List.of(appInstanceId, otherAppInstanceId)))
            .extracting(AppInstanceUsage::appInstanceId)
            .containsExactly(appInstanceId);
        assertThat(appInstanceUsageService.getUsages(otherUserId, List.of(appInstanceId, otherAppInstanceId)))
            .extracting(AppInstanceUsage::appInstanceId)
            .containsExactly(otherAppInstanceId);
        assertThat(appInstanceUsageService.getUsages(userId, List.of())).isEmpty();
    }

    @Test
    void testDeleteUsagesDeletesOnlyTheUsersRowsOfTheGivenInstances() {
        appInstanceUsageService.recordLaunch(userId, appInstanceId);
        appInstanceUsageService.recordLaunch(userId, otherAppInstanceId);
        appInstanceUsageService.recordLaunch(otherUserId, appInstanceId);

        appInstanceUsageService.deleteUsages(userId, List.of(appInstanceId));
        appInstanceUsageService.deleteUsages(userId, List.of());

        assertThat(appInstanceUsageService.getUsages(userId, List.of(appInstanceId, otherAppInstanceId)))
            .extracting(AppInstanceUsage::appInstanceId)
            .containsExactly(otherAppInstanceId);
        assertThat(appInstanceUsageService.getUsages(otherUserId, List.of(appInstanceId)))
            .extracting(AppInstanceUsage::appInstanceId)
            .containsExactly(appInstanceId);
    }

    @Test
    void testDeletingTheInstanceDeletesItsUsage() {
        appInstanceUsageService.recordLaunch(userId, appInstanceId);

        appInstanceService.delete(appInstanceId);

        assertThat(appInstanceUsageService.getUsages(userId, List.of(appInstanceId))).isEmpty();
    }

    private long createAppInstance(long appId, String name) {
        AppInstance appInstance = new AppInstance();

        appInstance.setAppId(appId);
        appInstance.setName(name);
        appInstance.setWorkspaceId(1L);
        appInstance.setOwnerUserId(7L);
        appInstance.setEnvironment(Environment.PRODUCTION);
        appInstance.setHostingTarget(AppHostingTarget.VERCEL);

        AppInstanceService.CreatedAppInstance createdAppInstance = TenantContext.callWithTenantId(
            "public", () -> appInstanceService.create(appInstance));

        return createdAppInstance.appInstance()
            .getId();
    }

    private AppInstanceUsage getUsage(long usageUserId, long usageAppInstanceId) {
        List<AppInstanceUsage> usages = appInstanceUsageService.getUsages(usageUserId, List.of(usageAppInstanceId));

        assertThat(usages).hasSize(1);

        return usages.getFirst();
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

    private void setLastUsedDate(long usageUserId, long usageAppInstanceId, Instant lastUsedDate) {
        jdbcTemplate.update(
            "UPDATE app_instance_usage SET last_used_date = ? WHERE user_id = ? AND app_instance_id = ?",
            Timestamp.from(lastUsedDate), usageUserId, usageAppInstanceId);
    }
}
