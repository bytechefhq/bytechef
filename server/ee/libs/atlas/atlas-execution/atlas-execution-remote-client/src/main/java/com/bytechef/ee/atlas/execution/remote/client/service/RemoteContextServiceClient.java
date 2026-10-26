/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.service;

import com.bytechef.atlas.execution.domain.Context.Classname;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.ContextKeyRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.ContextRemoteServiceGrpc;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.PushContextRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteContextServiceClient implements ContextService {

    private static final String EXECUTION_APP = "execution-app";

    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteContextServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.atlasExecutionProtoMapper = new AtlasExecutionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public void deleteStackContexts(long stackId, Classname classname) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FileEntry> getStackFileEntries(long stackId, Classname classname) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileEntry peek(long stackId, Classname classname) {
        return atlasExecutionProtoMapper.toFileEntry(getStub().peek(toContextKeyRequest(stackId, null, classname)));
    }

    @Override
    public FileEntry peek(long stackId, int subStackId, Classname classname) {
        return atlasExecutionProtoMapper
            .toFileEntry(getStub().peek(toContextKeyRequest(stackId, subStackId, classname)));
    }

    @Override
    public void push(long stackId, Classname classname, FileEntry value) {
        push(toContextKeyRequest(stackId, null, classname), value);
    }

    @Override
    public void push(long stackId, int subStackId, Classname classname, FileEntry value) {
        push(toContextKeyRequest(stackId, subStackId, classname), value);
    }

    private ContextRemoteServiceGrpc.ContextRemoteServiceBlockingStub getStub() {
        return ContextRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }

    private void push(ContextKeyRequest contextKeyRequest, FileEntry value) {
        getStub().push(
            PushContextRequest.newBuilder()
                .setKey(contextKeyRequest)
                .setValue(atlasExecutionProtoMapper.toProto(value))
                .build());
    }

    private static ContextKeyRequest toContextKeyRequest(
        long stackId, @Nullable Integer subStackId, Classname classname) {

        ContextKeyRequest.Builder builder = ContextKeyRequest.newBuilder()
            .setClassname(classname.name())
            .setStackId(stackId);

        if (subStackId != null) {
            builder.setSubStackId(subStackId);
        }

        return builder.build();
    }
}
