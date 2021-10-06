/*
 * Copyright 2002-2017 the original author or authors.
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
 */
package com.integri.atlas.workflow.core;

import java.util.Date;

import com.integri.atlas.workflow.core.context.ContextRepository;
import com.integri.atlas.workflow.core.context.MapContext;
import com.integri.atlas.workflow.core.job.Job;
import com.integri.atlas.workflow.core.job.JobStatus;
import com.integri.atlas.workflow.core.pipeline.Pipeline;
import com.integri.atlas.workflow.core.pipeline.PipelineRepository;
import com.integri.atlas.workflow.core.task.PipelineTask;
import com.integri.atlas.workflow.core.task.SimpleTaskExecution;
import com.integri.atlas.workflow.core.task.TaskDispatcher;
import com.integri.atlas.workflow.core.task.TaskEvaluator;
import com.integri.atlas.workflow.core.task.TaskExecution;
import com.integri.atlas.workflow.core.task.TaskExecutionRepository;
import com.integri.atlas.workflow.core.task.TaskStatus;
import com.integri.atlas.workflow.core.uuid.UUIDGenerator;

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
  public void execute (Job aJob) {
    Pipeline pipeline = pipelineRepository.findOne(aJob.getPipelineId());
    if(aJob.getStatus() != JobStatus.STARTED) {
      throw new IllegalStateException("should not be here");
    }
    else if(hasMoreTasks(aJob, pipeline)) {
      executeNextTask (aJob, pipeline);
    }
    else {
      throw new IllegalStateException("no tasks to execute!");
    }
  }

  private boolean hasMoreTasks (Job aJob, Pipeline aPipeline) {
    return aJob.getCurrentTask() < aPipeline.getTasks().size();
  }

  private SimpleTaskExecution nextTask(Job aJob, Pipeline aPipeline) {
    PipelineTask task = aPipeline.getTasks().get(aJob.getCurrentTask());
    SimpleTaskExecution mt = new SimpleTaskExecution (task.asMap());
    mt.setCreateTime(new Date());
    mt.setId(UUIDGenerator.generate());
    mt.setStatus(TaskStatus.CREATED);
    mt.setJobId(aJob.getId());
    mt.setPriority(aJob.getPriority());
    if(aPipeline.getRetry() > 0 && mt.getRetry() < 1) {
      mt.setRetry(aPipeline.getRetry());
    }
    return mt;
  }

  private void executeNextTask (Job aJob, Pipeline aPipeline) {
    TaskExecution nextTask = nextTask(aJob, aPipeline);
    MapContext context = new MapContext(contextRepository.peek(aJob.getId()));
    contextRepository.push(nextTask.getId(), context);
    TaskExecution evaluatedTask = taskEvaluator.evaluate(nextTask,context);
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
