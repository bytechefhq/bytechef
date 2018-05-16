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
package com.creactiviti.piper.core;

import java.util.ArrayList;
import java.util.List;

import com.creactiviti.piper.core.task.TaskExecution;

public class TaskCompletionHandlerChain implements TaskCompletionHandler {

  private List<TaskCompletionHandler> taskCompletionHandlers = new ArrayList<>();
  
  @Override
  public void handle (TaskExecution aJobTask) {
    for(TaskCompletionHandler handler : taskCompletionHandlers) {
      if(handler.canHandle(aJobTask)) {
        handler.handle(aJobTask);
      }
    }
  }

  @Override
  public boolean canHandle(TaskExecution aJobTask) {
    return true;
  }
  
  public void setTaskCompletionHandlers(List<TaskCompletionHandler> aTaskCompletionHandlers) {
    taskCompletionHandlers = aTaskCompletionHandlers;
  }

}
