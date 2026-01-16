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

package com.bytechef.atlas.execution.service;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
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
        Validate.notNull(taskExecution, "'taskExecution' must not be null");
        Validate.isTrue(taskExecution.getId() == null, "'taskExecution.id' must be null");
        Validate.notNull(taskExecution.getWorkflowTask(), "'taskExecution.workflowTask' must not be null");

        return taskExecutionRepository.save(taskExecution);
    }

    @Override
    public void delete(long id) {
        taskExecutionRepository.findById(id);
    }

    @Override
    public void deleteJobTaskExecutions(long jobId) {
        List<TaskExecution> taskExecutions = taskExecutionRepository.findAllByJobIdOrderByIdDesc(jobId);

        for (TaskExecution taskExecution : taskExecutions) {
            taskExecutionRepository.deleteById(Validate.notNull(taskExecution.getId(), "id"));
        }
    }

    @Override
    public Optional<TaskExecution> fetchLastJobTaskExecution(long jobId) {
        return taskExecutionRepository.findLastByJobId(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskExecution getTaskExecution(long id) {
        return taskExecutionRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException(String.format("TaskExecution with id '%s' not found", id)));
    }

    @Override
    public TaskExecution getTaskExecutionForUpdate(long id) {
        return taskExecutionRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new IllegalStateException(String.format("TaskExecution with id '%s' not found", id)));
    }

    @Override
    public List<TaskExecution> getJobTaskExecutions(long jobId) {
        return taskExecutionRepository.findAllByJobIdOrderByCreatedDate(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskExecution> getParentTaskExecutions(long parentId) {
        return taskExecutionRepository.findAllByParentIdOrderByTaskNumber(parentId);
    }

    @Override
    public TaskExecution update(TaskExecution taskExecution) {
        Validate.notNull(taskExecution, "'taskExecution' must not be null");

        long id = Validate.notNull(taskExecution.getId(), "id");

        TaskExecution currentTaskExecution = taskExecutionRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new IllegalStateException(String.format("TaskExecution with id '%s' not found", id)));

        try {
            TaskExecution.Status currentStatus = currentTaskExecution.getStatus();
            TaskExecution.Status status = taskExecution.getStatus();

            if (currentStatus.isTerminated() && status == TaskExecution.Status.STARTED) {
                currentTaskExecution.setStartDate(taskExecution.getStartDate());

                taskExecution = currentTaskExecution;
            } else if (status.isTerminated() && currentTaskExecution.getStatus() == TaskExecution.Status.STARTED) {
                taskExecution.setStartDate(currentTaskExecution.getStartDate());
            }

            // Do not override initial workflow task definition

            taskExecution.setWorkflowTask(currentTaskExecution.getWorkflowTask());

            return taskExecutionRepository.save(taskExecution);
        } finally {
            taskExecutionRepository.unlockForUpdate(id);
        }
    }
}
