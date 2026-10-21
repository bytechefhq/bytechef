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
import com.bytechef.ee.automation.app.domain.AppWorkflow;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppWorkflowRepositoryIntTest {

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppWorkflowRepository appWorkflowRepository;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void testSaveAndFindByAppIdAndWorkflowKey() {
        App app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));
        String workflowUuid = UUID.randomUUID()
            .toString();

        appWorkflowRepository.save(newBinding(app, "approve_refund", 11L, workflowUuid));

        AppWorkflow found = appWorkflowRepository.findByAppIdAndWorkflowKey(app.getId(), "approve_refund")
            .orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getAppId()).isEqualTo(app.getId());
        assertThat(found.getWorkflowKey()).isEqualTo("approve_refund");
        assertThat(found.getProjectDeploymentId()).isEqualTo(11L);
        assertThat(found.getWorkflowUuid()).isEqualTo(workflowUuid);
        assertThat(appWorkflowRepository.findByAppIdAndWorkflowKey(app.getId(), "missing")).isEmpty();
    }

    @Test
    void testFindAllByAppIdIsOrderedByWorkflowKey() {
        App app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));

        appWorkflowRepository.save(newBinding(app, "b", 11L, UUID.randomUUID()
            .toString()));
        appWorkflowRepository.save(newBinding(app, "a", 12L, UUID.randomUUID()
            .toString()));

        assertThat(appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(app.getId()))
            .extracting(AppWorkflow::getWorkflowKey)
            .containsExactly("a", "b");
    }

    @Test
    void testDuplicateWorkflowKeyPerAppIsRejected() {
        App app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));

        appWorkflowRepository.save(newBinding(app, "approve_refund", 11L, UUID.randomUUID()
            .toString()));

        AppWorkflow duplicate = newBinding(app, "approve_refund", 12L, UUID.randomUUID()
            .toString());

        assertThatThrownBy(() -> appWorkflowRepository.save(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testDeletingTheAppDeletesItsWorkflowBindings() {
        App app = appRepository.save(AppRepositoryIntTest.newApp("crm-dashboard", "a".repeat(64)));

        appWorkflowRepository.save(newBinding(app, "approve_refund", 11L, UUID.randomUUID()
            .toString()));

        appRepository.deleteById(app.getId());

        assertThat(appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(app.getId())).isEmpty();
    }

    private static AppWorkflow newBinding(App app, String workflowKey, long projectDeploymentId, String workflowUuid) {
        AppWorkflow appWorkflow = new AppWorkflow();

        appWorkflow.setAppId(app.getId());
        appWorkflow.setWorkflowKey(workflowKey);
        appWorkflow.setProjectDeploymentId(projectDeploymentId);
        appWorkflow.setWorkflowUuid(workflowUuid);

        return appWorkflow;
    }
}
