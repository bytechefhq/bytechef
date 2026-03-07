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

package com.bytechef.component.workflow.cluster;

import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_DESCRIPTION;
import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_NAME;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;

/**
 * @author Ivica Cardic
 */
public class WorkflowCallWorkflowTool {

    public static final String WORKFLOW_ID = "workflowId";

    public static ClusterElementDefinition<ToolFunction> of(SubflowDataSource subflowDataSource) {
        return ComponentDsl.<ToolFunction>clusterElement("callWorkflow")
            .title("Call Workflow")
            .description("Calls another workflow as an AI agent tool.")
            .type(TOOLS)
            .properties(
                string(TOOL_NAME)
                    .label("Name")
                    .description("The tool name exposed to the AI model.")
                    .expressionEnabled(false)
                    .required(true),
                string(TOOL_DESCRIPTION)
                    .label("Description")
                    .description("The tool description exposed to the AI model.")
                    .controlType(TEXT_AREA)
                    .expressionEnabled(false)
                    .required(true),
                string(WORKFLOW_ID)
                    .label("Workflow")
                    .description("The workflow to call when this tool is invoked.")
                    .options(
                        (ClusterElementDefinition.OptionsFunction<String>) (
                            inputParameters, connectionParameters, lookupDependsOnPaths, searchText,
                            context) -> subflowDataSource
                                .getSubWorkflows(PlatformType.AUTOMATION, searchText)
                                .stream()
                                .map(
                                    subWorkflowEntry -> option(
                                        subWorkflowEntry.name(), subWorkflowEntry.workflowUuid()))
                                .toList())
                    .required(true));
    }

    private WorkflowCallWorkflowTool() {
    }
}
