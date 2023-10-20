
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

package com.bytechef.hermes.workflow.facade;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.util.ComponentUtils;
import com.bytechef.hermes.workflow.dto.TaskExecutionDTO;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class TaskExecutionFacadeImpl implements TaskExecutionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public TaskExecutionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        TaskExecutionService taskExecutionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public TaskExecutionDTO getTaskExecution(long id) {
        TaskExecution taskExecution = taskExecutionService.getTaskExecution(id);

        return new TaskExecutionDTO(
            getComponentDefinition(taskExecution), contextService.peek(id, Context.Classname.TASK_EXECUTION),
            taskExecution);
    }

    @Override
    @SuppressFBWarnings("NP")
    public List<TaskExecutionDTO> getJobTaskExecutions(long jobId) {
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
