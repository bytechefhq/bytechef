/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.remote.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.facade.AuditEventFacade;
import com.bytechef.ee.platform.audit.remote.client.config.AuditRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.audit.remote.grpc.AuditEventProtoMapper;
import com.bytechef.ee.platform.audit.remote.grpc.AuditEventRemoteServiceGrpcServer;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventServiceImpl;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = AuditRemoteClientIntTestConfiguration.class)
@WithMockUser(authorities = "ROLE_ADMIN")
class RemoteAuditEventFacadeClientIntTest {

    @Autowired
    private AuditEventFacade auditEventFacade;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditEventServiceImpl auditEventService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    private ManagedChannel managedChannel;
    private RemoteAuditEventFacadeClient remoteAuditEventFacadeClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                new AuditEventRemoteServiceGrpcServer(
                    auditEventFacade, auditEventService, auditEventRepository, new AuditEventProtoMapper()))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteAuditEventFacadeClient = new RemoteAuditEventFacadeClient(new FixedGrpcChannels(managedChannel));

        persistenceAuditEventRepository.save(createPersistentAuditEvent("PROJECT_CREATED"));
        persistenceAuditEventRepository.save(createPersistentAuditEvent("PROJECT_CREATED"));
        persistenceAuditEventRepository.save(createPersistentAuditEvent("PROJECT_DELETED"));
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testFetchAuditEventsFiltersAndPages() {
        Page<PersistentAuditEvent> persistentAuditEventPage = remoteAuditEventFacadeClient.fetchAuditEvents(
            new AuditEventFilter("PROJECT_CREATED", null, null, null, true, null), PageRequest.of(0, 1));

        assertThat(persistentAuditEventPage.getTotalElements()).isEqualTo(2);
        assertThat(persistentAuditEventPage.getContent()).hasSize(1);
        assertThat(persistentAuditEventPage.getContent()
            .getFirst()
            .getEventType()).isEqualTo("PROJECT_CREATED");
    }

    @Test
    void testFetchEventTypes() {
        assertThat(remoteAuditEventFacadeClient.fetchEventTypes()).containsExactly(
            "PROJECT_CREATED", "PROJECT_DELETED");
    }

    private static PersistentAuditEvent createPersistentAuditEvent(String eventType) {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setData(Map.of("projectId", "1050"));
        persistentAuditEvent.setEventDate(LocalDateTime.now());
        persistentAuditEvent.setEventType(eventType);
        persistentAuditEvent.setPrincipal("admin@localhost.com");

        return persistentAuditEvent;
    }
}
