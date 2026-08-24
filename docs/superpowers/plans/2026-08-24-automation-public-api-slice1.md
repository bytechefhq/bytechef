# Public Automation API — Slice 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give `/api/automation/v1` a designed public contract — one credential rule, one error
schema, uuid-addressed configuration resources — and prove it on ten read endpoints plus the CLI
commands that drive them.

**Architecture:** Everything lands in the existing
`automation-configuration-public-rest` module, which is spec-first: `openapi.yaml` is the source of
truth, `generateOpenAPISpring` produces the `*Api` interfaces and `*Model` classes into `generated/`
(committed), and hand-written `*ApiController` classes implement those interfaces and delegate to
facades. Authorization is **not** re-implemented here: every read maps onto a facade method that
already carries the right `@PreAuthorize`, so the controllers stay thin. Two entities gain a `uuid`
column so they can be addressed publicly.

**Tech Stack:** Java 25, Spring Boot 4, openapi-generator 7.24.0 (`spring` generator, vendored
`pojo.mustache`), Liquibase, JUnit 5 + AssertJ + Mockito, Testcontainers, Spring Shell (CLI).

**Spec:** `docs/superpowers/specs/2026-08-24-automation-public-api-slice1-design.md`

## Global Constraints

- **EE licensing:** every file under `server/ee/` uses the ByteChef Enterprise license header and
  carries a `@version ee` Javadoc tag. Spotless picks the header from the `@version ee` **content**,
  not the path.
