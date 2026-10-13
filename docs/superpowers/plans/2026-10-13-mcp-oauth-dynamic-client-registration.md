# MCP OAuth Dynamic Client Registration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a user connect the MCP Client component to a hosted MCP server by clicking Sign in, with ByteChef discovering the authorization server, registering itself as an OAuth client and running PKCE, all without a pre-created OAuth app.

**Architecture:** A new generic platform capability in `platform-oauth2` (discovery, RFC 7591 registration, AES-GCM sealed authorization context) is used by `ConnectionDefinitionServiceImpl` when an authorization declares `protectedResourceUrl`. The browser carries the sealed context across the redirect and posts it back; `ConnectionFacadeImpl` unseals it and exchanges the code. The PKCE verifier moves into the sealed context for every PKCE component, closing an existing leak. `mcp-client` adds an "OAuth2 (automatic)" authorization and translates MCP SDK 401s so token refresh can retry.

**Tech Stack:** Java 25 / Spring Boot 4 (`java.net.http.HttpClient`, AES-GCM `Encryption`), WireMock 3.13 for fake servers, React 19 + Vitest in `client/`, the `@bytechef/embedded` SDK, MCP Java SDK 2.0.0 (`mcp-core`).

**Spec:** `docs/superpowers/specs/2026-10-13-mcp-oauth-dynamic-client-registration-design.md`

## Global Constraints

- Discovery mode is `OAUTH2_AUTHORIZATION_CODE_PKCE` plus a `protectedResourceUrl` function; no new `AuthorizationType`.
- One registered client per connection, stored in the connection's encrypted parameters; a reconnect registers again.
- The sealed context is produced with `com.bytechef.encryption.Encryption` (AES-GCM); expiry 10 minutes for discovery contexts, 1 hour for plain-PKCE contexts; bound to tenant, authenticated principal and component.
- `code_verifier` must never appear in `extraQueryParameters` again (any PKCE component).
- Every server-side URL (resource, metadata, registration, token) passes `UrlValidatorUtils.validate(url, allowedHosts)`; redirects are not followed; 10 s timeouts; 64 KB metadata cap; `https` required unless the host is allowlisted or is `localhost`/`127.0.0.1` under the `dev` profile.
- Property `bytechef.oauth2.discovery.allowed-hosts`, default empty, declared in yml (`application-bytechef.yml` and `server/ee/apps/config-server-app/src/main/resources/config/apps/application.yml`), never as a Java initializer.
- Registration sends no `scope`; `token_endpoint_auth_method` from the registration response wins over the requested one.
- Discovery mode is not offered through the embedded (connected-user) flow in this iteration.
- `ConnectionDialog` stays component-agnostic (no `mcpClient` branches).
- Shared `ConnectionDefinitionServiceImpl` runs in `worker-app` and `runtime-job-app`, which do not ship `platform-oauth2-service`: inject the new services there with `ObjectProvider` and fail with a clear `ConfigurationException` only when discovery is actually requested.
- Repo conventions: blank line before control statements, descriptive names, one test class per source class (`<Class>Test` / `<Class>IntTest`, no `Impl` in test names), test method names camelCase without underscores, `./gradlew spotlessApply` before every commit, subject-only commit messages (`<description>`; the user may prefix a ticket number), no attribution trailers, `EncodingUtils` for Base64, inject `JsonUtils` rather than `new JsonMapper()`.

## Review Focus

Inputs the spec implies but no task's tests exercised before this list was written; each now has a pinned test in the owning task.

1. A protected resource whose `WWW-Authenticate` header carries `resource_metadata` **and** whose `.well-known` fallback also exists but points elsewhere — the header must win (Task 4, `testDiscoverPrefersResourceMetadataFromWwwAuthenticateHeader`).
2. Registration response that returns `token_endpoint_auth_method: client_secret_basic` although `none` was requested — the token exchange must send HTTP Basic, not a form secret (Task 5 `testRegisterUsesAuthMethodFromResponse`, Task 7 `testDefaultCallbackUsesBasicAuthenticationForClientSecretBasic`).
3. A user who opens the OAuth step, waits 11 minutes, then signs in — the discovery context has expired and the dialog must show "Sign-in took too long" rather than a generic exchange failure (Task 6 `testUnsealRejectsExpiredDiscoveryContext`, Task 8 `testCreateRejectsExpiredAuthorizationContext`).
4. A token response without `refresh_token` and with `expires_in` as a string — persistence must not throw and the refresh scheduler must simply not schedule (Task 8 `testCreateStoresTokensWhenRefreshTokenIsMissing`).
5. An MCP SDK 401 wrapped inside a Reactor `RuntimeException` cause chain — the translation must still surface a `ProviderException(401)` (Task 13 `testCreateMcpSyncClientTranslatesWrappedAuthorizationError`).

## File Structure

**Create**

- `server/libs/platform/platform-oauth2/platform-oauth2-api/src/main/java/com/bytechef/platform/oauth2/domain/DiscoveredAuthorizationServer.java` — record: what discovery learned.
- `.../platform-oauth2-api/.../domain/RegisteredClient.java` — record: result of RFC 7591 registration.
- `.../platform-oauth2-api/.../domain/OAuth2AuthorizationContext.java` — record carried across the redirect.
- `.../platform-oauth2-api/.../service/OAuth2DiscoveryService.java` — interface: `discover`, `register`.
- `.../platform-oauth2-api/.../service/OAuth2AuthorizationContextService.java` — interface: `seal`, `unseal`.
- `.../platform-oauth2-api/.../exception/OAuth2ErrorType.java` — error keys for the dialog.
- `server/libs/platform/platform-oauth2/platform-oauth2-service/src/main/java/com/bytechef/platform/oauth2/service/OAuth2HttpClient.java` — validated, redirect-free, size-capped HTTP.
- `.../platform-oauth2-service/.../service/OAuth2DiscoveryClient.java` — RFC 9728 + RFC 8414 discovery.
- `.../platform-oauth2-service/.../service/OAuth2DynamicClientRegistrar.java` — RFC 7591 registration.
- `.../platform-oauth2-service/.../service/OAuth2DiscoveryServiceImpl.java` — composes the two.
- `.../platform-oauth2-service/.../service/OAuth2AuthorizationContextServiceImpl.java` — sealer.
- Tests beside each: `OAuth2HttpClientTest`, `OAuth2DiscoveryClientTest`, `OAuth2DynamicClientRegistrarTest`, `OAuth2DiscoveryServiceTest`, `OAuth2AuthorizationContextServiceTest`.
- `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/src/test/java/com/bytechef/platform/configuration/web/rest/OAuth2ApiControllerIntTest.java`.
- `client/src/shared/queries/platform/tests/oauth2.queries.test.ts`.
- `sdks/frontend/embedded/library/src/components/connect-dialog/useOAuth2.test.ts`.

**Modify**

- `sdks/backend/java/component-api/.../definition/Authorization.java` — constants, `ProtectedResourceUrlFunction`, `getProtectedResourceUrl()`.
- `sdks/backend/java/component-api/.../definition/ComponentDsl.java` — `ModifiableAuthorization.protectedResourceUrl(...)`.
- `server/libs/platform/platform-component/platform-component-api/.../domain/Authorization.java` — `discovery` flag.
- `.../platform-component-api/.../domain/OAuth2AuthorizationParameters.java` — `authorizationContext`.
- `server/libs/config/app-config/.../ApplicationProperties.java` — `Oauth2.Discovery.allowedHosts`.
- `server/apps/server-app/src/main/resources/config/application-bytechef.yml`, `server/ee/apps/config-server-app/src/main/resources/config/apps/application.yml` — discovery default.
- `.../platform-oauth2-api/build.gradle.kts`, `.../platform-oauth2-service/build.gradle.kts`, `.../platform-component-service/build.gradle.kts` — dependencies.
- `server/libs/platform/platform-component/platform-component-service/.../service/ConnectionDefinitionServiceImpl.java` — PKCE/discovery parameters, default callback and refresh.
- `.../platform-component-service/src/test/.../service/ConnectionDefinitionServiceTest.java` — real tests replace the `@Disabled` stubs.
- `server/libs/platform/platform-connection/platform-connection-service/.../facade/ConnectionFacadeImpl.java` — unseal, exchange, strip.
- `.../platform-connection-service/src/test/.../config/ConnectionIntTestConfigurationSharedMocks.java`, `.../facade/ConnectionFacadeIntTest.java`.
- `.../platform-configuration-rest-impl/openapi.yaml` (+ regenerated Spring model and TS client).
- `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml`, `.../mapper/ConnectedUserIntegrationMapper.java`.
- `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/.../facade/ConnectedUserIntegrationFacadeImpl.java` — skip discovery authorizations.
- `client/src/shared/components/connection/oauth2/useOAuth2.ts`, `OAuth2Button.tsx`, `ConnectionDialog.tsx`, `client/src/shared/queries/platform/oauth2.queries.ts` and their tests.
- `sdks/frontend/embedded/library/src/components/connect-dialog/types.ts`, `useOAuth2.ts`, `package.json`.
- `server/libs/modules/components/mcp-client/.../connection/McpClientConnection.java`, `.../util/McpClientUtils.java`, their tests, `src/test/resources/definition/mcp-client_v1.json`, `docs/content/docs/reference/components/mcp-client_v1.mdx`.
- `docs/superpowers/specs/2026-10-13-mcp-oauth-dynamic-client-registration-design.md` — one sentence (Task 12).

---

### Task 1: Component API — `protectedResourceUrl`, new constants, `authorizationContext`

