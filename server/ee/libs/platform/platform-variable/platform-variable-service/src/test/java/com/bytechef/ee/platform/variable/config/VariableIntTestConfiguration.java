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
 * Spring test configuration for {@code VariableServiceIntTest}. Wires the real {@link PropertyServiceImpl} over the
 * real {@code property} table (Testcontainers PostgreSQL), the default {@link DatabaseCredentialStore}, and
 * {@link VariableServiceImpl} on top of it -- so the test exercises the real unique constraint and the JDBC
 * value-encryption round trip instead of a mocked {@link PropertyService}. Beans are declared explicitly rather than
 * via a broad {@code @ComponentScan} of {@code com.bytechef.platform.configuration.service} so unrelated services in
 * that package (workflow test configuration, workflow node test output) don't drag in dependencies this test does not
 * need.
 *
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

    /**
     * Registers the {@code EncryptedMapWrapper} converters used by {@code Property.value} -- both directions, unlike
     * {@code PlatformConfigurationIntTestConfiguration} in platform-configuration-service, which registers only the
     * write-side {@link EncryptedMapWrapperToStringConverter}. Without the read-side
     * {@link EncryptedStringToMapWrapperConverter} too, Spring Data JDBC has no converter from the {@code TEXT} column
     * back to {@code EncryptedMapWrapper} and a saved property could not be read back.
     */
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
