# Embedded automation code workflows — completion (distributed, CLI, admin UI)

**Status:** Designed, not implemented. Completes the shipped feature
(`2026-07-27-embedded-automation-code-workflows-design.md`); three independently shippable
sub-projects, in dependency order.

## 1. Distributed EE support

Today `webhook-app` boots with all-throw remote-client stubs and the bridge degrades (log-once +
404/skip). Make it work:

- Implement the three remote clients in `embedded-configuration-remote-client` as real
  `LoadBalancedRestClient` calls — but ONLY the methods the webhook controllers actually invoke
  (`getConnectedUserWorkflows`, `getOrCreateReference`, the copy-mode resolution entry, the catalog
  lookup used by the sync bridge); every other method stays a throwing stub, per the repo's
  established partial-implementation pattern (`RemotePrincipalJobFacadeClient`).
- Serve them from `configuration-app` via a new `embedded-configuration-remote-rest` controller set
  under `/remote/...` — mirror `automation-task-remote-rest` (the approval-task precedent: facade
  host app owns the real beans; worker/webhook apps call over REST).
- The capability guard stays: topologies without configuration-app reachable still degrade
  gracefully. Remove nothing.
- Verification: `webhook-app` context test keeps passing; a wiring test proves the remote client
  hits the controller path (WireMock or the module's established remote-rest test style).

## 2. CLI

Extend the existing `bytechef` CLI (`cli/`, Spring Shell; profile auth already handled by
`configure`):

- `bytechef embedded code-workflow deploy --file <artifact> [--language JAVA|JS|PYTHON|RUBY]` →
  `POST /api/embedded/internal/automation/projects/deploy` (admin session/token; multipart
  `projectFile`). Prints the WARN-trigger validation outcome if the response carries it.
- `bytechef embedded code-workflow list [--output table]` → the catalog listing, showing name,
  `kind`, workflows.
- Out of scope: provision/enable commands (connected-user operations belong to customer backends,
  not the operator CLI).
- Follow the CLI's existing command/option/output conventions; docs in `cli/README.md`.
- Update `claude-code-plugin/bytechef-dev/skills/bytechef-code-workflow/SKILL.md` deploy examples
  to CLI-first (curl retained as the no-CLI fallback) — matching the plugin convention: CLI where a
  command exists (`bytechef component init` precedent), curl only where none does. Also swap the
  automation deploy example to `bytechef` CLI if an automation deploy command exists; add one if
  trivial, else leave curl.

## 3. Admin console UI (admin-only)

In the existing embedded admin pages (client/), no connected-user surface:

- **Catalog page** gains: `kind` badge (COPY/REFERENCE) on each catalog project; a **Deploy code
  workflow** action (file upload → the deploy endpoint; surface the 204 success and any
  trigger-validation warnings); redeploy = same action on an existing code project.
- **Code catalog project detail**: workflows with uuids; a **References** panel listing connected
  users referencing each workflow — external user id, environment, enabled, dangling(+reason) —
  served by a new admin GraphQL query (`ROLE_ADMIN`, embedded GraphQL module) that reads through
  the reference facade. No Project ids anywhere in the schema.
- Dangling references get a visible warning state; no admin mutation of user references in v1
  (read-only panel — mutations remain the customer backend's job through the public API).
- Client conventions apply: sort-keys, `*I`/`Props` naming, `twMerge`, hook ordering, post-turn
  `npm run check`.

## Cross-cutting

- Docs: the feature's user page drops the "API-only/degrades" caveats where each sub-project lands;
  CLI README + `docs` CLI page updated; CLAUDE.md entry amended (distributed no longer
  degrade-only).
- Each sub-project independently plannable and shippable; UI depends on nothing new from 1 or 2.
