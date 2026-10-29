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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.DeleteJobEvent;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

/**
 * @author Ivica Cardic
 */
public class ExecutionDataJobDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(ExecutionDataJobDeletionListener.class);

    private final @Nullable DataStorage dataStorage;

    @SuppressFBWarnings("EI2")
    public ExecutionDataJobDeletionListener(@Nullable DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    @EventListener
    public void onDeleteJob(DeleteJobEvent deleteJobEvent) {
        if (dataStorage == null) {
            return;
        }

        try {
            dataStorage.deleteScopeData(
                DataStorageScope.CURRENT_EXECUTION, String.valueOf(deleteJobEvent.getJobId()));
        } catch (UnsupportedOperationException exception) {
            log.debug("Scope-wide data-storage delete unsupported by this provider; skipping execution-data cleanup");
        }
    }
}
