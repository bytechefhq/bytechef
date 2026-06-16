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

package com.bytechef.ee.platform.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class PersistenceAuditEventRepositoryIntTest {

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @AfterEach
    public void afterEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    public void testFindAllFilteredByPrincipal() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("bob", "PERMISSION_CHECK", LocalDateTime.now());

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            "alice", null, null, null, null, PageRequest.of(0, 25));

        assertThat(page.getContent())
            .hasSize(1)
            .allSatisfy(event -> assertThat(event.getPrincipal()).isEqualTo("alice"));
    }

    @Test
    public void testFindAllFilteredByEventType() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("alice", "CONNECTION_CREATED", LocalDateTime.now());

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            null, "CONNECTION_CREATED", null, null, null, PageRequest.of(0, 25));

        assertThat(page.getContent())
            .hasSize(1)
            .allSatisfy(event -> assertThat(event.getEventType()).isEqualTo("CONNECTION_CREATED"));
    }

    @Test
    public void testFindAllFilteredByDateRange() {
        LocalDateTime now = LocalDateTime.now();

        save("alice", "PERMISSION_CHECK", now.minusDays(10));
        save("alice", "PERMISSION_CHECK", now.minusDays(1));

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            null, null, now.minusDays(5), now, null, PageRequest.of(0, 25));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    public void testFindAllFilteredWithNoFiltersReturnsAll() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("bob", "CONNECTION_CREATED", LocalDateTime.now());

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            null, null, null, null, null, PageRequest.of(0, 25));

        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    public void testFindDistinctEventTypes() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("bob", "CONNECTION_CREATED", LocalDateTime.now());

        List<String> types = persistenceAuditEventRepository.findDistinctEventTypes();

        assertThat(types).containsExactly("CONNECTION_CREATED", "PERMISSION_CHECK");
    }

    @Test
    public void testDeleteByEventDateBefore() {
        LocalDateTime now = LocalDateTime.now();

        save("alice", "PERMISSION_CHECK", now.minusDays(400));
        save("alice", "PERMISSION_CHECK", now.minusDays(10));

        int deleted = persistenceAuditEventRepository.deleteByEventDateBefore(now.minusDays(365));

        assertThat(deleted).isEqualTo(1);

        Page<PersistentAuditEvent> remaining = persistenceAuditEventRepository.findAllFiltered(
            null, null, null, null, null, PageRequest.of(0, 25));

        assertThat(remaining.getContent()).hasSize(1);
    }

    private void save(String principal, String eventType, LocalDateTime eventDate) {
        PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

        persistentAuditEvent.setPrincipal(principal);
        persistentAuditEvent.setEventType(eventType);
        persistentAuditEvent.setEventDate(eventDate);
        persistentAuditEvent.setData(Map.of("k", "v"));

        persistenceAuditEventRepository.save(persistentAuditEvent);
    }
}
