/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.remote.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.audit.AuditEventConverter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.facade.AuditEventFacade;
import com.bytechef.ee.platform.audit.remote.client.config.AuditRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.audit.remote.grpc.AuditEventProtoMapper;
import com.bytechef.ee.platform.audit.remote.grpc.AuditEventRemoteServiceGrpcServer;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventServiceImpl;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = AuditRemoteClientIntTestConfiguration.class)
class RemoteAuditEventRepositoryClientIntTest {

    @Autowired
    private AuditEventConverter auditEventConverter;

    @Autowired
    private AuditEventFacade auditEventFacade;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditEventServiceImpl auditEventService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    private ManagedChannel managedChannel;
    private Server server;

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testAddAuditEventIsStored() throws IOException {
        RemoteAuditEventRepositoryClient remoteAuditEventRepositoryClient = createClient(auditEventRepository);

        remoteAuditEventRepositoryClient.add(
            new AuditEvent(
                Instant.now(), "admin@localhost.com", "AUTHENTICATION_SUCCESS", Map.of("source", "login")));

        List<PersistentAuditEvent> persistentAuditEvents = findAll();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("AUTHENTICATION_SUCCESS");
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("admin@localhost.com");
        assertThat(persistentAuditEvent.getData()).containsEntry("source", "login");
    }

    @Test
    void testAddAuditEventStoresItsPlatformInThePlatformColumn() throws IOException {
        RemoteAuditEventRepositoryClient remoteAuditEventRepositoryClient = createClient(auditEventRepository);

        Map<String, Object> data = new HashMap<>();

        data.put("credentialType", "API_KEY");
        data.put(AuditEventConverter.PLATFORM_DATA_KEY, PlatformType.AUTOMATION);

        remoteAuditEventRepositoryClient
            .add(new AuditEvent(Instant.now(), "admin@localhost.com", "CREDENTIAL_USED", data));

        PersistentAuditEvent persistentAuditEvent = findAll().getFirst();

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData()).containsEntry("credentialType", "API_KEY")
            .doesNotContainKey(AuditEventConverter.PLATFORM_DATA_KEY);
    }

    @Test
    void testAddAuditEventTruncatesLongValues() throws IOException {
        RemoteAuditEventRepositoryClient remoteAuditEventRepositoryClient = createClient(auditEventRepository);

        remoteAuditEventRepositoryClient.add(
            new AuditEvent(
                Instant.now(), "admin@localhost.com", "AUTHENTICATION_FAILURE", Map.of("message", "x".repeat(300))));

        assertThat(findAll().getFirst()
            .getData()
            .get("message")).hasSize(256);
    }

    @Test
    void testAddAuditEventCarriesDetailsTenant() throws IOException {
        List<AuditEvent> receivedAuditEvents = new CopyOnWriteArrayList<>();

        RemoteAuditEventRepositoryClient remoteAuditEventRepositoryClient = createClient(
            new RecordingAuditEventRepository(receivedAuditEvents));

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

        mockHttpServletRequest.setRemoteAddr("client-host");

        Map<String, Object> data = new HashMap<>();

        data.put("details", new TenantWebAuthenticationDetails(mockHttpServletRequest, "tenant1"));

        remoteAuditEventRepositoryClient.add(
            new AuditEvent(Instant.now(), "user@tenant1.com", "AUTHENTICATION_SUCCESS", data));

        assertThat(receivedAuditEvents).hasSize(1);
        assertThat(receivedAuditEvents.getFirst()
            .getData()).containsEntry(AuditEventConverter.TENANT_ID_DATA_KEY, "tenant1")
                .containsEntry("remoteAddress", "client-host")
                .doesNotContainKey("details");
    }

    private RemoteAuditEventRepositoryClient createClient(AuditEventRepository serverAuditEventRepository)
        throws IOException {

        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .addService(
                new AuditEventRemoteServiceGrpcServer(
                    auditEventFacade, auditEventService, serverAuditEventRepository, new AuditEventProtoMapper()))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        return new RemoteAuditEventRepositoryClient(new FixedGrpcChannels(managedChannel), auditEventConverter);
    }

    private List<PersistentAuditEvent> findAll() {
        List<PersistentAuditEvent> persistentAuditEvents = new ArrayList<>();

        persistenceAuditEventRepository.findAll()
            .forEach(persistentAuditEvents::add);

        return persistentAuditEvents;
    }

    private record RecordingAuditEventRepository(List<AuditEvent> auditEvents) implements AuditEventRepository {

        @Override
        public void add(AuditEvent auditEvent) {
            auditEvents.add(auditEvent);
        }

        @Override
        public List<AuditEvent> find(String principal, Instant after, String type) {
            return List.copyOf(auditEvents);
        }
    }
}
