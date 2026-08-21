# Screenshots needed

Every entry below is a `TODO screenshot:` comment sitting in a published page where an
image belongs. Capture the screenshot, drop it beside the page (or under `public/`), and
replace the comment with a normal `![alt](path)` reference.

These are **comments, not broken images** — a page with an outstanding entry renders
cleanly today, it is just missing an illustration. That is deliberate: 31 placeholder
image files (a 1x1 PNG and a grey rectangle, each reused across many pages) used to stand
in for these, which rendered as empty boxes and looked like a broken build.

**Outstanding: 148**, of which **23** sit on pages marked coming soon in full and **20** on pages
with a coming-soon section. A page-level marker means the UI does not exist in the released
version, so there is nothing to photograph — capture those only once the feature ships. Each is
annotated below its heading.

## Conventions
- `.mdx` pages use `{/* TODO screenshot: … */}`; `.md` pages use `<!-- TODO screenshot: … -->`.
- Capture at a consistent width, in light mode unless the feature only exists in dark.
- Co-locate the image with its page (`some-page/image.png`) unless it is shared, in which
  case put it under `public/`.
- Redact real credentials, tokens, customer names and email addresses.

## Automation (74)

### `content/docs/platform/automation/ai/memories.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L11** — Memories page showing the AI sidebar, the Type filter, and the memories table with Title, Type, Description, Updated, and Actions columns
- [ ] **L37** — Left sidebar with the AI section nav (Skills, Memories) above the Type filter (All, User, Feedback, Project, Reference)
- [ ] **L104** — Memory detail dialog showing the title, type badge, name/updated line, description, and rendered Markdown content
- [ ] **L126** — Edit memory dialog showing the disabled Name field, the editable Title, Description, Type select, and Content (Markdown) text area, with Cancel and Save buttons
- [ ] **L140** — Delete confirmation dialog with the permanent-deletion warning and the Cancel / Delete permanently buttons

### `content/docs/platform/automation/build/connections/authentication/api-key.mdx`
- [ ] **L30** — Create Connection dialog for an API-key component showing the Name field and the API Key (and optional secret) input fields with the Save button

### `content/docs/platform/automation/build/connections/authentication/basic.mdx`
- [ ] **L19** — Create Connection dialog for a Basic Auth component showing the Username and Password fields with the Save button

### `content/docs/platform/automation/build/connections/authentication/bearer.mdx`
- [ ] **L28** — Create Connection dialog for a Bearer Token component showing the Token input field with the Save button

### `content/docs/platform/automation/build/connections/authentication/oauth2.mdx`
- [ ] **L19** — OAuth2 connection dialog — client ID and client secret fields with the Connect button that launches the provider consent flow

### `content/docs/platform/automation/build/connections/index.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L24** — Connections page — connection list with the Create Connection dialog open showing the component picker and auth fields

### `content/docs/platform/automation/build/ai-copilot.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L30** — workflow editor with the Copilot panel docked on the right — the header with Clean messages, a conversation in progress, and the composer showing the Build toggle and model picker; sparkle button visible on the editor toolbar
- [ ] **L101** — node properties panel with a field's sparkle button clicked open — the inline prompt popover where you describe the value to generate
- [ ] **L121** — the Copilot model picker open — the provider search box, the "Use workspace default" item, and a provider expanded to show its model list

### `content/docs/platform/automation/build/with-ai/hub/index.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L13** — AI Hub home — chats sidebar on the left (New Chat / More above the Chats list), the home composer in the main panel with the model picker open
- [ ] **L57** — chat composer close-up — model picker dropdown open showing the Workflow Chats / Agent Chats cascades above the catalog models, microphone and attachment buttons visible
- [ ] **L70** — AI Hub right-hand resource panel open beside a chat — a viewer tab (e.g. a data table or workflow) with the panel's tab strip and close toggle
- [ ] **L76** — AI Hub connectors page — the component connectors list (each row with an enable toggle, a tools chevron, and an ellipsis menu) above the custom MCP servers list, with the Add connector and Add MCP server buttons

### `content/docs/platform/automation/build/with-ai/hub/workflow-chats/using-chats.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L28** — the AI Hub composer's provider popup open on the home view, with the Tasks / Workflow Chats / Agent Chats cascades visible and the Workflow Chats submenu expanded

