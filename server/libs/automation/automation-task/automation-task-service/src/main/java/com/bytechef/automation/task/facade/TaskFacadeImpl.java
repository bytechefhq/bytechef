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

package com.bytechef.automation.task.facade;

import com.bytechef.automation.task.domain.Task;
import com.bytechef.automation.task.dto.TaskDTO;
import com.bytechef.automation.task.service.TaskService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class TaskFacadeImpl implements TaskFacade {

    private final TaskService taskService;

    @SuppressFBWarnings("EI")
    public TaskFacadeImpl(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public long countTasks() {
        return taskService.countTasks();
    }

    @Override
    public TaskDTO createTask(TaskDTO taskDTO) {
        Task task = toTask(taskDTO);

        return toTaskDTO(taskService.create(task));
    }

    @Override
    public void deleteTask(long id) {
        taskService.delete(id);
    }

    @Override
    public Optional<TaskDTO> fetchTask(String name) {
        return taskService.fetchTask(name)
            .map(this::toTaskDTO);
    }

    @Override
    public TaskDTO getTask(long id) {
        return toTaskDTO(taskService.getTask(id));
    }

    @Override
    public List<TaskDTO> getTasks() {
        return taskService.getTasks()
            .stream()
            .map(this::toTaskDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<TaskDTO> getTasks(List<Long> ids) {
        return taskService.getTasks(ids)
            .stream()
            .map(this::toTaskDTO)
            .collect(Collectors.toList());
    }

    @Override
    public TaskDTO updateTask(TaskDTO taskDTO) {
        Task task = toTask(taskDTO);

        return toTaskDTO(taskService.update(task));
    }

    private Task toTask(TaskDTO taskDTO) {
        return Task.builder()
            .id(taskDTO.id())
            .name(taskDTO.name())
            .description(taskDTO.description())
            .version(taskDTO.version())
            .build();
    }

    private TaskDTO toTaskDTO(Task task) {
        return new TaskDTO(
            task.getCreatedBy(),
            task.getCreatedDate(),
            task.getDescription(),
            task.getId(),
            task.getLastModifiedBy(),
            task.getLastModifiedDate(),
            task.getName(),
            task.getVersion());
    }
}
