# PR #5378 Review — "513 - Billing Stripe initial implementation"

PR: https://github.com/bytechefhq/bytechef/pull/5378
Branch: `513` (base `master`), 89 files, +7,270/−98
Reviewed: 2026-07-17

## Critical Issues

### 1. No admin authorization on billing endpoints — 🔴 Critical
**Files:** `server/libs/platform/platform-billing/platform-billing-rest/src/main/java/com/bytechef/platform/billing/web/rest/BillingApiController.java`, `server/libs/platform/platform-billing/platform-billing-service/src/main/java/com/bytechef/platform/billing/facade/BillingSubscriptionFacadeImpl.java`

`/api/platform/internal/billing/**` falls under the generic `/api/**` → `authenticated()` rule in `SecurityConfiguration.java`, and neither the controller nor the facade carries `@PreAuthorize`. Any logged-in non-admin user can cancel, upgrade, or create checkout sessions for the tenant's subscription. The client hides the page behind `AUTHORITIES.ADMIN` (`routes.tsx`), but that is cosmetic only.

**Fix:** add the admin check on the facade (per the repo convention of authorization living in facades).

### 2. Usage under-reporting on period rollover — 🔴 High
**Files:** `BillingSubscriptionFacadeImpl.handleSubscriptionUpdated`, `BillingUsageServiceImpl.doReportUsage`

On a billing-period change the code resets `lastReportedAt = null` and moves `currentPeriodStart` forward. Tasks completed between the last hourly report and the old period's end are never sent to Stripe — a permanent revenue leak every month for every tenant.

