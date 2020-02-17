
package com.creactiviti.piper.core;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.creactiviti.piper.core.messenger.Queues;
import com.creactiviti.piper.core.messenger.SynchMessenger;
import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.TaskExecution;

public class WorkerTests {

  @Test
  public void test1 () {
    Worker worker = new Worker();
    SynchMessenger messenger = new SynchMessenger();
    messenger.receive(Queues.COMPLETIONS, (t)-> Assertions.assertTrue(((TaskExecution)t).getOutput().equals("done")) );
    messenger.receive(Queues.EVENTS, (t)-> {} );
    worker.setMessenger(messenger);
    worker.setEventPublisher((e)->{});
    worker.setTaskHandlerResolver((jt) -> (t) -> "done");
    SimpleTaskExecution task = SimpleTaskExecution.create();
    task.setId("1234");
    task.setJobId("4567");
    worker.handle(task);
  }
  
  
  @Test
  public void test2 () {
    Worker worker = new Worker();
    SynchMessenger messenger = new SynchMessenger();
    messenger.receive(Queues.ERRORS, (t)-> Assertions.assertTrue( ((TaskExecution)t).getError().getMessage().equals("bad input") ) );
    messenger.receive(Queues.EVENTS, (t)-> {} );
    worker.setMessenger(messenger);
    worker.setEventPublisher((e)->{});
    worker.setTaskHandlerResolver((jt) -> (t) -> {
      throw new IllegalArgumentException("bad input");
    });
    SimpleTaskExecution task = SimpleTaskExecution.create();
    task.setId("1234");
    task.setJobId("4567");
    worker.handle(task);
  }
  
  
}
