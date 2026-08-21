# Connection Visibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add multi-layered visibility scoping (PRIVATE/PROJECT/WORKSPACE/ORGANIZATION) to ByteChef connections with permission enforcement, environment validation, and graceful user removal flows.

**Architecture:** Three-phase implementation. Phase 1 adds the core visibility model (PRIVATE/PROJECT/WORKSPACE), permission enforcement in the service layer, environment enforcement at deploy time, and grouped connection picker UI. Phase 2 adds ORGANIZATION scope with Settings page management. Phase 3 adds user removal flows, workflow pausing, and audit logging. All connection CRUD uses GraphQL (not REST) for new endpoints. Permission and environment enforcement live in the service layer.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring Data JDBC / Spring GraphQL / Liquibase / React 19 / TypeScript 5.9 / GraphQL Code Generator

**Key codebase patterns:**
- Domain entities in `*-api` modules, services/repos in `*-service` modules
- GraphQL controllers in `*-graphql` modules with `.graphqls` schema files
- `@QueryMapping` / `@MutationMapping` / `@SchemaMapping` / `@BatchMapping` annotations
- `SecurityUtils.getCurrentUserLogin()` for current user identity
- `@PreAuthorize("hasAuthority(\"ROLE_ADMIN\")")` for admin-only mutations
- `WorkspaceConnection` join table pattern for workspace scoping
- Frontend GraphQL operations in `client/src/graphql/automation/` → codegen to `client/src/shared/middleware/graphql.ts`

---

# Phase 1 — Core Visibility Model

## Task 1: Add ConnectionVisibility enum and update Connection entity

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/ConnectionVisibility.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/Connection.java`

- [ ] **Step 1: Create ConnectionVisibility enum**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.bytechef.platform.connection.domain;

/**
 * @author Ivica Cardic
 */
public enum ConnectionVisibility {

    PRIVATE,    // ordinal 0 — visible only to creator
    PROJECT,    // ordinal 1 — visible to members of shared projects
    WORKSPACE   // ordinal 2 — visible to all workspace members

}
```

- [ ] **Step 2: Add visibility field to Connection entity**

In `Connection.java`, add the field after the existing `credentialStatus` field (around line 69):

```java
@Column
private int visibility;
```

Add getter and setter:

```java
public ConnectionVisibility getVisibility() {
    return ConnectionVisibility.values()[visibility];
}

public void setVisibility(ConnectionVisibility visibility) {
    this.visibility = visibility.ordinal();
}
```

Update the default constructor to set `visibility = ConnectionVisibility.PRIVATE.ordinal()`:

```java
public Connection() {
    this.parameters = new EncryptedMapWrapper(Collections.emptyMap());
    this.visibility = ConnectionVisibility.PRIVATE.ordinal();
}
```

- [ ] **Step 3: Run compile**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/libs/platform/platform-connection/platform-connection-api/
git commit -m "Add ConnectionVisibility enum and visibility field to Connection entity"
```

---

## Task 2: Add Liquibase migration for visibility column and project_connection table

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-service/src/main/resources/config/liquibase/changelog/platform/connection/00000000000002_platform_connection_add_visibility.xml`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/00000000000002_automation_configuration_project_connection_init.xml`
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/resources/config/liquibase/changelog/platform/connection/db.changelog-master.xml` (add include)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/db.changelog-master.xml` (add include)

- [ ] **Step 1: Create visibility column migration**

File: `00000000000002_platform_connection_add_visibility.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="00000000000002-01" author="ivicac">
        <addColumn tableName="connection">
            <column name="visibility" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </addColumn>

        <comment>Set existing connections to WORKSPACE (2) to preserve current behavior</comment>
        <update tableName="connection">
            <column name="visibility" valueNumeric="2"/>
        </update>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: Create project_connection table migration**

File: `00000000000002_automation_configuration_project_connection_init.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="00000000000002-01" author="ivicac">
        <createTable tableName="project_connection">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="project_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="connection_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP WITHOUT TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="TIMESTAMP WITHOUT TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addUniqueConstraint tableName="project_connection"
                             columnNames="project_id, connection_id"
                             constraintName="uq_project_connection"/>

        <addForeignKeyConstraint baseTableName="project_connection"
                                 baseColumnNames="connection_id"
                                 referencedTableName="connection"
                                 referencedColumnNames="id"
                                 constraintName="fk_project_connection_connection"
                                 onDelete="CASCADE"/>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 3: Add includes to changelog masters**

Add to the platform connection `db.changelog-master.xml`:
```xml
<include file="config/liquibase/changelog/platform/connection/00000000000002_platform_connection_add_visibility.xml"/>
```

Add to the automation configuration `db.changelog-master.xml`:
```xml
<include file="config/liquibase/changelog/automation/configuration/00000000000002_automation_configuration_project_connection_init.xml"/>
```

- [ ] **Step 4: Commit**

```bash
git add server/libs/platform/platform-connection/platform-connection-service/src/main/resources/
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/
git commit -m "Add Liquibase migration for visibility column and project_connection table"
```

---

## Task 3: Create ProjectConnection entity, repository, and service

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/ProjectConnection.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/repository/ProjectConnectionRepository.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ProjectConnectionService.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ProjectConnectionServiceImpl.java`

- [ ] **Step 1: Create ProjectConnection entity**

Follow the exact pattern of `WorkspaceConnection.java` (same module):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */

package com.bytechef.automation.configuration.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("project_connection")
public class ProjectConnection {

    @Column("connection_id")
    private Long connectionId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("project_id")
    private Long projectId;

    @Version
    private int version;

    private ProjectConnection() {
    }

