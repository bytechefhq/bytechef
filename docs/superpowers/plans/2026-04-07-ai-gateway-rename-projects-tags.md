# AI Gateway: Rename, Projects & Dynamic Tags — Implementation Plan

> **Status (2026-04-13):** Rename + Projects shipped. Tags entity shipped as `AiGatewayTag` (new table + domain + service + GraphQL CRUD) — see `2026-04-12-phase8-gap-remediation.md` §E5. The migration of existing `AiObservabilityTraceTag` + routing-policy tag references from the platform `Tag` entity to `AiGatewayTag` is still open and tracked in phase 8 §E5.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename `AiLlmGateway` → `AiGateway` across the full stack, add a Projects entity for per-project setting overrides, and replace hardcoded tags with DB-stored tag definitions.

**Architecture:** The rename is a mechanical bulk find-and-replace across ~148 files plus directory renames. Projects and Tags are new Spring Data JDBC entities with GraphQL CRUD, following existing patterns (e.g., `AiGatewayBudget`, `WorkspaceAiGatewayProvider`). The Liquibase init migration is rewritten in place (pre-production).

**Tech Stack:** Java 25 / Spring Boot 4 / Spring Data JDBC / GraphQL (Spring for GraphQL) / Liquibase / React 19 / TypeScript / Vite / TanStack Query

**Spec:** `docs/superpowers/specs/2026-04-07-ai-gateway-rename-projects-tags-design.md`

---

## Task 1: Rename Gradle Module Directories

Rename the physical directories on disk. This must happen before any content changes so that Git tracks it as a rename.

**Directories to rename:**

| Before | After |
|---|---|
| `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/` | `server/ee/libs/automation/automation-ai/automation-ai-gateway/` |
| `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api/` | (handled by parent rename) |
| `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql/` | (handled by parent rename) |
| `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-public-rest/` | (handled by parent rename) |
| `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-remote-client/` | (handled by parent rename) |
| `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service/` | (handled by parent rename) |
| `client/src/pages/automation/ai-llm-gateway/` | `client/src/pages/automation/ai-gateway/` |
| `client/src/graphql/automation/ai-llm-gateway/` | `client/src/graphql/automation/ai-gateway/` |

Sub-module directories inside `automation-ai-gateway/` also need renaming:

| Before (inside parent) | After |
|---|---|
| `automation-ai-llm-gateway-api/` | `automation-ai-gateway-api/` |
| `automation-ai-llm-gateway-graphql/` | `automation-ai-gateway-graphql/` |
| `automation-ai-llm-gateway-public-rest/` | `automation-ai-gateway-public-rest/` |
| `automation-ai-llm-gateway-remote-client/` | `automation-ai-gateway-remote-client/` |
| `automation-ai-llm-gateway-service/` | `automation-ai-gateway-service/` |

And the Liquibase changelog directory:

| Before | After |
|---|---|
| `.../resources/config/liquibase/changelog/automation/ai_llm_gateway/` | `.../resources/config/liquibase/changelog/automation/ai_gateway/` |

- [x] **Step 1: Rename server module directories**

```bash
cd /Volumes/Data/bytechef/bytechef

# Rename sub-modules first (inside parent)
git mv server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-api \
       server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-gateway-api

git mv server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-graphql \
       server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-gateway-graphql

git mv server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-public-rest \
       server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-gateway-public-rest

git mv server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-remote-client \
       server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-gateway-remote-client

git mv server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-llm-gateway-service \
       server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/automation-ai-gateway-service

# Rename parent module
git mv server/ee/libs/automation/automation-ai/automation-ai-llm-gateway \
       server/ee/libs/automation/automation-ai/automation-ai-gateway
```

- [x] **Step 2: Rename Liquibase changelog directory**

```bash
git mv server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_llm_gateway \
       server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway
```

- [x] **Step 3: Rename client directories**

```bash
git mv client/src/pages/automation/ai-llm-gateway \
       client/src/pages/automation/ai-gateway

git mv client/src/graphql/automation/ai-llm-gateway \
       client/src/graphql/automation/ai-gateway
```

- [x] **Step 4: Commit directory renames**

```bash
git add -A
git commit -m "Rename AiLlmGateway directories to AiGateway"
```

---

## Task 2: Rename Java Package Directories

Rename the Java package from `llmgateway` to `gateway` inside every sub-module's `src/main/java` and `src/test/java` trees.

- [x] **Step 1: Rename package directories in all sub-modules**

```bash
cd /Volumes/Data/bytechef/bytechef

# For each sub-module, rename the package directory in both main and test
for module in api graphql public-rest remote-client service; do
  base="server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-${module}/src"

  # main/java
  if [ -d "${base}/main/java/com/bytechef/ee/automation/ai/llmgateway" ]; then
    git mv "${base}/main/java/com/bytechef/ee/automation/ai/llmgateway" \
           "${base}/main/java/com/bytechef/ee/automation/ai/gateway"
  fi

  # test/java
  if [ -d "${base}/test/java/com/bytechef/ee/automation/ai/llmgateway" ]; then
    git mv "${base}/test/java/com/bytechef/ee/automation/ai/llmgateway" \
           "${base}/test/java/com/bytechef/ee/automation/ai/gateway"
  fi
done
```

- [x] **Step 2: Commit package directory renames**

```bash
git add -A
git commit -m "Rename Java package llmgateway to gateway"
```

