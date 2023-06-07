
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

package com.bytechef.hermes.execution.sync;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.configuration.util.ComponentUtils;
import com.bytechef.hermes.execution.dto.TaskExecutionDTO;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.sync.JobSyncExecutor;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.execution.dto.JobDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class WorkflowTestExecutorImpl implements WorkflowTestExecutor {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final JobSyncExecutor jobSyncExecutor;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public WorkflowTestExecutorImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        JobSyncExecutor jobSyncExecutor, TaskExecutionService taskExecutionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.jobSyncExecutor = jobSyncExecutor;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public JobDTO execute(JobParameters jobParameters) {
        Job job = jobSyncExecutor.execute(jobParameters);

        return new JobDTO(
            job,
            CollectionUtils.map(
                taskExecutionService.getJobTaskExecutions(Objects.requireNonNull(job.getId())),
                taskExecution -> new TaskExecutionDTO(
                    getComponentDefinition(taskExecution),
                    contextService.peek(
                        Objects.requireNonNull(taskExecution.getId()), Context.Classname.TASK_EXECUTION),
                    taskExecution)));
    }

    private ComponentDefinitionDTO getComponentDefinition(TaskExecution taskExecution) {
        ComponentUtils.ComponentType componentType = ComponentUtils.getComponentType(taskExecution.getType());

        return componentDefinitionService.getComponentDefinition(
            componentType.componentName(), componentType.componentVersion());
    }
}
