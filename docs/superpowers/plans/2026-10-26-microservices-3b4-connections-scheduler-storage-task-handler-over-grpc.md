# Microservices Plan 3b-4: Connections, Scheduler, Data Storage and Task Handler over gRPC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connections, trigger and connection-refresh scheduling, component data storage and remote task handling go over gRPC with no stub left in their four clients, so plan 4's live checklist gets past creating a connection (item 3) and running a workflow from the editor (item 4).

**Architecture:** The same shape as plan 3b-1: a `*-remote-grpc-proto` contract and a `*-remote-grpc` module (mapper through `ProtoJsonMapper`, servers gated with `@ConditionalOnClass` on the implementation class) per area, and the existing `Remote*Client` classes rewritten on `GrpcChannels`. Connections are served by connection-app, scheduling by scheduler-app, data storage by execution-app (today no app serves it), task handling by the worker that offers the task's component. The four `*-remote-rest` modules are deleted.

**Tech Stack:** gRPC 1.83.1, protobuf 4.35.1, Spring Boot 4.1.1, the remote-grpc helpers from plan 3b-1 (`ProtoValues`, `ProtoJsonMapper`, `GrpcChannels`, `GrpcRemoteExceptionHandler`, `FixedGrpcChannels` test fixture), grpc-inprocess for wire tests.

**Spec:** `docs/superpowers/specs/2026-10-25-microservices-compose-design.md` (decisions 4 and 8). Pattern: plan 3b-1 (`docs/superpowers/plans/2026-10-26-microservices-3b1-definitions-over-grpc.md`) and its modules under `server/ee/libs/platform/platform-component/platform-component-remote-*`.

## Global Constraints

- gRPC replaces REST between services; typed proto fields, `google.protobuf.Struct`/`Value` for opaque maps and values; Java enums travel as strings (3b-1 ruling).
- Per area: `*-remote-grpc-proto`, `*-remote-grpc` (mapper + servers + `@Configuration @ConditionalOnClass(name = "<impl class>")`), clients stay in `*-remote-client`.
- Calls that run component code (task handling) use `bytechef.remote.grpc.execution-deadline`; all others the default deadline.
- Files under `server/ee/` carry the Enterprise license header and `@version ee`; CE files keep Apache 2.0. No prose comments in code. Config defaults in yml.
- Commits: subject only, no ticket number, commit by path, never amend, never push.

## Decided here (Claude's calls, open to override)

- **No stubs left in the four clients.** `RemoteConnectionFacadeClient` (11 methods, all stubs today), `RemoteConnectionServiceClient` (16), `RemoteTriggerSchedulerClient` and `RemoteConnectionRefreshSchedulerClient` (10), `RemoteJdbcDataStorageServiceClient` (5) and `RemoteTaskHandlerClient` (1) are implemented in full; they are thin CRUD or delegation calls, and finding them one by one in the live run costs more than implementing them.
- **Data storage is served by execution-app.** The client already targets `execution-app` and `data_entry` is in the `execution` Liquibase context, but no app includes the REST module today. execution-app gains `platform-data-storage-jdbc-service`.
- **Task handling goes to the worker that offers the component.** The component name is the task type's text before the first `/` (`randomHelper/v1/randomFloat` → `randomHelper`); when no worker advertises it (built-in handlers), the call goes to the balanced `worker-app` channel. The type travels in the message, not the URL, which also fixes today's 400 (Tomcat rejects the encoded slashes of the REST path variable).
- **Connection parameters cross the wire** as `Struct`, as they did over REST; they stay within the trusted network and the service token.

## Review Focus

1. A task whose input holds 64-bit ids or decimals must reach the worker unchanged (3b-1's `ProtoValues` sends those as strings; the handler must not see `5.0` for `5`). Pinned in Task 4 (`testHandleKeepsIntegerAndOrderedInputs`).
2. Creating a connection from the editor returns its id and the new connection lists with its tags; a connection without tags must list with an empty tag list, not null. Pinned in Task 1 (`testCreateAndListConnectionWithoutTags`).
3. A scheduled trigger's output map keeps its keys and types (cron schedule output feeds the first task). Pinned in Task 2 (`testScheduleScheduleTriggerPassesOutputUnchanged`).
4. Data storage `fetch` of a missing key returns `Optional.empty()` and `get` of a missing key fails with a message naming the key, not a gRPC `UNKNOWN`. Pinned in Task 3.
5. A task handler failing on the worker reaches the caller as a failed task with the worker's error message (the editor's run panel shows it). Pinned in Task 4 (`testHandleFailureCarriesTheWorkerMessage`).

---

### Task 1: Connections over gRPC

