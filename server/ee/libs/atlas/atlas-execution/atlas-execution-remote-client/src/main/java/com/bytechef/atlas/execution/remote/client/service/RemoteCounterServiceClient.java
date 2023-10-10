
/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.commons.webclient.LoadBalancedWebClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class RemoteCounterServiceClient implements CounterService {

    private static final String COUNTER_SERVICE = "/remote/counter-service";
    private static final String EXECUTION_APP = "execution-app";

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteCounterServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public void delete(long id) {
        loadBalancedWebClient.delete(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(COUNTER_SERVICE + "/delete/{id}")
                .build(id));
    }

    @Override
    public long decrement(long id) {
        return loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(COUNTER_SERVICE + "/decrement/{id}")
                .build(id),
            null, Long.class);
    }

    @Override
    public void set(long id, long value) {
        loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(COUNTER_SERVICE + "/set/{id}/{value}")
                .build(id, value),
            null);
    }
}
