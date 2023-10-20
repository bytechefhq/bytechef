
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

package com.bytechef.hermes.coordinator.error;

import com.bytechef.error.ErrorHandler;
import com.bytechef.error.ExecutionError;
import com.bytechef.hermes.domain.TriggerExecution;
import com.bytechef.hermes.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerExecutionErrorHandler implements ErrorHandler<TriggerExecution> {

    private static final Logger log = LoggerFactory.getLogger(TriggerExecutionErrorHandler.class);

    private final TriggerExecutionService triggerExecutionService;

    public TriggerExecutionErrorHandler(TriggerExecutionService triggerExecutionService) {
        this.triggerExecutionService = triggerExecutionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void handle(TriggerExecution triggerExecution) {
        ExecutionError error = triggerExecution.getError();

        Assert.notNull(error, "'error' must not be null");

        log.error(
            "Trigger {}: {}\n{}", triggerExecution.getId(), error.getMessage(), error.getStackTrace());

        // set task status to FAILED and persist

        triggerExecution.setEndDate(LocalDateTime.now());
        triggerExecution.setStatus(TriggerExecution.Status.FAILED);

        triggerExecutionService.update(triggerExecution);
    }
}
