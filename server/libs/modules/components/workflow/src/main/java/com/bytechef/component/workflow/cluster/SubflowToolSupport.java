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
import com.bytechef.platform.ai.tool.ToolSuspension;
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
 * @author Ivica Cardic
 */
final class SubflowToolSupport {

    private static final Logger log = LoggerFactory.getLogger(SubflowToolSupport.class);

    private static final String WORKFLOW = "workflow";

    static final String ERROR_RESOLVE_FAILED_PREFIX =
        "Error: could not resolve the requested sub-workflow: ";

    private SubflowToolSupport() {
    }

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

    static ActionContextAware requireGuardsPassed(
        ClusterElementContext context, String clusterElementName, String toolLabel) {

        ActionContext agentActionContext = resolveAgentActionContext(context, clusterElementName);

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

        return actionContextAware;
    }

    static Object suspendForSubflow(
        ActionContextAware actionContextAware, Subflow subflow, Map<String, ?> inputs, boolean editorEnvironment) {

        PendingSubflowRequest request = new PendingSubflowRequest(
            subflow.workflowId(), subflow.inputsName(), inputs, editorEnvironment, PlatformType.AUTOMATION);

        Map<String, Object> continueParameters = new HashMap<>();

        continueParameters.put(SubflowRequestConstants.PENDING_SUBFLOW, request);

        actionContextAware.suspend(new ActionContext.Suspend(continueParameters, null));

        return ToolSuspension.suspendedToolResult(actionContextAware);
    }

    private static @Nullable ActionContext resolveAgentActionContext(
        ClusterElementContext context, String clusterElementName) {

        if (context instanceof ClusterElementContextAware clusterElementContextAware) {
            return clusterElementContextAware.toActionContext(WORKFLOW, 1, clusterElementName, null);
        }

        return null;
    }
}
