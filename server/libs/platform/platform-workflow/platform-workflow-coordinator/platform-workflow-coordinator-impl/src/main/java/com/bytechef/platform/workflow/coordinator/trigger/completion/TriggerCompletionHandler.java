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

package com.bytechef.platform.workflow.coordinator.trigger.completion;

import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution.Status;
import com.bytechef.platform.workflow.execution.facade.InstanceJobFacade;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    @SuppressFBWarnings("EI")
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

        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(workflowExecutionId.getType());

        Map<String, Object> inputMap = (Map<String, Object>) instanceAccessor.getInputMap(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId());

        if (triggerExecution.getOutput() == null) {
            triggerExecution.addJobId(
                createJob(
                    workflowExecutionId,
                    MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), Map.of())),
                    workflowExecutionId.getInstanceId(), workflowExecutionId.getType()));
        } else {
            Object output = triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput());

            if (!triggerExecution.isBatch() && output instanceof Collection<?> triggerOutputValues) {
                for (Object triggerOutputValue : triggerOutputValues) {
                    triggerExecution.addJobId(
                        createJob(
                            workflowExecutionId,
                            MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), triggerOutputValue)),
                            workflowExecutionId.getInstanceId(), workflowExecutionId.getType()));
                }
            } else {
                triggerExecution.addJobId(
                    createJob(
                        workflowExecutionId,
                        MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), output)),
                        workflowExecutionId.getInstanceId(), workflowExecutionId.getType()));
            }
        }

        triggerExecutionService.update(triggerExecution);

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Trigger id={}, type='{}', name='{}' completed",
                triggerExecution.getId(), triggerExecution.getType(), triggerExecution.getName());
        }
    }

    private long createJob(
        WorkflowExecutionId workflowExecutionId, Map<String, ?> inpputMap, long instanceId, Type type) {

        return instanceJobFacade.createJob(
            new JobParameters(workflowExecutionId.getWorkflowId(), inpputMap), instanceId, type);
    }
}
