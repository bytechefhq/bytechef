/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflowbridge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.repository.ProjectCodeWorkflowRepository;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowServiceImpl;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
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
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring context for {@code AutomationCodeWorkflowBridgeIntTest}, wiring the REAL object graph that the test exercises:
 * {@code ProjectService}/{@code ProjectWorkflowService}/{@code ProjectDeploymentService}/
 * {@code ProjectDeploymentWorkflowService} (CE automation-configuration), {@code ProjectCodeWorkflowService} (EE
 * automation-configuration), {@code CodeWorkflowContainerService}/{@code CodeWorkflowContainerFacade} (platform code
 * workflow), {@code ConnectedUserService} (embedded-connected-user), and the whole embedded-configuration facade layer
 * ({@code AutomationWorkflowProjectFacade}, {@code AutomationWorkflowProjectCodeWorkflowFacade},
 * {@code ConnectedUserCodeWorkflowReferenceFacade}, {@code ConnectedUserProjectWorkflowManager},
 * {@code ConnectedUserWorkflowConnectionResolver}).
 *
 * <p>
 * Deliberately placed OUTSIDE the {@code com.bytechef.ee.embedded.configuration} package tree (unlike its sibling test
 * configs): this class's own {@code @ComponentScan} of that tree (needed to pick up the real embedded facades) would
 * otherwise recursively sweep up sibling test-only {@code @Configuration} classes living inside it (e.g.
 * {@code AutomationWorkflowProjectFacadeIntTestConfiguration}, {@code IntegrationIntTestConfiguration}, the nested
 * config in {@code IntegrationFacadeIntTest}) as extra beans -- and, more importantly, living inside that tree would
 * make THIS class get swept up by THEIR scans in turn, risking duplicate/incompatible bean definitions in whichever
 * test runs first. Keeping this config in a disjoint package avoids that cross-test contamination without having to
 * edit any shared test-support file.
 *
 * <p>
 * Mock boundary: {@code CodeWorkflowFileStorage} (blob storage mechanics, irrelevant to the deploy/redeploy/dangling
 * seam under test) and the {@code ProjectDeploymentFacade} collaborators unrelated to deployment provisioning itself
 * ({@code ComponentConnectionFacade}, {@code ConnectionService}, {@code EnvironmentService}, job/trigger execution
 * services, MCP/API-key/OAuth2 services) are mocked in the test class's {@code @MockitoBean} list. Everything else in
 * the deploy -> redeploy -> dangling-detection -> reference-provisioning chain is real, backed by the Testcontainers
 * Postgres instance.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.automation.configuration",
        "com.bytechef.ee.embedded.configuration",
        "com.bytechef.ee.embedded.connected.user.service",
        "com.bytechef.ee.platform.codeworkflow.configuration",
        "com.bytechef.encryption",
        "com.bytechef.platform.category",
        "com.bytechef.platform.configuration.service",
        "com.bytechef.platform.connection",
        "com.bytechef.platform.tag"
    },
    excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*IntTest.*"))
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.automation.configuration.repository")
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class AutomationCodeWorkflowBridgeIntTestConfiguration {

    @Bean
    ComponentDefinitionFacade componentDefinitionFacade() {
        return mock(ComponentDefinitionFacade.class);
    }

    @Bean
    CodeWorkflowFileStorage codeWorkflowFileStorage() {
        CodeWorkflowFileStorage codeWorkflowFileStorage = mock(CodeWorkflowFileStorage.class);

        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any(byte[].class)))
            .thenAnswer(invocation -> {
                String name = invocation.getArgument(0);

                FileEntry fileEntry = mock(FileEntry.class);

                when(fileEntry.getName())
                    .thenReturn(name);
                when(fileEntry.toId())
                    .thenReturn(String.valueOf(UUID.randomUUID()));

                return fileEntry;
            });

        return codeWorkflowFileStorage;
    }

    @Bean
    Evaluator evaluator() {
        return SpelEvaluator.create();
    }

    /**
     * Declared as a plain {@code @Bean} (its repository comes from the EE automation-configuration module's own
     * autoconfigured JDBC repositories, independent of component scanning) rather than picked up by scanning
     * {@code com.bytechef.ee.automation.configuration.service}: that package also holds
     * {@code CustomRoleServiceImpl}/{@code WorkspaceUserServiceImpl}/{@code PermissionServiceImpl} and others, which
     * pull in a much larger RBAC/audit object graph this test has no use for.
     */
    @Bean
    ProjectCodeWorkflowService projectCodeWorkflowService(ProjectCodeWorkflowRepository projectCodeWorkflowRepository) {
        return new ProjectCodeWorkflowServiceImpl(projectCodeWorkflowRepository);
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

    @Bean
    WorkflowTemplateProxyClient workflowTemplateProxyClient() {
        return mock(WorkflowTemplateProxyClient.class);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AutomationCodeWorkflowBridgeIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public AutomationCodeWorkflowBridgeIntTestJdbcConfiguration(ObjectMapper objectMapper) {
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
