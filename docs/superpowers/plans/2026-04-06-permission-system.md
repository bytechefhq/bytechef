# Permission System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add RBAC at workspace + project level using Spring Security `@PreAuthorize` on service methods, backed by a dedicated `PermissionService` bean. RBAC is an **EE-only feature** — the CE (free tier) has basic user management without role enforcement.

**Architecture:** `@PreAuthorize("@permissionService.hasProjectScope(#projectId, 'WORKFLOW_EDIT')")` on service methods. In EE, PermissionService resolves scopes from WorkspaceUser.role / ProjectMember.role (or custom role), cached via Spring Cache. In CE, PermissionService is a no-op mock that always returns `true` — effectively disabling RBAC in the free tier. Controllers keep coarse `@PreAuthorize("hasAuthority('ROLE_USER')")` gates. Facades do orchestration only — no auth checks.

**Tech Stack:** Spring Security 6 method security (`@PreAuthorize` + SpEL), Spring Data JDBC, Spring Cache (`@Cacheable`/`@CacheEvict`, backed by Caffeine in dev / Redis in prod), Liquibase migrations, existing `SecurityUtils` + `AuthorityConstants`.

**CE vs EE Differentiation:**
- **CE (Free Tier):** Basic user management (Owner + Member). No role hierarchy, no project-level roles, no permission scopes. `PermissionServiceImpl` always returns `true`. No `ProjectMember` entity.
- **EE (Paid Tier):** Full RBAC with 3-tier hierarchy (Tenant → Workspace → Project). Workspace roles (ADMIN, EDITOR, VIEWER), project roles (ADMIN, EDITOR, OPERATOR, VIEWER), fine-grained permission scopes, custom roles. `ProjectMember` entity with role/custom-role assignment.
- **Key differentiators vs. competitors:** 3-tier hierarchy (n8n has 2, Activepieces has 2), Operator role, AI Agent/MCP permission scopes, custom roles at Enterprise level.

---

## Architectural Decisions

1. **`@PreAuthorize` on services** — not custom AOP. Spring Security's method security interceptor is already wired via `@EnableMethodSecurity`. SpEL calls `@permissionService` bean. No compile-time dependency needed — the annotation is a string.
2. **PermissionService interface in CE, real implementation in EE** — The `PermissionService` interface lives in `automation-configuration-api` (CE). The real implementation with scope resolution, caching, and role hierarchy lives in `ee/automation-configuration-service` with `@ConditionalOnEEVersion`. A no-op mock in CE with `@ConditionalOnCEVersion` always permits access.
3. **Tenant role = existing authorities** — `ROLE_ADMIN` = tenant admin (bypasses all checks). `ROLE_USER` = member (needs explicit workspace/project roles in EE). No new `TenantRole` entity.
4. **WorkspaceRole stored as String in CE domain** — `WorkspaceUser.workspaceRole` is a `String` field. In EE, the `WorkspaceRole` enum (in `ee/automation-configuration-api`) is used for validation. The `workspace_role` column stores the enum name.
5. **ProjectMember is an EE-only entity** — `ProjectMember` domain, service, and repository all live in `ee/automation-configuration`. The `project_member` table Liquibase migration is in the EE service module.
6. **Custom roles are EE-only** — `@ConditionalOnEEVersion` on CustomRole service/repository.
7. **Security constants live in EE** — `PermissionScope`, `ProjectRole`, `WorkspaceRole`, `BuiltInRoleScopes` are in `ee/automation-configuration-api/security/constant/`. CE code doesn't reference these types.
8. **Audit log deferred** — not in this plan. Permission system must work first. Audit is a follow-up.

## File Structure

### EE Files (RBAC Implementation)

```
# Security constants (ee/automation-configuration-api)
server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/security/constant/
  WorkspaceRole.java              — ADMIN, EDITOR, VIEWER
  ProjectRole.java                — ADMIN, EDITOR, OPERATOR, VIEWER
  PermissionScope.java            — WORKFLOW_VIEW, WORKFLOW_CREATE, ... (all scopes)
  BuiltInRoleScopes.java          — ProjectRole → Set<PermissionScope> mapping

# ProjectMember domain (ee/automation-configuration-api)
server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/domain/
  ProjectMember.java              — Spring Data JDBC entity

# Service interfaces (ee/automation-configuration-api)
server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/service/
  ProjectMemberService.java       — CRUD + lookup interface
  CustomRoleScopeResolver.java    — SPI for custom role scope resolution

# Repository and service implementations (ee/automation-configuration-service)
server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/
  repository/ProjectMemberRepository.java    — Spring Data JDBC, @ConditionalOnEEVersion
  service/PermissionServiceImpl.java         — @ConditionalOnEEVersion, cached scope resolution
  service/ProjectScopeCacheService.java      — Spring Cache abstraction for scope caching
  service/ProjectMemberServiceImpl.java      — @ConditionalOnEEVersion, last-admin protection

# Database migrations (ee/automation-configuration-service)
server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/
  202604061200020_automation_configuration_added_table_project_member.xml
  202604061200050_automation_configuration_backfill_project_member.xml
```

### CE Files (Shared Interface + No-op Mock)

```
# PermissionService interface (automation-configuration-api — shared between CE and EE)
server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/
  PermissionService.java          — hasProjectScope, hasWorkspaceRole, isTenantAdmin, etc.

# CE no-op implementation (automation-configuration-service)
server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/
  PermissionServiceImpl.java      — @ConditionalOnCEVersion, always returns true

# Database migration for workspace_user role column (shared, CE service)
server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/
  202604061200010_automation_configuration_added_column_workspace_user_role.xml
```

### Modified Files

```
# WorkspaceUser.workspaceRole changed from WorkspaceRole enum to String
server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/WorkspaceUser.java

# WorkspaceUserService uses String for role parameters
server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/WorkspaceUserService.java
server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/WorkspaceUserServiceImpl.java

# Add @PreAuthorize to service methods (examples — full list depends on scope)
server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ProjectServiceImpl.java

# ProjectMemberGraphQlController: @ConditionalOnEEVersion (EE-only UI)
server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ProjectMemberGraphQlController.java
```

---

## Task 1: Permission Enums and Constants

**Files:**
- Create: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/constant/PermissionScope.java`
- Create: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/constant/WorkspaceRole.java`
- Create: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/constant/ProjectRole.java`
- Create: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/constant/BuiltInRoleScopes.java`

- [ ] **Step 1: Create PermissionScope enum**

```java
package com.bytechef.platform.security.constant;

public enum PermissionScope {

    // Workflow
    WORKFLOW_VIEW,
    WORKFLOW_CREATE,
    WORKFLOW_EDIT,
    WORKFLOW_DELETE,
    WORKFLOW_TOGGLE,

    // Execution
    EXECUTION_VIEW,
    EXECUTION_DATA,
    EXECUTION_RETRY,

    // Connection
    CONNECTION_VIEW,
    CONNECTION_CREATE,
    CONNECTION_EDIT,
    CONNECTION_DELETE,
    CONNECTION_USE,

    // Agent / MCP
    AGENT_VIEW,
    AGENT_CREATE,
    AGENT_EDIT,
    AGENT_EXECUTE,

    // Project management
    PROJECT_VIEW_MEMBERS,
    PROJECT_MANAGE_MEMBERS,
    PROJECT_SETTINGS,

    // Deployment
    DEPLOYMENT_PUSH,
    DEPLOYMENT_PULL
}
```

- [ ] **Step 2: Create WorkspaceRole enum**

```java
package com.bytechef.platform.security.constant;

public enum WorkspaceRole {

    ADMIN,
    EDITOR,
    VIEWER
}
```

- [ ] **Step 3: Create ProjectRole enum**

```java
package com.bytechef.platform.security.constant;

public enum ProjectRole {

    ADMIN,
    EDITOR,
    OPERATOR,
    VIEWER
}
```

- [ ] **Step 4: Create BuiltInRoleScopes mapping**

```java
package com.bytechef.platform.security.constant;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.bytechef.platform.security.constant.PermissionScope.*;

public final class BuiltInRoleScopes {

    private static final Map<ProjectRole, Set<PermissionScope>> ROLE_SCOPES = Map.of(
        ProjectRole.ADMIN, EnumSet.allOf(PermissionScope.class),

        ProjectRole.EDITOR, EnumSet.of(
            WORKFLOW_VIEW, WORKFLOW_CREATE, WORKFLOW_EDIT, WORKFLOW_TOGGLE,
            EXECUTION_VIEW, EXECUTION_DATA, EXECUTION_RETRY,
            CONNECTION_VIEW, CONNECTION_CREATE, CONNECTION_EDIT, CONNECTION_USE,
            AGENT_VIEW, AGENT_CREATE, AGENT_EDIT, AGENT_EXECUTE,
            PROJECT_VIEW_MEMBERS,
            DEPLOYMENT_PUSH),

        ProjectRole.OPERATOR, EnumSet.of(
            WORKFLOW_VIEW, WORKFLOW_TOGGLE,
            EXECUTION_VIEW, EXECUTION_DATA, EXECUTION_RETRY,
            CONNECTION_VIEW, CONNECTION_USE,
            AGENT_VIEW, AGENT_EXECUTE,
            PROJECT_VIEW_MEMBERS),

        ProjectRole.VIEWER, EnumSet.of(
            WORKFLOW_VIEW,
            EXECUTION_VIEW,
            CONNECTION_VIEW,
            AGENT_VIEW,
            PROJECT_VIEW_MEMBERS));

    public static Set<PermissionScope> getScopesForRole(ProjectRole role) {
        return ROLE_SCOPES.getOrDefault(role, Collections.emptySet());
    }

    private BuiltInRoleScopes() {
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew :server:libs:platform:platform-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/constant/PermissionScope.java \
       server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/constant/WorkspaceRole.java \
       server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/constant/ProjectRole.java \
       server/libs/platform/platform-api/src/main/java/com/bytechef/platform/security/constant/BuiltInRoleScopes.java
git commit -m "Add permission enums: PermissionScope, WorkspaceRole, ProjectRole, BuiltInRoleScopes"
```

