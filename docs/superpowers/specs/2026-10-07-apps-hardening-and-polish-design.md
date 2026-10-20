# Apps — Server Hardening and SDK/Template Polish (design)

Follow-ups to sub-projects A, A4, B and C of [Apps](2026-10-06-apps-governed-ai-built-apps-design.md), approved 2026-10-07. Items 7 (token-refresh race) and 8 (Component Rules for app calls) get their own designs.

## Server hardening (items 1-6, 9)

1. Retention across tenants. AppWorkflowRunRetentionJob and ToolInvocationLogRetentionJob iterate tenantService.getTenantIds(), each inside TenantContext.runWithTenantId(...); a tenant's failure is logged WARN and doesn't stop the rest (JobRetentionMonitor's pattern). Unit tests with two tenants, the first one throwing.

2. Proxy in-flight cap. Add a generic `maxResponseBytes(long)` option to the component SDK's `Http.Configuration` builder. HttpClientExecutor wraps the BINARY InputStream in a counting stream that stops past the limit and closes the upstream; the client deletes the stored spool and throws a specific exception type, so no temp file survives; the proxy sets it and maps the abort to the existing APP_PROXY_RESPONSE_TOO_LARGE (502). Existing read-back check stays as backstop. Separately, ProxyStorageException gets a handler: new code 123 APP_PROXY_STORAGE_FAILED (500, typed body) — add to both AppErrorType constants and the NAMES map.

3. Dedicated app executor. A fifth executor `appExecutor` built through AsyncConfiguration.createExecutor with limit `bytechef.app.execution.concurrency-limit` (yml default 10, no Java initializer); AppExecutionFacadeImpl moves executeAction/executeTool/proxy from syncWorkerExecutor to it. AsyncConfigurationTest's two hand-written executor lists and the `assertEquals(4, ...)` count updated. CLAUDE.md's "Four executors" paragraph becomes five, with the reason (apps must not starve the editor's Test button / API-Platform sync lane). Workflow runs (AppWorkflowExecutionFacadeImpl) unchanged.

4. Resolver rejects system deployments. AppCallableWorkflowResolver.resolve() rejects deployments whose project_deployment.name starts with SystemProjects.API_COLLECTION_DEPLOYMENT_NAME_PREFIX / MCP_SERVER_DEPLOYMENT_NAME_PREFIX / A2A_SERVER_DEPLOYMENT_NAME_PREFIX, and whose project is a system project (SystemProjects.isSystemProjectName or the __EMBEDDED__ prefix) — the same set the binding picker hides. Bind validates through resolve(), so binding is closed too. One test per prefix.

5. Per-definition schema failure. In AppExecutionFacadeImpl.getActions() and getTools(), catch RuntimeException per definition, fall back to {"type":"object"}, log WARN naming component + action/tool (add an slf4j Logger).

6. updateApp audit. Append APP_UPDATED at the END of AppAuditEvent; publish from AppFacadeImpl.updateApp with payload `changedFields` (list of field names) plus new values of `name`, `visibility`, `hostingTarget` when changed. AppFacadeTest asserts it; the event list in .agents/apps.md and the Apps spec docs/superpowers/specs/2026-10-06-apps-governed-ai-built-apps-design.md gets the entry.

9. Tool invocation log authorization. Split by audience; new ToolInvocationLogFacade (interface + impl, @PreAuthorize on the facade, controller unannotated and calls only the facade; facade calls the service, never repositories):
   - NEW query `workspaceToolInvocationLogs(workspaceId, environmentId, ...existing filters)` guarded `hasWorkspaceScopeInEnvironmentId(#workspaceId, 'EXECUTION_VIEW', #environmentId)`; repository gains workspace_id + environment predicates (service method with workspaceId/environment). Client: automation Tool Invocations page (useToolInvocations via AutomationToolInvocations) and App Activity tab switch to it, passing currentWorkspaceId/currentEnvironmentId.
   - EXISTING `toolInvocationLogs` becomes `isTenantAdmin()`; callers: embedded Tool Invocations page and Connected User sheet panel.
   - Tests: guard tests for both (@GraphQlTest per memory: controller tests are WebMvc/GraphQl IntTests, never plain controller unit tests; facade guard tests as the repo does them), and a repository IntTest proving another workspace's row is invisible.
   - Client changes: new .graphql operation, regenerate `src/shared/middleware/graphql.ts` with `npx graphql-codegen`, commit operations and generated file separately per CLAUDE.md; client commits use `client - ` subject style.

## SDK and template polish (items 11-16)

11. SDK robustness (sdks/frontend/automation/app/library/src/core): empty 2xx on an endpoint that must return a body -> BytechefAppError('INVALID_RESPONSE') not bare TypeError; invalid base64 -> INVALID_RESPONSE; proxy result bytes() returns a copy each call and text()/json() are computed lazily on first use; proxy body accepts ArrayBuffer and any ArrayBufferView (send as bytes), ProxyBody type widened; executeByName concurrent cold calls share one in-flight GET /tools while each caller still honours its own abort signal; add a test for the workflows poll TIMEOUT path.

12. Library housekeeping: vite.config.ts typechecked + linted via tsconfig.node.json (currently referenced by nothing); remove library/.npmignore (`files` governs; verify npm pack output unchanged); fix the stale workspace .prettierignore comment.

13. Template formatting: prettier check over our template files (src/server, src/routes, src/components/overview.tsx, scripts, and modified starter files vite.config.ts, router.tsx, styles.css) inside check:template and the workspace format script; generated files (routeTree.gen.ts, components/ui/*, package-lock.json) stay excluded.

14. Devtools: docs only. Correct .agents/apps.md:468 — the devtools event bus binds localhost:4206 (not 42069), only in development, falls back to an OS-assigned port on collision, so two dev servers don't fail.

15. bytechef:status: drop `--env-file-if-exists`; the script reads .env.local itself with util.parseEnv like check-bundle, printing its own clear message when absent (no Node "not found. Continuing" stderr noise). A variable already present in the environment wins over `.env.local`, as `--env-file-if-exists` gave and the template spec states.

16. Scaffolder: `@acme/my-app` -> directory `my-app`, package name `@acme/my-app`; `--url` must parse as http(s); CR/LF in `--url` or app key rejected; the npm `--` hint fires only when npm_config_url/npm_config_install is non-empty AND the flag did not reach the CLI itself.
