# Audit Events Settings Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an admin-only, EE-only "Audit Events" page under platform settings that lists `persistent_audit_event` rows with principal/type/date filters, and bridges existing CE `ConnectionAuditEvent` application events into the audit table so connection activity appears in the UI.

**Architecture:** Rename `PermissionAuditEventService` → `AuditEventService` and extend it with paginated read methods. Add a new EE `ConnectionAuditEventListener` that subscribes to CE `ConnectionAuditEvent`s and persists them via `AuditEventService`. Expose read queries through a new `platform-audit-graphql` module. The React page under `client/src/ee/pages/settings/platform/audit-events/` consumes these queries and renders a filterable table with a detail sheet for each event's `data` map.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring Data JDBC / Spring GraphQL / JUnit 5 / Testcontainers / React 19 / TypeScript 5.9 / GraphQL Codegen / TailwindCSS / shadcn/ui / vitest.

**Spec:** `docs/superpowers/specs/2026-04-15-audit-events-settings-page-design.md`

---

## File Structure

### Backend — modify

- `server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/service/PermissionAuditEventService.java` → rename to `AuditEventService.java`, extend with read methods.
- `server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/aspect/PermissionAuditAspect.java` — update field type/name after rename.
- `server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/repository/PersistenceAuditEventRepository.java` — add `findAllFiltered` + `findDistinctEventTypes`.

### Backend — create

- `server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/listener/ConnectionAuditEventListener.java`
- `server/ee/libs/platform/platform-audit/platform-audit-graphql/build.gradle.kts`
- `server/ee/libs/platform/platform-audit/platform-audit-graphql/src/main/resources/graphql/audit-event.graphqls`
- `server/ee/libs/platform/platform-audit/platform-audit-graphql/src/main/java/com/bytechef/ee/platform/audit/web/graphql/AuditEventGraphQlController.java`
- Tests:
  - `server/ee/libs/platform/platform-audit/platform-audit-service/src/test/java/com/bytechef/ee/platform/audit/listener/ConnectionAuditEventListenerTest.java`
  - `server/ee/libs/platform/platform-audit/platform-audit-service/src/test/java/com/bytechef/ee/platform/audit/service/AuditEventServiceTest.java`
  - `server/ee/libs/platform/platform-audit/platform-audit-service/src/testIntegration/java/com/bytechef/ee/platform/audit/repository/PersistenceAuditEventRepositoryIntTest.java`

### Backend — registration

- `settings.gradle.kts` — add `platform-audit-graphql` module.
- `server/apps/server-app/build.gradle.kts` — add dependency on `platform-audit-graphql`.

### Frontend — modify

- `client/codegen.ts` — add schema path for new graphql module.
- `client/src/routes.tsx` — add lazy import, route entry, nav item.

### Frontend — create

- `client/src/graphql/auditEvents/auditEvents.graphql`
- `client/src/ee/pages/settings/platform/audit-events/AuditEvents.tsx`
- `client/src/ee/pages/settings/platform/audit-events/components/AuditEventsFilterBar.tsx`
- `client/src/ee/pages/settings/platform/audit-events/components/AuditEventsTable.tsx`
- `client/src/ee/pages/settings/platform/audit-events/components/AuditEventDetailSheet.tsx`

### Frontend — regenerated

- `client/src/shared/middleware/graphql.ts` (via codegen — separate commit).

---

## Task 1: Rename `PermissionAuditEventService` → `AuditEventService`

**Files:**
- Rename: `server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/service/PermissionAuditEventService.java` → `AuditEventService.java`
- Modify: `server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/aspect/PermissionAuditAspect.java`

- [ ] **Step 1: Rename the service class**

```bash
git mv server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/service/PermissionAuditEventService.java \
  server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/service/AuditEventService.java
```

Then edit the file to replace the class name:

```java
package com.bytechef.ee.platform.audit.service;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit events. Uses REQUIRES_NEW so INSERTs succeed even when the caller is inside a
 * read-only transaction (permission aspect around @PreAuthorize methods, connection aspect
 * on facades marked read-only, etc.).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class AuditEventService {

    private final PersistenceAuditEventRepository persistenceAuditEventRepository;

    @SuppressFBWarnings("EI")
    public AuditEventService(PersistenceAuditEventRepository persistenceAuditEventRepository) {
        this.persistenceAuditEventRepository = persistenceAuditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(PersistentAuditEvent persistentAuditEvent) {
        persistenceAuditEventRepository.save(persistentAuditEvent);
    }
}
```

- [ ] **Step 2: Update `PermissionAuditAspect`**

Change the field type and constructor parameter type/name in `PermissionAuditAspect.java`:

```java
private final AuditEventService auditEventService;

public PermissionAuditAspect(AuditEventService auditEventService) {
    this.auditEventService = auditEventService;
}
```

And update the single call site inside `saveAuditEvent` from `permissionAuditEventService.save(...)` to `auditEventService.save(...)`.

Also update the import:

```java
import com.bytechef.ee.platform.audit.service.AuditEventService;
```

- [ ] **Step 3: Find and update any other references**