**Files:**
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/Authorization.java`
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java` (class `ModifiableAuthorization`, ~line 900)
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/Authorization.java`
- Modify: `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/domain/OAuth2AuthorizationParameters.java`
- Test: `server/libs/platform/platform-component/platform-component-api/src/test/java/com/bytechef/platform/component/domain/AuthorizationTest.java` (create if absent, otherwise add the methods)

**Interfaces:**
- Produces: `Authorization.ProtectedResourceUrlFunction { String apply(Parameters connectionParameters, Context context) throws Exception; }`, `Optional<ProtectedResourceUrlFunction> Authorization.getProtectedResourceUrl()`, DSL `ModifiableAuthorization.protectedResourceUrl(ProtectedResourceUrlFunction)`.
- Produces constants on `com.bytechef.component.definition.Authorization`: `AUTHORIZATION_CONTEXT = "authorizationContext"`, `CODE_VERIFIER = "code_verifier"`, `RESOURCE = "resource"`, `TOKEN_ENDPOINT_AUTH_METHOD = "tokenEndpointAuthMethod"`, `TOKEN_ENDPOINT_AUTH_METHOD_BASIC = "client_secret_basic"`, `TOKEN_ENDPOINT_AUTH_METHOD_NONE = "none"`, `TOKEN_ENDPOINT_AUTH_METHOD_POST = "client_secret_post"`.
- Produces: domain `Authorization.isDiscovery()`; `OAuth2AuthorizationParameters(String authorizationUrl, String clientId, Map<String,String> extraQueryParameters, Map<String,Boolean> scopes, String authorizationContext)` plus the old 4-argument constructor delegating with `null`.

- [ ] **Step 1: Write the failing domain test**

```java
package com.bytechef.platform.component.domain;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuthorizationTest {

    @Test
    void testIsDiscoveryWhenProtectedResourceUrlIsDeclared() {
        Authorization authorization = new Authorization(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .protectedResourceUrl((connectionParameters, context) -> connectionParameters.getRequiredString("url")));

        assertThat(authorization.isDiscovery()).isTrue();
    }

    @Test
    void testIsNotDiscoveryWithoutProtectedResourceUrl() {
        Authorization authorization = new Authorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));

        assertThat(authorization.isDiscovery()).isFalse();
    }

    @Test
    void testOAuth2AuthorizationParametersFourArgumentConstructorLeavesContextNull() {
        OAuth2AuthorizationParameters parameters = new OAuth2AuthorizationParameters(
            "https://auth.example.com/authorize", "client", Map.of(), Map.of());

        assertThat(parameters.authorizationContext()).isNull();
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-api:test --tests '*AuthorizationTest' --offline -q`
Expected: compilation failure — `protectedResourceUrl`, `isDiscovery`, `authorizationContext` do not exist.

- [ ] **Step 3: Add the function, getter and constants to the component API**

In `sdks/.../definition/Authorization.java`, next to the other constants (lines 31–73):

```java
    String AUTHORIZATION_CONTEXT = "authorizationContext";
    String CODE_VERIFIER = "code_verifier";
    String RESOURCE = "resource";
    String TOKEN_ENDPOINT_AUTH_METHOD = "tokenEndpointAuthMethod";
    String TOKEN_ENDPOINT_AUTH_METHOD_BASIC = "client_secret_basic";
    String TOKEN_ENDPOINT_AUTH_METHOD_NONE = "none";
    String TOKEN_ENDPOINT_AUTH_METHOD_POST = "client_secret_post";
```

Next to `getPkce()` (line ~295):

```java
    /**
     * The URL of the OAuth2 protected resource (for example an MCP server). When present, the platform discovers
     * the authorization server (RFC 9728, RFC 8414) and registers a client (RFC 7591) instead of reading
     * authorizationUrl, tokenUrl, clientId and clientSecret from the connection parameters.
     */
    Optional<ProtectedResourceUrlFunction> getProtectedResourceUrl();
```

Next to `PkceFunction` (line ~466):

```java
    @FunctionalInterface
    interface ProtectedResourceUrlFunction {

        String apply(Parameters connectionParameters, Context context) throws Exception;
    }
```

In `ComponentDsl.ModifiableAuthorization`: add field `private ProtectedResourceUrlFunction protectedResourceUrlFunction;` after `pkceFunction`, builder after `pkce(...)`:

```java
        public ModifiableAuthorization protectedResourceUrl(ProtectedResourceUrlFunction protectedResourceUrl) {
            this.protectedResourceUrlFunction = Objects.requireNonNull(protectedResourceUrl);

            return this;
        }
```

and getter after `getPkce()`:

```java
        @Override
        public Optional<ProtectedResourceUrlFunction> getProtectedResourceUrl() {
            return Optional.ofNullable(protectedResourceUrlFunction);
        }
```

Import `ProtectedResourceUrlFunction` where `PkceFunction` is imported. Any other `implements Authorization` class in the repo (`git grep -ln "implements Authorization" -- '*.java'`) gets the same getter returning `Optional.empty()`.

- [ ] **Step 4: Add the domain flag and the record field**

Domain `Authorization` (platform-component-api): field `private boolean discovery;`, in the definition constructor `this.discovery = authorization.getProtectedResourceUrl().isPresent();`, getter `public boolean isDiscovery() { return discovery; }`, and include `discovery` in `equals`/`hashCode`.

`OAuth2AuthorizationParameters`:

```java
@SuppressFBWarnings("EI")
public record OAuth2AuthorizationParameters(
    String authorizationUrl, String clientId, Map<String, String> extraQueryParameters, Map<String, Boolean> scopes,
    String authorizationContext) {

    public OAuth2AuthorizationParameters(
        String authorizationUrl, String clientId, Map<String, String> extraQueryParameters,
        Map<String, Boolean> scopes) {

        this(authorizationUrl, clientId, extraQueryParameters, scopes, null);
    }
}
```

- [ ] **Step 5: Run the test and the two modules' checks**

Run: `./gradlew :sdks:backend:java:component-api:check :server:libs:platform:platform-component:platform-component-api:check --offline -q`
Expected: BUILD SUCCESSFUL; the three new tests pass.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply --offline -q
git add sdks/backend/java/component-api server/libs/platform/platform-component/platform-component-api
git commit -m "Add protectedResourceUrl authorization function and OAuth2 authorization context field"
```

---

### Task 2: `platform-oauth2-api` — records, interfaces, error type, config property

**Files:**
- Create: `server/libs/platform/platform-oauth2/platform-oauth2-api/src/main/java/com/bytechef/platform/oauth2/domain/DiscoveredAuthorizationServer.java`
- Create: `.../domain/RegisteredClient.java`
- Create: `.../domain/OAuth2AuthorizationContext.java`
- Create: `.../service/OAuth2DiscoveryService.java`
- Create: `.../service/OAuth2AuthorizationContextService.java`
- Create: `.../exception/OAuth2ErrorType.java`
- Modify: `server/libs/platform/platform-oauth2/platform-oauth2-api/build.gradle.kts`
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (class `Oauth2`, line ~3954)
- Modify: `server/apps/server-app/src/main/resources/config/application-bytechef.yml` (line ~122), `server/ee/apps/config-server-app/src/main/resources/config/apps/application.yml` (line ~179)
- Test: `server/libs/platform/platform-oauth2/platform-oauth2-api/src/test/java/com/bytechef/platform/oauth2/domain/OAuth2AuthorizationContextTest.java`

**Interfaces (Produces):**

```java
public record DiscoveredAuthorizationServer(
    String issuer, String authorizationEndpoint, String tokenEndpoint, String registrationEndpoint,
    String resource, List<String> scopes, List<String> codeChallengeMethods, List<String> tokenEndpointAuthMethods) {}

public record RegisteredClient(String clientId, String clientSecret, String tokenEndpointAuthMethod) {}

public record OAuth2AuthorizationContext(
    String componentName, int connectionVersion, String codeVerifier, String clientId, String clientSecret,
    String tokenEndpointAuthMethod, String authorizationUrl, String tokenUrl, String issuer, String resource) {
    boolean isDiscovery();                       // clientId != null
    Map<String, Object> toConnectionParameters(); // discovery fields keyed by Authorization.* constants, nulls skipped
    static OAuth2AuthorizationContext ofPkce(String componentName, int connectionVersion, String codeVerifier);
    static OAuth2AuthorizationContext ofDiscovery(String componentName, int connectionVersion, String codeVerifier,
        RegisteredClient registeredClient, DiscoveredAuthorizationServer authorizationServer);
}

public interface OAuth2DiscoveryService {
    DiscoveredAuthorizationServer discover(String protectedResourceUrl);
    RegisteredClient register(DiscoveredAuthorizationServer authorizationServer);
}

public interface OAuth2AuthorizationContextService {
    String seal(OAuth2AuthorizationContext authorizationContext);
    OAuth2AuthorizationContext unseal(String sealedAuthorizationContext, String componentName);
}

public class OAuth2ErrorType extends AbstractErrorType {
    URL_NOT_ALLOWED(100), METADATA_NOT_FOUND(101), INVALID_METADATA(102), REGISTRATION_NOT_SUPPORTED(103),
    REGISTRATION_REJECTED(104), INVALID_AUTHORIZATION_CONTEXT(105), DISCOVERY_NOT_AVAILABLE(106)
}
```

`ApplicationProperties.Oauth2.getDiscovery().getAllowedHosts()` → `List<String>`.

- [ ] **Step 1: Write the failing record test**

```java
package com.bytechef.platform.oauth2.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization;
import java.util.List;
import org.junit.jupiter.api.Test;

class OAuth2AuthorizationContextTest {

    private static final DiscoveredAuthorizationServer AUTHORIZATION_SERVER = new DiscoveredAuthorizationServer(
        "https://auth.example.com", "https://auth.example.com/authorize", "https://auth.example.com/token",
        "https://auth.example.com/register", "https://mcp.example.com/mcp", List.of("read"), List.of("S256"),
        List.of("none"));

    @Test
    void testOfPkceIsNotDiscoveryAndExposesNoConnectionParameters() {
        OAuth2AuthorizationContext authorizationContext = OAuth2AuthorizationContext.ofPkce("canva", 1, "verifier");

        assertThat(authorizationContext.isDiscovery()).isFalse();
        assertThat(authorizationContext.toConnectionParameters()).isEmpty();
    }

    @Test
    void testOfDiscoveryExposesRegisteredClientAndEndpointsAsConnectionParameters() {
        OAuth2AuthorizationContext authorizationContext = OAuth2AuthorizationContext.ofDiscovery(
            "mcpClient", 1, "verifier", new RegisteredClient("client-1", null, "none"), AUTHORIZATION_SERVER);

        assertThat(authorizationContext.isDiscovery()).isTrue();
        assertThat(authorizationContext.toConnectionParameters())
            .containsEntry(Authorization.CLIENT_ID, "client-1")
            .containsEntry(Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "none")
            .containsEntry(Authorization.AUTHORIZATION_URL, "https://auth.example.com/authorize")
            .containsEntry(Authorization.TOKEN_URL, "https://auth.example.com/token")
            .containsEntry(Authorization.RESOURCE, "https://mcp.example.com/mcp")
            .doesNotContainKey(Authorization.CLIENT_SECRET);
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:platform:platform-oauth2:platform-oauth2-api:test --offline -q`
Expected: compilation failure (classes missing).

- [ ] **Step 3: Create the records, interfaces and error type**

`DiscoveredAuthorizationServer.java` and `RegisteredClient.java` exactly as in Interfaces (plain records, package `com.bytechef.platform.oauth2.domain`, Apache header like `OAuth2Service.java`).

`OAuth2AuthorizationContext.java`:

```java
package com.bytechef.platform.oauth2.domain;

import com.bytechef.component.definition.Authorization;
import java.util.HashMap;
import java.util.Map;

public record OAuth2AuthorizationContext(
    String componentName, int connectionVersion, String codeVerifier, String clientId, String clientSecret,
    String tokenEndpointAuthMethod, String authorizationUrl, String tokenUrl, String issuer, String resource) {

    public static OAuth2AuthorizationContext ofPkce(String componentName, int connectionVersion, String codeVerifier) {
        return new OAuth2AuthorizationContext(
            componentName, connectionVersion, codeVerifier, null, null, null, null, null, null, null);
    }

    public static OAuth2AuthorizationContext ofDiscovery(
        String componentName, int connectionVersion, String codeVerifier, RegisteredClient registeredClient,
        DiscoveredAuthorizationServer authorizationServer) {

        return new OAuth2AuthorizationContext(
            componentName, connectionVersion, codeVerifier, registeredClient.clientId(),
            registeredClient.clientSecret(), registeredClient.tokenEndpointAuthMethod(),
            authorizationServer.authorizationEndpoint(), authorizationServer.tokenEndpoint(),
            authorizationServer.issuer(), authorizationServer.resource());
    }

    public boolean isDiscovery() {
        return clientId != null;
    }

    public Map<String, Object> toConnectionParameters() {
        Map<String, Object> connectionParameters = new HashMap<>();

        putIfPresent(connectionParameters, Authorization.CLIENT_ID, clientId);
        putIfPresent(connectionParameters, Authorization.CLIENT_SECRET, clientSecret);
        putIfPresent(connectionParameters, Authorization.TOKEN_ENDPOINT_AUTH_METHOD, tokenEndpointAuthMethod);
        putIfPresent(connectionParameters, Authorization.AUTHORIZATION_URL, authorizationUrl);
        putIfPresent(connectionParameters, Authorization.TOKEN_URL, tokenUrl);
        putIfPresent(connectionParameters, Authorization.RESOURCE, resource);

        return connectionParameters;
    }

    private static void putIfPresent(Map<String, Object> connectionParameters, String key, String value) {
        if (value != null) {
            connectionParameters.put(key, value);
        }
    }
}
```

`OAuth2DiscoveryService.java`, `OAuth2AuthorizationContextService.java` in `com.bytechef.platform.oauth2.service` as in Interfaces.

`OAuth2ErrorType.java` in `com.bytechef.platform.oauth2.exception`, modeled on `ConnectionDefinitionErrorType`:

```java
public class OAuth2ErrorType extends AbstractErrorType {

    public static final OAuth2ErrorType URL_NOT_ALLOWED = new OAuth2ErrorType(100);
    public static final OAuth2ErrorType METADATA_NOT_FOUND = new OAuth2ErrorType(101);
    public static final OAuth2ErrorType INVALID_METADATA = new OAuth2ErrorType(102);
    public static final OAuth2ErrorType REGISTRATION_NOT_SUPPORTED = new OAuth2ErrorType(103);
    public static final OAuth2ErrorType REGISTRATION_REJECTED = new OAuth2ErrorType(104);
    public static final OAuth2ErrorType INVALID_AUTHORIZATION_CONTEXT = new OAuth2ErrorType(105);
    public static final OAuth2ErrorType DISCOVERY_NOT_AVAILABLE = new OAuth2ErrorType(106);

    private OAuth2ErrorType(int errorKey) {
        super(OAuth2ErrorType.class, errorKey);
    }
}
```

`platform-oauth2-api/build.gradle.kts`: add `implementation(project(":server:libs:core:exception:exception-api"))` and `implementation(project(":sdks:backend:java:component-api"))`.

- [ ] **Step 4: Add the config property and yml defaults**

In `ApplicationProperties.Oauth2` add, next to `resourceServer`:

```java
        private Discovery discovery = new Discovery();

        public Discovery getDiscovery() {
            return discovery;
        }

        public void setDiscovery(Discovery discovery) {
            this.discovery = discovery;
        }

        public static class Discovery {

            /**
             * Hosts that OAuth2 discovery and dynamic client registration may contact even when they resolve to a
             * private address or use plain http.
             */
            private List<String> allowedHosts = new ArrayList<>();

            public List<String> getAllowedHosts() {
                return allowedHosts;
            }

            public void setAllowedHosts(List<String> allowedHosts) {
                this.allowedHosts = allowedHosts;
            }
        }
```

In `application-bytechef.yml` under `oauth2:` (after `redirect-uri`), and the same block under `oauth2:` in `config-server-app/.../apps/application.yml`:

```yaml
    discovery:
      allowed-hosts: []
```

- [ ] **Step 5: Run the module check**

Run: `./gradlew :server:libs:platform:platform-oauth2:platform-oauth2-api:check :server:libs:config:app-config:check --offline -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply --offline -q
git add server/libs/platform/platform-oauth2/platform-oauth2-api server/libs/config/app-config server/apps/server-app/src/main/resources/config/application-bytechef.yml server/ee/apps/config-server-app/src/main/resources/config/apps/application.yml
git commit -m "Add OAuth2 discovery API records, services and discovery allowed-hosts property"
```

---

### Task 3: `OAuth2HttpClient` — validated, redirect-free, size-capped HTTP

**Files:**
- Modify: `server/libs/platform/platform-oauth2/platform-oauth2-service/build.gradle.kts`
- Create: `server/libs/platform/platform-oauth2/platform-oauth2-service/src/main/java/com/bytechef/platform/oauth2/service/OAuth2HttpClient.java`
- Test: `server/libs/platform/platform-oauth2/platform-oauth2-service/src/test/java/com/bytechef/platform/oauth2/service/OAuth2HttpClientTest.java`

**Interfaces (Produces):**

```java
@Component
public class OAuth2HttpClient {
    public record Response(int statusCode, String body, HttpHeaders headers) {}
    public OAuth2HttpClient(ApplicationProperties applicationProperties, Environment environment);
    public void validateUrl(String url);                        // throws ConfigurationException(URL_NOT_ALLOWED)
    public Response probe(String url, String jsonBody);         // POST, body discarded, returns status + headers
    public Response get(String url);                            // GET Accept: application/json
    public Response post(String url, String contentType, String body, Map<String, String> headers);
}
```

- [ ] **Step 1: Add dependencies**

`platform-oauth2-service/build.gradle.kts` becomes:

```kotlin
dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:encryption:encryption-api"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-oauth2:platform-oauth2-api"))

    testImplementation("org.springframework.security:spring-security-core")
    testImplementation(libs.org.wiremock.wiremock)
    testImplementation(project(":server:libs:test:test-support"))
}
```

- [ ] **Step 2: Write the failing test**

```java
package com.bytechef.platform.oauth2.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class OAuth2HttpClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private OAuth2HttpClient oAuth2HttpClient;

    @BeforeEach
    void beforeEach() {
        oAuth2HttpClient = createClient(List.of("localhost"), false);
    }

    static OAuth2HttpClient createClient(List<String> allowedHosts, boolean devProfile) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getOauth2()
            .getDiscovery()
            .setAllowedHosts(allowedHosts);

        Environment environment = mock(Environment.class);

        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(devProfile);

        return new OAuth2HttpClient(applicationProperties, environment);
    }

    @Test
    void testGetReturnsStatusBodyAndHeaders() {
        wireMock.stubFor(get(urlEqualTo("/metadata")).willReturn(
            aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"issuer\":\"x\"}")));

        OAuth2HttpClient.Response response = oAuth2HttpClient.get(wireMock.baseUrl() + "/metadata");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"issuer\":\"x\"}");
        assertThat(response.headers().firstValue("Content-Type")).contains("application/json");
    }

    @Test
    void testGetDoesNotFollowRedirects() {
        wireMock.stubFor(get(urlEqualTo("/redirect")).willReturn(
            aResponse().withStatus(302).withHeader("Location", wireMock.baseUrl() + "/metadata")));

        OAuth2HttpClient.Response response = oAuth2HttpClient.get(wireMock.baseUrl() + "/redirect");

        assertThat(response.statusCode()).isEqualTo(302);
    }

    @Test
    void testGetRejectsOversizedBody() {
        wireMock.stubFor(get(urlEqualTo("/big")).willReturn(aResponse().withStatus(200).withBody("x".repeat(65 * 1024))));

        assertThatThrownBy(() -> oAuth2HttpClient.get(wireMock.baseUrl() + "/big"))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.INVALID_METADATA);
    }

    @Test
    void testValidateUrlRejectsLoopbackHostThatIsNotAllowlisted() {
        OAuth2HttpClient strictClient = createClient(List.of(), false);

        assertThatThrownBy(() -> strictClient.validateUrl("http://localhost:1234/mcp"))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.URL_NOT_ALLOWED);
    }

    @Test
    void testValidateUrlRejectsPlainHttpForPublicHost() {
        assertThatThrownBy(() -> oAuth2HttpClient.validateUrl("http://mcp.example.com/mcp"))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.URL_NOT_ALLOWED);
    }

    @Test
    void testValidateUrlAllowsLocalhostUnderDevProfile() {
        OAuth2HttpClient devClient = createClient(List.of(), true);

        devClient.validateUrl("http://localhost:1234/mcp");
    }

    @Test
    void testPostSendsBodyAndHeaders() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(aResponse().withStatus(201).withBody("{}")));

        OAuth2HttpClient.Response response = oAuth2HttpClient.post(
            wireMock.baseUrl() + "/register", "application/json", "{\"a\":1}", Map.of("X-Test", "yes"));

        assertThat(response.statusCode()).isEqualTo(201);
        wireMock.verify(com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlEqualTo("/register"))
            .withHeader("X-Test", com.github.tomakehurst.wiremock.client.WireMock.equalTo("yes"))
            .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.equalToJson("{\"a\":1}")));
    }
}
```

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew :server:libs:platform:platform-oauth2:platform-oauth2-service:test --tests '*OAuth2HttpClientTest' --offline -q`
Expected: compilation failure (`OAuth2HttpClient` missing).

- [ ] **Step 4: Implement `OAuth2HttpClient`**

```java
package com.bytechef.platform.oauth2.service;

import com.bytechef.commons.util.UrlValidatorUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class OAuth2HttpClient {

    static final int MAX_RESPONSE_BYTES = 64 * 1024;

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Set<String> DEV_HOSTS = Set.of("localhost", "127.0.0.1");

    private final Set<String> allowedHosts;

    public OAuth2HttpClient(ApplicationProperties applicationProperties, Environment environment) {
        Set<String> hosts = new HashSet<>(
            applicationProperties.getOauth2()
                .getDiscovery()
                .getAllowedHosts());

        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            hosts.addAll(DEV_HOSTS);
        }

        this.allowedHosts = Set.copyOf(hosts);
    }

    public record Response(int statusCode, String body, HttpHeaders headers) {
    }

    public void validateUrl(String url) {
        if (!UrlValidatorUtils.isValid(url, allowedHosts)) {
            throw new ConfigurationException(
                "This server address isn't allowed: " + url, OAuth2ErrorType.URL_NOT_ALLOWED);
        }

        URI uri = URI.create(url);
        String scheme = uri.getScheme()
            .toLowerCase(Locale.ROOT);

        if (!"https".equals(scheme) && !isAllowlisted(uri.getHost())) {
            throw new ConfigurationException(
                "This server address must use https: " + url, OAuth2ErrorType.URL_NOT_ALLOWED);
        }
    }

    public Response probe(String url, String jsonBody) {
        validateUrl(url);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        return send(request, HttpResponse.BodyHandlers.discarding(), ignored -> "");
    }

    public Response get(String url) {
        validateUrl(url);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("Accept", "application/json")
            .GET()
            .build();

        return send(request, HttpResponse.BodyHandlers.ofByteArray(), this::toBody);
    }

    public Response post(String url, String contentType, String body, Map<String, String> headers) {
        validateUrl(url);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("Content-Type", contentType)
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body));

        headers.forEach(requestBuilder::header);

        return send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray(), this::toBody);
    }

    private <T> Response send(
        HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler,
        java.util.function.Function<T, String> bodyMapper) {

        try (HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(TIMEOUT)
            .build()) {

            HttpResponse<T> httpResponse = httpClient.send(request, bodyHandler);

            return new Response(httpResponse.statusCode(), bodyMapper.apply(httpResponse.body()), httpResponse.headers());
        } catch (IOException ioException) {
            throw new ConfigurationException(
                "Unable to reach " + request.uri() + ": " + ioException.getMessage(), ioException,
                OAuth2ErrorType.METADATA_NOT_FOUND);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new ConfigurationException(
                "Interrupted while calling " + request.uri(), interruptedException, OAuth2ErrorType.METADATA_NOT_FOUND);
        }
    }

    private String toBody(byte[] bytes) {
        if (bytes.length > MAX_RESPONSE_BYTES) {
            throw new ConfigurationException(
                "Response larger than " + MAX_RESPONSE_BYTES + " bytes", OAuth2ErrorType.INVALID_METADATA);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private boolean isAllowlisted(String host) {
        return host != null && allowedHosts.stream()
            .anyMatch(allowedHost -> allowedHost.equalsIgnoreCase(host));
    }
}
```

Replace `java.util.function.Function` with an import. The `discarding()` handler means a streaming 200 from an MCP server costs at most the 10 s timeout and no memory.

- [ ] **Step 5: Run the test**

Run: `./gradlew :server:libs:platform:platform-oauth2:platform-oauth2-service:test --tests '*OAuth2HttpClientTest' --offline -q`
Expected: 7 tests pass.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply --offline -q
git add server/libs/platform/platform-oauth2/platform-oauth2-service
git commit -m "Add validated OAuth2 HTTP client for discovery and registration"
```

---

### Task 4: `OAuth2DiscoveryClient` — RFC 9728 + RFC 8414 discovery

**Files:**
- Create: `server/libs/platform/platform-oauth2/platform-oauth2-service/src/main/java/com/bytechef/platform/oauth2/service/OAuth2DiscoveryClient.java`
- Test: `.../src/test/java/com/bytechef/platform/oauth2/service/OAuth2DiscoveryClientTest.java`

**Interfaces:**
- Consumes: `OAuth2HttpClient` (Task 3), `DiscoveredAuthorizationServer`, `OAuth2ErrorType` (Task 2).
- Produces: `@Component public class OAuth2DiscoveryClient { public DiscoveredAuthorizationServer discover(String protectedResourceUrl); }`

Discovery algorithm (the tests below pin each line):

1. `validateUrl(protectedResourceUrl)`; `probe` it with a JSON-RPC `initialize` body.
2. Status 401/403 with `WWW-Authenticate` containing `resource_metadata="<url>"` → that URL. Otherwise try `{origin}/.well-known/oauth-protected-resource{path}` then `{origin}/.well-known/oauth-protected-resource`. First 200 wins. Status 200 on the probe and no metadata found → `METADATA_NOT_FOUND` ("doesn't advertise OAuth").
3. Protected resource metadata: `resource` required; its origin must equal the protected resource URL's origin → else `INVALID_METADATA`. `authorization_servers[0]` or, if absent, the resource origin. `scopes_supported` optional.
4. Authorization server metadata for issuer `I` (origin `O`, path `p`): if `p` non-empty try `O/.well-known/oauth-authorization-server{p}`, `O/.well-known/openid-configuration{p}`, `I/.well-known/openid-configuration`; else `O/.well-known/oauth-authorization-server`, `O/.well-known/openid-configuration`. None → `METADATA_NOT_FOUND`.
5. `issuer` must equal `I` (both without trailing slash); `authorization_endpoint` and `token_endpoint` required; if `code_challenge_methods_supported` is present it must contain `S256` → else `INVALID_METADATA`. Every endpoint URL passes `validateUrl`.
6. Scopes: protected-resource `scopes_supported` when non-empty, else the authorization server's.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.oauth2.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(ObjectMapperSetupExtension.class)
class OAuth2DiscoveryClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private OAuth2DiscoveryClient oAuth2DiscoveryClient;
    private String baseUrl;

    @BeforeEach
    void beforeEach() {
        baseUrl = wireMock.baseUrl();
        oAuth2DiscoveryClient = new OAuth2DiscoveryClient(OAuth2HttpClientTest.createClient(List.of("localhost"), false));
    }

    private void stubAuthorizationServer(String issuerPath, String extraJson) {
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server" + issuerPath)).willReturn(
            aResponse().withStatus(200)
                .withBody("""
                    {"issuer":"%s%s","authorization_endpoint":"%s/authorize","token_endpoint":"%s/token",
                     "registration_endpoint":"%s/register","code_challenge_methods_supported":["S256"],
                     "token_endpoint_auth_methods_supported":["none","client_secret_basic"]%s}
                    """.formatted(baseUrl, issuerPath, baseUrl, baseUrl, baseUrl, extraJson))));
    }

    private void stubProtectedResource(String metadataPath, String scopesJson) {
        wireMock.stubFor(get(urlEqualTo(metadataPath)).willReturn(
            aResponse().withStatus(200)
                .withBody("""
                    {"resource":"%s/mcp","authorization_servers":["%s"]%s}
                    """.formatted(baseUrl, baseUrl, scopesJson))));
    }

    @Test
    void testDiscoverPrefersResourceMetadataFromWwwAuthenticateHeader() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(
            aResponse().withStatus(401)
                .withHeader("WWW-Authenticate",
                    "Bearer resource_metadata=\"" + baseUrl + "/.well-known/oauth-protected-resource/custom\"")));
        stubProtectedResource("/.well-known/oauth-protected-resource/custom", ",\"scopes_supported\":[\"read\"]");
        stubProtectedResource("/.well-known/oauth-protected-resource/mcp", ",\"scopes_supported\":[\"wrong\"]");
        stubAuthorizationServer("", "");

        DiscoveredAuthorizationServer authorizationServer = oAuth2DiscoveryClient.discover(baseUrl + "/mcp");

        assertThat(authorizationServer.scopes()).containsExactly("read");
        assertThat(authorizationServer.resource()).isEqualTo(baseUrl + "/mcp");
        assertThat(authorizationServer.registrationEndpoint()).isEqualTo(baseUrl + "/register");
    }

    @Test
    void testDiscoverFallsBackToWellKnownPathWithResourcePath() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(401)));
        stubProtectedResource("/.well-known/oauth-protected-resource/mcp", "");
        stubAuthorizationServer("", ",\"scopes_supported\":[\"as-scope\"]");

        DiscoveredAuthorizationServer authorizationServer = oAuth2DiscoveryClient.discover(baseUrl + "/mcp");

        assertThat(authorizationServer.scopes()).containsExactly("as-scope");
    }

    @Test
    void testDiscoverTriesOpenIdConfigurationForIssuerWithPath() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(401)));
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-protected-resource/mcp")).willReturn(
            aResponse().withStatus(200)
                .withBody("{\"resource\":\"%s/mcp\",\"authorization_servers\":[\"%s/tenant\"]}".formatted(baseUrl, baseUrl))));
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server/tenant")).willReturn(aResponse().withStatus(404)));
        wireMock.stubFor(get(urlEqualTo("/.well-known/openid-configuration/tenant")).willReturn(aResponse().withStatus(404)));
        wireMock.stubFor(get(urlEqualTo("/tenant/.well-known/openid-configuration")).willReturn(
            aResponse().withStatus(200)
                .withBody("{\"issuer\":\"%s/tenant\",\"authorization_endpoint\":\"%s/a\",\"token_endpoint\":\"%s/t\"}"
                    .formatted(baseUrl, baseUrl, baseUrl))));

        DiscoveredAuthorizationServer authorizationServer = oAuth2DiscoveryClient.discover(baseUrl + "/mcp");

        assertThat(authorizationServer.issuer()).isEqualTo(baseUrl + "/tenant");
        assertThat(authorizationServer.registrationEndpoint()).isNull();
        assertThat(authorizationServer.codeChallengeMethods()).isEmpty();
    }

    @Test
    void testDiscoverRejectsResourceFromAnotherOrigin() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(401)));
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-protected-resource/mcp")).willReturn(
            aResponse().withStatus(200)
                .withBody("{\"resource\":\"https://evil.example.com/mcp\",\"authorization_servers\":[\"%s\"]}".formatted(baseUrl))));

        assertThatThrownBy(() -> oAuth2DiscoveryClient.discover(baseUrl + "/mcp"))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.INVALID_METADATA);
    }

    @Test
    void testDiscoverRejectsIssuerMismatch() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(401)));
        stubProtectedResource("/.well-known/oauth-protected-resource/mcp", "");
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server")).willReturn(
            aResponse().withStatus(200)
                .withBody("{\"issuer\":\"https://other.example.com\",\"authorization_endpoint\":\"%s/a\",\"token_endpoint\":\"%s/t\"}"
                    .formatted(baseUrl, baseUrl))));

        assertThatThrownBy(() -> oAuth2DiscoveryClient.discover(baseUrl + "/mcp"))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.INVALID_METADATA);
    }

    @Test
    void testDiscoverRejectsAuthorizationServerWithoutS256() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(401)));
        stubProtectedResource("/.well-known/oauth-protected-resource/mcp", "");
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-authorization-server")).willReturn(
            aResponse().withStatus(200)
                .withBody("{\"issuer\":\"%s\",\"authorization_endpoint\":\"%s/a\",\"token_endpoint\":\"%s/t\",\"code_challenge_methods_supported\":[\"plain\"]}"
                    .formatted(baseUrl, baseUrl, baseUrl))));

        assertThatThrownBy(() -> oAuth2DiscoveryClient.discover(baseUrl + "/mcp"))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.INVALID_METADATA);
    }

    @Test
    void testDiscoverFailsWhenServerDoesNotRequireAuthorization() {
        wireMock.stubFor(post(urlEqualTo("/mcp")).willReturn(aResponse().withStatus(200).withBody("{}")));
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-protected-resource/mcp")).willReturn(aResponse().withStatus(404)));
        wireMock.stubFor(get(urlEqualTo("/.well-known/oauth-protected-resource")).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> oAuth2DiscoveryClient.discover(baseUrl + "/mcp"))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.METADATA_NOT_FOUND);
    }

    @Test
    void testDiscoverRejectsBlockedProtectedResourceUrl() {
        assertThatThrownBy(() -> oAuth2DiscoveryClient.discover("http://10.0.0.5/mcp"))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.URL_NOT_ALLOWED);
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:platform:platform-oauth2:platform-oauth2-service:test --tests '*OAuth2DiscoveryClientTest' --offline -q`
Expected: compilation failure (`OAuth2DiscoveryClient` missing).

- [ ] **Step 3: Implement `OAuth2DiscoveryClient`**

```java
package com.bytechef.platform.oauth2.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class OAuth2DiscoveryClient {

    private static final String INITIALIZE_REQUEST = """
        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},
         "clientInfo":{"name":"ByteChef","version":"1.0"}}}
        """;
    private static final Pattern RESOURCE_METADATA_PATTERN = Pattern.compile("resource_metadata=\"([^\"]+)\"");
    private static final String PROTECTED_RESOURCE_WELL_KNOWN = "/.well-known/oauth-protected-resource";
    private static final String AUTHORIZATION_SERVER_WELL_KNOWN = "/.well-known/oauth-authorization-server";
    private static final String OPENID_WELL_KNOWN = "/.well-known/openid-configuration";

    private final OAuth2HttpClient oAuth2HttpClient;

    public OAuth2DiscoveryClient(OAuth2HttpClient oAuth2HttpClient) {
        this.oAuth2HttpClient = oAuth2HttpClient;
    }

    public DiscoveredAuthorizationServer discover(String protectedResourceUrl) {
        oAuth2HttpClient.validateUrl(protectedResourceUrl);

        URI resourceUri = URI.create(protectedResourceUrl);
        Map<String, Object> protectedResourceMetadata = fetchProtectedResourceMetadata(resourceUri);

        String resource = requireString(protectedResourceMetadata, "resource");

        if (!originOf(URI.create(resource)).equals(originOf(resourceUri))) {
            throw new ConfigurationException(
                "Protected resource metadata advertises a resource from another origin: " + resource,
                OAuth2ErrorType.INVALID_METADATA);
        }

        String issuer = stripTrailingSlash(
            firstString(protectedResourceMetadata, "authorization_servers").orElse(originOf(resourceUri)));
        Map<String, Object> authorizationServerMetadata = fetchAuthorizationServerMetadata(issuer);

        String advertisedIssuer = stripTrailingSlash(requireString(authorizationServerMetadata, "issuer"));

        if (!advertisedIssuer.equals(issuer)) {
            throw new ConfigurationException(
                "Authorization server metadata issuer %s does not match %s".formatted(advertisedIssuer, issuer),
                OAuth2ErrorType.INVALID_METADATA);
        }

        List<String> codeChallengeMethods = strings(authorizationServerMetadata, "code_challenge_methods_supported");

        if (!codeChallengeMethods.isEmpty() && !codeChallengeMethods.contains("S256")) {
            throw new ConfigurationException(
                "Authorization server does not support PKCE S256", OAuth2ErrorType.INVALID_METADATA);
        }

        String authorizationEndpoint = requireString(authorizationServerMetadata, "authorization_endpoint");
        String tokenEndpoint = requireString(authorizationServerMetadata, "token_endpoint");
        String registrationEndpoint = optionalString(authorizationServerMetadata, "registration_endpoint");

        oAuth2HttpClient.validateUrl(authorizationEndpoint);
        oAuth2HttpClient.validateUrl(tokenEndpoint);

        if (registrationEndpoint != null) {
            oAuth2HttpClient.validateUrl(registrationEndpoint);
        }

        List<String> scopes = strings(protectedResourceMetadata, "scopes_supported");

        if (scopes.isEmpty()) {
            scopes = strings(authorizationServerMetadata, "scopes_supported");
        }

        return new DiscoveredAuthorizationServer(
            issuer, authorizationEndpoint, tokenEndpoint, registrationEndpoint, resource, scopes,
            codeChallengeMethods, strings(authorizationServerMetadata, "token_endpoint_auth_methods_supported"));
    }

    private Map<String, Object> fetchProtectedResourceMetadata(URI resourceUri) {
        OAuth2HttpClient.Response probeResponse = oAuth2HttpClient.probe(resourceUri.toString(), INITIALIZE_REQUEST);

        List<String> candidateUrls = new ArrayList<>();

        if (probeResponse.statusCode() == 401 || probeResponse.statusCode() == 403) {
            probeResponse.headers()
                .firstValue("WWW-Authenticate")
                .map(RESOURCE_METADATA_PATTERN::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .ifPresent(candidateUrls::add);
        }

        String origin = originOf(resourceUri);
        String path = stripTrailingSlash(resourceUri.getRawPath() == null ? "" : resourceUri.getRawPath());

        if (!path.isEmpty()) {
            candidateUrls.add(origin + PROTECTED_RESOURCE_WELL_KNOWN + path);
        }

        candidateUrls.add(origin + PROTECTED_RESOURCE_WELL_KNOWN);

        return fetchFirstJson(candidateUrls)
            .orElseThrow(() -> new ConfigurationException(
                "This server doesn't advertise OAuth; try Bearer Token or no authentication",
                OAuth2ErrorType.METADATA_NOT_FOUND));
    }

    private Map<String, Object> fetchAuthorizationServerMetadata(String issuer) {
        URI issuerUri = URI.create(issuer);
        String origin = originOf(issuerUri);
        String path = stripTrailingSlash(issuerUri.getRawPath() == null ? "" : issuerUri.getRawPath());

        List<String> candidateUrls = path.isEmpty()
            ? List.of(origin + AUTHORIZATION_SERVER_WELL_KNOWN, origin + OPENID_WELL_KNOWN)
            : List.of(
                origin + AUTHORIZATION_SERVER_WELL_KNOWN + path, origin + OPENID_WELL_KNOWN + path,
                issuer + OPENID_WELL_KNOWN);

        return fetchFirstJson(candidateUrls)
            .orElseThrow(() -> new ConfigurationException(
                "Authorization server metadata not found for " + issuer, OAuth2ErrorType.METADATA_NOT_FOUND));
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> fetchFirstJson(List<String> urls) {
        for (String url : urls) {
            OAuth2HttpClient.Response response = oAuth2HttpClient.get(url);

            if (response.statusCode() == 200) {
                try {
                    return Optional.of(JsonUtils.read(response.body(), Map.class));
                } catch (RuntimeException runtimeException) {
                    throw new ConfigurationException(
                        "Metadata at " + url + " is not valid JSON", runtimeException, OAuth2ErrorType.INVALID_METADATA);
                }
            }
        }

        return Optional.empty();
    }

    private static String requireString(Map<String, Object> metadata, String key) {
        String value = optionalString(metadata, key);

        if (value == null) {
            throw new ConfigurationException(
                "Metadata is missing required field " + key, OAuth2ErrorType.INVALID_METADATA);
        }

        return value;
    }

    private static String optionalString(Map<String, Object> metadata, String key) {
        return metadata.get(key) instanceof String string && !string.isBlank() ? string : null;
    }

    private static Optional<String> firstString(Map<String, Object> metadata, String key) {
        return strings(metadata, key).stream()
            .findFirst();
    }

    private static List<String> strings(Map<String, Object> metadata, String key) {
        if (metadata.get(key) instanceof List<?> list) {
            return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
        }

        return List.of();
    }

    private static String originOf(URI uri) {
        String origin = uri.getScheme() + "://" + uri.getHost();

        return uri.getPort() == -1 ? origin : origin + ":" + uri.getPort();
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
```

- [ ] **Step 4: Run the test**

Run: `./gradlew :server:libs:platform:platform-oauth2:platform-oauth2-service:test --tests '*OAuth2DiscoveryClientTest' --offline -q`
Expected: 8 tests pass.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply --offline -q
git add server/libs/platform/platform-oauth2/platform-oauth2-service
git commit -m "Add OAuth2 protected resource and authorization server discovery"
```

---

### Task 5: `OAuth2DynamicClientRegistrar` — RFC 7591 registration

**Files:**
- Create: `.../platform-oauth2-service/src/main/java/com/bytechef/platform/oauth2/service/OAuth2DynamicClientRegistrar.java`
- Test: `.../src/test/java/com/bytechef/platform/oauth2/service/OAuth2DynamicClientRegistrarTest.java`

**Interfaces:**
- Consumes: `OAuth2HttpClient`, `DiscoveredAuthorizationServer`, `RegisteredClient`, `OAuth2ErrorType`.
- Produces: `@Component public class OAuth2DynamicClientRegistrar { public RegisteredClient register(DiscoveredAuthorizationServer authorizationServer, String redirectUri); }`

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.oauth2.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(ObjectMapperSetupExtension.class)
class OAuth2DynamicClientRegistrarTest {

    private static final String REDIRECT_URI = "https://bytechef.example.com/callback";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private OAuth2DynamicClientRegistrar registrar;

    @BeforeEach
    void beforeEach() {
        registrar = new OAuth2DynamicClientRegistrar(OAuth2HttpClientTest.createClient(List.of("localhost"), false));
    }

    private DiscoveredAuthorizationServer authorizationServer(String registrationEndpoint, List<String> authMethods) {
        return new DiscoveredAuthorizationServer(
            wireMock.baseUrl(), wireMock.baseUrl() + "/authorize", wireMock.baseUrl() + "/token",
            registrationEndpoint, wireMock.baseUrl() + "/mcp", List.of(), List.of("S256"), authMethods);
    }

    @Test
    void testRegisterRequestsPublicClientWhenNoneIsSupported() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(
            aResponse().withStatus(201).withBody("{\"client_id\":\"public-1\",\"token_endpoint_auth_method\":\"none\"}")));

        RegisteredClient registeredClient = registrar.register(
            authorizationServer(wireMock.baseUrl() + "/register", List.of("client_secret_basic", "none")), REDIRECT_URI);

        assertThat(registeredClient).isEqualTo(new RegisteredClient("public-1", null, "none"));
        wireMock.verify(postRequestedFor(urlEqualTo("/register")).withRequestBody(equalToJson("""
            {"client_name":"ByteChef","redirect_uris":["%s"],"grant_types":["authorization_code","refresh_token"],
             "response_types":["code"],"token_endpoint_auth_method":"none"}
            """.formatted(REDIRECT_URI))));
    }

    @Test
    void testRegisterFallsBackToClientSecretBasicThenPost() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(
            aResponse().withStatus(201)
                .withBody("{\"client_id\":\"c\",\"client_secret\":\"s\",\"token_endpoint_auth_method\":\"client_secret_post\"}")));

        RegisteredClient registeredClient = registrar.register(
            authorizationServer(wireMock.baseUrl() + "/register", List.of("client_secret_post")), REDIRECT_URI);

        assertThat(registeredClient.tokenEndpointAuthMethod()).isEqualTo("client_secret_post");
        assertThat(registeredClient.clientSecret()).isEqualTo("s");
    }

    @Test
    void testRegisterUsesAuthMethodFromResponse() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(
            aResponse().withStatus(201)
                .withBody("{\"client_id\":\"c\",\"client_secret\":\"s\",\"token_endpoint_auth_method\":\"client_secret_basic\"}")));

        RegisteredClient registeredClient = registrar.register(
            authorizationServer(wireMock.baseUrl() + "/register", List.of()), REDIRECT_URI);

        assertThat(registeredClient.tokenEndpointAuthMethod()).isEqualTo("client_secret_basic");
    }

    @Test
    void testRegisterDefaultsToRequestedMethodWhenResponseOmitsIt() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(aResponse().withStatus(201).withBody("{\"client_id\":\"c\"}")));

        RegisteredClient registeredClient = registrar.register(
            authorizationServer(wireMock.baseUrl() + "/register", List.of()), REDIRECT_URI);

        assertThat(registeredClient.tokenEndpointAuthMethod()).isEqualTo("none");
    }

    @Test
    void testRegisterFailsWithoutRegistrationEndpoint() {
        assertThatThrownBy(() -> registrar.register(authorizationServer(null, List.of("none")), REDIRECT_URI))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.REGISTRATION_NOT_SUPPORTED);
    }

    @Test
    void testRegisterPassesThroughServerErrorDescription() {
        wireMock.stubFor(post(urlEqualTo("/register")).willReturn(
            aResponse().withStatus(400)
                .withBody("{\"error\":\"invalid_redirect_uri\",\"error_description\":\"redirect not allowed\"}")));

        assertThatThrownBy(() -> registrar.register(authorizationServer(wireMock.baseUrl() + "/register", List.of()), REDIRECT_URI))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("redirect not allowed")
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.REGISTRATION_REJECTED);
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:platform:platform-oauth2:platform-oauth2-service:test --tests '*OAuth2DynamicClientRegistrarTest' --offline -q`
Expected: compilation failure.

- [ ] **Step 3: Implement `OAuth2DynamicClientRegistrar`**

```java
package com.bytechef.platform.oauth2.service;

