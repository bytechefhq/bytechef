/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.remote.grpc;

import com.bytechef.ee.platform.audit.facade.AuditEventFacade;
import com.bytechef.ee.platform.audit.remote.grpc.proto.AuditEventRemoteServiceGrpc;
import com.bytechef.ee.platform.audit.remote.grpc.proto.EventTypes;
import com.bytechef.ee.platform.audit.remote.grpc.proto.FetchAuditEventsRequest;
import com.bytechef.ee.platform.audit.remote.grpc.proto.RemoteAuditEvent;
import com.bytechef.ee.platform.audit.remote.grpc.proto.RemoteAuditEventPage;
import com.bytechef.ee.platform.audit.remote.grpc.proto.RemotePersistentAuditEvents;
import com.bytechef.ee.platform.audit.remote.grpc.proto.UpdateJobCreatedPlatformRequest;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.constant.PlatformType;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.audit.AuditEventRepository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AuditEventRemoteServiceGrpcServer extends AuditEventRemoteServiceGrpc.AuditEventRemoteServiceImplBase {

    private final AuditEventFacade auditEventFacade;
    private final AuditEventProtoMapper auditEventProtoMapper;
    private final @Nullable AuditEventRepository auditEventRepository;
    private final AuditEventService auditEventService;

    @SuppressFBWarnings("EI")
    public AuditEventRemoteServiceGrpcServer(
        AuditEventFacade auditEventFacade, AuditEventService auditEventService,
        @Nullable AuditEventRepository auditEventRepository, AuditEventProtoMapper auditEventProtoMapper) {

        this.auditEventFacade = auditEventFacade;
        this.auditEventProtoMapper = auditEventProtoMapper;
        this.auditEventRepository = auditEventRepository;
        this.auditEventService = auditEventService;
    }

    @Override
    public void addAuditEvent(RemoteAuditEvent request, StreamObserver<Empty> responseObserver) {
        if (auditEventRepository != null) {
            auditEventRepository.add(auditEventProtoMapper.toAuditEvent(request));
        }

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchAuditEvents(
        FetchAuditEventsRequest request, StreamObserver<RemoteAuditEventPage> responseObserver) {

        responseObserver.onNext(
            auditEventProtoMapper.toProto(
                auditEventFacade.fetchAuditEvents(
                    auditEventProtoMapper.toAuditEventFilter(request), auditEventProtoMapper.toPageable(request))));
        responseObserver.onCompleted();
    }

    @Override
    public void fetchEventTypes(Empty request, StreamObserver<EventTypes> responseObserver) {
        responseObserver.onNext(
            EventTypes.newBuilder()
                .addAllEventTypes(auditEventFacade.fetchEventTypes())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void saveAuditEvents(RemotePersistentAuditEvents request, StreamObserver<Empty> responseObserver) {
        auditEventService.saveAll(auditEventProtoMapper.toPersistentAuditEvents(request));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void updateJobCreatedPlatform(
        UpdateJobCreatedPlatformRequest request, StreamObserver<Empty> responseObserver) {

        auditEventService.updateJobCreatedPlatform(
            request.getJobId(), PlatformType.valueOf(request.getPlatform()),
            auditEventProtoMapper.toLocalDateTime(request.getSince()));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
