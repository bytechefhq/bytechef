
package com.creactiviti.piper.core;


import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.creactiviti.piper.core.messagebroker.Queues;
import com.creactiviti.piper.core.messagebroker.SyncMessageBroker;
import com.creactiviti.piper.core.task.CancelTask;
import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.SpelTaskEvaluator;
import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandlerResolver;
import com.creactiviti.piper.core.uuid.UUIDGenerator;

public class WorkerTests {

  @Test
  public void test1 () {
    SyncMessageBroker messageBroker = new SyncMessageBroker();
    messageBroker.receive(Queues.COMPLETIONS, (t)-> Assertions.assertTrue(((TaskExecution)t).getOutput().equals("done")) );
    messageBroker.receive(Queues.EVENTS, (t)-> {} );

    Worker worker = Worker.builder()
        .withTaskHandlerResolver((jt) -> (t) -> "done")
        .withMessageBroker(messageBroker)
        .withEventPublisher((e)->{})
        .withTaskEvaluator(SpelTaskEvaluator.create())
        .build();

    SimpleTaskExecution task = new SimpleTaskExecution();
    task.setId("1234");
    task.setJobId("4567");
    worker.handle(task);
  }


  @Test
  public void test2 () {
    SyncMessageBroker messageBroker = new SyncMessageBroker();
    messageBroker.receive(Queues.ERRORS, (t)-> Assertions.assertTrue( ((TaskExecution)t).getError().getMessage().equals("bad input") ) );
    messageBroker.receive(Queues.EVENTS, (t)-> {} );
    Worker worker = Worker.builder()
        .withTaskHandlerResolver((jt) -> (t) -> {
          throw new IllegalArgumentException("bad input");
        })
        .withMessageBroker(messageBroker)
        .withEventPublisher((e)->{})
        .withTaskEvaluator(SpelTaskEvaluator.create())
        .build();
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.setId("1234");
    task.setJobId("4567");
    worker.handle(task);
  }

  @Test
  public void test3 () {
    SyncMessageBroker messageBroker = new SyncMessageBroker();
    messageBroker.receive(Queues.COMPLETIONS, (t)-> Assertions.assertEquals("done",(((TaskExecution)t).getOutput())));
    messageBroker.receive(Queues.EVENTS, (t)-> {} );
    Worker worker = Worker.builder()
        .withTaskHandlerResolver((t1) ->  {
          String type = t1.getType();
          if("var".equals(type)) {
            return (t2)->t2.getRequired("value");
          }
          else {
            throw new IllegalArgumentException("unknown type: " + type);
          }
        })
        .withMessageBroker(messageBroker)
        .withTaskEvaluator(SpelTaskEvaluator.create())
        .withEventPublisher((e)->{})
        .build();
    SimpleTaskExecution task = new SimpleTaskExecution();
    task.setId("1234");
    task.setJobId("4567");
    task.set(DSL.TYPE, "var");
    task.set("value", "${myVar}");
    task.set("pre", List.of(Map.of("name","myVar","type","var","value","done")));
    worker.handle(task);
  }

  @Test
  public void test4 () {

    String tempDir = new File (new File(System.getProperty("java.io.tmpdir")),UUIDGenerator.generate()).getAbsolutePath(); 

    SyncMessageBroker messageBroker = new SyncMessageBroker();
    messageBroker.receive(Queues.COMPLETIONS, (t)-> {
      Assertions.assertFalse(new File(tempDir).exists());
    });
    messageBroker.receive(Queues.EVENTS, (t)-> {} );

    Worker worker = Worker.builder()
        .withTaskHandlerResolver((t1) ->  {
          String type = t1.getType();
          if("var".equals(type)) {
            return (t2)->t2.getRequired("value");
          }
          else if("mkdir".equals(type)) {
            return (t2)->(new File (t2.getString("path")).mkdirs());
          }
          else if("rm".equals(type)) {
            return (t2)-> FileUtils.deleteQuietly((new File (t2.getString("path"))));
          }
          else if("pass".equals(type)) {
            Assertions.assertTrue(new File(tempDir).exists());
            return (t2)->null;
          }
          else {
            throw new IllegalArgumentException("unknown type: " + type);
          }
        })
        .withMessageBroker(messageBroker)
        .withEventPublisher((e)->{})
        .withTaskEvaluator(SpelTaskEvaluator.create())
        .build();

    SimpleTaskExecution task = new SimpleTaskExecution();

    task.setId("1234");
    task.setJobId("4567");
    task.set("type", "pass");
    task.set("pre", List.of(
        Map.of("type","mkdir","path",tempDir)
        ));
    task.set("post", List.of(
        Map.of("type","rm","path",tempDir)
        ));

    worker.handle(task);
  }

