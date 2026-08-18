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

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.platform.ai.tool.constant.ToolConstants.TOOL_DESCRIPTION;
import static com.bytechef.platform.ai.tool.constant.ToolConstants.TOOL_NAME;
import static com.bytechef.platform.component.constant.WorkflowConstants.NEW_WORKFLOW_CALL;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource.ResolvedAiAgent;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * n8n-parity "Call AI Agent" TOOLS cluster element: lets a workflow-canvas AI agent call a published (automation)
 * {@code AiAgent} as a tool, sharing the same durable-suspend runtime {@link WorkflowCallWorkflowTool} uses (see
 * {@link SubflowToolSupport}). Unlike {@code callWorkflow} — which exposes an arbitrary sub-workflow's own
 * {@code workflowCall} input schema as the LLM's tool arguments — this tool targets a fixed {@code {message}} contract:
 * every AiAgent-generated workflow's {@code workflowCall} channel always declares exactly
 * {@code {message, conversationId}} (see {@code WorkflowConstants.AI_AGENT_CALL_INPUT_SCHEMA}, which the component pins
 * on that channel's own {@code triggerParameters}), so a single static {@code message} property serves both the canvas
 * builder mapping and (once wired to a {@code fromAi(...)} expression, either by a human via the "let AI decide" toggle
 * or by {@code AiAgentWorkflowGenerator} for a generated {@code SUB_AGENT} row) the LLM function-calling schema — see
 * this class's {@code message} property javadoc for how that schema is actually derived.
 *
 * @author Ivica Cardic
 */
public class WorkflowCallAiAgentTool {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCallAiAgentTool.class);

    private static final String TOOL_LABEL = "Call AI Agent";

    static final String AGENT_UUID = "agentUuid";
    static final String MESSAGE = "message";
    static final String CONVERSATION_ID = "conversationId";

    // Same sharing convention as WorkflowCallWorkflowTool: canonical text/logic lives in SubflowToolSupport, these
    // fields are kept equal-valued on this class so tests can assert against WorkflowCallAiAgentTool.ERROR_* by name.

    static final String ERROR_NOT_AGENT_CONTEXT = SubflowToolSupport.notAgentContextError(TOOL_LABEL);

    static final String ERROR_ALREADY_SUSPENDED = SubflowToolSupport.alreadySuspendedError(TOOL_LABEL);

    static final String ERROR_AGENT_IS_SUBFLOW = SubflowToolSupport.ERROR_AGENT_IS_SUBFLOW;

    static final String ERROR_RESOLVE_FAILED_PREFIX = SubflowToolSupport.ERROR_RESOLVE_FAILED_PREFIX;

    /** Distinct from {@link #ERROR_RESOLVE_FAILED_PREFIX}: this one covers agentUuid -> workflow resolution. */
    static final String ERROR_AGENT_RESOLVE_FAILED_PREFIX = "Error: could not resolve the requested agent: ";

    private WorkflowCallAiAgentTool() {
    }

    public static ClusterElementDefinition<ToolFunction> of(
        CallableAiAgentDataSource callableAgentDataSource, SubflowResolver subflowResolver) {

        return ComponentDsl.<ToolFunction>clusterElement("callAiAgent")
            .title("Call AI Agent")
            .description("Calls another (published) agent as an AI agent tool.")
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
                string(AGENT_UUID)
                    .label("Agent")
                    .description("The published agent to call when this tool is invoked.")
                    .options(getAgentOptionsFunction(callableAgentDataSource))
                    .required(true),
                // A static, fixed-name property rather than callWorkflow's dynamicProperties(INPUTS): every
                // Agent-generated workflow's workflowCall trigger declares the exact same {message, conversationId}
                // input schema, so there is nothing to look up dynamically per-agent. The LLM tool schema is derived
                // by AiAgentToolFacade.getFunctionToolCallback -> AbstractToolFacade.extractFromAiResults, which scans
                // this cluster element's WHOLE configured parameter tree for embedded fromAi(...) expressions
                // (verified: it is the sole schema-generation path for every TOOLS cluster element, generic or
                // custom-ToolFunction alike) -- so a plain required "message" property, mapped to a fromAi(...)
                // expression at the value level, serves both the canvas builder mapping AND the LLM schema.
                string(MESSAGE)
                    .label("Message")
                    .description("The message sent to the agent.")
                    .controlType(TEXT_AREA)
                    .required(true),
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description(
                        "Optional memory-thread key passed to the called agent. Left blank, the sub-agent starts " +
                            "(or continues) whatever thread its own conversationId input resolves to."))
            .object(() -> getToolFunction(callableAgentDataSource, subflowResolver));
    }

    private static ToolFunction getToolFunction(
        CallableAiAgentDataSource callableAgentDataSource, SubflowResolver subflowResolver) {

        return (inputParameters, connectionParameters, context) -> {
            ActionContextAware actionContextAware;

            try {
                actionContextAware = SubflowToolSupport.requireGuardsPassed(context, TOOL_LABEL);
            } catch (SubflowToolSupport.GuardFailure guardFailure) {
                return guardFailure.getMessage();
            }

            String agentUuid = inputParameters.getRequiredString(AGENT_UUID);
            String message = inputParameters.getRequiredString(MESSAGE);
            String conversationId = inputParameters.getString(CONVERSATION_ID);

            boolean editorEnvironment = context.isEditorEnvironment();

            // Two resolution steps can throw: agentUuid -> target agent/workflow mapping (agent deleted/never
            // published), then workflow -> callable trigger (trigger removed). Per the design spec's Error handling
            // table, both must surface to the LLM as a tool-result error, not escape to Spring AI.

            ResolvedAiAgent resolvedAgent;

            try {
                resolvedAgent = callableAgentDataSource.resolveAgent(agentUuid, editorEnvironment);
            } catch (RuntimeException exception) {
                log.warn(
                    "Agent resolution failed for agentUuid={} (jobId={}): {}",
                    agentUuid, actionContextAware.getJobId(), exception.getMessage());

                return ERROR_AGENT_RESOLVE_FAILED_PREFIX + exception.getMessage();
            }

            Subflow subflow;

            // resolvedAgent.description() is intentionally unused here -- ResolvedAiAgent mirrors the
            // CallableAiAgentDataSource SPI shape (workflowUuid/name/description) rather than trimming it down to
            // just what this one caller happens to need; .name() earns its keep in the log line below.
            try {
                subflow = subflowResolver.resolveSubflow(
                    resolvedAgent.workflowUuid(), NEW_WORKFLOW_CALL, editorEnvironment);
            } catch (RuntimeException exception) {
                log.warn(
                    "Sub-workflow resolution failed for agentUuid={} agentName={} workflowUuid={} (jobId={}): {}",
                    agentUuid, resolvedAgent.name(), resolvedAgent.workflowUuid(), actionContextAware.getJobId(),
                    exception.getMessage());

                return ERROR_RESOLVE_FAILED_PREFIX + exception.getMessage();
            }

            Map<String, Object> inputs = new HashMap<>();

            inputs.put(MESSAGE, message);

            if (conversationId != null && !conversationId.isBlank()) {
                inputs.put(CONVERSATION_ID, conversationId);
            }

            return SubflowToolSupport.suspendForSubflow(actionContextAware, subflow, inputs, editorEnvironment);
        };
    }

    private static ClusterElementDefinition.OptionsFunction<String> getAgentOptionsFunction(
        CallableAiAgentDataSource callableAgentDataSource) {

        return (
            inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> callableAgentDataSource
                .getCallableAgents(searchText)
                .stream()
                .map(entry -> option(entry.title(), entry.agentUuid()))
                .toList();
    }
}
