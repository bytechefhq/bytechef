package com.creactiviti.piper.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Lazy;

import com.creactiviti.piper.core.context.MapContext;
import com.creactiviti.piper.core.error.Error;
import com.creactiviti.piper.core.messagebroker.Queues;
import com.creactiviti.piper.core.messagebroker.SyncMessageBroker;
import com.creactiviti.piper.core.task.MapTaskDispatcher;
import com.creactiviti.piper.core.task.TaskEvaluator;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;
import com.creactiviti.piper.core.task.TaskHandlerResolver;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
class MapTaskHandlerAdapter implements TaskHandler<List<?>> {
  
  private final TaskHandlerResolver taskHandlerResolver;
  private final TaskEvaluator taskEvaluator;
  
  public MapTaskHandlerAdapter(@Lazy TaskHandlerResolver aResolver, TaskEvaluator aTaskEvaluator) {
    taskHandlerResolver = Objects.requireNonNull(aResolver);
    taskEvaluator = Objects.requireNonNull(aTaskEvaluator);
  }
  
  @Override
  public List<?> handle (TaskExecution aTask) throws Exception {
    List<Object> result = new ArrayList<>();
    
    SyncMessageBroker messageBroker = new SyncMessageBroker();
    
    messageBroker.receive(Queues.COMPLETIONS, (message) -> {
      TaskExecution completion = (TaskExecution) message;
      result.add(completion.getOutput());
    });

    List<Error> errors = Collections.synchronizedList(new ArrayList<>());
    
    messageBroker.receive(Queues.ERRORS, (message) -> {
      TaskExecution erringTask = (TaskExecution) message;
      Error err = erringTask.getError();
      errors.add(err);
    });
    
    Worker worker = Worker.builder()
        .withTaskHandlerResolver(taskHandlerResolver)
        .withMessageBroker(messageBroker)
        .withEventPublisher((e)->{})
        .withExecutors(MoreExecutors.newDirectExecutorService())
        .withTaskEvaluator(taskEvaluator)
        .build();
    
    InMemoryContextRepository contextRepository = new InMemoryContextRepository();
    
    contextRepository.push(aTask.getId(), new MapContext());
    
    MapTaskDispatcher dispatcher = MapTaskDispatcher.builder()
                                                    .contextRepository(contextRepository)
                                                    .counterRepository(new InMemoryCounterRepository())
                                                    .messageBroker(messageBroker)
                                                    .taskDispatcher(worker::handle)
                                                    .taskExecutionRepository(new InMemoryTaskExecutionRepository())
                                                    .taskEvaluator(taskEvaluator)
                                                    .build();
    
    dispatcher.dispatch(aTask);
    
    if(errors.size() > 0) {
      StringBuilder errorMessage = new StringBuilder();
      for(Error e : errors) {
        if(errorMessage.length() > 3000) {
          errorMessage.append("\n")
                      .append("...");
          break;
        }
        if(errorMessage.length() > 0) {
          errorMessage.append("\n");
        }
        errorMessage.append(e.getMessage())
                    .append("\n")
                    .append(String.join("\n", Arrays.asList(e.getStackTrace())));
      }
      throw new RuntimeException(errorMessage.toString());
    }
    
    return result;
  }

}
