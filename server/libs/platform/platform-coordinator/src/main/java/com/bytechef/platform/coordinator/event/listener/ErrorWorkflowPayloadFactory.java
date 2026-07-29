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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.coordinator.monitor.OrphanedJobRecoveryMonitor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Builds the Error Trigger payload. Deliberately carries no task inputs or outputs: the job id is the handle, and a
 * handler needing more fetches it through the existing APIs, which keeps copying run data into a second execution
 * history an explicit choice.
 *
 * <p>
 * The keys produced here must match {@code WorkflowNewWorkflowErrorTrigger}'s output schema exactly -- any divergence
 * means the workflow editor shows data pills that do not exist at runtime.
 *
 * @author Ivica Cardic
 */
public class ErrorWorkflowPayloadFactory {

    private static final String DEFAULT_ERROR_MESSAGE = "Workflow run failed";

    public record ErrorWorkflowContext(
        long projectId, long projectWorkflowId, String workflowId, String label, String environment) {
    }

    private final String publicUrl;

    public ErrorWorkflowPayloadFactory(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    private static int getAutoRecoveryAttempts(Job job) {
        Object attempts = job.getMetadata(OrphanedJobRecoveryMonitor.AUTO_RECOVERY_ATTEMPTS);

        if (attempts instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }

    public Map<String, Object> build(
        Job job, @Nullable TaskExecution lastTaskExecution, ErrorWorkflowContext context) {

        Map<String, Object> error = new LinkedHashMap<>();

        ExecutionError executionError = job.getError();

        if (executionError == null && lastTaskExecution != null) {
            executionError = lastTaskExecution.getError();
        }

        String message = executionError == null ? null : executionError.getMessage();

        error.put("message", message == null ? DEFAULT_ERROR_MESSAGE : message);
        error.put("stackTrace", executionError == null ? null : String.join("\n", executionError.getStackTrace()));

        Map<String, Object> execution = new LinkedHashMap<>();

        execution.put("jobId", String.valueOf(job.getId()));
        execution.put(
            "url", publicUrl == null || publicUrl.isBlank() ? null : publicUrl + "/automation/executions/"
                + job.getId());
        execution.put("error", error);
        execution.put("lastTaskExecuted", lastTaskExecution == null ? null : lastTaskExecution.getName());
        // autoRecoveryAttempts, not n8n's retryOf: ByteChef resumes a job IN PLACE
        // (JobService.resumeToStatusStarted reuses the same id), so there is no prior job to point at. What a handler
        // can actually use is whether this run has already been auto-recovered from a crash, and how often.
        execution.put("autoRecoveryAttempts", getAutoRecoveryAttempts(job));

        Map<String, Object> workflow = new LinkedHashMap<>();

        workflow.put("projectId", String.valueOf(context.projectId()));
        workflow.put("projectWorkflowId", String.valueOf(context.projectWorkflowId()));
        workflow.put("workflowId", context.workflowId());
        workflow.put("label", context.label());

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("execution", execution);
        payload.put("workflow", workflow);
        payload.put("environment", context.environment());

        return payload;
    }
}
