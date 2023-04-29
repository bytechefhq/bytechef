
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

package com.bytechef.hermes.worker.rsocket.client.task.handler;

import com.bytechef.commons.rsocket.util.RSocketUtils;
import com.bytechef.hermes.domain.TriggerExecution;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerHandlerRSocketClient {

    private static final String WORKER_SERVICE_APP = "worker-service-app";

    private final DiscoveryClient discoveryClient;
    private final RSocketRequester.Builder rSocketRequesterBuilder;

    public TriggerHandlerRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {

        this.discoveryClient = discoveryClient;
        this.rSocketRequesterBuilder = rSocketRequesterBuilder;
    }

    @SuppressFBWarnings("NP")
    public Object handle(String type, TriggerExecution triggerExecution) {
        try {
            return getRSocketRequester(StringUtils.split(type, "/")[0])
                .route("TriggerHandler.handle")
                .data(Map.of("type", type, "triggerExecution", triggerExecution))
                .retrieveMono(Object.class)
                .toFuture()
                .get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private RSocketRequester getRSocketRequester(String componentName) {
        return RSocketUtils.getRSocketRequester(
            discoveryClient.getInstances(WORKER_SERVICE_APP), componentName, rSocketRequesterBuilder);
    }
}
