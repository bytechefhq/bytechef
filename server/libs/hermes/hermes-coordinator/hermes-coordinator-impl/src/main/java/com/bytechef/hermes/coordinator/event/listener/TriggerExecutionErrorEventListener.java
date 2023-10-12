
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

package com.bytechef.hermes.coordinator.event.listener;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import com.bytechef.hermes.coordinator.event.ErrorEvent;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.coordinator.event.TriggerExecutionErrorEvent;
import com.bytechef.hermes.execution.service.TriggerExecutionService;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerExecutionErrorEventListener implements ErrorEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TriggerExecutionErrorEventListener.class);

    private final TriggerExecutionService triggerExecutionService;

    public TriggerExecutionErrorEventListener(TriggerExecutionService triggerExecutionService) {
        this.triggerExecutionService = triggerExecutionService;
    }

    public void onErrorEvent(ErrorEvent errorEvent) {
        if (errorEvent instanceof TriggerExecutionErrorEvent triggerExecutionErrorEvent) {
            TriggerExecution triggerExecution = triggerExecutionErrorEvent.getTriggerExecution();
            ExecutionError error = Validate.notNull(errorEvent.getError(), "'error' must not be null");

            logger.error(
                "Trigger id={}: message={}\nstackTrace={}", triggerExecution.getId(), error.getMessage(),
                error.getStackTrace());

            // set task status to FAILED and persist

            triggerExecution.setEndDate(LocalDateTime.now());
            triggerExecution.setStatus(TriggerExecution.Status.FAILED);

            triggerExecutionService.update(triggerExecution);
        }
    }
}
