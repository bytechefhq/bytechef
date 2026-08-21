# T3 MCP API-Key Auth Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make both MCP servers authenticate on the server secret only, with a hardened secret check and a defined synthetic `"system"` principal (Management `ROLE_ADMIN`, Automation `ROLE_USER`), deleting the dead/unvalidated per-user API-key paths.

**Architecture:** Each provider validates the server secret and returns a token built from an on-the-fly `org.springframework.security.core.userdetails.User("system", "", [role])` via the existing `AbstractApiKeyAuthenticationToken(User)` constructor. The unauthenticated request token (built by the converter) still carries the tenantId; the result token does not need to (the filter sets `TenantContext` from the request token before the provider runs). Per-user identity is a later phase.

**Tech Stack:** Java 25, Spring Security (`AuthenticationProvider`), JUnit 5 + Mockito + AssertJ.

## Global Constraints

- Files under `server/libs/` use the Apache 2.0 header. (Spotless picks the header by file content.)
- Run `./gradlew spotlessApply` before every commit; per-module `:check` is the gate.
- Blank line before control statements and after a variable modification a later statement uses (Java style rules in CLAUDE.md).
- Test method names are camelCase without underscores; unit test classes end in `Test`.
- Commit messages: server-side `gecko <description>`; end every commit body with the `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer.
- Branch is `0_732`, the user commits in parallel — never `git commit --amend`; make fresh commits, stage only files this plan touches.
- Authorities come from `com.bytechef.platform.security.constant.AuthorityConstants` (`ADMIN = "ROLE_ADMIN"`, `USER = "ROLE_USER"`). Synthetic principal username: `"system"`. Constant-time secret comparison via `java.security.MessageDigest.isEqual`.
- Secret stays in the URL path; per-user identity (API key / OAuth) is deferred to a follow-up phase for both servers.

---

## File Structure

**Task 1 — Management MCP (module `server/libs/ai/ai-mcp/ai-mcp-server`):**
- Modify: `.../security/web/authentication/ManagementMcpServerApiKeyAuthenticationToken.java`
- Modify: `.../security/web/authentication/ManagementMcpServerApiKeyAuthenticationProvider.java`
- Modify: `.../security/web/configurer/ManagementMcpServerSecurityConfigurer.java`
- Modify: `.../config/ManagementMcpServerConfiguration.java` (bean factory params)
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server/build.gradle.kts` (add `platform-api` for `AuthorityConstants`)
- Test: `.../security/web/authentication/ManagementMcpServerApiKeyAuthenticationProviderTest.java`

**Task 2 — Automation MCP (module `server/libs/automation/automation-ai/automation-ai-mcp-server`):**
- Modify: `.../security/web/authentication/AutomationMcpServerApiKeyAuthenticationToken.java`
- Modify: `.../security/web/authentication/AutomationMcpServerApiKeyAuthenticationProvider.java`
- Modify: `.../security/web/configurer/AutomationMcpServerSecurityConfigurer.java`
- Test: `.../security/web/authentication/AutomationMcpServerApiKeyAuthenticationProviderTest.java`

**Task 3 — Close out:**
- Modify: `gecko-remediation-tasks.md`

---

## Task 1: Management MCP — secret-only auth

**Files:** see File Structure, Task 1.

**Interfaces:**
- Produces: `ManagementMcpServerApiKeyAuthenticationProvider(PropertyService)`; `authenticate` validates the `mcp.server` platform secret and returns a token from a `ROLE_ADMIN` `"system"` principal. Token gains a `(String mcpServerSecretKey, String tenantId)` request constructor and keeps `(User)`; loses the no-arg constructor and the `authSecretKey` field.

- [ ] **Step 1: Rewrite the token class**

Replace the body of `ManagementMcpServerApiKeyAuthenticationToken.java` (keep the Apache header) with:

