package com.creactiviti.piper.error;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.creactiviti.piper.core.error.ErrorHandler;
import com.creactiviti.piper.core.error.ErrorHandlerChain;
import com.creactiviti.piper.core.job.Job;
import com.creactiviti.piper.core.job.SimpleJob;
import com.creactiviti.piper.core.task.SimpleTaskExecution;
import com.creactiviti.piper.core.task.TaskExecution;

public class ErrorHandlerChainTests {

  @Test
  public void test1() {
    ErrorHandler errorHandler = new ErrorHandler<Job>() {
      public void handle(Job j) {
        Assert.assertEquals(SimpleJob.class, j.getClass()); 
      }
    };
    ErrorHandlerChain chain = new ErrorHandlerChain(Arrays.asList(errorHandler));
    chain.handle(new SimpleJob());
  }

  @Test
  public void test2() {
    ErrorHandler errorHandler1 = new ErrorHandler<Job>() {
      public void handle(Job j) {
        throw new IllegalStateException("should not get here");
      }
    };
    ErrorHandler errorHandler2 = new ErrorHandler<TaskExecution>() {
      public void handle(TaskExecution jt) {
        Assert.assertEquals(SimpleTaskExecution.class, jt.getClass()); 
      }
    };
    ErrorHandlerChain chain = new ErrorHandlerChain(Arrays.asList(errorHandler1,errorHandler2));
    chain.handle(SimpleTaskExecution.create());
  }
}
