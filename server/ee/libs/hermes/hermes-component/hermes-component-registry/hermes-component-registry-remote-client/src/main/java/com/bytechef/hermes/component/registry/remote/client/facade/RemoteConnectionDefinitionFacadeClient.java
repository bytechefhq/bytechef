
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

package com.bytechef.hermes.component.registry.remote.client.facade;

import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.registry.facade.RemoteConnectionDefinitionFacade;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnMissingClass(value = "com.bytechef.worker.WorkerApplication")
public class RemoteConnectionDefinitionFacadeClient extends AbstractWorkerClient
    implements RemoteConnectionDefinitionFacade {

    private static final String CONNECTION_DEFINITION_FACADE = "/connection-definition-facade";

    public RemoteConnectionDefinitionFacadeClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public Authorization.ApplyResponse executeAuthorizationApply(
        @NonNull String componentName,
        @NonNull ComponentConnection connection) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/execute-authorization-apply"),
            new ConnectionRequest(componentName, connection), Authorization.ApplyResponse.class);
    }

    @Override
    public Authorization.AuthorizationCallbackResponse executeAuthorizationCallback(
        @NonNull String componentName,
        @NonNull ComponentConnection connection, @NonNull String redirectUri) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/execute-authorization-callback"),
            new AuthorizationCallbackRequest(componentName, connection, redirectUri),
            Authorization.AuthorizationCallbackResponse.class);
    }

    @Override
    public Optional<String> executeBaseUri(
        @NonNull String componentName, ComponentConnection connection) {
        return Optional.ofNullable(
            defaultWebClient.post(
                uriBuilder -> toUri(uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/execute-base-uri"),
                new ConnectionRequest(componentName, connection), String.class));
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        @NonNull String componentName, ComponentConnection connection) {

        return defaultWebClient.post(
            uriBuilder -> toUri(
                uriBuilder, componentName, CONNECTION_DEFINITION_FACADE + "/get-oauth2-authorization-parameters"),
            new ConnectionRequest(componentName, connection),
            OAuth2AuthorizationParameters.class);
    }

    private record AuthorizationCallbackRequest(
        String componentName, ComponentConnection connection, String redirectUri) {
    }

    private record ConnectionRequest(String componentName, ComponentConnection connection) {
    }
}
