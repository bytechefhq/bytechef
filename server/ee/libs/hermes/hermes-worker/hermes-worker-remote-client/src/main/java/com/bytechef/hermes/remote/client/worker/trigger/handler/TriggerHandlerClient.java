
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

package com.bytechef.hermes.remote.client.worker.trigger.handler;

import com.bytechef.commons.discovery.util.WorkerDiscoveryUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandler.TriggerOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerHandlerClient {

    private final DefaultWebClient defaultWebClient;
    private final DiscoveryClient discoveryClient;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public TriggerHandlerClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        this.defaultWebClient = defaultWebClient;
        this.discoveryClient = discoveryClient;
        this.objectMapper = objectMapper;
    }

    @SuppressFBWarnings("NP")
    public TriggerOutput handle(String type, TriggerExecution triggerExecution) {
        ServiceInstance serviceInstance = WorkerDiscoveryUtils.filterServiceInstance(
            discoveryClient.getInstances("worker-service-app"), StringUtils.split(type, "/")[0],
            objectMapper);

        return defaultWebClient.post(
            uriBuilder -> uriBuilder
                .scheme("http")
                .host(serviceInstance.getHost())
                .port(serviceInstance.getPort())
                .path("/api/internal/trigger-handler/handle")
                .build(),
            Map.of("type", type, "triggerExecution", triggerExecution), TriggerOutput.class);
    }
}
