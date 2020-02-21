
package com.creactiviti.piper.core;


import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.creactiviti.piper.core.messagebroker.Queues;
import com.creactiviti.piper.core.messagebroker.SyncMessageBroker;
import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.TaskExecution;

public class WorkerTests {

  @Test
  public void test1 () {
    Worker worker = new Worker();
    SyncMessageBroker messageBroker = new SyncMessageBroker();
    messageBroker.receive(Queues.COMPLETIONS, (t)-> Assertions.assertTrue(((TaskExecution)t).getOutput().equals("done")) );
    messageBroker.receive(Queues.EVENTS, (t)-> {} );
    worker.setMessageBroker(messageBroker);
    worker.setEventPublisher((e)->{});
    worker.setTaskHandlerResolver((jt) -> (t) -> "done");
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.setId("1234");
    task.setJobId("4567");
    worker.handle(task);
  }
  
  
  @Test
  public void test2 () {
    Worker worker = new Worker();
    SyncMessageBroker messageBroker = new SyncMessageBroker();
    messageBroker.receive(Queues.ERRORS, (t)-> Assertions.assertTrue( ((TaskExecution)t).getError().getMessage().equals("bad input") ) );
    messageBroker.receive(Queues.EVENTS, (t)-> {} );
    worker.setMessageBroker(messageBroker);
    worker.setEventPublisher((e)->{});
    worker.setTaskHandlerResolver((jt) -> (t) -> {
      throw new IllegalArgumentException("bad input");
    });
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.setId("1234");
    task.setJobId("4567");
    worker.handle(task);
  }
  
  @Test
  public void test3 () {
    Worker worker = new Worker();
    SyncMessageBroker messageBroker = new SyncMessageBroker();
    messageBroker.receive(Queues.COMPLETIONS, (t)-> Assertions.assertEquals("done",(((TaskExecution)t).getOutput())));
    messageBroker.receive(Queues.EVENTS, (t)-> {} );
    worker.setMessageBroker(messageBroker);
    worker.setEventPublisher((e)->{});
    worker.setTaskHandlerResolver((t1) -> {
      String type = t1.getType();
      if("var".equals(type)) {
        return (t2)->t2.getRequired("value");
      }
      else {
        throw new IllegalArgumentException("unknown type: " + type);
      }
    });
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.setId("1234");
    task.setJobId("4567");
    task.set(DSL.TYPE, "var");
    task.set("value", "${myVar}");
    task.set(DSL.PRE, List.of(Map.of("name","myVar","type","var","value","done")));
    worker.handle(task);
  }
  
  
}
