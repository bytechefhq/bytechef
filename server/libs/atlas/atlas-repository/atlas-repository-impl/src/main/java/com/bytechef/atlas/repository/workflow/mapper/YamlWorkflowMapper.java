
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

package com.bytechef.atlas.repository.workflow.mapper;

import com.bytechef.atlas.domain.Workflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class YamlWorkflowMapper extends AbstractWorkflowMapper implements WorkflowMapperResolver {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

    public Workflow readValue(WorkflowResource workflowResource) {
        return readValue(workflowResource, OBJECT_MAPPER);
    }

    @Override
    public WorkflowMapper resolve(WorkflowResource workflowResource) {
        return workflowResource.getWorkflowFormat() == Workflow.Format.YAML
            ? this
            : null;
    }
}
