<!--
Sources — competitor documentation used to scope this ticket:
  Langfuse:
    - https://github.com/langfuse/langfuse
    - https://langfuse.com/docs/evaluation/overview
    - https://langfuse.com/docs/evaluation/evaluation-methods/llm-as-a-judge
    - https://langfuse.com/docs/evaluation/experiments/experiments-via-ui
    - https://langfuse.com/docs/prompt-management
    - https://langfuse.com/docs/tracing
  Helicone:
    - https://github.com/helicone/helicone
    - https://docs.helicone.ai/features/advanced-usage/scores
    - https://docs.helicone.ai/features/experiments
    - https://docs.helicone.ai/features/advanced-usage/llm-security
    - https://docs.helicone.ai/features/advanced-usage/caching
  Latitude:
    - https://github.com/latitude-dev/latitude-llm
    - https://docs.latitude.so/guides/evaluations/overview
    - https://docs.latitude.so/guides/evaluations/running-evaluations
-->

# ByteChef AI Gateway — Gap Analysis & Spec Bundle

**Date:** 2026-04-21
**Status:** Draft (for review)
**Owner:** Ivica Cardic
**Ticket:** TBD
**Edition:** `bytechef.edition=ee` unless otherwise noted.

## 1. Summary

This ticket closes the feature gap between ByteChef's `ai-gateway-app` and the three reference
open-source LLM platforms (Langfuse, Helicone, Latitude). It contains:

1. A ranked list of **17 gaps** (§5) — every feature found in at least one comparator that ByteChef
   does not ship, scored by customer impact and implementation cost.
2. Three **detailed sub-specs** sized to each spawn their own implementation plan:
   - **Spec A** — OTel-native trace ingestion endpoint (§6)
   - **Spec B** — External Scores ingestion API (§7)
   - **Spec C** — Datasets + Experiments framework (§8)

The three sub-specs are chosen because each unblocks a distinct customer segment: OTel for
polyglot engineering teams, Scores API for teams with an existing evaluator stack (RAGAS /
LangSmith / internal), and Datasets + Experiments for teams doing prompt regression testing.
They are independent — any two can ship without the third — but the metric schema introduced
in Spec A is reused by Specs B and C.

## 2. Baseline — What the AI Gateway Already Ships

<!-- Verified from the repo at time of writing — not claims from external sources. -->

- **Routing** — 7 strategies (simple, priority-fallback, weighted, cost-optimized,
  latency-optimized, intelligent, tag-based) across 8 providers (OpenAI, Anthropic, Azure,
  Cohere, DeepSeek, Google Gemini, Groq, Mistral). `AiGatewayRouter`,
  `AiGatewayRoutingStrategy`, `AiGatewayModelDeployment`.
- **API surface** — OpenAI-compatible `/api/ai-gateway/v1/chat/completions` + embeddings;
  streaming (SSE), tool calls, vision, multimodal. `AiGatewayChatCompletionApiController`.
- **Observability** — trace → span → session model with status/level hierarchy;
  `AiObservabilityTrace`, `AiObservabilitySpan`, `AiObservabilitySession`. Alert rules,
  notification channels, scheduled export, retention cleanup.
- **Prompt management** — `AiPrompt` + `AiPromptVersion` (DRAFT / PUBLISHED), variable
  extraction via `PromptVariableExtractor`, project-scoped registry.
- **Evaluations (live-trace only)** — `AiEvalRule` + `AiEvalExecution` + `AiEvalScore` +
  `AiEvalScoreConfig`. Async executor runs LLM-as-judge against incoming spans.
  No dataset/experiment layer.
- **Caching** — exact-match SHA-256 keyed (not semantic). Per-request `cache: boolean` flag.
- **Rate limiting** — TOKEN_COUNT / REQUEST_COUNT / COST × PER_USER / PER_TENANT /
  PER_API_KEY / PER_MODEL / GLOBAL, sliding windows.
- **Cost governance** — per-model pricing, `AiGatewayBudget` (SOFT / HARD enforcement,
  DAILY / WEEKLY / MONTHLY / YEARLY periods, alert thresholds), `AiGatewaySpendSummary`.
- **Auth / multi-tenancy** — `AiGatewayApiKeyAuthenticationProvider`, workspace-scoped
  everything, BYOK per `AiGatewayProvider.apiKey`.
