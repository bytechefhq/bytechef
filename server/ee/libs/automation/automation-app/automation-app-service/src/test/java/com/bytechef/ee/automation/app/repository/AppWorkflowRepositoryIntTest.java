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
import com.bytechef.ee.automation.app.domain.AppInstance;
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
    private AppInstanceRepository appInstanceRepository;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppWorkflowRepository appWorkflowRepository;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void testSaveAndFindByAppInstanceIdAndWorkflowKey() {
        AppInstance appInstance = saveAppInstance("crm-dashboard", "a".repeat(64));
        String workflowUuid = UUID.randomUUID()
            .toString();

        appWorkflowRepository.save(newBinding(appInstance, "approve_refund", 11L, workflowUuid));

        AppWorkflow found =
            appWorkflowRepository.findByAppInstanceIdAndWorkflowKey(appInstance.getId(), "approve_refund")
                .orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getAppInstanceId()).isEqualTo(appInstance.getId());
        assertThat(found.getWorkflowKey()).isEqualTo("approve_refund");
        assertThat(found.getProjectDeploymentId()).isEqualTo(11L);
        assertThat(found.getWorkflowUuid()).isEqualTo(workflowUuid);
        assertThat(appWorkflowRepository.findByAppInstanceIdAndWorkflowKey(appInstance.getId(), "missing")).isEmpty();
    }

    @Test
    void testFindAllByAppInstanceIdIsOrderedByWorkflowKey() {
        AppInstance appInstance = saveAppInstance("crm-dashboard", "a".repeat(64));

        appWorkflowRepository.save(newBinding(appInstance, "b", 11L, UUID.randomUUID()
            .toString()));
        appWorkflowRepository.save(newBinding(appInstance, "a", 12L, UUID.randomUUID()
            .toString()));

        assertThat(appWorkflowRepository.findAllByAppInstanceIdOrderByWorkflowKey(appInstance.getId()))
            .extracting(AppWorkflow::getWorkflowKey)
            .containsExactly("a", "b");
    }

    @Test
    void testDuplicateWorkflowKeyPerAppIsRejected() {
        AppInstance appInstance = saveAppInstance("crm-dashboard", "a".repeat(64));

        appWorkflowRepository.save(newBinding(appInstance, "approve_refund", 11L, UUID.randomUUID()
            .toString()));

        AppWorkflow duplicate = newBinding(appInstance, "approve_refund", 12L, UUID.randomUUID()
            .toString());

        assertThatThrownBy(() -> appWorkflowRepository.save(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testDeletingTheAppDeletesItsWorkflowBindings() {
        AppInstance appInstance = saveAppInstance("crm-dashboard", "a".repeat(64));

        appWorkflowRepository.save(newBinding(appInstance, "approve_refund", 11L, UUID.randomUUID()
            .toString()));

        appRepository.deleteById(appInstance.getAppId());

        assertThat(appWorkflowRepository.findAllByAppInstanceIdOrderByWorkflowKey(appInstance.getId())).isEmpty();
    }

    private static AppWorkflow
        newBinding(AppInstance appInstance, String workflowKey, long projectDeploymentId, String workflowUuid) {
        AppWorkflow appWorkflow = new AppWorkflow();

        appWorkflow.setAppInstanceId(appInstance.getId());
        appWorkflow.setWorkflowKey(workflowKey);
        appWorkflow.setProjectDeploymentId(projectDeploymentId);
        appWorkflow.setWorkflowUuid(workflowUuid);

        return appWorkflow;
    }

    private AppInstance saveAppInstance(String name, String hash) {
        App app = appRepository.save(AppRepositoryIntTest.newApp(name));

        return appInstanceRepository.save(AppInstanceRepositoryIntTest.newAppInstance(app.getId(), name, hash));
    }
}
