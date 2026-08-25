# Coming Soon inventory

Everything the docs mark as not yet available in the latest released version of ByteChef.
Two distinct mechanisms, and the difference decides whether a page can be hidden:

- **Whole-page** — `comingSoon: true` in the page's frontmatter. `app/(docs)/[...slug]/page.tsx`
  renders a *Coming soon* badge plus a warn callout. The entire page describes unreleased
  behaviour, so it is safe to hide from the sidebar.
- **Partial** — a shipped page that defers one feature inline, through a
  `<Callout type="warn" title="Coming soon">` under a specific heading, or an italic
  *(coming soon)* in a table cell or sentence. **Never hide the page** — the surrounding content is
  released. The section itself can be commented out when the feature is far enough out that
  describing it as arriving soon does not help.

Two ways something here is hidden, and the tables below mark both:

- **Page hidden** — the page's name is removed from its parent `meta.json` `pages` array. The URL
  keeps working, so inbound links, search and the sitemap are unaffected; only the sidebar entry
  goes away.
- **Section commented out** — the prose is wrapped in `{/* … */}` (`.mdx`) or `<!-- … -->` (`.md`)
  so it does not render. The source stays, and each block names this file, so restoring it when the
  feature ships is uncommenting rather than rewriting.

Both are reversible and neither deletes anything.

**50 whole-page** (22 hidden) · **30 partial** pages (7 with a commented-out section)

---

## Whole-page Coming Soon

### Automation

| Page | Path | Edition | Sidebar |
|---|---|---|---|
| Memories | [`/platform/automation/ai/memories`](/platform/automation/ai/memories) | CE | visible |
| AI Copilot | [`/platform/automation/build/ai-copilot`](/platform/automation/build/ai-copilot) | EE | visible |
| AI Hub | [`/platform/automation/build/hub`](/platform/automation/build/hub) | — | **hidden** |
| Workflow Chats | [`/platform/automation/build/hub/workflow-chats`](/platform/automation/build/hub/workflow-chats) | — | **hidden** |
| Advanced Behavior | [`/platform/automation/build/hub/workflow-chats/advanced`](/platform/automation/build/hub/workflow-chats/advanced) | — | **hidden** |
| Embed the Chat Widget | [`/platform/automation/build/hub/workflow-chats/chat-widget`](/platform/automation/build/hub/workflow-chats/chat-widget) | — | **hidden** |
| Enable Chat on a Workflow | [`/platform/automation/build/hub/workflow-chats/enable-chat`](/platform/automation/build/hub/workflow-chats/enable-chat) | — | **hidden** |
| Start a Workflow Chat | [`/platform/automation/build/hub/workflow-chats/using-chats`](/platform/automation/build/hub/workflow-chats/using-chats) | — | **hidden** |
| Testing Voice In The Editor | [`/platform/automation/build/voice/editor-testing`](/platform/automation/build/voice/editor-testing) | — | **hidden** |
| Voice Quickstart | [`/platform/automation/build/voice/quickstart`](/platform/automation/build/voice/quickstart) | — | **hidden** |
| Evals | [`/platform/automation/build/workflows/ai/agent/evals`](/platform/automation/build/workflows/ai/agent/evals) | CE | visible |
| Code Workflows | [`/platform/automation/build/workflows/code-workflows`](/platform/automation/build/workflows/code-workflows) | EE | visible |
| Data Streams | [`/platform/automation/build/workflows/data-streams`](/platform/automation/build/workflows/data-streams) | CE | visible |
| Error Workflows | [`/platform/automation/build/workflows/error-workflows`](/platform/automation/build/workflows/error-workflows) | CE | visible |
| Asset Files | [`/platform/automation/data/asset-files`](/platform/automation/data/asset-files) | CE | visible |
| Sources | [`/platform/automation/data/knowledge-base/sources`](/platform/automation/data/knowledge-base/sources) | CE | **hidden** |
| A2A Servers | [`/platform/automation/deploy/a2a-servers`](/platform/automation/deploy/a2a-servers) | CE | **hidden** |
| API Platform | [`/platform/automation/deploy/api-platform`](/platform/automation/deploy/api-platform) | — | visible |
| Guardrails | [`/platform/automation/settings/ai-agents/guardrails`](/platform/automation/settings/ai-agents/guardrails) | EE | **hidden** |
| System Prompt | [`/platform/automation/settings/ai-agents/system-prompt`](/platform/automation/settings/ai-agents/system-prompt) | EE | **hidden** |
| AI Hub Connectors | [`/platform/automation/settings/ai-hub-connectors`](/platform/automation/settings/ai-hub-connectors) | EE | **hidden** |
| API Keys | [`/platform/automation/settings/api-keys`](/platform/automation/settings/api-keys) | EE | visible |
| Git Configuration | [`/platform/automation/settings/git-configuration`](/platform/automation/settings/git-configuration) | EE | visible |
| Users | [`/platform/automation/settings/users`](/platform/automation/settings/users) | EE | visible |