- **Guardrails (component-level)** — `Guardrails v2` (see `2026-04-16-guardrails-v2-design.md`)
  ships nine detector types as cluster elements for workflow authors to compose. **Does not**
  apply at the gateway request path; gateway-level policy remains a gap.

## 3. Eval Execution Semantics — Verified

One dimension is frequently misrepresented in marketing and matters for §8, so it is called out
explicitly:

| Tool          | Live-trace evals (async post-ingest)                  | Versioned-dataset evals (offline / batch)                 |
|---------------|-------------------------------------------------------|-----------------------------------------------------------|
| **Langfuse**  | ✅ `LLM-as-a-Judge` on Observations / Traces           | ✅ **Experiments** — runs against latest dataset version   |
| **Helicone**  | ❌ "Not an evaluation framework" (Scores API only)     | ❌ **Experiments deprecated 2025-09-01**, no replacement   |
| **Latitude**  | ✅ **Live Mode** (per-evaluation toggle)               | ✅ **Batch Mode** on Datasets (regression / prompt A/B)    |
| **ByteChef**  | ✅ `AiEvalRule` on spans                               | ❌ **Not shipped**                                         |

ByteChef already owns the live-trace half; the missing half is Datasets + Experiments (§8).
Nobody runs evaluators synchronously inline — even live mode is async post-ingest — because judge
latency (seconds+) would kill the proxy SLA. Any design in this spec preserves that constraint.

## 4. Comparator Snapshot (one-line positioning)

- **Langfuse** — observability + prompt registry + evals, **no proxy**. OTel-native ingest;
  Python + TS/JS SDKs. Self-hosted: web + worker + Postgres + ClickHouse.
- **Helicone** — proxy-first gateway (base-URL swap), caching + rate-limit + header-driven
  LLM security. Evals deferred to external tools via Scores API. Self-hosted: web + worker
  + Postgres + ClickHouse + **MinIO/S3** + Cloudflare Workers.
- **Latitude** — full prompt engineering platform: PromptL DSL, agent runtime with subagents,
  evals (live + batch), experiments, GEPA optimizer. Self-hosted: Next.js + API + ClickHouse
  + Weaviate + Temporal + Redis.

## 5. Ranked Gap List

Priority legend — **P0:** blocks external adoption or compliance; **P1:** meaningful customer
pull; **P2:** nice-to-have / differentiator. "Spec here" = covered in §6–§8 of this document.

### Tier 1 — Integration & Security (P0)

| # | Gap                                      | Comparator(s)                | Disposition                                  |
|---|------------------------------------------|------------------------------|----------------------------------------------|
| 1 | **OTel-native trace ingestion**          | Langfuse, Latitude           | **Spec here — §6**                           |
| 2 | **External Scores ingestion API**        | Helicone, Langfuse           | **Spec here — §7**                           |
| 3 | **Gateway-level guardrails**             | Helicone (Prompt Guard, Llama Guard, Moderation) | Separate ticket — not the component-level `Guardrails v2` |
| 4 | **Data masking / payload omission**      | Helicone (omit headers), Langfuse (masking) | Separate ticket                      |
| 5 | **Polyglot SDKs (Python / TS / JS)**     | Langfuse, Latitude           | Follow-up — OTel (§6) mitigates in the short term |

### Tier 2 — Prompt Engineering UX (P1)

| #  | Gap                                            | Comparator(s)             | Disposition                            |
|----|------------------------------------------------|---------------------------|----------------------------------------|
| 6  | **Datasets + Experiments (versioned evals)**  | Langfuse, Latitude        | **Spec here — §8**                    |
| 7  | **Prompt environments / labels**              | Langfuse, Helicone        | Separate ticket — extends `AiPromptVersion` |
| 8  | **Gateway-side prompt rendering (`prompt_id + inputs`)** | Helicone, Latitude | Separate ticket — small; extends chat-completion controller |
| 9  | **Prompt A/B testing primitive**              | Langfuse                  | Follow-up — requires #7 first         |
| 10 | **Prompt composition / snippets**             | Langfuse, Latitude (PromptL) | Follow-up — requires `AiPrompt` model extension |
| 11 | **Playground / prompt IDE**                   | Langfuse, Latitude        | Separate ticket — UI scope, large     |

### Tier 3 — Advanced & Differentiating (P2)

