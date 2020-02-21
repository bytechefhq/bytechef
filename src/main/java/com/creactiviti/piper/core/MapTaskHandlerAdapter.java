package com.creactiviti.piper.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.context.MapContext;
import com.creactiviti.piper.core.messagebroker.Queues;
import com.creactiviti.piper.core.messagebroker.SyncMessageBroker;
import com.creactiviti.piper.core.task.MapTaskDispatcher;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;
import com.creactiviti.piper.core.task.TaskHandlerResolver;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
@Component("map")
class MapTaskHandlerAdapter implements TaskHandler<List<?>> {
  
  private final TaskHandlerResolver taskHandlerResolver;
  
  public MapTaskHandlerAdapter(@Lazy TaskHandlerResolver aResolver) {
    taskHandlerResolver = Objects.requireNonNull(aResolver);
  }
  
  @Override
  public List<?> handle (TaskExecution aTask) throws Exception {
    List<Object> result = new ArrayList<>();
    
    SyncMessageBroker messageBroker = new SyncMessageBroker();
    
    messageBroker.receive(Queues.COMPLETIONS, (message) -> {
      TaskExecution completion = (TaskExecution) message;
      result.add(completion.getOutput());
    });
    
    Worker worker = new Worker();
    
    worker.setTaskHandlerResolver(taskHandlerResolver);
    worker.setEventPublisher((event)->{});
    worker.setMessageBroker(messageBroker);
    
    InMemoryContextRepository contextRepository = new InMemoryContextRepository();
    
    contextRepository.push(aTask.getId(), new MapContext());
    
    MapTaskDispatcher dispatcher = MapTaskDispatcher.builder()
                                                    .contextRepository(contextRepository)
                                                    .counterRepository(new InMemoryCounterRepository())
                                                    .messageBroker(messageBroker)
                                                    .taskDispatcher((task)->worker.handle(task))
                                                    .taskExecutionRepository(new InMemoryTaskExecutionRepository())
                                                    .build();
    
    dispatcher.dispatch(aTask);
    
    return result;
  }

}
