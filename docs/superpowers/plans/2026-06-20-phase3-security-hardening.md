# Phase 3 Security Hardening (T26 + T27) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining gecko Phase 3 security findings — TOTP brute-force lockout, `config()` SpEL allowlist, activation-email enumeration, `Content-Disposition` header injection, filesystem path traversal, and cross-tenant in-memory chat-memory leakage.

**Architecture:** Each finding is a focused, independent change in its owning module. Server-side fixes are TDD'd against the service/util/action layer where the vulnerability lives so all callers benefit; the one client item (TipTap, EMBED_INIT) is already mitigated and out of scope.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC + Liquibase, JUnit 5 + Mockito + AssertJ, Spring `ContentDisposition` (spring-web), Caffeine (already used by the chat-memory holder).

## Global Constraints

- Source files under `server/ee/` use the ByteChef Enterprise license header and a `@version ee` Javadoc tag; files under `server/libs/` use the Apache 2.0 header. (Spotless picks the header by file content, not path — `@version ee` triggers EE.)
- Run `./gradlew spotlessApply` before every commit; `./gradlew check` gates the branch.
- Persist JDBC enums as INT ordinals; append new enum values at the end. (No new enums in this plan.)
- Blank line before control statements and after a variable modification that a later statement uses (Java style rules in CLAUDE.md).
- Test method names are camelCase without underscores; unit test classes end in `Test`, integration test classes end in `IntTest`.
- Commit messages: server-side `gecko <description>`; client-side `gecko client - <description>`. End every commit body with the `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer.
- Branch is `0_732` and the user commits in parallel — never `git commit --amend`; always make fresh commits and stage only files this plan touches.

---

## File Structure

**Task 1 — TOTP lockout (DB-persisted):**
- Modify: `server/libs/platform/platform-user/platform-user-api/src/main/java/com/bytechef/platform/user/domain/User.java` (two fields + accessors)
- Create: `.../platform-user-service/src/main/resources/config/liquibase/changelog/platform/user/20260620000000_platform_user_add_totp_lockout_columns.xml`
- Modify: `.../platform-user-service/.../config/liquibase/changelog/platform/user/index.xml` (or whichever file `<include>`s the user changelogs — confirm during the task)
- Create: `.../platform-user-api/src/main/java/com/bytechef/platform/user/exception/TotpLockedException.java`
- Modify: `.../platform-user-service/src/main/java/com/bytechef/platform/user/service/UserServiceImpl.java` (`verifyTotpCode`)
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (MFA threshold/window properties)
- Modify: `server/libs/config/security-config/src/main/java/com/bytechef/security/web/filter/TwoFactorVerificationFilter.java` (429 on lock)
- Modify: `.../platform-user-rest/.../web/rest/AccountController.java` (429 on lock for the `/api/mfa/verify` + disable paths)
- Test: `.../platform-user-service/src/test/java/com/bytechef/platform/user/service/UserServiceTotpLockoutTest.java`

**Task 2 — `config()` SpEL allowlist:**
- Modify: `server/libs/core/evaluator/evaluator-impl/src/main/java/com/bytechef/evaluator/Config.java`
- Test: `server/libs/core/evaluator/evaluator-impl/src/test/java/com/bytechef/evaluator/ConfigTest.java`

**Task 3 — Activation-email enumeration:**
- Modify: `.../platform-user-rest/.../web/rest/AccountController.java` (`sendActivationEmail`)
- Test: `.../platform-user-rest/src/test/java/com/bytechef/platform/user/web/rest/AccountControllerActivationTest.java`

**Task 4 — Content-Disposition sanitizer + 3 call sites:**
- Modify: `server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/StringUtils.java` (new `sanitizeForHeader`)
- Test: `server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/StringUtilsTest.java`
- Modify: `.../automation-configuration-rest-impl/.../web/rest/ProjectApiController.java:91`
- Modify: `server/libs/platform/platform-configuration/.../web/rest/AbstractWorkflowApiController.java:54`
- Modify: `server/ee/libs/automation/automation-api-platform/.../web/rest/ApiCollectionApiController.java:90` (EE header rules apply)

**Task 5 — Filesystem path canonicalization:**
- Modify: `server/libs/modules/components/filesystem/src/main/java/com/bytechef/component/filesystem/action/FilesystemWriteFileAction.java`
- Test: `.../filesystem/src/test/java/com/bytechef/component/filesystem/action/FilesystemWriteFileActionTest.java` (extend if present)

**Task 6 — In-memory chat-memory tenant isolation:**
- Modify: `server/libs/modules/components/ai/agent/chat-memory/chat-memory-in-memory/src/main/java/com/bytechef/component/ai/agent/chat/memory/memory/cluster/InMemoryChatMemory.java`
- Test: `.../chat-memory-in-memory/src/test/java/com/bytechef/component/ai/agent/chat/memory/memory/InMemoryChatMemoryTenantIsolationTest.java`

**Task 7 — Audit other chat-memory backends (read-only sweep + fix-if-needed):**
- Inspect: Jdbc / Mongo / Cosmos / Cassandra chat-memory cluster elements + `LangchainAgent`, `SpringAIAgent`, `CopilotConfiguration` for the same static-capture pattern.

---

## Task 1: TOTP brute-force lockout (DB-persisted)

**Files:** see File Structure, Task 1.

**Interfaces:**
- Consumes: `User.getTotpSecret()`, `userRepository.findByLogin`, `totpCodeVerifier.isValidCode` (existing in `UserServiceImpl`).
- Produces: `UserService.verifyTotpCode(String login, String code)` keeps its `boolean` return for the *valid/invalid* outcome but now throws `TotpLockedException` (unchecked) when the account is in a lockout window. New `User` fields `failedTotpAttempts`/`totpLockoutUntil` with getters/setters. New properties `bytechef.security.mfa.max-failed-attempts` (int, default 5) and `bytechef.security.mfa.lockout-duration` (Duration, default PT15M).

- [ ] **Step 1: Locate the user changelog include file**

Run: `grep -rln "add_totp_columns\|add_saml_scim_mfa" server/libs/platform/platform-user/platform-user-service/src/main/resources/config/liquibase`
Expected: prints an `index.xml` (or similarly named master) that `<include>`s the per-table changelogs. Note its path for Step 4. If no master lists them, they are discovered by directory — note that instead.

- [ ] **Step 2: Write the failing service test**

Create `server/libs/platform/platform-user/platform-user-service/src/test/java/com/bytechef/platform/user/service/UserServiceTotpLockoutTest.java`:

```java
/*
 * Copyright 2016-2025 the original author or authors.
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

package com.bytechef.platform.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.TotpLockedException;
import com.bytechef.platform.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTotpLockoutTest {

    @Mock
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();

        user.setLogin("user@localhost.com");
        user.setTotpSecret("SECRET");
    }

    @Test
    void testInvalidCodeIncrementsFailedAttempts() {
        // Arrange a service whose code verifier always rejects; see Step 5 for the seam.
        // Expect: after one invalid attempt, failedTotpAttempts == 1, no exception.
    }

    @Test
    void testReachingThresholdSetsLockout() {
        // After max-failed-attempts invalid attempts, totpLockoutUntil is set in the future.
    }

    @Test
    void testVerifyWhileLockedThrowsTotpLockedException() {
        user.setTotpLockoutUntil(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(userRepository.findByLogin(anyString())).thenReturn(Optional.of(user));

        // The service under test is constructed in Step 5; this asserts the contract.
        assertThatThrownBy(() -> verifyUnderTest("123456"))
            .isInstanceOf(TotpLockedException.class);
    }

    @Test
    void testValidCodeResetsCounters() {
        user.setFailedTotpAttempts(3);

        // A valid code clears failedTotpAttempts to 0 and totpLockoutUntil to null.
    }

    // Placeholder seam — replaced in Step 5 once the constructor wiring is known.
    private boolean verifyUnderTest(String code) {
        throw new UnsupportedOperationException("wired in Step 5");
    }
}
```

NOTE: this test references `User.setTotpLockoutUntil`, `User.setFailedTotpAttempts`, and `TotpLockedException`, which do not exist yet — that is the intended red state. Steps 3-5 make it real; in Step 5 you replace `verifyUnderTest` with a constructed `UserServiceImpl` call mirroring its real constructor (inspect `UserServiceImpl`'s constructor and mock its remaining dependencies with `lenient()`).

- [ ] **Step 3: Run the test to verify it fails to compile**

Run: `./gradlew :server:libs:platform:platform-user:platform-user-service:compileTestJava`
Expected: FAIL — `TotpLockedException`, `setTotpLockoutUntil`, `setFailedTotpAttempts` not found.

- [ ] **Step 4: Add the entity fields and Liquibase migration**

In `User.java`, after the `resetKey` field block, add:

```java
    @Column("failed_totp_attempts")
    private int failedTotpAttempts;

    @Column("totp_lockout_until")
    private Instant totpLockoutUntil;
```

(Ensure `import java.time.Instant;` is present — it already is, used by `createdDate`.) Add accessors near the other totp accessors:

```java
    public int getFailedTotpAttempts() {
        return failedTotpAttempts;
    }

    public Instant getTotpLockoutUntil() {
        return totpLockoutUntil;
    }

    public void setFailedTotpAttempts(int failedTotpAttempts) {
        this.failedTotpAttempts = failedTotpAttempts;
    }

    public void setTotpLockoutUntil(Instant totpLockoutUntil) {
        this.totpLockoutUntil = totpLockoutUntil;
    }
```

Create the migration `.../changelog/platform/user/20260620000000_platform_user_add_totp_lockout_columns.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20260620000000-1" author="Ivica Cardic">
        <addColumn tableName="user">
            <column name="failed_totp_attempts" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
            <column name="totp_lockout_until" type="TIMESTAMP"/>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

Register it: add an `<include file="config/liquibase/changelog/platform/user/20260620000000_platform_user_add_totp_lockout_columns.xml"/>` line next to the existing totp-columns include in the master/index file found in Step 1 (match the exact `file=`/`relativeToChangelogFile` style the neighbors use). If the changelogs are directory-discovered with no master, no include is needed — verify by grepping for `20260210000000_platform_user_add_totp_columns` and copying whatever registration form it uses.

- [ ] **Step 5: Add `TotpLockedException` and implement the lockout logic**

Create `.../platform-user-api/src/main/java/com/bytechef/platform/user/exception/TotpLockedException.java` (mirror the package/style of the sibling `UserNotFoundException`):

```java
/* Apache 2.0 header — copy verbatim from UserNotFoundException.java in the same package */
package com.bytechef.platform.user.exception;

