# gRPC remote-endpoint POC (ConnectionService) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the EE `ConnectionService` remote transport from REST to Spring gRPC on one service, behind a `rest|grpc` toggle, to produce a go/no-go + effort estimate for the full migration.

**Architecture:** New EE proto module generates gRPC stubs; a gRPC server bean in connection-app delegates to the real `ConnectionService`; a gRPC client bean implements `ConnectionService` on consumer apps. Hybrid proto marshalling (typed fields + `Struct` for the dynamic parameters map). Static target address. Auth+tenant parity via gRPC interceptors mirroring the two `/remote` filters.

**Tech Stack:** Spring Boot 4.0.7, Java 25, Gradle 9.4 (Kotlin DSL), `org.springframework.grpc:spring-grpc` (version resolved in Task 1), `com.google.protobuf` Gradle plugin + grpc-java protoc plugin, `protobuf-java-util` (Struct).

## Global Constraints

- All new files under `server/ee/` carry the **ByteChef Enterprise license header** + `@version ee` Javadoc tag (Spotless selects the header by `@version ee` content).
- The POC is **dormant by default**: `bytechef.remote.transport` defaults to `rest`; the REST path is byte-for-byte unchanged.
- Only the two read RPCs (`getConnection`, `getConnections`) are implemented; every other `ConnectionService` method stays `UnsupportedOperationException`.
- Auth+tenant parity is **mandatory**: mirror `RemoteServiceAuthenticationFilter` (`TenantConstants.INTERNAL_SERVICE_TOKEN`) and `RemoteMultiTenantFilter` (`TenantConstants.CURRENT_TENANT_ID`).
- If Task 1 proves spring-grpc cannot build under this toolchain, **STOP and report** — a hard incompatibility is itself the POC's answer; do not fabricate a workaround.

---

### Task 1: Tooling foundation — proto module builds gRPC stubs under the repo toolchain (GATE)

This is the make-or-break spike. Everything downstream depends on it. Keep the proto trivial here; the real proto is Task 2.

**Files:**
- Create: `server/ee/libs/platform/platform-connection/platform-connection-remote-grpc-proto/build.gradle.kts`
- Create: `.../platform-connection-remote-grpc-proto/src/main/proto/ping.proto` (throwaway smoke proto)
- Modify: `settings.gradle.kts` (include the new module)
- Modify: `gradle/libs.versions.toml` (add spring-grpc + protobuf/grpc versions/libs/plugins)

**Steps:**
- [ ] **Step 1: Resolve compatible versions.** Determine the latest `org.springframework.grpc:spring-grpc` release that supports Spring Boot 4.x, the matching `com.google.protobuf` Gradle plugin, `protoc`, `protoc-gen-grpc-java`, and grpc-java runtime. Add to `gradle/libs.versions.toml`: a `[versions]` entry for `grpc-java`, `protobuf`, `spring-grpc`; `[libraries]` for `io.grpc:grpc-stub`, `io.grpc:grpc-protobuf`, `com.google.protobuf:protobuf-java`, `com.google.protobuf:protobuf-java-util`, `org.springframework.grpc:spring-grpc-spring-boot-starter`, `org.springframework.grpc:spring-grpc-test`; a `[plugins]` entry for `com.google.protobuf`. Prefer the spring-grpc BOM (`org.springframework.grpc:spring-grpc-dependencies`) for version alignment.
- [ ] **Step 2: Write `ping.proto`** — trivial `service PingService { rpc Ping (PingRequest) returns (PingReply); }` with one string field each; `package com.bytechef.ee.platform.connection.remote.grpc`, `option java_multiple_files = true;`.
- [ ] **Step 3: Write the module `build.gradle.kts`** applying the protobuf plugin, configuring the `protoc` artifact + the grpc-java plugin, and depending on grpc-stub/grpc-protobuf/protobuf-java(-util). Mirror the repo's EE license-header expectations. Include the module in `settings.gradle.kts`.
- [ ] **Step 4: GATE — generate + compile.** Run: `./gradlew :server:ee:libs:platform:platform-connection:platform-connection-remote-grpc-proto:compileJava --console=plain` (capture the real exit code, do not pipe to tail). Expected: BUILD SUCCESSFUL with `PingServiceGrpc` + `PingRequest`/`PingReply` generated and on the classpath. If it fails on a version/toolchain incompatibility that cannot be resolved by version bumps, STOP and record the exact error as the POC finding.
- [ ] **Step 5: Commit** the module skeleton + version catalog additions.

---

### Task 2: `connection_service.proto` (hybrid) generates cleanly

**Files:**
- Delete: the throwaway `ping.proto`
- Create: `.../platform-connection-remote-grpc-proto/src/main/proto/connection_service.proto`

