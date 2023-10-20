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

package com.integri.atlas.engine.workflow.repository.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.workflow.Workflow;
import com.integri.atlas.engine.workflow.WorkflowFormat;
import com.integri.atlas.engine.workflow.WorkflowResource;

/**
 * @author Ivica Cardic
 */
public class JSONWorkflowMapper extends BaseWorkflowMapper implements WorkflowMapperResolver {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Workflow readValue(WorkflowResource aWorkflowResource) {
        return readValue(aWorkflowResource, objectMapper);
    }

    @Override
    public WorkflowMapper resolve(WorkflowResource workflowResource) {
        return workflowResource.getWorkflowFormat() == WorkflowFormat.JSON ? this : null;
    }
}
