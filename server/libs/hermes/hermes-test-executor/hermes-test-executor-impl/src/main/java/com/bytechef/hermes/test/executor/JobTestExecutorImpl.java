
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

package com.bytechef.hermes.test.executor;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.WorkflowFileStorage;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.execution.dto.TaskExecutionDTO;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.execution.dto.JobDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class JobTestExecutorImpl implements JobTestExecutor {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final JobSyncExecutor jobSyncExecutor;
    private final TaskExecutionService taskExecutionService;
    private final WorkflowFileStorage workflowFileStorage;

    @SuppressFBWarnings("EI")
    public JobTestExecutorImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        JobSyncExecutor jobSyncExecutor, TaskExecutionService taskExecutionService,
        WorkflowFileStorage workflowFileStorage) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.jobSyncExecutor = jobSyncExecutor;
        this.taskExecutionService = taskExecutionService;
        this.workflowFileStorage = workflowFileStorage;
    }

    @Override
    @SuppressFBWarnings("NP")
    public JobDTO execute(JobParameters jobParameters) {
        Job job = jobSyncExecutor.execute(jobParameters);

        return new JobDTO(
            job,
            workflowFileStorage.readContextValue(job.getOutputs()),
            CollectionUtils.map(
                taskExecutionService.getJobTaskExecutions(Objects.requireNonNull(job.getId())),
                taskExecution -> new TaskExecutionDTO(
                    getComponentDefinition(taskExecution),
                    workflowFileStorage.readContextValue(
                        contextService.peek(
                            Objects.requireNonNull(taskExecution.getId()), Context.Classname.TASK_EXECUTION)),
                    workflowFileStorage.readTaskExecutionOutput(taskExecution.getOutput()),
                    taskExecution)));
    }

    private ComponentDefinition getComponentDefinition(TaskExecution taskExecution) {
        ComponentOperation componentOperation = ComponentOperation.ofType(taskExecution.getType());

        return componentDefinitionService.getComponentDefinition(
            componentOperation.componentName(), componentOperation.componentVersion());
    }
}
