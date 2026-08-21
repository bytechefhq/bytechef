# Docs Remediation — Design (post-reorganization link repair, Enterprise fold, accuracy pass)

- **Date:** 2026-08-18
- **Branch:** `0_732`
- **Status:** Proposed
- **Scope:** `docs/` only. No server or client code changes.
- **Ticket:** 732

## 1. Summary

The docs reorganization nested every page under `/platform/…` but left the pages' internal links
pointing at the pre-reorg roots. The result is **528 broken links across 127 of 562 pages**, measured
by the link validator already wired into `npm run lint`.

Repairing those links surfaced three larger problems that a link fix alone would preserve:

1. **`platform/enterprise/**` is a parallel marketing tree.** 49 pages, ~36,000 words, written in
   positioning voice, shadowing the product guides on ten basenames — and responsible for 249 of the
   528 broken links.
2. **`platform/settings/**` is a tree of empty stubs.** Roughly twenty pages are frontmatter and
   nothing else — no body at all. The content those stubs need is sitting in the enterprise tree.
   The fold is therefore a **fill**, not a deletion.
3. **Page status is unstructured prose.** 67 pages say "Coming soon" in four incompatible forms, and
   the marketing site scrapes that prose as its authoritative feature-status gate.

This spec covers five phases: mechanical link repair, the Enterprise fold, stray-duplicate cleanup,
a full accuracy pass against the code, and a Fumadocs component pass.

## 2. Goals / non-goals

**Goals**

- Zero broken internal links, verified by `npm run lint`.
- One documentation tree in one voice: **guidance**, not positioning.
- Every EE-only capability discoverable from a single Enterprise index that links into the guides.
- Page status (`ee`, `comingSoon`) machine-readable in frontmatter.
- Every hand-written page checked against the code on branch `0_732`.

**Non-goals**

- Rewriting `content/docs/reference/**` (≈280 generated component pages) or
  `content/docs/openapi/**`. Both are generated; they are link-check targets, never edit targets.
- Redesigning the site chrome, theme, or navigation shell.
- Moving marketing prose to the website repo. Deleted positioning copy is deleted here; porting it
  to `bytechef-website` is a separate task.

## 3. Current state (measured, not assumed)

Run: `./node_modules/.bin/fumadocs-mdx && ./node_modules/.bin/bun ./scripts/lint.ts`
(`bun` is a devDependency; it is not on `PATH` but `node_modules/.bin/bun` exists.)

### 3.1 Broken links

528 instances, 127 files, **all `not-found`** — zero `invalid-fragment`. Headings were never moved;
only paths were. Classified by suffix-matching each dead target against the 562 live URLs:

| Class | Instances | Distinct targets |
|---|---:|---:|
| Resolves to exactly one existing page | 460 | — |
| Ambiguous (2+ candidates) | 14 | 6 |
| No candidate page exists anywhere | 54 | 25 |

By subtree: `platform/enterprise` 249, `platform/automation` 143, `platform/embedded` 93,
`platform/use-bytechef` 27, `platform/settings` 9, remainder 7.

The six ambiguous targets:

| Dead target | Candidates |
|---|---|
| `/embedded/connections` | `automation/build/connections`, `embedded/build/connections`, `settings/connections` |
| `/deploy/self-hosted/observability` | `enterprise/governance-security/observability`, `use-bytechef/self-hosted/management/observability` |
| `/automation/mcp-servers` | `automation/deploy/mcp-servers`, `embedded/configure/mcp-servers` |
| `/embedded/mcp-servers` | `automation/deploy/mcp-servers`, `embedded/configure/mcp-servers` |
| `/automation/build/flow-controls` | `automation/build/workflows/flow-controls`, `reference/flow-controls` |
| `/automation/knowledge-base` | `automation/data/knowledge-base`, `enterprise/data-knowledge/knowledge-base` |

Four of the six disambiguate themselves once phase 2 deletes the enterprise twin.

### 3.2 Empty stubs

Frontmatter-only, zero body:

`settings/users`, `settings/connections`, `settings/oauth2-clients`, `settings/identity-providers`,
`settings/audit-events`, `settings/license`, `settings/notifications`, `settings/workspaces`,
`settings/admin-api-keys`, `settings/components/custom-components`,
`settings/components/component-visibility`, `settings/components/api-connectors`,
`automation/settings/users`, `automation/settings/api-keys`, `automation/settings/git-configuration`,
`automation/settings/ai-hub-connectors`, `automation/settings/ai-agents/guardrails`,
`automation/settings/ai-agents/system-prompt`, `your-account/profile`, `your-account/appearance`,
`your-account/active-sessions`.

`settings/connections.mdx` additionally carries `title: Users` — a copy-paste error currently live.

### 3.3 Missing nav config

Four directories hold pages but no `meta.json`, so Fumadocs auto-lists everything in them
alphabetically: `automation/data` (3), `automation/deploy` (5), `embedded/build` (6),
`embedded/configure` (3). This is why `mcp-servers2.mdx` appears in the sidebar at all.

### 3.4 Status markers

67 hand-written pages mention "Coming soon", expressed as: inline `(coming soon)` ×51,
blockquote `> **Coming soon** ` ×38, `<Callout type="warn" title="Coming soon">` ×23, heading
suffix ×4.

### 3.5 Component usage

`Callout` 53 files, `Cards` 11, `EEBadge` 13. **`Tabs`, `Steps`, `Accordions`, `Files`,
`TypeTable`, `ImageZoom`, `Banner`: zero.** `Steps`, `TypeTable`, `ImageZoom` and `Banner` are not
registered in `mdx-components.tsx`. 43 hand-written pages are `.md` and cannot host JSX at all.

## 4. Phase 1 — Mechanical link repair (durable subset only)

**Only 188 of the 460 unambiguous links are safe to fix before the fold.** Splitting the 460 by
whether phase 2 touches either end:

| Subset | Instances | Why it must wait |
|---|---:|---|
| Resolve **into** `/platform/enterprise/*` | 240 | Target page is deleted by phase 2 |
| Live **inside** `/platform/enterprise/*` | 235 | Source file is deleted by phase 2 |
| Neither end is doomed — **durable** | 188 | — |

(The first two overlap; 272 of the 460 are entangled with phase 2 in one direction or the other.)

Phase 1 therefore rewrites **only the 188 durable instances** (87 distinct targets). Rewriting the
other 272 would mean editing files scheduled for deletion, and "repairing" links by aiming them at
pages about to vanish. Those resolve as a by-product of the fold instead, and phase 3 sweeps the
remainder to zero.

**Fragment preservation is mandatory.** 70 of the 528 broken targets carry a `#fragment`
(e.g. `…/custom-components#auto-generated-api-connectors-coming-soon`). The suffix-matching analysis
that produced the mapping table strips fragments to find the page; the rewrite must re-attach the
original fragment to the resolved path. Dropping them would silently destroy 70 deep links while the
validator reported success — it only reports `invalid-fragment` for fragments that are *present and
wrong*, never for fragments that were removed.

After re-attaching, fragments must be re-validated: a preserved-but-stale fragment converts a
`not-found` into an `invalid-fragment`, which is progress but not done.

- Driven by a script over the validator's output, not hand edits. The script is throwaway; it is not
  committed.
- Verification is the validator itself: instance count must fall from 528 to 340.
- Lands as one commit, independent of every later phase.

## 5. Phase 2 — The Enterprise fold

### 5.1 Shape

`platform/enterprise/**` collapses from 49 pages to **one**: `platform/enterprise/index.mdx`, a pure
index of EE capabilities. Each entry is a title, a one-line description, and a link to the guide page
that documents it. No prose is duplicated there.

The other 48 pages' content is redistributed into the product guides and **rewritten from positioning
voice into guidance voice** — "how you use it and what it does", never "why it is compelling".

### 5.2 Full 49-page mapping

**Fills an empty stub (10)**

