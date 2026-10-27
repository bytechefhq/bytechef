# Microservices Plan 3b-5: Embedded API Keys, Connected Users and Principal Kinds over gRPC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An embedded API key or a connected-user token (JWT) authenticates a request on execution-app and webhook-app the same way it does on configuration-app, and the caller keeps its kind (web user, API key user, connected user) on every gRPC call it causes, so connected-user checks still run on the receiving app.

**Architecture:** Task 1 extends the 3b-2 user-context interceptors in `remote-grpc` with a principal kind and the ids needed to rebuild the caller as an `AbstractApiKeyAuthenticationToken` or a `ConnectedUserAuthentication` on the receiving app. Task 2 adds `embedded-security-remote-*` modules so an app without the signing-key tables can look up the public key of a connected-user token on configuration-app. Task 3 puts the existing embedded API-key security (`embedded-security-web-impl`) on execution-app and webhook-app, fed by remote clients. Task 4 is the live run through the gateway.

**Tech Stack:** gRPC 1.83.1, protobuf 4.35.1, Spring Boot 4.1.1, Spring Security, JJWT, remote-grpc helpers (`GrpcChannels`, `FixedGrpcChannels`, `InProcessGrpc`, `GrpcRemoteExceptionHandler`), grpc-inprocess for wire tests, Testcontainers for app IntTests.

**Spec:** `docs/superpowers/specs/2026-10-25-microservices-compose-design.md`, decision "Per-request token auth stays put: API keys, embedded connected-user auth and MCP OAuth tokens authenticate each request on the service that receives it, as today." Pattern: plans 3b-2 (identity interceptors) and 3b-3 (remote modules).

## Global Constraints

- gRPC between services only; no REST. Per area: `*-remote-grpc-proto`, `*-remote-grpc` (servers + `@Configuration @ConditionalOnClass(name = "<impl class>")`), clients in `*-remote-client` (`@Component @ConditionalOnEEVersion`, methods no app calls throw `UnsupportedOperationException`).
- Identity metadata is trusted only because `ServiceTokenServerInterceptor` has already rejected calls without the shared token.
- No change in behavior of the monolith or the #6616 cluster: nothing here is added to `server-app`.
- Files under `server/ee/` carry the Enterprise license header and `@version ee`; CE files keep Apache 2.0. No prose comments in code or protos. Config defaults in yml. Blank line before control statements and after a variable modification; no chained calls on method results except builders, streams, Optional and assertions; descriptive names.
- Commits: subject only, no ticket number, commit by path, verify the branch before each commit, never amend, never push.

## Decided here (Claude's calls, open to override)

