package com.integri.atlas.workflow.error;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.integri.atlas.workflow.core.error.ErrorObject;
import com.integri.atlas.workflow.core.error.TaskExecutionErrorHandler;
import com.integri.atlas.workflow.core.event.EventPublisher;
import com.integri.atlas.workflow.core.job.JobRepository;
import com.integri.atlas.workflow.core.job.SimpleJob;
import com.integri.atlas.workflow.core.task.SimpleTaskExecution;
import com.integri.atlas.workflow.core.task.TaskDispatcher;
import com.integri.atlas.workflow.core.task.TaskExecutionRepository;

public class TaskExecutionErrorHandlerTests {

  private JobRepository jobRepo = mock(JobRepository.class);
  private TaskExecutionRepository taskRepo = mock(TaskExecutionRepository.class);
  private TaskDispatcher taskDispatcher = mock(TaskDispatcher.class);
  private EventPublisher eventPublisher = mock(EventPublisher.class);

  @Test
  public void test1 () {
    when(jobRepo.getByTaskId("1234")).thenReturn(new SimpleJob(Collections.singletonMap("id","4567")));
    TaskExecutionErrorHandler handler = new TaskExecutionErrorHandler();
    handler.setEventPublisher(eventPublisher);
    handler.setJobRepository(jobRepo);
    handler.setJobTaskRepository(taskRepo);
    SimpleTaskExecution errorable = new SimpleTaskExecution();
    errorable.setId("1234");
    errorable.setError(new ErrorObject("something bad happened", new String[0]));
    handler.handle(errorable);
    handler.handle(errorable);
    verify(taskDispatcher,times(0)).dispatch(any());
  }

  @Test
  public void test2 () {
    when(jobRepo.getByTaskId("1234")).thenReturn(new SimpleJob());
    TaskExecutionErrorHandler handler = new TaskExecutionErrorHandler();
    handler.setJobRepository(jobRepo);
    handler.setJobTaskRepository(taskRepo);
    handler.setTaskDispatcher(taskDispatcher);
    SimpleTaskExecution errorable = SimpleTaskExecution.of("retry", 1);
    errorable.setId("1234");
    errorable.setError(new ErrorObject("something bad happened", new String[0]));
    handler.handle(errorable);
    verify(taskDispatcher,times(1)).dispatch(any());
  }

}