/**
 * Thrown when TOTP verification is attempted while the account is in a lockout window after too
 * many failed attempts.
 *
 * @author Ivica Cardic
 */
public class TotpLockedException extends RuntimeException {

    public TotpLockedException() {
        super("Too many failed verification attempts. Try again later.");
    }
}
```

Read `UserServiceImpl`'s constructor and existing fields to find how `ApplicationProperties` (or a narrower config bean) is injected. Add the two MFA settings to `ApplicationProperties` under the security section — inspect the existing `Security` nested class first and follow its getter/setter style:

```java
    // inside ApplicationProperties.Security (or the nearest existing security holder)
    private final Mfa mfa = new Mfa();

    public Mfa getMfa() {
        return mfa;
    }

    public static class Mfa {

        /**
         * Number of consecutive failed TOTP verifications before the account is locked.
         */
        private int maxFailedAttempts = 5;

        /**
         * How long an account stays locked after reaching the failed-attempt threshold.
         */
        private Duration lockoutDuration = Duration.ofMinutes(15);

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public Duration getLockoutDuration() {
            return lockoutDuration;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public void setLockoutDuration(Duration lockoutDuration) {
            this.lockoutDuration = lockoutDuration;
        }
    }
```

(Add `import java.time.Duration;` if absent.) If `UserServiceImpl` does not already receive `ApplicationProperties`, add it to the constructor and update the Spring wiring; otherwise read the two values from the already-injected instance.

Rewrite `verifyTotpCode` in `UserServiceImpl` (note: drop `readOnly = true` — this method now writes):

```java
    @Override
    @Transactional
    public boolean verifyTotpCode(String login, String code) {
        User user = userRepository.findByLogin(login)
            .orElse(null);

        if (user == null || user.getTotpSecret() == null) {
            return false;
        }

        Instant lockoutUntil = user.getTotpLockoutUntil();

        if (lockoutUntil != null && Instant.now().isBefore(lockoutUntil)) {
            throw new TotpLockedException();
        }

        if (code == null || code.isBlank() || !totpCodeVerifier.isValidCode(user.getTotpSecret(), code)) {
            int attempts = user.getFailedTotpAttempts() + 1;

            user.setFailedTotpAttempts(attempts);

            if (attempts >= applicationProperties.getSecurity().getMfa().getMaxFailedAttempts()) {
                user.setTotpLockoutUntil(
                    Instant.now().plus(applicationProperties.getSecurity().getMfa().getLockoutDuration()));
            }

            userRepository.save(user);

            return false;
        }

        user.setFailedTotpAttempts(0);
        user.setTotpLockoutUntil(null);

        userRepository.save(user);

        return true;
    }
```

(Adjust `applicationProperties.getSecurity()...` to the actual accessor path you added in `ApplicationProperties`.) Now replace the `verifyUnderTest` seam in the test with a real `UserServiceImpl` constructed via Mockito mocks (mock `totpCodeVerifier` to control valid/invalid; `lenient()` the unused deps), and fill in the four test bodies.

- [ ] **Step 6: Run the service tests to verify they pass**

Run: `./gradlew :server:libs:platform:platform-user:platform-user-service:test --tests "*UserServiceTotpLockoutTest"`
Expected: PASS (4 tests).

- [ ] **Step 7: Map `TotpLockedException` to HTTP 429 at both call sites**

In `TwoFactorVerificationFilter.doFilterInternal`, wrap the verify call:

```java
        boolean valid;

        try {
            valid = userService.verifyTotpCode(userDetails.getUsername(), code);
        } catch (com.bytechef.platform.user.exception.TotpLockedException e) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

            return;
        }
