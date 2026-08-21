# T4 Tenant-ID Validation / SQLi Prevention Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent SQL injection through tenant identifiers by validating them against a strict charset at the central `TenantContext` chokepoint, with defense-in-depth at the two `SET search_path` sinks, fail-fast at the token parsers, and a 400 guard at the remote tenant filter.

**Architecture:** A new CE `TenantIdValidator` (`^[a-zA-Z0-9_]+$`) is enforced inside `TenantContext.setCurrentTenantId` — through which every set/run/call path funnels — so a malformed tenant id can never reach a sink. The raw-SQL sinks, the two base64 token parsers, the remote filter, and the EE `TenantRepository` all route through the same validator.

**Tech Stack:** Java 25, JDK regex (`java.util.regex.Pattern`), JUnit 5 + Mockito + AssertJ.

## Global Constraints

- Files under `server/ee/` use the ByteChef Enterprise license header and a `@version ee` Javadoc tag; files under `server/libs/` use the Apache 2.0 header. (Spotless picks the header by file content.)
- Run `./gradlew spotlessApply` before every commit; per-module `:check` is the gate.
- Blank line before control statements and after a variable modification a later statement uses (Java style rules in CLAUDE.md).
- Test method names are camelCase without underscores; unit test classes end in `Test`.
- Commit messages: server-side `gecko <description>`; end every commit body with the `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer.
- Branch is `0_732`, the user commits in parallel — never `git commit --amend`; make fresh commits and stage only files this plan touches.
- Canonical tenant-id / schema charset: `^[a-zA-Z0-9_]+$` (admits `public` and zero-padded numerics like `000001`; excludes every injection metacharacter; no hyphen).

---

## File Structure

**Task 1 — CE validator:**
- Create: `server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/TenantIdValidator.java`
- Test: `server/libs/core/tenant/tenant-api/src/test/java/com/bytechef/tenant/TenantIdValidatorTest.java`

**Task 2 — central chokepoint:**
- Modify: `server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/TenantContext.java`
- Test: `server/libs/core/tenant/tenant-api/src/test/java/com/bytechef/tenant/TenantContextTenantIdValidationTest.java`

**Task 3 — CE SQL sink:**
- Modify: `server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/sql/BaseDataSource.java`

**Task 4 — token parsers:**
- Modify: `server/libs/platform/platform-api/src/main/java/com/bytechef/platform/workflow/WorkflowExecutionId.java`
- Modify: `server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/JobResumeId.java`
- Test: `.../platform-api/src/test/java/com/bytechef/platform/workflow/WorkflowExecutionIdTenantValidationTest.java`
- Test: `.../platform-workflow-execution-api/src/test/java/com/bytechef/platform/workflow/execution/JobResumeIdTenantValidationTest.java`

**Task 5 — EE SQL sink:**
- Modify: `server/ee/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/ee/platform/scheduler/tenant/MultiTenantDriverDelegate.java`

**Task 6 — EE remote filter:**
- Modify: `server/ee/libs/core/remote/remote-rest/src/main/java/com/bytechef/ee/remote/web/filter/RemoteMultiTenantFilter.java`
- Test: `server/ee/libs/core/remote/remote-rest/src/test/java/com/bytechef/ee/remote/web/filter/RemoteMultiTenantFilterTest.java`

**Task 7 — EE validator consolidation:**
- Modify: `server/ee/libs/core/tenant/tenant-multi-service/src/main/java/com/bytechef/ee/tenant/repository/TenantRepository.java`

**Task 8 — close out:**
- Modify: `gecko-remediation-tasks.md`

---

## Task 1: CE `TenantIdValidator`

**Files:** see File Structure, Task 1.

**Interfaces:**
- Produces:
  - `TenantIdValidator.isValid(String tenantId)` → boolean.
  - `TenantIdValidator.validate(String tenantId)` → void, throws `IllegalArgumentException` if null/blank or not matching `^[a-zA-Z0-9_]+$`.
  - `TenantIdValidator.validateDatabaseSchema(String schema)` → void, same rule, schema-worded message.

- [ ] **Step 1: Write the failing test**

Create `server/libs/core/tenant/tenant-api/src/test/java/com/bytechef/tenant/TenantIdValidatorTest.java`:

```java
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

