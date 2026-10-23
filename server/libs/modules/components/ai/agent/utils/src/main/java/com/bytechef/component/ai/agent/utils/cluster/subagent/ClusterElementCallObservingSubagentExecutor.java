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

package com.bytechef.component.ai.agent.utils.cluster.subagent;

import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.configuration.domain.ClusterElement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentExecutor;
import org.springaicommunity.agent.common.task.subagent.TaskCall;

/**
 * Reports each run of a cluster element sub-agent to the {@link ClusterElementCallRecorder} under the sub-agent
 * element's workflow node name, against the job and task execution of the agent that owns the Task Tool, including runs
 * that execute in the background.
 *
 * @author Ivica Cardic
 */
public class ClusterElementCallObservingSubagentExecutor implements SubagentExecutor {

    private final SubagentExecutor delegate;
    private final long jobId;
    private final ClusterElementCallRecorder recorder;
    private final long taskExecutionId;

    @SuppressFBWarnings("EI2")
    public ClusterElementCallObservingSubagentExecutor(
        SubagentExecutor delegate, long jobId, long taskExecutionId, ClusterElementCallRecorder recorder) {

        this.delegate = delegate;
        this.jobId = jobId;
        this.recorder = recorder;
        this.taskExecutionId = taskExecutionId;
    }

    @Override
    public String execute(TaskCall taskCall, SubagentDefinition subagentDefinition) {
        if (!(subagentDefinition instanceof ClusterElementSubagentDefinition clusterElementSubagentDefinition)) {
            return delegate.execute(taskCall, subagentDefinition);
        }

        ClusterElement clusterElement = clusterElementSubagentDefinition.getClusterElement();

        String workflowNodeName = clusterElement.getWorkflowNodeName();

        recorder.callStarted(jobId, taskExecutionId, workflowNodeName);

        String result;

        try {
            result = delegate.execute(taskCall, subagentDefinition);
        } catch (RuntimeException exception) {
            String message = exception.getMessage();

            recorder.callFailed(
                jobId, taskExecutionId, workflowNodeName,
                message == null ? exception.getClass()
                    .getSimpleName() : message);

            throw exception;
        }

        recorder.callCompleted(jobId, taskExecutionId, workflowNodeName);

        return result;
    }

    @Override
    public String getKind() {
        return delegate.getKind();
    }
}
