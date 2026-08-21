# MCP Server API Key Authentication (Phase 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make a valid Bearer `ApiKey` mandatory on the automation (`/api/automation/{secretKey}/mcp`) and management (`/api/management/{secretKey}/mcp`) MCP endpoints, built on `org.springaicommunity:mcp-server-security` 0.1.13.

**Architecture:** Shared adapter classes in `platform-security-web-api` bridge the mcp-security library to ByteChef: a tenant-aware subclass of the library's `ApiKeyAuthenticationFilter` wraps request processing in `TenantContext`, a custom `AuthenticationConverter` reads `Authorization: Bearer <secretKey>` and captures the URL path secret, and an `ApiKeyEntityRepository` implementation delegates to `ApiKeyService`. Each MCP server module gets a thin `AuthenticationProvider` decorator around the library's `ApiKeyAuthenticationProvider` that adds ByteChef-specific checks (key type, path-secret identity, environment) and a rewritten security configurer. Spec: `docs/superpowers/specs/2026-07-05-mcp-server-api-key-oauth2-auth-design.md`.

**Deviation from spec (approved rationale):** The spec placed the automation environment check in the MCP tool filter because "the security filter doesn't know which `McpServer` the path targets." During planning we found the authentication converter *does* see the servlet path, so the path secret is captured into the credentials object and the check happens in the automation `AuthenticationProvider` decorator instead — all checks in one layer, tool filter untouched. Behavior is identical (401 on mismatch).

**Correction to the spec's error-handling note (verified 2026-07-05, post-implementation):** The spec described the 401 as carrying "a JSON problem body, matching the public API." That justification is inaccurate — ByteChef's public `ApiKeyAuthenticationFilter` and the global `/api/**` chain in `SecurityConfiguration` both use `HttpStatusEntryPoint(UNAUTHORIZED)`, i.e. a **bare 401 with an empty body**. The MCP endpoints inherit the same behavior through the library's default entry point, so they already match the public API's error shape. The follow-up "gap 2" (wire a JSON problem body) is therefore closed as **"already matches — no change needed"**; the only addition is an assertion, in the end-to-end integration tests, that the no-Bearer 401 has an empty body, to lock the contract.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Security 7, Spring AI 2.0.0, MCP SDK 2.0.0, `org.springaicommunity:mcp-server-security:0.1.13`, JUnit 5, Mockito, MockMvc + spring-security-test.

## Global Constraints

- Library version: `org.springaicommunity:mcp-server-security:0.1.13` (validated upstream against Boot 4.1.0 / Spring AI 2.0.0 / MCP SDK 2.0.0; ByteChef is on Boot 4.0.7 — Task 1 is the compatibility gate).
- Embedded MCP server (`server/ee/libs/embedded/...`) is **out of scope** — do not touch it.
- No Liquibase changes — the `api_key` table is reused as-is.
- Run `./gradlew spotlessApply` before every commit; commit only files this plan touches.
- Java style (from CLAUDE.md): one blank line before control statements; blank line between a variable modification and a statement using it; no blank line before class-closing `}`; no `TODO:` comments; test method names camelCase without underscores (applies to helpers too); descriptive variable names, no single-letter lambda params.
- Commit messages: plain imperative description, no ticket number (none exists), e.g. `Add mcp-server-security dependency`.
- All new files under `server/libs/` get the Apache 2.0 license header (copy from any existing file in the same module) and a `@author Ivica Cardic` Javadoc tag, matching neighbors.
- Gradle module paths used below:
  - `:server:libs:platform:platform-security-web:platform-security-web-api`
  - `:server:libs:platform:platform-security:platform-security-api`
  - `:server:libs:platform:platform-security:platform-security-service`
  - `:server:libs:automation:automation-ai:automation-ai-mcp-server`
  - `:server:libs:ai:ai-mcp:ai-mcp-server`

## Key library facts (verified against v0.1.13 source — do not re-derive)

- `org.springaicommunity.mcp.security.server.apikey.ApiKey` — interface: `String getId()`, `@Nullable String getSecret()`.
- `...apikey.ApiKeyEntity` — interface extending `ApiKey, CredentialsContainer`, adds `default List<GrantedAuthority> getAuthorities()` and `<T extends ApiKeyEntity> T copy()`.
- `...apikey.ApiKeyEntityRepository<T extends ApiKeyEntity>` — single method `@Nullable T findByKeyId(String keyId)`.
- `...apikey.authentication.ApiKeyAuthenticationProvider<T>` — looks up `findByKeyId(credentials.getId())`, then `passwordEncoder.matches(credentials.getSecret(), entity.getSecret())` with a **delegating password encoder** — so entity secrets returned as `"{noop}" + plaintext` compare correctly against the raw presented secret.
- `...apikey.authentication.ApiKeyAuthenticationToken` — `unauthenticated(ApiKey)` / `authenticated(ApiKeyEntity, authorities)`; `getCredentials()` returns the presented `ApiKey`, `getPrincipal()` the stored entity.
- `...apikey.web.ApiKeyAuthenticationFilter` — extends Spring Security's `AuthenticationFilter` (`OncePerRequestFilter`; `doFilterInternal` is overridable, `setRequestMatcher(...)` available); on `AuthenticationException` → 401 via `HttpStatusEntryPoint`; a converter that **throws** `BadCredentialsException` therefore produces 401.
- ByteChef facts: `ApiKey.secretKey` is generated as `String.valueOf(TenantKey.of())` → `TenantKey.parse(<presented secret>)` yields the tenant. `TenantContext.runWithTenantId(String, TenantContext.Runnable)` accepts a throwing lambda. `SecurityUtils.extractPrincipal` only understands `UserDetails` or `String` principals → the new entity **must implement `UserDetails`** or auditing/`@PreAuthorize` user resolution breaks.

---

### Task 1: Add mcp-server-security dependency + compatibility smoke test

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `server/libs/platform/platform-security-web/platform-security-web-api/build.gradle.kts`
- Test: `server/libs/platform/platform-security-web/platform-security-web-api/src/test/java/com/bytechef/platform/security/web/mcp/McpServerSecurityLibrarySmokeTest.java`

**Interfaces:**
- Produces: version catalog accessor `libs.org.springaicommunity.mcp.server.security`, exported (`api`) from `platform-security-web-api` so both MCP server modules see library types transitively.

- [ ] **Step 1: Add the version catalog entries**

In `gradle/libs.versions.toml`, add to the `[versions]` block (alphabetically near the other `springaicommunity-*` entries, around line 28):

```toml
springaicommunity-mcp-security = "0.1.13"
```

and to the `[libraries]` block (near the other `org-springaicommunity-*` entries, around line 70):

```toml
org-springaicommunity-mcp-server-security = { module = "org.springaicommunity:mcp-server-security", version.ref = "springaicommunity-mcp-security" }
```

- [ ] **Step 2: Add dependencies to platform-security-web-api**

In `server/libs/platform/platform-security-web/platform-security-web-api/build.gradle.kts`, the `dependencies` block currently has three `api(...)`, two `implementation(...)`, one `compileOnly(...)` entries and no test dependencies. Make it:

```kotlin
dependencies {
    api("org.springframework.security:spring-security-config")
    api("org.springframework.security:spring-security-web")
    api(libs.org.springaicommunity.mcp.server.security)
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework:spring-test")
}
```

- [ ] **Step 3: Verify no dependency cycle**

Run: `./gradlew :server:libs:platform:platform-security-web:platform-security-web-api:compileJava`
Expected: `BUILD SUCCESSFUL`. If Gradle reports a circular dependency involving `platform-security-api` or `platform-user-api`, stop and report — do not work around it silently.

- [ ] **Step 4: Write the smoke test (the day-one compatibility gate)**

Create `McpServerSecurityLibrarySmokeTest.java` (package `com.bytechef.platform.security.web.mcp`):

