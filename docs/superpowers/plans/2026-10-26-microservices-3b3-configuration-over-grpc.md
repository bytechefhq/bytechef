# Microservices Plan 3b-3: Configuration Services over gRPC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every call another app makes to configuration-app goes over gRPC, and free-form values (maps, task inputs and outputs, data storage values) keep their exact Java types on every gRPC service, so the Connections page lists connections (plan 4 item 3) and no app talks REST to configuration-app.

**Architecture:** Task 1 changes how free-form values travel: proto `bytes` fields holding Jackson JSON replace `google.protobuf.Struct`/`Value` in all 3b-1 and 3b-4 protos, and `ProtoJsonMapper` handles them generically. Tasks 2-4 follow the 3b-4 shape per area (`*-remote-grpc-proto`, `*-remote-grpc` with mapper, servers and a gated `@Configuration`, existing `Remote*Client` rewritten on `GrpcChannels`, channel `configuration-app`). Task 5 wires configuration-app and deletes the three `*-configuration-remote-rest` modules.

**Tech Stack:** gRPC 1.83.1, protobuf 4.35.1, Spring Boot 4.1.1, Jackson 3, remote-grpc helpers (`ProtoJsonMapper`, `GrpcChannels`, `GrpcRemoteExceptionHandler`, `FixedGrpcChannels`), grpc-inprocess for wire tests.

**Spec:** `docs/superpowers/specs/2026-10-25-microservices-compose-design.md` (decisions 4 and 8). Pattern: plan 3b-4 (`docs/superpowers/plans/2026-10-26-microservices-3b4-connections-scheduler-storage-task-handler-over-grpc.md`) and its modules under `server/ee/libs/platform/platform-connection/platform-connection-remote-*`.

## Global Constraints

- gRPC replaces REST between services; typed proto fields for structure; **free-form values are `bytes` holding Jackson JSON** (user decision 2026-10-26); Java enums travel as strings.
- Per area: `*-remote-grpc-proto`, `*-remote-grpc` (mapper + servers + `@Configuration @ConditionalOnClass(name = "<impl class>")`), clients stay in `*-remote-client`.
- Calls that run component code use `bytechef.remote.grpc.execution-deadline`; all others the default deadline.
- Files under `server/ee/` carry the Enterprise license header and `@version ee`; CE files keep Apache 2.0. No prose comments in code or protos. Config defaults in yml.
- Commits: subject only, no ticket number, commit by path, never amend, never push.

## Decided here (Claude's calls, open to override)

- **The `bytes` rule.** In these protos every `bytes` field carries the Jackson JSON of the Java property with the same name, and nothing else uses `bytes`. `ProtoJsonMapper` walks the message descriptor: on `toProto` it replaces that property's JSON node with the base64 of its JSON text (proto JSON's form of `bytes`), on `toObject` it decodes it back into a node. Direct fields go through a new `JsonBytes` helper. An unset or empty `bytes` field means `null`; an empty map is `{}`.
- **Fidelity is the monolith's JSON fidelity.** `Long` stays `Long`, `Double 5.0` stays `Double`, an `Integer` stays `Integer`; a `BigDecimal` reads back as `Double`, exactly as it does through the monolith's own JSON columns. `ProtoValues`, `toExactNumber` and `toIntegralNumber` are deleted.
- **Only the methods remote apps call are implemented.** 33 methods across 14 clients make real REST calls today; they move to gRPC. Four of those calls never had a controller (`getPublishedProjects`, `copyWorkflowTemplate`, `getAppEvent`, `getIntegrationInstanceWorkflow`) and two are silent placeholders (`fetchWorkflow` returns empty, `getWorkflowTestConfigurationInputs` returns null); all six become real RPCs. The other ~150 methods stay `UnsupportedOperationException` stubs: configuration-app calls them locally, and the live run catches any that a remote app reaches.
- **`Workflow` travels by its persistent fields** (`id`, `definition`, `format`, `version`, `last_modified_date`, `source_type`, `metadata` as bytes) and is rebuilt with `new Workflow(id, definition, format, lastModifiedDate, metadata)`, `setVersion`, `setSourceType`, `setNew(false)`, so its tasks, inputs and outputs are parsed from the definition as everywhere else. `createdBy`, `createdDate` and `lastModifiedBy` have no setters and are not carried.
- **Server gates:** automation on `com.bytechef.automation.configuration.service.ProjectServiceImpl`, embedded on `com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl`, platform configuration and workflows on `com.bytechef.platform.configuration.service.WorkflowTestConfigurationServiceImpl`. Each module is on configuration-app's classpath only.
- **configuration-app and coordinator-app drop core `remote-rest`** once the three REST modules are gone; after this plan only execution-app (plan 3b-2) serves `/remote/**` REST.

