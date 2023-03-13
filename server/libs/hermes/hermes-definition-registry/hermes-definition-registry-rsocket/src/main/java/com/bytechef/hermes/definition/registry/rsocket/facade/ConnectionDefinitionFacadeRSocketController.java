
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

package com.bytechef.hermes.definition.registry.rsocket.facade;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.definition.registry.facade.ConnectionDefinitionFacade;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class ConnectionDefinitionFacadeRSocketController {

    private final ConnectionDefinitionFacade connectionDefinitionFacade;

    public ConnectionDefinitionFacadeRSocketController(ConnectionDefinitionFacade connectionDefinitionFacade) {
        this.connectionDefinitionFacade = connectionDefinitionFacade;
    }

    @MessageMapping("ConnectionDefinitionFacade.fetchBaseUri")
    public Mono<String> fetchBaseUri(long connectionId) {
        return connectionDefinitionFacade.fetchBaseUri(connectionId)
            .map(Mono::just)
            .orElse(Mono.empty());
    }

    @MessageMapping("ConnectionDefinitionFacade.getAuthorizationContext")
    public Mono<Map<String, Map<String, List<String>>>> getAuthorizationContext(long connectionId) {
        Map<String, List<String>> returnHeaders = new HashMap<>();
        Map<String, List<String>> returnQueryParameters = new HashMap<>();

        connectionDefinitionFacade.applyAuthorization(connectionId, new Authorization.AuthorizationContext() {

            @Override
            public void setHeaders(Map<String, List<String>> headers) {
                returnHeaders.putAll(headers);
            }

            @Override
            public void setQueryParameters(Map<String, List<String>> queryParameters) {
                returnQueryParameters.putAll(queryParameters);
            }
        });

        return Mono.just(Map.of("headers", returnHeaders, "queryParameters", returnQueryParameters));
    }
}
