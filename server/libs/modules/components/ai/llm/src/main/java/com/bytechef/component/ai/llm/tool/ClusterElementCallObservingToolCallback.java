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

package com.bytechef.component.ai.llm.tool;

import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRef;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Records every call of a cluster element's tool callback, with its input, output and tool name, in the
 * {@link ClusterElementCallRecorder}, and binds the call while it runs so nested calls name it as their parent. A call
 * that throws is failed, a suspended call is marked suspended, and any other call completed, whatever its result says.
 *
 * @author Ivica Cardic
 */
public class ClusterElementCallObservingToolCallback implements ToolCallback {

    private final String componentName;
    private final ToolCallback delegate;
    private final long jobId;
    private final ClusterElementCallRecorder recorder;
    private final Supplier<@Nullable Suspend> suspendSupplier;
    private final long taskExecutionId;
    private final String workflowNodeName;

    @SuppressFBWarnings("EI2")
    public ClusterElementCallObservingToolCallback(
        ToolCallback delegate, String workflowNodeName, String componentName, long jobId, long taskExecutionId,
        ClusterElementCallRecorder recorder, Supplier<@Nullable Suspend> suspendSupplier) {

        this.componentName = componentName;
        this.delegate = delegate;
        this.jobId = jobId;
        this.recorder = recorder;
        this.suspendSupplier = suspendSupplier;
        this.taskExecutionId = taskExecutionId;
        this.workflowNodeName = workflowNodeName;
    }

    public static List<ToolCallback> wrap(
        List<ToolCallback> toolCallbacks, String workflowNodeName, String componentName,
        Context context) {
        if (!(context instanceof ActionContextAware actionContextAware)) {
            return toolCallbacks;
        }

        Long jobId = actionContextAware.getJobId();
        Long taskExecutionId = actionContextAware.getTaskExecutionId();

        if (jobId == null || taskExecutionId == null) {
            return toolCallbacks;
        }

        ClusterElementCallRecorder recorder = ClusterElementCallRecorder.getInstance();

        return toolCallbacks.stream()
            .map(toolCallback -> (ToolCallback) new ClusterElementCallObservingToolCallback(
                toolCallback, workflowNodeName, componentName, jobId, taskExecutionId, recorder,
                actionContextAware::getSuspend))
            .toList();
    }

    @Override
    public String call(String toolInput) {
        return observe(toolInput, () -> delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        return observe(toolInput, () -> delegate.call(toolInput, toolContext));
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    private String observe(String toolInput, Supplier<String> call) {
        ClusterElementCallRef parent = ClusterElementCallScope.current();
        ClusterElementCallRef ref = recorder.callStarted(
            jobId, taskExecutionId, workflowNodeName, componentName, delegate.getToolDefinition()
                .name(),
            parent == null ? null : parent.id(), toolInput);

        String result;

        try {
            result = ClusterElementCallScope.callWith(ref, call);
        } catch (RuntimeException exception) {
            if (!ToolSuspension.isToolSuspensionFailure(exception)) {
                String message = exception.getMessage();

                recorder.callFailed(
                    jobId, taskExecutionId, ref,
                    message == null ? exception.getClass()
                        .getSimpleName() : message);
            }

            throw exception;
        }

        if (ToolSuspension.isSuspendedToolResult(result, suspendSupplier.get())) {
            recorder.callSuspended(jobId, taskExecutionId, ref);
        } else {
            recorder.callCompleted(jobId, taskExecutionId, ref, result);
        }

        return result;
    }
}
