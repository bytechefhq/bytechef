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

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.creactiviti.piper.core.context.MapContext;
import com.creactiviti.piper.core.error.ErrorObject;
import com.creactiviti.piper.core.event.EventPublisher;
import com.creactiviti.piper.core.event.Events;
import com.creactiviti.piper.core.event.PiperEvent;
import com.creactiviti.piper.core.messagebroker.MessageBroker;
import com.creactiviti.piper.core.messagebroker.Queues;
import com.creactiviti.piper.core.task.ControlTask;
import com.creactiviti.piper.core.task.PipelineTask;
import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.SpelTaskEvaluator;
import com.creactiviti.piper.core.task.TaskEvaluator;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;
import com.creactiviti.piper.core.task.TaskHandlerResolver;
import com.creactiviti.piper.core.task.TaskStatus;
import com.creactiviti.piper.core.task.TempDir;
import com.creactiviti.piper.core.uuid.UUIDGenerator;

/**
 * <p>The class responsible for executing tasks spawned by the {@link Coordinator}.</p>
 * 
 * <p>Worker threads typically execute on a different
 * process than the {@link Coordinator} process and most likely
 * on a seperate node altogether.</p>
 * 
 * <p>Communication between the two is decoupled through the 
 * {@link MessageBroker} interface.</p>
 * 
 * @author Arik Cohen
 * @since Jun 12, 2016
 *
 */
public class Worker {

  private final ExecutorService executors = Executors.newCachedThreadPool();
  private final Map<String, TaskExecutionFuture<?>> taskExecutions = new ConcurrentHashMap<>();
  private final TaskEvaluator taskEvaluator = SpelTaskEvaluator.builder()
                                                               .methodExecutor("tempDir", new TempDir())
                                                               .build();
  
  private final Logger logger = LoggerFactory.getLogger(getClass());
  
  private TaskHandlerResolver taskHandlerResolver;
  private MessageBroker messageBroker;  
  private EventPublisher eventPublisher;
  
  private static final long DEFAULT_TIME_OUT = 24 * 60 * 60 * 1000; // 24 hours
  
