package com.creactiviti.piper.core;

import java.util.Map;

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskEvaluator;
import com.creactiviti.piper.core.task.TaskHandler;
import com.creactiviti.piper.core.task.TaskHandlerResolver;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class TaskDispatcherHandlerAdapterResolver implements TaskHandlerResolver {

  private final Map<String, TaskHandler<?>> taskHandlers;
  
  public TaskDispatcherHandlerAdapterResolver(TaskHandlerResolver aResolver, TaskEvaluator aTaskEvaluator) {
    taskHandlers = Map.of("map",new MapTaskHandlerAdapter(aResolver,aTaskEvaluator));
  }

  @Override
  public TaskHandler<?> resolve (Task aTask) {
    return taskHandlers.get(aTask.getType());
  }
  
}