Run: `grep -rn "PermissionAuditEventService" server/ /Volumes/Data/bytechef/bytechef --include='*.java'`
Expected: only the test class (if any) remains. Update every hit to `AuditEventService`.

- [ ] **Step 4: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-audit:platform-audit-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/
git commit -m "$(cat <<'EOF'
Rename PermissionAuditEventService to AuditEventService

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Write `AuditEventService` unit test (TDD for new read methods)

**Files:**
- Create: `server/ee/libs/platform/platform-audit/platform-audit-service/src/test/java/com/bytechef/ee/platform/audit/service/AuditEventServiceTest.java`

- [ ] **Step 1: Write failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
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

        when(persistenceAuditEventRepository.findAllFiltered(eq("alice"), eq("PERMISSION_CHECK"), any(), any(), any()))
            .thenReturn(page);

        Pageable pageable = PageRequest.of(0, 25);

        Page<PersistentAuditEvent> result = auditEventService.fetchAuditEvents(
            "alice", "PERMISSION_CHECK", LocalDateTime.now().minusDays(7), LocalDateTime.now(), pageable);

        assertThat(result.getContent()).hasSize(1);

        assertThat(result.getContent().getFirst().getId()).isEqualTo(42L);
    }

    @Test
    void testFetchEventTypesDelegatesToRepository() {
        when(persistenceAuditEventRepository.findDistinctEventTypes())
            .thenReturn(List.of("CONNECTION_CREATED", "PERMISSION_CHECK"));

        List<String> types = auditEventService.fetchEventTypes();

        assertThat(types).containsExactly("CONNECTION_CREATED", "PERMISSION_CHECK");
    }
}
```

- [ ] **Step 2: Run test — expect compile failures**

Run: `./gradlew :server:ee:libs:platform:platform-audit:platform-audit-service:test --tests AuditEventServiceTest`
Expected: compile error — `findAllFiltered`, `findDistinctEventTypes`, `fetchAuditEvents`, `fetchEventTypes` not defined.

---

## Task 3: Add repository methods

**Files:**
- Modify: `server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/repository/PersistenceAuditEventRepository.java`

- [ ] **Step 1: Add query methods**

Append inside the interface, keeping the existing derived methods intact:

```java
@Query("""
    SELECT * FROM persistent_audit_event
    WHERE (:principal IS NULL OR principal = :principal)
      AND (:eventType IS NULL OR event_type = :eventType)
      AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR event_date >= :fromDate)
      AND (CAST(:toDate AS TIMESTAMP) IS NULL OR event_date <= :toDate)
    """)
Page<PersistentAuditEvent> findAllFiltered(
    @Param("principal") String principal,
    @Param("eventType") String eventType,
    @Param("fromDate") LocalDateTime fromDate,
    @Param("toDate") LocalDateTime toDate,
    Pageable pageable);

