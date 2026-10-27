/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.config;

import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.Encryption;
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
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.credential.store.service.DatabaseCredentialStore;
import com.bytechef.platform.mcp.web.McpServerTypeDefinition;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.automation.ai.a2a", "com.bytechef.automation.ai.mcp", "com.bytechef.automation.configuration",
        "com.bytechef.commons.util", "com.bytechef.ee.automation.apiplatform.configuration",
        "com.bytechef.ee.automation.promotion", "com.bytechef.encryption", "com.bytechef.jackson.config",
        "com.bytechef.platform.category", "com.bytechef.platform.configuration.workflow.diff",
        "com.bytechef.platform.connection", "com.bytechef.platform.mcp", "com.bytechef.platform.tag"
    },
    excludeFilters = @Filter(
        type = FilterType.REGEX,
        pattern = "com\\.bytechef\\.automation\\.configuration\\.facade\\.AutomationSearchFacadeImpl"))
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableMethodSecurity
@Import({
    AutomationMethodSecurityConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class EnvironmentPromotionIntTestConfiguration {

    private static final String MATCH_NOTHING = "%1$s = 0";

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Bean
    DatabaseCredentialStore databaseCredentialStore() {
        return new DatabaseCredentialStore();
    }

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    EnvironmentService environmentService() {
        return () -> Arrays.asList(Environment.values());
    }

    @Bean
    McpServerTypeDefinition automationMcpServerTypeDefinition() {
        return new McpServerTypeDefinition() {

            @Override
            public PlatformType getType() {
                return PlatformType.AUTOMATION;
            }

            @Override
            public boolean isSharedAdministration() {
                return true;
            }

            @Override
            public String resolvePath(String secretKey) {
                return "/api/automation/" + secretKey + "/mcp";
            }
        };
    }

    @Bean
    ProjectOwnerContributor embeddedAutomationWorkflowProjectOwnerContributor() {
        return ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_AUTOMATION_WORKFLOW_PROJECT, MATCH_NOTHING);
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
    WebhookTriggerTestFacade webhookTriggerTestFacade() {
        return mock(WebhookTriggerTestFacade.class);
    }

    @Bean
    WorkflowFacade workflowFacade(WorkflowValidatorFacade workflowValidatorFacade, WorkflowService workflowService) {
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

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class EnvironmentPromotionIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public EnvironmentPromotionIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
                new FileEntryToStringConverter(objectMapper),
                new MapWrapperToStringConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper));
        }
    }
}