  @Test
  public void test5 () {

    String tempDir = new File (new File(System.getProperty("java.io.tmpdir")),UUIDGenerator.generate()).getAbsolutePath(); 

    SyncMessageBroker messageBroker = new SyncMessageBroker();
    messageBroker.receive(Queues.ERRORS, (t)-> {
      Assertions.assertFalse(new File(tempDir).exists());
    });
    messageBroker.receive(Queues.EVENTS, (t)-> {} );

    Worker worker = Worker.builder()
        .withTaskHandlerResolver((t1) ->  {
          String type = t1.getType();
          if("var".equals(type)) {
            return (t2)->t2.getRequired("value");
          }
          else if("mkdir".equals(type)) {
            return (t2)->(new File (t2.getString("path")).mkdirs());
          }
          else if("rm".equals(type)) {
            return (t2)-> FileUtils.deleteQuietly((new File (t2.getString("path"))));
          }
          else if("rogue".equals(type)) {
            Assertions.assertTrue(new File(tempDir).exists());
            return (t2)->{throw new RuntimeException("rogue");};
          }
          else {
            throw new IllegalArgumentException("unknown type: " + type);
          }
        })
        .withMessageBroker(messageBroker)
        .withEventPublisher((e)->{})
        .withTaskEvaluator(SpelTaskEvaluator.create())
        .build();


    SimpleTaskExecution task = new SimpleTaskExecution();

    task.setId("1234");
    task.setJobId("4567");
    task.set("type", "rogue");
    task.set("pre", List.of(
        Map.of("type","mkdir","path",tempDir)
        ));
    task.set("finalize", List.of(
        Map.of("type","rm","path",tempDir)
        ));

    worker.handle(task);
  }

  @Test
  public void test9 () {
    SyncMessageBroker messageBroker = new SyncMessageBroker();
    messageBroker.receive(Queues.COMPLETIONS, (t)-> {
      TaskExecution te = (TaskExecution) t;
      Assertions.assertNull(te.getOutput());
    });
    messageBroker.receive(Queues.EVENTS, (t)-> {} );


    MapTaskHandlerAdapter[] taskHandlerAdapterRef = new MapTaskHandlerAdapter[1];

    TaskHandlerResolver thr = (t1) -> {
      String type = t1.getType();
      if("var".equals(type)) {
        return (t2)->t2.getRequired("value");
      }
      if("pass".equals(type)) {
        return (t2)->null;
      }
      if("map".equals(type)) {
        return taskHandlerAdapterRef[0];
      }
      else {
        throw new IllegalArgumentException("unknown type: " + type);
      }
    };

    Worker worker = Worker.builder()
        .withTaskHandlerResolver(thr)
        .withMessageBroker(messageBroker)
        .withEventPublisher((e)->{})
        .withTaskEvaluator(SpelTaskEvaluator.create())
        .build();

    taskHandlerAdapterRef[0] = new MapTaskHandlerAdapter(thr,SpelTaskEvaluator.create());

    SimpleTaskExecution task = new SimpleTaskExecution();

    task.setId("1234");
    task.setJobId("4567");
    task.set("type", "pass");

    task.set("pre", List.of(
        Map.of("name","output","type","map","list", Arrays.asList(1,2,3),"iteratee",Map.of("type","var","value","${item}"))
        ));
    task.set("post", List.of(
        Map.of("name","output","type","map","list", Arrays.asList(1,2,3),"iteratee",Map.of("type","var","value","${item}"))
        ));
    task.set("finalize", List.of(
        Map.of("name","output","type","map","list", Arrays.asList(1,2,3),"iteratee",Map.of("type","var","value","${item}"))
        ));

    worker.handle(task);
  }

