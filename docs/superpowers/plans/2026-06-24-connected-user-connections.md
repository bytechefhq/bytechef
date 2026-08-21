# Connected-User-Scoped Connections Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Re-anchor embedded connected-user connections on `ConnectedUser` (new `connected_user_connection` table) and remove the per-workflow `connected_user_project_workflow_connection` table.

**Architecture:** Introduce a standalone Spring Data JDBC aggregate `ConnectedUserConnection` (id + `connected_user_id` + `connection_id`) with its own repository and service in `embedded-configuration`. The facade's write/read paths switch to it; the workflow→connection mapping, its table, and the now-dead `addConnection`/`isConnectionUsed` plumbing are removed. The writer REST endpoint is simplified to drop the meaningless `workflowUuid`.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, Liquibase, JUnit 5 + Mockito + AssertJ, OpenAPI generator (typescript-fetch), React 19/TS.

## Global Constraints

- All files under `server/ee/` use the **ByteChef Enterprise license header** and a `@version ee` Javadoc tag (Spotless picks the header by file content, so the tag must be present — including on test classes).
- Persist JDBC enums as INT ordinals; append new enum values at the end. (No enums change here, but keep in mind.)
- Spotless/Google Java Format wins on layout. Run `./gradlew spotlessApply` before every server commit.
- Never `git commit --amend` on branch `0_732` (user commits in parallel). Always fresh commits; stage only files this task touched.
- Server commit message convention: `<ticket> <description>`; client: `<ticket> client - <description>`. No ticket number assigned — use a clear descriptive message.
- End every commit message with:
  `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`
- Server gate: `./gradlew spotlessApply && ./gradlew check`. Client gate: `cd client && npm run check`.
- Existing dev DBs may need `docker compose -f server/docker-compose.dev.infra.yml down -v` before manual smoke if Liquibase checksums drift.

**Module path prefix (abbreviated below as `…/embedded-configuration-*`):**
`server/ee/libs/embedded/embedded-configuration/`

---

## Task 1: New persistence layer (entity + repository + service + create/backfill migration)

Additive only — the old table stays. Everything compiles and `check` stays green.

**Files:**
- Create: `…/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/domain/ConnectedUserConnection.java`
- Create: `…/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/service/ConnectedUserConnectionService.java`
- Create: `…/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/service/ConnectedUserConnectionServiceImpl.java`
- Create: `…/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/repository/ConnectedUserConnectionRepository.java`
- Create: `…/embedded-configuration-service/src/main/resources/config/liquibase/changelog/embedded/configuration/20240604183180_embedded_configuration_added_connected_user_connection.xml`
- Test: `…/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/service/ConnectedUserConnectionServiceTest.java`

**Interfaces:**
- Produces:
  - `ConnectedUserConnection` entity: `new ConnectedUserConnection()` + `setConnectedUserId(long)` + `setConnectionId(long)`; getters `Long getId()`, `Long getConnectedUserId()`, `Long getConnectionId()`.
  - `ConnectedUserConnectionService.create(long connectedUserId, long connectionId)` → void
  - `ConnectedUserConnectionService.getConnectionIds(long connectedUserId)` → `List<Long>`
  - Repository `findAllByConnectedUserId(long)` → `List<ConnectedUserConnection>`

- [ ] **Step 1: Write the failing test**

`ConnectedUserConnectionServiceTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserConnection;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserConnectionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserConnectionServiceTest {

    @Mock
    private ConnectedUserConnectionRepository connectedUserConnectionRepository;

    @Test
    void testCreate() {
        ConnectedUserConnectionService service =
            new ConnectedUserConnectionServiceImpl(connectedUserConnectionRepository);

        service.create(1L, 5L);

        ArgumentCaptor<ConnectedUserConnection> captor = ArgumentCaptor.forClass(ConnectedUserConnection.class);

        verify(connectedUserConnectionRepository).save(captor.capture());

        ConnectedUserConnection saved = captor.getValue();

        assertThat(saved.getConnectedUserId()).isEqualTo(1L);
        assertThat(saved.getConnectionId()).isEqualTo(5L);
    }

    @Test
    void testGetConnectionIds() {
        ConnectedUserConnectionService service =
            new ConnectedUserConnectionServiceImpl(connectedUserConnectionRepository);

        ConnectedUserConnection connection = new ConnectedUserConnection();

        connection.setConnectedUserId(1L);
        connection.setConnectionId(5L);

        when(connectedUserConnectionRepository.findAllByConnectedUserId(1L)).thenReturn(List.of(connection));

        assertThat(service.getConnectionIds(1L)).containsExactly(5L);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileTestJava`
Expected: FAIL — `ConnectedUserConnection`, `ConnectedUserConnectionService`, `ConnectedUserConnectionServiceImpl`, `ConnectedUserConnectionRepository` do not exist.

- [ ] **Step 3: Create the entity**

`ConnectedUserConnection.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.platform.connection.domain.Connection;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity that represents a connection owned by a ConnectedUser.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("connected_user_connection")
public class ConnectedUserConnection {

    @Id
    private Long id;

    @Column("connected_user_id")
    private AggregateReference<ConnectedUser, Long> connectedUserId;

    @Column("connection_id")
    private AggregateReference<Connection, Long> connectionId;

    public ConnectedUserConnection() {
    }

    @PersistenceCreator
    public ConnectedUserConnection(Long id, Long connectedUserId, Long connectionId) {
        this.id = id;
        this.connectedUserId = AggregateReference.to(connectedUserId);
        this.connectionId = AggregateReference.to(connectionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConnectedUserConnection that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public Long getConnectedUserId() {
        return connectedUserId.getId();
    }

    public Long getConnectionId() {
        return connectionId.getId();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setConnectedUserId(Long connectedUserId) {
        this.connectedUserId = AggregateReference.to(connectedUserId);
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = AggregateReference.to(connectionId);
    }

    @Override
    public String toString() {
        return "ConnectedUserConnection{" +
            "id=" + id +
            ", connectedUserId=" + connectedUserId +
            ", connectionId=" + connectionId +
            '}';
    }
}
```

- [ ] **Step 4: Create the repository**

`ConnectedUserConnectionRepository.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserConnection;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface ConnectedUserConnectionRepository extends ListCrudRepository<ConnectedUserConnection, Long> {

    @Query("""
        SELECT connected_user_connection.*
        FROM connected_user_connection
        WHERE connected_user_id = :connectedUserId
        """)
    List<ConnectedUserConnection> findAllByConnectedUserId(@Param("connectedUserId") long connectedUserId);
}
```

- [ ] **Step 5: Create the service interface**

`ConnectedUserConnectionService.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserConnectionService {

    void create(long connectedUserId, long connectionId);

    List<Long> getConnectionIds(long connectedUserId);
}
```

- [ ] **Step 6: Create the service implementation**

`ConnectedUserConnectionServiceImpl.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserConnection;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserConnectionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ConnectedUserConnectionServiceImpl implements ConnectedUserConnectionService {

    private final ConnectedUserConnectionRepository connectedUserConnectionRepository;

    public ConnectedUserConnectionServiceImpl(ConnectedUserConnectionRepository connectedUserConnectionRepository) {
        this.connectedUserConnectionRepository = connectedUserConnectionRepository;
    }

    @Override
    public void create(long connectedUserId, long connectionId) {
        ConnectedUserConnection connectedUserConnection = new ConnectedUserConnection();

        connectedUserConnection.setConnectedUserId(connectedUserId);
        connectedUserConnection.setConnectionId(connectionId);

        connectedUserConnectionRepository.save(connectedUserConnection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getConnectionIds(long connectedUserId) {
        return connectedUserConnectionRepository.findAllByConnectedUserId(connectedUserId)
            .stream()
            .map(ConnectedUserConnection::getConnectionId)
            .toList();
    }
}
```

