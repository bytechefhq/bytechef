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
package com.creactiviti.piper.core.task;

import java.util.List;

public interface TaskExecutionRepository {
  
  /**
   * Find a single {@link TaskExecution} instance by its id.
   * 
   * @param aId
   * @return TaskExecution
   */
  TaskExecution findOne (String aId);
  
  /**
   * Returns a collection of {@link TaskExecution} instances which 
   * are the children of the given parent id.
   * 
   * @param aParentId
   * @return
   */
  List<TaskExecution> findByParentId (String aParentId);

  /**
   * Creates a new persistent represenation of the given {@link TaskExecution}.
   * 
   * @param aTaskExecution
   */
  void create (TaskExecution aTaskExecution);

  /**
   * Merges the state of the given {@link TaskExecution} instance
   * with its persistent representation and returns the merged
   * version.
   * 
   * @param aTaskExecution
   */
  TaskExecution merge (TaskExecution aTaskExecution);
  
  /**
   * Returns the execution steps of the given job 
   * 
   * @param aJobId
   * @return List<TaskExecution>
   */
  List<TaskExecution> getExecution (String aJobId);

  
}
