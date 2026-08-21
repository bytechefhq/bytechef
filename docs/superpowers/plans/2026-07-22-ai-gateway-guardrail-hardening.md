# AI Gateway guardrail hardening — implementation plan

Spec: `docs/superpowers/specs/2026-07-22-ai-gateway-guardrail-hardening-design.md`

Phase 1 closes gateway-boundary gaps 1, 2, 3, 5. Ordered so each step compiles on its own.

## Step 1 — Secret redaction (gap 2)

- `AiGatewayGuardrails`: add secret-key `Pattern`s + `redactSecrets(String)`; add
  `globalSecretRedactionEnabled` ctor arg (`secret-redaction-enabled`); compute effective
  `redactSecrets` = global OR workspace `redactSecrets()`; run it after PII in `apply`.
- Files: `AiGatewayGuardrails.java`.

## Step 2 — Injection classifier (gap 3)

- New SPI `AiGatewayInjectionClassifier` in `platform-ai-gateway-api/.../guardrail`.
- New impl `PromptBasedInjectionClassifier` in `automation-ai-gateway-service/.../guardrail`,
  `@ConditionalOnProperty(... name = "injection-model")`, fail-open — mirror
  `PromptBasedModerationClassifier`.
- `AiGatewayGuardrails`: take `@Nullable AiGatewayInjectionClassifier`, add
  `globalInjectionDetectionEnabled` (`injection-detection-enabled`); run the check last in
  `apply`; block with `AiGatewayGuardrailException`.

## Step 3 — Response scanning (gap 1)

- `AiGatewayGuardrails`: add `redactResponseContent(String, workspaceId)` and
  `globalResponseScanEnabled` (`response-scan-enabled`); redaction-only (PII + secrets).
- `AiGatewayFacadeImpl.chatCompletion`: after the response is produced and before
  `processTracingHeaders`, rewrite each choice's message content via a new
  `redactResponse(response, workspaceId)` helper (non-streaming only). Document the streaming
  limitation on the streaming method.

## Step 4 — Embeddings coverage (gap 5)

- `AiGatewayGuardrails`: add `applyToInputs(List<String>, workspaceId)` — redact PII+secrets,
  block on blocked-terms/injection (no moderation).
- `AiGatewayFacadeImpl.embedding`: replace `request.input()` with the guarded list before
  building `EmbeddingRequest`.

## Step 5 — Workspace settings + GraphQL + client (all four toggles)

- `AiGatewayWorkspaceSettings` record: add `redactSecrets`, `injectionDetectionEnabled`,
  `scanResponses`.
- `AiGatewayWorkspaceSettingsServiceImpl`: new keys in `toMap`/`toSettings`.
- Update the two other `new AiGatewayWorkspaceSettings(...)` call sites (GraphQL controller,
  test helper).
- `ai-gateway-workspace-settings.graphqls`: add the three booleans to type + input.
- `AiGatewayWorkspaceSettingsGraphQlController`: add to input record + upsert mapping.
- Client: `AiGatewaySettings.tsx` form + `aiGatewayWorkspaceSettings.graphql` operation;
  regenerate `graphql.ts` via `npx graphql-codegen`.

## Step 6 — Tests + docs + format

- Extend `AiGatewayGuardrailsTest`; add `PromptBasedInjectionClassifierTest`.
- Update CLAUDE.md "AI Gateway content guardrails (EE)" and module README.
- `./gradlew spotlessApply` on the touched modules; compile where the toolchain allows.
- Commit (`732 …` server, `732 client - …` client) and push to
  `claude/bytechef-branch-0-732-i2mjs0`.

## Verification checklist (Phase 1)

- [x] Secret patterns redact and are ReDoS-safe (no nested optional quantifiers).
- [x] Injection classifier fails open; registered only with `injection-model` set.
- [x] Response redaction non-streaming; streaming handled in Phase 2a.
- [x] Embeddings inputs redacted/blocked before upstream call.
- [x] New toggles union global OR workspace, off by default.
- [x] All `new AiGatewayWorkspaceSettings(...)` call sites updated.
- [x] Tests cover each new path.

## Phase 2a — streaming response redaction