```java
package com.bytechef.platform.security.web.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.ApiKey;
import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntity;
import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntityRepository;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationProvider;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Validates that mcp-server-security 0.1.13 authenticates against ByteChef's intended usage pattern (opaque secret as
 * key id, {@code {noop}}-prefixed stored secret) on this project's Spring Boot version.
 *
 * @author Ivica Cardic
 */
class McpServerSecurityLibrarySmokeTest {

    private static final String SECRET_KEY = "bytechef_sk_smoke_test";

    private final ApiKeyEntityRepository<StubApiKeyEntity> apiKeyEntityRepository =
        keyId -> SECRET_KEY.equals(keyId) ? new StubApiKeyEntity(SECRET_KEY, "{noop}" + SECRET_KEY) : null;

    private final ApiKeyAuthenticationProvider<StubApiKeyEntity> apiKeyAuthenticationProvider =
        new ApiKeyAuthenticationProvider<>(apiKeyEntityRepository);

    @Test
    void testAuthenticateWithNoopEncodedSecretSucceeds() {
        Authentication authentication = apiKeyAuthenticationProvider.authenticate(
            ApiKeyAuthenticationToken.unauthenticated(new StubApiKey(SECRET_KEY)));

        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    void testAuthenticateWithUnknownSecretFails() {
        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> apiKeyAuthenticationProvider.authenticate(
                ApiKeyAuthenticationToken.unauthenticated(new StubApiKey("unknown"))));
    }

    private record StubApiKey(String secretKey) implements ApiKey {

        @Override
        public String getId() {
            return secretKey;
        }

        @Override
        public String getSecret() {
            return secretKey;
        }
    }

    private static final class StubApiKeyEntity implements ApiKeyEntity {

        private final String id;
        private String secret;

        private StubApiKeyEntity(String id, String secret) {
            this.id = id;
            this.secret = secret;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getSecret() {
            return secret;
        }

        @Override
        public List<GrantedAuthority> getAuthorities() {
            return List.of();
        }

        @Override
        public void eraseCredentials() {
            secret = null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ApiKeyEntity> T copy() {
            return (T) new StubApiKeyEntity(id, secret);
        }
    }
}
```

- [ ] **Step 5: Run the smoke test**

Run: `./gradlew :server:libs:platform:platform-security-web:platform-security-web-api:test --tests "com.bytechef.platform.security.web.mcp.McpServerSecurityLibrarySmokeTest"`
Expected: 2 tests PASS. If the library fails to resolve or throws at runtime on Boot 4.0.7, STOP — report the incompatibility; the fallback decision (bump Boot to 4.1.x) belongs to the user.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add gradle/libs.versions.toml server/libs/platform/platform-security-web/platform-security-web-api
git commit -m "Add mcp-server-security dependency and compatibility smoke test"
```

---

### Task 2: ApiKeyService — fetchApiKey and updateLastUsedDate

**Files:**
- Modify: `server/libs/platform/platform-security/platform-security-api/src/main/java/com/bytechef/platform/security/service/ApiKeyService.java`
- Modify: `server/libs/platform/platform-security/platform-security-api/src/main/java/com/bytechef/platform/security/domain/ApiKey.java`
- Modify: `server/libs/platform/platform-security/platform-security-service/src/main/java/com/bytechef/platform/security/service/ApiKeyServiceImpl.java`
- Modify: `server/libs/platform/platform-security/platform-security-service/build.gradle.kts` (add test deps — the module has none)
- Test: `server/libs/platform/platform-security/platform-security-service/src/test/java/com/bytechef/platform/security/service/ApiKeyServiceTest.java`

**Interfaces:**
- Produces: `Optional<ApiKey> fetchApiKey(String secretKey)` and `void updateLastUsedDate(long id)` on `ApiKeyService`; `ApiKey.setLastUsedDate(Instant lastUsedDate)`.
- Consumed by: Task 3's `McpApiKeyEntityRepository` and Tasks 4/6 providers.

- [ ] **Step 1: Add test dependencies**

In `server/libs/platform/platform-security/platform-security-service/build.gradle.kts`, append to the `dependencies` block:

```kotlin
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
```

- [ ] **Step 2: Write the failing test**

Create `ApiKeyServiceTest.java` (note: interface-named, no `Impl`, per project convention):

```java
package com.bytechef.platform.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.domain.ApiKey;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class ApiKeyServiceTest {

    private final com.bytechef.platform.security.repository.ApiKeyRepository apiKeyRepository =
        mock(com.bytechef.platform.security.repository.ApiKeyRepository.class);
    private final ApiKeyService apiKeyService = new ApiKeyServiceImpl(apiKeyRepository);

    @Test
    void testFetchApiKeyReturnsMatchingApiKey() {
        ApiKey apiKey = new ApiKey();

        when(apiKeyRepository.findBySecretKey("secret")).thenReturn(Optional.of(apiKey));

        assertThat(apiKeyService.fetchApiKey("secret")).contains(apiKey);
    }

    @Test
    void testFetchApiKeyReturnsEmptyWhenMissing() {
        when(apiKeyRepository.findBySecretKey("missing")).thenReturn(Optional.empty());

        assertThat(apiKeyService.fetchApiKey("missing")).isEmpty();
    }

    @Test
    void testUpdateLastUsedDateSetsTimestampAndSaves() {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(17L);

        when(apiKeyRepository.findById(17L)).thenReturn(Optional.of(apiKey));

        apiKeyService.updateLastUsedDate(17L);

        ArgumentCaptor<ApiKey> apiKeyArgumentCaptor = ArgumentCaptor.forClass(ApiKey.class);

        verify(apiKeyRepository).save(apiKeyArgumentCaptor.capture());

        ApiKey savedApiKey = apiKeyArgumentCaptor.getValue();

        assertThat(savedApiKey.getLastUsedDate()).isNotNull();
    }
}
```

Note: `ApiKeyRepository` lives in `platform-security-service` (`com.bytechef.platform.security.repository`), same module as the test — a plain import is fine; the fully-qualified form above is only to make the location explicit. Use a normal import in the actual file.

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :server:libs:platform:platform-security:platform-security-service:test --tests "com.bytechef.platform.security.service.ApiKeyServiceTest"`
Expected: COMPILE FAILURE — `fetchApiKey`, `updateLastUsedDate`, `setLastUsedDate` do not exist.

- [ ] **Step 4: Implement**

In `ApiKeyService.java`, add (keep methods alphabetically ordered with the existing ones):

```java
Optional<ApiKey> fetchApiKey(String secretKey);

void updateLastUsedDate(long id);
```

(Add `import java.util.Optional;` if absent.)

In `ApiKey.java`, add next to the other setters:

```java
public void setLastUsedDate(Instant lastUsedDate) {
    this.lastUsedDate = lastUsedDate;
}
```

In `ApiKeyServiceImpl.java`, add:

```java
@Override
@Transactional(readOnly = true)
public Optional<ApiKey> fetchApiKey(String secretKey) {
    return apiKeyRepository.findBySecretKey(secretKey);
}

@Override
public void updateLastUsedDate(long id) {
    apiKeyRepository.findById(id)
        .ifPresent(apiKey -> {
            apiKey.setLastUsedDate(Instant.now());

            apiKeyRepository.save(apiKey);
        });
}
```

(Add `import java.time.Instant;` and `import java.util.Optional;`.)

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :server:libs:platform:platform-security:platform-security-service:test --tests "com.bytechef.platform.security.service.ApiKeyServiceTest"`
Expected: 3 tests PASS.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-security
git commit -m "Add ApiKeyService fetchApiKey and updateLastUsedDate"
```

---

### Task 3: Shared MCP API-key security classes

**Files:**
- Create (all in `server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/mcp/`):
  - `McpApiKeyCredentials.java`
  - `McpApiKeyEntity.java`
  - `McpApiKeyEntityRepository.java`
  - `McpApiKeyAuthenticationConverter.java`
  - `TenantAwareApiKeyAuthenticationFilter.java`
  - `McpApiKeyHttpConfigurer.java`
- Test (in `.../src/test/java/com/bytechef/platform/security/web/mcp/`):
  - `McpApiKeyAuthenticationConverterTest.java`
  - `McpApiKeyEntityRepositoryTest.java`
  - `TenantAwareApiKeyAuthenticationFilterTest.java`