| From `platform/enterprise/` | Words | To |
|---|---:|---|
| `governance-security/users.mdx` | 802 | `settings/users.mdx` |
| `governance-security/oauth2-clients.mdx` | 652 | `settings/oauth2-clients.mdx` |
| `governance-security/api-keys.mdx` | 1069 | `settings/admin-api-keys.mdx` + `automation/settings/api-keys.mdx` |
| `governance-security/sso.mdx` | 922 | `settings/identity-providers.mdx` |
| `governance-security/audit-log.mdx` | 921 | `settings/audit-events.mdx` |
| `governance-security/license-gated-distribution.mdx` | 730 | `settings/license.mdx` |
| `governance-security/connection-visibility.mdx` | 834 | `settings/connections.mdx` (also fixes the `title: Users` bug) |
| `governance-security/component-policies.mdx` | 615 | `settings/components/component-visibility.mdx` |
| `extensibility/custom-components.mdx` | 3226 | `settings/components/custom-components.mdx` |
| `extensibility/api-connectors.mdx` | 1565 | `settings/components/api-connectors.mdx` |

**Merges into an existing guide (15)**

| From | Words | To |
|---|---:|---|
| `governance-security/rbac.mdx` | 550 | `settings/users.mdx` (roles section) |
| `governance-security/ai-guardrails.mdx` | 654 | `automation/settings/ai-agents/guardrails.mdx` |
| `governance-security/observability.mdx` | 687 | `use-bytechef/self-hosted/management/observability.mdx` |
| `governance-security/encrypted-credentials.mdx` | 934 | `use-bytechef/self-hosted/configuration/` |
| `governance-security/data-retention.mdx` | 451 | `use-bytechef/self-hosted/configuration/` |
| `data-knowledge/data-tables.mdx` | 726 | `automation/data/data-tables.mdx` |
| `data-knowledge/knowledge-base.mdx` | 753 | `automation/data/knowledge-base/index.mdx` |
| `data-knowledge/embedding-models.mdx` | 557 | `settings/ai-providers.mdx` (alongside `## Default models`) |
| `collaboration-devops/workflow-executions.mdx` | 690 | `automation/monitor/workflow-executions.mdx` |
| `collaboration-devops/workspaces-projects.mdx` | 533 | `settings/workspaces.mdx` + `automation/build/workflows/projects.mdx` |
| `collaboration-devops/git-backed-change-tracking.mdx` | 1386 | `automation/settings/git-configuration.mdx` |
| `collaboration-devops/workflow-versioning.mdx` | 535 | `automation/deploy/deploy-workflows.mdx` |
| `collaboration-devops/build-once-deploy-many.mdx` | 569 | `automation/deploy/deploy-workflows.mdx` |
| `collaboration-devops/environments.mdx` | 642 | `automation/deploy/environments.mdx` (new) |
| `extensibility/mcp-integration.mdx` | 523 | `automation/deploy/mcp-servers.mdx` + `settings/mcp-server.md` |

**Moves to self-hosted / ops (11)**

| From | Words | To |
|---|---:|---|
| `scale-reliability/horizontal-scaling.mdx` | 669 | `use-bytechef/self-hosted/installation/distributed.mdx` |
| `scale-reliability/distributed-scheduler.mdx` | 548 | `use-bytechef/self-hosted/installation/distributed.mdx` |
| `scale-reliability/message-brokers.mdx` | 528 | `use-bytechef/self-hosted/configuration/message-brokers.md` (new) |
| `scale-reliability/cloud-native-storage.mdx` | 568 | `use-bytechef/self-hosted/configuration/file-storage.md` (new) |
| `scale-reliability/multi-tenant-isolation.mdx` | 627 | `use-bytechef/self-hosted/architecture.mdx` |
| `scale-reliability/crash-recovery.mdx` | 373 | `use-bytechef/self-hosted/management/crash-recovery.mdx` (new) |
| `scale-reliability/plan-limits.mdx` | 418 | `use-bytechef/self-hosted/configuration/plan-limits.md` (new) |
| `scale-reliability/runtime-job.mdx` | 1371 | `use-bytechef/self-hosted/runtime-job.mdx` (new) — merged with the row below |
| `runtime-job-runner/index.mdx` | 778 | same page; these two are duplicates of each other |
| `governance-security/flexible-deployment.mdx` | 1033 | `use-bytechef/self-hosted/index.mdx` + `architecture.mdx` |
| `support-trust/production-migrations.mdx` | 667 | `use-bytechef/self-hosted/management/upgrades.mdx` |

**Becomes a new guide page — no home exists today (3)**