## Review Focus

1. A `Long` beyond 2^53 and a `Double 5.0` in task inputs, task output, data storage values and connection parameters come back with the same Java type and value. Pinned in Task 1 (`testHandleKeepsNumberTypes`, `testDataStorageKeepsNumberTypes`, `testConnectionParametersKeepNumberTypes`).
2. A `null` free-form map stays `null` and an empty one stays empty (connection parameters, workflow test inputs). Pinned in Task 1 (`JsonBytesTest.testNullAndEmptyStayApart`) and Task 4 (`testGetWorkflowTestConfigurationInputsWithoutConfigurationIsNull`).
3. A workflow read remotely has the tasks parsed from its definition, its version and its last modified date. Pinned in Task 4 (`testWorkflowRoundTripKeepsDefinitionAndTasks`).
4. `fetchWorkflow` of an unknown id is `Optional.empty()`; `getWorkflow` of an unknown id fails with a message naming the id. Pinned in Task 4.
5. A connection used by a project deployment, an integration instance configuration or a workflow test configuration reports used (`isConnectionUsed` true) from each source. Pinned in Tasks 2, 3 and 4; live item 3.

---

### Task 1: Free-form values as Jackson JSON bytes

**Files:**
- Create: `server/ee/libs/core/remote/remote-grpc/src/main/java/com/bytechef/ee/remote/grpc/JsonBytes.java`
- Modify: `ProtoJsonMapper.java` (descriptor walk for `bytes` fields; delete `toExactNumber`, `toIntegralNumber`); delete `ProtoValues.java` and `ProtoValuesTest.java`
- Modify: every `.proto` under `server/ee/libs` using `google.protobuf.Struct`, `Value` or `ListValue` (component definition, property, task dispatcher definition, connection, scheduler, data storage, task handler): same field number and name, type `bytes` (`repeated Value refresh_on` becomes `bytes refresh_on`; `rpc GetAll ... returns (google.protobuf.Struct)` returns a new `JsonValue { bytes value = 1; }`); their mappers, servers and clients replace `ProtoValues` calls with `JsonBytes`
- Test: `JsonBytesTest`, `ProtoJsonMapperTest` (rewritten), and the wire tests named below

**Interfaces:**
- Produces: `public JsonBytes(ObjectMapper objectMapper)`; `ByteString write(@Nullable Object value)` (null → `ByteString.EMPTY`); `@Nullable Object read(ByteString bytes)`; `<T> @Nullable T read(ByteString bytes, TypeReference<T> typeReference)`; `@Nullable Map<String, Object> readMap(ByteString bytes)`. `ProtoJsonMapper` keeps its signatures.

- [ ] **Step 1: Write the failing tests.**
  - `JsonBytesTest`: `testKeepsNumberTypes` (map `{"id": 9007199254740993L, "ratio": 5.0, "count": 5}` reads back with `Long`, `Double`, `Integer` values equal to the input); `testNullAndEmptyStayApart` (`readMap(write(null))` is null, `readMap(write(Map.of()))` is an empty map).
  - `ProtoJsonMapperTest`: build a descriptor at test time (`DescriptorProtos.FileDescriptorProto` → `Descriptors.FileDescriptor.buildFrom`) for `message Holder { optional string name = 1; optional bytes payload = 2; repeated Holder children = 3; }` and map through `DynamicMessage.newBuilder(descriptor)`: `testBytesFieldsCarryJackson` (a record `Holder(String name, Map<String, Object> payload, List<Holder> children)` with a `Long` 9007199254740993, a `Double 5.0` and a nested child payload round-trips equal, with the same value types).
  - `TaskHandlerRemoteServiceGrpcWireTest.testHandleKeepsNumberTypes` (replaces `testHandleKeepsIntegerAndLargeNumberInputs`; parameters `count` 5, `id` 9007199254740993L, `ratio` 5.0 arrive as `Integer`, `Long`, `Double`; output `Map.of("id", 9007199254740993L)` returns a `Long`).
  - `JdbcDataStorageRemoteServiceGrpcWireTest.testDataStorageKeepsNumberTypes` (`put` of `9007199254740993L` then `fetch` returns a `Long`; a `Double 5.0` returns a `Double`).
  - `ConnectionRemoteServiceGrpcWireTest.testConnectionParametersKeepNumberTypes` (a connection with parameters `{"port": 5432, "big": 9007199254740993L, "ratio": 5.0}` reads back with `Integer`, `Long`, `Double`).
