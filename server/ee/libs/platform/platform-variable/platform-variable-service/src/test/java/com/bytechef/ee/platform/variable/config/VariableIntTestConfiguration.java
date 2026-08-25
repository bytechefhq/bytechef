/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.config;

import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.ee.platform.variable.service.VariableService;
import com.bytechef.ee.platform.variable.service.VariableServiceImpl;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.repository.PropertyRepository;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.configuration.service.PropertyServiceImpl;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.service.DatabaseCredentialStore;
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
 * @version ee
 */
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
@EnableAutoConfiguration
@Configuration
public class VariableIntTestConfiguration {

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    Encryption encryption(EncryptionKey encryptionKey) {
        return new EncryptionImpl(encryptionKey);
    }

    @Bean
    CredentialStore databaseCredentialStore() {
        return new DatabaseCredentialStore();
    }

    @Bean
    PropertyService propertyService(List<CredentialStore> credentialStores, PropertyRepository propertyRepository) {
        return new PropertyServiceImpl(credentialStores, propertyRepository);
    }

    @Bean
    VariableService variableService(PropertyService propertyService) {
        return new VariableServiceImpl(propertyService);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class VariableIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public VariableIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper));
        }
    }
}
