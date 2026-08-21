# gRPC remote-endpoint POC — ConnectionService over Spring gRPC

**Date:** 2026-07-24
**Status:** Design — approved to proceed
**Author:** Ivica Cardic

## Motivation

ByteChef's distributed EE topology has every cross-app service call go through a hand-written
REST pair: a `*-remote-client` class that **implements the domain service interface** and delegates
the remotely-invoked methods to `LoadBalancedRestClient` (Spring Cloud LoadBalancer + a Redis-backed
discovery registry) over Jackson-JSON HTTP, and a paired `*-remote-rest` `@RestController` on the
target app that delegates to the real service bean. There are ~30 such pairs.

We want to evaluate migrating this transport to **gRPC** (via the Spring team's `spring-grpc`
project) for typed schemas, binary framing, and streaming potential. Rather than commit to all ~30
pairs, this POC migrates **one** service end-to-end to learn the real effort and risks — especially
domain-object marshalling and cross-app auth/tenant parity — before deciding.

## Scope (the one service)

`ConnectionService`, exposed today as:
- Server: `RemoteConnectionServiceController` (`/remote/connection-service`) — two read RPCs:
  `getConnection(long id)` and `getConnections(PlatformType type)`.
- Client: `RemoteConnectionServiceClient implements ConnectionService` in
  `platform-connection-remote-client` (carried by scheduler/worker/webhook/coordinator/configuration
  apps); every write method throws `UnsupportedOperationException`.
- Server app: `connection-app`.

The gRPC POC replaces exactly those two read RPCs. All write methods remain
`UnsupportedOperationException` (unchanged).

## Decisions (locked)

1. **Service:** ConnectionService (small surface, but a rich `Connection` domain type — proves the
   hardest part of the migration on a realistic object).
2. **Marshalling:** **Hybrid proto.** Model the stable scalar/enum fields as typed proto fields;
   carry the dynamic `parameters` map as `google.protobuf.Struct`. Produces a reusable mapper
   template + a realistic effort estimate for the full migration.
3. **Discovery:** **Static configured address** for the POC (change one variable). Spring Cloud
   LoadBalancer + `discovery-redis` name-resolution parity is a documented fast-follow.
4. **Coexistence:** a `bytechef.remote.transport=rest|grpc` property (default `rest`) selects which
   `ConnectionService` client bean is active — safe rollback + A/B, zero risk to the current path.

## Architecture

### Modules

- **New `platform-connection-remote-grpc-proto`** (EE) — owns `connection_service.proto` and
  generates Java message + gRPC stubs via the `com.google.protobuf` Gradle plugin + the grpc-java
  protoc plugin. Depends on `protobuf-java`, `protobuf-java-util` (Struct helpers), and the grpc
  runtime. This module is a pure generated-code carrier consumed by both client and server.
- **gRPC server** — a `@Service`/`@GrpcService`-style bean implementing the generated
  `ConnectionRemoteServiceGrpc.ConnectionRemoteServiceImplBase`, delegating to the real
  `ConnectionService`. Lives in `platform-connection-remote-rest` (or a sibling `-remote-grpc`
  module — decide in the plan to keep the REST module dependency-clean). Registered only in
  `connection-app` via a Spring gRPC server starter.
- **gRPC client** — `GrpcConnectionServiceClient implements ConnectionService` in
  `platform-connection-remote-client`, delegating the two reads to a generated blocking stub; write
  methods throw `UnsupportedOperationException`. Selected over `RemoteConnectionServiceClient` by the
  transport toggle.

### Proto (hybrid)

```proto
syntax = "proto3";
package com.bytechef.ee.platform.connection.remote.grpc;
import "google/protobuf/struct.proto";
option java_multiple_files = true;

enum ConnectionStatusProto { CONNECTION_STATUS_UNSPECIFIED = 0; /* mirror domain */ }
enum ConnectionVisibilityProto { CONNECTION_VISIBILITY_UNSPECIFIED = 0; /* ... */ }
enum PlatformTypeProto { PLATFORM_TYPE_UNSPECIFIED = 0; /* ... */ }

message ConnectionProto {
  int64  id = 1;
  string name = 2;
  string component_name = 3;
  int32  connection_version = 4;
  ConnectionStatusProto status = 5;
  ConnectionVisibilityProto visibility = 6;
  PlatformTypeProto type = 7;
  int32  environment = 8;
  google.protobuf.Struct parameters = 9;   // dynamic Map<String,Object>
  repeated int64 tag_ids = 10;
  // audit fields (created_by/date, last_modified_by/date, version, managed, ...) as needed
}
message ConnectionListProto { repeated ConnectionProto connections = 1; }

message GetConnectionRequest  { int64 id = 1; }
message GetConnectionsRequest { PlatformTypeProto type = 1; }

service ConnectionRemoteService {
  rpc GetConnection  (GetConnectionRequest)  returns (ConnectionProto);
  rpc GetConnections (GetConnectionsRequest) returns (ConnectionListProto);
}
```