- [ ] **Step 2: Run** `remote-grpc:test` and the three client modules' `test`. Expected: FAIL (`JsonBytes` missing; `Long` arrives as `String`, `5.0` as `Integer`).
- [ ] **Step 3: Implement** `JsonBytes` and the `ProtoJsonMapper` descriptor walk (recursing into message fields and repeated message fields), then switch the protos and every mapper, server and client that used `ProtoValues`. `git grep -n "ProtoValues\|google.protobuf.Struct\|google.protobuf.Value\|ListValue" -- 'server/ee'` lists nothing.
- [ ] **Step 4: Run** `check` on every `*-remote-grpc`, `*-remote-grpc-proto` and `*-remote-client` module under `server/ee/libs`, and `testIntegration` of worker-app, connection-app, scheduler-app and execution-app. Expected: PASS.
- [ ] **Step 5: Commit** "Carry free-form values as Jackson JSON over gRPC".

---

### Task 2: Automation configuration over gRPC

**Files:**
- Create: `server/ee/libs/automation/automation-configuration/automation-configuration-remote-grpc-proto/` (`automation_configuration_remote_service.proto`, package `bytechef.automation.configuration.remote.v1`; messages `RemoteProject`, `RemoteProjectDeployment`, `RemoteProjectDeploymentWorkflow`, `RemoteProjectDeploymentWorkflowConnection` with one field per Jackson-serialized property, map-valued properties as `bytes`)
- Create: `.../automation-configuration-remote-grpc/` (`AutomationConfigurationProtoMapper`; `ProjectRemoteServiceGrpcServer`, `ProjectDeploymentRemoteServiceGrpcServer`, `ProjectDeploymentWorkflowRemoteServiceGrpcServer`; `AutomationConfigurationGrpcServerConfiguration` gated on `ProjectServiceImpl`)
- Modify: `RemoteProjectServiceClient`, `RemoteProjectDeploymentServiceClient`, `RemoteProjectDeploymentWorkflowServiceClient` (constructor `GrpcChannels`, `ObjectMapper`; channel `configuration-app`), the client module's `build.gradle.kts` (drop `remote-client`), `settings.gradle.kts`; domain classes only where the round trip fails (`@JsonCreator` private no-arg constructor, non-final field)
- Test: `AutomationConfigurationProtoMapperTest` (round trip of each of the four domain objects with every field set), `AutomationConfigurationRemoteServiceGrpcWireTest` in the client module

RPCs: `ProjectRemoteService` `GetProject`, `GetProjects`, `GetProjectDeploymentProject`, `GetWorkflowProject`; `ProjectDeploymentRemoteService` `GetProjectDeployment`; `ProjectDeploymentWorkflowRemoteService` `GetProjectDeploymentWorkflow`, `FetchProjectDeploymentWorkflowConnection` (response with `optional RemoteProjectDeploymentWorkflowConnection`), `GetProjectDeploymentWorkflowConnections`, `IsConnectionUsed`.

