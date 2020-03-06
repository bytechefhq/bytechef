package com.creactiviti.piper.core.task;

import java.util.Objects;

import com.creactiviti.piper.core.Coordinator;
import com.creactiviti.piper.core.context.MapContext;
import com.creactiviti.piper.core.error.ErrorObject;
import com.creactiviti.piper.core.event.EventListener;
import com.creactiviti.piper.core.event.Events;
import com.creactiviti.piper.core.event.PiperEvent;
import com.creactiviti.piper.core.job.Job;
import com.creactiviti.piper.core.job.JobRepository;
import com.creactiviti.piper.core.job.JobStatus;

/**
 * an {@link EventListener} which is used for listening to subflow
 * job status events. When a sub-flow completes/fails or stops its
 * parent job and its parent task needs to be informed so as to
 * resume its execution.
 * 
 * @author Arik Cohen
 * @since Sep 06, 2018
 * @see SubflowTaskDispatcher
 */
public class SubflowJobStatusEventListener implements EventListener {

  private final JobRepository jobRepository;
  private final TaskExecutionRepository taskExecutionRepository;
  private final Coordinator coordinator;
  private final TaskEvaluator taskEvaluator;
  
  public SubflowJobStatusEventListener (JobRepository aJobRepository, TaskExecutionRepository aTaskExecutionRepository, Coordinator aCoordinator, TaskEvaluator aTaskEvaluator) {
    jobRepository = Objects.requireNonNull(aJobRepository);
    taskExecutionRepository = Objects.requireNonNull(aTaskExecutionRepository);
    coordinator = Objects.requireNonNull(aCoordinator);
    taskEvaluator = Objects.requireNonNull(aTaskEvaluator);
  }
  
  @Override
  public void onApplicationEvent (PiperEvent aEvent) {
    if(aEvent.getType().equals(Events.JOB_STATUS)) {
      
      String jobId = aEvent.getRequiredString("jobId");
      JobStatus status = JobStatus.valueOf(aEvent.getRequiredString("status"));
      Job job = jobRepository.getById(jobId);
      
      if(job.getParentTaskExecutionId() == null) {
        return; // not a subflow -- nothing to do
      }
      
      switch(status) {
        case CREATED:
        case STARTED:
          break;
        case STOPPED: {
          TaskExecution subflowTask = taskExecutionRepository.findOne(job.getParentTaskExecutionId());
          coordinator.stop(subflowTask.getJobId());
          break;
        }
        case FAILED: {
          SimpleTaskExecution errorable = SimpleTaskExecution.of(taskExecutionRepository.findOne(job.getParentTaskExecutionId()));
          errorable.setError(new ErrorObject("An error occured with subflow",new String[0]));
          coordinator.handleError(errorable);
          break;
        }
        case COMPLETED:{
          SimpleTaskExecution completion = SimpleTaskExecution.of(taskExecutionRepository.findOne(job.getParentTaskExecutionId()));
          Object output = job.getOutputs();
          if(completion.getOutput() != null) {
            TaskExecution evaluated = taskEvaluator.evaluate(completion, new MapContext ("execution", new MapContext("output", output)));
            completion = SimpleTaskExecution.of(evaluated);
          }
          else {
            completion.setOutput(output);
          }
          coordinator.complete(completion);
          break;
        }
        default:
          throw new IllegalStateException("Unnown status: " + status);
      }
    }
  }

}
