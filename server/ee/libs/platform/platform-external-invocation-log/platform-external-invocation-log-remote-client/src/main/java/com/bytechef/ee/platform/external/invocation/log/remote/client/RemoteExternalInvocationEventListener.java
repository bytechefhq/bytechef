/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.remote.client;

import com.bytechef.ee.platform.external.invocation.log.remote.grpc.ExternalInvocationEventProtoMapper;
import com.bytechef.ee.platform.external.invocation.log.remote.grpc.ExternalInvocationLogGrpcServerConfiguration;
import com.bytechef.ee.platform.external.invocation.log.remote.grpc.proto.ExternalInvocationLogRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.external.invocation.ExternalInvocationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnMissingClass(ExternalInvocationLogGrpcServerConfiguration.EXTERNAL_INVOCATION_LOG_EVENT_LISTENER)
public class RemoteExternalInvocationEventListener {

    public static final String SERVICE_ID = "execution-app";

    private static final Logger log = LoggerFactory.getLogger(RemoteExternalInvocationEventListener.class);

    private final GrpcChannels grpcChannels;
    private final ExternalInvocationEventProtoMapper externalInvocationEventProtoMapper;

    public RemoteExternalInvocationEventListener(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.externalInvocationEventProtoMapper = new ExternalInvocationEventProtoMapper(objectMapper);
    }

    @EventListener
    @Async("workerExecutor")
    public void onExternalInvocationEvent(ExternalInvocationEvent externalInvocationEvent) {
        try {
            ExternalInvocationLogRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID))
                .recordExternalInvocation(externalInvocationEventProtoMapper.toProto(externalInvocationEvent));
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to send external invocation log for source={} tool={} to {}: {}", externalInvocationEvent.source(),
                externalInvocationEvent.toolName(), SERVICE_ID, exception.getMessage());
        }
    }
}
