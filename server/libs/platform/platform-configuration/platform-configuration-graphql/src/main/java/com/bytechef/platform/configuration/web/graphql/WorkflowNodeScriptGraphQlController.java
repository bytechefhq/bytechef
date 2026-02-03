/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.configuration.dto.ScriptTestExecutionDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeScriptFacade;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * Exposes WorkflowNodeScript operations over GraphQL.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class WorkflowNodeScriptGraphQlController {

    private final WorkflowNodeScriptFacade workflowNodeScriptFacade;

    public WorkflowNodeScriptGraphQlController(WorkflowNodeScriptFacade workflowNodeScriptFacade) {
        this.workflowNodeScriptFacade = workflowNodeScriptFacade;
    }

    @MutationMapping
    public ScriptTestExecutionDTO testClusterElementScript(
        @Argument String workflowId, @Argument String workflowNodeName, @Argument String clusterElementType,
        @Argument String clusterElementWorkflowNodeName, @Argument Long environmentId) {

        return workflowNodeScriptFacade.testClusterElementScript(
            workflowId, workflowNodeName, clusterElementType, clusterElementWorkflowNodeName, environmentId);
    }

    @MutationMapping
    public ScriptTestExecutionDTO testWorkflowNodeScript(
        @Argument String workflowId, @Argument String workflowNodeName, @Argument Long environmentId) {

        return workflowNodeScriptFacade.testWorkflowNodeScript(workflowId, workflowNodeName, environmentId);
    }
}
