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
import com.bytechef.component.workflow.subflow.sync.SubflowSyncExecutor;
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
 * {@code AiAgent} as a tool, sharing the same runtime {@link WorkflowCallWorkflowTool} uses: synchronous in the editor,
 * a durable suspend/resume bridge in deployed runs (see {@link SubflowToolSupport}). Unlike {@code callWorkflow} —
 * which exposes an arbitrary sub-workflow's own {@code workflowCall} input schema as the LLM's tool arguments — this
 * tool targets a fixed {@code {message}} contract: every AiAgent-generated workflow's {@code workflowCall} channel
 * always declares exactly {@code {message, conversationId}} (see
 * {@code AiAgentWorkflowGenerator.WORKFLOW_CALL_INPUT_SCHEMA}), so a single static {@code message} property serves both
 * the canvas builder mapping and (once wired to a {@code fromAi(...)} expression, either by a human via the "let AI
 * decide" toggle or by {@code AiAgentWorkflowGenerator} for a generated {@code SUB_AGENT} row) the LLM function-calling
 * schema — see this class's {@code message} property javadoc for how that schema is actually derived.
 *
 * @author Ivica Cardic
 */
public class WorkflowCallAiAgentTool {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCallAiAgentTool.class);

    private static final String TOOL_LABEL = "Call AI Agent";

    static final String AGENT_UUID = "agentUuid";
    static final String MESSAGE = "message";
    static final String CONVERSATION_ID = "conversationId";

    static final String ERROR_NOT_AGENT_CONTEXT = SubflowToolSupport.notAgentContextError(TOOL_LABEL);

    static final String ERROR_ALREADY_SUSPENDED = SubflowToolSupport.alreadySuspendedError(TOOL_LABEL);

    static final String ERROR_RESOLVE_FAILED_PREFIX = SubflowToolSupport.ERROR_RESOLVE_FAILED_PREFIX;

    static final String ERROR_AGENT_RESOLVE_FAILED_PREFIX = "Error: could not resolve the requested agent: ";

    private WorkflowCallAiAgentTool() {
    }

    public static ClusterElementDefinition<ToolFunction> of(
        CallableAiAgentDataSource callableAgentDataSource, SubflowResolver subflowResolver,
        SubflowSyncExecutor subflowSyncExecutor) {

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
            .object(() -> getToolFunction(callableAgentDataSource, subflowResolver, subflowSyncExecutor));
    }

    private static ToolFunction getToolFunction(
        CallableAiAgentDataSource callableAgentDataSource, SubflowResolver subflowResolver,
        SubflowSyncExecutor subflowSyncExecutor) {

        return (inputParameters, connectionParameters, context) -> {
            String agentUuid = inputParameters.getRequiredString(AGENT_UUID);
            String message = inputParameters.getRequiredString(MESSAGE);
            String conversationId = inputParameters.getString(CONVERSATION_ID);

            Map<String, Object> inputs = new HashMap<>();

            inputs.put(MESSAGE, message);

            if (conversationId != null && !conversationId.isBlank()) {
                inputs.put(CONVERSATION_ID, conversationId);
            }

            if (context.isEditorEnvironment()) {
                ResolvedAiAgent resolvedAgent;

                try {
                    resolvedAgent = callableAgentDataSource.resolveAgent(agentUuid, true);
                } catch (RuntimeException exception) {
                    log.warn("Agent resolution failed for agentUuid={}: {}", agentUuid, exception.getMessage());

                    return ERROR_AGENT_RESOLVE_FAILED_PREFIX + exception.getMessage();
                }

                return SubflowToolSupport.executeSynchronously(
                    subflowSyncExecutor, resolvedAgent.workflowUuid(), inputs, true,
                    SubflowToolSupport.findCallerJobId(context, "callAiAgent"));
            }

            ActionContextAware actionContextAware;

            switch (SubflowToolSupport.checkAgentContext(context, "callAiAgent", TOOL_LABEL)) {
                case SubflowToolSupport.AgentContextCheck.Rejected rejected -> {
                    return rejected.error();
                }
                case SubflowToolSupport.AgentContextCheck.Passed passed -> actionContextAware =
                    passed.actionContextAware();
            }

            ResolvedAiAgent resolvedAgent;

            try {
                resolvedAgent = callableAgentDataSource.resolveAgent(agentUuid, false);
            } catch (RuntimeException exception) {
                log.warn(
                    "Agent resolution failed for agentUuid={} (jobId={}): {}",
                    agentUuid, actionContextAware.getJobId(), exception.getMessage());

                return ERROR_AGENT_RESOLVE_FAILED_PREFIX + exception.getMessage();
            }

            if (SubflowToolSupport.isSyncJob(actionContextAware, subflowSyncExecutor)) {
                return SubflowToolSupport.executeSynchronously(
                    subflowSyncExecutor, resolvedAgent.workflowUuid(), inputs, false, actionContextAware.getJobId());
            }

            Subflow subflow;

            try {
                subflow = subflowResolver.resolveSubflow(resolvedAgent.workflowUuid(), NEW_WORKFLOW_CALL, false);
            } catch (RuntimeException exception) {
                log.warn(
                    "Sub-workflow resolution failed for agentUuid={} agentName={} workflowUuid={} (jobId={}): {}",
                    agentUuid, resolvedAgent.name(), resolvedAgent.workflowUuid(), actionContextAware.getJobId(),
                    exception.getMessage());

                return ERROR_RESOLVE_FAILED_PREFIX + exception.getMessage();
            }

            return SubflowToolSupport.suspendForSubflow(actionContextAware, subflow, inputs);
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