**Interfaces:**
- Consumes: `ApiKeyService.fetchApiKey(String)`, `ApiKeyService.updateLastUsedDate(long)` (Task 2), library types (Task 1).
- Produces (used by Tasks 4–7):
  - `McpApiKeyCredentials(Environment environment, String mcpServerSecretKey, String secretKey)` with `getEnvironment()`, `getMcpServerSecretKey()`; implements library `ApiKey`.
  - `McpApiKeyEntity(com.bytechef.platform.security.domain.ApiKey apiKey, String login, List<GrantedAuthority> authorities)` with `getApiKeyId()`, `getEnvironment()`, `@Nullable getType()`; implements library `ApiKeyEntity` **and** `UserDetails`.
  - `McpApiKeyEntityRepository(ApiKeyService, AuthorityService, UserService)` implementing `ApiKeyEntityRepository<McpApiKeyEntity>`.
  - `McpApiKeyAuthenticationConverter(String pathPrefix)` implementing `AuthenticationConverter`.
  - `McpApiKeyHttpConfigurer(String pathPatternRegex, AuthenticationConverter, AuthenticationProvider)` extending `AbstractHttpConfigurer<McpApiKeyHttpConfigurer, HttpSecurity>`.

- [ ] **Step 1: Write the failing converter test**

`McpApiKeyAuthenticationConverterTest.java`:

```java
package com.bytechef.platform.security.web.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * @author Ivica Cardic
 */
class McpApiKeyAuthenticationConverterTest {

    private final McpApiKeyAuthenticationConverter mcpApiKeyAuthenticationConverter =
        new McpApiKeyAuthenticationConverter("/api/automation/");

    @Test
    void testConvertWithBearerTokenReturnsUnauthenticatedToken() {
        MockHttpServletRequest mockHttpServletRequest = getMockHttpServletRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer api-secret");

        ApiKeyAuthenticationToken apiKeyAuthenticationToken =
            (ApiKeyAuthenticationToken) mcpApiKeyAuthenticationConverter.convert(mockHttpServletRequest);

        McpApiKeyCredentials mcpApiKeyCredentials =
            (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();

        assertThat(apiKeyAuthenticationToken.isAuthenticated()).isFalse();
        assertThat(mcpApiKeyCredentials.getId()).isEqualTo("api-secret");
        assertThat(mcpApiKeyCredentials.getSecret()).isEqualTo("api-secret");
        assertThat(mcpApiKeyCredentials.getMcpServerSecretKey()).isEqualTo("server-secret");
        assertThat(mcpApiKeyCredentials.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testConvertWithEnvironmentHeaderUsesRequestedEnvironment() {
        MockHttpServletRequest mockHttpServletRequest = getMockHttpServletRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer api-secret");
        mockHttpServletRequest.addHeader("X-ENVIRONMENT", "staging");

        ApiKeyAuthenticationToken apiKeyAuthenticationToken =
            (ApiKeyAuthenticationToken) mcpApiKeyAuthenticationConverter.convert(mockHttpServletRequest);

        McpApiKeyCredentials mcpApiKeyCredentials =
            (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();

        assertThat(mcpApiKeyCredentials.getEnvironment()).isEqualTo(Environment.STAGING);
    }

    @Test
    void testConvertWithoutAuthorizationHeaderThrows() {
        MockHttpServletRequest mockHttpServletRequest = getMockHttpServletRequest();

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> mcpApiKeyAuthenticationConverter.convert(mockHttpServletRequest));
    }

    @Test
    void testConvertWithInvalidEnvironmentHeaderThrows() {
        MockHttpServletRequest mockHttpServletRequest = getMockHttpServletRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer api-secret");
        mockHttpServletRequest.addHeader("X-ENVIRONMENT", "bogus");

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> mcpApiKeyAuthenticationConverter.convert(mockHttpServletRequest));
    }

    private MockHttpServletRequest getMockHttpServletRequest() {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(
            "POST", "/api/automation/server-secret/mcp");

        mockHttpServletRequest.setServletPath("/api/automation/server-secret/mcp");

        return mockHttpServletRequest;
    }
}
```

- [ ] **Step 2: Run converter test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-security-web:platform-security-web-api:test --tests "com.bytechef.platform.security.web.mcp.McpApiKeyAuthenticationConverterTest"`
Expected: COMPILE FAILURE — classes do not exist.

- [ ] **Step 3: Implement McpApiKeyCredentials**

```java
package com.bytechef.platform.security.web.mcp;

import com.bytechef.platform.configuration.domain.Environment;
import org.springaicommunity.mcp.security.server.apikey.ApiKey;

/**
 * The API key presented on an MCP request, together with the request context (URL path secret, requested environment)
 * needed by the per-server authentication providers.
 *
 * @author Ivica Cardic
 */
public class McpApiKeyCredentials implements ApiKey {

    private final Environment environment;
    private final String mcpServerSecretKey;
    private final String secretKey;

    public McpApiKeyCredentials(Environment environment, String mcpServerSecretKey, String secretKey) {
        this.environment = environment;
        this.mcpServerSecretKey = mcpServerSecretKey;
        this.secretKey = secretKey;
    }

    @Override
    public String getId() {
        return secretKey;
    }

    @Override
    public String getSecret() {
        return secretKey;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getMcpServerSecretKey() {
        return mcpServerSecretKey;
    }
}
```

- [ ] **Step 4: Implement McpApiKeyEntity**

```java
package com.bytechef.platform.security.web.mcp;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

/**
 * Adapts a ByteChef {@link ApiKey} to the mcp-security {@link ApiKeyEntity}. Also implements {@link UserDetails} so
 * {@code SecurityUtils.getCurrentUserLogin()} keeps resolving the key's owning user for auditing and authorization.
 *
 * @author Ivica Cardic
 */
public class McpApiKeyEntity implements ApiKeyEntity, UserDetails {

    private final long apiKeyId;
    private final List<GrantedAuthority> authorities;
    private final Environment environment;
    private final String login;
    private @Nullable String secret;
    private final @Nullable PlatformType type;

    public McpApiKeyEntity(ApiKey apiKey, String login, List<GrantedAuthority> authorities) {
        Assert.notNull(apiKey.getId(), "'apiKey.id' must not be null");

        this.apiKeyId = apiKey.getId();
        this.authorities = List.copyOf(authorities);
        this.environment = apiKey.getEnvironment();
        this.login = login;
        this.secret = "{noop}" + apiKey.getSecretKey();
        this.type = apiKey.getType();
    }

    private McpApiKeyEntity(
        long apiKeyId, List<GrantedAuthority> authorities, Environment environment, String login,
        @Nullable String secret, @Nullable PlatformType type) {

        this.apiKeyId = apiKeyId;
        this.authorities = authorities;
        this.environment = environment;
        this.login = login;
        this.secret = secret;
        this.type = type;
    }

    @Override
    public String getId() {
        return login;
    }

    @Override
    @Nullable
    public String getSecret() {
        return secret;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public void eraseCredentials() {
        secret = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ApiKeyEntity> T copy() {
        return (T) new McpApiKeyEntity(apiKeyId, authorities, environment, login, secret, type);
    }

    @Override
    @Nullable
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return login;
    }

    public long getApiKeyId() {
        return apiKeyId;
    }

    public Environment getEnvironment() {
        return environment;
    }

    @Nullable
    public PlatformType getType() {
        return type;
    }
}
```

Note the import pair: ByteChef's `com.bytechef.platform.security.domain.ApiKey` and the library's `ApiKeyEntity` have distinct simple names, so no conflict; never import the library's `ApiKey` in this file.

- [ ] **Step 5: Implement McpApiKeyEntityRepository**

```java
package com.bytechef.platform.security.web.mcp;

import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntityRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Looks up ByteChef API keys by their opaque secret. Runs inside the tenant context established by
 * {@link TenantAwareApiKeyAuthenticationFilter}.
 *
 * @author Ivica Cardic
 */
public class McpApiKeyEntityRepository implements ApiKeyEntityRepository<McpApiKeyEntity> {

    private final ApiKeyService apiKeyService;
    private final AuthorityService authorityService;
    private final UserService userService;

    public McpApiKeyEntityRepository(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        this.apiKeyService = apiKeyService;
        this.authorityService = authorityService;
        this.userService = userService;
    }

    @Override
    @Nullable
    public McpApiKeyEntity findByKeyId(String keyId) {
        return apiKeyService.fetchApiKey(keyId)
            .flatMap(apiKey -> userService.fetchUser(apiKey.getUserId())
                .filter(User::isActivated)
                .map(user -> new McpApiKeyEntity(apiKey, user.getLogin(), getGrantedAuthorities(user))))
            .orElse(null);
    }

