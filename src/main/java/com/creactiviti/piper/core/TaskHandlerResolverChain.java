package com.creactiviti.piper.core;

import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.Task;
import com.creactiviti.piper.core.task.TaskHandler;
import com.creactiviti.piper.core.task.TaskHandlerResolver;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
@Primary
@Component
class TaskHandlerResolverChain implements TaskHandlerResolver {

  private final List<TaskHandlerResolver> resolvers;
  
  TaskHandlerResolverChain(List<TaskHandlerResolver> aResolvers) {
    resolvers = Objects.requireNonNull(aResolvers);
  }
  
  @Override
  public TaskHandler<?> resolve (Task aTask) {
    for(TaskHandlerResolver resolver : resolvers) {
      TaskHandler<?> handler = resolver.resolve(aTask);
      if(handler != null) {
        return handler;
      }
    }
    throw new IllegalArgumentException("Unknown task handler: " + aTask.getType());
  }

}
