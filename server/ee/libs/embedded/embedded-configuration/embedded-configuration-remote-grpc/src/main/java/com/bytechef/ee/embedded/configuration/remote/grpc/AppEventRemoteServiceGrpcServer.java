/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.ee.embedded.configuration.remote.grpc.proto.AppEventRemoteServiceGrpc;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.IdRequest;
import com.bytechef.ee.embedded.configuration.remote.grpc.proto.RemoteAppEvent;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppEventRemoteServiceGrpcServer extends AppEventRemoteServiceGrpc.AppEventRemoteServiceImplBase {

    private final EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper;
    private final AppEventService appEventService;

    @SuppressFBWarnings("EI")
    public AppEventRemoteServiceGrpcServer(
        AppEventService appEventService, EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        this.embeddedConfigurationProtoMapper = embeddedConfigurationProtoMapper;
        this.appEventService = appEventService;
    }

    @Override
    public void getAppEvent(IdRequest request, StreamObserver<RemoteAppEvent> responseObserver) {
        responseObserver.onNext(embeddedConfigurationProtoMapper.toProto(appEventService.getAppEvent(request.getId())));
        responseObserver.onCompleted();
    }
}
