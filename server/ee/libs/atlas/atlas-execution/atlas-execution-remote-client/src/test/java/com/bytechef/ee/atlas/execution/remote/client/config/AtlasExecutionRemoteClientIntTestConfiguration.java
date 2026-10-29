/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.config;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.repository.ContextRepository;
import com.bytechef.atlas.execution.repository.CounterRepository;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.CounterServiceImpl;
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
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * Real atlas execution services on a Postgres database, for testing the gRPC clients against the servers that wrap
 * them.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@EnableCaching
@Import({
    AtlasExecutionRemoteClientIntTestConfiguration.AtlasExecutionIntTestJdbcConfiguration.class,
    JacksonConfiguration.class, LiquibaseConfiguration.class
})
@SpringBootConfiguration
public class AtlasExecutionRemoteClientIntTestConfiguration {

    @Bean
    ContextService contextService(ContextRepository contextRepository) {
        return new ContextServiceImpl(contextRepository);
    }

    @Bean
    CounterService counterService(CounterRepository counterRepository) {
        return new CounterServiceImpl(counterRepository);
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
    JobService jobService(JobRepository jobRepository) {
        return new JobServiceImpl(jobRepository);
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
    WorkflowService workflowService(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class AtlasExecutionIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        AtlasExecutionIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new ExecutionErrorToStringConverter(objectMapper),
                new FileEntryToStringConverter(objectMapper),
                new MapWrapperToStringConverter(objectMapper),
                new StringToExecutionErrorConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper),
                new StringToWebhooksConverter(objectMapper),
                new StringToWorkflowTaskConverter(objectMapper),
                new WebhooksToStringConverter(objectMapper),
                new WorkflowTaskToStringConverter(objectMapper));
        }
    }
}
