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
package com.creactiviti.piper.core;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.creactiviti.piper.core.task.CounterRepository;
import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskExecutionRepository;
import com.creactiviti.piper.core.task.TaskStatus;

/**
 * @author Arik Cohen
 * @since June 4, 2017
 */
public class MapTaskCompletionHandler implements TaskCompletionHandler {

  private final TaskExecutionRepository taskExecutionRepo;
  private final TaskCompletionHandler taskCompletionHandler;
  private final CounterRepository counterRepository;
  
  public MapTaskCompletionHandler(TaskExecutionRepository aTaskExecutionRepo, TaskCompletionHandler aTaskCompletionHandler, CounterRepository aCounterRepository) {
    taskExecutionRepo = aTaskExecutionRepo;
    taskCompletionHandler = aTaskCompletionHandler;
    counterRepository = aCounterRepository;
  }
  
  @Override
  public void handle (TaskExecution aTaskExecution) {
    SimpleTaskExecution mtask = SimpleTaskExecution.createForUpdate(aTaskExecution);
    mtask.setStatus(TaskStatus.COMPLETED);
    taskExecutionRepo.merge(mtask);
    long subtasksLeft = counterRepository.decrement(aTaskExecution.getParentId());
    if(subtasksLeft == 0) {
      List<TaskExecution> children = taskExecutionRepo.findByParentId(aTaskExecution.getParentId());
      SimpleTaskExecution parentExecution = SimpleTaskExecution.createForUpdate(taskExecutionRepo.findOne(aTaskExecution.getParentId()));
      parentExecution.setEndTime(new Date ());
      parentExecution.setExecutionTime(parentExecution.getEndTime().getTime()-parentExecution.getStartTime().getTime());
      parentExecution.setOutput(children.stream().map(c->c.getOutput()).collect(Collectors.toList()));
      taskCompletionHandler.handle(parentExecution);
      counterRepository.delete(aTaskExecution.getParentId());
    }
  }

  @Override
  public boolean canHandle (TaskExecution aTaskExecution) {
    String parentId = aTaskExecution.getParentId();
    if(parentId!=null) {
      TaskExecution parentExecution = taskExecutionRepo.findOne(parentId);
      return parentExecution.getType().equals(DSL.MAP);
    }
    return false;
  }

}
