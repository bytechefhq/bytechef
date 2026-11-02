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

package com.bytechef.platform.workflow.execution.facade;

import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class TriggerStateFacadeImpl implements TriggerStateFacade {

    private final StateValueDataStorage stateValueDataStorage;
    private final TriggerStateService triggerStateService;

    @SuppressFBWarnings("EI")
    TriggerStateFacadeImpl(StateValueDataStorage stateValueDataStorage, TriggerStateService triggerStateService) {
        this.stateValueDataStorage = stateValueDataStorage;
        this.triggerStateService = triggerStateService;
    }

    @Override
    public void delete(WorkflowExecutionId workflowExecutionId) {
        Optional<Object> storedValueOptional = triggerStateService.fetchValue(workflowExecutionId);

        if (storedValueOptional.isPresent()) {
            stateValueDataStorage.delete(
                getKey(workflowExecutionId), storedValueOptional.get(),
                () -> triggerStateService.fetchValue(workflowExecutionId));

            triggerStateService.delete(workflowExecutionId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> fetchValue(WorkflowExecutionId workflowExecutionId) {
        Optional<Object> storedValueOptional = triggerStateService.fetchValue(workflowExecutionId);

        return storedValueOptional.map(
            storedValue -> (T) stateValueDataStorage.read(getKey(workflowExecutionId), storedValue));
    }

    @Override
    public void save(WorkflowExecutionId workflowExecutionId, Object value) {
        Optional<Object> previousStoredValueOptional = triggerStateService.fetchValue(workflowExecutionId);

        Object storedValue = stateValueDataStorage.write(
            getKey(workflowExecutionId), value,
            stateValueDataStorage.fetchLocation(
                workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getType()),
            previousStoredValueOptional.orElse(null), () -> triggerStateService.fetchValue(workflowExecutionId));

        triggerStateService.save(workflowExecutionId, storedValue);
    }

    private static StateValueDataStorage.Key getKey(WorkflowExecutionId workflowExecutionId) {
        return new StateValueDataStorage.Key(
            StateValueDataStorage.TRIGGER_STATE_COMPONENT_NAME, DataStorageScope.WORKFLOW,
            workflowExecutionId.getWorkflowUuid(), StateValueDataStorage.toKey(workflowExecutionId.toString()));
    }
}
