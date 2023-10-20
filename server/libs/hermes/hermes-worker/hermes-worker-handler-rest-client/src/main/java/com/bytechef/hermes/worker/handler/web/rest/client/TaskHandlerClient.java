
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

package com.bytechef.hermes.worker.handler.web.rest.client;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.commons.reactor.util.MonoUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class TaskHandlerClient {

    private final DiscoveryClient discoveryClient;

    public TaskHandlerClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @SuppressFBWarnings("NP")
    public Object handle(String type, TaskExecution taskExecution) {
        ServiceInstance serviceInstance = WorkerDiscoveryUtils.filterServiceInstance(
            discoveryClient.getInstances("worker-service-app"), StringUtils.split(type, "/")[0]);

        return MonoUtils.get(
            WebClient.create()
                .post()
                .uri(uriBuilder -> uriBuilder
                    .scheme("http")
                    .host(serviceInstance.getHost())
                    .port(serviceInstance.getPort())
                    .path("/api/internal/task-handler")
                    .build())
                .bodyValue(Map.of("type", type, "taskExecution", taskExecution))
                .retrieve()
                .bodyToMono(Object.class));
    }
}
