
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

package com.bytechef.hermes.workflow.executor;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.workflow.util.ComponentUtils;
import com.bytechef.hermes.workflow.dto.TaskExecutionDTO;
import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.workflow.dto.WorkflowResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class TestWorkflowExecutor implements WorkflowExecutor {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final JobSyncExecutor jobSyncExecutor;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public TestWorkflowExecutor(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        JobSyncExecutor jobSyncExecutor, TaskExecutionService taskExecutionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.jobSyncExecutor = jobSyncExecutor;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public WorkflowResponse execute(String workflowId) {
        return execute(workflowId, Map.of());
    }

    @Override
    @SuppressFBWarnings("NP")
    public WorkflowResponse execute(String workflowId, Map<String, Object> inputs) {
        Job job = jobSyncExecutor.execute(new JobParameters(inputs, workflowId));

        return new WorkflowResponse(
            job, CollectionUtils.map(
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
