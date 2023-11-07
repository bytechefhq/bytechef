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

package com.bytechef.hermes.webhook.executor;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.hermes.coordinator.event.TriggerWebhookEvent;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.facade.InstanceJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class WebhookExecutorImpl implements WebhookExecutor {

    private final ApplicationEventPublisher eventPublisher;
    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final InstanceJobFacade instanceJobFacade;
    private final JobSyncExecutor jobSyncExecutor;
    private final TriggerSyncExecutor triggerSyncExecutor;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public WebhookExecutorImpl(
        ApplicationEventPublisher eventPublisher, InstanceAccessorRegistry instanceAccessorRegistry,
        InstanceJobFacade instanceJobFacade, JobSyncExecutor jobSyncExecutor, TriggerSyncExecutor triggerSyncExecutor,
        TaskFileStorage taskFileStorage) {

        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.instanceJobFacade = instanceJobFacade;
        this.jobSyncExecutor = jobSyncExecutor;
        this.eventPublisher = eventPublisher;
        this.triggerSyncExecutor = triggerSyncExecutor;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public Object execute(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        Object outputs;

        TriggerOutput triggerOutput = triggerSyncExecutor.execute(workflowExecutionId, webhookRequest);

        Map<String, ?> inputMap = getInputMap(workflowExecutionId);

        if (!triggerOutput.batch() && triggerOutput.value() instanceof Collection<?> collectionOutput) {
            List<Map<String, ?>> outputsList = new ArrayList<>();

            for (Object outputItem : collectionOutput) {
                Job job = jobSyncExecutor.execute(
                    createJobParameters(workflowExecutionId, inputMap, outputItem),
                    (jobParameters, workflow) -> instanceJobFacade.createJob(
                        jobParameters, workflow, workflowExecutionId.getInstanceId(), workflowExecutionId.getType()));

                outputsList.add(taskFileStorage.readJobOutputs(job.getOutputs()));
            }

            return outputsList;
        } else {
            Job job = jobSyncExecutor.execute(
                createJobParameters(workflowExecutionId, inputMap, triggerOutput.value()),
                (jobParameters, workflow) -> instanceJobFacade.createJob(
                    jobParameters, workflow, workflowExecutionId.getInstanceId(), workflowExecutionId.getType()));

            outputs = job.getOutputs() == null ? null : taskFileStorage.readJobOutputs(job.getOutputs());
        }

        return outputs;
    }

    @Override
    public void executeAsync(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        eventPublisher.publishEvent(
            new TriggerWebhookEvent(new TriggerWebhookEvent.WebhookParameters(workflowExecutionId, webhookRequest)));
    }

    @Override
    public boolean validateAndExecuteAsync(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        boolean valid = triggerSyncExecutor.validate(workflowExecutionId, webhookRequest);

        if (valid) {
            executeAsync(workflowExecutionId, webhookRequest);
        }

        return valid;
    }

    @SuppressWarnings("unchecked")
    private static JobParameters createJobParameters(
        WorkflowExecutionId workflowExecutionId, Map<String, ?> inputMap, Object outputItem) {

        return new JobParameters(
            workflowExecutionId.getWorkflowId(),
            MapUtils.concat((Map<String, Object>) inputMap, Map.of(workflowExecutionId.getTriggerName(), outputItem)));
    }

    private Map<String, ?> getInputMap(WorkflowExecutionId workflowExecutionId) {
        InstanceAccessor instanceAccessor = instanceAccessorRegistry
            .getInstanceAccessor(workflowExecutionId.getType());

        return instanceAccessor.getInputMap(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId());
    }
}