### `content/docs/platform/automation/build/workflows/ai/agent/guardrails/index.md`
- [ ] **L61** — the AI Agent editor Guardrails slot showing a Check For Violations parent with a Model child and several child detectors (PII, Jailbreak, NSFW) attached, plus a Sanitize Text parent on the outbound side

### `content/docs/platform/automation/build/workflows/ai/agent/index.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L8** — the AI Agent node on the workflow canvas, expanded so its cluster-element children (Model, Tools, Memory) are visible beneath the root node

### `content/docs/platform/automation/build/workflows/ai/agentic-ai.md`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L34** — the Agentic AI node editor showing the Run action's Goal Description / Goal Output Binding / Goal Mode fields alongside two attached Action cluster elements with their input/output bindings

### `content/docs/platform/automation/build/workflows/data-pills.mdx`
- [ ] **L34** — workflow editor with the Data Pill Panel open on the left — trigger and upstream components expanded into their output field pills, next to a property field mid-insertion
- [ ] **L65** — a property field showing the Dynamic switch toggled on and the leading f(x) marker that indicates formula mode

### `content/docs/platform/automation/build/workflows/data-streams.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L25** — Data Stream simple editor on the Mapping step — step nav (Source / Destination / Mapping / Test) at the top, field-mapping rows and the Auto-map matching fields button in the body
- [ ] **L29** — Data Stream advanced editor — cluster-elements canvas with Source, Field Mapper processor, and Destination nodes wired to the Data Stream root

### `content/docs/platform/automation/build/workflows/forms.mdx`
- [ ] **L62** — workflow editor form trigger/step configuration — the form fields builder and page options

### `content/docs/platform/automation/build/workflows/human-in-the-loop.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L127** — workflow editor with an approval task node selected — its properties panel showing the interaction mode and approval channel

### `content/docs/platform/automation/build/workflows/projects.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L37** — Projects page with the Create Project dialog open — name and description fields and the Create button
- [ ] **L110** — project details pane with the workflow list and the Create Workflow menu (New workflow / Import / Generate with AI) open
- [ ] **L166** — Share project dialog with an active template link and the Copy link button

### `content/docs/platform/automation/build/workflows/templates.mdx`
- [ ] **L17** — project templates gallery — card grid with template titles, descriptions, and component icons

### `content/docs/platform/automation/build/workflows/workflows.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L134** — workflow canvas with a sticky note next to a few nodes — note in edit mode showing Markdown text and the color swatch row
- [ ] **L142** — workflow editor canvas toolbar — zoom, fit-to-screen, layout-direction, reset-layout, lock, undo/redo, and add-note buttons
- [ ] **L162** — Workflow Inputs sheet — inputs table with the add-input dialog open showing the Type, Name, Label, and Required fields
- [ ] **L259** — Project History sheet open, showing a Published and a Draft version
- [ ] **L281** — test chat panel with an active voice session, mic button in its recording state

### `content/docs/platform/automation/data/asset-files.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L15** — Asset Files page — file list with source badges and the drag-and-drop upload zone

### `content/docs/platform/automation/data/context-store.md`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L17** — Context Store page — list of sources with their sync status and the per-row actions menu (Refresh now / Disable / Delete)
- [ ] **L18** — Add Context Source dialog on step 1 (Connection) — the Context Store select, Source Name field, and the component grid picker
- [ ] **L29** — source detail dialog — the entity's indexed fields, cadence, and last-sync details

### `content/docs/platform/automation/data/data-tables.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L61** — Data Table detail grid — typed columns (string, number, boolean, date), an inline cell being edited, row-select checkboxes, and the ⋮ actions menu open showing Import CSV / Export CSV / Rename / Delete
- [ ] **L113** — Data Tables page with the storage-usage warning alert visible (usage approaching the cap)

### `content/docs/platform/automation/data/knowledge-base/add-documents.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L49** — knowledge base Documents tab — a list of uploaded documents with their status badges (Uploaded / Processing / Ready / Error) and per-row actions

### `content/docs/platform/automation/data/knowledge-base/create-a-knowledge-base.mdx`
- [ ] **L25** — New Knowledge Base dialog — Name, Description, the three chunk-setting fields, and the Upload Documents drop zone

