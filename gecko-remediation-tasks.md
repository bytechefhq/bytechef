# Gecko Findings — Remediation Task List

Companion to `gecko-security-report.md`. The 455 findings are consolidated into **27 fix tasks grouped by root cause**, so fixing one task usually clears many findings. Work top to bottom — tasks are ordered by priority and blast radius. Check off each as you go.

**Coverage:** the 27 tasks below account for all 455 findings (Critical 15, High 268, Medium 169, Low 3).

---

## Phase 0 — Critical (do first)

- [ ] **T1. Stop granting `ROLE_ADMIN` by default on signup & social login** _(3 findings — incl. 2× CVSS 9.4–9.8)_
  Self-registration and OAuth2/OIDC provisioning hand every new user admin authority. Default new users to a non-privileged role; require explicit admin assignment.
  Files: `platform-user/.../UserServiceImpl.java` (`registerUser`), `security/web/oauth2/CustomOidcUserService.java` (`defaultAuthority`), `security/web/oauth2/CustomOAuth2UserService.java` (`findOrCreateSocialUser(..., ADMIN)`).

- [ ] **T2. Authenticate all internal `/remote/**` microservice endpoints** _(~12 findings — 7× Critical)_
  Task-handler, trigger-state, trigger-lifecycle, trigger-definition, context, action-definition, principal-job, scheduler, and trigger-execution controllers are exposed with no auth; the Spring Cloud Config server is also open. Add a shared internal-auth filter (mTLS or service token) and remove `/remote/**` from permit-all; lock down `@EnableConfigServer`.
  Files: `security-config/.../SecurityConfiguration.java`, `RemoteTaskHandlerController`, `RemoteTriggerStateServiceController`, `RemotePrincipalJobFacadeController`, `RemoteTriggerDefinitionFacadeController`, `RemoteTriggerLifecycleFacadeController`, `RemoteContextServiceController`, `RemoteActionDefinitionFacadeController`, `RemoteTriggerSchedulerController`, `RemoteTriggerExecutionServiceController`, `RemoteWorkflowTestConfigurationServiceController`, `config-server-app`.

- [ ] **T3. Fix MCP API-key authentication bypass** _(3 findings — 9.1–9.4)_
  `authenticate()` allows omitting the `Authorization` header / fails to verify both secret-key and user API key. Require and validate both credentials; reject on missing/invalid.
  Files: `ManagementMcpServerApiKeyAuthenticationProvider.java`, `AutomationMcpServerApiKeyAuthenticationProvider.java`.

- [ ] **T4. Validate & parameterize tenant identifiers** _(4 findings — 2× 9.4 SQLi)_
  `CURRENT_TENANT_ID` header and base64 tenant tokens flow unsanitized into `SET search_path` / `TenantContext`. Whitelist tenant-ID charset, parameterize, and reject unknown tenants.
  Files: `RemoteMultiTenantFilter.java`, `JobResumeId.java` / `JobResumeFacadeImpl`, `WorkflowExecutionId.java`.

---

## Phase 1 — Code execution, injection, SSRF, webhook authenticity

- [ ] **T5. Lock down code-execution deploy endpoints** _(6 findings — 8.8/7.2 RCE)_
  Project/custom-component deploy endpoints execute uploaded JARs and polyglot scripts. Add strong authorization (admin/role), and sandbox or disable arbitrary code execution; validate artifact provenance.
  Files: `ProjectCodeWorkflowApiController.java`, `loader/automation/ProjectHandlerPolyglotEngine.java`, `web/rest/CustomComponentApiController.java`, `customcomponent/loader/ComponentHandlerPolyglotEngine.java`.

- [ ] **T6. Sandbox SpEL evaluation in the Condition task dispatcher** _(3 findings — 8.8/8.5 RCE)_
  `ConditionTaskUtils.resolveCase` evaluates user SpEL with the default context. Use a restricted `SimpleEvaluationContext` (no type/bean/constructor resolvers).
  File: `condition/util/ConditionTaskUtils.java`.

- [ ] **T7. Sandbox GraalVM polyglot & script-tool execution** _(3 findings — 8.5/7.8)_
  Script Tool, SkillsTool ZIP scripts, and FileSystemTools run unsandboxed. Constrain the GraalVM `Context` (no host access, filesystem allowlist, time/memory limits) and set a working-directory jail.
  Files: `script/engine/PolyglotEngine.java`, `ScriptToolDefinition`, `utils/cluster/AiAgentUtilsSkillsTool.java`, `AiAgentUtilsFileSystemTools.java`.

