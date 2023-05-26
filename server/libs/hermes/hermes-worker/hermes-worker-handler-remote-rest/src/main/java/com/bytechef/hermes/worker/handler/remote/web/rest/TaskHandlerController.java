
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

package com.bytechef.hermes.worker.handler.remote.web.rest;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerAccessor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
public class TaskHandlerController {

    private final TaskHandlerAccessor taskHandlerAccessor;

    public TaskHandlerController(TaskHandlerAccessor taskHandlerAccessor) {
        this.taskHandlerAccessor = taskHandlerAccessor;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/task-handler",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<Object> handle(@Valid @RequestBody TaskHandlerHandleRequest taskHandlerHandleRequest) {
        TaskHandler<?> taskHandler = taskHandlerAccessor.getTaskHandler(taskHandlerHandleRequest.type());

        try {
            Object output = taskHandler.handle(taskHandlerHandleRequest.taskExecution());

            if (output == null) {
                return ResponseEntity.noContent()
                    .build();
            } else {
                return ResponseEntity.ok(output);
            }
        } catch (TaskExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    record TaskHandlerHandleRequest(@NotNull String type, TaskExecution taskExecution) {
    }
}
