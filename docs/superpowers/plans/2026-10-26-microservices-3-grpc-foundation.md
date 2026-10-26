# Microservices Plan 3: gRPC Foundation and First Services Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Services call each other over gRPC through one shared foundation, and the three calls plan 2 left blocked work: user lookups (configuration-app's permission checks), API key checks and embedded connected users (execution-app and webhook-app API-key auth).

**Architecture:** A new EE module `remote-grpc` holds everything shared: properties, the internal service token and tenant interceptors, a channel factory (TLS unless the trusted-network switch is on), a Redis discovery name resolver that dials each instance's registered gRPC port, and server-side startup checks. Each service then adds only a `*-remote-grpc-proto` module (the `.proto`), a `*-remote-grpc` module (mapper, server, server registration) and gRPC methods in its `*-remote-client`. The gateway serves users and authorities; configuration-app serves API keys and connected users.

**Tech Stack:** Java 25, Spring Boot 4.1.1 with its gRPC starters (`org.springframework.boot:spring-boot-starter-grpc-server` / `-grpc-client`, versions from the Boot BOM), grpc-java, protobuf, the `com.google.protobuf` Gradle plugin, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-10-25-microservices-compose-design.md`. Template: POC commit `f3da212d18a` on `0_732` ("Add gRPC remote-transport POC for ConnectionService"); read it with `git show f3da212d18a`.

## Global Constraints

- All work is EE; files under `server/ee/` use the Enterprise license header and Java classes get `@version ee`.
- gRPC replaces REST between services; no transport flag. This plan does not port the 32 existing REST clients or delete `*-remote-rest` (plan 3b); it adds no `bytechef.remote.transport` property.
- TLS is the default. Plaintext only with the explicit switch `bytechef.remote.grpc.plaintext=true` (the "trusted network" switch), which compose, dev and tests set. The POC's loopback-only rule is dropped: it fails between containers.
- Spring Boot 4.1.1 manages gRPC: use `spring-boot-starter-grpc-server` / `-grpc-client` and the Boot-managed grpc/protobuf versions; do not add `org.springframework.grpc:*` 1.0.x (it supports Boot below 4.1 only).
- Passwords and TOTP secrets never cross the wire.
- No change in behavior of the monolith or the #6616 cluster.
- Commit messages are subject only, no ticket number. Commit by path. Never push. No em dashes in user-facing prose. Generated proto sources are build output, never committed.

## Out of scope (plan 3b)

- Porting the 32 working REST remote clients (including the POC's `ConnectionService`) and deleting the 11 `*-remote-rest` modules.
- Other stub calls the milestone 1 flow needs (found by running it in plan 4).

## Review Focus

1. A remote `fetch*` for a missing row returns `Optional.empty()` on the client, not an exception; a remote `get*` for a missing row throws. Pinned in Task 2 (`testFetchUserByLoginReturnsEmptyWhenMissing`, `testGetUserThrowsWhenMissing`).
2. A call made with no tenant bound (single tenant) and one with a tenant bound both reach the server with the right tenant. Pinned in Task 1 (`testTenantIsBoundOnServer`, `testNoTenantLeavesContextUnset`).
3. A server whose service token is blank refuses to start; a client call without the right token gets `UNAUTHENTICATED`. Pinned in Task 1.
4. An instance registered without a gRPC port is skipped by the resolver, not dialed on its HTTP port. Pinned in Task 1 (`testInstanceWithoutGrpcPortIsSkipped`).
5. `getCurrentUser()` on the remote user client resolves the login from the caller's own security context, not from the server's. Pinned in Task 2 (`testGetCurrentUserUsesCallerLogin`).

---

### Task 1: Shared gRPC foundation module

**Files:**
- Modify: `gradle/libs.versions.toml` (protobuf Gradle plugin alias `com-google-protobuf`; `protoc` and `protoc-gen-grpc-java` versions equal to the Boot-managed protobuf and grpc versions, read with `./gradlew :server:ee:libs:core:remote:remote-grpc:dependencyInsight --dependency grpc-stub`)
- Modify: `settings.gradle.kts`
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (`Remote` with nested `Grpc`: `String serviceToken`, `boolean plaintext`, `Map<String, String> targets`; no initializers on values)
- Modify: `server/ee/libs/config/microservice-config/src/main/resources/config/microservice.yml` (`bytechef.remote.grpc.plaintext: false`, `service-token:` empty), `microservice-dev.yml` (`plaintext: true`, a fixed dev token)
- Create module `server/ee/libs/core/remote/remote-grpc`, package `com.bytechef.ee.remote.grpc`: `GrpcMetadataKeys`, `ServiceTokenClientInterceptor`, `ServiceTokenServerInterceptor`, `TenantContextClientInterceptor`, `TenantContextServerInterceptor` (from the POC, renamed), `GrpcChannels` (interface), `ManagedGrpcChannels`, `DiscoveryClientNameResolver`, `DiscoveryClientNameResolverProvider`, `GrpcRemoteConfiguration`, `GrpcPortMetadataRegistrar`
- Tests in the same module

**Interfaces:**
- Produces:
  - `interface GrpcChannels { Channel getChannel(String serviceId); }`. `ManagedGrpcChannels` (`@Component`) caches one `ManagedChannel` per service id; target is `bytechef.remote.grpc.targets.<serviceId>` when set, else `discovery:///<serviceId>`; `round_robin`; TLS unless `plaintext`; intercepted by both client interceptors; channels shut down on context close.
  - `GrpcMetadataKeys.SERVICE_TOKEN` = key `x-bytechef-service-token`; `GrpcMetadataKeys.TENANT_ID` = key `x-bytechef-tenant-id` (gRPC keys are lowercase ASCII).
  - `GrpcRemoteConfiguration` registers both server interceptors as global server interceptors (the Boot gRPC server starter applies them to every `BindableService` bean; verify the annotation/bean type it uses in the starter) and fails startup when a `BindableService` bean exists and the token is blank, or when `plaintext` is false and `spring.grpc.server.ssl.bundle` is unset.
  - `GrpcPortMetadataRegistrar` registers metadata `grpc-port` = the started server's port through `ServiceMetadataRegistry` (discovery module) when the gRPC server starts.
  - `DiscoveryClientNameResolver` resolves `discovery:///<serviceId>` to `host:grpc-port` of each instance, skipping instances without `grpc-port`; `UNAVAILABLE` when none remain.

