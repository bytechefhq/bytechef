
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

package com.bytechef.atlas.rsocket;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@Controller
public class TaskExecutionRSocketController {

    private TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public TaskExecutionRSocketController(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    @MessageMapping("createTaskExecution")
    public Mono<TaskExecution> createTaskExecution(TaskExecution taskExecution) {
        return Mono.create(sink -> sink.success(taskExecutionService.add(taskExecution)));
    }

    @MessageMapping("getTaskExecution")
    public Mono<TaskExecution> getTaskExecution(String id) {
        return Mono.create(sink -> sink.success(taskExecutionService.getTaskExecution(id)));
    }

    @MessageMapping("getParentTaskExecutions")
    public Mono<List<TaskExecution>> getParentTaskExecutions(String parentId) {
        return Mono.create(sink -> sink.success(taskExecutionService.getParentTaskExecutions(parentId)));
    }

    @MessageMapping("updateTaskExecution")
    public Mono<TaskExecution> updateTaskExecution(TaskExecution taskExecution) {
        return Mono.create(sink -> sink.success(taskExecutionService.update(taskExecution)));
    }
}
