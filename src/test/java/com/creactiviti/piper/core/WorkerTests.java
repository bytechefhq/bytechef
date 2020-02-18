
package com.creactiviti.piper.core;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.creactiviti.piper.core.messagebroker.Queues;
import com.creactiviti.piper.core.messagebroker.SynchMessageBroker;
import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.TaskExecution;

public class WorkerTests {

  @Test
  public void test1 () {
    Worker worker = new Worker();
    SynchMessageBroker messageBroker = new SynchMessageBroker();
    messageBroker.receive(Queues.COMPLETIONS, (t)-> Assertions.assertTrue(((TaskExecution)t).getOutput().equals("done")) );
    messageBroker.receive(Queues.EVENTS, (t)-> {} );
    worker.setMessageBroker(messageBroker);
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
    SynchMessageBroker messageBroker = new SynchMessageBroker();
    messageBroker.receive(Queues.ERRORS, (t)-> Assertions.assertTrue( ((TaskExecution)t).getError().getMessage().equals("bad input") ) );
    messageBroker.receive(Queues.EVENTS, (t)-> {} );
    worker.setMessageBroker(messageBroker);
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
