
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

package com.bytechef.hermes.workflow.remote.client.service;

import com.bytechef.atlas.domain.Context.Classname;
import com.bytechef.atlas.service.ContextService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class ContextServiceClient implements ContextService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    @SuppressFBWarnings("EI")
    public ContextServiceClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    public Map<String, Object> peek(long stackId, Classname classname) {
        return loadBalancedWebClientBuilder
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/context-service/peek/{stackId}/{classname}")
                .build(stackId, classname))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block();
    }

    @Override
    public Map<String, Object> peek(long stackId, int subStackId, Classname classname) {
        return loadBalancedWebClientBuilder
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/context-service/peek/{stackId}/{subStackId}/{classname}")
                .build(stackId, subStackId, classname))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block();
    }

    @Override
    public void push(long stackId, Classname classname, Map<String, Object> context) {
        loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/context-service/push/{stackId}/{classname}")
                .build(stackId, classname))
            .bodyValue(context)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block();
    }

    @Override
    public void push(long stackId, int subStackId, Classname classname, Map<String, Object> context) {
        loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/context-service/push/{stackId}/{subStackId}/{classname}")
                .build(stackId, classname))
            .bodyValue(context)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block();
    }
}