    private List<GrantedAuthority> getGrantedAuthorities(User user) {
        return user.getAuthorityIds()
            .stream()
            .map(authorityService::fetchAuthority)
            .flatMap(Optional::stream)
            .map(Authority::getName)
            .map(authorityName -> (GrantedAuthority) new SimpleGrantedAuthority(authorityName))
            .toList();
    }
}
```

- [ ] **Step 6: Implement McpApiKeyAuthenticationConverter**

```java
package com.bytechef.platform.security.web.mcp;

import com.bytechef.platform.configuration.domain.Environment;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Converts an MCP request into an unauthenticated {@link ApiKeyAuthenticationToken}. The Bearer token is MANDATORY —
 * a missing or malformed Authorization header fails the request with 401 regardless of the URL path secret.
 *
 * @author Ivica Cardic
 */
public class McpApiKeyAuthenticationConverter implements AuthenticationConverter {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ENVIRONMENT_HEADER_NAME = "X-ENVIRONMENT";

    private final String pathPrefix;

    public McpApiKeyAuthenticationConverter(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER_NAME);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BadCredentialsException("Authorization token does not exist");
        }

        String secretKey = authorization.substring(BEARER_PREFIX.length());

        String servletPath = request.getServletPath();

        String mcpServerSecretKey = servletPath.replace(pathPrefix, "")
            .replace("/mcp", "");

        return ApiKeyAuthenticationToken.unauthenticated(
            new McpApiKeyCredentials(getEnvironment(request), mcpServerSecretKey, secretKey));
    }

    private Environment getEnvironment(HttpServletRequest request) {
        String environment = request.getHeader(ENVIRONMENT_HEADER_NAME);

        if (StringUtils.isBlank(environment)) {
            return Environment.PRODUCTION;
        }

        try {
            return Environment.valueOf(environment.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new BadCredentialsException("Invalid X-ENVIRONMENT header", illegalArgumentException);
        }
    }
}
```

- [ ] **Step 7: Implement TenantAwareApiKeyAuthenticationFilter**

```java
package com.bytechef.platform.security.web.mcp;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springaicommunity.mcp.security.server.apikey.web.ApiKeyAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Wraps the mcp-security authentication filter so that authentication AND all downstream request processing run
 * within the tenant resolved from the presented API key ({@code ApiKey.secretKey} values are {@link TenantKey}
 * strings).
 *
 * @author Ivica Cardic
 */
public class TenantAwareApiKeyAuthenticationFilter extends ApiKeyAuthenticationFilter {

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public TenantAwareApiKeyAuthenticationFilter(
        RequestMatcher requestMatcher, AuthenticationManager authenticationManager,
        AuthenticationConverter authenticationConverter) {

        super(authenticationManager, authenticationConverter);

        setRequestMatcher(requestMatcher);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain)
        throws ServletException, IOException {

        String authorization = httpServletRequest.getHeader(AUTHORIZATION_HEADER_NAME);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            // no tenant to resolve; the authentication converter rejects matching requests with 401

            super.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

            return;
        }

        TenantKey tenantKey;

        try {
            tenantKey = TenantKey.parse(authorization.substring(BEARER_PREFIX.length()));
        } catch (Exception exception) {
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            return;
        }

        TenantContext.runWithTenantId(
            tenantKey.getTenantId(),
            () -> super.doFilterInternal(httpServletRequest, httpServletResponse, filterChain));
    }
}
```

Note: `TenantContext.runWithTenantId` takes `TenantContext.Runnable` (`void run() throws Exception`), so the checked servlet exceptions compile; they surface wrapped in `RuntimeException`, matching the behavior of the existing `com.bytechef.platform.security.web.filter.ApiKeyAuthenticationFilter`. The parent's `doFilterInternal` already skips requests not matching the `RequestMatcher` passed to `setRequestMatcher`, so non-MCP paths (no Authorization header case included) pass through untouched.

- [ ] **Step 8: Implement McpApiKeyHttpConfigurer**

```java
package com.bytechef.platform.security.web.mcp;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * Registers mandatory API key authentication for an MCP server path, built on mcp-server-security with ByteChef
 * tenant routing.
 *
 * @author Ivica Cardic
 */
public class McpApiKeyHttpConfigurer extends AbstractHttpConfigurer<McpApiKeyHttpConfigurer, HttpSecurity> {

    private final AuthenticationConverter authenticationConverter;
    private final AuthenticationProvider authenticationProvider;
    private final String pathPatternRegex;

    public McpApiKeyHttpConfigurer(
        String pathPatternRegex, AuthenticationConverter authenticationConverter,
        AuthenticationProvider authenticationProvider) {

        this.authenticationConverter = authenticationConverter;
        this.authenticationProvider = authenticationProvider;
        this.pathPatternRegex = pathPatternRegex;
    }

    @Override
    public void init(HttpSecurity http) {
        http.authenticationProvider(authenticationProvider);

        CsrfConfigurer<?> csrf = http.getConfigurer(CsrfConfigurer.class);

        if (csrf != null) {
            csrf.ignoringRequestMatchers(RegexRequestMatcher.regexMatcher(pathPatternRegex));
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);

        TenantAwareApiKeyAuthenticationFilter tenantAwareApiKeyAuthenticationFilter =
            new TenantAwareApiKeyAuthenticationFilter(
                RegexRequestMatcher.regexMatcher(pathPatternRegex), authenticationManager, authenticationConverter);

        http.addFilterBefore(tenantAwareApiKeyAuthenticationFilter, BasicAuthenticationFilter.class);
    }
}
```

- [ ] **Step 9: Write the repository and filter tests**

`McpApiKeyEntityRepositoryTest.java`:

```java
package com.bytechef.platform.security.web.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Ivica Cardic
 */
class McpApiKeyEntityRepositoryTest {

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final UserService userService = mock(UserService.class);

    private final McpApiKeyEntityRepository mcpApiKeyEntityRepository = new McpApiKeyEntityRepository(
        apiKeyService, authorityService, userService);

    @Test
    void testFindByKeyIdReturnsEntityForActivatedUser() {
        ApiKey apiKey = getApiKey();

        when(apiKeyService.fetchApiKey("api-secret")).thenReturn(Optional.of(apiKey));

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of(5L));
        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));

        Authority authority = mock(Authority.class);

        when(authority.getName()).thenReturn("ROLE_ADMIN");
        when(authorityService.fetchAuthority(5L)).thenReturn(Optional.of(authority));

        McpApiKeyEntity mcpApiKeyEntity = mcpApiKeyEntityRepository.findByKeyId("api-secret");

        assertThat(mcpApiKeyEntity).isNotNull();
        assertThat(mcpApiKeyEntity.getId()).isEqualTo("admin@localhost.com");
        assertThat(mcpApiKeyEntity.getSecret()).isEqualTo("{noop}api-secret");
        assertThat(mcpApiKeyEntity.getApiKeyId()).isEqualTo(7L);
        assertThat(mcpApiKeyEntity.getType()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(mcpApiKeyEntity.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(mcpApiKeyEntity.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    void testFindByKeyIdReturnsNullWhenApiKeyIsMissing() {
        when(apiKeyService.fetchApiKey("missing")).thenReturn(Optional.empty());

        assertThat(mcpApiKeyEntityRepository.findByKeyId("missing")).isNull();
    }

    @Test
    void testFindByKeyIdReturnsNullWhenUserIsNotActivated() {
        ApiKey apiKey = getApiKey();

        when(apiKeyService.fetchApiKey("api-secret")).thenReturn(Optional.of(apiKey));

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(false);
        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));

        assertThat(mcpApiKeyEntityRepository.findByKeyId("api-secret")).isNull();
    }

    private ApiKey getApiKey() {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(7L);
        apiKey.setName("test");
        apiKey.setSecretKey("api-secret");
        apiKey.setType(PlatformType.AUTOMATION);
        apiKey.setEnvironment(Environment.PRODUCTION);
        apiKey.setUserId(100L);

        return apiKey;
    }
}
```

`TenantAwareApiKeyAuthenticationFilterTest.java`:

```java
package com.bytechef.platform.security.web.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @author Ivica Cardic
 */
