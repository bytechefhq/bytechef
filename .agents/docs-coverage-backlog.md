# Docs coverage backlog

Tracks UI surfaces found in the code that are **not yet documented with a dedicated page**, so
they can be prioritized later. Produced during the 2026-07 documentation deep pass. Everything
here was verified to exist in the client/server code; the reason each is deferred is noted.

Pages created during that pass (for context, not backlog): Agent Memories, Users & Invitations,
OAuth2 Clients, API Keys, API Connectors, and the three embedded editor/API pages
(integration workflows, automation workflows, Unified API). AI Hub connectors and the AI Agent
cross-link were folded into existing pages.

**2026-07-27 policy change:** features that are built on branch `0_732` but not yet in `master`
now get documented immediately, carrying the "Coming soon" marker, instead of sitting in this
backlog — see `docs/content/docs/automation/error-workflows.mdx` (`.mdx` `<Callout>` form) and
`docs/content/docs/platform/ai-gateway.md` (`.md` blockquote form) for the exact wording. All four
entries previously listed under the two "Deferred" sections below were verified against the code
and documented under that policy:

- **AI Gateway detail sections** (Providers/Models/Projects, Routing Policies, Budgets & Rate
  Limits, Prompts, Playground, Traces & Sessions, Scores, Alerts, Exports, Datasets &
  Experiments) — `platform/ai-gateway.md` now has one `##` section per group, grounded in the
  actual `client/src/pages/automation/ai/gateway/components/**` UI. No per-section "Coming soon"
  badge was added — the page-level one already covers every section.
- **Share / Share with Community** (project + workflow three-dot menus, flags `ff-1042` /
  `ff-2939`) — new "Share Projects and Workflows as Templates" section in
  `automation/build/projects.mdx`, marked "Coming soon".
- **Pull from Git / Git Configuration** (flag `ff-1039`) — already fully and accurately documented
  in `enterprise/collaboration-devops/git-backed-change-tracking.mdx` (verified against
  `ProjectGitConfigurationDialog.tsx` / `ProjectTabButtons.tsx`); no changes were needed there.
  Note: that page carries no page-level "Coming soon" marker of its own — pre-existing, out of
  scope for this pass.
- **Project History** (version-history sheet), **Deploy button**, **Run/Test**, and the
  **Output-panel toggle** — new "Editor Header Controls" section in `automation/build/workflows.mdx`.
  These four all already exist on `master` (verified: `DeployButton.tsx` is byte-identical between
  `master` and `0_732`; `OutputButton.tsx`/`WorkflowActionsButton.tsx`/`ProjectVersionHistorySheet.tsx`
  predate this branch by months), so they were documented as regular, already-available
  functionality — **no** "Coming soon" marker.
- **Voice test sessions** in the workflow test chat panel — a "Test with Chat" subsection (nested
  under the same "Editor Header Controls" section, `automation/build/workflows.mdx`) covers the
  base (already-`master`) test chat panel plainly, then marks the voice-session part "Coming soon".
  Verified `useWorkflowTestVoiceSession.ts` and `browser-voice/BrowserVoiceSession.ts` do **not**
  exist on `master` — genuinely 0_732-only, unlike the rest of the panel.

## Verified-absent / do NOT document

Found during the pass but confirmed **not** reachable UI or not implemented — recorded so they are
not re-flagged:

- **Connection reassignment dialog** — `ConnectionReassignmentDialog.tsx` exists but is not imported/rendered anywhere (dead code).
- **Deployment "Duplicate" action** — the deployment ellipsis menu exposes only Edit, Change Project Version, and Delete.
- **Unified API models beyond Account** — `CrmModelType`/`AccountingModelType` enums list many models, but only the **Account** resource is REST-exposed today (other paths are commented out in the OpenAPI spec). The Unified API page documents Account endpoints and flags the rest as not yet wired.

## Notes for whoever picks this up

- The `reference/` section (component/flow-control pages) is **auto-generated** by
  `./gradlew generateDocumentation` — do not hand-edit those.
- Screenshot placeholders use `{/* TODO screenshot: ... */}` in `.mdx` and `<!-- TODO screenshot: ... -->`
  in `.md`; grep `TODO screenshot` to find every spot awaiting a real image.