- [ ] **T8. Restrict class loading in message deserializers** _(2 findings — 8.5/7.9 RCE)_
  Redis & Kafka deserializers call `Class.forName()` on attacker-controlled type names. Replace with an explicit type allowlist / Jackson `PolymorphicTypeValidator`.
  Files: `redis/serializer/RedisMessageDeserializer.java`, `kafka/config/KafkaMessageBrokerConfiguration.java`.

- [ ] **T9. Escape shell commands in ClaudeCode actions** _(2 findings — 8.8 RCE)_
  Shell strings are built by concatenation. Use argument arrays / `ProcessBuilder` with no shell, or strictly escape inputs.
  Files: `code/action/ClaudeCodeChat.java`, `ClaudeCodeAddMCPAction`.

- [ ] **T10. Restrict DuckDB SQL to safe read-only operations** _(2 findings — 8.8 RCE / 7.8 LFI)_
  `MergeHelperSQLQueryAction` runs arbitrary SQL incl. DuckDB file functions. Restrict to SELECT, disable file/`read_*`/`COPY` functions.
  File: `mergehelper/util/MergeHelperUtils.java`.

- [ ] **T11. Parameterize SQL & filter-expression injections** _(6 findings — 8.8→7.0)_
  Replace string-concatenated queries/filters with parameter binding (and GraphQL variables for Monday).
  Files: `memory/action/VectorStoreChatMemoryDeleteAction.java`, `jdbc/operation/UpdateJdbcOperation.java`, `multi/pgvector/MultiTenantPgVectorStore.java`, `monday/action/MondayCreateBoardAction.java`, `snowflake/action/SnowflakeDeleteRowAction.java`.

- [ ] **T12. Harden XML handling (XXE + XML/TwiML injection)** _(4 findings)_
  Disable external entities/DTDs in `XmlUtils`; XML-encode values inserted into TwiML; validate XPath input.
  Files: `commons/util/XmlUtils.java`, `web/rest/TwimlController.java`.

- [ ] **T13. Add ReDoS / recursion guards** _(3 findings — 6.5 DoS)_
  Validate or time-bound user regexes; bound the recursive task-parent walk.
  Files: `helper/util/TextHelperUtils.java`, `mapper/action/DataMapperReplaceAllSpecifiedValuesAction.java`, `event/listener/TaskStartedApplicationEventListener.java`.

- [ ] **T14. JSON-escape user input in VoiceAgent settings** _(1 finding)_
  Build JSON with a serializer instead of string concatenation.
  File: `deepgram/action/DeepgramVoiceAgentAction.java`.

- [ ] **T15. Add SSRF defenses (URL allowlist / IP filtering)** _(~17 findings + open redirect)_
  Introduce a shared outbound-URL validator (scheme allowlist, DNS/IP resolution check blocking private/link-local ranges) and apply it to connection base-URI building, documentation fetch, job/task webhooks, JDBC/RabbitMQ/Cassandra hosts, OAuth tenant/region URLs, and the redirect validator.
  Files: `component/context/HttpClientExecutor.java`, `freshdesk/`, `pipeliner/`, `microsoft/commons/MicrosoftConnection.java`, `configuration/service/ApiConnectorAiServiceImpl.java`, `component/util/CustomActionUtils.java`, `execution/service/JobServiceImpl.java`, `event/listener/WebhookTaskStartedApplicationEventListener.java`, `client/action/GraphQlClientQueryAction.java`, `component/jdbc/DataSourceFactory.java`, `rabbitmq/util/RabbitMqUtils.java`, `cassandra/util/CassandraChatMemoryUtils.java`, `zoho/commons/ZohoConnection.java`, `rest/validator/RedirectValidator.java`, `guardrails/util/UrlDetectorUtils.java`.

- [ ] **T16. Implement webhook signature/origin verification across all triggers** _(~45 findings)_
  Add a per-provider `webhookValidate` that verifies the HMAC/signature header or secret token before processing. Build a shared helper and roll it out provider by provider.
  Providers: Stripe, Shopify, Slack, HubSpot, Twilio (incl. WhatsApp), Telegram, Pipedrive, Zeplin, Resend, Typeform, Linear, Box, Figma, WooCommerce, GitLab, GitHub, Attio, ClickUp, HeyGen, PagerDuty, Cal.com, MailerLite, Mailchimp, Infobip, Google Sheets/Calendar.

---

## Phase 2 — Systemic broken access control (IDOR) — the bulk (~370 findings)