    public ProjectConnection(Long connectionId, Long projectId) {
        Validate.notNull(connectionId, "connectionId must not be null");
        Validate.notNull(projectId, "projectId must not be null");

        this.connectionId = connectionId;
        this.projectId = projectId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ProjectConnection projectConnection)) {
            return false;
        }

        return Objects.equals(id, projectConnection.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Long getProjectId() {
        return projectId;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ProjectConnection{" +
            "id=" + id +
            ", projectId=" + projectId +
            ", connectionId=" + connectionId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            '}';
    }
}
```

- [ ] **Step 2: Create ProjectConnectionRepository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */

package com.bytechef.automation.configuration.repository;

import com.bytechef.automation.configuration.domain.ProjectConnection;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ProjectConnectionRepository extends ListCrudRepository<ProjectConnection, Long> {

    List<ProjectConnection> findAllByConnectionId(long connectionId);

    List<ProjectConnection> findAllByProjectId(long projectId);

    List<ProjectConnection> findAllByProjectIdIn(List<Long> projectIds);

    void deleteAllByConnectionId(long connectionId);

    void deleteByConnectionIdAndProjectId(long connectionId, long projectId);
}
```

- [ ] **Step 3: Create ProjectConnectionService interface**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.ProjectConnection;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectConnectionService {

    ProjectConnection create(long connectionId, long projectId);

    void delete(long connectionId, long projectId);

    void deleteByConnectionId(long connectionId);

    List<ProjectConnection> getProjectConnections(long projectId);

    List<ProjectConnection> getProjectConnectionsByProjectIds(List<Long> projectIds);

    List<ProjectConnection> getConnectionProjects(long connectionId);
}
```

- [ ] **Step 4: Create ProjectConnectionServiceImpl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.ProjectConnection;
import com.bytechef.automation.configuration.repository.ProjectConnectionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectConnectionServiceImpl implements ProjectConnectionService {

    private final ProjectConnectionRepository projectConnectionRepository;

    public ProjectConnectionServiceImpl(ProjectConnectionRepository projectConnectionRepository) {
        this.projectConnectionRepository = projectConnectionRepository;
    }

    @Override
    public ProjectConnection create(long connectionId, long projectId) {
        return projectConnectionRepository.save(new ProjectConnection(connectionId, projectId));
    }

    @Override
    public void delete(long connectionId, long projectId) {
        projectConnectionRepository.deleteByConnectionIdAndProjectId(connectionId, projectId);
    }

    @Override
    public void deleteByConnectionId(long connectionId) {
        projectConnectionRepository.deleteAllByConnectionId(connectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectConnection> getProjectConnections(long projectId) {
        return projectConnectionRepository.findAllByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectConnection> getProjectConnectionsByProjectIds(List<Long> projectIds) {
        return projectConnectionRepository.findAllByProjectIdIn(projectIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectConnection> getConnectionProjects(long connectionId) {
        return projectConnectionRepository.findAllByConnectionId(connectionId);
    }
}
```

- [ ] **Step 5: Run compile**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:compileJava :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add server/libs/automation/automation-configuration/
git commit -m "Add ProjectConnection entity, repository, and service"
```

---

## Task 4: Add visibility field to ConnectionDTO and ConnectionService

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/dto/ConnectionDTO.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionService.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java`

- [ ] **Step 1: Add visibility to ConnectionDTO record**

In `ConnectionDTO.java`, add `ConnectionVisibility visibility` to the record components. The record definition becomes:

```java
public record ConnectionDTO(
    boolean active, @Nullable AuthorizationType authorizationType, Map<String, ?> authorizationParameters,
    String baseUri, String componentName, Map<String, ?> connectionParameters, int connectionVersion, String createdBy,
    Instant createdDate, CredentialStatus credentialStatus, int environmentId, Long id, String lastModifiedBy,
    Instant lastModifiedDate, String name, Map<String, ?> parameters, List<Long> sharedProjectIds,
    List<Tag> tags, int version, ConnectionVisibility visibility)
```

Update the convenience constructor that takes a `Connection` object to pass `connection.getVisibility()` and an empty list for `sharedProjectIds`:

```java
public ConnectionDTO(
    boolean active, Map<String, ?> authorizationParameters, String baseUri, Connection connection,
    Map<String, ?> connectionParameters, List<Long> sharedProjectIds, List<Tag> tags) {

    this(
        active, connection.getAuthorizationType() == null ? null
            : AuthorizationType.values()[connection.getAuthorizationType()],
        authorizationParameters, baseUri, connection.getComponentName(), connectionParameters,
        connection.getConnectionVersion(), connection.getCreatedBy(), connection.getCreatedDate(),
        CredentialStatus.values()[connection.getCredentialStatus()], connection.getEnvironment(),
        connection.getId(), connection.getLastModifiedBy(), connection.getLastModifiedDate(), connection.getName(),
        connection.getParameters(), sharedProjectIds, tags, connection.getVersion(), connection.getVisibility());
}
```

- [ ] **Step 2: Add visibility update method to ConnectionService**

In `ConnectionService.java`, add:

```java
Connection updateVisibility(long id, ConnectionVisibility visibility);
```

In `ConnectionServiceImpl.java`, implement:

```java
@Override
public Connection updateVisibility(long id, ConnectionVisibility visibility) {
    Connection connection = connectionRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

    connection.setVisibility(visibility);

    return connectionRepository.save(connection);
}
```

- [ ] **Step 3: Fix all callers of the old ConnectionDTO constructor**

The old constructor signature changed (added `sharedProjectIds` parameter). Find all call sites using:

Run: `grep -rn "new ConnectionDTO(" server/libs/ --include="*.java"`

Update each call site to pass `List.of()` for `sharedProjectIds`. Key files:
- `ConnectionFacadeImpl.java` — the `toConnectionDTO` method
- Any other facade/service that constructs ConnectionDTO

- [ ] **Step 4: Run compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-connection/
git commit -m "Add visibility field to ConnectionDTO and ConnectionService"
```

---

## Task 5: Add visibility-aware filtering to WorkspaceConnectionFacade

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacade.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java`

- [ ] **Step 1: Add new methods to WorkspaceConnectionFacade interface**

```java
void shareConnectionToProject(long connectionId, long projectId);

void revokeConnectionFromProject(long connectionId, long projectId);

void promoteToWorkspace(long connectionId);

void demoteToPrivate(long connectionId);
```

- [ ] **Step 2: Update getConnections to apply visibility filtering**

In `WorkspaceConnectionFacadeImpl.java`, inject `ProjectConnectionService` and `SecurityUtils`. Update `getConnections`:

```java
@Override
public List<ConnectionDTO> getConnections(
    long workspaceId, String componentName, Integer connectionVersion, Long environmentId, Long tagId) {

    List<Long> connectionIds = CollectionUtils.map(
        workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId);

    if (connectionIds.isEmpty()) {
        return List.of();
    }

    List<ConnectionDTO> allConnections = connectionFacade.getConnections(
        componentName, connectionVersion, connectionIds, tagId, environmentId, PlatformType.AUTOMATION);

    String currentUserLogin = SecurityUtils.getCurrentUserLogin();

    // Get project IDs for projects the current user has access to in this workspace
    List<Long> userProjectIds = projectService.getWorkspaceProjectIds(workspaceId);

    List<Long> projectConnectionIds = CollectionUtils.map(
        projectConnectionService.getProjectConnectionsByProjectIds(userProjectIds),
        ProjectConnection::getConnectionId);

    return allConnections.stream()
        .filter(connection -> {
            ConnectionVisibility visibility = connection.visibility();

            return switch (visibility) {
                case WORKSPACE -> true;
                case PROJECT -> projectConnectionIds.contains(connection.id());
                case PRIVATE -> currentUserLogin.equals(connection.createdBy());
            };
        })
        .toList();
}
```

- [ ] **Step 3: Implement share/promote/demote methods**

```java
@Override
public void shareConnectionToProject(long connectionId, long projectId) {
    projectConnectionService.create(connectionId, projectId);

    connectionService.updateVisibility(connectionId, ConnectionVisibility.PROJECT);
}

@Override
public void revokeConnectionFromProject(long connectionId, long projectId) {
    projectConnectionService.delete(connectionId, projectId);

    List<ProjectConnection> remaining = projectConnectionService.getConnectionProjects(connectionId);

    if (remaining.isEmpty()) {
        connectionService.updateVisibility(connectionId, ConnectionVisibility.PRIVATE);
    }
}

@Override
@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
public void promoteToWorkspace(long connectionId) {
    connectionService.updateVisibility(connectionId, ConnectionVisibility.WORKSPACE);
}

@Override
@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
public void demoteToPrivate(long connectionId) {
    projectConnectionService.deleteByConnectionId(connectionId);

    connectionService.updateVisibility(connectionId, ConnectionVisibility.PRIVATE);
}
```

- [ ] **Step 4: Run compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/libs/automation/automation-configuration/
git commit -m "Add visibility-aware filtering and share/promote/demote to WorkspaceConnectionFacade"
```

---

## Task 6: Add permission enforcement in ConnectionService

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionService.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java`

- [ ] **Step 1: Add permission checks to mutating operations in ConnectionServiceImpl**

The service layer enforces that only the creator or an admin can edit/delete a connection:

```java
@Override
public void delete(long id) {
    Connection connection = connectionRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

    validateOwnerOrAdmin(connection);

    connectionRepository.delete(connection);
}

@Override
public Connection update(long id, String name, List<Long> tagIds, int version) {
    Connection connection = connectionRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

    validateOwnerOrAdmin(connection);

    connection.setName(name);
    connection.setVersion(version);
    connection.setConnectionTags(
        tagIds.stream()
            .map(ConnectionTag::new)
            .collect(Collectors.toSet()));

    return connectionRepository.save(connection);
}

private void validateOwnerOrAdmin(Connection connection) {
    String currentUserLogin = SecurityUtils.getCurrentUserLogin();

    if (!currentUserLogin.equals(connection.getCreatedBy()) &&
        !SecurityUtils.hasAuthority(AuthorityConstants.ADMIN)) {

        throw new AccessDeniedException(
            "Only the connection creator or an admin can modify connection " + connection.getId());
    }
}
```

- [ ] **Step 2: Add `hasAuthority` helper to SecurityUtils if not present**

Check if `SecurityUtils` already has a `hasAuthority` method. If not, add:

```java
public static boolean hasAuthority(String authority) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
        return false;
    }

    return authentication.getAuthorities().stream()
        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
}
```

- [ ] **Step 3: Run compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/libs/platform/platform-connection/ server/libs/platform/platform-api/
git commit -m "Add permission enforcement in ConnectionService for edit/delete operations"
```

---

## Task 7: Add environment enforcement in service layer

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacade.java` (or the relevant deployment creation code)
- Identify the exact deploy-time code path by searching for `ProjectDeploymentWorkflowConnection` usage

- [ ] **Step 1: Find the deployment creation code**

Run: `grep -rn "ProjectDeploymentWorkflowConnection" server/libs/automation/ --include="*.java" -l`

Identify where deployments are created/updated and workflow connections are resolved.

- [ ] **Step 2: Add environment validation**

In the deployment creation/update service method, add validation that all referenced connections match the target environment:

```java
private void validateDeploymentConnectionEnvironments(
    List<ProjectDeploymentWorkflowConnection> workflowConnections, int targetEnvironmentId) {

    for (ProjectDeploymentWorkflowConnection workflowConnection : workflowConnections) {
        if (workflowConnection.getConnectionId() == null) {
            continue;
        }

        Connection connection = connectionService.getConnection(workflowConnection.getConnectionId());

        if (connection.getEnvironment() != targetEnvironmentId) {
            throw new IllegalStateException(
                "Connection '" + connection.getName() + "' is " +
                    Environment.values()[connection.getEnvironment()] +
                    " but deployment targets " +
                    Environment.values()[targetEnvironmentId] +
                    ". All connections must match the deployment environment.");
        }
    }
}
```

Call this method during deployment creation and update, before persisting.

- [ ] **Step 3: Run compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/libs/automation/automation-configuration/
git commit -m "Add environment enforcement for deployment connections in service layer"
```

---

## Task 8: Add GraphQL schema and controller for connection visibility operations

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/connection.graphqls`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ConnectionGraphQlController.java`

- [ ] **Step 1: Extend the connection GraphQL schema**

Replace the content of `connection.graphqls`:

```graphql
enum ConnectionVisibility {
    PRIVATE
    PROJECT
    WORKSPACE
}

extend type Mutation {
    disconnectConnection(connectionId: ID!): Boolean!
    shareConnectionToProject(connectionId: ID!, projectId: ID!): Boolean!
    revokeConnectionFromProject(connectionId: ID!, projectId: ID!): Boolean!
    promoteConnectionToWorkspace(connectionId: ID!): Boolean!
    demoteConnectionToPrivate(connectionId: ID!): Boolean!
}
```

- [ ] **Step 2: Update ConnectionGraphQlController**

Add new mutation mappings:

```java
@MutationMapping(name = "shareConnectionToProject")
public boolean shareConnectionToProject(
    @Argument long connectionId, @Argument long projectId) {

    workspaceConnectionFacade.shareConnectionToProject(connectionId, projectId);

    return true;
}

@MutationMapping(name = "revokeConnectionFromProject")
public boolean revokeConnectionFromProject(
    @Argument long connectionId, @Argument long projectId) {

    workspaceConnectionFacade.revokeConnectionFromProject(connectionId, projectId);

    return true;
}

@MutationMapping(name = "promoteConnectionToWorkspace")
@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
public boolean promoteConnectionToWorkspace(@Argument long connectionId) {
    workspaceConnectionFacade.promoteToWorkspace(connectionId);

    return true;
}

@MutationMapping(name = "demoteConnectionToPrivate")
@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
public boolean demoteConnectionToPrivate(@Argument long connectionId) {
    workspaceConnectionFacade.demoteToPrivate(connectionId);

    return true;
}
```

Inject `WorkspaceConnectionFacade` in the controller constructor.

- [ ] **Step 3: Run compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-graphql/
git commit -m "Add GraphQL mutations for connection sharing, promotion, and demotion"
```

---

## Task 9: Update REST connection model to include visibility

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-rest/openapi/components/schemas/objects/connection_base.yaml`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/src/main/java/com/bytechef/automation/configuration/web/rest/ConnectionApiController.java`

- [ ] **Step 1: Add visibility to the OpenAPI connection base schema**

In `connection_base.yaml`, add after the `tags` property:

```yaml
    visibility:
      description: "The visibility scope of the connection"
      type: string
      enum:
        - PRIVATE
        - PROJECT
        - WORKSPACE
      readOnly: true
    sharedProjectIds:
      description: "IDs of projects this connection is shared with (only for PROJECT visibility)"
      type: array
      items:
        type: integer
        format: int64
      readOnly: true
```

- [ ] **Step 2: Regenerate REST models**

Run: `./gradlew generateOpenApiModels` (or the equivalent task — check the build.gradle.kts for the correct task name)

- [ ] **Step 3: Update ConnectionApiController to populate new fields**

Update the conversion from `ConnectionDTO` to the REST model to include `visibility` and `sharedProjectIds` fields.

- [ ] **Step 4: Run compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-connection/platform-connection-rest/
git add server/libs/automation/automation-configuration/automation-configuration-rest/
git commit -m "Add visibility and sharedProjectIds to REST connection model"
```

---

## Task 10: Frontend — Add visibility types and update GraphQL operations

**Files:**
- Create: `client/src/graphql/automation/configuration/shareConnectionToProject.graphql`
- Create: `client/src/graphql/automation/configuration/revokeConnectionFromProject.graphql`
- Create: `client/src/graphql/automation/configuration/promoteConnectionToWorkspace.graphql`
- Create: `client/src/graphql/automation/configuration/demoteConnectionToPrivate.graphql`
- Modify: `client/src/shared/mutations/automation/connections.mutations.ts`

- [ ] **Step 1: Create GraphQL operation files**

`shareConnectionToProject.graphql`:
```graphql
mutation shareConnectionToProject($connectionId: ID!, $projectId: ID!) {
    shareConnectionToProject(connectionId: $connectionId, projectId: $projectId)
}
```

`revokeConnectionFromProject.graphql`:
```graphql
mutation revokeConnectionFromProject($connectionId: ID!, $projectId: ID!) {
    revokeConnectionFromProject(connectionId: $connectionId, projectId: $projectId)
}
```

`promoteConnectionToWorkspace.graphql`:
```graphql
mutation promoteConnectionToWorkspace($connectionId: ID!) {
    promoteConnectionToWorkspace(connectionId: $connectionId)
}
```

`demoteConnectionToPrivate.graphql`:
```graphql
mutation demoteConnectionToPrivate($connectionId: ID!) {
    demoteConnectionToPrivate(connectionId: $connectionId)
}
```

- [ ] **Step 2: Regenerate GraphQL types**

Run: `cd client && npx graphql-codegen`

This regenerates `src/shared/middleware/graphql.ts` with the new mutation hooks.

- [ ] **Step 3: Add mutation hooks wrapper in connections.mutations.ts**

Add wrapper hooks that invalidate the connections query cache on success:

```typescript
export const useShareConnectionToProjectMutation = () => {
    const queryClient = useQueryClient();

    return useShareConnectionToProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});
        },
    });
};
```

(Repeat pattern for revoke, promote, demote)

- [ ] **Step 4: Commit**

```bash
cd client
git add src/graphql/automation/configuration/ src/shared/mutations/automation/ src/shared/middleware/graphql.ts
git commit -m "client - Add GraphQL operations and mutation hooks for connection visibility"
```

---

## Task 11: Frontend — Update connection picker to group by visibility

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTabConnectionSelect.tsx`

