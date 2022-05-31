/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.task.auth.dispatcher;

import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.task.auth.service.TaskAuthService;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TaskAuthTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private final TaskAuthService taskAuthService;

    public TaskAuthTaskDispatcherPreSendProcessor(TaskAuthService taskAuthService) {
        this.taskAuthService = taskAuthService;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        SimpleTaskExecution simpleTaskExecution = SimpleTaskExecution.of(taskExecution);

        Map<String, Object> taskAuthMap = taskExecution.getMap(Constants.AUTH);

        if (taskAuthMap != null) {
            simpleTaskExecution.set(
                Constants.AUTH,
                taskAuthService.getTaskAuth((String) taskAuthMap.get(Constants.ID))
            );
        }

        return simpleTaskExecution;
    }
}