- `StreamingResponseRedactor` (guardrail pkg): stateful safe-cut redactor with a bounded lookahead
  window; `push`/`flush`. Uses `AiGatewayGuardrails.sensitiveMatchRanges` (new package-private
  helper over the combined PII+secret pattern list) to avoid emitting across a matched span.
- `AiGatewayGuardrails`: `redactAll` made public; new global flag
  `response-scan-streaming-enabled`; `newStreamingResponseRedactor(workspaceId)` returns a redactor
  only when that flag AND response scanning are both active.
- `AiGatewayFacadeImpl.chatCompletionStreamInternal`: obtain the redactor; when non-null, mask each
  delta via `toStreamChunkResponse` and defer `finish_reason` (`AtomicReference`) onto a
  `concatWith` flush chunk. Null → unchanged. Added `streamChunkOf` helper.
- Tests: `StreamingResponseRedactorTest` (no-leak invariant under arbitrary chunking for windows ≥
  longest token; clean passthrough; null/empty) + `AiGatewayGuardrailsTest` gating cases.
- Note: activation is operator-level (latency trade-off), so no new per-workspace field / GraphQL /
  client change.

## Phase 2b — per-project guardrail scoping (gap 6)

- `AiGatewayProjectSettings` (platform-ai-gateway-api): guardrail-only record, PROJECT-scoped
  `Property` row (`ai_gateway_project_settings`) — no migration.
- `AiGatewayProjectSettingsService` + Impl (mirror the workspace service, `Property.Scope.PROJECT`).
- `AiGatewayProjectSettingsFacade` + Impl (admin-only read+write) + GraphQL
  (`ai-gateway-project-settings.graphqls` + controller). No client UI this phase (API only).
- `AiGatewayGuardrails`: optional `@Nullable AiGatewayProjectSettingsService` dep;
  `resolvePolicy(workspaceId, projectId)` unions the project overrides (additive, same as workspace);
  `projectId` overloads on `apply` / `applyToInputs` / `redactResponse` /
  `newStreamingResponseRedactor` (originals delegate with null → existing callers unchanged).
- `AiGatewayFacadeImpl`: `resolveProjectId(tags)` (project_id tag is a slug → entity → numeric id),
  threaded into all four guardrail call sites (sync + streaming + embeddings + response).
- Tests: `AiGatewayGuardrailsTest` project-overlay cases (enable, blocked-term, workspace∪project,
  response scanning).

## Gap 4 — Datadog/Splunk (documentation, not code)

Not a code gap: OTLP export already reaches Datadog and Splunk (both OTLP-capable), gateway meters
included. Resolved by documenting the concrete wiring in
`docs/content/docs/self-hosting/observability/index.mdx` (added Splunk to the backends table +
a "Datadog and Splunk (direct OTLP)" section). No dependency/build change.

## Follow-up hardening (post-review gaps)

- **Guardrail metrics**: `AiGatewayGuardrailMetrics` → `bytechef_ai_gateway_guardrail{event}`
  counter, recorded at each redact/block/flag point in `AiGatewayGuardrails` (`@Nullable` dep,
  `ObjectProvider<MeterRegistry>`).
- **User docs**: expanded the guardrails section of `docs/content/docs/platform/ai-gateway.md`
  (secret redaction, injection detection, response/streaming scanning, per-project overrides, the
  metric).
- **Facade test**: `AiGatewayFacadeTest` now builds guardrails via a `guardrails(responseScan)`
  helper (fixing the constructor for the new signature) + a response-redaction flow test.

## Per-project client UI

- `aiGatewayProjectSettings.graphql` operation (query + mutation) + regenerated hooks.
- `AiGatewayProjectGuardrailsSection` — the six guardrail toggles + blocked-terms input, embedded
  in the project **edit** dialog (`AiGatewayProjectDialog`, edit mode only), self-contained
  load/save with query invalidation. Verified: tsc + eslint + prettier clean.

## Remaining (not implemented)

- Per-API-key guardrail scoping (gap 6b) — no api-key settings-store primitive yet.
- Response tool-call arguments are not redacted (documented trade-off; avoids corrupting tool
  calls); response-side blocking not added (redaction-only by design).