- [ ] **Step 7: Create the Liquibase changelog (create table + index + FKs + backfill)**

`20240604183180_embedded_configuration_added_connected_user_connection.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20240604183180" author="Ivica Cardic">
        <createTable tableName="connected_user_connection">
            <column name="id" type="bigint" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="connected_user_id" type="bigint">
                <constraints nullable="false"/>
            </column>
            <column name="connection_id" type="bigint">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex indexName="idx_connected_user_connection_unique" tableName="connected_user_connection" unique="true">
            <column name="connected_user_id"/>
            <column name="connection_id"/>
        </createIndex>

        <sql>
            INSERT INTO connected_user_connection (connected_user_id, connection_id)
            SELECT DISTINCT cup.connected_user_id, cupwc.connection_id
            FROM connected_user_project_workflow_connection cupwc
            JOIN connected_user_project_workflow cupw ON cupw.id = cupwc.connected_user_project_workflow_id
            JOIN connected_user_project cup ON cup.id = cupw.connected_user_project_id
        </sql>
    </changeSet>

    <changeSet id="20240604183180-1" author="Ivica Cardic" contextFilter="mono">
        <addForeignKeyConstraint
            baseColumnNames="connected_user_id"
            baseTableName="connected_user_connection"
            constraintName="fk_connected_user_connection_connected_user"
            referencedColumnNames="id"
            referencedTableName="connected_user"/>

        <addForeignKeyConstraint
            baseColumnNames="connection_id"
            baseTableName="connected_user_connection"
            constraintName="fk_connected_user_connection_connection"
            referencedColumnNames="id"
            referencedTableName="connection"/>
    </changeSet>
</databaseChangeLog>
```

> Note: the changelog is auto-discovered via `includeAll` in
> `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml`
> (line 113, contextFilter `mono or configuration or multitenant`). No master edit needed.
> The `connection`-table FK lives in the `mono`-only changeset, mirroring the existing
> `20240604183140` pattern (in microservices the `connection` table is in another service).

- [ ] **Step 8: Run the service test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionServiceTest"`
Expected: PASS (2 tests).

- [ ] **Step 9: Format and commit**

