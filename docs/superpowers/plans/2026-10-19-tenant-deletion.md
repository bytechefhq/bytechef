# Tenant Deletion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A tenant admin in multi-tenant mode requests deletion of their organization; the tenant goes inert at once and is purged after a grace period.

**Architecture:** A public-schema `tenant_deletion` table records the request and doubles as the tombstone. The request facade marks the row, then runs ordered `TenantTeardownContributor` beans (one per owning module) inside the tenant while its schema still exists. Guards keyed on the tenant id reject every later entry into the tenant. A coordinator `@Scheduled` purger drops the schema and runs the contributors' after-drop cleanup once `purge_after` passes.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, plain JDBC for public-schema tenant tables, Liquibase, Quartz and AWS EventBridge Scheduler, Stripe Java SDK, React + Vitest.

**Spec:** `docs/superpowers/specs/2026-10-18-tenant-deletion-design.md`

## Global Constraints

- Work on branch `6239` (cut from `upstream/master`), not `0_732`.
- Server commit messages: `6239 <description>`; client: `6239 client - <description>`. No `Co-Authored-By` trailer.
- No explanatory inline code comments; type-level Javadoc only where the surrounding file has it. Rationale goes in commit messages.
- Files under `server/ee/` carry the Enterprise license header and the `@version ee` Javadoc tag.
- Monolith only (`server-app`); every new SPI method gets a `RemoteXxxClient` stub that throws `UnsupportedOperationException` and a no-op in `NoOpTriggerScheduler`.
- Grace period: `bytechef.tenant.deletion.grace-period`, default 30 days.
- Purge sweep: Spring `@Scheduled`, every 15 minutes, `@ConditionalOnCoordinator`.
- Status values are persisted as INT ordinals: `PENDING_DELETION=0, PURGED=1, CANCELLED=2`; append only.
- `public.tenant_deletion` is always referenced schema-qualified; every tenant datasource appends `public` to `search_path` (`BaseDataSource.setSearchPath`), so an unqualified name would also resolve from inside a tenant.
- After `DROP SCHEMA`, no code may run a tenant-scoped query for that tenant id: with the schema gone, the query lands on the public schema.
- Verification is `./gradlew :<module>:check` (static analysis + integration tests), with `DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock` for Testcontainers. Write Gradle task paths as `":${m}:check"` in zsh. Never judge a Gradle run through a pipe; redirect to a file and check `$?`.

## Review Focus

1. **A second deletion request while one is pending** must not reset `purge_after` or rerun teardown; it returns 204 and leaves the row as it is. Test in Task 4.
2. **One deployment's trigger disable throwing** (provider unreachable) must not stop the others: the rest are still disabled, the failure is recorded on the row, and the request still succeeds. Test in Task 4.
3. **Another user of the tenant still holding a session** after the request: their next API call gets 401 and the session is invalidated. Test in Task 6.
4. **A purge that crashes after the schema drop** must resume on the next sweep, run the after-drop steps, and mark the row purged without touching any tenant-scoped table. Test in Task 9.
5. **An inbound webhook for a pending tenant** gets 410 and nothing executes, so nothing is written to the public schema. Test in Task 6.

---

### Task 1: Correct the spec against the code

**Files:**
- Modify: `docs/superpowers/specs/2026-10-18-tenant-deletion-design.md`

- [ ] **Step 1: Apply the corrections**

Make these factual fixes:

