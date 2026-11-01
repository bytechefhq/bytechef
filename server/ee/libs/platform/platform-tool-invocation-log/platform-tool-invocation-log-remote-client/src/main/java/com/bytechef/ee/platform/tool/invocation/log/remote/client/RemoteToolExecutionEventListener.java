/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.remote.client;

import com.bytechef.ee.platform.tool.invocation.log.remote.grpc.ToolExecutionEventProtoMapper;
import com.bytechef.ee.platform.tool.invocation.log.remote.grpc.ToolInvocationLogGrpcServerConfiguration;
import com.bytechef.ee.platform.tool.invocation.log.remote.grpc.proto.ToolInvocationLogRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
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
@ConditionalOnMissingClass(ToolInvocationLogGrpcServerConfiguration.TOOL_INVOCATION_LOG_EVENT_LISTENER)
public class RemoteToolExecutionEventListener {

    public static final String SERVICE_ID = "execution-app";

    private static final Logger log = LoggerFactory.getLogger(RemoteToolExecutionEventListener.class);

    private final GrpcChannels grpcChannels;
    private final ToolExecutionEventProtoMapper toolExecutionEventProtoMapper;

    public RemoteToolExecutionEventListener(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.toolExecutionEventProtoMapper = new ToolExecutionEventProtoMapper(objectMapper);
    }

    @EventListener
    @Async("workerExecutor")
    public void onToolExecutionEvent(ToolExecutionEvent toolExecutionEvent) {
        try {
            ToolInvocationLogRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID))
                .recordToolExecution(toolExecutionEventProtoMapper.toProto(toolExecutionEvent));
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to send tool invocation log for surface={} tool={} to {}: {}", toolExecutionEvent.surface(),
                toolExecutionEvent.toolName(), SERVICE_ID, exception.getMessage());
        }
    }
}