### `content/docs/platform/automation/data/knowledge-base/search.mdx`
- [ ] **L20** — knowledge base Search tab — query box with a natural-language query, optional metadata filter, and result cards showing source document, chunk index, and relevance score

### `content/docs/platform/automation/deploy/ai-gateway.md`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L19** — AI → Gateway with the Gateway sidebar group expanded and the Monitoring dashboard open — request-volume, error-rate, latency, and cost-breakdown charts
- [ ] **L53** — Providers list with the Add Provider dialog open — Type, API Key, and Base URL fields and the Test Connection button
- [ ] **L59** — Routing Policies list with the Strategy dropdown open on the create dialog
- [ ] **L67** — Rate Limits list with a Per Property scope rule and the create-rule form
- [ ] **L75** — A prompt's detail view with the three environment cards and the version-history table
- [ ] **L81** — Playground in Compare mode with two model responses side by side
- [ ] **L89** — A trace detail view with the Tree span view expanded, showing a Generation span's Input/Output JSON
- [ ] **L95** — Scores → Analytics with a numeric score's trend chart and a boolean score's distribution pie chart
- [ ] **L101** — Alerts → Rules with the rule dialog open, showing the Metric/Condition/Threshold fields and the notification-channel checklist
- [ ] **L107** — Exports → Webhooks with a subscription's delivery history open
- [ ] **L115** — Experiments comparison view with the aggregate score-delta table

### `content/docs/platform/automation/deploy/api-platform.mdx`
- [ ] **L15** — API Collections page — a collection expanded to show its endpoints (method, path, workflow)
- [ ] **L27** — Create API Collection dialog — Project and Project Version selectors, Name, Context Path, and Collection Version fields

### `content/docs/platform/automation/deploy/workflows.mdx`
- [ ] **L58** — Project Deployments page with the deploy dialog — project version selector, environment, and enabled toggle
- [ ] **L106** — deployment workflow connections tab — each component mapped to a connection via dropdowns

### `content/docs/platform/automation/deploy/mcp-servers.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L28** — MCP Servers page with the Create MCP Server dialog — name field and server type
- [ ] **L55** — workflows-as-tools mapping panel — a workflow's tool name, description, and per-input fromAi value fields

### `content/docs/platform/automation/get-started/quick-start/build-first-workflow.mdx`
- [ ] **L39** — ByteChef sign-in screen — email/password fields, social sign-in buttons, and the Register link
- [ ] **L48** — Create Project dialog — Name, Description, Category, and Tags fields with the Save button
- [ ] **L93** — node properties panel open for the Spotify Create Playlist action — the action dropdown, the connection selector with "Create Connection" option, and the property fields

### `content/docs/platform/automation/monitor/workflow-executions.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L26** — Workflow Executions page — the filter bar (project, workflow, status, date range) above the executions table
- [ ] **L83** — failed execution detail — the task tree with the failed step highlighted and its input/output/error panel

### `content/docs/platform/automation/settings/ai-agents/guardrails.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L17** — Settings → AI Agents → Guardrails — the five toggles, the blocked-terms textarea, and the blocking-mode radio group

### `content/docs/platform/automation/settings/ai-agents/system-prompt.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L22** — Settings → AI Agents → System Prompt — the monospace textarea with the character counter beneath it and the Save button

### `content/docs/platform/automation/settings/ai-hub-connectors.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L17** — Settings → AI Hub Connectors — the Pre-built Connectors section with several connector rows, one expanded to show its per-tool toggles, and the Custom MCP section below

### `content/docs/platform/automation/settings/api-keys.mdx`
- [ ] **L36** — Settings → API Keys page — header with the Environment selector and the New API Key button, and the keys table below
- [ ] **L50** — Create API Key dialog in the "Save your API Key" state — the read-only secret field, the Copy button, the one-time-reveal warning text, and the Done button

### `content/docs/platform/automation/settings/git-configuration.mdx`
- [ ] **L28** — Settings → Git Configuration page — the URL, Username, and Password fields with the Save button

### `content/docs/platform/automation/settings/users.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L25** — Settings → Current Workspace → Users — the members table with Email / Name / Role columns, a role dropdown open, and the Invite User button in the header

