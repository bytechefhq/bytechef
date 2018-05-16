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
package com.creactiviti.piper.core.task;

import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.TaskCompletionHandler;

/**
 * <p>A {@link TaskCompletionHandler} implementation which handles completions 
 * of parallel construct tasks.</p>
 * 
 * <p>This handler keeps track of how many tasks were completed so far and 
 * when all parallel tasks completed for a given task it will then complete
 * the overall <code>parallel</code> task.</p> 
 * 
 * @author Arik Cohen
 * @since May 12, 2017
 * @see ParallelTaskDispatcher
 */
public class ParallelTaskCompletionHandler implements TaskCompletionHandler {

  private TaskExecutionRepository taskExecutionRepo;
  private TaskCompletionHandler taskCompletionHandler;
  private CounterRepository counterRepository;
  
  @Override
  public void handle (TaskExecution aTaskExecution) {
    SimpleTaskExecution mtask = SimpleTaskExecution.createForUpdate(aTaskExecution);
    mtask.setStatus(TaskStatus.COMPLETED);
    taskExecutionRepo.merge(mtask);
    long tasksLeft = counterRepository.decrement(aTaskExecution.getParentId());
    if(tasksLeft == 0) {
      taskCompletionHandler.handle(taskExecutionRepo.findOne(aTaskExecution.getParentId()));
      counterRepository.delete(aTaskExecution.getParentId());
    }
  }

  @Override
  public boolean canHandle (TaskExecution aTaskExecution) {
    String parentId = aTaskExecution.getParentId();
    if(parentId!=null) {
      TaskExecution parentExecution = taskExecutionRepo.findOne(parentId);
      return parentExecution.getType().equals(DSL.PARALLEL);
    }
    return false;
  }
  
  public void setTaskExecutionRepository(TaskExecutionRepository aTaskExecutionRepo) {
    taskExecutionRepo = aTaskExecutionRepo;
  }
  
  public void setTaskCompletionHandler(TaskCompletionHandler aTaskCompletionHandler) {
    taskCompletionHandler = aTaskCompletionHandler;
  }
  
  public void setCounterRepository(CounterRepository aCounterRepository) {
    counterRepository = aCounterRepository;
  }

}
