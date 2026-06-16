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

package com.bytechef.ee.platform.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class AuditEventServiceTest {

    private final PersistenceAuditEventRepository persistenceAuditEventRepository =
        mock(PersistenceAuditEventRepository.class);

    private final AuditEventService auditEventService = new AuditEventService(persistenceAuditEventRepository);

    @Test
    void testFetchAuditEventsDelegatesToRepository() {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setId(42L);

        Page<PersistentAuditEvent> page = new PageImpl<>(List.of(persistentAuditEvent));

        when(persistenceAuditEventRepository.findAllFiltered(
            eq("alice"), eq("PERMISSION_CHECK"), any(), any(), any(), any()))
                .thenReturn(page);

        Pageable pageable = PageRequest.of(0, 25);

        Page<PersistentAuditEvent> result = auditEventService.fetchAuditEvents(
            "alice", "PERMISSION_CHECK", LocalDateTime.now()
                .minusDays(7),
            LocalDateTime.now(), null, pageable);

        assertThat(result.getContent()).hasSize(1);

        assertThat(result.getContent()
            .getFirst()
            .getId()).isEqualTo(42L);
    }

    @Test
    void testFetchEventTypesDelegatesToRepository() {
        when(persistenceAuditEventRepository.findDistinctEventTypes())
            .thenReturn(List.of("CONNECTION_CREATED", "PERMISSION_CHECK"));

        List<String> types = auditEventService.fetchEventTypes();

        assertThat(types).containsExactly("CONNECTION_CREATED", "PERMISSION_CHECK");
    }
}
