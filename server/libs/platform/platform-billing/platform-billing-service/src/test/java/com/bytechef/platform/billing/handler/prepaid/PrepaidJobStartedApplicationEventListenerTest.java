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

package com.bytechef.platform.billing.handler.prepaid;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Matija Petanjek
 */
@ExtendWith(MockitoExtension.class)
class PrepaidJobStartedApplicationEventListenerTest {

    @Mock
    private PrepaidUnitConsumptionService prepaidUnitConsumptionService;

    @InjectMocks
    private PrepaidJobStartedApplicationEventListener prepaidJobStartedApplicationEventListener;

    @Test
    void testStartedEventConsumesUnit() {
        prepaidJobStartedApplicationEventListener.onApplicationEvent(new JobStatusApplicationEvent(5L, Status.STARTED));

        verify(prepaidUnitConsumptionService).consumeUnit(5L);
    }

    @Test
    void testOtherStatusesIgnored() {
        for (Status status : new Status[] {
            Status.CREATED, Status.COMPLETED, Status.FAILED, Status.STOPPED
        }) {
            prepaidJobStartedApplicationEventListener.onApplicationEvent(new JobStatusApplicationEvent(5L, status));
        }

        verify(prepaidUnitConsumptionService, never()).consumeUnit(anyLong());
    }

    @Test
    void testConsumeUnitFailureDoesNotPropagate() {
        doThrow(new IllegalStateException("db down")).when(prepaidUnitConsumptionService)
            .consumeUnit(5L);

        assertThatCode(
            () -> prepaidJobStartedApplicationEventListener.onApplicationEvent(
                new JobStatusApplicationEvent(5L, Status.STARTED))).doesNotThrowAnyException();
    }
}
