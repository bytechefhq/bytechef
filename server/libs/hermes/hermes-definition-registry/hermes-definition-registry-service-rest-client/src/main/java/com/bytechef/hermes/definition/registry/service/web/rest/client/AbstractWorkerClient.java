
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.hermes.definition.registry.service.web.rest.client;

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

public abstract class AbstractWorkerClient {

    protected static final String WORKER_SERVICE_APP = "worker-service-app";
    protected static final WebClient WORKER_WEB_CLIENT = WebClient.create();

    protected final DiscoveryClient discoveryClient;

    public AbstractWorkerClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    protected URI toUri(
        UriBuilder uriBuilder, String componentName, String path, Object... uriVariables) {

        return process(uriBuilder, componentName, path)
            .build(uriVariables);
    }

    protected URI toUri(
        UriBuilder uriBuilder, ServiceInstance serviceInstance, String path, Map<String, ?> uriVariables,
        Map<String, List<String>> queryParams) {

        return process(uriBuilder, serviceInstance, path)
            .queryParams(CollectionUtils.toMultiValueMap(queryParams))
            .build(uriVariables);
    }

    protected URI toUri(UriBuilder uriBuilder, ServiceInstance serviceInstance, String path, Object... uriVariables) {
        return process(uriBuilder, serviceInstance, path)
            .build(uriVariables);
    }

    private UriBuilder process(UriBuilder uriBuilder, String componentName, String path) {
        ServiceInstance serviceInstance = WorkerDiscoveryUtils.filterServiceInstance(
            discoveryClient.getInstances(WORKER_SERVICE_APP), componentName);

        return process(uriBuilder, serviceInstance, path);
    }

    private static UriBuilder process(UriBuilder uriBuilder, ServiceInstance serviceInstance, String path) {
        return uriBuilder
            .scheme("http")
            .host(serviceInstance.getHost())
            .port(serviceInstance.getPort())
            .path("/api/internal" + path);
    }
}
