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

import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.context.ContextRepository;
import com.creactiviti.piper.core.context.MapContext;
import com.creactiviti.piper.core.messagebroker.MessageBroker;
import com.creactiviti.piper.core.messagebroker.Queues;
import com.creactiviti.piper.core.uuid.UUIDGenerator;

/**
 * A {@link TaskDispatcher} implementation which implements a parallel
 * for-each construct. The dispatcher works by executing 
 * the <code>iteratee</code> function on each item on the <code>list</code>.
 * 
 * @author Arik Cohen
 * @since Apr 25, 2017
 */
public class EachTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

  private final TaskDispatcher taskDispatcher;
  private final TaskEvaluator taskEvaluator = new SpelTaskEvaluator();
  private final TaskExecutionRepository taskExecutionRepo;
  private final MessageBroker messageBroker;
  private final ContextRepository contextRepository;
  private final CounterRepository counterRepository;

  public EachTaskDispatcher (TaskDispatcher aTaskDispatcher, TaskExecutionRepository aTaskExecutionRepo, MessageBroker aMessageBroker, ContextRepository aContextRepository, CounterRepository aCounterRepository) {
    taskDispatcher = aTaskDispatcher;
    taskExecutionRepo = aTaskExecutionRepo;
    messageBroker = aMessageBroker;
    contextRepository = aContextRepository;
    counterRepository = aCounterRepository;
  }

  @Override
  public void dispatch (TaskExecution aTask) {
    List<Object> list = aTask.getList("list", Object.class);
    Assert.notNull(list,"'list' property can't be null");
    Map<String, Object> iteratee = aTask.getMap("iteratee");
    Assert.notNull(list,"'iteratee' property can't be null");
    
    SimpleTaskExecution parentEachTask = SimpleTaskExecution.of(aTask);
    parentEachTask.setStartTime(new Date ());
    parentEachTask.setStatus(TaskStatus.STARTED);
    taskExecutionRepo.merge(parentEachTask);
    
    if(list.size() > 0) {
      counterRepository.set(aTask.getId(), list.size());
      for(int i=0; i<list.size(); i++) {
        Object item = list.get(i);
        SimpleTaskExecution eachTask = SimpleTaskExecution.of(iteratee);
        eachTask.setId(UUIDGenerator.generate());
        eachTask.setParentId(aTask.getId());
        eachTask.setStatus(TaskStatus.CREATED);
        eachTask.setJobId(aTask.getJobId());
        eachTask.setCreateTime(new Date());
        eachTask.setPriority(aTask.getPriority());
        eachTask.setTaskNumber(i+1);
        MapContext context = new MapContext (contextRepository.peek(aTask.getId()));
        context.set(aTask.getString("itemVar","item"), item);
        context.set(aTask.getString("itemIndex","itemIndex"), i);
        contextRepository.push(eachTask.getId(), context);
        TaskExecution evaluatedEachTask = taskEvaluator.evaluate(eachTask, context);
        taskExecutionRepo.create(evaluatedEachTask);
        taskDispatcher.dispatch(evaluatedEachTask);
      }
    }
    else {
      SimpleTaskExecution completion = SimpleTaskExecution.of(aTask);
      completion.setEndTime(new Date());
      messageBroker.send(Queues.COMPLETIONS, completion);
    }
  }

  @Override
  public TaskDispatcher resolve (Task aTask) {
    if(aTask.getType().equals(DSL.EACH)) {
      return this;
    }
    return null;
  }

}