---

## Task 2: Database Migrations

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/202604061200010_automation_configuration_added_column_workspace_user_role.xml`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/202604061200020_automation_configuration_added_table_project_member.xml`

- [ ] **Step 1: Create migration to add workspace_role column to workspace_user**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="202604061200010" author="Ivica Cardic">
        <addColumn tableName="workspace_user">
            <column name="workspace_role" type="VARCHAR(20)" defaultValue="EDITOR">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

Note: Default `EDITOR` ensures existing workspace_user rows get a sensible role. The workspace creator should be updated to `ADMIN` via a data migration or application logic.

- [ ] **Step 2: Create migration to add project_member table**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="202604061200020" author="Ivica Cardic">
        <createTable tableName="project_member">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="project_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="user_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="project_role" type="VARCHAR(20)">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addUniqueConstraint
            tableName="project_member"
            columnNames="project_id,user_id"
            constraintName="uk_project_member_project_user"/>

        <addForeignKeyConstraint
            baseTableName="project_member"
            baseColumnNames="project_id"
            constraintName="fk_project_member_project"
            referencedTableName="project"
            referencedColumnNames="id"
            onDelete="CASCADE"/>

        <createIndex tableName="project_member" indexName="idx_project_member_user_id">
            <column name="user_id"/>
        </createIndex>
    </changeSet>

    <changeSet id="202604061200020-1" author="Ivica Cardic" contextFilter="mono">
        <addForeignKeyConstraint
            baseTableName="project_member"
            baseColumnNames="user_id"
            constraintName="fk_project_member_user"
            referencedTableName="user"
            referencedColumnNames="id"
            onDelete="CASCADE"/>
    </changeSet>
</databaseChangeLog>
```

Note: The `user_id` FK uses `contextFilter="mono"` because in EE microservices, the `user` table may be in a different database. This follows the existing pattern from `workspace_user` migration.

- [ ] **Step 3: Register migrations in the Liquibase master changelog**

Find the master changelog that includes the other `automation/configuration/` changesets and add:

```xml
<include file="config/liquibase/changelog/automation/configuration/202604061200010_automation_configuration_added_column_workspace_user_role.xml"/>
<include file="config/liquibase/changelog/automation/configuration/202604061200020_automation_configuration_added_table_project_member.xml"/>
```

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL (migrations compile, not yet applied)

- [ ] **Step 4: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/202604061200010_automation_configuration_added_column_workspace_user_role.xml \
       server/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/202604061200020_automation_configuration_added_table_project_member.xml
git commit -m "Add Liquibase migrations for workspace_user role column and project_member table"
```

---

## Task 3: Modify WorkspaceUser Domain — Add Role

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/WorkspaceUser.java`

- [ ] **Step 1: Add workspaceRole field to WorkspaceUser**

Add import and field:

```java
import com.bytechef.platform.security.constant.WorkspaceRole;
```

Add field (alphabetical with other fields, after `lastModifiedDate`):

```java
@Column("workspace_role")
private WorkspaceRole workspaceRole;
```

Modify the public constructor:

```java
public WorkspaceUser(Long userId, Long workspaceId) {
    this.userId = userId;
    this.workspaceId = workspaceId;
    this.workspaceRole = WorkspaceRole.EDITOR;
}
```

Add a second constructor:

```java
public WorkspaceUser(Long userId, Long workspaceId, WorkspaceRole workspaceRole) {
    this.userId = userId;
    this.workspaceId = workspaceId;
    this.workspaceRole = workspaceRole;
}
```

Add getter:

```java
public WorkspaceRole getWorkspaceRole() {
    return workspaceRole;
}
```

Add setter:

```java
public void setWorkspaceRole(WorkspaceRole workspaceRole) {
    this.workspaceRole = workspaceRole;
}
```

Update `toString()` to include `workspaceRole`.

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/WorkspaceUser.java
git commit -m "Add workspaceRole field to WorkspaceUser domain entity"
```

---

## Task 4: Create ProjectMember Domain Entity

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/ProjectMember.java`

- [ ] **Step 1: Create ProjectMember entity**

```java
package com.bytechef.automation.configuration.domain;

import com.bytechef.platform.security.constant.ProjectRole;
import java.time.Instant;
import java.util.Objects;
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
@Table("project_member")
public class ProjectMember {

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

    @Column("project_role")
    private ProjectRole projectRole;

    @Column("user_id")
    private Long userId;

    @Version
    private int version;

    private ProjectMember() {
    }

