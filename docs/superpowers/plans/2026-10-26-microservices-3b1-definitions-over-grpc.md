# Microservices Plan 3b-1: Definitions and Component Execution over gRPC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The workflow editor gets component, action, trigger, connection and task dispatcher definitions, and runs component code (options, dynamic properties, outputs, trigger and action execution), over gRPC instead of the broken REST remote calls, so plan 4's live checklist can continue from item 2.

**Architecture:** Typed proto contracts in `*-remote-grpc-proto` modules, a proto mapper and gRPC servers in `*-remote-grpc` modules, and the existing `Remote*Client` classes rewritten to call gRPC, exactly as plan 3 did for users. Worker-hosted services are called on the specific worker instance that advertises the component (discovery `components` metadata), so the shared channel layer gains a per-instance channel. Platform filtering (automation vs embedded) moves to the calling side, where the filters live. The two REST modules these replace are deleted.

**Tech Stack:** gRPC 1.83.1, protobuf 4.35.1 (Boot 4.1.1 managed), `com.google.protobuf` Gradle plugin 0.10.0, `protobuf-java-util` (JsonFormat), grpc-inprocess for wire tests, Spring Boot 4.1.1.

**Spec:** `docs/superpowers/specs/2026-10-25-microservices-compose-design.md` (decisions 4 and 8, step 4). Pattern to copy: plan 3 (`docs/superpowers/plans/2026-10-26-microservices-3-grpc-foundation.md`) and its user modules under `server/ee/libs/platform/platform-user/platform-user-remote-grpc*` and `platform-user-remote-client`.

