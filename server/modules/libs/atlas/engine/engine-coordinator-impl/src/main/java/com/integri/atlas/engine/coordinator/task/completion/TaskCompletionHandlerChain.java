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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator.task.completion;

import com.integri.atlas.engine.core.task.TaskExecution;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arik Cohen
 */
public class TaskCompletionHandlerChain implements TaskCompletionHandler {

    private List<TaskCompletionHandler> taskCompletionHandlers = new ArrayList<>();

    @Override
    public void handle(TaskExecution taskExecution) {
        for (TaskCompletionHandler taskCompletionHandler : taskCompletionHandlers) {
            if (taskCompletionHandler.canHandle(taskExecution)) {
                taskCompletionHandler.handle(taskExecution);
            }
        }
    }

    @Override
    public boolean canHandle(TaskExecution aJobTask) {
        return true;
    }

    public void setTaskCompletionHandlers(List<TaskCompletionHandler> taskCompletionHandlers) {
        this.taskCompletionHandlers = taskCompletionHandlers;
    }
}