@Query("SELECT DISTINCT event_type FROM persistent_audit_event ORDER BY event_type")
List<String> findDistinctEventTypes();
```

Add the required imports:

```java
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-audit:platform-audit-service:compileJava`
Expected: BUILD SUCCESSFUL.

---

## Task 4: Add service read methods

**Files:**
- Modify: `server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/service/AuditEventService.java`

- [ ] **Step 1: Add methods**

Add to `AuditEventService` (keep the existing `save`):

```java
@Transactional(readOnly = true)
public Page<PersistentAuditEvent> fetchAuditEvents(
    String principal, String eventType, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {

    return persistenceAuditEventRepository.findAllFiltered(principal, eventType, fromDate, toDate, pageable);
}

@Transactional(readOnly = true)
public List<String> fetchEventTypes() {
    return persistenceAuditEventRepository.findDistinctEventTypes();
}
```

Add imports:

```java
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 2: Run unit test — expect PASS**

Run: `./gradlew :server:ee:libs:platform:platform-audit:platform-audit-service:test --tests AuditEventServiceTest`
Expected: PASS (both test methods).

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/platform/platform-audit/platform-audit-service/src/
git commit -m "$(cat <<'EOF'
Add paginated read methods to AuditEventService

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Repository integration test

**Files:**
- Create: `server/ee/libs/platform/platform-audit/platform-audit-service/src/testIntegration/java/com/bytechef/ee/platform/audit/repository/PersistenceAuditEventRepositoryIntTest.java`

- [ ] **Step 1: Check if testIntegration source set exists**

Run: `ls server/ee/libs/platform/platform-audit/platform-audit-service/src/testIntegration 2>/dev/null || echo 'missing'`

If missing, check another EE module that has `testIntegration` for the `build.gradle.kts` pattern:
`grep -rln "testIntegration" server/ee/libs/platform/ --include='build.gradle.kts' | head -1`

If the module's `build.gradle.kts` doesn't enable the `testIntegration` source set, write the test under `src/test/java/` and name the class `PersistenceAuditEventRepositoryTest` instead (still a `@SpringBootTest`, still Testcontainers-backed). Adjust Task 5 steps accordingly.

- [ ] **Step 2: Write the test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("testint")
class PersistenceAuditEventRepositoryIntTest {

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void setUp() {
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testFindAllFilteredByPrincipal() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("bob", "PERMISSION_CHECK", LocalDateTime.now());

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            "alice", null, null, null, PageRequest.of(0, 25));

        assertThat(page.getContent())
            .hasSize(1)
            .allSatisfy(event -> assertThat(event.getPrincipal()).isEqualTo("alice"));
    }

    @Test
    void testFindAllFilteredByEventType() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("alice", "CONNECTION_CREATED", LocalDateTime.now());

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            null, "CONNECTION_CREATED", null, null, PageRequest.of(0, 25));

        assertThat(page.getContent())
            .hasSize(1)
            .allSatisfy(event -> assertThat(event.getEventType()).isEqualTo("CONNECTION_CREATED"));
    }

    @Test
    void testFindAllFilteredByDateRange() {
        LocalDateTime now = LocalDateTime.now();

        save("alice", "PERMISSION_CHECK", now.minusDays(10));
        save("alice", "PERMISSION_CHECK", now.minusDays(1));

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            null, null, now.minusDays(5), now, PageRequest.of(0, 25));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void testFindAllFilteredWithNoFiltersReturnsAll() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("bob", "CONNECTION_CREATED", LocalDateTime.now());

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            null, null, null, null, PageRequest.of(0, 25));

        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void testFindDistinctEventTypes() {
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("alice", "PERMISSION_CHECK", LocalDateTime.now());
        save("bob", "CONNECTION_CREATED", LocalDateTime.now());

        List<String> types = persistenceAuditEventRepository.findDistinctEventTypes();

        assertThat(types).containsExactly("CONNECTION_CREATED", "PERMISSION_CHECK");
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
```

- [ ] **Step 3: Run integration test**

Run: `./gradlew :server:ee:libs:platform:platform-audit:platform-audit-service:testIntegration --tests PersistenceAuditEventRepositoryIntTest`
Expected: all 5 tests PASS. If the module uses regular `test` source set instead, run `:test --tests PersistenceAuditEventRepositoryTest`.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/platform/platform-audit/platform-audit-service/src/
git commit -m "$(cat <<'EOF'
Add integration tests for AuditEvent repository filter query

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: `ConnectionAuditEventListener` — failing test first

**Files:**
- Create: `server/ee/libs/platform/platform-audit/platform-audit-service/src/test/java/com/bytechef/ee/platform/audit/listener/ConnectionAuditEventListenerTest.java`

- [ ] **Step 1: Verify the CE `ConnectionAuditEvent` shape**

Read: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/audit/ConnectionAuditEvent.java`
Confirm it's an `enum` (values like `CONNECTION_CREATED`, `CONNECTION_SHARED`, etc.).

Also read: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/audit/ConnectionAuditPublisher.java` to learn the published event class shape (likely a record/class wrapping `event`, `connectionId`, `data`).

Note: If the published Spring `ApplicationEvent` is a different class than `ConnectionAuditEvent` (e.g. `ConnectionAuditApplicationEvent` holding an enum `event` field), use that published class as the `@EventListener` parameter and adjust field accessors below. The test below assumes a published event with `event()` (enum), `connectionId()` (long), and `data()` (Map<String,Object>) accessors. Adjust to match actual shape.

- [ ] **Step 2: Write failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.connection.audit.ConnectionAuditEvent;
import com.bytechef.platform.connection.audit.ConnectionAuditPublisher.ConnectionAuditApplicationEvent;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConnectionAuditEventListenerTest {

    private final AuditEventService auditEventService = mock(AuditEventService.class);

    private final ConnectionAuditEventListener listener = new ConnectionAuditEventListener(auditEventService);

    @Test
    void testOnConnectionAuditEventPersistsRow() {
        ConnectionAuditApplicationEvent applicationEvent = new ConnectionAuditApplicationEvent(
            ConnectionAuditEvent.CONNECTION_SHARED, 7L, Map.of("projectId", 42L));

        listener.onConnectionAuditEvent(applicationEvent);

        ArgumentCaptor<PersistentAuditEvent> captor = ArgumentCaptor.forClass(PersistentAuditEvent.class);

        verify(auditEventService).save(captor.capture());

        PersistentAuditEvent persisted = captor.getValue();

        assertThat(persisted.getEventType()).isEqualTo("CONNECTION_SHARED");

        assertThat(persisted.getEventDate()).isNotNull();

        assertThat(persisted.getData())
            .containsEntry("connectionId", "7")
            .containsEntry("projectId", "42");
    }

    @Test
    void testFailureInServiceIsSwallowed() {
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
            .when(auditEventService)
            .save(org.mockito.ArgumentMatchers.any());

        ConnectionAuditApplicationEvent applicationEvent = new ConnectionAuditApplicationEvent(
            ConnectionAuditEvent.CONNECTION_CREATED, 1L, Map.of());

        listener.onConnectionAuditEvent(applicationEvent);
    }
}
```

If the published event class or its accessors differ (e.g. it's named `ConnectionAuditPublishedEvent` or its field is named differently), adjust the import, the constructor call, and the listener signature in Task 7 accordingly. **Do not invent fields that don't exist** — verify against `ConnectionAuditPublisher.java` before writing the test.

- [ ] **Step 3: Run test — expect compile failure**

Run: `./gradlew :server:ee:libs:platform:platform-audit:platform-audit-service:test --tests ConnectionAuditEventListenerTest`
Expected: compile error — `ConnectionAuditEventListener` not defined.

---

## Task 7: Implement `ConnectionAuditEventListener`

**Files:**
- Create: `server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/listener/ConnectionAuditEventListener.java`
- Modify: `server/ee/libs/platform/platform-audit/platform-audit-service/build.gradle.kts` (add `platform-connection-api` dependency if not present)

- [ ] **Step 1: Check existing dependencies**

Run: `cat server/ee/libs/platform/platform-audit/platform-audit-service/build.gradle.kts`

If `platform-connection-api` is not in `dependencies {}`, add:

```kotlin
implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
```

- [ ] **Step 2: Write the listener**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.audit.ConnectionAuditPublisher.ConnectionAuditApplicationEvent;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges CE {@link ConnectionAuditApplicationEvent} into the EE {@code persistent_audit_event} table so connection
 * activity appears in the audit UI alongside permission events.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectionAuditEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionAuditEventListener.class);

    private final AuditEventService auditEventService;

    @SuppressFBWarnings("EI")
    public ConnectionAuditEventListener(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @EventListener
    public void onConnectionAuditEvent(ConnectionAuditApplicationEvent applicationEvent) {
        try {
            PersistentAuditEvent persistentAuditEvent = new PersistentAuditEvent();

            String principal = SecurityUtils.fetchCurrentUserLogin()
                .orElse("anonymoususer");

            persistentAuditEvent.setEventDate(LocalDateTime.now());
            persistentAuditEvent.setEventType(applicationEvent.event()
                .name());
            persistentAuditEvent.setPrincipal(principal);

            Map<String, String> data = new HashMap<>();

            data.put("connectionId", String.valueOf(applicationEvent.connectionId()));

            applicationEvent.data()
                .forEach((key, value) -> data.put(key, value == null ? "null" : value.toString()));

            persistentAuditEvent.setData(data);

            auditEventService.save(persistentAuditEvent);
        } catch (Exception exception) {
            logger.error(
                "AUDIT PERSISTENCE FAILURE: Failed to save connection audit event {} for connectionId={}",
                applicationEvent.event(),
                applicationEvent.connectionId(),
                exception);
        }
    }
}
```

If the published application-event class is located or named differently, fix the import and accessor calls. Verify with: `grep -rn "publishEvent" server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/audit/`.

- [ ] **Step 3: Run test — expect PASS**

Run: `./gradlew :server:ee:libs:platform:platform-audit:platform-audit-service:test --tests ConnectionAuditEventListenerTest`
Expected: both tests PASS.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/platform/platform-audit/platform-audit-service/
git commit -m "$(cat <<'EOF'
Bridge ConnectionAuditEvent into persistent_audit_event

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Scaffold the `platform-audit-graphql` module

**Files:**
- Create: `server/ee/libs/platform/platform-audit/platform-audit-graphql/build.gradle.kts`
- Create: `server/ee/libs/platform/platform-audit/platform-audit-graphql/src/main/resources/graphql/audit-event.graphqls`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create the Gradle build file**

Write `server/ee/libs/platform/platform-audit/platform-audit-graphql/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework.data:spring-data-commons")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:platform:platform-audit:platform-audit-service"))
}
```

(Confirm the actual module paths by comparing to `server/ee/libs/platform/platform-user/platform-user-graphql/build.gradle.kts`; adjust if that reference uses different coordinates.)

- [ ] **Step 2: Register the module in `settings.gradle.kts`**

Add near the existing `platform-audit-service` include:

```kotlin
include("server:ee:libs:platform:platform-audit:platform-audit-graphql")
```

- [ ] **Step 3: Create the GraphQL schema**

Write `server/ee/libs/platform/platform-audit/platform-audit-graphql/src/main/resources/graphql/audit-event.graphqls`:

```graphql
extend type Query {
    auditEvents(
        principal: String
        eventType: String
        fromDate: Long
        toDate: Long
        page: Int = 0
        size: Int = 25
    ): AuditEventPageType!

    auditEventTypes: [String!]!
}

