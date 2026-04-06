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

package com.bytechef.component.workflow;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.platform.component.constant.WorkflowConstants.WORKFLOW;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.workflow.action.WorkflowResponseToWorkflowCallAction;
import com.bytechef.component.workflow.cluster.WorkflowCallWorkflowTool;
import com.bytechef.component.workflow.subflow.sync.SubflowSyncExecutor;
import com.bytechef.component.workflow.trigger.WorkflowNewWorkflowCallTrigger;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(WORKFLOW + "_v1_ComponentHandler")
public class WorkflowComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public WorkflowComponentHandler(SubflowDataSource subflowDataSource, SubflowSyncExecutor subflowSyncExecutor) {
        this.componentDefinition = component(WORKFLOW)
            .title("Workflow")
            .description("Triggers and actions for workflow-to-workflow communication.")
            .icon("path:assets/workflow.svg")
            .categories(ComponentCategory.HELPERS)
            .triggers(WorkflowNewWorkflowCallTrigger.TRIGGER_DEFINITION)
            .actions(WorkflowResponseToWorkflowCallAction.ACTION_DEFINITION)
            .clusterElements(WorkflowCallWorkflowTool.of(subflowDataSource, subflowSyncExecutor));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