- [ ] **Step 1: Group connections by visibility in the Select dropdown**

In the component where connections are rendered in the Select, group them:

```typescript
const groupedConnections = useMemo(() => {
    const groups: Record<string, typeof connections> = {
        WORKSPACE: [],
        PROJECT: [],
        PRIVATE: [],
    };

    for (const connection of connections || []) {
        const visibility = connection.visibility || 'WORKSPACE';

        groups[visibility].push(connection);
    }

    return groups;
}, [connections]);
```

Render with section headers:

```tsx
{Object.entries(groupedConnections).map(([visibility, groupConnections]) => {
    if (groupConnections.length === 0) {
        return null;
    }

    const label = visibility === 'PRIVATE' ? 'My Connections'
        : visibility === 'PROJECT' ? 'Project'
        : 'Workspace';

    return (
        <SelectGroup key={visibility}>
            <SelectLabel>{label}</SelectLabel>
            {groupConnections.map((connection) => (
                <SelectItem key={connection.id} value={String(connection.id)}>
                    {connection.name}
                </SelectItem>
            ))}
        </SelectGroup>
    );
})}
```

- [ ] **Step 2: Run client checks**

Run: `cd client && npm run check`
Expected: All lint, typecheck, and tests pass

- [ ] **Step 3: Commit**

