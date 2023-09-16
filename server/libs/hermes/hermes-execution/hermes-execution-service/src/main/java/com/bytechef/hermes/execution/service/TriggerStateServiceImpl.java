
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

package com.bytechef.hermes.execution.service;

import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.domain.TriggerState;
import com.bytechef.hermes.execution.repository.TriggerStateRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Service("triggerStorageService")
@Transactional
public class TriggerStateServiceImpl implements TriggerStateService, RemoteTriggerStateService {

    private final TriggerStateRepository triggerStateRepository;

    @SuppressFBWarnings("EI")
    public TriggerStateServiceImpl(TriggerStateRepository triggerStateRepository) {
        this.triggerStateRepository = triggerStateRepository;
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
                triggerLifecycle -> {
                    triggerLifecycle.setValue(value);

                    triggerStateRepository.save(triggerLifecycle);
                },
                () -> triggerStateRepository.save(new TriggerState(workflowExecutionId, value)));
    }
}
