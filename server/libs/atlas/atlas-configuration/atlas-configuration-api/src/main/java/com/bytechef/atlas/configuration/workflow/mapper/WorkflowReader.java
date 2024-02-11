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

package com.bytechef.atlas.configuration.workflow.mapper;

import com.bytechef.atlas.configuration.domain.Workflow;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WorkflowReader {

    private static final List<WorkflowMapper> workflowMapperResolvers = List.of(
        new JsonWorkflowMapper(), new YamlWorkflowMapper());

    public static Workflow readWorkflow(WorkflowResource workflowResource, int type) throws Exception {
        WorkflowMapper workflowMapper = getWorkflowMapper(workflowResource);

        return workflowMapper.readWorkflow(workflowResource, type);
    }

    public static Map<String, Object> readWorkflowMap(WorkflowResource workflowResource) throws Exception {
        WorkflowMapper workflowMapper = getWorkflowMapper(workflowResource);

        return workflowMapper.readWorkflowMap(workflowResource);
    }

    private static WorkflowMapper getWorkflowMapper(WorkflowResource workflowResource) {
        for (WorkflowMapper workflowMapperResolver : workflowMapperResolvers) {
            WorkflowMapper workflowMapper = workflowMapperResolver.resolve(workflowResource);

            if (workflowMapper != null) {
                return workflowMapper;
            }
        }

        throw new IllegalArgumentException(
            "WorkflowMapper does not exist for format type=" + workflowResource.getWorkflowFormat());
    }
}
