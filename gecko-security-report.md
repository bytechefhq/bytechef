# Gecko Security Scan Report — bytechef/bytechef

**Source:** [Gecko Security scan](https://app.gecko.security/scan/0ef83db6-942b-4c37-b412-a1daf793255e)
**Repository:** `bytechef/bytechef` · branch `master` · commit `1634b21`
**Scan created:** Jun 04, 2026, 22:12 · **Duration:** 8h 34m · **Status:** 100% complete
**Open findings:** 455
**Report generated:** Jun 09, 2026

> Findings are grouped first by **priority** (CVSS severity band) and then by **vulnerability type** within each band. Each entry shows the CVSS score, title, source file, and Gecko's short description. Confidence is 10/10 unless noted. File paths are abbreviated to their last path segments; full paths are available in Gecko.

---

## Executive summary

This scan surfaced **455 open vulnerabilities** across the bytechef codebase. The overwhelming majority are **broken access control / IDOR** issues (392 of 455, ~86%) — REST and GraphQL handlers that accept caller-supplied numeric IDs, workspace IDs, `externalUserId` path variables, or webhook payloads and act on them without verifying ownership, tenant membership, or request authenticity. The remainder are higher-impact code-execution and injection classes.

### Priority breakdown

| Priority | CVSS band | Count |
|----------|-----------|-------|
| 🔴 Critical | 9.0 – 10.0 | 15 |
| 🟠 High | 7.0 – 8.9 | 268 |
| 🟡 Medium | 4.0 – 6.9 | 169 |
| 🟢 Low | 0.1 – 3.9 | 3 |
| **Total** | | **455** |

### Type breakdown

| Type | Full name | Count |
|------|-----------|-------|
| IDOR | Insecure Direct Object Reference / Broken Access Control | 392 |
| RCE | Remote Code Execution (incl. SpEL/polyglot eval, deserialization, ReDoS/DoS, XML/JSON injection) | 23 |
| SSRF | Server-Side Request Forgery (incl. XXE, open redirect) | 19 |
| SQLI | SQL / filter-expression Injection | 8 |
| LFI | Local File Inclusion / path traversal read | 5 |
| AFO | Arbitrary File Operation (write/overwrite) | 4 |
| XSS | Cross-Site Scripting / HTTP header (CRLF) injection | 4 |

### Priority × type matrix

| Type | Critical | High | Medium | Low | Total |
|------|:--:|:--:|:--:|:--:|:--:|
| IDOR | 13 | 227 | 151 | 1 | 392 |
| RCE  | 0  | 19  | 4   | 0 | 23 |
| SSRF | 0  | 9   | 9   | 1 | 19 |
| SQLI | 2  | 6   | 0   | 0 | 8 |
| LFI  | 0  | 3   | 2   | 0 | 5 |
| AFO  | 0  | 3   | 1   | 0 | 4 |
| XSS  | 0  | 1   | 2   | 1 | 4 |
| **Total** | **15** | **268** | **169** | **3** | **455** |

### Key themes

- **Systemic missing authorization.** Hundreds of REST/GraphQL handlers resolve resources with bare `findById(id)` / `deleteById(id)` and never check the caller owns the resource or belongs to the workspace/tenant. This affects connections (credentials), API keys, signing keys, MCP servers, knowledge bases, projects, workflows, data tables, approval tasks, and more.
- **Unauthenticated internal `/remote/**` endpoints.** Several microservice-to-microservice controllers (task handler, trigger lifecycle, context, action definition, scheduler, principal job facade) are exposed with no authentication, allowing direct task/job/trigger execution.
- **Privilege escalation to `ROLE_ADMIN`.** Self-registration and social-login (OAuth2/OIDC) provisioning paths grant admin authority to ordinary users.
- **Code execution surfaces.** Project/custom-component deploy endpoints execute uploaded JARs and polyglot scripts; SpEL and GraalVM evaluation run unsandboxed; Redis/Kafka deserializers load attacker-controlled classes.
- **Unverified inbound webhooks.** Dozens of third-party triggers (Stripe, Shopify, Slack, HubSpot, Twilio, Linear, Pipedrive, WooCommerce, etc.) accept payloads without signature/origin verification, enabling workflow spoofing.

---

## 🔴 Critical (9.0 – 10.0) — 15 findings

### IDOR / Broken Access Control (13)

- **9.8 · Privilege Escalation granting ROLE_ADMIN via Self-Registration** — `user/service/UserServiceImpl.java`
  The `registerUser()` method in `UserServiceImpl` unconditionally assigns `ROLE_ADMIN` (via `authorityRepository.findByName(AuthorityConstants.ADMIN).ifPresent(authorities::add)`) to every self-registered user.

- **9.8 · Broken Access Control allowing unauthenticated task execution in RemoteTaskHandlerController** — `security/config/SecurityConfiguration.java`
  `RemoteTaskHandlerController` exposes `POST /remote/task-handler/handle/{type}` that accepts a fully attacker-controlled `TaskExecution` body and routes it directly to `taskHandlerRegistry.getTaskHandler(type).handle(taskExecution)` without authentication.

- **9.4 · Broken Access Control granting ROLE_ADMIN via Social Login Provisioning** — `web/oauth2/CustomOidcUserService.java`
  In `CustomOidcUserService.loadUser`, `defaultAuthority` is initialized to `AuthorityConstants.ADMIN` ("ROLE_ADMIN") before the conditional block that would override it.

- **9.4 · IDOR enabling unauthenticated trigger state read/write via Remote Endpoints** — `apps/execution-app/build.gradle.kts`
  `RemoteTriggerStateServiceController` exposes `GET /remote/trigger-state-service/fetch-value/{workflowExecutionId}` and `PUT /remote/trigger-state-service/save/{workflowExecutionId}` without any authentication or authorization.

- **9.4 · IDOR allowing unauthenticated job creation via Remote Facade** — `rest/facade/RemotePrincipalJobFacadeController.java`
  `RemotePrincipalJobFacadeController` exposes `POST /remote/principal-job-facade/create-job` and `create-sync-job` with no authentication or authorization controls.

- **9.4 · Broken Access Control exposing internal trigger execution API unauthenticated** — `security/config/SecurityConfiguration.java`
  `RemoteTriggerDefinitionFacadeController` (`@RestController` at `/remote/trigger-definition-facade/**`) exposes all trigger lifecycle operations including `executeTrigger`, `executeWebhookEnable/Disable`, `executeListenerEnable`, and webhook validation.

- **9.4 · IDOR granting ROLE_ADMIN to any OAuth2 Social Login User** — `web/oauth2/CustomOAuth2UserService.java`
  In `CustomOAuth2UserService.loadUser`, every newly OAuth2-authenticated user is auto-provisioned with `ROLE_ADMIN` via a hardcoded `userService.findOrCreateSocialUser(..., true, AuthorityConstants.ADMIN)`.

- **9.4 · IDOR enabling full MCP tool access via missing Authorization check** — `web/authentication/ManagementMcpServerApiKeyAuthenticationProvider.java`
  `ManagementMcpServerApiKeyAuthenticationProvider.authenticate()` explicitly allows callers to omit the `Authorization` header entirely.

- **9.1 · Broken Authentication allowing MCP API access without valid credentials** — `web/authentication/AutomationMcpServerApiKeyAuthenticationProvider.java`
  `AutomationMcpServerApiKeyAuthenticationProvider.authenticate()` contains two compounding flaws that together constitute a complete authentication bypass.

- **9.1 · IDOR exposing cross-user credentials via Connection Lookup** — `connection/service/ConnectionServiceImpl.java`
  `RemoteTriggerDefinitionFacadeController` exposes all trigger execution and webhook management operations at `/remote/trigger-definition-facade/*` with zero authentication.

- **9.0 · IDOR enabling unauthenticated trigger control in RemoteTriggerLifecycleFacadeController** — `apps/execution-app/build.gradle.kts`
  Exposes `POST /remote/trigger-lifecycle-facade/execute-trigger-enable` and `/execute-trigger-disable` invoking internal trigger lifecycle operations with no authentication.

- **9.0 · IDOR enabling unauthenticated context read and write via Remote Service** — `apps/execution-app/build.gradle.kts`
  `RemoteContextServiceController` exposes four endpoints under `/remote/context-service/**` that read and write workflow execution context via `contextService.peek()` / `push()`.

- **9.0 · IDOR enabling unauthenticated action execution in RemoteActionDefinitionFacadeController** — `apps/worker-app/build.gradle.kts`
  The five REST endpoints under `/remote/action-definition-facade` on the worker-app microservice accept unauthenticated HTTP POST requests.

### SQL Injection (2)

- **9.4 · SQLI via Tenant ID injection in Job Resume Handler** — `workflow/execution/JobResumeId.java`
  `resumeJob` in `JobResumeFacadeImpl` accepts a base64-encoded `id` from an unauthenticated public endpoint and calls `TenantContext.callWithTenantId()` with the extracted `tenantId` without character validation.

- **9.4 · SQLI via Tenant Header Injection in Scheduler Job Store** — `web/filter/RemoteMultiTenantFilter.java`
  `RemoteMultiTenantFilter` reads a tenant ID from the `CURRENT_TENANT_ID` header with no validation, stores it in `TenantContext`, and the value is concatenated unsanitized into a PostgreSQL `SET search_path TO` statement.

---

## 🟠 High (7.0 – 8.9) — 268 findings

### Remote Code Execution (19)

- **8.8 · RCE via Unrestricted DuckDB SQL in sqlQuery Action** — `mergehelper/util/MergeHelperUtils.java`
  `MergeHelperSQLQueryAction.perform` accepts an arbitrary SQL string from a workflow parameter and executes it against an in-process DuckDB instance via `Statement.executeQuery`.

- **8.8 · RCE via arbitrary code execution in Project Deploy Endpoint** — `web/rest/ProjectCodeWorkflowApiController.java`
  `deployProject` at `POST /v1/projects/deploy` accepts a multipart file upload and executes its contents as code on the server with no authorization gate beyond basic session auth.

- **8.8 · RCE via unsandboxed SpEL injection in Condition Task Dispatcher** — `condition/util/ConditionTaskUtils.java`
  `ConditionTaskUtils.resolveCase` evaluates Spring Expression Language (SpEL) expressions stored in workflow task parameters.

- **8.8 · RCE via Java compilation in OpenAPI Specification Import** — `web/graphql/ApiConnectorGraphQlController.java`
  A user-supplied OpenAPI spec string drives server-side Java source generation, in-process compilation via `JavaCompiler`, and class loading via `URLClassLoader.newInstance()`.

- **8.8 · RCE via arbitrary code upload in Project Deploy Endpoint** — `web/rest/ProjectCodeWorkflowApiController.java`
  The project-deploy endpoint accepts a multipart file upload (`projectFile`) and immediately executes the file's contents as code on the server.

- **8.8 · RCE via arbitrary code execution in Project Deploy Endpoint** — `web/rest/ProjectCodeWorkflowApiController.java`
  The project-deploy endpoint accepts a user-supplied binary file (JAR or polyglot script) and executes it directly on the server with no role-based authorization check.

- **8.8 · RCE via Shell Injection in ClaudeCode Chat Action** — `code/action/ClaudeCodeChat.java`
  `ClaudeCodeAddMCPAction.perform` constructs a shell command string by directly concatenating user-supplied `label`, `url`, and auth header values via `StringBuilder.append()` with no shell escaping.

- **8.8 · RCE via Unrestricted SpEL Injection in Condition Task Dispatcher** — `condition/util/ConditionTaskUtils.java`
  `resolveCase` evaluates a user-provided SpEL expression via `expressionParser.parseExpression(expression).getValue(Boolean.class)` without a restricted `EvaluationContext`.

- **8.8 · RCE via unsandboxed polyglot eval in Project Deploy Handler** — `loader/automation/ProjectHandlerPolyglotEngine.java`
  The project-deploy endpoint accepts a user-uploaded script file (JS/Python/Ruby) and passes its raw contents to `polyglotContext.eval(languageId, script)` in `ProjectHandlerPolyglotEngine.load()`.

- **8.8 · RCE via Shell Injection in ClaudeCode Workflow Actions** — `code/action/ClaudeCodeChat.java`
  `ClaudeCodeChat` and `ClaudeCodeAddMCPAction` concatenate attacker-controlled input parameters directly into a shell command string.

- **8.5 · RCE via Unsandboxed Script Execution in SkillsTool Engine** — `web/graphql/AiSkillGraphQlController.java` *(conf 9/10)*
  `AiAgentUtilsSkillsTool.apply` extracts script files from user-uploaded ZIP archives and passes their raw content as `scriptContent` to `PolyglotEngine.execute()` → `polyglotContext.eval(...)`.

- **8.5 · RCE via attacker-controlled class loading in RedisMessageDeserializer** — `redis/serializer/RedisMessageDeserializer.java`
  `RedisMessageDeserializer.deserialize()` extracts the `type` field from a Redis stream message and passes it directly to `Class.forName(redisMessage.type())`.

- **8.5 · RCE via Unsandboxed GraalVM Execution in Script Tool** — `script/engine/PolyglotEngine.java`
  `ScriptToolDefinition.apply` passes user-supplied script code to `PolyglotEngine.execute`, which evaluates it via GraalVM `Context.eval()`.

- **8.5 · RCE via Unrestricted SpEL in Condition Task Dispatcher** — `condition/util/ConditionTaskUtils.java`
  `resolveCase` evaluates SpEL via `expressionParser.parseExpression(expr).getValue(Boolean.class)` (lines 71, 83) without a restricted `EvaluationContext`.

- **8.2 · RCE via forged Stripe webhook events in Trigger Handlers** — `stripe/trigger/StripeNewCustomerTrigger.java`
  `StripeNewCustomerTrigger` and `StripeNewInvoiceTrigger` register dynamic Stripe webhooks but implement no `webhookValidate` function.

- **8.2 · XML Injection enabling TwiML manipulation in TwimlController** — `web/rest/TwimlController.java`
  `serveTwiml` accepts unauthenticated query params `callSid`, `callRef`, `subWorkflowId` and inserts them without XML encoding into a `String.formatted()` call building a TwiML XML document.

- **7.9 · RCE via arbitrary class loading in Kafka Message Converter** — `kafka/config/KafkaMessageBrokerConfiguration.java`
  `convertFromInternal` reads a Kafka header `_type` and passes it directly to `Class.forName()`, then builds a Jackson `JavaType` and calls `objectMapper.readValue()` on the payload.

- **7.2 · RCE via arbitrary code execution in Custom Component Deploy** — `customcomponent/loader/ComponentHandlerPolyglotEngine.java` *(conf 9/10)*
  `deployCustomComponent` at `POST /v1/custom-components/deploy` accepts a multipart upload and executes its contents without validating the file's bytecode or script content.

- **7.2 · RCE via arbitrary code execution in Custom Component Deploy** — `web/rest/CustomComponentApiController.java`
  `deployCustomComponent` accepts an uploaded file and immediately executes it as a Java classloader artifact (JAR) or polyglot script (JS/Python/Ruby) on the server.

### SQL Injection (6)

- **8.8 · SQLI enabling arbitrary data deletion via Delete Conversation Action** — `memory/action/VectorStoreChatMemoryDeleteAction.java`
  The `deleteConversation` action builds a Spring AI filter expression by concatenating the user-supplied `conversationId`: `METADATA_CONVERSATION_ID + " == '" + conversationId + "'"`.

- **8.5 · SQLI enabling arbitrary deletion via Filter Expression Injection in DeleteConversation Action** — `memory/action/VectorStoreChatMemoryDeleteAction.java`
  `VectorStoreChatMemoryDeleteAction.perform` constructs a Spring AI filter expression by concatenating `conversationId` without sanitization.

- **8.3 · SQLI via unparameterized WHERE clause in UpdateJdbcOperation** — `jdbc/operation/UpdateJdbcOperation.java`
  The `condition` parameter of the JDBC `update` action is a free-form string concatenated directly into an `UPDATE` statement.

- **8.1 · SQLI enabling data read and write via Knowledge Base Search** — `multi/pgvector/MultiTenantPgVectorStore.java`
  `doSimilaritySearch` and `doDelete(Filter.Expression)` concatenate the output of `filterExpressionConverter.convertExpression()` directly into SQL strings without parameterization.

- **7.8 · SQLI enabling GraphQL injection in MondayCreateBoardAction** — `monday/action/MondayCreateBoardAction.java`
  `perform` builds a GraphQL mutation string via `String.formatted()`, interpolating user-supplied `BOARD_NAME` and `DESCRIPTION` without escaping or GraphQL variables.

- **7.0 · SQLI via Unsanitized WHERE Clause in Delete Row Action** — `snowflake/action/SnowflakeDeleteRowAction.java` *(conf 9/10)*
  `SnowflakeDeleteRowAction.perform` constructs a `DELETE FROM` statement with an unsanitized WHERE clause.

### Server-Side Request Forgery (9)

- **8.2 · SSRF via TwiML XML Injection in TwiML Endpoint** — `web/rest/TwimlController.java`
  `TwimlController.buildTwimlResponse` builds a TwiML XML response via `String.formatted()` with attacker-controlled `callSid`, `subWorkflowId`, `workflowExecutionId` inserted into XML attributes.

- **7.1 · SSRF via unvalidated companyFile URL in CreateCustomer Action** — `component/context/HttpClientExecutor.java`
  `getUserIdByUsername` in `XUtils.java` concatenates the caller-supplied `username` directly into the URL path `"/users/by/username/" + username` with no encoding or validation.

- **7.1 · SSRF via unvalidated webhook URL in Task Started Listener** — `event/listener/WebhookTaskStartedApplicationEventListener.java`
  `onApplicationEvent()` iterates webhook registrations stored in a `Job` and calls `rest.postForObject(webhook.url(), webhookEvent, String.class)`.

- **7.1 · SSRF enabling internal host reach via Freshdesk Domain Field** — `component/freshdesk/FreshdeskComponentHandler.java`
  The `domain` connection parameter in `FreshdeskComponentHandler.modifyConnection()` is concatenated directly into the HTTP base URI without validation or allowlist.

- **7.1 · SSRF exposing internal network via GraphQL Client Endpoint** — `component/context/HttpClientExecutor.java`
  `MattermostComponentHandler.modifyConnection` builds an HTTP base URI via `"http://" + connectionParameters.getRequiredString(DOMAIN) + "/api/v4"`.

- **7.1 · SSRF exposing internal network via Pipeliner Connection baseUri** — `component/pipeliner/PipelinerComponentHandler.java`
  `PipelinerComponentHandler.modifyConnection` builds `baseUri` by concatenating `SERVER_URL` with `SPACE_ID` for all outbound calls.

- **7.1 · SSRF enabling OAuth credential theft via tenantId injection** — `microsoft/commons/MicrosoftConnection.java`
  `createConnection` builds OAuth2 URLs by concatenating the user-supplied `tenantId` into `https://login.microsoftonline.com/<tenantId>/oauth2/v2.0/token` without validation.

- **7.1 · SSRF allowing internal network access via Documentation URL fetch** — `configuration/service/ApiConnectorAiServiceImpl.java`
  `generateFromDocumentation` and `startGenerateFromDocumentationPreview` forward an attacker-supplied `documentationUrl` directly to `HttpClient.send(URI.create(documentationUrl))` with no scheme/host/IP validation.

- **7.1 · SSRF exposing internal services via Mixpanel Custom Action** — `component/util/CustomActionUtils.java`
  `MicrosoftSharePointNewFileTrigger.poll()` builds a Microsoft Graph URL by interpolating the user-supplied `SITE_ID` into a format string.

### Local File Inclusion / Path Traversal (3)

- **8.8 · LFI allowing arbitrary file write via componentName path traversal** — `configuration/facade/ApiConnectorFacadeImpl.java`
  `importOpenApiSpecification` accepts a user-controlled `name` transformed by `convertComponentName()` and passed as `componentName` to `OpenApiGenerator.generate()`.

- **7.8 · LFI allowing server filesystem escape in FileSystemTools Setup** — `utils/cluster/AiAgentUtilsFileSystemTools.java`
  `apply()` builds `FileSystemTools` via `FileSystemTools.builder().build()` without supplying the `workingDirectory` parameter.

- **7.8 · LFI and AFO via DuckDB file functions in SQL Query Action** — `mergehelper/util/MergeHelperUtils.java`
  `MergeHelperSQLQueryAction` passes a user-authored SQL string directly to DuckDB `stmt.executeQuery()` without restricting it to SELECT-only operations.

### Arbitrary File Operation (3)

- **8.5 · AFO allowing arbitrary file write via Workflow Label Path Traversal** — `git/operations/JGitWorkflowOperations.java`
  `write()` builds a path `new File(repositoryDir, workflowResource.getFilename())` where `getFilename()` returns `workflow.getLabel() + "." + format`.

- **8.3 · AFO permitting arbitrary file overwrite via FilesystemWriteFileAction** — `filesystem/action/FilesystemWriteFileAction.java`
  `FilesystemWriteFileAction.perform` passes a workflow-supplied `filename` to `Files.copy(inputStream, Path.of(fileName), REPLACE_EXISTING)` with no canonicalization or base-directory restriction.

- **8.3 · AFO via path traversal in File Data Storage Service** — `storage/service/FileDataStorageServiceImpl.java`
  `put` builds a filename by concatenating `componentName`, `scope.ordinal()`, `scopeId`, and the user-controlled `key` parameter without sanitization.

### Cross-Site Scripting (1)

- **7.6 · XSS via unsanitized HTML in PropertyMentionsInputEditor getContent** — `components/property-mentions-input/PropertyMentionsInputEditor.tsx`
  `getContent` builds an HTML string from the server-stored `value` prop and passes it to TipTap's `editor.commands.setContent`.

### IDOR / Broken Access Control (227)

- **8.8 · IDOR enabling cross-tenant data access in McpIntegrationInstanceConfiguration GraphQL Controller** — `mcp/service/McpIntegrationInstanceConfigurationServiceImpl.java`
  Accepts caller-supplied numeric `id` values and passes them to repository operations without ownership/tenant checks.

- **8.8 · IDOR allowing deletion of any project via deleteSharedProject** — `web/graphql/ProjectGraphQlController.java`
  `deleteSharedProject` accepts an arbitrary `Long id` → `projectFacade.deleteSharedProject(id)` → bare `findById(id)` with no ownership check.

- **8.8 · IDOR enabling full ApprovalTask takeover via GraphQL** — `web/graphql/ApprovalTaskGraphQlController.java`
  `approvalTask(id)`, `deleteApprovalTask(id)`, `updateApprovalTask(...)` exposed with no resource-level authorization check.

- **8.8 · IDOR enabling cross-user data access in McpTool GraphQL Handler** — `web/graphql/McpToolGraphQlController.java`
  Queries (`mcpTool`, `mcpTools`, `mcpToolsByComponentId`) and mutations (`updateMcpTool`, `deleteMcpTool`) accept attacker IDs passed directly to the repository.

- **8.8 · IDOR enabling cross-user access in Project Deployment Endpoints** — `configuration/facade/ProjectDeploymentFacadeImpl.java` *(conf 9/10)*
  REST endpoints under `/project-deployments/{id}` pass a numeric `id` directly to facade get/delete/enable/update methods.

- **8.8 · IDOR enabling cross-user API client access in ApiClientApiController** — `configuration/service/ApiClientServiceImpl.java`
  CRUD endpoints for API clients perform no ownership or authorization check on the caller-supplied `id`.

- **8.8 · IDOR enabling cross-user skill access in AiSkill GraphQL Handler** — `skill/service/AiSkillServiceImpl.java`
  Any user can read/modify/delete any AI skill by supplying an arbitrary numeric `id`.

- **8.8 · IDOR enabling full resource takeover in AiAgentEval GraphQL Controller** — `web/graphql/AiAgentEvalGraphQlController.java`
  Every query/mutation accepting an `id` passes it to a bare `repository.findById(id)` with no ownership check.

- **8.8 · IDOR enabling cross-tenant integration manipulation via Internal API** — `configuration/facade/IntegrationFacadeImpl.java`
  `IntegrationApiController` exposes delete/update/publish/create-workflow/get-versions over the integration `{id}` path.

- **8.8 · IDOR enabling secret rotation by unprivileged users in MCP Server Config** — `web/graphql/ManagementMcpServerGraphQlController.java`
  `managementMcpServerUrl` (query) and `updateManagementMcpServerUrl` (mutation) lack any method-level authorization.

- **8.8 · IDOR allowing cross-workspace API Collection access and deletion** — `configuration/facade/ApiCollectionFacadeImpl.java`
  `getApiCollection`, `deleteApiCollection`, `updateApiCollection`, `updateApiCollectionTags`, `getOpenApiSpecification` fetch via bare `findById(id)`.

- **8.8 · IDOR enabling cross-workspace data access in ApiCollection Endpoints** — `web/rest/ApiCollectionApiController.java`
  `getApiCollection`/`deleteApiCollection`/`updateApiCollection` pass `Long id` to `findById(id)` with no ownership check.

- **8.8 · IDOR enabling full takeover of any MCP Server resource** — `web/graphql/McpServerGraphQlController.java`
  Every mutation/query passes attacker `id` to `getMcpServer(id)` / `deleteMcpServer(id)` with no ownership check.

- **8.8 · IDOR enabling cross-user data access in McpServer GraphQL Handlers** — `mcp/service/McpServerServiceImpl.java`
  `getMcpServer`, `update`, `updateTags`, `delete` accept arbitrary numeric IDs with no ownership verification.

- **8.8 · IDOR enabling cross-user MCP project access and deletion** — `web/graphql/McpProjectGraphQlController.java`
  All five `McpProject` operations lack any authorization check beyond session authentication.

- **8.8 · IDOR enabling cross-workspace data access in KnowledgeBase GraphQL Handler** — `web/graphql/KnowledgeBaseGraphQlController.java`
  `knowledgeBase`, `updateKnowledgeBase`, `deleteKnowledgeBase`, `searchKnowledgeBase` accept a raw `Long id` with no workspace check.

- **8.8 · Broken Access Control allowing privilege escalation in IntegrationInstanceConfigurationApiController** — `web/rest/IntegrationInstanceConfigurationApiController.java`
  Exposes CRUD, enable/disable, and job-trigger ops on the integration-instance-configurations path family.

- **8.8 · IDOR enabling unauthorized access in AiSkill GraphQL Handler** — `web/graphql/AiSkillGraphQlController.java`
  `aiSkill`, `aiSkills`, `aiSkillFilePaths`, `aiSkillFileContent`, `updateAiSkill`, `updateAiSkillContent`, `deleteAiSkill` accept an `id` with no check.

- **8.8 · IDOR enabling cross-user data access in McpProjectWorkflow Mutations** — `web/graphql/McpProjectWorkflowGraphQlController.java`
  Query/mutation operations accept arbitrary numeric IDs with no ownership check.

- **8.8 · IDOR enabling cross-workspace data access in DataTableRow GraphQL Handler** — `configuration/service/DataTableServiceImpl.java`
  Resolves the target table via `getBaseNameById(input.tableId())` → bare `findById(id)` against the global `DataTable`.

- **8.8 · IDOR enabling full skill resource takeover via GraphQL API** — `skill/facade/AiSkillFacadeImpl.java`
  `deleteAiSkill`, `updateAiSkillContent`, `getAiSkillFileContent` accept a plain `long id` with no authorization.

- **8.8 · IDOR enabling unauthorized access in McpComponent GraphQL Operations** — `web/graphql/McpComponentGraphQlController.java`
  `mcpComponent`, `deleteMcpComponent`, `updateMcpComponentWithTools` pass a caller `id` directly to the repository.

- **8.8 · IDOR enabling cross-user task access in ApprovalTask GraphQL Handler** — `web/graphql/ApprovalTaskGraphQlController.java`
  `deleteApprovalTask`, `approvalTask`, `updateApprovalTask` pass a caller `id` to the service with no ownership check.

- **8.8 · IDOR enabling full skill takeover in AiSkill GraphQL API** — `web/graphql/AiSkillGraphQlController.java`
  Any user can read/modify/delete any other user's AI skills via an arbitrary numeric `id`.

- **8.8 · IDOR enabling cross-workspace DDL in Table Mutation Handlers** — `web/graphql/DataTableGraphQlController.java`
  Six table-mutation handlers (`dropDataTable`, `duplicateDataTable`, `renameDataTable`, `addDataTableColumn`, `removeDataTableColumn`, `renameDataTableColumn`) resolve `tableId` via `getBaseNameById`.

- **8.8 · IDOR enabling cross-user API client access in ApiClientApiController** — `configuration/service/ApiClientServiceImpl.java`
  IDOR in `ApiClientApiController` — no ownership check on client operations.

- **8.7 · Missing Twilio Signature Validation enabling workflow manipulation via Status Callback** — `web/rest/TwilioCallbackController.java`
  Three Twilio webhook endpoints accept unauthenticated POSTs without verifying the `X-Twilio-Signature` HMAC header.

- **8.7 · Missing Rate Limit enabling TOTP brute-force in MFA Verification Filter** — `web/filter/TwoFactorVerificationFilter.java`
  `POST /api/mfa/verify` enforces no rate limit, attempt counter, or lockout on TOTP submissions.

- **8.7 · IDOR enabling unauthenticated scheduler manipulation via Remote Trigger Controller** — `apps/scheduler-app/build.gradle.kts`
  `RemoteTriggerSchedulerController` exposes six unauthenticated POST endpoints under `/remote/trigger-scheduler/**` to cancel/schedule Quartz jobs.

- **8.7 · IDOR enabling cross-tenant data access via Webhook Executor** — `web/rest/WebhookTriggerController.java`
  The unauthenticated `GET/POST /webhooks/{id}` decodes `{id}` as a base64 `WorkflowExecutionId` embedding a `tenantId`.

- **8.6 · IDOR allowing workflow spoofing in Shopify Webhook Handler** — `shopify/trigger/ShopifyNewPaidOrderTrigger.java`
  `webhookRequest` returns `body.getContent()` without verifying the Shopify HMAC-SHA256 signature.

- **8.6 · Missing Slack Signature Verification enabling unauthorized workflow triggering** — `slack/trigger/SlackAnyEventTrigger.java`
  Accepts all POST bodies and passes `content.get("event")` into workflow execution without verifying `X-Slack-Signature`.

- **8.6 · IDOR enabling unauthenticated trigger execution manipulation in RemoteTriggerExecutionServiceController** — `apps/execution-app/build.gradle.kts` *(conf 9/10)*
  Registers three endpoints under `/remote/trigger-execution-service/` allowing create/read/update of `TriggerExecution` records.

- **8.5 · IDOR enabling cross-user data access in KnowledgeBase Document Handlers** — `knowledgebase/service/KnowledgeBaseDocumentTagServiceImpl.java`
  `updateKnowledgeBaseDocumentTags`, `knowledgeBaseDocument`, `deleteKnowledgeBaseDocument` pass a caller document ID to the repository with no ownership check.

- **8.5 · IDOR allowing cross-user workflow access via externalUserId** — `web/rest/ConnectedUserProjectWorkflowApiController.java`
  `/{externalUserId}/automation/workflows` (GET/POST/PUT/DELETE/publish/enable) trusts `externalUserId` without verifying it matches the authenticated identity.

- **8.3 · IDOR enabling cross-workspace MCP Server deletion via GraphQL** — `web/graphql/WorkspaceMcpServerGraphQlController.java`
  `deleteWorkspaceMcpServer` accepts an attacker `mcpServerId` and deletes the server and components with no workspace check.

- **8.3 · IDOR enabling cross-user signing key access in SigningKeyApiController** — `security/service/SigningKeyServiceImpl.java`
  `getSigningKey`/`deleteSigningKey`/`updateSigningKey` pass a numeric `id` to `findById`/`deleteById` with no ownership check.

- **8.3 · IDOR enabling cross-user deletion in MCP Server Mutations** — `web/graphql/McpServerGraphQlController.java`
  `deleteMcpServer`, `deleteMcpComponent`, `updateMcpComponentWithTools` pass a caller `id` to `deleteById`/`update`.

- **8.3 · IDOR enabling deletion and update of any API key** — `web/graphql/WorkspaceApiKeyGraphQlController.java`
  `deleteWorkspaceApiKey`/`updateWorkspaceApiKey` accept a caller `apiKeyId` with no workspace authorization.

- **8.3 · IDOR enabling unauthorized modification of API Connectors** — `web/graphql/ApiConnectorGraphQlController.java`
  All mutations (`createApiConnector`, `deleteApiConnector`, `enableApiConnector`, `updateApiConnector`, `importOpenApiSpecification`, `generateFromDocumentation`) lack a role annotation.

- **8.3 · IDOR enabling cross-user endpoint deletion in ApiCollectionEndpoint Service** — `configuration/service/ApiCollectionEndpointServiceImpl.java`
  `delete`/`update` operate on a raw numeric `id` with no ownership verification.

- **8.3 · IDOR enabling unauthorized task modification and deletion via Mutations** — `web/graphql/ApprovalTaskGraphQlController.java`
  `deleteApprovalTask(id)`/`updateApprovalTask` operate on any `ApprovalTask` without ownership verification.

- **8.3 · IDOR enabling cross-user API Key deletion and modification** — `security/facade/ApiKeyFacadeImpl.java`
  `delete`/`update`/`getApiKey` act on a caller `id` without verifying ownership.

- **8.3 · IDOR allowing cross-user signing key deletion and access** — `web/rest/SigningKeyApiController.java`
  `deleteSigningKey`/`getSigningKey`/`updateSigningKey` pass a bare numeric `id` to the service with no check.

- **8.3 · IDOR allowing unauthorized mutation of any MCP Server** — `mcp/facade/McpServerFacadeImpl.java`
  `deleteMcpServer`, `updateMcpServer`, `updateMcpServerTags`, `updateMcpServerUrl` pass a caller `id` directly to service/repository.

- **8.3 · IDOR enabling project deletion via missing ownership check** — `configuration/facade/ProjectFacadeImpl.java`
  `deleteProject` (DELETE …/projects/{id}) deletes a project with no ownership verification.

- **8.3 · IDOR enabling cross-user mutation in MCP Integration Configuration** — `web/graphql/McpIntegrationInstanceConfigurationGraphQlController.java`
  `delete`/`update`/`updateVersion` mutations operate on a caller numeric `id` with no ownership check.

- **8.3 · IDOR allowing cross-user workflow deletion via Workflow Delete Endpoint** — `web/rest/WorkflowApiController.java`
  Any user can delete any workflow via an arbitrary `workflowId` to `DELETE /workflows/{id}`.

- **8.3 · IDOR allowing integration admin ops via Connected User** — `web/rest/IntegrationApiController.java`
  Operator-facing admin integration endpoints are reachable by any authenticated connected user.

- **8.3 · IDOR enabling cross-user instance manipulation in ConnectedUserIntegrationInstanceFacade** — `configuration/facade/ConnectedUserIntegrationInstanceFacadeImpl.java`
  `enable`/`disable`/`updateIntegrationInstanceWorkflow` perform a tautological ownership check that never verifies the instance `id`.

- **8.3 · IDOR enabling cross-user job read and control via Job API** — `web/rest/JobApiController.java`
  `getJob`, `getJobsPage`, `getLatestJob`, `restartJob`, `stopJob` at `/api/platform/internal/jobs` lack per-resource authorization.

- **8.3 · IDOR enabling cross-workspace data access in Project Deployment Endpoints** — `configuration/service/ProjectDeploymentServiceImpl.java`
  Single-resource deployment endpoints call `getProjectDeployment(id)` → bare `findById` with no workspace scope.

- **8.3 · IDOR enabling cross-user API Key deletion and rename in WorkspaceApiKeyGraphQlController** — `configuration/facade/WorkspaceApiKeyFacadeImpl.java`
  `deleteWorkspaceApiKey(apiKeyId)`/`updateWorkspaceApiKey(apiKeyId, name)` accept a caller `apiKeyId` with no workspace check.

- **8.3 · IDOR enabling cross-user data access in Workflow Test Configuration** — `web/rest/WorkflowTestConfigurationApiController.java`
  `workflowId` flows through the facade/service to a query predicate with no ownership validation.

- **8.3 · IDOR enabling deletion of any AppEvent in AppEventApiController** — `web/rest/AppEventApiController.java`
  DELETE/GET/PUT `/api/embedded/internal/app-events/{id}` accept a caller `Long id`.

- **8.3 · IDOR enabling cross-user data access in McpProject GraphQL Handler** — `web/graphql/McpProjectGraphQlController.java`
  `mcpProject`, `mcpProjects`, `mcpProjectsByServerId`, `deleteMcpProject`, `updateMcpProject` accept attacker IDs.

- **8.3 · IDOR allowing cross-workspace data access in DataTableRow Operations** — `configuration/service/DataTableServiceImpl.java`
  Every operation (`dataTableRows`, `insertDataTableRow`, `deleteDataTableRow`, `exportDataTableCsv`, …) resolves `tableId` with no scope.

- **8.3 · IDOR enabling deletion and modification of any McpProject** — `mcp/facade/McpProjectFacadeImpl.java`
  `deleteMcpProject`/`updateMcpProject` pass a numeric ID directly to the facade with no ownership check.

- **8.3 · IDOR enabling signing key deletion without ownership check** — `web/rest/SigningKeyApiController.java`
  `SigningKeyApiController` operates on the matching row using a plain numeric `id` with no ownership verification.

- **8.3 · IDOR enabling workflow deletion and overwrite via Workflow API** — `configuration/facade/ProjectWorkflowFacadeImpl.java`
  `deleteWorkflow`, `updateWorkflow`, `exportSharedWorkflow`, `deleteSharedWorkflow`, `duplicateWorkflow` never verify ownership/workspace membership.

- **8.3 · IDOR enabling cross-user job access in Job Execution API** — `web/rest/JobApiController.java`
  `GET …/{id}`, `PUT …/{id}/restart`, `PUT …/{id}/stop` accept attacker job IDs with no check.

- **8.3 · IDOR enabling cross-workspace deletion and modification in KnowledgeBase Mutations** — `web/graphql/KnowledgeBaseGraphQlController.java`
  `deleteKnowledgeBase`/`updateKnowledgeBase` operate on a raw `id` with no workspace ownership check.

- **8.3 · IDOR enabling cross-user deletion in McpProject Mutation Handler** — `mcp/facade/McpProjectFacadeImpl.java`
  `deleteMcpProject`/`updateMcpProject` operate on a raw `id` with no scope verification.

- **8.3 · IDOR enabling unauthorized delete and disable of Custom Components** — `web/graphql/CustomComponentGraphQlController.java`
  `deleteCustomComponent`/`enableCustomComponent` lack any role-based authorization check.

- **8.3 · IDOR enabling unauthorized deletion of any Connection resource** — `web/rest/ConnectionApiController.java`
  `DELETE /connections/{id}` and `PATCH /connections/{id}` pass a caller numeric `id` directly.

- **8.3 · IDOR enabling cross-user API key access in ApiKeyGraphQlController** — `security/facade/ApiKeyFacadeImpl.java`
  `deleteApiKey`/`updateApiKey`/`apiKey` pass a bare numeric `id` to `deleteById`/`findById`/`save`.

- **8.3 · IDOR enabling arbitrary workflow definition overwrite** — `configuration/facade/IntegrationWorkflowFacadeImpl.java`
  `updateWorkflow` overwrites a workflow definition for an arbitrary `workflowId` with no integration ownership check.

- **8.3 · IDOR enabling unauthorized project management via GraphQL Mutations** — `web/graphql/AutomationWorkflowProjectGraphQlController.java`
  All project management mutations are exposed at `/graphql` with no method-level authorization.

- **8.3 · IDOR enabling cross-user project deletion and tampering via GraphQL** — `configuration/facade/AutomationWorkflowProjectFacadeImpl.java`
  Mutations accept caller project/workflow IDs with no ownership check.

- **8.3 · IDOR enabling endpoint deletion across workspaces in ApiCollectionEndpoint** — `web/rest/ApiCollectionEndpointApiController.java`
  DELETE/PUT `/api-collection-endpoints/{id}` accept a numeric ID with no workspace check.

- **8.3 · IDOR enabling cross-user key deletion in SigningKey Endpoint** — `security/service/SigningKeyServiceImpl.java`
  `deleteSigningKey`/`getSigningKey`/`updateSigningKey` pass `{id}` directly to the service with no check.

- **8.3 · IDOR enabling deletion of any MCP Server in deleteEmbeddedMcpServer** — `web/graphql/EmbeddedMcpServerGraphQlController.java`
  `deleteEmbeddedMcpServer` passes a caller `mcpServerId` to the facade with no ownership/tenant check.

- **8.3 · IDOR enabling cross-user data access in IntegrationInstanceConfiguration API** — `configuration/service/IntegrationInstanceConfigurationServiceImpl.java`
  Every operation accepts a user-supplied `id` path variable with no ownership check.

- **8.3 · IDOR enabling cross-user workflow execution in NodeTestOutput Facade** — `configuration/facade/WorkflowNodeTestOutputFacadeImpl.java`
  `saveWorkflowNodeTestOutput`/`saveClusterElementTestOutput` accept an arbitrary `workflowId`.

- **8.3 · IDOR allowing cross-user log access in LogFileGraphQlController** — `web/graphql/LogFileGraphQlController.java`
  `jobFileLogs`, `taskExecutionFileLogs`, `jobFileLogsExist`, `deleteJobFileLogs` read/delete logs by caller `jobId` with no check.

- **8.3 · IDOR enabling cross-workspace MCP server deletion via GraphQL** — `mcp/facade/WorkspaceMcpServerFacadeImpl.java`
  `deleteWorkspaceMcpServer` forwards `mcpServerId` to `mcpServerService.delete()` with no ownership check.

- **8.3 · IDOR enabling cross-user connection deletion via Connection API** — `connection/facade/ConnectionFacadeImpl.java`
  `deleteConnection` (DELETE …/{id}) passes a numeric `id` to `connectionRepository.deleteById(id)` with no check.

- **8.3 · IDOR enabling cross-workspace MCP server access and deletion** — `mcp/facade/WorkspaceMcpServerFacadeImpl.java`
  `getWorkspaceMcpServers`, `createWorkspaceMcpServer`, `deleteWorkspaceMcpServer` accept `workspaceId`/`mcpServerId` with no membership check.

- **8.3 · IDOR allowing deletion of any shared workflow template** — `web/graphql/ProjectWorkflowGraphQlController.java`
  `deleteSharedWorkflow` passes a `workflowId` to the facade with no ownership check.

- **8.3 · IDOR enabling cross-user log read and delete in LogFileGraphQlController** — `web/graphql/LogFileGraphQlController.java`
  Four log operations pass a caller `jobId` to storage operations with no ownership check.

- **8.3 · IDOR enabling cross-user signing key access via ID endpoint** — `security/service/SigningKeyServiceImpl.java`
  `getSigningKey`/`delete`/`update` use only the caller numeric `id`.

- **8.3 · IDOR enabling cross-workspace deletion in ApiCollectionEndpoint Handler** — `web/rest/ApiCollectionEndpointApiController.java`
  `deleteApiCollectionEndpoint`/`updateApiCollectionEndpoint` operate on `Long id` with no workspace check.

- **8.3 · IDOR enabling cross-workspace MCP Server deletion via GraphQL** — `mcp/facade/WorkspaceMcpServerFacadeImpl.java`
  `deleteWorkspaceMcpServer` allows any user to permanently delete an MCP server they do not own.

- **8.2 · Missing Signature Verification enabling webhook injection in HubspotNewContactTrigger** — `hubspot/trigger/HubspotNewContactTrigger.java`
  Accepts any inbound POST body without verifying the request originates from HubSpot.

- **8.2 · IDOR allowing workflow spoofing via unverified Shopify Webhook** — `shopify/trigger/ShopifyNewOrderTrigger.java`
  All three Shopify triggers accept POSTs without verifying the `X-Shopify-Hmac-SHA256` signature.

- **8.2 · Missing Webhook Signature Verification in WooCommerce Order Trigger** — `woocommerce/util/WoocommerceUtils.java`
  `WoocommerceNewOrderTrigger` returns `body.getContent()` without verifying `X-WC-Webhook-Signature`.

- **8.2 · Missing Webhook Signature Verification allowing event injection in HubSpot Triggers** — `hubspot/trigger/HubspotNewContactTrigger.java`
  Three HubSpot DYNAMIC_WEBHOOK triggers process payloads without verifying `X-HubSpot-Signature`.

- **8.2 · IDOR enabling workflow hijack via Telegram Webhook Spoofing** — `telegram/trigger/TelegramNewMessageTrigger.java`
  Registers a Telegram webhook without a `secret_token` and registers no `webhookValidate`.

- **8.2 · Missing Webhook Origin Validation in PipedriveNewDealTrigger** — `pipedrive/trigger/PipedriveNewDealTrigger.java`
  DYNAMIC_WEBHOOK trigger with no mechanism to verify the request originates from Pipedrive.

- **8.2 · IDOR enabling workflow injection via unverified Pipedrive Webhook** — `pipedrive/trigger/PipedriveNewPersonTrigger.java`
  DYNAMIC_WEBHOOK trigger with no `webhookValidate` function.

- **8.2 · Missing Webhook Signature Verification in Zeplin Project Note Trigger** — `zeplin/trigger/ZeplinProjectNoteTrigger.java`
  `webhookValidate` returns `204` unconditionally without inspecting `Zeplin-Signature`/`Zeplin-Delivery-Timestamp`.

- **8.2 · Spoofed Webhook Triggering Workflow via Unverified Resend Events** — `resend/trigger/ResendEmailDeliveredTrigger.java`
  Returns `body.getContent()` verbatim without verifying the request originated from Resend.

- **8.2 · Missing Webhook Signature Verification in Typeform Submission Trigger** — `typeform/trigger/TypeformNewSubmissionTrigger.java`
  Returns `body.getContent()` without verifying the POST originates from Typeform.

- **8.2 · Missing Webhook Origin Verification Allows Workflow Injection in PipedriveNewOrganizationTrigger** — `pipedrive/trigger/PipedriveNewOrganizationTrigger.java`
  Accepts all inbound POSTs without verifying they originate from Pipedrive.

- **8.2 · Missing Webhook Signature Verification in Stripe Invoice Trigger** — `stripe/trigger/StripeNewInvoiceTrigger.java`
  Accepts Stripe POSTs without verifying the `Stripe-Signature` HMAC header.

- **8.2 · IDOR enabling unauthorized workflow job creation via Remote Facade** — `apps/execution-app/build.gradle.kts` *(conf 9/10)*
  `RemotePrincipalJobFacadeController` exposes two unauthenticated POST endpoints for job creation.

- **8.2 · IDOR enabling unauthorized workflow execution via Twilio Status Callback** — `web/rest/TwilioCallbackController.java`
  `POST /webhooks/twilio/status` is permit-all and accepts attacker `CallSid`/`CallStatus` with no signature verification.

- **8.2 · Webhook Spoofing enabling workflow injection in Linear Triggers** — `linear/trigger/LinearUpdatedIssueTrigger.java`
  All three Linear triggers omit a `webhookValidate` function entirely.

- **8.1 · IDOR enabling cross-user project data access in Project Export Handler** — `web/rest/ProjectApiController.java`
  `exportProject` (and `deleteProject`, `duplicateProject`, `getProject`, `publishProject`, `updateProject`) pass `@PathVariable Long id` with no ownership verification.

- **8.1 · IDOR enabling cross-user project access in ProjectGitApiController** — `web/rest/ProjectGitApiController.java`
  All five endpoints use a numeric `Long id` for DB/git operations with no ownership/membership verification.

- **8.1 · IDOR enabling deletion of any MCP Server by authenticated user** — `web/graphql/EmbeddedMcpServerGraphQlController.java`
  IDOR in `deleteEmbeddedMcpServer`.

- **8.1 · IDOR enabling cross-user log access in LogFileGraphQlController** — `web/graphql/LogFileGraphQlController.java`
  Log queries/mutation pass a caller `jobId` directly to `readLogEntriesByJobId`/`deleteLogEntries`.

- **8.1 · IDOR enabling cross-user data access via Integration Instance Header** — `unified/facade/UnifiedApiFacadeImpl.java`
  `getComponentConnection` branches on whether `integrationInstanceId` is null, enabling cross-user access.

- **8.1 · IDOR enabling cross-workspace connection deletion in Connection Handler** — `web/rest/ConnectionApiController.java` *(conf 9/10)*
  `DELETE /connections/{id}` deletes by numeric ID with no workspace ownership check.

- **8.1 · IDOR enabling cross-user document deletion in KnowledgeBase Facade** — `knowledgebase/facade/KnowledgeBaseDocumentFacadeImpl.java`
  `deleteKnowledgeBaseDocument(Long id)`/`updateKnowledgeBaseDocumentTags(...)` perform no ownership/workspace check.

- **8.1 · IDOR enabling deletion and mutation of any Connection** — `web/rest/ConnectionApiController.java`
  `DELETE`/`PATCH /connections/{id}` act on any connection record with no ownership check.

- **8.1 · IDOR allowing unauthorized AppEvent management via Embedded API** — `web/rest/AppEventApiController.java`
  Full CRUD on `AppEvent` (platform event schema) at `/api/embedded/internal/app-events`.

- **8.1 · IDOR enabling cross-user data access in WorkflowTestConfiguration API** — `configuration/facade/WorkflowTestConfigurationFacadeImpl.java`
  Records keyed only on `workflowId` with no member/owner check.

- **8.1 · IDOR allowing cross-user workflow deletion in Integration Instance Delete Handler** — `configuration/facade/ConnectedUserIntegrationFacadeImpl.java`
  `deleteIntegrationInstance` calls `deleteByIntegrationInstanceId` unconditionally (line 131) before any ownership check.

- **8.1 · IDOR enabling cross-user integration instance access in UnifiedApiFacade** — `unified/facade/UnifiedApiFacadeImpl.java`
  `x-instance-id` header (caller `Long`) flows into `getComponentConnection`.

- **8.1 · IDOR enabling cross-tenant workflow deletion via GraphQL Mutation** — `mcp/facade/McpIntegrationInstanceConfigurationWorkflowFacadeImpl.java`
  `deleteMcpIntegrationInstanceConfigurationWorkflow` deletes by raw numeric `id` with no ownership check.

- **8.1 · IDOR enabling cross-tenant MCP tool invocation via Path Variable** — `server/config/EmbeddedMcpServerConfiguration.java`
  `/api/embedded/{secretKey}/mcp` stores `secretKey` in the transport context without validating it against the JWT.

- **8.1 · Broken Access Control bypassing user auth in Automation MCP Endpoint** — `web/authentication/AutomationMcpServerApiKeyAuthenticationProvider.java`
  `authenticate()` should verify both the MCP secret key and a user API key but does not.

- **8.1 · IDOR exposing Git credentials across Workspace boundaries** — `web/rest/GitConfigurationApiController.java`
  `GET`/`PUT /workspaces/{id}/git-configuration` with no workspace-membership/role check.

- **8.1 · IDOR enabling cross-user tool execution via Instance ID** — `execution/util/ConnectionIdHelper.java`
  `executeTool` accepts an `X-Instance-Id` header passed as `instanceId` to `getConnectionId`.

- **8.1 · IDOR enabling cross-user instance deletion in ConnectedUserIntegrationFacade** — `configuration/facade/ConnectedUserIntegrationFacadeImpl.java`
  `deleteIntegrationInstance` deletes workflows unconditionally (line 131) before ownership check.

- **8.1 · IDOR enabling cross-user project data access in ProjectGitApiController** — `web/rest/ProjectGitApiController.java`
  All five methods use a caller `{id}` for service/repository calls with no authorization check.

- **8.1 · IDOR enabling workflow deletion via unowned Integration Instance ID** — `configuration/facade/ConnectedUserIntegrationFacadeImpl.java`
  `deleteIntegrationInstance` calls `deleteByIntegrationInstanceId` before any ownership verification.

- **8.1 · IDOR allowing cross-tenant data access in CRM Account Endpoints** — `unified/facade/UnifiedApiFacadeImpl.java`
  `createAccount`/`getAccount`/`listAccounts`/`updateAccount` forward an optional `x-instance-id` to `getComponentConnection`.

- **8.1 · IDOR enabling cross-user data access in IntegrationInstanceApiController** — `web/rest/IntegrationInstanceApiController.java`
  All four endpoints under integration-instances/{id} accept an attacker numeric `id`.

- **8.1 · IDOR allowing cross-user API key takeover in ApiClientServiceImpl** — `configuration/service/ApiClientServiceImpl.java`
  Every `ApiClient` endpoint delegates to `ApiClientServiceImpl` with no ownership check.

- **8.1 · IDOR enabling chunk modification and deletion via GraphQL Mutations** — `knowledgebase/facade/KnowledgeBaseDocumentChunkFacadeImpl.java`
  `updateKnowledgeBaseDocumentChunk`/`deleteKnowledgeBaseDocumentChunk` accept a raw numeric `id` with no ownership verification.

- **8.1 · IDOR allowing cross-user conversation access in CosmosDB Chat Memory** — `cosmosdb/util/CosmosDbChatMemoryUtils.java`
  `getFirstMessages()` calls `findConversationIds()` with no user/tenant scope, returning every conversation ID.

- **8.1 · IDOR enabling cross-tenant data access via Integration Instance Header** — `unified/facade/UnifiedApiFacadeImpl.java`
  Accounting API endpoints forward an optional `x-instance-id` header to `UnifiedApiFacade`.

- **8.1 · IDOR enabling cross-user connection access in Connection API** — `web/rest/ConnectionApiController.java`
  `GET`/`DELETE`/`PATCH /connections/{id}` pass `Long id` directly to the facade with no check.

- **8.1 · IDOR enabling workflow deletion in deleteMcpProjectWorkflow Mutation** — `mcp/facade/McpProjectWorkflowFacadeImpl.java`
  `deleteMcpProjectWorkflow` passes an attacker `id` directly to the facade with no ownership check.

- **8.1 · IDOR enabling cross-workspace deletion in Connection Delete Handler** — `configuration/facade/WorkspaceConnectionFacadeImpl.java`
  `delete(long connectionId)` deletes a connection with no workspace ownership check.

- **8.1 · IDOR enabling cross-user workflow deletion in GraphQL Mutations** — `configuration/facade/ConnectedUserProjectFacadeImpl.java`
  `deleteConnectedUserProjectWorkflow`/`enableConnectedUserProjectWorkflow` pass a raw `id` with no ownership check.

- **8.1 · IDOR exposing cross-workspace Git credentials via Configuration API** — `web/rest/GitConfigurationApiController.java`
  `GET`/`PUT /workspaces/{id}/git-configuration` accept an arbitrary workspace ID with no membership check.

- **8.1 · IDOR enabling cross-user deletion in Notification Delete Handler** — `web/rest/NotificationApiController.java`
  `DELETE …/{notificationId}` passes the ID directly to `deleteById()` with no ownership check.

- **8.1 · IDOR allowing cross-user action execution in ActionApiController** — `web/rest/ActionApiController.java`
  `executeAction` passes `externalUserId` directly to `actionFacade.executeAction()` with no identity check.

- **8.1 · IDOR exposing Git credentials across all Workspaces** — `web/rest/GitConfigurationApiController.java`
  `getGitConfiguration`/`updateGitConfiguration` accept an arbitrary workspace `id` with no membership check.

- **8.1 · IDOR enabling tenant impersonation via unvalidated CURRENT_TENANT_ID header** — `web/filter/RemoteMultiTenantFilter.java`
  Reads `CURRENT_TENANT_ID` and passes it to `TenantContext.runWithTenantId()` for all non-`/remote/**` paths.

- **8.1 · IDOR enabling cross-user conversation deletion in ChatMemoryDeleteAction** — `builtin/action/ChatMemoryDeleteAction.java`
  Takes a `conversationId` from input and calls `deleteByConversationId` with no ownership check.

- **8.1 · IDOR allowing arbitrary MCP server deletion via GraphQL Mutation** — `web/graphql/EmbeddedMcpServerGraphQlController.java`
  `deleteEmbeddedMcpServer` deletes a server, components, and tools with no ownership check.

- **8.1 · IDOR enabling cross-user script execution in WorkflowNodeScript Mutations** — `web/graphql/WorkflowNodeScriptGraphQlController.java`
  `testWorkflowNodeScript`/`testClusterElementScript` execute a node's code by `workflowId` with no ownership check.

- **8.1 · IDOR allowing cross-user workflow modification in WorkflowNodeParameter Endpoints** — `web/rest/WorkflowNodeParameterApiController.java`
  Six REST endpoints accept an arbitrary workflow `{id}` path parameter.

- **8.1 · JWT Token Hijack via Unconstrained postMessage in Workflow Builder** — `workflow-builder/hooks/useWorkflowBuilder.ts`
  `useWorkflowBuilder` accepts an `EMBED_INIT` postMessage and writes `event.data.params.jwtToken` into `sessionStorage` with no origin constraint.

- **8.1 · IDOR enabling cross-workspace MCP Server deletion via GraphQL** — `mcp/facade/WorkspaceMcpServerFacadeImpl.java`
  `deleteWorkspaceMcpServer` deletes a server, components, and tools with no workspace check.

- **8.1 · IDOR enabling cross-workspace API key deletion in deleteWorkspaceApiKey** — `configuration/facade/WorkspaceApiKeyFacadeImpl.java`
  `deleteWorkspaceApiKey` deletes an API key with no workspace ownership check.

- **8.1 · IDOR enabling cross-environment data access in DataTableRow Operations** — `web/graphql/DataTableRowGraphQlController.java`
  Caller-supplied `environmentId` passed to `DataTableRowServiceImpl` without normalization against allowed enum ordinals.

- **8.1 · IDOR exposing cross-user log access in EditorLogFileGraphQlController** — `web/graphql/EditorLogFileGraphQlController.java`
  `editorJobFileLogs`, `editorTaskExecutionFileLogs`, `editorJobFileLogsExist`, `deleteJobFileLogs` pass a caller `jobId` directly to storage.

- **8.1 · IDOR enabling cross-user tool execution via Integration Instance** — `execution/util/ConnectionIdHelper.java`
  `executeTool`'s `X-Instance-Id` header flows into `getConnectionId()`.

- **8.1 · IDOR allowing workflow data destruction via unowned Instance ID** — `configuration/facade/ConnectedUserIntegrationFacadeImpl.java`
  `deleteIntegrationInstance` (line 130) deletes workflows before the ownership check.

- **8.1 · IDOR exposing all users' MCP tools via mcpTools Query** — `web/graphql/McpToolGraphQlController.java`
  `mcpTools` query and CRUD mutations perform no resource-level authorization.

- **8.1 · IDOR enabling forced public sharing of any project via exportSharedProject** — `web/graphql/ProjectGraphQlController.java`
  `exportSharedProject` accepts an arbitrary project `id` → `projectFacade.exportSharedProject(id, description)`.

- **8.1 · IDOR enabling cross-user workflow modification in Parameter Update Handler** — `configuration/facade/WorkflowNodeParameterFacadeImpl.java`
  PATCH/DELETE/GET endpoints under workflows/{id} use a caller `workflowId`.

- **8.1 · IDOR enabling arbitrary workflow deletion in deleteProjectWorkflow** — `configuration/facade/AutomationWorkflowProjectFacadeImpl.java`
  `deleteAutomationWorkflowProjectWorkflow` delegates to `deleteWorkflow(workflowUuid)` without first verifying the workflow's embedded project.

- **8.1 · IDOR enabling workflow deletion by unprivileged users** — `configuration/facade/IntegrationWorkflowFacadeImpl.java`
  `deleteWorkflow` deletes an arbitrary `workflowId` with no integration ownership check.

- **8.1 · IDOR enabling cross-user conversation deletion in JdbcChatMemoryDeleteAction** — `jdbc/action/JdbcChatMemoryDeleteAction.java`
  `perform` passes a caller `conversationId` to `deleteByConversationId` with no ownership verification.

- **7.8 · Missing Webhook Signature Verification in Linear Issue Trigger** — `linear/util/LinearUtils.java`
  `LinearNewIssueTrigger.webhookRequest` processes all POSTs without verifying the payload was signed by Linear.

- **7.8 · Missing Webhook Signature Verification enabling workflow spoofing in BoxNewFolderTrigger** — `box/trigger/BoxNewFolderTrigger.java`
  Returns the `source` field as trigger data without verifying the Box HMAC-SHA256 signature.

- **7.8 · Missing Webhook Passcode Validation in FigmaNewCommentTrigger** — `figma/trigger/FigmaNewCommentTrigger.java`
  Passes `body.getContent()` to the trigger without verifying the payload's `passcode` field.

- **7.8 · IDOR enabling user impersonation in Embedded API Key Auth** — `web/authentication/EmbeddedApiKeyAuthenticationProvider.java`
  Extracts `externalUserId` from the URL path in `EmbeddedApiKeyAuthenticationConverter` and trusts it.

- **7.8 · IDOR enabling cross-tenant tool execution via X-Instance-Id Header** — `execution/util/ConnectionIdHelper.java`
  `POST /{externalUserId}/tools` accepts an optional `X-Instance-Id` (raw `Long`) identifying an instance.

- **7.8 · IDOR enabling cross-tenant data read and deletion in KnowledgeBaseDocument Handler** — `web/graphql/KnowledgeBaseDocumentGraphQlController.java`
  `knowledgeBaseDocument` query and `deleteKnowledgeBaseDocument` mutation pass a caller `id` directly to the repository/facade.

- **7.8 · IDOR enabling cross-user data access in ConnectedUser Workflow Endpoints** — `web/rest/ConnectedUserProjectWorkflowApiController.java`
  `/{externalUserId}/automation/workflows` trusts an attacker `externalUserId` path parameter.

- **7.8 · IDOR enabling workflow hijack via missing HeyGen Webhook Signature** — `heygen/util/HeyGenUtils.java`
  `webhookRequest` in HeyGen completed/failed triggers returns trigger output without verifying origin.

- **7.8 · IDOR enabling cross-user data access in Workflow Test Configuration API** — `web/rest/WorkflowTestConfigurationApiController.java`
  All five endpoints query by caller `workflowId` (`findBy...`) with no ownership check.

- **7.8 · IDOR enabling cross-tenant chat memory access in InMemoryChatMemory** — `memory/cluster/InMemoryChatMemory.java`
  A `static final` `MessageWindowChatMemory` is shared at class-load time via `InMemoryChatMemoryRepositoryHolder.getInstance()`.

- **7.8 · IDOR enabling cross-user data access in WorkflowTestConfiguration Endpoints** — `configuration/facade/WorkflowTestConfigurationFacadeImpl.java`
  GET/PUT/DELETE under `/workflow-test-configurations/{workflowId}` never verify ownership.

- **7.8 · IDOR allowing unauthenticated workflow test data poisoning via WebhookTriggerTestController** — `security/config/WebhookAuthorizeHttpRequestContributor.java`
  `executeWorkflow` at `/webhooks/{id}/test/environments/{environmentId}` is registered without auth because `/webhooks/**` is in the permit-all list.

- **7.8 · IDOR enabling cross-workspace deletion in deleteKnowledgeBase Mutation** — `knowledgebase/facade/WorkspaceKnowledgeBaseFacadeImpl.java`
  `deleteKnowledgeBase` accepts an arbitrary `@Argument Long id` → `deleteWorkspaceKnowledgeBase(id)` with no workspace check.

- **7.8 · IDOR enabling environment boundary bypass in API Key Authentication** — `web/filter/AbstractApiKeyAuthenticationConverter.java`
  `getEnvironment` reads the `X-ENVIRONMENT` header with no validation and converts to an `Environment` ordinal.

- **7.8 · Missing Rate Limit enabling TOTP brute force in MFA Verification** — `user/service/UserServiceImpl.java`
  `POST /api/mfa/verify` accepts unlimited TOTP submissions with no lockout or rate limiting.

- **7.8 · IDOR enabling cross-user action execution in ActionApiController** — `web/rest/ActionApiController.java`
  `executeAction` passes `externalUserId` to `ActionFacadeImpl.executeAction` with no identity verification.

- **7.8 · IDOR enabling cross-user connection hijack in Action Execution** — `execution/util/ConnectionIdHelper.java`
  `getConnectionId` has an unsafe branch when `instanceId` is non-null.

- **7.8 · IDOR enabling cross-user data deletion in MongoDb Chat Memory Delete Action** — `mongodb/action/MongoDbChatMemoryDeleteAction.java`
  `perform` passes a caller `conversationId` to `deleteByConversationId` with no ownership check.

- **7.8 · IDOR enabling workflow spoofing via GitLab Webhook Trigger** — `gitlab/trigger/GitlabNewIssueTrigger.java`
  Registers a GitLab hook without a `secret_token` and declares no `webhookValidate`.

- **7.8 · Missing Webhook Origin Verification in Attio Task Trigger** — `attio/trigger/AttioTaskCreatedTrigger.java`
  Processes any POST without verifying it originated from Attio.

- **7.8 · IDOR enabling unauthorized ClickUp data access via Webhook Handler** — `clickup/util/ClickupUtils.java`
  Passes a raw `list_id` from an unauthenticated webhook into a server-side HTTP call path.

- **7.8 · IDOR allowing cross-user credential abuse in executeAction** — `execution/util/ConnectionIdHelper.java`
  Non-null `instanceId` branch calls `getIntegrationInstance(instanceId)` enabling cross-user credential use.

- **7.8 · IDOR enabling cross-user workflow creation via API Key Path** — `web/authentication/EmbeddedApiKeyAuthenticationProvider.java`
  Server-to-server copy/template endpoints take the target user identity from the URL path.

- **7.8 · IDOR enabling cross-tenant memory access in InMemoryChatMemory** — `memory/cluster/InMemoryChatMemory.java`
  Static `inMemoryChatMemory` initialized once at class-load via `InMemoryChatMemoryRepositoryHolder.getInstance()`.

- **7.8 · Missing Webhook Signature Verification allowing workflow spoofing in WooCommerce Triggers** — `woocommerce/util/WoocommerceUtils.java`
  `WoocommerceNewOrderTrigger`/`WoocommerceNewCouponTrigger` never register `.webhookValidate()`.

- **7.8 · Missing Webhook Signature Verification Allows Stripe Event Forgery** — `stripe/trigger/StripeNewCustomerTrigger.java`
  Processes Stripe POSTs without verifying the `Stripe-Signature` HMAC header.

- **7.8 · IDOR enabling cross-user impersonation in Embedded API Auth** — `web/authentication/EmbeddedApiKeyAuthenticationProvider.java`
  `EmbeddedApiKeyAuthenticationConverter` extracts `externalUserId` from the path and stores it in the auth token.

- **7.8 · IDOR enabling cross-user data access in Document Upload Handler** — `web/rest/KnowledgeBaseDocumentApiController.java`
  `uploadDocument` at `POST /internal/knowledge-bases/{id}/documents` never verifies KB ownership.

- **7.6 · IDOR allowing cross-user mutation in Notification API Controller** — `web/rest/NotificationApiController.java`
  `deleteNotification`/`updateNotification`/`getNotifications` operate on attacker IDs with no ownership check.

- **7.6 · IDOR enabling cross-user API key access in ApiKeyFacade** — `security/facade/ApiKeyFacadeImpl.java`
  `deleteApiKey`/`updateApiKey`/`apiKey` (and workspace counterparts) accept a raw numeric `id`.

- **7.6 · IDOR enabling cross-user workflow trigger manipulation in WebhookTriggerTest** — `web/rest/WebhookTriggerTestApiController.java`
  `start`/`stopWebhookTriggerTest` enable/disable triggers by `workflowId` with no ownership check.

- **7.6 · IDOR enabling unauthorized workflow execution in Job Creation Handler** — `web/rest/IntegrationInstanceConfigurationApiController.java`
  `createIntegrationInstanceConfigurationWorkflowJob` looks up a record by attacker `id` with no role check.

- **7.6 · IDOR enabling cross-user webhook trigger manipulation in WebhookTriggerTestFacadeImpl** — `web/rest/WebhookTriggerTestApiController.java`
  Any user can supply an arbitrary `workflowId` to `/webhooks/{workflowId}/test/start` and `stop`.

- **7.6 · IDOR enabling cross-user workflow tampering in IntegrationInstance Update** — `configuration/facade/ConnectedUserIntegrationInstanceFacadeImpl.java`
  `update`/`enableIntegrationInstanceWorkflow` never verify the fetched `IntegrationInstance` belongs to the user.

- **7.6 · IDOR enabling unauthorized mutation of MCP Workflow Configuration** — `web/graphql/McpIntegrationInstanceConfigurationWorkflowGraphQlController.java`
  Create/update/delete MCP workflow config mutations with no ownership check.

- **7.6 · IDOR enabling cross-user mutation in MCP Tool GraphQL Handler** — `web/graphql/McpToolGraphQlController.java`
  `mcpTool(id)`, `deleteMcpTool(id)`, `updateMcpTool(id, input)` accept attacker IDs with no check.

- **7.6 · IDOR enabling cross-user notification access in NotificationApiController** — `notification/service/NotificationServiceImpl.java`
  `DELETE`/`PUT /notifications/{notificationId}` and `GET /notifications` lack ownership checks.

- **7.6 · IDOR enabling cross-workspace data access in ApiCollection Endpoints** — `configuration/facade/ApiCollectionFacadeImpl.java`
  GET/DELETE/PUT/openapi.json on `/api-collections/{id}` with no ownership/membership check.

- **7.6 · IDOR allowing cross-deployment workflow modification in Update Handler** — `configuration/service/ProjectDeploymentWorkflowServiceImpl.java`
  `PUT /project-deployments/{id}/project-deployment-workflows/{...}` performs an ownership-blind update.

- **7.6 · IDOR allowing workflow content overwrite via Update Endpoint** — `configuration/facade/ProjectWorkflowFacadeImpl.java`
  `updateWorkflow` accepts a caller `workflowId` and full `definition` payload.

- **7.6 · IDOR allowing unauthorized modification of App Events via Internal API** — `web/rest/AppEventApi.java`
  CRUD on `/api/embedded/internal/app-events` with no role-level authorization.

- **7.6 · IDOR enabling cross-user task manipulation in ApprovalTaskGraphQlController** — `web/graphql/ApprovalTaskGraphQlController.java`
  `approvalTask`/`deleteApprovalTask`/`updateApprovalTask` operate on a caller `id` with no ownership check.

- **7.6 · IDOR enabling cross-user data tampering in Eval Scenario Mutations** — `eval/service/AiAgentEvalScenarioServiceImpl.java`
  `update`/`deleteAiAgentEvalScenario` pass a caller `id` to the service with no ownership check.

- **7.6 · IDOR enabling cross-user mutation in AiAgentJudge Mutations** — `eval/service/AiAgentJudgeServiceImpl.java`
  `delete`/`updateAiAgentJudge` pass a numeric `id` to `deleteById`/`findById` with no scoping.

- **7.6 · IDOR allowing judge tampering in AiAgentEvalGraphQlController** — `web/graphql/AiAgentEvalGraphQlController.java`
  `update`/`deleteAiAgentJudge` fetch/mutate/delete an `AiAgentJudge` by attacker `Long id`.

- **7.6 · IDOR enabling deletion and mutation of any workflow node test output** — `configuration/service/WorkflowNodeTestOutputServiceImpl.java`
  `delete`/`save`/`checkWorkflowNodeTestOutputExists` scoped solely to a caller `workflowId`.

- **7.5 · IDOR enabling unauthenticated file read in FileEntryController** — `security/config/WebhookAuthorizeHttpRequestContributor.java`
  `GET /file-entries/{id}/content` is `permitAll` via `getPermitAllRequestMatcherPaths()`.

- **7.5 · IDOR allowing workflow spoofing via unverified PagerDuty Webhook** — `pagerduty/trigger/PagerDutyNewOrUpdatedIncidentTrigger.java`
  Returns `content.get("event")` for workflow execution without verifying PagerDuty origin.

- **7.5 · IDOR exposing all service configs via unauthenticated Config Server** — `apps/config-server-app/build.gradle.kts`
  `ConfigServerApplication` enables Spring Cloud Config Server (`@EnableConfigServer`) with no authentication.

- **7.3 · IDOR enabling cross-workspace document deletion via GraphQL Mutation** — `web/graphql/KnowledgeBaseDocumentGraphQlController.java`
  `deleteKnowledgeBaseDocument` deletes a document, its file, and vector chunks with no workspace check.

- **7.3 · IDOR allowing cross-workspace chunk tampering via GraphQL Mutations** — `knowledgebase/facade/KnowledgeBaseDocumentChunkFacadeImpl.java`
  `update`/`deleteKnowledgeBaseDocumentChunk` pass a raw chunk `id` with no workspace check.

- **7.3 · IDOR allowing arbitrary workflow overwrite via MCP Tool** — `configuration/facade/ProjectWorkflowFacadeImpl.java`
  `updateClusterElementTask` MCP tool gets and updates a workflow by caller `workflowId` with no ownership check.

- **7.3 · IDOR enabling cross-user KB write in KnowledgeBaseLoadAction** — `knowledgebase/service/KnowledgeBaseServiceImpl.java`
  `KnowledgeBaseLoadAction.perform()` passes a workflow-supplied `KNOWLEDGE_BASE_ID` to `getKnowledgeBase(id)` with no tenancy check.

- **7.1 · IDOR allowing approval outcome tampering in ApprovalController** — `workflow/execution/ApprovalId.java`
  `/approvals/{id}` accepts a base64 token `tenantId:jobId:uuid:approved` with no integrity protection.

- **7.1 · IDOR enabling workflow deletion via Integration Instance Handler** — `configuration/facade/ConnectedUserIntegrationFacadeImpl.java` *(conf 9/10)*
  `deleteIntegrationInstance` calls `deleteByIntegrationInstanceId` unconditionally before any ownership check.

- **7.1 · Missing Webhook Signature Verification in GithubEventsTrigger** — `github/trigger/GithubEventsTrigger.java`
  Registers a GitHub webhook with no shared secret and accepts POSTs without verifying `X-Hub-Signature-256`.

- **7.1 · IDOR enabling cross-user workflow mutation in saveClusterElementTestOutput** — `web/graphql/WorkflowNodeTestOutputGraphQlController.java`
  `saveClusterElementTestOutput` accepts a `workflowId` never validated against ownership.

- **7.1 · IDOR enabling cross-workspace tag write in DataTableTagService** — `configuration/service/DataTableTagServiceImpl.java`
  `updateTags` fetches a `DataTable` by caller `tableId` with no workspace check.

- **7.1 · IDOR enabling cross-project Git operations in ProjectGitFacade** — `configuration/facade/ProjectGitFacadeImpl.java`
  `pullProjectFromGit`/`pushProjectToGit`/`getRemoteBranches` accept a raw `projectId` with no membership check.

- **7.1 · IDOR exposing connection credentials via ClusterElement Options Query** — `component/facade/ClusterElementDefinitionFacadeImpl.java`
  `clusterElementOptions` loads a connection via a caller `connectionId` → `getConnection(connectionId)`.

- **7.1 · IDOR allowing workflow injection into any user project** — `web/graphql/ProjectWorkflowGraphQlController.java`
  `importWorkflowTemplate` imports into a caller-supplied `projectId` with no ownership check.

- **7.1 · IDOR enabling cross-user workflow manipulation in Integration Instance Workflow API** — `web/rest/IntegrationInstanceWorkflowApiController.java`
  Three server-to-server methods trust an `externalUserId` path parameter.

- **7.1 · IDOR enabling cross-user MCP tool state tampering in McpIntegrationInstanceToolApiController** — `web/rest/McpIntegrationInstanceToolApiController.java`
  `.../mcp-tools/{mcpToolId}/enable` ignores `externalUserId` (suppressed unused) and never verifies it.

- **7.1 · IDOR enabling cross-user tool mutation via updateMcpTool** — `web/graphql/McpToolGraphQlController.java`
  `updateMcpTool` overwrites any matching record via `McpToolServiceImpl.update()` with no ownership check.

- **7.1 · IDOR allowing approval decision tampering in ApprovalController** — `workflow/execution/ApprovalId.java`
  `ApprovalId` encodes `tenantId:jobId:uuid:approved` in plain base64 with no integrity protection.

- **7.1 · IDOR enabling MCP tool state tampering in Integration Instance Endpoint** — `web/rest/McpIntegrationInstanceToolApiController.java`
  `enableMcpIntegrationInstanceTool` accepts a raw `integrationInstanceId` and enables/creates tool records with no ownership check.

- **7.1 · IDOR enabling cross-user workflow manipulation in IntegrationInstance Handler** — `configuration/facade/ConnectedUserIntegrationInstanceFacadeImpl.java`
  `enable`/`updateIntegrationInstanceWorkflow` perform a flawed ownership check before mutation.

- **7.1 · IDOR exposing cross-user logs via Job Log GraphQL Queries** — `web/graphql/LogFileGraphQlController.java`
  IDOR in the GraphQL log query and mutation handlers.

- **7.1 · IDOR enabling cross-user data overwrite in McpProjectWorkflow Update** — `web/graphql/McpProjectWorkflowGraphQlController.java`
  `updateMcpProjectWorkflow` updates `parameters` by caller `id` with no ownership check.

- **7.1 · IDOR enabling overwrite of any user shared workflow export** — `web/graphql/ProjectWorkflowGraphQlController.java`
  `exportSharedWorkflow` exports any workflow by ID, overwriting the shared-template file.

- **7.1 · IDOR allowing project injection into arbitrary Workspace** — `configuration/facade/ProjectFacadeImpl.java`
  `importProject` at `POST /workspaces/{workspaceId}/projects/import` creates a project with no membership check.

- **7.1 · IDOR allowing cross-user workflow manipulation in MCP Workflow Controller** — `mcp/facade/McpIntegrationInstanceWorkflowFacadeImpl.java`
  Three REST endpoints mutate another user's `IntegrationInstanceWorkflow` records.

- **7.1 · IDOR enabling cross-user data write in WorkflowNodeTestOutput Endpoints** — `web/rest/WorkflowNodeTestOutputApiController.java`
  Three mutating endpoints accept a caller `workflowId` path variable.

- **7.1 · IDOR allowing unauthorized git pull and project publish** — `configuration/facade/ProjectGitFacadeImpl.java`
  `pullProjectFromGit` accepts a caller `id` with no ownership check.

- **7.1 · IDOR enabling cross-user workflow tampering in IntegrationInstance Facade** — `configuration/facade/ConnectedUserIntegrationInstanceFacadeImpl.java`
  `enable`/`updateIntegrationInstanceWorkflow` fetch by unrestricted `findById` then check only that `externalUserId` exists.

- **7.1 · IDOR allowing cross-user MCP Tool parameter tampering** — `web/graphql/McpToolGraphQlController.java`
  `updateMcpTool` accepts a caller `id` and overwrites the target `McpTool` record with no ownership or authorization check.

- **7.1 · IDOR allowing cross-user connection hijack in WorkflowTestConfiguration Mutations** — `configuration/facade/WorkflowTestConfigurationFacadeImpl.java`
  `saveWorkflowTestConfigurationConnection`/`saveClusterElementTestConfigurationConnection` associate a caller `connectionId` with an arbitrary `workflowId` with no ownership check on either.

---

## 🟡 Medium (4.0 – 6.9) — 169 findings

### Remote Code Execution / DoS (4)

- **6.5 · ReDoS causing service DoS via unvalidated Regex in Match Action** — `helper/util/TextHelperUtils.java`
  `TextHelperMatchAction.perform` passes a user-supplied `regularExpression` directly to `Pattern.compile()` + `matcher.find()`.

- **6.5 · RCE enabling DoS via ReDoS in String Mapping Handler** — `mapper/action/DataMapperReplaceAllSpecifiedValuesAction.java`
  `fillOutput` (line 284) calls `Pattern.compile(key.toString())` on a user-supplied `FROM` mapping value.

- **6.5 · DoS via unbounded recursion in TaskStarted Event Listener** — `event/listener/TaskStartedApplicationEventListener.java` *(conf 9/10)*
  `onApplicationEvent` recurses up the task parent chain by re-entering itself when `getParentId()` is non-null.

- **5.1 · JSON Injection enabling config tampering in VoiceAgent Settings Builder** — `deepgram/action/DeepgramVoiceAgentAction.java`
  `buildSettingsMessage` builds a JSON string by concatenating user-controlled parameters without JSON escaping.

### Server-Side Request Forgery / XXE (9)

- **6.5 · SSRF exposing internal services via Job Webhook URL** — `execution/service/JobServiceImpl.java`
  `Job.Webhook.url` is used as-is in `RestTemplate.postForObject(webhook.url(), ...)`.

- **6.2 · XXE enabling file read via XmlHelper Parse Action** — `commons/util/XmlUtils.java` *(conf 9/10)*
  `XmlHelperParseAction.perform()` passes user XML to `XmlUtils.read()` → `xmlMapper.readValue()` on the raw input.

- **5.3 · SSRF via unvalidated ENTITY_TYPE in Update Record Action** — `crm/action/MicrosoftDynamicsCrmUpdateRecordAction.java`
  `ClickupNewTaskTrigger.webhookRequest` passes the raw body to `getCreatedObject`, appending `task_id` to a path string.

- **4.3 · SSRF enabling internal network access via Cassandra Contact Points** — `cassandra/util/CassandraChatMemoryUtils.java`
  SSRF in `CassandraChatMemoryUtils.getChatMemoryRepository()`.

- **4.3 · SSRF via unvalidated hostname in RabbitMQ Connection** — `rabbitmq/util/RabbitMqUtils.java`
  `RabbitMqConnection` passes a free-form `hostname`/`port` into `ConnectionFactory.setHost/setPort` then `newConnection()`.

- **4.3 · SSRF via unvalidated region parameter in Zoho OAuth Token Exchange** — `zoho/commons/ZohoConnection.java`
  SSRF in the Zoho OAuth2 connection setup.

- **4.0 · SSRF enabling internal network access via GraphQL Endpoint Field** — `client/action/GraphQlClientQueryAction.java`
  `perform()` passes a free-form `GRAPHQL_ENDPOINT` URL to `context.http(http -> http.post(url))` with no validation.

- **4.0 · SSRF via Unvalidated URL in Documentation Fetch Feature** — `configuration/service/ApiConnectorAiServiceImpl.java`
  `generateFromDocumentation`/`startGenerateFromDocumentationPreview` pass `documentationUrl` to `URI.create()` + `HttpClient.send()`.

- **4.0 · SSRF via unrestricted JDBC host in DataSourceFactory** — `component/jdbc/DataSourceFactory.java`
  `getDataSource()` interpolates user `host`/`port`/`database` into a JDBC URL with no hostname/IP validation.

### Local File Inclusion / Path Traversal (2)

- **5.1 · Guardrail bypass via path traversal in URL Allowlist Check** — `guardrails/util/UrlDetectorUtils.java`
  `matchesSchemeEntry`/`urlAllowed` compare paths with a raw `String.startsWith` on `uri.getRawPath()`.

- **4.0 · XPath Injection enabling data extraction in XML File Read Action** — `commons/util/XmlUtils.java`
  `XmlFileReadAction.perform()` passes a `PATH` parameter directly to `xPath.compile(path)` in `XmlUtils.parse()`.

### Arbitrary File Operation (1)

- **6.2 · AFO via unsanitized filename in S3 key construction** — `aws/service/AwsFileStorageServiceImpl.java`
  `combinePaths()` sanitizes only `directory`, appending `filename` to the S3 key completely unsanitized (line 219).

### Cross-Site Scripting / Header Injection (2)

- **4.9 · HTTP Header Injection via CRLF in OpenAPI Spec Download** — `web/rest/ApiCollectionApiController.java`
  `getOpenApiSpecification` builds the `Content-Disposition` header from the collection `name` after only `toLowerCase`/`replace`/`trim`.

- **4.3 · HTTP Header Injection via Workflow Label in Export Handler** — `web/rest/AbstractWorkflowApiController.java` *(conf 9/10)*
  `exportWorkflow` concatenates `workflow.getLabel()` unsanitized into the `Content-Disposition` header.

### IDOR / Broken Access Control (151)

- **6.9 · Missing Webhook Validation enabling spoofed SMS trigger injection** — `infobip/trigger/InfobipNewSMSTrigger.java`
  Returns the raw body via `body.getContent()` with no source validation.

- **6.8 · IDOR enabling cross-user workflow creation in AutomationWorkflowProjectApiController** — `web/rest/AutomationWorkflowProjectApiController.java`
  `copyWorkflowTemplate`/`generateProjectWorkflow` trust an `externalUserId` path parameter.

- **6.8 · IDOR enabling workflow state tampering in Integration Instance Workflow API** — `configuration/facade/ConnectedUserIntegrationInstanceFacadeImpl.java`
  Frontend enable/disable/update endpoints pass an attacker instance `id` to the facade.

- **6.8 · IDOR enabling cross-workspace mutation in updateKnowledgeBase Mutation** — `web/graphql/KnowledgeBaseGraphQlController.java`
  `updateKnowledgeBase` passes a bare `id` to the service with no workspace check.

- **6.8 · IDOR allowing cross-user document injection in Knowledge Base Upload** — `web/rest/KnowledgeBaseDocumentApiController.java`
  `uploadDocument` accepts an arbitrary KB `{id}` with no access check.

- **6.5 · IDOR exposing cross-user workflow data in WorkflowNodeOptionFacade** — `web/rest/WorkflowNodeOptionApiController.java`
  `getWorkflowNodeOptions`/`getClusterElementNodeOptions` fetch by `workflowId` with no ownership check.

- **6.5 · IDOR exposing all tenants' MCP Servers in embeddedMcpServers query** — `mcp/service/McpServerServiceImpl.java` *(conf 9/10)*
  `embeddedMcpServers` returns every EMBEDDED `McpServer` with no tenant filter.

- **6.5 · Sensitive Config Exposure via Unrestricted config() Formula Function** — `bytechef/evaluator/Config.java`
  The whitelisted `config` SpEL function calls `Environment.getProperty(propertyName)` on an attacker-controlled name.

- **6.5 · IDOR allowing cross-user MCP workflow mutation in McpIntegrationInstanceWorkflowApiController** — `web/rest/McpIntegrationInstanceWorkflowApiController.java`
  `.../mcp-workflows/{workflowUuid}/enable` and update endpoints mutate another tenant's instance by numeric `id`.

- **6.5 · IDOR exposing cross-workspace job data via Execution Lookup** — `execution/facade/ProjectWorkflowExecutionFacadeImpl.java`
  `getWorkflowExecution(long id)` fetches any job by global ID with no workspace check.

- **6.5 · IDOR exposing all workflows cross-user via Unscoped Listing Endpoint** — `configuration/facade/ProjectWorkflowFacadeImpl.java`
  `getWorkflows` returns every workflow in the system with no scope filter.

- **6.5 · IDOR allowing credential theft via Connection ID in ClusterElement Facade** — `component/facade/ClusterElementDefinitionFacadeImpl.java`
  `getComponentConnection(Long connectionId)` resolves via bare `findById(id)` with no scope filter.

- **6.5 · IDOR exposing MCP server secret keys via GraphQL Query** — `web/graphql/McpServerGraphQlController.java`
  `mcpServer(id)` returns a full `McpServer` including `secretKey` with no ownership check.

- **6.5 · IDOR enabling cross-workspace data read in Knowledge Base Search** — `web/graphql/KnowledgeBaseGraphQlController.java`
  `searchKnowledgeBase` forwards a caller `id` with no workspace check.

- **6.5 · IDOR exposing cross-workspace knowledge base data via knowledgeBase Query** — `web/graphql/KnowledgeBaseGraphQlController.java`
  `knowledgeBase` returns the full object for any ID regardless of workspace.

- **6.5 · IDOR exposing all ConnectedUserProjects via unscoped findAll** — `configuration/service/ConnectedUserProjectServiceImpl.java` *(conf 9/10)*
  `getConnectedUserProjects` calls `findAll()` ignoring the `connectedUserId`/`environment` filters.

- **6.5 · IDOR exposing all tenant data via ConnectedUserProjects Query** — `configuration/service/ConnectedUserProjectServiceImpl.java`
  The query handler forwards args to a service that ignores filters and calls `findAll()`.

- **6.5 · Spoofed Webhook Payload injection in CalCom Booking Trigger** — `calcom/util/CalComUtils.java`
  `CalComBookingEndedTrigger.webhookRequest` accepts any POST with no signature verification.

- **6.5 · IDOR enabling cross-tenant MCP tool state write in McpIntegrationInstanceToolApiController** — `mcp/facade/McpIntegrationInstanceToolFacadeImpl.java`
  `enable`/`disableMcpIntegrationInstanceTool` pass a caller `integrationInstanceId` with no ownership check.

- **6.5 · IDOR exposing cross-tenant project data in ConnectedUser Query** — `web/graphql/ConnectedUserProjectGraphQlController.java`
  `connectedUserProjects` returns all projects for an arbitrary `connectedUserId` with no authorization.

- **6.5 · IDOR exposing connected user PII via GraphQL Query** — `web/graphql/ConnectedUserGraphQlController.java`
  `connectedUser`/`connectedUsers` pass attacker `id`/filters to the service with no authorization.

- **6.5 · Missing Passcode Verification enabling spoofed webhook in FigmaNewCommentTrigger** — `figma/trigger/FigmaNewCommentTrigger.java`
  Registers a `passcode` but never verifies it on incoming requests.

- **6.5 · IDOR enabling cross-user data access in McpProject Queries** — `mcp/service/McpProjectServiceImpl.java`
  `mcpProject`/`mcpProjects` delegate directly to the service with no ownership check.

- **6.5 · IDOR exposing cross-workspace chat workflows in workspaceChatWorkflows** — `web/graphql/ProjectDeploymentWorkflowGraphQlController.java`
  Accepts an arbitrary `workspaceId` with no membership check.

- **6.5 · IDOR exposing cross-workspace deployment workflows in projectDeploymentWorkflow** — `web/graphql/ProjectDeploymentWorkflowGraphQlController.java`
  Returns the full `ProjectDeploymentWorkflow` (incl. connections) for a caller composite ID.

- **6.5 · IDOR enabling cross-workspace connection read via GetConnection** — `web/rest/ConnectionApiController.java`
  `GET /connections/{id}` returns the full connection (credentials/auth params) with no ownership check.

- **6.5 · IDOR enabling cross-user workflow disclosure via Validator Query** — `workflow/validator/WorkflowValidatorFacadeImpl.java`
  `validateWorkflowById` passes a caller `workflowId` to `getWorkflow` with no check.

- **6.5 · IDOR exposing cross-user workflow data in Node Output API** — `web/rest/WorkflowNodeOutputApiController.java`
  Three node-output endpoints return workflow structure/test data by `workflowId` with no ownership check.

- **6.5 · IDOR exposing cross-user credentials in Connection Read** — `web/rest/ConnectionApiController.java`
  `GET /connections/{id}` fetches by raw numeric ID with no workspace check.

- **6.5 · IDOR exposing cross-user workflow data via Options Endpoints** — `configuration/facade/WorkflowNodeOptionFacadeImpl.java`
  Both options endpoints fetch workflow/test config/connection IDs by `workflowId` with no ownership check.

- **6.5 · IDOR exposing arbitrary workflow definitions via validateWorkflowById** — `workflow/validator/WorkflowValidatorFacadeImpl.java`
  `validateWorkflowById` → `getWorkflow` → `findById(id)` with no ownership check.

- **6.5 · IDOR exposing workflow execution data in getWorkflowExecution** — `web/rest/WorkflowExecutionApiController.java`
  `getWorkflowExecution(long id)` fetches a job by caller ID with no authorization.

- **6.5 · IDOR enabling cross-tenant data read in Workflow Execution Handler** — `execution/facade/IntegrationWorkflowExecutionFacadeImpl.java`
  `getWorkflowExecution` retrieves a `Job` and data by `Long id` with no tenant check.

- **6.5 · IDOR enabling skill archive theft in Download Handler** — `web/rest/AiSkillDownloadController.java`
  `GET /api/ai/agent-skills/{id}/download` returns the `.skill` ZIP for any ID with no access check.

- **6.5 · IDOR enabling cross-workspace connection mutation in updateConnection** — `web/rest/ConnectionApiController.java`
  `updateConnection(Long id, ...)` → `save(...)` with no connection ownership check.

- **6.5 · IDOR enabling cross-tenant connection data theft in getConnections** — `configuration/facade/ConnectedUserConnectionFacadeImpl.java`
  `getConnections` fetches each caller-supplied connection ID with no ownership check.

- **6.5 · IDOR allowing cross-user integration data access in getIntegration** — `web/rest/IntegrationApiController.java`
  `GET /{externalUserId}/integrations[/{id}]` queries by path `externalUserId` with no authorization.

- **6.5 · IDOR enabling cross-workspace connection deletion via DeleteConnection** — `web/rest/ConnectionApiController.java`
  `DELETE /connections/{id}` deletes by ID with no ownership/workspace check.

- **6.5 · IDOR enabling cross-user workflow data access in WorkflowNodeOptionApiController** — `web/rest/WorkflowNodeOptionApiController.java`
  Both options endpoints → `getWorkflow(workflowId)` with no ownership check.

- **6.5 · IDOR enabling cross-user data read in ApprovalTask Query** — `web/graphql/ApprovalTaskGraphQlController.java`
  `approvalTask(id)`/`approvalTasksByIds(ids)` fetch records by DB IDs with no authorization.

- **6.5 · IDOR exposing cross-user connections in ConnectedUser Connection Handler** — `web/rest/ConnectionApiController.java`
  `getConnectedUserConnections` returns all credentials for a caller `connectedUserId`.

- **6.5 · IDOR enabling API key rename via updateWorkspaceApiKey Mutation** — `security/facade/ApiKeyFacadeImpl.java`
  IDOR in `updateWorkspaceApiKey`.

- **6.5 · IDOR enabling cross-workspace data read in Workflow Execution Endpoint** — `execution/facade/ProjectWorkflowExecutionFacadeImpl.java`
  `GET /internal/workflow-executions/{id}` fetches by bare numeric `id` with no workspace check.

- **6.5 · IDOR enabling cross-workspace data access in workspaceProjectDeployments** — `web/graphql/ProjectDeploymentGraphQlController.java`
  Forwards an attacker `workspaceId` to `getProjectDeployments()` with no membership check.

- **6.5 · IDOR exposing cross-workspace webhook URLs in DataTableWebhooks Query** — `web/graphql/DataTableWebhookGraphQlController.java`
  `dataTableWebhooks` resolves `tableId` via `getBaseNameById` → `findById(id)` with no scope.

- **6.5 · IDOR exposing cross-user workflow data in Dynamic Properties API** — `web/rest/WorkflowNodeDynamicPropertiesApiController.java`
  Both dynamic-properties endpoints → `getWorkflow(workflowId)` with no ownership check.

- **6.5 · IDOR exposing cross-user eval results via GraphQL queries** — `eval/service/AiAgentEvalResultServiceImpl.java`
  `fetch`/`getAgentEvalResult(long id)` use `findById(id)` with no ownership/tenant validation.

- **6.5 · IDOR exposing arbitrary workflow data via AI Copilot endpoint** — `copilot/agent/WorkflowEditorSpringAIAgent.java`
  `createSystemMessage` reads `workflowId` from request body → `getWorkflow` with no authorization.

- **6.5 · IDOR exposing all tenant projects via GraphQL projects Query** — `web/graphql/ProjectGraphQlController.java`
  `projects` (line 108) calls `projectService.getProjects()` with no workspace scope.

- **6.5 · IDOR enabling cross-user chat history read in GetMessages Action** — `builtin/action/ChatMemoryGetMessagesAction.java`
  `perform()` passes a caller `conversationId` to `findByConversationId` with no authorization.

- **6.5 · IDOR exposing cross-user connection credentials via connectionIds** — `configuration/facade/ConnectedUserConnectionFacadeImpl.java`
  `getConnections` streams each caller ID to `getConnection(id)` with no scope check.

- **6.5 · IDOR exposing cross-workspace deployments in Project Deployment API** — `configuration/facade/ProjectDeploymentFacadeImpl.java`
  `getWorkspaceProjectDeployments` queries by arbitrary `workspaceId` path variable.

- **6.5 · IDOR enabling unauthorized workflow trigger via Webhook Handler** — `calendar/trigger/GoogleCalendarEventTrigger.java`
  Processes push notifications without verifying Google origin.

- **6.5 · IDOR exposing all platform API keys via adminApiKeys Query** — `web/graphql/ApiKeyGraphQlController.java`
  `adminApiKeys(environmentId)` carries no role-gating annotation.

- **6.5 · Missing Webhook Signature Validation Allowing Workflow Spoofing** — `mailerlite/trigger/MailerLiteSubscriberCreatedTrigger.java`
  Passes the body to `getContent` without verifying the `Signature` HMAC-SHA256 header.

- **6.5 · IDOR exposing cross-user job logs in EditorLogFileGraphQlController** — `web/graphql/EditorLogFileGraphQlController.java`
  Three editor-log resolvers pass a caller `jobId` to storage with no ownership check.

- **6.5 · IDOR exposing all MCP Projects via mcpProjects GraphQL Query** — `mcp/service/McpProjectServiceImpl.java`
  `mcpProjects` → `findAll()` returns every `McpProject` regardless of owner.

- **6.5 · IDOR allowing cross-user data read in Notification List Handler** — `notification/service/NotificationServiceImpl.java`
  `GET /notifications` returns every record via `findAll()` with no user filter.

- **6.5 · IDOR enabling cross-user workflow read via validateWorkflowById** — `workflow/validator/WorkflowValidatorFacadeImpl.java`
  `validateWorkflowById` passes an attacker `workflowId` straight through.

- **6.5 · IDOR enabling cross-tenant data read in Workflow Execution Endpoint** — `execution/facade/IntegrationWorkflowExecutionFacadeImpl.java`
  `GET …-executions/{id}` → `jobService.getJob(id)` with no tenant filter.

- **6.3 · IDOR enabling cross-user run cancellation in cancelEvalRun** — `eval/facade/AiAgentEvalRunFacadeImpl.java`
  `cancelEvalRun(long id)` marks a run `FAILED` by bare ID with no ownership check.

- **6.3 · IDOR enabling cross-user data manipulation in WorkflowNodeTestOutput API** — `web/rest/WorkflowNodeTestOutputApiController.java` *(conf 9/10)*
  `…/{id}/workflow-nodes/{name}/test-outputs` accept a workflow `id` with no ownership check.

- **6.3 · IDOR allowing cross-user webhook trigger manipulation in WebhookTriggerTestApi** — `web/rest/WebhookTriggerTestApiController.java`
  `start`/`stopWebhookTriggerTest` accept a `workflowId` + `environmentId` with no ownership check.

- **6.3 · IDOR enabling cross-user trigger control in Webhook Test Endpoints** — `web/rest/WebhookTriggerTestApiController.java`
  `start`/`stopWebhookTriggerTest` → `enableTrigger`/`disableTrigger` by `workflowId` with no access check.

- **6.2 · IDOR exposing all workflows via MCP Tool Search** — `tool/automation/ReadProjectWorkflowTools.java`
  `searchWorkflows` enumerates all workflow definitions across projects/tenants when `projectId` is omitted.

- **6.2 · IDOR allowing document injection into any Knowledge Base** — `web/rest/KnowledgeBaseDocumentApiController.java`
  `uploadDocument` uploads to an arbitrary KB `{id}` with no access check.

- **6.2 · IDOR enabling cross-user memory read in LangchainAgent** — `agui/langchain4j/LangchainAgent.java`
  `buildAssistant()` shares one `chatMemory` across users with no per-user/thread binding.

- **6.2 · IDOR allowing cross-workspace data read in Knowledge Base Search Tool** — `knowledgebase/cluster/KnowledgeBaseVectorStore.java`
  `KnowledgeBaseSearchTool` passes a `knowledgeBaseId` directly with no workspace check.

- **6.2 · IDOR enabling cross-user skill data read in AiSkill Tools** — `skill/facade/AiSkillFacadeImpl.java`
  `getAiSkill`/`getAiSkillFileContent`/`getAiSkillFilePaths` pass a caller `id` to the service with no check.

- **6.2 · IDOR enabling cross-workspace modification in updateKnowledgeBase Mutation** — `web/graphql/KnowledgeBaseGraphQlController.java`
  `updateKnowledgeBase` (line 93) forwards `Long id` + payload to the service with no workspace check.

- **6.2 · IDOR exposing cross-workspace knowledge base content in Search** — `web/graphql/KnowledgeBaseGraphQlController.java`
  `searchKnowledgeBase` passes a caller `id` to the facade with no workspace check.

- **6.2 · IDOR enabling cross-workspace data read in searchKnowledgeBase** — `web/graphql/KnowledgeBaseGraphQlController.java`
  `searchKnowledgeBase` forwards a caller KB `id` with no workspace check.

- **6.2 · IDOR exposing all server secrets via Config SpEL Function** — `bytechef/evaluator/Config.java`
  The `Config` SpEL `MethodExecutor` returns any Spring `Environment` property with no allowlist/prefix restriction.

- **6.1 · IDOR enabling workflow spoofing via unverified Box Webhook** — `box/trigger/BoxNewFileTrigger.java`
  `BoxNewFileTrigger.webhookRequest` processes callbacks without verifying the Box HMAC-SHA256 signature.

- **6.1 · Missing Webhook Verification enabling unauthorized trigger execution in GoogleSheetsNewRowTrigger** — `sheets/trigger/GoogleSheetsNewRowTrigger.java` *(conf 9/10)*
  `webhookEnable` registers a Drive push channel without a `Channel.setToken(secret)`.

- **6.1 · Missing Twilio Signature Validation in WhatsApp Message Trigger** — `twilio/trigger/TwilioNewWhatsappMessageTrigger.java`
  `webhookRequest` returns `body.getContent()` with no `.webhookValidate()` handler.

- **6.1 · Missing Webhook Signature Verification in Linear Issue Triggers** — `linear/util/LinearUtils.java`
  `LinearRemovedIssueTrigger`/shared `executeIssueTriggerQuery` process payloads without verifying `X-Linear-Signature`.

- **6.1 · IDOR allowing spoofed webhook execution in AttioRecordCreatedTrigger** — `attio/trigger/AttioRecordCreatedTrigger.java`
  Accepts all inbound webhook POSTs without verifying Attio origin.

- **6.1 · Missing Webhook Signature Validation in Attio Trigger Handlers** — `attio/trigger/AttioRecordCreatedTrigger.java` *(conf 9/10)*
  `AttioRecordCreatedTrigger`/`AttioTaskCreatedTrigger` never register `.webhookValidate()`.

- **6.1 · IDOR enabling cross-tenant execution via forged WorkflowExecutionId** — `platform/workflow/WorkflowExecutionId.java`
  `parse()` (line 61) deserializes a base64 string placing `items[0]` directly into `tenantId` with no integrity verification.

- **6.1 · IDOR enabling unauthorized workflow trigger in CalCom Booking Webhook** — `calcom/util/CalComUtils.java`
  Returns the raw `payload` field without verifying Cal.com origin.

- **5.4 · IDOR enabling cross-user data access in Notification CRUD** — `notification/service/NotificationServiceImpl.java`
  `NotificationFacadeImpl` get/update/delete operations perform no ownership check.

- **5.4 · IDOR enabling cross-user MCP tool state manipulation** — `mcp/facade/ConnectedUserMcpServerFacadeImpl.java`
  `enableConnectedUserMcpTool` calls `updateEnabled(id, enable)` with no ownership check.

- **5.4 · IDOR allowing cross-user webhook trigger manipulation in WebhookTriggerTestApiController** — `web/rest/WebhookTriggerTestApiController.java`
  `start`/`stopWebhookTriggerTest` → `enableTrigger`/`disableTrigger` with no ownership check.

- **5.4 · IDOR enabling cross-user MCP tool state modification in enableConnectedUserMcpTool** — `web/graphql/ConnectedUserMcpServerGraphQlController.java`
  Passes a caller `id` to `enableMcpTool` → `updateEnabled(id, enable)`.

- **5.4 · IDOR enabling cross-user git config access in ProjectGitApiController** — `web/rest/ProjectGitApiController.java`
  `GET`/`PUT /projects/{id}/project-git-configuration` accept an arbitrary project ID.

- **5.4 · IDOR allowing tag overwrite in ProjectDeployment Update Handler** — `web/rest/ProjectDeploymentTagApiController.java`
  `PUT …-deployments/{id}/tags` accepts an arbitrary deployment `id` with no ownership check.

- **5.4 · IDOR enabling cross-user webhook trigger control in WebhookTriggerTestApiController** — `web/rest/WebhookTriggerTestApiController.java`
  `start`/`stopWebhookTriggerTest` accept `workflowId`/`environmentId` with no membership check.

- **5.4 · IDOR enabling unauthorized workflow test abort in stopWorkflowTest** — `web/rest/WorkflowTestApiController.java`
  `POST /workflow-tests/{jobId}/stop` cancels a test by `jobId` with no ownership check.

- **5.4 · IDOR enabling cross-user workflow test configuration tampering** — `configuration/facade/WorkflowTestConfigurationFacadeImpl.java`
  Save/delete config + connection operations accept attacker `workflowId`/`connectionId`/`key`/`value`.

- **5.4 · IDOR allowing unauthorized workflow state control via externalUserId** — `web/rest/ConnectedUserProjectWorkflowApiController.java`
  `disable`/`enableProjectWorkflow` ignore the `externalUserId` parameter entirely.

- **5.4 · IDOR allowing connection hijacking in WorkflowTestConfiguration Mutation** — `configuration/facade/WorkflowTestConfigurationFacadeImpl.java`
  `saveWorkflowTestConfigurationConnection` associates a caller `connectionId` after only matching component name.

- **5.4 · IDOR allowing cross-user connection abuse in ClusterElementDefinitionFacade** — `component/facade/ClusterElementDefinitionFacadeImpl.java`
  `getComponentConnection(Long connectionId)` fetches by ID with no workspace access check.

- **5.3 · IDOR enabling email enumeration via Activation Email Endpoint** — `web/rest/AccountController.java`
  `POST /api/send-activation-email` (public) throws `UserNotFoundException` for unknown emails, enabling enumeration.

- **5.3 · Missing Webhook Signature Verification in Email Delivered Trigger** — `resend/trigger/ResendEmailDeliveredTrigger.java`
  Returns `body.getContent()` without verifying Resend origin.

- **5.3 · IDOR exposing connection usage enumeration via unprotected Remote Endpoint** — `rest/service/RemoteWorkflowTestConfigurationServiceController.java` *(conf 9/10)*
  `/remote/workflow-test-configuration-service/is-connection-used/{connectionId}` has no security filter chain.

- **5.3 · IDOR exposing workflow config via unauthenticated Trigger Form API** — `security/config/PlatformConfigurationAuthorizeHttpRequestContributor.java`
  Registers the trigger-form path as `permitAll`.

- **5.3 · IDOR exposing approval form data via unvalidated job token** — `execution/facade/ApprovalFormFacadeImpl.java` *(conf 9/10)*
  `GET …-form/{id}` (public) accepts a base64 capability token `tenantId:jobId:uuid`.

- **5.3 · Missing Webhook Signature Verification in WooCommerce Coupon Trigger** — `woocommerce/trigger/WoocommerceNewCouponTrigger.java`
  Returns `body.getContent()` without verifying `X-WC-Webhook-Signature`.

- **5.1 · IDOR enabling unauthorized tag mutation in Document Tag Mutation** — `knowledgebase/facade/KnowledgeBaseDocumentFacadeImpl.java` *(conf 9/10)*
  `updateKnowledgeBaseDocumentTags` accepts a `knowledgeBaseDocumentId` with no ownership check.

- **5.1 · IDOR enabling cross-workspace tag write in KnowledgeBaseTagFacade** — `knowledgebase/facade/KnowledgeBaseTagFacadeImpl.java`
  `updateKnowledgeBaseTags` overwrites tags by `knowledgeBaseId` with no workspace check.

- **5.0 · IDOR exposing approval form data via unvalidated UUID token** — `execution/facade/ApprovalFormFacadeImpl.java`
  `GET …-form/{id}` accepts a base64 `JobResumeId` token `tenantId:jobId:uuid`.

- **5.0 · Missing Webhook Verification enabling spoofed trigger in MailchimpSubscribeTrigger** — `mailchimp/trigger/MailchimpSubscribeTrigger.java` *(conf 9/10)*
  Returns `body.getContent()` with no origin/HMAC/secret validation.

- **4.3 · IDOR exposing cross-workspace connection data via Search** — `connection/search/ConnectionSearchAssetProvider.java`
  `search()` calls `getConnections(AUTOMATION)` → `findAll()`, bypassing workspace scoping.

- **4.3 · IDOR allowing tag modification on any Connection object** — `web/rest/ConnectionTagApiController.java`
  `updateConnectionTags` accepts an arbitrary connection `id` with no ownership check.

- **4.3 · IDOR exposing all API collections across workspaces in Search** — `configuration/search/ApiCollectionSearchAssetProvider.java`
  `search()` calls `getApiCollections(null, null, null, null)` with all filters null.

- **4.3 · IDOR exposing cross-workspace deployments via Search Handler** — `configuration/search/ProjectDeploymentSearchAssetProvider.java`
  `search()` calls `getProjectDeployments()` with no workspace filter.

- **4.3 · IDOR enabling cross-user workflow data read in Dynamic Properties API** — `web/rest/WorkflowNodeDynamicPropertiesApiController.java`
  Both endpoints → `getWorkflow(workflowId)` with no ownership check.

- **4.3 · IDOR allowing cross-user workflow data access in WorkflowNodeOutputApiController** — `web/rest/WorkflowNodeOutputApiController.java`
  All three endpoints → `getWorkflow(workflowId)` with no ownership check.

- **4.3 · IDOR enabling cross-workspace tag writes in updateKnowledgeBaseTags** — `knowledgebase/facade/KnowledgeBaseTagFacadeImpl.java`
  `updateKnowledgeBaseTags` passes a caller `knowledgeBaseId` to `updateTags()` with no check.

- **4.3 · IDOR allowing tag modification on any Project resource** — `configuration/facade/ProjectTagFacadeImpl.java`
  `updateProjectTags` overwrites a project's tags with no ownership check.

- **4.3 · IDOR exposing cross-workspace MCP server data via GraphQL query** — `web/graphql/WorkspaceMcpServerGraphQlController.java`
  `workspaceMcpServers` returns all servers for a caller `workspaceId` with no membership check.

- **4.3 · IDOR exposing execution logs via Editor Log Reader** — `web/graphql/EditorLogFileGraphQlController.java`
  Editor-log queries accept an arbitrary `jobId` with no ownership check.

- **4.3 · IDOR allowing tag mutation on any DataTable via GraphQL** — `configuration/service/DataTableTagServiceImpl.java`
  `updateDataTableTags` fetches by caller `tableId` with no workspace check.

- **4.3 · IDOR exposing arbitrary user workspace data in getUserWorkspaces** — `web/rest/WorkspaceApiController.java`
  `GET …/{id}/workspaces` passes a user `id` to `getUserWorkspaces(id)` with no authorization.

- **4.3 · IDOR exposing workflow internals via unscoped Workflow Node Output** — `web/rest/WorkflowNodeOutputApiController.java` *(conf 9/10)*
  Three endpoints fetch a workflow by arbitrary `workflowId` with no access check.

- **4.3 · IDOR enabling unauthorized tag modification in Deployment Tag Update** — `configuration/facade/ProjectDeploymentFacadeImpl.java`
  `updateProjectDeploymentTags(id, tags)` writes with no ownership check.

- **4.3 · IDOR exposing user workspace memberships in getUserWorkspaces** — `web/rest/WorkspaceApiController.java`
  `GET /internal/users/{id}/workspaces` returns a user's memberships for an arbitrary ID.

- **4.3 · IDOR enabling tag tampering on arbitrary projects via updateProjectTags** — `configuration/facade/ProjectTagFacadeImpl.java`
  `PUT …/{id}/tags` updates a project's tags with no ownership check.

- **4.3 · IDOR exposing integration data via GraphQL Query Handler** — `web/graphql/IntegrationGraphQlController.java`
  `integration` query returns the `Integration` object for a caller `id` with no role check.

- **4.3 · IDOR exposing component connections via arbitrary WorkflowId** — `web/graphql/ComponentConnectionGraphQlController.java`
  `clusterElement`/`workflowNodeComponentConnections` pass a caller `workflowId` to the facade with no check.

- **4.3 · IDOR enabling cross-user workflow data read via GraphQL** — `web/graphql/WorkflowNodeParameterGraphQlController.java`
  `clusterElement`/`workflowNodeMissingRequiredProperties` fetch by `workflowId` with no ownership check.

- **4.3 · IDOR exposing workflow internal state in update error response** — `configuration/service/IntegrationInstanceWorkflowServiceImpl.java`
  `update()` formats the whole object (not its ID) into a not-found exception, leaking internal state.

- **4.3 · IDOR exposing cross-user workspace membership in User Workspaces Endpoint** — `web/rest/WorkspaceApiController.java`
  `GET …/{id}/workspaces` returns memberships for an arbitrary user `id`.

- **4.3 · IDOR exposing cross-workspace Git configs in ProjectGitApiController** — `web/rest/ProjectGitApiController.java`
  `getWorkspaceProjectGitConfigurations`/`getProjectGitConfiguration` use a caller ID with no membership check.

- **4.3 · IDOR enabling cross-user data read in ApprovalFormApiController** — `execution/facade/ApprovalFormFacadeImpl.java`
  `GET …-form/{id}` fetches approval form params via a `JobResumeId` decoded from `id`.

- **4.3 · IDOR allowing cross-tenant data read in Trigger Form Handler** — `web/rest/TriggerFormApiController.java`
  `getTriggerForm` returns trigger form config for any deployment via a base64 `WorkflowExecutionId`.

- **4.3 · IDOR allowing tag overwrite on arbitrary Connection resources** — `web/rest/ConnectionTagApiController.java`
  `updateConnectionTags` passes a caller `id` to `update(id, tags)` with no ownership check.

- **4.3 · IDOR exposing cross-user workflow data in IntegrationWorkflowGraphQlController** — `web/graphql/IntegrationWorkflowGraphQlController.java`
  `integrationWorkflowsByIntegrationId` returns all workflows for a caller `integrationId` with no check.

- **4.3 · IDOR enabling tag overwrite on any API Collection** — `web/rest/ApiCollectionTagApiController.java`
  `PUT …/api-collections/{id}/tags` replaces tags for any collection ID.

- **4.3 · IDOR exposing any user workspace list in getUserWorkspaces** — `web/rest/WorkspaceApiController.java`
  `getUserWorkspaces` passes a `Long id` to the facade with no authorization.

- **4.3 · IDOR exposing cross-user workflow metadata in Search Handler** — `configuration/search/WorkflowSearchAssetProvider.java`
  `search()` calls `getLatestProjectWorkflows()` with no scope filter.

- **4.3 · IDOR exposing other users' workspaces in getUserWorkspaces** — `web/rest/WorkspaceApiController.java`
  `GET …/{id}/workspaces` returns a user's list with no owner/admin check.

- **4.3 · IDOR exposing cross-user workspace data in getUserWorkspaces** — `web/rest/WorkspaceApiController.java`
  CE `GET /internal/users/{id}/workspaces` passes an arbitrary `id` to the facade.

- **4.3 · IDOR exposing cross-user workflow data in Node Options API** — `configuration/facade/WorkflowNodeOptionFacadeImpl.java`
  Options endpoints fetch workflow/test config by `workflowId` with no access check.

- **4.3 · IDOR exposing cross-tenant tag data in ProjectDeployment Tag Listing** — `configuration/facade/ProjectDeploymentFacadeImpl.java`
  `GET …-deployments/tags` returns tags aggregated from all deployments with no workspace/tenant filter.

- **4.3 · IDOR exposing cross-tenant workflow schemas in Subflow Options** — `configuration/subflow/SubflowDataSourceImpl.java`
  `SubflowDataSourceImpl` exposes three methods callable by any authenticated user with no ownership/tenant check.

- **4.3 · IDOR allowing unauthorized tag update on Integration Configurations** — `web/rest/IntegrationInstanceConfigurationTagApiController.java`
  `PUT …-instance-configurations/{id}/tags` passes a `Long id` to `update(id, tagIds)` with no ownership check.

- **4.3 · IDOR allowing tag overwrite on arbitrary Integration objects** — `web/rest/IntegrationTagApiController.java`
  `PUT …/{id}/tags` updates an integration's tag associations with no ownership check.

- **4.3 · IDOR exposing cross-workspace data via Search GraphQL Query** — `web/graphql/AutomationSearchGraphQlController.java`
  `automationSearch` calls `AutomationSearchFacadeImpl.search()` which fans out to all `SearchAssetProvider`s with no scoping.

- **4.3 · IDOR exposing cross-user workflow data in Node Description API** — `configuration/facade/WorkflowNodeDescriptionFacadeImpl.java`
  Node-description endpoints → `getWorkflow(workflowId)` with no ownership check.

- **4.3 · IDOR exposing workflow connection metadata in ComponentConnectionFacade** — `web/graphql/ComponentConnectionGraphQlController.java` *(conf 9/10)*
  Component-connection queries fetch workflow data by `workflowId` with no ownership check.

- **4.0 · IDOR exposing cross-tenant AppEvent data in getOptions** — `configuration/domain/AppEvent.java`
  `AppEventTrigger.getOptions` → `getAppEvents()` → `findAll(Sort...)` with no tenant predicate.

- **4.0 · IDOR exposing cross-user connections via getFrontendConnections** — `configuration/facade/ConnectedUserConnectionFacadeImpl.java`
  `getFrontendConnections` fetches each `connectionIds` entry with no ownership check.

- **4.0 · IDOR enabling cross-user data modification in updateConnectedUser** — `web/rest/ConnectedUserApiController.java`
  `PATCH /{externalUserId}` updates a user's profile with no self-check.

- **4.0 · IDOR exposing all conversation IDs in Cassandra Chat Memory** — `cassandra/action/CassandraChatMemoryListConversationsAction.java` *(conf 9/10)*
  `perform` calls `findConversationIds()` with no per-user/tenant predicate.

- **4.0 · IDOR enabling cross-project data read in getTriggerForm** — `web/rest/TriggerFormApiController.java`
  `getTriggerForm` decodes a `WorkflowExecutionId` (sequential `jobPrincipalId`) and returns config for any project.

- **4.0 · IDOR enabling unauthorized tag writes in ApiCollection Tag Update** — `web/rest/ApiCollectionTagApiController.java`
  `PUT …/api-collections/{id}/tags` applies tag updates to any collection by ID.

- **4.0 · IDOR enabling cross-user message leak in Copilot Chat Agent** — `spring/ai/SpringAIAgent.java`
  A single shared `State`/`messages` list per agent type leaks messages across users.

- **4.0 · IDOR exposing cross-workspace Knowledge Base names in Search** — `knowledgebase/search/KnowledgeBaseDocumentSearchAssetProvider.java`
  `search` → `getKnowledgeBases()` → `findAll()` with no workspace filter.

- **4.0 · IDOR exposing all knowledge bases in Search Provider** — `knowledgebase/search/KnowledgeBaseSearchAssetProvider.java`
  `search()` → `getKnowledgeBases()` → `findAll()` with no scoping filter.

---

## 🟢 Low (0.1 – 3.9) — 3 findings

### IDOR / Broken Access Control (1)

- **3.5 · Spoofed Webhook Injection via Missing Origin Validation in UpdatedOrganization Trigger** — `pipedrive/util/PipedriveUtils.java` *(conf 9/10)*
  `webhookRequest` (line 76) returns the `current` field as trigger output without verifying Pipedrive origin.

### Server-Side Request Forgery (1)

- **3.6 · Open Redirect bypass via scheme-only URL in RedirectValidator** — `rest/validator/RedirectValidator.java` *(conf 9/10)*
  `isRelativePath()` misclassifies scheme-only URLs (e.g. `http:evil.com`) as relative because it checks `!url.contains("://")`.

### Cross-Site Scripting / Header Injection (1)

- **3.8 · HTTP Header Injection via Project Name in Export Handler** — `web/rest/ProjectApiController.java`
  `exportProject` concatenates the stored project `name` into the `Content-Disposition` header with no sanitization.

---

*End of report — 455 findings (15 Critical, 268 High, 169 Medium, 3 Low). Counts reconcile with the Gecko dashboard totals.*

