
package com.creactiviti.piper.core.task;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.creactiviti.piper.core.messagebroker.Queues;

public class WorkTaskExecutorTests {

  @Test
  public void test1 () {
    WorkTaskDispatcher executor = new WorkTaskDispatcher((k,m)->Assertions.assertEquals(Queues.TASKS, k));
    executor.dispatch(new SimpleTaskExecution());
  }
  
  @Test
  public void test2 () {
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.setNode("encoder");
    WorkTaskDispatcher executor = new WorkTaskDispatcher((k,m)->Assertions.assertEquals("encoder", k));
    executor.dispatch(task);
  }
  
  @Test
  public void test3 () {
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.setNode("encoder.xlarge");
    WorkTaskDispatcher executor = new WorkTaskDispatcher((k,m)->Assertions.assertEquals("encoder.xlarge", k));
    executor.dispatch(task);
  }
  
}
