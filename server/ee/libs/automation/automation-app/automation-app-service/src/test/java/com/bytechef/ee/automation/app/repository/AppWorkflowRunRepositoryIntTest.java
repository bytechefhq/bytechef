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
import com.bytechef.ee.automation.app.domain.AppInstance;
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
    private AppInstanceRepository appInstanceRepository;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppWorkflowRunRepository appWorkflowRunRepository;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void testFindByAppInstanceIdAndRunIdReturnsTheRun() {
        AppInstance appInstance = saveAppInstance("crm-dashboard", "a".repeat(64));
        String runId = UUID.randomUUID()
            .toString();

        appWorkflowRunRepository.save(newRun(appInstance, runId, Instant.now()));

        AppWorkflowRun found = appWorkflowRunRepository.findByAppInstanceIdAndRunId(appInstance.getId(), runId)
            .orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getAppInstanceId()).isEqualTo(appInstance.getId());
        assertThat(found.getRunId()).isEqualTo(runId);
        assertThat(found.getWorkflowKey()).isEqualTo("approve_refund");
        assertThat(found.getJobId()).isEqualTo(99L);
        assertThat(found.getCreatedDate()).isNotNull();
    }

    @Test
    void testAnotherAppsRunIdIsNotFound() {
        AppInstance appInstanceA = saveAppInstance("crm-dashboard", "a".repeat(64));
        AppInstance appInstanceB = saveAppInstance("billing-portal", "b".repeat(64));
        String runId = UUID.randomUUID()
            .toString();

        appWorkflowRunRepository.save(newRun(appInstanceA, runId, Instant.now()));

        assertThat(appWorkflowRunRepository.findByAppInstanceIdAndRunId(appInstanceB.getId(), runId)).isEmpty();
    }

    @Test
    void testDeletingTheAppDeletesItsRuns() {
        AppInstance appInstance = saveAppInstance("crm-dashboard", "a".repeat(64));

        appWorkflowRunRepository.save(newRun(appInstance, UUID.randomUUID()
            .toString(), Instant.now()));

        appRepository.deleteById(appInstance.getAppId());

        assertThat(appWorkflowRunRepository.findAll())
            .noneMatch(appWorkflowRun -> appInstance.getId()
                .equals(appWorkflowRun.getAppInstanceId()));
    }

    @Test
    void testDeleteByCreatedDateBeforeKeepsNewerRuns() {
        AppInstance appInstance = saveAppInstance("crm-dashboard", "a".repeat(64));
        Instant now = Instant.now();
        String oldRunId = UUID.randomUUID()
            .toString();
        String newRunId = UUID.randomUUID()
            .toString();

        appWorkflowRunRepository.save(newRun(appInstance, oldRunId, now.minus(100, ChronoUnit.DAYS)));
        appWorkflowRunRepository.save(newRun(appInstance, newRunId, now));

        int deleted = appWorkflowRunRepository.deleteByCreatedDateBefore(now.minus(90, ChronoUnit.DAYS));

        assertThat(deleted).isEqualTo(1);
        assertThat(appWorkflowRunRepository.findAll())
            .extracting(AppWorkflowRun::getRunId)
            .containsExactly(newRunId);
    }

    private static AppWorkflowRun newRun(AppInstance appInstance, String runId, Instant createdDate) {
        AppWorkflowRun appWorkflowRun = new AppWorkflowRun();

        appWorkflowRun.setAppInstanceId(appInstance.getId());
        appWorkflowRun.setRunId(runId);
        appWorkflowRun.setWorkflowKey("approve_refund");
        appWorkflowRun.setJobId(99L);
        appWorkflowRun.setCreatedDate(createdDate);

        return appWorkflowRun;
    }

    private AppInstance saveAppInstance(String name, String hash) {
        App app = appRepository.save(AppRepositoryIntTest.newApp(name));

        return appInstanceRepository.save(AppInstanceRepositoryIntTest.newAppInstance(app.getId(), name, hash));
    }
}