```java
package com.bytechef.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
public class ManagementMcpServerApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private String mcpServerSecretKey;

    public ManagementMcpServerApiKeyAuthenticationToken(String mcpServerSecretKey, String tenantId) {
        super(-1, tenantId);

        this.mcpServerSecretKey = mcpServerSecretKey;
    }

    @SuppressFBWarnings("EI")
    public ManagementMcpServerApiKeyAuthenticationToken(User user) {
        super(user);
    }

    public String getMcpServerSecretKey() {
        return mcpServerSecretKey;
    }
}
```

- [ ] **Step 2: Rewrite the provider**

Replace `ManagementMcpServerApiKeyAuthenticationProvider.java` (keep the Apache header) with:

```java
package com.bytechef.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
public class ManagementMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerApiKeyAuthenticationProvider(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ManagementMcpServerApiKeyAuthenticationToken token =
            (ManagementMcpServerApiKeyAuthenticationToken) authentication;

        Property property = propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null);

        String configuredSecretKey = property == null ? null : (String) property.get("secretKey");
        String providedSecretKey = token.getMcpServerSecretKey();

        if (configuredSecretKey == null || configuredSecretKey.isBlank() || providedSecretKey == null ||
            providedSecretKey.isBlank() ||
            !MessageDigest.isEqual(
                configuredSecretKey.getBytes(StandardCharsets.UTF_8),
                providedSecretKey.getBytes(StandardCharsets.UTF_8))) {

            throw new BadCredentialsException("Invalid MCP server secret key");
        }

        return new ManagementMcpServerApiKeyAuthenticationToken(
            new User("system", "", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ManagementMcpServerApiKeyAuthenticationToken.class);
    }
}
```

- [ ] **Step 3: Simplify the configurer**

In `ManagementMcpServerSecurityConfigurer.java`: change the constructor to take only `PropertyService`, drop the `ApiKeyService`/`AuthorityService`/`UserService` imports, and stop reading the auth header in the converter. Replace the constructor and converter:

```java
    public ManagementMcpServerSecurityConfigurer(PropertyService propertyService) {
        super(
            PATH_PATTERN, new McpServerApiKeyAuthenticationConverter(),
            new ManagementMcpServerApiKeyAuthenticationProvider(propertyService));
    }
```
```java
    private static class McpServerApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

        @Override
        public Authentication convert(HttpServletRequest request) {
            String servletPath = request.getServletPath();

            String mcpServerSecretKey = servletPath.replace("/api/management/", "")
                .replace("/mcp", "");

            TenantKey tenantKey = TenantKey.parse(mcpServerSecretKey);

            return new ManagementMcpServerApiKeyAuthenticationToken(mcpServerSecretKey, tenantKey.getTenantId());
        }
    }
```
Remove the now-unused imports `com.bytechef.platform.security.service.ApiKeyService`, `com.bytechef.platform.user.service.AuthorityService`, `com.bytechef.platform.user.service.UserService`.

- [ ] **Step 4: Simplify the configuration bean**

In `ManagementMcpServerConfiguration.java`, the `mcpServerSecurityConfigurerContributor` bean (lines ~132-148) currently takes four services only to build the configurer. Replace it with:

```java
    @Bean
    SecurityConfigurerContributor mcpServerSecurityConfigurerContributor(PropertyService propertyService) {
        return new SecurityConfigurerContributor() {

            @Override
            @SuppressWarnings("unchecked")
            public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T
                getSecurityConfigurerAdapter() {

                return (T) new ManagementMcpServerSecurityConfigurer(propertyService);
            }
        };
    }
```
Remove the now-unused imports in this file: `com.bytechef.platform.security.service.ApiKeyService`, `com.bytechef.platform.user.service.AuthorityService`, `com.bytechef.platform.user.service.UserService` — **only if** they are not used elsewhere in the file (grep the file first: `grep -n "ApiKeyService\|AuthorityService\|UserService" <file>`; remove each import only when it has no remaining references).

- [ ] **Step 5: Add the `platform-api` dependency (for `AuthorityConstants`)**

The management module does not yet depend on `platform-api`. In `server/libs/ai/ai-mcp/ai-mcp-server/build.gradle.kts`, add to `dependencies`:
```kotlin
    implementation(project(":server:libs:platform:platform-api"))
```

