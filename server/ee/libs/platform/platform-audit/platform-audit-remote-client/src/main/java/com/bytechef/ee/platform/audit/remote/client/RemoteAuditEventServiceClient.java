/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.remote.client;

import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.remote.grpc.AuditEventProtoMapper;
import com.bytechef.ee.platform.audit.remote.grpc.proto.AuditEventRemoteServiceGrpc;
import com.bytechef.ee.platform.audit.remote.grpc.proto.UpdateJobCreatedPlatformRequest;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.constant.PlatformType;
import com.google.protobuf.Empty;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteAuditEventServiceClient implements AuditEventService {

    static final String SERVICE_ID = "execution-app";

    private static final Logger log = LoggerFactory.getLogger(RemoteAuditEventServiceClient.class);

    private final AuditEventProtoMapper auditEventProtoMapper = new AuditEventProtoMapper();
    private final GrpcChannels grpcChannels;

    public RemoteAuditEventServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public Page<PersistentAuditEvent> fetchAuditEvents(AuditEventFilter auditEventFilter, Pageable pageable) {
        return auditEventProtoMapper.toPage(
            getStub().fetchAuditEvents(auditEventProtoMapper.toProto(auditEventFilter, pageable)), pageable);
    }

    @Override
    public List<String> fetchEventTypes() {
        return getStub().fetchEventTypes(Empty.getDefaultInstance())
            .getEventTypesList();
    }

    @Override
    public void saveAll(List<PersistentAuditEvent> persistentAuditEvents) {
        List<PersistentAuditEvent> pendingAuditEvents = List.copyOf(persistentAuditEvents);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send(pendingAuditEvents);

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCompletion(int status) {
                send(pendingAuditEvents);
            }
        });
    }

    @Override
    public void updateJobCreatedPlatform(long jobId, PlatformType platform, LocalDateTime since) {
        getStub().updateJobCreatedPlatform(
            UpdateJobCreatedPlatformRequest.newBuilder()
                .setJobId(jobId)
                .setPlatform(platform.name())
                .setSince(auditEventProtoMapper.toTimestamp(since))
                .build());
    }

    private AuditEventRemoteServiceGrpc.AuditEventRemoteServiceBlockingStub getStub() {
        return AuditEventRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));
    }

    private void send(List<PersistentAuditEvent> persistentAuditEvents) {
        try {
            getStub().saveAuditEvents(auditEventProtoMapper.toProto(persistentAuditEvents));
        } catch (RuntimeException exception) {
            log.error(
                "AUDIT PERSISTENCE FAILURE: {} audit event(s) were not recorded, audit trail is incomplete: {}",
                persistentAuditEvents.size(), persistentAuditEvents, exception);
        }
    }
}
