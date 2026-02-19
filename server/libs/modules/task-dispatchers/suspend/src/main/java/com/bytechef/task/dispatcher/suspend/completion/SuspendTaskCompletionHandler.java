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

package com.bytechef.task.dispatcher.suspend.completion;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.domain.TaskExecution.Status;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class SuspendTaskCompletionHandler implements TaskCompletionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SuspendTaskCompletionHandler.class);

    private final ContextService contextService;
    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final TaskStateService taskStateService;

    @SuppressFBWarnings("EI")
    public SuspendTaskCompletionHandler(
        ContextService contextService, ApplicationEventPublisher eventPublisher, JobService jobService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, TaskStateService taskStateService) {

        this.contextService = contextService;
        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.taskStateService = taskStateService;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        return taskExecution.getParentId() == null &&
            taskExecution.getMetadata()
                .containsKey(MetadataConstants.JOB_RESUME_ID);
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        taskExecution.setHandled(true);

        taskExecution.setStatus(Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        Job job = jobService.getTaskExecutionJob(Validate.notNull(taskExecution.getId(), "id"));

        if (job == null) {
            taskExecution.setStatus(Status.FAILED);

            taskExecutionService.update(taskExecution);

            throw new IllegalStateException(
                "Job not found for task execution id=" + taskExecution.getId() +
                    ", jobId=" + taskExecution.getJobId());
        }

        long jobId = Objects.requireNonNull(job.getId());

        String name = taskExecution.getName();
        FileEntry output = taskExecution.getOutput();

        Map<String, Object> newContext = new HashMap<>(
            taskFileStorage.readContextValue(
                contextService.peek(jobId, Context.Classname.JOB)));

        if (name != null) {
            if (output == null) {
                newContext.put(name, null);
            } else {
                newContext.put(name, taskFileStorage.readTaskExecutionOutput(output));
            }
        }

        contextService.push(
            jobId, Context.Classname.JOB,
            taskFileStorage.storeContextValue(jobId, Context.Classname.JOB, newContext));

        String jobResumeIdString = MapUtils.getString(taskExecution.getMetadata(), MetadataConstants.JOB_RESUME_ID);

        JobResumeId jobResumeId = JobResumeId.parse(jobResumeIdString);

        Suspend suspend = MapUtils.get(taskExecution.getMetadata(), MetadataConstants.SUSPEND, Suspend.class);

        taskStateService.save(jobResumeId, suspend);

        Map<String, Object> jobMetadata = new HashMap<>(job.getMetadata());

        jobMetadata.put(MetadataConstants.JOB_RESUME_ID, jobResumeIdString);

        job.setMetadata(jobMetadata);

        jobService.update(job);

        job = jobService.setStatusToStopped(jobId);

        eventPublisher.publishEvent(new JobStatusApplicationEvent(jobId, job.getStatus()));

        if (logger.isDebugEnabled()) {
            logger.debug("Suspend completion handler completed for job id={}", jobId);
        }
    }
}
