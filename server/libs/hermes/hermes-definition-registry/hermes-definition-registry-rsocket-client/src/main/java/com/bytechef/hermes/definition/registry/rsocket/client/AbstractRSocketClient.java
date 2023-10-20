
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

package com.bytechef.hermes.definition.registry.rsocket.client;

import com.bytechef.commons.rsocket.util.RSocketUtils;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.messaging.rsocket.RSocketRequester;

public abstract class AbstractRSocketClient {

    protected static final String WORKER_SERVICE_APP = "worker-service-app";

    protected final DiscoveryClient discoveryClient;
    protected final RSocketRequester.Builder rSocketRequesterBuilder;

    public AbstractRSocketClient(DiscoveryClient discoveryClient, RSocketRequester.Builder rSocketRequesterBuilder) {
        this.discoveryClient = discoveryClient;
        this.rSocketRequesterBuilder = rSocketRequesterBuilder;
    }

    protected RSocketRequester getRSocketRequester(String componentName) {
        return RSocketUtils.getRSocketRequester(
            discoveryClient.getInstances(WORKER_SERVICE_APP), componentName, rSocketRequesterBuilder);
    }
}
