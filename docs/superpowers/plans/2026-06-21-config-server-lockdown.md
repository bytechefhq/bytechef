# Config Server Lockdown Implementation Plan (T2 follow-up)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Require HTTP Basic auth on the Spring Cloud Config server and give every EE client the matching credentials, while leaving health probes open and the clients fail-open.

**Architecture:** Add `spring-boot-starter-security` + a `SecurityFilterChain` to `config-server-app` (permit `/actuator/health/**`, authenticate everything else, HTTP Basic, CSRF off, stateless), with the user from `spring.security.user.*` sourced from `BYTECHEF_CONFIG_SERVER_*`. Each of the 9 client apps adds `spring.cloud.config.username/password` to its local config.

**Tech Stack:** Java 25, Spring Security (HTTP Basic), Spring Cloud Config, JUnit 5 + Spring Boot test (`TestRestTemplate`).

## Global Constraints

- Files under `server/ee/` use the ByteChef Enterprise license header and a `@version ee` Javadoc tag; files under `server/libs/` use the Apache 2.0 header. (Spotless picks the header by file content.)
- Run `./gradlew spotlessApply` before every commit; per-module `:check` is the gate.
- Blank line before control statements and after a variable modification a later statement uses (Java style rules in CLAUDE.md).
- Integration test classes end in `IntTest`; unit test classes end in `Test`.
- Commit messages: server-side `gecko <description>`; end every commit body with the `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer.
- Branch is `0_732`, the user commits in parallel — never `git commit --amend`; make fresh commits, stage only files this plan touches.
- Credentials: `BYTECHEF_CONFIG_SERVER_USERNAME` (default `configserver`), `BYTECHEF_CONFIG_SERVER_PASSWORD` (dev default `dev-config-server-secret`; empty in prod base → must be set via env). Permit `/actuator/health/**`; authenticate everything else. Keep client `optional:` (fail-open).

---

## File Structure

**Task 1 — config server security:**
- Modify: `server/ee/apps/config-server-app/build.gradle.kts` (add security starter + test deps)
- Create: `server/ee/apps/config-server-app/src/main/java/com/bytechef/config/server/ConfigServerSecurityConfiguration.java`
- Modify: `server/ee/apps/config-server-app/src/main/resources/config/application.yml` (user from env)
- Modify: `server/ee/apps/config-server-app/src/main/resources/config/application-dev.yml` (dev password)
- Test: `server/ee/apps/config-server-app/src/test/java/com/bytechef/config/server/ConfigServerSecurityConfigurationIntTest.java`

**Task 2 — client credentials (9 apps):**
- Modify each `server/ee/apps/<app>/src/main/resources/config/application.yml` for: `scheduler-app`, `worker-app`, `webhook-app`, `api-gateway-app`, `coordinator-app`, `ai-gateway-app`, `connection-app`, `configuration-app`, `execution-app`.

**Task 3 — close out:**
- Modify: `gecko-remediation-tasks.md`

---

## Task 1: Lock down the config server with HTTP Basic auth

**Files:** see File Structure, Task 1.

**Interfaces:**
- Produces: a `SecurityFilterChain` on `config-server-app` that returns `401` for unauthenticated non-probe requests and `200` for valid Basic credentials; `/actuator/health/**` permitted. Credential from `spring.security.user.{name,password}`.

- [ ] **Step 1: Add the security + test dependencies**

In `server/ee/apps/config-server-app/build.gradle.kts`, add to `dependencies`:
```kotlin
    implementation("org.springframework.boot:spring-boot-starter-security")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
```

- [ ] **Step 2: Write the failing integration test**

Create `ConfigServerSecurityConfigurationIntTest.java`. It boots a minimal app (nested `@SpringBootApplication` importing only the security config + a stub controller) so it exercises the filter chain without starting `@EnableConfigServer`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.config.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ConfigServerSecurityConfigurationIntTest.TestApp.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.security.user.name=configserver",
        "spring.security.user.password=test-secret",
        "spring.cloud.config.enabled=false"
    })
class ConfigServerSecurityConfigurationIntTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void testUnauthenticatedRequestRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/test"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAuthenticatedRequestAllowed() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("configserver", "test-secret")
            .getForEntity(url("/test"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testWrongPasswordRejected() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("configserver", "wrong")
            .getForEntity(url("/test"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testHealthProbePermittedWithoutAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            url("/actuator/health/liveness"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @SpringBootApplication
    @Import(ConfigServerSecurityConfiguration.class)
    static class TestApp {

        @RestController
        static class StubController {

            @GetMapping("/test")
            String test() {
                return "ok";
            }

            // Stands in for the actuator liveness probe to verify the permit rule
            // without pulling the actuator endpoint infrastructure into the slice.
            @GetMapping("/actuator/health/liveness")
            String liveness() {
                return "UP";
            }
        }
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :server:ee:apps:config-server-app:test --tests "*ConfigServerSecurityConfigurationIntTest"`
Expected: FAIL — `ConfigServerSecurityConfiguration` not found (compile error).

- [ ] **Step 4: Create the security configuration**

Create `ConfigServerSecurityConfiguration.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.config.server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Requires HTTP Basic authentication for the config server. Kubernetes health probes
 * ({@code /actuator/health/**}) are left open; every other endpoint requires the configured
 * {@code spring.security.user} credentials.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@EnableWebSecurity
public class ConfigServerSecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(
                sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                authorize -> authorize
                    .requestMatchers("/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:ee:apps:config-server-app:test --tests "*ConfigServerSecurityConfigurationIntTest"`
Expected: PASS (4 tests). If `/actuator/health/liveness` returns 401, confirm the `requestMatchers("/actuator/health/**")` permit precedes `anyRequest().authenticated()`.

- [ ] **Step 6: Wire the credential from env (server config)**

In `server/ee/apps/config-server-app/src/main/resources/config/application.yml`, add a `security` block under `spring:` (sibling of `application`, `cloud`, `profiles`):
```yaml
spring:
  application:
    name: config-server-app
  cloud:
    config:
      server:
        native:
          searchLocations: classpath:/config/apps
  security:
    user:
      name: ${BYTECHEF_CONFIG_SERVER_USERNAME:configserver}
      password: ${BYTECHEF_CONFIG_SERVER_PASSWORD:}
  profiles:
    active: dev,native
```
(Merge into the existing `spring:` block; do not duplicate keys.)

In `server/ee/apps/config-server-app/src/main/resources/config/application-dev.yml`, add the dev password so local runs work:
```yaml
spring:
  security:
    user:
      password: dev-config-server-secret
```
(Merge into the existing `spring:` block.)

- [ ] **Step 7: Format and commit**

Run: `./gradlew :server:ee:apps:config-server-app:spotlessApply`

```bash
git add server/ee/apps/config-server-app/build.gradle.kts \
        server/ee/apps/config-server-app/src/main/java/com/bytechef/config/server/ConfigServerSecurityConfiguration.java \
        server/ee/apps/config-server-app/src/main/resources/config/application.yml \
        server/ee/apps/config-server-app/src/main/resources/config/application-dev.yml \
        server/ee/apps/config-server-app/src/test/java/com/bytechef/config/server/ConfigServerSecurityConfigurationIntTest.java
git commit -m "gecko Require HTTP Basic auth on the config server (T2)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Give the 9 client apps the config-server credentials

**Files:** the 9 EE app `application.yml` files.

**Interfaces:** consumes `BYTECHEF_CONFIG_SERVER_USERNAME` / `BYTECHEF_CONFIG_SERVER_PASSWORD`.

- [ ] **Step 1: Add credentials to each client config**

In **each** of these files:
```
server/ee/apps/scheduler-app/src/main/resources/config/application.yml
server/ee/apps/worker-app/src/main/resources/config/application.yml
server/ee/apps/webhook-app/src/main/resources/config/application.yml
server/ee/apps/api-gateway-app/src/main/resources/config/application.yml
server/ee/apps/coordinator-app/src/main/resources/config/application.yml
server/ee/apps/ai-gateway-app/src/main/resources/config/application.yml
server/ee/apps/connection-app/src/main/resources/config/application.yml
server/ee/apps/configuration-app/src/main/resources/config/application.yml
server/ee/apps/execution-app/src/main/resources/config/application.yml
```
replace the (identical) block:
```yaml
  config:
    import: optional:configserver:http://localhost:6111
```
with:
```yaml
  cloud:
    config:
      username: ${BYTECHEF_CONFIG_SERVER_USERNAME:configserver}
      password: ${BYTECHEF_CONFIG_SERVER_PASSWORD:dev-config-server-secret}
  config:
    import: optional:configserver:http://localhost:6111
```
This adds a `spring.cloud.config` credentials block (2-space indent under `spring:`) immediately before the existing `spring.config.import`. The `optional:` prefix is preserved (fail-open). For `worker-app` and `configuration-app` the same `config:` / `import:` snippet appears (at different line numbers) — the replacement text is identical; `configuration-app` already has a commented-out `# cloud:` block above, which is inert and can remain.

NOTE: the dev default `dev-config-server-secret` must match the config server's dev password from Task 1 Step 6.

- [ ] **Step 2: Sanity-check YAML for all nine**

Run:
```bash
for app in scheduler-app worker-app webhook-app api-gateway-app coordinator-app ai-gateway-app connection-app configuration-app execution-app; do
  echo "--- $app"; grep -n "cloud:\|config:\|username:\|password:\|import:" server/ee/apps/$app/src/main/resources/config/application.yml | head
done
```
Expected: each shows a `cloud:` → `config:` → `username:`/`password:` block and the unchanged `config: import: optional:configserver:...`. Verify indentation (username/password at 6 spaces under `cloud: config:`).

- [ ] **Step 3: Commit**

```bash
git add server/ee/apps/scheduler-app/src/main/resources/config/application.yml \
        server/ee/apps/worker-app/src/main/resources/config/application.yml \
        server/ee/apps/webhook-app/src/main/resources/config/application.yml \
        server/ee/apps/api-gateway-app/src/main/resources/config/application.yml \
        server/ee/apps/coordinator-app/src/main/resources/config/application.yml \
        server/ee/apps/ai-gateway-app/src/main/resources/config/application.yml \
        server/ee/apps/connection-app/src/main/resources/config/application.yml \
        server/ee/apps/configuration-app/src/main/resources/config/application.yml \
        server/ee/apps/execution-app/src/main/resources/config/application.yml
git commit -m "gecko Authenticate config-server clients with Basic credentials (T2)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Close out the tracker

- [ ] **Step 1: Check the config-server module**

Run: `./gradlew :server:ee:apps:config-server-app:check`
Expected: BUILD SUCCESSFUL. Fix any checkstyle/PMD/SpotBugs findings the new code introduces.

- [ ] **Step 2: Mark T2 fully done in the tracker**

In `gecko-remediation-tasks.md`, change `- [~] **T2.` to `- [x] **T2.` and append a line after the existing "/remote auth done" note:
> **Config-server lockdown done** (spec/plan `docs/superpowers/{specs,plans}/2026-06-21-config-server-lockdown*`): `config-server-app` now requires HTTP Basic auth (new `ConfigServerSecurityConfiguration` — permits `/actuator/health/**`, authenticates everything else, CSRF off, stateless), credential from `spring.security.user.*` via `BYTECHEF_CONFIG_SERVER_USERNAME`/`PASSWORD` (dev default for local). All 9 client apps send `spring.cloud.config.username/password`; `optional:` import kept (fail-open — a cred mismatch degrades a client to local config rather than crashing). T2 complete.

- [ ] **Step 3: Commit**

```bash
git add gecko-remediation-tasks.md
git commit -m "gecko Mark T2 complete (config-server lockdown done)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- Server security dep + `SecurityFilterChain` (probe permit, Basic, CSRF off, stateless) → Task 1 ✓
- Credential from `spring.security.user.*` via env + dev default → Task 1 Steps 6 ✓
- 9 client credential blocks, `optional:` kept → Task 2 ✓
- Dedicated `BYTECHEF_CONFIG_SERVER_*` creds → Tasks 1 & 2 ✓
- Sliced security integration test (401/200/probe) → Task 1 Step 2 ✓
- Fail-open caveat → recorded in Task 3 tracker note ✓
- Out of scope (fail-closed posture, Vault) → not implemented (correct) ✓

**Placeholder scan:** No TBD/TODO; every code/config step shows full content. The Task 2 per-file note about line numbers differing is an explicit "same text, different location" instruction, not a gap.

**Type consistency:** `ConfigServerSecurityConfiguration` (class + `filterChain` bean) matches its test `@Import`. Property keys `spring.security.user.name/password` (server) and `spring.cloud.config.username/password` (clients) both resolve `BYTECHEF_CONFIG_SERVER_USERNAME`/`BYTECHEF_CONFIG_SERVER_PASSWORD`; the dev default `dev-config-server-secret` is identical on both sides. The test's `spring.cloud.config.enabled=false` prevents the test app from trying to contact a config server.
