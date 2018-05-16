/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.creactiviti.piper.core.job;

import java.util.Date;
import java.util.List;

import com.creactiviti.piper.core.Accessor;
import com.creactiviti.piper.core.error.Errorable;
import com.creactiviti.piper.core.error.Prioritizable;
import com.creactiviti.piper.core.pipeline.Pipeline;
import com.creactiviti.piper.core.task.TaskExecution;

/**
 * Represents an instance of a job.
 * 
 * @author Arik Cohen
 * @since Jun 12, 2016
 */
public interface Job extends Errorable, Prioritizable {

  /**
   * Return the ID of the job.
   */
  String getId ();
  
  /**
   * Return the {@link JobStatus}
   * 
   * @return The job's status.
   */
  JobStatus getStatus ();
  
  /**
   * Returns the index of the step on the job's 
   * pipeline on which the job is working on right 
   * now.
   * 
   * @return int
   *           The step ordinal number
   */
  int getCurrentTask ();
  
  /**
   * Returns the list of tasks that were executed as part
   * of the job instance.
   * 
   * @return {@link List}
   */
  List<TaskExecution> getExecution ();

  /**
   * Return the job's pipeline id.
   * 
   * @return {@link Pipeline} 
   */
  String getPipelineId ();
  
  /**
   * Return the job's human-readable name.
   * 
   * @return {@link Pipeline} 
   */
  String getLabel ();

  /**
   * Return the time when the job was originally 
   * created.
   * 
   * @return {@link Date}
   */
  Date getCreateTime ();
    
  /**
   * Return the time of when the job began 
   * execution.
   * 
   * @return {@link Date}
   */
  Date getStartTime ();  
  
  /**
   * Get time execution entered end status: COMPLETED, STOPPED, FAILED 
   * 
   * @return {@link Date}
   */
  Date getEndTime ();
  
  /**
   * Get the list of tags assigned to the job.
   * 
   * @return String[]
   */
  String[] getTags ();

  /**
   * Get the key-value map of inputs passed 
   * to the job when it was created.
   * 
   * @return {@link Accessor}
   */
  Accessor getInputs ();

  /**
   * Get the key-value map of outputs returned
   * by the job.
   * 
   * @return {@link Accessor}
   */
  Accessor getOutputs ();

  /**
   * Get the list of webhooks configured
   * for this job.
   * 
   * @return {@link List}
   */
  List<Accessor> getWebhooks ();
  
}