```

In `AccountController` at the `/api/mfa/verify` (line ~292) and disable (line ~312) paths, catch `TotpLockedException` and translate to a 429 response consistent with how that controller signals other failures (inspect the surrounding methods — if they return a body/throw a mapped exception, match it; the minimal form is rethrowing as a `ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)`).

- [ ] **Step 8: Format, build, commit**

Run: `./gradlew spotlessApply :server:libs:platform:platform-user:platform-user-service:compileJava :server:libs:config:security-config:compileJava`
Expected: BUILD SUCCESSFUL.

```bash
git add server/libs/platform/platform-user/platform-user-api/src/main/java/com/bytechef/platform/user/domain/User.java \
        server/libs/platform/platform-user/platform-user-api/src/main/java/com/bytechef/platform/user/exception/TotpLockedException.java \
        server/libs/platform/platform-user/platform-user-service/src/main/resources/config/liquibase/changelog/platform/user/20260620000000_platform_user_add_totp_lockout_columns.xml \
        server/libs/platform/platform-user/platform-user-service/src/main/resources/config/liquibase/changelog/platform/user/*index*.xml \
        server/libs/platform/platform-user/platform-user-service/src/main/java/com/bytechef/platform/user/service/UserServiceImpl.java \
        server/libs/platform/platform-user/platform-user-service/src/test/java/com/bytechef/platform/user/service/UserServiceTotpLockoutTest.java \
        server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java \
        server/libs/config/security-config/src/main/java/com/bytechef/security/web/filter/TwoFactorVerificationFilter.java \
        server/libs/platform/platform-user/platform-user-rest/src/main/java/com/bytechef/platform/user/web/rest/AccountController.java
git commit -m "gecko Rate-limit TOTP verification with DB-persisted lockout (T26)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: `config()` SpEL allowlist

**Files:** `evaluator/Config.java`, `evaluator/ConfigTest.java`.

**Interfaces:**
- Consumes: `Environment` (already a `Config` constructor arg).
- Produces: `Config.execute` reads `bytechef.workflow.config.allowed-prefixes` (comma-or-list property) from the same `Environment`; only property names matching an allowed prefix are returned, everything else throws the existing `SpelEvaluationException(PROPERTY_OR_FIELD_NOT_READABLE)`.

- [ ] **Step 1: Write the failing test**

Create `server/libs/core/evaluator/evaluator-impl/src/test/java/com/bytechef/evaluator/ConfigTest.java`:

```java
/* Apache 2.0 header — copy verbatim from Config.java */
package com.bytechef.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.mock.env.MockEnvironment;

