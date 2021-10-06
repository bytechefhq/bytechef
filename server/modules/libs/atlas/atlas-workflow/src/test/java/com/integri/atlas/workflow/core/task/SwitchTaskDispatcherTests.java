
package com.integri.atlas.workflow.core.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.integri.atlas.workflow.core.context.ContextRepository;
import com.integri.atlas.workflow.core.context.MapContext;
import com.integri.atlas.workflow.core.messagebroker.MessageBroker;
import com.google.common.collect.ImmutableMap;

public class SwitchTaskDispatcherTests {

  private TaskExecutionRepository taskRepo = mock(TaskExecutionRepository.class);
  private TaskDispatcher taskDispatcher = mock(TaskDispatcher.class);
  private MessageBroker messageBroker = mock(MessageBroker.class);
  private ContextRepository contextRepository = mock(ContextRepository.class);

  @Test
  public void test1 ()  {
    when(contextRepository.peek(any())).thenReturn(new MapContext());
    SwitchTaskDispatcher dispatcher = new SwitchTaskDispatcher(taskDispatcher, taskRepo,messageBroker,contextRepository,SpelTaskEvaluator.create());
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.set("cases", Arrays.asList(ImmutableMap.of("key", "k1","tasks",Arrays.asList(ImmutableMap.of("type","print")))));
    task.set("expression", "k1");
    dispatcher.dispatch(task);
    ArgumentCaptor<TaskExecution> argument = ArgumentCaptor.forClass(TaskExecution.class);
    verify(taskDispatcher,times(1)).dispatch(argument.capture());
    Assertions.assertEquals("print", argument.getValue().getType());
  }

  @Test
  public void test2 ()  {
    when(contextRepository.peek(any())).thenReturn(new MapContext());
    SwitchTaskDispatcher dispatcher = new SwitchTaskDispatcher(taskDispatcher, taskRepo,messageBroker,contextRepository,SpelTaskEvaluator.create());
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.set("cases", Arrays.asList(ImmutableMap.of("key", "k1","tasks",Arrays.asList(ImmutableMap.of("type","print")))));
    task.set("expression", "k2");
    dispatcher.dispatch(task);
    verify(taskDispatcher,times(0)).dispatch(any());
  }

  @Test
  public void test3 ()  {
    when(contextRepository.peek(any())).thenReturn(new MapContext());
    SwitchTaskDispatcher dispatcher = new SwitchTaskDispatcher(taskDispatcher, taskRepo,messageBroker,contextRepository,SpelTaskEvaluator.create());
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.set("cases", Arrays.asList(
      ImmutableMap.of("key", "k1","tasks",Arrays.asList(ImmutableMap.of("type","print"))),
      ImmutableMap.of("key", "k2","tasks",Arrays.asList(ImmutableMap.of("type","sleep"))))
    );
    task.set("expression", "k2");
    dispatcher.dispatch(task);
    ArgumentCaptor<TaskExecution> argument = ArgumentCaptor.forClass(TaskExecution.class);
    verify(taskDispatcher,times(1)).dispatch(argument.capture());
    Assertions.assertEquals("sleep", argument.getValue().getType());
  }

  @Test
  public void test4 ()  {
    when(contextRepository.peek(any())).thenReturn(new MapContext());
    SwitchTaskDispatcher dispatcher = new SwitchTaskDispatcher(taskDispatcher, taskRepo,messageBroker,contextRepository,SpelTaskEvaluator.create());
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.set("cases", Arrays.asList(
      ImmutableMap.of("key", "k1","tasks",Arrays.asList(ImmutableMap.of("type","print"))),
      ImmutableMap.of("key", "k2","tasks",Arrays.asList(ImmutableMap.of("type","sleep")))
    ));
    task.set("default", Collections.singletonMap("value", "1234"));
    task.set("expression", "k99");
    dispatcher.dispatch(task);
    ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<TaskExecution> arg2 = ArgumentCaptor.forClass(TaskExecution.class);
    verify(messageBroker,times(1)).send(arg1.capture(),arg2.capture());
    Assertions.assertEquals("1234", arg2.getValue().getOutput());
  }

}