## Embedded (36)

### `content/docs/platform/embedded/administration/api-keys.md`
- [ ] **L6** — Embedded → Settings — the settings landing page showing the available sections
- [ ] **L12** — Embedded → Settings → API Keys — the keys table with the environment selector and the New API Key button

### `content/docs/platform/embedded/administration/signing-keys.md`
- [ ] **L6** — Embedded → Settings — the settings landing page showing the available sections
- [ ] **L12** — Embedded → Settings → Signing Keys — the signing-keys table with the New Signing Key button

### `content/docs/platform/embedded/build/app-events.md`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L34** — New App Event dialog showing the Name field and the JSON Schema code editor pre-filled with an example payload

### `content/docs/platform/embedded/build/automations/automation-workflows.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L31** — the Automation Workflow editor with the left sidebar, header breadcrumb, canvas, and Publish button
- [ ] **L69** — the editor left sidebar showing the project selector, search, sort, New Workflow button, and workflow cards
- [ ] **L95** — the Publish Project popover with the Description field
- [ ] **L101** — the Project Version History sheet with version accordion entries
- [ ] **L122** — the white-label Workflow Builder rendered inside a host app, showing the header label, Publish button, and canvas

### `content/docs/platform/embedded/build/permission-expressions.mdx`
- [ ] **L82** — the Permission Expression textarea on an integration workflow row (or the New Integration dialog), showing a sample SpEL expression like metadata['tier'] == 'gold'

### `content/docs/platform/embedded/build/unified-api.mdx`
- [ ] **L202** — the Unified API category filter (Accounting / CRM) on the Integrations list, shown when the Unified API feature flag is enabled

### `content/docs/platform/embedded/build/workflows/field-mapping.mdx`
- [ ] **L78** — the Connect dialog's field-mapping step — the object-type selector above the rows mapping application fields to integration fields

### `content/docs/platform/embedded/build/workflows/index.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L10** — the full integration workflow editor — left workflows sidebar, integration + workflow breadcrumb, canvas, and the header action buttons on the right
- [ ] **L34** — the left workflows sidebar showing the integration selector, search box, sort menu, New Workflow button, and workflow cards
- [ ] **L60** — the settings menu open, showing the Workflow and Integration tabs
- [ ] **L93** — the header action buttons — Output, Test/Chat, and Publish — with the output panel open below the canvas
- [ ] **L109** — the Publish popover with the Description field and the Publish button
- [ ] **L123** — the Integration Version History panel listing versions with status badges and descriptions

### `content/docs/platform/embedded/configure/instance-configurations.md`
- [ ] **L44** — The New Instance Configuration wizard on the Workflows step, showing the step indicator (Basic / OAuth2 Connection / Workflows) and the per-workflow enable toggles with connection and input fields

### `content/docs/platform/embedded/configure/mcp-servers.md`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L45** — New MCP Server dialog showing the Name field and the Require authentication / Enforce tool authorization toggles
- [ ] **L56** — Add Component dialog on the Select Tools step, showing the list of the component's actions with per-tool selection and the tool properties popover

### `content/docs/platform/embedded/get-started/initial-setup/adding-an-integration.mdx`
- [ ] **L16** — New Integration dialog with the component picker open and a component (e.g. Gmail) selected
- [ ] **L57** — the integration editor header with the Publish control open
- [ ] **L61** — the New Instance Configuration dialog with its integration, environment and connection fields

### `content/docs/platform/embedded/get-started/initial-setup/displaying-the-connect-dialog.mdx`
- [ ] **L42** — the ByteChef Connect dialog as it renders inside a host application, showing the integration list and a Connect button

### `content/docs/platform/embedded/get-started/initial-setup/installing-the-sdk.mdx`
- [ ] **L34** — the Create Signing Key dialog, including the one-time secret reveal state

### `content/docs/platform/embedded/get-started/quick-start/index.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L6** — the Embedded Quick Start landing page
- [ ] **L34** — the Create Signing Key dialog, including the one-time secret reveal state
- [ ] **L59** — the integration editor header with the Publish control open
- [ ] **L71** — the New Instance Configuration dialog with its integration, environment and connection fields
- [ ] **L138** — the ByteChef Connect dialog as it renders inside a host application, showing the integration list and a Connect button

