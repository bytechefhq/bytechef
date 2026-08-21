# MCP Server Optional Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make API-key/OAuth authentication opt-in per MCP server — existing servers stay unauthenticated (unchanged), new servers require authentication by default — across the automation, management, and embedded MCP servers.

**Architecture:** A per-server `authenticationRequired` flag (a `mcp_server` column for automation/embedded; a key in the `mcp.server` tenant `Property` for management). The shared API-key converter stops rejecting token-less requests; each server's authentication provider resolves the target server from the URL path secret and, when the server opts out, returns a successful *anonymous* authentication (no authorities) so the request satisfies the global `/api/** → authenticated()` rule while the tool filter sees empty authorities. A server-side invariant forbids `authenticationRequired=false` together with `enforceToolAuthorization=true`.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Security, Spring Data JDBC, Liquibase, GraphQL (Spring for GraphQL), React 19 + TypeScript + graphql-codegen, JUnit 5 + Testcontainers.

**Design spec:** `docs/superpowers/specs/2026-07-15-mcp-server-optional-authentication-design.md`

## Global Constraints

- Server files under `server/ee/` use the ByteChef Enterprise license header and a `@version ee` Javadoc tag; all other server files use the Apache 2.0 header. (The embedded server is EE.)
- Java: one blank line before control statements and after a variable modification that precedes its use; no `_` method prefixes; descriptive variable names; no trailing blank line before a class's closing brace.
- Enum/JDBC storage unchanged here; the new column is a plain `BOOLEAN NOT NULL DEFAULT false`.
- GraphQL enum values are SCREAMING_SNAKE_CASE (not relevant to the boolean fields added here, but keep in mind for any new inputs).
- Client: ESLint `sort-keys` (object keys alphabetical, no autofix), interface names end in `I`/`Props`, named imports sorted, `twMerge` (not `cn()`), Lucide icons imported with `Icon` suffix. Run `npm run check` in `client/` before committing client changes.
- Run `./gradlew spotlessApply` before committing server changes; integration test classes end in `IntTest`, unit tests end in `Test` and drop `Impl` from the name.
- Commit messages: server `<ticket> <description>`; client `<ticket> client - <description>`. No ticket number is assigned yet — use a descriptive message without a number, or the ticket the user supplies. End commit messages with the `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>` trailer. Create fresh commits (never amend) on branch `0_732`; stage only files this task touched.

## File Structure

**Shared domain / persistence (automation + embedded)**
- Modify: `server/libs/platform/platform-mcp/platform-mcp-api/src/main/java/com/bytechef/platform/mcp/domain/McpServer.java` — add `authenticationRequired` field (Java default `true`), getter/setter.
- Create: `server/libs/platform/platform-mcp/platform-mcp-service/src/main/resources/config/liquibase/changelog/platform/mcp/20260715000001_platform_mcp_authentication_required.xml` — add column defaulting `false` (backfills existing rows).
- Modify: `server/libs/platform/platform-mcp/platform-mcp-service/src/main/java/com/bytechef/platform/mcp/service/McpServerServiceImpl.java` — `update(McpServer)` copies both flags + invariant check.
- Test: `server/libs/platform/platform-mcp/platform-mcp-service/src/test/java/com/bytechef/platform/mcp/service/McpServerServiceIntTest.java` — default + hydration + invariant.

**Automation GraphQL + client**
- Modify: `server/libs/platform/platform-mcp/platform-mcp-graphql/src/main/resources/graphql/mcp-server.graphqls` — add fields.
- Modify: `server/libs/platform/platform-mcp/platform-mcp-graphql/src/main/java/com/bytechef/platform/mcp/web/graphql/McpServerGraphQlController.java` — wire `authenticationRequired`.
- Modify: `client/src/graphql/automation/configuration/updateMcpServer.graphql` (+ the workspace/embedded list queries that select `enforceToolAuthorization`).
- Modify: `client/src/pages/automation/mcp-servers/components/McpServerDialog.tsx` and `client/src/ee/pages/embedded/mcp-servers/components/McpServerDialog.tsx` — toggle + coupling.
- Regenerate: `client/src/shared/middleware/graphql.ts` + `graphql-types.ts` via `npx graphql-codegen`.

**Shared enforcement**
- Create: `server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/mcp/McpAnonymousAuthenticationToken.java`.
- Modify: `.../platform/security/web/mcp/McpApiKeyAuthenticationConverter.java` — permissive on missing token.
- Test: `.../platform/security/web/mcp/McpApiKeyAuthenticationConverterTest.java`.

**Automation enforcement**
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/security/web/authentication/AutomationMcpServerApiKeyAuthenticationProvider.java`.
- Test: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/test/java/com/bytechef/automation/ai/mcp/server/security/web/AutomationMcpServerSecurityIntTest.java`.

**Management**
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server-configuration/ai-mcp-server-configuration-graphql/src/main/java/com/bytechef/ai/mcp/server/configuration/service/ManagementMcpServerService.java` + `ManagementMcpServerServiceImpl.java`.
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/security/web/authentication/ManagementMcpServerApiKeyAuthenticationProvider.java`.
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server-configuration/ai-mcp-server-configuration-graphql/src/main/resources/graphql/management-mcp-server.graphqls` + controller.
- Modify: `client/src/pages/settings/platform/mcp-server/McpServer.tsx` + management client graphql ops + regen.

**Embedded (risk-flagged phase)**
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/src/main/java/com/bytechef/ee/embedded/ai/mcp/server/security/web/...` (signing-key converter/provider, OAuth2 converter/provider) and `McpDiscoveryAuthenticationFilter` usage.

---

## Phase 1 — Shared domain & persistence

### Task 1: `authenticationRequired` field, migration, hydration test