package com.bytechef.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TenantIdValidatorTest {

    @Test
    void testValidTenantIds() {
        assertThat(TenantIdValidator.isValid("public")).isTrue();
        assertThat(TenantIdValidator.isValid("000001")).isTrue();
        assertThat(TenantIdValidator.isValid("tenant_1")).isTrue();
    }

    @Test
    void testInvalidTenantIds() {
        assertThat(TenantIdValidator.isValid("public\"; DROP TABLE users; --")).isFalse();
        assertThat(TenantIdValidator.isValid("a b")).isFalse();
        assertThat(TenantIdValidator.isValid("a;b")).isFalse();
        assertThat(TenantIdValidator.isValid("a-b")).isFalse();
        assertThat(TenantIdValidator.isValid("")).isFalse();
        assertThat(TenantIdValidator.isValid(null)).isFalse();
    }

    @Test
    void testValidateThrowsOnInjection() {
        assertThatThrownBy(() -> TenantIdValidator.validate("public\"; DROP TABLE users; --"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testValidatePassesForValid() {
        assertThatCode(() -> TenantIdValidator.validate("000001")).doesNotThrowAnyException();
    }

    @Test
    void testValidateDatabaseSchema() {
        assertThatCode(() -> TenantIdValidator.validateDatabaseSchema("bytechef_000001")).doesNotThrowAnyException();
        assertThatCode(() -> TenantIdValidator.validateDatabaseSchema("public")).doesNotThrowAnyException();
        assertThatThrownBy(() -> TenantIdValidator.validateDatabaseSchema("bytechef_x\"; DROP"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails to compile**

Run: `./gradlew :server:libs:core:tenant:tenant-api:compileTestJava`
Expected: FAIL — `TenantIdValidator` not found.

- [ ] **Step 3: Create `TenantIdValidator`**

Create `.../tenant/TenantIdValidator.java`:

```java
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

package com.bytechef.tenant;

import java.util.regex.Pattern;

/**
 * Validates tenant identifiers and tenant-derived database schema names against a strict, SQL-identifier-safe charset
 * ({@code ^[a-zA-Z0-9_]+$}). Tenant ids flow into {@code SET search_path} statements that PostgreSQL cannot
 * parameterize, so whitelisting the charset is the SQL-injection mitigation.
 *
 * @author Ivica Cardic
 */
public final class TenantIdValidator {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private TenantIdValidator() {
    }

    public static boolean isValid(String tenantId) {
        return tenantId != null && VALID_PATTERN.matcher(tenantId)
            .matches();
    }

    public static void validate(String tenantId) {
        if (!isValid(tenantId)) {
            throw new IllegalArgumentException(
                "Invalid tenant ID. Must contain only alphanumeric characters and underscores.");
        }
    }

    public static void validateDatabaseSchema(String schema) {
        if (!isValid(schema)) {
            throw new IllegalArgumentException(
                "Invalid database schema name '" + schema + "'. Must contain only alphanumeric characters and " +
                    "underscores.");
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:libs:core:tenant:tenant-api:test --tests "*TenantIdValidatorTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Format and commit**

Run: `./gradlew :server:libs:core:tenant:tenant-api:spotlessApply`

```bash
git add server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/TenantIdValidator.java \
        server/libs/core/tenant/tenant-api/src/test/java/com/bytechef/tenant/TenantIdValidatorTest.java
git commit -m "gecko Add shared TenantIdValidator (T4)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Enforce at the `TenantContext` chokepoint

**Files:** `TenantContext.java` (+ new test).

**Interfaces:**
- Consumes: `TenantIdValidator.validate(String)`.
- Produces: `TenantContext.setCurrentTenantId(String)` now throws `IllegalArgumentException` for a malformed id (in addition to the existing null check). `runWithTenantId` propagates it; `callWithTenantId` wraps it in `RuntimeException` (its existing catch behavior).

- [ ] **Step 1: Write the failing test**

Create `server/libs/core/tenant/tenant-api/src/test/java/com/bytechef/tenant/TenantContextTenantIdValidationTest.java`:

```java
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

package com.bytechef.tenant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TenantContextTenantIdValidationTest {

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testSetCurrentTenantIdAcceptsValid() {
        assertThatCode(() -> TenantContext.setCurrentTenantId("000001")).doesNotThrowAnyException();
        assertThatCode(() -> TenantContext.setCurrentTenantId("public")).doesNotThrowAnyException();
    }

    @Test
    void testSetCurrentTenantIdRejectsInjection() {
        assertThatThrownBy(() -> TenantContext.setCurrentTenantId("public\"; DROP TABLE users; --"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRunWithTenantIdRejectsInjection() {
        assertThatThrownBy(() -> TenantContext.runWithTenantId("a;b", () -> {
        })).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCallWithTenantIdRejectsInjection() {
        // callWithTenantId wraps thrown exceptions in RuntimeException (pre-existing behavior).
        assertThatThrownBy(() -> TenantContext.callWithTenantId("a;b", () -> "x"))
            .isInstanceOf(RuntimeException.class);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:libs:core:tenant:tenant-api:test --tests "*TenantContextTenantIdValidationTest"`
Expected: FAIL — injection ids are currently accepted.

- [ ] **Step 3: Add validation to `setCurrentTenantId`**

In `TenantContext.java`, replace:
```java
    public static void setCurrentTenantId(String tenantId) {
        Assert.notNull(tenantId, "tenantId must not be null");

        currentTenant.set(tenantId);

        MDC.put("tenantId", tenantId);
    }
```
with:
```java
    public static void setCurrentTenantId(String tenantId) {
        Assert.notNull(tenantId, "tenantId must not be null");

        TenantIdValidator.validate(tenantId);

        currentTenant.set(tenantId);

        MDC.put("tenantId", tenantId);
    }
```
(`TenantIdValidator` is in the same package — no import needed.)

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:libs:core:tenant:tenant-api:test --tests "*TenantContextTenantIdValidationTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Run the full tenant-api test suite (guard against over-strict rejection)**

Run: `./gradlew :server:libs:core:tenant:tenant-api:test`
Expected: PASS. If any existing test set a tenant id outside the charset, that is a real signal — confirm the id is illegitimate before adjusting the test; do not loosen the validator.

- [ ] **Step 6: Format and commit**

Run: `./gradlew :server:libs:core:tenant:tenant-api:spotlessApply`

```bash
git add server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/TenantContext.java \
        server/libs/core/tenant/tenant-api/src/test/java/com/bytechef/tenant/TenantContextTenantIdValidationTest.java
git commit -m "gecko Validate tenant id at the TenantContext chokepoint (T4)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Defense-in-depth at the CE SQL sink

**Files:** `BaseDataSource.java`.

**Interfaces:**
- Consumes: `TenantIdValidator.validateDatabaseSchema(String)`.

- [ ] **Step 1: Add the schema check before concatenation**

In `BaseDataSource.setSearchPath`, replace:
```java
    protected void setSearchPath(Connection connection) throws SQLException {
        String currentDatabaseSchema = TenantContext.getCurrentDatabaseSchema(getVectorSchemaSuffix());

        try (PreparedStatement statement =
            connection.prepareStatement(SET_SEARCH_PATH_STATEMENT + currentDatabaseSchema)) {

            statement.execute();
        }
    }
```
with:
```java
    protected void setSearchPath(Connection connection) throws SQLException {
        String currentDatabaseSchema = TenantContext.getCurrentDatabaseSchema(getVectorSchemaSuffix());

        TenantIdValidator.validateDatabaseSchema(currentDatabaseSchema);

        try (PreparedStatement statement =
            connection.prepareStatement(SET_SEARCH_PATH_STATEMENT + currentDatabaseSchema)) {

            statement.execute();
        }
    }
```
Add the import `import com.bytechef.tenant.TenantIdValidator;` (alongside the existing `import com.bytechef.tenant.TenantContext;`).

- [ ] **Step 2: Compile**

Run: `./gradlew :server:libs:core:tenant:tenant-api:compileJava`
Expected: BUILD SUCCESSFUL. (No new unit test: once Task 2 lands, the normal setter path cannot produce an invalid schema to drive this guard with; it is defense-in-depth for future callers and is exercised by `TenantIdValidatorTest`.)

- [ ] **Step 3: Format and commit**

Run: `./gradlew :server:libs:core:tenant:tenant-api:spotlessApply`

```bash
git add server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/sql/BaseDataSource.java
git commit -m "gecko Validate schema name before SET search_path in BaseDataSource (T4)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Fail-fast in the token parsers

**Files:** `WorkflowExecutionId.java`, `JobResumeId.java` (+ tests).

**Interfaces:**
- Consumes: `TenantIdValidator.validate(String)`.
- Produces: `WorkflowExecutionId.parse` / `JobResumeId.parse` throw `IllegalArgumentException` when the decoded tenant segment is malformed.

- [ ] **Step 1: Write the failing tests**

Create `.../platform-api/src/test/java/com/bytechef/platform/workflow/WorkflowExecutionIdTenantValidationTest.java`:

```java
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

package com.bytechef.platform.workflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.EncodingUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowExecutionIdTenantValidationTest {

    @Test
    void testParseRejectsMalformedTenantSegment() {
        String malformed = EncodingUtils.base64EncodeToString("a;b:0:1:wf-uuid:trigger");

        assertThatThrownBy(() -> WorkflowExecutionId.parse(malformed))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

Create `.../platform-workflow-execution-api/src/test/java/com/bytechef/platform/workflow/execution/JobResumeIdTenantValidationTest.java`:

```java
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

package com.bytechef.platform.workflow.execution;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.EncodingUtils;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class JobResumeIdTenantValidationTest {

    @Test
    void testParseRejectsMalformedTenantSegment() {
        String malformed = EncodingUtils.base64EncodeToString("a;b:1:" + UUID.randomUUID());

        assertThatThrownBy(() -> JobResumeId.parse(malformed))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

NOTE: confirm the exact base64 encode helper name on `EncodingUtils` (`base64EncodeToString` mirrors the `base64DecodeToString` used in `parse`). If the encoder is named differently, use the actual name — check with `grep -n "base64Encode" server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/EncodingUtils.java`.

- [ ] **Step 2: Run the tests to verify they fail**

Run:
```bash
./gradlew :server:libs:platform:platform-api:test --tests "*WorkflowExecutionIdTenantValidationTest" \
  :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api:test --tests "*JobResumeIdTenantValidationTest"
```
Expected: FAIL — malformed tenant segments are currently accepted.

- [ ] **Step 3: Add validation to both parsers**

In `WorkflowExecutionId.parse`, after the split, validate `items[0]`. Replace:
```java
    public static WorkflowExecutionId parse(String id) {
        id = EncodingUtils.base64DecodeToString(id);

        String[] items = id.split(":");

        return new WorkflowExecutionId(
            items[0], PlatformType.values()[Integer.parseInt(items[1])], Long.parseLong(items[2]), items[3], items[4]);
    }
```
with:
```java
    public static WorkflowExecutionId parse(String id) {
        id = EncodingUtils.base64DecodeToString(id);

        String[] items = id.split(":");

        TenantIdValidator.validate(items[0]);

        return new WorkflowExecutionId(
            items[0], PlatformType.values()[Integer.parseInt(items[1])], Long.parseLong(items[2]), items[3], items[4]);
    }
```
Add `import com.bytechef.tenant.TenantIdValidator;` (the file already imports `com.bytechef.tenant.TenantContext`).

In `JobResumeId.parse`, validate `items[0]` before constructing. Replace the final return:
```java
        return new JobResumeId(items[0], Long.parseLong(items[1]), parsedUuid.toString());
```
with:
```java
        TenantIdValidator.validate(items[0]);

        return new JobResumeId(items[0], Long.parseLong(items[1]), parsedUuid.toString());
```
Add `import com.bytechef.tenant.TenantIdValidator;` (the file already imports `com.bytechef.tenant.TenantContext`).

- [ ] **Step 4: Run the tests to verify they pass**

Run:
```bash
./gradlew :server:libs:platform:platform-api:test --tests "*WorkflowExecutionIdTenantValidationTest" \
  :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api:test --tests "*JobResumeIdTenantValidationTest"
```
Expected: PASS (1 test each).

- [ ] **Step 5: Format and commit**

Run:
```bash
./gradlew :server:libs:platform:platform-api:spotlessApply \
  :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api:spotlessApply
```

```bash
git add server/libs/platform/platform-api/src/main/java/com/bytechef/platform/workflow/WorkflowExecutionId.java \
        server/libs/platform/platform-api/src/test/java/com/bytechef/platform/workflow/WorkflowExecutionIdTenantValidationTest.java \
        server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/main/java/com/bytechef/platform/workflow/execution/JobResumeId.java \
        server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-api/src/test/java/com/bytechef/platform/workflow/execution/JobResumeIdTenantValidationTest.java
git commit -m "gecko Validate tenant segment when parsing execution/resume tokens (T4)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 5: Defense-in-depth at the EE SQL sink

**Files:** `MultiTenantDriverDelegate.java` (EE).

**Interfaces:**
- Consumes: `TenantIdValidator.validateDatabaseSchema(String)`.

- [ ] **Step 1: Add the schema check before concatenation**

In `MultiTenantDriverDelegate.setSearchPath`, replace:
```java
    private static void setSearchPath(Connection connection, String databaseSchemaName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + databaseSchemaName);
        }
    }
```
with:
```java
    private static void setSearchPath(Connection connection, String databaseSchemaName) throws SQLException {
        TenantIdValidator.validateDatabaseSchema(databaseSchemaName);

        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + databaseSchemaName);
        }
    }
```
Add `import com.bytechef.tenant.TenantIdValidator;` (the file already imports `com.bytechef.tenant.TenantContext`). Keep the EE license header and `@version ee` already on the file.

- [ ] **Step 2: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-scheduler:platform-scheduler-impl:compileJava`
Expected: BUILD SUCCESSFUL. (Defense-in-depth; no Connection-mock test, same rationale as Task 3.)

- [ ] **Step 3: Format and commit**

Run: `./gradlew :server:ee:libs:platform:platform-scheduler:platform-scheduler-impl:spotlessApply`

```bash
git add server/ee/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/ee/platform/scheduler/tenant/MultiTenantDriverDelegate.java
git commit -m "gecko Validate schema name before SET search_path in scheduler delegate (T4)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 6: 400 guard in `RemoteMultiTenantFilter` (EE)

**Files:** `RemoteMultiTenantFilter.java` (+ new test).

**Interfaces:**
- Consumes: `TenantIdValidator.isValid(String)`.
- Produces: a missing/blank/malformed `CURRENT_TENANT_ID` header on a `/remote/**` request yields HTTP 400 and the chain is not invoked; a valid header runs the chain under that tenant as before.

- [ ] **Step 1: Write the failing test**

Create `server/ee/libs/core/remote/remote-rest/src/test/java/com/bytechef/ee/remote/web/filter/RemoteMultiTenantFilterTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.web.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RemoteMultiTenantFilterTest {

    private final RemoteMultiTenantFilter filter = new RemoteMultiTenantFilter();

    @Test
    void testMissingHeaderReturnsBadRequest() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.CURRENT_TENANT_ID)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(400);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void testMalformedHeaderReturnsBadRequest() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.CURRENT_TENANT_ID)).thenReturn("a;b");

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(400);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void testValidHeaderProceeds() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.CURRENT_TENANT_ID)).thenReturn("000001");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
```

NOTE: `doFilterInternal` is `protected` in `OncePerRequestFilter`; the test is in the same package, so it can call it. Confirm the EE module has `mockito`/`junit`/`jakarta.servlet` on its test classpath; if the module currently has no `src/test`, add the standard test deps to its `build.gradle.kts` (mirror a sibling EE rest module's test block).

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:core:remote:remote-rest:test --tests "*RemoteMultiTenantFilterTest"`
Expected: FAIL — current filter passes null/`a;b` straight to `runWithTenantId` (no 400).

- [ ] **Step 3: Add the guard**

In `RemoteMultiTenantFilter.java`, replace `doFilterInternal`:
```java
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String currentTenantId = request.getHeader(TenantConstants.CURRENT_TENANT_ID);

        TenantContext.runWithTenantId(currentTenantId, () -> filterChain.doFilter(request, response));
    }
```
with:
```java
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String currentTenantId = request.getHeader(TenantConstants.CURRENT_TENANT_ID);

        if (!TenantIdValidator.isValid(currentTenantId)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);

            return;
        }

        TenantContext.runWithTenantId(currentTenantId, () -> filterChain.doFilter(request, response));
    }
```
Add `import com.bytechef.tenant.TenantIdValidator;` (alongside the existing `com.bytechef.tenant.TenantContext` / `TenantConstants` imports).

NOTE: the test verifies `sendError(400)`; `HttpServletResponse.SC_BAD_REQUEST == 400`, so the production code uses the constant and the mock verification matches the int. Keep them consistent.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:core:remote:remote-rest:test --tests "*RemoteMultiTenantFilterTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Format and commit**

Run: `./gradlew :server:ee:libs:core:remote:remote-rest:spotlessApply`

```bash
git add server/ee/libs/core/remote/remote-rest/src/main/java/com/bytechef/ee/remote/web/filter/RemoteMultiTenantFilter.java \
        server/ee/libs/core/remote/remote-rest/src/test/java/com/bytechef/ee/remote/web/filter/RemoteMultiTenantFilterTest.java
# also add build.gradle.kts if Step 1 required adding test deps
git commit -m "gecko Reject missing/invalid tenant header with 400 in RemoteMultiTenantFilter (T4)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 7: Consolidate EE `TenantRepository` validation

**Files:** `TenantRepository.java` (EE).

**Interfaces:**
- Consumes: `TenantIdValidator.validate(String)`.

- [ ] **Step 1: Delegate the private validator**

In `TenantRepository.java`, replace the private method:
```java
    private static void validateTenantId(String tenantId) {
        if (tenantId == null || !TENANT_ID_PATTERN.matcher(tenantId)
            .matches()) {
            throw new IllegalArgumentException(
                "Invalid tenant ID. Must contain only alphanumeric characters, underscores, and hyphens.");
        }
    }
```
with:
```java
    private static void validateTenantId(String tenantId) {
        TenantIdValidator.validate(tenantId);
    }
```
Add `import com.bytechef.tenant.TenantIdValidator;`. Then remove the now-unused `TENANT_ID_PATTERN` field (line ~46) and its `import java.util.regex.Pattern;` if nothing else in the file uses `Pattern` (check with `grep -n "Pattern" <file>` first). Keep the EE header / `@version ee`.

NOTE: this is a deliberate tightening — the old pattern allowed a hyphen, the shared one does not. Real tenant ids are zero-padded numerics, so no existing tenant is affected.

- [ ] **Step 2: Compile + run any existing TenantRepository tests**

Run: `./gradlew :server:ee:libs:core:tenant:tenant-multi-service:compileJava :server:ee:libs:core:tenant:tenant-multi-service:test 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL. If an existing test asserted a hyphenated id was valid, update it (hyphens are no longer permitted — confirm the id is illegitimate, then fix the test).

- [ ] **Step 3: Format and commit**

Run: `./gradlew :server:ee:libs:core:tenant:tenant-multi-service:spotlessApply`

```bash
git add server/ee/libs/core/tenant/tenant-multi-service/src/main/java/com/bytechef/ee/tenant/repository/TenantRepository.java
git commit -m "gecko Consolidate TenantRepository validation onto TenantIdValidator (T4)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 8: Close out the tracker

- [ ] **Step 1: Run check on all touched modules**

Run:
```bash
./gradlew \
  :server:libs:core:tenant:tenant-api:check \
  :server:libs:platform:platform-api:check \
  :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api:check \
  :server:ee:libs:platform:platform-scheduler:platform-scheduler-impl:check \
  :server:ee:libs:core:remote:remote-rest:check \
  :server:ee:libs:core:tenant:tenant-multi-service:check
```
Expected: BUILD SUCCESSFUL. Fix any checkstyle/PMD/SpotBugs findings the new code introduces. If a pre-existing test in one of these modules is already broken on the branch (unrelated to this change), confirm it fails without these changes and note it rather than fixing out-of-scope code.

- [ ] **Step 2: Mark T4 done in the tracker**

In `gecko-remediation-tasks.md`, change `- [ ] **T4.` to `- [x] **T4.` and append:
> **Done** (spec/plan `docs/superpowers/{specs,plans}/2026-06-21-tenant-id-validation*`): shared CE `TenantIdValidator` (`^[a-zA-Z0-9_]+$`) enforced at the `TenantContext.setCurrentTenantId` chokepoint (covers the remote header filter, both base64 token parsers, and the scheduler), plus defense-in-depth schema validation at both `SET search_path` sinks (`BaseDataSource`, `MultiTenantDriverDelegate`), fail-fast in `WorkflowExecutionId.parse`/`JobResumeId.parse`, and a 400 guard in `RemoteMultiTenantFilter`. EE `TenantRepository` consolidated onto the shared validator (drops the never-used hyphen). Charset confirmed against live schemas (`bytechef_000001`…, `public`).

- [ ] **Step 3: Commit**

```bash
git add gecko-remediation-tasks.md
git commit -m "gecko Mark T4 tenant-id validation / SQLi done

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- Shared `TenantIdValidator` → Task 1 ✓
- Central chokepoint (`setCurrentTenantId`) → Task 2 ✓
- SQL sink hardening (BaseDataSource + MultiTenantDriverDelegate) → Tasks 3, 5 ✓
- Token-parser fail-fast (WorkflowExecutionId + JobResumeId) → Task 4 ✓
- Remote-filter 400 guard → Task 6 ✓
- EE TenantRepository consolidation → Task 7 ✓
- Charset `^[a-zA-Z0-9_]+$`, IllegalArgumentException, 400 mapping → encoded in Tasks 1/2/6 ✓
- Close-out / tracker → Task 8 ✓

**Placeholder scan:** No TBD/TODO; every code step shows full before/after. The two "confirm the helper/dep name" notes (EncodingUtils encoder in Task 4, EE test deps in Task 6) are explicit verify-then-use instructions, not silent gaps.

**Type consistency:** `TenantIdValidator.isValid(String)` / `validate(String)` / `validateDatabaseSchema(String)` are used identically across Tasks 1–7. Exception type is `IllegalArgumentException` everywhere; the filter maps invalid → `sendError(SC_BAD_REQUEST)` (== 400, matching the test). Package `com.bytechef.tenant` means no import is needed in `TenantContext` (Task 2) but is needed in the other-package consumers (Tasks 3–7) — called out per task.
