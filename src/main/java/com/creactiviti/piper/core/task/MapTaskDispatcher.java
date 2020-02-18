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
 *//* 
 * Copyright (C) Creactiviti LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Arik Cohen <arik@creactiviti.com>, June 2017
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
 * @author Arik Cohen
 * @since Jun 4, 2017
 */
public class MapTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

  private final TaskDispatcher taskDispatcher;
  private final TaskEvaluator taskEvaluator;
  private final TaskExecutionRepository taskExecutionRepo;
  private final MessageBroker messageBroker;
  private final ContextRepository contextRepository;
  private final CounterRepository counterRepository;

  private MapTaskDispatcher (Builder aBuilder) {
    taskDispatcher = aBuilder.taskDispatcher;
    taskExecutionRepo = aBuilder.taskExecutionRepo;
    messageBroker = aBuilder.messageBroker;
    contextRepository = aBuilder.contextRepository;
    counterRepository = aBuilder.counterRepository;
    taskEvaluator = aBuilder.taskEvaluator;
  }

  @Override
  public void dispatch (TaskExecution aTask) {
    List<Object> list = aTask.getList("list", Object.class);
    Assert.notNull(list,"'list' property can't be null");
    Map<String, Object> iteratee = aTask.getMap("iteratee");
    Assert.notNull(list,"'iteratee' property can't be null");
    
    SimpleTaskExecution parentMapTask = SimpleTaskExecution.createForUpdate(aTask);
    parentMapTask.setStartTime(new Date ());
    parentMapTask.setStatus(TaskStatus.STARTED);
    taskExecutionRepo.merge(parentMapTask);
    
    if(list.size() > 0) {
      counterRepository.set(aTask.getId(), list.size());
      for(int i=0; i<list.size(); i++) {
        Object item = list.get(i);
        SimpleTaskExecution mapTask = SimpleTaskExecution.createFromMap(iteratee);
        mapTask.setId(UUIDGenerator.generate());
        mapTask.setParentId(aTask.getId());
        mapTask.setStatus(TaskStatus.CREATED);
        mapTask.setJobId(aTask.getJobId());
        mapTask.setCreateTime(new Date());
        mapTask.setPriority(aTask.getPriority());
        mapTask.setTaskNumber(i+1);
        MapContext context = new MapContext (contextRepository.peek(aTask.getId()));
        context.set(aTask.getString("itemVar","item"), item);
        context.set(aTask.getString("itemIndex","itemIndex"), i);
        contextRepository.push(mapTask.getId(), context);
        TaskExecution evaluatedEachTask = taskEvaluator.evaluate(mapTask, context);
        taskExecutionRepo.create(evaluatedEachTask);
        taskDispatcher.dispatch(evaluatedEachTask);
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
    if(aTask.getType().equals(DSL.MAP)) {
      return this;
    }
    return null;
  }
  
  public static Builder builder () {
    return new Builder();
  }
  
  public static class Builder { 
    
    private TaskDispatcher taskDispatcher;
    private TaskEvaluator taskEvaluator = new SpelTaskEvaluator();
    private TaskExecutionRepository taskExecutionRepo;
    private MessageBroker messageBroker;
    private ContextRepository contextRepository;
    private CounterRepository counterRepository;
    
    public Builder taskDispatcher(TaskDispatcher aTaskDispatcher) {
      taskDispatcher = aTaskDispatcher;
      return this;
    }
    
    public Builder taskEvaluator(TaskEvaluator aTaskEvaluator) {
      taskEvaluator = aTaskEvaluator;
      return this;
    }
    
    public Builder taskExecutionRepository(TaskExecutionRepository aTaskExecutionRepository) {
      taskExecutionRepo = aTaskExecutionRepository;
      return this;
    }
    
    public Builder messageBroker (MessageBroker aMessageBroker) {
      messageBroker = aMessageBroker;
      return this;
    }
    
    public Builder contextRepository(ContextRepository aContextRepository) {
      contextRepository = aContextRepository;
      return this;
    }
    
    public Builder counterRepository(CounterRepository aCounterRepository) {
      counterRepository = aCounterRepository;
      return this;
    }
    
    public MapTaskDispatcher build () {
      return new MapTaskDispatcher(this);
    }
    
  }

}
