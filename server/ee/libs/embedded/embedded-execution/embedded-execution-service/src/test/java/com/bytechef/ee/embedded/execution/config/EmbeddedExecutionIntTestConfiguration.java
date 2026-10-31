/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.config;

import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserServiceImpl;
import com.bytechef.ee.embedded.execution.facade.ToolFacadeImpl;
import com.bytechef.ee.embedded.execution.util.ConnectionIdHelper;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads the embedded tool facade together with the real connected user and integration services and their JDBC
 * repositories. The automation configuration, category, security and tag modules are on the test runtime classpath only
 * for the Liquibase changelogs the embedded tables reference, so their repository configurations are excluded.
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
        "com.bytechef.platform.category.config.PlatformCategoryJdbcRepositoryConfiguration",
        "com.bytechef.platform.security.config.PlatformSecurityJdbcRepositoryConfiguration",
        "com.bytechef.platform.tag.config.PlatformTagJdbcRepositoryConfiguration"
    })
@Import({
    ConnectedUserServiceImpl.class, ConnectionIdHelper.class, IntegrationInstanceConfigurationServiceImpl.class,
    IntegrationInstanceServiceImpl.class, IntegrationServiceImpl.class, JacksonConfiguration.class,
    LiquibaseConfiguration.class, ToolFacadeImpl.class
})
@Configuration
public class EmbeddedExecutionIntTestConfiguration {

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
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
            return Arrays.asList(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
                new MapWrapperToStringConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper));
        }
    }
}