class TenantAwareApiKeyAuthenticationFilterTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);

    private final TenantAwareApiKeyAuthenticationFilter tenantAwareApiKeyAuthenticationFilter =
        new TenantAwareApiKeyAuthenticationFilter(
            RegexRequestMatcher.regexMatcher("^/api/automation/.+/mcp"), authenticationManager,
            new McpApiKeyAuthenticationConverter("/api/automation/"));

    @Test
    void testRequestWithoutBearerTokenIsRejectedWith401() throws Exception {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(
            "POST", "/api/automation/server-secret/mcp");

        mockHttpServletRequest.setServletPath("/api/automation/server-secret/mcp");

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        tenantAwareApiKeyAuthenticationFilter.doFilter(
            mockHttpServletRequest, mockHttpServletResponse, new MockFilterChain());

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(401);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void testRequestWithMalformedTenantKeyIsRejectedWith401() throws Exception {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(
            "POST", "/api/automation/server-secret/mcp");

        mockHttpServletRequest.setServletPath("/api/automation/server-secret/mcp");
        mockHttpServletRequest.addHeader("Authorization", "Bearer not-a-tenant-key");

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        tenantAwareApiKeyAuthenticationFilter.doFilter(
            mockHttpServletRequest, mockHttpServletResponse, new MockFilterChain());

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(401);
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void testNonMatchingRequestPassesThrough() throws Exception {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", "/api/other");

        mockHttpServletRequest.setServletPath("/api/other");

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        tenantAwareApiKeyAuthenticationFilter.doFilter(
            mockHttpServletRequest, mockHttpServletResponse, new MockFilterChain());

        assertThat(mockHttpServletResponse.getStatus()).isEqualTo(200);
        verifyNoInteractions(authenticationManager);
    }
}
```

Caveat for the malformed-tenant-key test: if `TenantKey.parse("not-a-tenant-key")` happens to parse successfully (inspect `TenantKey.parse` — it may treat un-prefixed strings as the default tenant), the request proceeds into authentication and fails there instead; in that case assert 401 all the same but drop the `verifyNoInteractions(authenticationManager)` line and stub `authenticationManager.authenticate` to throw `BadCredentialsException`. Check the actual `TenantKey.parse` behavior before finalizing this test.

- [ ] **Step 10: Run all Task 3 tests**

Run: `./gradlew :server:libs:platform:platform-security-web:platform-security-web-api:test`
Expected: all tests PASS (smoke test from Task 1 included).

- [ ] **Step 11: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-security-web
git commit -m "Add shared MCP API key security classes built on mcp-server-security"
```

---

### Task 4: Automation MCP server — mandatory API key

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp-server/build.gradle.kts`
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/security/web/authentication/AutomationMcpServerApiKeyAuthenticationProvider.java` (full rewrite)
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/security/web/configurer/AutomationMcpServerSecurityConfigurer.java` (full rewrite)
- Delete: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/security/web/authentication/AutomationMcpServerApiKeyAuthenticationToken.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/config/AutomationMcpServerConfiguration.java` (contributor bean wiring, lines 228–240)
- Test: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/test/java/com/bytechef/automation/ai/mcp/server/security/web/authentication/AutomationMcpServerApiKeyAuthenticationProviderTest.java`

**Interfaces:**
- Consumes: Task 3 shared classes; `McpServerService.getMcpServer(String secretKey)` (throws when absent); `McpServer.getEnvironment()`.
- Produces: `AutomationMcpServerApiKeyAuthenticationProvider(ApiKeyService, AuthorityService, McpServerService, UserService)`; `AutomationMcpServerSecurityConfigurer(ApiKeyService, AuthorityService, McpServerService, UserService)` extending `McpApiKeyHttpConfigurer`.

- [ ] **Step 1: Add module dependencies**

In `server/libs/automation/automation-ai/automation-ai-mcp-server/build.gradle.kts`, add to the `dependencies` block (alphabetical among the project deps):

```kotlin
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
```

and append (the module currently has no test dependencies):

```kotlin
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-webmvc")
    testImplementation("org.springframework.security:spring-security-test")
```

- [ ] **Step 2: Write the failing provider test**

`AutomationMcpServerApiKeyAuthenticationProviderTest.java`:

```java
package com.bytechef.automation.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpApiKeyCredentials;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
class AutomationMcpServerApiKeyAuthenticationProviderTest {

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final UserService userService = mock(UserService.class);

    private final AutomationMcpServerApiKeyAuthenticationProvider automationMcpServerApiKeyAuthenticationProvider =
        new AutomationMcpServerApiKeyAuthenticationProvider(
            apiKeyService, authorityService, mcpServerService, userService);

    @Test
    void testAuthenticateWithValidAutomationApiKeySucceeds() {
        mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);
        mockMcpServer(Environment.PRODUCTION);

        Authentication authentication = automationMcpServerApiKeyAuthenticationProvider.authenticate(
            getUnauthenticatedToken());

        assertThat(authentication.isAuthenticated()).isTrue();
        verify(apiKeyService).updateLastUsedDate(7L);
    }

    @Test
    void testAuthenticateWithWrongTypeApiKeyFails() {
        mockApiKey(PlatformType.EMBEDDED, Environment.PRODUCTION);

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> automationMcpServerApiKeyAuthenticationProvider.authenticate(getUnauthenticatedToken()));
    }

    @Test
    void testAuthenticateWithEnvironmentMismatchFails() {
        mockApiKey(PlatformType.AUTOMATION, Environment.STAGING);
        mockMcpServer(Environment.PRODUCTION);

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> automationMcpServerApiKeyAuthenticationProvider.authenticate(getUnauthenticatedToken()));
    }

    @Test
    void testAuthenticateWithUnknownMcpServerSecretKeyFails() {
        mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

        when(mcpServerService.getMcpServer("server-secret")).thenThrow(new IllegalArgumentException());

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> automationMcpServerApiKeyAuthenticationProvider.authenticate(getUnauthenticatedToken()));
    }

    private ApiKeyAuthenticationToken getUnauthenticatedToken() {
        return ApiKeyAuthenticationToken.unauthenticated(
            new McpApiKeyCredentials(Environment.PRODUCTION, "server-secret", "api-secret"));
    }

    private void mockApiKey(PlatformType type, Environment environment) {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(7L);
        apiKey.setName("test");
        apiKey.setSecretKey("api-secret");
        apiKey.setType(type);
        apiKey.setEnvironment(environment);
        apiKey.setUserId(100L);

        when(apiKeyService.fetchApiKey("api-secret")).thenReturn(Optional.of(apiKey));

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of());
        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));
    }

    private void mockMcpServer(Environment environment) {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getEnvironment()).thenReturn(environment);
        when(mcpServerService.getMcpServer("server-secret")).thenReturn(mcpServer);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:test --tests "com.bytechef.automation.ai.mcp.server.security.web.authentication.AutomationMcpServerApiKeyAuthenticationProviderTest"`
Expected: COMPILE FAILURE — the provider's current constructor takes only `McpServerService`.

- [ ] **Step 4: Rewrite the provider**

Replace the entire body of `AutomationMcpServerApiKeyAuthenticationProvider.java` (keep the license header):

```java
package com.bytechef.automation.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpApiKeyCredentials;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntity;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntityRepository;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationProvider;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Authenticates automation MCP requests: delegates secret validation to mcp-server-security, then enforces that the
 * key is an AUTOMATION key whose environment matches the target MCP server's environment.
 *
 * @author Ivica Cardic
 */
