# T2 Internal /remote Service Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Authenticate internal `/remote/**` microservice calls with a shared service token — a fail-closed servlet filter on the receiving side and an outgoing header on every client call.

**Architecture:** A new `@Component OncePerRequestFilter` in `remote-rest` validates the `X-Bytechef-Internal-Token` header on `/remote/**` (constant-time, fail-closed), ordered before `RemoteMultiTenantFilter`. The single client chokepoint `AbstractRestClient` adds the token (and the existing tenant header) to every call via a `headers()` consumer. The token comes from `bytechef.internal.service-token`.

**Tech Stack:** Java 25, Spring servlet filters + RestClient, JUnit 5 + Mockito + AssertJ.

## Global Constraints

- Files under `server/ee/` use the ByteChef Enterprise license header and a `@version ee` Javadoc tag; files under `server/libs/` use the Apache 2.0 header. (Spotless picks the header by file content.)
- Run `./gradlew spotlessApply` before every commit; per-module `:check` is the gate.
- Blank line before control statements and after a variable modification a later statement uses (Java style rules in CLAUDE.md).
- Test method names are camelCase without underscores; unit test classes end in `Test`.
- Commit messages: server-side `gecko <description>`; end every commit body with the `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer.
- Branch is `0_732`, the user commits in parallel — never `git commit --amend`; make fresh commits, stage only files this plan touches.
- Property `bytechef.internal.service-token`; header constant `TenantConstants.INTERNAL_SERVICE_TOKEN` = `"X-Bytechef-Internal-Token"`. Fail-closed: blank/unconfigured server token → reject. Constant-time compare via `java.security.MessageDigest.isEqual`.
- Out of scope: Spring Cloud Config server lockdown (separate follow-up), mTLS, per-user identity over `/remote`.

---

## File Structure

**Task 1 — shared constant + server filter:**
- Modify: `server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/constant/TenantConstants.java` (add `INTERNAL_SERVICE_TOKEN`)
- Create: `server/ee/libs/core/remote/remote-rest/src/main/java/com/bytechef/ee/remote/web/filter/RemoteServiceAuthenticationFilter.java`
- Modify: `server/ee/libs/core/remote/remote-rest/src/main/java/com/bytechef/ee/remote/web/filter/RemoteMultiTenantFilter.java` (add `@Order`)
- Test: `server/ee/libs/core/remote/remote-rest/src/test/java/com/bytechef/ee/remote/web/filter/RemoteServiceAuthenticationFilterTest.java`

**Task 2 — client outgoing header:**
- Modify: `server/ee/libs/core/remote/remote-client/src/main/java/com/bytechef/ee/remote/client/AbstractRestClient.java`
- Modify: `server/ee/libs/core/remote/remote-client/build.gradle.kts` (test deps, if missing)
- Test: `server/ee/libs/core/remote/remote-client/src/test/java/com/bytechef/ee/remote/client/AbstractRestClientHeadersTest.java`

**Task 3 — distribute the token (config):**
- Modify: `server/ee/apps/config-server-app/src/main/resources/config/apps/application.yml` (env-ref, fail-closed)
- Modify: the dev overlay (`.../config/apps/application-dev.yml` if present, else `.../config/application-dev.yml`) — dev default token

**Task 4 — close out:**
- Modify: `gecko-remediation-tasks.md`

---

## Task 1: Shared constant + server-side `RemoteServiceAuthenticationFilter`

**Files:** see File Structure, Task 1.

**Interfaces:**
- Produces: `TenantConstants.INTERNAL_SERVICE_TOKEN` (String `"X-Bytechef-Internal-Token"`); `RemoteServiceAuthenticationFilter(@Value String serviceToken)` — a servlet filter that rejects (`401`) any `/remote/**` request lacking a matching token, fail-closed.

- [ ] **Step 1: Add the header constant**

In `TenantConstants.java`, add the constant:
```java
    public static final String CURRENT_TENANT_ID = "CURRENT_TENANT_ID";
    public static final String INTERNAL_SERVICE_TOKEN = "X-Bytechef-Internal-Token";
    public static final String TENANT_PREFIX = "bytechef";
```
(It lives here because `tenant-api` is the only module both `remote-rest` and `remote-client` depend on, and it already hosts the transport-header constant `CURRENT_TENANT_ID`.)

- [ ] **Step 2: Write the failing filter test**

Create `RemoteServiceAuthenticationFilterTest.java`:

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
class RemoteServiceAuthenticationFilterTest {

    @Test
    void testValidTokenProceeds() throws Exception {
        RemoteServiceAuthenticationFilter filter = new RemoteServiceAuthenticationFilter("secret-token");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.INTERNAL_SERVICE_TOKEN)).thenReturn("secret-token");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void testMissingTokenRejected() throws Exception {
        RemoteServiceAuthenticationFilter filter = new RemoteServiceAuthenticationFilter("secret-token");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.INTERNAL_SERVICE_TOKEN)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void testWrongTokenRejected() throws Exception {
        RemoteServiceAuthenticationFilter filter = new RemoteServiceAuthenticationFilter("secret-token");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.INTERNAL_SERVICE_TOKEN)).thenReturn("wrong");

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void testBlankServerTokenRejected() throws Exception {
        RemoteServiceAuthenticationFilter filter = new RemoteServiceAuthenticationFilter("");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(TenantConstants.INTERNAL_SERVICE_TOKEN)).thenReturn("anything");

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(401);
        verify(chain, never()).doFilter(any(), any());
    }
}
```

NOTE: `doFilterInternal` is `protected` in `OncePerRequestFilter`; the test is in the same package so it can call it. Confirm the module has mockito/junit on its test classpath (it does — `RemoteMultiTenantFilterTest` lives here).

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:core:remote:remote-rest:test --tests "*RemoteServiceAuthenticationFilterTest"`
Expected: FAIL — `RemoteServiceAuthenticationFilter` not found.

- [ ] **Step 4: Create the filter**

Create `RemoteServiceAuthenticationFilter.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.web.filter;

import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates internal {@code /remote/**} microservice calls with a shared service token. Fail-closed: if no token is
 * configured, or the request token is missing or does not match, the request is rejected with 401. Runs before
 * {@link RemoteMultiTenantFilter} so an unauthenticated caller never establishes tenant context.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Order(1)
public class RemoteServiceAuthenticationFilter extends OncePerRequestFilter {

    private static final PathPatternRequestMatcher.Builder BUILDER = PathPatternRequestMatcher.withDefaults();

    private static final RequestMatcher REQUEST_MATCHER = new NegatedRequestMatcher(BUILDER.matcher("/remote/**"));

    private final String serviceToken;

    public RemoteServiceAuthenticationFilter(
        @Value("${bytechef.internal.service-token:}") String serviceToken) {

        this.serviceToken = serviceToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String providedToken = request.getHeader(TenantConstants.INTERNAL_SERVICE_TOKEN);

        if (serviceToken == null || serviceToken.isBlank() || providedToken == null || providedToken.isBlank() ||
            !MessageDigest.isEqual(
                serviceToken.getBytes(StandardCharsets.UTF_8), providedToken.getBytes(StandardCharsets.UTF_8))) {

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return REQUEST_MATCHER.matches(request);
    }
}
```

- [ ] **Step 5: Order RemoteMultiTenantFilter after the auth filter**

In `RemoteMultiTenantFilter.java`, add an `@Order(2)` annotation on the class (so the auth filter at `@Order(1)` runs first) and the import:
```java
import org.springframework.core.annotation.Order;
```
```java
@Component
@Order(2)
public class RemoteMultiTenantFilter extends OncePerRequestFilter {
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:core:remote:remote-rest:test --tests "*RemoteServiceAuthenticationFilterTest"`
Expected: PASS (4 tests).

- [ ] **Step 7: Format and commit**

Run: `./gradlew :server:libs:core:tenant:tenant-api:spotlessApply :server:ee:libs:core:remote:remote-rest:spotlessApply`

```bash
git add server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/constant/TenantConstants.java \
        server/ee/libs/core/remote/remote-rest/src/main/java/com/bytechef/ee/remote/web/filter/RemoteServiceAuthenticationFilter.java \
        server/ee/libs/core/remote/remote-rest/src/main/java/com/bytechef/ee/remote/web/filter/RemoteMultiTenantFilter.java \
        server/ee/libs/core/remote/remote-rest/src/test/java/com/bytechef/ee/remote/web/filter/RemoteServiceAuthenticationFilterTest.java
git commit -m "gecko Authenticate /remote calls with a fail-closed service-token filter (T2)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Client — attach the token on every `/remote` call

**Files:** see File Structure, Task 2.

**Interfaces:**
- Consumes: `TenantConstants.CURRENT_TENANT_ID`, `TenantConstants.INTERNAL_SERVICE_TOKEN`.
- Produces: `AbstractRestClient.headers()` — a package-private `Consumer<HttpHeaders>` that sets both the tenant header and the service-token header; every method applies it via `.headers(headers())`.

- [ ] **Step 1: Confirm test deps on remote-client**

Run: `grep -n "testImplementation\|mockito\|junit\|assertj\|spring-test" server/ee/libs/core/remote/remote-client/build.gradle.kts`
If junit/assertj/`spring-test` (for `ReflectionTestUtils`) are not present, add to `dependencies`:
```kotlin
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework:spring-test")
```
(`HttpHeaders` comes from `spring-web`, already on the main classpath via RestClient.)

- [ ] **Step 2: Write the failing test**

Create `AbstractRestClientHeadersTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AbstractRestClientHeadersTest {

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testHeadersSetsTenantAndServiceToken() {
        DefaultRestClient client = new DefaultRestClient();

        ReflectionTestUtils.setField(client, "serviceToken", "secret-token");

        TenantContext.setCurrentTenantId("public");

        HttpHeaders httpHeaders = new HttpHeaders();

        client.headers()
            .accept(httpHeaders);

        assertThat(httpHeaders.getFirst(TenantConstants.CURRENT_TENANT_ID)).isEqualTo("public");
        assertThat(httpHeaders.getFirst(TenantConstants.INTERNAL_SERVICE_TOKEN)).isEqualTo("secret-token");
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:core:remote:remote-client:test --tests "*AbstractRestClientHeadersTest"`
Expected: FAIL — `headers()` / `serviceToken` not found.

- [ ] **Step 4: Add the token field + `headers()` consumer and apply it**

In `AbstractRestClient.java`:

Add imports:
```java
import com.bytechef.tenant.constant.TenantConstants;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
```
(`TenantConstants` is already imported; keep one.)

Add the field (after `private final RestClient restClient;`):
```java
    @Value("${bytechef.internal.service-token:}")
    private String serviceToken;
```

Add the consumer (package-private so the test can call it):
```java
    Consumer<HttpHeaders> headers() {
        return httpHeaders -> {
            httpHeaders.set(TenantConstants.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());
            httpHeaders.set(TenantConstants.INTERNAL_SERVICE_TOKEN, serviceToken);
        };
    }
```

Replace **every** occurrence of:
```java
            .header(TenantConstants.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId())
```
with:
```java
            .headers(headers())
```
There are 10 such occurrences (get/delete/post/put variants). After replacement, the per-method bodies read e.g.:
```java
    @Retryable
    public void get(Function<UriBuilder, URI> uriFunction) {
        restClient
            .get()
            .uri(uriFunction)
            .headers(headers())
            .retrieve()
            .toBodilessEntity();
    }
```
and for the body methods:
```java
        RestClient.RequestBodySpec requestBodySpec = restClient
            .post()
            .uri(uriFunction)
            .headers(headers());
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:core:remote:remote-client:test --tests "*AbstractRestClientHeadersTest"`
Expected: PASS (1 test).

- [ ] **Step 6: Compile the module**

Run: `./gradlew :server:ee:libs:core:remote:remote-client:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Format and commit**

Run: `./gradlew :server:ee:libs:core:remote:remote-client:spotlessApply`

```bash
git add server/ee/libs/core/remote/remote-client/src/main/java/com/bytechef/ee/remote/client/AbstractRestClient.java \
        server/ee/libs/core/remote/remote-client/build.gradle.kts \
        server/ee/libs/core/remote/remote-client/src/test/java/com/bytechef/ee/remote/client/AbstractRestClientHeadersTest.java
git commit -m "gecko Attach internal service token on outbound /remote calls (T2)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Distribute the token via config

**Files:** EE config-server app config.

**Interfaces:** consumes `bytechef.internal.service-token`.

- [ ] **Step 1: Add the prod-ready, fail-closed property to the shared app config**

In `server/ee/apps/config-server-app/src/main/resources/config/apps/application.yml`, add under the `bytechef:` tree (create the key path if absent):
```yaml
bytechef:
  internal:
    service-token: ${BYTECHEF_INTERNAL_SERVICE_TOKEN:}
```
This resolves to the env var in production, and to **empty** when unset — which the filter treats as fail-closed (rejects all `/remote`). Verify the exact existing `bytechef:` block indentation in the file and merge rather than duplicate the key.

- [ ] **Step 2: Add a dev default so local distributed dev / integration works**

Find the dev overlay served to apps:
`ls server/ee/apps/config-server-app/src/main/resources/config/apps/ | grep dev` and `ls .../config/ | grep dev`.
In the dev overlay that applies to all apps (prefer `.../config/apps/application-dev.yml`; if it does not exist, use `.../config/application-dev.yml`), set a concrete dev token:
```yaml
bytechef:
  internal:
    service-token: dev-internal-service-token
```
This keeps `dev`-profile distributed runs working without an env var, while production stays fail-closed (no default). Merge into any existing `bytechef:` block.

- [ ] **Step 3: Commit**

```bash
git add server/ee/apps/config-server-app/src/main/resources/config/apps/application.yml
# also add the dev overlay file you edited in Step 2
git commit -m "gecko Distribute internal service token to EE apps (T2)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Close out the tracker

- [ ] **Step 1: Run check on both remote modules**

Run:
```bash
./gradlew \
  :server:libs:core:tenant:tenant-api:check \
  :server:ee:libs:core:remote:remote-rest:check \
  :server:ee:libs:core:remote:remote-client:check
```
Expected: BUILD SUCCESSFUL. Fix any checkstyle/PMD/SpotBugs findings the new code introduces. If a pre-existing test in one of these modules is already broken on the branch (unrelated), confirm it fails without these changes and note it rather than fixing out-of-scope code.

- [ ] **Step 2: Mark T2 partial in the tracker**

In `gecko-remediation-tasks.md`, change `- [ ] **T2.` to `- [~] **T2.` and append:
> **/remote auth done** (spec/plan `docs/superpowers/{specs,plans}/2026-06-21-remote-service-auth*`): added a fail-closed `RemoteServiceAuthenticationFilter` (`@Component` servlet filter in `remote-rest`, ordered before `RemoteMultiTenantFilter`) that validates a shared `X-Bytechef-Internal-Token` header (constant-time) on every `/remote/**` request; the token is attached on all outbound calls at the `AbstractRestClient` chokepoint and distributed via `bytechef.internal.service-token` (env var in prod, dev default for local). Servlet filter (not a Spring Security chain) because lightweight EE apps have no Spring Security. This is service-to-service authentication, orthogonal to the facade-tier user authorization (the `/remote` services have no `@PreAuthorize` and no user principal by design). **Still open (deferred):** Spring Cloud Config server lockdown (`@EnableConfigServer`) — separate follow-up.

