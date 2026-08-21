# gRPC remote-endpoint POC — findings (ConnectionService)

**Date:** 2026-07-24
**Spec:** `docs/superpowers/specs/2026-07-24-grpc-remote-connectionservice-poc-design.md`
**Plan:** `docs/superpowers/plans/2026-07-24-grpc-remote-connectionservice-poc.md`

## Verdict: **GO** on technical feasibility.

Every core risk of migrating the EE remote transport from REST to gRPC has been retired on one real
service (`ConnectionService`) with passing tests. The only remaining work is productionization
(standing up the Spring-managed gRPC server in an app + discovery), not feasibility.

## What is proven (with evidence)

| Risk | Result | Evidence |
|------|--------|----------|
| Codegen toolchain builds under Gradle 9.4.1 / Java 25 | ✅ | Task 1 — `protobuf-gradle-plugin 0.10.0` + `protoc 4.35.0` + `protoc-gen-grpc-java 1.83.0` generate stubs that compile. `compileJava` green. |
| Rich domain-object hybrid marshalling round-trips | ✅ | Task 3 — `ConnectionProtoMapperTest` 3/3: scalars, enums (by name), and the dynamic `parameters` map via `Struct` (nested map, list, integer). |
| Transport + server/client end-to-end | ✅ | Tasks 4–6 — `ConnectionRemoteServiceGrpcWireTest` in-process call returns the mapped result. |
| Service-auth parity (`X-Bytechef-Internal-Token`) | ✅ | Wire test: bad token → `UNAUTHENTICATED`; constant-time check mirrors `RemoteServiceAuthenticationFilter`. |
| Tenant-context parity (`CURRENT_TENANT_ID`) | ✅ | Wire test: caller's `TenantContext` propagates to the server **handler thread** (captured off a separate executor). |
| Coexistence + instant rollback | ✅ | Task 5 — `bytechef.remote.transport=rest\|grpc` `@ConditionalOnProperty` selects REST vs gRPC `ConnectionService` bean; default `rest`, dormant. `check` green. |

Commits: `72221bb` (Task 1) · `b332021` (Task 2) · `55d4557` (Task 3) · `75a71ba` (Tasks 4–6) ·
`8cee92e` (Task 5 client/toggle).

## Notable technical findings

- **`Struct` numeric fidelity:** `google.protobuf.Struct` stores all numbers as `double`. An integer
  parameter round-trips as `Double` unless normalized; the mapper narrows integral doubles back to
  `Long`. Genuine `double`-vs-`long` intent is ambiguous through `Struct` — acceptable for connection
  parameters (mostly strings/ids/flags), but the full migration should decide per-service whether any
  domain type needs exact numeric typing (→ typed proto fields instead of `Struct`).
- **`sun.misc.Unsafe` warning:** protobuf-java 4.35.0 logs a benign terminal-deprecation warning for
  `Unsafe::arrayBaseOffset` on Java 25. Warning only; no failure. Worth tracking against future
  protobuf releases.
- **Enum-by-name mapping** (not ordinal) proved clean and is the recommended template — robust to the
  domain's int-ordinal storage drift; proto carries a `*_UNSPECIFIED = 0` proto3 sentinel.
- **Static-analysis on generated code:** the proto module must disable checkstyle/pmd/spotbugs (they
  analyze the main source set, which the protobuf plugin extends with generated code). Hand-written
  gRPC code lives in a separate, fully-checked module — a clean separation to keep for the migration.

## Update — items 1 & 2 now DONE

- **Spring-managed gRPC server in `connection-app` — DONE.** spring-grpc **1.0.3 is Spring Boot
  4-native** (its autoconfig lives under `org.springframework.boot.grpc.*`). `ConnectionGrpcServerConfiguration`
  registers the intercepted service as a `BindableService` bean; the spring-grpc server starter serves
  it. `ConnectionGrpcServerAutoConfigIntTest` boots the real starter under Boot 4.0.7 on a free port,
  starts a Netty gRPC server, and calls `getConnection` through a Netty client — asserting result +
  tenant propagation. connection-app wired with the server starter (`spring.grpc.server.enabled=false`
  by default → dormant) and assembles cleanly. Server-side TLS is config-only via
  `spring.grpc.server.ssl.bundle` (+ `ssl.client-auth` for mTLS) — set alongside enabling the server.