```bash
cd client
git add src/pages/platform/workflow-editor/
git commit -m "client - Group connections by visibility scope in connection picker"
```

---

## Task 12: Frontend — Add scope badges and action menu to connection list

**Files:**
- Create: `client/src/pages/automation/connections/components/ConnectionScopeBadge.tsx`
- Modify: Connection list page component (find the connections list page in `client/src/pages/automation/connections/`)

- [ ] **Step 1: Create ConnectionScopeBadge component**

```tsx
import {LockIcon, FolderIcon, BuildingIcon} from 'lucide-react';

import {twMerge} from 'tailwind-merge';

interface ConnectionScopeBadgeProps {
    visibility: 'PRIVATE' | 'PROJECT' | 'WORKSPACE';
}

const ConnectionScopeBadge = ({visibility}: ConnectionScopeBadgeProps) => {
    const config = {
        PRIVATE: {icon: LockIcon, label: 'Private', className: 'text-gray-500'},
        PROJECT: {icon: FolderIcon, label: 'Project', className: 'text-blue-500'},
        WORKSPACE: {icon: BuildingIcon, label: 'Workspace', className: 'text-green-500'},
    };

    const {className, icon: IconComponent, label} = config[visibility];

    return (
        <span className={twMerge('inline-flex items-center gap-1 text-xs', className)}>
            <IconComponent className="size-3" />

            {label}
        </span>
    );
};

export default ConnectionScopeBadge;
```

