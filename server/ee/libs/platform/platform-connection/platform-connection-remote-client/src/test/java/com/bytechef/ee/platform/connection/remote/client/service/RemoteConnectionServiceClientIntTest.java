/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionProtoMapper;
import com.bytechef.ee.platform.connection.remote.grpc.ConnectionRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.connection.service.ConnectionServiceImpl;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = RemoteConnectionServiceClientIntTest.Config.class)
@Import(PostgreSQLContainerConfiguration.class)
class RemoteConnectionServiceClientIntTest {

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ObjectMapper objectMapper;

    private ManagedChannel managedChannel;
    private RemoteConnectionServiceClient remoteConnectionServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                new ConnectionRemoteServiceGrpcServer(connectionService, new ConnectionProtoMapper(objectMapper)))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteConnectionServiceClient = new RemoteConnectionServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        connectionRepository.deleteAll();
    }

    @Test
    void testUpdateCreatedByHandsTheConnectionToTheConnectedUser() {
        Connection connection = connectionService.create(
            Connection.builder()
                .authorizationType(AuthorizationType.BEARER_TOKEN)
                .componentName("httpClient")
                .connectionVersion(1)
                .name("provisioned")
                .parameters(Map.of("token", "secret"))
                .type(PlatformType.EMBEDDED)
                .build());

        assertThat(connection.getCreatedBy()).isEqualTo("system");

        Connection updatedConnection = remoteConnectionServiceClient.updateCreatedBy(
            connection.getId(), "external-user");

        assertThat(updatedConnection.getId()).isEqualTo(connection.getId());
        assertThat(updatedConnection.getCreatedBy()).isEqualTo("external-user");

        Connection storedConnection = connectionService.getConnection(connection.getId());

        assertThat(storedConnection.getCreatedBy()).isEqualTo("external-user");
        assertThat(storedConnection.getName()).isEqualTo("provisioned");
        assertThat(storedConnection.getParameters()).isEqualTo(Map.of("token", "secret"));
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class, RemoteConnectionServiceClientIntTest.Config.ConnectionJdbcConfiguration.class
    })
    @SpringBootConfiguration
    static class Config {

        @Bean
        ConnectionService connectionService(ConnectionRepository connectionRepository) {
            return new ConnectionServiceImpl(connectionRepository);
        }

        @Bean
        Encryption encryption(EncryptionKey encryptionKey) {
            return new EncryptionImpl(encryptionKey);
        }

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class ConnectionJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final Encryption encryption;
            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            ConnectionJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
                this.encryption = encryption;
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return List.of(
                    new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                    new EncryptedStringToMapWrapperConverter(encryption, objectMapper));
            }
        }
    }
}
