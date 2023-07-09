
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

package com.bytechef.hermes.execution.remote.client.service;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerExecutionServiceClient implements TriggerExecutionService {

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public TriggerExecutionServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public TriggerExecution create(TriggerExecution triggerExecution) {
        return loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/api/internal/trigger-execution-service/create")
                .build(),
            triggerExecution, TriggerExecution.class);
    }

    @Override
    public TriggerExecution getTriggerExecution(long id) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/api/internal/trigger-execution-service/get-trigger-execution/{id}")
                .build(id),
            TriggerExecution.class);
    }

    @Override
    public TriggerExecution update(TriggerExecution triggerExecution) {
        return loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/api/internal/trigger-execution-service/update")
                .build(),
            triggerExecution, TriggerExecution.class);
    }
}