**Fix:** report the tail window (old `lastReportedAt` → old `currentPeriodEnd`) before resetting the cursor. (PR description says usage logic will be revisited — flagged so it doesn't get lost.)

### 3. Unhandled Stripe event types and invalid signatures return HTTP 500 — 🔴 High
**Files:** `BillingSubscriptionFacadeImpl.extractTenantId`, `StripeClientImpl.verifyWebhookSignature`

Any webhook event type outside the 3 handled ones throws `IllegalArgumentException` → 500. Stripe retries 5xx for days and will eventually disable the endpoint; one extra event type enabled in the Stripe dashboard (e.g. `invoice.payment_succeeded`) makes the endpoint permanently failing. Invalid signature also maps to 500 instead of 400.

**Fix:** acknowledge unrecognized events with 200 (log + optionally record); map `SignatureVerificationException` to 400.

### 4. 200-with-empty-body breaks the generated client in the default state — 🔴 High
**Files:** `BillingApiController.getCurrentSubscription`, generated `client/src/shared/middleware/platform/billing/apis/BillingApi.ts`, `client/src/shared/queries/platform/billing.queries.ts`

With no subscription (every fresh install), `ResponseEntity.ok(null)` produces an empty body; the typescript-fetch client calls `response.json()` which throws `SyntaxError: Unexpected end of JSON input`. React-query retries 3× and lands in error state — the UI shows "Trial" only because `data` stays `undefined` by accident, with console noise and a retry storm on every poll tick.

**Fix:** return 204/404 and handle it in `billing.queries.ts`, or make the response schema nullable.

### 5. Wrong license header for an EE file — 🟠 Medium
**File:** `server/ee/libs/platform/platform-scheduler/platform-scheduler-aws/src/main/java/com/bytechef/ee/platform/scheduler/aws/AwsStripeUsageReportScheduler.java`

Sits under `server/ee/` but carries the Apache 2.0 header and lacks the `@version ee` Javadoc tag (its two sibling new files got it right). Per CLAUDE.md, all `server/ee/` files must use the ByteChef Enterprise license header.

## Suggestions

### 1. `stripeProductId` fields actually store subscription item ids
**Files:** `BillingSubscription.java`, `20260429000000_platform_billing_init.xml`, `StripeClient.java`

`stripeProductId`/`stripeUsageProductId` store subscription **item** ids (`si_…`) — `handleCheckoutSessionCompleted` sets `flatItem.getId()`, and `updateSubscription` passes the value as `existingFlatItemId`. Rename to `stripeItemId`/`stripeUsageItemId` (incl. DB columns) before this schema ships; renaming later costs a migration. *(Correctness/naming)*

### 2. Unused parameters in `scheduleDowngrade`
**File:** `StripeClientImpl.scheduleDowngrade`

`existingFlatItemId` and `existingMeteredItemId` are never used (phases are rebuilt from the schedule). Drop them. *(Dead code)*

### 3. Double-billing window in usage reporting
**File:** `BillingUsageServiceImpl.doReportUsage`

If `reportMeterEvent` succeeds but the subsequent `save()` fails, the next run re-reports the same window under a *different* idempotency key (the key includes fire time). Derive the idempotency key from `subscriptionId + lowerBound` instead, or persist first and report from a durable cursor. *(Correctness)*

### 4. Check-then-act race in webhook dedup
**File:** `BillingSubscriptionFacadeImpl.handleWebhookEvent`

`isEventProcessed` → process → `save` allows concurrent duplicate deliveries to both process; the second dies on the unique constraint with a 500. Insert the event row first and treat duplicate-key as "already processed". *(Concurrency)*

### 5. Full Stripe event logged at INFO
**File:** `BillingSubscriptionFacadeImpl`

`log.info("Processing webhook event: {}", event)` dumps the entire Stripe event (customer email, addresses). Log id + type only. *(Privacy)*

### 6. Magic status ordinal and odd repository placement
**File:** `BillingSubscriptionRepository.countCompletedTaskExecutions`

Hardcodes `status = 2` (ordinal of COMPLETED) and lives on the *subscription* repository. Reference the enum constant at minimum; consider a dedicated usage repository. Also decide explicitly whether editor test-runs should count toward billed usage. *(Maintainability)*

### 7. Global static `Stripe.apiKey`
**File:** `StripeClientImpl` constructor

Process-wide mutable state; stripe-java's instance-based `com.stripe.StripeClient` avoids it and makes tests that construct real `StripeClientImpl` less order-sensitive. *(Design)*

### 8. AWS scheduler robustness
**File:** `AwsStripeUsageReportScheduler`

- `catch (ConflictException)` logged at **error** for an expected idempotent case — use debug/info.
- Verify `rate(1 hours)` (plural with value 1) against EventBridge Scheduler validation.
- The `StripeUsageReporting` schedule *group* must be provisioned — `createSchedule` fails if the group doesn't exist. *(Robustness)*

### 9. Multi-node Quartz duplicate reporting
**File:** `BillingSchedulingConfiguration`

Hourly Quartz job runs on every coordinator node; with a non-clustered (RAM) job store, N nodes report N times. The shared `scheduledFireTime`-based idempotency key mostly masks it, but the `lastReportedAt` save races. Document or gate on a leader/clustered store. *(Concurrency)*

### 10. Four copy-pasted polling effects
**File:** `client/src/ee/pages/settings/platform/billing/Billing.tsx` (lines ~316–406)

Four `useEffect`s differing only in predicate — extract a single `usePendingBillingPoll(param, isSettled)` hook. *(Simplification)*

### 11. Division by zero in usage bar
**File:** `client/src/ee/pages/settings/platform/billing/components/PlanCard.tsx`

`taskLimit = 0` yields `NaN%` widths (`tasksUsed / taskLimit`) and `Infinity` bar zones. Guard the denominator. *(Edge case)*

### 12. Plan metadata duplicated client-side; hardcoded colors
**Files:** `SelectPlanDialog.tsx`, `PlanTierCard.tsx`

Plan tiers/prices/features are duplicated client-side and `PLAN_TIERS` re-implements the server's `planTier()`. Fine for v1; consider a `/plans` endpoint later. Also hardcoded hex colors (`#b3c9ed`, `#e6eef9`) and the `text-[0px]` whitespace hack — use theme tokens. *(Maintainability)*

### 13. Live Stripe ids and localhost URLs in default config
**File:** `server/apps/server-app/src/main/resources/config/application-bytechef.yml`

Default config ships real Stripe product ids, a test customer-portal URL, and `localhost:5173` success/cancel URLs. Empty placeholders + docs would be safer defaults for a file every deployment inherits. *(Config hygiene)*
