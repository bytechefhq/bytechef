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
 * a strategy interface used for resolving a 
 * {@link TaskHandler} implementation which can handle
 * the given {@link TaskExecution} instance. Implementations 
 * are expected to return <code>null</code> if unable 
 * to resolve an appropriate {@link TaskHandler} implementation
 * to allow for chaining multiple {@link TaskHandlerResolver} 
 * implementations.
 * 
 * @author Arik Cohen
 * @since Jan 28, 2017
 */
public interface TaskHandlerResolver {

  TaskHandler<?> resolve (Task aTask);
  
}