Enum encoding note: the `Connection` domain stores several enums as `int` codes (status, type,
visibility, credentialStoreType, authorizationType). The mapper converts between the domain int
codes / `PlatformType`/`ConnectionStatus`/`ConnectionVisibility` enums and the proto enums; add a
`*_UNSPECIFIED = 0` zero value per proto3 convention. The `parameters` field is
`EncryptedMapWrapper` on the domain object — the mapper resolves it to a `Map<String,Object>` and
encodes it to `Struct` (and back), reusing the existing Jackson-configured conversion utilities.

### Mapping

A hand-written `ConnectionProtoMapper` (not MapStruct — the Struct + int-enum conversions are
custom): `Connection -> ConnectionProto` and `ConnectionProto -> Connection`. This is the artifact
the full migration would template from; its round-trip fidelity is the POC's primary technical
result.

### Discovery / transport

- `connection-app` exposes a gRPC server on a fixed port (Spring gRPC server starter,
  `spring.grpc.server.port`).
- Clients build a channel to `bytechef.grpc.connection-app.address` (host:port) — plaintext within
  the trusted cluster network, matching the current REST posture.
- **Follow-up (documented):** resolve `connection-app` via the existing Spring Cloud LoadBalancer +
  `discovery-redis` so gRPC matches REST discovery. The riskiest integration unknown; deliberately
  out of this POC.

### Auth + tenant parity (must-have)

The REST `/remote/**` endpoints are guarded by `RemoteServiceAuthenticationFilter` (checks the
`TenantConstants.INTERNAL_SERVICE_TOKEN` header) and `RemoteMultiTenantFilter` (reads
`TenantConstants.CURRENT_TENANT_ID` and sets the tenant context). The gRPC POC mirrors both:
- **Client interceptor:** attaches `INTERNAL_SERVICE_TOKEN` and `CURRENT_TENANT_ID` as gRPC
  metadata on every call (token from the same config source the REST client uses; tenant from
  `TenantContext.getCurrentTenantId()`).
- **Server interceptor(s):** validate the internal-service token (reject `UNAUTHENTICATED`
  otherwise) and set `TenantContext.setCurrentTenantId(...)` for the duration of the call, clearing
  it after — behavioral parity with the two filters.

### Coexistence / rollback

`bytechef.remote.transport` (`rest` default | `grpc`) drives `@ConditionalOnProperty` on the two
client beans so exactly one `ConnectionService` remote implementation is active. Flipping to `grpc`
(and pointing `bytechef.grpc.connection-app.address` at connection-app) switches transport with no
code change; flipping back is instant rollback.

## Testing

- `ConnectionProtoMapperTest` — round-trip a fully-populated `Connection` (incl. a non-trivial
  `parameters` map and each enum) through proto and back; assert field equality.
- gRPC integration test — start an in-process gRPC server bound to the real service impl over a
  mocked `ConnectionService`, call both RPCs through the generated blocking stub, assert results and
  that the tenant/token metadata reached the server interceptor.
- The transport toggle is verified by a small context test asserting the right bean is active per
  property value.

## Risks / open items

- **spring-grpc + Spring Boot 4 / Gradle 9 compatibility:** the exact `spring-grpc` version and the
  protobuf/grpc-java Gradle plugin setup must be pinned to versions that build under this repo's
  toolchain — resolved as the first implementation step (add to the version catalog); a hard
  incompatibility here is the main POC risk and, if hit, is itself a valuable POC finding.
- **Struct fidelity:** `google.protobuf.Struct` represents numbers as doubles — integer-valued
  entries in `parameters` may need care on the return trip. The mapper test must cover this.
- **EE headers:** all new files under `server/ee/` carry the ByteChef Enterprise license header +
  `@version ee`.

## Rollout

Additive and dormant by default (`transport=rest`). No change to the REST path, no schema change, no
impact on the monolith (which doesn't use remote clients). The POC is judged by: does it build under
the repo toolchain, does the mapper round-trip cleanly, and does an in-process gRPC call with
tenant/token metadata succeed — yielding a go/no-go plus a per-service effort estimate for the full
migration.
