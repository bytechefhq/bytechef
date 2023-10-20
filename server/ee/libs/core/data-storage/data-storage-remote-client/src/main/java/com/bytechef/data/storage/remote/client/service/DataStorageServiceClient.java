
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

package com.bytechef.data.storage.remote.client.service;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.data.storage.service.DataStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class DataStorageServiceClient implements DataStorageService {

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public DataStorageServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public <T> Optional<T> fetch(String context, int scope, long scopeId, String key) {
        return Optional.ofNullable(get(scope, scopeId, key, new ParameterizedTypeReference<T>() {}));
    }

    @Override
    public <T> T get(String context, int scope, long scopeId, String key) {
        return Optional.ofNullable(get(scope, scopeId, key, new ParameterizedTypeReference<T>() {}))
            .orElseThrow();
    }

    @Override
    public void put(String context, int scope, long scopeId, String key, Object value) {
        loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/internal/data-storage-service/save/{context}/{scope}/{scopeId}/{key}")
                .build(context, scope, scope, key),
            value);
    }

    private <T> T get(int scope, long scopeId, String key, ParameterizedTypeReference<T> responseTypeRef) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/internal/data-storage-service/fetch-value/{scope}/{scopeId}/{key}")
                .build(scope, scopeId, key),
            responseTypeRef);
    }
}