- **Scheduler sweep:** one-time tasks are keyed by bare job id: Quartz `JobKey(String.valueOf(jobId), "OneTimeTask")`, EventBridge `OneTimeTask_resume_<jobId>`.
- **Connection refresh keys:** Quartz `tenantId + connectionId`; EventBridge `ConnectionRefresh_<tenantId>_<connectionId>`.
- **Method name:** `BaseDataSource.getSearchPath` → `setSearchPath`.
- **Billing table:** `billing_subscription` is a tenant-schema table.
- **Scheduler jobs:** `OneTimeSchedulerJob` and `DynamicWebhookTriggerRefreshJob` run with no tenant context.
- **Purge resume model:** replace `last_completed_step` with `purge_claimed_date`, and resume by rerunning idempotent steps rather than recording a step index. Every purge step is idempotent, and the claim column doubles as the distributed lock the spec left open.
- **Deployment listing:** there is no finder for enabled deployments. `ProjectDeploymentService.disableAllProjectDeployments()` only flips the flag and must not be used for teardown.

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/specs/2026-10-18-tenant-deletion-design.md
git commit -m "6239 docs - Correct the tenant deletion design against the code"
```

### Task 2: `tenant_deletion` table, tenant-api types, repository

**Files:**
- Create: `server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/domain/TenantDeletion.java`
- Create: `server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/service/TenantDeletionService.java`
- Create: `server/ee/libs/core/tenant/tenant-multi-service/src/main/resources/config/liquibase/changelog/core/tenant/20261019000001_core_tenant_deletion_init.xml`
- Modify: `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml` (one `includeAll` next to the Quartz line 63)
- Create: `server/ee/libs/core/tenant/tenant-multi-service/src/main/java/com/bytechef/ee/tenant/repository/TenantDeletionRepository.java`
- Create: `server/ee/libs/core/tenant/tenant-multi-service/src/main/java/com/bytechef/ee/tenant/service/TenantDeletionServiceImpl.java`
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (`Tenant` class, line 4567)
- Test: `server/ee/libs/core/tenant/tenant-multi-service/src/test/java/com/bytechef/ee/tenant/repository/TenantDeletionRepositoryIntTest.java`

**Interfaces:**
- Produces, all of which every later task uses:
  - `record TenantDeletion(String tenantId, Status status, String requestedBy, Instant requestedDate, Instant purgeAfter, @Nullable String stripeCustomerId, @Nullable String stripeSubscriptionId, @Nullable Instant purgeClaimedDate, List<String> teardownFailures)` with nested `enum Status { PENDING_DELETION, PURGED, CANCELLED }`.
  - `TenantDeletionService` (tenant-api):
    - `boolean markPendingDeletion(String tenantId, String requestedBy, Instant purgeAfter)` returns false when a `PENDING_DELETION` row already exists.
    - `Optional<TenantDeletion> fetchTenantDeletion(String tenantId)`
    - `boolean isInactive(String tenantId)`: true for `PENDING_DELETION` or `PURGED`.
    - `void recordStripeIds(String tenantId, String customerId, String subscriptionId)`
    - `void recordTeardownFailures(String tenantId, List<String> failures)`
    - `List<TenantDeletion> getDueForPurge(Instant now)`
    - `boolean claimForPurge(String tenantId, Instant now)`
    - `void markPurged(String tenantId)`
  - `ApplicationProperties.Tenant.getDeletion().getGracePeriod()` returns a `Duration` (default `Duration.ofDays(30)`).

- [ ] **Step 1: Write the failing integration test**

`@SpringBootTest` with `PostgreSQLContainerConfiguration` and `LiquibaseConfiguration`, autowiring `TenantDeletionRepository`. Tests:

- `testMarkPendingDeletionIsIdempotent`: the first `markPendingDeletion("000001", "admin@x", t)` returns true; a second with `t.plusSeconds(60)` returns false; `fetchTenantDeletion("000001").get().purgeAfter()` equals `t`.
- `testClaimForPurgeIsExclusive`: the first `claimForPurge("000001", now)` returns true, the second false, and `claimForPurge("000001", now.plus(Duration.ofHours(2)))` returns true (a stale claim older than 1 hour is reclaimable).
- `testGetDueForPurgeReturnsOnlyElapsedPendingRows`: rows with `purgeAfter` in the past and the future and a `PURGED` row → only the elapsed pending one.
- `testTeardownFailuresRoundTrip`: `recordTeardownFailures("000001", List.of("a", "b"))` reads back as `["a", "b"]`.

- [ ] **Step 2: Run, expect FAIL**

Run: `DOCKER_HOST=… ./gradlew :server:ee:libs:core:tenant:tenant-multi-service:testIntegration --tests '*TenantDeletionRepositoryIntTest'`
Expected: compilation failure, `TenantDeletionRepository` not found.

- [ ] **Step 3: Changelog**

Table `tenant_deletion` columns:

| column | type | constraint |
|---|---|---|
| `tenant_id` | VARCHAR(32) | PK |
| `status` | INT | not null |
| `requested_by` | VARCHAR(256) | not null |
| `requested_date` | TIMESTAMP | not null |
| `purge_after` | TIMESTAMP | not null |
| `stripe_customer_id` | VARCHAR(256) | |
| `stripe_subscription_id` | VARCHAR(256) | |
| `purge_claimed_date` | TIMESTAMP | |
| `teardown_failures` | TEXT | JSON array, default `'[]'` |

The master.xml entry is `<includeAll path="classpath:config/liquibase/changelog/core/tenant/" errorIfMissingOrEmpty="false" contextFilter="mono"/>`. Tenant schemas load with context `multitenant` only, so `mono` keeps the table out of them.

- [ ] **Step 4: Implement `TenantDeletionRepository` (`@Repository`, constructor `(DataSource)`)**

Plain JDBC in `TenantRepository`'s style, every statement against `public.tenant_deletion`. `markPendingDeletion` is `INSERT … ON CONFLICT (tenant_id) DO UPDATE … WHERE tenant_deletion.status = 2` (re-request after a cancel) and returns `updateCount == 1`. `claimForPurge` is `UPDATE … SET purge_claimed_date = ? WHERE tenant_id = ? AND status = 0 AND (purge_claimed_date IS NULL OR purge_claimed_date < ? - interval '1 hour')`. Serialize `teardown_failures` with `JsonUtils`.

- [ ] **Step 5: Implement `TenantDeletionServiceImpl`**

`@Service @ConditionalOnEEVersion @ConditionalOnMultiTenant`, delegating to the repository. `isInactive` reads a cached `Set<String>` of inactive tenant ids. The set is reloaded when older than 30 seconds and right after each local `markPendingDeletion` and `markPurged`.

- [ ] **Step 6: Add the `Tenant.Deletion` property class** with a `gracePeriod` field defaulting to `Duration.ofDays(30)`.

- [ ] **Step 7: Run, expect PASS**, then `:server:ee:libs:core:tenant:tenant-multi-service:check` and `:server:libs:core:tenant:tenant-api:check`.

- [ ] **Step 8: Commit** `6239 Add the tenant_deletion table and TenantDeletionService`

### Task 3: Keep pending tenants out of tenant lookups and id allocation

**Files:**
- Modify: `server/ee/libs/core/tenant/tenant-multi-service/src/main/java/com/bytechef/ee/tenant/repository/TenantRepository.java` (`findTenantIds` :82, `findMaxTenantId` :176, the `*Exist` paths)
- Modify: `server/ee/libs/core/tenant/tenant-multi-service/src/main/java/com/bytechef/ee/tenant/service/MultiTenantService.java` (`tenantIdsByUserEmailExist`, `tenantIdsByUserLoginExist`)
- Test: `server/ee/libs/core/tenant/tenant-multi-service/src/test/java/com/bytechef/ee/tenant/repository/TenantRepositoryDeletionIntTest.java`

**Interfaces:**
- Consumes: the Task 2 table.
- Produces:
  - `TenantRepository.findTenantIds()` excludes tenants whose row status is 0 or 1.
  - `TenantRepository.findTenantIdsIncludingPendingDeletion()`: live plus pending tenants.
  - `findMaxTenantId()` is the max over schemas and `tenant_deletion.tenant_id`.
  - `MultiTenantService.tenantIdsByUser{Email,Login}Exist` search live **and** pending tenants.

- [ ] **Step 1: Write the failing integration test**

Create schemas `bytechef_000001` and `bytechef_000002`, each with a minimal `user` table, plus a pending row for `000002`. Tests:

- `testFindTenantIdsExcludesPendingTenant`: `["000001"]`.
- `testFindTenantIdsByUserEmailSkipsPendingTenant`: an email that exists only in 000002 → empty.
- `testEmailExistsIncludesPendingTenant`: `MultiTenantService.tenantIdsByUserEmailExist(thatEmail)` → true.
- `testFindMaxTenantIdCountsTombstones`: drop `bytechef_000002`, mark its row `PURGED` → `findMaxTenantId()` is `"000002"`, so `createTenant()` allocates `"000003"`.

- [ ] **Step 2: Run, expect FAIL** on all four assertions.

- [ ] **Step 3: Implement**

Add a `NOT EXISTS (SELECT 1 FROM public.tenant_deletion d WHERE 'bytechef_' || d.tenant_id = schema_name AND d.status IN (0, 1))` clause to `findTenantIds`. `findTenantIdsIncludingPendingDeletion` is the same query restricted to `status = 0`, plus the live ones. Make `findTenantIdsByQueryWithParam` take the id list as a parameter so the `*Exist` paths pass the including-pending list.

- [ ] **Step 4: Run, expect PASS**, then module `check`. Also run `:server:libs:platform:platform-user:platform-user-rest:testIntegration --tests '*MultiTenantAccountControllerIntTest'`, because its `@BeforeEach` deletes tenants and relies on these lookups.

- [ ] **Step 5: Commit** `6239 Exclude tenants pending deletion from tenant lookups and never reuse their ids`

### Task 4: Teardown SPI and the request facade

**Files:**
- Create: `server/libs/core/tenant/tenant-api/src/main/java/com/bytechef/tenant/teardown/TenantTeardownContributor.java`
- Create: `server/libs/platform/platform-user/platform-user-api/src/main/java/com/bytechef/platform/user/facade/OrganizationDeletionFacade.java`
- Create: `server/libs/platform/platform-user/platform-user-service/src/main/java/com/bytechef/platform/user/facade/OrganizationDeletionFacadeImpl.java`
- Create: `server/libs/platform/platform-user/platform-user-api/src/main/java/com/bytechef/platform/user/exception/{OrganizationDeletionConfirmationException,OrganizationDeletionNotAvailableException}.java`, plus two new `UserErrorType` constants (next free numbers after `INVALID_ROLE`)
- Create: `server/libs/platform/platform-user/platform-user-service/src/main/java/com/bytechef/platform/user/teardown/PersistentTokenTenantTeardownContributor.java`
- Test: `server/libs/platform/platform-user/platform-user-service/src/test/java/com/bytechef/platform/user/facade/OrganizationDeletionFacadeTest.java`

**Interfaces:**
- Consumes: `TenantDeletionService` (Task 2), `ApplicationProperties.Tenant.Deletion` (Task 2).
- Produces:

```java
public interface TenantTeardownContributor {
    default List<String> onDeletionRequested(String tenantId) { return List.of(); }
    default void beforeSchemaDrop(String tenantId) {}
    default void afterSchemaDrop(String tenantId) {}
}
```

  - Beans carry `@Order`. Request order: 100 deployments, 200 embedded, 300 suspended jobs, 400 scheduler sweep, 500 billing, 600 persistent tokens.
  - `onDeletionRequested` and `beforeSchemaDrop` run inside `TenantContext.runWithTenantId(tenantId)` and `SecurityUtils.runAsSystem`.
  - `afterSchemaDrop` must not query the tenant's tables.
  - `onDeletionRequested` returns human-readable failure lines (`"projectDeployment 42: <message>"`).
  - `OrganizationDeletionFacade.requestDeletion(String confirmation, String password)` returns `Instant purgeAfter`, and is guarded by `@PreAuthorize("hasAuthority(\"ROLE_ADMIN\")")`.

- [ ] **Step 1: Write the failing unit tests** (Mockito; `TenantContext.setCurrentTenantId("000001")` in setUp, reset in tearDown)

- `testRejectsMismatchedConfirmation`: throws `OrganizationDeletionConfirmationException` and never calls `markPendingDeletion`.
- `testRejectsWrongPasswordForLocalAccount`: throws `InvalidPasswordException`.
- `testExternalProviderAdminConfirmsWithEmailOnly`: `authProvider = "github"`, password null → proceeds.
- `testRejectsWhenNotMultiTenant` and `testRejectsDefaultTenant`: throw `OrganizationDeletionNotAvailableException`.
- `testMarksPendingThenRunsContributorsInOrder`: InOrder over `markPendingDeletion("000001", login, now+30d)`, contributor A, then contributor B. Returns that `purgeAfter`.
- `testContributorFailureIsRecordedAndDoesNotStopTheRest` (Review Focus 2): A throws `RuntimeException("boom")` and B returns `["x"]` → B still runs; `recordTeardownFailures("000001", [<A's class simple name>: boom, x])`.
- `testSecondRequestIsNoOp` (Review Focus 1): `markPendingDeletion` returns false → no contributor runs; the existing row's `purgeAfter` is returned.

- [ ] **Step 2: Run, expect FAIL** (types missing).

- [ ] **Step 3: Implement `OrganizationDeletionFacadeImpl`**

`@Component @ConditionalOnCoordinator`, not `@Transactional`. Constructor: `(ApplicationProperties, PasswordEncoder, ObjectProvider<TenantDeletionService>, List<TenantTeardownContributor>, TenantService, UserService, Clock)`.

- Confirmation: the trimmed, case-insensitive email, or the login when the email is null.
- Password: required only when `authProvider` is null or `UserConstants.AUTH_PROVIDER_LOCAL`. The validation shape is the one already written in `/private/tmp/claude-502/-Volumes-Data-bytechef/ee552675-c828-4f07-b7aa-d97edbbfb405/scratchpad/6239.patch` (`AccountDeletionFacadeImpl`).
- Each contributor call is wrapped in try/catch `RuntimeException`.

- [ ] **Step 4: Implement `PersistentTokenTenantTeardownContributor`** (`@Component @Order(600)`): `persistentTokenRepository.deleteAll()`. Test: `testDeletesAllTokensOfTheTenant`.

- [ ] **Step 5: Run, expect PASS**, then `check` on tenant-api, platform-user-api and platform-user-service.

- [ ] **Step 6: Commit** `6239 Add the tenant teardown SPI and the organization deletion request facade`

### Task 5: Disable triggers, embedded instances, suspended jobs, schedules

**Files:**
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/teardown/ProjectDeploymentTenantTeardownContributor.java` (`@Order(100)`)
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/teardown/IntegrationInstanceTenantTeardownContributor.java` (`@Order(200)`, `@ConditionalOnEEVersion`)
- Create: `server/libs/platform/platform-workflow/platform-workflow-execution/platform-workflow-execution-service/src/main/java/com/bytechef/platform/workflow/execution/teardown/{SuspendedJobTenantTeardownContributor,SchedulerTenantTeardownContributor}.java` (`@Order(300)`, `@Order(400)`)
- Modify: `TriggerScheduler.java` and `ConnectionRefreshScheduler.java` (platform-scheduler-api): add `void cancelAllForTenant(String tenantId)`
- Modify: `QuartzTriggerScheduler`, `QuartzConnectionRefreshScheduler`, `AwsTriggerScheduler`, `AwsConnectionRefreshScheduler`, `NoOpTriggerScheduler`, `RemoteTriggerSchedulerClient`, `RemoteConnectionRefreshSchedulerClient`
- Modify: `atlas-execution-api/.../service/JobService.java` + impl, `atlas-execution-repository-api/.../repository/JobRepository.java` + `JdbcJobRepository`: `List<Long> getJobIds(Job.Status status)` / `findAllIdsByStatus(int status)`
- Tests: one `*TenantTeardownContributorTest` per contributor; `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/test/java/com/bytechef/platform/scheduler/QuartzTriggerSchedulerCancelAllForTenantTest.java`; `AwsTriggerSchedulerCancelAllForTenantTest.java` in the AWS module

**Interfaces:**
- Consumes: `TenantTeardownContributor` (Task 4).
- Produces: `TriggerScheduler.cancelAllForTenant(String)`, `ConnectionRefreshScheduler.cancelAllForTenant(String)`, `JobService.getJobIds(Job.Status)`.

- [ ] **Step 1: Write the failing tests**

- **Deployments:**
  - `testDisablesEveryEnabledDeployment`: deployments 1 (enabled) and 2 (disabled) → `enableProjectDeployment(1, false)` only.
  - `testFailureOnOneDeploymentDoesNotStopTheNext`: 1 throws → 3 is still disabled; returns `["projectDeployment 1: <message>"]`.
- **Embedded:** `testDisablesEveryEnabledConfiguration` → `enableIntegrationInstanceConfiguration(id, false)` for each enabled configuration from `IntegrationInstanceConfigurationService.getIntegrationInstanceConfigurations()`. That facade method disables the triggers of every enabled instance whose connected user is enabled; connected-user project workflows are project deployments, covered by the deployments contributor.
- **Suspended jobs:** `testCancelsOneTimeTaskOfEveryStoppedJob` → `cancelOneTimeTask(7)` and `cancelOneTimeTask(9)` for `getJobIds(STOPPED) = [7, 9]`; a throwing cancel is recorded, not rethrown.
- **Scheduler:** `testCancelsTriggerAndConnectionRefreshJobsForTenant` → both `cancelAllForTenant("000001")`.
- **Quartz** (`StdSchedulerFactory` with `org.quartz.jobStore.class=org.quartz.simpl.RAMJobStore`), `testCancelAllForTenantLeavesOtherTenants`:
  - Setup: schedule a schedule trigger and a polling trigger for `WorkflowExecutionId`s of tenants 000001 and 000002, and a connection-refresh job for each tenant.
  - Expect: after `cancelAllForTenant("000001")`, only the 000002 jobs remain.
- **AWS** (mocked `SchedulerClient`), `testCancelAllForTenantDeletesMatchingSchedules`:
  - Setup: `listSchedules` pages per group.
  - Expect: names whose 16-character tail decodes to a `000001:` prefix are deleted, and `ConnectionRefresh_000001_*` is deleted; 000002 is untouched.

- [ ] **Step 2: Run, expect FAIL.**

- [ ] **Step 3: Implement**

- **Deployments:** `projectDeploymentService.getProjectDeployments()` filtered on `isEnabled()`.
- **Quartz:** for groups `SCHEDULE_TRIGGER`, `POLLING_TRIGGER`, `DYNAMIC_WEBHOOK_TRIGGER_REFRESH`, decode each key with `WorkflowExecutionId.parse` (skip keys that fail to parse) and delete on a tenant match. For `CONNECTION_OAUTH_2_TOKEN_REFRESH`, match `getJobDetail(key).getJobDataMap().getString("tenantId")`.
- **AWS:** `listSchedules(groupName, namePrefix=group)` per group. Base64-decode the 16-character name tail and compare the text before the first `:`. Match `ConnectionRefresh_<tenantId>_` by prefix.
- **`JdbcJobRepository.findAllIdsByStatus`:** `SELECT id FROM job WHERE status = ?`.

- [ ] **Step 4: Run, expect PASS**, then `check` on every touched module: automation-configuration-service, embedded-configuration-service, platform-workflow-execution-service, platform-scheduler-api, platform-scheduler-impl, platform-scheduler-aws, platform-scheduler-remote-client, atlas-execution-api, atlas-execution-repository-jdbc, and the runtime-job-app module that holds `NoOpTriggerScheduler`.

- [ ] **Step 5: Commit** `6239 Tear down a tenant's triggers, embedded instances, suspended jobs and schedules on deletion`

### Task 6: Pending tenants are inert

**Files:**
- Create: `server/libs/platform/platform-security-web/platform-security-web-api/src/main/java/com/bytechef/platform/security/web/filter/InactiveTenantFilter.java` and `.../web/config/InactiveTenantSecurityConfigurerContributor.java` (model: `TrialFilterSecurityConfigurerContributor`)
- Modify: `WebhookTriggerController` (`executeWorkflow`, `sseStreamWorkflow`), `ApprovalController:79`, `TriggerFormApiController:66`, `ApiPlatformHandlerController:139`, `WorkflowContinuationHelper:79`
- Modify: `TriggerCoordinator` (`onTriggerListenerEvent`, `onTriggerPollEvent`, `onTriggerWebhookEvent`), `ConnectionOAuth2TokenRefreshJob`, AWS `ConnectionRefreshListener`
- Modify: `BillingWebhookFacadeImpl.processWebhookEvent`
- Tests: `InactiveTenantFilterTest`, `WebhookTriggerControllerInactiveTenantTest`, `TriggerCoordinatorInactiveTenantTest`, `BillingWebhookFacadeInactiveTenantTest`

**Interfaces:**
- Consumes: `TenantDeletionService.isInactive(String)` (Task 2), always through `ObjectProvider<TenantDeletionService>`. An absent bean (single-tenant, distributed apps) means active.

- [ ] **Step 1: Write the failing tests**

- **Filter:**
  - `testInvalidatesSessionOfInactiveTenant` (Review Focus 3): session attribute `CURRENT_TENANT_ID = "000001"`, `isInactive` true → 401, `session.isInvalid()`, chain not called.
  - `testRejectsCredentialRequestWhoseTenantContextIsInactive`: no session, `TenantContext` 000001 inactive → 401.
  - `testPassesActiveTenant`.
- **Webhook** (Review Focus 5): `testWebhookForInactiveTenantReturns410AndDoesNotExecute`. A `WorkflowExecutionId` for 000001, inactive → status 410, and `webhookWorkflowExecutor` is never invoked.
- **TriggerCoordinator:** `testListenerEventForInactiveTenantIsDropped`, so `triggerCompletionHandler` is never invoked.
- **Billing:** `testEventForInactiveTenantIsAcknowledgedAndDropped`, so the handler is never called and nothing is thrown.

- [ ] **Step 2: Run, expect FAIL.**

- [ ] **Step 3: Implement**

The filter is added with `http.addFilterAfter(filter, AuthorizationFilter.class)`, so the API-key and MCP JWT filters have already wrapped the chain in their tenant. It checks the session attribute first, then `TenantContext.getCurrentTenantId()` when that is not `public`. Controllers return `ResponseEntity.status(HttpStatus.GONE)` before entering the tenant. Event handlers log at debug and return.

- [ ] **Step 4: Run, expect PASS**, then `check` on platform-security-web-api, platform-webhook-rest-impl, platform-workflow-coordinator-impl, platform-scheduler-impl, platform-scheduler-aws, platform-billing-service and the approval / trigger-form / api-platform modules touched.

- [ ] **Step 5: Commit** `6239 Reject every entry into a tenant pending deletion`

### Task 7: Stripe immediate cancellation

**Files:**
- Modify: `server/libs/platform/platform-billing/platform-billing-service/src/main/java/com/bytechef/platform/billing/client/StripeClient.java` + `StripeClientImpl.java`
- Create: `server/libs/platform/platform-billing/platform-billing-service/src/main/java/com/bytechef/platform/billing/teardown/BillingTenantTeardownContributor.java` (`@Order(500)`, `@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")`)
- Test: `BillingTenantTeardownContributorTest.java`

**Interfaces:**
- Consumes: `TenantDeletionService.recordStripeIds` (Task 2), `TenantTeardownContributor` (Task 4).
- Produces: `void StripeClient.cancelImmediately(String stripeSubscriptionId, String tenantId)`.

- [ ] **Step 1: Write the failing tests**

- `testSnapshotsIdsThenCancels`: `fetchCurrentSubscription()` returns customer `cus_1`, subscription `sub_1` → InOrder `recordStripeIds("000001", "cus_1", "sub_1")` then `cancelImmediately("sub_1", "000001")`.
- `testNoSubscriptionIsNoOp`.
- `testStripeFailureIsReturnedAsFailureLine`: a `PaymentClientException` → returns `["billing: <message>"]`.

- [ ] **Step 2: Run, expect FAIL.**

- [ ] **Step 3: Implement `cancelImmediately`**: `Subscription.retrieve`, `releaseScheduleIfPresent`, then `subscription.cancel()`, wrapped like `cancelAtPeriodEnd` (99-114). The Stripe customer is not deleted.

- [ ] **Step 4: Run, expect PASS**, then platform-billing-service `check`.

- [ ] **Step 5: Commit** `6239 Cancel a tenant's Stripe subscription immediately on deletion`

### Task 8: After-drop cleanup contributors

**Files:**
- Modify: `FileStorageService.java` (file-storage-api): `default void deleteTenantFiles(String tenantId) {}`. Implement in `FilesystemFileStorageService` (recursive delete of `baseDirPath.resolve(tenantId)`) and `AwsFileStorageServiceImpl` (list `tenantId + "/"` and delete in batches of 1000).
- Create: `FileStorageTenantTeardownContributor` beans next to `FilesystemFileStorageConfiguration` and `AwsFileStorageConfiguration`.
- Create: `server/ee/libs/config/tenant-multi-pgvector-config/src/main/java/com/bytechef/ee/tenant/multi/pgvector/VectorStoreTenantTeardownContributor.java`, with the same conditions as `MultiTenantPgVectorDataSourceConfiguration`. It uses `@Qualifier("pgVectorJdbcTemplate")` to run `DROP SCHEMA IF EXISTS bytechef_vectorstore_<tenantId> CASCADE` after validating the tenant id against `^[a-zA-Z0-9_]+$`.
- Create: chat-memory contributors in `ai-chat-memory-aws-config` (empty and delete bucket `<bucketPrefix>-<tenantId>`), `ai-chat-memory-redis-config` (drop index `getIndexName(tenantId)`, delete keys `getKeyPrefix(tenantId) + "*"` by SCAN), and `ai-chat-memory-in-memory-config` (remove the map entry). JDBC needs none: its table dies with the schema.
- Create: `server/libs/platform/platform-user/platform-user-service/src/main/java/com/bytechef/platform/user/teardown/RegistrationUserTenantTeardownContributor.java` (`beforeSchemaDrop`)
- Tests: one per contributor plus `FilesystemFileStorageServiceDeleteTenantFilesTest` (temp dir; 000001 removed, 000002 kept).

**Interfaces:**
- Consumes: `TenantTeardownContributor` (Task 4).
- Produces: `FileStorageService.deleteTenantFiles(String tenantId)`.

- [ ] **Step 1: Write the failing tests**

- `testRegistrationRowDeletedWhenLoginInNoOtherTenant`: tenant users `[a@x]`, `getTenantIdsByUserLogin("a@x")` empty → `userService.delete("a@x")` runs with `TenantContext` `public`.
- `testRegistrationRowKeptWhenLoginInAnotherTenant`: returns `["000005"]` → no delete.
- `testVectorStoreSchemaDropped`: verify the exact SQL.
- `testRejectsTenantIdWithSqlCharacters`: `"1; DROP"` → `IllegalArgumentException`.
- Redis: verify the dropped index name and the scanned prefix. S3: verify the bucket name.

- [ ] **Step 2: Run, expect FAIL.**

- [ ] **Step 3: Implement** per the file list.

- [ ] **Step 4: Run, expect PASS**, then `check` on file-storage-api, file-storage-filesystem-service, file-storage-aws-impl, tenant-multi-pgvector-config, the three chat-memory config modules and platform-user-service.

- [ ] **Step 5: Commit** `6239 Remove a deleted tenant's files, vector schema, chat memory and registration rows`

### Task 9: Purge sweep

**Files:**
- Create: `server/ee/libs/core/tenant/tenant-multi-service/src/main/java/com/bytechef/ee/tenant/purge/TenantDeletionPurger.java` (`@Component @ConditionalOnEEVersion @ConditionalOnMultiTenant @ConditionalOnCoordinator`)
- Test: `TenantDeletionPurgerTest.java`

**Interfaces:**
- Consumes: `TenantDeletionService.getDueForPurge`, `claimForPurge` and `markPurged`; `TenantRepository`; `TenantService.deleteTenant`; `List<TenantTeardownContributor>`.
- Produces: `@Scheduled(fixedDelayString = "PT15M") void purgeDueTenants()`.

- [ ] **Step 1: Write the failing tests**

- `testPurgeRunsBeforeDropThenDropThenAfterDrop`: InOrder `beforeSchemaDrop`, `deleteTenant("000001")`, `afterSchemaDrop`, `markPurged`.
- `testSkipsUnclaimedTenant`: `claimForPurge` false → nothing runs.
- `testResumesAfterSchemaAlreadyDropped` (Review Focus 4): the schema no longer exists → `beforeSchemaDrop` and `deleteTenant` are skipped; `afterSchemaDrop` and `markPurged` run.
- `testAfterDropFailureLeavesRowPendingForNextSweep`: an after-drop contributor throws → `markPurged` is not called, and the next tenant is still processed.

- [ ] **Step 2: Run, expect FAIL.**

- [ ] **Step 3: Implement**

The schema-exists check is a new `TenantRepository.schemaExists(String tenantId)` (`information_schema.schemata`). The before-drop phase runs inside `TenantContext.runWithTenantId` and `SecurityUtils.runAsSystem`. The after-drop phase runs in the default tenant, so a contributor slip cannot reach a tenant table.

- [ ] **Step 4: Run, expect PASS**, then tenant-multi-service `check`.

- [ ] **Step 5: Commit** `6239 Purge tenants whose deletion grace period has elapsed`

### Task 10: REST endpoint, admin last-admin guard, tenant info

**Files:**
- Create: `server/libs/platform/platform-user/platform-user-rest/src/main/java/com/bytechef/platform/user/web/rest/OrganizationDeletionController.java`: `@RestController @RequestMapping("/api/account/organization") @ConditionalOnCoordinator`.
  - `POST /delete` takes body `record OrganizationDeletionRequest(String confirmation, String password)` and returns 200 with `record OrganizationDeletionResponse(Instant purgeAfter)`.
  - It then clears the session and the remember-me cookie with `SecurityContextLogoutHandler` and `CookieClearingLogoutHandler(AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY)`. The `/api/account` path is the one `MultiTenantInternalFilter` puts in the session's tenant.
- Modify: `UserManagementFacadeImpl.deleteUser`: reject deleting yourself and deleting the last `ROLE_ADMIN` (`getUsersByAuthorityName`). Reuse the `LastAdminDeletionException` shape from the saved patch.
- Create: `server/libs/config/app-config/src/main/java/com/bytechef/config/actuate/info/TenantInfoContributor.java`: contributes `tenant: {mode: "multi"|"single", deletionGracePeriodDays: <long>}`.
- Tests:
  - Add `testRequestOrganizationDeletion`, `testRequestOrganizationDeletionRejectsNonAdmin` and `testRequestOrganizationDeletionRejectsWrongConfirmation` to `MultiTenantAccountControllerIntTest`. Assert the row is `PENDING_DELETION`, the response carries `purgeAfter`, and a non-admin gets 403.
  - `UserManagementFacadeImplTest`: `testDeleteUserRejectsSelf` and `testDeleteUserRejectsLastAdmin`.
  - `TenantInfoContributorTest`.

**Interfaces:**
- Consumes: `OrganizationDeletionFacade.requestDeletion` (Task 4).
- Produces: `POST /api/account/organization/delete`; the `/actuator/info` `tenant.mode` and `tenant.deletionGracePeriodDays` fields (Task 11).

- [ ] **Step 1: Write the failing tests** above.
- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement** per the file list. Facade-thrown `AbstractException`s already map to 400 through the global handler; a facade `AccessDeniedException` would map to 500, so the controller adds a local `@ExceptionHandler(AccessDeniedException.class)` returning 403.
- [ ] **Step 4: Run, expect PASS**, then `check` on platform-user-rest, platform-user-service and app-config.
- [ ] **Step 5: Commit** `6239 Expose organization deletion to tenant admins and guard deleting the last admin`

### Task 11: Client "Delete organization"

**Files:**
- Modify: `client/src/shared/stores/useApplicationInfoStore.tsx`: add `tenant: {deletionGracePeriodDays: number; mode: 'multi' | 'single'}` to the info type, defaulting to `{deletionGracePeriodDays: 30, mode: 'single'}`.
- Create: `client/src/pages/account/settings/AccountProfileDeleteOrganization.tsx`, adapted from `AccountProfileDeleteAccount.tsx` in the saved patch (`/private/tmp/claude-502/-Volumes-Data-bytechef/ee552675-c828-4f07-b7aa-d97edbbfb405/scratchpad/6239.patch`).
- Modify: `client/src/pages/account/settings/AccountProfile.tsx`: render the section last, only when `useIsTenantAdmin()` and `tenant.mode === 'multi'`.
- Test: `client/src/pages/account/settings/tests/AccountProfileDeleteOrganization.test.tsx`

**Interfaces:**
- Consumes: `POST /api/account/organization/delete` returning `{purgeAfter}`, and the info `tenant.*` fields (Task 10).

- [ ] **Step 1: Write the failing tests**

- `testHiddenForNonAdmin` and `testHiddenInSingleTenantMode`.
- `testDeleteButtonDisabledUntilEmailAndPasswordEntered`.
- `testNoPasswordForExternalProviderAdmin`.
- `testRequestsDeletionAndLogsOut`: the POST body is `{confirmation, password}`; the toast reads `Your organization will be deleted on <purgeAfter as a locale date>.`; `logout` and `clearAuthentication` are called.
- `testStaysLoggedInWhenServerRejects`.

- [ ] **Step 2: Run, expect FAIL** (`npx vitest run src/pages/account/settings/tests/AccountProfileDeleteOrganization.test.tsx`).

- [ ] **Step 3: Implement**, with this exact copy:
  - Heading: `Delete organization`
  - Section text: `Permanently delete this organization and everything in it: workflows, connections, executions, files and members. Billing stops immediately. After {n} days the data is deleted for good.`
  - Dialog title: `Delete Organization`
  - Confirm label: `Type <email> to confirm`
  - Button: `Delete organization`

- [ ] **Step 4: Run, expect PASS**, then `npx eslint --max-warnings=0` on the touched files and `npm run typecheck`.

- [ ] **Step 5: Commit** `6239 client - Add Delete organization for tenant admins`
