
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

package com.bytechef.hermes.coordinator.trigger.completion;

import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.hermes.execution.domain.TriggerExecution.Status;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.service.TriggerExecutionService;
import com.bytechef.hermes.execution.service.TriggerStateService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerCompletionHandler {

    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final JobFacade jobFacade;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerStateService triggerStateService;

    public TriggerCompletionHandler(
        InstanceAccessorRegistry instanceAccessorRegistry, JobFacade jobFacade,
        TriggerExecutionService triggerExecutionService, TriggerStateService triggerStateService) {

        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.jobFacade = jobFacade;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerStateService = triggerStateService;
    }

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

        InstanceAccessor instanceAccessor =
            instanceAccessorRegistry.getInstanceAccessor(
                workflowExecutionId.getInstanceType());

        Map<String, Object> inputMap = (Map<String, Object>) instanceAccessor.getInputMap(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId());
        Map<String, ?> metadata = Map.of(
            MetadataConstants.INSTANCE_ID, workflowExecutionId.getInstanceId(),
            MetadataConstants.INSTANCE_TYPE, workflowExecutionId.getInstanceType());

        if (!triggerExecution.isBatch() && triggerExecution.getOutput() instanceof Collection<?> collectionOutput) {
            for (Object outputItem : collectionOutput) {
                createJob(
                    workflowExecutionId, MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), outputItem)),
                    metadata);
            }
        } else {
            createJob(
                workflowExecutionId,
                MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), triggerExecution.getOutput())), metadata);
        }
    }

    private void createJob(WorkflowExecutionId workflowExecutionId, Map<String, ?> inpputMap, Map<String, ?> metadata) {
        jobFacade.createJob(new JobParameters(workflowExecutionId.getWorkflowId(), inpputMap, metadata));
    }
}
