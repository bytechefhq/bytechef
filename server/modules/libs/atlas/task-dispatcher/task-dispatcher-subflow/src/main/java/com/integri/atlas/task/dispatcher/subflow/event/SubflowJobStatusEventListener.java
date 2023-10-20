/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.task.dispatcher.subflow.event;

import com.integri.atlas.engine.context.MapContext;
import com.integri.atlas.engine.coordinator.Coordinator;
import com.integri.atlas.engine.coordinator.event.EventListener;
import com.integri.atlas.engine.error.ErrorObject;
import com.integri.atlas.engine.event.Events;
import com.integri.atlas.engine.event.WorkflowEvent;
import com.integri.atlas.engine.job.Job;
import com.integri.atlas.engine.job.JobStatus;
import com.integri.atlas.engine.job.repository.JobRepository;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.task.execution.evaluator.TaskEvaluator;
import com.integri.atlas.engine.task.execution.repository.TaskExecutionRepository;
import com.integri.atlas.task.dispatcher.subflow.SubflowTaskDispatcher;
import java.util.Objects;

/**
 * an {@link EventListener} which is used for listening to subflow
 * job status events. When a sub-flow completes/fails or stops its
 * parent job and its parent task needs to be informed so as to
 * resume its execution.
 *
 * @author Arik Cohen
 * @since Sep 06, 2018
 * @see SubflowTaskDispatcher
 */
public class SubflowJobStatusEventListener implements EventListener {

    private final JobRepository jobRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final Coordinator coordinator;
    private final TaskEvaluator taskEvaluator;

    public SubflowJobStatusEventListener(
        JobRepository aJobRepository,
        TaskExecutionRepository aTaskExecutionRepository,
        Coordinator aCoordinator,
        TaskEvaluator aTaskEvaluator
    ) {
        jobRepository = Objects.requireNonNull(aJobRepository);
        taskExecutionRepository = Objects.requireNonNull(aTaskExecutionRepository);
        coordinator = Objects.requireNonNull(aCoordinator);
        taskEvaluator = Objects.requireNonNull(aTaskEvaluator);
    }

    @Override
    public void onApplicationEvent(WorkflowEvent aEvent) {
        if (aEvent.getType().equals(Events.JOB_STATUS)) {
            String jobId = aEvent.getRequiredString("jobId");
            JobStatus status = JobStatus.valueOf(aEvent.getRequiredString("status"));
            Job job = jobRepository.getById(jobId);

            if (job.getParentTaskExecutionId() == null) {
                return; // not a subflow -- nothing to do
            }

            switch (status) {
                case CREATED:
                case STARTED:
                    break;
                case STOPPED:
                    {
                        TaskExecution subflowTask = taskExecutionRepository.findOne(job.getParentTaskExecutionId());
                        coordinator.stop(subflowTask.getJobId());
                        break;
                    }
                case FAILED:
                    {
                        SimpleTaskExecution errorable = SimpleTaskExecution.of(
                            taskExecutionRepository.findOne(job.getParentTaskExecutionId())
                        );
                        errorable.setError(new ErrorObject("An error occured with subflow", new String[0]));
                        coordinator.handleError(errorable);
                        break;
                    }
                case COMPLETED:
                    {
                        SimpleTaskExecution completion = SimpleTaskExecution.of(
                            taskExecutionRepository.findOne(job.getParentTaskExecutionId())
                        );
                        Object output = job.getOutputs();
                        if (completion.getOutput() != null) {
                            TaskExecution evaluated = taskEvaluator.evaluate(
                                completion,
                                new MapContext("execution", new MapContext("output", output))
                            );
                            completion = SimpleTaskExecution.of(evaluated);
                        } else {
                            completion.setOutput(output);
                        }
                        coordinator.complete(completion);
                        break;
                    }
                default:
                    throw new IllegalStateException("Unnown status: " + status);
            }
        }
    }
}
