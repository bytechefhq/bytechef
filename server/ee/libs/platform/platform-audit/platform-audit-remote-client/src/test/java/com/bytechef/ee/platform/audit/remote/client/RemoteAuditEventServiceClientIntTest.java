/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.facade.AuditEventFacade;
import com.bytechef.ee.platform.audit.remote.client.config.AuditRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.audit.remote.grpc.AuditEventProtoMapper;
import com.bytechef.ee.platform.audit.remote.grpc.AuditEventRemoteServiceGrpcServer;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventServiceImpl;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = AuditRemoteClientIntTestConfiguration.class)
class RemoteAuditEventServiceClientIntTest {

    @Autowired
    private AuditEventFacade auditEventFacade;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditEventServiceImpl auditEventService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    private ManagedChannel managedChannel;
    private RemoteAuditEventServiceClient remoteAuditEventServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .addService(
                new AuditEventRemoteServiceGrpcServer(
                    auditEventFacade, auditEventService, auditEventRepository, new AuditEventProtoMapper()))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteAuditEventServiceClient = new RemoteAuditEventServiceClient(new FixedGrpcChannels(managedChannel));
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testSaveAllWithoutTransactionSendsImmediately() {
        remoteAuditEventServiceClient.saveAll(List.of(createPersistentAuditEvent("PROJECT_CREATED")));

        List<PersistentAuditEvent> persistentAuditEvents = findAll();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo("PROJECT_CREATED");
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("admin@localhost.com");
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData()).containsEntry("projectId", "1050");
    }

    @Test
    void testSaveAllAfterCommitSendsOnce() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            remoteAuditEventServiceClient.saveAll(List.of(createPersistentAuditEvent("PROJECT_CREATED")));

            assertThat(persistenceAuditEventRepository.count()).isZero();
        });

        assertThat(findAll()).hasSize(1);
    }

    @Test
    void testSaveAllAfterRollbackSendsRolledBackEvents() {
        PersistentAuditEvent persistentAuditEvent = createPersistentAuditEvent("PROJECT_DELETED");
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCompletion(int status) {
                    Map<String, String> data = new LinkedHashMap<>(persistentAuditEvent.getData());

                    data.put("result", "ROLLED_BACK");

                    persistentAuditEvent.setData(data);
                }
            });

            remoteAuditEventServiceClient.saveAll(List.of(persistentAuditEvent));

            transactionStatus.setRollbackOnly();
        });

        List<PersistentAuditEvent> persistentAuditEvents = findAll();

        assertThat(persistentAuditEvents).hasSize(1);
        assertThat(persistentAuditEvents.getFirst()
            .getData()).containsEntry("result", "ROLLED_BACK");
    }

    @Test
    void testAuditedCallInsideATransactionThatRollsBackIsSentAsRolledBackAndNotReportedLost(CapturedOutput output) {
        StaticListableBeanFactory staticListableBeanFactory = new StaticListableBeanFactory();

        staticListableBeanFactory.addBean("projectAuditMapper", new ProjectAuditMapper());

        AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory(new ProjectOperations());

        aspectJProxyFactory.addAspect(
            new AuditAspect(
                new ApplicationProperties(), remoteAuditEventServiceClient,
                new AuditMapperResolver(staticListableBeanFactory)));

        ProjectOperations projectOperations = aspectJProxyFactory.getProxy();
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            projectOperations.deleteProject();

            transactionStatus.setRollbackOnly();
        });

        assertThat(findAll())
            .singleElement()
            .satisfies(persistentAuditEvent -> {
                assertThat(persistentAuditEvent.getEventType()).isEqualTo("PROJECT_DELETED");
                assertThat(persistentAuditEvent.getData()).containsEntry("result", "ROLLED_BACK");
            });
        assertThat(output).doesNotContain("AUDIT PERSISTENCE FAILURE");
    }

    @Test
    void testSaveAllWhenServerIsDownDoesNotThrow() {
        server.shutdownNow();

        assertThatCode(
            () -> remoteAuditEventServiceClient.saveAll(List.of(createPersistentAuditEvent("PROJECT_CREATED"))))
                .doesNotThrowAnyException();
    }

    @Test
    void testSaveAllDropsNullValues() {
        PersistentAuditEvent persistentAuditEvent = createPersistentAuditEvent("PROJECT_CREATED");
        Map<String, String> data = new HashMap<>(persistentAuditEvent.getData());

        data.put("missing", null);

        persistentAuditEvent.setData(data);

        remoteAuditEventServiceClient.saveAll(List.of(persistentAuditEvent));

        assertThat(findAll().getFirst()
            .getData()).doesNotContainKey("missing")
                .containsEntry("projectId", "1050");
    }

    @Test
    void testUpdateJobCreatedPlatformSetsPlatform() {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setData(Map.of("jobId", "7"));
        persistentAuditEvent.setEventDate(LocalDateTime.now());
        persistentAuditEvent.setEventType("JOB_CREATED");
        persistentAuditEvent.setPrincipal("system");

        persistenceAuditEventRepository.save(persistentAuditEvent);

        remoteAuditEventServiceClient.updateJobCreatedPlatform(
            7L, PlatformType.EMBEDDED, LocalDateTime.now()
                .minusHours(1));

        assertThat(findAll().getFirst()
            .getPlatform()).isEqualTo(PlatformType.EMBEDDED);
    }

    private List<PersistentAuditEvent> findAll() {
        List<PersistentAuditEvent> persistentAuditEvents = new ArrayList<>();

        persistenceAuditEventRepository.findAll()
            .forEach(persistentAuditEvents::add);

        return persistentAuditEvents;
    }

    static class ProjectAuditMapper implements AuditMapper {

        @Override
        public Map<String, String> map(AuditInvocation auditInvocation) {
            return Map.of("projectId", "1050");
        }
    }

    static class ProjectOperations {

        @Audited(event = "PROJECT_DELETED", mapper = ProjectAuditMapper.class)
        public void deleteProject() {
        }
    }

    private static PersistentAuditEvent createPersistentAuditEvent(String eventType) {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setData(Map.of("projectId", "1050"));
        persistentAuditEvent.setEventDate(LocalDateTime.now());
        persistentAuditEvent.setEventType(eventType);
        persistentAuditEvent.setPlatform(PlatformType.AUTOMATION);
        persistentAuditEvent.setPrincipal("admin@localhost.com");

        return persistentAuditEvent;
    }
}
