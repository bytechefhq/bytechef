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
package com.creactiviti.piper.core.event;

import com.creactiviti.piper.core.task.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @since Sep 06, 2018
 */
public class TaskProgressedEventListener implements EventListener {

  private final TaskExecutionRepository taskExecutionRepository;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public TaskProgressedEventListener(TaskExecutionRepository aTaskExecutionRepository) {
    taskExecutionRepository = aTaskExecutionRepository;
  }

  @Override
  public void onApplicationEvent (PiperEvent aEvent) {
    if(Events.TASK_PROGRESSED.equals(aEvent.getType())) {
      String taskId = aEvent.getString("taskId");
      int progress = aEvent.getInteger("progress", 0);

      TaskExecution task = taskExecutionRepository.findOne(taskId);

      if(task == null) {
        logger.error("Unknown task: {}",taskId);
      } else {
        SimpleTaskExecution mtask = SimpleTaskExecution.createForUpdate(task);

        mtask.setProgress(progress);

        taskExecutionRepository.merge(mtask);
      }
    }
  }

}