- **Formatting:** run `./gradlew spotlessApply` before every commit. Never judge a Gradle run piped
  into `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep for
  `^> Task .* FAILED`.
- **Blank line before control statements**, blank line after a variable modification that a
  following statement uses, no trailing blank line before a class's closing brace.
- **No `TODO:` comments** — Checkstyle's `TodoComment` rule forbids them.
- **Test naming:** unit tests end `Test`, integration tests end `IntTest`, method names are camelCase
  with no underscores — this applies to private helpers too.
- **Generated sources are committed.** After editing `openapi.yaml`, run the module's
  `generateOpenAPISpring` and commit `generated/` in the same commit as the spec change.
- **Enum ordinals are persisted as INT** — append new values, never reorder. This plan introduces no
  new enum values.
- **Liquibase:** `project_deployment` and `workspace` are long-released tables, so every schema
  change is a NEW changeset file. Never edit an init changelog. Verify changelog edits with an
  existing `*IntTest` (Testcontainers builds the schema from scratch); the `liquibase` Spring profile
  does not apply migrations via `bootRun`.
- **Commit messages:** `<ticket> <description>` for server, `<ticket> client - <description>` for
  client. Use the ticket number the work is filed under; if none, omit the prefix.

## Module paths used throughout

| Short name | Path |
|---|---|
| `public-rest` | `server/ee/libs/automation/automation-configuration/automation-configuration-public-rest` |
| `security-web` | `server/ee/libs/automation/automation-security-web/automation-security-web-impl` |
| `config-service` | `server/libs/automation/automation-configuration/automation-configuration-service` |
| `changelog` | `config-service/src/main/resources/config/liquibase/changelog/automation/configuration` |
| `cli-embedded` | `cli/commands/embedded` |
| `cli-automation` | `cli/commands/automation` |

Gradle project paths:
- `:server:ee:libs:automation:automation-configuration:automation-configuration-public-rest`
- `:server:ee:libs:automation:automation-security-web:automation-security-web-impl`
- `:server:libs:automation:automation-configuration:automation-configuration-service`
- `:cli:clients:automation-configuration`, `:cli:commands:automation`

---

### Task 1: Reject non-AUTOMATION keys on `/api/automation/v1`

**Files:**
- Modify: `security-web/src/main/java/com/bytechef/ee/automation/security/web/authentication/AutomationApiKeyAuthenticationProvider.java`
- Test: `security-web/src/test/java/com/bytechef/ee/automation/security/web/authentication/AutomationApiKeyAuthenticationProviderTest.java` (create)

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: the guarantee every later task relies on — a request reaching an
  `/api/automation/v1` controller was authenticated by an `AUTOMATION`-typed key, and the principal
  carries the key owner's real Spring authorities.

Background the implementer needs: `ApiKey.getType()` returns a `PlatformType`
(`AUTOMATION`, `EMBEDDED`) or `null`. `null` means an **admin** key, because
`getAdminApiKeys(environmentId)` is `getApiKeys(environmentId, null)`. Today this provider performs
no type check at all, so admin and `EMBEDDED` keys authenticate here.

- [ ] **Step 1: Write the failing test**

Create `AutomationApiKeyAuthenticationProviderTest.java`:

```java
package com.bytechef.ee.automation.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class AutomationApiKeyAuthenticationProviderTest {

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final UserService userService = mock(UserService.class);

    private final AutomationApiKeyAuthenticationProvider provider =
        new AutomationApiKeyAuthenticationProvider(apiKeyService, authorityService, userService);

    @Test
    void testAdminApiKeyIsRejected() {
        stubApiKeyOfType(null);

        assertThatThrownBy(() -> provider.authenticate(token()))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Automation API key required");
    }

    @Test
    void testEmbeddedApiKeyIsRejected() {
        stubApiKeyOfType(PlatformType.EMBEDDED);

        assertThatThrownBy(() -> provider.authenticate(token()))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Automation API key required");
    }

    private void stubApiKeyOfType(PlatformType platformType) {
        ApiKey apiKey = new ApiKey();

        apiKey.setSecretKey("btc_x");
        apiKey.setUserId(1L);

        if (platformType != null) {
            apiKey.setType(platformType);
        }

        when(apiKeyService.getApiKey("btc_x", 1L)).thenReturn(apiKey);
    }

    private static AutomationApiKeyAuthenticationToken token() {
        return new AutomationApiKeyAuthenticationToken(1, "btc_x", "000000");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-security-web:automation-security-web-impl:test --tests '*AutomationApiKeyAuthenticationProviderTest*'
```

Expected: FAIL. Without the type check the provider proceeds to `userService.fetchUser(1L)`, which
the mock returns empty for, so it throws `UsernameNotFoundException` rather than
`BadCredentialsException`.

- [ ] **Step 3: Add the type check**

In `AutomationApiKeyAuthenticationProvider.authenticate`, immediately after the `getApiKey` try/catch
block and before `userService.fetchUser(...)`:

```java
if (apiKey.getType() != PlatformType.AUTOMATION) {
    throw new BadCredentialsException("Automation API key required");
}
```

Add the import `com.bytechef.platform.constant.PlatformType`. Extend the class Javadoc to say why:

```java
/**
 * Authenticates {@code /api/automation/v<n>/} as the API key's own ByteChef user, with that user's
 * real authorities.
 *
 * <p>
 * Only an {@code AUTOMATION} key is accepted. An {@code EMBEDDED} key belongs to a different
 * surface, and the admin key -- the one carrying no {@code PlatformType} -- is reserved for the
 * tenant-wide operations under {@code /api/platform/v1}. Before this check the provider accepted all
 * three, which made the admin key a universal credential.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
```

- [ ] **Step 4: Pin the path routing**

The provider only matters if the configurer actually claims the path. Add
`security-web/src/test/java/com/bytechef/ee/automation/security/web/configurer/AutomationApiKeyPathRoutingTest.java`:

```java
package com.bytechef.ee.automation.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Pins that every public automation path is claimed by {@link AutomationApiKeySecurityConfigurer},
 * and therefore runs through the key-type check in
 * {@code AutomationApiKeyAuthenticationProvider}. Without this, narrowing the pattern would silently
 * move an endpoint onto a different chain and reinstate admin-key access.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationApiKeyPathRoutingTest {

    @Test
    void testEveryPublicAutomationPathIsClaimed() throws Exception {
        RequestMatcher requestMatcher = regexMatcher(readPathPattern());

        for (String path : new String[] {
            "/api/automation/v1/workspaces",
            "/api/automation/v1/projects",
            "/api/automation/v1/projects/00000000-0000-0000-0000-0000000000aa",
            "/api/automation/v1/workflows/00000000-0000-0000-0000-0000000000bb",
            "/api/automation/v1/project-deployments",
            "/api/automation/v1/workflow-executions"
        }) {
            assertThat(requestMatcher.matches(request(path)))
                .as("automation configurer must claim %s", path)
                .isTrue();
        }
    }

    @Test
    void testInternalPathsAreNotClaimed() throws Exception {
        RequestMatcher requestMatcher = regexMatcher(readPathPattern());

        assertThat(requestMatcher.matches(request("/api/automation/internal/projects")))
            .isFalse();
    }

    private static String readPathPattern() throws Exception {
        Field field = AutomationApiKeySecurityConfigurer.class.getDeclaredField("PATH_PATTERN");

        field.setAccessible(true);

        return (String) field.get(null);
    }

    private static HttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);

        // RegexRequestMatcher matches on servletPath + pathInfo, which MockHttpServletRequest leaves
        // empty when only the request URI is given.
        request.setServletPath(path);

        return request;
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :server:ee:libs:automation:automation-security-web:automation-security-web-impl:check
```

Expected: PASS, no `^> Task .* FAILED`.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-security-web
git commit -m "Require an automation API key on /api/automation/v1"
```

---

### Task 2: Declare the `Error` schema on the public spec

**Files:**
- Modify: `public-rest/openapi.yaml`
- Modify (regenerate): `public-rest/generated/**`
- Create: `public-rest/src/main/java/com/bytechef/ee/automation/configuration/public_/web/rest/PublicApiExceptionHandler.java`
- Test: `public-rest/src/test/java/com/bytechef/ee/automation/configuration/public_/web/rest/PublicApiExceptionHandlerTest.java` (create)

**Interfaces:**
- Consumes: nothing.
- Produces: `ErrorModel` with fields `type`, `title`, `status`, `detail`, `errorKey`; every later
  task references `#/components/schemas/Error` from its 4xx/5xx responses.

- [ ] **Step 1: Add the schema and a reusable response to `openapi.yaml`**

Under `components.schemas`, add:

```yaml
    Error:
      type: "object"
      description: "A problem detail, as defined by RFC 9457, extended with a stable machine-readable key."
      required:
        - "status"
        - "errorKey"
      properties:
        type:
          description: "A URI identifying the problem type."
          type: "string"
        title:
          description: "A short human-readable summary of the problem type."
          type: "string"
        status:
          description: "The HTTP status code."
          type: "integer"
          format: "int32"
        detail:
          description: "A human-readable explanation specific to this occurrence."
          type: "string"
        errorKey:
          description: "A stable machine-readable key. Branch on this, never on the message text."
          type: "string"
```

Under `components.responses`, add:

```yaml
    BadRequestError:
      description: "The request was malformed."
      content:
        application/problem+json:
          schema:
            $ref: "#/components/schemas/Error"
    ForbiddenError:
      description: "The caller is authenticated but not permitted to perform this operation."
      content:
        application/problem+json:
          schema:
            $ref: "#/components/schemas/Error"
    NotFoundError:
      description: "No such resource."
      content:
        application/problem+json:
          schema:
            $ref: "#/components/schemas/Error"
```

Give the existing `UnauthorizedError` response the same body:

```yaml
    UnauthorizedError:
      description: "Access token is missing or invalid."
      content:
        application/problem+json:
          schema:
            $ref: "#/components/schemas/Error"
```

- [ ] **Step 2: Regenerate and confirm the model appeared**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:generateOpenAPISpring
ls server/ee/libs/automation/automation-configuration/automation-configuration-public-rest/generated/src/main/java/com/bytechef/ee/automation/configuration/public_/web/rest/model/ErrorModel.java
```

Expected: the file exists.

- [ ] **Step 3: Write the failing handler test**

Create `PublicApiExceptionHandlerTest.java`:

```java
package com.bytechef.ee.automation.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.configuration.public_.web.rest.model.ErrorModel;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

class PublicApiExceptionHandlerTest {

    private final PublicApiExceptionHandler publicApiExceptionHandler = new PublicApiExceptionHandler();

    @Test
    void testAccessDeniedMapsToForbiddenWithStableKey() {
        ResponseEntity<ErrorModel> responseEntity =
            publicApiExceptionHandler.handleAccessDenied(new AccessDeniedException("nope"));

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ErrorModel errorModel = responseEntity.getBody();

        assertThat(errorModel).isNotNull();
        assertThat(errorModel.getErrorKey()).isEqualTo("FORBIDDEN");
        assertThat(errorModel.getStatus()).isEqualTo(403);
    }

    @Test
    void testNoSuchElementMapsToNotFoundWithStableKey() {
        ResponseEntity<ErrorModel> responseEntity =
            publicApiExceptionHandler.handleNotFound(new java.util.NoSuchElementException("gone"));

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ErrorModel errorModel = responseEntity.getBody();

        assertThat(errorModel).isNotNull();
        assertThat(errorModel.getErrorKey()).isEqualTo("NOT_FOUND");
    }

    @Test
    void testDetailNeverEchoesTheExceptionMessage() {
        ResponseEntity<ErrorModel> responseEntity =
            publicApiExceptionHandler.handleAccessDenied(new AccessDeniedException("workspace 42 denied"));

        ErrorModel errorModel = responseEntity.getBody();

        assertThat(errorModel).isNotNull();
        assertThat(errorModel.getDetail()).doesNotContain("42");
    }
}
```

The third test is the one that matters: an exception message can carry ids and internal detail, and
this is a public surface.

- [ ] **Step 4: Run it to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:test --tests '*PublicApiExceptionHandlerTest*'
```

Expected: FAIL to compile — `PublicApiExceptionHandler` does not exist.

- [ ] **Step 5: Write the handler**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import com.bytechef.ee.automation.configuration.public_.web.rest.model.ErrorModel;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Renders every error on the public automation API as the declared {@code Error} schema.
 *
 * <p>
 * {@code detail} is written here, never copied from the exception: an exception message is an
 * internal artifact and routinely carries ids, table names and workspace numbers that a public
 * caller has no business seeing. Consumers branch on {@code errorKey}, which is why it is a closed
 * set of constants rather than anything derived from the throwable.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestControllerAdvice(basePackages = "com.bytechef.ee.automation.configuration.public_.web.rest")
public class PublicApiExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorModel> handleAccessDenied(AccessDeniedException accessDeniedException) {
        return toResponseEntity(
            HttpStatus.FORBIDDEN, "FORBIDDEN", "The caller is not permitted to perform this operation.");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorModel> handleNotFound(NoSuchElementException noSuchElementException) {
        return toResponseEntity(HttpStatus.NOT_FOUND, "NOT_FOUND", "No such resource.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorModel> handleBadRequest(IllegalArgumentException illegalArgumentException) {
        return toResponseEntity(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "The request was malformed.");
    }

    private static ResponseEntity<ErrorModel> toResponseEntity(HttpStatus httpStatus, String errorKey, String detail) {
        ErrorModel errorModel = new ErrorModel()
            .type("https://docs.bytechef.io/errors/" + errorKey.toLowerCase())
            .title(httpStatus.getReasonPhrase())
            .status(httpStatus.value())
            .detail(detail)
            .errorKey(errorKey);

        return ResponseEntity.status(httpStatus)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(errorModel);
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:check
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-configuration/automation-configuration-public-rest
git commit -m "Declare an error schema on the public automation API"
```

---

### Task 3: Give `workspace` and `project_deployment` a uuid

**Files:**
- Create: `changelog/20260824120000_automation_configuration_added_workspace_and_deployment_uuid.xml`
- Modify: `.../automation-configuration-api/.../domain/Workspace.java`
- Modify: `.../automation-configuration-api/.../domain/ProjectDeployment.java`
- Modify: `.../automation-configuration-api/.../service/ProjectDeploymentService.java`
- Modify: `config-service/.../repository/ProjectDeploymentRepository.java`
- Modify: `config-service/.../service/ProjectDeploymentServiceImpl.java`
- Test: `config-service/src/test/java/.../service/WorkspaceServiceIntTest.java` (extend or create)

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `Workspace#getUuid(): UUID`, `Workspace#setUuid(UUID)`
  - `ProjectDeployment#getUuid(): UUID`, `ProjectDeployment#setUuid(UUID)`
  - `ProjectDeploymentService#getProjectDeployment(UUID uuid): ProjectDeployment`

  Task 8 calls `getProjectDeployment(UUID)` to resolve a public uuid to the numeric id the guarded
  facade method takes. **Workspace deliberately gets no `getWorkspace(UUID)`** — nothing in this
  slice addresses a workspace by uuid, because `workspaceId` collection parameters take the numeric
  id. The column and the model field exist so later slices can address workspaces by uuid; the
  resolver arrives with the first caller that needs it.

Mirror `Project` exactly — it already has this shape. Both tables are long released, so this is a
new changeset file; do not touch any init changelog.

- [ ] **Step 1: Write the changeset**

Create `changelog/20260824120000_automation_configuration_added_workspace_and_deployment_uuid.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="20260824120000-1" author="Ivica Cardic">
        <addColumn tableName="workspace">
            <column name="uuid" type="${uuid_type}"/>
        </addColumn>
        <addColumn tableName="project_deployment">
            <column name="uuid" type="${uuid_type}"/>
        </addColumn>
    </changeSet>

    <changeSet id="20260824120000-2" author="Ivica Cardic" dbms="postgresql">
        <sql>
            UPDATE workspace
            SET uuid = gen_random_uuid()
            WHERE uuid IS NULL;
        </sql>
        <sql>
            UPDATE project_deployment
            SET uuid = gen_random_uuid()
            WHERE uuid IS NULL;
        </sql>
    </changeSet>

    <changeSet id="20260824120000-3" author="Ivica Cardic">
        <addNotNullConstraint tableName="workspace" columnName="uuid"/>
        <addUniqueConstraint tableName="workspace" columnNames="uuid"/>
        <addNotNullConstraint tableName="project_deployment" columnName="uuid"/>
        <addUniqueConstraint tableName="project_deployment" columnNames="uuid"/>
    </changeSet>
</databaseChangeLog>
```

This is the shape of `202508311958010_automation_configuration_added_column_uuid.xml`, which did the
same job for `project`. The three-changeset split matters: adding the column, backfilling, and then
constraining are separate so the backfill can be re-run independently if it fails partway.

- [ ] **Step 2: Add the domain fields**

In `Workspace.java` and `ProjectDeployment.java`, mirror `Project`:

```java
    private UUID uuid;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
```

Place the field with the other fields and the accessors with the other accessors, matching the
file's existing ordering. Import `java.util.UUID`.

- [ ] **Step 3: Add the repository finders**

In `WorkspaceRepository` and `ProjectDeploymentRepository`, mirror
`ProjectRepository.findByUuid`:

```java
    Optional<ProjectDeployment> findByUuid(UUID uuid);
```

Spring Data JDBC derives the query from the method name; no `@Query` needed. `WorkspaceRepository`
gets no finder — see the Interfaces note above.

- [ ] **Step 4: Write the failing service test**

In `config-service`'s integration test source set, add to the workspace int test:

```java
    @Test
    void testCreatedWorkspaceIsAssignedAUuid() {
        Workspace workspace = workspaceService.create(new Workspace("test-workspace"));

        assertThat(workspace.getUuid()).isNotNull();
    }

    @Test
    void testGetProjectDeploymentByUuidReturnsTheSameRowAsById() {
        ProjectDeployment projectDeployment = projectDeploymentService.create(aProjectDeployment());

        ProjectDeployment byUuid = projectDeploymentService.getProjectDeployment(projectDeployment.getUuid());

        assertThat(byUuid.getId()).isEqualTo(projectDeployment.getId());
    }

    @Test
    void testGetProjectDeploymentByUnknownUuidThrows() {
        assertThatThrownBy(() -> projectDeploymentService.getProjectDeployment(UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class);
    }
```

Build `aProjectDeployment()` from whatever the module's existing deployment int tests already use as
a fixture; do not invent a new builder.

If no `WorkspaceServiceIntTest` exists yet, create one following the module's existing
`@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` + `@ActiveProfiles("testint")`
pattern. This test is also what proves the changeset applies — Testcontainers builds the schema from
scratch, which is stronger evidence than any `bootRun` check.

- [ ] **Step 5: Run it to verify it fails**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests '*WorkspaceServiceIntTest*'
```

Expected: FAIL to compile — `getProjectDeployment(UUID)` does not exist.

- [ ] **Step 6: Add the service methods**

To `ProjectDeploymentService`:

```java
    ProjectDeployment getProjectDeployment(UUID uuid);
```

And to `ProjectDeploymentServiceImpl` — note this method takes a uuid, resolves it, and is **not**
guarded, because the caller passes the resolved id to a guarded method next:

```java
    /**
     * Resolves a public uuid to its deployment row. Deliberately unguarded: it is the resolution half
     * of the public API's uuid -> id hop, and the permission check happens on the guarded facade
     * method the caller invokes with the resolved id. A uuid is not enumerable, so returning 404 here
     * before any check leaks nothing.
     */
    @Override
    @Transactional(readOnly = true)
    public ProjectDeployment getProjectDeployment(UUID uuid) {
        return projectDeploymentRepository.findByUuid(uuid)
            .orElseThrow(() -> new IllegalArgumentException("Project deployment not found"));
    }
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:check :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration
```

Expected: PASS. If Testcontainers cannot reach Docker, point it at the OrbStack socket rather than
`/var/run/docker.sock`.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-configuration
git commit -m "Add a uuid to workspace and project deployment"
```

---

### Task 4: `GET /workspaces`

**Files:**
- Modify: `public-rest/openapi.yaml`
- Modify (regenerate): `public-rest/generated/**`
- Create: `public-rest/src/main/java/com/bytechef/ee/automation/configuration/public_/web/rest/WorkspaceApiController.java`
- Test: `public-rest/src/test/java/com/bytechef/ee/automation/configuration/public_/web/rest/WorkspaceApiControllerTest.java` (create)
- Modify: `public-rest/build.gradle.kts` (add the `platform-user-api` dependency)

**Interfaces:**
- Consumes: `Workspace#getUuid()` from Task 3; `#/components/schemas/Error` from Task 2.
- Produces: `WorkspaceModel` with `uuid` (string) and `name` (string). Every later collection task
  documents `workspaceId` as the numeric id a caller reads from **this** endpoint.

**The one thing to get right.** `WorkspaceService.getWorkspaces()` returns *every workspace in the
tenant* and carries no `@PreAuthorize` — its Javadoc says so explicitly. Calling it here would turn
this endpoint into tenant-wide enumeration. Use `WorkspaceFacade.getUserWorkspaces(userId)` instead,
which is guarded `isTenantAdmin() or isCurrentUser(#id)` and filters by membership for non-admins in
EE. The user id comes from `userService.getCurrentUser().getId()`.

- [ ] **Step 1: Add the endpoint to `openapi.yaml`**

Register the tag alongside the existing ones:

```yaml
  - name: "workspace"
    description: "The Automation Workspace Public API"
```

Add the path:

```yaml
  /workspaces:
    get:
      description: "List the workspaces the caller is a member of."
      summary: "List workspaces"
      tags:
        - "workspace"
      operationId: "getWorkspaces"
      responses:
        "200":
          description: "The list of workspaces."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/Workspace"
        "401":
          $ref: "#/components/responses/UnauthorizedError"
      security:
        - bearerAuth: [ ]
```

And the schema:

```yaml
    Workspace:
      type: "object"
      description: "A workspace."
      required:
        - "uuid"
        - "name"
      properties:
        uuid:
          description: "The uuid of the workspace. Use this to identify a workspace across environments."
          type: "string"
          format: "uuid"
        id:
          description: "The numeric id of the workspace, as accepted by the workspaceId query parameter."
          type: "integer"
          format: "int64"
        name:
          description: "The name of the workspace."
          type: "string"
```

Note this model carries **both** identifiers on purpose: `workspaceId` collection parameters take
the numeric id, so a caller needs a way to obtain it. This is the only model in the slice that
exposes both.

- [ ] **Step 2: Regenerate**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:generateOpenAPISpring
```

Expected: `generated/.../WorkspaceApi.java` and `generated/.../model/WorkspaceModel.java` appear.

- [ ] **Step 3: Write the failing test**

```java
package com.bytechef.ee.automation.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkspaceModel;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class WorkspaceApiControllerTest {

    private final UserService userService = mock(UserService.class);
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    private final WorkspaceApiController workspaceApiController =
        new WorkspaceApiController(userService, workspaceFacade);

    @Test
    void testGetWorkspacesReturnsOnlyTheCallersWorkspaces() {
        User user = new User();

        user.setId(7L);

        when(userService.getCurrentUser()).thenReturn(user);

        Workspace workspace = new Workspace();

        workspace.setId(1L);
        workspace.setName("Acme");
        workspace.setUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        when(workspaceFacade.getUserWorkspaces(7L)).thenReturn(List.of(workspace));

        ResponseEntity<List<WorkspaceModel>> responseEntity = workspaceApiController.getWorkspaces();

        List<WorkspaceModel> workspaceModels = responseEntity.getBody();

        assertThat(workspaceModels).hasSize(1);
        assertThat(workspaceModels.getFirst()
            .getName()).isEqualTo("Acme");
        assertThat(workspaceModels.getFirst()
            .getId()).isEqualTo(1L);
    }

    @Test
    void testControllerCannotReachTheUnguardedTenantWideListing() {
        boolean dependsOnWorkspaceService = java.util.Arrays
            .stream(WorkspaceApiController.class.getDeclaredFields())
            .anyMatch(field -> field.getType() == WorkspaceService.class);

        assertThat(dependsOnWorkspaceService)
            .as(
                "WorkspaceService.getWorkspaces() returns every workspace in the tenant and carries no "
                    + "@PreAuthorize; this endpoint must reach workspaces only through "
                    + "WorkspaceFacade.getUserWorkspaces")
            .isFalse();
    }
}
```

The second test needs explaining, because the obvious version of it does not work. Verifying
`never()` on a `WorkspaceService` mock would be vacuous — the controller does not take one, so the
assertion passes regardless of what the controller does. The check that actually bites is structural:
the controller must not hold a `WorkspaceService` field at all. That is what stops someone
"simplifying" the facade call into the unguarded service, which is the failure mode Risk 3 names.

Drop `WorkspaceService` from the test's mocks — it is only referenced as a type literal above.

- [ ] **Step 4: Run it to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:test --tests '*WorkspaceApiControllerTest*'
```

Expected: FAIL to compile — `WorkspaceApiController` does not exist.

- [ ] **Step 5: Write the controller**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkspaceModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lists the workspaces the caller belongs to.
 *
 * <p>
 * Deliberately goes through {@link WorkspaceFacade#getUserWorkspaces(long)} rather than
 * {@code WorkspaceService#getWorkspaces()}. The latter returns every workspace in the tenant and
 * carries no {@code @PreAuthorize} -- it is a trusted-caller method -- so reaching it from a public
 * endpoint would make this tenant-wide enumeration. The facade method is guarded
 * {@code isTenantAdmin() or isCurrentUser(#id)} and filters by membership for non-admins.
 *
 * <p>
 * This is the entry point for the whole read surface: every collection endpoint takes a required
 * {@code workspaceId}, and this is where a caller obtains one. That is why {@code WorkspaceModel} is
 * the single model in the slice carrying both the uuid and the numeric id.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.configuration.public_.web.rest.WorkspaceApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class WorkspaceApiController implements WorkspaceApi {

    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI2")
    public WorkspaceApiController(UserService userService, WorkspaceFacade workspaceFacade) {
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @Override
    public ResponseEntity<List<WorkspaceModel>> getWorkspaces() {
        long userId = userService.getCurrentUser()
            .getId();

        List<WorkspaceModel> workspaceModels = workspaceFacade.getUserWorkspaces(userId)
            .stream()
            .map(WorkspaceApiController::toModel)
            .toList();

        return ResponseEntity.ok(workspaceModels);
    }

    private static WorkspaceModel toModel(Workspace workspace) {
        return new WorkspaceModel()
            .uuid(Objects.requireNonNull(workspace.getUuid())
                .toString())
            .id(workspace.getId())
            .name(workspace.getName());
    }
}
```

- [ ] **Step 6: Add the module dependency**

`public-rest/build.gradle.kts` does not yet depend on the user module. Add, in the existing
alphabetical block of `implementation(project(...))` lines:

```kotlin
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:check
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-configuration/automation-configuration-public-rest
git commit -m "Add the public workspaces listing to the automation API"
```

---

### Task 5: `GET /projects` and `GET /projects/{uuid}`

**Files:**
- Modify: `public-rest/openapi.yaml`
- Modify (regenerate): `public-rest/generated/**`
- Create: `public-rest/src/main/java/com/bytechef/ee/automation/configuration/public_/web/rest/ProjectApiController.java`
- Test: `public-rest/src/test/java/.../ProjectApiControllerTest.java` (create)

**Interfaces:**
- Consumes: `Error` schema (Task 2); `ProjectService#getProject(UUID)`, which already exists.
- Produces: `ProjectModel` with `uuid`, `name`, `description`, `lastVersion`, `lastPublishedDate`,
  `status`. Tasks 6 and 7 reference `#/components/schemas/Project`.

No new `@PreAuthorize` is needed. `ProjectFacadeImpl.getWorkspaceProjects` is already guarded
`hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')` and `getProject(long)` is already guarded
`hasPermission(#id, 'Project', 'WORKFLOW_VIEW')`. The controller resolves uuid → id and delegates.

- [ ] **Step 1: Add both paths and the schema to `openapi.yaml`**

Tag:

```yaml
  - name: "project"
    description: "The Automation Project Public API"
```

Paths:

```yaml
  /projects:
    get:
      description: "List the projects in a workspace."
      summary: "List projects"
      tags:
        - "project"
      operationId: "getProjects"
      parameters:
        - name: "workspaceId"
          description: "The id of a workspace, as returned by GET /workspaces."
          in: "query"
          required: true
          schema:
            type: "integer"
            format: "int64"
        - name: "categoryId"
          description: "Restrict to a single category."
          in: "query"
          required: false
          schema:
            type: "integer"
            format: "int64"
        - name: "tagId"
          description: "Restrict to a single tag."
          in: "query"
          required: false
          schema:
            type: "integer"
            format: "int64"
      responses:
        "200":
          description: "The list of projects."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/Project"
        "401":
          $ref: "#/components/responses/UnauthorizedError"
        "403":
          $ref: "#/components/responses/ForbiddenError"
      security:
        - bearerAuth: [ ]
  /projects/{uuid}:
    get:
      description: "Get a project by uuid."
      summary: "Get a project"
      tags:
        - "project"
      operationId: "getProject"
      parameters:
        - name: "uuid"
          description: "The uuid of a project."
          in: "path"
          required: true
          schema:
            type: "string"
            format: "uuid"
      responses:
        "200":
          description: "The project."
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Project"
        "401":
          $ref: "#/components/responses/UnauthorizedError"
        "403":
          $ref: "#/components/responses/ForbiddenError"
        "404":
          $ref: "#/components/responses/NotFoundError"
      security:
        - bearerAuth: [ ]
```

`GET /projects` returns an array rather than a `Page`: the underlying
`ProjectFacade.getWorkspaceProjects` returns a `List`, not a `Page`, and inventing pagination over an
in-memory list would be a lie about what the server does. Paging lands on `/project-deployments` and
`/workflow-executions`, which are genuinely paged. Note this in the endpoint description so the
asymmetry is deliberate and visible.

Schema:

```yaml
    Project:
      type: "object"
      description: "An automation project."
      required:
        - "uuid"
        - "name"
      properties:
        uuid:
          description: "The uuid of the project."
          type: "string"
          format: "uuid"
        name:
          description: "The name of the project."
          type: "string"
        description:
          description: "The description of the project."
          type: "string"
        lastVersion:
          description: "The last version of the project."
          type: "integer"
        lastPublishedDate:
          description: "The date the last version was published."
          type: "string"
          format: "date-time"
        status:
          description: "Whether the project has a published version."
          type: "string"
          enum:
            - "DRAFT"
            - "PUBLISHED"
```

- [ ] **Step 2: Regenerate**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:generateOpenAPISpring
```

- [ ] **Step 3: Write the failing test**

```java
package com.bytechef.ee.automation.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ProjectModel;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ProjectApiControllerTest {

    private static final UUID PROJECT_UUID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private final ProjectFacade projectFacade = mock(ProjectFacade.class);
    private final ProjectService projectService = mock(ProjectService.class);

    private final ProjectApiController projectApiController =
        new ProjectApiController(projectFacade, projectService);

    @Test
    void testGetProjectResolvesTheUuidAndDelegatesWithTheNumericId() {
        Project project = new Project();

        project.setId(5L);
        project.setName("Acme");

        when(projectService.getProject(PROJECT_UUID)).thenReturn(project);

        projectApiController.getProject(PROJECT_UUID.toString());

        verify(projectFacade).getProject(5L);
    }

    @Test
    void testGetProjectWithUnknownUuidThrowsBeforeReachingTheGuardedFacade() {
        when(projectService.getProject(PROJECT_UUID))
            .thenThrow(new IllegalArgumentException("Project not found"));

        assertThatThrownBy(() -> projectApiController.getProject(PROJECT_UUID.toString()))
            .isInstanceOf(IllegalArgumentException.class);

        verify(projectFacade, org.mockito.Mockito.never())
            .getProject(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void testGetProjectsPassesTheWorkspaceIdThrough() {
        when(projectFacade.getWorkspaceProjects(null, null, false, null, null, null, 3L))
            .thenReturn(java.util.List.of());

        ResponseEntity<java.util.List<ProjectModel>> responseEntity =
            projectApiController.getProjects(3L, null, null);

        assertThat(responseEntity.getBody()).isEmpty();
    }
}
```

The second test is the one that matters: it pins that an unknown uuid fails at resolution, before
the guarded facade is reached, which is what makes 404-before-403 correct rather than accidental.

- [ ] **Step 4: Run it to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:test --tests '*ProjectApiControllerTest*'
```

Expected: FAIL to compile — `ProjectApiController` does not exist.

- [ ] **Step 5: Write the controller**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ProjectModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read endpoints for automation projects.
 *
 * <p>
 * Authorization is not repeated here. {@code ProjectFacadeImpl#getWorkspaceProjects} already carries
 * {@code hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')} and {@code #getProject(long)}
 * carries {@code hasPermission(#id, 'Project', 'WORKFLOW_VIEW')}; this controller only translates the
 * public uuid into the numeric id those guards take. The resolution runs first, so an unknown uuid
 * answers 404 without a permission check ever running -- acceptable because a uuid is not
 * enumerable.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.configuration.public_.web.rest.ProjectApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ProjectApiController implements ProjectApi {

    private final ProjectFacade projectFacade;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI2")
    public ProjectApiController(ProjectFacade projectFacade, ProjectService projectService) {
        this.projectFacade = projectFacade;
        this.projectService = projectService;
    }

    @Override
    public ResponseEntity<List<ProjectModel>> getProjects(
        Long workspaceId, @Nullable Long categoryId, @Nullable Long tagId) {

        List<ProjectModel> projectModels =
            projectFacade.getWorkspaceProjects(null, categoryId, false, null, null, tagId, workspaceId)
                .stream()
                .map(this::toModel)
                .toList();

        return ResponseEntity.ok(projectModels);
    }

    @Override
    public ResponseEntity<ProjectModel> getProject(String uuid) {
        Project project = projectService.getProject(UUID.fromString(uuid));

        return ResponseEntity.ok(toModel(projectFacade.getProject(Objects.requireNonNull(project.getId()))));
    }

    private ProjectModel toModel(ProjectDTO projectDTO) {
        return new ProjectModel()
            .uuid(Objects.requireNonNull(projectDTO.uuid())
                .toString())
            .name(projectDTO.name())
            .description(projectDTO.description())
            .lastVersion(projectDTO.lastVersion())
            .status(
                projectDTO.lastPublishedDate() == null
                    ? ProjectModel.StatusEnum.DRAFT
                    : ProjectModel.StatusEnum.PUBLISHED);
    }
}
```

If `ProjectDTO` exposes `uuid()` under a different accessor name, read the record's component list
and adjust — do not add a uuid field to the DTO, it is already carried on `Project`.

- [ ] **Step 6: Run tests to verify they pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:check
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-configuration/automation-configuration-public-rest
git commit -m "Add public project read endpoints to the automation API"
```

---

### Task 6: `GET /projects/{uuid}/versions` and `GET /projects/{uuid}/workflows`

**Files:**
- Modify: `public-rest/openapi.yaml`
- Modify (regenerate): `public-rest/generated/**`
- Modify: `public-rest/src/main/java/.../ProjectApiController.java`
- Modify: `public-rest/src/test/java/.../ProjectApiControllerTest.java`

**Interfaces:**
- Consumes: `ProjectApiController` and `Project` schema from Task 5.
- Produces: `ProjectVersionModel` (`version`, `description`, `publishedDate`, `status`) and
  `ProjectWorkflowModel` (`uuid`, `label`, `description`) — Task 7 reuses `ProjectWorkflowModel`.

`ProjectFacade.getProjectVersions(long)` is guarded `hasPermission(#id, 'Project', 'WORKFLOW_VIEW')`.
For workflows, use `ProjectWorkflowService.getProjectWorkflows(long projectId)` when no version is
given, and `getProjectWorkflows(long projectId, int projectVersion)` when `?version=` is supplied —
but call the guarded `projectFacade.getProject(id)` **first** so the permission check runs, because
`ProjectWorkflowService` methods are unguarded trusted-caller methods.

- [ ] **Step 1: Add the two paths and two schemas to `openapi.yaml`**

```yaml
  /projects/{uuid}/versions:
    get:
      description: "List a project's versions, newest first."
      summary: "List project versions"
      tags:
        - "project"
      operationId: "getProjectVersions"
      parameters:
        - name: "uuid"
          description: "The uuid of a project."
          in: "path"
          required: true
          schema:
            type: "string"
            format: "uuid"
      responses:
        "200":
          description: "The list of project versions."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/ProjectVersion"
        "401":
          $ref: "#/components/responses/UnauthorizedError"
        "403":
          $ref: "#/components/responses/ForbiddenError"
        "404":
          $ref: "#/components/responses/NotFoundError"
      security:
        - bearerAuth: [ ]
  /projects/{uuid}/workflows:
    get:
      description: "List a project's workflows. Defaults to the latest version."
      summary: "List project workflows"
      tags:
        - "project"
      operationId: "getProjectWorkflows"
      parameters:
        - name: "uuid"
          description: "The uuid of a project."
          in: "path"
          required: true
          schema:
            type: "string"
            format: "uuid"
        - name: "version"
          description: "The project version. Defaults to the latest."
          in: "query"
          required: false
          schema:
            type: "integer"
      responses:
        "200":
          description: "The list of project workflows."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/ProjectWorkflow"
        "401":
          $ref: "#/components/responses/UnauthorizedError"
        "403":
          $ref: "#/components/responses/ForbiddenError"
        "404":
          $ref: "#/components/responses/NotFoundError"
      security:
        - bearerAuth: [ ]
```

```yaml
    ProjectVersion:
      type: "object"
      description: "One version of an automation project."
      required:
        - "version"
      properties:
        version:
          description: "The version number. Versions are addressed by this, not by a uuid."
          type: "integer"
        description:
          description: "The description recorded when the version was published."
          type: "string"
        publishedDate:
          description: "The date the version was published."
          type: "string"
          format: "date-time"
        status:
          description: "Whether this version is a draft or published."
          type: "string"
          enum:
            - "DRAFT"
            - "PUBLISHED"
    ProjectWorkflow:
      type: "object"
      description: "A workflow belonging to an automation project."
      required:
        - "uuid"
      properties:
        uuid:
          description: "The uuid of the workflow."
          type: "string"
          format: "uuid"
        label:
          description: "The label of the workflow."
          type: "string"
        description:
          description: "The description of the workflow."
          type: "string"
```

- [ ] **Step 2: Regenerate**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:generateOpenAPISpring
```

- [ ] **Step 3: Write the failing tests**

Add to `ProjectApiControllerTest`:

```java
    @Test
    void testGetProjectWorkflowsRunsThePermissionCheckBeforeTheUnguardedService() {
        Project project = new Project();

        project.setId(5L);

        when(projectService.getProject(PROJECT_UUID)).thenReturn(project);
        when(projectWorkflowService.getProjectWorkflows(5L)).thenReturn(java.util.List.of());

        projectApiController.getProjectWorkflows(PROJECT_UUID.toString(), null);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(projectFacade, projectWorkflowService);

        inOrder.verify(projectFacade)
            .getProject(5L);
        inOrder.verify(projectWorkflowService)
            .getProjectWorkflows(5L);
    }

    @Test
    void testGetProjectWorkflowsWithVersionUsesTheVersionedLookup() {
        Project project = new Project();

        project.setId(5L);

        when(projectService.getProject(PROJECT_UUID)).thenReturn(project);
        when(projectWorkflowService.getProjectWorkflows(5L, 2)).thenReturn(java.util.List.of());

        projectApiController.getProjectWorkflows(PROJECT_UUID.toString(), 2);

        verify(projectWorkflowService).getProjectWorkflows(5L, 2);
    }
```

Add the field `private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);`
and pass it as the third constructor argument.

The ordering assertion is the substance: `ProjectWorkflowService` is a trusted-caller service with no
`@PreAuthorize`, so if the guarded `getProject` call were dropped or moved after it, workflows would
be readable without the permission check and no other test would notice.

- [ ] **Step 4: Run to verify failure**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:test --tests '*ProjectApiControllerTest*'
```

Expected: FAIL to compile — the constructor takes two arguments and the methods do not exist.

- [ ] **Step 5: Extend the controller**

Add the collaborator and the two methods:

```java
    @Override
    public ResponseEntity<List<ProjectVersionModel>> getProjectVersions(String uuid) {
        long projectId = resolveProjectId(uuid);

        List<ProjectVersionModel> projectVersionModels = projectFacade.getProjectVersions(projectId)
            .stream()
            .map(projectVersion -> new ProjectVersionModel()
                .version(projectVersion.getVersion())
                .description(projectVersion.getDescription())
                .status(
                    projectVersion.getPublishedDate() == null
                        ? ProjectVersionModel.StatusEnum.DRAFT
                        : ProjectVersionModel.StatusEnum.PUBLISHED))
            .toList();

        return ResponseEntity.ok(projectVersionModels);
    }

    @Override
    public ResponseEntity<List<ProjectWorkflowModel>> getProjectWorkflows(String uuid, @Nullable Integer version) {
        long projectId = resolveProjectId(uuid);

        // The permission check lives on the facade, and ProjectWorkflowService is an unguarded
        // trusted-caller service, so the guarded read must happen before the workflow lookup.
        projectFacade.getProject(projectId);

        List<ProjectWorkflow> projectWorkflows = version == null
            ? projectWorkflowService.getProjectWorkflows(projectId)
            : projectWorkflowService.getProjectWorkflows(projectId, version);

        List<ProjectWorkflowModel> projectWorkflowModels = projectWorkflows.stream()
            .map(projectWorkflow -> new ProjectWorkflowModel()
                .uuid(Objects.requireNonNull(projectWorkflow.getUuid())
                    .toString()))
            .toList();

        return ResponseEntity.ok(projectWorkflowModels);
    }

    private long resolveProjectId(String uuid) {
        Project project = projectService.getProject(UUID.fromString(uuid));

        return Objects.requireNonNull(project.getId());
    }
```

`label` and `description` come from the underlying atlas `Workflow`, not from `ProjectWorkflow`. If
they are not reachable without an extra `WorkflowService` lookup, leave them unset for now and note
it in the endpoint description — populating them is a Task 7 concern, where the workflow itself is
read.

- [ ] **Step 6: Run tests to verify they pass**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:check
```

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-configuration/automation-configuration-public-rest
git commit -m "Add public project version and workflow listings to the automation API"
```

---

### Task 7: `GET /workflows/{uuid}`

**Files:**
- Modify: `public-rest/openapi.yaml`
- Modify (regenerate): `public-rest/generated/**`
- Create: `public-rest/src/main/java/.../WorkflowApiController.java`
- Test: `public-rest/src/test/java/.../WorkflowApiControllerTest.java` (create)

**Interfaces:**
- Consumes: `Error` schema (Task 2).
- Produces: `WorkflowModel` with `uuid`, `label`, `description`, `version`, `definition` (string).

**The commitment this task must not make.** `definition` is a **string**, not an object. Declaring it
as structured JSON would publish the workflow-definition schema by accident and leave slice 5 with
nothing to decide. Keep it `type: "string"` with a description saying the shape is not part of the
`v1` contract.

Resolution path: `ProjectWorkflowService.getLastPublishedWorkflowId(String workflowUuid)` maps the
public uuid to the atlas workflow id; `ProjectWorkflowService.getProjectWorkflows(null, workflowUuid)`
gives the owning project id for the permission check. Call the guarded
`projectFacade.getProject(projectId)` before reading the workflow, for the same reason as Task 6.

- [ ] **Step 1: Add the path and schema**

```yaml
  - name: "workflow"
    description: "The Automation Workflow Public API"
```

```yaml
  /workflows/{uuid}:
    get:
      description: "Get a workflow by uuid, including its definition."
      summary: "Get a workflow"
      tags:
        - "workflow"
      operationId: "getWorkflow"
      parameters:
        - name: "uuid"
          description: "The uuid of a workflow."
          in: "path"
          required: true
          schema:
            type: "string"
            format: "uuid"
      responses:
        "200":
          description: "The workflow."
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Workflow"
        "401":
          $ref: "#/components/responses/UnauthorizedError"
        "403":
          $ref: "#/components/responses/ForbiddenError"
        "404":
          $ref: "#/components/responses/NotFoundError"
      security:
        - bearerAuth: [ ]
```

```yaml
    Workflow:
      type: "object"
      description: "A workflow belonging to an automation project."
      required:
        - "uuid"
      properties:
        uuid:
          description: "The uuid of the workflow."
          type: "string"
          format: "uuid"
        label:
          description: "The label of the workflow."
          type: "string"
        description:
          description: "The description of the workflow."
          type: "string"
        version:
          description: "The project version this workflow belongs to."
          type: "integer"
        definition:
          description: "The workflow definition, as a JSON document. Its internal shape is NOT part of\
            \ the v1 contract and may change without a version bump -- treat it as opaque text, safe to\
            \ store and diff but not to parse against a fixed schema."
          type: "string"
```

- [ ] **Step 2: Regenerate**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:generateOpenAPISpring
```

- [ ] **Step 3: Write the failing test**

```java
package com.bytechef.ee.automation.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowModel;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class WorkflowApiControllerTest {

    private static final UUID WORKFLOW_UUID = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    private final ProjectFacade projectFacade = mock(ProjectFacade.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final WorkflowApiController workflowApiController =
        new WorkflowApiController(projectFacade, projectWorkflowService, workflowService);

    @Test
    void testGetWorkflowReturnsTheDefinitionAsAnOpaqueString() {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow();

        projectWorkflow.setProjectId(5L);
        projectWorkflow.setUuid(WORKFLOW_UUID);

        when(projectWorkflowService.getProjectWorkflows(null, WORKFLOW_UUID.toString()))
            .thenReturn(List.of(projectWorkflow));
        when(projectWorkflowService.getLastPublishedWorkflowId(WORKFLOW_UUID.toString()))
            .thenReturn("wf-1");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn("Nightly sync");
        when(workflow.getDefinition()).thenReturn("{\"tasks\":[]}");
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);

        ResponseEntity<WorkflowModel> responseEntity = workflowApiController.getWorkflow(WORKFLOW_UUID.toString());

        WorkflowModel workflowModel = responseEntity.getBody();

        assertThat(workflowModel).isNotNull();
        assertThat(workflowModel.getLabel()).isEqualTo("Nightly sync");
        assertThat(workflowModel.getDefinition()).isEqualTo("{\"tasks\":[]}");
    }

    @Test
    void testGetWorkflowChecksTheOwningProjectBeforeReadingTheWorkflow() {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow();

        projectWorkflow.setProjectId(5L);
        projectWorkflow.setUuid(WORKFLOW_UUID);

        when(projectWorkflowService.getProjectWorkflows(null, WORKFLOW_UUID.toString()))
            .thenReturn(List.of(projectWorkflow));
        when(projectWorkflowService.getLastPublishedWorkflowId(WORKFLOW_UUID.toString()))
            .thenReturn("wf-1");
        when(workflowService.getWorkflow("wf-1")).thenReturn(mock(Workflow.class));

        workflowApiController.getWorkflow(WORKFLOW_UUID.toString());

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(projectFacade, workflowService);

        inOrder.verify(projectFacade)
            .getProject(5L);
        inOrder.verify(workflowService)
            .getWorkflow("wf-1");
    }
}
```

- [ ] **Step 4: Run to verify failure**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:test --tests '*WorkflowApiControllerTest*'
```

Expected: FAIL to compile.

- [ ] **Step 5: Write the controller**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.WorkflowModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read endpoint for a single workflow.
 *
 * <p>
 * {@code definition} is returned as an opaque string on purpose. Publishing it as structured,
 * schema-declared JSON would freeze the workflow-definition contract as a side effect of a read
 * endpoint; that commitment belongs to its own slice, made deliberately.
 *
 * <p>
 * {@link ProjectWorkflowService} and {@link WorkflowService} are unguarded trusted-caller services,
 * so the guarded {@code projectFacade.getProject(projectId)} runs first and supplies the permission
 * check.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.configuration.public_.web.rest.WorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class WorkflowApiController implements WorkflowApi {

    private final ProjectFacade projectFacade;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public WorkflowApiController(
        ProjectFacade projectFacade, ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.projectFacade = projectFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public ResponseEntity<WorkflowModel> getWorkflow(String uuid) {
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(null, uuid);

        if (projectWorkflows.isEmpty()) {
            throw new NoSuchElementException("Workflow not found");
        }

        ProjectWorkflow projectWorkflow = projectWorkflows.getFirst();

        projectFacade.getProject(projectWorkflow.getProjectId());

        Workflow workflow = workflowService.getWorkflow(projectWorkflowService.getLastPublishedWorkflowId(uuid));

        return ResponseEntity.ok(
            new WorkflowModel()
                .uuid(Objects.requireNonNull(projectWorkflow.getUuid())
                    .toString())
                .label(workflow.getLabel())
                .description(workflow.getDescription())
                .version(projectWorkflow.getProjectVersion())
                .definition(workflow.getDefinition()));
    }
}
```

Add `implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))` to
`public-rest/build.gradle.kts` if it is not already present — it is, as of this writing.

- [ ] **Step 6: Run tests to verify they pass, then commit**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:check
./gradlew spotlessApply
git add server/ee/libs/automation/automation-configuration/automation-configuration-public-rest
git commit -m "Add the public workflow read endpoint to the automation API"
```

---

### Task 8: `GET /project-deployments` and `GET /project-deployments/{uuid}`

**Files:**
- Modify: `public-rest/openapi.yaml`
- Modify (regenerate): `public-rest/generated/**`
- Create: `public-rest/src/main/java/.../ProjectDeploymentApiController.java`
- Test: `public-rest/src/test/java/.../ProjectDeploymentApiControllerTest.java` (create)

**Interfaces:**
- Consumes: `ProjectDeploymentService#getProjectDeployment(UUID)` from Task 3.
- Produces: `ProjectDeploymentModel` with `uuid`, `name`, `description`, `enabled`, `environment`,
  `projectUuid`, `projectVersion`.

Both facade methods are already guarded:
`getWorkspaceProjectDeployments` on `hasPermission(#id, 'Workspace', 'DEPLOYMENT_VIEW')` and
`getProjectDeployment(long)` on `hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_VIEW')` — note
the second targets the deployment itself, not its workspace, so no derived lookup is needed.

- [ ] **Step 1: Add the paths and schema**

Tag:

```yaml
  - name: "project-deployment"
    description: "The Automation Project Deployment Public API"
```

Paths:

```yaml
  /project-deployments:
    get:
      description: "List the project deployments in a workspace."
      summary: "List project deployments"
      tags:
        - "project-deployment"
      operationId: "getProjectDeployments"
      parameters:
        - name: "workspaceId"
          description: "The id of a workspace, as returned by GET /workspaces."
          in: "query"
          required: true
          schema:
            type: "integer"
            format: "int64"
        - name: "environment"
          description: "Restrict to one environment."
          in: "query"
          required: false
          schema:
            type: "string"
            enum:
              - "DEVELOPMENT"
              - "STAGING"
              - "PRODUCTION"
        - name: "projectUuid"
          description: "Restrict to deployments of one project."
          in: "query"
          required: false
          schema:
            type: "string"
            format: "uuid"
        - name: "enabled"
          description: "Restrict to enabled or disabled deployments."
          in: "query"
          required: false
          schema:
            type: "boolean"
      responses:
        "200":
          description: "The list of project deployments."
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/ProjectDeployment"
        "401":
          $ref: "#/components/responses/UnauthorizedError"
        "403":
          $ref: "#/components/responses/ForbiddenError"
      security:
        - bearerAuth: [ ]
  /project-deployments/{uuid}:
    get:
      description: "Get a project deployment by uuid."
      summary: "Get a project deployment"
      tags:
        - "project-deployment"
      operationId: "getProjectDeployment"
      parameters:
        - name: "uuid"
          description: "The uuid of a project deployment."
          in: "path"
          required: true
          schema:
            type: "string"
            format: "uuid"
      responses:
        "200":
          description: "The project deployment."
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ProjectDeployment"
        "401":
          $ref: "#/components/responses/UnauthorizedError"
        "403":
          $ref: "#/components/responses/ForbiddenError"
        "404":
          $ref: "#/components/responses/NotFoundError"
      security:
        - bearerAuth: [ ]
```

Schema:

```yaml
    ProjectDeployment:
      type: "object"
      description: "A deployment of an automation project into one environment."
      required:
        - "uuid"
        - "enabled"
      properties:
        uuid:
          description: "The uuid of the deployment."
          type: "string"
          format: "uuid"
        name:
          description: "The name of the deployment."
          type: "string"
        description:
          description: "The description of the deployment."
          type: "string"
        enabled:
          description: "Whether the deployment is enabled."
          type: "boolean"
        environment:
          description: "The environment the deployment runs in."
          type: "string"
          enum:
            - "DEVELOPMENT"
            - "STAGING"
            - "PRODUCTION"
        projectUuid:
          description: "The uuid of the deployed project."
          type: "string"
          format: "uuid"
        projectVersion:
          description: "The deployed project version."
          type: "integer"
```

- [ ] **Step 2: Regenerate**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:generateOpenAPISpring
```

- [ ] **Step 3: Write the failing test**

```java
    @Test
    void testGetProjectDeploymentResolvesTheUuidAndDelegatesWithTheNumericId() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(9L);
        projectDeployment.setUuid(DEPLOYMENT_UUID);

        when(projectDeploymentService.getProjectDeployment(DEPLOYMENT_UUID)).thenReturn(projectDeployment);

        projectDeploymentApiController.getProjectDeployment(DEPLOYMENT_UUID.toString());

        verify(projectDeploymentFacade).getProjectDeployment(9L);
    }

    @Test
    void testGetProjectDeploymentsPassesTheWorkspaceIdThrough() {
        when(projectDeploymentFacade.getWorkspaceProjectDeployments(3L, null, null, null, null, null))
            .thenReturn(List.of());

        ResponseEntity<List<ProjectDeploymentModel>> responseEntity =
            projectDeploymentApiController.getProjectDeployments(3L, null, null, null);

        assertThat(responseEntity.getBody()).isEmpty();
    }
```

Read the real `getWorkspaceProjectDeployments` signature before writing the stub — it has several
overloads and the parameter order matters. Adjust the argument list to match; do not change the
facade.

- [ ] **Step 4: Run to verify failure**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:test --tests '*ProjectDeploymentApiControllerTest*'
```

Expected: FAIL to compile — `ProjectDeploymentApiController` does not exist.

- [ ] **Step 5: Write the controller**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.public_.web.rest.model.ProjectDeploymentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read endpoints for project deployments.
 *
 * <p>
 * Both facade methods are already guarded -- {@code getWorkspaceProjectDeployments} on
 * {@code hasPermission(#id, 'Workspace', 'DEPLOYMENT_VIEW')} and {@code getProjectDeployment(long)}
 * on {@code hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_VIEW')}. Note the second targets the
 * deployment's own id, not its workspace, so resolving the uuid is a single hop with no derived
 * lookup.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.configuration.public_.web.rest.ProjectDeploymentApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ProjectDeploymentApiController implements ProjectDeploymentApi {

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI2")
    public ProjectDeploymentApiController(
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectService projectService) {

        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @Override
    public ResponseEntity<List<ProjectDeploymentModel>> getProjectDeployments(
        Long workspaceId, @Nullable String environment, @Nullable String projectUuid, @Nullable Boolean enabled) {

        Long projectId = projectUuid == null
            ? null
            : projectService.getProject(UUID.fromString(projectUuid))
                .getId();

        List<ProjectDeploymentModel> projectDeploymentModels =
            projectDeploymentFacade.getWorkspaceProjectDeployments(workspaceId, environment, projectId, null, enabled)
                .stream()
                .map(this::toModel)
                .toList();

        return ResponseEntity.ok(projectDeploymentModels);
    }

    @Override
    public ResponseEntity<ProjectDeploymentModel> getProjectDeployment(String uuid) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(UUID.fromString(uuid));

        return ResponseEntity.ok(
            toModel(projectDeploymentFacade.getProjectDeployment(Objects.requireNonNull(projectDeployment.getId()))));
    }

    private ProjectDeploymentModel toModel(ProjectDeploymentDTO projectDeploymentDTO) {
        return new ProjectDeploymentModel()
            .uuid(Objects.requireNonNull(projectDeploymentDTO.uuid())
                .toString())
            .name(projectDeploymentDTO.name())
            .description(projectDeploymentDTO.description())
            .enabled(projectDeploymentDTO.enabled())
            .projectVersion(projectDeploymentDTO.projectVersion());
    }
}
```

`getWorkspaceProjectDeployments` has several overloads and the real parameter order may differ from
the call above — read the signature in `ProjectDeploymentFacade` and adapt the call site. Do not
change the facade. Likewise, if `ProjectDeploymentDTO` does not expose `uuid()`, read the uuid from
the `ProjectDeployment` row instead; do not add a field to the DTO.

- [ ] **Step 6: Run tests, then commit**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:check
./gradlew spotlessApply
git add server/ee/libs/automation/automation-configuration/automation-configuration-public-rest
git commit -m "Add public project deployment read endpoints to the automation API"
```

---

### Task 9: `pageSize` and uuid filters on `/workflow-executions`

**Files:**
- Modify: `public-rest/openapi.yaml`
- Modify (regenerate): `public-rest/generated/**`
- Modify: `public-rest/src/main/java/.../WorkflowExecutionApiController.java`
- Modify: `public-rest/src/test/java/.../WorkflowExecutionApiControllerTest.java`

**Interfaces:**
- Consumes: `ProjectService#getProject(UUID)` (exists) and
  `ProjectDeploymentService#getProjectDeployment(UUID)` (Task 3).
- Produces: nothing later tasks depend on.

This is the only task touching an already-published operation, so everything here is **additive**:
new optional parameters, no removals, no changed types. `projectId` and `projectDeploymentId` stay
and keep working.

- [ ] **Step 1: Add the parameters to `openapi.yaml`**

On `/workflow-executions`, add:

```yaml
        - name: "projectUuid"
          description: "Restrict to one project, by uuid. Prefer this over projectId."
          in: "query"
          required: false
          schema:
            type: "string"
            format: "uuid"
        - name: "projectDeploymentUuid"
          description: "Restrict to one deployment, by uuid. Prefer this over projectDeploymentId."
          in: "query"
          required: false
          schema:
            type: "string"
            format: "uuid"
        - name: "pageSize"
          description: "The number of elements per page. Defaults to 20, maximum 100."
          in: "query"
          required: false
          schema:
            type: "integer"
            format: "int32"
```

Mark the two numeric filters deprecated by adding `deprecated: true` to each, and extend their
descriptions with "Deprecated: use projectUuid." / "Deprecated: use projectDeploymentUuid."

Add the same `pageSize` parameter to `/project-deployments` from Task 8.

- [ ] **Step 2: Regenerate**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:generateOpenAPISpring
```

- [ ] **Step 3: Write the failing tests**

```java
    @Test
    void testProjectUuidFilterResolvesToTheNumericProjectId() {
        Project project = new Project();

        project.setId(5L);

        when(projectService.getProject(PROJECT_UUID)).thenReturn(project);

        workflowExecutionApiController.getWorkflowExecutionsPage(
            3L, null, null, null, null, null, null, PROJECT_UUID.toString(), null, null, 0, null);

        verify(projectWorkflowExecutionFacade).getWorkflowExecutions(
            null, 2L, null, null, null, 5L, null, null, 3L, 0);
    }

    @Test
    void testBothProjectFiltersSuppliedIsRejected() {
        assertThatThrownBy(() -> workflowExecutionApiController.getWorkflowExecutionsPage(
            3L, null, null, null, null, 7L, null, PROJECT_UUID.toString(), null, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId");
    }

    @Test
    void testPageSizeAboveTheMaximumIsRejected() {
        assertThatThrownBy(() -> workflowExecutionApiController.getWorkflowExecutionsPage(
            3L, null, null, null, null, null, null, null, null, null, 0, 500))
                .isInstanceOf(IllegalArgumentException.class);
    }
```

Adjust the argument lists to the regenerated method signature — the generator orders parameters as
they appear in the spec. The second test matters: accepting both a numeric and a uuid filter for the
same resource, with different values, is ambiguous and must be an error rather than a silent
precedence rule.

- [ ] **Step 4: Run to verify failure**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:test --tests '*WorkflowExecutionApiControllerTest*'
```

- [ ] **Step 5: Implement**

In `getWorkflowExecutionsPage`, before building the facade call:

```java
        Long resolvedProjectId = resolveFilterId(
            "projectId", projectId, "projectUuid", projectUuid,
            uuid -> projectService.getProject(uuid)
                .getId());
        Long resolvedProjectDeploymentId = resolveFilterId(
            "projectDeploymentId", projectDeploymentId, "projectDeploymentUuid", projectDeploymentUuid,
            uuid -> projectDeploymentService.getProjectDeployment(uuid)
                .getId());

        if (pageSize != null && (pageSize < 1 || pageSize > MAX_PAGE_SIZE)) {
            throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
```

with:

```java
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Accepts either the deprecated numeric filter or its uuid replacement, never both. Supplying
     * both is rejected rather than resolved by precedence: a caller migrating from one to the other
     * would otherwise get silently different results depending on which the server happens to prefer.
     */
    private static @Nullable Long resolveFilterId(
        String numericName, @Nullable Long numericValue, String uuidName, @Nullable String uuidValue,
        Function<UUID, Long> resolver) {

        if (numericValue != null && uuidValue != null) {
            throw new IllegalArgumentException("Supply either " + numericName + " or " + uuidName + ", not both");
        }

        if (uuidValue != null) {
            return resolver.apply(UUID.fromString(uuidValue));
        }

        return numericValue;
    }
```

Pass `resolvedProjectId` and `resolvedProjectDeploymentId` to the facade in place of the raw values.
Thread `pageSize` through if the facade accepts it; if it does not, leave the parameter declared and
validated but note in the endpoint description that it is not yet honoured, and raise it as a
follow-up rather than changing the facade signature in this slice.

- [ ] **Step 6: Run tests, then commit**

```bash
./gradlew :server:ee:libs:automation:automation-configuration:automation-configuration-public-rest:check
./gradlew spotlessApply
git add server/ee/libs/automation/automation-configuration/automation-configuration-public-rest
git commit -m "Add uuid filters and a page size to the public workflow executions endpoint"
```

---

### Task 10: Move `POST /projects/deploy` onto a scope check

**Files:**
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/facade/ProjectCodeWorkflowFacadeImpl.java`
- Test: same module's test source set — `ProjectCodeWorkflowFacadeAuthorizationIntTest.java` (create)

**Interfaces:**
- Consumes: nothing.
- Produces: nothing later tasks depend on. This is the last inconsistency on the surface.

`ProjectCodeWorkflowFacadeImpl#save(long workspaceId, byte[] bytes, Language language)` is guarded
`hasAuthority(ADMIN)` while its neighbour `ProjectGitFacadeImpl#pullProjectFromGit` uses
`hasPermission(#projectId, 'Project', 'DEPLOYMENT_PULL')`. The `workspaceId` the deploy acts on
arrives from the caller and nothing checks it. `PROJECT_CREATE` on `Workspace` already exists and is
used in four places, including `ProjectFacadeImpl#createProject`.

- [ ] **Step 1: Write the failing test**

```java
    @Test
    void testSaveDeniedForAWorkspaceTheCallerCannotCreateProjectsIn() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThatThrownBy(() -> projectCodeWorkflowFacade.save(1L, new byte[0], Language.JAVASCRIPT))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testSaveNoLongerAcceptsAdminAuthorityAlone() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertThatThrownBy(() -> projectCodeWorkflowFacade.save(1L, new byte[0], Language.JAVASCRIPT))
            .isInstanceOf(AccessDeniedException.class);
    }
```

Follow the `@SpringBootTest(classes = …Config.class)` + `@EnableMethodSecurity` shape used by
`AutomationProjectCodeWorkflowApiControllerListAuthorizationIntTest`, with a mocked
`PermissionEvaluator` bean returning false. Place the test outside any component-scanned controller
package.

The second test is the behaviour change Risk 4 in the spec names: an admin with no `PROJECT_CREATE`
in the target workspace loses access they had. Pinning it means the change cannot be quietly
reverted.

- [ ] **Step 2: Run to verify failure**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration --tests '*ProjectCodeWorkflowFacadeAuthorizationIntTest*'
```

Expected: the second test FAILS — `ROLE_ADMIN` currently satisfies the guard.

- [ ] **Step 3: Change the guard**

```java
    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'PROJECT_CREATE')")
    public void save(long workspaceId, byte[] bytes, Language language) {
```

Replace the Javadoc's admin rationale with the new one:

```java
    /**
     * Deploying a code workflow loads and executes the uploaded artifact on the server. The gate is
     * {@code PROJECT_CREATE} on the target workspace rather than a blanket {@code ROLE_ADMIN}: the
     * workspace id arrives from the caller, so a role check would authorize the verb while leaving
     * the object unchecked. This also matches {@code ProjectGitFacadeImpl#pullProjectFromGit}, the
     * only other guarded operation on this surface.
     */
```

- [ ] **Step 4: Run tests, then commit**

```bash
./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:check :server:libs:automation:automation-configuration:automation-configuration-service:testIntegration
./gradlew spotlessApply
git add server/libs/automation/automation-configuration
git commit -m "Guard the public project deploy on a workspace scope instead of the admin role"
```

---

### Task 11: CLI commands

**Files:**
- Modify: `cli/clients/automation-configuration/build.gradle.kts`
- Delete: `cli/clients/automation-configuration/openapi.yaml`
- Modify (regenerate): `cli/clients/automation-configuration/generated/**`
- Modify: `cli-automation/src/main/java/com/bytechef/cli/command/automation/AutomationClientFactory.java`
- Create: `cli-automation/src/main/java/com/bytechef/cli/command/automation/AutomationWorkspaceCommand.java`
- Create: `cli-automation/src/main/java/com/bytechef/cli/command/automation/AutomationWorkflowCommand.java`
- Create: `cli-automation/src/main/java/com/bytechef/cli/command/automation/AutomationDeploymentCommand.java`
- Modify: `cli-automation/src/main/java/com/bytechef/cli/command/automation/AutomationProjectCommand.java`
- Test: `cli-automation/src/test/java/com/bytechef/cli/command/automation/*CommandTest.java`

**Interfaces:**
- Consumes: every endpoint from Tasks 4–9.
- Produces: the commands listed in §8 of the spec.

- [ ] **Step 1: Repoint the client at the server spec**

`cli/clients/automation-configuration/build.gradle.kts` line 9 reads
`inputSpec.set("$projectDir/openapi.yaml")` — a vendored copy, unlike the three embedded clients
which reference the server module. Change it to:

```kotlin
    inputSpec.set("${rootDir}/server/ee/libs/automation/automation-configuration/automation-configuration-public-rest/openapi.yaml")
```

Then delete `cli/clients/automation-configuration/openapi.yaml`. The copy is byte-identical to the
server spec as of the start of this slice, so nothing changes behaviourally — but leaving it would
mean every endpoint added in Tasks 4–9 is invisible to the CLI.

- [ ] **Step 2: Regenerate and verify the new APIs appeared**

```bash
./gradlew :cli:clients:automation-configuration:generateClient
ls cli/clients/automation-configuration/generated/src/main/java/com/bytechef/cli/client/automationconfiguration/api/
```

Expected: `WorkspaceApi.java`, `ProjectApi.java`, `WorkflowApi.java`, `ProjectDeploymentApi.java`
alongside the existing ones. Use `generateClient`, never the plugin's default `openApiGenerate`,
which is unconfigured and fails with "generator name must be specified".

- [ ] **Step 3: Write the failing command test**

```java
    @Test
    void testWorkspaceListHitsTheAutomationV1Surface() throws Exception {
        try (StubApi stub = StubApi.start(200, "[]")) {
            int code = CliApplication.execute(new String[] {
                "automation", "workspace", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                "PRODUCTION"
            });

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/automation/v1/workspaces"),
                "expected /api/automation/v1/workspaces but was " + stub.lastPath());
        }
    }
```

Mirror `EmbeddedCodeWorkflowCommandTest`'s use of `StubApi`. Write one such test per new command.

- [ ] **Step 4: Run to verify failure**

```bash
./gradlew :cli:commands:automation:test
```

Expected: FAIL — the command does not exist, so `CliApplication.execute` returns a non-zero
unknown-command code.

- [ ] **Step 5: Add the factory methods and the commands**

To `AutomationClientFactory`, one accessor per generated API, following the existing shape:

```java
    static WorkspaceApi workspaceApi(CliConfig config) {
        return new WorkspaceApi(apiClient(config));
    }
```

Then the command classes. `AutomationWorkspaceCommand`:

```java
@org.springframework.stereotype.Component
public class AutomationWorkspaceCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();

    @Command(name = "automation workspace list", description = "List the workspaces you belong to.")
    public void workspaceList(
        @Option(longName = "output", defaultValue = "json") String output,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        try {
            new OutputRenderer(System.out).render(
                AutomationClientFactory.workspaceApi(config)
                    .getWorkspaces(),
                output);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    private CliConfig resolve(String profile, String host, String token, String environment) {
        return AutomationSupport.resolve(configPath, environmentVariables, profile, host, token, environment);
    }
}
```

Follow the same template for `automation project list|get|versions|workflows`,
`automation workflow get`, and `automation deployment list|get`. Collection commands read
`--workspace-id` from the profile via the resolved `CliConfig`; add an explicit `--workspace-id`
option that overrides it, matching how `--environment` already works.

If `AutomationSupport` does not exist, mirror `EmbeddedSupport` in `cli-embedded` — do not duplicate
its body inline in each command.

- [ ] **Step 6: Translate the new 401 into an actionable message**

Risk 1 in the spec: a profile holding an admin key now fails against every automation command.
In `AutomationClientFactory.toCliException`, extend the 401/403 branch:

```java
        if (status == 401 || status == 403) {
            return new CliException(
                2,
                "Authentication failed (HTTP " + status
                    + "). The automation API requires an API key of type AUTOMATION; an Admin API Key is rejected.");
        }
```

- [ ] **Step 7: Run tests, then commit**

```bash
./gradlew :cli:commands:automation:check :cli:cli-app:check
./gradlew spotlessApply
git add cli
git commit -m "Add automation read commands to the CLI"
```

---

### Task 12: Documentation

**Files:**
- Modify: `docs/content/docs/openapi/meta.json`
- Modify: `docs/content/docs/openapi/index.mdx`
- Modify: `cli/README.md`

**Interfaces:**
- Consumes: the tags added in Tasks 4–8.
- Produces: nothing.

- [ ] **Step 1: Add the new reference pages to the navigation**

Page names are `<specKey>-<tag>`; the automation spec's key is `automation`. Add to `meta.json`'s
`pages` array, in the `--- Automation ---` group, after the existing three:

```json
    "automation-workspace",
    "automation-project",
    "automation-workflow",
    "automation-project-deployment",
```

- [ ] **Step 2: Add the bullets to `index.mdx`**

In the Automation section:

```markdown
- **[Workspaces](/openapi/automation-workspace)** — the workspaces you belong to. Start here: every collection endpoint takes a `workspaceId`.
- **[Projects](/openapi/automation-project)** — list and inspect projects, their versions and their workflows.
- **[Workflows](/openapi/automation-workflow)** — a single workflow, including its definition.
- **[Deployments](/openapi/automation-project-deployment)** — list and inspect project deployments.
```

- [ ] **Step 3: Update the authentication table in `index.mdx`**

The row currently reads "Automation (all three) | A workspace **API Key**." Replace with:

```markdown
| Automation | An **API Key** of type Automation. Admin API Keys are rejected — they are reserved for `/api/platform/v1`. |
```

Also update the "Base URLs" table row, which says "Automation (all three)", since there are now
seven groups.

- [ ] **Step 4: Document the new CLI commands in `cli/README.md`**

Add an "Automation read commands" subsection listing each command with an example invocation,
following the existing sections' style, and state that the profile token must be an Automation API
Key.

- [ ] **Step 5: Commit**

```bash
git add docs cli/README.md
git commit -m "docs - Document the automation public read API and its CLI commands"
```

---