**Files:**
- Modify: `server/libs/platform/platform-mcp/platform-mcp-api/src/main/java/com/bytechef/platform/mcp/domain/McpServer.java`
- Create: `server/libs/platform/platform-mcp/platform-mcp-service/src/main/resources/config/liquibase/changelog/platform/mcp/20260715000001_platform_mcp_authentication_required.xml`
- Test: `server/libs/platform/platform-mcp/platform-mcp-service/src/test/java/com/bytechef/platform/mcp/service/McpServerServiceIntTest.java`

**Interfaces:**
- Produces: `McpServer.isAuthenticationRequired(): boolean`, `McpServer.setAuthenticationRequired(boolean)`. A newly constructed `McpServer` returns `true`; a row loaded from the DB returns its column value.

- [ ] **Step 1: Write the failing test** (append to `McpServerServiceIntTest`)

```java
@Test
void testNewMcpServerDefaultsAuthenticationRequiredTrue() {
    McpServer mcpServer = mcpServerService.create(
        "auth-default", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

    McpServer loaded = mcpServerService.getMcpServer(mcpServer.getSecretKey());

    assertThat(loaded.isAuthenticationRequired()).isTrue();
}

@Test
void testLegacyRowLoadsAuthenticationRequiredFalse() {
    McpServer mcpServer = mcpServerService.create(
        "auth-legacy", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

    new JdbcTemplate(dataSource).update(
        "UPDATE mcp_server SET authentication_required = false WHERE id = ?", mcpServer.getId());

    McpServer loaded = mcpServerService.getMcpServer(mcpServer.getSecretKey());

    assertThat(loaded.isAuthenticationRequired()).isFalse();
}
```

Check the test class already `@Autowired`s a `DataSource`; if not, add one (mirror `AutomationMcpServerSecurityIntTest`). Add imports for `JdbcTemplate`, `assertThat`, `PlatformType`, `Environment` as needed.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-service:test --tests "*McpServerServiceIntTest"`
Expected: compile failure (`isAuthenticationRequired` undefined) or test failure.

- [ ] **Step 3: Add the field and accessors to `McpServer`**

After the `enforceToolAuthorization` field (`McpServer.java:64-65`):

```java
    @Column("authentication_required")
    private boolean authenticationRequired = true;
```

After `setEnforceToolAuthorization` (around line 212-214), add:

```java
    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    public void setAuthenticationRequired(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }
```

Note: the Java default `= true` gives new instances `true`; Spring Data JDBC overwrites the field from the column on load (the no-arg `McpServer()` constructor + property population path), so legacy rows load `false`.

- [ ] **Step 4: Create the migration**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="20260715000001-1" author="Ivica Cardic">
        <addColumn tableName="mcp_server">
            <column name="authentication_required" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

The `platform/mcp/` directory is picked up by `<includeAll>` in `master.xml` — no master edit needed. If a stale copy exists under `build/resources/`, delete it before running tests.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-service:test --tests "*McpServerServiceIntTest"`
Expected: PASS. (This confirms both the Java default and the hydration override.)

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-mcp/platform-mcp-api/src/main/java/com/bytechef/platform/mcp/domain/McpServer.java \
        server/libs/platform/platform-mcp/platform-mcp-service/src/main/resources/config/liquibase/changelog/platform/mcp/20260715000001_platform_mcp_authentication_required.xml \
        server/libs/platform/platform-mcp/platform-mcp-service/src/test/java/com/bytechef/platform/mcp/service/McpServerServiceIntTest.java
