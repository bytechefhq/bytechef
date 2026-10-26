# Microservices Plan 3b-2: User Identity and Execution Services over gRPC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A logged-in user's identity reaches every app a request passes through, every call to execution-app goes over gRPC, and REST between services is gone, so connections can be edited and deleted, the project deployment page loads, and a synchronous webhook answers.

**Architecture:** Task 1 adds a client/server interceptor pair to `remote-grpc` that carries the caller's login and authorities as call metadata and rebuilds the Spring `SecurityContext` on the server. Task 2 turns remote failures into the right HTTP status. Tasks 3 and 4 port the atlas and platform execution clients the 3b-3 way (`*-remote-grpc-proto`, `*-remote-grpc` with mapper, servers and a gated `@Configuration`, clients on `GrpcChannels`, channel `execution-app`). Task 5 wires execution-app and deletes the last REST modules, including core `remote-rest` and `remote-client`.

**Tech Stack:** gRPC 1.83.1, protobuf 4.35.1, Spring Boot 4.1.1, Spring Security, Jackson 3, remote-grpc helpers (`ProtoJsonMapper`, `JsonBytes`, `GrpcChannels`, `GrpcRemoteExceptionHandler`, `FixedGrpcChannels`), grpc-inprocess for wire tests.

**Spec:** `docs/superpowers/specs/2026-10-25-microservices-compose-design.md` (decisions 4 and 8). Pattern: plan 3b-3 (`docs/superpowers/plans/2026-10-26-microservices-3b3-configuration-over-grpc.md`) and its modules under `server/ee/libs/automation/automation-configuration/automation-configuration-remote-*`.

## Global Constraints

- gRPC replaces REST between services; typed proto fields for structure; free-form values are `bytes` holding Jackson JSON; Java enums travel as strings; Jackson creator problems are solved with mix-ins in the proto mapper, not annotations on domain classes.
- Per area: `*-remote-grpc-proto`, `*-remote-grpc` (mapper + servers + `@Configuration @ConditionalOnClass(name = "<impl class>")`), clients stay in `*-remote-client`.
- Calls that run component code or wait for a job use `bytechef.remote.grpc.execution-deadline`; all others the default deadline.
- Files under `server/ee/` carry the Enterprise license header and `@version ee`; CE files keep Apache 2.0. No prose comments in code or protos. Config defaults in yml.
- Commits: subject only, no ticket number, commit by path, never amend, never push.

## Decided here (Claude's calls, open to override)

