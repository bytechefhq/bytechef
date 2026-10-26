/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.grpc;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.ContextKeyRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.ContextRemoteServiceGrpc;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.PushContextRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteFileEntry;
import com.bytechef.file.storage.domain.FileEntry;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 */
public class ContextRemoteServiceGrpcServer extends ContextRemoteServiceGrpc.ContextRemoteServiceImplBase {

    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper;
    private final ContextService contextService;

    @SuppressFBWarnings("EI")
    public ContextRemoteServiceGrpcServer(
        ContextService contextService, AtlasExecutionProtoMapper atlasExecutionProtoMapper) {

        this.atlasExecutionProtoMapper = atlasExecutionProtoMapper;
        this.contextService = contextService;
    }

    @Override
    public void peek(ContextKeyRequest request, StreamObserver<RemoteFileEntry> responseObserver) {
        Context.Classname classname = Context.Classname.valueOf(request.getClassname());

        FileEntry fileEntry = request.hasSubStackId()
            ? contextService.peek(request.getStackId(), request.getSubStackId(), classname)
            : contextService.peek(request.getStackId(), classname);

        responseObserver.onNext(atlasExecutionProtoMapper.toProto(fileEntry));
        responseObserver.onCompleted();
    }

    @Override
    public void push(PushContextRequest request, StreamObserver<Empty> responseObserver) {
        ContextKeyRequest key = request.getKey();

        Context.Classname classname = Context.Classname.valueOf(key.getClassname());
        FileEntry fileEntry = atlasExecutionProtoMapper.toFileEntry(request.getValue());

        if (key.hasSubStackId()) {
            contextService.push(key.getStackId(), key.getSubStackId(), classname, fileEntry);
        } else {
            contextService.push(key.getStackId(), classname, fileEntry);
        }

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