    public ProjectMember(Long projectId, Long userId, ProjectRole projectRole) {
        this.projectId = projectId;
        this.userId = userId;
        this.projectRole = projectRole;
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

    public ProjectRole getProjectRole() {
        return projectRole;
    }

    public Long getUserId() {
        return userId;
    }

    public int getVersion() {
        return version;
    }

    public void setProjectRole(ProjectRole projectRole) {
        this.projectRole = projectRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProjectMember projectMember = (ProjectMember) o;

        return Objects.equals(id, projectMember.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ProjectMember{" +
            "id=" + id +
            ", projectId=" + projectId +
            ", userId=" + userId +
            ", projectRole=" + projectRole +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-api:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/domain/ProjectMember.java
git commit -m "Add ProjectMember domain entity for project-level RBAC"
```

---

## Task 5: ProjectMember Repository and Service

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/repository/ProjectMemberRepository.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ProjectMemberService.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ProjectMemberServiceImpl.java`

- [ ] **Step 1: Create ProjectMemberRepository**

```java
package com.bytechef.automation.configuration.repository;

import com.bytechef.automation.configuration.domain.ProjectMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ProjectMemberRepository extends ListCrudRepository<ProjectMember, Long> {

    List<ProjectMember> findAllByProjectId(long projectId);

    List<ProjectMember> findAllByUserId(long userId);

    Optional<ProjectMember> findByProjectIdAndUserId(long projectId, long userId);

    void deleteByProjectIdAndUserId(long projectId, long userId);

    long countByProjectId(long projectId);
}
```

- [ ] **Step 2: Create ProjectMemberService interface**

```java
package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.ProjectMember;
import com.bytechef.platform.security.constant.ProjectRole;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ProjectMemberService {

    ProjectMember addProjectMember(long projectId, long userId, ProjectRole projectRole);

    void deleteProjectMember(long projectId, long userId);

    Optional<ProjectMember> fetchProjectMember(long projectId, long userId);

    List<ProjectMember> getProjectMembers(long projectId);

    List<ProjectMember> getUserProjectMemberships(long userId);

    ProjectMember updateProjectMemberRole(long projectId, long userId, ProjectRole projectRole);
}
```

- [ ] **Step 3: Create ProjectMemberServiceImpl**

```java
package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.ProjectMember;
import com.bytechef.automation.configuration.repository.ProjectMemberRepository;
import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.security.constant.ProjectRole;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnCEVersion
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectMemberServiceImpl(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    @Override
    public ProjectMember addProjectMember(long projectId, long userId, ProjectRole projectRole) {
        Optional<ProjectMember> existingMember = projectMemberRepository.findByProjectIdAndUserId(projectId, userId);

        if (existingMember.isPresent()) {
            throw new IllegalArgumentException(
                "User " + userId + " is already a member of project " + projectId);
        }

        return projectMemberRepository.save(new ProjectMember(projectId, userId, projectRole));
    }

    @Override
    public void deleteProjectMember(long projectId, long userId) {
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectMember> fetchProjectMember(long projectId, long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMember> getProjectMembers(long projectId) {
        return projectMemberRepository.findAllByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMember> getUserProjectMemberships(long userId) {
        return projectMemberRepository.findAllByUserId(userId);
    }

    @Override
    public ProjectMember updateProjectMemberRole(long projectId, long userId, ProjectRole projectRole) {
        ProjectMember projectMember = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "User " + userId + " is not a member of project " + projectId));

        projectMember.setProjectRole(projectRole);

        return projectMemberRepository.save(projectMember);
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/repository/ProjectMemberRepository.java \
       server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/ProjectMemberService.java \
       server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ProjectMemberServiceImpl.java
git commit -m "Add ProjectMemberService with repository for project membership management"
```

---

## Task 6: Update WorkspaceUserService and Repository

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/WorkspaceUserService.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/WorkspaceUserServiceImpl.java`
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/repository/WorkspaceUserRepository.java`

- [ ] **Step 1: Add findByUserIdAndWorkspaceId to WorkspaceUserRepository**

Add method:

```java
Optional<WorkspaceUser> findByUserIdAndWorkspaceId(long userId, long workspaceId);
```

- [ ] **Step 2: Add methods to WorkspaceUserService interface**

Add imports for `Optional` and `WorkspaceRole`, then add methods:

```java
Optional<WorkspaceUser> fetchWorkspaceUser(long userId, long workspaceId);

WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole);

WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole);
```

- [ ] **Step 3: Implement new methods in WorkspaceUserServiceImpl**

```java
@Override
@Transactional(readOnly = true)
public Optional<WorkspaceUser> fetchWorkspaceUser(long userId, long workspaceId) {
    return workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId);
}

@Override
public WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole) {
    return workspaceUserRepository.save(new WorkspaceUser(userId, workspaceId, workspaceRole));
}

@Override
public WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole) {
    WorkspaceUser workspaceUser = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
        .orElseThrow(() -> new IllegalArgumentException(
            "User " + userId + " is not a member of workspace " + workspaceId));

    workspaceUser.setWorkspaceRole(workspaceRole);

    return workspaceUserRepository.save(workspaceUser);
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/WorkspaceUserService.java \
       server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/WorkspaceUserServiceImpl.java \
       server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/repository/WorkspaceUserRepository.java
git commit -m "Add role-aware methods to WorkspaceUserService and repository"
```

---

## Task 7: PermissionService — The Authorization Engine

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/PermissionService.java`
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java`

No new dependencies needed — Spring Cache (`@Cacheable`/`@CacheEvict`) is already configured in `cache-config` with `@EnableCaching`. The existing `CacheConfiguration` provides Caffeine (dev) or Redis (prod) as the backing store. The `TenantKeyGenerator` is the default key generator, so cache keys are automatically tenant-scoped.

- [ ] **Step 1: Create PermissionService interface**

```java
package com.bytechef.automation.configuration.service;

/**
 * Core authorization engine. Registered as Spring bean "permissionService" for use
 * in @PreAuthorize SpEL expressions:
 *
 * <pre>
 * {@code @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'WORKFLOW_EDIT')")}
 * </pre>
 *
 * @author Ivica Cardic
 */
public interface PermissionService {

    // Tenant-level checks (based on existing ROLE_ADMIN authority)

    boolean isTenantAdmin();

    // Workspace-level checks

    boolean hasWorkspaceRole(long workspaceId, String minimumRole);

    // Project-level checks (scope-based)

    boolean hasProjectScope(long projectId, String scope);

    boolean hasProjectRole(long projectId, String minimumRole);

    // Cache management

    void evictProjectScopeCache(long userId, long projectId);

    void evictAllProjectScopeCacheForUser(long userId);
}
```

- [ ] **Step 2: Create PermissionServiceImpl with Spring Cache**

Uses `@Cacheable` for scope lookups and `@CacheEvict` for invalidation. The cache name `"projectScopes"` is auto-created by `CaffeineCacheManager` (which has no fixed list — it creates caches on demand). The `TenantKeyGenerator` (default) automatically prefixes keys with the current tenant ID.

```java
package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.ProjectMember;
import com.bytechef.automation.configuration.repository.ProjectMemberRepository;
import com.bytechef.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.constant.BuiltInRoleScopes;
import com.bytechef.platform.security.constant.PermissionScope;
import com.bytechef.platform.security.constant.ProjectRole;
import com.bytechef.platform.security.constant.WorkspaceRole;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.service.UserService;
import java.util.Collections;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service("permissionService")
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private static final String PROJECT_SCOPES_CACHE = "projectScopes";

    private final ProjectMemberRepository projectMemberRepository;
    private final UserService userService;
    private final WorkspaceUserRepository workspaceUserRepository;

    public PermissionServiceImpl(
        ProjectMemberRepository projectMemberRepository, UserService userService,
        WorkspaceUserRepository workspaceUserRepository) {

        this.projectMemberRepository = projectMemberRepository;
        this.userService = userService;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    public boolean isTenantAdmin() {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        if (isTenantAdmin()) {
            return true;
        }

        WorkspaceRole minimum = WorkspaceRole.valueOf(minimumRole);
        long userId = getCurrentUserId();

        return workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .map(member -> member.getWorkspaceRole().ordinal() <= minimum.ordinal())
            .orElse(false);
    }

    @Override
    public boolean hasProjectScope(long projectId, String scope) {
        if (isTenantAdmin()) {
            return true;
        }

        PermissionScope required = PermissionScope.valueOf(scope);
        long userId = getCurrentUserId();

        Set<PermissionScope> scopes = getProjectScopes(userId, projectId);

        return scopes.contains(required);
    }

    @Override
    public boolean hasProjectRole(long projectId, String minimumRole) {
        if (isTenantAdmin()) {
            return true;
        }

        ProjectRole minimum = ProjectRole.valueOf(minimumRole);
        long userId = getCurrentUserId();

        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
            .map(member -> member.getProjectRole().ordinal() <= minimum.ordinal())
            .orElse(false);
    }

    @Override
    @CacheEvict(value = PROJECT_SCOPES_CACHE)
    public void evictProjectScopeCache(long userId, long projectId) {
        // Spring Cache handles eviction via the annotation.
        // Method args (userId, projectId) form the cache key via TenantKeyGenerator.
    }

    @Override
    @CacheEvict(value = PROJECT_SCOPES_CACHE, allEntries = true)
    public void evictAllProjectScopeCacheForUser(long userId) {
        // Evicts all entries in the projectScopes cache for the current tenant.
        // NOTE: This is broader than ideal (evicts all users in the tenant).
        // For per-user eviction, iterate project memberships and call
        // evictProjectScopeCache(userId, projectId) per entry instead.
    }

    @Cacheable(value = PROJECT_SCOPES_CACHE)
    public Set<PermissionScope> getProjectScopes(long userId, long projectId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
            .map(ProjectMember::getProjectRole)
            .map(BuiltInRoleScopes::getScopesForRole)
            .orElse(Collections.emptySet());
    }

    private long getCurrentUserId() {
        String login = SecurityUtils.getCurrentUserLogin();

        return userService.getUser(login).getId();
    }
}
```

**Key points:**
- `@Cacheable(value = "projectScopes")` on `getProjectScopes(userId, projectId)` — cache key is auto-generated from `(userId, projectId)` by `TenantKeyGenerator`, which also includes the tenant ID.
- `@CacheEvict` on eviction methods uses the same cache name and key derivation.
- `getProjectScopes` must be `public` for Spring's cache proxy to intercept it (private methods bypass the proxy).
- No direct Caffeine dependency — Spring Cache abstraction means this works with both Caffeine (dev) and Redis (prod).
- `UserRepository.findByLogin` is already `@Cacheable` (see `USERS_BY_LOGIN_CACHE`), so `getCurrentUserId()` is already cached at the repository level.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/service/PermissionService.java \
       server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java
git commit -m "Add PermissionService with Spring Cache-backed scope resolution for RBAC"
```

---

## Task 8: Write PermissionService Unit Tests

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/PermissionServiceTest.java`

- [ ] **Step 1: Write the test class**

```java
package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectMember;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.automation.configuration.repository.ProjectMemberRepository;
import com.bytechef.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.security.constant.PermissionScope;
import com.bytechef.platform.security.constant.ProjectRole;
import com.bytechef.platform.security.constant.WorkspaceRole;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class PermissionServiceTest {

    private ProjectMemberRepository projectMemberRepository;
    private UserService userService;
    private WorkspaceUserRepository workspaceUserRepository;
    private PermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        projectMemberRepository = mock(ProjectMemberRepository.class);
        userService = mock(UserService.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        permissionService = new PermissionServiceImpl(
            projectMemberRepository, userService, workspaceUserRepository);
    }

    @Test
    void testHasProjectScopeAsEditor() {
        setUpSecurityContext("editor@test.com", "ROLE_USER");
        setUpUserLookup("editor@test.com", 100L);

        when(projectMemberRepository.findByProjectIdAndUserId(1L, 100L))
            .thenReturn(Optional.of(new ProjectMember(1L, 100L, ProjectRole.EDITOR)));

        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_CREATE")).isTrue();
        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_EDIT")).isTrue();
        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_DELETE")).isFalse();
    }

    @Test
    void testHasProjectScopeAsViewer() {
        setUpSecurityContext("viewer@test.com", "ROLE_USER");
        setUpUserLookup("viewer@test.com", 101L);

        when(projectMemberRepository.findByProjectIdAndUserId(1L, 101L))
            .thenReturn(Optional.of(new ProjectMember(1L, 101L, ProjectRole.VIEWER)));

        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_CREATE")).isFalse();
        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_EDIT")).isFalse();
    }

    @Test
    void testHasProjectScopeAsOperator() {
        setUpSecurityContext("operator@test.com", "ROLE_USER");
        setUpUserLookup("operator@test.com", 102L);

        when(projectMemberRepository.findByProjectIdAndUserId(1L, 102L))
            .thenReturn(Optional.of(new ProjectMember(1L, 102L, ProjectRole.OPERATOR)));

        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_TOGGLE")).isTrue();
        assertThat(permissionService.hasProjectScope(1L, "EXECUTION_RETRY")).isTrue();
        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_CREATE")).isFalse();
        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_EDIT")).isFalse();
    }

    @Test
    void testTenantAdminBypassesAllChecks() {
        setUpSecurityContext("admin@test.com", "ROLE_ADMIN");

        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_DELETE")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(1L, "ADMIN")).isTrue();
        assertThat(permissionService.isTenantAdmin()).isTrue();
    }

    @Test
    void testNonMemberHasNoScope() {
        setUpSecurityContext("outsider@test.com", "ROLE_USER");
        setUpUserLookup("outsider@test.com", 999L);

        when(projectMemberRepository.findByProjectIdAndUserId(1L, 999L))
            .thenReturn(Optional.empty());

        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_VIEW")).isFalse();
    }

    @Test
    void testHasWorkspaceRoleAsAdmin() {
        setUpSecurityContext("wsadmin@test.com", "ROLE_USER");
        setUpUserLookup("wsadmin@test.com", 200L);

        WorkspaceUser workspaceUser = new WorkspaceUser(200L, 1L, WorkspaceRole.ADMIN);

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(200L, 1L))
            .thenReturn(Optional.of(workspaceUser));

        assertThat(permissionService.hasWorkspaceRole(1L, "ADMIN")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(1L, "EDITOR")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(1L, "VIEWER")).isTrue();
    }

    @Test
    void testHasWorkspaceRoleAsViewer() {
        setUpSecurityContext("wsviewer@test.com", "ROLE_USER");
        setUpUserLookup("wsviewer@test.com", 201L);

        WorkspaceUser workspaceUser = new WorkspaceUser(201L, 1L, WorkspaceRole.VIEWER);

        when(workspaceUserRepository.findByUserIdAndWorkspaceId(201L, 1L))
            .thenReturn(Optional.of(workspaceUser));

        assertThat(permissionService.hasWorkspaceRole(1L, "VIEWER")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(1L, "EDITOR")).isFalse();
        assertThat(permissionService.hasWorkspaceRole(1L, "ADMIN")).isFalse();
    }

    @Test
    void testScopeResolutionAfterRoleChange() {
        setUpSecurityContext("cached@test.com", "ROLE_USER");
        setUpUserLookup("cached@test.com", 300L);

        when(projectMemberRepository.findByProjectIdAndUserId(1L, 300L))
            .thenReturn(Optional.of(new ProjectMember(1L, 300L, ProjectRole.VIEWER)));

        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_EDIT")).isFalse();

        // Simulate role change — in a real scenario, @CacheEvict on
        // evictProjectScopeCache() handles invalidation via Spring Cache.
        // In this unit test (no Spring context), we test the resolution logic directly.
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 300L))
            .thenReturn(Optional.of(new ProjectMember(1L, 300L, ProjectRole.EDITOR)));

        // getProjectScopes is the @Cacheable method — call it directly to verify resolution
        Set<PermissionScope> scopes = permissionService.getProjectScopes(300L, 1L);

        assertThat(scopes).contains(PermissionScope.WORKFLOW_EDIT);
    }

    private void setUpSecurityContext(String login, String authority) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                login, "password", AuthorityUtils.createAuthorityList(authority)));
    }

    private void setUpUserLookup(String login, long userId) {
        User user = mock(User.class);

        when(user.getId()).thenReturn(userId);
        when(userService.getUser(login)).thenReturn(user);
    }
}
```

- [ ] **Step 2: Run the tests**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests "*PermissionServiceTest"`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/PermissionServiceTest.java
git commit -m "Add unit tests for PermissionService scope resolution and caching"
```

---

## Task 9: Apply @PreAuthorize to ProjectService

This task demonstrates the pattern. Apply the same pattern to other services (ConnectionService, WorkflowService, etc.) in follow-up tasks.

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ProjectServiceImpl.java`

- [ ] **Step 1: Read current ProjectServiceImpl to identify methods needing authorization**

Read the full file to understand which methods need project-level or workspace-level checks.

- [ ] **Step 2: Add @PreAuthorize annotations to mutation methods**

Add import:

```java
import org.springframework.security.access.prepost.PreAuthorize;
```

Add annotations to methods that modify project state. Examples:

```java
@Override
@PreAuthorize("@permissionService.hasWorkspaceRole(#project.workspaceId, 'EDITOR')")
public Project create(Project project) {
    // ...
}

@Override
@PreAuthorize("@permissionService.hasProjectScope(#id, 'WORKFLOW_DELETE')")
public void delete(long id) {
    // ...
}

@Override
@PreAuthorize("@permissionService.hasProjectScope(#id, 'WORKFLOW_EDIT')")
public Project update(long id, List<Long> tagIds) {
    // ...
}

@Override
@PreAuthorize("@permissionService.hasProjectScope(#project.id, 'WORKFLOW_EDIT')")
public Project update(Project project) {
    // ...
}
```

Read-only methods that return project data should check `WORKFLOW_VIEW` or rely on workspace membership filtering in the facade (existing pattern). The appropriate granularity depends on whether the method is project-specific (needs `hasProjectScope`) or workspace-wide (filtered by facade):

```java
@Override
@PreAuthorize("@permissionService.hasProjectScope(#id, 'WORKFLOW_VIEW')")
@Transactional(readOnly = true)
public Project getProject(long id) {
    // ...
}
```

**Important:** Methods called internally by other services (e.g., `getWorkflowProject(workflowId)`) should NOT get `@PreAuthorize` if they're part of internal orchestration. Only methods that represent user-facing operations need authorization. Use judgment per method.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run existing tests to check for regressions**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test`
Expected: Some tests may fail because they don't set up a security context. Tests that call `@PreAuthorize`-annotated methods need a security context. Fix by adding `@WithMockUser` or setting up `SecurityContextHolder` in test setup.

- [ ] **Step 5: Fix failing tests by adding security context**

For integration tests, add `@WithMockUser(authorities = "ROLE_ADMIN")` to test classes so the admin bypass lets existing tests pass unchanged:

```java
@SpringBootTest
@WithMockUser(authorities = "ROLE_ADMIN")
class ProjectServiceIntTest {
    // existing tests pass because ROLE_ADMIN bypasses @PreAuthorize
}
```

For unit tests that call service methods directly without Spring Security context, either:
- Mock the security context in `@BeforeEach`
- Or add `@WithMockUser` if using `@SpringBootTest`

- [ ] **Step 6: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/ProjectServiceImpl.java
git commit -m "Add @PreAuthorize checks to ProjectService for project-level RBAC"
```

---

## Task 10: Refactor WorkspaceFacade to Use PermissionService

Replace the inline if/else authority check in `WorkspaceFacadeImpl.getUserWorkspaces()` with a call to `PermissionService`.

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceFacadeImpl.java`

- [ ] **Step 1: Read current WorkspaceFacadeImpl**

Read the full file to understand the current inline authorization pattern.

- [ ] **Step 2: Inject PermissionService and simplify getUserWorkspaces**

Replace the manual authority check with `permissionService.isTenantAdmin()`:

Before (current code):
```java
List<String> userAuthorityNames = user.getAuthorityIds()
    .stream()
    .map(authorityId -> CollectionUtils.getFirst(
        authorities, authority -> Objects.equals(authority.getId(), authorityId)))
    .map(Authority::getName)
    .toList();

if (!userAuthorityNames.contains(AuthorityConstants.ADMIN)) {
    // filter workspaces
}
```

After:
```java
if (!permissionService.isTenantAdmin()) {
    List<Long> userWorkspaceIds = workspaceUserService.getUserWorkspaceUsers(id)
        .stream()
        .map(WorkspaceUser::getWorkspaceId)
        .toList();

    workspaces = workspaces.stream()
        .filter(workspace -> userWorkspaceIds.contains(workspace.getId()))
        .toList();
}
```

This removes the dependency on `AuthorityService` and `UserService` from the facade (if they were only used for this check). The facade becomes simpler.

- [ ] **Step 3: Remove unused dependencies if applicable**

If `AuthorityService` and `UserService` were only injected for the permission check, remove them from the constructor.

- [ ] **Step 4: Verify compilation and tests**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 5: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceFacadeImpl.java
git commit -m "Refactor WorkspaceFacade to use PermissionService instead of inline authority checks"
```

---

## Task 11: ProjectMember Integration Test

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/ProjectMemberServiceIntTest.java`

- [ ] **Step 1: Write integration test**

Follow the existing integration test pattern (Testcontainers, `@SpringBootTest`, `@ActiveProfiles("testint")`):

```java
package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.domain.ProjectMember;
import com.bytechef.platform.security.constant.ProjectRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testint")
@WithMockUser(authorities = "ROLE_ADMIN")
class ProjectMemberServiceIntTest {

    @Autowired
    private ProjectMemberService projectMemberService;

    @Test
    void testAddAndFetchProjectMember() {
        // NOTE: Requires a project with known ID to exist in test data.
        // Adapt projectId based on test fixtures.
        long projectId = 1050L;
        long userId = 1050L;

        ProjectMember member = projectMemberService.addProjectMember(
            projectId, userId, ProjectRole.EDITOR);

        assertThat(member.getId()).isNotNull();
        assertThat(member.getProjectRole()).isEqualTo(ProjectRole.EDITOR);

        Optional<ProjectMember> fetched = projectMemberService.fetchProjectMember(projectId, userId);

        assertThat(fetched).isPresent();
        assertThat(fetched.get().getProjectRole()).isEqualTo(ProjectRole.EDITOR);
    }

    @Test
    void testUpdateProjectMemberRole() {
        long projectId = 1050L;
        long userId = 1051L;

        projectMemberService.addProjectMember(projectId, userId, ProjectRole.VIEWER);

        ProjectMember updated = projectMemberService.updateProjectMemberRole(
            projectId, userId, ProjectRole.ADMIN);

        assertThat(updated.getProjectRole()).isEqualTo(ProjectRole.ADMIN);
    }

    @Test
    void testGetProjectMembers() {
        long projectId = 1050L;
        long userId = 1052L;

        projectMemberService.addProjectMember(projectId, userId, ProjectRole.OPERATOR);

        List<ProjectMember> members = projectMemberService.getProjectMembers(projectId);

        assertThat(members).isNotEmpty();
        assertThat(members).anyMatch(
            member -> member.getUserId().equals(userId));
    }

    @Test
    void testDeleteProjectMember() {
        long projectId = 1050L;
        long userId = 1053L;

        projectMemberService.addProjectMember(projectId, userId, ProjectRole.VIEWER);
        projectMemberService.deleteProjectMember(projectId, userId);

        Optional<ProjectMember> fetched = projectMemberService.fetchProjectMember(projectId, userId);

        assertThat(fetched).isEmpty();
    }

    @Test
    void testAddDuplicateMemberThrows() {
        long projectId = 1050L;
        long userId = 1054L;

        projectMemberService.addProjectMember(projectId, userId, ProjectRole.EDITOR);

        assertThatThrownBy(
            () -> projectMemberService.addProjectMember(projectId, userId, ProjectRole.VIEWER))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

**Note:** This test skeleton needs adaptation based on the actual test infrastructure (test data fixtures, database setup, etc.). The exact project/user IDs depend on what the test database seeds.

- [ ] **Step 2: Run integration tests**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests "*ProjectMemberServiceIntTest"`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/service/ProjectMemberServiceIntTest.java
git commit -m "Add integration tests for ProjectMemberService"
```

---

## Task 12: Auto-Assign Project Creator as ADMIN

When a project is created, the creator should automatically become a ProjectMember with ADMIN role.

**Files:**
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectFacadeImpl.java`

- [ ] **Step 1: Read current ProjectFacadeImpl.createProject()**

Read the file to understand the create flow.

- [ ] **Step 2: Inject ProjectMemberService and UserService, add auto-assignment**

After the project is created (has an ID), add the creator as ADMIN:

```java
// In createProject method, after project = projectService.create(project):

String currentLogin = SecurityUtils.getCurrentUserLogin();
User currentUser = userService.getUser(currentLogin);

projectMemberService.addProjectMember(project.getId(), currentUser.getId(), ProjectRole.ADMIN);
```

- [ ] **Step 3: Verify compilation and tests**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectFacadeImpl.java
git commit -m "Auto-assign project creator as ADMIN ProjectMember on creation"
```

---

## Task 13: Auto-Assign Workspace Creator as ADMIN

When a workspace user is added via existing flows, ensure the workspace creator gets ADMIN role. For existing workspace_user rows, the migration default is EDITOR — the workspace creator should be updated.

**Files:**
- Modify: flows that create WorkspaceUser entries

- [ ] **Step 1: Find all places that create WorkspaceUser**

Search for `new WorkspaceUser(` in the codebase. Update to include `WorkspaceRole.ADMIN` or `WorkspaceRole.EDITOR` as appropriate.

- [ ] **Step 2: Update workspace creation flow**

When a workspace is created and the creator is added as a member, use:

```java
workspaceUserService.addWorkspaceUser(userId, workspaceId, WorkspaceRole.ADMIN);
```

When inviting a new member to a workspace, default to EDITOR:

```java
workspaceUserService.addWorkspaceUser(userId, workspaceId, WorkspaceRole.EDITOR);
```

- [ ] **Step 3: Add data migration for existing default workspace**

Create a Liquibase changeset to set the admin user's workspace_role to ADMIN for the default workspace (ID 1049):

```xml
<changeSet id="202604061200010-1" author="Ivica Cardic">
    <update tableName="workspace_user">
        <column name="workspace_role" value="ADMIN"/>
        <where>workspace_id = 1049 AND user_id = (SELECT id FROM "user" WHERE login = 'admin')</where>
    </update>
</changeSet>
```

- [ ] **Step 4: Verify and commit**

```bash
git commit -m "Assign workspace roles on user creation; migrate default workspace admin"
```

---

## Task 14: GraphQL Schema for Member Management

**Files:**
- Create: GraphQL schema for project member and workspace member mutations
- Create: GraphQL controller for member management

This task adds the API layer for managing members and roles. The exact location depends on whether this goes in the existing `automation-configuration-graphql` module or a new one.

- [ ] **Step 1: Add GraphQL schema**

Create `project_member.graphqls`:

```graphql
extend type Query {
    projectMembers(projectId: Long!): [ProjectMember!]!
}

extend type Mutation {
    addProjectMember(projectId: Long!, userId: Long!, role: ProjectRole!): ProjectMember!
    updateProjectMemberRole(projectId: Long!, userId: Long!, role: ProjectRole!): ProjectMember!
    removeProjectMember(projectId: Long!, userId: Long!): Boolean!
}

type ProjectMember {
    id: ID!
    projectId: Long!
    userId: Long!
    role: ProjectRole!
    createdDate: Long
}

enum ProjectRole {
    ADMIN
    EDITOR
    OPERATOR
    VIEWER
}
```

- [ ] **Step 2: Create GraphQL controller**

```java
@Controller
public class ProjectMemberGraphQlController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberGraphQlController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @QueryMapping
    public List<ProjectMember> projectMembers(@Argument long projectId) {
        return projectMemberService.getProjectMembers(projectId);
    }

    @MutationMapping
    @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_MEMBERS')")
    public ProjectMember addProjectMember(
            @Argument long projectId, @Argument long userId, @Argument ProjectRole role) {

        return projectMemberService.addProjectMember(projectId, userId, role);
    }

    @MutationMapping
    @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_MEMBERS')")
    public ProjectMember updateProjectMemberRole(
            @Argument long projectId, @Argument long userId, @Argument ProjectRole role) {

        return projectMemberService.updateProjectMemberRole(projectId, userId, role);
    }

    @MutationMapping
    @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_MEMBERS')")
    public boolean removeProjectMember(@Argument long projectId, @Argument long userId) {
        projectMemberService.deleteProjectMember(projectId, userId);
        return true;
    }
}
```

**Note:** `@PreAuthorize` on the GraphQL controller is acceptable here because these are member-management operations that don't go through a service with its own `@PreAuthorize`. The controller is the entry point for this specific operation.

- [ ] **Step 3: Register codegen, verify compilation**

Add schema path to `client/codegen.ts` schema array. Run `npx graphql-codegen`.

- [ ] **Step 4: Commit**

```bash
git commit -m "Add GraphQL schema and controller for project member management"
```

---

## Task 15: Evict Cache on Role Changes

When a project member's role changes (or they're added/removed), evict the Caffeine cache.

**Files:**
- Modify: `ProjectMemberServiceImpl.java`

- [ ] **Step 1: Inject PermissionService and call eviction**

```java
@Service
@Transactional
@ConditionalOnCEVersion
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final PermissionService permissionService;
    private final ProjectMemberRepository projectMemberRepository;

    // ... constructor updated with PermissionService

    @Override
    public ProjectMember addProjectMember(long projectId, long userId, ProjectRole projectRole) {
        // ... existing code ...

        ProjectMember saved = projectMemberRepository.save(new ProjectMember(projectId, userId, projectRole));

        permissionService.evictProjectScopeCache(userId, projectId);

        return saved;
    }

    @Override
    public void deleteProjectMember(long projectId, long userId) {
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);

        permissionService.evictProjectScopeCache(userId, projectId);
    }

    @Override
    public ProjectMember updateProjectMemberRole(long projectId, long userId, ProjectRole projectRole) {
        // ... existing code ...

        ProjectMember saved = projectMemberRepository.save(projectMember);

        permissionService.evictProjectScopeCache(userId, projectId);

        return saved;
    }
}
```

- [ ] **Step 2: Verify compilation and tests**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git commit -m "Evict permission cache on project member role changes"
```

---

## Task 16: Custom Roles — EE Domain Model and Migrations

Custom roles allow EE tenants to define fine-grained permission sets beyond the built-in ADMIN/EDITOR/OPERATOR/VIEWER roles. A `CustomRole` has a name and a set of `PermissionScope` entries. A `ProjectMember` can reference either a built-in `ProjectRole` OR a `CustomRole` (XOR constraint).

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/domain/CustomRole.java`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/domain/CustomRoleScope.java`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/202604061200030_automation_configuration_added_table_custom_role.xml`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/resources/config/liquibase/changelog/automation/configuration/202604061200040_automation_configuration_added_column_project_member_custom_role.xml`

- [ ] **Step 1: Create Liquibase migration for custom_role and custom_role_scope tables**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="202604061200030" author="Ivica Cardic">
        <createTable tableName="custom_role">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="description" type="VARCHAR(500)"/>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addUniqueConstraint
            tableName="custom_role"
            columnNames="name"
            constraintName="uk_custom_role_name"/>

        <createTable tableName="custom_role_scope">
            <column name="custom_role_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="scope" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addPrimaryKey tableName="custom_role_scope" columnNames="custom_role_id,scope"/>

        <addForeignKeyConstraint
            baseTableName="custom_role_scope"
            baseColumnNames="custom_role_id"
            constraintName="fk_custom_role_scope_custom_role"
            referencedTableName="custom_role"
            referencedColumnNames="id"
            onDelete="CASCADE"/>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Create migration to add custom_role_id to project_member**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="202604061200040" author="Ivica Cardic">
        <addColumn tableName="project_member">
            <column name="custom_role_id" type="BIGINT"/>
        </addColumn>

        <addForeignKeyConstraint
            baseTableName="project_member"
            baseColumnNames="custom_role_id"
            constraintName="fk_project_member_custom_role"
            referencedTableName="custom_role"
            referencedColumnNames="id"
            onDelete="SET NULL"/>

        <!-- Make project_role nullable (was NOT NULL) — now either project_role or custom_role_id is set -->
        <dropNotNullConstraint tableName="project_member" columnName="project_role" columnDataType="VARCHAR(20)"/>

        <!-- XOR constraint: exactly one of project_role or custom_role_id must be set -->
        <sql>
            ALTER TABLE project_member ADD CONSTRAINT chk_project_member_role_xor
            CHECK ((project_role IS NOT NULL AND custom_role_id IS NULL) OR
                   (project_role IS NULL AND custom_role_id IS NOT NULL));
        </sql>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 3: Create CustomRole entity**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("custom_role")
public class CustomRole {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private String description;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @MappedCollection(idColumn = "custom_role_id")
    private Set<CustomRoleScope> scopes = new HashSet<>();

    @Version
    private int version;

    public CustomRole() {
    }

    public CustomRole(String name, String description, Set<CustomRoleScope> scopes) {
        this.name = name;
        this.description = description;
        this.scopes = scopes;
    }

    // Getters, setters, equals, hashCode, toString — follow WorkspaceUser pattern
    // ... (full implementation follows the standard domain entity pattern)

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Set<CustomRoleScope> getScopes() { return scopes; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setScopes(Set<CustomRoleScope> scopes) { this.scopes = scopes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return Objects.equals(id, ((CustomRole) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
```

- [ ] **Step 4: Create CustomRoleScope entity**

```java
package com.bytechef.ee.automation.configuration.domain;

import com.bytechef.platform.security.constant.PermissionScope;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("custom_role_scope")
public class CustomRoleScope {

    @Column("scope")
    private PermissionScope scope;

    private CustomRoleScope() {
    }

    public CustomRoleScope(PermissionScope scope) {
        this.scope = scope;
    }

    public PermissionScope getScope() {
        return scope;
    }
}
```

- [ ] **Step 5: Update ProjectMember to support custom_role_id**

Add to `ProjectMember.java`:

```java
@Column("custom_role_id")
private Long customRoleId;

public Long getCustomRoleId() { return customRoleId; }
public void setCustomRoleId(Long customRoleId) { this.customRoleId = customRoleId; }
```

- [ ] **Step 6: Verify compilation and commit**

```bash
git commit -m "Add CustomRole EE domain model with migrations and ProjectMember extension"
```

---

## Task 17: Custom Roles — EE Service Layer

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/ee/automation/configuration/service/CustomRoleService.java`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/service/CustomRoleServiceImpl.java`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/repository/CustomRoleRepository.java`

- [ ] **Step 1: Create CustomRoleRepository**

```java
package com.bytechef.ee.automation.configuration.repository;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public interface CustomRoleRepository extends ListCrudRepository<CustomRole, Long> {

    Optional<CustomRole> findByName(String name);
}
```

- [ ] **Step 2: Create CustomRoleService interface**

```java
package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.platform.security.constant.PermissionScope;
import java.util.List;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CustomRoleService {

    CustomRole createCustomRole(String name, String description, Set<PermissionScope> scopes);

    void deleteCustomRole(long roleId);

    CustomRole getCustomRole(long roleId);

    List<CustomRole> getCustomRoles();

    CustomRole updateCustomRole(long roleId, String name, String description, Set<PermissionScope> scopes);
}
```

- [ ] **Step 3: Create CustomRoleServiceImpl**

```java
package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.repository.ProjectMemberRepository;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.CustomRoleScope;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.PermissionScope;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class CustomRoleServiceImpl implements CustomRoleService {

    private final CustomRoleRepository customRoleRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public CustomRoleServiceImpl(
        CustomRoleRepository customRoleRepository, ProjectMemberRepository projectMemberRepository) {

        this.customRoleRepository = customRoleRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Override
    @PreAuthorize("@permissionService.isTenantAdmin()")
    public CustomRole createCustomRole(String name, String description, Set<PermissionScope> scopes) {
        Set<CustomRoleScope> customRoleScopes = scopes.stream()
            .map(CustomRoleScope::new)
            .collect(Collectors.toSet());

        return customRoleRepository.save(new CustomRole(name, description, customRoleScopes));
    }

    @Override
    @PreAuthorize("@permissionService.isTenantAdmin()")
    public void deleteCustomRole(long roleId) {
        long memberCount = projectMemberRepository.countByCustomRoleId(roleId);

        if (memberCount > 0) {
            throw new IllegalStateException(
                "Cannot delete custom role — assigned to " + memberCount + " project members");
        }

        customRoleRepository.deleteById(roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomRole getCustomRole(long roleId) {
        return customRoleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Custom role not found: " + roleId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomRole> getCustomRoles() {
        return customRoleRepository.findAll();
    }

    @Override
    @PreAuthorize("@permissionService.isTenantAdmin()")
    public CustomRole updateCustomRole(
        long roleId, String name, String description, Set<PermissionScope> scopes) {

        CustomRole customRole = getCustomRole(roleId);

        customRole.setName(name);
        customRole.setDescription(description);

        Set<CustomRoleScope> customRoleScopes = scopes.stream()
            .map(CustomRoleScope::new)
            .collect(Collectors.toSet());

        customRole.setScopes(customRoleScopes);

        return customRoleRepository.save(customRole);
    }
}
```

- [ ] **Step 4: Add countByCustomRoleId to ProjectMemberRepository**

```java
long countByCustomRoleId(long customRoleId);
```

- [ ] **Step 5: Update PermissionService to resolve custom role scopes**

In `PermissionServiceImpl.getProjectScopes()`, extend resolution to handle custom roles:

```java
@Cacheable(value = PROJECT_SCOPES_CACHE)
public Set<PermissionScope> getProjectScopes(long userId, long projectId) {
    return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
        .map(member -> {
            if (member.getProjectRole() != null) {
                return BuiltInRoleScopes.getScopesForRole(member.getProjectRole());
            }

            if (member.getCustomRoleId() != null && customRoleRepository != null) {
                return customRoleRepository.findById(member.getCustomRoleId())
                    .map(customRole -> customRole.getScopes().stream()
                        .map(CustomRoleScope::getScope)
                        .collect(Collectors.toSet()))
                    .orElse(Collections.emptySet());
            }

            return Collections.<PermissionScope>emptySet();
        })
        .orElse(Collections.emptySet());
}
```

Inject `CustomRoleRepository` as `@Nullable` (it won't be available in CE):

```java
public PermissionServiceImpl(
    ProjectMemberRepository projectMemberRepository, UserService userService,
    WorkspaceUserRepository workspaceUserRepository,
    @Nullable CustomRoleRepository customRoleRepository) {
    // ...
    this.customRoleRepository = customRoleRepository;
}
```

- [ ] **Step 6: Verify compilation and commit**

```bash
git commit -m "Add CustomRole EE service with CRUD, scope resolution, and @PreAuthorize guards"
```

---

## Task 18: Custom Roles — EE GraphQL API

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/custom_role.graphqls`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/ee/automation/configuration/web/graphql/CustomRoleGraphQlController.java`

- [ ] **Step 1: Create GraphQL schema**

```graphql
extend type Query {
    customRoles: [CustomRole!]!
    customRole(id: ID!): CustomRole!
}

extend type Mutation {
    createCustomRole(input: CreateCustomRoleInput!): CustomRole!
    updateCustomRole(id: ID!, input: UpdateCustomRoleInput!): CustomRole!
    deleteCustomRole(id: ID!): Boolean!
}

type CustomRole {
    id: ID!
    name: String!
    description: String
    scopes: [String!]!
    createdDate: Long
}

input CreateCustomRoleInput {
    name: String!
    description: String
    scopes: [String!]!
}

input UpdateCustomRoleInput {
    name: String!
    description: String
    scopes: [String!]!
}
```

- [ ] **Step 2: Create GraphQL controller**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.service.CustomRoleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.PermissionScope;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
public class CustomRoleGraphQlController {

    private final CustomRoleService customRoleService;

    public CustomRoleGraphQlController(CustomRoleService customRoleService) {
        this.customRoleService = customRoleService;
    }

    @QueryMapping
    public List<CustomRole> customRoles() {
        return customRoleService.getCustomRoles();
    }

    @QueryMapping
    public CustomRole customRole(@Argument long id) {
        return customRoleService.getCustomRole(id);
    }

    @MutationMapping
    public CustomRole createCustomRole(@Argument CreateCustomRoleInput input) {
        Set<PermissionScope> scopes = input.scopes().stream()
            .map(PermissionScope::valueOf)
            .collect(Collectors.toSet());

        return customRoleService.createCustomRole(input.name(), input.description(), scopes);
    }

    @MutationMapping
    public CustomRole updateCustomRole(@Argument long id, @Argument UpdateCustomRoleInput input) {
        Set<PermissionScope> scopes = input.scopes().stream()
            .map(PermissionScope::valueOf)
            .collect(Collectors.toSet());

        return customRoleService.updateCustomRole(id, input.name(), input.description(), scopes);
    }

    @MutationMapping
    public boolean deleteCustomRole(@Argument long id) {
        customRoleService.deleteCustomRole(id);
        return true;
    }

    record CreateCustomRoleInput(String name, String description, List<String> scopes) {}
    record UpdateCustomRoleInput(String name, String description, List<String> scopes) {}
}
```

- [ ] **Step 3: Commit**

```bash
git commit -m "Add CustomRole EE GraphQL schema and controller"
```

---

## Task 19: Permission Audit Log — Extend Existing EE Audit Module

ByteChef already has an EE audit module at `server/ee/libs/platform/platform-audit/` with `PersistentAuditEvent` and `PersistentAuditEventData`. Rather than creating a new audit system, extend this existing module to capture permission-related events.

**Files:**
- Create: `server/ee/libs/platform/platform-audit/platform-audit-service/src/main/java/com/bytechef/ee/platform/audit/aspect/PermissionAuditAspect.java`
- Modify: `server/ee/libs/platform/platform-audit/platform-audit-service/build.gradle.kts` (add spring-security dependency)

- [ ] **Step 1: Create PermissionAuditAspect**

This AOP aspect intercepts `@PreAuthorize`-annotated methods and logs the result (ALLOWED/DENIED) using the existing `PersistenceAuditEventRepository`.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEventData;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
@ConditionalOnEEVersion
public class PermissionAuditAspect {

    private final PersistenceAuditEventRepository auditEventRepository;

    public PermissionAuditAspect(PersistenceAuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Around("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public Object auditPermissionCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        String principal = SecurityUtils.fetchCurrentUserLogin().orElse("anonymous");
        String methodName = joinPoint.getSignature().toShortString();
        String result = "ALLOWED";

        try {
            return joinPoint.proceed();
        } catch (AccessDeniedException accessDeniedException) {
            result = "DENIED";

            throw accessDeniedException;
        } finally {
            PersistentAuditEvent auditEvent = new PersistentAuditEvent();

            auditEvent.setPrincipal(principal);
            auditEvent.setEventDate(Instant.now());
            auditEvent.setEventType("PERMISSION_CHECK");

            Set<PersistentAuditEventData> eventData = new HashSet<>();

            eventData.add(new PersistentAuditEventData("method", methodName));
            eventData.add(new PersistentAuditEventData("result", result));

            auditEvent.setData(eventData);

            auditEventRepository.save(auditEvent);
        }
    }
}
```

**Note:** This aspect logs every `@PreAuthorize` invocation. In production, consider filtering to only log DENIED events, or using async/batch writing to avoid performance impact. The existing `PersistentAuditEvent` entity and repository handle persistence.

- [ ] **Step 2: Add spring-aop dependency if not present**

Check `build.gradle.kts` for the audit module and add if needed:

```kotlin
implementation("org.springframework:spring-aop")
implementation("org.aspectj:aspectjweaver")
implementation("org.springframework.security:spring-security-core")
```

- [ ] **Step 3: Verify and commit**

```bash
git commit -m "Add PermissionAuditAspect to log @PreAuthorize checks in EE audit module"
```

---

## Task 20: Full Service Rollout — Apply @PreAuthorize to All Key Services

Systematically add `@PreAuthorize` to all user-facing service methods. Each service is a sub-step.

**Approach:** For each service, identify methods that represent user-facing operations (not internal orchestration). Add the appropriate scope check. Methods called only by other services internally should NOT get `@PreAuthorize`.

- [ ] **Step 1: ProjectDeploymentService**

Methods like `enableProjectDeployment`, `createProjectDeployment` need `WORKFLOW_TOGGLE` or `DEPLOYMENT_PUSH` scopes. Read the service, identify entry points, add annotations:

```java
@PreAuthorize("@permissionService.hasProjectScope(#projectId, 'DEPLOYMENT_PUSH')")
public ProjectDeployment createProjectDeployment(long projectId, ...) { ... }
```

- [ ] **Step 2: WorkspaceConnectionFacade / ConnectionService**

Connection mutations need project or workspace scope checks:

```java
// In relevant service methods:
@PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'EDITOR')")
public long createConnection(long workspaceId, ...) { ... }

@PreAuthorize("@permissionService.hasProjectScope(#projectId, 'CONNECTION_EDIT')")
public void updateConnection(long projectId, ...) { ... }
```

- [ ] **Step 3: WorkspaceMcpServerService / AgentService**

Agent and MCP operations:

```java
@PreAuthorize("@permissionService.hasProjectScope(#projectId, 'AGENT_CREATE')")
public McpServer createMcpServer(long projectId, ...) { ... }

@PreAuthorize("@permissionService.hasProjectScope(#projectId, 'AGENT_EXECUTE')")
public Object executeAgent(long projectId, ...) { ... }
```

- [ ] **Step 4: ProjectWorkflowService**

Workflow CRUD operations:

```java
@PreAuthorize("@permissionService.hasProjectScope(#projectId, 'WORKFLOW_CREATE')")
public ProjectWorkflow addWorkflow(long projectId, ...) { ... }

@PreAuthorize("@permissionService.hasProjectScope(#projectId, 'WORKFLOW_DELETE')")
public void deleteWorkflow(long projectId, ...) { ... }
```

- [ ] **Step 5: Fix all broken tests**

After adding `@PreAuthorize` to services, existing tests that call these methods without a security context will fail. For each test class:
- Add `@WithMockUser(authorities = "ROLE_ADMIN")` for integration tests (admin bypass)
- Set up `SecurityContextHolder` in `@BeforeEach` for unit tests

- [ ] **Step 6: Run full test suite**

Run: `./gradlew check`
Expected: All tests pass

- [ ] **Step 7: Commit per service module**

Each service modification should be a separate commit for clean git history:

```bash
git commit -m "Add @PreAuthorize to ProjectDeploymentService"
git commit -m "Add @PreAuthorize to ConnectionService"
git commit -m "Add @PreAuthorize to AgentService and McpServerService"
git commit -m "Add @PreAuthorize to ProjectWorkflowService"
```

---

## Task 21: EE Remote Client Stubs

EE microservice apps (e.g., `configuration-app`, `coordinator-app`) use remote client stubs for services they don't host locally. New services (`ProjectMemberService`, `PermissionService`) need stubs.

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-remote-client/src/main/java/com/bytechef/ee/automation/configuration/remote/client/service/RemoteProjectMemberServiceClient.java`
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-remote-client/src/main/java/com/bytechef/ee/automation/configuration/remote/client/service/RemotePermissionServiceClient.java`

- [ ] **Step 1: Create RemoteProjectMemberServiceClient**

Follow the existing `RemoteWorkspaceConnectionFacadeClient` pattern — `@Component @ConditionalOnEEVersion`, all methods throw `UnsupportedOperationException`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.ProjectMember;
import com.bytechef.automation.configuration.service.ProjectMemberService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.ProjectRole;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteProjectMemberServiceClient implements ProjectMemberService {

    @Override
    public ProjectMember addProjectMember(long projectId, long userId, ProjectRole projectRole) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProjectMember(long projectId, long userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ProjectMember> fetchProjectMember(long projectId, long userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectMember> getProjectMembers(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectMember> getUserProjectMemberships(long userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectMember updateProjectMemberRole(long projectId, long userId, ProjectRole projectRole) {
        throw new UnsupportedOperationException();
    }
}
```

- [ ] **Step 2: Create RemotePermissionServiceClient**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemotePermissionServiceClient implements PermissionService {

    @Override
    public boolean isTenantAdmin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasProjectScope(long projectId, String scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasProjectRole(long projectId, String minimumRole) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictProjectScopeCache(long userId, long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evictAllProjectScopeCacheForUser(long userId) {
        throw new UnsupportedOperationException();
    }
}
```

- [ ] **Step 3: Add dependencies to remote-client build.gradle.kts**

Ensure the remote-client module depends on the API modules where interfaces live.

- [ ] **Step 4: Commit**

```bash
git commit -m "Add EE remote client stubs for ProjectMemberService and PermissionService"
```

---

## Task 22: Client-Side — Permission Store and Role Hooks

**Files:**
- Create: `client/src/shared/stores/usePermissionStore.ts`
- Create: `client/src/shared/hooks/useHasProjectScope.ts`
- Create: `client/src/shared/hooks/useHasWorkspaceRole.ts`
- Modify: `client/src/shared/models/user.model.ts` (if workspace/project role info is needed)

- [ ] **Step 1: Create permission store**

```typescript
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface ProjectPermissionsI {
    [projectId: number]: string[]; // Array of PermissionScope strings
}

interface PermissionStoreI {
    projectPermissions: ProjectPermissionsI;
    workspaceRoles: {[workspaceId: number]: string};
    setProjectPermissions: (projectId: number, scopes: string[]) => void;
    setWorkspaceRole: (workspaceId: number, role: string) => void;
    clearPermissions: () => void;
}

export const usePermissionStore = create<PermissionStoreI>()(
    devtools(
        (set) => ({
            clearPermissions: () => set({projectPermissions: {}, workspaceRoles: {}}),
            projectPermissions: {},
            setProjectPermissions: (projectId, scopes) =>
                set((state) => ({
                    projectPermissions: {...state.projectPermissions, [projectId]: scopes},
                })),
            setWorkspaceRole: (workspaceId, role) =>
                set((state) => ({
                    workspaceRoles: {...state.workspaceRoles, [workspaceId]: role},
                })),
            workspaceRoles: {},
        }),
        {name: 'PermissionStore'}
    )
);
```

- [ ] **Step 2: Create useHasProjectScope hook**

```typescript
import {usePermissionStore} from '@/shared/stores/usePermissionStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {AUTHORITIES} from '@/shared/constants';
import {useShallow} from 'zustand/react/shallow';

export const useHasProjectScope = (projectId: number, scope: string): boolean => {
    const {account} = useAuthenticationStore(
        useShallow((state) => ({account: state.account}))
    );

    const projectPermissions = usePermissionStore(
        (state) => state.projectPermissions[projectId]
    );

    // Tenant admin bypasses all checks
    if (account?.authorities?.includes(AUTHORITIES.ADMIN)) {
        return true;
    }

    return projectPermissions?.includes(scope) || false;
};
```

- [ ] **Step 3: Create GraphQL query to fetch user's project scopes**

Create `client/src/graphql/automation/configuration/projectMemberScopes.graphql`:

```graphql
query ProjectMemberScopes($projectId: Long!) {
    projectMemberScopes(projectId: $projectId)
}
```

- [ ] **Step 4: Commit**

```bash
git commit -m "client - Add permission store and useHasProjectScope hook"
```

---

## Task 23: Client-Side — Project Member Management UI

**Files:**
- Create: `client/src/pages/automation/project/components/project-header/components/settings-menu/ProjectMembersDialog.tsx`
- Create: `client/src/pages/automation/project/components/project-header/components/settings-menu/ProjectMembersTable.tsx`
- Create: `client/src/pages/automation/project/components/project-header/components/settings-menu/AddProjectMemberDialog.tsx`
- Create: `client/src/graphql/automation/configuration/projectMembers.graphql`
- Create: `client/src/graphql/automation/configuration/addProjectMember.graphql`
- Create: `client/src/graphql/automation/configuration/updateProjectMemberRole.graphql`
- Create: `client/src/graphql/automation/configuration/removeProjectMember.graphql`

- [ ] **Step 1: Create GraphQL operation files**

`projectMembers.graphql`:
```graphql
query ProjectMembers($projectId: Long!) {
    projectMembers(projectId: $projectId) {
        id
        projectId
        userId
        role
        createdDate
    }
}
```

`addProjectMember.graphql`:
```graphql
mutation AddProjectMember($projectId: Long!, $userId: Long!, $role: ProjectRole!) {
    addProjectMember(projectId: $projectId, userId: $userId, role: $role) {
        id
        projectId
        userId
        role
    }
}
```

`updateProjectMemberRole.graphql`:
```graphql
mutation UpdateProjectMemberRole($projectId: Long!, $userId: Long!, $role: ProjectRole!) {
    updateProjectMemberRole(projectId: $projectId, userId: $userId, role: $role) {
        id
        role
    }
}
```

`removeProjectMember.graphql`:
```graphql
mutation RemoveProjectMember($projectId: Long!, $userId: Long!) {
    removeProjectMember(projectId: $projectId, userId: $userId)
}
```

- [ ] **Step 2: Run GraphQL codegen**

Run: `cd client && npx graphql-codegen`

- [ ] **Step 3: Create ProjectMembersDialog component**

Follow the `EditUserDialog` pattern from `client/src/pages/settings/platform/users/`:

```typescript
import {Dialog, DialogContent, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Button} from '@/components/ui/button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {TrashIcon, UserPlusIcon} from 'lucide-react';
// ... use generated hooks for queries and mutations

interface ProjectMembersDialogProps {
    onClose: () => void;
    open: boolean;
    projectId: number;
}

const ProjectMembersDialog = ({onClose, open, projectId}: ProjectMembersDialogProps) => {
    // Fetch members via useProjectMembersQuery
    // Render table with role select dropdowns
    // Add member button opens AddProjectMemberDialog
    // Delete button with confirmation
    // Role change triggers updateProjectMemberRole mutation + query invalidation

    return (
        <Dialog onOpenChange={onClose} open={open}>
            <DialogContent className="max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Project Members</DialogTitle>
                </DialogHeader>
                {/* Table of members with role selects and delete buttons */}
                {/* Add member button */}
            </DialogContent>
        </Dialog>
    );
};
```

- [ ] **Step 4: Integrate into project settings menu**

Add a "Members" option to the existing project settings menu (`SettingsMenu.tsx`) that opens `ProjectMembersDialog`.

- [ ] **Step 5: Run client checks**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests pass

- [ ] **Step 6: Commit**

```bash
git commit -m "client - Add project member management dialog with role assignment"
```

---

## Task 24: Client-Side — Permission-Based Button Visibility

Use the `useHasProjectScope` hook to conditionally show/hide UI elements based on the user's permissions.

**Files:**
- Modify: Various project page components

- [ ] **Step 1: Hide edit/delete buttons for users without WORKFLOW_EDIT**

In workflow editor components, wrap action buttons:

```typescript
const hasEditScope = useHasProjectScope(projectId, 'WORKFLOW_EDIT');

// In JSX:
{hasEditScope && (
    <Button onClick={handleEdit}>
        <PencilIcon className="size-4" />
    </Button>
)}
```

- [ ] **Step 2: Hide deployment controls for users without DEPLOYMENT_PUSH**

```typescript
const hasDeployScope = useHasProjectScope(projectId, 'DEPLOYMENT_PUSH');

{hasDeployScope && <PublishButton />}
```

- [ ] **Step 3: Show "Members" menu only for PROJECT_MANAGE_MEMBERS**

```typescript
const canManageMembers = useHasProjectScope(projectId, 'PROJECT_MANAGE_MEMBERS');

{canManageMembers && (
    <DropdownMenuItem onClick={() => setMembersDialogOpen(true)}>
        <UsersIcon className="mr-2 size-4" />
        Members
    </DropdownMenuItem>
)}
```

- [ ] **Step 4: Handle read-only mode gracefully**

For VIEWER users, show components in read-only mode rather than hiding everything. Disable form inputs and hide action buttons, but still show data.

- [ ] **Step 5: Run client checks and commit**

Run: `cd client && npm run check`

```bash
git commit -m "client - Add permission-based visibility for project UI actions"
```

---

## Task 25: Data Migration for Existing Users

Backfill `project_member` rows for existing projects so current users retain access after the permission system is activated.

**Files:**
- Create: Liquibase migration with SQL to populate project_member from existing workspace_user relationships

- [ ] **Step 1: Create data migration**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="202604061200050" author="Ivica Cardic">
        <!-- Backfill: for every project, add all workspace members as EDITOR project members.
             The project creator (project.created_by matches user.login) gets ADMIN role. -->

        <!-- Step 1: Add workspace members as EDITOR to all projects in their workspace -->
        <sql>
            INSERT INTO project_member (project_id, user_id, project_role, created_date, created_by,
                                        last_modified_date, last_modified_by, version)
            SELECT p.id, wu.user_id, 'EDITOR', NOW(), 'system', NOW(), 'system', 0
            FROM project p
            JOIN workspace_user wu ON wu.workspace_id = p.workspace_id
            WHERE NOT EXISTS (
                SELECT 1 FROM project_member pm
                WHERE pm.project_id = p.id AND pm.user_id = wu.user_id
            );
        </sql>

        <!-- Step 2: Promote project creators to ADMIN -->
        <sql>
            UPDATE project_member pm
            SET project_role = 'ADMIN'
            FROM project p
            JOIN "user" u ON u.login = p.created_by
            WHERE pm.project_id = p.id AND pm.user_id = u.id;
        </sql>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Test migration on dev database**

Run: Start server and verify migration applies cleanly. Check `project_member` table has expected rows.

- [ ] **Step 3: Commit**

```bash
git commit -m "Add data migration to backfill project_member from existing workspace memberships"
```

---

## Updated Dependency Graph

```
Phase 1: Core RBAC (Tasks 1-15 — already in plan)
    Task 1 (Enums) ──────────┐
                              ├── Task 3 (WS role) ── Task 6 (WS service) ── Task 13 (WS creator)
    Task 2 (Migrations) ─────┤
                              ├── Task 4 (PM entity) ── Task 5 (PM service) ── Task 11 (PM test) ── Task 15 (Cache evict)
                              └── Task 7 (PermService) ── Task 8 (PS tests) ── Task 9 (@PreAuth ProjectSvc)
                                                                                   ├── Task 10 (WS facade refactor)
                                                                                   ├── Task 12 (Auto-assign creator)
                                                                                   └── Task 14 (GraphQL API)

Phase 2: Custom Roles EE (Tasks 16-18, depends on Phase 1)
    Task 16 (CustomRole domain) ── Task 17 (CustomRole service) ── Task 18 (CustomRole GraphQL)

Phase 3: Audit (Task 19, depends on Phase 1)
    Task 19 (PermissionAuditAspect)

Phase 4: Full Rollout (Task 20, depends on Phase 1)
    Task 20 (All services @PreAuthorize)

Phase 5: EE Remote Clients (Task 21, depends on Tasks 5, 7)
    Task 21 (Remote client stubs)

Phase 6: Client UI (Tasks 22-24, depends on Tasks 14, 20)
    Task 22 (Permission store) ── Task 23 (Members dialog) ── Task 24 (Button visibility)

Phase 7: Data Migration (Task 25, run LAST after all code is deployed)
    Task 25 (Backfill project_member)
```

Phases 2, 3, 4, 5 can run in parallel after Phase 1. Phase 6 depends on Phase 4. Phase 7 runs last.
