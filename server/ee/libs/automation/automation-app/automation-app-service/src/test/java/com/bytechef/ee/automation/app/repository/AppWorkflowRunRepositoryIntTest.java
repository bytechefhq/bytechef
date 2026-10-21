/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppWorkflowRun;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class AppWorkflowRunRepositoryIntTest {

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppWorkflowRunRepository appWorkflowRunRepository;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void testFindByAppIdAndRunIdReturnsTheRun() {
        App app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));
        String runId = UUID.randomUUID()
            .toString();

        appWorkflowRunRepository.save(newRun(app, runId, Instant.now()));

        AppWorkflowRun found = appWorkflowRunRepository.findByAppIdAndRunId(app.getId(), runId)
            .orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getAppId()).isEqualTo(app.getId());
        assertThat(found.getRunId()).isEqualTo(runId);
        assertThat(found.getWorkflowKey()).isEqualTo("approve_refund");
        assertThat(found.getJobId()).isEqualTo(99L);
        assertThat(found.getCreatedDate()).isNotNull();
    }

    @Test
    void testAnotherAppsRunIdIsNotFound() {
        App appA = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));
        App appB = appRepository.save(AppRepositoryIntTest.newApp("billing-portal", "b".repeat(64)));
        String runId = UUID.randomUUID()
            .toString();

        appWorkflowRunRepository.save(newRun(appA, runId, Instant.now()));

        assertThat(appWorkflowRunRepository.findByAppIdAndRunId(appB.getId(), runId)).isEmpty();
    }

    @Test
    void testDeletingTheAppDeletesItsRuns() {
        App app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));

        appWorkflowRunRepository.save(newRun(app, UUID.randomUUID()
            .toString(), Instant.now()));

        appRepository.deleteById(app.getId());

        assertThat(appWorkflowRunRepository.findAll())
            .noneMatch(appWorkflowRun -> app.getId()
                .equals(appWorkflowRun.getAppId()));
    }

    @Test
    void testDeleteByCreatedDateBeforeKeepsNewerRuns() {
        App app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));
        Instant now = Instant.now();
        String oldRunId = UUID.randomUUID()
            .toString();
        String newRunId = UUID.randomUUID()
            .toString();

        appWorkflowRunRepository.save(newRun(app, oldRunId, now.minus(100, ChronoUnit.DAYS)));
        appWorkflowRunRepository.save(newRun(app, newRunId, now));

        int deleted = appWorkflowRunRepository.deleteByCreatedDateBefore(now.minus(90, ChronoUnit.DAYS));

        assertThat(deleted).isEqualTo(1);
        assertThat(appWorkflowRunRepository.findAll())
            .extracting(AppWorkflowRun::getRunId)
            .containsExactly(newRunId);
    }

    private static AppWorkflowRun newRun(App app, String runId, Instant createdDate) {
        AppWorkflowRun appWorkflowRun = new AppWorkflowRun();

        appWorkflowRun.setAppId(app.getId());
        appWorkflowRun.setRunId(runId);
        appWorkflowRun.setWorkflowKey("approve_refund");
        appWorkflowRun.setJobId(99L);
        appWorkflowRun.setCreatedDate(createdDate);

        return appWorkflowRun;
    }
}
