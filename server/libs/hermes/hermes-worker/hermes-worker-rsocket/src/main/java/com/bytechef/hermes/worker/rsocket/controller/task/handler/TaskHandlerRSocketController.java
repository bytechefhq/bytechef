
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

package com.bytechef.hermes.worker.rsocket.controller.task.handler;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerAccessor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@Controller
public class TaskHandlerRSocketController {

    private final TaskHandlerAccessor taskHandlerAccessor;

    public TaskHandlerRSocketController(TaskHandlerAccessor taskHandlerAccessor) {
        this.taskHandlerAccessor = taskHandlerAccessor;
    }

    @MessageMapping("TaskHandler.handle")
    public Mono<Object> handle(TaskHandlerHandleRequest taskHandlerHandleRequest) {
        TaskHandler<?> taskHandler = taskHandlerAccessor.getTaskHandler(taskHandlerHandleRequest.type());

        try {
            Object output = taskHandler.handle(taskHandlerHandleRequest.taskExecution());

            if (output == null) {
                return Mono.empty();
            } else {
                return Mono.just(output);
            }
        } catch (TaskExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    record TaskHandlerHandleRequest(String type, TaskExecution taskExecution) {
    }
}
