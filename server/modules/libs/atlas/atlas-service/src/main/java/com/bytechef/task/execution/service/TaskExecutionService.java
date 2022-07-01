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

package com.bytechef.task.execution.service;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.repository.TaskExecutionRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
public class TaskExecutionService {

    private final TaskExecutionRepository taskExecutionRepository;

    public TaskExecutionService(TaskExecutionRepository taskExecutionRepository) {
        this.taskExecutionRepository = taskExecutionRepository;
    }

    public void create(TaskExecution taskExecution) {
        taskExecutionRepository.create(taskExecution);
    }

    @Transactional(readOnly = true)
    public TaskExecution getTaskExecution(String id) {
        return taskExecutionRepository.findOne(id);
    }

    @Transactional(readOnly = true)
    public List<TaskExecution> getParentTaskExecutions(String parentId) {
        return taskExecutionRepository.findAllByParentId(parentId);
    }

    public TaskExecution merge(TaskExecution taskExecution) {
        return taskExecutionRepository.merge(taskExecution);
    }
}
