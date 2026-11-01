/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.remote.client;

import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.facade.AuditEventFacade;
import com.bytechef.ee.platform.audit.remote.grpc.AuditEventProtoMapper;
import com.bytechef.ee.platform.audit.remote.grpc.proto.AuditEventRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.google.protobuf.Empty;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteAuditEventFacadeClient implements AuditEventFacade {

    private final AuditEventProtoMapper auditEventProtoMapper = new AuditEventProtoMapper();
    private final GrpcChannels grpcChannels;

    public RemoteAuditEventFacadeClient(GrpcChannels grpcChannels) {
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

    private AuditEventRemoteServiceGrpc.AuditEventRemoteServiceBlockingStub getStub() {
        return AuditEventRemoteServiceGrpc.newBlockingStub(
            grpcChannels.getChannel(RemoteAuditEventServiceClient.SERVICE_ID));
    }
}