- **Neutral rebuilt tokens.** The receiving app rebuilds callers as two new classes in `remote-grpc`, not as `EmbeddedApiKeyAuthenticationToken`/`PlatformApiKeyAuthenticationToken`, so coordinator-app and worker-app need no embedded or platform security modules. Every downstream check reads `ConnectedUserAuthentication` (`ConnectedUserAuthentications`, `AutomationMethodSecurityExpressionRoot`) or `AbstractApiKeyAuthenticationToken.getEnvironmentId()` (`ConnectedUserIntegrationInstanceFacadeImpl`). The one `instanceof EmbeddedApiKeyAuthenticationToken` check (`EmbeddedAutomationAuthorizationSkipFilter`) is an HTTP filter and never sees a gRPC call.
- **Kinds.** `user` (3b-2 behaviour: `UsernamePasswordAuthenticationToken`, `RememberMeAuthenticationToken`), `api-key` (any other authenticated `AbstractApiKeyAuthenticationToken`: platform, automation, API platform), `connected-user` (any authenticated `ConnectedUserAuthentication`; checked first, because `EmbeddedApiKeyAuthenticationToken` is both). OAuth2/MCP `JwtAuthenticationToken` and anything else still carries no identity.
- **Fail closed on bad metadata.** A login without a kind, an unknown kind, or a missing or non-numeric id binds no identity, exactly like a call without a user. A user check on the receiving app then denies.
- **Public keys travel as X.509 bytes, looked up per request.** No cache on the calling app; a connected-user JWT costs at most two lookups (the JWT service's in-memory keys, then the signing keys table), both on configuration-app, in the same order as `EmbeddedApiKeyAuthenticationConverter.SigningKeyLocator`. Keys the embedded MCP facade signs live in configuration-app's local cache, so they verify only while configuration-app runs one instance.

## Review Focus

1. A connected user arriving over gRPC is a `ConnectedUserAuthentication` with the same external user id, connected user id, environment and API-key flag, not a plain user. Pinned in Task 1 (`testConnectedUserIsRebuiltAsConnectedUser`, `testConnectedUserSurvivesASecondHop`).
2. Missing, unknown or malformed principal metadata binds no identity. Pinned in Task 1 (`testLoginWithoutKindBindsNoIdentity`, `testUnknownKindBindsNoIdentity`, `testMalformedConnectedUserIdBindsNoIdentity`).
3. A wrong API key or a JWT signed with the wrong key gets 401 on execution-app and webhook-app. Pinned in Task 3 (`testInvalidApiKeyIsRejected`, `testTokenWithWrongSignatureIsRejected`).
4. A JWT for one external user cannot act on another user's path. Pinned in Task 3 (`testTokenForAnotherUserIsRejected`).
5. An unknown signing key id is a failure, never a success. Pinned in Task 2 (`testUnknownSigningKeyFailsWithItsMessage`).

---

### Task 1: Principal kinds over gRPC

**Files:**
- Modify: `server/ee/libs/core/remote/remote-grpc/build.gradle.kts` (`implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))`, `testImplementation(testFixtures(project(":server:libs:platform:platform-security-web:platform-security-web-api")))`)
- Modify: `.../remote/grpc/interceptor/GrpcMetadataKeys.java`, `UserContextClientInterceptor.java`, `UserContextServerInterceptor.java`
- Create: `.../remote/grpc/security/RemoteApiKeyAuthenticationToken.java`, `.../remote/grpc/security/RemoteConnectedUserAuthenticationToken.java`
- Test: `src/test/java/com/bytechef/ee/remote/grpc/interceptor/UserContextServerInterceptorTest.java`, `src/test/java/com/bytechef/ee/remote/grpc/EchoService.java`

**Interfaces:**
- Produces (metadata keys, ASCII marshaller): `PRINCIPAL_KIND` = `x-bytechef-principal-kind` (`user` | `api-key` | `connected-user`), `ENVIRONMENT_ID` = `x-bytechef-environment-id`, `CONNECTED_USER_ID` = `x-bytechef-connected-user-id`, `API_KEY_AUTHENTICATED` = `x-bytechef-api-key-authenticated` (`true` | `false`). `USER_LOGIN` carries the username, or the external user id for `connected-user`; `USER_AUTHORITY` is sent for `user` and `api-key` only.
- Produces: `RemoteApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken` with `RemoteApiKeyAuthenticationToken(long environmentId, org.springframework.security.core.userdetails.User user)`.
- Produces: `RemoteConnectedUserAuthenticationToken extends AbstractApiKeyAuthenticationToken implements ConnectedUserAuthentication` with `RemoteConnectedUserAuthenticationToken(long environmentId, long connectedUserId, User user, boolean apiKeyAuthenticated)`; `externalUserId()` is `user.getUsername()`; the user has no authorities, as in `EmbeddedApiKeyAuthenticationProvider`.

- [ ] **Step 1: Add the echo method and the failing tests.** `EchoService` gets `ECHO_PRINCIPAL` and `echoPrincipal(Channel)` answering `describePrincipal(SecurityContextHolder.getContext().getAuthentication())`: `connected-user|<externalUserId>|<connectedUserId>|<environmentId>|<apiKeyAuthenticated>` for a `ConnectedUserAuthentication`, `api-key|<login>|<environmentId>|<authorities joined by ,>` for an `AbstractApiKeyAuthenticationToken`, otherwise `describeUser(...)`. New tests in `UserContextServerInterceptorTest`:

```java
@Test void testConnectedUserIsRebuiltAsConnectedUser()      // TestConnectedUserAuthentication("external-user-1", 7L, 2L, true) -> "connected-user|external-user-1|7|2|false"
@Test void testConnectedUserSurvivesASecondHop()            // new RemoteConnectedUserAuthenticationToken(2L, 7L, new User("external-user-1", "", List.of()), true) -> "connected-user|external-user-1|7|2|true"
@Test void testUnauthenticatedConnectedUserCarriesNoIdentity() // TestConnectedUserAuthentication(..., false) -> echoUser isEmpty()
@Test void testApiKeyUserKeepsEnvironmentAndAuthorities()  // new RemoteApiKeyAuthenticationToken(1L, new User("api@localhost.com", "", [ROLE_USER])) -> "api-key|api@localhost.com|1|ROLE_USER"
@Test void testLoginWithoutKindBindsNoIdentity()            // test client interceptor puts USER_LOGIN only -> echoUser isEmpty()
@Test void testUnknownKindBindsNoIdentity()                 // PRINCIPAL_KIND "robot" + USER_LOGIN -> echoUser isEmpty()
@Test void testMalformedConnectedUserIdBindsNoIdentity()    // kind connected-user, CONNECTED_USER_ID "seven" -> echoUser isEmpty()
```

The three raw-metadata tests pass their own `ClientInterceptor` instead of `UserContextClientInterceptor` to `InProcessGrpc`. Existing tests stay; `testApiKeyPrincipalCarriesNoIdentity` (a `TestingAuthenticationToken`) still expects no identity.

- [ ] **Step 2: Run them.** `./gradlew :server:ee:libs:core:remote:remote-grpc:test --tests '*UserContextServerInterceptorTest'`. Expected: compile failure on the two token classes, then, once they exist as empty shells, the new tests FAIL (empty echo or `user|...`).

- [ ] **Step 3: Implement.** Client: pick the kind (connected-user, then api-key, then user) only when the authentication is authenticated; put `PRINCIPAL_KIND` plus that kind's fields. Server: read the kind; bind `UsernamePasswordAuthenticationToken.authenticated(login, null, authorities)` for `user`, `RemoteApiKeyAuthenticationToken` for `api-key`, `RemoteConnectedUserAuthenticationToken` for `connected-user`; bind nothing on any missing or unparsable field. Keep the `onHalfClose` bind-and-restore.

- [ ] **Step 4: Run the module.** `./gradlew :server:ee:libs:core:remote:remote-grpc:spotlessApply :server:ee:libs:core:remote:remote-grpc:check`. Expected: BUILD SUCCESSFUL, all interceptor tests PASS.

- [ ] **Step 5: Commit.** `Carry API key users and connected users across gRPC as their own kinds`

### Task 2: Signing-key and JWT public keys over gRPC

**Files:**
- Create modules (copy `embedded-connected-user-remote-*` build files; register all three in `settings.gradle.kts` next to them):
  - `server/ee/libs/embedded/embedded-security/embedded-security-remote-grpc-proto` with `src/main/proto/embedded_security_remote_service.proto` (package `bytechef.embedded.security.remote.v1`, `java_package` `com.bytechef.ee.embedded.security.remote.grpc.proto`, `java_multiple_files`)
  - `server/ee/libs/embedded/embedded-security/embedded-security-remote-grpc`: `SigningKeyRemoteServiceGrpcServer`, `JwtTokenRemoteServiceGrpcServer`, `EmbeddedSecurityGrpcServerConfiguration` `@ConditionalOnClass(name = "com.bytechef.ee.embedded.security.service.SigningKeyServiceImpl")`, registered in `AutoConfiguration.imports` like its neighbours
  - `server/ee/libs/embedded/embedded-security/embedded-security-remote-client`: `RemoteSigningKeyServiceClient implements SigningKeyService`, `RemoteJwtTokenServiceClient implements JwtTokenService`, channel `configuration-app`
- Modify: `server/ee/apps/configuration-app/build.gradle.kts` (`embedded-security-remote-grpc`), `ConfigurationApplicationIntTest` (served-services assertion)
- Test: `embedded-security-remote-client/src/test/java/.../EmbeddedSecurityRemoteServiceGrpcWireTest.java`

**Interfaces:**
- Proto: `service SigningKeyRemoteService { rpc GetPublicKey(SigningKeyPublicKeyRequest) returns (PublicKeyResponse); }`, `service JwtTokenRemoteService { rpc GetPublicKey(JwtPublicKeyRequest) returns (PublicKeyResponse); }`, `message SigningKeyPublicKeyRequest { string key_id = 1; int64 environment_id = 2; }`, `message JwtPublicKeyRequest { string key_id = 1; }`, `message PublicKeyResponse { optional bytes encoded = 1; }` (`PublicKey.getEncoded()`, X.509, RSA).
- Produces: `RemoteSigningKeyServiceClient.getPublicKey(String keyId, long environmentId)` and `RemoteJwtTokenServiceClient.getPublicKey(String keyId)` (null when absent), decoding with `KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded))`. Every other method of both interfaces throws `UnsupportedOperationException`.

- [ ] **Step 1: Write the failing wire tests** (in-process server with mocked `SigningKeyService`/`JwtTokenService`, a 2048-bit RSA key generated in the test):

```java
@Test void testSigningKeyPublicKeyRoundTrip()          // getPublicKey("kid-1", 2L).getEncoded() equals the mock key's encoded bytes
@Test void testUnknownSigningKeyFailsWithItsMessage()  // mock throws IllegalArgumentException("Signing key not found for keyId: kid-x") -> assertThatThrownBy(...).hasMessageContaining("Signing key not found")
@Test void testJwtPublicKeyRoundTrip()                 // getPublicKey("kid-1") encoded bytes equal
@Test void testMissingJwtPublicKeyIsNull()             // mock returns null -> client returns null
```

- [ ] **Step 2: Run them.** `./gradlew :server:ee:libs:embedded:embedded-security:embedded-security-remote-client:test`. Expected: FAIL (clients throw `UnsupportedOperationException`).

- [ ] **Step 3: Implement the proto, servers, configuration and clients.**

- [ ] **Step 4: Add the served-services assertion** to `ConfigurationApplicationIntTest`: `contains("bytechef.embedded.security.remote.v1.SigningKeyRemoteService", "bytechef.embedded.security.remote.v1.JwtTokenRemoteService")`. Watch it fail before the configuration-app dependency is added, then pass.

- [ ] **Step 5: Run checks.** `spotlessApply` and `check` on the three new modules; `:server:ee:apps:configuration-app:testIntegration`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit.** `Serve signing-key and JWT public keys over gRPC`

### Task 3: Embedded API-key and connected-user authentication on execution-app and webhook-app

**Files:**
- Modify: `server/ee/apps/execution-app/build.gradle.kts`, `server/ee/apps/webhook-app/build.gradle.kts`: add `:server:ee:libs:embedded:embedded-security-web:embedded-security-web-impl`, `:server:ee:libs:embedded:embedded-security:embedded-security-remote-client`, `:server:ee:libs:platform:platform-security:platform-security-remote-client` (`ApiKeyService`). `ConnectedUserService` and `EnvironmentService` already come from `embedded-connected-user-remote-client` and `automation-configuration-remote-client`.
- Create: `server/ee/apps/execution-app/src/test/java/com/bytechef/execution/ExecutionEmbeddedAuthenticationIntTest.java`, `server/ee/apps/webhook-app/src/test/java/com/bytechef/webhook/WebhookEmbeddedAuthenticationIntTest.java`

**Interfaces:**
- Consumes: Task 2's clients (as `SigningKeyService`/`JwtTokenService` beans).
- Requests: header `Authorization: Bearer <token>`, header `X-ENVIRONMENT: PRODUCTION`. An API key is `TenantKey.of("public").toString()`. A JWT has header `kid` = `TenantKey.of("public").toString()`, subject `external-user-1`, claim `environmentId` = `Environment.PRODUCTION.ordinal()`, signed RS256 with a test RSA key.
- Paths: execution-app `GET /api/embedded/v1/external-user-1/tools`; webhook-app `POST /api/embedded/v1/external-user-1/app-events` with body `{}`.

- [ ] **Step 1: Write the failing IntTests** (same setup as `ExecutionSessionIntTest`: `@AutoConfigureMockMvc`, Postgres and Redis containers; `@MockitoBean` for `ApiKeyService`, `ConnectedUserService`, `SigningKeyService`, `JwtTokenService`; an enabled `ConnectedUser` with external id `external-user-1` returned by `fetchConnectedUser("external-user-1", Environment.PRODUCTION.ordinal())`). Status helper returns 500 on `ServletException`, as in `ExecutionSessionIntTest.anonymousStatus`. Both classes:

```java
@Test void testRequestWithoutCredentialsIsRejected()   // status 401
@Test void testValidApiKeyIsAccepted()                 // apiKeyService.exists(key, PRODUCTION.ordinal()) true -> status isNotIn(401, 403)
@Test void testInvalidApiKeyIsRejected()               // exists false -> 401
@Test void testConnectedUserTokenIsAccepted()          // jwtTokenService.getPublicKey(kid) null, signingKeyService.getPublicKey(kid, PRODUCTION.ordinal()) the test key -> isNotIn(401, 403)
@Test void testTokenWithWrongSignatureIsRejected()     // signed with another key -> 401
@Test void testTokenForAnotherUserIsRejected()         // subject external-user-2 on the external-user-1 path -> 401
```

- [ ] **Step 2: Run them.** `./gradlew :server:ee:apps:execution-app:testIntegration --tests '*EmbeddedAuthenticationIntTest'` (and webhook-app). Expected: the accept tests FAIL with 401, because neither app authenticates API keys yet.

- [ ] **Step 3: Add the dependencies.** If the context fails on a missing bean, add only the remote client that supplies it and record a ruling.

- [ ] **Step 4: Run all app tests.** `:server:ee:apps:execution-app:testIntegration`, `:server:ee:apps:webhook-app:testIntegration`, `check` on both. Expected: BUILD SUCCESSFUL; the session IntTests still pass.

- [ ] **Step 5: Commit.** `Authenticate embedded API keys and connected users on execution-app and webhook-app`

### Task 4: Live run

- [ ] **Step 1: Rebuild** with the scratchpad `rebuild.sh`. Expected: every container healthy.
- [ ] **Step 2: Create an embedded API key** for the Production environment and a connected user `live-user` through the UI or its GraphQL as `ms-admin@localhost.com`.
- [ ] **Step 3: Call through the gateway:**
  - `GET http://localhost:8080/api/embedded/v1/live-user/tools` with the key and `X-ENVIRONMENT: PRODUCTION`. Expected: 200 and a JSON list.
  - The same with a wrong key. Expected: 401.
  - The same with a connected-user JWT: create a signing key in the UI, sign a JWT with its private key (`node:crypto`, RS256, `kid` = the key id). Expected: 200.
  - `POST /api/embedded/v1/live-user/app-events` with the key. Expected: not 401/403.
- [ ] **Step 4: Fix every unimplemented remote call the run hits** the way the sync-webhook fixes did: a wire test that fails on the stub, the RPC, one commit each (`... over gRPC`), rebuild, call again. Record each as a ruling (RPC outside this plan's list).
- [ ] **Step 5: Confirm the kind travelled.** With JFR or a debug log on configuration-app, check that a gRPC call caused by the connected-user request ran with a `RemoteConnectedUserAuthenticationToken` (for example in `ConnectedUserAuthentications.fetchCurrent`). Record the evidence in the ledger.
