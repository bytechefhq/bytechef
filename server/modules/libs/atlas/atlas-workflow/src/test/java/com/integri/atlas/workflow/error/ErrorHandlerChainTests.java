package com.integri.atlas.workflow.error;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.integri.atlas.workflow.core.error.ErrorHandler;
import com.integri.atlas.workflow.core.error.ErrorHandlerChain;
import com.integri.atlas.workflow.core.job.Job;
import com.integri.atlas.workflow.core.job.SimpleJob;
import com.integri.atlas.workflow.core.task.SimpleTaskExecution;
import com.integri.atlas.workflow.core.task.TaskExecution;

public class ErrorHandlerChainTests {

  @Test
  public void test1() {
    ErrorHandler errorHandler = new ErrorHandler<Job>() {
      @Override
      public void handle(Job j) {
        Assertions.assertEquals(SimpleJob.class, j.getClass());
      }
    };
    ErrorHandlerChain chain = new ErrorHandlerChain(Arrays.asList(errorHandler));
    chain.handle(new SimpleJob());
  }

  @Test
  public void test2() {
    ErrorHandler errorHandler1 = new ErrorHandler<Job>() {
      @Override
      public void handle(Job j) {
        throw new IllegalStateException("should not get here");
      }
    };
    ErrorHandler errorHandler2 = new ErrorHandler<TaskExecution>() {
      @Override
      public void handle(TaskExecution jt) {
        Assertions.assertEquals(SimpleTaskExecution.class, jt.getClass());
      }
    };
    ErrorHandlerChain chain = new ErrorHandlerChain(Arrays.asList(errorHandler1,errorHandler2));
    chain.handle(new SimpleTaskExecution());
  }
}
