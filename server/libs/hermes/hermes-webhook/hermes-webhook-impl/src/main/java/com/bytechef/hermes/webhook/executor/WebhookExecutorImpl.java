
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

package com.bytechef.hermes.webhook.executor;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.sync.executor.JobSyncExecutor;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.coordinator.TriggerCoordinator;
import com.bytechef.hermes.coordinator.instance.InstanceWorkflowAccessor;
import com.bytechef.hermes.coordinator.instance.InstanceWorkflowAccessorRegistry;
import com.bytechef.hermes.definition.registry.component.trigger.TriggerOutput;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.definition.registry.component.trigger.WebhookRequest;
import com.bytechef.hermes.execution.message.broker.TriggerMessageRoute;
import com.bytechef.message.broker.MessageBroker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WebhookExecutorImpl implements WebhookExecutor {

    private final InstanceWorkflowAccessorRegistry instanceWorkflowAccessorRegistry;
    private final JobFacade jobFacade;
    private final JobSyncExecutor jobSyncExecutor;
    private final MessageBroker messageBroker;
    private final TriggerSyncExecutor triggerSyncExecutor;

    @SuppressFBWarnings("EI")
    public WebhookExecutorImpl(
        InstanceWorkflowAccessorRegistry instanceWorkflowAccessorRegistry, JobFacade jobFacade,
        JobSyncExecutor jobSyncExecutor, MessageBroker messageBroker, TriggerSyncExecutor triggerSyncExecutor) {

        this.instanceWorkflowAccessorRegistry = instanceWorkflowAccessorRegistry;
        this.jobFacade = jobFacade;
        this.jobSyncExecutor = jobSyncExecutor;
        this.messageBroker = messageBroker;
        this.triggerSyncExecutor = triggerSyncExecutor;
    }

    @Override
    public Object execute(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        Object outputs;

        TriggerOutput triggerOutput = triggerSyncExecutor.execute(workflowExecutionId, webhookRequest);

        InstanceWorkflowAccessor instanceWorkflowAccessor =
            instanceWorkflowAccessorRegistry.getInstanceWorkflowAccessor(
                workflowExecutionId.getInstanceType());

        Map<String, ?> inputs = instanceWorkflowAccessor.getInputs(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId());

        if (!triggerOutput.batch() && triggerOutput.value() instanceof Collection<?> collectionOutput) {
            List<Map<String, ?>> outputsList = new ArrayList<>();

            for (Object outputItem : collectionOutput) {
                Job job = jobSyncExecutor.execute(createJobParameters(workflowExecutionId, inputs, outputItem));

                outputsList.add(job.getOutputs());
            }

            return outputsList;
        } else {
            Job job = jobSyncExecutor.execute(createJobParameters(workflowExecutionId, inputs, triggerOutput.value()));

            outputs = job.getOutputs();
        }

        return outputs;
    }

    @Override
    public void executeAsync(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        messageBroker.send(
            TriggerMessageRoute.WEBHOOKS,
            new TriggerCoordinator.WebhookParameters(workflowExecutionId, webhookRequest));
    }

    @Override
    public boolean validateAndExecute(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        boolean valid = triggerSyncExecutor.validate(workflowExecutionId, webhookRequest);

        if (valid) {
            jobFacade.createJob(
                new JobParameters(workflowExecutionId.getWorkflowId(), getInputs(workflowExecutionId)));
        }

        return valid;
    }

    @SuppressWarnings("unchecked")
    private static JobParameters createJobParameters(
        WorkflowExecutionId workflowExecutionId, Map<String, ?> inputs, Object outputItem) {

        return new JobParameters(
            workflowExecutionId.getWorkflowId(),
            MapUtils.concat(
                (Map<String, Object>) inputs, Map.of(workflowExecutionId.getWorkflowTriggerName(), outputItem)));
    }

    private Map<String, ?> getInputs(WorkflowExecutionId workflowExecutionId) {
        InstanceWorkflowAccessor instanceWorkflowAccessor = instanceWorkflowAccessorRegistry
            .getInstanceWorkflowAccessor(workflowExecutionId.getInstanceType());

        return instanceWorkflowAccessor.getInputs(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId());
    }
}
