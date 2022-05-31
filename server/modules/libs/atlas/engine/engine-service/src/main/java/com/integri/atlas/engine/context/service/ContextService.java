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

package com.integri.atlas.engine.context.service;

import com.integri.atlas.engine.context.Context;
import com.integri.atlas.engine.context.repository.ContextRepository;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.task.execution.repository.TaskExecutionRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ContextService {

    private final ContextRepository contextRepository;
    private final TaskExecutionRepository taskExecutionRepository;

    public ContextService(ContextRepository contextRepository, TaskExecutionRepository taskExecutionRepository) {
        this.contextRepository = contextRepository;
        this.taskExecutionRepository = taskExecutionRepository;
    }

    public void deleteJobContext(String jobId) {
        List<TaskExecution> taskExecutions = taskExecutionRepository.findAllByJobIdOrderByTaskNumber(jobId);

        for (TaskExecution taskExecution : taskExecutions) {
            deleteTaskExecutionContext(taskExecution);
        }

        contextRepository.delete(jobId);
    }

    public void push(String stackId, Context context) {
        contextRepository.push(stackId, context);
    }

    @Transactional(readOnly = true)
    public Context peek(String stackId) {
        return contextRepository.peek(stackId);
    }

    private void deleteTaskExecutionContext(TaskExecution taskExecution) {
        List<TaskExecution> taskExecutions = taskExecutionRepository.findAllByParentId(taskExecution.getId());

        for (TaskExecution curTaskExecution : taskExecutions) {
            deleteTaskExecutionContext(curTaskExecution);
        }

        contextRepository.delete(taskExecution.getId());
    }
}