### Embedded

| Page | Path | Edition | Sidebar |
|---|---|---|---|
| Automation Code Workflows | [`/platform/embedded/build/automations/automation-code-workflows`](/platform/embedded/build/automations/automation-code-workflows) | CE | **hidden** |
| Automation workflows | [`/platform/embedded/build/automations/automation-workflows`](/platform/embedded/build/automations/automation-workflows) | EE | visible |
| Theming and Localization | [`/platform/embedded/build/theming-and-localization`](/platform/embedded/build/theming-and-localization) | — | visible |
| Unified API | [`/platform/embedded/build/unified-api`](/platform/embedded/build/unified-api) | — | **hidden** |
| Field Mapping | [`/platform/embedded/build/workflows/field-mapping`](/platform/embedded/build/workflows/field-mapping) | — | visible |

### Platform Settings

| Page | Path | Edition | Sidebar |
|---|---|---|---|
| Admin API Keys | [`/platform/settings/admin-api-keys`](/platform/settings/admin-api-keys) | EE | visible |
| Audit Events | [`/platform/settings/audit-events`](/platform/settings/audit-events) | EE | visible |
| API Connectors | [`/platform/settings/components/api-connectors`](/platform/settings/components/api-connectors) | — | **hidden** |
| Component Visibility | [`/platform/settings/components/component-visibility`](/platform/settings/components/component-visibility) | EE | visible |
| Custom Components | [`/platform/settings/components/custom-components`](/platform/settings/components/custom-components) | EE | visible |
| Connections | [`/platform/settings/connections`](/platform/settings/connections) | EE | visible |
| Identity Providers | [`/platform/settings/identity-providers`](/platform/settings/identity-providers) | — | visible |
| License | [`/platform/settings/license`](/platform/settings/license) | EE | visible |
| MCP Server | [`/platform/settings/mcp-server`](/platform/settings/mcp-server) | CE | visible |
| OAuth2 Clients | [`/platform/settings/oauth2-clients`](/platform/settings/oauth2-clients) | EE | **hidden** |
| Users | [`/platform/settings/users`](/platform/settings/users) | — | visible |

### Self-Hosted

| Page | Path | Edition | Sidebar |
|---|---|---|---|
| Distributed (Coordinator/Worker) | [`/platform/use-bytechef/self-hosted/installation/distributed`](/platform/use-bytechef/self-hosted/installation/distributed) | CE | **hidden** |
| Crash Recovery | [`/platform/use-bytechef/self-hosted/management/crash-recovery`](/platform/use-bytechef/self-hosted/management/crash-recovery) | CE | visible |

### Your Account

| Page | Path | Edition | Sidebar |
|---|---|---|---|
| Appearance | [`/platform/your-account/appearance`](/platform/your-account/appearance) | CE | visible |

### API Reference

| Page | Path | Edition | Sidebar |
|---|---|---|---|
| Deploy a new code based project | [`/openapi/automation-project-code-workflow/deployProject`](/openapi/automation-project-code-workflow/deployProject) | — | **hidden** |
| Get a workflow execution by id | [`/openapi/automation-workflow-execution/getWorkflowExecution`](/openapi/automation-workflow-execution/getWorkflowExecution) | — | **hidden** |
| Get workflow executions | [`/openapi/automation-workflow-execution/getWorkflowExecutionsPage`](/openapi/automation-workflow-execution/getWorkflowExecutionsPage) | — | **hidden** |
| Embedded Automation Bridge | [`/openapi/backend/embedded-configuration-automation-project-code-workflow`](/openapi/backend/embedded-configuration-automation-project-code-workflow) | — | visible |
| Embedded Tool Invocations | [`/openapi/backend/embedded-tool-invocation`](/openapi/backend/embedded-tool-invocation) | — | visible |
| Embedded Workflow Executions | [`/openapi/backend/embedded-workflow-execution`](/openapi/backend/embedded-workflow-execution) | — | visible |
| Custom Components | [`/openapi/custom-components`](/openapi/custom-components) | — | **hidden** |

---

## Partial — shipped pages with a deferred feature

