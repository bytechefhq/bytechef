
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

package com.bytechef.commons.rsocket.util;

import com.bytechef.commons.discovery.util.DiscoveryUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketRequester.Builder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ivica Cardic
 */
public class RSocketUtils {

    private static final Map<URI, RSocketRequester> R_SOCKET_REQUESTER_MAP = new ConcurrentHashMap<>();

    public static RSocketRequester getRSocketRequester(
        ServiceInstance serviceInstance, Builder rSocketRequesterBuilder) {

        return R_SOCKET_REQUESTER_MAP.computeIfAbsent(
            DiscoveryUtils.toWebSocketUri(serviceInstance),
            rSocketRequesterBuilder::websocket);
    }

    public static RSocketRequester getRSocketRequester(
        List<ServiceInstance> serviceInstances, String componentName, Builder rSocketRequesterBuilder) {

        return R_SOCKET_REQUESTER_MAP.computeIfAbsent(
            DiscoveryUtils.toWebSocketUri(DiscoveryUtils.filterServiceInstance(serviceInstances, componentName)),
            rSocketRequesterBuilder::websocket);
    }
}