class ConfigTest {

    @Test
    void testReturnsValueForAllowedPrefix() throws Exception {
        MockEnvironment environment = new MockEnvironment();

        environment.setProperty("bytechef.workflow.config.allowed-prefixes", "app.");
        environment.setProperty("app.setting", "value");

        Config config = new Config(environment);

        assertThat(config.execute(null, null, "app.setting").getValue()).isEqualTo("value");
    }

    @Test
    void testDeniesPropertyOutsideAllowlist() {
        MockEnvironment environment = new MockEnvironment();

        environment.setProperty("bytechef.workflow.config.allowed-prefixes", "app.");
        environment.setProperty("spring.datasource.password", "secret");

        Config config = new Config(environment);

        assertThatThrownBy(() -> config.execute(null, null, "spring.datasource.password"))
            .isInstanceOf(SpelEvaluationException.class);
    }

    @Test
    void testEmptyAllowlistDeniesEverything() {
        MockEnvironment environment = new MockEnvironment();

        environment.setProperty("app.setting", "value");

        Config config = new Config(environment);

        assertThatThrownBy(() -> config.execute(null, null, "app.setting"))
            .isInstanceOf(SpelEvaluationException.class);
    }
}
```

NOTE: `Config` is currently package-private with a package-private constructor — same package, so the test can construct it. `MockEnvironment` comes from `spring-test`; confirm `evaluator-impl` has `spring-test` on its test classpath (it does for `SpelEvaluatorTest`).

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:libs:core:evaluator:evaluator-impl:test --tests "*ConfigTest"`
Expected: FAIL — `testDeniesPropertyOutsideAllowlist` / `testEmptyAllowlistDeniesEverything` fail because every property is currently returned.

- [ ] **Step 3: Implement the allowlist check**

Edit `Config.execute`:

```java
    private static final String ALLOWED_PREFIXES_PROPERTY = "bytechef.workflow.config.allowed-prefixes";

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        String propertyName = (String) arguments[0];

        if (!isAllowed(propertyName)) {
            throw new SpelEvaluationException(
                SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, propertyName, Environment.class);
        }

        String value = environment.getProperty(propertyName);

        if (value == null) {
            throw new SpelEvaluationException(
                SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, propertyName, Environment.class);
        }

        return new TypedValue(value);
    }

    private boolean isAllowed(String propertyName) {
        String[] prefixes = environment.getProperty(ALLOWED_PREFIXES_PROPERTY, String[].class, new String[0]);

        for (String prefix : prefixes) {
            if (!prefix.isBlank() && propertyName.startsWith(prefix.trim())) {
                return true;
            }
        }

        return false;
    }
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:libs:core:evaluator:evaluator-impl:test --tests "*ConfigTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Format and commit**

Run: `./gradlew spotlessApply :server:libs:core:evaluator:evaluator-impl:compileJava`

```bash
git add server/libs/core/evaluator/evaluator-impl/src/main/java/com/bytechef/evaluator/Config.java \
        server/libs/core/evaluator/evaluator-impl/src/test/java/com/bytechef/evaluator/ConfigTest.java