- [ ] **Step 6: Write the provider test**

Create `.../security/web/authentication/ManagementMcpServerApiKeyAuthenticationProviderTest.java`:

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

package com.bytechef.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Ivica Cardic
 */
class ManagementMcpServerApiKeyAuthenticationProviderTest {

    private final PropertyService propertyService = mock(PropertyService.class);
    private final ManagementMcpServerApiKeyAuthenticationProvider provider =
        new ManagementMcpServerApiKeyAuthenticationProvider(propertyService);

    @Test
    void testValidSecretAuthenticatesAsAdmin() {
        givenConfiguredSecret("topsecret");

        Authentication result = provider.authenticate(
            new ManagementMcpServerApiKeyAuthenticationToken("topsecret", "public"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)).contains(AuthorityConstants.ADMIN);
    }

    @Test
    void testWrongSecretRejected() {
        givenConfiguredSecret("topsecret");

        assertThatThrownBy(() -> provider.authenticate(
            new ManagementMcpServerApiKeyAuthenticationToken("wrong", "public")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testBlankConfiguredSecretRejected() {
        givenConfiguredSecret("");

        assertThatThrownBy(() -> provider.authenticate(
            new ManagementMcpServerApiKeyAuthenticationToken("", "public")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testMissingPropertyRejected() {
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(null);

        assertThatThrownBy(() -> provider.authenticate(
            new ManagementMcpServerApiKeyAuthenticationToken("topsecret", "public")))
                .isInstanceOf(BadCredentialsException.class);
    }

    private void givenConfiguredSecret(String secret) {
        Property property = mock(Property.class);

        when(property.get("secretKey")).thenReturn(secret);
        when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null)).thenReturn(property);
    }
}
```

- [ ] **Step 7: Run the test**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:test --tests "*ManagementMcpServerApiKeyAuthenticationProviderTest"`
Expected: PASS (4 tests). If it fails to compile on `AuthorityConstants`, confirm Step 5's dependency was added.

- [ ] **Step 8: Compile the module (catches configurer/configuration cascade)**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:compileJava`
Expected: BUILD SUCCESSFUL. Fix any remaining references to removed constructors/params.

- [ ] **Step 9: Format and commit**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:spotlessApply`

```bash
git add server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/security/web/authentication/ManagementMcpServerApiKeyAuthenticationToken.java \
        server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/security/web/authentication/ManagementMcpServerApiKeyAuthenticationProvider.java \
        server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/security/web/configurer/ManagementMcpServerSecurityConfigurer.java \
        server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/config/ManagementMcpServerConfiguration.java \
        server/libs/ai/ai-mcp/ai-mcp-server/build.gradle.kts \
        server/libs/ai/ai-mcp/ai-mcp-server/src/test/java/com/bytechef/ai/mcp/server/security/web/authentication/ManagementMcpServerApiKeyAuthenticationProviderTest.java
git commit -m "gecko Make Management MCP auth secret-only with ROLE_ADMIN system principal (T3)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Automation MCP — secret-only auth

**Files:** see File Structure, Task 2.

**Interfaces:**
- Produces: `AutomationMcpServerApiKeyAuthenticationProvider(McpServerService)`; `authenticate` rejects blank/unknown/disabled servers and returns a token from a `ROLE_USER` `"system"` principal. Token gains a `(String mcpServerSecretKey, String tenantId)` request constructor and a `(User)` constructor; loses the 1-arg `authSecretKey` constructor, the `authSecretKey` field, and the `getPrincipal()` override.

- [ ] **Step 1: Rewrite the token class**

Replace the body of `AutomationMcpServerApiKeyAuthenticationToken.java` (keep the Apache header) with:

```java
package com.bytechef.automation.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
public class AutomationMcpServerApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

    private String mcpServerSecretKey;

    public AutomationMcpServerApiKeyAuthenticationToken(String mcpServerSecretKey, String tenantId) {
        super(-1, tenantId);

        this.mcpServerSecretKey = mcpServerSecretKey;
    }

    @SuppressFBWarnings("EI")
    public AutomationMcpServerApiKeyAuthenticationToken(User user) {
        super(user);
    }

    public String getMcpServerSecretKey() {
        return mcpServerSecretKey;
    }
}
```

- [ ] **Step 2: Rewrite the provider**

Replace `AutomationMcpServerApiKeyAuthenticationProvider.java` (keep the Apache header) with:

```java
package com.bytechef.automation.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
public class AutomationMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    public AutomationMcpServerApiKeyAuthenticationProvider(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        AutomationMcpServerApiKeyAuthenticationToken token =
            (AutomationMcpServerApiKeyAuthenticationToken) authentication;

        String secretKey = token.getMcpServerSecretKey();

        if (secretKey == null || secretKey.isBlank()) {
            throw new BadCredentialsException("Invalid secret key");
        }

        McpServer mcpServer;

        try {
            mcpServer = mcpServerService.getMcpServer(secretKey);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new BadCredentialsException("Invalid secret key", illegalArgumentException);
        }

        if (!mcpServer.isEnabled()) {
            throw new BadCredentialsException("MCP server is disabled");
        }

        return new AutomationMcpServerApiKeyAuthenticationToken(
            new User("system", "", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER))));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(AutomationMcpServerApiKeyAuthenticationToken.class);
    }
}
```

- [ ] **Step 3: Drop the auth header from the configurer converter**

In `AutomationMcpServerSecurityConfigurer.java`, replace the converter's `convert` so it no longer reads the auth header and uses the 2-arg token constructor:

```java
        @Override
        public Authentication convert(HttpServletRequest request) {
            String servletPath = request.getServletPath();

            String mcpServerSecretKey = servletPath.replace("/api/automation/", "")
                .replace("/mcp", "");

            TenantKey tenantKey = TenantKey.parse(mcpServerSecretKey);

            return new AutomationMcpServerApiKeyAuthenticationToken(mcpServerSecretKey, tenantKey.getTenantId());
        }