- [ ] **Step 1: Write the failing tests**
- `ServiceTokenServerInterceptorTest`: `testMatchingTokenPasses`, `testMissingTokenIsUnauthenticated`, `testWrongTokenIsUnauthenticated`, `testBlankConfiguredTokenRejectsEverything`.
- `TenantContextServerInterceptorTest`: `testTenantIsBoundOnServer` (client binds `000002`, server handler sees `TenantContext.getCurrentTenantId()` = `000002`, cleared after the call), `testNoTenantLeavesContextUnset`.
- `GrpcRemoteWireTest`: in-process server running the standard gRPC health service (`io.grpc:grpc-services` `HealthStatusManager`) with both server interceptors; a client through `TenantContextClientInterceptor` + `ServiceTokenClientInterceptor` gets `SERVING`; with a wrong token gets `UNAUTHENTICATED`.
- `DiscoveryClientNameResolverTest`: `testResolvesGrpcPortFromMetadata` (instance `10.0.0.5:8080` with `grpc-port=9090` resolves to `10.0.0.5:9090`), `testInstanceWithoutGrpcPortIsSkipped`, `testNoInstancesIsUnavailable`.
- `ManagedGrpcChannelsTest`: `testTargetOverrideIsUsed`, `testDefaultTargetIsDiscovery`, `testChannelIsCachedPerServiceId`.
- `GrpcRemoteConfigurationTest` (`ApplicationContextRunner`): `testBlankTokenFailsWithBindableService`, `testPlaintextFalseWithoutSslBundleFails`, `testPlaintextTrueWithTokenStarts`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:libs:core:remote:remote-grpc:test`
Expected: FAIL (classes missing).

- [ ] **Step 3: Implement** the module per the Interfaces block, porting the POC classes; dependencies: `spring-boot-starter-grpc-server` and `-grpc-client` (Boot-managed), `spring-cloud-commons`, `tenant-api`, `app-config`, EE `discovery-api`/`discovery-redis` for `ServiceMetadataRegistry` (whichever module defines it), `grpc-netty-shaded` runtime; tests `grpc-inprocess`, `grpc-services`, `grpc-testing`.

- [ ] **Step 4: Run module check**

Run: `./gradlew :server:ee:libs:core:remote:remote-grpc:check :server:libs:config:app-config:check :server:ee:libs:config:microservice-config:check`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml settings.gradle.kts server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java server/ee/libs/config/microservice-config server/ee/libs/core/remote/remote-grpc
git commit -m "Add the shared gRPC foundation for calls between microservices"
```