> Recommended approach: **T17 first** (build the shared authorization primitive), then close out each domain (T18–T25). Each domain task is a focused sweep of related handlers using the new primitive.

- [x] **T17. Build a central resource-authorization layer** _(enabler for T18–T25)_
  Add an ownership/tenant/workspace authorization check (e.g. a `@PreAuthorize` SpEL helper or a `ResourceAccessChecker` invoked in facades) that every resource lookup/mutation must pass. Establishes the pattern reused below.
  **Done:** `ResourceOwnershipResolver` SPI (per-domain bean: resourceType → workspaceId/ownerUserId) + `PermissionService.hasResourceScope`/`isResourceOwner` (CE owner-isolation/fail-closed; EE workspace-scope/owner; both with tenant-admin bypass) + `ProjectWorkspacePermissionEvaluator` prefix tokens `'<Type>:ResourceScope'` and `'<Type>:ResourceOwner'`. Spec: `docs/superpowers/specs/2026-06-19-centralized-idor-authorization-design.md`; plan: `docs/superpowers/plans/2026-06-19-centralized-idor-authorization.md`.

- [x] **T18. Connections & credentials** _(~20 findings)_ — enforce workspace/owner scope on get/update/delete/tag and connected-user connection endpoints; stop returning credentials cross-workspace.
  Files: `web/rest/ConnectionApiController.java`, `connection/facade/ConnectionFacadeImpl.java`, `WorkspaceConnectionFacadeImpl`, `ConnectedUserConnectionFacadeImpl`, `ClusterElementDefinitionFacadeImpl`, `ConnectionTagApiController`, `ConnectionSearchAssetProvider`.
  **Done:** `ConnectionApiController` get/delete/update + `ConnectionTagApiController.updateConnectionTags` gated via `Connection:ResourceScope` (EE workspace-scope, CE owner-isolation honoring the PRIVATE model); workspace listing gated via `WorkspaceScope`. `ConnectionOwnershipResolver` resolves owning workspace (`workspace_connection`) + creator. `ClusterElement{Option,DynamicProperties,Field}GraphQlController` now gate `connectionId` (null-safe `Connection:ResourceScope` `CONNECTION_USE`) so a cross-workspace connection can't be used to fetch options/properties/fields. **Belongs to other tasks:** embedded `ConnectedUser*` connection paths → **T23**. They run under no platform `SecurityContext` (the caller is an API-key-authenticated embedded principal keyed by `externalUserId`/`X-Instance-Id`, not a platform user/workspace), so `Connection:ResourceScope` / owner-isolation cannot bind. The correct fix is T23's: **verify `externalUserId` (URL path) and `X-Instance-Id`/`x-instance-id` (headers) match the authenticated principal before resolving the connection/instance** — not a workspace-scope gate. The identical connection-loading pattern for non-cluster **action/trigger** node options (`ActionDefinitionFacadeImpl`/`TriggerDefinitionFacadeImpl` via `WorkflowNodeOption*`) → T22.
  **Split-out follow-up (T18a, separate slice):** per-row result filtering for `ConnectionSearchAssetProvider.search` / `findAll`-style listings — these return cross-workspace rows and need a membership-scoped query, not a single `hasPermission` gate. Not closed in this cycle.
  **Superseded in part (2026-08-10, resource-visibility branch).** The CE half of this remediation was **deliberately reversed**: CE now force-writes `WORKSPACE` visibility, so workspace members share connections by default and CE owner-isolation for connections is gone. This was a product decision, not an oversight — CE has no authorization boundary between workspace members in the first place, and the exposure is narrower than it looks (REST obfuscates `authorizationParameters`, nulls `parameters`, and no facade mutates credentials after creation, so sharing conveys *use*, not *read* or *write*). The EE half is not merely intact but tightened: **visibility is now a precondition of every `hasResourceScope` check**, closing a hole this task left open where a member holding `CONNECTION_EDIT` could operate by id on a colleague's PRIVATE connection that the list correctly hid. CE owner-isolation is untouched for every resource type that did not register a `ResourceVisibilityProvider` (API keys, signing keys, API clients). See `docs/superpowers/specs/2026-08-10-resource-visibility-design.md` §3.5 and §7; the guard is `PermissionServiceVisibilityTest`.