| From | Words | To |
|---|---:|---|
| `extensibility/code-workflows.mdx` | 2516 | `automation/build/workflows/code-workflows.mdx` (new) |
| `extensibility/polyglot-scripting.mdx` | 540 | `automation/build/workflows/code-workflows.mdx` (same page, scripting section) |
| `extensibility/built-in-components.mdx` | 538 | `automation/build/workflows/components.mdx` (new; fronts `reference/components`) |

**Merges into embedded (1)**

| From | Words | To |
|---|---:|---|
| `embedded-ipaas/index.mdx` | 162 | `embedded/get-started/index.mdx` |

**Section indexes — dissolve into the Enterprise index (6)**

`collaboration-devops/index.mdx`, `data-knowledge/index.mdx`, `extensibility/index.mdx`,
`governance-security/index.mdx`, `scale-reliability/index.mdx`, `support-trust/index.mdx`.

**Deleted outright — sales copy with no guidance equivalent (2)**

`support-trust/support-slas.mdx` (519w), `support-trust/source-available-code.mdx` (616w).

**Rewritten as the surviving index (1)**

`enterprise/index.mdx` (1027w) → the EE capability index described in §5.1.

Totals: 10 + 15 + 11 + 3 + 1 + 6 + 2 + 1 = **49**.

### 5.3 Redirects

Two distinct URL namespaces, and conflating them is the easy mistake:

- **`/platform/enterprise/*`** — live today, deleted by this phase. Needs `permanent: true`
  redirects in `next.config.ts` so external and indexed links survive. One entry per deleted page,
  pointing at its mapped destination (not a blanket redirect to the index — a blanket rule would
  send someone looking for SSO to a page that merely links to SSO).
- **`/enterprise/*`** — pre-reorg paths, already 404. These are what the broken links point at.
  Phase 1 rewriting the links is the fix; redirects here are optional insurance and are **not**
  part of this spec.

`next.config.ts` already has a `redirects()` block with three entries, so the pattern exists.

## 6. Phase 3 — Stray duplicates and nav config

- **`embedded/configure/mcp-servers2.mdx`** (907w, "Embedded MCP", positioning voice) vs
  **`mcp-servers.md`** (569w, UI walkthrough). Same treatment as the enterprise tree: the
  positioning page's factual content (credential types, the `X-Environment` header, the optional-auth
  toggle, tenant scoping) folds into `mcp-servers.md`; the marketing framing is dropped;
  `mcp-servers2.mdx` is deleted.
- **`automation/build/workflows/human-in-the-loop2.mdx`** (1645w, "Coming soon" advanced HITL) vs
  **`human-in-the-loop.mdx`** (1506w, shipped basics). Both are explicitly listed in
  `build/workflows/meta.json`. Merge into one page, with the unshipped channels behind
  `comingSoon` frontmatter and a single callout, and remove the `human-in-the-loop2` nav entry.
- **Add the four missing `meta.json` files** (§3.3) so page order in those directories is declared
  rather than alphabetical.

## 7. Phase 4 — Accuracy pass

All ~201 hand-written pages (158 `.mdx` + 43 `.md`, excluding `reference/` and `openapi/`), checked
against the code on `0_732`, in batches by subtree. Suggested batch order, highest drift risk first
per the areas `CLAUDE.md` records as most-changed:

1. `automation/build/with-ai/**` + `automation/build/workflows/ai/**` — AI Hub, agents, guardrails, evals
2. `automation/deploy/**` + `settings/mcp-server.md` — MCP, A2A, AI Gateway, API platform
3. `platform/embedded/**` — the embedded bridge, connections, MCP
4. `platform/settings/**` + `automation/settings/**` — post-fold, these are newly written
5. `use-bytechef/self-hosted/**` — configuration properties, installation
6. `automation/data/**`, `automation/build/workflows/**` (non-AI), `automation/get-started/**`
7. `developer-guide/**`

Per the agreed policy: **clear-cut errors are fixed directly** — wrong property names, renamed
classes, dead endpoints, stale enum values — and each batch reports a changelog of every claim
altered. Where a page describes intended-but-unshipped behavior, it is marked `comingSoon` rather
than deleted.

