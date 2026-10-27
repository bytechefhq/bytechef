/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.ee.platform.component.remote.grpc.proto.CodeEditorScriptInputFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.component.remote.grpc.proto.GetScriptInputParametersRequest;
import com.bytechef.ee.platform.component.remote.grpc.proto.ValueResponse;
import com.bytechef.platform.component.facade.CodeEditorScriptInputFacade;
import io.grpc.stub.StreamObserver;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeEditorScriptInputFacadeRemoteServiceGrpcServer
    extends CodeEditorScriptInputFacadeRemoteServiceGrpc.CodeEditorScriptInputFacadeRemoteServiceImplBase {

    private final CodeEditorScriptInputFacade codeEditorScriptInputFacade;
    private final ComponentProtoMapper componentProtoMapper;

    public CodeEditorScriptInputFacadeRemoteServiceGrpcServer(
        CodeEditorScriptInputFacade codeEditorScriptInputFacade, ComponentProtoMapper componentProtoMapper) {

        this.codeEditorScriptInputFacade = codeEditorScriptInputFacade;
        this.componentProtoMapper = componentProtoMapper;
    }

    @Override
    public void getScriptInputParameters(
        GetScriptInputParametersRequest request, StreamObserver<ValueResponse> responseObserver) {

        Map<String, ?> sourceInputParameters = componentProtoMapper.toMap(request.getSourceInputParameters());

        Map<String, Object> scriptInputParameters = codeEditorScriptInputFacade.getScriptInputParameters(
            request.getRootComponentName(), request.getRootComponentVersion(), request.getSourceComponentName(),
            request.getSourceComponentVersion(), request.getSourceClusterElementName(),
            sourceInputParameters == null ? Map.of() : sourceInputParameters,
            request.hasSourceConnectionId() ? request.getSourceConnectionId() : null);

        responseObserver.onNext(
            ValueResponse.newBuilder()
                .setValue(componentProtoMapper.toBytes(scriptInputParameters))
                .build());
        responseObserver.onCompleted();
    }
}
