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

package com.bytechef.atlas.workflow.repository.mapper;

import com.bytechef.atlas.workflow.WorkflowFormat;
import com.bytechef.atlas.workflow.WorkflowResource;
import com.bytechef.atlas.workflow.domain.Workflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class YamlWorkflowMapper extends BaseWorkflowMapper implements WorkflowMapperResolver {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public Workflow readValue(WorkflowResource aResource) {
        return readValue(aResource, mapper);
    }

    @Override
    public WorkflowMapper resolve(WorkflowResource workflowResource) {
        return workflowResource.getWorkflowFormat() == WorkflowFormat.YAML ? this : null;
    }
}
