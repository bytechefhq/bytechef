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

package com.bytechef.platform.scheduler.db.task;

import com.github.kagkarlsson.scheduler.task.ExecutionComplete;
import com.github.kagkarlsson.scheduler.task.ExecutionOperations;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RescheduleAtTest {

    @Test
    void testCompleteReschedulesToGivenInstant() {
        Instant executionTime = Instant.parse("2030-01-01T00:00:00Z");
        ExecutionComplete executionComplete = Mockito.mock(ExecutionComplete.class);

        @SuppressWarnings("unchecked")
        ExecutionOperations<Object> executionOperations = Mockito.mock(ExecutionOperations.class);

        new RescheduleAt<>(executionTime).complete(executionComplete, executionOperations);

        Mockito.verify(executionOperations)
            .reschedule(executionComplete, executionTime);
    }
}
