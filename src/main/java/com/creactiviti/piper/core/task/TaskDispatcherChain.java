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

public class TaskDispatcherChain implements TaskDispatcher<Task> {
  
  private List<TaskDispatcherResolver> resolvers;
  
  public TaskDispatcherChain() {
  }
  
  @Override
  public void dispatch (Task aTask) {
    for(TaskDispatcherResolver resolver : resolvers) {
      TaskDispatcher executor = resolver.resolve(aTask);
      if(executor != null) {
        executor.dispatch(aTask);
        return;
      }
    }
    throw new IllegalArgumentException("Unable to execute task: " + aTask);
  }

  public void setResolvers(List<TaskDispatcherResolver> aResolvers) {
    resolvers = aResolvers;
  }

}