```bash
./gradlew spotlessApply
git add "server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/domain/ConnectedUserConnection.java" \
        "server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/service/ConnectedUserConnectionService.java" \
        "server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/service/ConnectedUserConnectionServiceImpl.java" \
        "server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/repository/ConnectedUserConnectionRepository.java" \
        "server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/resources/config/liquibase/changelog/embedded/configuration/20240604183180_embedded_configuration_added_connected_user_connection.xml" \
        "server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/service/ConnectedUserConnectionServiceTest.java"
git commit -m "$(cat <<'EOF'
Add ConnectedUserConnection aggregate + backfill migration

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Switch facade + REST + remote-client + client to the new model

Re-point the write/read paths at `ConnectedUserConnectionService`, rename the writer
operation, and drop `workflowUuid`. The old table and old service methods still exist
(removed in Task 3), so `check` stays green throughout.

**Files:**
- Modify: `…/embedded-configuration-api/.../facade/ConnectedUserConnectionFacade.java`
- Modify: `…/embedded-configuration-service/.../facade/ConnectedUserConnectionFacadeImpl.java`
- Modify: `…/embedded-configuration-remote-client/.../facade/RemoteConnectedUserConnectionFacadeClient.java`
- Modify: `…/embedded-configuration-rest/embedded-configuration-rest-impl/openapi.yaml` (lines ~384-413)
- Modify: `…/embedded-configuration-rest/embedded-configuration-rest-impl/.../web/rest/ConnectionApiController.java:63-70`
- Regenerated (by gradle task): `…/embedded-configuration-rest-api/generated/**` and `client/src/ee/shared/middleware/embedded/configuration/**`
- Modify: `client/src/ee/shared/mutations/embedded/connections.mutations.ts:9-21`
- Modify: `client/src/ee/pages/embedded/workflow-builder/WorkflowBuilder.tsx:96-99`
- Test: `…/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserConnectionFacadeTest.java` (new)

**Interfaces:**
- Consumes (from Task 1): `ConnectedUserConnectionService.create(long, long)`, `ConnectedUserConnectionService.getConnectionIds(long)`.
- Produces:
  - `ConnectedUserConnectionFacade.createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO)` → `long`
  - REST `POST /connected-users/{connectedUserId}/connections`, operationId `createConnectedUserConnection`.
  - Client `getCreateConnectedUserConnection(connectedUserId: number)`.

- [ ] **Step 1: Write the failing facade test**

`ConnectedUserConnectionFacadeTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserConnectionFacadeTest {

    @Mock
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Mock
    private ConnectedUserService connectedUserService;

    @Mock
    private ConnectionFacade connectionFacade;

    @Mock
    private IntegrationInstanceService integrationInstanceService;

    private ConnectedUserConnectionFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new ConnectedUserConnectionFacadeImpl(
            connectedUserConnectionService, connectedUserService, connectionFacade, integrationInstanceService);
    }

    @Test
    void testCreateConnectedUserConnection() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .build();

        when(connectionFacade.create(connectionDTO, PlatformType.EMBEDDED)).thenReturn(5L);

        long connectionId = facade.createConnectedUserConnection(1L, connectionDTO);

        assertThat(connectionId).isEqualTo(5L);

        verify(connectedUserConnectionService).create(1L, 5L);
    }

    @Test
    void testGetConnectionsMergesInstanceAndConnectedUserConnectionIds() {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setId(1L);
        connectedUser.setEnvironment(Environment.PRODUCTION);

        when(connectedUserService.getConnectedUser(1L)).thenReturn(connectedUser);

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectionId(10L);

        when(integrationInstanceService.getIntegrationInstances(1L, "slack", Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance));
        when(connectedUserConnectionService.getConnectionIds(1L)).thenReturn(List.of(20L));
        when(connectionFacade.getConnections(List.of(10L, 20L), PlatformType.EMBEDDED)).thenReturn(List.of());

        facade.getConnections(1L, "slack", List.of());

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.captor();

        verify(connectionFacade).getConnections(captor.capture(), eq(PlatformType.EMBEDDED));

        assertThat(captor.getValue()).containsExactly(10L, 20L);
    }
}
```

> Verify exact construction APIs while writing: confirm `ConnectionDTO.builder().build()` exists,
> and that `ConnectedUser` exposes `setId(Long)` / `setEnvironment(Environment)` and
> `IntegrationInstance` exposes `setConnectionId(Long)` (read the domain classes; adjust the test
> to real setters/constructors if they differ — do not invent APIs).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileTestJava`
Expected: FAIL — `createConnectedUserConnection` and the 4-arg `ConnectedUserConnectionFacadeImpl` constructor don't exist yet.

- [ ] **Step 3: Update the facade interface**

`ConnectedUserConnectionFacade.java` — replace the `createConnectedUserProjectWorkflowConnection` method:

```java
    long createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO);

    List<ConnectionDTO> getConnections(Long connectedUserId, String componentName, List<Long> connectionIds);
```

- [ ] **Step 4: Rewrite the facade implementation**

Replace the body of `ConnectedUserConnectionFacadeImpl.java` (imports + fields + constructor + methods):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ConnectedUserConnectionFacadeImpl implements ConnectedUserConnectionFacade {

    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionFacade connectionFacade;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionFacadeImpl(
        ConnectedUserConnectionService connectedUserConnectionService, ConnectedUserService connectedUserService,
        ConnectionFacade connectionFacade, IntegrationInstanceService integrationInstanceService) {

        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectedUserService = connectedUserService;
        this.connectionFacade = connectionFacade;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    public long createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO) {
        long connectionId = connectionFacade.create(connectionDTO, PlatformType.EMBEDDED);

        connectedUserConnectionService.create(connectedUserId, connectionId);

        return connectionId;
    }

    @Override
    public List<ConnectionDTO> getConnections(
        Long connectedUserId, String componentName, List<Long> connectionIds) {

        List<Long> allConnectionIds = new ArrayList<>();

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        allConnectionIds.addAll(
            integrationInstanceService
                .getIntegrationInstances(connectedUser.getId(), componentName, connectedUser.getEnvironment())
                .stream()
                .map(IntegrationInstance::getConnectionId)
                .toList());

        allConnectionIds.addAll(connectedUserConnectionService.getConnectionIds(connectedUser.getId()));

        List<ConnectionDTO> connectionDTOs = new ArrayList<>(
            connectionFacade.getConnections(allConnectionIds, PlatformType.EMBEDDED));

        connectionDTOs.addAll(
            connectionIds.stream()
                .map(connectionFacade::getConnection)
                .toList());

        return connectionDTOs
            .stream()
            .filter(connectionDTO -> componentName.equals(connectionDTO.componentName()))
            .toList();
    }
}
```

- [ ] **Step 5: Update the remote-client stub**

`RemoteConnectedUserConnectionFacadeClient.java` — replace the create method:

```java
    @Override
    public long createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO) {
        throw new UnsupportedOperationException();
    }
```

(Leave the `getConnections` stub unchanged.)

- [ ] **Step 6: Run the facade test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacadeTest"`
Expected: PASS (2 tests).

- [ ] **Step 7: Update the OpenAPI spec**

In `openapi.yaml`, replace the entire `/connected-users/{connectedUserId}/workflows/{workflowUuid}/connections` block (path + post operation) with:

```yaml
  /connected-users/{connectedUserId}/connections:
    post:
      description: "Create a new connection for the connected user."
      summary: "Create a new connection for the connected user"
      tags:
        - "connection"
      operationId: "createConnectedUserConnection"
      parameters:
        - name: "connectedUserId"
          description: "The id of a connected user."
          in: "path"
          required: true
          schema:
            type: "integer"
            format: "int64"
      requestBody:
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/Connection"
        required: true
      responses:
        "200":
          description: "The connection id."
          content:
            application/json:
              schema:
                type: integer
                format: int64
```

- [ ] **Step 8: Regenerate REST API (Java) + TS middleware**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-impl:generateOpenAPI`
Expected: regenerates `embedded-configuration-rest-api/generated/**` (new `createConnectedUserConnection` in `ConnectionApi.java`) and `client/src/ee/shared/middleware/embedded/configuration/**` (renamed method, dropped `workflowUuid`).

- [ ] **Step 9: Update the controller**

`ConnectionApiController.java` — replace the `createConnectedUserProjectWorkflowConnection` override (lines 63-70):

```java
    @Override
    public ResponseEntity<Long> createConnectedUserConnection(Long connectedUserId, ConnectionModel connectionModel) {
        return ResponseEntity.ok(
            connectedUserConnectionFacade.createConnectedUserConnection(
                connectedUserId, conversionService.convert(connectionModel, ConnectionDTO.class)));
    }
```

- [ ] **Step 10: Compile the server slice**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-impl:compileJava`
Expected: PASS.

- [ ] **Step 11: Update the client mutation factory**

`client/src/ee/shared/mutations/embedded/connections.mutations.ts` — replace lines 9-21:

```ts
export const getCreateConnectedUserConnection =
    (connectedUserId: number) => (mutationProps?: CreateConnectionMutationProps) =>
        useMutation<number, Error, Connection>({
            mutationFn: (connection: Connection) => {
                return new ConnectionApi().createConnectedUserConnection({
                    connectedUserId,
                    connection,
                });
            },
            onError: mutationProps?.onError,
            onSuccess: mutationProps?.onSuccess,
        });
```

- [ ] **Step 12: Update the client call site**

`client/src/ee/pages/embedded/workflow-builder/WorkflowBuilder.tsx`:
- Update the import on line 5: `getCreateConnectedUserProjectWorkflowConnection` → `getCreateConnectedUserConnection`.
- Replace lines 96-99:

```tsx
                                useCreateConnectionMutation: getCreateConnectedUserConnection(
                                    connectedUserProjectWorkflow.connectedUserId!
                                ),
```

- [ ] **Step 13: Run client checks**

Run: `cd client && npm run check`
Expected: PASS (lint + typecheck + tests). Fix any sort-keys/import-order issues manually.

- [ ] **Step 14: Format and commit (server + client together)**

```bash
cd /Volumes/Data/bytechef/bytechef
./gradlew spotlessApply
git add "server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserConnectionFacade.java" \
        "server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserConnectionFacadeImpl.java" \
        "server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-client/src/main/java/com/bytechef/ee/embedded/configuration/remote/client/facade/RemoteConnectedUserConnectionFacadeClient.java" \
        "server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl/openapi.yaml" \
        "server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl/src/main/java/com/bytechef/ee/embedded/configuration/web/rest/ConnectionApiController.java" \
        "server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-api/generated" \
        "server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserConnectionFacadeTest.java"
git commit -m "$(cat <<'EOF'
Anchor embedded connected-user connections on the connected user

Switch the write/read paths and REST surface to createConnectedUserConnection,
dropping the meaningless workflowUuid path param.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
git add client/src/ee/shared/middleware/embedded/configuration \
        client/src/ee/shared/mutations/embedded/connections.mutations.ts \
        client/src/ee/pages/embedded/workflow-builder/WorkflowBuilder.tsx
git commit -m "$(cat <<'EOF'
- client - Use createConnectedUserConnection mutation

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Remove the old per-workflow connection plumbing + drop the table

Now that nothing reads or writes the old table, delete the mapping, dead service
methods, the cleanup in `deleteProjectWorkflow`, and the table itself — all in one
task so `check` stays green (Spring Data JDBC would fail to load
`ConnectedUserProjectWorkflow` if the mapping outlived the table).

**Files:**
- Delete: `…/embedded-configuration-api/.../domain/ConnectedUserProjectWorkflowConnection.java`
- Modify: `…/embedded-configuration-api/.../domain/ConnectedUserProjectWorkflow.java`
- Modify: `…/embedded-configuration-api/.../service/ConnectedUserProjectWorkflowService.java`
- Modify: `…/embedded-configuration-service/.../service/ConnectedUserProjectWorkflowServiceImpl.java`
- Modify: `…/embedded-configuration-service/.../repository/ConnectedUserProjectWorkflowRepository.java`
- Modify: `…/embedded-configuration-service/.../facade/ConnectedUserProjectFacadeImpl.java` (deleteProjectWorkflow, lines 248-275)
- Create: `…/embedded-configuration-service/.../changelog/embedded/configuration/20240604183190_embedded_configuration_dropped_connected_user_project_workflow_connection.xml`

**Interfaces:**
- Consumes: nothing new.
- Produces: `ConnectedUserProjectWorkflow` with no `connections`; `ConnectedUserProjectWorkflowService` without `addConnection`/`isConnectionUsed`.

- [ ] **Step 1: Pre-flight grep — confirm zero remaining references**

Run:
```bash
cd /Volumes/Data/bytechef/bytechef
grep -rn "ConnectedUserProjectWorkflowConnection\|connected_user_project_workflow_connection" --include="*.java" --include="*.xml" server | grep -v "/generated/" | grep -v "20240604183140\|20240604183190"
grep -rn "\.addConnection\b" --include="*.java" server | grep -i "connectedUserProjectWorkflow"
grep -rn "implements ConnectedUserProjectWorkflowService" --include="*.java" server
```
Expected: only the files this task edits/deletes appear (entity, service impl, repository). The `implements ConnectedUserProjectWorkflowService` check should show only `ConnectedUserProjectWorkflowServiceImpl` — if a remote-client stub also implements it, add that file to this task and drop the two methods there too.

- [ ] **Step 2: Remove the mapping from `ConnectedUserProjectWorkflow`**

In `ConnectedUserProjectWorkflow.java`:
- Delete the field (lines ~69-70):
  ```java
  @MappedCollection(idColumn = "connected_user_project_workflow_id")
  private Set<ConnectedUserProjectWorkflowConnection> connections = new HashSet<>();
  ```
- Delete the methods `addConnection`, `getConnections`, `getConnectionsCount`, `setConnections`.
- Remove `", connections=" + connections +` from `toString`.
- Remove now-unused imports: `java.util.HashSet`, `java.util.Set`, `org.springframework.data.relational.core.mapping.MappedCollection`. Keep `java.util.List` only if still used elsewhere in the class (it is not after removal — remove it too).

- [ ] **Step 3: Delete the child entity**

```bash
git rm "server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/domain/ConnectedUserProjectWorkflowConnection.java"
```

- [ ] **Step 4: Remove dead methods from the workflow service interface**

In `ConnectedUserProjectWorkflowService.java`, delete:
```java
    void addConnection(long connectedUserProjectId, long projectWorkflowId, long connectionId);
```
and
```java
    boolean isConnectionUsed(long connectionId);
```

- [ ] **Step 5: Remove dead methods from the workflow service impl**

In `ConnectedUserProjectWorkflowServiceImpl.java`, delete the `addConnection` method (lines ~36-44) and the `isConnectionUsed` method (lines ~85-91).

- [ ] **Step 6: Remove the dead query from the repository**

In `ConnectedUserProjectWorkflowRepository.java`, delete the
`findConnectedUserProjectWorkflowConnectionIdsByConnectionId` method and its `@Query` (lines ~37-41).
Remove the now-unused `org.springframework.data.repository.query.Param` /
`org.springframework.data.jdbc.repository.query.Query` imports **only if** no longer used
(the `findAllByConnectedUserProjectId` `@Query` still uses both — keep them).

- [ ] **Step 7: Strip connection cleanup from `deleteProjectWorkflow`**

In `ConnectedUserProjectFacadeImpl.java`, replace the `deleteProjectWorkflow(String, String, Environment)` body (lines 248-275) with:

```java
    @Override
    public void deleteProjectWorkflow(String externalUserId, String workflowUuid, Environment environment) {
        ConnectedUserProject connectedUserProject = checkConnectedUserProject(externalUserId, environment);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            connectedUserProject.getProjectId(), workflowUuid);

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            connectedUserProjectWorkflowService
                .fetchConnectedUserProjectWorkflow(connectedUserProject.getId(), projectWorkflow.getId())
                .ifPresent(connectedUserProjectWorkflow ->
                    connectedUserProjectWorkflowService.delete(connectedUserProjectWorkflow.getId()));

            projectWorkflowFacade.deleteWorkflow(projectWorkflow.getWorkflowId());
        }
    }
```

Then remove the now-unused `ConnectedUserProjectWorkflowConnection` import. Check whether the
`connectionService` field and `java.util.HashSet`/`java.util.Set` imports are still used elsewhere
in the file:
```bash
grep -n "connectionService\|HashSet\|Set<" server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectFacadeImpl.java
```
If `connectionService` has no other use, remove the field, its constructor parameter, and its import (and update any `ConnectedUserProjectFacadeImpl(...)` test constructors — e.g. `ConnectedUserProjectFacadeTest` uses positional `null`s; drop the corresponding slot). If it is still used, leave it.

- [ ] **Step 8: Create the drop-table migration**

`20240604183190_embedded_configuration_dropped_connected_user_project_workflow_connection.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20240604183190" author="Ivica Cardic">
        <dropTable tableName="connected_user_project_workflow_connection"/>
    </changeSet>
</databaseChangeLog>
```

> `dropTable` removes the table's own FK constraints and indexes in PostgreSQL, so no explicit
> `dropForeignKeyConstraint` is needed. The backfill in `20240604183180` already ran first
> (lower filename sorts earlier under `includeAll`).

- [ ] **Step 9: Compile + run the module's tests**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:compileJava :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test`
Expected: PASS. If `ConnectedUserProjectFacadeTest`'s constructor arg count changed in Step 7, it was updated there.

- [ ] **Step 10: Format and commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/embedded/embedded-configuration
git commit -m "$(cat <<'EOF'
Remove per-workflow connected-user connection table and dead plumbing

Drop connected_user_project_workflow_connection and the now-unused
addConnection/isConnectionUsed paths; workflow delete no longer cascades
to user-scoped connections.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Full verification + manual smoke

**Files:** none (verification only).

- [ ] **Step 1: Full server check**

Run: `./gradlew spotlessApply && ./gradlew check`
Expected: BUILD SUCCESSFUL. The full `check` loads EE app contexts (which use the remote-client) and runs Liquibase against the test DB — this is the gate that catches mapping/migration/context regressions.

- [ ] **Step 2: Full client check**

Run: `cd client && npm run check`
Expected: PASS.

- [ ] **Step 3: Manual smoke (fresh DB)**

```bash
cd server
docker compose -f docker-compose.dev.infra.yml down -v
docker compose -f docker-compose.dev.infra.yml up -d
cd ..
./gradlew -p server/apps/server-app bootRun
```
Verify on startup: Liquibase applies `20240604183180` and `20240604183190` with no errors, and the
`connected_user_connection` table exists while `connected_user_project_workflow_connection` is gone:
```sql
\d connected_user_connection
SELECT to_regclass('connected_user_project_workflow_connection');  -- expect NULL
```
Then, against an embedded connected user, exercise:
- `POST /internal/connected-users/{connectedUserId}/connections` → returns a connection id, inserts a `connected_user_connection` row.
- `GET /internal/connected-users/{connectedUserId}/components/{componentName}/connections` → returns that connection.

- [ ] **Step 4: Update spec status**

Edit `docs/superpowers/specs/2026-06-24-connected-user-connections-design.md` — change `Status: Proposed` to `Status: Implemented`. Commit:

```bash
git add docs/superpowers/specs/2026-06-24-connected-user-connections-design.md
git commit -m "$(cat <<'EOF'
Mark connected-user connections spec implemented

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review

**Spec coverage:**
- Anchor on `ConnectedUser` / new standalone aggregate → Task 1 (entity, repo, service). ✅
- Remove `connected_user_project_workflow_connection` + entity mapping + dead methods → Task 3. ✅
- Read simplifies to instance ∪ `getConnectionIds` → Task 2 facade rewrite. ✅
- Connections persist on workflow delete (cleanup removed) → Task 3 Step 7. ✅
- Clean writer API (`createConnectedUserConnection`, drop `workflowUuid`) → Task 2 (openapi + controller + remote-client + client). ✅
- Backfill migration (dedup via FK-chain walk) → Task 1 Step 7. ✅
- Drop old table after code stops referencing it → Task 3 Step 8. ✅
- Tests (facade unit + service unit) → Tasks 1 & 2. ✅
- Out-of-scope items (deleteConnectedUser cascade, generic /connections) untouched. ✅

**Placeholder scan:** No TBD/TODO; every code step shows full content. Steps that depend on real
APIs (`ConnectionDTO.builder()`, `ConnectedUser`/`IntegrationInstance` setters, `connectionService`
field usage) carry explicit "verify against the real class" instructions rather than assumptions.

**Type consistency:** `createConnectedUserConnection(long, ConnectionDTO)` and the 4-arg
`ConnectedUserConnectionFacadeImpl` constructor match between interface (Task 2 Step 3), impl (Step 4),
remote-client (Step 5), controller (Step 9), and the test (Step 1). `ConnectedUserConnectionService`
signatures match between interface, impl, and both tests.
