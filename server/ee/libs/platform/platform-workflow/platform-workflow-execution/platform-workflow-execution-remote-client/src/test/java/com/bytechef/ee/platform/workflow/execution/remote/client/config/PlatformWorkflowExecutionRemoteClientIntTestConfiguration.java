/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.config;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.repository.ContextRepository;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.scheduler.QuartzConnectionRefreshScheduler;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacadeImpl;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacadeImpl;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacadeImpl;
import com.bytechef.platform.workflow.execution.repository.PrincipalJobRepository;
import com.bytechef.platform.workflow.execution.repository.TaskStateRepository;
import com.bytechef.platform.workflow.execution.repository.TriggerExecutionRepository;
import com.bytechef.platform.workflow.execution.repository.TriggerStateRepository;
import com.bytechef.platform.workflow.execution.repository.converter.JobResumeIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToJobResumeIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToTaskStateValueConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToTriggerStateValueConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowExecutionIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowTriggerConverter;
import com.bytechef.platform.workflow.execution.repository.converter.TaskStateValueToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.TriggerStateValueToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowExecutionIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowTriggerToStringConverter;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobServiceImpl;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.execution.service.TaskStateServiceImpl;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionServiceImpl;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.platform.workflow.execution.service.TriggerStateServiceImpl;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.quartz.Scheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

/**
 * Real atlas and platform workflow execution services on a Postgres database and an in-memory Quartz scheduler, for
 * testing the gRPC clients against the servers that wrap them.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@EnableCaching
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class,
    PlatformWorkflowExecutionRemoteClientIntTestConfiguration.PlatformWorkflowExecutionIntTestJdbcConfiguration.class
})
@SpringBootConfiguration
public class PlatformWorkflowExecutionRemoteClientIntTestConfiguration {

    @Bean
    ConnectionLifecycleFacade connectionLifecycleFacade(Scheduler scheduler) {
        return new ConnectionLifecycleFacadeImpl(new QuartzConnectionRefreshScheduler(scheduler));
    }

    @Bean
    ContextService contextService(ContextRepository contextRepository) {
        return new ContextServiceImpl(contextRepository);
    }

    @Bean
    JobFacade jobFacade(
        ApplicationEventPublisher applicationEventPublisher, ContextService contextService, JobService jobService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        return new JobFacadeImpl(
            applicationEventPublisher, contextService, jobService, taskExecutionService, taskFileStorage,
            workflowService);
    }

    @Bean
    JobResumeFacade jobResumeFacade(
        ApplicationEventPublisher applicationEventPublisher, ObjectProvider<ApprovalTokens> approvalTokensProvider,
        JobFacade jobFacade, JobService jobService, ObjectProvider<MeterRegistry> meterRegistryObjectProvider,
        PlatformTransactionManager platformTransactionManager, TaskExecutionService taskExecutionService,
        TaskStateService taskStateService) {

        return new JobResumeFacadeImpl(
            applicationEventPublisher, approvalTokensProvider, jobFacade, jobService, meterRegistryObjectProvider,
            platformTransactionManager, taskExecutionService, taskStateService);
    }

    @Bean
    JobService jobService(JobRepository jobRepository) {
        return new JobServiceImpl(jobRepository);
    }

    @Bean
    PrincipalJobFacade principalJobFacade(
        PrincipalJobService principalJobService, JobFacade jobFacade, JobService jobService,
        WorkflowService workflowService) {

        return new PrincipalJobFacadeImpl(principalJobService, jobFacade, jobService, workflowService);
    }

    @Bean
    PrincipalJobService principalJobService(PrincipalJobRepository principalJobRepository) {
        return new PrincipalJobServiceImpl(principalJobRepository);
    }

    @Bean
    TaskExecutionService taskExecutionService(TaskExecutionRepository taskExecutionRepository) {
        return new TaskExecutionServiceImpl(taskExecutionRepository);
    }

    @Bean
    TaskFileStorage taskFileStorage() {
        return new TaskFileStorageImpl(new Base64FileStorageService());
    }

    @Bean
    TaskStateService taskStateService(TaskStateRepository taskStateRepository) {
        return new TaskStateServiceImpl(taskStateRepository);
    }

    @Bean
    TriggerExecutionService triggerExecutionService(TriggerExecutionRepository triggerExecutionRepository) {
        return new TriggerExecutionServiceImpl(triggerExecutionRepository);
    }

    @Bean
    TriggerStateService triggerStateService(TriggerStateRepository triggerStateRepository) {
        return new TriggerStateServiceImpl(triggerStateRepository);
    }

    @Bean
    WorkflowService workflowService(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class PlatformWorkflowExecutionIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        PlatformWorkflowExecutionIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new ExecutionErrorToStringConverter(objectMapper),
                new FileEntryToStringConverter(objectMapper),
                new JobResumeIdToStringConverter(),
                new MapWrapperToStringConverter(objectMapper),
                new StringToExecutionErrorConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper),
                new StringToJobResumeIdConverter(),
                new StringToMapWrapperConverter(objectMapper),
                new StringToTaskStateValueConverter(objectMapper),
                new StringToTriggerStateValueConverter(objectMapper),
                new StringToWebhooksConverter(objectMapper),
                new StringToWorkflowExecutionIdConverter(),
                new StringToWorkflowTaskConverter(objectMapper),
                new StringToWorkflowTriggerConverter(objectMapper),
                new TaskStateValueToStringConverter(objectMapper),
                new TriggerStateValueToStringConverter(objectMapper),
                new WebhooksToStringConverter(objectMapper),
                new WorkflowExecutionIdToStringConverter(),
                new WorkflowTaskToStringConverter(objectMapper),
                new WorkflowTriggerToStringConverter(objectMapper));
        }
    }
}