type AuditEventType {
    data: [AuditEventDataEntryType!]!
    eventDate: Long!
    eventType: String!
    id: ID!
    principal: String
}

type AuditEventDataEntryType {
    key: String!
    value: String!
}

type AuditEventPageType {
    content: [AuditEventType!]!
    number: Int!
    size: Int!
    totalElements: Int!
    totalPages: Int!
}
```

- [ ] **Step 4: Verify module loads**

Run: `./gradlew :server:ee:libs:platform:platform-audit:platform-audit-graphql:compileJava`
Expected: BUILD SUCCESSFUL (empty module compiles).

---

## Task 9: `AuditEventGraphQlController`

**Files:**
- Create: `server/ee/libs/platform/platform-audit/platform-audit-graphql/src/main/java/com/bytechef/ee/platform/audit/web/graphql/AuditEventGraphQlController.java`

- [ ] **Step 1: Write the controller**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing read-only access to persistent audit events.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class AuditEventGraphQlController {

    private final AuditEventService auditEventService;

    @SuppressFBWarnings("EI")
    public AuditEventGraphQlController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @QueryMapping(name = "auditEvents")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AuditEventPage auditEvents(
        @Argument String principal, @Argument String eventType, @Argument Long fromDate, @Argument Long toDate,
        @Argument Integer page, @Argument Integer size) {

        PageRequest pageRequest = PageRequest.of(
            page == null ? 0 : page, size == null ? 25 : size, Sort.by(Sort.Direction.DESC, "eventDate"));

        Page<PersistentAuditEvent> persistentPage = auditEventService.fetchAuditEvents(
            principal, eventType, toLocalDateTime(fromDate), toLocalDateTime(toDate), pageRequest);

        List<AuditEvent> content = persistentPage.getContent()
            .stream()
            .map(AuditEventGraphQlController::toAuditEvent)
            .toList();

        return new AuditEventPage(
            content, persistentPage.getNumber(), persistentPage.getSize(), persistentPage.getTotalElements(),
            persistentPage.getTotalPages());
    }

    @QueryMapping(name = "auditEventTypes")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<String> auditEventTypes() {
        return auditEventService.fetchEventTypes();
    }

    private static LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }

    private static AuditEvent toAuditEvent(PersistentAuditEvent persistentAuditEvent) {
        Map<String, String> dataMap = persistentAuditEvent.getData();

        List<AuditEventDataEntry> data = dataMap.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new AuditEventDataEntry(entry.getKey(), entry.getValue()))
            .toList();

        return new AuditEvent(
            data,
            persistentAuditEvent.getEventDate()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli(),
            persistentAuditEvent.getEventType(),
            persistentAuditEvent.getId(),
            persistentAuditEvent.getPrincipal());
    }

    record AuditEvent(List<AuditEventDataEntry> data, Long eventDate, String eventType, Long id, String principal) {
    }

    record AuditEventDataEntry(String key, String value) {
    }

    record AuditEventPage(List<AuditEvent> content, int number, int size, long totalElements, int totalPages) {
    }
}
```

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-audit:platform-audit-graphql:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Register graphql module in server-app**

