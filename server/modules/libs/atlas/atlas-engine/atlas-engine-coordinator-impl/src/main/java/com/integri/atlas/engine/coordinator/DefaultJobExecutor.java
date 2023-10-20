/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator;

import com.integri.atlas.engine.core.context.ContextRepository;
import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.JobStatus;
import com.integri.atlas.engine.coordinator.pipeline.Pipeline;
import com.integri.atlas.engine.coordinator.pipeline.PipelineRepository;
import com.integri.atlas.engine.core.task.PipelineTask;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskDispatcher;
import com.integri.atlas.engine.core.task.TaskEvaluator;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskExecutionRepository;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.uuid.UUIDGenerator;
import java.util.Date;

/**
 *
 * @author Arik Cohen
 * @since Apr 24, 2017
 */
public class DefaultJobExecutor implements JobExecutor {

    private PipelineRepository pipelineRepository;
    private TaskExecutionRepository jobTaskRepository;
    private ContextRepository contextRepository;
    private TaskDispatcher taskDispatcher;
    private TaskEvaluator taskEvaluator;

    @Override
    public void execute(Job aJob) {
        Pipeline pipeline = pipelineRepository.findOne(aJob.getPipelineId());
        if (aJob.getStatus() != JobStatus.STARTED) {
            throw new IllegalStateException("should not be here");
        } else if (hasMoreTasks(aJob, pipeline)) {
            executeNextTask(aJob, pipeline);
        } else {
            throw new IllegalStateException("no tasks to execute!");
        }
    }

    private boolean hasMoreTasks(Job aJob, Pipeline aPipeline) {
        return aJob.getCurrentTask() < aPipeline.getTasks().size();
    }

    private SimpleTaskExecution nextTask(Job aJob, Pipeline aPipeline) {
        PipelineTask task = aPipeline.getTasks().get(aJob.getCurrentTask());
        SimpleTaskExecution mt = new SimpleTaskExecution(task.asMap());
        mt.setCreateTime(new Date());
        mt.setId(UUIDGenerator.generate());
        mt.setStatus(TaskStatus.CREATED);
        mt.setJobId(aJob.getId());
        mt.setPriority(aJob.getPriority());
        if (aPipeline.getRetry() > 0 && mt.getRetry() < 1) {
            mt.setRetry(aPipeline.getRetry());
        }
        return mt;
    }

    private void executeNextTask(Job aJob, Pipeline aPipeline) {
        TaskExecution nextTask = nextTask(aJob, aPipeline);
        MapContext context = new MapContext(contextRepository.peek(aJob.getId()));
        contextRepository.push(nextTask.getId(), context);
        TaskExecution evaluatedTask = taskEvaluator.evaluate(nextTask, context);
        jobTaskRepository.create(evaluatedTask);
        taskDispatcher.dispatch(evaluatedTask);
    }

    public void setPipelineRepository(PipelineRepository aPipelineRepository) {
        pipelineRepository = aPipelineRepository;
    }

    public void setJobTaskRepository(TaskExecutionRepository aJobTaskRepository) {
        jobTaskRepository = aJobTaskRepository;
    }

    public void setContextRepository(ContextRepository aContextRepository) {
        contextRepository = aContextRepository;
    }

    public void setTaskDispatcher(TaskDispatcher aTaskDispatcher) {
        taskDispatcher = aTaskDispatcher;
    }

    public void setTaskEvaluator(TaskEvaluator aTaskEvaluator) {
        taskEvaluator = aTaskEvaluator;
    }
}