**Files:**
- Create: `server/ee/libs/platform/platform-connection/platform-connection-remote-grpc-proto/` (`connection_remote_service.proto`, package `bytechef.platform.connection.remote.v1`; messages `RemoteConnection`, `RemoteConnectionDTO`, `RemoteTag` following the Jackson shape of `Connection`, `ConnectionDTO`, `Tag`, built like plan 3b-1 Task 4)
- Create: `.../platform-connection-remote-grpc/` (`ConnectionProtoMapper` on `ProtoJsonMapper`; `ConnectionRemoteServiceGrpcServer`, `ConnectionFacadeRemoteServiceGrpcServer`; `ConnectionGrpcServerConfiguration` gated on `com.bytechef.platform.connection.service.ConnectionServiceImpl`)
- Modify: `RemoteConnectionServiceClient`, `RemoteConnectionFacadeClient` (constructor `GrpcChannels`, `ObjectMapper`; channel `connection-app`), the module's `build.gradle.kts`; `settings.gradle.kts`; domain classes only where the round trip fails (smallest fix: `@JsonCreator` private no-arg constructor, non-final field)
- Test: `ConnectionProtoMapperTest` (round trip of a `Connection` with every field set, including `parameters`, `tagIds` and `credentialStatus`, and of a `ConnectionDTO` with two tags), `ConnectionRemoteServiceGrpcWireTest` in the client module

Services: `ConnectionRemoteService` with one RPC per `ConnectionService` method, `ConnectionFacadeRemoteService` with one RPC per `ConnectionFacade` method (both `getConnections` overloads and both `update` overloads as separate RPCs). Nullable Java arguments are proto `optional`.