| #  | Gap                                   | Comparator(s)        | Disposition                              |
|----|---------------------------------------|----------------------|------------------------------------------|
| 12 | **Semantic cache (vector similarity)** | Neither (all do exact-match); table-stakes for support-bots | Follow-up |
| 13 | **Per-request webhooks (HMAC-signed)** | Helicone             | Follow-up — thin layer over existing alert infra |
| 14 | **Issue clustering / failure-mode discovery** | Latitude      | Follow-up — requires embedding infra     |
| 15 | **Prompt optimizer (GEPA-style)**     | Latitude             | Deferred — depends on §8                 |
| 16 | **Payload offloading to S3**          | Helicone, Langfuse   | Follow-up — storage-cost optimization    |
| 17 | **Human-in-the-loop annotation queues** | Latitude, Langfuse | Follow-up                                |

### Out of Scope (explicit non-goals)

- **Agent runtime inside the gateway** — Latitude bundles this; ByteChef splits it into
  `server/ee/libs/ai/ai-agent/` + `ai-copilot-app` calling the gateway as a client. Keep the
  separation.
- **Built-in vector DB** — the knowledge-base module (`automation-knowledge-base`) owns this;
  not the gateway's concern.
- **Git-backed prompt storage** — none of the comparators do this credibly; `AiPromptVersion`
  is sufficient.

## 6. Spec A — OTel-Native Trace Ingestion

<!-- Source pattern: Langfuse OTel endpoint https://langfuse.com/docs/opentelemetry -->

### 6.1 Goal

Accept OpenTelemetry OTLP traffic (Protobuf over gRPC + HTTP/JSON) and map OTel spans onto
`AiObservabilityTrace` / `AiObservabilitySpan` without requiring customers to write a
ByteChef-specific SDK. Unblocks Go / Rust / JVM / C# teams who already instrument with OTel.

### 6.2 Endpoint

- **Path:** `POST /api/ai-gateway/v1/otlp/traces` (HTTP/JSON + HTTP/Protobuf).
- **gRPC:** `:4317/opentelemetry.proto.collector.trace.v1.TraceService/Export`.
  Optional phase-2; HTTP covers 90% of SDKs and avoids a second listener.
- **Auth:** existing `AiGatewayApiKeyAuthenticationProvider` — key passed as `Authorization:
  Bearer <key>` header (HTTP) or `api-key` metadata (gRPC). Workspace resolved from key.

### 6.3 Span Mapping

Adopt the **Semantic Conventions for GenAI** (OTel working group — `gen_ai.*` attributes):

| OTel attribute                         | `AiObservabilitySpan` field            |
|----------------------------------------|----------------------------------------|
| `gen_ai.system`                        | `provider`                             |
| `gen_ai.request.model`                 | `model` (requested)                    |
| `gen_ai.response.model`                | `model` (actual, after routing)        |
| `gen_ai.usage.input_tokens`            | `inputTokens`                          |
| `gen_ai.usage.output_tokens`           | `outputTokens`                         |
| `gen_ai.request.temperature`           | `metadata.temperature`                 |
| trace_id / span_id / parent_span_id    | native trace hierarchy                 |
| `status` / `status.code`               | `status` (SUCCESS / FAILED)            |
| `duration`                             | `latencyMs`                            |
| span kind + attributes                 | `type` (CHAT_COMPLETION / EMBEDDING / …) |
| `gen_ai.prompt` / `.completion` events | `input` / `output`                     |
| resource attributes                    | `metadata` (JSON)                      |
| trace-level `session.id` (custom)      | `AiObservabilitySession.id`            |

Cost is computed server-side from token counts × `AiGatewayModel.{input,output}CostPerMTokens`
so inbound traces carry accurate cost regardless of SDK.

### 6.4 Module Layout

- **New:** `server/libs/platform/platform-ai-otlp/` (api + service submodules). OTLP
  Protobuf generation via the `build.buf.build/opentelemetry/opentelemetry-proto` artifact;
  no hand-rolled schemas.
- **New controller:** `AiGatewayOtlpController` in `automation-ai-gateway-rest`.
- **`AutoConfiguration` class** registered via
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  per the repo's Spring Data JDBC module pattern (see CLAUDE.md "New Spring Data JDBC Modules").

### 6.5 Scale & Back-Pressure

- Protobuf parsing is cheap; LLM-as-judge evaluators (§8) run async so no synchronous cost on
  the ingest path.
