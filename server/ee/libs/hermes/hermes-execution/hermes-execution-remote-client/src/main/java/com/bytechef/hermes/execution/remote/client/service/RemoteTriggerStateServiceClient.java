
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
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component("triggerStorageService")
public class RemoteTriggerStateServiceClient implements TriggerStateService {

    private static final String EXECUTION_APP = "execution-app";
    private static final String TRIGGER_STORAGE_SERVICE = "/remote/trigger-state-service";

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteTriggerStateServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public <T> Optional<T> fetchValue(WorkflowExecutionId workflowExecutionId) {
        return Optional.ofNullable(
            loadBalancedWebClient.get(
                uriBuilder -> uriBuilder
                    .host(EXECUTION_APP)
                    .path(TRIGGER_STORAGE_SERVICE + "/fetch-value/{workflowExecutionId}")
                    .build(workflowExecutionId),
                new ParameterizedTypeReference<T>() {}));
    }

    @Override
    public void save(WorkflowExecutionId workflowExecutionId, Object value) {
        loadBalancedWebClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TRIGGER_STORAGE_SERVICE + "/save/{workflowExecutionId}")
                .build(workflowExecutionId),
            value);
    }
}