git commit -m "gecko Restrict config() SpEL function to allowed property prefixes (T26)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Activation-email enumeration

**Files:** `AccountController.java`, `AccountControllerActivationTest.java`.

**Interfaces:**
- Consumes: `userService.fetchUserByEmail(String)` → `Optional<User>`, `mailService.sendActivationEmail(User)`.
- Produces: `sendActivationEmail` returns 204 for any email, dispatching mail only when the user exists.

- [ ] **Step 1: Write the failing test**

Create `.../platform-user-rest/src/test/java/com/bytechef/platform/user/web/rest/AccountControllerActivationTest.java`:

```java
/* Apache 2.0 header */
package com.bytechef.platform.user.web.rest;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.MailService;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountControllerActivationTest {

    @Mock
    private UserService userService;

    @Mock
    private MailService mailService;

    @Test
    void testUnknownEmailDoesNotThrowAndSendsNoMail() {
        when(userService.fetchUserByEmail("ghost@localhost.com")).thenReturn(Optional.empty());

        accountController().sendActivationEmail("ghost@localhost.com");

        verify(mailService, never()).sendActivationEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testKnownEmailSendsMail() {
        User user = new User();

        when(userService.fetchUserByEmail("user@localhost.com")).thenReturn(Optional.of(user));

        accountController().sendActivationEmail("user@localhost.com");

        verify(mailService).sendActivationEmail(user);
    }

    // Construct AccountController with its real constructor; mock remaining deps as needed.
    private AccountController accountController() {
        throw new UnsupportedOperationException("wire to real AccountController constructor");
    }
}
```

NOTE: replace the `accountController()` helper with the controller's real constructor (inspect `AccountController`'s constructor signature; mock the other dependencies). `testUnknownEmailDoesNotThrowAndSendsNoMail` fails today because the controller throws `UserNotFoundException`.

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:libs:platform:platform-user:platform-user-rest:test --tests "*AccountControllerActivationTest"`
Expected: FAIL — unknown-email test throws `UserNotFoundException`.

- [ ] **Step 3: Make the endpoint non-enumerable**

Replace the body of `sendActivationEmail`:

```java
    @PostMapping("/send-activation-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendActivationEmail(@RequestBody String email) {
        userService.fetchUserByEmail(email)
            .ifPresent(mailService::sendActivationEmail);
    }
```

Remove the now-unused `UserNotFoundException` import if nothing else in the file uses it (check first).

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:libs:platform:platform-user:platform-user-rest:test --tests "*AccountControllerActivationTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Format and commit**

Run: `./gradlew spotlessApply :server:libs:platform:platform-user:platform-user-rest:compileJava`

```bash
git add server/libs/platform/platform-user/platform-user-rest/src/main/java/com/bytechef/platform/user/web/rest/AccountController.java \
        server/libs/platform/platform-user/platform-user-rest/src/test/java/com/bytechef/platform/user/web/rest/AccountControllerActivationTest.java
git commit -m "gecko Make activation-email endpoint non-enumerable (T26)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Content-Disposition sanitizer + call sites

**Files:** `StringUtils.java` (+ test), `ProjectApiController.java`, `AbstractWorkflowApiController.java`, `ApiCollectionApiController.java`.

**Interfaces:**
- Produces: `StringUtils.toContentDispositionHeaderValue(String filename)` → a complete, safe `Content-Disposition: attachment` header value (CR/LF/control chars stripped, RFC 5987 `filename*` for unicode). Used at all three export sites.

- [ ] **Step 1: Write the failing util test**

Add to `server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/StringUtilsTest.java`:

```java
    @Test
    void testToContentDispositionHeaderValueStripsCrlf() {
        String header = StringUtils.toContentDispositionHeaderValue("evil\r\nSet-Cookie: x.zip");

        org.assertj.core.api.Assertions.assertThat(header)
            .doesNotContain("\r")
            .doesNotContain("\n")
            .startsWith("attachment;");
    }

    @Test
    void testToContentDispositionHeaderValueEncodesUnicode() {
        String header = StringUtils.toContentDispositionHeaderValue("résumé.zip");

        org.assertj.core.api.Assertions.assertThat(header).contains("filename*=UTF-8''");
    }
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:libs:core:commons:commons-util:test --tests "*StringUtilsTest"`
Expected: FAIL — `toContentDispositionHeaderValue` not defined.

- [ ] **Step 3: Implement the helper**

Add to `StringUtils`:

```java
    public static String toContentDispositionHeaderValue(String filename) {
        String sanitized = filename == null
            ? ""
            : filename.replaceAll("[\\p{Cntrl}]", "");

        return org.springframework.http.ContentDisposition.attachment()
            .filename(sanitized, java.nio.charset.StandardCharsets.UTF_8)
            .build()
            .toString();
    }
```

