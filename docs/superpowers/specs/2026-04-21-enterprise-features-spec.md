<!--
Sources — competitor enterprise feature documentation used to scope this ticket:
  - https://www.sim.ai/blog/enterprise
  - https://docs.sim.ai/enterprise
  - https://docs.gumloop.com/enterprise-features/sso_saml_scim
  - https://docs.gumloop.com/enterprise-features/user_groups
  - https://docs.gumloop.com/enterprise-features/organization_data_export
  - https://docs.gumloop.com/enterprise-features/audit_logging
  - https://docs.gumloop.com/enterprise-features/ai_model_control
  - https://docs.gumloop.com/enterprise-features/organization_analytics
  - https://docs.gumloop.com/enterprise-features/app-policies/overview
  - https://docs.gumloop.com/enterprise-features/app-policies/app-rules
  - https://docs.gumloop.com/enterprise-features/app-policies/domain-restrictions
  - https://docs.gumloop.com/enterprise-features/app-policies/app-claims
-->

# ByteChef Enterprise Features — Scope & Epic Breakdown

**Date:** 2026-04-21
**Status:** Draft (for review)
**Owner:** Ivica Cardic
**Ticket:** TBD
**Edition:** `bytechef.edition=ee` only unless otherwise noted.

## 1. Summary

This ticket defines the ByteChef Enterprise Edition (EE) feature set by synthesizing enterprise
capabilities shipped by two reference competitors (Sim and Gumloop). Features are grouped into
**six epics**; each epic lists required modules, extension points in the existing codebase, and
deltas vs. what is already shipped in `server/ee/`. Every epic is sized to spawn its own detailed
design spec under `docs/superpowers/specs/` before implementation.

The goal is **parity on enterprise governance surface area**, not parity on implementation details.
Where ByteChef already has infrastructure (EE microservices, connection visibility, audit aspects),
the ticket extends it rather than rebuilding. Where ByteChef has nothing (app policies, org
analytics dashboards, SCIM), the ticket calls out the net-new modules.

## 2. Current EE Baseline (what already exists)

<!-- These are load-bearing facts verified from the repo at time of writing, not claims from sources. -->

- **EE microservices** — `server/ee/apps/`: `api-gateway-app`, `ai-gateway-app`, `ai-copilot-app`,
  `config-server-app`, `configuration-app`, `connection-app`, `coordinator-app`, `execution-app`,
  `scheduler-app`, `webhook-app`, `worker-app`, `runtime-job-app`.
- **Connection visibility** — `PRIVATE | WORKSPACE | PROJECT | ORGANIZATION` enforced in
  `ConnectionFacadeImpl.create()`; promote/demote/share GraphQL mutations are already live
  (admin-gated via `@PreAuthorize`).
- **Audit infrastructure** — `server/ee/libs/platform/platform-audit/platform-audit-service`
  exposes `PermissionAuditAspect`; `platform-connection-service` has `ConnectionAuditAspect`.
  Audit event emission exists but the log viewer / export / retention surface does not.
- **AI routing** — `ai-gateway-app` already routes model traffic; org-level model policy / BYOK /
  proxy routing are not yet bolted onto it.
- **Metrics** — Micrometer `MeterRegistry` wired via `ObjectProvider` in
  `ConnectionFacadeImpl` (`bytechef_connection_create` counter with `visibility` tag). Pattern is
  established for future enterprise metrics.
- **EE gating pattern** — `@ConditionalOnEEVersion` with remote-client stubs in
  `*-remote-client` modules. Any new SPI added for EE features must follow this pattern.

## 3. Epic 1 — Identity, Access & Directory Sync

<!--
Sources:
  - https://docs.gumloop.com/enterprise-features/sso_saml_scim
  - https://docs.gumloop.com/enterprise-features/user_groups
  - https://docs.sim.ai/enterprise (SSO & SAML section)
-->

### 3.1 Single Sign-On (SAML 2.0 + OIDC)

- Protocols: SAML 2.0 and OIDC.
- Required IdP coverage: Okta, Microsoft Entra ID, Google Workspace, JumpCloud, Ping Identity,
  generic SAML/OIDC.
