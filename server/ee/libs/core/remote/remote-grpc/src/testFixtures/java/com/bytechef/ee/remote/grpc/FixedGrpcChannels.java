/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import io.grpc.Channel;
import org.springframework.cloud.client.ServiceInstance;

/**
 * Hands out one channel for every service and instance, for wire tests against an in-process server.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class FixedGrpcChannels implements GrpcChannels {

    private final Channel channel;

    public FixedGrpcChannels(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Channel getChannel(String serviceId) {
        return channel;
    }

    @Override
    public Channel getChannel(ServiceInstance serviceInstance) {
        return channel;
    }
}