  /**
   * Handle the execution of a {@link TaskExecution}. Implementors
   * are expected to execute the task asynchronously. 
   * 
   * @param aTask
   *          The task to execute.
   */
  public void handle (TaskExecution aTask) {
    Future<?> future = executors.submit(() -> {
      try {
        eventPublisher.publishEvent(PiperEvent.of(Events.TASK_STARTED,"taskId",aTask.getId(),"jobId",aTask.getJobId()));
        SimpleTaskExecution completion = doExecuteTask(aTask);
        messageBroker.send(Queues.COMPLETIONS, completion);
      }
      catch (InterruptedException e) {
        // ignore
      }
      catch (Exception e) {
        TaskExecutionFuture<?> myFuture = taskExecutions.get(aTask.getId());
        if(!myFuture.isCancelled()) {
          handleException(aTask, e);
        }
      }
      finally {
        taskExecutions.remove(aTask.getId());
      }
    });
    
    taskExecutions.put(aTask.getId(), new TaskExecutionFuture<>(aTask,future));
    
    try {
      future.get(calculateTimeout(aTask), TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      handleException(aTask, e);
    }
    catch (CancellationException e) {
      logger.debug("Cancelled task: {}", aTask.getId());
    }
    
  }
  
  /**
   * Handle control tasks. Control tasks are used by the Coordinator
   * to control Worker instances. For example to stop an ongoing task
   * or to adjust something on a worker outside the context of a job.
   */
  public void handle (ControlTask aControlTask) {
    logger.debug("received control task: {}", aControlTask);
    if(ControlTask.TYPE_CANCEL.equals(aControlTask.getType())) {
      String jobId = aControlTask.getRequiredString("jobId");
      for(TaskExecutionFuture<?> tef : taskExecutions.values()) {
        if(tef.taskExecution.getJobId().equals(jobId)) {
          logger.info("Cancelling task {}->{}",jobId,tef.taskExecution.getId());
          tef.cancel(true);
        }
      }
    }
  }
  
  Map<String, TaskExecutionFuture<?>> getTaskExecutions() {
    return Collections.unmodifiableMap(taskExecutions);
  }
  
  private SimpleTaskExecution doExecuteTask (TaskExecution aTask) throws Exception {
    MapContext context = new MapContext();
    try {
      long startTime = System.currentTimeMillis();
      logger.debug("Recived task: {}",aTask);
      
      // pre tasks
      executeSubTasks(aTask, aTask.getPre(), context);
      
      TaskExecution evaluatedTask = taskEvaluator.evaluate(aTask, context);
      
      TaskHandler<?> taskHandler = taskHandlerResolver.resolve(evaluatedTask);
      Object output = taskHandler.handle(evaluatedTask);
      SimpleTaskExecution completion = SimpleTaskExecution.of(evaluatedTask);
      if(output!=null) {
        if(completion.getOutput() != null) {
          TaskExecution evaluated = taskEvaluator.evaluate(completion, new MapContext ("execution", new MapContext("output", output)));
          completion = SimpleTaskExecution.of(evaluated);
        }
        else {
          completion.setOutput(output);
        }
      }
      completion.setStatus(TaskStatus.COMPLETED);
      completion.setProgress(100);
      completion.setEndTime(new Date());
      completion.setExecutionTime(System.currentTimeMillis()-startTime);
      
      // post tasks
      executeSubTasks(aTask, aTask.getPost(), context);
      
      return completion;
    }
    finally {
      // finalize tasks
      executeSubTasks(aTask, aTask.getFinalize(), context);
    }
  }
  
  private void executeSubTasks (TaskExecution aTask, List<PipelineTask> aSubTasks, MapContext aContext) throws Exception {
    for(PipelineTask subTask : aSubTasks) {
      SimpleTaskExecution subTaskExecution = new SimpleTaskExecution(subTask.asMap());
      subTaskExecution.setId(UUIDGenerator.generate());
      subTaskExecution.setJobId(aTask.getJobId());
      TaskExecution evaluatedTask = taskEvaluator.evaluate(subTaskExecution, aContext);
      SimpleTaskExecution completion = doExecuteTask(evaluatedTask);
      if(completion.getName() != null) {
        aContext.set(completion.getName(), completion.getOutput());
      }
    }
  }
  
  private void handleException (TaskExecution aTask, Exception aException) {
    logger.error(aException.getMessage(),aException);
    SimpleTaskExecution task = SimpleTaskExecution.of(aTask);
    task.setError(new ErrorObject(aException.getMessage(),ExceptionUtils.getStackFrames(aException)));
    task.setStatus(TaskStatus.FAILED);
    messageBroker.send(Queues.ERRORS, task);
  }
  
  private long calculateTimeout (TaskExecution aTask) {
    if(aTask.getTimeout() != null) {
      return Duration.parse("PT"+aTask.getTimeout()).toMillis();
    }
    return DEFAULT_TIME_OUT;
  }

  public void setTaskHandlerResolver(TaskHandlerResolver aTaskHandlerResolver) {
    taskHandlerResolver = aTaskHandlerResolver;
  }

  public void setMessageBroker(MessageBroker aMessageBroker) {
    messageBroker = aMessageBroker;
  }
  
  public void setEventPublisher(EventPublisher aEventPublisher) {
    eventPublisher = aEventPublisher;
  }
  
  private static class TaskExecutionFuture<T> implements Future<T> {

    private final Future<T> future;
    private final TaskExecution taskExecution;
    
    TaskExecutionFuture (TaskExecution aTaskExecution, Future<T> aFuture) {
      taskExecution = Objects.requireNonNull(aTaskExecution);
      future = Objects.requireNonNull(aFuture);
    }

    @Override
    public boolean cancel(boolean aMayInterruptIfRunning) {
      return future.cancel(aMayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
      return future.isCancelled();
    }

    @Override
    public boolean isDone() {
      return future.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
      return future.get();
    }

    @Override
    public T get(long aTimeout, TimeUnit aUnit) throws InterruptedException, ExecutionException, TimeoutException {
      return future.get(aTimeout, aUnit);
    }
    
  }
  
}