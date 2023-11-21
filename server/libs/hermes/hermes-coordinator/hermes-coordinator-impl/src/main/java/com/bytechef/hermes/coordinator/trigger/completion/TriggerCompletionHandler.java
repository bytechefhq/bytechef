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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.domain.TriggerExecution.Status;
import com.bytechef.hermes.execution.facade.InstanceJobFacade;
import com.bytechef.hermes.execution.service.TriggerExecutionService;
import com.bytechef.hermes.execution.service.TriggerStateService;
import com.bytechef.hermes.file.storage.TriggerFileStorage;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerCompletionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TriggerCompletionHandler.class);

    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final InstanceJobFacade instanceJobFacade;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorage triggerFileStorage;
    private final TriggerStateService triggerStateService;

    public TriggerCompletionHandler(
        InstanceAccessorRegistry instanceAccessorRegistry, InstanceJobFacade instanceJobFacade,
        TriggerExecutionService triggerExecutionService, TriggerFileStorage triggerFileStorage,
        TriggerStateService triggerStateService) {

        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.instanceJobFacade = instanceJobFacade;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerFileStorage = triggerFileStorage;
        this.triggerStateService = triggerStateService;
    }

    @SuppressWarnings("unchecked")
    public void handle(TriggerExecution triggerExecution) {
        Validate.notNull(triggerExecution, "'triggerExecution' must not be null");
        Validate.notNull(triggerExecution.getId(), "'triggerExecution.id' must not be null");

        if (logger.isDebugEnabled()) {
            logger.debug("handle: triggerExecution={}", triggerExecution);
        }

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
            instanceAccessorRegistry.getInstanceAccessor(workflowExecutionId.getType());

        Map<String, Object> inputMap = (Map<String, Object>) instanceAccessor.getInputMap(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId());

        Object output = triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput());

        if (!triggerExecution.isBatch() && output instanceof Collection<?> collectionOutput) {
            for (Object outputItem : collectionOutput) {
                triggerExecution.addJobId(
                    createJob(
                        workflowExecutionId, MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), outputItem)),
                        workflowExecutionId.getInstanceId(), workflowExecutionId.getType()));
            }
        } else {
            triggerExecution.addJobId(
                createJob(
                    workflowExecutionId,
                    MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), output)),
                    workflowExecutionId.getInstanceId(), workflowExecutionId.getType()));
        }

        triggerExecutionService.update(triggerExecution);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Task id={}, type='{}', name='{}' completed",
                triggerExecution.getId(), triggerExecution.getType(), triggerExecution.getName());
        }
    }

    private long createJob(
        WorkflowExecutionId workflowExecutionId, Map<String, ?> inpputMap, long instanceId, int type) {

        Job job = instanceJobFacade.createAsyncJob(
            new JobParameters(workflowExecutionId.getWorkflowId(), inpputMap), instanceId, type);

        return Validate.notNull(job.getId(), "id");
    }
}