---

## Task 3: Bulk Content Rename — Server Java Files

Replace all occurrences of `AiLlmGateway`, `aiLlmGateway`, `llmgateway`, `llm-gateway`, `llm_gateway` in Java source files, config files, and build files.

- [x] **Step 1: Replace in all Java files under the gateway module**

```bash
cd /Volumes/Data/bytechef/bytechef

# PascalCase: AiLlmGateway -> AiGateway (class names, type references)
find server/ee/libs/automation/automation-ai/automation-ai-gateway -name "*.java" | xargs sed -i '' 's/AiLlmGateway/AiGateway/g'

# Package declaration: ai.llmgateway -> ai.gateway
find server/ee/libs/automation/automation-ai/automation-ai-gateway -name "*.java" | xargs sed -i '' 's/ai\.llmgateway/ai.gateway/g'

# Config property prefix: bytechef.ai.llm-gateway -> bytechef.ai.gateway
find server/ee/libs/automation/automation-ai/automation-ai-gateway -name "*.java" | xargs sed -i '' 's/bytechef\.ai\.llm-gateway/bytechef.ai.gateway/g'

# REST path: /api/llm-gateway/ -> /api/ai-gateway/
find server/ee/libs/automation/automation-ai/automation-ai-gateway -name "*.java" | xargs sed -i '' 's|/api/llm-gateway/|/api/ai-gateway/|g'
```

- [x] **Step 2: Rename Java files (class names)**

```bash
cd /Volumes/Data/bytechef/bytechef

find server/ee/libs/automation/automation-ai/automation-ai-gateway -name "AiLlmGateway*.java" | while read file; do
  newfile=$(echo "$file" | sed 's/AiLlmGateway/AiGateway/g')
  git mv "$file" "$newfile"
done
```

- [x] **Step 3: Replace in AutoConfiguration.imports**

```bash
sed -i '' 's/ai\.llmgateway/ai.gateway/g; s/AiLlmGateway/AiGateway/g' \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

- [x] **Step 4: Replace in OpenAPI spec**

```bash
sed -i '' 's/AiLlmGateway/AiGateway/g; s/aiLlmGateway/aiGateway/g; s|/api/llm-gateway/|/api/ai-gateway/|g; s/AI LLM Gateway/AI Gateway/g; s/ai-llm-gateway/ai-gateway/g' \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/openapi.yaml
```

- [x] **Step 5: Replace in build.gradle.kts files**

```bash
# All build.gradle.kts that reference the old module paths
sed -i '' 's/automation-ai-llm-gateway/automation-ai-gateway/g' \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/build.gradle.kts \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/build.gradle.kts \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/build.gradle.kts \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-remote-client/build.gradle.kts \
  server/apps/server-app/build.gradle.kts \
  server/ee/apps/llm-gateway-app/build.gradle.kts
```

- [x] **Step 6: Replace in settings.gradle.kts**

```bash
sed -i '' 's/automation-ai-llm-gateway/automation-ai-gateway/g' settings.gradle.kts
```

- [x] **Step 7: Replace in application config files**

```bash
# application.yml files
sed -i '' 's/llm-gateway/ai-gateway/g; s/llmGateway/aiGateway/g' \
  server/apps/server-app/src/main/resources/config/application.yml \
  server/apps/server-app/src/main/resources/config/application-bytechef.yml \
  server/apps/server-app/src/main/resources/config/application-local.yml

# EE config server files
sed -i '' 's/llm-gateway/ai-gateway/g; s/llmGateway/aiGateway/g' \
  server/ee/apps/config-server-app/src/main/resources/config/apps/llm-gateway-app.yml

# API gateway routes
sed -i '' 's/llm-gateway/ai-gateway/g' \
  server/ee/apps/config-server-app/src/main/resources/config/apps/apigateway-app.yml
```

- [x] **Step 8: Replace in Liquibase master.xml**

```bash
sed -i '' 's/ai_llm_gateway/ai_gateway/g' \
  server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml
```

- [x] **Step 9: Verify build compiles**

```bash
./gradlew clean compileJava 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [x] **Step 10: Commit server rename**

```bash
git add -A
git commit -m "Rename AiLlmGateway to AiGateway in server code"
```

---

## Task 4: Rename Liquibase Init Migration Tables

Rewrite the init migration XML to use `ai_gateway_*` table names instead of `ai_llm_gateway_*`.

- [x] **Step 1: Rename table references in init migration**

```bash
sed -i '' 's/ai_llm_gateway/ai_gateway/g' \
  server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000001_ai_llm_gateway_init.xml
```

- [x] **Step 2: Rename the migration file itself**

```bash
git mv server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000001_ai_llm_gateway_init.xml \
       server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000001_ai_gateway_init.xml
```

- [x] **Step 3: Update Spring Data JDBC @Table annotations**

All domain classes use `@Table("ai_llm_gateway_*")`. Update them:

```bash
find server/ee/libs/automation/automation-ai/automation-ai-gateway -name "*.java" | xargs sed -i '' 's/@Table("ai_llm_gateway_/@Table("ai_gateway_/g; s/@Table("workspace_ai_llm_gateway_/@Table("workspace_ai_gateway_/g'
```

Also check for any `@Column` or native query references:

```bash
grep -rn "ai_llm_gateway\|workspace_ai_llm_gateway" server/ee/libs/automation/automation-ai/automation-ai-gateway --include="*.java"
```

