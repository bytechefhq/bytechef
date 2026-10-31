/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.config;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.service.EnvironmentServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowServiceImpl;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserServiceImpl;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.file.storage.TriggerFileStorageImpl;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowTriggerConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowTriggerToStringConverter;
import com.bytechef.platform.workflow.execution.service.PrincipalJobServiceImpl;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads the real atlas execution, platform workflow execution, embedded configuration and connected user services over
 * their JDBC repositories, together with the JDBC workflow repository. The automation configuration, category, security
 * and tag modules are on the test classpath only for the Liquibase changelogs the embedded tables reference, so their
 * repository and method security configurations are excluded.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = "com.bytechef.encryption",
    excludeFilters = @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
@EnableAutoConfiguration(
    excludeName = {
        "com.bytechef.automation.configuration.config.AutomationConfigurationJdbcRepositoryConfiguration",
        "com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration",
        "com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationJdbcRepositoryConfiguration",
        "com.bytechef.platform.category.config.PlatformCategoryJdbcRepositoryConfiguration",
        "com.bytechef.platform.security.config.PlatformSecurityJdbcRepositoryConfiguration",
        "com.bytechef.platform.tag.config.PlatformTagJdbcRepositoryConfiguration"
    })
@EnableCaching
@Import({
    ConnectedUserServiceImpl.class, ContextServiceImpl.class, IntegrationInstanceConfigurationServiceImpl.class,
    IntegrationInstanceServiceImpl.class, IntegrationServiceImpl.class, IntegrationWorkflowServiceImpl.class,
    JacksonConfiguration.class, JobServiceImpl.class, LiquibaseConfiguration.class, PrincipalJobServiceImpl.class,
    TaskExecutionServiceImpl.class, TriggerExecutionServiceImpl.class
})
@Configuration
public class EmbeddedWorkflowExecutionIntTestConfiguration {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    EnvironmentService environmentService() {
        return new EnvironmentServiceImpl(new ApplicationProperties());
    }

    @Bean
    Evaluator evaluator() {
        return SpelEvaluator.create();
    }

    @Bean
    TaskFileStorage taskFileStorage() {
        return new TaskFileStorageImpl(new Base64FileStorageService());
    }

    @Bean
    TriggerFileStorage triggerFileStorage() {
        return new TriggerFileStorageImpl(new Base64FileStorageService());
    }

    @Bean
    WorkflowService workflowService(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class EmbeddedWorkflowExecutionIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public EmbeddedWorkflowExecutionIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
                new ExecutionErrorToStringConverter(objectMapper),
                new FileEntryToStringConverter(objectMapper),
                new MapWrapperToStringConverter(objectMapper),
                new StringToExecutionErrorConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper),
                new StringToWebhooksConverter(objectMapper),
                new StringToWorkflowTaskConverter(objectMapper),
                new StringToWorkflowTriggerConverter(objectMapper),
                new WebhooksToStringConverter(objectMapper),
                new WorkflowTaskToStringConverter(objectMapper),
                new WorkflowTriggerToStringConverter(objectMapper));
        }
    }
}