import static com.bytechef.component.definition.Authorization.TOKEN_ENDPOINT_AUTH_METHOD_BASIC;
import static com.bytechef.component.definition.Authorization.TOKEN_ENDPOINT_AUTH_METHOD_NONE;
import static com.bytechef.component.definition.Authorization.TOKEN_ENDPOINT_AUTH_METHOD_POST;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OAuth2DynamicClientRegistrar {

    private static final List<String> PREFERRED_AUTH_METHODS = List.of(
        TOKEN_ENDPOINT_AUTH_METHOD_NONE, TOKEN_ENDPOINT_AUTH_METHOD_BASIC, TOKEN_ENDPOINT_AUTH_METHOD_POST);

    private final OAuth2HttpClient oAuth2HttpClient;

    public OAuth2DynamicClientRegistrar(OAuth2HttpClient oAuth2HttpClient) {
        this.oAuth2HttpClient = oAuth2HttpClient;
    }

    public RegisteredClient register(DiscoveredAuthorizationServer authorizationServer, String redirectUri) {
        String registrationEndpoint = authorizationServer.registrationEndpoint();

        if (registrationEndpoint == null) {
            throw new ConfigurationException(
                "This server doesn't allow automatic registration; use OAuth2 (manual)",
                OAuth2ErrorType.REGISTRATION_NOT_SUPPORTED);
        }

        String requestedAuthMethod = selectAuthMethod(authorizationServer.tokenEndpointAuthMethods());

        Map<String, Object> registrationRequest = new LinkedHashMap<>();

        registrationRequest.put("client_name", "ByteChef");
        registrationRequest.put("redirect_uris", List.of(redirectUri));
        registrationRequest.put("grant_types", List.of("authorization_code", "refresh_token"));
        registrationRequest.put("response_types", List.of("code"));
        registrationRequest.put("token_endpoint_auth_method", requestedAuthMethod);

        OAuth2HttpClient.Response response = oAuth2HttpClient.post(
            registrationEndpoint, "application/json", JsonUtils.write(registrationRequest), Map.of());

        Map<String, Object> responseBody = readBody(response.body());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            Object errorDescription = responseBody.getOrDefault("error_description", responseBody.get("error"));

            throw new ConfigurationException(
                "Registration rejected: " + (errorDescription == null ? response.body() : errorDescription),
                OAuth2ErrorType.REGISTRATION_REJECTED);
        }

        if (!(responseBody.get("client_id") instanceof String clientId) || clientId.isBlank()) {
            throw new ConfigurationException(
                "Registration response is missing client_id", OAuth2ErrorType.REGISTRATION_REJECTED);
        }

        String clientSecret = responseBody.get("client_secret") instanceof String secret && !secret.isBlank()
            ? secret : null;
        String authMethod = responseBody.get("token_endpoint_auth_method") instanceof String method && !method.isBlank()
            ? method : requestedAuthMethod;

        return new RegisteredClient(clientId, clientSecret, authMethod);
    }

    private static String selectAuthMethod(List<String> supportedAuthMethods) {
        if (supportedAuthMethods.isEmpty()) {
            return TOKEN_ENDPOINT_AUTH_METHOD_NONE;
        }

        return PREFERRED_AUTH_METHODS.stream()
            .filter(supportedAuthMethods::contains)
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                "Authorization server supports no usable token endpoint authentication method: " + supportedAuthMethods,
                OAuth2ErrorType.REGISTRATION_NOT_SUPPORTED));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readBody(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }

        try {
            return JsonUtils.read(body, Map.class);
        } catch (RuntimeException runtimeException) {
            return Map.of();
        }
    }
}
```

- [ ] **Step 4: Run the test**

Run: `./gradlew :server:libs:platform:platform-oauth2:platform-oauth2-service:test --tests '*OAuth2DynamicClientRegistrarTest' --offline -q`
Expected: 6 tests pass.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply --offline -q
git add server/libs/platform/platform-oauth2/platform-oauth2-service
git commit -m "Add OAuth2 dynamic client registration"
```

