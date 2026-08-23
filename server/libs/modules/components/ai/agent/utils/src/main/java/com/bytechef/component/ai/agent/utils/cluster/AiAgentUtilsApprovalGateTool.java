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

package com.bytechef.component.ai.agent.utils.cluster;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;

import com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.tool.constant.ToolConstants;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Gates the tools attached beneath it: every call raises an approval request through this gate's own channels and
 * suspends the run instead of executing. Enforcement is structural rather than a flag on a tool, so gating is visible
 * on the canvas and the LLM cannot route around it — it never sees the gate at all, only the tools, which arrive
 * already wrapped.
 *
 * <p>
 * Channels and expiry belong to the gate rather than the agent, so one agent can escalate destructive tools to Slack
 * with a short expiry while routine ones go to chat.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsApprovalGateTool {

    private static final String APPROVAL = "approval";
    /**
     * The gate's cluster element name. Public because {@code AiAgentUtilsComponentHandler} (which registers its allowed
     * child types) and {@code AiAgentUtilsTaskTool} (which refuses to run a gate inside a subagent) both match on it —
     * three private copies of one string is how the rename to {@code *Tool} nearly went half-done.
     */
    public static final String APPROVAL_GATE = "approvalGateTool";
    private static final String DAYS = "DAYS";
    private static final String NAME = "name";
    private static final String REQUEST_APPROVAL = "requestApproval";

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ClusterElementToolCallbacks clusterElementToolCallbacks;

    @Nullable
    private final ToolExecutionRecorder toolExecutionRecorder;

    public final ClusterElementDefinition<MultipleConnectionsToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsApprovalGateTool(
        ClusterElementToolCallbacks clusterElementToolCallbacks,
        ClusterElementDefinitionService clusterElementDefinitionService,
        @Nullable ToolExecutionRecorder toolExecutionRecorder) {

        this.clusterElementToolCallbacks = clusterElementToolCallbacks;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.toolExecutionRecorder = toolExecutionRecorder;

        this.clusterElementDefinition =
            ComponentDsl.<MultipleConnectionsToolCallbackProviderFunction>clusterElement(APPROVAL_GATE)
                .title("Approval Gate Tool")
                .description("Require human approval before the agent may call any tool attached to this gate.")
                .type(TOOLS)
                .properties(
                    string(NAME)
                        .label("Name")
                        .description("Identifies this gate in the approval request and in the tools list.")
                        .required(true),
                    integer(ToolConstants.APPROVAL_EXPIRES_IN)
                        .label("Expires In")
                        .description("How long an approval request stays valid. Defaults to 60 days when blank.")
                        .minValue(1)
                        .required(false),
                    string(ToolConstants.APPROVAL_EXPIRES_IN_UNIT)
                        .label("Expires In Unit")
                        .options(
                            option("Hours", ToolConstants.APPROVAL_EXPIRES_IN_UNIT_HOURS),
                            option("Days", DAYS))
                        .defaultValue(DAYS)
                        .required(false))
                .object(() -> this::apply);
    }

    /**
     * Rejects children that cannot be gated. {@code requestApproval} already suspends, so gating it would deliver two
     * approval requests and set a suspend whose continueParameters cannot be resumed; a nested gate would do the same.
     * Package-private so a test can pin both rejections without building a whole cluster element map.
     */
    static void checkGatableChild(String componentName, String clusterElementName) {
        if (APPROVAL.equals(componentName) && REQUEST_APPROVAL.equals(clusterElementName)) {
            throw new IllegalStateException(
                "'requestApproval' cannot be attached to an approval gate — it already raises an approval of its " +
                    "own. Attach it directly to the agent instead.");
        }

        if (APPROVAL_GATE.equals(clusterElementName)) {
            throw new IllegalStateException(
                "An approval gate cannot be attached to another approval gate. Attach the tools to one gate.");
        }
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections, Context context) {

        ActionContextAware actionContextAware = (ActionContextAware) context;

        return ToolCallbackProvider.from(
            buildGatedToolCallbacks(
                ClusterElementMap.of(extensions), componentConnections, actionContextAware.isEditorEnvironment(),
                (ActionContext) context, getApprovalExpiry(inputParameters)));
    }

    /**
     * Wraps every callback of every gated element exactly once. Package-private so a test can pin that directly: a tool
     * element contributing two callbacks must produce two gated callbacks, not one gate around a collection.
     */
    List<ToolCallback> buildGatedToolCallbacks(
        ClusterElementMap clusterElementMap, Map<String, ComponentConnection> componentConnections,
        boolean editorEnvironment, ActionContext context, @Nullable Duration approvalExpiry) {

        List<ClusterElement> approvalChannelClusterElements = clusterElementMap.getClusterElements(APPROVAL_CHANNELS);

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement toolClusterElement : clusterElementMap.getClusterElements(TOOLS)) {
            checkGatableChild(toolClusterElement.getComponentName(), toolClusterElement.getClusterElementName());

            List<ToolCallback> elementToolCallbacks = clusterElementToolCallbacks.build(
                toolClusterElement, componentConnections, editorEnvironment, context);

            for (ToolCallback toolCallback : elementToolCallbacks) {
                toolCallbacks.add(
                    new ApprovalGateToolCallback(
                        toolCallback, approvalChannelClusterElements, componentConnections,
                        clusterElementDefinitionService, context, toolExecutionRecorder, approvalExpiry));
            }
        }

        return toolCallbacks;
    }

    /**
     * Resolves the gate's expiry, or null to let {@link ApprovalGateToolCallback} apply its 60-day default. Moved off
     * per-tool parameters; the stored key names are unchanged.
     */
    @Nullable
    private static Duration getApprovalExpiry(Parameters inputParameters) {
        Object approvalExpiresIn = inputParameters.get(ToolConstants.APPROVAL_EXPIRES_IN);

        if (!(approvalExpiresIn instanceof Number approvalExpiresInNumber) || approvalExpiresInNumber.longValue() < 1) {
            return null;
        }

        Object approvalExpiresInUnit = inputParameters.get(ToolConstants.APPROVAL_EXPIRES_IN_UNIT);

        if (ToolConstants.APPROVAL_EXPIRES_IN_UNIT_HOURS.equals(approvalExpiresInUnit)) {
            return Duration.ofHours(approvalExpiresInNumber.longValue());
        }

        return Duration.ofDays(approvalExpiresInNumber.longValue());
    }
}