- **Identity travels only beside the service token.** The client sends `x-bytechef-user-login` and `x-bytechef-user-authorities` (comma-separated) when the current `Authentication` is authenticated, not anonymous, and its principal is a `UserDetails` or a `String`. The server trusts them because `ServiceTokenServerInterceptor` has already rejected any call without the shared token. A call without a user (scheduler, coordinator, message listeners) carries no identity and runs exactly as before. Embedded API-key principals are not carried; plan 3b-5 owns them.
- **The server binds the identity for the handler only.** Like `TenantContextServerInterceptor`, it sets the `SecurityContext` in `onHalfClose` and restores the previous context afterwards, so nothing leaks to the next call on a pooled thread.
- **Remote failures keep their meaning over REST.** `GrpcRemoteExceptionHandler` maps `AccessDeniedException` to `PERMISSION_DENIED`. A new `@RestControllerAdvice` in `remote-grpc`, ordered before `GlobalResponseEntityExceptionHandler`, turns a `StatusRuntimeException` into a problem detail: `PERMISSION_DENIED` → 403, `UNAUTHENTICATED` → 401, `NOT_FOUND` → 404, `INVALID_ARGUMENT` → 400, everything else → 500. The detail is the status description.
- **`RemoteTaskExecution` moves down to atlas.** It and `TaskExecutionProtoMapper` move from `platform-workflow-worker-remote-grpc(-proto)` to `atlas-execution-remote-grpc(-proto)`, and the worker proto imports `atlas_execution_remote_service.proto`. If the protobuf plugin cannot resolve the import from the dependency jar, duplicate the three messages in the worker proto and record a ruling.
- **Only the methods remote apps call are implemented.** These are the ~38 methods that make real REST calls today, plus three placeholders that remote apps reach: `PrincipalJobService.fetchLastWorkflowJobId` (the project deployment page), `TriggerStateService.delete` (trigger disable), and `JobResumeFacade.resumeJob` (webhook-app's resume endpoint). `JobService.create(JobParametersDTO, Workflow)` stays a stub, because both of its callers (`JobFacadeImpl`, `PrincipalJobFacadeImpl`) run on execution-app. `resumeJobStreaming` stays a stub, because it takes a callback.
- **Two existing client bugs are fixed by the port.**
  - `RemoteTriggerLifecycleFacadeClient.executeTriggerDisable` posts to the enable path.
  - `TriggerRequest.connectionId` is a primitive `long`, which throws an NPE for a trigger without a connection (the 3b-3 live failure).
- **Server gates:** atlas on `com.bytechef.atlas.execution.service.JobServiceImpl`, platform execution on `com.bytechef.platform.workflow.execution.service.TriggerExecutionServiceImpl`. Both are on execution-app's classpath only.

## Review Focus

1. A call made with no logged-in user (a coordinator or scheduler thread) carries no identity, and the callee sees no `Authentication`. Pinned in Task 1 (`testCallWithoutUserCarriesNoIdentity`).
2. The identity bound for one call is gone for the next call on the same server thread. Pinned in Task 1 (`testIdentityIsClearedAfterTheCall`).
3. A user denied by a remote app gets HTTP 403 with the reason, not a 500. Pinned in Task 2 (`testPermissionDeniedBecomesForbidden`); live in Task 6.
4. Disabling a trigger disables it, and enabling a trigger without a connection works. Pinned in Task 4 (`testTriggerDisableCallsDisable`, `testTriggerEnableWithoutConnection`).
5. Job inputs, trigger state and task execution outputs keep their number types across the call. Pinned in Tasks 3 and 4 (`testJobKeepsNumberTypes`, `testTriggerStateKeepsNumberTypes`).

---

### Task 1: User identity over gRPC

**Files:**
- Create: `server/ee/libs/core/remote/remote-grpc/src/main/java/com/bytechef/ee/remote/grpc/interceptor/UserContextClientInterceptor.java`, `.../interceptor/UserContextServerInterceptor.java`
- Modify: `GrpcMetadataKeys` (`USER_LOGIN` = `x-bytechef-user-login`, `USER_AUTHORITIES` = `x-bytechef-user-authorities`), `ManagedGrpcChannels` (add the client interceptor next to `TenantContextClientInterceptor`), `GrpcRemoteConfiguration` (`@Bean @GlobalServerInterceptor UserContextServerInterceptor`), `remote-grpc/build.gradle.kts` (`implementation("org.springframework.security:spring-security-core")`)
- Test: `UserContextServerInterceptorTest`, `GrpcRemoteWireTest` (new cases)

**Interfaces:**
- Produces: `public UserContextClientInterceptor()`; `public UserContextServerInterceptor()`; both stateless.

- [ ] **Step 1: Write the failing tests.**
  - `GrpcRemoteWireTest`: an in-process server with `UserContextServerInterceptor` around `EchoService`. The echo handler records `SecurityContextHolder.getContext().getAuthentication()`. The client channel uses `UserContextClientInterceptor`.
    - `testCallCarriesTheUser`: the caller runs inside `SecurityContextHolder` holding `UsernamePasswordAuthenticationToken.authenticated("admin@localhost.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")))`. The handler sees principal `admin@localhost.com` and exactly those two authorities, and `isAuthenticated()` is true.
    - `testCallWithoutUserCarriesNoIdentity`: an empty context, and then an `AnonymousAuthenticationToken`. In both cases the handler sees a null `Authentication`.
  - `UserContextServerInterceptorTest.testIdentityIsClearedAfterTheCall`: drive the listener with metadata for `user@localhost.com` on the current thread. After `onHalfClose` returns, `SecurityContextHolder.getContext().getAuthentication()` is the one set before the call (null).
- [ ] **Step 2: Run** `:server:ee:libs:core:remote:remote-grpc:test`. Expected: FAIL (the interceptors don't exist).
- [ ] **Step 3: Implement both interceptors and register them.**
  - Client: send the user only for an authenticated, non-anonymous token whose principal is a `UserDetails` (its username) or a `String`.
  - Server: when `USER_LOGIN` is present, wrap `onHalfClose` in `SecurityUtils`-style save/set/restore using `UsernamePasswordAuthenticationToken.authenticated(login, null, authorities)`.
- [ ] **Step 4: Run** `check` on `remote-grpc`, and `testIntegration` of connection-app and configuration-app. Expected: PASS.
- [ ] **Step 5: Commit** "Carry the calling user across gRPC".

---

### Task 2: Remote failures keep their HTTP status

**Files:**
- Modify: `GrpcRemoteExceptionHandler` (`AccessDeniedException` → `PERMISSION_DENIED` with the message; `remote-grpc` already gets spring-security in Task 1)
- Create: `server/ee/libs/core/remote/remote-grpc/src/main/java/com/bytechef/ee/remote/grpc/GrpcStatusExceptionAdvice.java` (`@RestControllerAdvice @Order(Ordered.HIGHEST_PRECEDENCE)`, `@ExceptionHandler(StatusRuntimeException.class)` returning `ResponseEntity<ProblemDetail>`), registered from `GrpcRemoteConfiguration`; `implementation("org.springframework:spring-web")`
- Test: `GrpcRemoteExceptionHandlerTest.testAccessDeniedBecomesPermissionDenied`, `GrpcStatusExceptionAdviceTest`

**Interfaces:**
- Produces: `ResponseEntity<ProblemDetail> handleStatusRuntimeException(StatusRuntimeException statusRuntimeException)`.

- [ ] **Step 1: Write the failing tests.**
  - `testAccessDeniedBecomesPermissionDenied`: `new AccessDeniedException("Only the connection creator or an admin can modify connection 7")` → code `PERMISSION_DENIED`, description equal to the message.
  - `GrpcStatusExceptionAdviceTest` (plain unit test):
    - `testPermissionDeniedBecomesForbidden`: status 403, detail equal to the description.
    - `testStatusesMapToHttp`: `UNAUTHENTICATED` → 401, `NOT_FOUND` → 404, `INVALID_ARGUMENT` → 400, `INTERNAL` → 500.
- [ ] **Step 2: Run** the `remote-grpc` tests. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `check` on `remote-grpc`. Expected: PASS.
- [ ] **Step 5: Commit** "Answer remote failures with their HTTP status".

---

### Task 3: Atlas execution over gRPC

**Files:**
- Create: `server/ee/libs/atlas/atlas-execution/atlas-execution-remote-grpc-proto/` (`atlas_execution_remote_service.proto`, package `bytechef.atlas.execution.remote.v1`). Messages:
  - `RemoteTaskExecution`, `RemoteExecutionError`, `RemoteFileEntry`, moved from the worker proto with the same fields.
  - `RemoteJob`: one field per Jackson-serialized property of `Job`; `inputs` and `metadata` as `bytes`; `outputs` as `RemoteFileEntry`; `webhooks` as a repeated message.
  - `RemoteJobParameters` for `JobParametersDTO`.
- Create: `.../atlas-execution-remote-grpc/` with:
  - `AtlasExecutionProtoMapper`; `TaskExecutionProtoMapper`, moved here.
  - Servers `JobFacadeRemoteServiceGrpcServer`, `JobRemoteServiceGrpcServer`, `TaskExecutionRemoteServiceGrpcServer`, `ContextRemoteServiceGrpcServer`, `CounterRemoteServiceGrpcServer`.
  - `AtlasExecutionGrpcServerConfiguration`, gated on `JobServiceImpl`.
- Modify:
  - The five clients in `atlas-execution-remote-client` (constructor `GrpcChannels`, `ObjectMapper`; channel `execution-app`) and its `build.gradle.kts` (drop `remote-client`).
  - The worker proto and worker modules, to use the moved messages and mapper.
  - `settings.gradle.kts`.
- Test: `AtlasExecutionProtoMapperTest` (round trip of `Job`, `TaskExecution`, `JobParametersDTO` and `FileEntry`, every field set), `AtlasExecutionRemoteServiceGrpcWireTest` in the client module; the worker wire tests stay green

RPCs:
- `JobFacadeRemoteService`: `CreateJob`.
- `JobRemoteService`: `FetchLastWorkflowJob`, `GetJob`, `GetTaskExecutionJob`, `ResumeToStatusStarted`, `SetStatusToStarted`, `SetStatusToStopped`, `Update`.
- `TaskExecutionRemoteService`: `CancelIfUnfinished`, `CompleteIfNotCancelled`, `Create`, `GetJobTaskExecutions`, `GetParentTaskExecutions`, `GetTaskExecution`, `GetTaskExecutionForUpdate`, `Update`.
- `ContextRemoteService`: `Peek` and `Push` (`optional int32 sub_stack_id`, `classname` as string).
- `CounterRemoteService`: `Decrement`, `Delete`, `Set`.

- [ ] **Step 1: Write the failing tests.**
  - Mapper round trips.
  - One wire test per RPC: the mocked service receives equal arguments and the client returns the mapped result.
  - `testJobKeepsNumberTypes`: job inputs `{"id": 9007199254740993L, "ratio": 5.0}` come back as `Long` and `Double`.
  - `testFetchLastWorkflowJobWithoutJobIsEmpty`.
  - `testPeekWithAndWithoutSubStack`: two calls, each mocked overload hit once.
  - `testGetUnknownJobFailsWithItsMessage`: the mock throws `IllegalArgumentException("Job 7 not found")`.
- [ ] **Step 2: Run** the new modules' and the client module's tests. Expected: FAIL.
- [ ] **Step 3: Implement** (move the worker messages first, and keep the worker wire tests green).
- [ ] **Step 4: Run** `check` on the new modules, the client module and the three `platform-workflow-worker-remote-*` modules, and `testIntegration` of worker-app. Expected: PASS.
- [ ] **Step 5: Commit** "Serve atlas execution over gRPC".

---

### Task 4: Platform workflow execution over gRPC

**Files:**
- Create: `server/ee/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-remote-grpc-proto/` (`platform_workflow_execution_remote_service.proto`, package `bytechef.platform.workflow.execution.remote.v1`). It imports the atlas proto for `RemoteJob`, `RemoteJobParameters` and `RemoteFileEntry`. Its own messages:
  - `RemoteTriggerExecution`: one field per Jackson-serialized property; `metadata`, `parameters` and `state` as `bytes`; `workflow_trigger` as `bytes` of its `toMap()`; `workflow_execution_id` as a string.
  - `RemoteWorkflowNodeType`: `name`, `version`, `optional operation`.
- Create: `.../platform-workflow-execution-remote-grpc/` with:
  - `PlatformWorkflowExecutionProtoMapper`.
  - Servers `TriggerLifecycleFacadeRemoteServiceGrpcServer`, `ConnectionLifecycleFacadeRemoteServiceGrpcServer`, `JobResumeFacadeRemoteServiceGrpcServer`, `PrincipalJobFacadeRemoteServiceGrpcServer`, `PrincipalJobRemoteServiceGrpcServer`, `TriggerExecutionRemoteServiceGrpcServer`, `TriggerStateRemoteServiceGrpcServer`.
  - `PlatformWorkflowExecutionGrpcServerConfiguration`, gated on `TriggerExecutionServiceImpl`.
- Modify: the seven clients in `platform-workflow-execution-remote-client`, its `build.gradle.kts`, `settings.gradle.kts`
- Test: `PlatformWorkflowExecutionProtoMapperTest`, `PlatformWorkflowExecutionRemoteServiceGrpcWireTest`

RPCs:
- `TriggerLifecycleFacadeRemoteService`: `ExecuteTriggerDisable`, `ExecuteTriggerEnable`. Both use the execution deadline; `optional int64 connection_id`; `optional string webhook_url`.
- `ConnectionLifecycleFacadeRemoteService`: `ScheduleConnectionRefresh` (`parameters` as bytes), `DeleteScheduledConnectionRefresh`.
- `JobResumeFacadeRemoteService`: `ResumeExpiredJob`, `ResumeJob` (`data` as bytes). Both return the outcome as a string.
- `PrincipalJobFacadeRemoteService`: `CreateChildJob`, `CreateJob`, `CreateSyncJob` (execution deadline). `PlatformType` travels as a string.
- `PrincipalJobRemoteService`: `FetchJobPrincipalId`, `FetchLastJobId`, `FetchLastWorkflowJobId` (`repeated string workflow_ids`). Each returns `optional int64`.
- `TriggerExecutionRemoteService`: `Create`, `GetTriggerExecution`, `Update`.
- `TriggerStateRemoteService`: `Delete`, `FetchValue` (`optional bytes`), `Save`.

- [ ] **Step 1: Write the failing tests.**
  - Mapper round trip of `TriggerExecution` with every field set, including a `WorkflowTrigger` whose `toMap()` comes back equal.
  - One wire test per RPC.
  - `testTriggerDisableCallsDisable`: the mock verifies `executeTriggerDisable`, never `executeTriggerEnable`.
  - `testTriggerEnableWithoutConnection`: a null `connectionId` arrives as null.
  - `testFetchLastWorkflowJobIdWithoutJobIsEmpty`.
  - `testTriggerStateKeepsNumberTypes`: `9007199254740993L` returns a `Long`; an unset value returns `Optional.empty()`.
  - `testResumeJobReturnsTheOutcome`: `GONE`.
  - `testCreateSyncJobReturnsTheJob`.
- [ ] **Step 2: Run** the new modules' and the client module's tests. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `check` on the three modules. Expected: PASS.
- [ ] **Step 5: Commit** "Serve platform workflow execution over gRPC".

---

### Task 5: Wire execution-app and delete REST between services

**Files:**
- Modify:
  - `server/ee/apps/execution-app/build.gradle.kts`: add `atlas-execution-remote-grpc` and `platform-workflow-execution-remote-grpc`; drop `platform-workflow-execution-remote-rest` and core `remote-rest`.
  - The `build.gradle.kts` of `embedded-ai-mcp-remote-client`, `platform-ai-skill-remote-client` and `platform-notification-remote-client`: drop `remote-client`, which none of them uses.
  - Any app that still declares `remote-client` or `remote-rest`.
- Delete: `atlas-execution-remote-rest`, `platform-workflow-execution-remote-rest`, `server/ee/libs/core/remote/remote-rest`, `server/ee/libs/core/remote/remote-client`, and their `settings.gradle.kts` includes.
- Test: `ExecutionApplicationIntTest.testExecutionGrpcServicesAreServed` (lists every service declared in Tasks 3 and 4)

- [ ] **Step 1: Write the failing service-listing test.**
- [ ] **Step 2: Run** execution-app `testIntegration`. Expected: FAIL.
- [ ] **Step 3: Rewire and delete.** These must list nothing:
  - `git grep -n "remote-rest\|core:remote:remote-client\|LoadBalancedRestClient" -- ':!docs'`
  - `git grep -ln "/remote/" -- 'server/ee/**/src/main/**/*.java'`
- [ ] **Step 4: Run** `check testIntegration` on every app, `check` on every remote module, and `:server:apps:server-app:testIntegration`. Expected: PASS.
- [ ] **Step 5: Commit** wiring ("Serve execution over gRPC from execution-app") and deletion ("Remove REST between microservices") separately.

---

### Task 6: Live run

- [ ] **Step 1:** Rebuild every app image and bring the `bytechef-ms` stack up. Expected: all apps healthy.
- [ ] **Step 2:** Connections. Expected:
  - Renaming "Live Airtable" and tagging it `crm` returns 204, and the list shows the change.
  - Deleting connection 1051 ("Delete Me", left by plan 3b-3) returns 204.
  - The connection-app log shows the call ran as `ms-admin@localhost.com`.
- [ ] **Step 3:** Project deployments. Expected:
  - The page lists deployment 1051 without an HTTP error.
  - Enabling it returns 200, and `trigger_1` of workflow 1053 is enabled.
- [ ] **Step 4:** Synchronous webhook. Call the webhook URL of deployment 1051. Expected: an HTTP response carrying the Random Helper output; the execution appears under Workflow Executions.
- [ ] **Step 5:** Item 4 again (editor Test of Live Workflow). Expected: DONE with a float output.
- [ ] **Step 6:** Fix only what this plan owns, each with a failing test first; record the rest in the ledger.