- Apply the existing `PER_API_KEY` rate limiter to OTLP as `REQUEST_COUNT` with a generous
  default (e.g. 10k spans/min). Traces exceeding the limit return `429` with `Retry-After`,
  matching the existing gateway semantics.

### 6.6 Out of Scope (Spec A)

- OTel **metrics** / **logs** ingest — traces only in phase 1.
- Sampling configuration on the server side — clients control via `OTEL_TRACES_SAMPLER`.
- Automatic prompt-version correlation from OTel attributes — can ride on §8.

## 7. Spec B — External Scores Ingestion API

<!-- Source pattern: Helicone https://docs.helicone.ai/features/advanced-usage/scores -->

### 7.1 Goal

Let external evaluators (RAGAS, LangSmith, DeepEval, internal judge microservices) post scores
back to ByteChef traces. Complements — does not replace — the existing `AiEvalRule`
live-judge path.

### 7.2 Endpoint

```
POST /api/ai-gateway/v1/traces/{traceId}/scores
POST /api/ai-gateway/v1/spans/{spanId}/scores
```

Request body:

```json
{
  "name": "faithfulness",
  "value": 0.87,
  "dataType": "NUMERIC",
  "comment": "optional human-readable note",
  "source": "ragas@0.2.3",
  "metadata": { "evaluator_version": "0.2.3", "run_id": "..." }
}
```

- `dataType` — `NUMERIC | BOOLEAN | TEXT` (matches `AiEvalScoreConfig` data types;
  persist as INT ordinal).
- `value` — JSON scalar; server validates against `dataType`.
- `source` — free-text identifier of the external evaluator; indexed for filtering.
- Batch variant: `POST /api/ai-gateway/v1/scores/batch` — array of the above with `traceId`
  or `spanId` per row. Same 1000-entry cap used by OTel export (keeps payload sane).

### 7.3 Persistence

Reuse `AiEvalScore` with a new `source` field (`INTERNAL` ordinal for existing
`AiEvalRule` output, `EXTERNAL` for scores from this API, append-only per the
`feedback_enum_storage` convention). Existing aggregations in `AiGatewaySpendGraphQlController`
patterns extend to score rollups without a parallel table.

### 7.4 Auth

- `Authorization: Bearer <api-key>`; workspace derived from the key.
- The target `traceId` / `spanId` must belong to the same workspace. Cross-workspace writes
  return `403` — not `404` — to surface the boundary.

### 7.5 Audit

Every score write emits a `platform-audit` event (type `ai.score.recorded`) so scored
production traffic is attributable. Reuse existing `ConnectionAuditAspect` pattern —
do not introduce a parallel audit pipeline.

### 7.6 Metrics

- `bytechef_ai_score_recorded{source, data_type, workspace}` — counter.
- `bytechef_ai_score_value{name}` — distribution summary (for numeric scores, bucketed).

Wired via `ObjectProvider<MeterRegistry>` per the established pattern.

### 7.7 Out of Scope (Spec B)

- Rendering scores in a customer dashboard — follow-up UI ticket.
- Alerting on score threshold breaches — extend existing alert rules in a follow-up.

## 8. Spec C — Datasets + Experiments Framework

<!-- Source pattern: Langfuse Experiments + Latitude Batch Mode -->

### 8.1 Goal

Let teams (a) curate datasets from production traces, (b) version them, and (c) run
evaluators over a dataset to measure regressions when prompt versions / models change.

Closes the versioned-eval half of §3. Complements the existing live-trace evaluators in
`AiEvalRule` — same evaluators, different target.

### 8.2 Data Model

```
ai_dataset
├── id              UUID
├── workspace_id    UUID
├── project_id      UUID NULL
├── name            TEXT
├── description     TEXT
├── created_at      TIMESTAMPTZ
├── archived_at     TIMESTAMPTZ NULL
└── tags            TEXT[]

ai_dataset_version
├── id              UUID
├── dataset_id      UUID
├── version_number  INT              (monotonic per dataset)
├── created_at      TIMESTAMPTZ
├── label           TEXT NULL        (e.g. "golden", "smoke-v2")
└── frozen          BOOLEAN          (immutable once set)

ai_dataset_item
├── id              UUID
├── dataset_id      UUID
├── dataset_version_id UUID
├── input           JSONB            (chat messages, tool context, …)
├── expected_output JSONB NULL       (golden reference for regression tests)
├── metadata        JSONB
├── source_trace_id UUID NULL        (provenance — trace this row was promoted from)
└── created_at      TIMESTAMPTZ
```

