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

package com.bytechef.platform.workflow.test.executor;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.definition.WebhookResponse;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.coordinator.job.JobSyncExecutor;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.webhook.executor.constant.WebhookConstants;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
public class JobTestExecutor {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final Evaluator evaluator;
    private final JobService jobService;
    private final JobSyncExecutor jobSyncExecutor;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public JobTestExecutor(
        ComponentDefinitionService componentDefinitionService, ContextService contextService, Evaluator evaluator,
        JobService jobService, JobSyncExecutor jobSyncExecutor,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.evaluator = evaluator;
        this.jobService = jobService;
        this.jobSyncExecutor = jobSyncExecutor;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    public JobDTO execute(JobParametersDTO jobParametersDTO) {
        Job job = jobSyncExecutor.execute(jobParametersDTO, false);

        try {
            return new JobDTO(
                job, getOutputs(job),
                CollectionUtils.map(
                    taskExecutionService.getJobTaskExecutions(Validate.notNull(job.getId(), "id")),
                    taskExecution -> {
                        Map<String, ?> context = taskFileStorage.readContextValue(
                            contextService.peek(
                                Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION));

                        WorkflowTask workflowTask = taskExecution.getWorkflowTask();
                        DefinitionResult definitionResult = getDefinition(taskExecution);

                        Object output = taskExecution.getOutput() == null
                            ? null
                            : taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput());

                        return new TaskExecutionDTO(
                            taskExecution, definitionResult.title(), definitionResult.icon(),
                            workflowTask.evaluateParameters(context, evaluator), output);
                    }));
        } finally {
            jobService.deleteJob(Validate.notNull(job.getId(), "id"));
        }
    }

    private DefinitionResult getDefinition(TaskExecution taskExecution) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(taskExecution.getType());

        if (componentDefinitionService.hasComponentDefinition(
            workflowNodeType.name(), workflowNodeType.version())) {

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                workflowNodeType.name(), workflowNodeType.version());

            return new DefinitionResult(componentDefinition.getTitle(), componentDefinition.getIcon());
        }

        TaskDispatcherDefinition taskDispatcherDefinition = taskDispatcherDefinitionService.getTaskDispatcherDefinition(
            workflowNodeType.name(), workflowNodeType.version());

        return new DefinitionResult(taskDispatcherDefinition.getTitle(), taskDispatcherDefinition.getIcon());
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getOutputs(Job job) {
        Map<String, ?> outputs = null;

        if (job.getOutputs() != null) {
            outputs = taskFileStorage.readJobOutputs(job.getOutputs());

            if (outputs.containsKey(WebhookConstants.WEBHOOK_RESPONSE)) {
                WebhookResponse webhookResponse = MapUtils.getRequired(
                    outputs, WebhookConstants.WEBHOOK_RESPONSE, new TypeReference<>() {});

                outputs = (Map<String, ?>) webhookResponse.getBody();
            } else {
                outputs = taskFileStorage.readContextValue(job.getOutputs());
            }
        }

        return outputs;
    }

    record DefinitionResult(String title, String icon) {
    }
}
