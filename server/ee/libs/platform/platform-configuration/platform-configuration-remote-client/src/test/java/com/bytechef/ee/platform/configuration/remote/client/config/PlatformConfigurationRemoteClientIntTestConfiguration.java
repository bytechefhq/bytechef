/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.client.config;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.component.airtable.AirtableComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionServiceImpl;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionServiceImpl;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacadeImpl;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationConnectionRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationServiceImpl;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactoryResolver;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan("com.bytechef.platform.configuration.workflow.connection")
@EnableAutoConfiguration
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class,
    PlatformConfigurationRemoteClientIntTestConfiguration.PlatformConfigurationJdbcConfiguration.class
})
@SpringBootConfiguration
public class PlatformConfigurationRemoteClientIntTestConfiguration {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean
    ClusterElementDefinitionService clusterElementDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory,
        ObjectProvider<MeterRegistry> meterRegistryObjectProvider) {

        return new ClusterElementDefinitionServiceImpl(
            componentDefinitionRegistry, contextFactory, meterRegistryObjectProvider);
    }

    @Bean
    ComponentConnectionFacade componentConnectionFacade(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentConnectionFactoryResolver componentConnectionFactoryResolver,
        ComponentDefinitionService componentDefinitionService, WorkflowService workflowService) {

        return new ComponentConnectionFacadeImpl(
            clusterElementDefinitionService, List.of(), componentDefinitionService,
            componentConnectionFactoryResolver, workflowService);
    }

    @Bean
    ComponentDefinitionRegistry componentDefinitionRegistry() {
        return new ComponentDefinitionRegistry(
            new ApplicationProperties(), List.of(new AirtableComponentHandler()), List::of, List.of());
    }

    @Bean
    ComponentDefinitionService componentDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory) {

        return new ComponentDefinitionServiceImpl(List.of(), componentDefinitionRegistry, contextFactory);
    }

    @Bean
    ContextFactory contextFactory() {
        return Mockito.mock(ContextFactory.class);
    }

    @Bean
    WorkflowService workflowService(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
    }

    @Bean
    WorkflowTestConfigurationService workflowTestConfigurationService(
        WorkflowTestConfigurationRepository workflowTestConfigurationRepository,
        WorkflowTestConfigurationConnectionRepository workflowTestConfigurationConnectionRepository) {

        return new WorkflowTestConfigurationServiceImpl(
            workflowTestConfigurationRepository, workflowTestConfigurationConnectionRepository);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class PlatformConfigurationJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        PlatformConfigurationJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(new MapWrapperToStringConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper));
        }
    }
}