Item-to-version is many-to-one; a new version freezes a snapshot of all items at the time of
version creation (copy-on-freeze, not shared rows — keeps experiments reproducible).

```
ai_experiment
├── id              UUID
├── workspace_id    UUID
├── dataset_version_id UUID
├── prompt_version_id UUID NULL      (what was tested)
├── model           TEXT
├── status          INT              (PENDING=0, RUNNING=1, COMPLETED=2, FAILED=3)
├── created_by      UUID
├── started_at      TIMESTAMPTZ NULL
├── completed_at    TIMESTAMPTZ NULL
└── metadata        JSONB

ai_experiment_run
├── id              UUID
├── experiment_id   UUID
├── dataset_item_id UUID
├── trace_id        UUID             (links to synthetic trace recorded for the run)
├── status          INT
├── latency_ms      INT NULL
├── cost            NUMERIC(20,6) NULL
└── created_at      TIMESTAMPTZ
```

All enums persist as INT ordinals; append-only per `feedback_enum_storage`.

### 8.3 Dataset Ingest Paths

1. **Promote from trace** — `POST /ai-gateway/v1/datasets/{id}/items/from-trace`, body
   `{ traceId, expectedOutput?, metadata? }`. Copies inputs (and optionally outputs as
   expected reference). Provenance recorded in `source_trace_id`.
2. **CSV / JSONL upload** — `POST /ai-gateway/v1/datasets/{id}/items/bulk`.
3. **Programmatic** — single-item `POST`.

All ingest paths are versioned: if the target dataset version is `frozen`, a new unfrozen
version is auto-created before the insert.

### 8.4 Experiment Execution

- **Trigger:** `POST /ai-gateway/v1/experiments`, body
  `{ datasetVersionId, promptVersionId, model, evaluatorIds: [...] }`.
- **Execution:** `atlas-coordinator` dispatches one task per dataset item to `atlas-worker`,
  same pattern as live workflow execution. Each run generates a synthetic trace in the
  gateway (replays through the same request path — routing, cost, tokens all real).
- **Scoring:** matching live `AiEvalRule` evaluators run on each generated trace
  post-completion; scores land in `AiEvalScore` linked to `experiment_run.trace_id`.
  External scorers can also target experiment traces via Spec B (§7).

### 8.5 Comparison Surface

- GraphQL query: `experimentComparison(experimentIds: [ID!]!)` — returns per-item
  diffs (output A vs output B, score A vs score B), aggregate score deltas,
  cost/latency deltas.
- Typical flow: run Experiment X on prompt v1, Experiment Y on prompt v2 against the same
  dataset version, compare. Regression test in CI = "no score dropped more than N%".

### 8.6 Module Layout

- **New:** `server/libs/platform/platform-ai-dataset/` and `platform-ai-experiment/`
  (api + service + rest submodules each).
- **New Liquibase changelog** under `server/libs/config/liquibase-config/`.
- **EE remote-client stubs** in matching `*-remote-client` modules per
  `@ConditionalOnEEVersion` pattern (required — EE microservices fail to start without).

### 8.7 Out of Scope (Spec C)

- **GEPA-style prompt optimizer** (Latitude) — requires this spec but is its own track.
- **Human annotation queues** — deferred; a dataset with no `expected_output` is the
  precondition but the labeling UI is separate.
- **Automated dataset synthesis from traces** — a clustering / sampling job that picks
  "interesting" traces is future work.

## 9. Cross-Cutting Concerns

- **Metrics:** every new feature ships at least one `bytechef_<feature>_*` counter via
  `ObjectProvider<MeterRegistry>`. No feature ships without telemetry.
- **Remote-client stubs:** every new SPI added to a platform module requires a
  `@Component @ConditionalOnEEVersion` stub in the matching `*-remote-client` module —
  see CLAUDE.md "EE Microservice Remote Client Pattern".
- **EE license + Javadoc:** every file under `server/ee/` gets the ByteChef Enterprise
  license header (not Apache 2.0) and the `@version ee` Javadoc tag.
- **GraphQL conventions:** SCREAMING_SNAKE_CASE enums, `@PreAuthorize` by default;
  orphan-recovery exception pattern (`demoteConnectionToPrivate`) for resource owners.
- **Enum storage:** all new enums persist as INT ordinals, append-only — per the
  `feedback_enum_storage` memory.

