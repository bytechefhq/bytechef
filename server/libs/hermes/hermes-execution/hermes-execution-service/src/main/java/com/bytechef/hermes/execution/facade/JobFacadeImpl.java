
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

package com.bytechef.hermes.execution.facade;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.hermes.configuration.util.ComponentUtils;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.execution.dto.JobDTO;
import com.bytechef.hermes.execution.dto.TaskExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class JobFacadeImpl implements JobFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public JobFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService, JobService jobService,
        TaskExecutionService taskExecutionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public JobDTO getJob(long id) {
        return new JobDTO(jobService.getJob(id), getJobTaskExecutions(id));
    }

    @SuppressFBWarnings("NP")
    private List<TaskExecutionDTO> getJobTaskExecutions(long jobId) {
        return taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .map(taskExecution -> new TaskExecutionDTO(
                getComponentDefinition(taskExecution),
                contextService.peek(Objects.requireNonNull(taskExecution.getId()), Context.Classname.TASK_EXECUTION),
                taskExecutionService.getTaskExecution(taskExecution.getId())))
            .toList();
    }

    private ComponentDefinitionDTO getComponentDefinition(TaskExecution taskExecution) {
        ComponentUtils.ComponentType componentType = ComponentUtils.getComponentType(taskExecution.getType());

        return componentDefinitionService.getComponentDefinition(
            componentType.componentName(), componentType.componentVersion());
    }
}