**Interfaces:**
- Produces: generated `ConnectionRemoteServiceGrpc`, `ConnectionProto`, `ConnectionListProto`, `GetConnectionRequest`, `GetConnectionsRequest`, and proto enums `ConnectionStatusProto`, `ConnectionVisibilityProto`, `PlatformTypeProto`.

**Steps:**
- [ ] **Step 1: Enumerate the domain enum values** for `ConnectionStatus`, `ConnectionVisibility`, `PlatformType` (read the domain sources) so the proto enums mirror them 1:1, each with a `*_UNSPECIFIED = 0` zero value.
- [ ] **Step 2: Write `connection_service.proto`** per the spec's hybrid message (typed scalars/enums + `google.protobuf.Struct parameters`, `repeated int64 tag_ids`, audit fields). `import "google/protobuf/struct.proto";`.
- [ ] **Step 3: Compile** the proto module; assert the generated types exist.
- [ ] **Step 4: Commit.**

---

### Task 3: `ConnectionProtoMapper` with round-trip fidelity

**Files:**
- Create: mapper in a shared EE module both client and server can depend on — `platform-connection-remote-grpc-proto` (co-located with the stubs) or a small `-remote-grpc-support` module. Decide to keep the proto module as the shared carrier: put the mapper there.
- Create: `ConnectionProtoMapperTest` (unit).

**Interfaces:**
- Produces: `ConnectionProtoMapper` with `ConnectionProto toProto(Connection)` and `Connection toDomain(ConnectionProto)` (static methods or a `@Component` — static is simplest for a pure mapper).
- Consumes: `Connection` domain (`platform-connection-api`), `protobuf-java-util` Struct helpers.

**Steps:**
- [ ] **Step 1: Write `ConnectionProtoMapperTest`** first (TDD): build a fully-populated `Connection` (id, name, componentName, connectionVersion, environment, each enum, a `parameters` map containing a string, an integer, a boolean, and a nested map, plus tag ids), map to proto and back, assert field equality. Explicitly assert the integer-valued parameter survives the `Struct` round-trip (Struct stores numbers as double — the mapper must restore integral values).
- [ ] **Step 2: Run the test — expect FAIL** (mapper absent).
- [ ] **Step 3: Write `ConnectionProtoMapper`** — scalar/enum field copies (domain int-code ↔ proto enum), `Map<String,Object>` ↔ `Struct` via `protobuf-java-util` (`Structs`/`JsonFormat`), resolving `EncryptedMapWrapper` to a plain map and restoring integral doubles. Reuse the repo's Jackson-configured conversion utilities where they help.
- [ ] **Step 4: Run the test — expect PASS.**
- [ ] **Step 5: Commit.**

---

### Task 4: gRPC server impl + auth/tenant server interceptors (connection-app side)

**Files:**
- Create: `platform-connection-remote-grpc` module (server-side; keeps the REST module clean) OR add to `platform-connection-remote-rest`. Decide: new `-remote-grpc` module for server + interceptors, depending on the proto module + `platform-connection-api`.
- Create: `ConnectionRemoteServiceGrpcServer` (extends `ConnectionRemoteServiceGrpc.ConnectionRemoteServiceImplBase`).
- Create: `InternalServiceTokenServerInterceptor`, `TenantContextServerInterceptor`.
- Create: server-side integration test (Task 6 covers the wire test; here a focused unit test of the service impl over a mocked `ConnectionService`).

**Interfaces:**
- Consumes: `ConnectionService`, `ConnectionProtoMapper`, generated stubs.
- Produces: a Spring-registered gRPC service bean + two `ServerInterceptor`s.

**Steps:**
- [ ] **Step 1: Write the service impl** — `getConnection` / `getConnections` delegate to `ConnectionService`, map via `ConnectionProtoMapper`, complete the `StreamObserver`. Errors → `StatusRuntimeException` (`NOT_FOUND` for a missing connection, etc.).
- [ ] **Step 2: Write `InternalServiceTokenServerInterceptor`** — read the `INTERNAL_SERVICE_TOKEN` metadata key; reject with `Status.UNAUTHENTICATED` when absent/mismatched (compare against the same configured token the REST filter uses).
- [ ] **Step 3: Write `TenantContextServerInterceptor`** — read `CURRENT_TENANT_ID` metadata, `TenantContext.setCurrentTenantId(...)` around dispatch, clear in a finally (use a `SimpleForwardingServerCallListener` or `Contexts.interceptCall`).
- [ ] **Step 4: Unit-test the service impl** over a mocked `ConnectionService` (both RPCs, mapping correct).
- [ ] **Step 5: Commit.**

---

### Task 5: gRPC client (`implements ConnectionService`) + client interceptors + transport toggle