(Confirm `commons-util` has `spring-web` on its compile classpath; if not, add `implementation("org.springframework:spring-web")` to its `build.gradle.kts` — `ContentDisposition` lives in spring-web. If adding the dep is undesirable, implement the RFC 5987 encoding inline instead, but prefer the Spring builder.)

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:libs:core:commons:commons-util:test --tests "*StringUtilsTest"`
Expected: PASS.

- [ ] **Step 5: Apply at the three export sites**

`ProjectApiController.java:91` — replace:

```java
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + project.getName() + ".zip" + "\"")
```
with:
```java
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                StringUtils.toContentDispositionHeaderValue(project.getName() + ".zip"))
```

`AbstractWorkflowApiController.java:54` — replace:
```java
            .header(HttpHeaders.CONTENT_DISPOSITION, "filename=\"" + fileName + "\"")
```
with:
```java
            .header(HttpHeaders.CONTENT_DISPOSITION, StringUtils.toContentDispositionHeaderValue(fileName))
```

`ApiCollectionApiController.java:90` (EE — keep the EE license header / `@version ee` already on the file) — replace:
```java
        bodyBuilder.header(HttpHeaders.CONTENT_DISPOSITION, "filename=\"" + getFilename(id) + "\"");
```
with:
```java
        bodyBuilder.header(
            HttpHeaders.CONTENT_DISPOSITION, StringUtils.toContentDispositionHeaderValue(getFilename(id)));
```

Add `import com.bytechef.commons.util.StringUtils;` to each file and confirm each module already depends on `commons-util` (ProjectApiController's module does — verified; check the other two `build.gradle.kts` and add the `implementation(project(":server:libs:core:commons:commons-util"))` line if missing).

- [ ] **Step 6: Build the three modules**

Run: `./gradlew spotlessApply :server:libs:automation:automation-configuration:automation-configuration-rest:automation-configuration-rest-impl:compileJava :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-api:compileJava`
Then compile the EE api-platform module (resolve its exact Gradle path with `./gradlew projects | grep api-platform-configuration-rest` first).
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/StringUtils.java \
        server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/StringUtilsTest.java \
        server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-impl/src/main/java/com/bytechef/automation/configuration/web/rest/ProjectApiController.java \
        server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-api/src/main/java/com/bytechef/platform/configuration/web/rest/AbstractWorkflowApiController.java \
        server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-rest/src/main/java/com/bytechef/ee/automation/apiplatform/configuration/web/rest/ApiCollectionApiController.java
# also add any build.gradle.kts touched
git commit -m "gecko Sanitize Content-Disposition export headers against CRLF injection (T27)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 5: Filesystem path canonicalization

**Files:** `FilesystemWriteFileAction.java` (+ test).

**Interfaces:**
- Produces: `FilesystemWriteFileAction.perform` rejects filenames containing a NUL byte and normalizes the path (`Path.normalize()`) before `Files.copy`.

- [ ] **Step 1: Write the failing test**

Check for an existing test class first: `ls server/libs/modules/components/filesystem/src/test/java/com/bytechef/component/filesystem/action/`. Add to (or create) `FilesystemWriteFileActionTest.java`:

```java
    @org.junit.jupiter.api.Test
    void testRejectsNullByteInFilename() {
        com.bytechef.component.definition.Parameters inputParameters =
            org.mockito.Mockito.mock(com.bytechef.component.definition.Parameters.class);

        org.mockito.Mockito.when(inputParameters.getRequiredString(
            com.bytechef.component.filesystem.constant.FilesystemConstants.FILENAME))
            .thenReturn("/tmp/evil .txt");

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            FilesystemWriteFileAction.perform(inputParameters, inputParameters,
                org.mockito.Mockito.mock(com.bytechef.component.definition.Context.class)))
            .isInstanceOf(IllegalArgumentException.class);
    }
```

(Adjust the `FILENAME` constant import to its real location — find it via `grep -rn "FILENAME" server/libs/modules/components/filesystem/.../constant`.)

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :server:libs:modules:components:filesystem:test --tests "*FilesystemWriteFileActionTest"`
Expected: FAIL — no validation today (or a different exception type from `Path.of`).

- [ ] **Step 3: Implement canonicalization**

Edit `perform`:

```java
    protected static Map<String, ?> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) throws IOException {

        String fileName = inputParameters.getRequiredString(FILENAME);

        if (fileName.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Path path = Path.of(fileName)
            .normalize();

        try (InputStream inputStream = context.file(
            file -> file.getInputStream(inputParameters.getRequiredFileEntry(FILE_ENTRY)))) {

            return Map.of("bytes", Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING));
        }
    }
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:libs:modules:components:filesystem:test --tests "*FilesystemWriteFileActionTest"`
Expected: PASS.

- [ ] **Step 5: Format and commit**

Run: `./gradlew spotlessApply :server:libs:modules:components:filesystem:compileJava`

