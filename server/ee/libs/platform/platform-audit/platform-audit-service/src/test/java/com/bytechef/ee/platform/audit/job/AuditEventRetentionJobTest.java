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

package com.bytechef.ee.platform.audit.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditEventRetentionJobTest {

    private final PersistenceAuditEventRepository persistenceAuditEventRepository =
        mock(PersistenceAuditEventRepository.class);

    @Test
    void testPurgeExpiredEventsPassesCutoffToRepository() {
        when(persistenceAuditEventRepository.deleteByEventDateBefore(any())).thenReturn(3);

        AuditEventRetentionJob job = new AuditEventRetentionJob(persistenceAuditEventRepository, 30L);

        LocalDateTime before = LocalDateTime.now()
            .minusDays(30);

        job.purgeExpiredEvents();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(persistenceAuditEventRepository).deleteByEventDateBefore(captor.capture());

        LocalDateTime cutoff = captor.getValue();

        // cutoff must be ~30 days ago (allow 1 minute slack for clock drift between the test calls)
        org.assertj.core.api.Assertions.assertThat(cutoff)
            .isBetween(before.minusMinutes(1), before.plusMinutes(1));
    }
}