---

### Task 2: Users and authorities over gRPC

**Files:**
- Create: `server/ee/libs/platform/platform-user/platform-user-remote-grpc-proto` (`src/main/proto/user_remote_service.proto`, build file as the POC's proto module: generated code only, static analysis off)
- Create: `server/ee/libs/platform/platform-user/platform-user-remote-grpc` (`UserProtoMapper`, `UserRemoteServiceGrpcServer`, `AuthorityRemoteServiceGrpcServer`, `UserGrpcServerConfiguration`)
- Modify: `server/ee/libs/platform/platform-user/platform-user-remote-client` (`RemoteUserServiceClient`, `RemoteAuthorityServiceClient`)
- Modify: `settings.gradle.kts`

**Interfaces:**
- Consumes: Task 1 `GrpcChannels`.
- Produces: proto services `UserRemoteService` (`GetUserById`, `GetUserByLogin`, `FetchUserById`, `FetchUserByLogin`, `FetchUserByEmail`) and `AuthorityRemoteService` (`FetchAuthority`, `GetAuthorities`); fetch responses carry an optional user/authority. `UserGrpcServerConfiguration` exposes both servers as `BindableService` beans, `@ConditionalOnBean(UserService.class)` on the real service, so they appear only where users live (the gateway). Clients call service id `api-gateway-app`. `RemoteUserServiceClient.getCurrentUser()` / `fetchCurrentUser()` read the login from the caller's `SecurityContextHolder` and call `GetUserByLogin` / `FetchUserByLogin`. All other `UserService`/`AuthorityService` methods keep throwing `UnsupportedOperationException`. The proto `User` carries every `User` field except the password hash and the TOTP secret; enums and instants map explicitly.

- [ ] **Step 1: Write the failing tests**
- `UserProtoMapperTest`: `testUserRoundTrip` (all mapped fields equal after `toProto`/`fromProto`; password and TOTP secret are absent on the result), `testAuthorityRoundTrip`.
- `UserRemoteServiceGrpcWireTest` (in-process server with the real server classes over a Mockito `UserService` / `AuthorityService`; client is the real `RemoteUserServiceClient` with a `GrpcChannels` lambda returning the in-process channel): `testGetUserByLogin`, `testGetUserThrowsWhenMissing`, `testFetchUserByLoginReturnsEmptyWhenMissing`, `testFetchUserById`, `testGetCurrentUserUsesCallerLogin` (caller context `admin@localhost.com`, server mock asserted with that login), `testFetchAuthority`, `testGetAuthorities`, `testUnportedMethodStillThrowsUnsupported` (`registerUser`).

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:libs:platform:platform-user:platform-user-remote-grpc:test :server:ee:libs:platform:platform-user:platform-user-remote-client:test`
Expected: FAIL (modules or methods missing).

- [ ] **Step 3: Implement** the proto, mapper, servers, server configuration and client methods. The server maps a missing row on `get*` to `Status.NOT_FOUND` with the service's message; the client rethrows it as the exception type the local `UserServiceImpl.getUser` throws for a missing user.

- [ ] **Step 4: Run the checks**

Run: `./gradlew :server:ee:libs:platform:platform-user:platform-user-remote-grpc:check :server:ee:libs:platform:platform-user:platform-user-remote-client:check`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts server/ee/libs/platform/platform-user
git commit -m "Serve users and authorities over gRPC"
```

---

### Task 3: API keys over gRPC

**Files:**
- Create: `server/ee/libs/platform/platform-security/platform-security-remote-grpc-proto` (`api_key_remote_service.proto`), `platform-security-remote-grpc` (`ApiKeyProtoMapper`, `ApiKeyRemoteServiceGrpcServer`, `ApiKeyGrpcServerConfiguration`), `platform-security-remote-client` (`RemoteApiKeyServiceClient`, `@Component @ConditionalOnEEVersion`)
- Modify: `settings.gradle.kts`

**Interfaces:**
- Consumes: Task 1 `GrpcChannels`.
- Produces: `ApiKeyRemoteService` with `Exists(secretKey, environmentId)`, `FetchApiKey(secretKey)`, `GetApiKeyBySecretKey(secretKey)`, `GetApiKeyBySecretKeyAndEnvironment(secretKey, environmentId)`; client service id `configuration-app`; other `ApiKeyService` methods throw `UnsupportedOperationException`. Server registered `@ConditionalOnBean(ApiKeyService.class)` on the real service.

- [ ] **Step 1: Write the failing tests**
- `ApiKeyProtoMapperTest`: `testApiKeyRoundTrip` (id, name, secretKey, type, environment, userId, lastUsedDate, created/modified fields).
- `ApiKeyRemoteServiceGrpcWireTest`: `testExists`, `testFetchApiKeyReturnsEmptyWhenMissing`, `testGetApiKeyBySecretKeyAndEnvironment`, `testGetApiKeyThrowsWhenMissing`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:libs:platform:platform-security:platform-security-remote-grpc:test :server:ee:libs:platform:platform-security:platform-security-remote-client:test`
Expected: FAIL.

- [ ] **Step 3: Implement** as in the Interfaces block.

- [ ] **Step 4: Run the checks**

Run: `./gradlew :server:ee:libs:platform:platform-security:platform-security-remote-grpc:check :server:ee:libs:platform:platform-security:platform-security-remote-client:check`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts server/ee/libs/platform/platform-security
git commit -m "Serve API key lookups over gRPC"
```

---

### Task 4: Connected users over gRPC

**Files:**
- Create: `server/ee/libs/embedded/embedded-connected-user/embedded-connected-user-remote-grpc-proto` (`connected_user_remote_service.proto`), `embedded-connected-user-remote-grpc` (`ConnectedUserProtoMapper`, `ConnectedUserRemoteServiceGrpcServer`, `ConnectedUserGrpcServerConfiguration`)
- Modify: `server/ee/libs/embedded/embedded-connected-user/embedded-connected-user-remote-client` (`RemoteConnectedUserServiceClient`)
- Modify: `settings.gradle.kts`

**Interfaces:**
- Consumes: Task 1 `GrpcChannels`.
- Produces: `ConnectedUserRemoteService` with `FetchConnectedUserByExternalId(externalId, environmentId)`, `CreateConnectedUser(externalId, environmentId)`; client service id `configuration-app`; methods `fetchConnectedUser(String, long)` and `createConnectedUser(String, long)` implemented, the rest keep throwing. Server `@ConditionalOnBean(ConnectedUserService.class)` on the real service.

- [ ] **Step 1: Write the failing tests**
- `ConnectedUserProtoMapperTest`: `testConnectedUserRoundTrip` (every persisted field, including metadata maps through `google.protobuf.Struct`).
- `ConnectedUserRemoteServiceGrpcWireTest`: `testFetchConnectedUser`, `testFetchConnectedUserReturnsEmptyWhenMissing`, `testCreateConnectedUser`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-remote-grpc:test :server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-remote-client:test`
Expected: FAIL.

- [ ] **Step 3: Implement** as in the Interfaces block.

- [ ] **Step 4: Run the checks**

Run: `./gradlew :server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-remote-grpc:check :server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-remote-client:check`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts server/ee/libs/embedded/embedded-connected-user
git commit -m "Serve connected user lookups over gRPC"
```

---

### Task 5: Apps serve and call the new gRPC services

**Files:**
- Modify: `server/ee/apps/api-gateway-app/build.gradle.kts` (`remote-grpc`, `platform-user-remote-grpc`, `spring-boot-starter-grpc-server`)
- Modify: `server/ee/apps/configuration-app/build.gradle.kts` (`remote-grpc`, `platform-security-remote-grpc`, `embedded-connected-user-remote-grpc`, gRPC server starter)
- Modify: `server/ee/apps/execution-app/build.gradle.kts`, `server/ee/apps/webhook-app/build.gradle.kts` (`remote-grpc`, `platform-security-remote-client`, `platform-user-remote-client` where missing, and back: CE/EE `platform-security-web-impl`, `embedded-security-web-impl`, which plan 2 left out for lack of `ApiKeyService`)
- Modify: each of these apps' `config/<app>-dev.yml` (`spring.grpc.server.port` = HTTP port + 10000 for the two servers: gateway `17111`, configuration-app `17222`) and `src/test/resources/config/application-testint.yml` (`bytechef.remote.grpc.plaintext: true`, a test token, `spring.grpc.server.port: 0`)
- Test: `ApiGatewayApplicationIntTest`, `ConfigurationApplicationIntTest`, `ExecutionSessionIntTest`, `WebhookSessionIntTest`

**Interfaces:**
- Consumes: Tasks 1 to 4.

- [ ] **Step 1: Write the failing tests**
- `ApiGatewayApplicationIntTest.testUserGrpcServicesAreServed`: `BindableService` beans include `UserRemoteService` and `AuthorityRemoteService` (by service descriptor name); `testGrpcPortIsRegisteredInDiscovery`: the gateway's instance from `DiscoveryClient.getInstances("api-gateway-app")` has metadata `grpc-port`.
- `ConfigurationApplicationIntTest.testApiKeyAndConnectedUserGrpcServicesAreServed`, and `testUserServiceIsTheGrpcClient` (the `UserService` bean is `RemoteUserServiceClient`).
- `ExecutionSessionIntTest.testEmbeddedApiKeyIsCheckedOverGrpc`: the test starts a grpc-netty server on a random port running `ApiKeyRemoteServiceGrpcServer` and `ConnectedUserRemoteServiceGrpcServer` over Mockito services (`exists("embedded-secret", env)` true; connected user found) with the token interceptor, and points `bytechef.remote.grpc.targets.configuration-app` at it via `@DynamicPropertySource`; a request to `/api/embedded/v1/ext-user/tools` with that key (as `EmbeddedApiKeyAuthenticationConverter` expects it) is not `401`; with key `wrong` it is `401`.
- `WebhookSessionIntTest.testEmbeddedApiKeyIsCheckedOverGrpc`: same against `/api/embedded/v1/ext-user/workflows/uuid`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:apps:api-gateway-app:testIntegration :server:ee:apps:configuration-app:testIntegration :server:ee:apps:execution-app:testIntegration :server:ee:apps:webhook-app:testIntegration`
Expected: FAIL.

- [ ] **Step 3: Implement** the dependencies and configuration.

- [ ] **Step 4: Run all EE app checks and the monolith**

Run: `./gradlew :server:ee:apps:api-gateway-app:check :server:ee:apps:configuration-app:check :server:ee:apps:execution-app:check :server:ee:apps:webhook-app:check :server:ee:apps:connection-app:testIntegration :server:ee:apps:coordinator-app:testIntegration :server:ee:apps:worker-app:testIntegration :server:ee:apps:scheduler-app:testIntegration :server:apps:server-app:testIntegration`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/apps
git commit -m "Serve users from the gateway and API keys from configuration over gRPC and call them from the other microservices"
```
