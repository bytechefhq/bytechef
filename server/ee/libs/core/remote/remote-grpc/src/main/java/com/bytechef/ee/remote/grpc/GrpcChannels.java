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
 * Gives remote clients a channel to the microservice that serves them.
 *
 * @version ee
 */
public interface GrpcChannels {

    Channel getChannel(String serviceId);

    Channel getChannel(ServiceInstance serviceInstance);
}