### `content/docs/platform/embedded/get-started/quick-start/sample-app/index.md`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L6** — the running sample application with an embedded ByteChef surface visible

### `content/docs/platform/embedded/get-started/white-label-execution.mdx`
- [ ] **L54** — the embedded workflow builder rendered inside a host app's UI, white-labeled with the host product's branding

### `content/docs/platform/embedded/monitor/connected-users.md`
- [ ] **L68** — Connected user detail side sheet open, showing the Profile header card and the Integrations / MCP Servers / Automation Workflows tabs, plus the Enable/Disable and Delete actions in the sheet header

### `content/docs/platform/embedded/monitor/workflow-executions.md`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L66** — Workflow execution detail sheet open, showing the workflow panel with a selected step and its Input / Output / Error tabs

## Settings and administration (29)

### `content/docs/platform/settings/admin-api-keys.mdx`
- [ ] **L26** — Settings → Admin API Keys page — header with the Environment selector and the New API Key button, and the keys table below
- [ ] **L57** — Create API Key dialog in the "Save your API Key" state — the read-only secret field, the Copy button, the one-time-reveal warning text, and the Done button

### `content/docs/platform/settings/ai-providers.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L22** — AI Providers page — the provider accordion list, each row with its icon, capability badges (Text / Image / Embeddings / Copilot Docs), and an enable toggle, with the environment selector at the top
- [ ] **L33** — an expanded provider row — the inline credentials form with the API Key input and Save button

### `content/docs/platform/settings/audit-events.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L14** — Settings → Audit Events page — the results table with columns Date, Principal, Event Type and a per-row chevron; the left filter sidebar showing Principal, Search data, Event type, From date, and To date; the header showing the active "Filter by" summary
- [ ] **L31** — audit event detail panel — the slide-over showing the event type header with timestamp, the Principal and Event ID rows, and the Data key/value list

### `content/docs/platform/settings/components/api-connectors.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L38** — Settings → Components page with the API Connectors tab selected — a list of connector rows each showing "Title - name", an endpoint-count toggle, an enable/disable switch, the modified date, and the ellipsis menu; the "New Component" dropdown button open in the header
- [ ] **L46** — endpoint detail side panel open — method badge + endpoint name in the header, path beneath it, the API Connector name, a description, and a read-only YAML OpenAPI specification editor
- [ ] **L64** — the "New Component" dropdown expanded, showing the three API Connector routes — API Connector, Import Open API File, and API Connector from Docs URL
- [ ] **L92** — the manual Define Endpoints step with the Add/Edit Endpoint dialog open on its Form tab, showing Method, Path, Operation ID, Summary, Description, and the Parameters / Request Body / Responses sections
- [ ] **L122** — the AI route's Select Endpoints step — endpoints grouped by resource with checkboxes, the "N of M endpoints selected" counter, and the Select All / Deselect All buttons
- [ ] **L135** — the Edit API Connector wizard on its Define Endpoints step, hydrated from an existing connector's specification

### `content/docs/platform/settings/components/component-visibility.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L24** — Settings → Components page, Component Visibility tab — the search box and the component list with each row showing icon, title, description, and an on/off toggle, one row expanded to show its per-action and per-trigger switches

### `content/docs/platform/settings/components/custom-components.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L68** — Settings → Custom Components — list of custom component rows each showing title, name, version + language badges, an enable/disable switch, and the Import Component button in the header
- [ ] **L83** — custom component detail page — Monaco source editor with a JavaScript component open, the header showing the component title + language badge, Back arrow, and Save button

### `content/docs/platform/settings/connections.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L30** — Settings → Connections — the Organization Connections table with Name / Component / Environment / Created By / Last Modified columns, the Environment selector, and the New Connection button

### `content/docs/platform/settings/identity-providers.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L33** — Settings → Identity Providers — the Add Identity Provider dialog with Type set to OIDC, showing Issuer URI, Client ID, Client Secret, Scopes, Email Domains, Default Role, and the Auto-provision / Enforce SSO / Enabled checkboxes

