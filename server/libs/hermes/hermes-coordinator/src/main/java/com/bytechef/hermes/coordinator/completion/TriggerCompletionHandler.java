
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

package com.bytechef.hermes.coordinator.completion;

import com.bytechef.hermes.coordinator.job.InstanceJobFactoryAccessor;
import com.bytechef.hermes.domain.TriggerExecution.Status;
import com.bytechef.hermes.domain.TriggerExecution;
import com.bytechef.hermes.job.InstanceJobFactory;
import com.bytechef.hermes.service.TriggerExecutionService;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerCompletionHandler {

    private final InstanceJobFactoryAccessor instanceJobFactoryAccessor;
    private final TriggerExecutionService triggerExecutionService;

    public TriggerCompletionHandler(
        InstanceJobFactoryAccessor instanceJobFactoryAccessor, TriggerExecutionService triggerExecutionService) {

        this.instanceJobFactoryAccessor = instanceJobFactoryAccessor;
        this.triggerExecutionService = triggerExecutionService;
    }

    public void handle(TriggerExecution triggerExecution) {
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        InstanceJobFactory instanceJobFactory = instanceJobFactoryAccessor.getInstanceJobFactory(
            workflowExecutionId.getInstanceType());

        instanceJobFactory.createJob(workflowExecutionId.getWorkflowId(), workflowExecutionId.getInstanceId());

        triggerExecution.setStatus(Status.COMPLETED);

        triggerExecutionService.update(triggerExecution);
    }
}