- **Redis-LB discovery parity — DONE (unit-verified; runtime boundary noted).** spring-grpc 1.0.3 has
  no built-in Spring Cloud discovery, so a custom gRPC `NameResolver` bridges it: `DiscoveryClientNameResolver`
  + `DiscoveryClientNameResolverProvider` resolve a `discovery:///connection-app` target through the
  Spring Cloud `DiscoveryClient` (which `discovery-redis` implements), and `GrpcDiscoveryConfiguration`
  registers the provider (gated on `transport=grpc`, skipped when no `DiscoveryClient`). The client channel
  uses `round_robin` so calls distribute across discovered instances — genuine parity with
  `LoadBalancedRestClient`. To use: set `bytechef.grpc.connection-app.address=discovery:///connection-app`.
  `DiscoveryClientNameResolverTest` proves the resolve/error mapping against a mocked `DiscoveryClient`;
  **full multi-instance behavior needs a live discovery-redis cluster + ≥2 connection-app instances to
  verify at runtime** (not reproducible in a unit build).

## Update — item 3 (catalog + ApplicationProperties) now DONE

- **Version catalog — DONE.** The protobuf/gRPC/spring-grpc versions moved into
  `gradle/libs.versions.toml` (`grpc`, `protobuf`, `spring-grpc` versions; grpc-stub/protobuf/
  netty-shaded/inprocess/testing libraries; the `com.google.protobuf` plugin), referenced via `libs.*`
  in the four build files. Gotcha found: naming the plugin version `protobuf-gradle-plugin` collided
  with the `protobuf` version (shared prefix makes `libs.versions.protobuf` a group accessor, not a
  `Provider<String>`) — the plugin version is inlined in the `[plugins]` entry to keep `protobuf` a leaf.
- **`ApplicationProperties` strict binding — DONE.** `bytechef.remote.transport`,
  `bytechef.grpc.connection-app.address`, and `bytechef.grpc.plaintext-enabled` are now declared as
  typed fields (`Remote`, `Grpc`/`Grpc.ConnectionApp` nested classes) in the central strict-bound
  `ApplicationProperties`, so operators can set them in yml without an unknown-property startup
  failure. The `@Value`/`@ConditionalOnProperty` reads are unchanged (they read the Environment
  directly); the fields exist to satisfy strict binding.

## Remaining productionization (NOT feasibility — scoped next steps)
4. **Transport security (TLS/mTLS).** The commit security review flagged the client's plaintext
   channel as HIGH: the internal service token is a bearer credential, so sending it over an
   unencrypted channel exposes a reusable credential to anyone sniffing the internal network (a real
   risk the existing plaintext REST transport shares, and not one to carry forward). **Fixed on the
   client:** `GrpcConnectionServiceClient` now defaults to `useTransportSecurity()` (TLS); plaintext
   requires the explicit off-by-default `bytechef.grpc.plaintext-enabled=true` AND a loopback target
   (`localhost`/`127.0.0.1`/`::1`), else it refuses to start. **Still to do on the server:** the
   deferred `connection-app` gRPC server (item 1) must terminate TLS (`TlsServerCredentials` /
   spring-grpc TLS config), ideally mutual TLS between services — a security *improvement* over the
   plaintext REST status quo. Do NOT ship a `grpc`-transport deployment over an untrusted network
   without server-side TLS.
5. **Write methods + other services.** Only the two reads are implemented; the ~30 other remote pairs
   remain on REST.

## Effort estimate for the full migration (per service)

Given the proven template — codegen toolchain, hybrid mapper, shared interceptors, transport toggle,
spring-grpc server autoconfig, and discovery name-resolver, all now in place and tested — each
additional remote pair is roughly: a `.proto` (mirror the domain shape) + a hand-written mapper (the
bulk; trivial for scalar-only services, moderate for `Struct`-carrying dynamic maps) + a ~20-line
server config bean + client, reusing everything shared. The one-time infrastructure cost is paid;
subsequent services are mapper + proto + thin server/client wiring.

The whole gRPC path for `ConnectionService` is proven end-to-end (codegen, marshalling, transport,
auth/tenant parity, spring-grpc Boot-4 server autoconfig, discovery). Recommended next steps before a
full rollout: promote the remaining productionization items (version catalog, `ApplicationProperties`
fields, server-side TLS bundle, live-cluster discovery verification), then migrate services in batches
using the template.