### `content/docs/platform/settings/license.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L14** — Settings → License page — the .lic file drop zone with the Upload License button (no license yet), or the activated License Details card (status badge, Holder, Expires At, Allowed Jobs, Max Users, Features) with the Replace / Remove buttons

### `content/docs/platform/settings/mcp-server.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L21** — Settings → MCP Server page — the Require authentication toggle at the top, and the client tabs (Claude / Cursor / Windsurf / Other) below with a config snippet and its Refresh control

### `content/docs/platform/settings/notifications.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L17** — Settings → Notifications — the notifications table with Name / Events / Last Modified Date / Last Modified By / Actions columns and the New Notification button in the header

### `content/docs/platform/settings/oauth2-clients.mdx`  <sub>**Coming soon — whole page.** Skip until it ships.</sub>
- [ ] **L29** — Settings → OAuth2 Clients — the registered clients table showing the Name, Client ID, Issued, and Scopes columns with a delete (trash) icon at the end of each row
- [ ] **L55** — the "Are you absolutely sure?" delete confirmation dialog with its Cancel and Delete buttons

### `content/docs/platform/settings/users.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L18** — Settings → Users page — the users table with Email / Name / Role / Status columns, per-row edit and delete icons, and the Invite User button in the header
- [ ] **L44** — Invite User dialog — the Email field, the read-only Password field with its Regenerate button, and the Role dropdown
- [ ] **L123** — Workspace Members dialog — the members table with User / Role / Added columns, a role dropdown open on one row showing ADMIN / EDITOR / VIEWER, and the "Add user" row visible

### `content/docs/platform/settings/workspaces.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L18** — Settings → Workspaces — the workspace list with the New Workspace button and a row's ⋮ menu open showing Edit / Delete

### `content/docs/platform/your-account/active-sessions.mdx`
- [ ] **L14** — Your Account → Active sessions — the table with IP Address / User agent / Date columns and a per-row Invalidate button, plus the Refresh button in the header

### `content/docs/platform/your-account/appearance.mdx`
- [ ] **L11** — Your Account → Appearance — the Theme radio group with the Light, Dark, and System preview cards, System selected

### `content/docs/platform/your-account/profile.mdx`  <sub>Has a coming-soon section; some captures may not exist yet.</sub>
- [ ] **L13** — Your Account → Profile — the four stacked sections: Profile, Change password, Two-Factor Authentication, and Linked Accounts

## Self-hosted (7)

### `content/docs/platform/use-bytechef/self-hosted/installation/aws-ec2.md`
- [ ] **L49** — the ByteChef login screen reached at the EC2 instance's public URL, with the "Create account" link visible

### `content/docs/platform/use-bytechef/self-hosted/installation/aws-ecs.md`
- [ ] **L69** — the ECS service showing a healthy running task, or the first-login screen reached through the ALB URL

### `content/docs/platform/use-bytechef/self-hosted/installation/azure.md`
- [ ] **L55** — the ByteChef first-login screen reached at the Azure Container App ingress URL, with the "Create account" link visible

### `content/docs/platform/use-bytechef/self-hosted/installation/digitalocean.mdx`
- [ ] **L69** — the ByteChef first-login screen reached at the DigitalOcean App Platform / Droplet public URL, with the "Create account" link visible

### `content/docs/platform/use-bytechef/self-hosted/installation/google-cloud.md`
- [ ] **L45** — the ByteChef first-login screen reached at the Cloud Run service URL, with the "Create account" link visible

### `content/docs/platform/use-bytechef/self-hosted/installation/kubernetes.md`
- [ ] **L69** — kubectl get pods -n bytechef output showing the bytechef pod Running, or the first-login screen reached through the port-forward

### `content/docs/platform/use-bytechef/self-hosted/installation/local-docker.mdx`
- [ ] **L29** — the ByteChef login screen at http://localhost:8080/login on a fresh install, with the "Create account" link highlighted

## Developer guide (2)

### `content/docs/developer-guide/build-component/create-component-definition.mdx`
- [ ] **L50** — the newly built component (with its title and SVG icon) appearing in the workflow editor's right-hand component panel / node picker

### `content/docs/developer-guide/generate-component/customize-component.mdx`
- [ ] **L29** — the generated component listed with its icon under its chosen category in the workflow editor's component panel