## 10. Sequencing (Proposed)

Ordered by customer unblock / dependency:

1. **Spec A — OTel ingestion (§6).** Unblocks polyglot teams immediately; no dependencies.
2. **Spec B — External Scores API (§7).** Small surface; unblocks customers already running
   external evaluators.
3. **Spec C — Datasets + Experiments (§8).** Largest scope; depends on nothing in §6/§7
   but reuses their schema conventions. Should ship third so the Spec B endpoint can be
   exercised against experiment traces before general availability.

Remaining Tier 1 items (Guardrails at gateway level, Data Masking, Polyglot SDKs) spawn
their own tickets — they are independent of this bundle.

## 11. Out of Scope (Explicit — Whole Bundle)

- Agent runtime inside the gateway (Latitude pattern) — ByteChef's separation stays.
- Built-in vector DB — `automation-knowledge-base` owns this.
- Semantic caching — follow-up.
- Client-side SDKs — OTel (§6) covers the polyglot gap short-term.
- Prompt playground / IDE — UI scope, separate ticket.

## 12. Open Questions

1. **Cost attribution on OTLP ingest (§6.3)** — trust client-reported token counts or
   always recompute? Recomputation is correct when the gateway routed the call; trust is
   correct when an external model (not routed by us) emitted the span. Needs a heuristic
   (e.g. "trust only if `gen_ai.system` is unknown to us").
2. **Spec B cross-workspace semantics** — `403` vs `404` on cross-workspace score writes.
   Leaning 403 to expose the boundary, but 404 is the pedantic answer. Pick before shipping.
3. **Experiment run storage (§8.4)** — do experiment-run traces count against the
   customer's observability retention quota? Leaning no (separate pool), but that doubles
   the trace table footprint.
4. **Dataset version freezing (§8.2)** — is `frozen` a user-set flag at creation or
   auto-set when an experiment references the version? Leaning auto-set + manual override.
5. **Reuse of `AiEvalRule` for experiment scoring (§8.4)** — does the same evaluator
   configuration apply seamlessly to replayed traces, or does experiment scoring need a
   distinct `AiEvalRule.target = EXPERIMENT_TRACE` discriminator?
6. **Ordering vs `Guardrails v2`** — does gateway-level guardrails (Tier 1 #3) ride on the
   component-level detectors shipped by `2026-04-16-guardrails-v2-design.md` (reuse the
   detector classes as gateway advisors), or does it pull in a new stack (Prompt Guard /
   Llama Guard bundled weights)? Material cost difference.

## 13. Related ByteChef Specs

- `2026-04-11-ai-gateway-observability-platform-design.md` — trace/span/session model
  that §6 extends.
- `2026-04-16-guardrails-v2-design.md` — component-level detectors; orthogonal to Tier 1 #3
  (gateway-level policy).
- `2026-04-21-enterprise-features-spec.md` — Epic 4 (AI Model Control) covers org-level
  model allow/deny lists, BYOK, proxy routing; does not overlap with this spec's Tier 1/2.
- `2026-04-21-api-reference-design.md` — token-auth + docs infrastructure; §6 / §7 / §8
  endpoints will be documented using the template landed there.

## 14. References

<!-- Competitor documentation cited throughout this spec — repeated here for discoverability. -->

### Langfuse

- Repo: https://github.com/langfuse/langfuse
- Evaluation overview: https://langfuse.com/docs/evaluation/overview
- LLM-as-Judge: https://langfuse.com/docs/evaluation/evaluation-methods/llm-as-a-judge
- Experiments: https://langfuse.com/docs/evaluation/experiments/experiments-via-ui
- OpenTelemetry ingest: https://langfuse.com/docs/opentelemetry
- Prompt management: https://langfuse.com/docs/prompt-management

### Helicone

- Repo: https://github.com/helicone/helicone
- Scores API: https://docs.helicone.ai/features/advanced-usage/scores
- Experiments (deprecated): https://docs.helicone.ai/features/experiments
- LLM security: https://docs.helicone.ai/features/advanced-usage/llm-security
- Caching: https://docs.helicone.ai/features/advanced-usage/caching

### Latitude

- Repo: https://github.com/latitude-dev/latitude-llm
- Evaluations overview: https://docs.latitude.so/guides/evaluations/overview
- Running evaluations: https://docs.latitude.so/guides/evaluations/running-evaluations