git commit -m "Add authenticationRequired flag to McpServer with existing-false new-true default"
```

### Task 2: Persist both flags on update + enforce the invariant

Fixes a latent bug: `McpServerServiceImpl.update(McpServer)` currently does **not** copy `enforceToolAuthorization` onto the re-fetched entity, so that edit is silently dropped. We copy both flags here and add the `authenticationRequired=false ⇒ enforceToolAuthorization=false` invariant.

**Files:**
- Modify: `server/libs/platform/platform-mcp/platform-mcp-service/src/main/java/com/bytechef/platform/mcp/service/McpServerServiceImpl.java:129-139`
- Test: `server/libs/platform/platform-mcp/platform-mcp-service/src/test/java/com/bytechef/platform/mcp/service/McpServerServiceIntTest.java`

**Interfaces:**
- Consumes: `McpServer.isAuthenticationRequired()/setAuthenticationRequired(boolean)` from Task 1.
- Produces: `McpServerServiceImpl.update(McpServer)` now persists `enforceToolAuthorization` and `authenticationRequired`, and throws `IllegalArgumentException` when the resulting state has `authenticationRequired=false && enforceToolAuthorization=true`.

- [ ] **Step 1: Write the failing tests** (append to `McpServerServiceIntTest`)

```java
@Test
void testUpdatePersistsAuthenticationRequired() {
    McpServer mcpServer = mcpServerService.create(
        "auth-update", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

    mcpServer.setAuthenticationRequired(false);

    mcpServerService.update(mcpServer);

    McpServer loaded = mcpServerService.getMcpServer(mcpServer.getSecretKey());

    assertThat(loaded.isAuthenticationRequired()).isFalse();
}

@Test
void testUpdateRejectsNoAuthWithToolAuthorization() {
    McpServer mcpServer = mcpServerService.create(
        "auth-invariant", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

    mcpServer.setAuthenticationRequired(false);
    mcpServer.setEnforceToolAuthorization(true);

    assertThatThrownBy(() -> mcpServerService.update(mcpServer))
        .isInstanceOf(IllegalArgumentException.class);
}
```

Add `import static org.assertj.core.api.Assertions.assertThatThrownBy;` if missing.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-service:test --tests "*McpServerServiceIntTest"`
Expected: `testUpdatePersistsAuthenticationRequired` fails (value not persisted); `testUpdateRejectsNoAuthWithToolAuthorization` fails (no exception).

- [ ] **Step 3: Update `update(McpServer)`**

Replace the body at `McpServerServiceImpl.java:129-139` with:

```java
    @Override
    public McpServer update(McpServer mcpServer) {
        if (!mcpServer.isAuthenticationRequired() && mcpServer.isEnforceToolAuthorization()) {
            throw new IllegalArgumentException(
                "enforceToolAuthorization requires authenticationRequired to be enabled");
        }

        McpServer currentMcpServer = getMcpServer(mcpServer.getId());

        currentMcpServer.setName(mcpServer.getName());
        currentMcpServer.setEnabled(mcpServer.isEnabled());
        currentMcpServer.setAuthenticationRequired(mcpServer.isAuthenticationRequired());
        currentMcpServer.setEnforceToolAuthorization(mcpServer.isEnforceToolAuthorization());
        currentMcpServer.setSecretKey(mcpServer.getSecretKey());
        currentMcpServer.setTagIds(mcpServer.getTagIds());
        currentMcpServer.setVersion(mcpServer.getVersion());

        return mcpServerRepository.save(currentMcpServer);
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-service:test --tests "*McpServerServiceIntTest"`
Expected: PASS.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-mcp/platform-mcp-service/src/main/java/com/bytechef/platform/mcp/service/McpServerServiceImpl.java \
        server/libs/platform/platform-mcp/platform-mcp-service/src/test/java/com/bytechef/platform/mcp/service/McpServerServiceIntTest.java
git commit -m "Persist enforceToolAuthorization and authenticationRequired on McpServer update with invariant"
```

---

## Phase 2 — Automation GraphQL + client

### Task 3: Expose `authenticationRequired` over GraphQL

**Files:**
- Modify: `server/libs/platform/platform-mcp/platform-mcp-graphql/src/main/resources/graphql/mcp-server.graphqls`
- Modify: `server/libs/platform/platform-mcp/platform-mcp-graphql/src/main/java/com/bytechef/platform/mcp/web/graphql/McpServerGraphQlController.java:85-96,162`
- Test: `server/libs/platform/platform-mcp/platform-mcp-graphql/src/test/java/com/bytechef/platform/mcp/web/graphql/McpServerGraphQlControllerIntTest.java`

**Interfaces:**
- Consumes: `McpServerServiceImpl.update(McpServer)` invariant from Task 2.
- Produces: `McpServer.authenticationRequired: Boolean!` (query) and `McpServerUpdateInput.authenticationRequired: Boolean` (mutation input).

- [ ] **Step 1: Add schema fields**

In `mcp-server.graphqls`, add to the `McpServer` type (after `enforceToolAuthorization`):

```graphql
    authenticationRequired: Boolean!
```

And to `McpServerUpdateInput`:

```graphql
    authenticationRequired: Boolean
```

- [ ] **Step 2: Wire the controller**

In `McpServerGraphQlController.updateMcpServer` (lines 85-96), after the `enforceToolAuthorization` block and before `return mcpServer;`, add:

```java
        if (input.authenticationRequired() != null) {
            mcpServer.setAuthenticationRequired(input.authenticationRequired());

            mcpServer = mcpServerService.update(mcpServer);
        }
```

Extend the record at line 162:

```java
    public record McpServerUpdateInput(
        String name, Boolean enabled, Boolean enforceToolAuthorization, Boolean authenticationRequired) {
    }
```

Note: because `update(McpServer)` enforces the invariant, the client must send `enforceToolAuthorization=false` in the same mutation when turning authentication off (Task 4 guarantees this). Consider ordering so `authenticationRequired` is applied in the same object state — the existing two-call structure works because each `update(McpServer)` re-reads the persisted flags; if a test exercises the both-off transition, set both in the input.

- [ ] **Step 3: Add a controller int test**

In `McpServerGraphQlControllerIntTest`, add a test that mutates `authenticationRequired` and asserts the returned value (mirror the existing `enforceToolAuthorization` test in that file; if none exists, mirror an `enabled` update test). Include a case asserting the invariant mutation surfaces an error.

- [ ] **Step 4: Run tests**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-graphql:test --tests "*McpServerGraphQlControllerIntTest"`
Expected: PASS.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-mcp/platform-mcp-graphql/
git commit -m "Expose McpServer authenticationRequired over GraphQL"
```

### Task 4: Client toggle with tool-authorization coupling

**Files:**
- Modify: `client/src/graphql/automation/configuration/updateMcpServer.graphql`
- Modify: `client/src/graphql/automation/configuration/workspaceMcpServers.graphql`, `client/src/graphql/embedded/configuration/embeddedMcpServers.graphql` (add `authenticationRequired` to the selection sets that already select `enforceToolAuthorization`)
- Modify: `client/src/pages/automation/mcp-servers/components/McpServerDialog.tsx`
- Modify: `client/src/ee/pages/embedded/mcp-servers/components/McpServerDialog.tsx`
- Regenerate: `client/src/shared/middleware/graphql.ts`, `client/src/shared/middleware/graphql-types.ts`

**Interfaces:**
- Consumes: `authenticationRequired` from the GraphQL schema (Task 3).
- Produces: dialogs that submit `authenticationRequired`, and force `enforceToolAuthorization=false` in the form when `authenticationRequired` is off.

- [ ] **Step 1: Update the GraphQL operations**

In `updateMcpServer.graphql`, add `authenticationRequired` to the selection set:

```graphql
mutation updateMcpServer($id: ID!, $input: McpServerUpdateInput!) {
    updateMcpServer(id: $id, input: $input) {
        id
        name
        enabled
        enforceToolAuthorization
        authenticationRequired
    }
}
```

Add `authenticationRequired` to the `McpServer` selection in `workspaceMcpServers.graphql` and `embeddedMcpServers.graphql` wherever `enforceToolAuthorization` is selected.

- [ ] **Step 2: Regenerate types**

Run: `cd client && npx graphql-codegen`
Expected: `graphql.ts` / `graphql-types.ts` now include `authenticationRequired`.

- [ ] **Step 3: Update `McpServerDialog.tsx` (automation)**

Add `authenticationRequired: z.boolean()` to `formSchema` (alphabetical order — before `enabled`). Add to `defaultValues`:

```tsx
            authenticationRequired: mcpServer?.authenticationRequired ?? true,
```

In `onSubmit`'s update `input`, add `authenticationRequired: values.authenticationRequired,` (keep keys sorted). Add a new `FormField` (only rendered when editing, i.e. inside the existing `{mcpServer && (...)}` region) for `authenticationRequired`, mirroring the `enforceToolAuthorization` checkbox block. Couple the two: when `authenticationRequired` is `false`, disable the `enforceToolAuthorization` checkbox and force its value off. Read the auth value with `const authenticationRequired = form.watch('authenticationRequired');` (placed with the other derived values), then:

```tsx
                                            <Checkbox
                                                checked={field.value}
                                                disabled={!authenticationRequired}
                                                onCheckedChange={field.onChange}
                                            />
```

and in an effect (placed last, before `return`):

```tsx
    useEffect(() => {
        if (!authenticationRequired) {
            form.setValue('enforceToolAuthorization', false);
        }
    }, [authenticationRequired, form]);
```

Add a `FormDescription` for the auth toggle, e.g. "Require an API key or OAuth token in addition to the server URL. Existing servers default to off." Import `useEffect` from `react`.

- [ ] **Step 4: Mirror in the embedded dialog**

Apply the identical changes to `client/src/ee/pages/embedded/mcp-servers/components/McpServerDialog.tsx`.

- [ ] **Step 5: Run client checks**

Run: `cd client && npm run check`
Expected: lint, typecheck, tests all pass. Fix any `sort-keys` / import-order findings manually.

- [ ] **Step 6: Commit (client convention)**

```bash
git add client/src/graphql/ client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts \
        client/src/pages/automation/mcp-servers/components/McpServerDialog.tsx \
        client/src/ee/pages/embedded/mcp-servers/components/McpServerDialog.tsx
git commit -m "client - Add authenticationRequired toggle to MCP server dialogs"
```

---

## Phase 3 — Shared enforcement + automation

### Task 5: Anonymous MCP authentication token

**Files:**
- Create: `server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/mcp/McpAnonymousAuthenticationToken.java`
- Test: `server/libs/platform/platform-security-web/platform-security-web-api/src/test/java/com/bytechef/platform/security/web/mcp/McpAnonymousAuthenticationTokenTest.java`

**Interfaces:**
- Produces: `McpAnonymousAuthenticationToken` — an `Authentication` with `isAuthenticated()==true`, `getAuthorities()` empty, and principal = the MCP server secret key. Must **not** extend `AnonymousAuthenticationToken` (Spring's `.authenticated()` rejects that class), so it satisfies the global `/api/** → authenticated()` rule.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.security.web.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

class McpAnonymousAuthenticationTokenTest {

    @Test
    void testTokenIsAuthenticatedWithoutAuthorities() {
        McpAnonymousAuthenticationToken token = new McpAnonymousAuthenticationToken("server-secret");

        assertThat(token.isAuthenticated()).isTrue();
        assertThat(token.getAuthorities()).isEmpty();
        assertThat(token.getName()).isEqualTo("server-secret");
        assertThat(token).isNotInstanceOf(AnonymousAuthenticationToken.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-security-web:platform-security-web-api:test --tests "*McpAnonymousAuthenticationTokenTest"`
Expected: compile failure (class missing).

- [ ] **Step 3: Implement the token**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ... (standard Apache header) ...
 */

package com.bytechef.platform.security.web.mcp;

import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * A successful authentication for an MCP server that does not require a credential. It is authenticated with no granted
 * authorities, so it satisfies the {@code /api/** -> authenticated()} rule while granting no tool authorities. It
 * intentionally does not extend {@link org.springframework.security.authentication.AnonymousAuthenticationToken},
 * which Spring's {@code authenticated()} authorization manager rejects.
 *
 * @author Ivica Cardic
 */
public class McpAnonymousAuthenticationToken extends AbstractAuthenticationToken {

    private final String mcpServerSecretKey;

    public McpAnonymousAuthenticationToken(String mcpServerSecretKey) {
        super(List.of());

        this.mcpServerSecretKey = mcpServerSecretKey;

        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return mcpServerSecretKey;
    }

    @Override
    public String getName() {
        return mcpServerSecretKey;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-security-web:platform-security-web-api:test --tests "*McpAnonymousAuthenticationTokenTest"`
Expected: PASS.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/mcp/McpAnonymousAuthenticationToken.java \
        server/libs/platform/platform-security-web/platform-security-web-api/src/test/java/com/bytechef/platform/security/web/mcp/McpAnonymousAuthenticationTokenTest.java
git commit -m "Add McpAnonymousAuthenticationToken for optional MCP authentication"
```

### Task 6: Make the shared converter permissive on a missing token

**Files:**
- Modify: `server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/mcp/McpApiKeyAuthenticationConverter.java`
- Test: `server/libs/platform/platform-security-web/platform-security-web-api/src/test/java/com/bytechef/platform/security/web/mcp/McpApiKeyAuthenticationConverterTest.java`

**Interfaces:**
- Consumes: `McpApiKeyCredentials(Environment, mcpServerSecretKey, secretKey)` — `secretKey` may now be `null` (no token presented).
- Produces: `convert(request)` returns an unauthenticated `ApiKeyAuthenticationToken` even when no `Bearer` header is present (with `secretKey == null`); it no longer throws for a missing token. It still parses the path secret and environment.

- [ ] **Step 1: Update the converter test**

Find the existing test asserting a missing-`Authorization` request throws `BadCredentialsException` and change it to assert a token with a `null` secret is produced. Add:

```java
@Test
void testConvertWithoutAuthorizationHeaderProducesNullSecretToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    request.setServletPath("/api/automation/server-secret/mcp");

    ApiKeyAuthenticationToken token = (ApiKeyAuthenticationToken) converter.convert(request);

    McpApiKeyCredentials credentials = (McpApiKeyCredentials) token.getCredentials();

    assertThat(credentials.getMcpServerSecretKey()).isEqualTo("server-secret");
    assertThat(credentials.getSecret()).isNull();
}
```

Keep any test asserting a **malformed** (non-`Bearer`) header behavior consistent with the new rule: a non-`Bearer` `Authorization` is treated as "no token" (null secret), not a throw. Adjust or remove the old throwing assertion.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-security-web:platform-security-web-api:test --tests "*McpApiKeyAuthenticationConverterTest"`
Expected: FAIL.

- [ ] **Step 3: Update `convert`**

Replace the missing-token guard so it produces a null-secret token instead of throwing:

```java
    @Override
    public Authentication convert(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER_NAME);

        String secretKey = null;

        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            secretKey = authorization.substring(BEARER_PREFIX.length());
        }

        String servletPath = request.getServletPath();

        String mcpServerSecretKey = servletPath.replace(pathPrefix, "")
            .replace("/mcp", "")
            .replace("/sse", "")
            .replace("/message", "");

        return ApiKeyAuthenticationToken.unauthenticated(
            new McpApiKeyCredentials(getEnvironment(request), mcpServerSecretKey, secretKey));
    }
```

Update the class Javadoc: the Bearer token is no longer mandatory at the converter; the provider decides based on the target server's `authenticationRequired`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-security-web:platform-security-web-api:test --tests "*McpApiKeyAuthenticationConverterTest"`
Expected: PASS.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/mcp/McpApiKeyAuthenticationConverter.java \
        server/libs/platform/platform-security-web/platform-security-web-api/src/test/java/com/bytechef/platform/security/web/mcp/McpApiKeyAuthenticationConverterTest.java
git commit -m "Let the MCP api key converter tolerate a missing token"
```

### Task 7: Automation provider short-circuits when auth is not required

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/security/web/authentication/AutomationMcpServerApiKeyAuthenticationProvider.java`
- Test: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/test/java/com/bytechef/automation/ai/mcp/server/security/web/AutomationMcpServerSecurityIntTest.java`

**Interfaces:**
- Consumes: `McpAnonymousAuthenticationToken` (Task 5); `McpApiKeyCredentials.getSecret()` possibly `null` (Task 6); `McpServer.isAuthenticationRequired()` (Task 1).
- Produces: provider that returns `McpAnonymousAuthenticationToken` when the resolved server has `authenticationRequired=false` (ignoring any token), and otherwise enforces the existing key/type/environment checks (401 when no token).

- [ ] **Step 1: Update the int test**

In `AutomationMcpServerSecurityIntTest`, make `mockMcpServer` stub the flag as authentication-enforcing by default:

```java
    private void mockMcpServer(Environment environment) {
        mockMcpServer(environment, true);
    }

    private void mockMcpServer(Environment environment, boolean authenticationRequired) {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getEnvironment()).thenReturn(environment);
        when(mcpServer.isAuthenticationRequired()).thenReturn(authenticationRequired);
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
    }
```

Because the provider now resolves the server *first*, the two rejection tests that don't currently stub the server must stub it as auth-required. In `testInitializeWithoutBearerTokenReturnsUnauthorizedWithEmptyBody` and `testInitializeWithWrongTypeApiKeyIsRejected`, add `mockMcpServer(ENVIRONMENT);` after the `seedApiKey(...)` line. (The wrong-path test keeps its existing `getMcpServer("wrong-server-secret")` throw stub, which still yields 401.)

Add two new tests:

```java
@Test
void testInitializeWithoutBearerTokenWhenAuthenticationNotRequiredSucceeds() {
    mockMcpServer(ENVIRONMENT, false);

    try (McpSyncClient mcpSyncClient = createMcpSyncClient(MCP_SERVER_SECRET_KEY, null, null)) {
        McpSchema.InitializeResult initializeResult = mcpSyncClient.initialize();

        assertThat(initializeResult).isNotNull();
        assertThat(initializeResult.serverInfo()
            .name()).isEqualTo("automation-mcp-server");

        McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

        assertThat(listToolsResult).isNotNull();
        assertThat(listToolsResult.tools()).isEmpty();
    }
}

@Test
void testTokenIgnoredWhenAuthenticationNotRequired() throws Exception {
    String secretKey = seedApiKey(PlatformType.EMBEDDED, ENVIRONMENT);

    mockMcpServer(ENVIRONMENT, false);

    HttpResponse<String> httpResponse = postInitialize(MCP_SERVER_SECRET_KEY, secretKey, null);

    assertThat(httpResponse.statusCode()).isEqualTo(200);
}
```

(The second test proves a mismatched/otherwise-invalid key is *ignored* — not rejected — when the server opts out, matching the "ignore tokens entirely" decision.)

- [ ] **Step 2: Run tests to verify the new/updated ones fail**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:testIntegration --tests "*AutomationMcpServerSecurityIntTest"`
(If the module runs these under `test`, use `:test`.) Expected: the two new tests FAIL (currently token-less → 401).

- [ ] **Step 3: Update the provider**

Rewrite `authenticate` so it resolves the server first and short-circuits:

```java
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken apiKeyAuthenticationToken = (ApiKeyAuthenticationToken) authentication;

        McpApiKeyCredentials mcpApiKeyCredentials = (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();

        McpServer mcpServer = getMcpServer(mcpApiKeyCredentials.getMcpServerSecretKey());

        if (!mcpServer.isAuthenticationRequired()) {
            return new McpAnonymousAuthenticationToken(mcpApiKeyCredentials.getMcpServerSecretKey());
        }

        if (mcpApiKeyCredentials.getSecret() == null) {
            throw new BadCredentialsException("Authorization token does not exist");
        }

        Authentication authenticatedAuthentication = apiKeyAuthenticationProvider.authenticate(authentication);

        if (authenticatedAuthentication == null) {
            return null;
        }

        McpApiKeyEntity mcpApiKeyEntity = (McpApiKeyEntity) authenticatedAuthentication.getPrincipal();

        if (mcpApiKeyEntity.getType() != PlatformType.AUTOMATION) {
            throw new BadCredentialsException("Invalid API key");
        }

        if (mcpServer.getEnvironment() != mcpApiKeyEntity.getEnvironment()) {
            throw new BadCredentialsException("Invalid API key");
        }

        apiKeyService.updateLastUsedDate(mcpApiKeyEntity.getApiKeyId());

        return authenticatedAuthentication;
    }
```

Add the import for `com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken`. Note the environment check now uses the already-resolved `mcpServer` (the old code re-fetched it), so `getMcpServer` is called once.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:testIntegration --tests "*AutomationMcpServerSecurityIntTest"`
Expected: PASS (all: valid key, SSE, no-token-required 401, wrong-type 401, env-mismatch 401, wrong-path 401, no-token-not-required 200, token-ignored 200).

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-ai/automation-ai-mcp-server/
git commit -m "Short-circuit automation MCP auth when the server does not require authentication"
```

---

## Phase 4 — Management server

### Task 8: Store `authenticationRequired` in the management `Property`

**Files:**
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server-configuration/ai-mcp-server-configuration-graphql/src/main/java/com/bytechef/ai/mcp/server/configuration/service/ManagementMcpServerService.java`
- Modify: `.../ManagementMcpServerServiceImpl.java`
- Test: create `.../ai-mcp-server-configuration-graphql/src/test/java/com/bytechef/ai/mcp/server/configuration/service/ManagementMcpServerServiceIntTest.java` (or a unit test with a mocked `PropertyService`).

**Interfaces:**
- Produces:
  - `ManagementMcpServerService.isAuthenticationRequired(): boolean` — reads `authenticationRequired` from the `mcp.server` property; a missing key returns `false`.
  - `ManagementMcpServerService.updateAuthenticationRequired(boolean): boolean` — writes the flag, preserving the existing `secretKey`.
  - `getManagementMcpServerUrl()` writes `authenticationRequired=true` when it mints a brand-new secret; `updateManagementMcpServerUrl()` preserves the current flag (defaulting `true` when none exists yet).

- [ ] **Step 1: Write the failing test** (unit test with a mocked `PropertyService`)

```java
@Test
void testAuthenticationRequiredDefaultsFalseWhenKeyMissing() {
    when(propertyService.fetchProperty("mcp.server", Property.Scope.PLATFORM, null))
        .thenReturn(Optional.of(new Property(Map.of("secretKey", "abc"))));

    assertThat(managementMcpServerService.isAuthenticationRequired()).isFalse();
}

@Test
void testUpdateAuthenticationRequiredPreservesSecretKey() {
    when(propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null))
        .thenReturn(new Property(Map.of("secretKey", "abc")));

    managementMcpServerService.updateAuthenticationRequired(true);

    verify(propertyService).save(
        eq("mcp.server"),
        argThat(map -> Objects.equals(map.get("secretKey"), "abc")
            && Boolean.TRUE.equals(map.get("authenticationRequired"))),
        eq(Property.Scope.PLATFORM), isNull());
}
```

Adjust construction of `Property` to whatever its test constructor/fixture is in this module (check `Property`'s API — it exposes `get(String)`; use the same shape the existing code reads).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server-configuration:ai-mcp-server-configuration-graphql:test --tests "*ManagementMcpServerService*"`
Expected: compile failure (methods missing).

- [ ] **Step 3: Add interface methods**

In `ManagementMcpServerService`:

```java
    boolean isAuthenticationRequired();

    boolean updateAuthenticationRequired(boolean authenticationRequired);
```

- [ ] **Step 4: Implement in `ManagementMcpServerServiceImpl`**

Add (keep the `@PreAuthorize("isTenantAdmin()")` gate consistent with the other secret-adjacent operations — reading the flag can stay ungated if the URL getter is the sensitive one; gate the update):

```java
    @Override
    public boolean isAuthenticationRequired() {
        return propertyService.fetchProperty(MCP_SERVER_PROPERTY_KEY, Property.Scope.PLATFORM, null)
            .map(property -> Boolean.TRUE.equals(property.get("authenticationRequired")))
            .orElse(false);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public boolean updateAuthenticationRequired(boolean authenticationRequired) {
        Optional<Property> propertyOptional = propertyService.fetchProperty(
            MCP_SERVER_PROPERTY_KEY, Property.Scope.PLATFORM, null);

        String secretKey = propertyOptional
            .map(property -> (String) property.get("secretKey"))
            .orElseGet(() -> String.valueOf(TenantKey.of()));

        propertyService.save(
            MCP_SERVER_PROPERTY_KEY,
            Map.of("secretKey", secretKey, "authenticationRequired", authenticationRequired),
            Property.Scope.PLATFORM, null);

        return authenticationRequired;
    }
```

Update the two existing methods to persist the flag when they save:
- `getManagementMcpServerUrl()` — in the `else` branch that mints a new secret, save `Map.of("secretKey", secretKey, "authenticationRequired", true)`.
- `updateManagementMcpServerUrl()` — read the current flag (default `true` if none) and include it in the saved map so rotating the URL preserves it.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server-configuration:ai-mcp-server-configuration-graphql:test --tests "*ManagementMcpServerService*"`
Expected: PASS.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/ai/ai-mcp/ai-mcp-server-configuration/
git commit -m "Store authenticationRequired in the management MCP server property"
```

### Task 9: Management provider short-circuits when auth is not required

**Files:**
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/security/web/authentication/ManagementMcpServerApiKeyAuthenticationProvider.java`
- Modify: `.../ManagementMcpServerSecurityConfigurer.java` — no signature change expected; it already receives `PropertyService`.
- Test: `server/libs/ai/ai-mcp/ai-mcp-server/src/test/java/com/bytechef/ai/mcp/server/security/web/ManagementMcpServerSecurityIntTest.java`

**Interfaces:**
- Consumes: `McpAnonymousAuthenticationToken`; `McpApiKeyCredentials.getSecret()` (nullable); the `mcp.server` property's `authenticationRequired`.
- Produces: management provider returns `McpAnonymousAuthenticationToken` when the property's `authenticationRequired` is false/absent; otherwise enforces the existing admin-key + secret-match + environment checks.

- [ ] **Step 1: Update the int test**

Mirror the automation approach in `ManagementMcpServerSecurityIntTest`: the property fixture must set `authenticationRequired=true` for the existing rejection tests (so token-less → 401), and add:
- a test where the property has `authenticationRequired=false` (or the key absent) and a token-less request initializes + lists tools (200);
- a test where a token is presented but ignored when not required.

Match the fixture style already used in that file (it seeds the `mcp.server` property). Where the fixture builds the property map, add the flag.

- [ ] **Step 2: Run tests to verify the new ones fail**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:testIntegration --tests "*ManagementMcpServerSecurityIntTest"`
Expected: new tests FAIL.

- [ ] **Step 3: Update the provider**

```java
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationToken apiKeyAuthenticationToken = (ApiKeyAuthenticationToken) authentication;

        McpApiKeyCredentials mcpApiKeyCredentials = (McpApiKeyCredentials) apiKeyAuthenticationToken.getCredentials();

        Property property = propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null);

        if (!Objects.equals(property.get("secretKey"), mcpApiKeyCredentials.getMcpServerSecretKey())) {
            throw new BadCredentialsException("Invalid MCP server secret key");
        }

        if (!Boolean.TRUE.equals(property.get("authenticationRequired"))) {
            return new McpAnonymousAuthenticationToken(mcpApiKeyCredentials.getMcpServerSecretKey());
        }

        if (mcpApiKeyCredentials.getSecret() == null) {
            throw new BadCredentialsException("Authorization token does not exist");
        }

        Authentication authenticatedAuthentication = apiKeyAuthenticationProvider.authenticate(authentication);

        if (authenticatedAuthentication == null) {
            return null;
        }

        McpApiKeyEntity mcpApiKeyEntity = (McpApiKeyEntity) authenticatedAuthentication.getPrincipal();

        if (mcpApiKeyEntity.getType() != null) {
            throw new BadCredentialsException("Invalid API key");
        }

        if (mcpApiKeyEntity.getEnvironment() != mcpApiKeyCredentials.getEnvironment()) {
            throw new BadCredentialsException("Invalid API key");
        }

        apiKeyService.updateLastUsedDate(mcpApiKeyEntity.getApiKeyId());

        return authenticatedAuthentication;
    }
```

We validate the path secret against the configured server secret *before* the short-circuit so a wrong path secret still 401s even on an unauthenticated server (the URL secret remains the baseline protection). Add the `McpAnonymousAuthenticationToken` import.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:testIntegration --tests "*ManagementMcpServerSecurityIntTest"`
Expected: PASS.

- [ ] **Step 5: Format and commit**

```bash
./gradlew spotlessApply
git add server/libs/ai/ai-mcp/ai-mcp-server/
git commit -m "Short-circuit management MCP auth when the server does not require authentication"
```

### Task 10: Management GraphQL + settings UI

**Files:**
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server-configuration/ai-mcp-server-configuration-graphql/src/main/resources/graphql/management-mcp-server.graphqls`
- Modify: `.../web/graphql/ManagementMcpServerGraphQlController.java`
- Modify: `client/src/graphql/platform/configuration/*` (add a query for the flag + a mutation) and `client/src/pages/settings/platform/mcp-server/McpServer.tsx`
- Regenerate: `client/src/shared/middleware/graphql.ts` / `graphql-types.ts`

**Interfaces:**
- Consumes: `ManagementMcpServerService.isAuthenticationRequired()/updateAuthenticationRequired(boolean)` (Task 8).
- Produces: `managementMcpServerAuthenticationRequired: Boolean!` query and `updateManagementMcpServerAuthenticationRequired(authenticationRequired: Boolean!): Boolean!` mutation.

- [ ] **Step 1: Extend the schema**

```graphql
extend type Query {
  managementMcpServerUrl: String
  managementMcpServerAuthenticationRequired: Boolean!
}

extend type Mutation {
  updateManagementMcpServerUrl: String!
  updateManagementMcpServerAuthenticationRequired(authenticationRequired: Boolean!): Boolean!
}
```

- [ ] **Step 2: Wire the controller**

```java
    @QueryMapping
    boolean managementMcpServerAuthenticationRequired() {
        return managementMcpServerService.isAuthenticationRequired();
    }

    @MutationMapping
    boolean updateManagementMcpServerAuthenticationRequired(@Argument boolean authenticationRequired) {
        return managementMcpServerService.updateAuthenticationRequired(authenticationRequired);
    }
```

Add the `org.springframework.graphql.data.method.annotation.Argument` import.

- [ ] **Step 3: Add client operations + regen**

Create `client/src/graphql/platform/configuration/managementMcpServerAuthenticationRequired.graphql` (query) and `updateManagementMcpServerAuthenticationRequired.graphql` (mutation). Run `cd client && npx graphql-codegen`.

- [ ] **Step 4: Add the toggle to `McpServer.tsx`**

Add a checkbox/switch bound to the query value and the mutation, following the existing patterns in that settings component. The management server has no `enforceToolAuthorization` UI, so no coupling logic is needed here (management is a single per-tenant server; the tool-authorization invariant only applies to `mcp_server` rows). If a future tool-authorization toggle is added to management, apply the same coupling.

- [ ] **Step 5: Run checks**

Run: `cd client && npm run check`
Expected: PASS.

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server-configuration:ai-mcp-server-configuration-graphql:test`
Expected: PASS.

- [ ] **Step 6: Commit (split server/client)**

```bash
./gradlew spotlessApply
git add server/libs/ai/ai-mcp/ai-mcp-server-configuration/
git commit -m "Expose management MCP authenticationRequired over GraphQL"
git add client/src/graphql/platform/configuration/ client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts client/src/pages/settings/platform/mcp-server/McpServer.tsx
git commit -m "client - Add authenticationRequired toggle to management MCP server settings"
```

---

## Phase 5 — Embedded (risk-flagged)

> Per the design spec §5: embedded is the highest-effort surface (signing-key + OAuth2 + the RFC 9728 discovery challenge) and carries an open semantic question — an anonymous embedded caller has no connected-user identity. Resolve the spike before committing to the full implementation. Acceptable fallback: embedded keeps authentication mandatory and its client toggle is disabled + labeled, documented as a known limitation.

### Task 11: Spike — does an anonymous embedded caller have enough context?

**Files (read-only investigation):**
- `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/src/main/java/com/bytechef/ee/embedded/ai/mcp/server/config/EmbeddedMcpServerConfiguration.java` (its `contextExtractor` + tool filter — what identity does it read?)
- `.../security/web/authentication/EmbeddedMcpServerApiKeyAuthenticationProvider.java` and `EmbeddedMcpServerOAuth2AuthenticationProvider.java` (what connected-user do they establish, and does the tool filter depend on it?)

- [ ] **Step 1: Determine** whether embedded tool listing needs a connected user, or whether the URL path secret alone resolves the integration-instance/tool context. Write findings (2-3 sentences) into the plan or the spec.

- [ ] **Step 2: Decide** one of:
  - (a) **Full support** — the URL secret is sufficient; proceed to Task 12.
  - (b) **Fallback** — embedded keeps mandatory auth; in `client/src/ee/pages/embedded/mcp-servers/components/McpServerDialog.tsx`, disable the `authenticationRequired` toggle for embedded with a tooltip ("Authentication is always required for embedded MCP servers"), and note the limitation in the spec's §5. Skip Task 12.

Record the decision explicitly before proceeding.

### Task 12: Embedded enforcement (only if Task 11 chose full support)

**Files:**
- Modify: `.../security/web/configurer/EmbeddedMcpServerApiKeyAuthenticationConverter.java` — tolerate a missing token (mirror Task 6).
- Modify: `.../security/web/authentication/EmbeddedMcpServerApiKeyAuthenticationProvider.java` and `EmbeddedMcpServerOAuth2AuthenticationProvider.java` — short-circuit to `McpAnonymousAuthenticationToken` when the resolved `McpServer.isAuthenticationRequired()` is false (both providers must agree; resolve the server via `McpServerService` by the path secret / connected-user server lookup used there).
- Modify: `.../security/web/configurer/EmbeddedMcpServerSecurityConfigurer.java` — make `McpDiscoveryAuthenticationFilter` skip the challenge when the resolved server opts out (e.g. wrap/condition the entry point on `authenticationRequired`), so a token-less request to an opted-out server is not answered with the discovery 401.
- Test: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/src/test/java/com/bytechef/ee/embedded/ai/mcp/server/security/web/EmbeddedMcpServerOAuth2SecurityIntTest.java` (+ a signing-key variant if present).

**Interfaces:**
- Consumes: `McpAnonymousAuthenticationToken`; `McpServer.isAuthenticationRequired()`.
- Produces: embedded server that, when `authenticationRequired=false`, serves token-less requests anonymously across all three interception points (signing-key filter, OAuth2 filter, discovery filter).

- [ ] **Step 1: Write failing int tests** — token-less request to an `authenticationRequired=false` embedded server: no discovery 401, initialize + listTools succeeds; `authenticationRequired=true` server: unchanged (discovery challenge / 401). Mirror the fixture style already in the embedded security int test (all `server/ee/` files need the EE license header + `@version ee`).

- [ ] **Step 2: Run to verify they fail.**

Run: `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-server:testIntegration --tests "*EmbeddedMcp*SecurityIntTest"`
Expected: FAIL.

- [ ] **Step 3: Implement** the converter tolerance, both providers' short-circuit, and the discovery-filter condition (EE header + `@version ee` on any new file).

- [ ] **Step 4: Run to verify they pass.**

Run: `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-server:testIntegration --tests "*EmbeddedMcp*SecurityIntTest"`
Expected: PASS.

- [ ] **Step 5: Enable the embedded client toggle** (it was already added in Task 4; if Task 11 chose fallback this task is skipped, so no change needed here).

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/
git commit -m "Short-circuit embedded MCP auth when the server does not require authentication"
```

---

## Final verification

- [ ] `./gradlew spotlessApply && ./gradlew check` (or the affected-module `:test`/`:testIntegration` tasks) — all green.
- [ ] `cd client && npm run check` — all green.
- [ ] Manual smoke (optional, via `/run` or a local server): create a new automation MCP server → `authenticationRequired` defaults on; token-less `tools/list` → 401. Toggle it off → token-less `tools/list` → works. Confirm the enforce-tool-authorization toggle is disabled while auth is off.

## Self-review notes (author)

- **Spec coverage:** §1 defaults → Task 1 (+ Task 8 for management); §2 mechanism → Tasks 5-7, 9, 12; §3 invariant → Task 2 (server) + Task 4 (client); §4 GraphQL/client → Tasks 3-4, 10; §5 embedded risk → Tasks 11-12. Covered.
- **Latent bug:** Task 2 also repairs the dropped `enforceToolAuthorization` copy on `update(McpServer)` — required because the invariant and the new flag both live on that path.
- **Type consistency:** `McpAnonymousAuthenticationToken(String)`, `McpServer.isAuthenticationRequired()`, `McpApiKeyCredentials.getSecret()` (nullable) used consistently across Tasks 5-9, 12.
- **Assumption to verify during execution:** the exact test-time API of `Property` (constructor / fixture) in the management module — the plan's mocks may need adjusting to the real `Property` shape.