- [ ] **Step 3: Commit**

```bash
git add gecko-remediation-tasks.md
git commit -m "gecko Mark T2 /remote service-auth done (config-server lockdown deferred)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- Shared token + header constant → Task 1 (constant) + Task 3 (property) ✓
- Server filter (fail-closed, constant-time, ordered before tenant filter, servlet not Spring Security) → Task 1 ✓
- Client header at AbstractRestClient chokepoint → Task 2 ✓
- Token distribution (prod env var + dev default) → Task 3 ✓
- Editions (mono unaffected) → no code needed; remote-client is EE-only and filter only fires on `/remote` ✓
- Out of scope (config server, mTLS, per-user identity) → recorded in Task 4 tracker note ✓

**Placeholder scan:** No TBD/TODO; every code step shows full code. The Task 3 dev-overlay "find the file" steps are explicit `ls`-then-edit instructions (the exact dev filename must be confirmed against the repo), not silent gaps.

**Type consistency:** `TenantConstants.INTERNAL_SERVICE_TOKEN` is defined in Task 1 and used by the filter (Task 1) and the client `headers()` (Task 2). `RemoteServiceAuthenticationFilter(String serviceToken)` constructor matches its test usage. `AbstractRestClient.headers()` (package-private `Consumer<HttpHeaders>`) matches the test's `client.headers().accept(...)` and the private `serviceToken` field set via `ReflectionTestUtils`. Property key `bytechef.internal.service-token` is identical across the filter `@Value`, the client `@Value`, and the YAML.
