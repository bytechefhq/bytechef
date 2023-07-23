
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

package com.bytechef.hermes.configuration.remote.client.service;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.hermes.configuration.service.OAuth2Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class OAuth2ServiceClient implements OAuth2Service {

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public OAuth2ServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public Map<String, ?> checkPredefinedParameters(
        String componentName, @RequestBody Map<String, ?> connectionParameters) {

        return loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host("configuration-service-app")
                .path("/api/internal/oauth2-service/check-predefined-parameters/{componentName}")
                .build(componentName),
            connectionParameters,
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public String getRedirectUri() {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("configuration-service-app")
                .path("/api/internal/oauth2-service/get-redirect-uri")
                .build(),
            String.class);
    }

    @Override
    public List<String> getPredefinedApps() {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("configuration-service-app")
                .path("/api/internal/oauth2-service/get-predefined-apps")
                .build(),
            new ParameterizedTypeReference<>() {});
    }
}