```
(The configurer constructor already takes only `McpServerService`; no other change there.)

- [ ] **Step 4: Write the provider test**

Create `.../security/web/authentication/AutomationMcpServerApiKeyAuthenticationProviderTest.java`:

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

package com.bytechef.automation.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Ivica Cardic
 */
class AutomationMcpServerApiKeyAuthenticationProviderTest {

    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final AutomationMcpServerApiKeyAuthenticationProvider provider =
        new AutomationMcpServerApiKeyAuthenticationProvider(mcpServerService);

    @Test
    void testValidEnabledServerAuthenticatesAsUser() {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.isEnabled()).thenReturn(true);
        when(mcpServerService.getMcpServer("secret123")).thenReturn(mcpServer);

        Authentication result = provider.authenticate(
            new AutomationMcpServerApiKeyAuthenticationToken("secret123", "public"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)).contains(AuthorityConstants.USER);
    }

    @Test
    void testUnknownSecretRejected() {
        when(mcpServerService.getMcpServer("nope")).thenThrow(new IllegalArgumentException("not found"));

        assertThatThrownBy(() -> provider.authenticate(
            new AutomationMcpServerApiKeyAuthenticationToken("nope", "public")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testDisabledServerRejected() {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.isEnabled()).thenReturn(false);
        when(mcpServerService.getMcpServer("secret123")).thenReturn(mcpServer);

        assertThatThrownBy(() -> provider.authenticate(
            new AutomationMcpServerApiKeyAuthenticationToken("secret123", "public")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testBlankSecretRejectedBeforeLookup() {
        assertThatThrownBy(() -> provider.authenticate(
            new AutomationMcpServerApiKeyAuthenticationToken("", "public")))
                .isInstanceOf(BadCredentialsException.class);

        verify(mcpServerService, never()).getMcpServer("");
    }
}
```