  @Test
  public void test6 () throws InterruptedException {
    ExecutorService executors = Executors.newSingleThreadExecutor();
    SyncMessageBroker messageBroker = new SyncMessageBroker();

    Worker worker = Worker.builder()
        .withTaskHandlerResolver((jt) -> (t) -> {
          System.out.println("sleeping...");
          TimeUnit.SECONDS.sleep(5);
          System.out.println("woke up!");
          return null;
        })
        .withMessageBroker(messageBroker)
        .withEventPublisher((e)->{})
        .withTaskEvaluator(SpelTaskEvaluator.create())
        .build();

    SimpleTaskExecution task = new SimpleTaskExecution();
    task.setId("1234");
    task.setJobId("4567");
    // execute the task
    executors.submit(() -> worker.handle(task));
    // give it a second to start executing
    TimeUnit.SECONDS.sleep(1);
    Assertions.assertEquals(1, worker.getTaskExecutions().size());
    // cancel the execution of the task
    worker.handle(new CancelTask(task.getJobId(),task.getId()));
    // give it a second to cancel
    TimeUnit.SECONDS.sleep(1);
    Assertions.assertEquals(0, worker.getTaskExecutions().size());
  }

  @Test
  public void test7 () throws InterruptedException {
    ExecutorService executors = Executors.newFixedThreadPool(2);
    SyncMessageBroker messageBroker = new SyncMessageBroker();

    Worker worker = Worker.builder()
        .withTaskHandlerResolver((jt) -> (t) -> {
          System.out.println("sleeping...");
          TimeUnit.SECONDS.sleep(5);
          System.out.println("woke up!");
          return null;
        })
        .withMessageBroker(messageBroker)
        .withEventPublisher((e)->{})
        .withTaskEvaluator(SpelTaskEvaluator.create())
        .build();

    SimpleTaskExecution task1 = new SimpleTaskExecution();
    task1.setId("1111");
    task1.setJobId("2222");
    // execute the task
    executors.submit(() -> worker.handle(task1));

    SimpleTaskExecution task2 = new SimpleTaskExecution();
    task2.setId("3333");
    task2.setJobId("4444");
    // execute the task
    executors.submit(() -> worker.handle(task2));

    // give it a second to start executing
    TimeUnit.SECONDS.sleep(1);

    Assertions.assertEquals(2, worker.getTaskExecutions().size());
    // cancel the execution of the task
    worker.handle(new CancelTask(task1.getJobId(),task1.getId()));
    // give it a second to cancel
    TimeUnit.SECONDS.sleep(1);
    Assertions.assertEquals(1, worker.getTaskExecutions().size());
  }

  @Test
  public void test8 () throws InterruptedException {
    ExecutorService executors = Executors.newFixedThreadPool(2);
    SyncMessageBroker messageBroker = new SyncMessageBroker();
    Worker worker = Worker.builder()
        .withTaskHandlerResolver((jt) -> (t) -> {
          System.out.println("sleeping...");
          TimeUnit.SECONDS.sleep(5);
          System.out.println("woke up!");
          return null;
        })
        .withMessageBroker(messageBroker)
        .withEventPublisher((e)->{})
        .withTaskEvaluator(SpelTaskEvaluator.create())
        .build();

    SimpleTaskExecution task1 = new SimpleTaskExecution();
    task1.setId("1111");
    task1.setJobId("2222");
    // execute the task
    executors.submit(() -> worker.handle(task1));

    SimpleTaskExecution task2 = new SimpleTaskExecution();
    task2.setId("3333");
    task2.setJobId("2222");
    task2.setParentId(task1.getId());
    // execute the task
    executors.submit(() -> worker.handle(task2));

    // give it a second to start executing
    TimeUnit.SECONDS.sleep(1);

    Assertions.assertEquals(2, worker.getTaskExecutions().size());
    // cancel the execution of the task
    worker.handle(new CancelTask(task1.getJobId(),task1.getId()));
    // give it a second to cancel
    TimeUnit.SECONDS.sleep(1);
    Assertions.assertEquals(0, worker.getTaskExecutions().size());
  }

}