Edit `server/apps/server-app/build.gradle.kts`, add near the existing audit-service / user-graphql implementation lines:

```kotlin
implementation(project(":server:ee:libs:platform:platform-audit:platform-audit-graphql"))
```

- [ ] **Step 4: Full check**

Run: `./gradlew spotlessApply :server:ee:libs:platform:platform-audit:platform-audit-graphql:check :server:apps:server-app:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-audit/platform-audit-graphql/ settings.gradle.kts server/apps/server-app/build.gradle.kts
git commit -m "$(cat <<'EOF'
Add platform-audit-graphql module with audit events query

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 10: Client — wire up GraphQL operation + codegen

**Files:**
- Modify: `client/codegen.ts`
- Create: `client/src/graphql/auditEvents/auditEvents.graphql`
- Modify (generated): `client/src/shared/middleware/graphql.ts`

- [ ] **Step 1: Add schema path to `codegen.ts`**

Edit `client/codegen.ts` `schema` array, append:

```ts
'../server/ee/libs/platform/platform-audit/platform-audit-graphql/src/main/resources/graphql/*.graphqls',
```

- [ ] **Step 2: Create the operation file**

Write `client/src/graphql/auditEvents/auditEvents.graphql`:

```graphql
query AuditEvents(
    $principal: String
    $eventType: String
    $fromDate: Long
    $toDate: Long
    $page: Int
    $size: Int
) {
    auditEvents(
        principal: $principal
        eventType: $eventType
        fromDate: $fromDate
        toDate: $toDate
        page: $page
        size: $size
    ) {
        content {
            data {
                key
                value
            }
            eventDate
            eventType
            id
            principal
        }
        number
        size
        totalElements
        totalPages
    }
}

query AuditEventTypes {
    auditEventTypes
}
```

- [ ] **Step 3: Regenerate GraphQL types**

Run: `cd client && npx graphql-codegen`
Expected: `src/shared/middleware/graphql.ts` updated; new exports `useAuditEventsQuery`, `useAuditEventTypesQuery`, types `AuditEventsQuery`, etc.

Verify: `grep -n 'useAuditEventsQuery' client/src/shared/middleware/graphql.ts`
Expected: at least one match.

- [ ] **Step 4: Commit operation + codegen separately**

```bash
git add client/codegen.ts client/src/graphql/auditEvents/
git commit -m "$(cat <<'EOF'
Add auditEvents GraphQL operation

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"

git add client/src/shared/middleware/graphql.ts
git commit -m "$(cat <<'EOF'
Regenerate graphql.ts for audit events query

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 11: Client — Filter bar component

**Files:**
- Create: `client/src/ee/pages/settings/platform/audit-events/components/AuditEventsFilterBar.tsx`