- SP-initiated login only (matches Gumloop's stance).
- Dedicated org login URL (e.g. `bytechef.io/org/{slug}`) so the UPN doesn't need to be typed.
- Configurable session timeout; TLS 1.3 enforced at edge (api-gateway).
- JIT provisioning on first successful SAML assertion (account created from assertion claims).
- Env-var toggle for self-hosted: `BYTECHEF_SSO_ENABLED`.

### 3.2 SCIM 2.0 User Provisioning

- IdP coverage phase 1: Okta, Entra ID (matches Gumloop's phase 1).
- Lifecycle events: create, update, deprovision (soft delete → hard delete on retention expiry).
- Bearer-token auth per org; tokens generated in admin UI; rotate/revoke supported.
- Scheduled sync (15 min) + manual trigger button.
- Group-to-permission-group mapping (see §3.4 for the mapping semantics).
- **Constraint to adopt:** one permission group per user, with admin-defined priority ordering
  when multiple IdP groups match (Gumloop pattern). Simpler than multi-group and avoids
  intersection-semantics ambiguity.
- Audit events emitted for every SCIM operation (hooks into Epic 2).

### 3.3 Domain-Restricted Organization Signup

<!-- Source: https://docs.gumloop.com/enterprise-features/domain-restrictions — this is the org-level variant -->

- Admin configures `@corp.com` allowlist; new signups from other domains are rejected.
- Orthogonal to §5.3 (Domain Restrictions on OAuth flows) — this controls *ByteChef account*
  creation, not downstream app connections.

### 3.4 User Groups (Permission Groups)

<!-- Source: https://docs.gumloop.com/enterprise-features/user_groups -->

- Each org has a default group created at org creation; baseline permissions for all members.
- One group per user at any time. Reassignment moves them (no multi-group membership).
- Four permission axes (map directly from Gumloop's model — all four are needed):
  1. **Feature Restrictions** — toggle platform features: project/workflow creation, connection
     management, MCP tool creation, public sharing, data-table access, knowledge-base access.
  2. **User-Based Rate Limits** — concurrent workflow execution quotas per group. Enforced in
     `atlas-coordinator`.
  3. **App Scopes** — OAuth scope allowlist per connector category (restrict what scopes a
     group can grant when creating connections).
  4. **Node/Component Denylist** — block specific components or entire categories (e.g. block
     `code/scriptExecutor` for a "Business Analyst" group). Enforced in the workflow editor
     (client) AND at execution time in `atlas-worker` (defense in depth).

### 3.5 Admin API for IAM

<!-- Source: https://docs.sim.ai/enterprise (Admin API) -->

- CRUD endpoints for orgs, workspaces, members, permission groups.
- Drives infrastructure-as-code setups where `ORGANIZATIONS_ENABLED=true` but
  self-service invitations are off.

### Modules affected (Epic 1)

- **New:** `platform-identity-sso`, `platform-identity-scim` (under `server/ee/libs/platform/`).
- **Extend:** existing `com.bytechef.ee.platform.user` / `platform-rbac` for permission groups;
  `api-gateway-app` for SAML/OIDC redirect handling.

## 4. Epic 2 — Audit Logging & Organization Data Export

<!--
Sources:
  - https://docs.gumloop.com/enterprise-features/audit_logging
  - https://docs.gumloop.com/enterprise-features/organization_data_export
-->

### 4.1 Audit Log Event Coverage

Seven event categories (direct adoption from Gumloop's model — no reason to deviate):

| Category       | Example events                                                      |
|----------------|----------------------------------------------------------------------|
| Authentication | `USER_SIGN_IN`, `USER_SIGN_OUT`, session refresh                    |
| Credentials    | Connection create/update/delete/retrieve; credential access         |
| Teams          | Workspace/project create, member add/remove, rename                 |
| Organization   | Member invite/accept/revoke, domain config, branding update         |
| Permissions    | Group create/delete, membership change, permission-axis change     |
| Workflows      | Workflow execute, terminate, complete, result retrieval             |
| Files          | File upload, download, delete; data-table import/export             |

### 4.2 Schema

```
audit_event
├── id                UUID
├── occurred_at       TIMESTAMPTZ
├── event_type        TEXT        (e.g. "connection.credential_retrieved")
├── actor_user_id     UUID
├── actor_ip          INET
├── session_id        TEXT
├── org_id            UUID
├── workspace_id      UUID NULL
├── target_entity_type TEXT       (e.g. "connection", "workflow")
├── target_entity_id  TEXT
├── action_data       JSONB       (event-specific payload)
└── correlation_id    UUID NULL   (groups related events — already a ByteChef pattern)
```

Build on the existing `platform-audit` module — don't create a parallel schema.

### 4.3 Retention

- Default: 1 year.
- Configurable per-org up to contractual limit.
- Hot storage (PostgreSQL) for the last N days; cold storage (S3 export) beyond that.
- SOC2/GDPR: retention policy is itself an auditable config.

### 4.4 Access & Viewing

- **Admin only** — no Manager/Security composable sub-roles in phase 1 (keeps RBAC simple).
- Viewer UI: event-type dropdown filter + full-text search on `action_data` + date range.
- Record detail pane shows JSON payload + related correlation-id chain.

### 4.5 Programmatic Access

- REST: `GET /api/v1/audit-logs` with `event_type`, `from`, `to`, `actor_user_id`,
  `target_entity_id`, pagination cursor.
- GraphQL equivalent under the automation schema for dashboard use.
- SIEM pull: same REST endpoint. Push (webhook-per-event) deferred to v2.

### 4.6 Organization Data Export

Three export types (matches Gumloop):
1. **Workflow executions** — execution history, timestamps, durations, workflow metadata.
2. **Agents/Workflows config** — definitions, versions, component configs, system prompts.
3. **Billing/Usage logs** — per-action cost, run counts, user attribution. Scoped to the whole
   org only (team-level breakdown is in analytics; this is the raw ledger).

- CSV format, ISO-8601 timestamps.
- Admin-only; runs async via `atlas-coordinator`.
- Endpoints: `POST /api/v1/export` returns `exportId`; `GET /api/v1/export/{exportId}` polls
  status and returns presigned S3 URL on completion.

### Modules affected (Epic 2)

- **Extend:** `platform-audit-service`, `platform-audit-rest` (new), `platform-audit-facade`.
- **New:** `platform-org-export` under `server/ee/libs/platform/`.

## 5. Epic 3 — App Policies (Governance)

<!--
Sources:
  - https://docs.gumloop.com/enterprise-features/app-policies/overview
  - https://docs.gumloop.com/enterprise-features/app-policies/app-rules
  - https://docs.gumloop.com/enterprise-features/app-policies/domain-restrictions
  - https://docs.gumloop.com/enterprise-features/app-policies/app-claims
-->

App Policies is the most architecturally distinctive epic — it introduces a **policy evaluation
pipeline** applied at every tool/connector invocation. Fail-closed semantics: if policy evaluation
errors, the call is denied.

### 5.1 App Rules

- **Actions:** `BLOCK` (deny) or `TAG` (allow but mark for audit).
- **Phases:** `PRE_EXECUTION` (on args) and `POST_EXECUTION` (on result). Post-rules are the
  only way to catch data-leakage cases (e.g. a search that returned classified fields).
- **Targeting:** org → permission group → user → agent instance → specific tool. Most-specific
  wins; ties resolved by rule priority integer.
- **Condition DSL:** CEL (Common Expression Language — Google's).
  - Pre-rule vars: `args`, `tool_name`, `server_id` (component name), `user`, `workspace`.
  - Post-rule vars: above + `output`.
  - Natural-language → CEL compilation via the existing `ai-copilot-app` as admin UX sugar.
- **Precedence (decision — Gumloop leaves this undefined):**
  1. Evaluate all applicable rules in priority order.
  2. First `BLOCK` short-circuits and denies.
  3. All matching `TAG` rules fire (additive, all tags recorded).
  4. If zero rules match → allow.
- **Enforcement point:** `platform-component` invocation path, specifically the
  `ComponentHandler#performAction` wrapper. Rules run in the worker (`atlas-worker`) so policy
  is enforced even for agent-driven tool calls and MCP-driven calls.

### 5.2 App Claims

- Admin OAuths into a target provider workspace (Slack, Salesforce, Notion, etc.); ByteChef
  records the workspace's **stable provider ID**.
- Enforcement: OAuth callback for any other org into that same workspace ID is rejected.
- **Important:** claims are OAuth-boundary gatekeeping, not shared credentials. Distinct from
  `ORGANIZATION`-visibility connections, which share one credential across the org.
- Admin actions: disable (stop enforcing), rename (display-only), delete (release claim).
- Requires each component to expose a **`workspaceIdExtractor`** — new SPI on
  `ConnectionDefinition`. Components that can't extract a stable workspace ID (most do not)
  are excluded from claim support; UI disables the feature per-component.

### 5.3 Domain Restrictions (OAuth-level)

- Per-app allowlist of domains. During OAuth, extract email from provider, match against list,
  reject on miss without persisting credentials.
- Applies only to components that reliably surface an email (Google Workspace, Slack,
  Microsoft 365 — same limitation Gumloop documents).
- Multiple domains per app (for multi-brand orgs).
- **Only affects new connections** — existing credentials continue to work. Separate
  "revoke existing" bulk action is needed to actually flush out non-compliant connections
  after a policy change.

### Modules affected (Epic 3)

- **New:** `platform-app-policy`, `platform-app-policy-rest`, `platform-app-policy-cel` under
  `server/ee/libs/platform/`.
- **Extend:** `platform-component-api` — add `workspaceIdExtractor` SPI to
  `ConnectionDefinition`; add `emailExtractor` SPI for Domain Restrictions.
- **Hook point:** `ComponentHandler#performAction` wrapper in `atlas-worker`; OAuth callback
  handler in `connection-app`.

## 6. Epic 4 — AI Model Control

<!-- Source: https://docs.gumloop.com/enterprise-features/ai_model_control -->

### 6.1 Model Access Control

- **Modes:** `ALLOW_LIST` (strict, only listed models usable) or `DENY_LIST` (listed are
  blocked). Per-org setting.
- **Granularity:**
  - Provider-level bulk (all OpenAI, all Anthropic, …).
  - Individual model (e.g. allow `claude-opus-4-7` but not `claude-haiku-4-5`).
- **Fallback model:** admin-configured global fallback; when a workflow requests a blocked
  model, router rewrites to fallback rather than failing the execution. Emits a `TAG`-style
  audit event so admins see routing decisions.
- Enforcement: `ai-gateway-app` (already exists) — extend the model router.

### 6.2 Organization API Keys (BYOK)

- Org-level keys override user/workspace keys for all routing.
- Supports: OpenAI, Anthropic, Google, Azure OpenAI, DeepSeek, Grok, Bedrock, local
  (Ollama/vLLM for self-hosted).
- Consolidates billing (org pays provider directly, no ByteChef markup) and eliminates shadow
  personal keys.
- Stored encrypted in `core-encryption`-backed table; never surfaced to users after save.

### 6.3 Proxy Routing

- Admin-configured proxy URL per provider; all provider calls go through it.
- Use cases: EU-only routing (data residency), corporate egress proxies (SSL inspection),
  custom model-name mapping (e.g. route `gpt-4o` → internal deployment).
- Implemented as a per-org HTTP client config in the `ai-gateway-app`.

### 6.4 Audit of Model Usage

All admin changes to model policy are audit events (Epic 2). Per-call routing decisions are
available as metrics (prompt cost, tokens, routed-model) under the existing ai-gateway
observability platform (see `2026-04-11-ai-gateway-observability-platform-design.md`).

### Modules affected (Epic 4)

- **Extend:** `ai-gateway-app` — model router, proxy config, fallback routing.
- **New:** `platform-ai-policy` (under `server/ee/libs/platform/`) for allow/deny list CRUD.

## 7. Epic 5 — Organization Analytics

<!-- Source: https://docs.gumloop.com/enterprise-features/organization_analytics -->

### 7.1 Tracked Metrics

- Execution count, duration, credit/cost consumption per workflow / per user / per project /
  per connection.
- Active-user counts (DAU/WAU/MAU).
- Agent chat volume and cost per chat.
- Cost by AI provider & model (feeds into Epic 4 decisions).
- Error rate per workflow (Gumloop explicitly does NOT track this; ByteChef should — we already
  have execution failure data in `atlas-execution`).

### 7.2 Granularity

- Per-user, per-workflow, per-project, per-agent, per-connection.
- Default 90-day window; extendable.

### 7.3 Surfaces

- **Dashboard** — React page under `client/` workspace section; shows top-N tables, time-series
  charts, usage trends.
- **CSV export** — same underlying data as §4.6 but filtered/aggregated.
- **Conversational query** — expose the analytics dataset to `ai-copilot-app` so admins can ask
  "why did our OpenAI spend double last week?" and get a data-backed answer. Directionally
  novel vs. both competitors; leverages ByteChef's existing copilot.

### 7.4 Access

- Admin sees org-wide data.
- Non-admin sees only their own activity.

### Modules affected (Epic 5)

- **New:** `platform-analytics` (query service, rollup tables).
- **Extend:** `client/` — new analytics dashboard.
- **Extend:** `ai-copilot-app` — analytics tool surface.

## 8. Epic 6 — Deployment, Branding & Data Residency

<!--
Sources:
  - https://www.sim.ai/blog/enterprise (Self-Hosted, Whitelabeling, Compliance sections)
  - https://docs.sim.ai/enterprise (Self-Hosted Configuration)
-->

### 8.1 Self-Hosted / On-Prem Deployment

- Existing Docker Compose and Helm charts already cover this.
- **Gap to close:** air-gapped documentation + an "offline bundle" build target that produces
  images + Helm chart + component JAR snapshots with no runtime registry fetches.

### 8.2 Whitelabeling

- Env-var driven (`BYTECHEF_BRAND_*`): logo URL, primary color, support URL, product name,
  favicon. Applied at client build time and via runtime config.
- Hide "Powered by ByteChef" footer when `BYTECHEF_BRAND_HIDE_ATTRIBUTION=true` (EE-only flag).

### 8.3 Env-Var Feature Toggles (self-hosted)

Direct adoption of Sim's toggle list — every EE feature in this ticket needs an env-var kill
switch so self-hosted operators can opt-in incrementally:

- `BYTECHEF_SSO_ENABLED`
- `BYTECHEF_SCIM_ENABLED`
- `BYTECHEF_AUDIT_LOGS_ENABLED`
- `BYTECHEF_APP_POLICIES_ENABLED`
- `BYTECHEF_AI_MODEL_CONTROL_ENABLED`
- `BYTECHEF_ANALYTICS_ENABLED`
- `BYTECHEF_ORGANIZATIONS_ENABLED` (umbrella for workspaces/teams)
- `BYTECHEF_WHITELABELING_ENABLED`

Each feature's Spring config must guard its beans with the matching `@ConditionalOnProperty`.

### 8.4 Data Residency / Compliance Posture

- **Self-hosted** — residency is by definition whatever the customer runs.
- **Cloud EE** — regional deployments (US, EU at minimum). Per-org pinned region; no
  cross-region data movement for execution traces, credentials, or audit logs.
- SOC 2 Type II — control-mapping document and continuous-evidence collection (Vanta/Drata
  integration is a separate follow-up).
- GDPR — data export (Epic 2) + user deletion (right-to-be-forgotten) API; already partially
  covered by deprovision in §3.2.

### Modules affected (Epic 6)

- **Extend:** `client/` for branding; Helm charts for air-gap; Spring config classes for
  feature toggles.
- **New:** no new modules, but new `@ConditionalOnProperty` guards across all EE beans.

## 9. Cross-Cutting Concerns

- **Metrics:** every EE feature gets at least one `bytechef_<feature>_*` counter using the
  existing `ObjectProvider<MeterRegistry>` pattern. No feature ships without telemetry.
- **Remote client pattern:** every new SPI added to a platform module needs a corresponding
  `@Component @ConditionalOnEEVersion` stub in the matching `*-remote-client` module. Failing
  to do this breaks EE microservice startup (well-established gotcha, see
  `automation-configuration-remote-client`).
- **EE header + Javadoc:** every file under `server/ee/` gets the ByteChef Enterprise license
  header (not Apache 2.0) and the `@version ee` Javadoc tag.
- **GraphQL conventions:** new admin mutations use SCREAMING_SNAKE_CASE enum values and
  `@PreAuthorize` unless explicitly allowing the resource owner (orphan-recovery pattern from
  `demoteConnectionToPrivate`).

## 10. Sequencing (proposed)

Order is chosen to maximize early customer value and unblock downstream epics:

1. **Epic 2 (Audit + Export)** — foundation for everything else; other epics emit events here.
2. **Epic 1 (IAM)** — unblocks enterprise pilots. SSO is table stakes.
3. **Epic 4 (AI Model Control)** — quick win; extends existing `ai-gateway-app`.
4. **Epic 5 (Analytics)** — depends on §4.6 data pipeline.
5. **Epic 3 (App Policies)** — largest scope; needs new CEL runtime and SPI extensions.
6. **Epic 6 (Whitelabel + Residency)** — mostly existing infra + config; small scope.

## 11. Out of Scope (explicit)

- Billing/metering infrastructure (credit system itself). Assumed to exist or be a separate
  track.
- Per-region backup/restore of PostgreSQL — infra concern, not a product feature.
- Multi-tenant cell isolation (running untrusted customer code in hardened sandboxes) —
  separate security track.
- Mobile clients / mobile SSO.

## 12. Open Questions

1. **Permission groups — single vs multi:** Gumloop enforces one group per user (simpler).
   ByteChef's existing RBAC may already support multi. Decision needed before Epic 1 kicks off.
2. **App Rules DSL — CEL vs custom:** CEL is proven but adds a runtime dependency. Should we
   build on Jexl (already pulled in via `core-evaluator`) instead?
3. **Audit retention cold storage:** S3-compatible object store only, or also PostgreSQL
   partitioned-by-month?
4. **BYOK scope:** does "org-level overrides user-level" apply only to AI provider keys, or to
   all connector credentials (e.g. an org-level GitHub PAT overriding user-level)?
5. **App Claims rollout:** claim release process when a customer offboards — who owns the
   workspace ID record after contract termination? Needs legal + product input.

## 13. References

<!-- Competitor documentation cited throughout this spec — repeated here for discoverability. -->

- Sim blog: https://www.sim.ai/blog/enterprise
- Sim enterprise docs: https://docs.sim.ai/enterprise
- Gumloop SSO/SAML/SCIM: https://docs.gumloop.com/enterprise-features/sso_saml_scim
- Gumloop User Groups: https://docs.gumloop.com/enterprise-features/user_groups
- Gumloop Data Export: https://docs.gumloop.com/enterprise-features/organization_data_export
- Gumloop Audit Logging: https://docs.gumloop.com/enterprise-features/audit_logging
- Gumloop AI Model Control: https://docs.gumloop.com/enterprise-features/ai_model_control
- Gumloop Org Analytics: https://docs.gumloop.com/enterprise-features/organization_analytics
- Gumloop App Policies (overview): https://docs.gumloop.com/enterprise-features/app-policies/overview
- Gumloop App Rules: https://docs.gumloop.com/enterprise-features/app-policies/app-rules
- Gumloop Domain Restrictions: https://docs.gumloop.com/enterprise-features/app-policies/domain-restrictions
- Gumloop App Claims: https://docs.gumloop.com/enterprise-features/app-policies/app-claims

## 14. Related ByteChef Specs

- `2026-04-06-connection-visibility-phase1-design.md` — visibility tiers (Epic 3 builds on this).
- `2026-04-07-audit-connection-annotation-design.md` — audit aspect (Epic 2 extends this).
- `2026-04-11-ai-gateway-observability-platform-design.md` — model observability (Epic 4 hooks in).
- `2026-04-15-audit-events-settings-page-design.md` — audit settings UI (Epic 2 UI).
