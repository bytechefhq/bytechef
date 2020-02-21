package com.creactiviti.piper.core;

import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;
import com.creactiviti.piper.core.task.TaskHandlerResolver;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TaskDispatcherHandlerAdapterResolver implements TaskHandlerResolver {

  private final Map<String, TaskHandler<?>> taskHandlers;
  
  public TaskDispatcherHandlerAdapterResolver(@Lazy TaskHandlerResolver aResolver) {
    taskHandlers = Map.of("map",new MapTaskHandlerAdapter(aResolver));
  }

  @Override
  public TaskHandler<?> resolve (Task aTask) {
    return taskHandlers.get(aTask.getType());
  }
  
}
