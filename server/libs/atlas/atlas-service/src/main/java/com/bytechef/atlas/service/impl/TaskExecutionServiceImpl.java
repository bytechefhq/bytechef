
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
import java.util.Optional;

import org.springframework.data.jdbc.core.mapping.AggregateReference;
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
        taskExecution.setNew(true);

        return taskExecutionRepository.save(taskExecution);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskExecution getTaskExecution(String id) {
        Assert.notNull(id, "id cannot be null.");

        return taskExecutionRepository.findById(id)
            .orElseThrow();
    }

    @Override
    public List<TaskExecution> getJobTaskExecutions(String jobId) {
        Assert.notNull(jobId, "jobId cannot be null.");

        return taskExecutionRepository.findAllByJobOrderByCreatedDate(
            new AggregateReference.IdOnlyAggregateReference<>(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskExecution> getParentTaskExecutions(String parentId) {
        Assert.notNull(parentId, "workflparentIdow cannot be null.");

        return taskExecutionRepository.findAllByParent(new AggregateReference.IdOnlyAggregateReference<>(parentId));
    }

    @Override
    public TaskExecution update(TaskExecution taskExecution) {
        Assert.notNull(taskExecution, "taskExecution cannot be null.");

        Optional<TaskExecution> currentTaskExecutionOptional = taskExecutionRepository
            .findByIdForUpdate(taskExecution.getId());

        if (currentTaskExecutionOptional.isPresent()) {
            TaskExecution currentTaskExecution = currentTaskExecutionOptional.get();

            if (currentTaskExecution.getStatus()
                .isTerminated() && taskExecution.getStatus() == TaskStatus.STARTED) {
                taskExecution = new TaskExecution(currentTaskExecution);

                taskExecution.setStartTime(taskExecution.getStartTime());
            } else if (taskExecution.getStatus()
                .isTerminated()
                && currentTaskExecution.getStatus() == TaskStatus.STARTED) {
                taskExecution.setStartTime(currentTaskExecution.getStartTime());
            }
        }

        return taskExecutionRepository.save(taskExecution);
    }
}
