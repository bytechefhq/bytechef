/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client;

import com.bytechef.ee.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.ee.remote.client.DefaultRestClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public abstract class AbstractWorkerClient {

    protected static final String WORKER_APP = "worker-app";
    protected final DefaultRestClient defaultRestClient;

    protected final DiscoveryClient discoveryClient;
    protected final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public AbstractWorkerClient(
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        this.defaultRestClient = defaultRestClient;
        this.discoveryClient = discoveryClient;
        this.objectMapper = objectMapper;
    }

    protected URI toUri(
        UriBuilder uriBuilder, String componentName, String path, Object... uriVariables) {

        return build(uriBuilder, componentName, path)
            .build(uriVariables);
    }

    protected URI toUri(
        UriBuilder uriBuilder, ServiceInstance serviceInstance, String path, Map<String, ?> uriVariables,
        Map<String, List<String>> queryParams) {

        return build(uriBuilder, serviceInstance, path)
            .queryParams(org.springframework.util.CollectionUtils.toMultiValueMap(queryParams))
            .build(uriVariables);
    }

    protected URI toUri(UriBuilder uriBuilder, ServiceInstance serviceInstance, String path, Object... uriVariables) {
        return build(uriBuilder, serviceInstance, path)
            .build(uriVariables);
    }

    private UriBuilder build(UriBuilder uriBuilder, String componentName, String path) {
        ServiceInstance serviceInstance = WorkerDiscoveryUtils.filterServiceInstance(
            discoveryClient.getInstances(WORKER_APP), componentName);

        return build(uriBuilder, serviceInstance, path);
    }

    private static UriBuilder build(UriBuilder uriBuilder, ServiceInstance serviceInstance, String path) {
        return uriBuilder
            .scheme("http")
            .host(serviceInstance.getHost())
            .port(serviceInstance.getPort())
            .path("/remote" + path);
    }
}
