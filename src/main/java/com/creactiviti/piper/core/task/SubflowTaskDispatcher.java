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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.messenger.Messenger;
import com.creactiviti.piper.core.messenger.Queues;

/**
 * a {@link TaskDispatcher} implementation which handles the 'subflow' 
 * task type. Subflows are essentially isolated job instances started 
 * by the parent 'subflow' task which is the owner of the sub-flow. 
 * 
 * @author Arik Cohen
 * @since Sep 06, 2018
 */
public class SubflowTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

  private final Messenger messenger;
  
  public SubflowTaskDispatcher (Messenger aMessenger) {
    messenger = aMessenger;
  }
  
  @Override
  public void dispatch(TaskExecution aTask) {
    Map<String, Object> params = new HashMap<>();
    params.put(DSL.INPUTS, aTask.getMap(DSL.INPUTS, Collections.emptyMap()));
    params.put(DSL.PARENT_TASK_EXECUTION_ID, aTask.getId());
    params.put(DSL.PIPELINE_ID, aTask.getRequiredString(DSL.PIPELINE_ID));
    messenger.send(Queues.SUBFLOWS, params);
  }

  @Override
  public TaskDispatcher resolve(Task aTask) {
    if(aTask.getType().equals("subflow")) {
      return this;
    }
    return null;
  }

}
