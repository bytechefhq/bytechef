/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.security.remote.grpc.ApiKeyRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.audit.ApiKeyAuditPublisher;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.service.ApiKeyServiceImpl;
import com.bytechef.platform.user.callback.UserCallback;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = RemoteApiKeyServiceClientIntTest.Config.class,
    properties = "spring.liquibase.contexts=user,configuration")
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class RemoteApiKeyServiceClientIntTest {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private UserRepository userRepository;

    private ManagedChannel managedChannel;
    private RemoteApiKeyServiceClient remoteApiKeyServiceClient;
    private Server server;
    private User user;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new ApiKeyRemoteServiceGrpcServer(apiKeyService))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteApiKeyServiceClient = new RemoteApiKeyServiceClient(new FixedGrpcChannels(managedChannel));

        User newUser = new User();

        newUser.setActivated(true);
        newUser.setEmail("owner@example.com");
        newUser.setLogin("owner");
        newUser.setPassword("password-hash");

        user = userRepository.save(newUser);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        apiKeyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testExistsMatchesTheSecretKeyInItsEnvironment() {
        ApiKey apiKey = createApiKey(Environment.STAGING, PlatformType.AUTOMATION);

        assertThat(remoteApiKeyServiceClient.exists(apiKey.getSecretKey(), Environment.STAGING.ordinal())).isTrue();
        assertThat(remoteApiKeyServiceClient.exists(apiKey.getSecretKey(), Environment.PRODUCTION.ordinal()))
            .isFalse();
        assertThat(remoteApiKeyServiceClient.exists("unknown-secret-key", Environment.STAGING.ordinal())).isFalse();
    }

    @Test
    void testFetchApiKeyReturnsTheStoredApiKey() {
        ApiKey apiKey = createApiKey(Environment.STAGING, PlatformType.EMBEDDED);

        Instant lastUsedDate = Instant.now()
            .truncatedTo(ChronoUnit.MICROS);

        apiKey.setLastUsedDate(lastUsedDate);

        apiKeyRepository.save(apiKey);

        assertThat(remoteApiKeyServiceClient.fetchApiKey(apiKey.getSecretKey()))
            .hasValueSatisfying(fetchedApiKey -> {
                assertThat(fetchedApiKey.getId()).isEqualTo(apiKey.getId());
                assertThat(fetchedApiKey.getName()).isEqualTo("api-key");
                assertThat(fetchedApiKey.getSecretKey()).isEqualTo(apiKey.getSecretKey());
                assertThat(fetchedApiKey.getEnvironment()).isEqualTo(Environment.STAGING);
                assertThat(fetchedApiKey.getType()).isEqualTo(PlatformType.EMBEDDED);
                assertThat(fetchedApiKey.getUserId()).isEqualTo(user.getId());
                assertThat(fetchedApiKey.getCreatedBy()).isEqualTo("system");
                assertThat(fetchedApiKey.getLastUsedDate()).isEqualTo(lastUsedDate);
            });
        assertThat(remoteApiKeyServiceClient.getApiKey(apiKey.getSecretKey())
            .getId()).isEqualTo(apiKey.getId());
    }

    @Test
    void testFetchApiKeyReturnsEmptyWhenMissing() {
        createApiKey(Environment.STAGING, PlatformType.AUTOMATION);

        assertThat(remoteApiKeyServiceClient.fetchApiKey("unknown-secret-key")).isEmpty();

        assertThatThrownBy(() -> remoteApiKeyServiceClient.getApiKey("unknown-secret-key"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Api key not found.");
    }

    @Test
    void testGetApiKeyBySecretKeyAndEnvironmentReturnsTheStoredApiKey() {
        ApiKey apiKey = createApiKey(Environment.PRODUCTION, PlatformType.AUTOMATION);

        ApiKey fetchedApiKey = remoteApiKeyServiceClient.getApiKey(
            apiKey.getSecretKey(), Environment.PRODUCTION.ordinal());

        assertThat(fetchedApiKey.getId()).isEqualTo(apiKey.getId());
        assertThat(fetchedApiKey.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(fetchedApiKey.getType()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(fetchedApiKey.getUserId()).isEqualTo(user.getId());
    }

    @Test
    void testGetApiKeyBySecretKeyAndEnvironmentThrowsForAnotherEnvironment() {
        ApiKey apiKey = createApiKey(Environment.PRODUCTION, PlatformType.AUTOMATION);

        assertThatThrownBy(
            () -> remoteApiKeyServiceClient.getApiKey(apiKey.getSecretKey(), Environment.STAGING.ordinal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Api key not found for the specified environment.");
    }

    @Test
    void testUnportedMethodStillThrowsUnsupported() {
        assertThatThrownBy(() -> remoteApiKeyServiceClient.delete(1L))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private ApiKey createApiKey(Environment environment, PlatformType type) {
        ApiKey apiKey = new ApiKey();

        apiKey.setEnvironment(environment);
        apiKey.setName("api-key");
        apiKey.setType(type);
        apiKey.setUserId(user.getId());

        return apiKeyService.create(apiKey);
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class, UserCallback.class,
        RemoteApiKeyServiceClientIntTest.Config.ApiKeyJdbcConfiguration.class
    })
    @SpringBootConfiguration
    static class Config {

        @Bean
        ApiKeyService apiKeyService(
            ApplicationEventPublisher applicationEventPublisher, ApiKeyRepository apiKeyRepository) {

            return new ApiKeyServiceImpl(new ApiKeyAuditPublisher(applicationEventPublisher), apiKeyRepository);
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class ApiKeyJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
