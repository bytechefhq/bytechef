
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.workflow.mapper;

import com.bytechef.atlas.domain.Workflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.Map;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
class YamlWorkflowMapper extends AbstractWorkflowMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

    public Workflow readWorkflow(WorkflowResource workflowResource) {
        return readWorkflow(workflowResource, OBJECT_MAPPER);
    }

    @Override
    public Map<String, Object> readWorkflowMap(WorkflowResource workflowResource) {
        return readWorkflowMap(workflowResource, OBJECT_MAPPER);
    }

    @Override
    public WorkflowMapper resolve(WorkflowResource workflowResource) {
        return workflowResource.getWorkflowFormat() == Workflow.Format.YAML ? this : null;
    }
}
