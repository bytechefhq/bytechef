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

package com.bytechef.platform.component.log.listener;

import com.bytechef.atlas.execution.event.JobDeletedEvent;
import com.bytechef.platform.component.log.LogFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author Ivica Cardic
 */
@Component
public class JobDeletedEventListener {

    private static final Logger log = LoggerFactory.getLogger(JobDeletedEventListener.class);

    private final LogFileStorage logFileStorage;

    @SuppressFBWarnings("EI")
    public JobDeletedEventListener(LogFileStorage logFileStorage) {
        this.logFileStorage = logFileStorage;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onJobDeleted(JobDeletedEvent jobDeletedEvent) {
        long jobId = jobDeletedEvent.jobId();

        try {
            logFileStorage.deleteLogEntries(jobId);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete the logs of deleted job id={}", jobId, exception);
        }
    }
}