- [ ] **Step 1: Check which Select / DateRangePicker primitives exist**

Run:
```
ls client/src/components/ui/ 2>/dev/null | grep -iE 'select|date|popover|input'
```

Expected: at least `select.tsx`, `input.tsx`, some form of date picker (may be `calendar.tsx` + `popover.tsx`). If no date-range picker exists, fall back to two separate date inputs — do not invent a new component.

- [ ] **Step 2: Write the filter bar**

```tsx
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useAuditEventTypesQuery} from '@/shared/middleware/graphql';
import {XIcon} from 'lucide-react';
import {useEffect, useState} from 'react';

interface AuditEventsFilterBarPropsI {
    fromDate?: number;
    onChange: (filters: {
        eventType?: string;
        fromDate?: number;
        principal?: string;
        toDate?: number;
    }) => void;
    toDate?: number;
}

const AuditEventsFilterBar = ({fromDate, onChange, toDate}: AuditEventsFilterBarPropsI) => {
    const [eventType, setEventType] = useState<string | undefined>();
    const [principal, setPrincipal] = useState('');

    const {data: eventTypesData} = useAuditEventTypesQuery();

    useEffect(() => {
        const handle = window.setTimeout(() => {
            onChange({
                eventType,
                fromDate,
                principal: principal.trim() || undefined,
                toDate,
            });
        }, 300);

        return () => window.clearTimeout(handle);
    }, [principal, eventType, fromDate, toDate, onChange]);

    return (
        <fieldset className="flex flex-wrap items-end gap-3 border-0 p-0">
            <label className="flex flex-col gap-1 text-sm">
                <span className="text-muted-foreground">Principal</span>

                <Input
                    className="w-60"
                    onChange={(event) => setPrincipal(event.target.value)}
                    placeholder="e.g. admin@localhost.com"
                    value={principal}
                />
            </label>

            <label className="flex flex-col gap-1 text-sm">
                <span className="text-muted-foreground">Event type</span>

                <Select onValueChange={(value) => setEventType(value === '__all__' ? undefined : value)} value={eventType ?? '__all__'}>
                    <SelectTrigger className="w-60">
                        <SelectValue />
                    </SelectTrigger>

                    <SelectContent>
                        <SelectItem value="__all__">All</SelectItem>

                        {(eventTypesData?.auditEventTypes ?? []).map((type) => (
                            <SelectItem key={type} value={type}>
                                {type}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </label>

            <label className="flex flex-col gap-1 text-sm">
                <span className="text-muted-foreground">From</span>

                <Input
                    className="w-44"
                    onChange={(event) =>
                        onChange({
                            eventType,
                            fromDate: event.target.value ? new Date(event.target.value).getTime() : undefined,
                            principal: principal.trim() || undefined,
                            toDate,
                        })
                    }
                    type="date"
                    value={fromDate ? new Date(fromDate).toISOString().slice(0, 10) : ''}
                />
            </label>

            <label className="flex flex-col gap-1 text-sm">
                <span className="text-muted-foreground">To</span>

                <Input
                    className="w-44"
                    onChange={(event) =>
                        onChange({
                            eventType,
                            fromDate,
                            principal: principal.trim() || undefined,
                            toDate: event.target.value ? new Date(event.target.value).getTime() : undefined,
                        })
                    }
                    type="date"
                    value={toDate ? new Date(toDate).toISOString().slice(0, 10) : ''}
                />
            </label>

            {(principal || eventType || fromDate || toDate) && (
                <button
                    className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                    onClick={() => {
                        setEventType(undefined);
                        setPrincipal('');

                        onChange({});
                    }}
                    type="button"
                >
                    <XIcon className="size-4" /> Clear
                </button>
            )}
        </fieldset>
    );
};

export default AuditEventsFilterBar;
```

- [ ] **Step 3: Ensure it typechecks**

Run: `cd client && npm run typecheck`
Expected: no new errors in this file. If the `Select` / `Input` primitives import from a different path in this repo, adjust the imports.

---

## Task 12: Client — Detail sheet component

**Files:**
- Create: `client/src/ee/pages/settings/platform/audit-events/components/AuditEventDetailSheet.tsx`

- [ ] **Step 1: Check sheet primitive exists**

Run: `ls client/src/components/ui/sheet.tsx`
Expected: file exists. If not, use `dialog.tsx` as a fallback and adjust imports.

- [ ] **Step 2: Write the sheet**