Expected: No matches remaining.

- [x] **Step 4: Commit**

```bash
git add -A
git commit -m "Rename database tables from ai_llm_gateway to ai_gateway"
```

---

## Task 5: Rename GraphQL Schemas

Rename GraphQL schema files and update their contents.

- [x] **Step 1: Rename .graphqls files**

```bash
cd server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql

for file in ai-llm-gateway-*.graphqls; do
  newfile=$(echo "$file" | sed 's/ai-llm-gateway/ai-gateway/g')
  git mv "$file" "$newfile"
done

for file in workspace-ai-llm-gateway-*.graphqls; do
  newfile=$(echo "$file" | sed 's/ai-llm-gateway/ai-gateway/g')
  git mv "$file" "$newfile"
done
```

- [x] **Step 2: Replace content in all .graphqls files**

```bash
cd /Volumes/Data/bytechef/bytechef

find server/ee/libs/automation/automation-ai/automation-ai-gateway -name "*.graphqls" | xargs sed -i '' \
  's/AiLlmGateway/AiGateway/g; s/aiLlmGateway/aiGateway/g; s/workspaceAiLlmGateway/workspaceAiGateway/g'
```

- [x] **Step 3: Commit**

```bash
git add -A
git commit -m "Rename GraphQL schemas from AiLlmGateway to AiGateway"
```

---

## Task 6: Rename Client Code

Rename all client-side TypeScript/React files, components, types, and imports.

- [x] **Step 1: Replace content in all client TS/TSX files**

```bash
cd /Volumes/Data/bytechef/bytechef

# Replace in client gateway component files
find client/src/pages/automation/ai-gateway -name "*.tsx" -o -name "*.ts" | xargs sed -i '' \
  's/AiLlmGateway/AiGateway/g; s/aiLlmGateway/aiGateway/g; s/ai-llm-gateway/ai-gateway/g'

# Replace in routes.tsx
sed -i '' 's/AiLlmGateway/AiGateway/g; s/ai-llm-gateway/ai-gateway/g' client/src/routes.tsx

# Replace in App.tsx (if any references)
sed -i '' 's/AiLlmGateway/AiGateway/g; s/ai-llm-gateway/ai-gateway/g' client/src/App.tsx

# Replace in codegen.ts
sed -i '' 's/ai-llm-gateway/ai-gateway/g' client/codegen.ts
```

- [x] **Step 2: Rename client component files**

```bash
cd client/src/pages/automation/ai-gateway

# Rename all AiLlmGateway*.tsx files
find . -name "AiLlmGateway*.tsx" | while read file; do
  newfile=$(echo "$file" | sed 's/AiLlmGateway/AiGateway/g')
  git mv "$file" "$newfile"
done
```

- [x] **Step 3: Rename client GraphQL operation files**

```bash
cd /Volumes/Data/bytechef/bytechef/client/src/graphql/automation/ai-gateway

for file in aiLlmGateway*.graphql; do
  newfile=$(echo "$file" | sed 's/aiLlmGateway/aiGateway/g')
  git mv "$file" "$newfile"
done

for file in workspaceAiLlmGateway*.graphql; do
  newfile=$(echo "$file" | sed 's/AiLlmGateway/AiGateway/g')
  git mv "$file" "$newfile"
done
```

- [x] **Step 4: Replace content in GraphQL operation files**

```bash
cd /Volumes/Data/bytechef/bytechef

find client/src/graphql/automation/ai-gateway -name "*.graphql" | xargs sed -i '' \
  's/AiLlmGateway/AiGateway/g; s/aiLlmGateway/aiGateway/g; s/workspaceAiLlmGateway/workspaceAiGateway/g'
```

- [x] **Step 5: Regenerate GraphQL types**

```bash
cd client
npx graphql-codegen
```

- [x] **Step 6: Update remaining references in generated graphql.ts**

The generated `client/src/shared/middleware/graphql.ts` should now have `AiGateway*` types. Verify no `AiLlmGateway` references remain:

```bash
grep -rn "AiLlmGateway\|aiLlmGateway\|ai-llm-gateway\|ai_llm_gateway" client/src/
```

Expected: No matches.

- [x] **Step 7: Commit**

```bash
git add -A
git commit -m "Rename AiLlmGateway to AiGateway in client code"
```

---

## Task 7: Verify Full Rename

- [x] **Step 1: Search for any remaining old references**

```bash
cd /Volumes/Data/bytechef/bytechef

# Check server
grep -rn "AiLlmGateway\|aiLlmGateway\|ai\.llmgateway\|llmgateway\|ai_llm_gateway\|ai-llm-gateway\|llm-gateway" \
  server/ settings.gradle.kts --include="*.java" --include="*.xml" --include="*.yml" --include="*.yaml" --include="*.graphqls" --include="*.kts" --include="*.imports" \
  | grep -v "build/" | grep -v ".gradle/"

# Check client
grep -rn "AiLlmGateway\|aiLlmGateway\|ai-llm-gateway" client/src/ client/codegen.ts \
  --include="*.ts" --include="*.tsx" --include="*.graphql"
```

Expected: No matches (except possibly docs/ files which are informational).

- [x] **Step 2: Compile server**

```bash
./gradlew clean compileJava
```

Expected: BUILD SUCCESSFUL

- [x] **Step 3: Check client typecheck**