```bash
git add server/libs/modules/components/filesystem/src/main/java/com/bytechef/component/filesystem/action/FilesystemWriteFileAction.java \
        server/libs/modules/components/filesystem/src/test/java/com/bytechef/component/filesystem/action/FilesystemWriteFileActionTest.java
git commit -m "gecko Reject null bytes and normalize path in FilesystemWriteFileAction (T27)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

NOTE: the spec also lists `FileDataStorageServiceImpl`, `JGitWorkflowOperations`, and `AwsFileStorageServiceImpl`. Treat each as a small follow-on within this task: open the file, find where an externally-supplied key/path is used to build a filesystem path or storage key, and apply the same NUL-byte rejection + `normalize()` (for object stores, reject `..` segments in the key). If a file already normalizes/validates, leave it and note that in the commit body. Commit these together as a second commit `gecko Normalize storage paths against traversal (T27)` if changes are needed.

---

## Task 6: In-memory chat-memory tenant isolation

**Files:** `InMemoryChatMemory.java` (+ new test).

**Interfaces:**
- Consumes: `InMemoryChatMemoryRepositoryHolder.getInstance()` (already tenant-scoped via `TenantContext`).
- Produces: `InMemoryChatMemory.apply(...)` builds a fresh `MessageWindowChatMemory` per call so the tenant-scoped repository is resolved in the caller's context (no `static final` capture).

- [ ] **Step 1: Write the failing isolation test**

Create `.../chat-memory-in-memory/src/test/java/com/bytechef/component/ai/agent/chat/memory/memory/InMemoryChatMemoryTenantIsolationTest.java`:

```java
/* Apache 2.0 header */
package com.bytechef.component.ai.agent.chat.memory.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.agent.chat.memory.memory.util.InMemoryChatMemoryRepositoryHolder;
import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;

class InMemoryChatMemoryTenantIsolationTest {

    @Test
    void testHolderReturnsDistinctRepositoriesPerTenant() {
        ChatMemoryRepository tenantA = TenantContext.callWithTenantId(
            "tenantA", InMemoryChatMemoryRepositoryHolder::getInstance);
        ChatMemoryRepository tenantB = TenantContext.callWithTenantId(
            "tenantB", InMemoryChatMemoryRepositoryHolder::getInstance);

        assertThat(tenantA).isNotSameAs(tenantB);
    }
}
```

NOTE: confirm the exact `TenantContext` API for running code under a tenant id (`grep -n "public static" server/libs/.../tenant/TenantContext.java`). If there is no `callWithTenantId`, use the existing set/reset pattern (`TenantContext.setCurrentTenantId("tenantA"); try { ... } finally { TenantContext.resetCurrentTenantId(); }`). This test passes today (the holder is already per-tenant) and acts as the regression guard for Step 3 — but first add a test that FAILS on the static capture, below.

- [ ] **Step 2: Add the test that exposes the static-capture bug**

The real defect is that `apply()` returns the class-load-time instance regardless of tenant. Add:

```java
    @Test
    void testApplyResolvesRepositoryInCallerTenantContext() {
        // The MessageWindowChatMemory returned by apply() must be backed by the CURRENT
        // tenant's repository, not the one captured at class-load. We assert that two
        // apply() calls under different tenants do not share the same underlying ChatMemory.
        ChatMemoryFunctionResultPair pair = invokeApplyUnderTwoTenants();

        assertThat(pair.tenantA()).isNotSameAs(pair.tenantB());
    }
```

Because `apply` needs `Parameters`/`ComponentConnection` args, write `invokeApplyUnderTwoTenants()` to call `InMemoryChatMemory.apply(params, params, params, Map.of())` under each tenant with a mocked `Parameters` returning a CONVERSATION_ID, and capture `result.chatMemory()` (the `ChatMemoryFunction.Result` accessor — inspect `ChatMemoryFunction.Result` for the real accessor name). Define the small `ChatMemoryFunctionResultPair` record inline in the test. With the current `static final` field, both calls return the same instance → FAIL.

- [ ] **Step 3: Run to verify the new test fails**

Run: `./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-in-memory:test --tests "*InMemoryChatMemoryTenantIsolationTest"`
Expected: `testApplyResolvesRepositoryInCallerTenantContext` FAILS (same instance returned); the holder test passes.

- [ ] **Step 4: Move the build into `apply()`**

Edit `InMemoryChatMemory.java` — delete the `static final` field and build per call:

```java
public class InMemoryChatMemory {

    public static final ClusterElementDefinition<ChatMemoryFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("In Memory Chat Memory")
            .description("Memory is retrieved and added as prior messages in the conversation.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .options(InMemoryChatMemoryUtils.getFirstMessages())
                    .required(true))
            .type(CHAT_MEMORY)
            .object(() -> InMemoryChatMemory::apply);

