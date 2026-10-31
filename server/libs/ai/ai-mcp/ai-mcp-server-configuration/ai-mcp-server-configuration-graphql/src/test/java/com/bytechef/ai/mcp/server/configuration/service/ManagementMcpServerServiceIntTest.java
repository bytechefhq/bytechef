/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.mcp.server.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.repository.PropertyRepository;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.configuration.service.PropertyServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ManagementMcpServerServiceIntTest.ManagementMcpServerServiceIntTestConfiguration.class,
    properties = "bytechef.public-url=http://localhost:8080", webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ManagementMcpServerServiceIntTest {

    private static final String MCP_SERVER_PROPERTY_KEY = "mcp.server";

    @Autowired
    private ManagementMcpServerService managementMcpServerService;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyService propertyService;

    @BeforeEach
    void beforeEach() {
        propertyRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        propertyRepository.deleteAll();
    }

    @Test
    void testGetManagementMcpServerUrlFirstTimeSavesOnlyTheSecretKey() {
        assertThat(fetchMcpServerProperty()).isEmpty();

        String url = managementMcpServerService.getManagementMcpServerUrl();

        Map<String, ?> value = getMcpServerPropertyValue();

        assertThat(value).containsOnlyKeys("secretKey");
        assertThat(url).isEqualTo("http://localhost:8080/api/management/" + value.get("secretKey") + "/mcp");
        assertThat(managementMcpServerService.getManagementMcpServerUrl()).isEqualTo(url);
    }

    @Test
    void testGetManagementMcpServerUrlReusesTheStoredSecretKey() {
        saveSecretKey("abc");

        assertThat(managementMcpServerService.getManagementMcpServerUrl())
            .isEqualTo("http://localhost:8080/api/management/abc/mcp");

        assertThat(getMcpServerPropertyValue()).isEqualTo(Map.of("secretKey", "abc"));
        assertThat(propertyRepository.count()).isEqualTo(1);
    }

    @Test
    void testGetManagementMcpServerCanonicalUrlReturnsTenantlessUrlWithoutTouchingTheProperty() {
        assertThat(managementMcpServerService.getManagementMcpServerCanonicalUrl())
            .isEqualTo("http://localhost:8080/api/management/mcp");

        assertThat(fetchMcpServerProperty()).isEmpty();
    }

    @Test
    void testUpdateManagementMcpServerUrlRotatesTheSecretKeyAndDropsTheLegacyAuthenticationFlag() {
        propertyService.save(
            MCP_SERVER_PROPERTY_KEY, Map.of("authentication", true, "secretKey", "abc"), Property.Scope.PLATFORM,
            null);

        String url = managementMcpServerService.updateManagementMcpServerUrl();

        Map<String, ?> value = getMcpServerPropertyValue();

        assertThat(value).containsOnlyKeys("secretKey");
        assertThat(value.get("secretKey")).isNotEqualTo("abc");
        assertThat(url).isEqualTo("http://localhost:8080/api/management/" + value.get("secretKey") + "/mcp");
    }

    @Nested
    @TestPropertySource(properties = "bytechef.public-url=http://localhost:8080/")
    class TrailingSlashPublicUrlIntTest {

        @Autowired
        private ManagementMcpServerService trailingSlashManagementMcpServerService;

        @Test
        void testGetManagementMcpServerCanonicalUrlStripsTrailingSlashFromPublicUrl() {
            assertThat(trailingSlashManagementMcpServerService.getManagementMcpServerCanonicalUrl())
                .isEqualTo("http://localhost:8080/api/management/mcp");
        }

        @Test
        void testGetManagementMcpServerUrlStripsTrailingSlashFromPublicUrl() {
            saveSecretKey("abc");

            assertThat(trailingSlashManagementMcpServerService.getManagementMcpServerUrl())
                .isEqualTo("http://localhost:8080/api/management/abc/mcp");
        }
    }

    private Optional<Property> fetchMcpServerProperty() {
        return propertyService.fetchProperty(MCP_SERVER_PROPERTY_KEY, Property.Scope.PLATFORM, null);
    }

    private Map<String, ?> getMcpServerPropertyValue() {
        Property property = fetchMcpServerProperty().orElseThrow();

        return property.getValue();
    }

    private void saveSecretKey(String secretKey) {
        propertyService.save(MCP_SERVER_PROPERTY_KEY, Map.of("secretKey", secretKey), Property.Scope.PLATFORM, null);
    }

    @ComponentScan(basePackages = "com.bytechef.encryption")
    @Configuration
    @EnableAutoConfiguration(excludeName = "org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration")
    @EnableConfigurationProperties(ApplicationProperties.class)
    @Import({
        JacksonConfiguration.class, LiquibaseConfiguration.class, ManagementMcpServerServiceImpl.class,
        PostgreSQLContainerConfiguration.class, PropertyServiceImpl.class
    })
    static class ManagementMcpServerServiceIntTestConfiguration {

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }

        @Configuration
        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class ManagementMcpServerServiceIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final Encryption encryption;
            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            ManagementMcpServerServiceIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
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
}
