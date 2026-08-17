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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared guard-then-suspend machinery for the AI-agent tools that hand off to a durable sub-workflow call
 * ({@link WorkflowCallWorkflowTool}, {@link WorkflowCallAgentTool}) — every such tool must run the same three guards
 * (agent context present, no suspend already pending this turn, the calling agent is not itself a sub-workflow) before
 * it may suspend, and the same {@link PendingSubflowRequest}-carrying suspend at the end. Each tool supplies its own
 * {@code toolLabel} (e.g. {@code "Call Workflow"}, {@code "Call Agent"}) so the LLM-facing error text and log lines
 * name the right tool, and keeps its own error constants (equal to what this class produces) so existing tests that
 * assert against those constants by name keep compiling and passing unchanged.
 *
 * @author Ivica Cardic
 */
final class SubflowToolSupport {

    private static final Logger log = LoggerFactory.getLogger(SubflowToolSupport.class);

    /** Generic — no tool name embedded, so both tools can share the literal value verbatim. */
    static final String ERROR_AGENT_IS_SUBFLOW =
        "Error: calling a sub-workflow as a tool is not supported when the agent itself runs as a sub-workflow.";

    /** Generic — no tool name embedded, so both tools can share the literal value verbatim. */
    static final String ERROR_RESOLVE_FAILED_PREFIX =
        "Error: could not resolve the requested sub-workflow: ";

    private SubflowToolSupport() {
    }

    /**
     * Thrown by {@link #requireGuardsPassed(ClusterElementContext, String)} when a guard fails — {@link #getMessage()}
     * is the exact LLM-facing error string the calling tool should return (without suspending). Kept as an internal
     * control-flow exception (never propagated past the tool's own {@code ToolFunction} lambda) rather than a
     * nullable-field result object so callers get a plain, always-non-null {@link ActionContextAware} back on the
     * success path — avoids the nullness ambiguity a two-nullable-field result type would otherwise force onto every
     * caller.
     */
    static final class GuardFailure extends RuntimeException {

        GuardFailure(String message) {
            super(message);
        }
    }

    static String notAgentContextError(String toolLabel) {
        return "Error: the " + toolLabel + " tool can only be used by an AI agent.";
    }

    static String alreadySuspendedError(String toolLabel) {
        return "Error: another tool already suspended the agent in this turn; only one suspending tool call "
            + "(including " + toolLabel + ") is supported per turn.";
    }

    /**
     * Runs the three suspend guards in order, logging and throwing {@link GuardFailure} (carrying the exact
     * LLM-readable error string to return) on the first one that fails. Never suspends itself — the caller suspends
     * once it has also resolved whatever it needs to resolve.
     *
     * @throws GuardFailure if any guard fails
     */
    static ActionContextAware requireGuardsPassed(ClusterElementContext context, String toolLabel) {
        ActionContext agentActionContext = resolveAgentActionContext(context);

        if (!(agentActionContext instanceof ActionContextAware actionContextAware)) {
            log.warn("{} tool invoked outside an AI agent context (contract violation)", toolLabel);

            throw new GuardFailure(notAgentContextError(toolLabel));
        }

        if (actionContextAware.getSuspend() != null) {
            log.warn(
                "{} tool invoked after another tool already suspended the agent (jobId={})", toolLabel,
                actionContextAware.getJobId());

            throw new GuardFailure(alreadySuspendedError(toolLabel));
        }

        // The agent is itself a sub-workflow (parentTaskExecutionId != null). JobServiceImpl.resumeToStatusStarted
        // asserts parentTaskExecutionId == null, so a later resumeJob on the agent would throw and the agent would be
        // parked forever -- exactly the silent-park failure mode #5055 was filed against. Fail fast with an
        // LLM-readable error instead of suspending into an irrecoverable state.

        if (actionContextAware.getParentTaskExecutionId() != null) {
            log.warn(
                "{} tool invoked from an agent that itself runs as a sub-workflow (jobId={}, "
                    + "parentTaskExecutionId={}); refusing to suspend",
                toolLabel, actionContextAware.getJobId(), actionContextAware.getParentTaskExecutionId());

            throw new GuardFailure(ERROR_AGENT_IS_SUBFLOW);
        }

        return actionContextAware;
    }

    /**
     * Builds the {@link PendingSubflowRequest}, stashes it in the agent's {@code Suspend.continueParameters}, and
     * suspends — the common tail both tools run once they have resolved a target {@link Subflow} and built their own
     * {@code inputs} map.
     */
    static Object suspendForSubflow(
        ActionContextAware actionContextAware, Subflow subflow, Map<String, ?> inputs, boolean editorEnvironment) {

        PendingSubflowRequest request = new PendingSubflowRequest(
            subflow.workflowId(), subflow.inputsName(), inputs, editorEnvironment, PlatformType.AUTOMATION);

        Map<String, Object> continueParameters = new HashMap<>();

        continueParameters.put(SubflowRequestConstants.PENDING_SUBFLOW, request);

        actionContextAware.suspend(new ActionContext.Suspend(continueParameters, null));

        return ToolSuspendConstants.SUSPENDED_SENTINEL;
    }

    private static @Nullable ActionContext resolveAgentActionContext(ClusterElementContext context) {
        if (context instanceof ClusterElementContextAware clusterElementContextAware) {
            return clusterElementContextAware.getAgentActionContext();
        }

        return null;
    }
}