    protected static ChatMemoryFunction.Result apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) {

        MessageWindowChatMemory inMemoryChatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(InMemoryChatMemoryRepositoryHolder.getInstance())
            .build();

        return new ChatMemoryFunction.Result(
            MessageChatMemoryAdvisor.builder(inMemoryChatMemory)
                .build(),
            inMemoryChatMemory);
    }
}
```

- [ ] **Step 5: Run to verify pass**

Run: `./gradlew :server:libs:modules:components:ai:agent:chat-memory:chat-memory-in-memory:test --tests "*InMemoryChatMemoryTenantIsolationTest"`
Expected: PASS (both tests).

- [ ] **Step 6: Format and commit**

Run: `./gradlew spotlessApply :server:libs:modules:components:ai:agent:chat-memory:chat-memory-in-memory:compileJava`

```bash
git add server/libs/modules/components/ai/agent/chat-memory/chat-memory-in-memory/src/main/java/com/bytechef/component/ai/agent/chat/memory/memory/cluster/InMemoryChatMemory.java \
        server/libs/modules/components/ai/agent/chat-memory/chat-memory-in-memory/src/test/java/com/bytechef/component/ai/agent/chat/memory/memory/InMemoryChatMemoryTenantIsolationTest.java
git commit -m "gecko Resolve in-memory chat memory per-invocation for tenant isolation (T27)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 7: Audit remaining chat-memory backends + agents

**Files:** read-only sweep; fix only where the static-capture-of-scoped-state pattern recurs.

- [ ] **Step 1: Grep for the pattern**

Run:
```bash
grep -rn "private static final .*ChatMemory\|static final .*ChatMemoryRepository" \
  server/libs/modules/components/ai server/ee/libs/ai --include="*.java" | grep -v build
grep -rln "InMemoryChatMemoryRepositoryHolder\|getInstance()" server/libs server/ee --include="*.java" | grep -v build
```
Expected: a list of Jdbc/Mongo/Cosmos/Cassandra cluster elements and `LangchainAgent`/`SpringAIAgent`/`CopilotConfiguration`.

- [ ] **Step 2: Classify each hit**

For each file, determine whether a tenant- or user-scoped instance is captured in a `static`/singleton field and reused across invocations. JDBC/Mongo/etc. repositories that key rows by `conversationId` in a shared backing store are safe **iff** `conversationId` is globally unique (UUID/NanoID) — note this in the audit. Anything that statically captures per-tenant state gets the same per-invocation fix as Task 6.

- [ ] **Step 3: Apply fixes (only if needed) and commit**

For each genuinely-affected file, write a failing isolation test (mirror Task 6), apply the per-invocation fix, verify, and commit:
```bash
git commit -m "gecko Fix tenant isolation in <backend> chat memory (T27)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```
If the sweep finds nothing else broken, record that conclusion in the task and in `gecko-remediation-tasks.md` rather than forcing a change.

---

## Task 8: Close out the tracker

- [ ] **Step 1: Run the full check**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL. Fix any Spotless/Checkstyle/PMD/SpotBugs findings the new code introduced.

- [ ] **Step 2: Mark T26 and T27 done**

Edit `gecko-remediation-tasks.md`: change `- [ ] **T26.` → `- [x] **T26.` and `- [ ] **T27.` → `- [x] **T27.`, appending a short note that the TipTap XSS (7.6) and EMBED_INIT origin were verified already-mitigated and that the chat-memory backend audit (Task 7) concluded `<result>`.

- [ ] **Step 3: Commit**

```bash
git add gecko-remediation-tasks.md
git commit -m "gecko Mark T26 + T27 Phase 3 hardening done

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- T26.1 TOTP lockout → Task 1 ✓
- T26.2 `config()` allowlist → Task 2 ✓
- T26.3 activation enumeration → Task 3 ✓
- T26.4 EMBED_INIT origin → verified-done in spec; no task (intentional) ✓
- T27.5 Content-Disposition → Task 4 ✓
- T27.6 filesystem path safety (4 files) → Task 5 (+ follow-on note for the 3 storage classes) ✓
- T27.7 chat-memory isolation → Task 6 (in-memory) + Task 7 (audit other backends/agents) ✓
- T27 TipTap XSS → verified-done in spec; Task references the line-52 escaping residual to confirm — ADD: fold the line-52 confirmation into Task 7's sweep or note it is out of scope. (Out of scope per spec; confirmation is advisory.)

**Placeholder scan:** The `verifyUnderTest`/`accountController()`/`invokeApplyUnderTwoTenants()` seams are explicit "wire to the real constructor" instructions, not silent TODOs — each names exactly what to substitute and why the indirection exists (the real constructor signatures must be read from the codebase at implementation time). Acceptable.

**Type consistency:** `TotpLockedException` (Task 1) is referenced only in Task 1. `toContentDispositionHeaderValue` (Task 4) name is identical at definition and all three call sites. `InMemoryChatMemoryRepositoryHolder.getInstance()` matches the verified source. `ChatMemoryFunction.Result` accessor name is flagged for confirmation at implementation time (Task 6 Step 2) rather than guessed.