**Plan 3b split (user's call, 2026-10-26):** 3b-1 definitions and component execution (this plan, served by worker-app and coordinator-app); 3b-2 execution-app services; 3b-3 configuration-app services; 3b-4 connection, scheduler, data storage and task handler, then delete the remaining `*-remote-rest`; 3b-5 JWT/signing-key lookup and API-key auth on execution-app and webhook-app.

## Global Constraints

- gRPC replaces REST between services; no transport flag is kept. Typed proto fields, `google.protobuf.Struct` for opaque maps (spec decision 4).
- Module layout per service: `*-remote-grpc-proto` (contract), `*-remote-grpc` (mapper + servers, `@ConditionalOnClass(name = "<implementation class>")` gate), client classes stay in `*-remote-client` (plan 3).
- Service token and tenant context ride the shared interceptors from `remote-grpc`; no service adds its own.
- Files under `server/ee/` carry the ByteChef Enterprise license header and `@version ee` on classes; CE files keep Apache 2.0.
- Config defaults live in yml, never as Java initializers on `ApplicationProperties`.
- No prose comments in code; short interface docs are fine.
- Commits: subject only, no ticket number, no trailers; commit by path; generated files and Liquibase in their own commits; never push.
- No em dashes in user-facing prose.

## Decided here (Claude's calls, open to override)

- **Proto to domain goes through protobuf `JsonFormat` and `JsonUtils`.** The definition domain classes (`com.bytechef.platform.component.domain.*`, `com.bytechef.platform.workflow.task.dispatcher.domain.*`) have no setters and are built only from SDK definitions, whose constructors read icons from the worker's classpath; rebuilding them on the caller needs either ~50 new constructors or Jackson's private-constructor path. The wire stays fully typed (the user's choice): `toProto` writes the domain object with `JsonUtils` and parses it with `JsonFormat.parser()` (unknown fields fail, so a domain field without a proto field breaks the mapper test); `toDomain` prints the proto with `JsonFormat.printer()` and reads it with `JsonUtils`. Each domain class that Jackson cannot build gets the smallest fix (a private no-arg constructor or a subtype registration), found by the round-trip tests.
- **Platform filtering on the caller.** `ComponentDefinitionServiceImpl.getComponentDefinitions(..., platformType)` skips the platform filter when `platformType` is null; the worker is called with null and the client applies the `ComponentDefinitionFilter` beans of its own app. The filters (`ProjectComponentDefinitionFilter`, `IntegrationComponentDefinitionFilter`) live in the configuration services, never on a worker.
- **One property message.** `property.proto` holds one `RemoteProperty` message (type enum plus the union of per-type fields) used by component and task dispatcher definitions alike; the task dispatcher proto imports it.
- **Message size.** Definition lists carry SVG icons for every component; channels and servers accept inbound messages up to `bytechef.remote.grpc.max-inbound-message-size` (default `64MB` in `microservice.yml`).

## Review Focus

1. Integral numbers inside input maps cross `Struct` as doubles; a component reading `inputParameters.getInteger("count")` must still get `5`, not `5.0`. Pinned in Task 1 (`ProtoValuesTest.testIntegralNumbersComeBackAsIntegerOrLong`).
2. Two workers advertising the same component return the definition twice; the editor must list it once. Pinned in Task 5 (`testDefinitionsFromTwoWorkersAreMergedOncePerNameAndVersion`).
3. A component no live worker advertises must fail with a message naming the component, not a `NullPointerException`. Pinned in Task 5 (`testComponentOnNoWorkerFailsNamingTheComponent`).
4. All component definitions with icons exceed gRPC's default 4 MiB inbound limit. Pinned in Task 2 (`testChannelAcceptsConfiguredInboundMessageSize`) and measured live in Task 9.
5. `getComponentDefinition(name, null)` and `getTaskDispatcherDefinition(name, null)` mean "latest"; an unset optional must reach the worker as null, not 0. Pinned in Task 5 and Task 7 wire tests.

---

### Task 1: Struct and Value helpers

**Files:**
- Create: `server/ee/libs/core/remote/remote-grpc/src/main/java/com/bytechef/ee/remote/grpc/ProtoValues.java`
- Modify: `server/ee/libs/core/remote/remote-grpc/build.gradle.kts` (`api("com.google.protobuf:protobuf-java-util")` if the catalog or Boot BOM manages it; otherwise add it to `libs.versions.toml` at the protobuf version)
- Test: `server/ee/libs/core/remote/remote-grpc/src/test/java/com/bytechef/ee/remote/grpc/ProtoValuesTest.java`

**Interfaces:**
- Produces: `ProtoValues.toStruct(@Nullable Map<String, ?> map): Struct` (null → empty), `ProtoValues.toMap(Struct struct): Map<String, Object>`, `ProtoValues.toValue(@Nullable Object object): Value`, `ProtoValues.toObject(Value value): @Nullable Object`. Values other than null, Boolean, Number, String, Map and List are first converted with `JsonUtils.convertValue(object, Object.class)`. Integral doubles come back as `Integer` when they fit, else `Long`; other numbers as `Double`.

- [ ] **Step 1: Write the failing tests** in `ProtoValuesTest` (with `@ExtendWith(ObjectMapperSetupExtension.class)`):
  - `testMapRoundTrip`: `{"name": "a", "count": 5, "ratio": 0.5, "flag": true, "nothing": null, "list": [1, "x"], "nested": {"k": "v"}}` survives `toMap(toStruct(map))` unchanged (`isEqualTo`, so `count` is `Integer` 5).
  - `testIntegralNumbersComeBackAsIntegerOrLong`: `toObject(toValue(5))` is `Integer` 5, `toObject(toValue(5_000_000_000L))` is `Long`, `toObject(toValue(2.5))` is `Double`.
  - `testNullMapIsEmptyStruct`: `toStruct(null).getFieldsCount()` is 0.
  - `testLocalDateBecomesIsoString`: `toObject(toValue(LocalDate.of(2026, 10, 26)))` is `"2026-10-26"`.
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:core:remote:remote-grpc:test --tests '*ProtoValuesTest'`. Expected: FAIL (class missing).
- [ ] **Step 3: Implement `ProtoValues`.**
- [ ] **Step 4: Run** `./gradlew :server:ee:libs:core:remote:remote-grpc:check`. Expected: PASS.
- [ ] **Step 5: Commit** `git add server/ee/libs/core/remote/remote-grpc gradle/libs.versions.toml` with "Convert maps and values to protobuf Struct and Value for gRPC calls".

---

### Task 2: Per-instance channels and inbound message size

**Files:**
- Modify: `server/ee/libs/core/remote/remote-grpc/src/main/java/com/bytechef/ee/remote/grpc/GrpcChannels.java` (drop `@FunctionalInterface`; add `Channel getChannel(ServiceInstance serviceInstance)`)
- Modify: `.../ManagedGrpcChannels.java` (instance channels cached by `serviceInstance.getInstanceId()`, target `host:<grpc-port metadata>`, same interceptors and plaintext/TLS switch; `maxInboundMessageSize` on every channel)
- Modify: `.../GrpcRemoteConfiguration.java` (server side: apply the same size to the gRPC server, through the Spring gRPC server customizer or `spring.grpc.server.max-inbound-message-size` mapped in `microservice.yml`)
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (`Remote.Grpc.maxInboundMessageSize: DataSize`, no initializer)
- Modify: `server/ee/libs/config/microservice-config/src/main/resources/config/microservice.yml` (`bytechef.remote.grpc.max-inbound-message-size: 64MB`)
- Test: `server/ee/libs/core/remote/remote-grpc/src/test/java/com/bytechef/ee/remote/grpc/ManagedGrpcChannelsTest.java` (create, or extend if it exists)

**Interfaces:**
- Produces: `GrpcChannels.getChannel(ServiceInstance serviceInstance): Channel`. Throws `IllegalStateException("Service instance <instanceId> of <serviceId> publishes no grpc-port")` when the metadata is missing.

- [ ] **Step 1: Write the failing tests:** `testInstanceChannelIsCachedPerInstanceId` (same instance twice → same `Channel`; two instance ids → two channels); `testInstanceWithoutGrpcPortFails` (message names the instance id); `testChannelAcceptsConfiguredInboundMessageSize` (in-process server returning a 6 MiB `bytes` payload: call succeeds with `max-inbound-message-size=8MB`, fails with `RESOURCE_EXHAUSTED` at the 4 MiB default when the property is unset; build the channel through the same builder method `ManagedGrpcChannels` uses so the test exercises the production setting).
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:core:remote:remote-grpc:test`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :server:ee:libs:core:remote:remote-grpc:check :server:libs:config:app-config:check`. Expected: PASS.
- [ ] **Step 5: Commit** "Open gRPC channels to a single service instance and accept large definition messages".

---

### Task 3: Unfiltered definitions when no platform type is given

**Files:**
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/ComponentDefinitionServiceImpl.java` (both `getComponentDefinitions(..., PlatformType)` overloads: `platformType == null` → no platform filter)
- Test: the existing `ComponentDefinitionServiceImpl` test class in that module (`ComponentDefinitionServiceTest` or `...IntTest`; add there, do not create a second class)

- [ ] **Step 1: Write the failing test** `testGetComponentDefinitionsWithoutPlatformTypeSkipsPlatformFilter`: with a filter bean that rejects component `request` for AUTOMATION, `getComponentDefinitions(true, null, null, true, null, null)` contains `request`, and with `PlatformType.AUTOMATION` it does not.
- [ ] **Step 2: Run** the module's test task. Expected: FAIL with `Unsupported mode type: null`.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew :server:libs:platform:platform-component:platform-component-service:check`. Expected: PASS.
- [ ] **Step 5: Commit** "Return component definitions without platform filtering when no platform type is given".

---

### Task 4: Component definition contract and mapper

**Files:**
- Create: `server/ee/libs/platform/platform-component/platform-component-remote-grpc-proto/` (`build.gradle.kts` as `platform-user-remote-grpc-proto`; `src/main/proto/property.proto`, `component_definition_remote_service.proto`; package `bytechef.platform.component.remote.v1`, `java_package = "com.bytechef.ee.platform.component.remote.grpc.proto"`)
- Create: `server/ee/libs/platform/platform-component/platform-component-remote-grpc/` (`build.gradle.kts`; `ComponentProtoMapper.java` in `com.bytechef.ee.platform.component.remote.grpc`)
- Modify: `settings.gradle.kts` (both includes)
- Modify: domain classes under `com.bytechef.platform.component.domain` only where the round-trip test shows Jackson cannot build them (smallest fix each)
- Test: `server/ee/libs/platform/platform-component/platform-component-remote-grpc/src/test/java/com/bytechef/ee/platform/component/remote/grpc/ComponentProtoMapperTest.java`

Contract rules (so every message is determined): one message per domain class in `com.bytechef.platform.component.domain` and per facade result type (`OutputResponse`, `WebhookEnableOutput`, `TriggerOutput`, `WebhookRequest`, `WebhookValidateResponse`), named `Remote<Class>`; one field per Jackson-serialized property, snake_case of the property name; `Object` properties as `google.protobuf.Value`, `Map` properties as `google.protobuf.Struct`, enums as proto enums with the Java constant names; `optional` on every scalar. `RemoteProperty` carries a `PropertyType` enum and the union of the per-type fields, nested properties as `repeated RemoteProperty`.

Services in `component_definition_remote_service.proto` (request messages typed field by field from the client signatures; `optional int32 version` where Java has `@Nullable Integer`):
- `ComponentDefinitionRemoteService`: `GetComponentDefinition`, `GetComponentDefinitions` (flags and `repeated string include`; no platform type), `GetComponentDefinitionVersions`.
- `ActionDefinitionRemoteService`: `GetActionDefinition`, `GetActionDefinitions`.
- `TriggerDefinitionRemoteService`: `GetTriggerDefinition`, `GetTriggerDefinitions`, `GetWebhookTriggerFlags`, `ExecuteWorkflowNodeDescription`.
- `ConnectionDefinitionRemoteService`: `GetAuthorizationType`, `GetConnectionDefinition`, `GetComponentConnectionDefinitions`, `GetConnectionDefinitions`.
- `ActionDefinitionFacadeRemoteService`: `ExecuteDynamicProperties`, `ExecuteOptions` (both overloads: one request with `optional int64 connection_id`, `map<string, int64> connection_ids` and `Struct extensions`), `ExecuteOutput`, `ExecutePerform`.
- `TriggerDefinitionFacadeRemoteService`: `ExecuteDynamicProperties`, `ExecuteDynamicWebhookRefresh`, `ExecuteListenerDisable`, `ExecuteListenerEnable`, `ExecuteOptions`, `ExecuteOutput`, `ExecuteTrigger`, `ExecuteWebhookDisable`, `ExecuteWebhookEnable`, `ExecuteWebhookValidate`.

**Interfaces:**
- Produces: `ComponentProtoMapper` with static `toProto(X)`/`toX(RemoteX)` pairs for `ComponentDefinition`, `ActionDefinition`, `TriggerDefinition`, `ConnectionDefinition`, `Property` (and lists), `Option`, `WebhookTriggerFlags`, `OutputResponse`, `WebhookEnableOutput`, `TriggerOutput`, `WebhookRequest`, `WebhookValidateResponse`; implemented through `JsonFormat` and `JsonUtils` as decided above. Task 7 imports `property.proto` from this module.

- [ ] **Step 1: Write the failing tests** (`@ExtendWith(ObjectMapperSetupExtension.class)`): `testComponentDefinitionsRoundTrip` builds `new ComponentDefinition(handler.getDefinition())` for the `ComponentHandler`s of the modules `:server:libs:modules:components:http-client`, `:server:libs:modules:components:ai:agent` (cluster root), `:server:libs:modules:components:data-mapper`, `:server:libs:modules:components:webhook` and `:server:libs:modules:components:schedule` (added as `testImplementation`, handlers loaded with `ServiceLoader.load(ComponentHandler.class)`) and asserts `toComponentDefinition(toProto(definition))` `usingRecursiveComparison().isEqualTo(definition)`; `testFacadeResultsRoundTrip` does the same for one hand-built instance of each facade result type with every field set.
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:platform:platform-component:platform-component-remote-grpc:test`. Expected: FAIL (modules missing).
- [ ] **Step 3: Write the protos and the mapper;** run the test, and for each domain class Jackson cannot build, apply the smallest fix in the CE domain class and rerun.
- [ ] **Step 4: Run** `./gradlew :server:ee:libs:platform:platform-component:platform-component-remote-grpc:check :server:libs:platform:platform-component:platform-component-api:check`. Expected: PASS.
- [ ] **Step 5: Commit** domain fixes first ("Let component definition domain objects be read back from JSON"), then the two modules and `settings.gradle.kts` ("Add the gRPC contract for component definitions and component execution").

---

### Task 5: Definition services over gRPC

**Files:**
- Create in `platform-component-remote-grpc`: `ComponentDefinitionRemoteServiceGrpcServer`, `ActionDefinitionRemoteServiceGrpcServer`, `TriggerDefinitionRemoteServiceGrpcServer`, `ConnectionDefinitionRemoteServiceGrpcServer`, `ComponentGrpcServerConfiguration` (`@ConditionalOnClass(name = "com.bytechef.platform.component.service.ComponentDefinitionServiceImpl")`)
- Modify in `platform-component-remote-client`: `RemoteComponentDefinitionServiceClient`, `RemoteActionDefinitionServiceClient`, `RemoteTriggerDefinitionServiceClient`, `RemoteConnectionDefinitionServiceClient` (constructor takes `GrpcChannels`, `DiscoveryClient` and, for the component client, `List<ComponentDefinitionFilter>`; no `DefaultRestClient`), `AbstractWorkerClient` (instance selection only; becomes the home of `getWorkerInstance(String componentName)` and `getWorkerInstances()`), `build.gradle.kts`
- Test: `server/ee/libs/platform/platform-component/platform-component-remote-client/src/test/java/com/bytechef/ee/platform/component/remote/client/service/ComponentDefinitionRemoteServiceGrpcWireTest.java` (replaces the REST-era `RemoteComponentDefinitionServiceClientTest`)

**Interfaces:**
- Consumes: Task 2 `GrpcChannels.getChannel(ServiceInstance)`, Task 3 null platform type, Task 4 mapper and stubs.
- Produces: the four clients keep their `ComponentDefinitionService`/`ActionDefinitionService`/`TriggerDefinitionService`/`ConnectionDefinitionService` signatures.

Client behavior: per-component calls go to `WorkerDiscoveryUtils.filterServiceInstance(instances, componentName)`; list calls fan out to `filterServiceInstances(instances)` and merge, keeping one definition per name and version; the component client then applies the filter bean that `supports(platformType)` (no filter when `platformType` is null; `IllegalArgumentException("Unsupported platform type: <type>")` when none supports it). `UnsupportedOperationException` stubs stay stubs.

- [ ] **Step 1: Write the failing wire tests** (in-process server with mocked services, a `GrpcChannels` whose `getChannel(ServiceInstance)` returns the in-process channel, two `DefaultServiceInstance`s with `grpc-port` and `components` metadata): `testGetComponentDefinitionsAppliesCallerPlatformFilter`, `testDefinitionsFromTwoWorkersAreMergedOncePerNameAndVersion`, `testComponentOnNoWorkerFailsNamingTheComponent`, `testGetComponentDefinitionWithoutVersionAsksForLatest` (worker mock receives `null`), and one call per remaining RPC of the four services asserting the mock received the same arguments and the client returned the mapped result.
- [ ] **Step 2: Run** `./gradlew :server:ee:libs:platform:platform-component:platform-component-remote-client:test`. Expected: FAIL.
- [ ] **Step 3: Implement the servers and rewrite the four clients.**
- [ ] **Step 4: Run** `./gradlew :server:ee:libs:platform:platform-component:platform-component-remote-client:check :server:ee:libs:platform:platform-component:platform-component-remote-grpc:check`. Expected: PASS.
- [ ] **Step 5: Commit** "Serve component, action, trigger and connection definitions over gRPC".

---

### Task 6: Component execution facades over gRPC

**Files:**
- Create in `platform-component-remote-grpc`: `ActionDefinitionFacadeRemoteServiceGrpcServer`, `TriggerDefinitionFacadeRemoteServiceGrpcServer` (registered by `ComponentGrpcServerConfiguration`, gated on `com.bytechef.platform.component.facade.ActionDefinitionFacadeImpl`)
- Modify in `platform-component-remote-client`: `RemoteActionDefinitionFacadeClient`, `RemoteTriggerDefinitionFacadeClient` (every non-stub method calls the worker that advertises `componentName`)
- Test: `.../ComponentFacadeRemoteServiceGrpcWireTest.java`

**Interfaces:**
- Consumes: Task 1 `ProtoValues` for `inputParameters`, `extensions`, `continueParameters`, `resumeData`, `triggerState` and `executePerform`'s `Object` result; Task 4 mapper for typed results.

- [ ] **Step 1: Write the failing wire test:** one test per RPC (15) asserting the facade mock receives equal arguments (maps compared with `isEqualTo`, so `Integer` stays `Integer`; `Instant suspendExpiresAt` and `PlatformType` preserved; null `connectionId` stays null) and the client returns the mapped result; `testExecutePerformReturnsNestedResult` returns `Map.of("rows", List.of(Map.of("id", 1)))` and gets it back equal.
- [ ] **Step 2: Run** the client module's tests. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** both modules' `check`. Expected: PASS.
- [ ] **Step 5: Commit** "Run component actions and triggers on workers over gRPC".

---

### Task 7: Task dispatcher definitions over gRPC

**Files:**
- Create: `server/ee/libs/platform/platform-workflow/platform-workflow-task-dispatcher/platform-workflow-task-dispatcher-registry/platform-workflow-task-dispatcher-registry-remote-grpc-proto/` (`task_dispatcher_definition_remote_service.proto` importing `property.proto` from Task 4 through a `protobuf(project(...))` dependency; package `bytechef.platform.workflow.task.dispatcher.remote.v1`)
- Create: `.../platform-workflow-task-dispatcher-registry-remote-grpc/` (`TaskDispatcherProtoMapper`, `TaskDispatcherDefinitionRemoteServiceGrpcServer`, configuration gated on the registry's `TaskDispatcherDefinitionService` implementation class)
- Modify: `RemoteTaskDispatcherDefinitionServiceClient` (calls `getChannel("coordinator-app")`)
- Modify: `settings.gradle.kts`; task dispatcher domain classes only where the round-trip test fails
- Test: `TaskDispatcherProtoMapperTest` (round trip of the definitions of `loop`, `branch`, `condition`, `each`, `parallel`, `map`, `subflow` task dispatchers, built as the registry builds them), `TaskDispatcherDefinitionRemoteServiceGrpcWireTest` (`GetTaskDispatcherDefinition` with and without version, `GetTaskDispatcherDefinitions`, `GetTaskDispatcherDefinitionVersions`, `ExecuteOutput`)

Services: `TaskDispatcherDefinitionRemoteService` with `GetTaskDispatcherDefinition`, `GetTaskDispatcherDefinitions`, `GetTaskDispatcherDefinitionVersions`, `ExecuteOutput`.

- [ ] **Step 1: Write the failing tests** above.
- [ ] **Step 2: Run** the new modules' and the client module's tests. Expected: FAIL.
- [ ] **Step 3: Implement** (domain fixes first, as in Task 4).
- [ ] **Step 4: Run** the three modules' `check`. Expected: PASS.
- [ ] **Step 5: Commit** domain fixes, then "Serve task dispatcher definitions over gRPC".

---

### Task 8: Wire the apps and delete the two REST modules

**Files:**
- Modify: `server/ee/apps/worker-app/build.gradle.kts` (add `platform-component-remote-grpc`; remove `platform-component-remote-rest`)
- Modify: `server/ee/apps/coordinator-app/build.gradle.kts` (add `...-registry-remote-grpc`; remove `...-registry-remote-rest`)
- Modify: every app with `platform-component-remote-client` or `...-registry-remote-client` (add `remote-grpc` if missing)
- Delete: `server/ee/libs/platform/platform-component/platform-component-remote-rest/`, `.../platform-workflow-task-dispatcher-registry-remote-rest/`, their `settings.gradle.kts` includes
- Test: `WorkerApplicationIntTest`, `CoordinatorApplicationIntTest`, `ConfigurationApplicationIntTest` (add to the existing classes)

- [ ] **Step 1: Write the failing tests:** worker-app serves `bytechef.platform.component.remote.v1.ComponentDefinitionRemoteService`, `ActionDefinitionRemoteService`, `TriggerDefinitionRemoteService`, `ConnectionDefinitionRemoteService`, `ActionDefinitionFacadeRemoteService`, `TriggerDefinitionFacadeRemoteService`; coordinator-app serves `bytechef.platform.workflow.task.dispatcher.remote.v1.TaskDispatcherDefinitionRemoteService`; configuration-app serves none of them (same `BindableService` listing as plan 3's `testApiKeyAndConnectedUserGrpcServicesAreServed`).
- [ ] **Step 2: Run** the three apps' `testIntegration`. Expected: FAIL.
- [ ] **Step 3: Rewire, delete the REST modules,** and confirm `git grep -n "platform-component-remote-rest\|task-dispatcher-registry-remote-rest\|/remote/component-definition-service\|/remote/task-dispatcher-definition-service"` prints nothing.
- [ ] **Step 4: Run** `check` and `testIntegration` of every touched app and `:server:apps:server-app:testIntegration` (the monolith must not change). Expected: PASS.
- [ ] **Step 5: Commit** the rewiring, then the deletion ("Remove the REST remote modules for component and task dispatcher definitions").

---

### Task 9: Live run from plan 4 Task 5, item 2

**Files:** whatever the run shows is missing, fixed test-first in the owning module.

- [ ] **Step 1: Rebuild and start** with plan 4's commands: all `bootJar`s, `docker compose -f docker-compose.microservices.yml -p bytechef-ms up -d --build --wait`. Expected: all healthy.
- [ ] **Step 2: Measure** the largest definitions response: worker-app log or a client-side debug log of the `GetComponentDefinitions` response size. Expected: below the configured 64 MB with room; record the number in the ledger.
- [ ] **Step 3: Run plan 4 Task 5's checklist from item 2** in the headless Playwright scripts (`scratchpad/live/*.mjs`). The editor loads components and task dispatchers, a component task can be added. Record every failure with the failing call.
- [ ] **Step 4: Fix only failures in what this plan owns** (definitions and component execution), test-first. Failures in calls owned by 3b-2 to 3b-5 are recorded in the ledger with the owning plan, not fixed here; if one blocks the remaining checklist, stop and report.
- [ ] **Step 5: Commit** each fix on its own.
