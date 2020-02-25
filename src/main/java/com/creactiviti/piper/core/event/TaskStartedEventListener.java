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
 */
package com.creactiviti.piper.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.creactiviti.piper.core.job.Job;
import com.creactiviti.piper.core.job.JobRepository;
import com.creactiviti.piper.core.job.JobStatus;
import com.creactiviti.piper.core.task.CancelTask;
import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.TaskDispatcher;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskExecutionRepository;
import com.creactiviti.piper.core.task.TaskStatus;

/**
 *  
 * @author Arik Cohen
 * @since Apt 9, 2017
 */
public class TaskStartedEventListener implements EventListener {

  private final TaskExecutionRepository taskExecutionRepository;
  private final TaskDispatcher taskDispatcher;
  private final JobRepository jobRepository;
  
  private final Logger logger = LoggerFactory.getLogger(getClass());
  
  public TaskStartedEventListener (TaskExecutionRepository aTaskExecutionRepository, TaskDispatcher aTaskDispatcher, JobRepository aJobRepository) {
    taskExecutionRepository = aTaskExecutionRepository;
    taskDispatcher = aTaskDispatcher;
    jobRepository = aJobRepository;
  }

  @Override
  public void onApplicationEvent (PiperEvent aEvent) {
    if(Events.TASK_STARTED.equals(aEvent.getType())) {
      String taskId = aEvent.getString("taskId");
      TaskExecution task = taskExecutionRepository.findOne(taskId);
      if(task == null) {
        logger.error("Unkown task: {}",taskId);
        return;
      }
      
      Job job = jobRepository.getByTaskId(taskId);
      
      if (task.getStatus() == TaskStatus.CANCELLED || job.getStatus() != JobStatus.STARTED) {
        taskDispatcher.dispatch(new CancelTask(task.getJobId(),task.getId()));
      }
      else {
        SimpleTaskExecution mtask = SimpleTaskExecution.of(task);
        if(mtask.getStartTime()==null && mtask.getStatus() != TaskStatus.STARTED) {
          mtask.setStartTime(aEvent.getCreateTime());
          mtask.setStatus(TaskStatus.STARTED);
          taskExecutionRepository.merge(mtask);
        }
        if(mtask.getParentId()!=null) {
          PiperEvent pevent = PiperEvent.of(Events.TASK_STARTED,"taskId",mtask.getParentId());
          onApplicationEvent(pevent);
        }
      }
    }
  }

}
