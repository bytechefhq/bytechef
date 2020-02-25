package com.creactiviti.piper.core.job;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.creactiviti.piper.core.Accessor;
import com.creactiviti.piper.core.error.Error;

/**
 * @author Arik Cohen
 * @since Feb, 25 2020
 */
public class JobSummary {
  
  private final Job job;
  
  public JobSummary(Job aJob) {
    job = Objects.requireNonNull(aJob);
  }

  public int getPriority() {
    return job.getPriority();
  }

  public Error getError() {
    return job.getError();
  }

  public String getId() {
    return job.getId();
  }

  public String getParentTaskExecutionId() {
    return job.getParentTaskExecutionId();
  }

  public JobStatus getStatus() {
    return job.getStatus();
  }

  public int getCurrentTask() {
    return job.getCurrentTask();
  }

  public String getPipelineId() {
    return job.getPipelineId();
  }

  public String getLabel() {
    return job.getLabel();
  }

  public Date getCreateTime() {
    return job.getCreateTime();
  }

  public Date getStartTime() {
    return job.getStartTime();
  }

  public Date getEndTime() {
    return job.getEndTime();
  }

  public String[] getTags() {
    return job.getTags();
  }

  public Accessor getInputs() {
    return job.getInputs();
  }

  public Accessor getOutputs() {
    return job.getOutputs();
  }

  public List<Accessor> getWebhooks() {
    return job.getWebhooks();
  }

}
