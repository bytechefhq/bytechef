
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

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
public class WorkflowServiceClient implements WorkflowService {

    @Override
    public Workflow create(String definition, Workflow.Format format, Workflow.SourceType sourceType) {
        return null;
    }

    @Override
    public void delete(String id) {

    }

    @Override
    public Workflow getWorkflow(String id) {
        return null;
    }

    @Override
    public List<Workflow> getWorkflows() {
        return null;
    }

    @Override
    public Workflow update(String id, String definition) {
        return null;
    }

    @Override
    public List<Workflow> getWorkflows(List<String> workflowIds) {
        return null;
    }
}