```tsx
import {Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {AuditEventsQuery} from '@/shared/middleware/graphql';

type AuditEvent = NonNullable<AuditEventsQuery['auditEvents']['content']>[number];

interface AuditEventDetailSheetPropsI {
    auditEvent?: AuditEvent;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const AuditEventDetailSheet = ({auditEvent, onOpenChange, open}: AuditEventDetailSheetPropsI) => {
    return (
        <Sheet onOpenChange={onOpenChange} open={open}>
            <SheetContent className="w-[480px] sm:max-w-[480px]">
                <SheetHeader>
                    <SheetTitle>{auditEvent?.eventType ?? 'Audit Event'}</SheetTitle>

                    <SheetDescription>
                        {auditEvent?.eventDate ? new Date(auditEvent.eventDate).toLocaleString() : ''}
                    </SheetDescription>
                </SheetHeader>

                {auditEvent && (
                    <dl className="mt-6 grid grid-cols-[120px_1fr] gap-x-4 gap-y-2 text-sm">
                        <dt className="text-muted-foreground">Principal</dt>

                        <dd>{auditEvent.principal ?? 'anonymous'}</dd>

                        <dt className="text-muted-foreground">Event ID</dt>

                        <dd className="font-mono">{auditEvent.id}</dd>

                        {auditEvent.data.length > 0 && (
                            <>
                                <dt className="col-span-2 pt-4 font-semibold text-foreground">Data</dt>

                                {auditEvent.data.map((entry) => (
                                    <div className="contents" key={entry.key}>
                                        <dt className="break-words text-muted-foreground">{entry.key}</dt>

                                        <dd className="break-words font-mono">{entry.value}</dd>
                                    </div>
                                ))}
                            </>
                        )}
                    </dl>
                )}
            </SheetContent>
        </Sheet>
    );
};

export default AuditEventDetailSheet;
```

- [ ] **Step 3: Typecheck**

Run: `cd client && npm run typecheck`
Expected: no new errors in this file.

---

## Task 13: Client — Table + page

**Files:**
- Create: `client/src/ee/pages/settings/platform/audit-events/components/AuditEventsTable.tsx`
- Create: `client/src/ee/pages/settings/platform/audit-events/AuditEvents.tsx`

- [ ] **Step 1: Check Table / Pagination primitives**

Run:
```
ls client/src/components/ui/ | grep -iE 'table|pagination|button'
```

Expected: `table.tsx`, probably `pagination.tsx`, `button.tsx`. If `pagination.tsx` is missing, render simple Prev/Next buttons.

- [ ] **Step 2: Write the table component**

```tsx
import {Button} from '@/components/ui/button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {AuditEventsQuery} from '@/shared/middleware/graphql';
import {ChevronRightIcon} from 'lucide-react';

type AuditEvent = NonNullable<AuditEventsQuery['auditEvents']['content']>[number];

interface AuditEventsTablePropsI {
    auditEvents: AuditEvent[];
    onRowClick: (auditEvent: AuditEvent) => void;
}

const AuditEventsTable = ({auditEvents, onRowClick}: AuditEventsTablePropsI) => {
    if (auditEvents.length === 0) {
        return <p className="p-8 text-center text-sm text-muted-foreground">No audit events match the current filters.</p>;
    }

    return (
        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead>Date</TableHead>

                    <TableHead>Principal</TableHead>

                    <TableHead>Event Type</TableHead>

                    <TableHead className="w-8" />
                </TableRow>
            </TableHeader>

            <TableBody>
                {auditEvents.map((auditEvent) => (
                    <TableRow className="cursor-pointer" key={auditEvent.id} onClick={() => onRowClick(auditEvent)}>
                        <TableCell className="whitespace-nowrap font-mono text-xs">
                            {new Date(auditEvent.eventDate).toLocaleString()}
                        </TableCell>

                        <TableCell>{auditEvent.principal ?? 'anonymous'}</TableCell>

                        <TableCell>{auditEvent.eventType}</TableCell>

                        <TableCell>
                            <Button size="icon" variant="ghost">
                                <ChevronRightIcon className="size-4" />
                            </Button>
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    );
};

export default AuditEventsTable;
```

- [ ] **Step 3: Write the page**

```tsx
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Button} from '@/components/ui/button';
import {AuditEventsQuery, useAuditEventsQuery} from '@/shared/middleware/graphql';
import {useCallback, useState} from 'react';

import AuditEventDetailSheet from './components/AuditEventDetailSheet';
import AuditEventsFilterBar from './components/AuditEventsFilterBar';
import AuditEventsTable from './components/AuditEventsTable';

type AuditEvent = NonNullable<AuditEventsQuery['auditEvents']['content']>[number];

interface FiltersI {
    eventType?: string;
    fromDate?: number;
    principal?: string;
    toDate?: number;
}

const PAGE_SIZE = 25;

const AuditEvents = () => {
    const [filters, setFilters] = useState<FiltersI>({});
    const [page, setPage] = useState(0);
    const [selected, setSelected] = useState<AuditEvent | undefined>();
    const [sheetOpen, setSheetOpen] = useState(false);

    const {data, isLoading} = useAuditEventsQuery({
        eventType: filters.eventType,
        fromDate: filters.fromDate,
        page,
        principal: filters.principal,
        size: PAGE_SIZE,
        toDate: filters.toDate,
    });

    const handleFilterChange = useCallback((next: FiltersI) => {
        setFilters(next);
        setPage(0);
    }, []);

    const totalPages = data?.auditEvents.totalPages ?? 0;

    const content = data?.auditEvents.content ?? [];

    return (
        <LayoutContainer header={<Header centerTitle={true} position="main" title="Audit Events" />}>
            <div className="flex flex-col gap-4 p-6">
                <AuditEventsFilterBar
                    fromDate={filters.fromDate}
                    onChange={handleFilterChange}
                    toDate={filters.toDate}
                />

                {isLoading ? (
                    <p className="p-8 text-center text-sm text-muted-foreground">Loading…</p>
                ) : (
                    <AuditEventsTable
                        auditEvents={content}
                        onRowClick={(auditEvent) => {
                            setSelected(auditEvent);
                            setSheetOpen(true);
                        }}
                    />
                )}

                {totalPages > 1 && (
                    <div className="flex items-center justify-end gap-2">
                        <Button disabled={page === 0} onClick={() => setPage((current) => current - 1)} variant="outline">
                            Previous
                        </Button>

                        <span className="text-sm text-muted-foreground">
                            Page {page + 1} of {totalPages}
                        </span>

                        <Button
                            disabled={page + 1 >= totalPages}
                            onClick={() => setPage((current) => current + 1)}
                            variant="outline"
                        >
                            Next
                        </Button>
                    </div>
                )}

                <AuditEventDetailSheet auditEvent={selected} onOpenChange={setSheetOpen} open={sheetOpen} />
            </div>
        </LayoutContainer>
    );
};

export default AuditEvents;
```

