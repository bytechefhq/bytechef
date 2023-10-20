
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

package com.bytechef.hermes.coordinator.trigger.completion;

import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.coordinator.instance.InstanceWorkflowAccessor;
import com.bytechef.hermes.coordinator.instance.InstanceWorkflowAccessorRegistry;
import com.bytechef.hermes.execution.domain.TriggerExecution.Status;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.service.TriggerExecutionService;
import com.bytechef.hermes.execution.service.TriggerStateService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerCompletionHandler {

    private final InstanceWorkflowAccessorRegistry instanceWorkflowAccessorRegistry;
    private final JobFacade jobFacade;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerStateService triggerStateService;

    public TriggerCompletionHandler(
        InstanceWorkflowAccessorRegistry instanceWorkflowAccessorRegistry, JobFacade jobFacade,
        TriggerExecutionService triggerExecutionService, TriggerStateService triggerStateService) {

        this.instanceWorkflowAccessorRegistry = instanceWorkflowAccessorRegistry;
        this.jobFacade = jobFacade;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerStateService = triggerStateService;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public void handle(TriggerExecution triggerExecution) {
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        triggerExecution.setStatus(Status.COMPLETED);

        if (triggerExecution.getId() == null) {
            triggerExecutionService.create(triggerExecution);
        } else {
            triggerExecutionService.update(triggerExecution);
        }

        if (triggerExecution.getState() != null) {
            triggerStateService.save(workflowExecutionId, triggerExecution.getState());
        }

        InstanceWorkflowAccessor instanceWorkflowAccessor =
            instanceWorkflowAccessorRegistry.getInstanceWorkflowAccessor(
                workflowExecutionId.getInstanceType());

        Map<String, Object> inputs = (Map<String, Object>) instanceWorkflowAccessor.getInputs(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId());

        if (!triggerExecution.isBatch() && triggerExecution.getOutput() instanceof Collection<?> collectionOutput) {
            for (Object outputItem : collectionOutput) {
                createJob(
                    workflowExecutionId, MapUtils.concat(inputs, Map.of(triggerExecution.getName(), outputItem)));
            }
        } else {
            createJob(
                workflowExecutionId,
                MapUtils.concat(inputs, Map.of(triggerExecution.getName(), triggerExecution.getOutput())));
        }
    }

    private void createJob(WorkflowExecutionId workflowExecutionId, Map<String, ?> inputs) {
        jobFacade.createJob(new JobParameters(workflowExecutionId.getWorkflowId(), inputs));
    }
}
