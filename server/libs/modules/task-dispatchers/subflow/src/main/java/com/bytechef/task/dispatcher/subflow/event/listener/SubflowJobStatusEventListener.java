/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.task.dispatcher.subflow.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition.CallableResponse;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.constant.MetadataConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * an {@link EventListener} which is used for listening to subflow job status events. When a sub-flow completes/fails or
 * stops its parent job and its parent task needs to be informed to resume its execution.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Sep 06, 2018
 * @see com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher
 */
public class SubflowJobStatusEventListener implements ApplicationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SubflowJobStatusEventListener.class);

    private final Evaluator evaluator;
    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI2")
    public SubflowJobStatusEventListener(
        Evaluator evaluator, ApplicationEventPublisher eventPublisher, JobService jobService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {
        this.evaluator = evaluator;

        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent) {
            Job.Status status = jobStatusApplicationEvent.getStatus();
            Job job = jobService.getJob(jobStatusApplicationEvent.getJobId());

            if (job.getParentTaskExecutionId() == null) {
                return; // not a subflow -- nothing to do
            }

            switch (status) {
                case CREATED, STARTED -> {
                }
                case STOPPED -> {
                    TaskExecution subflowTaskExecution = taskExecutionService.getTaskExecution(
                        job.getParentTaskExecutionId());

                    eventPublisher.publishEvent(
                        new StopJobEvent(Objects.requireNonNull(subflowTaskExecution.getJobId())));

                }
                case FAILED -> {
                    TaskExecution erroredTaskExecution = taskExecutionService.getTaskExecution(
                        job.getParentTaskExecutionId());

                    erroredTaskExecution.setError(
                        new ExecutionError(
                            "Subflow job %d (workflow '%s') failed".formatted(job.getId(), job.getWorkflowId()),
                            List.of()));

                    eventPublisher.publishEvent(new TaskExecutionErrorEvent(erroredTaskExecution));
                }
                case COMPLETED -> {
                    TaskExecution completionTaskExecution = taskExecutionService.getTaskExecution(
                        job.getParentTaskExecutionId());

                    Object output = getCallableResponseOutput(job)
                        .orElse(taskFileStorage.readJobOutputs(job.getOutputs()));

                    if (completionTaskExecution.getOutput() == null) {
                        completionTaskExecution.setOutput(
                            taskFileStorage.storeTaskExecutionOutput(
                                Objects.requireNonNull(completionTaskExecution.getJobId()),
                                Objects.requireNonNull(completionTaskExecution.getId()), output));
                    } else {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> context = MapUtils.concat(
                            (Map<String, Object>) taskFileStorage.readTaskExecutionOutput(
                                completionTaskExecution.getOutput()),
                            (Map<String, Object>) output);

                        completionTaskExecution.evaluate(context, evaluator);
                    }

                    eventPublisher.publishEvent(new TaskExecutionCompleteEvent(completionTaskExecution));
                }
                default -> throw new IllegalArgumentException("Unknown status=%s".formatted(status));
            }
        }
    }

    private Optional<Object> getCallableResponseOutput(Job job) {
        try {
            return taskExecutionService.fetchLastJobTaskExecution(Objects.requireNonNull(job.getId()))
                .filter(
                    lastTaskExecution -> {
                        Map<String, ?> metadata = lastTaskExecution.getMetadata();

                        return metadata.containsKey(MetadataConstants.CALLABLE_RESPONSE);
                    })
                .map(lastTaskExecution -> {
                    CallableResponse callableResponse = ConvertUtils.convertValue(
                        taskFileStorage.readTaskExecutionOutput(lastTaskExecution.getOutput()),
                        CallableResponse.class);

                    return callableResponse.output();
                });
        } catch (Exception exception) {
            logger.warn("Failed to extract callable response output from job {}: {}",
                job.getId(), exception.getMessage());

            return Optional.empty();
        }
    }
}
