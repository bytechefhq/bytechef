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

package com.bytechef.platform.webhook.executor;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.HttpStatus;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.coordinator.job.JobSyncExecutor;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent.WebhookParameters;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public class WebhookWorkflowExecutorImpl implements WebhookWorkflowExecutor {

    private final ApplicationEventPublisher eventPublisher;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final PrincipalJobFacade principalJobFacade;
    private final JobSyncExecutor jobSyncExecutor;
    private final WebhookWorkflowSyncExecutor webhookWorkflowSyncExecutor;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public WebhookWorkflowExecutorImpl(
        ApplicationEventPublisher eventPublisher, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        PrincipalJobFacade principalJobFacade, JobSyncExecutor jobSyncExecutor,
        WebhookWorkflowSyncExecutor webhookWorkflowSyncExecutor, TaskFileStorage taskFileStorage) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.principalJobFacade = principalJobFacade;
        this.jobSyncExecutor = jobSyncExecutor;
        this.eventPublisher = eventPublisher;
        this.webhookWorkflowSyncExecutor = webhookWorkflowSyncExecutor;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void execute(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        eventPublisher.publishEvent(
            new TriggerWebhookEvent(new WebhookParameters(workflowExecutionId, webhookRequest)));
    }

    @Override
    @Nullable
    public Object executeSync(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        Object outputs;

        TriggerOutput triggerOutput = webhookWorkflowSyncExecutor.execute(workflowExecutionId, webhookRequest);

        Map<String, ?> inputMap = getInputMap(workflowExecutionId);
        String workflowId = getWorkflowId(workflowExecutionId);

        if (!triggerOutput.batch() && triggerOutput.value() instanceof Collection<?> triggerOutputValues) {
            List<Map<String, ?>> outputsList = new ArrayList<>();

            for (Object triggerOutputValue : triggerOutputValues) {
                Job job = jobSyncExecutor.execute(
                    createJobParameters(workflowExecutionId, workflowId, inputMap, triggerOutputValue),
                    jobParameters -> principalJobFacade.createSyncJob(
                        jobParameters, workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getType()));

                outputsList.add(taskFileStorage.readJobOutputs(job.getOutputs()));
            }

            outputs = outputsList;
        } else {
            Job job = jobSyncExecutor.execute(
                createJobParameters(workflowExecutionId, workflowId, inputMap, triggerOutput.value()),
                jobParameters -> principalJobFacade.createSyncJob(
                    jobParameters, workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getType()));

            outputs = job.getOutputs() == null ? null : taskFileStorage.readJobOutputs(job.getOutputs());
        }

        return outputs;
    }

    @Override
    public WebhookValidateResponse validateAndExecuteAsync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        WebhookValidateResponse response = webhookWorkflowSyncExecutor.validate(workflowExecutionId, webhookRequest);

        if (response.status() == HttpStatus.OK.getValue()) {
            execute(workflowExecutionId, webhookRequest);
        }

        return response;
    }

    @Override
    public WebhookValidateResponse validateOnEnable(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        return webhookWorkflowSyncExecutor.validateOnEnable(workflowExecutionId, webhookRequest);
    }

    @SuppressWarnings("unchecked")
    private static JobParametersDTO createJobParameters(
        WorkflowExecutionId workflowExecutionId, String workflowId, Map<String, ?> inputMap,
        Object triggerOutputValue) {

        return new JobParametersDTO(
            workflowId,
            MapUtils.concat(
                (Map<String, Object>) inputMap, Map.of(workflowExecutionId.getTriggerName(), triggerOutputValue)));
    }

    private Map<String, ?> getInputMap(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        return jobPrincipalAccessor.getInputMap(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    private String getWorkflowId(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        return jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }
}
