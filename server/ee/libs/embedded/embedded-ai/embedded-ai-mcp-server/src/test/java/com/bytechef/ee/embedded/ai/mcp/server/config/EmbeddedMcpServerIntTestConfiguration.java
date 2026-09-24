/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.config;

import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserServiceImpl;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import com.bytechef.platform.mcp.service.McpServerServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@EnableCaching
@Import({
    ConnectedUserServiceImpl.class, EncryptionImpl.class, IntegrationInstanceConfigurationServiceImpl.class,
    IntegrationInstanceConfigurationWorkflowServiceImpl.class, IntegrationInstanceServiceImpl.class,
    IntegrationInstanceWorkflowServiceImpl.class, IntegrationServiceImpl.class, JacksonConfiguration.class,
    LiquibaseConfiguration.class, McpComponentServiceImpl.class, McpIntegrationInstanceConfigurationServiceImpl.class,
    McpIntegrationInstanceConfigurationWorkflowServiceImpl.class, McpIntegrationInstanceToolServiceImpl.class,
    McpServerServiceImpl.class, PostgreSQLContainerConfiguration.class, WorkflowServiceImpl.class
})
@Configuration
public class EmbeddedMcpServerIntTestConfiguration {

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    Evaluator evaluator() {
        return SpelEvaluator.create();
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class EmbeddedMcpServerIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public EmbeddedMcpServerIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new MapWrapperToStringConverter(objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
                new StringToMapWrapperConverter(objectMapper));
        }
    }
}
