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
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.domain.TriggerExecution.Status;
import com.bytechef.hermes.execution.service.TriggerExecutionService;
import com.bytechef.hermes.execution.service.TriggerStateService;
import com.bytechef.hermes.file.storage.facade.TriggerFileStorageFacade;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerCompletionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TriggerCompletionHandler.class);

    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final JobFacade jobFacade;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorageFacade triggerFileStorageFacade;
    private final TriggerStateService triggerStateService;

    public TriggerCompletionHandler(
        InstanceAccessorRegistry instanceAccessorRegistry, JobFacade jobFacade,
        TriggerExecutionService triggerExecutionService,
        @Qualifier("workflowAsyncTriggerFileStorageFacade") TriggerFileStorageFacade triggerFileStorageFacade,
        TriggerStateService triggerStateService) {

        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.jobFacade = jobFacade;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerFileStorageFacade = triggerFileStorageFacade;
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
            instanceAccessorRegistry.getInstanceAccessor(workflowExecutionId.getInstanceType());

        Map<String, Object> inputMap = (Map<String, Object>) instanceAccessor.getInputMap(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId());
        Map<String, ?> metadata = Map.of(
            MetadataConstants.INSTANCE_ID, workflowExecutionId.getInstanceId(),
            MetadataConstants.INSTANCE_TYPE, workflowExecutionId.getInstanceType());

        Object output = triggerFileStorageFacade.readTriggerExecutionOutput(triggerExecution.getOutput());

        if (!triggerExecution.isBatch() && output instanceof Collection<?> collectionOutput) {
            for (Object outputItem : collectionOutput) {
                createJob(
                    workflowExecutionId, MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), outputItem)),
                    metadata);
            }
        } else {
            createJob(
                workflowExecutionId,
                MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), output)), metadata);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Task id={}, type='{}', name='{}' completed",
                triggerExecution.getId(), triggerExecution.getType(), triggerExecution.getName());
        }
    }

    private void createJob(WorkflowExecutionId workflowExecutionId, Map<String, ?> inpputMap, Map<String, ?> metadata) {
        jobFacade.createJob(new JobParameters(workflowExecutionId.getWorkflowId(), inpputMap, metadata));
    }
}