- [ ] **Step 2: Add badge to connection list items**

Find the connection list component and add `<ConnectionScopeBadge visibility={connection.visibility} />` next to each connection name.

- [ ] **Step 3: Add Share/Promote/Demote actions to the connection action menu**

Add menu items conditionally:
- **Share to Project** — shown for PRIVATE connections where `connection.createdBy` matches current user
- **Promote to Workspace** — shown for PROJECT connections when user is admin
- **Demote to Private** — shown for WORKSPACE connections when user is admin

Each action calls the corresponding GraphQL mutation hook from Task 10.

- [ ] **Step 4: Run client checks**

Run: `cd client && npm run check`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd client
git add src/pages/automation/connections/
git commit -m "client - Add scope badges and share/promote/demote actions to connection list"
```

---

## Task 13: Integration tests for Phase 1

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/ProjectConnectionServiceIntTest.java`

- [ ] **Step 1: Write integration tests**

Test scenarios:
1. Create PRIVATE connection → only creator sees it via `getConnections`
2. Share connection to project → project members see it
3. Revoke share → connection reverts to PRIVATE
4. Promote to WORKSPACE → all workspace members see it
5. Demote to PRIVATE → only creator sees it
6. Delete connection → cascades to `project_connection` rows
7. Environment mismatch at deploy time → `IllegalStateException` thrown

- [ ] **Step 2: Run integration tests**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/test/
git commit -m "Add integration tests for connection visibility Phase 1"
```

---

# Phase 2 — Organization Connections

## Task 14: Add ORGANIZATION to ConnectionVisibility enum

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/ConnectionVisibility.java`

- [ ] **Step 1: Add ORGANIZATION value**

```java
public enum ConnectionVisibility {

    PRIVATE,        // ordinal 0
    PROJECT,        // ordinal 1
    WORKSPACE,      // ordinal 2
    ORGANIZATION    // ordinal 3

}
```

- [ ] **Step 2: Update visibility filtering in WorkspaceConnectionFacadeImpl**

Add `ORGANIZATION` case to the visibility filter switch:

```java
case ORGANIZATION -> true;  // Visible to all members across all workspaces
```

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-connection/
git add server/libs/automation/automation-configuration/
git commit -m "Add ORGANIZATION visibility scope to ConnectionVisibility enum"
```

---

## Task 15: Create OrganizationConnectionFacade and service

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/OrganizationConnectionFacade.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/OrganizationConnectionFacadeImpl.java`

- [ ] **Step 1: Create OrganizationConnectionFacade interface**

