package com.creactiviti.piper.core;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.SpelTaskEvaluator;
import com.creactiviti.piper.core.task.TaskHandlerResolver;

public class MapTaskHandlerAdapterTests {

  @Test
  public void test1 () throws Exception {
    TaskHandlerResolver resolver = (task)->(t)->t.get("value");
    MapTaskHandlerAdapter adapter = new MapTaskHandlerAdapter(resolver,SpelTaskEvaluator.create());
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.setId("1234");
    task.setJobId("4567");
    task.set("list", List.of(1,2,3));
    task.set("iteratee", Map.of("type","var","value","${item}"));
    List<?> results = adapter.handle(task);
    Assertions.assertEquals(List.of(1,2,3),results);
  }
  
  @Test
  public void test2 () throws Exception {
    Assertions.assertThrows(RuntimeException.class, () -> {
      TaskHandlerResolver resolver = (task)->(t)->{
        throw new IllegalArgumentException("i'm rogue");
      };
      MapTaskHandlerAdapter adapter = new MapTaskHandlerAdapter(resolver,SpelTaskEvaluator.create());
      SimpleTaskExecution task = new SimpleTaskExecution();
      task.setId("1234");
      task.setJobId("4567");
      task.set("list", List.of(1,2,3));
      task.set("iteratee", Map.of("type","rogue"));
      adapter.handle(task);
    });
  }
  
}
