/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.task.web.graphql;

import com.bytechef.automation.task.dto.TaskDTO;
import com.bytechef.automation.task.facade.TaskFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
public class TaskGraphQlController {

    private final TaskFacade taskFacade;

    @SuppressFBWarnings("EI")
    public TaskGraphQlController(TaskFacade taskFacade) {
        this.taskFacade = taskFacade;
    }

    @MutationMapping
    public TaskDTO createTask(@Argument TaskInput task) {
        return taskFacade.createTask(toTaskDTO(task));
    }

    @MutationMapping
    public boolean deleteTask(@Argument long id) {
        taskFacade.deleteTask(id);

        return true;
    }

    @QueryMapping
    public TaskDTO task(@Argument long id) {
        return taskFacade.getTask(id);
    }

    @QueryMapping
    public List<TaskDTO> tasks() {
        return taskFacade.getTasks();
    }

    @QueryMapping
    public List<TaskDTO> tasksByIds(@Argument List<Long> ids) {
        return taskFacade.getTasks(ids);
    }

    @MutationMapping
    public TaskDTO updateTask(@Argument TaskInput task) {
        return taskFacade.updateTask(toTaskDTO(task));
    }

    private TaskDTO toTaskDTO(TaskInput taskInput) {
        return new TaskDTO(
            null,
            null,
            taskInput.description(),
            taskInput.id(),
            null,
            null,
            taskInput.name(),
            taskInput.version() == null ? 0 : taskInput.version());
    }

    // Only name and description fields are considered when creating/updating tasks
    record TaskInput(Long id, String name, String description, Integer version) {
    }
}
