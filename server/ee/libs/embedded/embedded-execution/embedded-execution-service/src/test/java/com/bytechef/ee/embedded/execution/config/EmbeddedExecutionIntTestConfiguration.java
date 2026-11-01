/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.config;

import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserServiceImpl;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogEventListener;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogServiceImpl;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.tool.execution.config.ToolExecutionConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = {
    "com.bytechef.encryption", "com.bytechef.ee.embedded.execution.facade", "com.bytechef.ee.embedded.execution.util"
})
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableMethodSecurity
@Import({
    ConnectedUserServiceImpl.class, IntegrationInstanceConfigurationServiceImpl.class,
    EmbeddedExecutionIntTestSupport.class, IntegrationInstanceServiceImpl.class, IntegrationServiceImpl.class,
    JacksonConfiguration.class,
    LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class, ToolExecutionConfiguration.class,
    ToolInvocationLogEventListener.class, ToolInvocationLogServiceImpl.class
})
@Configuration
public class EmbeddedExecutionIntTestConfiguration {

    @Bean
    ActionDefinitionFacade actionDefinitionFacade() {
        return mock(ActionDefinitionFacade.class);
    }

    @Bean
    ClusterElementDefinitionFacade clusterElementDefinitionFacade() {
        return mock(ClusterElementDefinitionFacade.class);
    }

    @Bean
    ClusterElementDefinitionService clusterElementDefinitionService() {
        return mock(ClusterElementDefinitionService.class);
    }

    @Bean
    ComponentDefinitionService componentDefinitionService() {
        return mock(ComponentDefinitionService.class);
    }

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    ProjectOwnershipService projectOwnershipService() {
        return mock(ProjectOwnershipService.class);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class EmbeddedExecutionIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public EmbeddedExecutionIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
                new MapWrapperToStringConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper));
        }
    }
}