```bash
cd client && npm run typecheck
```

Expected: No errors.

- [x] **Step 4: Fix any remaining issues found in steps 1-3**

Address any straggler references that the bulk sed missed.

- [x] **Step 5: Commit fixes if any**

```bash
git add -A
git commit -m "Fix remaining AiLlmGateway references after rename"
```

---

## Task 8: Add AiGatewayProject Domain Entity

Create the project entity, repository, and service in the API module.

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiGatewayProject.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiGatewayProjectRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayProjectService.java`

- [x] **Step 1: Create AiGatewayProject domain class**

Follow the pattern of `AiGatewayBudget.java` (workspace-scoped entity with audit fields + version).

```java
package com.bytechef.ee.automation.ai.gateway.domain;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_gateway_project")
public class AiGatewayProject {

    @Id
    private Long id;

    @Column("workspace_id")
    private long workspaceId;

    private String name;

    private String slug;

    @Nullable
    private String description;

    @Nullable
    @Column("routing_policy_id")
    private Long routingPolicyId;

    @Nullable
    @Column("compression_enabled")
    private Boolean compressionEnabled;

    @Nullable
    @Column("retry_max_attempts")
    private Integer retryMaxAttempts;

    @Nullable
    @Column("timeout_seconds")
    private Integer timeoutSeconds;

    @Nullable
    @Column("caching_enabled")
    private Boolean cachingEnabled;

    @Nullable
    @Column("cache_ttl_minutes")
    private Integer cacheTtlMinutes;

    @Nullable
    @Column("log_retention_days")
    private Integer logRetentionDays;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Version
    private long version;

