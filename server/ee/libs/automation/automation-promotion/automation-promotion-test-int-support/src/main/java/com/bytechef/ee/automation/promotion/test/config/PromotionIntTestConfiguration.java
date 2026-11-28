/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.test.config;

import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacadeImpl;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableMethodSecurity
@Import({
    LiquibaseConfiguration.class, PromotionIntTestJdbcConfiguration.class
})
@TestConfiguration
public class PromotionIntTestConfiguration {

    private static final String MATCH_NOTHING = "%1$s = 0";

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    EnvironmentService environmentService() {
        return () -> Arrays.asList(Environment.values());
    }

    @Bean
    ProjectOwnerContributor embeddedAutomationProjectOwnerContributor() {
        return ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_AUTOMATION_PROJECT, MATCH_NOTHING);
    }

    @Bean
    ProjectOwnerContributor embeddedConnectedUserProjectOwnerContributor() {
        return ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CONNECTED_USER_PROJECT, MATCH_NOTHING);
    }

    @Bean
    ProjectDeploymentOwnerContributor embeddedProjectDeploymentOwnerContributor() {
        return ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, MATCH_NOTHING);
    }

    @Bean
    Evaluator evaluator() {
        return SpelEvaluator.create();
    }

    @Bean
    RecordingPermissionService permissionService() {
        return new RecordingPermissionService();
    }

    @Bean
    WebhookTriggerTestFacade webhookTriggerTestFacade() {
        return mock(WebhookTriggerTestFacade.class);
    }

    @Bean
    WorkflowFacade workflowFacade(
        ComponentConnectionFacade componentConnectionFacade, ComponentDefinitionService componentDefinitionService,
        WorkflowValidatorFacade workflowValidatorFacade, WorkflowService workflowService) {

        return new WorkflowFacadeImpl(
            componentConnectionFacade, componentDefinitionService, workflowValidatorFacade, workflowService);
    }

    @Bean
    WorkflowValidatorFacade workflowValidatorFacade() {
        return mock(WorkflowValidatorFacade.class);
    }

    @Bean
    WorkflowService workflowService(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
    }
}
