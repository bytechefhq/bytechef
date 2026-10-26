/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client;

import com.bytechef.ee.discovery.util.WorkerDiscoveryUtils;
import java.util.Optional;
import java.util.Set;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Picks the worker instances that advertise components in their discovery metadata.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkerInstances {

    private static final String WORKER_APP = "worker-app";

    private final DiscoveryClient discoveryClient;

    public WorkerInstances(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public Optional<ServiceInstance> fetchInstance(String componentName) {
        return WorkerDiscoveryUtils.fetchServiceInstance(discoveryClient.getInstances(WORKER_APP), componentName);
    }

    public ServiceInstance getInstance(String componentName) {
        return WorkerDiscoveryUtils.filterServiceInstance(discoveryClient.getInstances(WORKER_APP), componentName);
    }

    public Set<ServiceInstance> getInstances() {
        return WorkerDiscoveryUtils.filterServiceInstances(discoveryClient.getInstances(WORKER_APP));
    }
}
