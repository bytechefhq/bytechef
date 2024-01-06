/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.hermes.test.executor;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.component.registry.OperationType;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.execution.dto.JobDTO;
import com.bytechef.hermes.execution.dto.TaskExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
public class JobTestExecutor {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final JobSyncExecutor jobSyncExecutor;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public JobTestExecutor(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        JobSyncExecutor jobSyncExecutor, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.jobSyncExecutor = jobSyncExecutor;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    public JobDTO execute(JobParameters jobParameters) {
        Job job = jobSyncExecutor.execute(jobParameters);

        return new JobDTO(
            job,
            job.getOutputs() == null ? null : taskFileStorage.readContextValue(job.getOutputs()),
            CollectionUtils.map(
                taskExecutionService.getJobTaskExecutions(Validate.notNull(job.getId(), "id")),
                taskExecution -> new TaskExecutionDTO(
                    getComponentDefinition(taskExecution),
                    taskFileStorage.readContextValue(
                        contextService.peek(
                            Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION)),
                    taskExecution.getOutput() == null
                        ? null
                        : taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()),
                    taskExecution)));
    }

    private ComponentDefinition getComponentDefinition(TaskExecution taskExecution) {
        OperationType operationType = OperationType.ofType(taskExecution.getType());

        return componentDefinitionService.getComponentDefinition(
            operationType.componentName(), operationType.componentVersion());
    }
}
