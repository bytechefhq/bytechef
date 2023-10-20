
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

package com.bytechef.atlas.execution.remote.client.service;

import com.bytechef.atlas.execution.service.CounterService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Ivica Cardic
 */
@Component
public class CounterServiceClient implements CounterService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    public CounterServiceClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    public void delete(long id) {
        loadBalancedWebClientBuilder
            .build()
            .delete()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/counter-service/delete/{id}")
                .build(id))
            .retrieve()
            .toBodilessEntity()
            .block();
    }

    @Override
    @SuppressFBWarnings("NP")
    public long decrement(long id) {
        return loadBalancedWebClientBuilder
            .build()
            .put()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/counter-service/decrement/{id}")
                .build(id))
            .retrieve()
            .bodyToMono(Long.class)
            .block();
    }

    @Override
    public void set(long id, long value) {
        loadBalancedWebClientBuilder
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .host("platform-service-app")
                .path("/api/internal/counter-service/set/{id}/{value}")
                .build(id, value))
            .retrieve()
            .toBodilessEntity()
            .block();
    }
}