These pages stay in the sidebar: the surrounding content is released. A section marked
**commented out** does not render — its feature is far enough out that describing it as
arriving soon was not helping a reader.

| Page | Sections | State |
|---|---|---|
| [`/openapi`](/openapi) | (page intro) | rendered |
| [`/developer-guide/architecture`](/developer-guide/architecture) | **AI & Agents** | **all commented out** |
| [`/platform/automation/ai/skills`](/platform/automation/ai/skills) | Create With AI; The Skills Copilot; Skills as Tools for Other Agents; Test with Evals | rendered |
| [`/platform/automation/build/connections`](/platform/automation/build/connections) | Get Started; Managing Connections | rendered |
| [`/platform/automation/build/workflows/ai/agent`](/platform/automation/build/workflows/ai/agent) | Chat Memory Slot; Chat memory vs Auto Memory; Guardrails Slot; The Agent Utils toolset; Test with Evals; **Realtime Chat** | **1 of 6 commented out** |
| [`/platform/automation/build/workflows/ai/agent/agent-utils`](/platform/automation/build/workflows/ai/agent/agent-utils) | Ask User Question; Agent Client; Auto Memory | rendered |
| [`/platform/automation/build/workflows/flow-controls`](/platform/automation/build/workflows/flow-controls) | (page intro); Outputs; Graph | rendered |
| [`/platform/automation/build/workflows/human-in-the-loop`](/platform/automation/build/workflows/human-in-the-loop) | Coming soon: expanded approvals | rendered |
| [`/platform/automation/build/workflows/projects`](/platform/automation/build/workflows/projects) | New Code Workflow; Share Projects and Workflows as Templates | rendered |
| [`/platform/automation/build/workflows/workflows`](/platform/automation/build/workflows/workflows) | Multiple triggers; Stream data in bulk; Add Sticky Notes; Canvas Controls; Test with Chat (**voice half commented out**) | **1 of 5 partly commented out** |
| [`/platform/automation/data/data-tables`](/platform/automation/data/data-tables) | Storage limits | rendered |
| [`/platform/automation/data/knowledge-base/add-documents`](/platform/automation/data/knowledge-base/add-documents) | Storage limits | rendered |
| [`/platform/automation/monitor/workflow-executions`](/platform/automation/monitor/workflow-executions) | Retention; Reading executions programmatically | rendered |
| [`/platform/embedded/build/app-events`](/platform/embedded/build/app-events) | Firing an App Event from your application | rendered |
| [`/platform/embedded/build/integrations`](/platform/embedded/build/integrations) | AI Copilot | rendered |
| [`/platform/embedded/build/workflows`](/platform/embedded/build/workflows) | AI Copilot | rendered |
| [`/platform/embedded/get-started`](/platform/embedded/get-started) | What you get; Going deeper; How the pieces fit together | rendered |
| [`/platform/embedded/get-started/quick-start`](/platform/embedded/get-started/quick-start) | Let your users build their own workflows (optional) | rendered |
| [`/platform/embedded/get-started/quick-start/sample-app`](/platform/embedded/get-started/quick-start/sample-app) | What the sample demonstrates | rendered |
| [`/platform/embedded/get-started/tenant-isolated-security`](/platform/embedded/get-started/tenant-isolated-security) | Layer 3: Crypto | rendered |
| [`/platform/embedded/monitor/failures-and-retries`](/platform/embedded/monitor/failures-and-retries) | No replay from your product; **No automatic retry policy** | **1 of 2 commented out** |
| [`/platform/embedded/monitor/workflow-executions`](/platform/embedded/monitor/workflow-executions) | Execution Detail View | rendered |
| [`/platform/settings/ai-providers`](/platform/settings/ai-providers) | What uses activated providers | rendered |
| [`/platform/settings/notifications`](/platform/settings/notifications) | Delivery types | rendered |
| [`/platform/settings/workspaces`](/platform/settings/workspaces) | Edit, manage members, or delete | rendered |
| [`/platform/use-bytechef/self-hosted/architecture`](/platform/use-bytechef/self-hosted/architecture) | AI & Agents | **3 of 6 commented out** |
| [`/platform/use-bytechef/self-hosted/configuration`](/platform/use-bytechef/self-hosted/configuration) | Rotation; Data retention | rendered |
| [`/platform/use-bytechef/self-hosted/configuration/environment-variables`](/platform/use-bytechef/self-hosted/configuration/environment-variables) | AI Copilot Configuration; AI Brave Configuration; **AI Gateway Configuration**; AI Hub Configuration; AI Knowledge Base Configuration; AI MCP Server Configuration; **Context Store Configuration**; Component Configuration; Data Table Configuration; OAuth2 Configuration; Plan Limits Configuration; Execution Recovery; Code Workflow | **2 of 15 commented out** |
| [`/platform/use-bytechef/self-hosted/management/observability`](/platform/use-bytechef/self-hosted/management/observability) | **Datadog and Splunk (direct OTLP)** | **all commented out** |
| [`/platform/your-account/profile`](/platform/your-account/profile) | Two-Factor Authentication; Linked Accounts | rendered |

