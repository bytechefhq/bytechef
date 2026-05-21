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

import static com.bytechef.atlas.configuration.constant.WorkflowConstants.INPUTS;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.platform.ai.tool.constant.ToolConstants.TOOL_DESCRIPTION;
import static com.bytechef.platform.ai.tool.constant.ToolConstants.TOOL_NAME;
import static com.bytechef.platform.component.constant.WorkflowConstants.NEW_WORKFLOW_CALL;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ClusterElementDefinition.OutputFunction;
import com.bytechef.component.definition.ClusterElementDefinition.PropertiesFunction;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class WorkflowCallWorkflowTool {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCallWorkflowTool.class);

    private static final String WORKFLOW_UUID = "workflowUuid";

    // Error tokens returned to the LLM. Extracted as constants so tests can assert by equality (not substring),
    // and so callers downstream can branch on them without parsing free-form text.

    static final String ERROR_NOT_AGENT_CONTEXT =
        "Error: the Call Workflow tool can only be used by an AI agent.";

    static final String ERROR_ALREADY_SUSPENDED =
        "Error: another tool already suspended the agent in this turn; only one suspending tool call " +
            "(including Call Workflow) is supported per turn.";

    static final String ERROR_AGENT_IS_SUBFLOW =
        "Error: calling a sub-workflow as a tool is not supported when the agent itself runs as a sub-workflow.";

    static final String ERROR_RESOLVE_FAILED_PREFIX =
        "Error: could not resolve the requested sub-workflow: ";

    private WorkflowCallWorkflowTool() {
    }

    public static ClusterElementDefinition<ToolFunction> of(
        SubflowDataSource subflowDataSource, SubflowResolver subflowResolver) {

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
                string(WORKFLOW_UUID)
                    .label("Workflow")
                    .description("The workflow to call when this tool is invoked.")
                    .options(getWorkflowOptionsFunction(subflowDataSource))
                    .required(true),
                dynamicProperties(INPUTS)
                    .description("The input parameters for the sub-workflow.")
                    .propertiesLookupDependsOn(WORKFLOW_UUID)
                    .properties(getPropertiesFunction(subflowDataSource)))
            .object(() -> getToolFunction(subflowResolver))
            .output(getOutputFunction(subflowDataSource));
    }

    private static ToolFunction getToolFunction(SubflowResolver subflowResolver) {
        return (inputParameters, connectionParameters, context) -> {
            ActionContext agentActionContext = resolveAgentActionContext(context);

            if (!(agentActionContext instanceof ActionContextAware actionContextAware)) {
                log.warn("Call Workflow tool invoked outside an AI agent context (contract violation)");

                return ERROR_NOT_AGENT_CONTEXT;
            }

            if (actionContextAware.getSuspend() != null) {
                log.warn(
                    "Call Workflow tool invoked after another tool already suspended the agent (jobId={})",
                    actionContextAware.getJobId());

                return ERROR_ALREADY_SUSPENDED;
            }

            // The agent is itself a sub-workflow (parentTaskExecutionId != null). JobServiceImpl.resumeToStatusStarted
            // asserts parentTaskExecutionId == null, so a later resumeJob on the agent would throw and the agent
            // would be parked forever -- exactly the silent-park failure mode #5055 was filed against. Fail fast
            // with an LLM-readable error instead of suspending into an irrecoverable state.

            if (actionContextAware.getParentTaskExecutionId() != null) {
                log.warn(
                    "Call Workflow tool invoked from an agent that itself runs as a sub-workflow "
                        + "(jobId={}, parentTaskExecutionId={}); refusing to suspend",
                    actionContextAware.getJobId(), actionContextAware.getParentTaskExecutionId());

                return ERROR_AGENT_IS_SUBFLOW;
            }

            String workflowUuid = inputParameters.getRequiredString(WORKFLOW_UUID);
            Map<String, ?> inputs = inputParameters.getMap(INPUTS, Collections.emptyMap());

            boolean editorEnvironment = context.isEditorEnvironment();

            // Resolution can throw -- e.g., workflow missing/unpublished, trigger removed. Per the design spec's
            // Error handling table, this must surface to the LLM as a tool-result error, not escape to Spring AI.

            Subflow subflow;

            try {
                subflow = subflowResolver.resolveSubflow(workflowUuid, NEW_WORKFLOW_CALL, editorEnvironment);
            } catch (RuntimeException exception) {
                log.warn(
                    "Sub-workflow resolution failed for uuid={} (jobId={}): {}",
                    workflowUuid, actionContextAware.getJobId(), exception.getMessage());

                return ERROR_RESOLVE_FAILED_PREFIX + exception.getMessage();
            }

            PendingSubflowRequest request = new PendingSubflowRequest(
                subflow.workflowId(), subflow.inputsName(), inputs, editorEnvironment, PlatformType.AUTOMATION);

            Map<String, Object> continueParameters = new HashMap<>();

            continueParameters.put(SubflowRequestConstants.PENDING_SUBFLOW, request);

            agentActionContext.suspend(new ActionContext.Suspend(continueParameters, null));

            return ToolSuspendConstants.SUSPENDED_SENTINEL;
        };
    }

    private static ActionContext resolveAgentActionContext(ClusterElementContext context) {
        if (context instanceof ClusterElementContextAware clusterElementContextAware) {
            return clusterElementContextAware.getAgentActionContext();
        }

        return null;
    }

    private static ClusterElementDefinition.OptionsFunction<String> getWorkflowOptionsFunction(
        SubflowDataSource subflowDataSource) {

        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> subflowDataSource
            .getSubWorkflows(PlatformType.AUTOMATION, NEW_WORKFLOW_CALL, searchText)
            .stream()
            .map(subWorkflowEntry -> option(subWorkflowEntry.name(), subWorkflowEntry.workflowUuid()))
            .toList();
    }

    private static OutputFunction getOutputFunction(SubflowDataSource subflowDataSource) {
        return (inputParameters, connectionParameters, context) -> {
            String workflowUuid = inputParameters.getString(WORKFLOW_UUID);

            if (workflowUuid == null || workflowUuid.isEmpty()) {
                return null;
            }

            BaseValueProperty<?> outputSchema = subflowDataSource.getSubWorkflowOutputSchema(workflowUuid);

            if (outputSchema == null) {
                return null;
            }

            return BaseOutputDefinition.OutputResponse.of(toComponentProperty(outputSchema));
        };
    }

    private static PropertiesFunction getPropertiesFunction(SubflowDataSource subflowDataSource) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, context) -> {

            String workflowUuid = inputParameters.getString(WORKFLOW_UUID);

            if (workflowUuid == null || workflowUuid.isEmpty()) {
                return List.of();
            }

            BaseValueProperty<?> inputSchema = subflowDataSource.getSubWorkflowInputSchema(workflowUuid);

            if (!(inputSchema instanceof BaseProperty.BaseObjectProperty<?> objectProperty)) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<? extends BaseValueProperty<?>> baseProperties =
                (List<? extends BaseValueProperty<?>>) objectProperty.getProperties()
                    .orElse(List.of());

            List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

            for (BaseValueProperty<?> baseProperty : baseProperties) {
                ModifiableValueProperty<?, ?> property = toComponentProperty(baseProperty);

                properties.add(property);
            }

            return properties;
        };
    }

    @SuppressWarnings("unchecked")
    private static ModifiableValueProperty<?, ?> toComponentProperty(BaseValueProperty<?> baseProperty) {
        String name = baseProperty.getName();

        ModifiableValueProperty<?, ?> property;

        if (baseProperty instanceof BaseProperty.BaseArrayProperty<?> arrayProperty) {
            ComponentDsl.ModifiableArrayProperty newArrayProperty = ComponentDsl.array(name);

            arrayProperty.getItems()
                .ifPresent(items -> {
                    List<ModifiableValueProperty<?, ?>> itemProperties =
                        ((List<BaseValueProperty<?>>) items).stream()
                            .<ModifiableValueProperty<?, ?>>map(WorkflowCallWorkflowTool::toComponentProperty)
                            .toList();

                    newArrayProperty.items(itemProperties);
                });

            property = newArrayProperty;
        } else if (baseProperty instanceof BaseProperty.BaseBooleanProperty) {
            property = ComponentDsl.bool(name);
        } else if (baseProperty instanceof BaseProperty.BaseDateProperty) {
            property = ComponentDsl.date(name);
        } else if (baseProperty instanceof BaseProperty.BaseDateTimeProperty) {
            property = ComponentDsl.dateTime(name);
        } else if (baseProperty instanceof BaseProperty.BaseIntegerProperty) {
            property = ComponentDsl.integer(name);
        } else if (baseProperty instanceof BaseProperty.BaseNumberProperty) {
            property = ComponentDsl.number(name);
        } else if (baseProperty instanceof BaseProperty.BaseObjectProperty<?> objectProperty) {
            ComponentDsl.ModifiableObjectProperty newObjectProperty = ComponentDsl.object(name);

            objectProperty.getProperties()
                .ifPresent(childProperties -> {
                    List<ModifiableValueProperty<?, ?>> childComponentProperties =
                        ((List<BaseValueProperty<?>>) childProperties).stream()
                            .<ModifiableValueProperty<?, ?>>map(WorkflowCallWorkflowTool::toComponentProperty)
                            .toList();

                    newObjectProperty.properties(childComponentProperties);
                });

            property = newObjectProperty;
        } else {
            property = string(name);
        }

        baseProperty.getDescription()
            .ifPresent(property::description);

        baseProperty.getRequired();

        return property;
    }
}
