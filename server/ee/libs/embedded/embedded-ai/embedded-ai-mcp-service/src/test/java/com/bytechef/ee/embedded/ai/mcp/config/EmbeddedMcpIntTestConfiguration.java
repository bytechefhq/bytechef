/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.config;

import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.ee.embedded.ai.mcp.event.McpToolBeforeDeleteEventListener;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolServiceImpl;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.mcp.service.McpToolServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * Boots the real {@link McpToolServiceImpl}, {@link McpIntegrationInstanceToolServiceImpl} and
 * {@link McpToolBeforeDeleteEventListener} on a PostgreSQL schema built from every changelog on the test classpath.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Import({
    EncryptionImpl.class, JacksonConfiguration.class, LiquibaseConfiguration.class,
    McpIntegrationInstanceToolServiceImpl.class, McpToolBeforeDeleteEventListener.class, McpToolServiceImpl.class,
    PostgreSQLContainerConfiguration.class
})
@EnableAutoConfiguration
@Configuration
public class EmbeddedMcpIntTestConfiguration {

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class EmbeddedMcpIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public EmbeddedMcpIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
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