Bold section names are the commented-out ones.

### Voice (2026-08-21)

Every voice surface is `0_732`-only and absent from master, so all of it is commented out rather
than badged: `BrowserVoiceSessionTrigger` (`browser/v1/voiceSession`), the client's
`BrowserVoiceSession` / `voiceMode` / `checkVoiceSupport`, and the AI Agent's `realtimeChat`
action. Four sections in three pages, listed above. Restore by uncommenting each block once those
ship — the source is intact and each block names this file.

### Feature-flagged UI counts as Coming Soon (2026-08-21)

A control that exists in master's source but sits behind a PostHog flag that is **off** renders for
nobody, so it is treated exactly like unshipped code: whole pages get `comingSoon: true`, individual
sections get the callout or are commented out. Reading the source is not enough to tell — check a
running instance, or check the gate.

`client/src/shared/layout/Settings.tsx` is the authoritative gate for the settings nav:

| Settings nav item | Flag | Docs page |
|---|---|---|
| `users` | `ff-3900` | `/platform/settings/users` |
| `identity-providers` | `ff-1040` | `/platform/settings/identity-providers` |
| `admin-api-keys` | `ff-1024` | `/platform/settings/admin-api-keys` |
| `custom-components` | `ff-1024` | `/platform/settings/components/custom-components` |
| `api-connectors` | `ff-207` | `/platform/settings/components/api-connectors` |
| `/account/appearance` | `ff-445` | `/platform/your-account/appearance` |
| `git-configuration` | `ff-1039` | `/platform/automation/settings/git-configuration` |
| `workspace-api-keys` | `ff-1025` / `ff-1039` / `ff-4814` | `/platform/automation/settings/api-keys` |

Section-level cases: **Share** (`ff_1042`) and **Share with Community** (`ff_2939`) on the projects
page. Sticky notes and voice are the plainer kind — that code is simply not on master at all.

**EE-gated is NOT the same thing.** An Enterprise-only page is released for EE customers and keeps
its `<EEBadge />` without a Coming Soon mark; only the flag decides.

---

## Keeping this current

The whole-page table is generated. Do not edit its rows by hand:

    npm run coming-soon         # check — fails if the table drifted or a marker is missing
    npm run coming-soon:write   # regenerate the table and the counts

`scripts/coming-soon.mts` derives each row from `comingSoon: true` frontmatter and computes the
Sidebar column by walking every parent `meta.json`, since a page is only reachable when each folder
above it is listed too. The `openapi/` pages are generated, but five of them do carry the flag, so
they belong in the table — the scan does not skip that directory. Hand-editing is what let the
counts drift to "43 whole-page (17 hidden)" when the truth was 42 and 24.

**Commenting a section out: add `@coming-soon` inside the comment.**

    {/* @coming-soon voiceSession has not shipped; restore when it does.

    ## Voice
    ... original prose, unchanged ...
    */}

The marker is what the registry matches on. It replaced a scan that looked for the words "coming
soon" in the comment text, which meant rewording a block silently dropped its entry — that is how
the observability section went missing once. A marker cannot be reworded away, and the check fails
when a block that is structurally a section (it contains a heading or a table row) has none, so the
failure is loud instead of silent. Comment the original text and add a note around it rather than
rewriting it.

The partial table is still curated by hand: its section names are editorial, and the markers vary
(`<Callout type="warn" title="Coming soon">`, `**Coming soon.**`, an inline `(coming soon)`). Keep
it in sync yourself.

**A page can only be in one table.** When a partial page is promoted to whole-page `comingSoon`,
delete its partial row — otherwise it is counted twice. That happened to the AI Hub and settings
pages the day they were marked.

**Verify a flag before recording it; do not infer it from the folder.** `api-connectors` is gated by
`ff-207`, not the `ff-1024` that `custom-components` uses, and `/account/appearance` is gated by
`ff-445` — both were recorded wrongly at first by guessing from the docs path. `Settings.tsx` is the
only authority.
