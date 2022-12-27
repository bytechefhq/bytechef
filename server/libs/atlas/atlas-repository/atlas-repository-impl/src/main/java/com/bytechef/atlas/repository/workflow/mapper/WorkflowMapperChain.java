
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

package com.bytechef.atlas.repository.workflow.mapper;

import com.bytechef.atlas.domain.Workflow;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class WorkflowMapperChain implements WorkflowMapper {

    private final List<WorkflowMapperResolver> workflowMapperResolvers;

    public WorkflowMapperChain(List<WorkflowMapperResolver> workflowMapperResolvers) {
        this.workflowMapperResolvers = workflowMapperResolvers;
    }

    @Override
    public Workflow readValue(WorkflowResource workflowResource) {
        for (WorkflowMapperResolver workflowMapperResolver : workflowMapperResolvers) {
            WorkflowMapper workflowMapper = workflowMapperResolver.resolve(workflowResource);

            if (workflowMapper != null) {
                return workflowMapper.readValue(workflowResource);
            }
        }

        throw new IllegalArgumentException(
            "WorkflowMapper does not exist for format type " + workflowResource.getWorkflowFormat());
    }
}