- [ ] **Step 1: Write the failing tests:** mapper round trips; one wire test per RPC asserting the mocked service received equal arguments and the client returned the mapped result; `testIsConnectionUsed` (mock true for 7, false for 8); `testFetchMissingConnectionIsEmpty`; `testGetUnknownProjectFailsWithItsMessage` (mock throws `IllegalArgumentException("Project 7 not found")`; the client's exception message contains it).
- [ ] **Step 2: Run** the new modules' and the client module's tests. Expected: FAIL.
- [ ] **Step 3: Implement** (domain fixes first).
- [ ] **Step 4: Run** `check` on the three modules and `automation-configuration-api`. Expected: PASS.
- [ ] **Step 5: Commit** domain fixes ("Let automation configuration be read back from JSON"), then "Serve automation configuration over gRPC".

---

### Task 3: Embedded configuration over gRPC

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-grpc-proto/` (`embedded_configuration_remote_service.proto`, package `bytechef.embedded.configuration.remote.v1`; messages for `Integration`, `IntegrationInstanceConfiguration`, `IntegrationInstanceConfigurationWorkflow`, `IntegrationInstanceConfigurationWorkflowConnection`, `IntegrationInstanceWorkflow`, `AppEvent`, `AutomationWorkflowProjectDTO`, built as in Task 2)
- Create: `.../embedded-configuration-remote-grpc/` (`EmbeddedConfigurationProtoMapper`; one server per service below; `EmbeddedConfigurationGrpcServerConfiguration` gated on `IntegrationServiceImpl`)
- Modify: the eight clients in `embedded-configuration-remote-client` (`RemoteIntegrationServiceClient`, `RemoteIntegrationInstanceConfigurationServiceClient`, `RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient`, `RemoteIntegrationInstanceWorkflowServiceClient`, `RemoteAppEventServiceClient`, `RemoteConnectedUserProjectDeploymentResolverClient`, `RemoteAutomationWorkflowProjectFacadeClient`, `RemoteConnectedUserProjectFacadeClient`), its `build.gradle.kts`, `settings.gradle.kts`; `RemoteConnectedUserProjectDeploymentResolverClientTest` is rewritten as part of the wire test
- Test: `EmbeddedConfigurationProtoMapperTest`, `EmbeddedConfigurationRemoteServiceGrpcWireTest`

RPCs: `IntegrationRemoteService` `GetIntegration`, `GetIntegrations`, `GetIntegrationInstanceIntegration`, `GetWorkflowIntegration`; `IntegrationInstanceConfigurationRemoteService` `GetIntegrationInstanceConfiguration`; `IntegrationInstanceConfigurationWorkflowRemoteService` `GetIntegrationInstanceConfigurationWorkflow`, `GetIntegrationInstanceConfigurationWorkflowConnection`, `IsConnectionUsed`; `IntegrationInstanceWorkflowRemoteService` `GetIntegrationInstanceWorkflow`; `AppEventRemoteService` `GetAppEvent`; `ConnectedUserProjectDeploymentResolverRemoteService` `GetProjectDeploymentIds`; `AutomationWorkflowProjectFacadeRemoteService` `GetPublishedProjects`; `ConnectedUserProjectFacadeRemoteService` `CopyWorkflowTemplate` (`Environment` as string).

- [ ] **Step 1: Write the failing tests:** as Task 2 Step 1 for these RPCs, with `testIsConnectionUsed` and `testCopyWorkflowTemplateReturnsTheNewWorkflowId`.
- [ ] **Step 2: Run** the new modules' and the client module's tests. Expected: FAIL.
- [ ] **Step 3: Implement** (domain fixes first).
- [ ] **Step 4: Run** `check` on the three modules and `embedded-configuration-api`. Expected: PASS.
- [ ] **Step 5: Commit** domain fixes ("Let embedded configuration be read back from JSON"), then "Serve embedded configuration over gRPC".

---

### Task 4: Platform configuration and workflows over gRPC

**Files:**
- Create: `server/ee/libs/platform/platform-configuration/platform-configuration-remote-grpc-proto/` (`platform_configuration_remote_service.proto`, package `bytechef.platform.configuration.remote.v1`; `RemoteWorkflow` per "Decided here", `RemoteComponentConnection` from the `ComponentConnection` record, workflow task and trigger arguments as `bytes` of their `toMap()`)
- Create: `.../platform-configuration-remote-grpc/` (`PlatformConfigurationProtoMapper` with `toWorkflow(RemoteWorkflow)`; `WorkflowRemoteServiceGrpcServer`, `WorkflowTestConfigurationRemoteServiceGrpcServer`, `ComponentConnectionFacadeRemoteServiceGrpcServer`; `PlatformConfigurationGrpcServerConfiguration` gated on `WorkflowTestConfigurationServiceImpl`)
- Modify: `RemoteWorkflowServiceClient`, `RemoteWorkflowTestConfigurationServiceClient`, `RemoteComponentConnectionFacadeClient`, the client module's `build.gradle.kts`, `settings.gradle.kts`
- Test: `PlatformConfigurationProtoMapperTest`, `PlatformConfigurationRemoteServiceGrpcWireTest`

RPCs: `WorkflowRemoteService` `GetWorkflow`, `FetchWorkflow` (response with `optional RemoteWorkflow`), `GetWorkflows` (by ids); `WorkflowTestConfigurationRemoteService` `IsConnectionUsed`, `GetWorkflowTestConfigurationInputs` (`bytes` inputs, unset means null); `ComponentConnectionFacadeRemoteService` `GetWorkflowTaskComponentConnections`, `GetWorkflowTriggerComponentConnections`.

- [ ] **Step 1: Write the failing tests:** `testWorkflowRoundTripKeepsDefinitionAndTasks` (a JSON workflow with two tasks, version 3 and a last modified date: the client's `getWorkflow` result has equal `getTasks()`, `getDefinition()`, `getVersion()`, `getLastModifiedDate()`, `getMetadata()`); `testFetchUnknownWorkflowIsEmpty`; `testGetUnknownWorkflowFailsWithItsMessage` (mock throws `IllegalArgumentException("Workflow abc not found")`); `testGetWorkflowsKeepsRequestOrder`; `testIsConnectionUsed`; `testGetWorkflowTestConfigurationInputs` (`{"id": 9007199254740993L}` returns a `Long`); `testGetWorkflowTestConfigurationInputsWithoutConfigurationIsNull`; `testGetComponentConnectionsForTaskAndTrigger` (the mocked facade receives a `WorkflowTask`/`WorkflowTrigger` whose `toMap()` equals the caller's).
- [ ] **Step 2: Run** the new modules' and the client module's tests. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `check` on the three modules. Expected: PASS.
- [ ] **Step 5: Commit** "Serve workflows and platform configuration over gRPC".

---

### Task 5: Wire configuration-app and delete the three REST modules

**Files:**
- Modify: `server/ee/apps/configuration-app/build.gradle.kts` (the three `*-configuration-remote-grpc` modules replace the three `*-configuration-remote-rest` modules; drop core `remote-rest`; `testImplementation("io.grpc:grpc-api")` if missing), `server/ee/apps/coordinator-app/build.gradle.kts` (drop core `remote-rest`), configuration-app's test yml (gRPC port 0, plaintext, token) if missing
- Delete: `automation-configuration-remote-rest`, `embedded-configuration-remote-rest`, `platform-configuration-remote-rest` and their `settings.gradle.kts` includes
- Test: `ConfigurationApplicationIntTest.testConfigurationGrpcServicesAreServed` (lists every service declared in Tasks 2-4)

- [ ] **Step 1: Write the failing service-listing test.**
- [ ] **Step 2: Run** configuration-app `testIntegration`. Expected: FAIL.
- [ ] **Step 3: Rewire and delete;** `git grep -n "configuration-remote-rest"` lists nothing, and `git grep -ln "LoadBalancedRestClient" -- 'server/ee/libs/*/src/main/*.java'` lists only execution-app clients (plan 3b-2).
- [ ] **Step 4: Run** `check testIntegration` on every app and `:server:apps:server-app:testIntegration`. Expected: PASS.
- [ ] **Step 5: Commit** wiring ("Serve configuration over gRPC from configuration-app") and deletion ("Remove the configuration REST modules") separately.

---

### Task 6: Live run

- [ ] **Step 1:** Rebuild every app image and bring the `bytechef-ms` stack up. Expected: all apps healthy.
- [ ] **Step 2:** Item 3: open Connections. Expected: the list shows "Live Airtable" (id 1050) with no HTTP error; edit its name and tags; delete a second connection created for the purpose.
- [ ] **Step 3:** Item 4 again: Live Workflow from the editor. Expected: DONE with a float output.
- [ ] **Step 4:** Synchronous webhook: give a workflow a webhook trigger, deploy its project, call the webhook URL. Expected: a response, or a stop at the first execution-app REST call (owned by plan 3b-2), recorded with the call that stopped it.
- [ ] **Step 5:** Fix only what this plan owns, each with a failing test first; record the rest in the ledger.