- [x] **T19. API keys & signing keys** _(~14 findings)_ — verify ownership/workspace on delete/update/get/rename across REST + GraphQL; gate `adminApiKeys`.
  Files: `ApiKeyFacadeImpl`, `ApiKeyGraphQlController`, `WorkspaceApiKeyFacadeImpl`, `WorkspaceApiKeyGraphQlController`, `SigningKeyApiController`, `SigningKeyServiceImpl`, `ApiClientServiceImpl`.
  **Done:** new `API_KEY_VIEW/CREATE/EDIT/DELETE` scopes (appended; `BuiltInRoleScopes` + `EnumOrdinalPinTest` updated). `WorkspaceApiKeyFacadeImpl` gated by `API_KEY_*` workspace scopes (`ApiKeyOwnershipResolver` resolves owning workspace + owner). Personal `ApiKeyGraphQlController` get/update/delete owner-isolated via `ApiKey:ResourceOwner`; `adminApiKeys` tenant-admin only. `SigningKeyServiceImpl` and `ApiClientServiceImpl` get/update/delete owner-isolated via `SigningKey:ResourceOwner` / `ApiClient:ResourceOwner` (EE-only; `SigningKeyOwnershipResolver` via `user_id`, `ApiClientOwnershipResolver` via `created_by`→userId). `ApiClientService.getApiClients()` now owner-filters its listing (createdBy; tenant admins see all).

- [x] **T20. MCP servers / projects / tools / components / integration-instance config** _(~45 findings)_ — add ownership/tenant checks to every MCP GraphQL/REST handler; stop returning `secretKey`; scope `findAll`-style queries.
  Files: `McpServerGraphQlController`/`McpServerServiceImpl`/`McpServerFacadeImpl`, `McpProjectGraphQlController`/`McpProjectServiceImpl`/`McpProjectFacadeImpl`, `McpProjectWorkflowGraphQlController`, `McpToolGraphQlController`/`McpToolServiceImpl`, `McpComponentGraphQlController`, `EmbeddedMcpServerGraphQlController`, `WorkspaceMcpServerGraphQlController`/`WorkspaceMcpServerFacadeImpl`, `ManagementMcpServerGraphQlController`, `McpIntegrationInstance*` controllers/facades, `EmbeddedMcpServerConfiguration`.
  **Done (all gates on impl/service tier, never controllers):**
  - `McpServerServiceImpl` — `getMcpServer(long)` `McpServer:ResourceRole` VIEWER, `update` EDITOR, `getMcpServerSecretKey(long)` `Tenant ADMIN` (no masking — admin-only per agreement); `McpServerFacadeImpl` deleteMcpServer/updateMcpServerTags EDITOR. `McpServerGraphQlController.secretKey` delegates to the admin-gated service.
  - `McpProjectServiceImpl`/`McpProjectFacadeImpl` — fetch VIEWER, create(server EDITOR)/delete/update/clone `McpProject:ResourceRole` EDITOR.
  - `McpProjectWorkflowServiceImpl`/`McpProjectWorkflowFacadeImpl` — fetch VIEWER, delete/update/updateParameters `McpProjectWorkflow:ResourceRole` EDITOR.
  - `McpComponentServiceImpl`/`McpServerFacadeImpl` — create(server EDITOR), update/deleteMcpComponent `McpComponent:ResourceRole` EDITOR, `getMcpComponents()` global findAll → `Tenant ADMIN`.
  - `McpToolServiceImpl` — create(component EDITOR), update/delete EDITOR, `fetchMcpTool` VIEWER, `getMcpTools()` global findAll → `Tenant ADMIN`.
  - `ManagementMcpServerGraphQlController` — secret-URL mint/rotate moved into new admin-gated `ManagementMcpServerService` (`Tenant ADMIN`).
  - `WorkspaceMcpServerFacadeImpl` — list VIEWER, create EDITOR, delete `McpServer:ResourceRole` EDITOR (prior slice).
  - New repository-based ownership resolvers (recursion-safe): McpServer, McpProject, McpProjectWorkflow, McpComponent, McpTool → owning workspace via `workspace_mcp_server`.
  - Runtime-shared reads (`getMcpComponent`, `getMcpServerMcpComponents`, `getMcpComponentMcpTools`) intentionally left ungated — the MCP runtime assembles tool lists through them with no user `SecurityContext`.
  **Residual (tracked):** global `mcpServers($type)`/`mcpProjects()` findAll stay ungated — a workspace page consumes them, so the correct fix is server-side **workspace-scoping** (add `workspaceId` arg + filter), an API change rather than a blanket gate. **Belongs to T23:** `EmbeddedMcpServerGraphQlController`, `EmbeddedMcpServerConfiguration`, `McpIntegrationInstance*` run under an embedded API-key principal (no platform workspace/user), so they need T23's `externalUserId`/`X-Instance-Id` match, not a workspace-scope gate.

