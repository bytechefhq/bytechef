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


/**
 * The strategey interface used for resolving the 
 * apprpriate {@link TaskDispatcher} instance for a 
 * given {@link TaskExecution}.
 * 
 * @author Arik Cohen
 * @since Mar 26, 2017
 */
public interface TaskDispatcherResolver {
  
  /**
   * Resolves a {@link TaskDispatcher} for the given
   * {@link TaskExecution} instance or <code>null</code>
   * if one can not be resolved. 
   * 
   * @param aTask
   *           The {@link TaskExecution} instance
   * @return a {@link TaskDispatcher} instance to execute the given task or <code>null</code> if 
   *         unable to resolve one. 
   */
  TaskDispatcher resolve (Task aTask);
  
}