- [ ] **Step 5: Run the test**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:test --tests "*AutomationMcpServerApiKeyAuthenticationProviderTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: Compile the module**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Format and commit**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:spotlessApply`

```bash
git add server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/security/web/authentication/AutomationMcpServerApiKeyAuthenticationToken.java \
        server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/security/web/authentication/AutomationMcpServerApiKeyAuthenticationProvider.java \
        server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/security/web/configurer/AutomationMcpServerSecurityConfigurer.java \
        server/libs/automation/automation-ai/automation-ai-mcp-server/src/test/java/com/bytechef/automation/ai/mcp/server/security/web/authentication/AutomationMcpServerApiKeyAuthenticationProviderTest.java
git commit -m "gecko Make Automation MCP auth secret-only with ROLE_USER system principal (T3)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Close out the tracker

- [ ] **Step 1: Run check on both modules**

Run:
```bash
./gradlew \
  :server:libs:ai:ai-mcp:ai-mcp-server:check \
  :server:libs:automation:automation-ai:automation-ai-mcp-server:check
```
Expected: BUILD SUCCESSFUL. Fix any checkstyle/PMD/SpotBugs findings the new code introduces (e.g. an unused-import the rewrites left behind). If a pre-existing test in either module is already broken on the branch (unrelated), confirm it fails without these changes and note it rather than fixing out-of-scope code.

- [ ] **Step 2: Mark T3 done in the tracker**

In `gecko-remediation-tasks.md`, change `- [ ] **T3.` to `- [x] **T3.` and append:
> **Done** (spec/plan `docs/superpowers/{specs,plans}/2026-06-21-mcp-apikey-auth*`): reframed from "require both credentials" to **secret-only auth** for both MCP servers, since the per-user API key was dead code (nothing read `getAuthSecretKey()`, the principal, or authorities; tenant comes from the secret, automation scope from the `McpServer`→workspace mapping). Both providers now validate the server secret (Management: `mcp.server` property, blank-reject + constant-time `MessageDigest.isEqual`; Automation: `getMcpServer` lookup which throws on unknown + an `isEnabled()` check) and return a synthetic `"system"` principal — `ROLE_ADMIN` (management) / `ROLE_USER` (automation). Deleted the unvalidated API-key branches/fields. Secret stays in the URL; **per-user identity (API key/OAuth) deferred to a follow-up phase** for both servers.

- [ ] **Step 3: Commit**

```bash
git add gecko-remediation-tasks.md
git commit -m "gecko Mark T3 MCP API-key auth done (secret-only; per-user identity deferred)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- Synthetic system principal (shared shape) → Tasks 1 & 2 (inline `new User("system","",[role])`) ✓
- Management: secret hardening + ROLE_ADMIN + delete API-key branch/deps → Task 1 ✓
- Automation: blank-reject + lookup + isEnabled + ROLE_USER + drop authSecretKey → Task 2 ✓
- Token classes (drop authSecretKey, drop no-arg/1-arg ctors, keep request + User ctors) → Tasks 1 & 2 ✓
- Secret in URL kept; per-user deferred → reflected (converter still derives secret from path; no header read) ✓
- Tests per provider (valid→role, blank, wrong, missing/disabled) → Tasks 1 & 2 ✓
- Tracker close-out + honesty note → Task 3 ✓

**Placeholder scan:** No TBD/TODO; every code step shows full file/replacement. The two "remove import only if unused" notes (Task 1 Step 4) are explicit grep-then-remove instructions, not silent gaps.

**Type consistency:** `ManagementMcpServerApiKeyAuthenticationProvider(PropertyService)` matches the configurer (Step 3) and configuration bean (Step 4). Both tokens expose `getMcpServerSecretKey()` and a `(String mcpServerSecretKey, String tenantId)` request ctor + `(User)` ctor, used identically by their providers, converters, and tests. `AuthorityConstants.ADMIN`/`.USER` and `new User("system","",[...])` are identical across both providers. `McpServer.isEnabled()` and `PropertyService.getProperty(String, Property.Scope, null)` / `Property.get("secretKey")` match the verified signatures.