- [ ] **T21. Knowledge bases / documents / chunks / tags / search** _(~20 findings)_ — workspace-scope all KB GraphQL/REST handlers, document upload, chunk mutations, tag writes, and search/load actions.
  Files: `KnowledgeBaseGraphQlController`, `WorkspaceKnowledgeBaseFacadeImpl`, `KnowledgeBaseDocument*` (controller/facade/service/tag), `KnowledgeBaseDocumentChunkFacadeImpl`, `KnowledgeBaseTagFacadeImpl`, `KnowledgeBaseServiceImpl`/`KnowledgeBaseVectorStore`, KB search asset providers.

- [x] **T22. Projects, workflows, deployments, data tables** _(~55 findings)_ — enforce ownership on project export/import/delete/publish/git, workflow delete/update/export/duplicate/validate/node-options/outputs/parameters/dynamic-properties, project-deployment endpoints, and all DataTable/Row/tag/webhook handlers; scope unscoped listing/search providers.
  Files: `ProjectApiController`/`ProjectFacadeImpl`/`ProjectGraphQlController`/`ProjectTagFacadeImpl`/`ProjectGitApiController`/`ProjectGitFacadeImpl`, `WorkflowApiController`/`ProjectWorkflowFacadeImpl`/`ProjectWorkflowGraphQlController`/`AutomationWorkflowProject*`, `WorkflowValidatorFacadeImpl`, `WorkflowNodeOption*`/`WorkflowNodeOutput*`/`WorkflowNodeParameter*`/`WorkflowNodeDynamicProperties*`/`WorkflowNodeDescription*`/`WorkflowNodeScript*`/`WorkflowTestConfiguration*`/`WorkflowTest*`, `ComponentConnectionGraphQlController`, `ProjectDeployment*` (controller/facade/service/tag/workflow), `IntegrationWorkflowFacadeImpl`, `DataTableGraphQlController`/`DataTableRowGraphQlController`/`DataTableServiceImpl`/`DataTableTagServiceImpl`/`DataTableWebhookGraphQlController`, `WorkflowSearchAssetProvider`/`AutomationSearchGraphQlController`/`SubflowDataSourceImpl`.
  **Done (all gates on impl/facade tier, never controllers):**
  - `ProjectFacadeImpl` (15 gates) / `ProjectWorkflowFacadeImpl` (10) — `ProjectScope` (PROJECT_DELETE/SETTINGS/WORKFLOW_*) on project-id ops; `@permissionService.hasWorkflowScope(#workflowId, …)` on String-UUID workflow ops; `WorkspaceRole` VIEWER/EDITOR on workspace-id list/create; global cross-workspace `getProjects()` → `Tenant ADMIN` (prior slices).
  - `ProjectGitFacadeImpl`/`ProjectGitApiController` (prior slice).
  - `ProjectTagFacadeImpl.updateProjectTags` → `ProjectScope WORKFLOW_EDIT`.
  - `WorkflowValidatorFacadeImpl.validateWorkflowById` → `hasWorkflowScope VIEW`; `validateWorkflow(definition)` stays ungated (caller-supplied content, no IDOR).
  - `WorkflowNodeParameterFacadeImpl` (8) — get*/missing-required → VIEW, update*/delete* param → EDIT, all `hasWorkflowScope`.
  - `WorkflowNodeOutputFacadeImpl`/`WorkflowNodeDynamicPropertiesFacadeImpl`/`WorkflowNodeDescriptionFacadeImpl`/`WorkflowNodeScriptFacadeImpl` (13) — reads → VIEW, script test-executions → EDIT. `@PreAuthorize` precedes `@Cacheable` so cache hits are authorized.
  - `WorkflowTestConfigurationFacadeImpl` (5) — editor writes → EDIT; `removeUnusedWorkflowTestConfigurationConnections` stays ungated (internal after-save listener).
  - `ProjectDeploymentFacadeImpl` (10) + new `ProjectDeploymentOwnershipResolver` — per-deployment-id ops via `ProjectDeployment:ResourceRole` VIEWER/EDITOR, create via `ProjectScope`, workspace list via `WorkspaceRole`. Embedded-only overloads left ungated (reached via self-invocation) so `ConnectedUserProjectFacadeImpl` is unaffected; negative tests pin this.
  - DataTable/Row/tag/webhook — `WorkspaceDataTableFacadeImpl` `DataTable:ResourceRole` (prior slice).
  **Documented exception (T22a, #16 done):** action/trigger node-option + cluster-element option/property/field + workflow test-config **connection-USE** checks stay at the controller because `ClusterElementDefinitionFacade`/option-loading is shared with worker/embedded execution (no user `SecurityContext`). The complementary workflow-edit gates are on the facades above.
  **Residuals (need workspace-scoped queries, an API/query change rather than a `@PreAuthorize` gate):** `getProjectTags()`/`getProjectDeploymentTags()` global tag findAll consumed by non-admin workspace pages; `WorkflowSearchAssetProvider.search` / `SubflowDataSourceImpl` / `AutomationSearchGraphQlController` return results from `getLatestProjectWorkflows()` etc. without filtering to the caller's workspace — fix by scoping the underlying query to the current workspace. **Belongs to T23:** `IntegrationWorkflowFacadeImpl` (embedded).

- [x] **T23. Integration instances & embedded `externalUserId`/`X-Instance-Id` trust** _(~35 findings)_ — verify that `externalUserId` (URL path) and `X-Instance-Id`/`x-instance-id` (headers) match the authenticated principal before resolving connections/instances/workflows/tools/actions; fix the tautological ownership checks.
  Files: `EmbeddedApiKeyAuthenticationProvider`/`Converter`, `ConnectionIdHelper.java`, `UnifiedApiFacadeImpl.java`, `ActionApiController`, `IntegrationApiController`/`IntegrationInstanceApiController`/`IntegrationInstanceConfigurationApiController`, `ConnectedUserIntegration(Instance)FacadeImpl`, `ConnectedUserProject(Workflow)*`, `IntegrationInstanceWorkflowApiController`, `McpIntegrationInstanceTool*`.
  **Done (primary IDOR vector closed):** New `SecurityUtils.checkCurrentUserLogin(expectedLogin)` (throws `AccessDeniedException` on mismatch/unauthenticated, unit-tested). Threat: in API-key mode the embedded principal is derived from the path `externalUserId` segment (`EmbeddedApiKeyAuthenticationConverter`), but in **JWT mode** the principal is the signed JWT subject — so a per-user JWT for user A could place user B's id in the path and act as B. Added `checkCurrentUserLogin(externalUserId)` as the first statement of **every** embedded public endpoint that takes `externalUserId` from the path (30 call sites across `IntegrationInstance`/`Integration`/`IntegrationInstanceWorkflow`/`AutomationWorkflowProject`/`McpIntegrationInstanceTool`/`McpIntegrationInstanceWorkflow`/`ConnectedUser`/`ConnectedUserProjectWorkflow` in configuration-public-rest, and `Action`/`Tool`/`Connection` in execution-public-rest). The `*Frontend*` variants already derive `externalUserId` via `fetchCurrentUserLogin()` and are untouched. The check is a no-op in API-key mode and closes the JWT cross-user gap.
  **X-Instance-Id covered by composition:** `ConnectionIdHelper.getConnectionId` already enforces `integrationInstance.connectedUserId == connectedUser.id` (AccessDenied otherwise); with the new guard ensuring `connectedUser` is the authenticated principal, the `X-Instance-Id` header can no longer reference another user's instance. The Unified API (`AccountingAccount*`/`CrmAccount*` controllers) derives `externalUserId` from `fetchCurrentUserLogin()` and was already safe.
  **Facade audit (done):** `ConnectedUserIntegrationFacadeImpl.deleteIntegrationInstance` deleted instance workflows unconditionally then gated the instance delete on a tautological `connectedUser.getExternalId() == externalUserId` (always true) — replaced with the real `integrationInstance.connectedUserId == resolved connectedUser.id` check before any delete. `UnifiedApiFacadeImpl.getComponentConnection` trusted a caller-supplied `integrationInstanceId` and used its connection without an ownership check — now verifies ownership in the explicit-id branch. `ConnectedUserIntegrationInstanceFacadeImpl` was already correct (`isOwnedByConnectedUser` real comparison). `ConnectedUserProjectFacadeImpl` externalUserId methods are scoped via `checkConnectedUserProject` (resolve the user's own project, look up workflows within it). Tests assert the denied paths perform no mutation (a positive-only test would pass against the tautological code).
  **Out of scope:** `IntegrationWorkflowFacadeImpl` takes no `externalUserId` — it manages admin integration-definition templates (workspace/admin surface), not connected-user data; the internal `embedded-configuration-rest-impl/ConnectionApiController` (workflow-builder surface, separate auth) is not part of the embedded public API.

- [~] **T24. Jobs, executions, logs, approval tasks/forms, notifications, eval, app-events** _(~40 findings)_ — add per-resource authorization to job/execution APIs, log-file GraphQL readers, approval task mutations and approval-form/token endpoints, notification CRUD, AI-agent eval (scenario/judge/result/run), and AppEvent CRUD; sign capability tokens (approval/trigger-form) with HMAC instead of plain base64.
  Files: `JobApiController`, `WorkflowExecutionApiController`/`ProjectWorkflowExecutionFacadeImpl`/`IntegrationWorkflowExecutionFacadeImpl`, `LogFileGraphQlController`/`EditorLogFileGraphQlController`, `ApprovalTaskGraphQlController`, `ApprovalFormFacadeImpl`/`ApprovalId.java`/`TriggerFormApiController`, `NotificationApiController`/`NotificationServiceImpl`, `AiAgentEvalGraphQlController` + eval services, `AppEventApiController`.
  **Done (all gates on impl/service tier):**
  - **Executions:** `ProjectWorkflowExecutionFacadeImpl` — `getWorkflowExecution`/`getWorkflowExecutionTaskExecution` → `Job:ResourceRole` VIEWER, `getWorkflowExecutions` → `WorkspaceRole` VIEWER. New `JobOwnershipResolver` (job → workflowId → owning project → workspace; fails closed for unknown/non-project jobs).
  - **Logs:** `LogFileStorageImpl` reads → `Job:ResourceRole` VIEWER, `deleteLogEntries` → EDITOR; `EditorLogFileStorageReaderImpl` reads → VIEWER (reuse `Job:ResourceRole`). Worker write path `storeLogEntry` stays ungated.
  - **Notifications:** `NotificationFacadeImpl` list/create/update + `NotificationServiceImpl.delete` → `Tenant ADMIN` (platform-settings config). Dispatcher `getNotifications(eventType)` (job-status event listener, no SecurityContext) stays ungated.
  - **App events:** `AppEventServiceImpl` create/delete/update → `Tenant ADMIN`; reads (`getAppEvent`/`getAppEvents`) stay ungated (called by `AppEventTrigger` during execution).
  - **AI-agent eval:** authoring writes on the 6 service impls (scenario/test/judge/scenario-judge/tool-simulation create/update/delete + eval-run create/delete, 17 methods) → `Tenant ADMIN`. Left ungated (no user `SecurityContext`): all reads + result/verdict writes + `updateAgentEvalRun` (async `AiAgentEvalRunExecutor`), and `deleteAgentEvalTestsByWorkflowId`/`deleteAgentJudgesByWorkflowId` (workflow pre-delete listener). Residual: the eval read queries are admin-tooling reads, not gated (gating them would break the executor's scenario/judge reads — would need a per-controller facade).
  **Approval / trigger-form HMAC signing — DONE** (spec Part A): new `ApprovalTokens` SPI + `ApprovalTokensImpl`/autoconfig (`platform-workflow-execution-token-service`), HMAC-SHA256 `v1.<exp>.<payload>.<sig>` over the opaque inner token (`JobResumeId`/`ApprovalId` Base64), key derived from `EncryptionKey` via `bytechef-approval-token-signed-v1` (or explicit `bytechef.approval.signed-token.secret`). Mint at `ActionContextImpl` (approval links + resume URL, via `ContextFactoryImpl`); verify at `ApprovalController`/`ApprovalFormFacadeImpl`/`JobResumeFacadeImpl` via `resolveInnerToken` (unwrap signed / reject tampered-or-expired / accept legacy while `required=false`). Stored `JOB_RESUME_ID` metadata stays unsigned so the stored-uuid match is unaffected. Migration: flip `bytechef.approval.signed-token.required=true` after the in-flight-token window. Signer unit-tested (round-trip, forged-field/forged-sig rejection, expiry, rotation).

- [~] **T25. Workspace/Git config, custom components, file entries, search providers** _(~25 findings)_ — membership-check workspace git-config and `getUserWorkspaces`; gate custom-component mutations; authenticate `/file-entries/{id}/content` or sign its URLs; scope all `SearchAssetProvider` `findAll()` calls.
  Files: `GitConfigurationApiController`, `WorkspaceApiController`, `CustomComponentGraphQlController`, `FileEntryController`/`WebhookAuthorizeHttpRequestContributor`, search asset providers, `IntegrationGraphQlController`/`IntegrationTagApiController`/`IntegrationInstanceConfigurationTagApiController`, `ConnectedUserGraphQlController`/`ConnectedUserApiController`, `AppEventTrigger`.
  **Done (gates on facade/service tier):**
  - `GitConfigurationFacadeImpl` — `fetchGitConfiguration`/`save` → `WorkspaceRole ADMIN` (per-workspace credential setting). The git-sync listener uses `getGitConfiguration` which self-invokes `fetchGitConfiguration` (proxy-bypassed) and stays ungated.
  - Custom components — `CustomComponentFacadeImpl` delete/getCustomComponents/getCustomComponentDefinition + `CustomComponentServiceImpl` enableCustomComponent/getCustomComponent → `hasAuthority(ROLE_ADMIN)`, matching the pre-existing `save` (code-upload) gate. Runtime registry uses the service `getCustomComponents()` (plural), left ungated.
  - Connected-user management — `ConnectedUserFacadeImpl` getConnectedUser/getConnectedUsers/enableConnectedUser → `Tenant ADMIN` (the `/internal` admin dashboard, not runtime-shared; the runtime/dispatch path uses `ConnectedUserService.getConnectedUser(externalUserId, environment)` directly).
  - Embedded integration tags — `IntegrationTagFacadeImpl` + `IntegrationInstanceConfigurationFacadeImpl` tag list/update → `Tenant ADMIN` (tenant-level embedded admin config, no runtime callers).
  **Already addressed (prior signing rollout):** `/file-entries/{id}/content` HMAC signing exists (`FileEntryTokens.toSignedToken` + `FileEntryController` accepts signed `v1.<exp>.<payload>.<sig>` and legacy ids while `bytechef.file-storage.signed-url.required=false`). Flipping `required=true` is the deployment-time hardening step.
  - **`ConnectedUserGraphQlController`** — DONE: routed `connectedUser(id)`/`connectedUsers` through new `Tenant ADMIN`-gated entity passthroughs `ConnectedUserFacade.getConnectedUserEntity`/`getConnectedUserEntities` (gate off the controller; runtime-shared `ConnectedUserService.getConnectedUser(long)` untouched).
  - **`WorkspaceApiController.getUserWorkspaces`** — DONE: EE `WorkspaceFacadeImpl` already enforced `Tenant ADMIN or User/SELF`; applied the same gate to the CE impl for consistency. Internal membership-check callers (AI Hub / asset-file / auto-memory) all pass `userService.getCurrentUser().getId()`, so they satisfy SELF.
  **Remaining (design-required — spec written, awaiting review):**
  - **Search providers** — 10 `SearchAssetProvider` impls each `findAll` cross-workspace. The SPI has no workspace param, so the fix is a `SearchContext{query, limit, accessibleWorkspaceIds}` change with the aggregator computing the caller's accessible workspaces once and each provider filtering. See `docs/superpowers/specs/2026-06-20-approval-token-signing-and-search-scoping-design.md` Part B.

---

## Phase 3 — Remaining hardening

- [ ] **T26. Auth & session hardening** _(~6 findings)_
  Rate-limit/lockout TOTP verification (`TwoFactorVerificationFilter`, `UserServiceImpl` `/api/mfa/verify`); constrain the `EMBED_INIT` postMessage origin before storing the JWT (`useWorkflowBuilder.ts`); restrict the `config()` SpEL function to an allowlist/prefix (`bytechef/evaluator/Config.java`); make the activation-email endpoint non-enumerable (`AccountController`).

- [ ] **T27. Output encoding, file-path safety & shared-state isolation** _(~15 findings)_
  Sanitize `Content-Disposition` headers (CRLF) in project/workflow/api-collection export (`ProjectApiController`, `AbstractWorkflowApiController`, `ApiCollectionApiController`); sanitize TipTap HTML (`PropertyMentionsInputEditor.tsx`); canonicalize/jail file paths in write/storage actions (`FilesystemWriteFileAction`, `FileDataStorageServiceImpl`, `JGitWorkflowOperations`, `AwsFileStorageServiceImpl`); bind chat-memory/agent state per user/tenant instead of sharing static instances (`InMemoryChatMemory`, `LangchainAgent`, `SpringAIAgent`/`CopilotConfiguration`, Chat-memory get/delete actions across Jdbc/Mongo/Cosmos/Cassandra).

---

### Notes
- Counts are approximate per-task tallies; exact per-finding detail (CVSS score, file, description) is in `gecko-security-report.md`, grouped by priority then type.
- Many IDOR tasks (T18–T25) share the same fix shape — once **T17** lands, each domain sweep is largely mechanical.
- Recommended order: T1–T4 (Critical) → T5–T16 (RCE/injection/SSRF/webhooks) → T17 then T18–T25 (IDOR sweeps) → T26–T27.
