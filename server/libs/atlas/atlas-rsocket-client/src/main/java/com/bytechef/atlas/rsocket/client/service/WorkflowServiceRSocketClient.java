
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

package com.bytechef.atlas.rsocket.client.service;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class WorkflowServiceRSocketClient implements WorkflowService {

    private final RSocketRequester rSocketRequester;

    public WorkflowServiceRSocketClient(RSocketRequester rSocketRequester) {
        this.rSocketRequester = rSocketRequester;
    }

    @Override
    public void clearCache() {
        rSocketRequester
            .route("clearCache")
            .send()
            .block();
    }

    @Override
    public Workflow create(String definition, Workflow.Format format, Workflow.SourceType sourceType) {
        return rSocketRequester
            .route("createWorkflow")
            .data(new Workflow(definition, format, sourceType))
            .retrieveMono(Workflow.class)
            .block();
    }

    @Override
    public void delete(String id) {
        rSocketRequester.route("deleteWorkflow")
            .data(id)
            .send()
            .block();
    }

    @Override
    public Workflow getWorkflow(String id) {
        return rSocketRequester
            .route("getWorkflow")
            .data(id)
            .retrieveMono(Workflow.class)
            .block();
    }

    @Override
    public List<Workflow> getWorkflows() {
        return rSocketRequester
            .route("getWorkflows")
            .retrieveMono(new ParameterizedTypeReference<List<Workflow>>() {})
            .block();
    }

    @Override
    public Workflow update(String id, String definition) {
        return rSocketRequester
            .route("updateWorkflow")
            .data(new Workflow(id, definition))
            .retrieveMono(Workflow.class)
            .block();
    }
}
