/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacadeImpl;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.githubproxy.client.WorkflowTemplateProxyClient;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.automation.configuration",
        "com.bytechef.ee.embedded.configuration",
        "com.bytechef.encryption",
        "com.bytechef.platform.category",
        "com.bytechef.platform.configuration.service",
        "com.bytechef.platform.connection",
        "com.bytechef.platform.tag"
    })
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.automation.configuration.repository")
@Import({
    IntegrationIntTestConfiguration.IntegrationIntTestJdbcConfiguration.class,
    JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class AutomationWorkflowProjectFacadeIntTestConfiguration {

    @Bean
    ComponentDefinitionFacade componentDefinitionFacade() {
        return mock(ComponentDefinitionFacade.class);
    }

    @Bean
    Evaluator evaluator() {
        return SpelEvaluator.create();
    }

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    SharedTemplateFileStorage sharedTemplateFileStorage() {
        SharedTemplateFileStorage sharedTemplateFileStorage = mock(SharedTemplateFileStorage.class);

        when(sharedTemplateFileStorage.storeFileContent(anyString(), any(InputStream.class)))
            .thenAnswer(invocation -> {
                String name = invocation.getArgument(0);

                FileEntry fileEntry = mock(FileEntry.class);

                when(fileEntry.getName())
                    .thenReturn(name);
                when(fileEntry.toId())
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
        ComponentConnectionFacade componentConnectionFacade,
        ComponentDefinitionService componentDefinitionService,
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
    WorkflowTemplateProxyClient workflowTemplateProxyClient() {
        return mock(WorkflowTemplateProxyClient.class);
    }
}
