/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.remote.client;

import com.bytechef.ee.platform.audit.AuditEventConverter;
import com.bytechef.ee.platform.audit.remote.grpc.AuditEventProtoMapper;
import com.bytechef.ee.platform.audit.remote.grpc.proto.AuditEventRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteAuditEventRepositoryClient implements AuditEventRepository {

    private static final String DETAILS = "details";

    private static final Logger log = LoggerFactory.getLogger(RemoteAuditEventRepositoryClient.class);

    private final AuditEventConverter auditEventConverter;
    private final AuditEventProtoMapper auditEventProtoMapper = new AuditEventProtoMapper();
    private final GrpcChannels grpcChannels;

    public RemoteAuditEventRepositoryClient(GrpcChannels grpcChannels, AuditEventConverter auditEventConverter) {
        this.auditEventConverter = auditEventConverter;
        this.grpcChannels = grpcChannels;
    }

    @Override
    public void add(AuditEvent auditEvent) {
        Map<String, Object> data = new HashMap<>(auditEvent.getData());

        if (data.get(DETAILS) instanceof TenantWebAuthenticationDetails tenantWebAuthenticationDetails &&
            tenantWebAuthenticationDetails.getTenantId() != null) {

            data.putIfAbsent(AuditEventConverter.TENANT_ID_DATA_KEY, tenantWebAuthenticationDetails.getTenantId());
        }

        try {
            AuditEventRemoteServiceGrpc.AuditEventRemoteServiceStub auditEventRemoteServiceStub =
                AuditEventRemoteServiceGrpc.newStub(grpcChannels.getChannel(RemoteAuditEventServiceClient.SERVICE_ID));

            auditEventRemoteServiceStub.addAuditEvent(
                auditEventProtoMapper.toProto(auditEvent, auditEventConverter.convertDataToStrings(data)),
                new AuditEventStreamObserver(auditEvent));
        } catch (RuntimeException exception) {
            logPersistenceFailure(auditEvent, exception);
        }
    }

    @Override
    public List<AuditEvent> find(String principal, Instant after, String type) {
        throw new UnsupportedOperationException();
    }

    private static void logPersistenceFailure(AuditEvent auditEvent, Throwable throwable) {
        log.error(
            "AUDIT PERSISTENCE FAILURE: the {} event of {} was not recorded, audit trail is incomplete",
            auditEvent.getType(), auditEvent.getPrincipal(), throwable);
    }

    private record AuditEventStreamObserver(AuditEvent auditEvent) implements StreamObserver<Empty> {

        @Override
        public void onNext(Empty empty) {
            log.trace("Recorded the {} event of {}", auditEvent.getType(), auditEvent.getPrincipal());
        }

        @Override
        public void onError(Throwable throwable) {
            logPersistenceFailure(auditEvent, throwable);
        }

        @Override
        public void onCompleted() {
            log.trace("Completed recording the {} event of {}", auditEvent.getType(), auditEvent.getPrincipal());
        }
    }
}