---

### Task 6: `OAuth2DiscoveryServiceImpl` and the sealer `OAuth2AuthorizationContextServiceImpl`

**Files:**
- Create: `.../platform-oauth2-service/src/main/java/com/bytechef/platform/oauth2/service/OAuth2DiscoveryServiceImpl.java`
- Create: `.../platform-oauth2-service/src/main/java/com/bytechef/platform/oauth2/service/OAuth2AuthorizationContextServiceImpl.java`
- Test: `.../src/test/java/com/bytechef/platform/oauth2/service/OAuth2DiscoveryServiceTest.java`
- Test: `.../src/test/java/com/bytechef/platform/oauth2/service/OAuth2AuthorizationContextServiceTest.java`

**Interfaces:**
- Consumes: `OAuth2DiscoveryClient` (Task 4), `OAuth2DynamicClientRegistrar` (Task 5), `Encryption`, `TenantContext.getCurrentTenantId()`, `SecurityUtils.getCurrentUserLogin()`, `ApplicationProperties.Oauth2.getRedirectUri()`.
- Produces: `@Service OAuth2DiscoveryServiceImpl implements OAuth2DiscoveryService` (`register` uses the configured redirect URI); `@Service OAuth2AuthorizationContextServiceImpl implements OAuth2AuthorizationContextService` with constructors `(Encryption encryption)` and `(Encryption encryption, Clock clock)`; constants `DISCOVERY_TTL = Duration.ofMinutes(10)`, `PKCE_TTL = Duration.ofHours(1)`.

Sealed payload is JSON `{context fields..., "tenantId", "principal", "expiresAt" (epoch seconds)}` encrypted with `Encryption.encrypt`. `unseal` throws `ConfigurationException(OAuth2ErrorType.INVALID_AUTHORIZATION_CONTEXT)` when decryption fails, the JSON is malformed, `expiresAt <= now`, or tenant/principal/component do not match the current request.

- [ ] **Step 1: Write the failing tests**

`OAuth2DiscoveryServiceTest`:

```java
package com.bytechef.platform.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class OAuth2DiscoveryServiceTest {

    @Test
    void testRegisterUsesConfiguredRedirectUri() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getOauth2()
            .setRedirectUri("https://bytechef.example.com/callback");

        OAuth2DiscoveryClient discoveryClient = mock(OAuth2DiscoveryClient.class);
        OAuth2DynamicClientRegistrar registrar = mock(OAuth2DynamicClientRegistrar.class);
        DiscoveredAuthorizationServer authorizationServer = new DiscoveredAuthorizationServer(
            "https://auth.example.com", "https://auth.example.com/a", "https://auth.example.com/t",
            "https://auth.example.com/r", "https://mcp.example.com/mcp", List.of(), List.of(), List.of());
        RegisteredClient registeredClient = new RegisteredClient("c", null, "none");

        when(registrar.register(authorizationServer, "https://bytechef.example.com/callback")).thenReturn(registeredClient);

        OAuth2DiscoveryServiceImpl service = new OAuth2DiscoveryServiceImpl(
            applicationProperties, discoveryClient, registrar);

        assertThat(service.register(authorizationServer)).isSameAs(registeredClient);
    }
}
```

`OAuth2AuthorizationContextServiceTest`:

```java
package com.bytechef.platform.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.encryption.Encryption;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.OAuth2AuthorizationContext;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(ObjectMapperSetupExtension.class)
class OAuth2AuthorizationContextServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");
    private static final OAuth2AuthorizationContext PKCE_CONTEXT = OAuth2AuthorizationContext.ofPkce("canva", 1, "verifier");
    private static final OAuth2AuthorizationContext DISCOVERY_CONTEXT = OAuth2AuthorizationContext.ofDiscovery(
        "mcpClient", 1, "verifier", new RegisteredClient("client-1", "secret-1", "client_secret_basic"),
        new DiscoveredAuthorizationServer(
            "https://auth.example.com", "https://auth.example.com/a", "https://auth.example.com/t",
            "https://auth.example.com/r", "https://mcp.example.com/mcp", List.of(), List.of(), List.of()));

    private Encryption encryption;
    private MutableClock clock;
    private OAuth2AuthorizationContextServiceImpl service;

    @BeforeEach
    void beforeEach() {
        encryption = mock(Encryption.class);

        when(encryption.encrypt(anyString())).thenAnswer(invocation -> "sealed:" + invocation.getArgument(0));
        when(encryption.decrypt(anyString())).thenAnswer(invocation -> {
            String sealed = invocation.getArgument(0);

            if (!sealed.startsWith("sealed:")) {
                throw new IllegalStateException("tampered");
            }

            return sealed.substring("sealed:".length());
        });

        clock = new MutableClock(NOW);
        service = new OAuth2AuthorizationContextServiceImpl(encryption, clock);

        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("user@localhost.com", "n/a"));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSealAndUnsealDiscoveryContextRoundTrip() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(DISCOVERY_CONTEXT));

        OAuth2AuthorizationContext unsealed = TenantContext.callWithTenantId(
            "tenant-a", () -> service.unseal(sealed, "mcpClient"));

        assertThat(unsealed).isEqualTo(DISCOVERY_CONTEXT);
    }

    @Test
    void testUnsealRejectsTamperedContext() {
        assertInvalid(() -> service.unseal("garbage", "canva"));
    }

    @Test
    void testUnsealRejectsExpiredDiscoveryContext() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(DISCOVERY_CONTEXT));

        clock.advance(Duration.ofMinutes(10).plusSeconds(1));

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "mcpClient")));
    }

    @Test
    void testPkceContextStaysValidForAnHour() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(PKCE_CONTEXT));

        clock.advance(Duration.ofMinutes(59));

        assertThat(TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "canva")))
            .isEqualTo(PKCE_CONTEXT);

        clock.advance(Duration.ofMinutes(2));

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "canva")));
    }

    @Test
    void testUnsealRejectsOtherTenant() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(PKCE_CONTEXT));

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-b", () -> service.unseal(sealed, "canva")));
    }

    @Test
    void testUnsealRejectsOtherPrincipal() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(PKCE_CONTEXT));

        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("other@localhost.com", "n/a"));

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "canva")));
    }

    @Test
    void testUnsealRejectsOtherComponent() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(PKCE_CONTEXT));

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "mcpClient")));
    }

    private static void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.INVALID_AUTHORIZATION_CONTEXT);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
```

Note `TenantContext.callWithTenantId` wraps a `Callable`, so a `ConfigurationException` thrown inside surfaces unchanged (it is a `RuntimeException`).

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:libs:platform:platform-oauth2:platform-oauth2-service:test --tests '*OAuth2DiscoveryServiceTest' --tests '*OAuth2AuthorizationContextServiceTest' --offline -q`
Expected: compilation failure.

- [ ] **Step 3: Implement `OAuth2DiscoveryServiceImpl`**

```java
package com.bytechef.platform.oauth2.service;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import org.springframework.stereotype.Service;

@Service
public class OAuth2DiscoveryServiceImpl implements OAuth2DiscoveryService {

    private final OAuth2DiscoveryClient oAuth2DiscoveryClient;
    private final OAuth2DynamicClientRegistrar oAuth2DynamicClientRegistrar;
    private final String redirectUri;

    public OAuth2DiscoveryServiceImpl(
        ApplicationProperties applicationProperties, OAuth2DiscoveryClient oAuth2DiscoveryClient,
        OAuth2DynamicClientRegistrar oAuth2DynamicClientRegistrar) {

        this.oAuth2DiscoveryClient = oAuth2DiscoveryClient;
        this.oAuth2DynamicClientRegistrar = oAuth2DynamicClientRegistrar;
        this.redirectUri = applicationProperties.getOauth2()
            .getRedirectUri();
    }

