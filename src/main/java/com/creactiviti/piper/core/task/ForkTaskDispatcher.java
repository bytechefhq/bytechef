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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.creactiviti.piper.core.ForkTaskCompletionHandler;
import com.creactiviti.piper.core.context.ContextRepository;
import com.creactiviti.piper.core.context.MapContext;
import com.creactiviti.piper.core.messenger.MessageBroker;
import com.creactiviti.piper.core.messenger.Queues;
import com.creactiviti.piper.core.uuid.UUIDGenerator;

/**
 * Implements a Fork/Join construct. 
 * 
 * Fork/Join tasks are expected to have a "branches" 
 * property which contains a list of task list.
 * 
 * Each branch executes in isolation, in parallel 
 * to the other branches in the fork and has its 
 * own context namespace.
 * 
 * <pre>
 *   - type: fork
 *     branches: 
 *       - - name: randomNumber
 *           label: Generate a random number
 *           type: randomInt
 *           startInclusive: 0
 *           endInclusive: 5000
 *         
 *         - type: sleep
 *           millis: ${randomNumber}
 *         
 *       - - name: randomNumber
 *           label: Generate a random number
 *           type: randomInt
 *           startInclusive: 0
 *           endInclusive: 5000
 *         
 *         - type: sleep
 *           millis: ${randomNumber}
 * </pre>
 * 
 * @author Arik Cohen
 * @since May 11, 2017
 * @see ForkTaskCompletionHandler
 */
public class ForkTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

  private TaskDispatcher taskDispatcher;
  private TaskEvaluator taskEvaluator = new SpelTaskEvaluator();
  private TaskExecutionRepository taskExecutionRepo;
  private MessageBroker messageBroker;
  private ContextRepository contextRepository;
  private CounterRepository counterRepository;

  @Override
  public void dispatch (TaskExecution aTask) {
    List<List> branches = aTask.getList("branches", List.class);
    Assert.notNull(branches,"'branches' property can't be null");
    SimpleTaskExecution forkTask = SimpleTaskExecution.createForUpdate(aTask);
    forkTask.setStartTime(new Date ());
    forkTask.setStatus(TaskStatus.STARTED);
    taskExecutionRepo.merge(forkTask);
    if(branches.size() > 0) {
      counterRepository.set(aTask.getId(), branches.size());
      for(int i=0; i<branches.size(); i++) {
        List branch = branches.get(i);
        Assert.isTrue(branch.size()>0, "branch " + i + " does not contain any tasks");
        Map<String,Object> task = (Map<String, Object>) branch.get(0);
        SimpleTaskExecution execution = SimpleTaskExecution.createFromMap(task);
        execution.setId(UUIDGenerator.generate());
        execution.setStatus(TaskStatus.CREATED);
        execution.setCreateTime(new Date());
        execution.set("branch", i);
        execution.setTaskNumber(1);
        execution.setJobId(aTask.getJobId());
        execution.setParentId(aTask.getId());
        execution.setPriority(aTask.getPriority());
        MapContext context = new MapContext (contextRepository.peek(aTask.getId()));
        contextRepository.push(aTask.getId()+"/"+i, context);
        contextRepository.push(execution.getId(), context);
        TaskExecution evaluatedExecution = taskEvaluator.evaluate(execution, context);
        taskExecutionRepo.create(evaluatedExecution);
        taskDispatcher.dispatch(evaluatedExecution);
      }
    }
    else {
      SimpleTaskExecution completion = SimpleTaskExecution.createForUpdate(aTask);
      completion.setEndTime(new Date());
      messageBroker.send(Queues.COMPLETIONS, completion);
    }
  }

  @Override
  public TaskDispatcher resolve (Task aTask) {
    if(aTask.getType().equals("fork")) {
      return this;
    }
    return null;
  }
  
  public void setContextRepository(ContextRepository aContextRepository) {
    contextRepository = aContextRepository;
  }
  
  public void setCounterRepository(CounterRepository aCounterRepository) {
    counterRepository = aCounterRepository;
  }
  
  public void setMessageBroker(MessageBroker aMessageBroker) {
    messageBroker = aMessageBroker;
  }
  
  public void setTaskDispatcher(TaskDispatcher aTaskDispatcher) {
    taskDispatcher = aTaskDispatcher;
  }
  
  public void setTaskEvaluator(TaskEvaluator aTaskEvaluator) {
    taskEvaluator = aTaskEvaluator;
  }

  public void setTaskExecutionRepo(TaskExecutionRepository aTaskExecutionRepo) {
    taskExecutionRepo = aTaskExecutionRepo;
  }

}