public class AutomationMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyAuthenticationProvider<McpApiKeyEntity> apiKeyAuthenticationProvider;
    private final ApiKeyService apiKeyService;
    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    public AutomationMcpServerApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, McpServerService mcpServerService,
        UserService userService) {

        this.apiKeyAuthenticationProvider = new ApiKeyAuthenticationProvider<>(
            new McpApiKeyEntityRepository(apiKeyService, authorityService, userService));
        this.apiKeyService = apiKeyService;
        this.mcpServerService = mcpServerService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication authenticatedAuthentication = apiKeyAuthenticationProvider.authenticate(authentication);

        if (authenticatedAuthentication == null) {
            return null;
        }

        ApiKeyAuthenticationToken apiKeyAuthenticationToken = (ApiKeyAuthenticationToken) authentication;

        McpApiKeyCredentials mcpApiKeyCredentials = (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();
        McpApiKeyEntity mcpApiKeyEntity = (McpApiKeyEntity) authenticatedAuthentication.getPrincipal();

        if (mcpApiKeyEntity.getType() != PlatformType.AUTOMATION) {
            throw new BadCredentialsException("Invalid API key");
        }

        McpServer mcpServer = getMcpServer(mcpApiKeyCredentials.getMcpServerSecretKey());

        if (mcpServer.getEnvironment() != mcpApiKeyEntity.getEnvironment()) {
            throw new BadCredentialsException("Invalid API key");
        }

        apiKeyService.updateLastUsedDate(mcpApiKeyEntity.getApiKeyId());

        return authenticatedAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private McpServer getMcpServer(String mcpServerSecretKey) {
        try {
            return mcpServerService.getMcpServer(mcpServerSecretKey);
        } catch (Exception exception) {
            throw new BadCredentialsException("Invalid MCP server secret key", exception);
        }
    }
}
```

- [ ] **Step 5: Rewrite the configurer, delete the token**

Replace `AutomationMcpServerSecurityConfigurer.java` (keep license header):

```java
package com.bytechef.automation.ai.mcp.server.security.web.configurer;

import com.bytechef.automation.ai.mcp.server.security.web.authentication.AutomationMcpServerApiKeyAuthenticationProvider;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpApiKeyAuthenticationConverter;
import com.bytechef.platform.security.web.mcp.McpApiKeyHttpConfigurer;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;

/**
 * @author Ivica Cardic
 */
public class AutomationMcpServerSecurityConfigurer extends McpApiKeyHttpConfigurer {

    private static final String PATH_PATTERN = "^/api/automation/.+/mcp";

    public AutomationMcpServerSecurityConfigurer(
        ApiKeyService apiKeyService, AuthorityService authorityService, McpServerService mcpServerService,
        UserService userService) {

        super(
            PATH_PATTERN, new McpApiKeyAuthenticationConverter("/api/automation/"),
            new AutomationMcpServerApiKeyAuthenticationProvider(
                apiKeyService, authorityService, mcpServerService, userService));
    }
}
```

Delete `AutomationMcpServerApiKeyAuthenticationToken.java`:

```bash
git rm server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/security/web/authentication/AutomationMcpServerApiKeyAuthenticationToken.java
```

- [ ] **Step 6: Update the contributor bean**

In `AutomationMcpServerConfiguration.java`, replace the `automationMcpServerSecurityConfigurerContributor` bean (currently lines 228–240) with:

```java
@Bean
SecurityConfigurerContributor automationMcpServerSecurityConfigurerContributor(
    ApiKeyService apiKeyService, AuthorityService authorityService, McpServerService mcpServerService,
    UserService userService) {

    return new SecurityConfigurerContributor() {

        @Override
        @SuppressWarnings("unchecked")
        public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T
            getSecurityConfigurerAdapter() {

            return (T) new AutomationMcpServerSecurityConfigurer(
                apiKeyService, authorityService, mcpServerService, userService);
        }
    };
}
```

Add imports: `com.bytechef.platform.security.service.ApiKeyService`, `com.bytechef.platform.user.service.AuthorityService`, `com.bytechef.platform.user.service.UserService`.

- [ ] **Step 7: Run tests and compile**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:test`
Expected: provider test PASSES (4 tests); whole module compiles.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-ai/automation-ai-mcp-server
git commit -m "Require API key authentication on automation MCP server"
```

---

### Task 5: Automation MCP security filter chain test

**Files:**
- Test: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/test/java/com/bytechef/automation/ai/mcp/server/security/web/configurer/AutomationMcpServerSecurityFilterChainTest.java`

**Interfaces:**
- Consumes: `AutomationMcpServerSecurityConfigurer` (Task 4). No new production code — this task hardens confidence in the full chain (filter → converter → provider) and is the regression suite for the closed vulnerability.

- [ ] **Step 1: Write the filter chain test**

```java
package com.bytechef.automation.ai.mcp.server.security.web.configurer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author Ivica Cardic
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AutomationMcpServerSecurityFilterChainTest.SecurityFilterChainTestConfiguration.class)
@WebAppConfiguration
class AutomationMcpServerSecurityFilterChainTest {

    private static final String API_SECRET_KEY = String.valueOf(TenantKey.of());
    private static final String MCP_SERVER_SECRET_KEY = "server-secret";

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        reset(apiKeyService, authorityService, mcpServerService, userService);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @Test
    void testRequestWithoutBearerTokenIsRejected() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY)))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());
    }

    @Test
    void testRequestWithValidApiKeySucceeds() throws Exception {
        mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);
        mockMcpServer(Environment.PRODUCTION);

        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                    .header("Authorization", "Bearer " + API_SECRET_KEY))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());
    }

    @Test
    void testRequestWithUnknownApiKeyIsRejected() throws Exception {
        when(apiKeyService.fetchApiKey(API_SECRET_KEY)).thenReturn(Optional.empty());

        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                    .header("Authorization", "Bearer " + API_SECRET_KEY))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());
    }

    @Test
    void testRequestWithWrongTypeApiKeyIsRejected() throws Exception {
        mockApiKey(PlatformType.EMBEDDED, Environment.PRODUCTION);

        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                    .header("Authorization", "Bearer " + API_SECRET_KEY))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());
    }

    @Test
    void testRequestWithEnvironmentMismatchIsRejected() throws Exception {
        mockApiKey(PlatformType.AUTOMATION, Environment.STAGING);
        mockMcpServer(Environment.PRODUCTION);

        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                    .header("Authorization", "Bearer " + API_SECRET_KEY))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());
    }

    @Test
    void testRequestWithUnknownMcpServerSecretKeyIsRejected() throws Exception {
        mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenThrow(new IllegalArgumentException());

        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/automation/%s/mcp".formatted(MCP_SERVER_SECRET_KEY))
                    .header("Authorization", "Bearer " + API_SECRET_KEY))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());
    }

    private void mockApiKey(PlatformType type, Environment environment) {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(7L);
        apiKey.setName("test");
        apiKey.setSecretKey(API_SECRET_KEY);
        apiKey.setType(type);
        apiKey.setEnvironment(environment);
        apiKey.setUserId(100L);

        when(apiKeyService.fetchApiKey(API_SECRET_KEY)).thenReturn(Optional.of(apiKey));

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of());
        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));
    }

    private void mockMcpServer(Environment environment) {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getEnvironment()).thenReturn(environment);
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class SecurityFilterChainTestConfiguration {

        @Bean
        ApiKeyService apiKeyService() {
            return mock(ApiKeyService.class);
        }

        @Bean
        AuthorityService authorityService() {
            return mock(AuthorityService.class);
        }

        @Bean
        McpServerService mcpServerService() {
            return mock(McpServerService.class);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        RouterFunction<ServerResponse> mcpStubRouterFunction() {
            return RouterFunctions.route()
                .POST("/api/automation/{secretKey}/mcp", request -> ServerResponse.ok()
                    .build())
                .build();
        }

        @Bean
        SecurityFilterChain securityFilterChain(
            HttpSecurity http, ApiKeyService apiKeyService, AuthorityService authorityService,
            McpServerService mcpServerService, UserService userService) throws Exception {

            return http
                .authorizeHttpRequests(authorize -> authorize.anyRequest()
                    .permitAll())
                .with(
                    new AutomationMcpServerSecurityConfigurer(
                        apiKeyService, authorityService, mcpServerService, userService),
                    Customizer.withDefaults())
                .build();
        }
    }
}
```

Notes for the implementer: `McpServer` is a final class — Mockito 5 inline mock maker (the default) handles it. `API_SECRET_KEY = String.valueOf(TenantKey.of())` produces a real tenant-key-format secret so `TenantKey.parse` in the filter succeeds for the default tenant. CSRF stays at defaults deliberately — a green POST in `testRequestWithValidApiKeySucceeds` proves the configurer's CSRF ignore rule works.

- [ ] **Step 2: Run the test**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:test --tests "com.bytechef.automation.ai.mcp.server.security.web.configurer.AutomationMcpServerSecurityFilterChainTest"`
Expected: 6 tests PASS. `testRequestWithoutBearerTokenIsRejected` is the regression test for the vulnerability this whole phase closes — if it fails, the feature does not work.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-ai/automation-ai-mcp-server
git commit -m "Add automation MCP security filter chain tests"
```

---

### Task 6: Management MCP server — mandatory API key

**Files:**
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server/build.gradle.kts` (test deps only — main deps already present)
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/security/web/authentication/ManagementMcpServerApiKeyAuthenticationProvider.java` (full rewrite)
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/security/web/configurer/ManagementMcpServerSecurityConfigurer.java` (full rewrite)
- Delete: `server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/security/web/authentication/ManagementMcpServerApiKeyAuthenticationToken.java`
- Test: `server/libs/ai/ai-mcp/ai-mcp-server/src/test/java/com/bytechef/ai/mcp/server/security/web/authentication/ManagementMcpServerApiKeyAuthenticationProviderTest.java`

**Interfaces:**
- Consumes: Task 3 shared classes; `PropertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)` → `Property.get("secretKey")`.
- Produces: `ManagementMcpServerApiKeyAuthenticationProvider(ApiKeyService, AuthorityService, PropertyService, UserService)`; `ManagementMcpServerSecurityConfigurer` with the **unchanged** constructor signature `(ApiKeyService, AuthorityService, PropertyService, UserService)` — the contributor bean in `ManagementMcpServerConfiguration` does not change.

- [ ] **Step 1: Add test dependencies**

In `server/libs/ai/ai-mcp/ai-mcp-server/build.gradle.kts`, the module already has assertj/junit/mockito test deps. Add:

```kotlin
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-webmvc")
    testImplementation("org.springframework.security:spring-security-test")