```java
package com.bytechef.automation.configuration.facade;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface OrganizationConnectionFacade {

    long create(ConnectionDTO connectionDTO);

    void delete(long connectionId);

    List<ConnectionDTO> getOrganizationConnections(Long environmentId);

    ConnectionDTO update(long connectionId, String name, int version);
}
```

- [ ] **Step 2: Implement OrganizationConnectionFacadeImpl**

Organization connections bypass workspace scoping — they are created with `visibility = ORGANIZATION` and are accessible from all workspaces. Only users with `ROLE_ADMIN` can create/edit/delete them.

```java
@Component
@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
public class OrganizationConnectionFacadeImpl implements OrganizationConnectionFacade {

    private final ConnectionFacade connectionFacade;
    private final ConnectionService connectionService;

    // Constructor injection...

    @Override
    public long create(ConnectionDTO connectionDTO) {
        long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

        connectionService.updateVisibility(connectionId, ConnectionVisibility.ORGANIZATION);

        return connectionId;
    }

    @Override
    public void delete(long connectionId) {
        Connection connection = connectionService.getConnection(connectionId);

        if (connection.getVisibility() != ConnectionVisibility.ORGANIZATION) {
            throw new IllegalArgumentException("Connection " + connectionId + " is not an organization connection");
        }

        connectionFacade.delete(connectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getOrganizationConnections(Long environmentId) {
        // Query all connections with ORGANIZATION visibility
        return connectionService.getConnections(PlatformType.AUTOMATION)
            .stream()
            .filter(connection -> connection.getVisibility() == ConnectionVisibility.ORGANIZATION)
            .filter(connection -> environmentId == null || connection.getEnvironment() == environmentId.intValue())
            .map(connection -> connectionFacade.getConnection(connection.getId()))
            .toList();
    }

    @Override
    public ConnectionDTO update(long connectionId, String name, int version) {
        connectionService.update(connectionId, name, List.of(), version);

        return connectionFacade.getConnection(connectionId);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-configuration/
git commit -m "Add OrganizationConnectionFacade for managing organization-scoped connections"
```

---

## Task 16: Add GraphQL schema and controller for organization connections

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/organization-connection.graphqls`
- Create: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/OrganizationConnectionGraphQlController.java`

- [ ] **Step 1: Create GraphQL schema**

```graphql
extend type Query {
    organizationConnections(environmentId: ID): [OrganizationConnection!]!
}

extend type Mutation {
    createOrganizationConnection(input: CreateOrganizationConnectionInput!): ID!
    deleteOrganizationConnection(connectionId: ID!): Boolean!
    updateOrganizationConnection(connectionId: ID!, name: String!, version: Int!): Boolean!
}

type OrganizationConnection {
    id: ID!
    name: String!
    componentName: String!
    environmentId: Int!
    visibility: ConnectionVisibility!
    createdBy: String
    createdDate: String
    lastModifiedDate: String
}

input CreateOrganizationConnectionInput {
    name: String!
    componentName: String!
    connectionVersion: Int!
    environmentId: Int!
    parameters: Map!
}
```

- [ ] **Step 2: Create OrganizationConnectionGraphQlController**

Follow the existing controller patterns (e.g., `WorkspaceApiKeyGraphQlController`). All mutations require `ROLE_ADMIN`.

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-graphql/
git commit -m "Add GraphQL schema and controller for organization connections"
```

---

## Task 17: Frontend — Settings page for organization connections

**Files:**
- Create: `client/src/pages/settings/connections/SettingsConnections.tsx`
- Create: `client/src/pages/settings/connections/components/OrganizationConnectionDialog.tsx`
- Create: `client/src/graphql/automation/configuration/organizationConnections.graphql`
- Create: `client/src/graphql/automation/configuration/createOrganizationConnection.graphql`
- Create: `client/src/graphql/automation/configuration/deleteOrganizationConnection.graphql`

- [ ] **Step 1: Create GraphQL operations**

Query and mutation files for org connections following existing patterns.

- [ ] **Step 2: Run codegen**

Run: `cd client && npx graphql-codegen`

- [ ] **Step 3: Create SettingsConnections page**

A table view of all organization connections with environment badges. CRUD via dialog. Only accessible by admin users. Wire into the Settings routing.

- [ ] **Step 4: Create OrganizationConnectionDialog**

Reuse connection parameter patterns from existing `ConnectionDialog.tsx`. Set visibility to ORGANIZATION server-side.

- [ ] **Step 5: Add route to Settings**

Add the connections page to the Settings navigation and routes.

- [ ] **Step 6: Run client checks**

Run: `cd client && npm run check`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
cd client
git add src/pages/settings/ src/graphql/automation/configuration/ src/shared/middleware/graphql.ts
git commit -m "client - Add Settings page for organization connection management"
```

---

## Task 18: Update connection picker to include ORGANIZATION group

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTabConnectionSelect.tsx`
- Modify: `client/src/pages/automation/connections/components/ConnectionScopeBadge.tsx`

- [ ] **Step 1: Add ORGANIZATION to picker grouping**

Update the `groupedConnections` useMemo to include an ORGANIZATION group:

```typescript
const groups: Record<string, typeof connections> = {
    ORGANIZATION: [],
    WORKSPACE: [],
    PROJECT: [],
    PRIVATE: [],
};
```

The ORGANIZATION label renders as "Organization" in the group header.

- [ ] **Step 2: Update ConnectionScopeBadge**

Add ORGANIZATION config:

```typescript
ORGANIZATION: {icon: GlobeIcon, label: 'Organization', className: 'text-purple-500'},
```

- [ ] **Step 3: Run client checks and commit**

```bash
cd client && npm run check
git add src/pages/
git commit -m "client - Add ORGANIZATION group to connection picker and scope badge"
```

---

# Phase 3 — User Removal, Workflow Pausing, Audit Logging

## Task 19: Add `status` column to connection table

**Files:**
- Create Liquibase migration: `server/libs/platform/platform-connection/platform-connection-service/src/main/resources/config/liquibase/changelog/platform/connection/00000000000003_platform_connection_add_status.xml`
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/ConnectionStatus.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/Connection.java`

