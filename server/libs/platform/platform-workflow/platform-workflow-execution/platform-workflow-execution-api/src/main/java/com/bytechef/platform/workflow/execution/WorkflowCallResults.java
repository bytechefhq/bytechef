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

package com.bytechef.platform.workflow.execution;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.token.ApprovalFormUrls;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the outcome of a workflow run started through a {@code workflow/newWorkflowCall} trigger: the
 * {@code responseToWorkflowCall} output when the workflow responded, otherwise the job outputs; and, for a run paused
 * on a human approval, the hosted form URL and the resume token. Shared by every caller that starts such runs on
 * someone's behalf (MCP servers, apps) so they report results and approvals the same way.
 *
 * @author Ivica Cardic
 */
public final class WorkflowCallResults {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCallResults.class);

    private WorkflowCallResults() {
    }

    public static Optional<Object> fetchCallableResponseOutput(
        Job job, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        try {
            return taskExecutionService.fetchLastJobTaskExecution(Objects.requireNonNull(job.getId()))
                .filter(WorkflowCallResults::isCallableResponse)
                .map(lastTaskExecution -> {
                    ActionDefinition.CallableResponse callableResponse = ConvertUtils.convertValue(
                        taskFileStorage.readTaskExecutionOutput(lastTaskExecution.getOutput()),
                        ActionDefinition.CallableResponse.class);

                    return callableResponse.output();
                });
        } catch (Exception exception) {
            log.warn(
                "Failed to extract callable response output from job {}: {}", job.getId(), exception.getMessage());

            return Optional.empty();
        }
    }

    public static Optional<String> fetchApprovalFormUrl(
        Job job, @Nullable String publicUrl, @Nullable ApprovalTokens approvalTokens) {

        if (!isPendingApproval(job)) {
            return Optional.empty();
        }

        return ApprovalFormUrls.buildFormUrl(publicUrl, getJobResumeId(job), approvalTokens);
    }

    public static Optional<String> fetchResumeToken(Job job, @Nullable ApprovalTokens approvalTokens) {
        if (!isPendingApproval(job)) {
            return Optional.empty();
        }

        return ApprovalFormUrls.buildResumeToken(getJobResumeId(job), approvalTokens);
    }

    /**
     * A {@code STOPPED} run that carries a resume id is paused on a human approval; a {@code STOPPED} run without one
     * was stopped.
     */
    public static boolean isPendingApproval(Job job) {
        return job.getStatus() == Job.Status.STOPPED && getJobResumeId(job) != null;
    }

    public static @Nullable Object readResult(
        Job job, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        Optional<Object> callableResponseOutput = fetchCallableResponseOutput(job, taskExecutionService,
            taskFileStorage);

        if (callableResponseOutput.isPresent()) {
            return callableResponseOutput.get();
        }

        if (job.getOutputs() == null) {
            return null;
        }

        return taskFileStorage.readJobOutputs(job.getOutputs());
    }

    private static @Nullable String getJobResumeId(Job job) {
        Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        return jobResumeId == null ? null : jobResumeId.toString();
    }

    private static boolean isCallableResponse(TaskExecution taskExecution) {
        Map<String, ?> metadata = taskExecution.getMetadata();

        return metadata.containsKey(MetadataConstants.CALLABLE_RESPONSE);
    }
}
