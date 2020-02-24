package com.creactiviti.piper.taskhandler.time;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.creactiviti.piper.core.task.SimpleTaskExecution;

public class SleepTests {

  @Test
  public void test1 () throws InterruptedException {
    Sleep sleep = new Sleep();
    long now = System.currentTimeMillis();
    sleep.handle(SimpleTaskExecution.of("duration", "1s"));
    long delta = System.currentTimeMillis()-now;
    Assertions.assertTrue(delta >= 1000 && delta <= 1100);
  }
  
  @Test
  public void test2 () throws InterruptedException {
    Sleep sleep = new Sleep();
    long now = System.currentTimeMillis();
    sleep.handle(SimpleTaskExecution.of("millis", 500));
    long delta = System.currentTimeMillis()-now;
    Assertions.assertTrue(delta >= 500 && delta <= 600);
  }
  
  @Test
  public void test3 () throws InterruptedException {
    Sleep sleep = new Sleep();
    long now = System.currentTimeMillis();
    sleep.handle(new SimpleTaskExecution());
    long delta = System.currentTimeMillis()-now;
    Assertions.assertTrue(delta >= 1000 && delta <= 1100);
  }
  
}