    @Override
    public DiscoveredAuthorizationServer discover(String protectedResourceUrl) {
        return oAuth2DiscoveryClient.discover(protectedResourceUrl);
    }

    @Override
    public RegisteredClient register(DiscoveredAuthorizationServer authorizationServer) {
        return oAuth2DynamicClientRegistrar.register(authorizationServer, redirectUri);
    }
}
```

- [ ] **Step 4: Implement `OAuth2AuthorizationContextServiceImpl`**

```java
package com.bytechef.platform.oauth2.service;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.encryption.Encryption;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.OAuth2AuthorizationContext;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.tenant.TenantContext;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OAuth2AuthorizationContextServiceImpl implements OAuth2AuthorizationContextService {

    static final Duration DISCOVERY_TTL = Duration.ofMinutes(10);
    static final Duration PKCE_TTL = Duration.ofHours(1);

    private static final String EXPIRES_AT = "expiresAt";
    private static final String PRINCIPAL = "principal";
    private static final String TENANT_ID = "tenantId";

    private final Clock clock;
    private final Encryption encryption;

    @Autowired
    public OAuth2AuthorizationContextServiceImpl(Encryption encryption) {
        this(encryption, Clock.systemUTC());
    }

    OAuth2AuthorizationContextServiceImpl(Encryption encryption, Clock clock) {
        this.encryption = encryption;
        this.clock = clock;
    }

    @Override
    public String seal(OAuth2AuthorizationContext authorizationContext) {
        Duration ttl = authorizationContext.isDiscovery() ? DISCOVERY_TTL : PKCE_TTL;

        Map<String, Object> payload = new HashMap<>();

        payload.put("componentName", authorizationContext.componentName());
        payload.put("connectionVersion", authorizationContext.connectionVersion());
        payload.put("codeVerifier", authorizationContext.codeVerifier());
        payload.put("clientId", authorizationContext.clientId());
        payload.put("clientSecret", authorizationContext.clientSecret());
        payload.put("tokenEndpointAuthMethod", authorizationContext.tokenEndpointAuthMethod());
        payload.put("authorizationUrl", authorizationContext.authorizationUrl());
        payload.put("tokenUrl", authorizationContext.tokenUrl());
        payload.put("issuer", authorizationContext.issuer());
        payload.put("resource", authorizationContext.resource());
        payload.put(TENANT_ID, TenantContext.getCurrentTenantId());
        payload.put(PRINCIPAL, SecurityUtils.getCurrentUserLogin());
        payload.put(EXPIRES_AT, clock.instant()
            .plus(ttl)
            .getEpochSecond());

        return encryption.encrypt(JsonUtils.write(payload));
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2AuthorizationContext unseal(String sealedAuthorizationContext, String componentName) {
        Map<String, Object> payload;

        try {
            payload = JsonUtils.read(encryption.decrypt(sealedAuthorizationContext), Map.class);
        } catch (RuntimeException runtimeException) {
            throw invalid("Sign-in took too long or the request was altered; try again", runtimeException);
        }

        long expiresAt = ((Number) payload.get(EXPIRES_AT)).longValue();

        if (expiresAt <= clock.instant()
            .getEpochSecond()) {

            throw invalid("Sign-in took too long; try again", null);
        }

        if (!Objects.equals(payload.get(TENANT_ID), TenantContext.getCurrentTenantId()) ||
            !Objects.equals(payload.get(PRINCIPAL), SecurityUtils.getCurrentUserLogin()) ||
            !Objects.equals(payload.get("componentName"), componentName)) {

            throw invalid("This sign-in does not belong to the current user or component; try again", null);
        }

        return new OAuth2AuthorizationContext(
            componentName, ((Number) payload.get("connectionVersion")).intValue(),
            (String) payload.get("codeVerifier"), (String) payload.get("clientId"), (String) payload.get("clientSecret"),
            (String) payload.get("tokenEndpointAuthMethod"), (String) payload.get("authorizationUrl"),
            (String) payload.get("tokenUrl"), (String) payload.get("issuer"), (String) payload.get("resource"));
    }

    private static ConfigurationException invalid(String message, Throwable cause) {
        return cause == null
            ? new ConfigurationException(message, OAuth2ErrorType.INVALID_AUTHORIZATION_CONTEXT)
            : new ConfigurationException(message, cause, OAuth2ErrorType.INVALID_AUTHORIZATION_CONTEXT);
    }
}
```

Never log `payload`, `sealedAuthorizationContext` or the decrypted JSON.

- [ ] **Step 5: Run the whole module check**

Run: `./gradlew :server:libs:platform:platform-oauth2:platform-oauth2-service:check --offline -q`
Expected: BUILD SUCCESSFUL, all five test classes green.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply --offline -q
git add server/libs/platform/platform-oauth2/platform-oauth2-service
git commit -m "Add OAuth2 discovery service and sealed authorization context service"
```

---

### Task 7: `ConnectionDefinitionServiceImpl` — discovery parameters, sealed PKCE, public-client token calls

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-service/build.gradle.kts`
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ConnectionDefinitionServiceImpl.java` (constructor ~line 36; `getOAuth2AuthorizationParameters` private overload ~line 727; `getDefaultAuthorizationCallbackFunction` ~line 532; `getDefaultRefreshFunction` ~line 597; `getDefaultClientSecret` ~line 661)
- Modify: `.../platform-component-service/src/test/java/com/bytechef/platform/component/service/ConnectionDefinitionServiceTest.java` (replace the `@Disabled` stubs)

**Interfaces:**
- Consumes: `OAuth2DiscoveryService`, `OAuth2AuthorizationContextService`, `OAuth2AuthorizationContext` (Tasks 2, 6); `Authorization.getProtectedResourceUrl()`, new constants and the 5-argument `OAuth2AuthorizationParameters` (Task 1).
- Produces: constructor `ConnectionDefinitionServiceImpl(ComponentDefinitionRegistry, ContextFactory, TokenRefreshHandler, ObjectProvider<OAuth2DiscoveryService>, ObjectProvider<OAuth2AuthorizationContextService>)`. For every `OAUTH2_AUTHORIZATION_CODE_PKCE` authorization the returned `OAuth2AuthorizationParameters.authorizationContext()` is non-null and `extraQueryParameters` contains `code_challenge` + `code_challenge_method` but **not** `code_verifier`; in discovery mode it also contains `resource`. The default callback and refresh read `tokenEndpointAuthMethod` and `resource` from the connection parameters.

- [ ] **Step 1: Add the dependency**

In `platform-component-service/build.gradle.kts` add `implementation(project(":server:libs:platform:platform-oauth2:platform-oauth2-api"))` (the existing `testImplementation` line for the same project stays). `platform-oauth2-api` depends only on `platform-component-api`, so there is no cycle.

- [ ] **Step 2: Replace the disabled stubs with failing tests**

Rewrite `ConnectionDefinitionServiceTest` (keep the Apache header, drop the `@Disabled` methods):

```java
package com.bytechef.platform.component.service;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.aspect.TokenRefreshHandler;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.OAuth2AuthorizationContext;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import com.bytechef.platform.oauth2.service.OAuth2AuthorizationContextService;
import com.bytechef.platform.oauth2.service.OAuth2DiscoveryService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(ObjectMapperSetupExtension.class)
class ConnectionDefinitionServiceTest {

    private static final String COMPONENT_NAME = "mcpClient";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private ComponentDefinitionRegistry componentDefinitionRegistry;
    private OAuth2AuthorizationContextService oAuth2AuthorizationContextService;
    private OAuth2DiscoveryService oAuth2DiscoveryService;
    private ConnectionDefinitionServiceImpl connectionDefinitionService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
        oAuth2AuthorizationContextService = mock(OAuth2AuthorizationContextService.class);
        oAuth2DiscoveryService = mock(OAuth2DiscoveryService.class);

        ContextFactory contextFactory = mock(ContextFactory.class);

        when(contextFactory.createContext(anyString(), any())).thenReturn(mock(Context.class));

        ObjectProvider<OAuth2DiscoveryService> discoveryProvider = mock(ObjectProvider.class);
        ObjectProvider<OAuth2AuthorizationContextService> contextProvider = mock(ObjectProvider.class);

        when(discoveryProvider.getIfAvailable()).thenReturn(oAuth2DiscoveryService);
        when(contextProvider.getIfAvailable()).thenReturn(oAuth2AuthorizationContextService);
        when(oAuth2AuthorizationContextService.seal(any())).thenReturn("sealed-context");

        connectionDefinitionService = new ConnectionDefinitionServiceImpl(
            componentDefinitionRegistry, contextFactory, mock(TokenRefreshHandler.class), discoveryProvider,
            contextProvider);
    }

    private void registerAuthorization(Authorization authorization) {
        when(componentDefinitionRegistry.getAuthorization(COMPONENT_NAME, 1, authorization.getType()))
            .thenReturn(authorization);
    }

    @Test
    void testPkceParametersSealTheVerifierInsteadOfExposingIt() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));

        OAuth2AuthorizationParameters parameters = connectionDefinitionService.getOAuth2AuthorizationParameters(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of(Authorization.AUTHORIZATION_URL, "https://auth.example.com/authorize", Authorization.CLIENT_ID, "manual"));

        assertThat(parameters.authorizationContext()).isEqualTo("sealed-context");
        assertThat(parameters.extraQueryParameters())
            .containsKey("code_challenge")
            .containsEntry("code_challenge_method", "S256")
            .doesNotContainKey("code_verifier")
            .doesNotContainKey("resource");

        ArgumentCaptor<OAuth2AuthorizationContext> captor = ArgumentCaptor.forClass(OAuth2AuthorizationContext.class);

        org.mockito.Mockito.verify(oAuth2AuthorizationContextService).seal(captor.capture());

        assertThat(captor.getValue().isDiscovery()).isFalse();
        assertThat(captor.getValue().codeVerifier()).isNotBlank();
    }

    @Test
    void testDiscoveryParametersUseDiscoveredServerAndRegisteredClient() {
        registerAuthorization(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .protectedResourceUrl((connectionParameters, context) -> connectionParameters.getRequiredString("url")));

        DiscoveredAuthorizationServer authorizationServer = new DiscoveredAuthorizationServer(
            "https://auth.example.com", "https://auth.example.com/authorize", "https://auth.example.com/token",
            "https://auth.example.com/register", "https://mcp.example.com/mcp", List.of("read", "write"),
            List.of("S256"), List.of("none"));

        when(oAuth2DiscoveryService.discover("https://mcp.example.com/mcp")).thenReturn(authorizationServer);
        when(oAuth2DiscoveryService.register(authorizationServer)).thenReturn(new RegisteredClient("reg-1", null, "none"));

        OAuth2AuthorizationParameters parameters = connectionDefinitionService.getOAuth2AuthorizationParameters(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE, Map.of("url", "https://mcp.example.com/mcp"));

        assertThat(parameters.authorizationUrl()).isEqualTo("https://auth.example.com/authorize");
        assertThat(parameters.clientId()).isEqualTo("reg-1");
        assertThat(parameters.scopes()).containsOnlyKeys("read", "write");
        assertThat(parameters.extraQueryParameters()).containsEntry("resource", "https://mcp.example.com/mcp");
        assertThat(parameters.extraQueryParameters()).doesNotContainKey("code_verifier");

        ArgumentCaptor<OAuth2AuthorizationContext> captor = ArgumentCaptor.forClass(OAuth2AuthorizationContext.class);

        org.mockito.Mockito.verify(oAuth2AuthorizationContextService).seal(captor.capture());

        assertThat(captor.getValue().clientId()).isEqualTo("reg-1");
        assertThat(captor.getValue().tokenUrl()).isEqualTo("https://auth.example.com/token");
    }

    @Test
    void testDefaultCallbackOmitsClientSecretAndSendsResourceForPublicClient() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));
        wireMock.stubFor(post(urlEqualTo("/token")).willReturn(
            aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600}")));

        AuthorizationCallbackResponse response = connectionDefinitionService.executeAuthorizationCallback(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of(
                Authorization.CLIENT_ID, "reg-1", Authorization.TOKEN_URL, wireMock.baseUrl() + "/token",
                Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "none", Authorization.RESOURCE, "https://mcp.example.com/mcp",
                Authorization.CODE, "the-code", Authorization.CODE_VERIFIER, "the-verifier"),
            "https://bytechef.example.com/callback");

        assertThat(response.result()).containsEntry("access_token", "at");
        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withRequestBody(containing("client_id=reg-1"))
            .withRequestBody(containing("code_verifier=the-verifier"))
            .withRequestBody(containing("resource=https%3A%2F%2Fmcp.example.com%2Fmcp"))
            .withRequestBody(notMatching(".*client_secret=.*")));
    }

    @Test
    void testDefaultCallbackUsesBasicAuthenticationForClientSecretBasic() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));
        wireMock.stubFor(post(urlEqualTo("/token")).willReturn(
            aResponse().withStatus(200).withBody("{\"access_token\":\"at\"}")));

        connectionDefinitionService.executeAuthorizationCallback(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of(
                Authorization.CLIENT_ID, "reg-1", Authorization.CLIENT_SECRET, "s3cret",
                Authorization.TOKEN_URL, wireMock.baseUrl() + "/token",
                Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "client_secret_basic", Authorization.CODE, "c"),
            "https://bytechef.example.com/callback");

        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withHeader("Authorization", equalTo("Basic cmVnLTE6czNjcmV0"))
            .withRequestBody(notMatching(".*client_secret=.*")));
    }

    @Test
    void testDefaultCallbackStillSendsFormSecretWhenNoAuthMethodIsStored() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE));
        wireMock.stubFor(post(urlEqualTo("/token")).willReturn(
            aResponse().withStatus(200).withBody("{\"access_token\":\"at\"}")));

        connectionDefinitionService.executeAuthorizationCallback(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE,
            Map.of(
                Authorization.CLIENT_ID, "legacy", Authorization.CLIENT_SECRET, "legacy-secret",
                Authorization.TOKEN_URL, wireMock.baseUrl() + "/token", Authorization.CODE, "c"),
            "https://bytechef.example.com/callback");

        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withRequestBody(containing("client_secret=legacy-secret")));
    }

    @Test
    void testDefaultRefreshSendsResourceAndOmitsSecretForPublicClient() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));
        wireMock.stubFor(post(urlEqualTo("/token")).willReturn(
            aResponse().withStatus(200).withBody("{\"access_token\":\"new-at\",\"refresh_token\":\"new-rt\",\"expires_in\":600}")));

        Map<String, Object> connectionParameters = new HashMap<>();

        connectionParameters.put(Authorization.CLIENT_ID, "reg-1");
        connectionParameters.put(Authorization.TOKEN_URL, wireMock.baseUrl() + "/token");
        connectionParameters.put(Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "none");
        connectionParameters.put(Authorization.RESOURCE, "https://mcp.example.com/mcp");
        connectionParameters.put(Authorization.REFRESH_TOKEN, "rt");

        RefreshTokenResponse response = connectionDefinitionService.executeRefresh(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE, connectionParameters);

        assertThat(response.accessToken()).isEqualTo("new-at");
        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withRequestBody(containing("grant_type=refresh_token"))
            .withRequestBody(containing("resource=https%3A%2F%2Fmcp.example.com%2Fmcp"))
            .withRequestBody(notMatching(".*client_secret=.*")));
    }
}
```

If `executeRefresh`'s signature differs from `(String, int, AuthorizationType, Map)`, adapt the call to the signature at `ConnectionDefinitionService.java:58` — the assertion stays. `component`/`connection` imports are unused if the registry mock is enough; remove unused imports before committing.

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests '*ConnectionDefinitionServiceTest' --offline -q`
Expected: compilation failure (constructor arity, missing imports) — and, once it compiles, the PKCE test fails because `code_verifier` is still in `extraQueryParameters`.

- [ ] **Step 4: Change the constructor and inject the providers**

Add fields and constructor parameters:

```java
    private final ObjectProvider<OAuth2AuthorizationContextService> oAuth2AuthorizationContextServiceProvider;
    private final ObjectProvider<OAuth2DiscoveryService> oAuth2DiscoveryServiceProvider;

    public ConnectionDefinitionServiceImpl(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory,
        TokenRefreshHandler tokenRefreshHandler, ObjectProvider<OAuth2DiscoveryService> oAuth2DiscoveryServiceProvider,
        ObjectProvider<OAuth2AuthorizationContextService> oAuth2AuthorizationContextServiceProvider) {
```

Spring is the only caller today (`git grep -n "new ConnectionDefinitionServiceImpl("` returns nothing besides the new test), so no other call site changes.

- [ ] **Step 5: Rewrite the PKCE branch of the private `getOAuth2AuthorizationParameters`**

Replace the whole `if (authorization.getType() == AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) { ... }` block with a call `return getOAuth2AuthorizationCodePkceParameters(componentName, connectionVersion, authorization, parameters, context, authorizationUrlFunction, clientIdFunction, scopesFunction, oAuth2AuthorizationExtraQueryParametersFunction);` and add:

```java
    private OAuth2AuthorizationParameters getOAuth2AuthorizationCodePkceParameters(
        String componentName, int connectionVersion, Authorization authorization, Parameters parameters,
        Context context, AuthorizationUrlFunction authorizationUrlFunction, ClientIdFunction clientIdFunction,
        ScopesFunction scopesFunction,
        OAuth2AuthorizationExtraQueryParametersFunction oAuth2AuthorizationExtraQueryParametersFunction) {

        OAuth2AuthorizationContextService authorizationContextService = requireAvailable(
            oAuth2AuthorizationContextServiceProvider, "OAuth2 authorization context service");
        PkceFunction pkceFunction = authorization.getPkce()
            .orElse(getDefaultPkceFunction());

        try {
            String verifier = generateVerifier(context);
            Authorization.Pkce pkce = pkceFunction.apply(verifier, generateChallenge(verifier, context), "S256", context);

            Map<String, String> extraQueryParameters = new LinkedHashMap<>(
                oAuth2AuthorizationExtraQueryParametersFunction.apply(parameters, context));

            extraQueryParameters.put("code_challenge", pkce.challenge());
            extraQueryParameters.put("code_challenge_method", pkce.challengeMethod());

            Optional<ProtectedResourceUrlFunction> protectedResourceUrlFunction = authorization.getProtectedResourceUrl();

            if (protectedResourceUrlFunction.isPresent()) {
                OAuth2DiscoveryService discoveryService = requireAvailable(
                    oAuth2DiscoveryServiceProvider, "OAuth2 discovery service");

                DiscoveredAuthorizationServer authorizationServer = discoveryService.discover(
                    protectedResourceUrlFunction.get()
                        .apply(parameters, context));
                RegisteredClient registeredClient = discoveryService.register(authorizationServer);

                extraQueryParameters.put(Authorization.RESOURCE, authorizationServer.resource());

                String authorizationContext = authorizationContextService.seal(
                    OAuth2AuthorizationContext.ofDiscovery(
                        componentName, connectionVersion, pkce.verifier(), registeredClient, authorizationServer));

                return new OAuth2AuthorizationParameters(
                    authorizationServer.authorizationEndpoint(), registeredClient.clientId(), extraQueryParameters,
                    toScopes(authorizationServer.scopes()), authorizationContext);
            }

            String authorizationContext = authorizationContextService.seal(
                OAuth2AuthorizationContext.ofPkce(componentName, connectionVersion, pkce.verifier()));

            return new OAuth2AuthorizationParameters(
                authorizationUrlFunction.apply(parameters, context), clientIdFunction.apply(parameters, context),
                extraQueryParameters, scopesFunction.apply(parameters, context), authorizationContext);
        } catch (ConfigurationException configurationException) {
            throw configurationException;
        } catch (Exception e) {
            throw new ConfigurationException(e, ConnectionDefinitionErrorType.INVALID_OAUTH2_AUTHORIZATION_PARAMETERS);
        }
    }

    private static <T> T requireAvailable(ObjectProvider<T> provider, String name) {
        T service = provider.getIfAvailable();

        if (service == null) {
            throw new ConfigurationException(
                name + " is not available in this deployment", ConnectionDefinitionErrorType.INVALID_OAUTH2_AUTHORIZATION_PARAMETERS);
        }

        return service;
    }

    private static Map<String, Boolean> toScopes(List<String> scopes) {
        Map<String, Boolean> scopeMap = new LinkedHashMap<>();

        for (String scope : scopes) {
            scopeMap.put(scope, true);
        }

        return scopeMap;
    }
```

Re-throwing `ConfigurationException` unchanged keeps the discovery error types (`OAuth2ErrorType.*`) visible to the dialog instead of collapsing them into `INVALID_OAUTH2_AUTHORIZATION_PARAMETERS`.

- [ ] **Step 6: Make the default callback and refresh public-client aware**

Change `getDefaultClientSecret` to `return MapUtils.getString(connectionParameters, Authorization.CLIENT_SECRET);` (no longer required).

In `getDefaultAuthorizationCallbackFunction`, replace the two `builder.query("client_id"/"client_secret", ...)` lines and the request construction with:

```java
            String clientId = clientIdFunction.apply(connectionParameters, context);
            String clientSecret = clientSecretFunction.apply(connectionParameters, context);
            String tokenEndpointAuthMethod = connectionParameters.getString(
                Authorization.TOKEN_ENDPOINT_AUTH_METHOD, Authorization.TOKEN_ENDPOINT_AUTH_METHOD_POST);

            builder.query("client_id", clientId);

            if (sendsSecretInBody(clientSecret, tokenEndpointAuthMethod)) {
                builder.query("client_secret", clientSecret);
            }

            builder.query("code", code);
            builder.query("grant_type", "authorization_code");
            builder.query("redirect_uri", redirectUri);

            if (codeVerifier != null) {
                builder.query("code_verifier", codeVerifier);
            }

            String resource = connectionParameters.getString(Authorization.RESOURCE);

            if (StringUtils.isNotBlank(resource)) {
                builder.query("resource", resource);
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .POST(builder.build())
                .uri(URI.create(tokenUrlFunction.apply(connectionParameters, context)))
                .header("Accept", "application/json");

            applyBasicAuthentication(requestBuilder, clientId, clientSecret, tokenEndpointAuthMethod);
```

and send `requestBuilder.build()`. Apply the same four changes to `getDefaultRefreshFunction` (its `resource` comes from `connectionParameters` too). Add the two helpers:

```java
    private static boolean sendsSecretInBody(String clientSecret, String tokenEndpointAuthMethod) {
        return StringUtils.isNotBlank(clientSecret) &&
            !Authorization.TOKEN_ENDPOINT_AUTH_METHOD_BASIC.equals(tokenEndpointAuthMethod) &&
            !Authorization.TOKEN_ENDPOINT_AUTH_METHOD_NONE.equals(tokenEndpointAuthMethod);
    }

    private static void applyBasicAuthentication(
        HttpRequest.Builder requestBuilder, String clientId, String clientSecret, String tokenEndpointAuthMethod) {

        if (Authorization.TOKEN_ENDPOINT_AUTH_METHOD_BASIC.equals(tokenEndpointAuthMethod) &&
            StringUtils.isNotBlank(clientSecret)) {

            requestBuilder.header(
                "Authorization", "Basic " + EncodingUtils.base64EncodeToString(clientId + ":" + clientSecret));
        }
    }
```

A connection without `tokenEndpointAuthMethod` defaults to `client_secret_post`, so every existing OAuth2 connection sends exactly what it sent before.

- [ ] **Step 7: Run the tests and the module check**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:check --offline -q`
Expected: BUILD SUCCESSFUL; the six tests pass; `TokenRefreshAspectIntTest` and the other existing tests still pass.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply --offline -q
git add server/libs/platform/platform-component/platform-component-service
git commit -m "Seal the PKCE verifier and support discovery-based OAuth2 authorization parameters"
```

---

### Task 8: `ConnectionFacadeImpl` — unseal, exchange, persist, strip

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/facade/ConnectionFacadeImpl.java` (constructor ~line 61, `create` ~line 91, `replaceAuthorizationParameters` ~line 130, `resolveOAuth2AuthorizationCode` ~line 169)
- Modify: `.../platform-connection-service/src/test/java/com/bytechef/platform/connection/config/ConnectionIntTestConfigurationSharedMocks.java` (line ~35)
- Modify: `.../platform-connection-service/src/test/java/com/bytechef/platform/connection/facade/ConnectionFacadeIntTest.java`

**Interfaces:**
- Consumes: `OAuth2AuthorizationContextService.unseal(sealed, componentName)`, `OAuth2AuthorizationContext.isDiscovery()/toConnectionParameters()/codeVerifier()`, `OAuth2ErrorType.DISCOVERY_NOT_AVAILABLE`, `Authorization.AUTHORIZATION_CONTEXT/CODE/CODE_VERIFIER`.
- Produces: constructor gains `OAuth2AuthorizationContextService oAuth2AuthorizationContextService` (required: `platform-connection-service` ships only in `server-app` and `connection-app`, both of which include `platform-oauth2-service`). After `create` / `replaceAuthorizationParameters`, stored parameters never contain `code`, `code_verifier`, `authorizationContext` or `state`.

- [ ] **Step 1: Add the mock and write the failing tests**

In `ConnectionIntTestConfigurationSharedMocks` add `OAuth2AuthorizationContextService.class` to the `@MockitoBean(types = {...})` list (import `com.bytechef.platform.oauth2.service.OAuth2AuthorizationContextService`).

In `ConnectionFacadeIntTest` add fields `@Autowired private OAuth2AuthorizationContextService oAuth2AuthorizationContextService; @Autowired private OAuth2Service oAuth2Service;` and these tests (imports: `ArgumentCaptor`, `AuthorizationCallbackResponse`, `OAuth2AuthorizationContext`, `DiscoveredAuthorizationServer`, `RegisteredClient`, `ConfigurationException`, `OAuth2ErrorType`, `assertThatThrownBy`, `anyMap`, `eq`, `Authorization`):

```java
    private static final OAuth2AuthorizationContext DISCOVERY_CONTEXT = OAuth2AuthorizationContext.ofDiscovery(
        "componentName", 1, "sealed-verifier", new RegisteredClient("reg-1", null, "none"),
        new DiscoveredAuthorizationServer(
            "https://auth.example.com", "https://auth.example.com/authorize", "https://auth.example.com/token",
            "https://auth.example.com/register", "https://mcp.example.com/mcp", List.of(), List.of("S256"), List.of("none")));

    private ConnectionDTO oAuth2ConnectionDTO(Map<String, ?> parameters) {
        return ConnectionDTO.builder()
            .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("oauth")
            .parameters(parameters)
            .build();
    }

    private void stubOAuth2Exchange(Map<String, Object> tokenResponse) {
        when(connectionDefinitionService.getAuthorizationType("componentName", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE))
            .thenReturn(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE);
        when(oAuth2Service.checkPredefinedParameters(eq("componentName"), anyMap()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(oAuth2Service.getRedirectUri()).thenReturn("https://bytechef.example.com/callback");
        when(connectionDefinitionService.executeAuthorizationCallback(
            eq("componentName"), eq(1), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE), anyMap(),
            eq("https://bytechef.example.com/callback")))
            .thenReturn(new AuthorizationCallbackResponse(tokenResponse));
    }

    @Test
    void testCreateExchangesCodeWithUnsealedDiscoveryContextAndStripsTransientParameters() {
        stubOAuth2Exchange(Map.of("access_token", "at", "refresh_token", "rt", "expires_in", 3600));
        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName")).thenReturn(DISCOVERY_CONTEXT);

        long connectionId = connectionFacade.create(
            oAuth2ConnectionDTO(Map.of(
                "url", "https://mcp.example.com/mcp", Authorization.CODE, "the-code", "state", "s",
                Authorization.AUTHORIZATION_CONTEXT, "sealed")),
            PlatformType.AUTOMATION);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);

        verify(connectionDefinitionService).executeAuthorizationCallback(
            eq("componentName"), eq(1), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE), captor.capture(),
            eq("https://bytechef.example.com/callback"));

        Assertions.assertThat(captor.getValue())
            .containsEntry(Authorization.CODE_VERIFIER, "sealed-verifier")
            .containsEntry(Authorization.CLIENT_ID, "reg-1")
            .containsEntry(Authorization.TOKEN_URL, "https://auth.example.com/token")
            .containsEntry(Authorization.RESOURCE, "https://mcp.example.com/mcp");

        Map<String, ?> storedParameters = connectionRepository.findById(connectionId)
            .orElseThrow()
            .getParameters();

        Assertions.assertThat(storedParameters)
            .containsEntry("url", "https://mcp.example.com/mcp")
            .containsEntry(Authorization.CLIENT_ID, "reg-1")
            .containsEntry(Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "none")
            .containsEntry(Authorization.AUTHORIZATION_URL, "https://auth.example.com/authorize")
            .containsEntry(Authorization.RESOURCE, "https://mcp.example.com/mcp")
            .containsEntry("access_token", "at")
            .doesNotContainKeys(Authorization.CODE, Authorization.CODE_VERIFIER, Authorization.AUTHORIZATION_CONTEXT, "state");
    }

    @Test
    void testCreateUsesSealedVerifierForPlainPkceContext() {
        stubOAuth2Exchange(Map.of("access_token", "at"));
        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName"))
            .thenReturn(OAuth2AuthorizationContext.ofPkce("componentName", 1, "sealed-verifier"));

        long connectionId = connectionFacade.create(
            oAuth2ConnectionDTO(Map.of(
                Authorization.CLIENT_ID, "manual", Authorization.CODE, "c", Authorization.AUTHORIZATION_CONTEXT, "sealed")),
            PlatformType.AUTOMATION);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);

        verify(connectionDefinitionService).executeAuthorizationCallback(
            eq("componentName"), eq(1), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE), captor.capture(), any());

        Assertions.assertThat(captor.getValue()).containsEntry(Authorization.CODE_VERIFIER, "sealed-verifier");
        Assertions.assertThat(connectionRepository.findById(connectionId).orElseThrow().getParameters())
            .containsEntry(Authorization.CLIENT_ID, "manual")
            .doesNotContainKeys(Authorization.CODE_VERIFIER, Authorization.AUTHORIZATION_CONTEXT);
    }

    @Test
    void testCreateWithoutAuthorizationContextBehavesAsBefore() {
        stubOAuth2Exchange(Map.of("access_token", "at"));

        long connectionId = connectionFacade.create(
            oAuth2ConnectionDTO(Map.of(Authorization.CLIENT_ID, "manual", Authorization.CODE, "c", "state", "s")),
            PlatformType.AUTOMATION);

        verify(oAuth2AuthorizationContextService, never()).unseal(any(), any());
        Assertions.assertThat(connectionRepository.findById(connectionId).orElseThrow().getParameters())
            .containsEntry("access_token", "at")
            .doesNotContainKeys("state", Authorization.CODE);
    }

    @Test
    void testCreateRejectsDiscoveryContextForEmbeddedConnection() {
        stubOAuth2Exchange(Map.of("access_token", "at"));
        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName")).thenReturn(DISCOVERY_CONTEXT);

        assertThatThrownBy(() -> connectionFacade.create(
            oAuth2ConnectionDTO(Map.of(Authorization.CODE, "c", Authorization.AUTHORIZATION_CONTEXT, "sealed")),
            PlatformType.EMBEDDED))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.DISCOVERY_NOT_AVAILABLE);

        verify(connectionDefinitionService, never()).executeAuthorizationCallback(any(), anyInt(), any(), anyMap(), any());
    }

    @Test
    void testCreateRejectsExpiredAuthorizationContext() {
        stubOAuth2Exchange(Map.of("access_token", "at"));
        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName"))
            .thenThrow(new ConfigurationException("Sign-in took too long; try again", OAuth2ErrorType.INVALID_AUTHORIZATION_CONTEXT));

        assertThatThrownBy(() -> connectionFacade.create(
            oAuth2ConnectionDTO(Map.of(Authorization.CODE, "c", Authorization.AUTHORIZATION_CONTEXT, "sealed")),
            PlatformType.AUTOMATION))
            .isInstanceOf(ConfigurationException.class)
            .extracting("errorType")
            .isEqualTo(OAuth2ErrorType.INVALID_AUTHORIZATION_CONTEXT);

        Assertions.assertThat(connectionRepository.count()).isZero();
    }

    @Test
    void testCreateStoresTokensWhenRefreshTokenIsMissing() {
        stubOAuth2Exchange(Map.of("access_token", "at", "expires_in", "3600"));
        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName")).thenReturn(DISCOVERY_CONTEXT);

        long connectionId = connectionFacade.create(
            oAuth2ConnectionDTO(Map.of(Authorization.CODE, "c", Authorization.AUTHORIZATION_CONTEXT, "sealed")),
            PlatformType.AUTOMATION);

        Assertions.assertThat(connectionRepository.findById(connectionId).orElseThrow().getParameters())
            .containsEntry("access_token", "at")
            .containsEntry("expires_in", "3600")
            .doesNotContainKey("refresh_token");
    }
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:testIntegration --tests '*ConnectionFacadeIntTest' --offline -q`
Expected: the new tests fail (`unseal` never called, `code` still stored, EMBEDDED case not rejected); existing tests pass.

- [ ] **Step 3: Implement**

Constructor: add `OAuth2AuthorizationContextService oAuth2AuthorizationContextService` parameter and field.

`create`: move `connection.setType(type);` **before** `resolveOAuth2AuthorizationCode(connection);` and replace the `parameters.remove("state")` block with `removeTransientOAuth2Parameters(connection);`. `replaceAuthorizationParameters`: replace its `updatedParameters.remove("state")` block with `removeTransientOAuth2Parameters(connection); connectionService.replaceConnectionParameters(id, connection.getParameters());`.

```java
    private void resolveOAuth2AuthorizationCode(Connection connection) {
        if (connection.getAuthorizationType() == null || !connection.containsParameter(Authorization.CODE)) {
            return;
        }

        AuthorizationType authorizationType = connectionDefinitionService.getAuthorizationType(
            connection.getComponentName(), connection.getConnectionVersion(), connection.getAuthorizationType());

        if (authorizationType != AuthorizationType.OAUTH2_AUTHORIZATION_CODE &&
            authorizationType != AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE) {

            return;
        }

        Map<String, Object> callbackParameters = new HashMap<>(
            oAuth2Service.checkPredefinedParameters(connection.getComponentName(), connection.getParameters()));
        OAuth2AuthorizationContext authorizationContext = unsealAuthorizationContext(connection);

        if (authorizationContext != null) {
            callbackParameters.put(Authorization.CODE_VERIFIER, authorizationContext.codeVerifier());

            if (authorizationContext.isDiscovery()) {
                if (connection.getType() == PlatformType.EMBEDDED) {
                    throw new ConfigurationException(
                        "Automatic OAuth2 isn't available for embedded integrations yet; use OAuth2 (manual)",
                        OAuth2ErrorType.DISCOVERY_NOT_AVAILABLE);
                }

                callbackParameters.putAll(authorizationContext.toConnectionParameters());
            }
        }

        AuthorizationCallbackResponse authorizationCallbackResponse = Objects.requireNonNull(
            connectionDefinitionService.executeAuthorizationCallback(
                connection.getComponentName(), connection.getConnectionVersion(), connection.getAuthorizationType(),
                callbackParameters, oAuth2Service.getRedirectUri()),
            "Authorization callback returned no response for component " + connection.getComponentName());

        if (authorizationContext != null && authorizationContext.isDiscovery()) {
            connection.putAllParameters(authorizationContext.toConnectionParameters());
        }

        connection.putAllParameters(authorizationCallbackResponse.result());

        // existing predefined-app clientId/clientSecret fill and the missing-refresh-token warning stay here unchanged,
        // reading from `callbackParameters` instead of `predefinedParameters`
    }

    private OAuth2AuthorizationContext unsealAuthorizationContext(Connection connection) {
        Map<String, ?> parameters = connection.getParameters();
        Object sealedAuthorizationContext = parameters.get(Authorization.AUTHORIZATION_CONTEXT);

        if (!(sealedAuthorizationContext instanceof String sealed) || sealed.isBlank()) {
            return null;
        }

        return oAuth2AuthorizationContextService.unseal(sealed, connection.getComponentName());
    }

    private static void removeTransientOAuth2Parameters(Connection connection) {
        Map<String, Object> parameters = new HashMap<>(connection.getParameters());

        parameters.remove("state");
        parameters.remove(Authorization.CODE);
        parameters.remove(Authorization.CODE_VERIFIER);
        parameters.remove(Authorization.AUTHORIZATION_CONTEXT);

        connection.setParameters(parameters);
    }
```

The predefined-app block previously read `predefinedParameters.get(CLIENT_ID)`; it now reads `callbackParameters.get(CLIENT_ID)` and must guard against `null` values before building `Map.of(...)` (skip the fill when either value is null, which is the only new path reaching it: a discovery context has no predefined app).

- [ ] **Step 4: Run the integration tests**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:testIntegration --tests '*ConnectionFacadeIntTest' --offline -q`
Expected: all tests pass, including the six new ones.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply --offline -q
git add server/libs/platform/platform-connection/platform-connection-service
git commit -m "Exchange OAuth2 authorization codes with the sealed authorization context"
```

---

### Task 9: REST models — `authorizationContext` in the automation and embedded APIs

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/openapi.yaml` (schema `OAuth2AuthorizationParameters`, line ~2581)
- Regenerate: `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-api/generated/...` and `client/src/shared/middleware/platform/configuration/...`
- Create: `.../platform-configuration-rest-impl/src/test/java/com/bytechef/platform/configuration/web/rest/OAuth2ApiControllerIntTest.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/openapi.yaml` (schema `OAuth2`, line ~2466) and regenerate its Spring model and `client/src/ee/shared/middleware/embedded/public`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/mapper/ConnectedUserIntegrationMapper.java` (line ~113)

**Interfaces:**
- Consumes: `OAuth2AuthorizationParameters.authorizationContext()` (Task 1).
- Produces: REST/TS field `authorizationContext?: string` on `OAuth2AuthorizationParameters` (automation) and `OAuth2` (embedded public API).

- [ ] **Step 1: Write the failing WebMvc test**

```java
package com.bytechef.platform.configuration.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.web.rest.config.PlatformConfigurationRestTestConfiguration;
import com.bytechef.platform.configuration.web.rest.config.WorkflowConfigurationRestTestConfigurationSharedMocks;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

@ContextConfiguration(classes = PlatformConfigurationRestTestConfiguration.class)
@WebMvcTest(OAuth2ApiController.class)
@WorkflowConfigurationRestTestConfigurationSharedMocks
class OAuth2ApiControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuth2ParametersFacade oAuth2ParametersFacade;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @Test
    void testGetOAuth2AuthorizationParametersReturnsAuthorizationContext() {
        when(oAuth2ParametersFacade.getOAuth2AuthorizationParameters(
            eq("mcpClient"), eq(1), anyMap(), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)))
            .thenReturn(new OAuth2AuthorizationParameters(
                "https://auth.example.com/authorize", "reg-1", Map.of("resource", "https://mcp.example.com/mcp"),
                Map.of("read", true), "sealed-context"));

        webTestClient.post()
            .uri("/internal/oauth2/authorization-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"componentName":"mcpClient","connectionVersion":1,
                 "authorizationType":"OAUTH2_AUTHORIZATION_CODE_PKCE","parameters":{"url":"https://mcp.example.com/mcp"}}
                """)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.authorizationUrl").isEqualTo("https://auth.example.com/authorize")
            .jsonPath("$.clientId").isEqualTo("reg-1")
            .jsonPath("$.extraQueryParameters.resource").isEqualTo("https://mcp.example.com/mcp")
            .jsonPath("$.authorizationContext").isEqualTo("sealed-context");
    }
}
```

The URI prefix (`/internal`) must match the `servers:` entry of `platform-configuration-rest-impl/openapi.yaml` and the other `*ApiControllerIntTest` classes in the same package; copy theirs if it differs.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:testIntegration --tests '*OAuth2ApiControllerIntTest' --offline -q`
Expected: FAIL — `$.authorizationContext` missing from the response.

- [ ] **Step 3: Add the field to both OpenAPI specs and regenerate**

`platform-configuration-rest-impl/openapi.yaml`, inside `OAuth2AuthorizationParameters.properties` after `scopes`:

```yaml
        authorizationContext:
          description: "Opaque, server-sealed state the client must post back with the authorization code."
          type: "string"
```

`embedded-configuration-public-rest/openapi.yaml`, inside `OAuth2.properties` after `scopes`: the same three lines.

Run:

```bash
./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:generateOpenAPI :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:generateOpenAPISpring :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:generateOpenAPITypeScriptFetch --offline -q
```

Expected: `OAuth2AuthorizationParametersModel.java`, `client/src/shared/middleware/platform/configuration/models/OAuth2AuthorizationParameters.ts`, the embedded `OAuth2Model.java` and `client/src/ee/shared/middleware/embedded/public/models/OAuth2.ts` all gain `authorizationContext`. `git status` must show only those generated files plus the two specs changed (if the generator rewrites unrelated files, revert them with `git checkout -- <path>`).

`OAuth2AuthorizationParametersMapper` (platform-configuration-rest-api) is MapStruct and maps by name, so it needs no change. In `ConnectedUserIntegrationMapper` add, next to the four existing `@Mapping` lines:

```java
        @Mapping(target = "authorizationContext", source = "oAuth2.oAuth2AuthorizationParameters.authorizationContext")
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:testIntegration --tests '*OAuth2ApiControllerIntTest' :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:check --offline -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit (generated files in the same commit as their spec)**

```bash
./gradlew spotlessApply --offline -q
git add server/libs/platform/platform-configuration server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest client/src/shared/middleware/platform/configuration client/src/ee/shared/middleware/embedded/public
git commit -m "Expose OAuth2 authorizationContext in the authorization parameters APIs"
```

---

### Task 10: Client — carry `authorizationContext`, drop `code_verifier`, pin the query

**Files:**
- Modify: `client/src/shared/components/connection/oauth2/useOAuth2.ts` (`CodePayloadI` line 14, props line ~106, `extraQueryParametersRef` line ~137, `onCodeSuccess` call line ~220)
- Modify: `client/src/shared/components/connection/OAuth2Button.tsx` (props interface line 7, `useOAuth2({...})` line ~38)
- Modify: `client/src/shared/components/connection/ConnectionDialog.tsx` (`<OAuth2Button ...>` line ~903)
- Modify: `client/src/shared/queries/platform/oauth2.queries.ts`
- Test: `client/src/shared/components/connection/oauth2/tests/useOAuth2.test.ts`
- Test: `client/src/shared/components/connection/tests/OAuth2Button.test.tsx`
- Create: `client/src/shared/queries/platform/tests/oauth2.queries.test.ts`

**Interfaces:**
- Consumes: `OAuth2AuthorizationParameters.authorizationContext` from the regenerated TS model (Task 9).
- Produces: `UseOAuth2Props.authorizationContext?: string`; `CodePayloadI.authorizationContext?: string` (replaces `code_verifier`); `OAuth2ButtonProps.authorizationContext?: string`; exported `getOAuth2AuthorizationParametersQueryOptions(request, enabled)` with `staleTime: Infinity` and `refetchOnWindowFocus: false`.

- [ ] **Step 1: Write the failing tests**

Append to `useOAuth2.test.ts` inside the existing `describe`:

```ts
    it('should never put code_verifier into the authorization URL', () => {
        const props: UseOAuth2Props = {
            ...defaultProps,
            extraQueryParameters: {code_challenge: 'challenge', code_challenge_method: 'S256'},
        };

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        const openCall = vi.mocked(window.open).mock.calls[0];
        const url = new URL(openCall[0] as string);

        expect(url.searchParams.get('code_challenge')).toBe('challenge');
        expect(url.searchParams.has('code_verifier')).toBe(false);
    });

    it('should include authorizationContext in the code payload', () => {
        const onCodeSuccess = vi.fn();
        const props: UseOAuth2Props = {...defaultProps, authorizationContext: 'sealed-context', onCodeSuccess};

        const {result} = renderHook(() => useOAuth2(props));

        act(() => {
            result.current.getAuth();
        });

        const savedState = sessionStorage.getItem(OAUTH_STATE_KEY);

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {payload: {code: 'the-code', state: savedState}, type: OAUTH_RESPONSE},
                })
            );
        });

        expect(onCodeSuccess).toHaveBeenCalledWith({
            authorizationContext: 'sealed-context',
            code: 'the-code',
            state: savedState,
        });
    });
```

Append to `OAuth2Button.test.tsx` (it renders the real hook, so assert through the popup URL and payload):

```ts
    it('should forward authorizationContext into the code payload', () => {
        const onCodeSuccess = vi.fn();

        render(
            <OAuth2Button
                authorizationContext="sealed-context"
                authorizationUrl="https://auth.example.com/authorize"
                clientId="client"
                onClick={(getAuth) => getAuth()}
                onCodeSuccess={onCodeSuccess}
                redirectUri="https://bytechef.example.com/callback"
                responseType="code"
            />
        );

        screen.getByRole('button', {name: 'Connect'}).click();

        const savedState = sessionStorage.getItem('react-use-oauth2-state-key');

        window.dispatchEvent(
            new MessageEvent('message', {
                data: {payload: {code: 'c', state: savedState}, type: 'react-use-oauth2-response'},
            })
        );

        expect(onCodeSuccess).toHaveBeenCalledWith(expect.objectContaining({authorizationContext: 'sealed-context'}));
    });
```

Wrap the click and dispatch in `act(...)` if the existing tests in that file do so. Create `oauth2.queries.test.ts`:

```ts
import {describe, expect, it} from 'vitest';

import {getOAuth2AuthorizationParametersQueryOptions} from '../oauth2.queries';

describe('getOAuth2AuthorizationParametersQueryOptions', () => {
    it('never refetches once the authorization parameters were fetched', () => {
        const options = getOAuth2AuthorizationParametersQueryOptions(
            {
                authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
                componentName: 'mcpClient',
                connectionVersion: 1,
                parameters: {},
            },
            true
        );

        expect(options.staleTime).toBe(Infinity);
        expect(options.refetchOnWindowFocus).toBe(false);
        expect(options.refetchOnReconnect).toBe(false);
    });
});
```

- [ ] **Step 2: Run them to verify they fail**

Run: `cd client && npx vitest run src/shared/components/connection src/shared/queries/platform`
Expected: the three new tests fail (`code_verifier` is in the URL, `authorizationContext` undefined, query options export missing).

- [ ] **Step 3: Implement `useOAuth2.ts`**

```ts
export interface CodePayloadI {
    authorizationContext?: string;
    code: string;
    [key: string]: string | undefined;
}
```

Add `authorizationContext?: string;` to `UseOAuth2Props` (keep keys alphabetical) and destructure it. Add a ref next to `extraQueryParametersRef`:

```ts
    const authorizationContextRef = useRef(authorizationContext);
```

Replace the `onCodeSuccess({...payload, code_verifier: extraQueryParametersRef.current?.code_verifier})` call with:

```ts
                        onCodeSuccess({
                            ...payload,
                            authorizationContext: authorizationContextRef.current,
                        });
```

and in the existing `useEffect` that syncs `extraQueryParametersRef` add `authorizationContextRef.current = authorizationContext;` with `authorizationContext` in its dependency array. The URL is built from `extraQueryParametersRef.current` as before; it no longer contains the verifier because the server stopped sending it (Task 7).

- [ ] **Step 4: Implement `OAuth2Button.tsx` and `ConnectionDialog.tsx`**

`OAuth2Button.tsx`: add `authorizationContext?: string;` to `OAuth2ButtonProps`, destructure it, pass `authorizationContext,` into `useOAuth2({...})`.

`ConnectionDialog.tsx`, in the `<OAuth2Button` element: add `authorizationContext={oAuth2AuthorizationParameters.authorizationContext}` as the first prop (alphabetical).

- [ ] **Step 5: Pin the query**

`oauth2.queries.ts`:

```ts
export const getOAuth2AuthorizationParametersQueryOptions = (
    request: GetOAuth2AuthorizationParametersRequest,
    enabled?: boolean
) => ({
    queryKey: OAuth2Keys.oAuth2AuthorizationParameters(request),
    queryFn: () =>
        new Oauth2Api().getOAuth2AuthorizationParameters({
            getOAuth2AuthorizationParametersRequest: request,
        }),
    enabled: enabled === undefined ? true : enabled,
    refetchOnReconnect: false,
    refetchOnWindowFocus: false,
    staleTime: Infinity,
});

export const useGetOAuth2AuthorizationParametersQuery = (
    request: GetOAuth2AuthorizationParametersRequest,
    enabled?: boolean
) => useQuery<OAuth2AuthorizationParameters, Error>(getOAuth2AuthorizationParametersQueryOptions(request, enabled));
```

(The file already disables `sort-keys`.) Every fetch of this query registers an OAuth client on the remote server, so it must run once per OAuth step.

- [ ] **Step 6: Run the client checks**

Run: `cd client && npm run format && npm run check`
Expected: lint, typecheck and all tests pass; `grep -rn "code_verifier" src/shared/components/connection` returns nothing.

- [ ] **Step 7: Commit**

```bash
git add client/src/shared/components/connection client/src/shared/queries/platform
git commit -m "client - Carry the sealed OAuth2 authorization context through the connection dialog"
```

---

### Task 11: Embedded SDK — same pass-through, minor release

**Files:**
- Modify: `sdks/frontend/embedded/library/src/components/connect-dialog/types.ts` (`IntegrationType.connectionConfig.oauth2`, `CodePayloadI`)
- Modify: `sdks/frontend/embedded/library/src/components/connect-dialog/useOAuth2.ts`
- Modify: `sdks/frontend/embedded/library/package.json` (`"version": "0.4.0"` → `"0.5.0"`)
- Create: `sdks/frontend/embedded/library/src/components/connect-dialog/useOAuth2.test.ts`

**Interfaces:**
- Consumes: embedded public API `OAuth2.authorizationContext` (Task 9).
- Produces: `Oauth2Props.authorizationContext?: string`; `CodePayloadI.authorizationContext?: string`. `index.tsx` already spreads `integration.connectionConfig.oauth2` into `useOAuth2(...)`, so once the type carries the field it flows through with no change there.

- [ ] **Step 1: Write the failing test**

```ts
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useOAuth2 from './useOAuth2';

describe('useOAuth2 (embedded)', () => {
    beforeEach(() => {
        vi.spyOn(window, 'open').mockReturnValue({closed: false, close: vi.fn()} as unknown as Window);
    });

    afterEach(() => {
        vi.restoreAllMocks();
        sessionStorage.clear();
    });

    it('posts authorizationContext back with the authorization code', () => {
        const onCodeSuccess = vi.fn();

        const {result} = renderHook(() =>
            useOAuth2({
                authorizationContext: 'sealed-context',
                authorizationUrl: 'https://auth.example.com/authorize',
                clientId: 'client',
                extraQueryParameters: {code_challenge: 'challenge', code_challenge_method: 'S256'},
                onCodeSuccess,
                redirectUri: 'https://bytechef.example.com/callback',
                responseType: 'code',
                scope: 'read',
            })
        );

        act(() => {
            result.current.getAuth();
        });

        const popupUrl = new URL(vi.mocked(window.open).mock.calls[0][0] as string);

        expect(popupUrl.searchParams.has('code_verifier')).toBe(false);

        const savedState = sessionStorage.getItem('react-use-oauth2-state-key');

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {payload: {code: 'the-code', state: savedState}, type: 'react-use-oauth2-response'},
                })
            );
        });

        expect(onCodeSuccess).toHaveBeenCalledWith({
            authorizationContext: 'sealed-context',
            code: 'the-code',
            state: savedState,
        });
    });
});
```

If the hook validates `state` against `sessionStorage` under a different key, read the key constant from `useOAuth2.ts` (line 4) instead of the literal.

- [ ] **Step 2: Run it to verify it fails**

Run: `cd sdks/frontend/embedded/library && npx vitest run src/components/connect-dialog/useOAuth2.test.ts`
Expected: FAIL — payload lacks `authorizationContext` (and the prop is a type error).

- [ ] **Step 3: Implement**

`types.ts`: add `authorizationContext?: string;` to `connectionConfig.oauth2` and to `CodePayloadI` (keep keys alphabetical).

`useOAuth2.ts`: add `authorizationContext?: string;` to `Oauth2Props`, destructure it, keep it in `const authorizationContextRef = useRef(authorizationContext);` (synced in the existing `useEffect` that updates `extraQueryParametersRef`), and change the code branch to:

```ts
                    if (responseType === 'code' && onCodeSuccess) {
                        onCodeSuccess({
                            ...payload,
                            authorizationContext: authorizationContextRef.current,
                        });
                    }
```

`package.json`: bump `version` to `0.5.0`.

- [ ] **Step 4: Run the SDK checks**

Run: `cd sdks/frontend/embedded/library && npm run lint && npm run typecheck && npx vitest run`
Expected: all green.

- [ ] **Step 5: Commit**

```bash
git add sdks/frontend/embedded/library
git commit -m "embedded sdk - Carry the sealed OAuth2 authorization context in the connect dialog"
```

---

### Task 12: Embedded facade — skip discovery authorizations; align the spec sentence

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserIntegrationFacadeImpl.java` (line ~266, and the same block inside `toConnectedUserIntegrationDTO` ~line 398 if it computes parameters there too)
- Modify: `docs/superpowers/specs/2026-10-13-mcp-oauth-dynamic-client-registration-design.md` (section 3, "Embedded exclusion" bullet)

**Interfaces:**
- Consumes: domain `Authorization.isDiscovery()` (Task 1).
- Behaviour: the integration DTO carries `oAuth2AuthorizationParameters = null` for a discovery authorization, so no client is registered on page load and the list endpoint keeps working. The actual rejection with `OAuth2ErrorType.DISCOVERY_NOT_AVAILABLE` happens at connection creation (Task 8, `testCreateRejectsDiscoveryContextForEmbeddedConnection`).

- [ ] **Step 1: Guard the eager computation**

Change the condition at line ~270 (and its twin in `toConnectedUserIntegrationDTO`, if present) to:

```java
        if (StringUtils.startsWith(type.name(), "OAUTH2") && !authorization.isDiscovery()) {
```

Add the EE `@version ee` tag only if the class lacks it (the file already carries the ByteChef Enterprise header).

- [ ] **Step 2: Align the spec**

In the spec, replace the "Embedded exclusion" bullet with:

> **Embedded exclusion:** the embedded facade does not compute OAuth2 parameters for a discovery-mode authorization (the DTO carries `null`), so listing integrations never registers a client. A connection posted with a discovery context and `PlatformType.EMBEDDED` is rejected by `ConnectionFacadeImpl` with `OAuth2ErrorType.DISCOVERY_NOT_AVAILABLE`. Only the automation `ConnectionDialog` path runs discovery.

- [ ] **Step 3: Compile and run the EE module's existing tests**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:check --offline -q`
Expected: BUILD SUCCESSFUL. No new test here: the facade's only new branch is a null assignment, and the user-visible behaviour is pinned in Task 8.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply --offline -q
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-service docs/superpowers/specs/2026-10-13-mcp-oauth-dynamic-client-registration-design.md
git commit -m "Skip OAuth2 discovery parameters for embedded integrations"
```

---

### Task 13: `mcp-client` — "OAuth2 (automatic)" authorization and 401 translation

**Files:**
- Modify: `server/libs/modules/components/mcp-client/src/main/java/com/bytechef/component/mcp/client/connection/McpClientConnection.java` (`.authorizations(...)`, line ~60)
- Modify: `server/libs/modules/components/mcp-client/src/main/java/com/bytechef/component/mcp/client/util/McpClientUtils.java` (`getToolOptions` line ~78, `getToolProperties` line ~96, new helper)
- Modify: `server/libs/modules/components/mcp-client/src/main/java/com/bytechef/component/mcp/client/cluster/McpClientTool.java` (`apply`, line ~96) and `.../action/McpClientCallToolAction.java` (`perform`, line ~73)
- Modify: `server/libs/modules/components/mcp-client/src/test/java/com/bytechef/component/mcp/client/util/McpClientUtilsTest.java`
- Delete then regenerate: `server/libs/modules/components/mcp-client/src/test/resources/definition/mcp-client_v1.json`, `server/libs/modules/components/mcp-client/build/resources/test/definition/`
- Regenerate: `docs/content/docs/reference/components/mcp-client_v1.mdx`

**Interfaces:**
- Consumes: `ComponentDsl.ModifiableAuthorization.protectedResourceUrl(...)` (Task 1); MCP SDK 2.0.0 `io.modelcontextprotocol.client.transport.McpHttpClientTransportAuthorizationException` (has `getResponseInfo().statusCode()`, extends `McpTransportException extends RuntimeException`).
- Produces: `public static <T> T translateAuthorizationErrors(Supplier<T> mcpCall)` in `McpClientUtils` — rethrows a `ProviderException(statusCode, message)` when the thrown exception or any cause is `McpHttpClientTransportAuthorizationException`, otherwise rethrows unchanged. `RefreshCredentialsUtils.matches` walks the cause chain for a `ProviderException` with a status code, which is what makes `TokenRefreshAspect` refresh and retry.

- [ ] **Step 1: Write the failing tests**

Add to `McpClientUtilsTest`:

```java
    @Test
    void testTranslateAuthorizationErrorsMapsSdk401ToProviderException() {
        McpHttpClientTransportAuthorizationException authorizationException = authorizationException(401);

        assertThatThrownBy(() -> McpClientUtils.translateAuthorizationErrors(() -> {
            throw authorizationException;
        }))
            .isInstanceOf(ProviderException.class)
            .extracting("statusCode")
            .isEqualTo(401);
    }

    @Test
    void testCreateMcpSyncClientTranslatesWrappedAuthorizationError() {
        RuntimeException wrapped = new RuntimeException("reactor wrapper", authorizationException(403));

        assertThatThrownBy(() -> McpClientUtils.translateAuthorizationErrors(() -> {
            throw wrapped;
        }))
            .isInstanceOf(ProviderException.class)
            .extracting("statusCode")
            .isEqualTo(403);
    }

    @Test
    void testTranslateAuthorizationErrorsLeavesOtherExceptionsAlone() {
        IllegalStateException other = new IllegalStateException("boom");

        assertThatThrownBy(() -> McpClientUtils.translateAuthorizationErrors(() -> {
            throw other;
        })).isSameAs(other);
    }

    @Test
    void testCreateMcpSyncClientAppliesBearerTokenForDiscoveryConnection() {
        assertAppliedAuthorizationType(
            Map.of(
                Authorization.ACCESS_TOKEN, "access-token",
                Authorization.AUTHORIZATION_URL, "https://auth.example.com/authorize",
                Authorization.TOKEN_URL, "https://auth.example.com/token",
                Authorization.CLIENT_ID, "reg-1",
                Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "none",
                Authorization.RESOURCE, "https://mcp.example.com/mcp"),
            AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
    }

    private static McpHttpClientTransportAuthorizationException authorizationException(int statusCode) {
        HttpResponse.ResponseInfo responseInfo = mock(HttpResponse.ResponseInfo.class);

        when(responseInfo.statusCode()).thenReturn(statusCode);

        return new McpHttpClientTransportAuthorizationException(
            "Authorization error", new HttpRequestSnapshot(URI.create("https://mcp.example.com/mcp"), "POST",
                HttpHeaders.of(Map.of(), (name, value) -> true)),
            responseInfo);
    }
```

Imports: `com.bytechef.component.exception.ProviderException`, `io.modelcontextprotocol.client.transport.HttpRequestSnapshot`, `io.modelcontextprotocol.client.transport.McpHttpClientTransportAuthorizationException`, `java.net.URI`, `java.net.http.HttpHeaders`, `java.net.http.HttpResponse`, `static org.assertj.core.api.Assertions.assertThatThrownBy`. `assertAppliedAuthorizationType` already exists in this test class (added with the client-credentials fix).

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:libs:modules:components:mcp-client:test --tests '*McpClientUtilsTest' --offline -q`
Expected: compilation failure (`translateAuthorizationErrors` missing); the discovery-connection test passes already (its parameters match the existing authorization-code inference) and stays as a regression guard.

- [ ] **Step 3: Add the authorization and the translation**

`McpClientConnection`: insert as the **first** entry of `.authorizations(...)`:

```java
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .title("OAuth2 (automatic)")
                .description(
                    "Sign in to the MCP server directly. ByteChef discovers the authorization server and registers " +
                        "itself; no client id or secret is needed.")
                .protectedResourceUrl((connectionParameters, context) -> connectionParameters.getRequiredString(URL)),
```

Rename the existing `"OAuth2 Authorization Code"` title to `"OAuth2 (manual)"`.

`McpClientUtils`:

```java
    public static <T> T translateAuthorizationErrors(Supplier<T> mcpCall) {
        try {
            return mcpCall.get();
        } catch (RuntimeException runtimeException) {
            Throwable throwable = runtimeException;

            while (throwable != null) {
                if (throwable instanceof McpHttpClientTransportAuthorizationException authorizationException) {
                    HttpResponse.ResponseInfo responseInfo = authorizationException.getResponseInfo();

                    throw new ProviderException(responseInfo.statusCode(), authorizationException.getMessage());
                }

                throwable = throwable.getCause();
            }

            throw runtimeException;
        }
    }
```

Wrap the MCP calls: in `getToolOptions` and `getToolProperties` wrap the body of each `try (McpSyncClient ...)` block's `initialize()` + `listTools()` in `translateAuthorizationErrors(() -> { ... })`; in `McpClientTool.apply` change `mcpSyncClient.initialize();` to `McpClientUtils.translateAuthorizationErrors(mcpSyncClient::initialize);`; in `McpClientCallToolAction.perform` wrap `initialize()` and `callTool(...)` the same way. Imports: `java.util.function.Supplier`, `java.net.http.HttpResponse`, `com.bytechef.component.exception.ProviderException`, `io.modelcontextprotocol.client.transport.McpHttpClientTransportAuthorizationException`.

- [ ] **Step 4: Regenerate the definition snapshot and run the module check**

```bash
rm -f server/libs/modules/components/mcp-client/src/test/resources/definition/mcp-client_v1.json
rm -rf server/libs/modules/components/mcp-client/build/resources/test/definition
./gradlew :server:libs:modules:components:mcp-client:check --offline -q
```

Expected: BUILD SUCCESSFUL; the regenerated `mcp-client_v1.json` lists `OAUTH2_AUTHORIZATION_CODE_PKCE` with title `OAuth2 (automatic)` first and no properties.

- [ ] **Step 5: Regenerate the component reference docs**

Run: `./gradlew generateDocumentation --offline -q`
Expected: `docs/content/docs/reference/components/mcp-client_v1.mdx` gains an "OAuth2 (automatic)" connection section with an empty properties table and the renamed "OAuth2 (manual)" section. Revert any unrelated doc files the task rewrote (`git status` should show only `mcp-client_v1.mdx` under `docs/`).

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply --offline -q
git add server/libs/modules/components/mcp-client docs/content/docs/reference/components/mcp-client_v1.mdx
git commit -m "Add automatic OAuth2 authorization to the MCP Client component"
```

---

### Task 14: Whole-branch gates and live check

**Files:** none new.

- [ ] **Step 1: Server checks on every touched module**

```bash
./gradlew spotlessCheck \
  :sdks:backend:java:component-api:check \
  :server:libs:platform:platform-component:platform-component-api:check \
  :server:libs:platform:platform-component:platform-component-service:check \
  :server:libs:platform:platform-oauth2:platform-oauth2-api:check \
  :server:libs:platform:platform-oauth2:platform-oauth2-service:check \
  :server:libs:platform:platform-connection:platform-connection-service:check \
  :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:check \
  :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:check \
  :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:check \
  :server:libs:modules:components:mcp-client:check --offline -q
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Integration tests (separate task type, not covered by `check`)**

```bash
./gradlew :server:libs:platform:platform-component:platform-component-service:testIntegration \
  :server:libs:platform:platform-connection:platform-connection-service:testIntegration \
  :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:testIntegration --offline -q
```

Expected: BUILD SUCCESSFUL (Docker must be running for Testcontainers).

- [ ] **Step 3: Client and SDK**

```bash
cd client && npm run check && cd ../sdks/frontend/embedded/library && npm run lint && npm run typecheck && npx vitest run
```

Expected: all green; `grep Unhandled` on the vitest output returns nothing.

- [ ] **Step 4: Boot smoke and live sign-in**

Start the infra and server (`cd server && docker compose -f docker-compose.dev.infra.yml up -d && cd .. && ./gradlew -p server/apps/server-app bootRun`) and the client (`cd client && npm run dev`). In Connections → New → MCP Client, pick **OAuth2 (automatic)**, enter `https://ai.todoist.net/mcp` (public client) and sign in; repeat with `https://mcp.figma.com/mcp` (confidential client, `client_secret_basic`). For each: the connection is created, `parameters` in the `connection` table contain `clientId`, `tokenEndpointAuthMethod`, `resource` and `access_token` but no `code`, `code_verifier` or `authorizationContext`, and the MCP Client tool options list the server's tools. Also confirm an existing PKCE component (Canva) still connects and that `code_verifier` is absent from its popup URL (browser dev tools, Network tab).

- [ ] **Step 5: Final review**

Run the repo's code-review skill over `master..HEAD` and address findings before opening a PR. Do not push or open a PR without the user's explicit go-ahead.

---

## Self-Review Notes

- **Spec coverage:** §1 component API → Task 1; §2 platform units → Tasks 3–6; §3 parameters + embedded exclusion + query pinning → Tasks 7, 10, 12; §4 exchange/persist/strip → Task 8; §5 refresh → Task 7; §6 client and SDK → Tasks 9–11; §7 mcp-client → Task 13; Security (SSRF, metadata validation, sealed context) → Tasks 3, 4, 6; Error handling table → `OAuth2ErrorType` (Task 2) with messages in Tasks 3–8; Testing gates → Task 14; docs regeneration → Task 13.
- **Type consistency:** `OAuth2AuthorizationContext.ofPkce(componentName, connectionVersion, codeVerifier)` and `ofDiscovery(componentName, connectionVersion, codeVerifier, RegisteredClient, DiscoveredAuthorizationServer)` are used identically in Tasks 2, 6, 7, 8. `RegisteredClient(clientId, clientSecret, tokenEndpointAuthMethod)` has three components everywhere. `OAuth2DiscoveryService.register(DiscoveredAuthorizationServer)` takes one argument; the registrar's `register(server, redirectUri)` takes two.
- **Known plan deviation from the spec:** the embedded facade *skips* parameter computation rather than throwing, so the integrations list stays usable; the rejection moves to connection creation. Task 12 updates the spec to say so.
- **Deployment note:** `ConnectionDefinitionServiceImpl` uses `ObjectProvider` because `worker-app` and `runtime-job-app` ship `platform-component-service` without `platform-oauth2-service`; `ConnectionFacadeImpl` uses a required dependency because both apps that ship it (`server-app`, `connection-app`) include `platform-oauth2-service`.
