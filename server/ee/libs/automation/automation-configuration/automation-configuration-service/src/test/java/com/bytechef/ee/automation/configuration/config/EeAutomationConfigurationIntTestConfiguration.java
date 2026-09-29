/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.config;

import static org.mockito.Mockito.mock;

import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.Encryption;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring test configuration for EE automation-configuration repository {@code *IntTest}s. Wires the EE repository
 * package and the CE automation-configuration repository package (so the EE tests can construct CE {@code Project} /
 * {@code Workspace} rows the EE {@code workspace_user} rows depend on), plus Liquibase for full schema bootstrap.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.automation.configuration.repository",
        "com.bytechef.ee.automation.configuration.repository"
    })
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@Import(LiquibaseConfiguration.class)
@Configuration
public class EeAutomationConfigurationIntTestConfiguration {

    @Bean
    Encryption encryption() {
        return mock(Encryption.class);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class EeAutomationConfigurationIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        @Override
        protected List<?> userConverters() {
            ObjectMapper objectMapper = new JsonMapper();

            return List.of(
                new MapWrapperToStringConverter(objectMapper), new StringToMapWrapperConverter(objectMapper));
        }
    }
}
