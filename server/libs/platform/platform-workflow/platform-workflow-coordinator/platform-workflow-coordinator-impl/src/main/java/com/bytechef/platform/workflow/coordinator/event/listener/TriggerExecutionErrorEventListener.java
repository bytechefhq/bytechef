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

package com.bytechef.platform.workflow.coordinator.event.listener;

import com.bytechef.error.ExecutionError;
import com.bytechef.platform.workflow.coordinator.event.ErrorEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionErrorEvent;
import com.bytechef.platform.workflow.coordinator.trigger.error.TriggerErrorHandler;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerExecutionErrorEventListener implements ErrorEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TriggerExecutionErrorEventListener.class);

    private final TriggerExecutionService triggerExecutionService;
    private final TriggerErrorHandler triggerErrorHandler;

    @SuppressFBWarnings("EI")
    public TriggerExecutionErrorEventListener(
        TriggerErrorHandler triggerErrorHandler, TriggerExecutionService triggerExecutionService) {

        this.triggerExecutionService = triggerExecutionService;
        this.triggerErrorHandler = triggerErrorHandler;
    }

    public void onErrorEvent(ErrorEvent errorEvent) {
        if (errorEvent instanceof TriggerExecutionErrorEvent triggerExecutionErrorEvent) {
            TriggerExecution triggerExecution = triggerExecutionErrorEvent.getTriggerExecution();
            ExecutionError error = Validate.notNull(triggerExecutionErrorEvent.getError(), "'error' must not be null");

            logger.error(
                "Trigger id={}: message={}\nstackTrace={}", triggerExecution.getId(), error.getMessage(),
                error.getStackTrace());

            try {
                triggerErrorHandler.handleError(triggerExecution);
            } catch (Exception ex) {
                logger.debug("Job creation during trigger error handling failed: {}", ex.getMessage(), ex);
            }

            triggerExecutionService.update(triggerExecution);
        }
    }
}