- [ ] **Step 1: Create ConnectionStatus enum**

```java
package com.bytechef.platform.connection.domain;

public enum ConnectionStatus {

    ACTIVE,                 // ordinal 0
    PENDING_REASSIGNMENT,   // ordinal 1
    REVOKED                 // ordinal 2

}
```

- [ ] **Step 2: Create Liquibase migration**

```xml
<changeSet id="00000000000003-01" author="ivicac">
    <addColumn tableName="connection">
        <column name="status" type="INT" defaultValueNumeric="0">
            <constraints nullable="false"/>
        </column>
    </addColumn>
</changeSet>
```

- [ ] **Step 3: Add field to Connection entity**

```java
@Column
private int status;
```

With getter/setter using `ConnectionStatus.values()[status]`. Default to `ACTIVE` in constructor.

- [ ] **Step 4: Add status to ConnectionDTO**

Add `ConnectionStatus status` to the record and update constructors.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-connection/
git commit -m "Add ConnectionStatus enum and status column to connection table"
```

---

## Task 20: Create connection audit log table and service

**Files:**
- Create Liquibase migration for `connection_audit_log` table
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/ConnectionAuditLog.java`
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/ConnectionAuditEventType.java`
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionAuditLogService.java`
- Create: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionAuditLogServiceImpl.java`
- Create: Repository interface

- [ ] **Step 1: Create ConnectionAuditEventType enum**

```java
package com.bytechef.platform.connection.domain;

public enum ConnectionAuditEventType {

    CREATED,
    SHARED,
    PROMOTED,
    DEMOTED,
    USED_IN_EXECUTION,
    REASSIGNED,
    ROTATED,
    REVOKED,
    WORKFLOW_PAUSED,
    WORKFLOW_RESUMED

}
```

- [ ] **Step 2: Create Liquibase migration**

```xml
<createTable tableName="connection_audit_log">
    <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
    </column>
    <column name="connection_id" type="BIGINT">
        <constraints nullable="false"/>
    </column>
    <column name="event_type" type="INT">
        <constraints nullable="false"/>
    </column>
    <column name="performed_by" type="VARCHAR(256)">
        <constraints nullable="false"/>
    </column>
    <column name="details" type="TEXT"/>
    <column name="created_date" type="TIMESTAMP WITHOUT TIME ZONE">
        <constraints nullable="false"/>
    </column>
</createTable>

<addForeignKeyConstraint baseTableName="connection_audit_log"
                         baseColumnNames="connection_id"
                         referencedTableName="connection"
                         referencedColumnNames="id"
                         constraintName="fk_connection_audit_log_connection"
                         onDelete="CASCADE"/>

<createIndex tableName="connection_audit_log" indexName="idx_connection_audit_log_connection_id">
    <column name="connection_id"/>
</createIndex>
```

- [ ] **Step 3: Create entity, repository, and service**

`ConnectionAuditLog` entity with fields: `id`, `connectionId`, `eventType` (int mapped to enum), `performedBy`, `details` (JSON text), `createdDate`.

Service with `log(connectionId, eventType, details)` method that populates `performedBy` from `SecurityUtils.getCurrentUserLogin()`.

- [ ] **Step 4: Wire audit logging into existing operations**

Add `connectionAuditLogService.log(...)` calls in:
- `WorkspaceConnectionFacadeImpl.create()` → `CREATED`
- `WorkspaceConnectionFacadeImpl.shareConnectionToProject()` → `SHARED`
- `WorkspaceConnectionFacadeImpl.promoteToWorkspace()` → `PROMOTED`
- `WorkspaceConnectionFacadeImpl.demoteToPrivate()` → `DEMOTED`

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-connection/
git add server/libs/automation/automation-configuration/
git commit -m "Add connection audit log table, service, and wire into existing operations"
```

---

## Task 21: Implement user removal connection resolution flow

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/ConnectionReassignmentFacade.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ConnectionReassignmentFacadeImpl.java`
- Create GraphQL schema and controller for reassignment operations

- [ ] **Step 1: Create ConnectionReassignmentFacade**

```java
package com.bytechef.automation.configuration.facade;

import java.util.List;

public interface ConnectionReassignmentFacade {

    /**
     * Returns all connections owned by the user within the workspace that need resolution.
     */
    List<ConnectionReassignmentItem> getUnresolvedConnections(long workspaceId, String userLogin);

    /**
     * Reassigns a connection to a new owner.
     */
    void reassignConnection(long connectionId, String newOwnerLogin);

    /**
     * Reassigns all connections owned by user in workspace to new owner.
     */
    void reassignAllConnections(long workspaceId, String userLogin, String newOwnerLogin);

    /**
     * Returns workflows that depend on unresolved connections.
     */
    List<AffectedWorkflow> getAffectedWorkflows(long workspaceId, String userLogin);
}
```

The `ConnectionReassignmentItem` record contains: `connectionId`, `connectionName`, `visibility`, `environment`, `dependentWorkflowCount`.

The `AffectedWorkflow` record contains: `workflowId`, `workflowName`, `connectionIds`.

- [ ] **Step 2: Implement reassignment logic**

The implementation:
1. Queries all connections where `createdBy = userLogin` in the workspace
2. For each connection, finds dependent workflows via `ProjectDeploymentWorkflowConnection`
3. On reassign: updates `createdBy` field on the connection (requires a new `ConnectionService.updateCreatedBy` method)
4. Logs `REASSIGNED` audit event

- [ ] **Step 3: Create GraphQL schema**

```graphql
extend type Query {
    unresolvedConnections(workspaceId: ID!, userLogin: String!): [ConnectionReassignmentItem!]!
    affectedWorkflows(workspaceId: ID!, userLogin: String!): [AffectedWorkflow!]!
}