- [ ] **Step 1: Write the failing tests:** the mapper round trips; in the wire test one test per RPC asserting the mocked service or facade received equal arguments and the client returned the mapped result, plus `testCreateAndListConnectionWithoutTags` (facade `create` returns the id; `getConnections(...)` returns a DTO whose `tags` is an empty list) and `testGetConnectionOfUnknownIdFailsWithItsMessage` (mock throws `IllegalArgumentException("Connection 7 not found")`; the client's exception message contains it).
- [ ] **Step 2: Run** the new modules' and the client module's tests. Expected: FAIL.
- [ ] **Step 3: Implement** (domain fixes first).
- [ ] **Step 4: Run** `check` on the three modules and `platform-connection-api`. Expected: PASS.
- [ ] **Step 5: Commit** domain fixes ("Let connections be read back from JSON"), then "Serve connections over gRPC".

---

### Task 2: Scheduling over gRPC

**Files:**
- Create: `server/ee/libs/platform/platform-scheduler/platform-scheduler-remote-grpc-proto/` (`scheduler_remote_service.proto`, package `bytechef.platform.scheduler.remote.v1`; `WorkflowExecutionId` as its `toString()` text, read back with `WorkflowExecutionId.parse`; `Instant` as `google.protobuf.Timestamp`; output maps as `Struct`)
- Create: `.../platform-scheduler-remote-grpc/` (`TriggerSchedulerRemoteServiceGrpcServer`, `ConnectionRefreshSchedulerRemoteServiceGrpcServer`, configuration gated on `com.bytechef.platform.scheduler.QuartzTriggerScheduler`)
- Modify: `RemoteTriggerSchedulerClient`, `RemoteConnectionRefreshSchedulerClient` (channel `scheduler-app`), including the `cancelOneTimeTask` stub
- Test: `SchedulerRemoteServiceGrpcWireTest` in the client module

- [ ] **Step 1: Write the failing tests:** one per RPC (8 trigger, 2 refresh) asserting equal arguments on the mocked scheduler, plus `testScheduleScheduleTriggerPassesOutputUnchanged` (output `{"datetime": "2026-10-26T10:00", "hour": 10, "dayOfWeek": [1, 2]}` arrives `isEqualTo`).
- [ ] **Step 2: Run** the client module's tests. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `check` on the three modules. Expected: PASS.
- [ ] **Step 5: Commit** "Schedule triggers and connection refreshes over gRPC".

---

### Task 3: Data storage over gRPC, served by execution-app

**Files:**
- Create: `server/ee/libs/platform/platform-data-storage/platform-data-storage-jdbc/platform-data-storage-jdbc-remote-grpc-proto/` (`data_storage_remote_service.proto`; values as `google.protobuf.Value`, `getAll` as `Struct`)
- Create: `.../platform-data-storage-jdbc-remote-grpc/` (`JdbcDataStorageRemoteServiceGrpcServer`, configuration gated on `com.bytechef.platform.data.storage.jdbc.service.JdbcDataStorageServiceImpl`)
- Modify: `RemoteJdbcDataStorageServiceClient` (channel `execution-app`; `get` of a missing key throws `IllegalArgumentException("No data storage value for key <key>")`), the client's `build.gradle.kts`
- Modify: `server/ee/apps/execution-app/build.gradle.kts` (add `platform-data-storage-jdbc-service` and the new `-remote-grpc`)
- Test: `JdbcDataStorageRemoteServiceGrpcWireTest`; `ExecutionApplicationIntTest` gains `testDataStorageGrpcServiceIsServed`

- [ ] **Step 1: Write the failing tests:** `put` then `fetch` round trip of a nested map value; `fetch` of a missing key is empty; `get` of a missing key fails naming the key; `getAll`; `delete`; and the execution-app service listing.
- [ ] **Step 2: Run** the client module's tests and execution-app's `testIntegration`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `check` on the three modules and execution-app's `check testIntegration`. Expected: PASS.
- [ ] **Step 5: Commit** "Serve component data storage from execution-app over gRPC".

---

### Task 4: Remote task handling over gRPC

**Files:**
- Create: `server/ee/libs/platform/platform-workflow/platform-workflow-worker/platform-workflow-worker-remote-grpc-proto/` (`task_handler_remote_service.proto`: `Handle(HandleTaskRequest{string type; RemoteTaskExecution task_execution})` returns `ValueResponse`-shaped `HandleTaskResponse{optional google.protobuf.Value output}`; `RemoteTaskExecution` follows the Jackson shape of `TaskExecution`, maps as `Struct`)
- Create: `.../platform-workflow-worker-remote-grpc/` (`TaskExecutionProtoMapper` on `ProtoJsonMapper`, `TaskHandlerRemoteServiceGrpcServer` delegating to `TaskHandlerRegistry.getTaskHandler(type).handle(taskExecution)`, configuration gated on `com.bytechef.atlas.worker.config.TaskHandlerRegistryConfiguration`, which only worker-app includes; configuration-app and webhook-app hold a forwarding registry from `WorkerHandlerConfiguration` and must not serve)
- Modify: `RemoteTaskHandlerClient` (`GrpcChannels`, `DiscoveryClient`, `ApplicationProperties`, `ObjectMapper`; routing as decided above; `execution-deadline`), its configuration `WorkerHandlerConfiguration`, the client's `build.gradle.kts`
- Test: `TaskExecutionProtoMapperTest` (round trip of a `TaskExecution` with every field set), `TaskHandlerRemoteServiceGrpcWireTest`

- [ ] **Step 1: Write the failing tests:** `testHandleRoutesToTheWorkerOfferingTheComponent` (two instances, only the second advertises `randomHelper`; the call lands there); `testHandleWithoutAdvertisingWorkerUsesTheServiceChannel`; `testHandleKeepsIntegerAndOrderedInputs` (parameters `{"count": 5, "id": 9007199254740993, "zeta": 1, "alpha": 2}` reach the handler with `count` an `Integer`, `id` the string `"9007199254740993"`, keys in order); `testHandleFailureCarriesTheWorkerMessage` (handler throws `TaskExecutionException` with "Bad input"; the client throws an exception whose message contains "Bad input").
- [ ] **Step 2: Run** the new modules' and the client module's tests. Expected: FAIL.
- [ ] **Step 3: Implement** (domain fixes in `atlas-execution-api` first if the round trip needs them).
- [ ] **Step 4: Run** `check` on the touched modules. Expected: PASS.
- [ ] **Step 5: Commit** domain fixes, then "Hand tasks to workers over gRPC".

---

### Task 5: Wire the apps and delete the four REST modules

**Files:**
- Modify: `connection-app`, `scheduler-app`, `execution-app`, `worker-app` `build.gradle.kts` (servers: the new `-remote-grpc` modules replace the `-remote-rest` ones); consumers (`configuration-app`, `coordinator-app`, `webhook-app`, `worker-app`, `scheduler-app`, `execution-app`) keep their `-remote-client` dependencies
- Modify: `scheduler-app-dev.yml`, `connection-app-dev.yml`, `execution-app-dev.yml` (dev gRPC ports `1` + HTTP port, as plan 3b-1)
- Delete: `platform-connection-remote-rest`, `platform-scheduler-remote-rest`, `platform-data-storage-jdbc-remote-rest`, `platform-workflow-worker-remote-rest` and their `settings.gradle.kts` includes; `server/ee/libs/core/remote/remote-rest` and `remote-client` too if `git grep` shows no remaining user
- Test: each serving app's `*ApplicationIntTest` lists its new services (as plan 3b-1 Task 8)

- [ ] **Step 1: Write the failing service-listing tests.**
- [ ] **Step 2: Run** those apps' `testIntegration`. Expected: FAIL.
- [ ] **Step 3: Rewire and delete;** `git grep -n "remote-rest\|LoadBalancedRestClient\|DefaultRestClient"` lists only modules still owned by plans 3b-2 and 3b-3.
- [ ] **Step 4: Run** `check testIntegration` on every app and `:server:apps:server-app:testIntegration`. Expected: PASS.
- [ ] **Step 5: Commit** wiring and deletion as separate commits (`git rm` paths staged only in the deletion commit).

---

### Task 6: Live run of plan 4 Task 5, items 3 and 4

- [ ] **Step 1: Rebuild and start** the Compose stack (plan 4 commands). Expected: all healthy.
- [ ] **Step 2: Item 3:** open Connections, create a connection for an API-key component with a fake key; it lists with its tags. Record failures.
- [ ] **Step 3: Item 4:** run the Live Workflow from the editor (Random Helper, Random Float with its required inputs filled); the test output panel shows the result. Record failures.
- [ ] **Step 4: Fix only what this plan owns,** test-first; failures owned by 3b-2, 3b-3 or 3b-5 go to the ledger with their owner, and the run stops if they block.
- [ ] **Step 5: Commit** each fix on its own.
