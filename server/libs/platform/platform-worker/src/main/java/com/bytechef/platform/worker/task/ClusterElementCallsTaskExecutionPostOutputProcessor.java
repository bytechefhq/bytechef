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

package com.bytechef.platform.worker.task;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCall;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.workflow.execution.constant.TaskExecutionMetadataConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * Stores the cluster element calls recorded during a task execution in its metadata, merged with any calls an earlier
 * run of the same task execution stored before it was suspended. Runs after the streaming processors, which return only
 * once a streamed response has finished.
 *
 * @author Ivica Cardic
 */
public class ClusterElementCallsTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    private final ClusterElementCallRecorder recorder;

    @SuppressFBWarnings("EI2")
    public ClusterElementCallsTaskExecutionPostOutputProcessor(ClusterElementCallRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void onTaskExecutionFinished(TaskExecution taskExecution) {
        Map<String, ClusterElementCall> drainedCalls = recorder.drain(Validate.notNull(taskExecution.getId(), "id"));

        if (drainedCalls.isEmpty()) {
            return;
        }

        Map<String, ClusterElementCall> calls = ClusterElementCallRecorder.merge(
            getStoredCalls(taskExecution), drainedCalls);

        taskExecution.putMetadata(
            TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS,
            calls.values()
                .stream()
                .sorted(Comparator.comparing(ClusterElementCall::startDate))
                .map(ClusterElementCall::toMap)
                .toList());
    }

    @Override
    public Object process(TaskExecution taskExecution, Object output) {
        return output;
    }

    private static Map<String, ClusterElementCall> getStoredCalls(TaskExecution taskExecution) {
        Map<String, ?> metadata = taskExecution.getMetadata();

        Map<String, ClusterElementCall> storedCalls = new LinkedHashMap<>();

        if (metadata.get(TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS) instanceof List<?> calls) {
            for (Object call : calls) {
                if (call instanceof Map<?, ?> callMap) {
                    @SuppressWarnings("unchecked")
                    ClusterElementCall clusterElementCall = ClusterElementCall.fromMap((Map<String, ?>) callMap);

                    storedCalls.put(clusterElementCall.workflowNodeName(), clusterElementCall);
                }
            }
        }

        return storedCalls;
    }
}
