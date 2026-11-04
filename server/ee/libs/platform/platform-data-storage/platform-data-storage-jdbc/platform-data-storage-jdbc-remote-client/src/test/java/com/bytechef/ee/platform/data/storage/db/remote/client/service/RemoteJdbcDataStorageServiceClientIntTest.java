/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.storage.db.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.data.jdbc.converter.StringToTypedValueConverter;
import com.bytechef.commons.data.jdbc.converter.TypedValueToStringConverter;
import com.bytechef.ee.platform.data.storage.db.remote.web.rest.service.RemoteJdbcDataStorageServiceController;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.data.storage.jdbc.config.JdbcDataStorageConfiguration;
import com.bytechef.platform.data.storage.jdbc.domain.VersionedValue;
import com.bytechef.platform.data.storage.jdbc.repository.DataStorageRepository;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageService;
import com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageUpdates;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(
    classes = RemoteJdbcDataStorageServiceClientIntTest.RemoteJdbcDataStorageIntTestConfiguration.class,
    properties = "bytechef.data-storage.provider=jdbc")
class RemoteJdbcDataStorageServiceClientIntTest {

    private static final String COMPONENT_NAME = "dataStorage";
    private static final long ENVIRONMENT_ID = 0;
    private static final String SCOPE_ID = "1";

    @Autowired
    private DataStorageRepository dataStorageRepository;

    @Autowired
    private JdbcDataStorageService jdbcDataStorageService;

    private RemoteJdbcDataStorageServiceClient remoteJdbcDataStorageServiceClient;

    @BeforeEach
    void beforeEach() {
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new RemoteJdbcDataStorageServiceController(jdbcDataStorageService))
            .build();

        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl("http://localhost")
            .requestFactory(new MockMvcClientHttpRequestFactory(mockMvc));

        remoteJdbcDataStorageServiceClient = new RemoteJdbcDataStorageServiceClient(
            new LoadBalancedRestClient(restClientBuilder));
    }

    @AfterEach
    void afterEach() {
        dataStorageRepository.deleteAll();
    }

    @Test
    void testFetchVersionedAndPutIfVersionRoundTrip() {
        assertThat(fetchVersioned("key")).isEmpty();

        assertThat(putIfVersion("key", "first", null)).isTrue();
        assertThat(putIfVersion("key", "again", null)).isFalse();

        VersionedValue<Object> versionedValue = fetchVersioned("key").orElseThrow();

        assertThat(versionedValue.value()).isEqualTo("first");
        assertThat(putIfVersion("key", "second", versionedValue.version())).isTrue();
        assertThat(putIfVersion("key", "third", versionedValue.version())).isFalse();

        VersionedValue<Object> updatedVersionedValue = fetchVersioned("key").orElseThrow();

        assertThat(updatedVersionedValue.value()).isEqualTo("second");
        assertThat(updatedVersionedValue.version()).isGreaterThan(versionedValue.version());
    }

    @Test
    void testUpdateThroughTheClientKeepsEveryConcurrentIncrement() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try (ExecutorService executorService = Executors.newFixedThreadPool(8)) {
            for (int threadIndex = 0; threadIndex < 8; threadIndex++) {
                futures.add(executorService.submit(() -> {
                    startLatch.await();

                    for (int iteration = 0; iteration < 10; iteration++) {
                        JdbcDataStorageUpdates.update(
                            remoteJdbcDataStorageServiceClient, COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID,
                            "counter", ENVIRONMENT_ID, PlatformType.AUTOMATION,
                            (Optional<Number> currentValue) -> (Number) (currentValue.map(Number::intValue)
                                .orElse(0) + 1));
                    }

                    return null;
                }));
            }

            startLatch.countDown();

            for (Future<?> future : futures) {
                future.get();
            }
        }

        Number counter = fetchVersioned("counter").map(versionedValue -> (Number) versionedValue.value())
            .orElseThrow();

        assertThat(counter.intValue()).isEqualTo(80);
    }

    private Optional<VersionedValue<Object>> fetchVersioned(String key) {
        return remoteJdbcDataStorageServiceClient.fetchVersioned(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, key, ENVIRONMENT_ID, PlatformType.AUTOMATION);
    }

    private boolean putIfVersion(String key, Object value, Integer expectedVersion) {
        return remoteJdbcDataStorageServiceClient.putIfVersion(
            COMPONENT_NAME, DataStorageScope.WORKFLOW, SCOPE_ID, key, ENVIRONMENT_ID, PlatformType.AUTOMATION, value,
            expectedVersion);
    }

    @EnableAutoConfiguration
    @Import({
        JdbcDataStorageConfiguration.class, LiquibaseConfiguration.class
    })
    @Configuration
    static class RemoteJdbcDataStorageIntTestConfiguration {

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class RemoteJdbcDataStorageIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            RemoteJdbcDataStorageIntTestJdbcConfiguration(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return List.of(
                    new StringToTypedValueConverter(objectMapper), new TypedValueToStringConverter(objectMapper));
            }
        }
    }
}
