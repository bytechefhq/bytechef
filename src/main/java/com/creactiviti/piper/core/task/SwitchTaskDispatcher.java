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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.util.Assert;

import com.creactiviti.piper.core.Accessor;
import com.creactiviti.piper.core.MapObject;
import com.creactiviti.piper.core.SwitchTaskCompletionHandler;
import com.creactiviti.piper.core.context.ContextRepository;
import com.creactiviti.piper.core.context.MapContext;
import com.creactiviti.piper.core.messagebroker.MessageBroker;
import com.creactiviti.piper.core.messagebroker.Queues;
import com.creactiviti.piper.core.uuid.UUIDGenerator;

/**
 * @author Arik Cohen
 * @since Jun 3, 2017
 * @see SwitchTaskCompletionHandler
 */
public class SwitchTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

  private final TaskDispatcher taskDispatcher;
  private final TaskEvaluator taskEvaluator = new SpelTaskEvaluator();
  private final TaskExecutionRepository taskExecutionRepo;
  private final ContextRepository contextRepository;
  private final MessageBroker messageBroker;
  
  public SwitchTaskDispatcher (TaskDispatcher aTaskDispatcher, TaskExecutionRepository aTaskRepo, MessageBroker aMessageBroker, ContextRepository aContextRepository) {
    taskDispatcher = aTaskDispatcher;
    taskExecutionRepo = aTaskRepo;
    messageBroker = aMessageBroker;
    contextRepository = aContextRepository;
  }

  @Override
  public void dispatch (TaskExecution aTask) {
    SimpleTaskExecution switchTask = SimpleTaskExecution.createForUpdate(aTask);
    switchTask.setStartTime(new Date ());
    switchTask.setStatus(TaskStatus.STARTED);
    taskExecutionRepo.merge(switchTask);
    Accessor selectedCase = resolveCase(aTask);
    if(selectedCase.containsKey("tasks")) {
      List<MapObject> tasks = selectedCase.getList("tasks", MapObject.class,Collections.emptyList());
      if(tasks.size() > 0) {
        MapObject task = tasks.get(0);
        SimpleTaskExecution execution = SimpleTaskExecution.createFromMap(task);
        execution.setId(UUIDGenerator.generate());
        execution.setStatus(TaskStatus.CREATED);
        execution.setCreateTime(new Date());
        execution.setTaskNumber(1);
        execution.setJobId(switchTask.getJobId());
        execution.setParentId(switchTask.getId());
        execution.setPriority(switchTask.getPriority());
        MapContext context = new MapContext (contextRepository.peek(switchTask.getId()));
        contextRepository.push(execution.getId(), context);
        TaskExecution evaluatedExecution = taskEvaluator.evaluate(execution, context);
        taskExecutionRepo.create(evaluatedExecution);
        taskDispatcher.dispatch(evaluatedExecution);
      }
      else  {
        SimpleTaskExecution completion = SimpleTaskExecution.createForUpdate(aTask);
        completion.setStartTime(new Date());
        completion.setEndTime(new Date());
        completion.setExecutionTime(0);
        messageBroker.send(Queues.COMPLETIONS, completion);
      }
    }
    else {
      SimpleTaskExecution completion = SimpleTaskExecution.createForUpdate(aTask);
      completion.setStartTime(new Date());
      completion.setEndTime(new Date());
      completion.setExecutionTime(0);
      completion.setOutput(selectedCase.get("value"));
      messageBroker.send(Queues.COMPLETIONS, completion);
    }
  }

  private Accessor resolveCase (TaskExecution aSwitch) {
    Object expression = aSwitch.getRequired("expression");
    List<MapObject> cases = aSwitch.getList("cases", MapObject.class);
    Assert.notNull(cases,"you must specify 'cases' in a switch statement");
    for(MapObject oneCase : cases) {
      Object key = oneCase.getRequired("key");
      if(key.equals(expression)) {
        return oneCase;
      }
    }
    return new MapObject( aSwitch.getMap("default",Collections.emptyMap()) );    
  }
  
  @Override
  public TaskDispatcher resolve (Task aTask) {
    if(aTask.getType().equals("switch")) {
      return this;
    }
    return null;
  }
  
}