extend type Mutation {
    reassignConnection(connectionId: ID!, newOwnerLogin: String!): Boolean!
    reassignAllConnections(workspaceId: ID!, userLogin: String!, newOwnerLogin: String!): Boolean!
}

type ConnectionReassignmentItem {
    connectionId: ID!
    connectionName: String!
    visibility: ConnectionVisibility!
    environmentId: Int!
    dependentWorkflowCount: Int!
}

type AffectedWorkflow {
    workflowId: String!
    workflowName: String!
    connectionIds: [ID!]!
}
```

- [ ] **Step 4: Commit**

```bash
git add server/libs/automation/automation-configuration/
git commit -m "Add connection reassignment facade and GraphQL operations for user removal"
```

---

## Task 22: Implement workflow pausing on unresolved connections

**Files:**
- Modify: deployment/execution service to check connection status before executing
- Modify: `ConnectionReassignmentFacadeImpl` to pause workflows when connections enter `PENDING_REASSIGNMENT`

- [ ] **Step 1: Add connection status check in workflow execution path**

In the workflow execution code path (likely in the atlas-coordinator or automation-execution module), add a pre-execution check:

```java
private void validateConnectionsActive(List<Long> connectionIds) {
    for (Long connectionId : connectionIds) {
        Connection connection = connectionService.getConnection(connectionId);

        if (connection.getStatus() == ConnectionStatus.PENDING_REASSIGNMENT) {
            throw new WorkflowPausedException(
                "Workflow paused: connection '" + connection.getName() +
                    "' is pending reassignment");
        }

        if (connection.getStatus() == ConnectionStatus.REVOKED) {
            throw new IllegalStateException(
                "Connection '" + connection.getName() + "' has been revoked");
        }
    }
}
```

- [ ] **Step 2: Add pause/resume tracking to deployments**

When connections enter `PENDING_REASSIGNMENT`:
1. Find all deployments using those connections
2. Set deployment status to paused (or disable the deployment workflows)
3. Log `WORKFLOW_PAUSED` audit events

When connections are reassigned (status returns to `ACTIVE`):
1. Find all paused deployments that depended on the connection
2. Re-enable them
3. Log `WORKFLOW_RESUMED` audit events

- [ ] **Step 3: Commit**

```bash
git add server/libs/
git commit -m "Add workflow pausing when connections enter PENDING_REASSIGNMENT status"
```

---

## Task 23: Frontend — User removal connection resolution dialog

**Files:**
- Create: `client/src/pages/automation/connections/components/ConnectionReassignmentDialog.tsx`
- Create: `client/src/graphql/automation/configuration/unresolvedConnections.graphql`
- Create: `client/src/graphql/automation/configuration/reassignConnection.graphql`
- Create: `client/src/graphql/automation/configuration/reassignAllConnections.graphql`
- Create: `client/src/graphql/automation/configuration/affectedWorkflows.graphql`

- [ ] **Step 1: Create GraphQL operations**

Query and mutation files for reassignment.

- [ ] **Step 2: Run codegen**

Run: `cd client && npx graphql-codegen`

- [ ] **Step 3: Create ConnectionReassignmentDialog**

The dialog shows:
1. List of connections owned by the departing user, grouped by scope and environment
2. For each connection: name, scope badge, environment badge, dependent workflow count
3. A "Reassign to" user picker (workspace members dropdown)
4. Bulk action: "Reassign all to [user]"
5. Warning banner: "X workflows will be paused until connections are resolved"
6. Individual reassign buttons per connection
7. Confirm button that calls `reassignAllConnections` or individual `reassignConnection` mutations

- [ ] **Step 4: Wire dialog into user removal flow**

Add the dialog as a mandatory step before user removal completes. When admin clicks "Remove user" in workspace settings, show this dialog if the user owns any connections.

- [ ] **Step 5: Run client checks**

Run: `cd client && npm run check`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
cd client
git add src/pages/ src/graphql/ src/shared/middleware/graphql.ts
git commit -m "client - Add connection reassignment dialog for user removal flow"
```

---

## Task 24: Final integration tests for Phases 2 and 3

**Files:**
- Create integration tests for organization connections
- Create integration tests for user removal flow
- Create integration tests for workflow pausing

- [ ] **Step 1: Organization connection tests**

Test scenarios:
1. Admin creates org connection → visible in all workspaces
2. Non-admin cannot create org connection → `AccessDeniedException`
3. Org connection appears in workspace connection picker

- [ ] **Step 2: User removal tests**

Test scenarios:
1. Get unresolved connections for user → returns all owned connections
2. Reassign connection → `createdBy` updated, audit log written
3. Reassign all → bulk operation succeeds
4. Affected workflows identified correctly

- [ ] **Step 3: Workflow pausing tests**

Test scenarios:
1. Connection enters `PENDING_REASSIGNMENT` → dependent workflows paused
2. Connection reassigned (back to `ACTIVE`) → workflows resume
3. Revoked connection → execution blocked

- [ ] **Step 4: Run all tests**

Run: `./gradlew testIntegration`
Expected: All pass

- [ ] **Step 5: Commit**

```bash
git add server/libs/
git commit -m "Add integration tests for organization connections, reassignment, and workflow pausing"
```

---

## Task 25: Run full build and format

- [ ] **Step 1: Format and build**

```bash
./gradlew spotlessApply
./gradlew compileJava
cd client && npm run check
```

- [ ] **Step 2: Fix any issues and commit**

```bash
git add -A
git commit -m "Final formatting and build fixes for connection visibility feature"
```