(If `LayoutContainer` isn't the right wrapper here — check how `AdminApiKeys` is wrapped and mirror that. The page body matters; the chrome mirrors its siblings.)

- [ ] **Step 4: Typecheck**

Run: `cd client && npm run typecheck`
Expected: no new errors.

- [ ] **Step 5: Commit**

```bash
git add client/src/ee/pages/settings/platform/audit-events/
git commit -m "$(cat <<'EOF'
732 client - Add Audit Events settings page

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 14: Client — Route + nav wiring

**Files:**
- Modify: `client/src/routes.tsx`

- [ ] **Step 1: Add the lazy import near the other EE settings imports**

Around line 77 of `routes.tsx`:

```tsx
const AuditEvents = lazy(() => import('@/ee/pages/settings/platform/audit-events/AuditEvents'));
```

- [ ] **Step 2: Add the route entry**

Inside the platform-settings `children` array (right after the `admin-api-keys` entry):

```tsx
{
    element: (
        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
            <EEVersion>
                <LazyLoadWrapper>
                    <AuditEvents />
                </LazyLoadWrapper>
            </EEVersion>
        </PrivateRoute>
    ),
    path: 'audit-events',
},
```

- [ ] **Step 3: Add the nav item**

Inside `navItems[]`, right after `admin-api-keys`:

```tsx
{
    href: 'audit-events',
    title: 'Audit Events',
},
```

- [ ] **Step 4: Full client check**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests PASS. Fix any sort-keys / interface-naming / hook-order violations inline before proceeding.

- [ ] **Step 5: Commit**

```bash
git add client/src/routes.tsx
git commit -m "$(cat <<'EOF'
732 client - Wire Audit Events route and nav

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 15: End-to-end manual smoke test

- [ ] **Step 1: Start infra + server**

```bash
cd server && docker compose -f docker-compose.dev.infra.yml up -d && cd ..
./gradlew -p server/apps/server-app bootRun
```

Wait for "Started ServerApp" in the log.

- [ ] **Step 2: Start client**

```bash
cd client && npm run dev
```

- [ ] **Step 3: Log in and verify**

Open `http://localhost:3000`, log in as `admin@localhost.com` / `admin`.

Navigate to Settings → Audit Events (under Organization). Verify:
- Page loads.
- At least one row is present (your login triggered a `@PreAuthorize` somewhere → `PERMISSION_CHECK` row).
- Filter bar: type a principal, pick an event type, set a date range — the table filters.
- Click a row → sheet opens with event data.
- Create a connection and share it with a project → new `CONNECTION_CREATED` and `CONNECTION_SHARED` rows appear within ~1 second (refresh page).

- [ ] **Step 4: Verify non-admin is blocked**

Log out, log in as `user@localhost.com` / `user`. Navigate directly to `/settings/audit-events`. Expected: redirected / blocked by `PrivateRoute`.

- [ ] **Step 5: Verify CE build doesn't break**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL across the repo (the new listener and service are `@ConditionalOnEEVersion` so CE builds remain green).

---

## Final self-review

- All spec sections covered: rename ✓ (Task 1), repo extension ✓ (Task 3, 5), service read methods ✓ (Task 2, 4), listener bridge ✓ (Task 6, 7), graphql module + controller ✓ (Tasks 8, 9), codegen ✓ (Task 10), page + components ✓ (Tasks 11–13), route/nav ✓ (Task 14), smoke test ✓ (Task 15).
- Testing: unit (AuditEventService, Listener), integration (repository), manual smoke. Client has typecheck-level verification; a formal vitest for the page is omitted for v1 (consistent with how `AdminApiKeys` ships without its own vitest).
- No placeholders: every code block is complete.
- Known assumptions flagged where the engineer must verify against real code (published event class shape, shadcn primitives, source-set naming). These are verification steps with explicit fallback instructions, not hand-waves.
