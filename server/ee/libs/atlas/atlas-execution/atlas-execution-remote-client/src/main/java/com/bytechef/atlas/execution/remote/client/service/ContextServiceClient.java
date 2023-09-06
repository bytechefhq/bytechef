
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

import com.bytechef.atlas.execution.domain.Context.Classname;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class ContextServiceClient implements ContextService {

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public ContextServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public FileEntry peek(long stackId, Classname classname) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/internal/context-service/peek/{stackId}/{classname}")
                .build(stackId, classname),
            FileEntry.class);
    }

    @Override
    public FileEntry peek(long stackId, int subStackId, Classname classname) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/internal/context-service/peek/{stackId}/{subStackId}/{classname}")
                .build(stackId, subStackId, classname),
            FileEntry.class);
    }

    @Override
    public void push(long stackId, Classname classname, FileEntry value) {
        loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/internal/context-service/push/{stackId}/{classname}")
                .build(stackId, classname),
            value, FileEntry.class);
    }

    @Override
    public void push(long stackId, int subStackId, Classname classname, FileEntry value) {
        loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/internal/context-service/push/{stackId}/{subStackId}/{classname}")
                .build(stackId, classname),
            value, new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