Specific things to verify, drawn from `CLAUDE.md`'s record of recent changes: `AiHubChatKind`
(`STANDARD` / `WORKFLOW_CHAT` / `AGENT_CHAT`, and that `TASK` is gone), agent scheduling as an
`ai_agent_channel` row rather than a task entity, the AI model catalog CE/EE split, guardrails as a
standalone module rather than gateway-only, `bytechef.ai.auto-memory.provider`, plan limits and
enforcement properties, and the draft/publish model for custom components and code workflows.

## 8. Phase 5 — Fumadocs components and frontmatter schema

### 8.1 Frontmatter schema

Extend `source.config.ts`'s `frontmatterSchema.extend({...})` — which already carries `preview`,
`index` and `method` — with two booleans:

```ts
ee: z.boolean().default(false),
comingSoon: z.boolean().default(false),
```

`ee: true` marks a wholly-EE page; `<EEBadge />` continues to mark EE-only *sections* inline.
`comingSoon: true` replaces the four ad-hoc prose forms as the machine-readable status signal, with
one `<Callout>` rendered from it rather than hand-written per page.

This is deliberately a structured replacement for something the marketing site currently obtains by
scraping prose (`bytechef-website/plans/website-ia/07-feature-status.md` re-baselines against these
banners). Converting the 67 pages is part of this phase.

### 8.2 Component registration

Add to `mdx-components.tsx`: `Steps`/`Step` (`fumadocs-ui/components/steps`), `TypeTable`
(`.../type-table`), `ImageZoom` (`.../image-zoom`), `Banner` (`.../banner`). `Tabs`, `Files` and
`Accordion` are already registered but unused.

### 8.3 Application

Applied where the content already has the shape, not sprayed for coverage:

- **`Steps`** — numbered procedures. Strongest candidates: `embedded/get-started/quick-start`,
  `initial-setup/*`, `automation/get-started/quick-start/*`, `data/knowledge-base/*`,
  the installation guides.
- **`Tabs`** — per-platform and per-language variants. Strongest candidates: the seven
  `self-hosted/installation/*` pages, `connections/authentication/*`, SDK snippets.
- **`TypeTable`** — configuration property tables, notably
  `self-hosted/configuration/environment-variables.md` (4174w).
- **`Files`** — project scaffolding in `developer-guide/`.
- **`ImageZoom`** — the screenshot-heavy UI walkthroughs.

`.md` → `.mdx` renaming happens only on pages that actually gain a component. URLs are unaffected by
the extension, so no redirects are needed for renames.

## 9. Verification

Per phase:

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
./node_modules/.bin/bun ./scripts/lint.ts      # link validator; must end clean
npm run types:check                             # frontmatter schema changes
npm run build                                   # phases 2 and 5, before landing
```

Expected link-instance counts: 528 → 340 after phase 1 → 0 after phases 2–3.

Fragment check: the count of `invalid-fragment` results must be 0 at every phase boundary. It is
0 today, so any appearance means a rewrite re-attached a fragment to the wrong page.

## 10. Commit plan

One commit per phase, `732 docs - <description>` per the repo's convention (docs are neither
client- nor server-side; recent docs commits use a `docs - ` prefix). Phase 4 commits per batch.

## 11. Risks and decisions

- **Voice conversion is judgment work, not mechanical.** ~36,000 words are being rewritten, not
  moved. This is the phase most likely to need review iteration, and the reason phase 1 is kept
  independent of it.
- **Redirect coverage is per-page by design.** A blanket `/platform/enterprise/:path*` → index rule
  is cheaper but degrades every inbound deep link into a landing page.
- **Deleted sales copy is not preserved here.** `support-slas` and `source-available-code` are
  removed from the docs repo; recovering them means `git show`. Porting them to the website is out
  of scope.
- **The accuracy pass fixes directly.** Review happens through the diff and the per-batch changelog,
  not through a pre-approval gate on each claim.
- **Fragment targets interact with phase 5.** Several surviving fragments point at headings such
  as `#code-workflows-coming-soon` and `#microservices-topology-coming-soon`. Converting those
  pages to `comingSoon` frontmatter removes the heading and breaks the fragment. Phase 5 must
  re-run the validator and repoint them, not just delete the prose.
- **`enterprise/index.mdx` becomes a maintenance obligation.** Every new EE feature needs a row.
  Nothing enforces it; the `ee: true` frontmatter flag at least makes an audit query possible.