    // Getters and setters following the pattern of AiGatewayBudget
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(long workspaceId) { this.workspaceId = workspaceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    @Nullable
    public String getDescription() { return description; }
    public void setDescription(@Nullable String description) { this.description = description; }

    @Nullable
    public Long getRoutingPolicyId() { return routingPolicyId; }
    public void setRoutingPolicyId(@Nullable Long routingPolicyId) { this.routingPolicyId = routingPolicyId; }

    @Nullable
    public Boolean getCompressionEnabled() { return compressionEnabled; }
    public void setCompressionEnabled(@Nullable Boolean compressionEnabled) { this.compressionEnabled = compressionEnabled; }

    @Nullable
    public Integer getRetryMaxAttempts() { return retryMaxAttempts; }
    public void setRetryMaxAttempts(@Nullable Integer retryMaxAttempts) { this.retryMaxAttempts = retryMaxAttempts; }

    @Nullable
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(@Nullable Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    @Nullable
    public Boolean getCachingEnabled() { return cachingEnabled; }
    public void setCachingEnabled(@Nullable Boolean cachingEnabled) { this.cachingEnabled = cachingEnabled; }

    @Nullable
    public Integer getCacheTtlMinutes() { return cacheTtlMinutes; }
    public void setCacheTtlMinutes(@Nullable Integer cacheTtlMinutes) { this.cacheTtlMinutes = cacheTtlMinutes; }

    @Nullable
    public Integer getLogRetentionDays() { return logRetentionDays; }
    public void setLogRetentionDays(@Nullable Integer logRetentionDays) { this.logRetentionDays = logRetentionDays; }

    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedDate() { return createdDate; }
    public String getLastModifiedBy() { return lastModifiedBy; }
    public Instant getLastModifiedDate() { return lastModifiedDate; }
    public long getVersion() { return version; }
}
```

- [x] **Step 2: Create AiGatewayProjectRepository**

```java
package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 */
@Repository
public interface AiGatewayProjectRepository extends CrudRepository<AiGatewayProject, Long> {

    List<AiGatewayProject> findByWorkspaceId(long workspaceId);

    Optional<AiGatewayProject> findByWorkspaceIdAndSlug(long workspaceId, String slug);
}
```

- [x] **Step 3: Create AiGatewayProjectService interface**

```java
package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProject;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiGatewayProjectService {

    AiGatewayProject createProject(AiGatewayProject project);

    void deleteProject(long id);

    Optional<AiGatewayProject> fetchProject(long id);

    List<AiGatewayProject> getProjectsByWorkspaceId(long workspaceId);

    Optional<AiGatewayProject> fetchProjectByWorkspaceIdAndSlug(long workspaceId, String slug);

    AiGatewayProject updateProject(AiGatewayProject project);
}
```

- [x] **Step 4: Commit**

```bash
git add -A
git commit -m "Add AiGatewayProject domain entity, repository, and service interface"
```

---

## Task 9: Add AiGatewayTag Domain Entity

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/domain/AiGatewayTag.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/repository/AiGatewayTagRepository.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayTagService.java`

- [x] **Step 1: Create AiGatewayTag domain class**

```java
package com.bytechef.ee.automation.ai.gateway.domain;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_gateway_tag")
public class AiGatewayTag {

    @Id
    private Long id;

    @Column("workspace_id")
    private long workspaceId;

    private String key;

    @Nullable
    private String description;

    private boolean required;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Version
    private long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(long workspaceId) { this.workspaceId = workspaceId; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    @Nullable
    public String getDescription() { return description; }
    public void setDescription(@Nullable String description) { this.description = description; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedDate() { return createdDate; }
    public String getLastModifiedBy() { return lastModifiedBy; }
    public Instant getLastModifiedDate() { return lastModifiedDate; }
    public long getVersion() { return version; }
}
```

- [x] **Step 2: Create AiGatewayTagRepository**

```java
package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayTag;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 */
@Repository
public interface AiGatewayTagRepository extends CrudRepository<AiGatewayTag, Long> {

    List<AiGatewayTag> findByWorkspaceId(long workspaceId);
}
```

- [x] **Step 3: Create AiGatewayTagService interface**

```java
package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayTag;
import java.util.List;

/**
 * @version ee
 */
public interface AiGatewayTagService {

    AiGatewayTag createTag(AiGatewayTag tag);

    void deleteTag(long id);

    List<AiGatewayTag> getTagsByWorkspaceId(long workspaceId);

    AiGatewayTag updateTag(AiGatewayTag tag);
}
```

- [x] **Step 4: Commit**

```bash
git add -A
git commit -m "Add AiGatewayTag domain entity, repository, and service interface"
```

---

## Task 10: Add Service Implementations

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayProjectServiceImpl.java`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/service/AiGatewayTagServiceImpl.java`

- [x] **Step 1: Create AiGatewayProjectServiceImpl**

Follow the pattern of existing service impls (e.g., `AiGatewayBudgetServiceImpl`).

```java
package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProject;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayProjectRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @version ee
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayProjectServiceImpl implements AiGatewayProjectService {

    private final AiGatewayProjectRepository aiGatewayProjectRepository;

    public AiGatewayProjectServiceImpl(AiGatewayProjectRepository aiGatewayProjectRepository) {
        this.aiGatewayProjectRepository = aiGatewayProjectRepository;
    }

    @Override
    public AiGatewayProject createProject(AiGatewayProject project) {
        return aiGatewayProjectRepository.save(project);
    }

    @Override
    public void deleteProject(long id) {
        aiGatewayProjectRepository.deleteById(id);
    }

    @Override
    public Optional<AiGatewayProject> fetchProject(long id) {
        return aiGatewayProjectRepository.findById(id);
    }

    @Override
    public List<AiGatewayProject> getProjectsByWorkspaceId(long workspaceId) {
        return aiGatewayProjectRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    public Optional<AiGatewayProject> fetchProjectByWorkspaceIdAndSlug(long workspaceId, String slug) {
        return aiGatewayProjectRepository.findByWorkspaceIdAndSlug(workspaceId, slug);
    }

    @Override
    public AiGatewayProject updateProject(AiGatewayProject project) {
        return aiGatewayProjectRepository.save(project);
    }
}
```

- [x] **Step 2: Create AiGatewayTagServiceImpl**

```java
package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayTag;
import com.bytechef.ee.automation.ai.gateway.repository.AiGatewayTagRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @version ee
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayTagServiceImpl implements AiGatewayTagService {

    private final AiGatewayTagRepository aiGatewayTagRepository;

    public AiGatewayTagServiceImpl(AiGatewayTagRepository aiGatewayTagRepository) {
        this.aiGatewayTagRepository = aiGatewayTagRepository;
    }

    @Override
    public AiGatewayTag createTag(AiGatewayTag tag) {
        return aiGatewayTagRepository.save(tag);
    }

    @Override
    public void deleteTag(long id) {
        aiGatewayTagRepository.deleteById(id);
    }

    @Override
    public List<AiGatewayTag> getTagsByWorkspaceId(long workspaceId) {
        return aiGatewayTagRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    public AiGatewayTag updateTag(AiGatewayTag tag) {
        return aiGatewayTagRepository.save(tag);
    }
}
```

- [x] **Step 3: Commit**

```bash
git add -A
git commit -m "Add AiGatewayProject and AiGatewayTag service implementations"
```

---

## Task 11: Update Liquibase Migration — Add New Tables and Columns

Modify the init migration to add `ai_gateway_project`, `ai_gateway_tag` tables and add `project_id` columns to existing tables.

**File:** `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000001_ai_gateway_init.xml`

- [x] **Step 1: Add ai_gateway_project table to init migration**

Add this changeset after the existing tables (before the closing `</databaseChangeLog>` tag):

```xml
<changeSet id="00000000000001-10" author="bytechef">
    <createTable tableName="ai_gateway_project">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints nullable="false" primaryKey="true" primaryKeyName="pk_ai_gateway_project"/>
        </column>
        <column name="workspace_id" type="BIGINT">
            <constraints nullable="false"/>
        </column>
        <column name="name" type="VARCHAR(256)">
            <constraints nullable="false"/>
        </column>
        <column name="slug" type="VARCHAR(256)">
            <constraints nullable="false"/>
        </column>
        <column name="description" type="VARCHAR(1024)"/>
        <column name="routing_policy_id" type="BIGINT"/>
        <column name="compression_enabled" type="BOOLEAN"/>
        <column name="retry_max_attempts" type="INTEGER"/>
        <column name="timeout_seconds" type="INTEGER"/>
        <column name="caching_enabled" type="BOOLEAN"/>
        <column name="cache_ttl_minutes" type="INTEGER"/>
        <column name="log_retention_days" type="INTEGER"/>
        <column name="created_by" type="VARCHAR(256)">
            <constraints nullable="false"/>
        </column>
        <column name="created_date" type="TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="last_modified_by" type="VARCHAR(256)">
            <constraints nullable="false"/>
        </column>
        <column name="last_modified_date" type="TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="version" type="BIGINT"/>
    </createTable>

    <addUniqueConstraint tableName="ai_gateway_project" columnNames="workspace_id, slug"
                         constraintName="uq_ai_gateway_project_workspace_slug"/>

    <addForeignKeyConstraint baseTableName="ai_gateway_project" baseColumnNames="routing_policy_id"
                             referencedTableName="ai_gateway_routing_policy" referencedColumnNames="id"
                             constraintName="fk_ai_gateway_project_routing_policy"/>

    <createIndex tableName="ai_gateway_project" indexName="ix_ai_gateway_project_workspace">
        <column name="workspace_id"/>
    </createIndex>
</changeSet>
```

- [x] **Step 2: Add ai_gateway_tag table**

```xml
<changeSet id="00000000000001-11" author="bytechef">
    <createTable tableName="ai_gateway_tag">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints nullable="false" primaryKey="true" primaryKeyName="pk_ai_gateway_tag"/>
        </column>
        <column name="workspace_id" type="BIGINT">
            <constraints nullable="false"/>
        </column>
        <column name="key" type="VARCHAR(256)">
            <constraints nullable="false"/>
        </column>
        <column name="description" type="VARCHAR(1024)"/>
        <column name="required" type="BOOLEAN" defaultValueBoolean="false">
            <constraints nullable="false"/>
        </column>
        <column name="created_by" type="VARCHAR(256)">
            <constraints nullable="false"/>
        </column>
        <column name="created_date" type="TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="last_modified_by" type="VARCHAR(256)">
            <constraints nullable="false"/>
        </column>
        <column name="last_modified_date" type="TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="version" type="BIGINT"/>
    </createTable>

    <addUniqueConstraint tableName="ai_gateway_tag" columnNames="workspace_id, key"
                         constraintName="uq_ai_gateway_tag_workspace_key"/>

    <createIndex tableName="ai_gateway_tag" indexName="ix_ai_gateway_tag_workspace">
        <column name="workspace_id"/>
    </createIndex>
</changeSet>
```

- [x] **Step 3: Add project_id to budget, request_log, and spend_summary tables**

Add `project_id` column to the existing table definitions in the init migration:

In the `ai_gateway_budget` createTable (changeset -3), add after `workspace_id`:
```xml
<column name="project_id" type="BIGINT"/>
```

Update the unique constraint on budget from `(workspace_id)` to `(workspace_id, project_id)`.

In `ai_gateway_request_log` createTable (changeset -6), add:
```xml
<column name="project_id" type="BIGINT"/>
```

Add index:
```xml
<createIndex tableName="ai_gateway_request_log" indexName="ix_ai_gateway_request_log_project">
    <column name="project_id"/>
</createIndex>
```

In `ai_gateway_spend_summary` createTable (changeset -7), add:
```xml
<column name="project_id" type="BIGINT"/>
```

- [x] **Step 4: Update domain classes for project_id**

Add `@Nullable @Column("project_id") private Long projectId;` with getter/setter to:
- `AiGatewayBudget.java`
- `AiGatewayRequestLog.java`
- `AiGatewaySpendSummary.java`

- [x] **Step 5: Commit**

```bash
git add -A
git commit -m "Add Liquibase tables for AiGatewayProject and AiGatewayTag, add project_id columns"
```

---

## Task 12: Add GraphQL Schema and Controller for Projects

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-gateway-project.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiGatewayProjectGraphQlController.java`

- [x] **Step 1: Create GraphQL schema**

```graphql
type AiGatewayProject {
    id: ID!
    workspaceId: ID!
    name: String!
    slug: String!
    description: String
    routingPolicyId: ID
    compressionEnabled: Boolean
    retryMaxAttempts: Int
    timeoutSeconds: Int
    cachingEnabled: Boolean
    cacheTtlMinutes: Int
    logRetentionDays: Int
    createdBy: String
    createdDate: DateTime
    lastModifiedBy: String
    lastModifiedDate: DateTime
    version: Long
}

input CreateAiGatewayProjectInput {
    workspaceId: ID!
    name: String!
    slug: String!
    description: String
    routingPolicyId: ID
    compressionEnabled: Boolean
    retryMaxAttempts: Int
    timeoutSeconds: Int
    cachingEnabled: Boolean
    cacheTtlMinutes: Int
    logRetentionDays: Int
}

input UpdateAiGatewayProjectInput {
    name: String
    slug: String
    description: String
    routingPolicyId: ID
    compressionEnabled: Boolean
    retryMaxAttempts: Int
    timeoutSeconds: Int
    cachingEnabled: Boolean
    cacheTtlMinutes: Int
    logRetentionDays: Int
}

extend type Query {
    aiGatewayProjects(workspaceId: ID!): [AiGatewayProject!]!
    aiGatewayProject(id: ID!): AiGatewayProject!
}

extend type Mutation {
    createAiGatewayProject(input: CreateAiGatewayProjectInput!): AiGatewayProject!
    updateAiGatewayProject(id: ID!, input: UpdateAiGatewayProjectInput!): AiGatewayProject!
    deleteAiGatewayProject(id: ID!): Boolean!
}
```

- [x] **Step 2: Create GraphQL controller**

Follow the pattern of `AiGatewayBudgetGraphQlController`. Map inputs to domain objects, delegate to service.

- [x] **Step 3: Commit**

```bash
git add -A
git commit -m "Add GraphQL schema and controller for AiGatewayProject"
```

---

## Task 13: Add GraphQL Schema and Controller for Tags

**Files:**
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/ai-gateway-tag.graphqls`
- Create: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiGatewayTagGraphQlController.java`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-public-rest/src/main/java/com/bytechef/ee/automation/ai/gateway/public_/web/rest/AiGatewayTagApiController.java`

- [x] **Step 1: Create GraphQL schema for tags**

```graphql
type AiGatewayTag {
    id: ID!
    workspaceId: ID!
    key: String!
    description: String
    required: Boolean!
    createdBy: String
    createdDate: DateTime
    lastModifiedBy: String
    lastModifiedDate: DateTime
    version: Long
}

input CreateAiGatewayTagInput {
    workspaceId: ID!
    key: String!
    description: String
    required: Boolean
}

input UpdateAiGatewayTagInput {
    key: String
    description: String
    required: Boolean
}

extend type Query {
    aiGatewayTags(workspaceId: ID!): [AiGatewayTag!]!
}

extend type Mutation {
    createAiGatewayTag(input: CreateAiGatewayTagInput!): AiGatewayTag!
    updateAiGatewayTag(id: ID!, input: UpdateAiGatewayTagInput!): AiGatewayTag!
    deleteAiGatewayTag(id: ID!): Boolean!
}
```

- [x] **Step 2: Rewrite the existing AiGatewayTagGraphQlController**

Replace the hardcoded tag list with a query to `AiGatewayTagService.getTagsByWorkspaceId()`. The GraphQL controller for tags should already exist from the rename — update it to use the new service.

- [x] **Step 3: Update the REST AiGatewayTagApiController** — **WONTFIX** (2026-04-13 audit): The REST endpoint is `AiGatewayRoutingPolicyTagApiController#listTags(routingPolicyId)` — it returns tags attached to a routing policy via `routingPolicy.getTagIds()`, which reference platform `Tag` rows (`AiGatewayRoutingPolicyTag.tagId` = `AggregateReference<Tag, Long>`). Rewiring it to `AiGatewayTagService` would change the endpoint's meaning (workspace tags ≠ routing-policy tags) and break the existing contract. Workspace `AiGatewayTag` listing is already exposed via GraphQL (`aiGatewayTags(workspaceId)`); no REST consumer needs it.

- [x] **Step 4: Commit** — covered by tag UI commit.

```bash
git add -A
git commit -m "Add GraphQL schema and controller for AiGatewayTag, replace hardcoded tags"
```

---

## Task 14: Update AiGatewayFacade for Project Resolution

Modify the main facade to resolve project settings from the `X-Project-Id` header.

**File:** `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayFacade.java`

- [x] **Step 1: Add AiGatewayProjectService dependency**

Inject `AiGatewayProjectService` into `AiGatewayFacade`.

- [x] **Step 2: Add project resolution method**

```java
private AiGatewayProject resolveProject(long workspaceId, @Nullable String projectSlug) {
    if (projectSlug == null || projectSlug.isBlank()) {
        return null;
    }

    return aiGatewayProjectService.fetchProjectByWorkspaceIdAndSlug(workspaceId, projectSlug)
        .orElse(null);
}
```

- [x] **Step 3: Add setting resolution helpers**

```java
private boolean resolveCompressionEnabled(@Nullable AiGatewayProject project) {
    if (project != null && project.getCompressionEnabled() != null) {
        return project.getCompressionEnabled();
    }

    return true; // system default
}

private int resolveRetryMaxAttempts(@Nullable AiGatewayProject project) {
    if (project != null && project.getRetryMaxAttempts() != null) {
        return project.getRetryMaxAttempts();
    }

    return 2; // system default
}

// Same pattern for timeoutSeconds, cachingEnabled, cacheTtlMinutes
```

- [x] **Step 4: Update chat completion and embedding methods**

Read `X-Project-Id` from the request, resolve the project, use resolved settings for compression/retry/timeout/caching, and pass `projectId` to the request log.

- [x] **Step 5: Commit**

```bash
git add -A
git commit -m "Add project resolution to AiGatewayFacade for per-project settings"
```

---

## Task 15: Add Client GraphQL Operations for Projects and Tags

**Files:**
- Create: `client/src/graphql/automation/ai-gateway/aiGatewayProjects.graphql`
- Create: `client/src/graphql/automation/ai-gateway/aiGatewayTags.graphql`

- [x] **Step 1: Create project GraphQL operations**

```graphql
query AiGatewayProjects($workspaceId: ID!) {
    aiGatewayProjects(workspaceId: $workspaceId) {
        id
        workspaceId
        name
        slug
        description
        routingPolicyId
        compressionEnabled
        retryMaxAttempts
        timeoutSeconds
        cachingEnabled
        cacheTtlMinutes
        logRetentionDays
        createdDate
        lastModifiedDate
    }
}

mutation CreateAiGatewayProject($input: CreateAiGatewayProjectInput!) {
    createAiGatewayProject(input: $input) {
        id
        name
        slug
    }
}

mutation UpdateAiGatewayProject($id: ID!, $input: UpdateAiGatewayProjectInput!) {
    updateAiGatewayProject(id: $id, input: $input) {
        id
        name
        slug
    }
}

mutation DeleteAiGatewayProject($id: ID!) {
    deleteAiGatewayProject(id: $id)
}
```

- [x] **Step 2: Create tag GraphQL operations** — shipped at `client/src/graphql/automation/ai-gateway/aiGatewayTags.graphql` (2026-04-13). Actual shape follows the landed GraphQL schema (`color`/`name`/`workspaceId`), not the speculative `key`/`required` fields sketched below.

```graphql
query AiGatewayTags($workspaceId: ID!) {
    aiGatewayTags(workspaceId: $workspaceId) {
        id
        workspaceId
        key
        description
        required
        createdDate
        lastModifiedDate
    }
}

mutation CreateAiGatewayTag($input: CreateAiGatewayTagInput!) {
    createAiGatewayTag(input: $input) {
        id
        key
    }
}

mutation UpdateAiGatewayTag($id: ID!, $input: UpdateAiGatewayTagInput!) {
    updateAiGatewayTag(id: $id, input: $input) {
        id
        key
    }
}

mutation DeleteAiGatewayTag($id: ID!) {
    deleteAiGatewayTag(id: $id)
}
```

- [x] **Step 3: Regenerate GraphQL types**

```bash
cd client && npx graphql-codegen
```

- [x] **Step 4: Commit** — pending user-triggered commit.

```bash
git add -A
git commit -m "Add client GraphQL operations for AiGatewayProject and AiGatewayTag"
```

---

## Task 16: Add Client UI for Projects

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/projects/AiGatewayProjects.tsx`
- Create: `client/src/pages/automation/ai-gateway/components/projects/AiGatewayProjectDialog.tsx`
- Modify: `client/src/pages/automation/ai-gateway/AiGateway.tsx` — add Projects tab

- [x] **Step 1: Create AiGatewayProjects list component**

Follow the pattern of `AiGatewayProviders.tsx` — table with name, slug, description, routing policy, edit/delete actions.

- [x] **Step 2: Create AiGatewayProjectDialog**

Form with: name, slug, description, routing policy select, compression toggle, retry/timeout/caching/retention number inputs. All override fields optional.

- [x] **Step 3: Add Projects tab to AiGateway.tsx**

Add `'projects'` to `AiGatewayPageType` union and render `<AiGatewayProjects />` when active. Add navigation button.

- [x] **Step 4: Commit**

```bash
git add -A
git commit -m "Add Projects UI to AI Gateway page"
```

---

## Task 17: Add Client UI for Tags

**Files:**
- Create: `client/src/pages/automation/ai-gateway/components/tags/AiGatewayTags.tsx`
- Create: `client/src/pages/automation/ai-gateway/components/tags/AiGatewayTagDialog.tsx`
- Modify: `client/src/pages/automation/ai-gateway/AiGateway.tsx` — add Tags tab

- [x] **Step 1: Create AiGatewayTags list component** — shipped at `client/src/pages/automation/ai-gateway/components/tags/AiGatewayTags.tsx`. Columns follow the landed schema (name + color swatch), not the speculative key/description/required sketched here.

- [x] **Step 2: Create AiGatewayTagDialog** — shipped at `components/tags/AiGatewayTagDialog.tsx`. Fields: name + color (color-picker + hex input).

- [x] **Step 3: Add Tags tab to AiGateway.tsx** — wired; `'tags'` added to `AiGatewayPageType` union, nav item between Projects and Routing Policies.

- [x] **Step 4: Commit** — pending user-triggered commit.

```bash
git add -A
git commit -m "Add Tags UI to AI Gateway page"
```

---

## Task 18: Final Verification

- [x] **Step 1: Full server build**

```bash
./gradlew clean compileJava
```

Expected: BUILD SUCCESSFUL

- [x] **Step 2: Client checks**

```bash
cd client
npm run check
```

Expected: No errors.

- [x] **Step 3: Run server tests**

```bash
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test
```

- [x] **Step 4: Search for any remaining old references**

```bash
grep -rn "AiLlmGateway\|aiLlmGateway\|ai_llm_gateway\|ai-llm-gateway\|llmgateway\|llm-gateway" \
  server/ client/src/ settings.gradle.kts client/codegen.ts \
  --include="*.java" --include="*.xml" --include="*.yml" --include="*.yaml" \
  --include="*.graphqls" --include="*.graphql" --include="*.kts" --include="*.imports" \
  --include="*.ts" --include="*.tsx" \
  | grep -v "build/" | grep -v ".gradle/" | grep -v "node_modules/" | grep -v "docs/"
```

Expected: No matches.

- [x] **Step 5: Final commit if any fixes needed**

```bash
git add -A
git commit -m "Final cleanup for AI Gateway rename and new features"
```

---

## Audit 2026-04-13 — Remaining Unfinished Items

**All items closed as of 2026-04-13.**

- Task 13 Step 3 (REST `AiGatewayTagApiController` rewire) → **WONTFIX** — existing controller is routing-policy-scoped and correct; workspace tags exposed via GraphQL only.
- Task 15 Step 2 (tag GraphQL ops) → shipped: `client/src/graphql/automation/ai-gateway/aiGatewayTags.graphql`.
- Task 17 Steps 1–3 (Tags UI + tab) → shipped: `client/src/pages/automation/ai-gateway/components/tags/{AiGatewayTags.tsx,AiGatewayTagDialog.tsx}` + nav wired in `AiGateway.tsx`.
- Step 4 commit boxes → pending a user-triggered commit; not blocking.

