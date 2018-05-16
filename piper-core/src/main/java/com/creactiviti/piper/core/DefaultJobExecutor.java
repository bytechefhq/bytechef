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
package com.creactiviti.piper.core;

import com.creactiviti.piper.core.context.ContextRepository;
import com.creactiviti.piper.core.context.MapContext;
import com.creactiviti.piper.core.job.Job;
import com.creactiviti.piper.core.job.JobStatus;
import com.creactiviti.piper.core.pipeline.Pipeline;
import com.creactiviti.piper.core.pipeline.PipelineRepository;
import com.creactiviti.piper.core.task.PipelineTask;
import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.SpelTaskEvaluator;
import com.creactiviti.piper.core.task.TaskDispatcher;
import com.creactiviti.piper.core.task.TaskEvaluator;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskExecutionRepository;

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
  private TaskEvaluator taskEvaluator = new SpelTaskEvaluator();
  
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

  private TaskExecution nextTask(Job aJob, Pipeline aPipeline) {
    PipelineTask task = aPipeline.getTasks().get(aJob.getCurrentTask());
    SimpleTaskExecution mt = SimpleTaskExecution.createFrom (task);
    mt.setJobId(aJob.getId());
    mt.setPriority(aJob.getPriority());
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

}