**Files:**
- Create: `GrpcConnectionServiceClient implements ConnectionService` in `platform-connection-remote-client`.
- Create: `InternalServiceTokenClientInterceptor`, `TenantContextClientInterceptor`.
- Modify: client bean config to select `rest` vs `grpc` via `@ConditionalOnProperty(bytechef.remote.transport)`.

**Interfaces:**
- Consumes: generated blocking stub, `ConnectionProtoMapper`, `bytechef.grpc.connection-app.address`.
- Produces: a `ConnectionService` bean active when `transport=grpc`.

**Steps:**
- [ ] **Step 1: Write the client interceptors** — attach `INTERNAL_SERVICE_TOKEN` (from config) and `CURRENT_TENANT_ID` (from `TenantContext.getCurrentTenantId()`) as call metadata.
- [ ] **Step 2: Write `GrpcConnectionServiceClient`** — build a `ManagedChannel` to the configured address with the two interceptors; `getConnection`/`getConnections` call the blocking stub and map proto→domain; all write methods throw `UnsupportedOperationException`.
- [ ] **Step 3: Add the toggle** — annotate `RemoteConnectionServiceClient` with `@ConditionalOnProperty(name="bytechef.remote.transport", havingValue="rest", matchIfMissing=true)` and `GrpcConnectionServiceClient` with `havingValue="grpc"`, so exactly one is active. Register `bytechef.grpc.connection-app.address` in `ApplicationProperties` (strict binding).
- [ ] **Step 4: Context test** — assert the correct bean is active for each property value.
- [ ] **Step 5: Commit.**

---

### Task 6: In-process gRPC integration test (both RPCs + metadata propagation)

**Files:**
- Create: `ConnectionRemoteServiceGrpcIntTest` using spring-grpc test / grpc in-process transport.

**Steps:**
- [ ] **Step 1: Stand up an in-process server** bound to the real service impl (over a mocked `ConnectionService`) with both server interceptors; build an in-process channel with both client interceptors.
- [ ] **Step 2: Call `getConnection`** — assert the mapped domain result AND that the server interceptor observed the token + tenant metadata (set a tenant, assert the mock was invoked under that tenant / captured tenant equals sent).
- [ ] **Step 3: Call `getConnections`** — assert the list round-trips.
- [ ] **Step 4: Negative auth** — a call with no/invalid token → `UNAUTHENTICATED`.
- [ ] **Step 5: Commit.**

---

### Task 7: Wire the gRPC server into connection-app + config

**Files:**
- Modify: `server/ee/apps/connection-app/build.gradle.kts` (depend on `-remote-grpc`), `application.yml`/config (spring-grpc server port).
- Modify: consumer app(s) — ensure `platform-connection-remote-client` still resolves with the toggle default `rest` (no behavior change).

**Steps:**
- [ ] **Step 1: Add the server module dependency** to connection-app; configure `spring.grpc.server.port` and register the interceptors.
- [ ] **Step 2: Assemble** — `./gradlew :server:ee:apps:connection-app:compileJava` and one consumer, e.g. `:server:ee:apps:worker-app:compileJava` → BUILD SUCCESSFUL (real exit code).
- [ ] **Step 3: Commit.**

---

### Task 8: Full verification + POC findings write-up

**Steps:**
- [ ] **Step 1: `check` the touched modules** (proto, remote-grpc, remote-client) + compile connection-app + one consumer app; capture real exit codes.
- [ ] **Step 2: Write findings** into the spec's Risks section or a short `docs/superpowers/notes/` file: did it build under the toolchain? mapper fidelity (Struct integer issue?)? interceptor parity? estimated per-service effort for the full migration; go/no-go recommendation; the discovery follow-up.
- [ ] **Step 3: Commit.**

---

## Self-Review

**Spec coverage:** hybrid proto → Tasks 2-3; static-address transport → Tasks 5,7; auth+tenant parity → Tasks 4,5; toggle/coexistence → Task 5; testing (mapper round-trip, in-process wire, negative auth, toggle) → Tasks 3,5,6; discovery + other-methods explicitly deferred → Global Constraints + Task 8 findings.

**Placeholder scan:** the only unresolved literals are external version numbers (Task 1 Step 1) — a genuine resolve-at-impl fact gated by a build, not a logic placeholder. Server/mapper module placement decisions are called out explicitly in Tasks 3/4.

**Type consistency:** `ConnectionProtoMapper.toProto/toDomain`, generated `ConnectionRemoteServiceGrpc`/`ConnectionProto`, `GrpcConnectionServiceClient`, `bytechef.remote.transport`, `bytechef.grpc.connection-app.address`, and the two `TenantConstants` keys are referenced consistently across tasks.

**Risk gate:** Task 1 is an explicit make-or-break spike; a hard spring-grpc/Boot-4 incompatibility is a valid terminal POC finding, not a failure to force through.
