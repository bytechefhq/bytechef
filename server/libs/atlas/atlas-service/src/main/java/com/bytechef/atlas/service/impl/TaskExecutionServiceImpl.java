
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

package com.bytechef.atlas.service.impl;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.repository.TaskExecutionRepository;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.execution.TaskStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Transactional
public class TaskExecutionServiceImpl implements TaskExecutionService {

    private final TaskExecutionRepository taskExecutionRepository;

    @SuppressFBWarnings("EI2")
    public TaskExecutionServiceImpl(TaskExecutionRepository taskExecutionRepository) {
        this.taskExecutionRepository = taskExecutionRepository;
    }

    @Override
    public TaskExecution create(TaskExecution taskExecution) {
        Assert.notNull(taskExecution, "'taskExecution' must not be null.");

        taskExecution.setId(null);

        return taskExecutionRepository.save(taskExecution);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskExecution getTaskExecution(long id) {
        return taskExecutionRepository.findById(id)
            .orElseThrow();
    }

    @Override
    public List<TaskExecution> getJobTaskExecutions(long jobId) {
        return taskExecutionRepository.findAllByJobRefOrderByCreatedDate(AggregateReference.to(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskExecution> getParentTaskExecutions(long parentId) {
        return taskExecutionRepository.findAllByParentRef(AggregateReference.to(parentId));
    }

    @Override
    @SuppressFBWarnings("NP")
    public TaskExecution update(@NonNull TaskExecution taskExecution) {
        Assert.notNull(taskExecution, "'taskExecution' must not be null.");

        TaskExecution currentTaskExecution = taskExecutionRepository.findByIdForUpdate(taskExecution.getId())
            .orElseThrow();

        TaskStatus currentTaskStatus = currentTaskExecution.getStatus();
        TaskStatus taskStatus = taskExecution.getStatus();

        if (currentTaskStatus.isTerminated() && taskStatus == TaskStatus.STARTED) {
            currentTaskExecution.setStartTime(taskExecution.getStartTime());

            taskExecution = currentTaskExecution;
        } else if (taskStatus.isTerminated() && currentTaskExecution.getStatus() == TaskStatus.STARTED) {
            taskExecution.setStartTime(currentTaskExecution.getStartTime());
        }

        return taskExecutionRepository.save(taskExecution);
    }
}
