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
package com.creactiviti.piper.taskhandler.time;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.creactiviti.piper.core.task.TaskExecution;
import com.creactiviti.piper.core.task.TaskHandler;

@Component("time/sleep")
public class Sleep implements TaskHandler<Object> {

  @Override
  public Object handle (TaskExecution aTask) throws InterruptedException {
    if(aTask.containsKey("millis")) {
      Thread.sleep(aTask.getLong("millis"));
    }
    else if (aTask.containsKey("duration")) {
      Thread.sleep(aTask.getDuration("duration").toMillis());
    }
    else {
      TimeUnit.SECONDS.sleep(1);
    }
    return null;
  }

}
