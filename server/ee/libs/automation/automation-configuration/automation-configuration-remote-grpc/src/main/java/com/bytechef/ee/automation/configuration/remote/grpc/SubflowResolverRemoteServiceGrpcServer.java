/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.ee.automation.configuration.remote.grpc.proto.RemoteSubflow;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.ResolveSubflowRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.SubflowResolverRemoteServiceGrpc;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SubflowResolverRemoteServiceGrpcServer
    extends SubflowResolverRemoteServiceGrpc.SubflowResolverRemoteServiceImplBase {

    private final SubflowResolver subflowResolver;

    @SuppressFBWarnings("EI")
    public SubflowResolverRemoteServiceGrpcServer(SubflowResolver subflowResolver) {
        this.subflowResolver = subflowResolver;
    }

    @Override
    public void resolveSubflow(ResolveSubflowRequest request, StreamObserver<RemoteSubflow> responseObserver) {
        Subflow subflow = subflowResolver.resolveSubflow(
            request.getWorkflowUuid(), request.getTriggerName(), request.getEditorEnvironment());

        responseObserver.onNext(
            RemoteSubflow.newBuilder()
                .setInputsName(subflow.inputsName())
                .setWorkflowId(subflow.workflowId())
                .build());
        responseObserver.onCompleted();
    }
}
