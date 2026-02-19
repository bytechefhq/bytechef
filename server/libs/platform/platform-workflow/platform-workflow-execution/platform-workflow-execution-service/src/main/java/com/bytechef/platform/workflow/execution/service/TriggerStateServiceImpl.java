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

package com.bytechef.platform.workflow.execution.service;

import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerState;
import com.bytechef.platform.workflow.execution.repository.TriggerStateRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service("triggerStorageService")
@Transactional
public class TriggerStateServiceImpl implements TriggerStateService {

    private final TriggerStateRepository triggerStateRepository;

    @SuppressFBWarnings("EI")
    public TriggerStateServiceImpl(TriggerStateRepository triggerStateRepository) {
        this.triggerStateRepository = triggerStateRepository;
    }

    @Override
    public void delete(WorkflowExecutionId workflowExecutionId) {
        triggerStateRepository
            .findByWorkflowExecutionId(workflowExecutionId.toString())
            .ifPresent(triggerStateRepository::delete);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional
    public <T> Optional<T> fetchValue(WorkflowExecutionId workflowExecutionId) {
        return triggerStateRepository.findByWorkflowExecutionId(workflowExecutionId.toString())
            .map(triggerLifecycle -> (T) triggerLifecycle.getValue());
    }

    @Override
    public void save(WorkflowExecutionId workflowExecutionId, Object value) {
        triggerStateRepository
            .findByWorkflowExecutionId(workflowExecutionId.toString())
            .ifPresentOrElse(
                triggerState -> {
                    triggerState.setValue(value);

                    triggerStateRepository.save(triggerState);
                },
                () -> triggerStateRepository.save(new TriggerState(workflowExecutionId, value)));
    }
}
