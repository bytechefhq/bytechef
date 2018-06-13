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

import com.creactiviti.piper.core.context.Context;

/**
 * Strategy interface for evaluating a JobTask.
 *
 * @author Arik Cohen
 * @since Mar 31, 2017
 */
public interface TaskEvaluator {

  /**
   * Evaluate the {@link TaskExecution}
   * 
   * @param aJobTask
   *          The {@link TaskExecution} instance to evaluate
   * @param aContext
   *          The context to evaluate the task against
   * @return the evaluate {@link TaskExecution}.
   */
  TaskExecution evaluate (TaskExecution aJobTask, Context aContext);
  
}
