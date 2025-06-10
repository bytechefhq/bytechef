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

package com.bytechef.automation.task.service;

import com.bytechef.automation.task.domain.Task;
import com.bytechef.automation.task.repository.TaskRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public long countTasks() {
        return taskRepository.count();
    }

    @Override
    public Task create(Task task) {
        Assert.notNull(task, "'task' must not be null");
        Assert.isTrue(task.getId() == null, "'id' must be null");
        Assert.notNull(task.getName(), "'name' must not be null");

        return taskRepository.save(task);
    }

    @Override
    public void delete(long id) {
        taskRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Task> fetchTask(String name) {
        return taskRepository.findByNameIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Task getTask(long id) {
        return OptionalUtils.get(taskRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> getTasks() {
        return CollectionUtils.toList(taskRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> getTasks(List<Long> ids) {
        return CollectionUtils.toList(taskRepository.findAllById(ids));
    }

    @Override
    public Task update(Task task) {
        Assert.notNull(task, "'task' must not be null");
        Assert.notNull(task.getId(), "'id' must not be null");
        Assert.notNull(task.getName(), "'name' must not be null");

        Task curTask = getTask(task.getId());

        curTask.setDescription(task.getDescription());
        curTask.setName(task.getName());
        curTask.setVersion(task.getVersion());

        return taskRepository.save(curTask);
    }
}
