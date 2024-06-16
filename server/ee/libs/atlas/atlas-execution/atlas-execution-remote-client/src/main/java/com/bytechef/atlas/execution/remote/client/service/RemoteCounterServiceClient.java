/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atlas.execution.remote.client.service;

import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.remote.client.LoadBalancedRestClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteCounterServiceClient implements CounterService {

    private static final String COUNTER_SERVICE = "/remote/counter-service";
    private static final String EXECUTION_APP = "execution-app";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteCounterServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public void delete(long id) {
        loadBalancedRestClient.delete(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(COUNTER_SERVICE + "/delete/{id}")
                .build(id));
    }

    @Override
    public long decrement(long id) {
        return loadBalancedRestClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(COUNTER_SERVICE + "/decrement/{id}")
                .build(id),
            null, Long.class);
    }

    @Override
    public void set(long id, long value) {
        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(COUNTER_SERVICE + "/set/{id}/{value}")
                .build(id, value),
            null);
    }
}