```

- [ ] **Step 2: Write the failing provider test**

`ManagementMcpServerApiKeyAuthenticationProviderTest.java` — same structure as the automation one; the deltas:

```java
package com.bytechef.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpApiKeyCredentials;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
class ManagementMcpServerApiKeyAuthenticationProviderTest {

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final PropertyService propertyService = mock(PropertyService.class);
    private final UserService userService = mock(UserService.class);

    private final ManagementMcpServerApiKeyAuthenticationProvider managementMcpServerApiKeyAuthenticationProvider =
        new ManagementMcpServerApiKeyAuthenticationProvider(
            apiKeyService, authorityService, propertyService, userService);

    @BeforeEach
    void beforeEach() {
        Property property = mock(Property.class);

        when(property.get("secretKey")).thenReturn("server-secret");
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);
    }

    @Test
    void testAuthenticateWithValidAdminApiKeySucceeds() {
        mockApiKey(null, Environment.PRODUCTION);

        Authentication authentication = managementMcpServerApiKeyAuthenticationProvider.authenticate(
            getUnauthenticatedToken(Environment.PRODUCTION, "server-secret"));

        assertThat(authentication.isAuthenticated()).isTrue();
        verify(apiKeyService).updateLastUsedDate(7L);
    }

    @Test
    void testAuthenticateWithTypedApiKeyFails() {
        mockApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> managementMcpServerApiKeyAuthenticationProvider.authenticate(
                getUnauthenticatedToken(Environment.PRODUCTION, "server-secret")));
    }

    @Test
    void testAuthenticateWithWrongMcpServerSecretKeyFails() {
        mockApiKey(null, Environment.PRODUCTION);

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> managementMcpServerApiKeyAuthenticationProvider.authenticate(
                getUnauthenticatedToken(Environment.PRODUCTION, "wrong-server-secret")));
    }

    @Test
    void testAuthenticateWithEnvironmentMismatchFails() {
        mockApiKey(null, Environment.PRODUCTION);

        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> managementMcpServerApiKeyAuthenticationProvider.authenticate(
                getUnauthenticatedToken(Environment.STAGING, "server-secret")));
    }

    private ApiKeyAuthenticationToken getUnauthenticatedToken(Environment environment, String mcpServerSecretKey) {
        return ApiKeyAuthenticationToken.unauthenticated(
            new McpApiKeyCredentials(environment, mcpServerSecretKey, "api-secret"));
    }

    private void mockApiKey(PlatformType type, Environment environment) {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(7L);
        apiKey.setName("test");
        apiKey.setSecretKey("api-secret");
        apiKey.setType(type);
        apiKey.setEnvironment(environment);
        apiKey.setUserId(100L);

        when(apiKeyService.fetchApiKey("api-secret")).thenReturn(Optional.of(apiKey));

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of());
        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));
    }
}
```

Note: `ApiKey.setType(null)` is a no-op in the domain class (the setter ignores null), so a fresh `ApiKey` with no `setType` call has `getType() == null` — for the admin-key case simply do not call `setType`. Adjust `mockApiKey` accordingly: only call `apiKey.setType(type)` when `type != null`.

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:test --tests "com.bytechef.ai.mcp.server.security.web.authentication.ManagementMcpServerApiKeyAuthenticationProviderTest"`
Expected: COMPILE FAILURE (the current provider takes the same four services but the test exercises `McpApiKeyCredentials`, which the old token flow cannot accept) or test failures — either confirms red.

- [ ] **Step 4: Rewrite the provider**

Replace `ManagementMcpServerApiKeyAuthenticationProvider.java` (keep license header):

```java
package com.bytechef.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpApiKeyCredentials;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntity;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntityRepository;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationProvider;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Authenticates management MCP requests: delegates secret validation to mcp-server-security, then enforces that the
 * key is an admin key (no platform type), that the URL path secret matches the configured MCP server secret, and that
 * the key's environment matches the requested environment.
 *
 * @author Ivica Cardic
 */
public class ManagementMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyAuthenticationProvider<McpApiKeyEntity> apiKeyAuthenticationProvider;
    private final ApiKeyService apiKeyService;
    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, PropertyService propertyService,
        UserService userService) {

        this.apiKeyAuthenticationProvider = new ApiKeyAuthenticationProvider<>(
            new McpApiKeyEntityRepository(apiKeyService, authorityService, userService));
        this.apiKeyService = apiKeyService;
        this.propertyService = propertyService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication authenticatedAuthentication = apiKeyAuthenticationProvider.authenticate(authentication);

        if (authenticatedAuthentication == null) {
            return null;
        }

        ApiKeyAuthenticationToken apiKeyAuthenticationToken = (ApiKeyAuthenticationToken) authentication;

        McpApiKeyCredentials mcpApiKeyCredentials = (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();
        McpApiKeyEntity mcpApiKeyEntity = (McpApiKeyEntity) authenticatedAuthentication.getPrincipal();

        if (mcpApiKeyEntity.getType() != null) {
            throw new BadCredentialsException("Invalid API key");
        }

        Property property = propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null);

        if (!Objects.equals(property.get("secretKey"), mcpApiKeyCredentials.getMcpServerSecretKey())) {
            throw new BadCredentialsException("Invalid MCP server secret key");
        }

        if (mcpApiKeyEntity.getEnvironment() != mcpApiKeyCredentials.getEnvironment()) {
            throw new BadCredentialsException("Invalid API key");
        }

        apiKeyService.updateLastUsedDate(mcpApiKeyEntity.getApiKeyId());

        return authenticatedAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
```

- [ ] **Step 5: Rewrite the configurer, delete the token**

Replace `ManagementMcpServerSecurityConfigurer.java` (keep license header):

```java
package com.bytechef.ai.mcp.server.security.web.configurer;

import com.bytechef.ai.mcp.server.security.web.authentication.ManagementMcpServerApiKeyAuthenticationProvider;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpApiKeyAuthenticationConverter;
import com.bytechef.platform.security.web.mcp.McpApiKeyHttpConfigurer;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;

/**
 * @author Ivica Cardic
 */
public class ManagementMcpServerSecurityConfigurer extends McpApiKeyHttpConfigurer {

    private static final String PATH_PATTERN = "^/api/management/.+/mcp";

    public ManagementMcpServerSecurityConfigurer(
        ApiKeyService apiKeyService, AuthorityService authorityService, PropertyService propertyService,
        UserService userService) {

        super(
            PATH_PATTERN, new McpApiKeyAuthenticationConverter("/api/management/"),
            new ManagementMcpServerApiKeyAuthenticationProvider(
                apiKeyService, authorityService, propertyService, userService));
    }
}
```

Delete the token:

```bash
git rm server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/security/web/authentication/ManagementMcpServerApiKeyAuthenticationToken.java
```

The contributor bean in `ManagementMcpServerConfiguration` keeps the same constructor call — verify it compiles unchanged.

- [ ] **Step 6: Run tests**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:test`
Expected: provider test PASSES (4 tests); pre-existing `ManagementMcpServerToolCallbackProviderTest` still PASSES.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/libs/ai/ai-mcp/ai-mcp-server
git commit -m "Require API key authentication on management MCP server"
```

---

### Task 7: Management MCP security filter chain test

**Files:**
- Test: `server/libs/ai/ai-mcp/ai-mcp-server/src/test/java/com/bytechef/ai/mcp/server/security/web/configurer/ManagementMcpServerSecurityFilterChainTest.java`

**Interfaces:**
- Consumes: `ManagementMcpServerSecurityConfigurer` (Task 6).

- [ ] **Step 1: Write the filter chain test**

Mirror `AutomationMcpServerSecurityFilterChainTest` (Task 5) with these deltas — same `@ExtendWith`/`@ContextConfiguration`/`@WebAppConfiguration` shell, same `mockApiKey` helper (but only call `setType` when non-null, and no `mockMcpServer`):

- Mock beans: `ApiKeyService`, `AuthorityService`, `PropertyService`, `UserService`.
- Stub route: `POST("/api/management/{secretKey}/mcp", ...)`; configurer under test: `new ManagementMcpServerSecurityConfigurer(apiKeyService, authorityService, propertyService, userService)`.
- `@BeforeEach` additionally stubs the platform property (re-stub after `reset(...)`):

```java
Property property = mock(Property.class);

when(property.get("secretKey")).thenReturn(MCP_SERVER_SECRET_KEY);
when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);
```

- Tests:
  - `testRequestWithoutBearerTokenIsRejected` → POST `/api/management/server-secret/mcp`, expect 401. **This is the test that proves the anonymous-access branch is gone.**
  - `testRequestWithValidAdminApiKeySucceeds` → admin key (no type, PRODUCTION), Bearer header, expect 200.
  - `testRequestWithTypedApiKeyIsRejected` → key with `PlatformType.AUTOMATION`, expect 401.
  - `testRequestWithWrongPathSecretIsRejected` → POST `/api/management/wrong-secret/mcp` with valid admin key, expect 401.
  - `testRequestWithEnvironmentMismatchIsRejected` → admin key with `Environment.PRODUCTION`, request carries header `X-ENVIRONMENT: STAGING`, expect 401.

- [ ] **Step 2: Run the test**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:test --tests "com.bytechef.ai.mcp.server.security.web.configurer.ManagementMcpServerSecurityFilterChainTest"`
Expected: 5 tests PASS.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add server/libs/ai/ai-mcp/ai-mcp-server
git commit -m "Add management MCP security filter chain tests"
```

---

### Task 8: Client — connection snippets show the Authorization header

**Files:**
- Modify: `client/src/shared/components/mcp-server/McpServerConfiguration.tsx`

**Interfaces:**
- Consumes: nothing new server-side; purely presentational. The component is shared by the management MCP settings page (`client/src/pages/settings/platform/mcp-server/McpServer.tsx`) and the automation MCP server list.

- [ ] **Step 1: Read the component fully before editing**

Read `client/src/shared/components/mcp-server/McpServerConfiguration.tsx` end to end (it has Claude/Cursor/Windsurf/Other tabs, each embedding `codeSnippet1`/`codeSnippet2`/`codeSnippet3`).

- [ ] **Step 2: Update the snippets**

Replace the three snippet constants so every variant carries the API key header, using a `YOUR_API_KEY` placeholder:

```tsx
const codeSnippet1 = `{
  "mcpServers": {
    "ByteChef": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "${mcpServerUrl}",
        "--header",
        "Authorization: Bearer YOUR_API_KEY"
      ]
    }
  }
}`;

const codeSnippet2 = `{
  "mcpServers": {
    "ByteChef": {
      "headers": {
        "Authorization": "Bearer YOUR_API_KEY"
      },
      "url": "${mcpServerUrl}"
    }
  }
}`;

const codeSnippet3 = mcpServerUrl;
```

(Keys inside the template strings are plain text, not object literals — the ESLint `sort-keys` rule does not apply, but keep them alphabetical anyway for consistency: `headers` before `url`.)

- [ ] **Step 3: Add the API-key guidance line**

Directly under the existing `<Alert>` in the Claude tab (and mirrored in the other tabs next to their snippets), add one sentence of guidance:

```tsx
<p className="text-sm text-muted-foreground">
    Replace YOUR_API_KEY with an API key from Settings. Requests without a valid API key are rejected.
</p>
```

Follow the component's existing markup patterns; if the other tabs share a layout helper, add the line once in the shared spot rather than four times.

- [ ] **Step 4: Verify**

```bash
cd client
npm run format
npm run check
```

Expected: format clean, lint + typecheck + tests PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/mcp-server/McpServerConfiguration.tsx
git commit -m "client - Show Authorization header in MCP server connection snippets"
```

---

### Task 9: Full verification and release note

**Files:**
- No new files; runs project-wide checks.

- [ ] **Step 1: Format and static analysis**

```bash
./gradlew spotlessApply
./gradlew :server:libs:platform:platform-security-web:platform-security-web-api:check \
  :server:libs:platform:platform-security:platform-security-service:check \
  :server:libs:automation:automation-ai:automation-ai-mcp-server:check \
  :server:libs:ai:ai-mcp:ai-mcp-server:check
```

Expected: BUILD SUCCESSFUL — Checkstyle, PMD, SpotBugs, and all tests green. Fix any style findings (common ones: missing blank line before control statements, SpotBugs `EI` on constructor-injected mutable deps → `@SuppressFBWarnings("EI")` as the existing providers do).

- [ ] **Step 2: Whole-server compile guard**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL — catches any consumer of the deleted token classes elsewhere in the monorepo. If another module references `AutomationMcpServerApiKeyAuthenticationToken` or `ManagementMcpServerApiKeyAuthenticationToken`, fix that call site to use the library's `ApiKeyAuthenticationToken` and report it in the summary.

- [ ] **Step 3: Commit any stragglers**

```bash
git status
```

If `spotlessApply` or check-fixes touched files, commit them:

```bash
git add -u
git commit -m "Apply formatting and static analysis fixes for MCP API key authentication"
```

- [ ] **Step 4: Record the release note**

Include this text verbatim in the final summary to the user (destined for release notes — do not commit it anywhere):

> **Breaking change:** The automation (`/api/automation/{secretKey}/mcp`) and management (`/api/management/{secretKey}/mcp`) MCP endpoints now require an API key sent as `Authorization: Bearer <api-key>` in addition to the existing URL. Automation MCP servers accept Automation API keys (Settings → API Keys) matching the server's environment; the management MCP server accepts Admin API Keys. Existing MCP client configurations keep their URLs but must add the header. The embedded MCP server is unchanged.

---

## Plan self-review notes

- **Spec coverage:** mandatory Bearer on both servers (Tasks 4/6), type + environment checks (Tasks 4/6), anonymous branch deletion (Task 6 rewrite has no null-token path), principal with user authorities (Task 3 entity + repository), `last_used_date` on success only (providers update after all checks), tenant routing before repository access (Task 3 filter), 401 without `WWW-Authenticate` (library's `HttpStatusEntryPoint` behavior), day-one compatibility spike (Task 1), UI snippet + release note (Tasks 8/9). Phase 2 items are explicitly out of scope. The spec's "real streamable-HTTP handshake" integration test is downgraded to filter-chain MockMvc tests plus a stub route — standing up the full MCP transport in a module test would drag in the whole Atlas engine; flag this consciously in the final summary as remaining manual-verification scope (`bootRun` + an MCP client).
- **Environment-check placement deviation** from the spec is documented in the header.
- **Type consistency:** `McpApiKeyCredentials(Environment, String mcpServerSecretKey, String secretKey)` and `McpApiKeyEntity(ApiKey, String login, List<GrantedAuthority>)` constructor orders are used identically in Tasks 3, 4, 5, 6, 7; `McpApiKeyHttpConfigurer(String, AuthenticationConverter, AuthenticationProvider)` order matches both subclass `super(...)` calls.
