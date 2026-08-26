/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.contextstore.audit.ContextStoreSourceAuditPublisher;
import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreSourceFacadeImpl;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreService;
import com.bytechef.ee.platform.contextstore.clickhouse.ClickHouseTableProvisioner;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacadeImpl;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * Wires the real {@code automation.configuration} graph (so {@link ProjectDeploymentFacade} actually persists and
 * registers triggers) together with a real {@link WorkspaceContextStoreSourceFacadeImpl}. The Context Store-specific
 * collaborators are Mockito mocks because the scheduling behavior under test only exercises the source→deployment
 * delegation, not the ClickHouse/replica plumbing.
 *
 * @version ee
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.commons.util", "com.bytechef.jackson.config",
        "com.bytechef.platform.category", "com.bytechef.automation.configuration", "com.bytechef.platform.connection",
        "com.bytechef.platform.tag", "com.bytechef.platform.configuration.service"
    })
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@Import(LiquibaseConfiguration.class)
@Configuration
public class ContextStoreSchedulingIntTestConfiguration {

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private PrincipalJobFacade principalJobFacade;

    @Bean
    Evaluator evaluator() {
        return SpelEvaluator.create();
    }

    @Bean
    SharedTemplateFileStorage sharedFileStorage() {
        SharedTemplateFileStorage sharedTemplateFileStorage = mock(SharedTemplateFileStorage.class);

        Mockito.when(
            sharedTemplateFileStorage.storeFileContent(anyString(), any(InputStream.class)))
            .thenAnswer(invocation -> {
                String name = invocation.getArgument(0);

                FileEntry fileEntry = mock(FileEntry.class);

                Mockito.when(fileEntry.getName())
                    .thenReturn(name);
                Mockito.when(fileEntry.toId())
                    .thenReturn(String.valueOf(UUID.randomUUID()));

                return fileEntry;
            });

        return sharedTemplateFileStorage;
    }

    @Bean
    WebhookTriggerTestFacade webhookTriggerTestFacade() {
        return mock(WebhookTriggerTestFacade.class);
    }

    @Bean
    WorkflowFacade workflowFacade(
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

    @Bean
    ContextStoreSourceAuditPublisher contextStoreSourceAuditPublisher() {
        return mock(ContextStoreSourceAuditPublisher.class);
    }

    @Bean
    ContextStoreSourceService contextStoreSourceService() {
        return mock(ContextStoreSourceService.class);
    }

    @Bean
    WorkspaceContextStoreService workspaceContextStoreService() {
        return mock(WorkspaceContextStoreService.class);
    }

    @Bean
    WorkspaceContextStoreSourceFacadeImpl workspaceContextStoreSourceFacade(
        ObjectProvider<ClickHouseTableProvisioner> clickHouseTableProvisionerProvider,
        ContextStoreSourceAuditPublisher contextStoreSourceAuditPublisher,
        ContextStoreSourceService contextStoreSourceService, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService,
        WorkspaceContextStoreService workspaceContextStoreService) {

        return new WorkspaceContextStoreSourceFacadeImpl(
            clickHouseTableProvisionerProvider, componentDefinitionService, contextStoreSourceAuditPublisher,
            contextStoreSourceService, principalJobFacade, projectDeploymentFacade, projectDeploymentService,
            projectDeploymentWorkflowService, projectService, projectWorkflowService, new SyncTaskExecutor(),
            workflowService, workspaceContextStoreService, List.of());
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class ContextStoreSchedulingIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public ContextStoreSchedulingIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new FileEntryToStringConverter(objectMapper),
                new MapWrapperToStringConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper));
        }
    }
}
