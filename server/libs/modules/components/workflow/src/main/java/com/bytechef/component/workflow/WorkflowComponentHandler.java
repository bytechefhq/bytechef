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
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.workflow.action.WorkflowResponseToWorkflowCallAction;
import com.bytechef.component.workflow.cluster.WorkflowCallAiAgentTool;
import com.bytechef.component.workflow.cluster.WorkflowCallWorkflowTool;
import com.bytechef.component.workflow.trigger.WorkflowNewWorkflowCallTrigger;
import com.bytechef.component.workflow.trigger.WorkflowNewWorkflowErrorTrigger;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(WORKFLOW + "_v1_ComponentHandler")
public final class WorkflowComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    /**
     * {@code SubflowDataSource}/{@code SubflowResolver} are required constructor dependencies (mirroring how they were
     * wired before this class ever saw an agents-aware element) because their implementation lives in
     * {@code automation-configuration-service}, which every assembly of this component is already built alongside.
     * {@code CallableAiAgentDataSource} instead comes through an {@link ObjectProvider}: its implementation lives in
     * {@code automation-ai-agent-service}, a separate module this one has no compile-time dependency on, so the
     * {@code callAiAgent} cluster element is added only when a bean happens to be present and silently omitted (rather
     * than failing component-handler construction) when it is not.
     */
    public WorkflowComponentHandler(
        SubflowDataSource subflowDataSource, SubflowResolver subflowResolver,
        ObjectProvider<CallableAiAgentDataSource> callableAgentDataSourceProvider) {

        List<ClusterElementDefinition<?>> clusterElementDefinitions = new ArrayList<>();

        clusterElementDefinitions.add(WorkflowCallWorkflowTool.of(subflowDataSource, subflowResolver));

        CallableAiAgentDataSource callableAgentDataSource = callableAgentDataSourceProvider.getIfAvailable();

        if (callableAgentDataSource != null) {
            clusterElementDefinitions.add(WorkflowCallAiAgentTool.of(callableAgentDataSource, subflowResolver));
        }

        this.componentDefinition = component(WORKFLOW)
            .title("Workflow")
            .description("Triggers and actions for workflow-to-workflow communication.")
            .icon("path:assets/workflow.svg")
            .categories(ComponentCategory.HELPERS)
            .triggers(
                WorkflowNewWorkflowCallTrigger.TRIGGER_DEFINITION,
                WorkflowNewWorkflowErrorTrigger.TRIGGER_DEFINITION)
            .actions(WorkflowResponseToWorkflowCallAction.ACTION_DEFINITION)
            .clusterElements(clusterElementDefinitions.toArray(new ClusterElementDefinition<?>[0]));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
