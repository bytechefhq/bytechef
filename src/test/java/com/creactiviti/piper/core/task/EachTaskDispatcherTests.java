
package com.creactiviti.piper.core.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.creactiviti.piper.core.context.ContextRepository;
import com.creactiviti.piper.core.context.MapContext;
import com.creactiviti.piper.core.messagebroker.MessageBroker;

public class EachTaskDispatcherTests {
  
  private TaskExecutionRepository taskRepo = mock(TaskExecutionRepository.class);
  private TaskDispatcher taskDispatcher = mock(TaskDispatcher.class);
  private MessageBroker messageBroker = mock(MessageBroker.class);
  private ContextRepository contextRepository = mock(ContextRepository.class);
  private CounterRepository counterRepository = mock(CounterRepository.class);
  
  @Test
  public void test1 ()  {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      EachTaskDispatcher dispatcher = new EachTaskDispatcher(null,null,null,null,null);
      dispatcher.dispatch(SimpleTaskExecution.create());
    });
  }
  
  @Test
  public void test2 ()  {
    when(contextRepository.peek(any())).thenReturn(new MapContext());
    EachTaskDispatcher dispatcher = new EachTaskDispatcher(taskDispatcher, taskRepo,messageBroker,contextRepository,counterRepository);
    SimpleTaskExecution task = SimpleTaskExecution.create();
    task.set("list", Arrays.asList(1,2,3));
    task.set("iteratee", Collections.singletonMap("type", "print"));
    dispatcher.dispatch(task);
    verify(taskDispatcher,times(3)).dispatch(any());
    verify(messageBroker,times(0)).send(any(),any());
  }
  
  @Test
  public void test3 ()  {
    EachTaskDispatcher dispatcher = new EachTaskDispatcher(taskDispatcher, taskRepo,messageBroker,contextRepository,counterRepository);
    SimpleTaskExecution task = SimpleTaskExecution.create();
    task.set("list", Arrays.asList());
    task.set("iteratee", Collections.singletonMap("type", "print"));
    dispatcher.dispatch(task);
    verify(taskDispatcher,times(0)).dispatch(any());
    verify(messageBroker,times(1)).send(any(),any());
  }

}
