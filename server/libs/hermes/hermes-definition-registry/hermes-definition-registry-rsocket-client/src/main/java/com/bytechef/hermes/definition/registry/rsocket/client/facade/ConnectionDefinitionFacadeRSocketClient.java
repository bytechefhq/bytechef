
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

package com.bytechef.hermes.definition.registry.rsocket.client.facade;

import com.bytechef.commons.util.MonoUtils;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.facade.ConnectionDefinitionFacade;
import com.bytechef.hermes.definition.registry.rsocket.client.util.ServiceInstanceUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class ConnectionDefinitionFacadeRSocketClient implements ConnectionDefinitionFacade {

    private final ConnectionService connectionService;
    private final DiscoveryClient discoveryClient;
    private final RSocketRequester.Builder rSocketRequesterBuilder;

    @SuppressFBWarnings("EI")
    public ConnectionDefinitionFacadeRSocketClient(
        ConnectionService connectionService, DiscoveryClient discoveryClient,
        RSocketRequester.Builder rSocketRequesterBuilder) {

        this.connectionService = connectionService;
        this.discoveryClient = discoveryClient;
        this.rSocketRequesterBuilder = rSocketRequesterBuilder;
    }

    @Override
    public void applyAuthorization(long connectionId, Authorization.AuthorizationContext authorizationContext) {
        Connection connection = connectionService.getConnection(connectionId);

        Map<String, Map<String, List<String>>> authorizationContextMap = MonoUtils.get(
            rSocketRequesterBuilder
                .websocket(ServiceInstanceUtils.toWebSocketUri(
                    ServiceInstanceUtils.filterServiceInstance(
                        discoveryClient.getInstances("worker-service-app"), connection.getComponentName())))
                .route("ConnectionDefinitionFacade.getAuthorizationContext")
                .data(connectionId)
                .retrieveMono(new ParameterizedTypeReference<>() {}));

        authorizationContext.setHeaders(authorizationContextMap.get("header"));
        authorizationContext.setQueryParameters(authorizationContextMap.get("queryParameters"));
    }

    @Override
    public Optional<String> fetchBaseUri(long connectionId) {
        Connection connection = connectionService.getConnection(connectionId);

        return Optional.ofNullable(
            MonoUtils.get(
                rSocketRequesterBuilder
                    .websocket(ServiceInstanceUtils.toWebSocketUri(
                        ServiceInstanceUtils.filterServiceInstance(
                            discoveryClient.getInstances("worker-service-app"), connection.getComponentName())))
                    .route("ConnectionDefinitionFacade.fetchBaseUri")
                    .data(connectionId)
                    .retrieveMono(String.class)));
    }
}
