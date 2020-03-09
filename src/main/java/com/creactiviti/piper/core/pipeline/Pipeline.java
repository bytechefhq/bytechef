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
package com.creactiviti.piper.core.pipeline;

import java.util.List;

import com.creactiviti.piper.core.Accessor;
import com.creactiviti.piper.core.error.Errorable;
import com.creactiviti.piper.core.task.PipelineTask;

/**
 * Pipelines are the the blueprints that describe
 * the execution of a job.
 * 
 * @author Arik Cohen
 * @since Jun 12, 2016
 */
public interface Pipeline extends Errorable {

  /**
   * Returns the unique identifier of the pipeline. 
   */
  String getId ();
  
  /**
   * Returns a descriptive name for the pipeline. 
   */
  String getLabel ();
  
  /**
   * Returns the steps that make up the pipeline.
   */
  List<PipelineTask> getTasks ();

  /**
   * Returns the pipeline's expected inputs
   */
  List<Accessor> getInputs ();

  /**
   * Returns the pipeline's expected outputs
   */
  List<Accessor> getOutputs ();
  
  /**
   * Defines the maximum number of times that 
   * this message may be retries. 
   * 
   * @return int the maximum number of retries. 
   */
  int getRetry ();
  
}
