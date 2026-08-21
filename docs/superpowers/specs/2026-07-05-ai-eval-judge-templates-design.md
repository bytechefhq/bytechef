# AI Eval Judge Templates + Retrieval Context — Design

**Date:** 2026-07-05
**Branch:** `ai-eval-judge-templates` (off `0_732`)
**Status:** Draft for review

## Summary

Add a catalog of 8 built-in LLM-as-a-judge evaluator templates to the eval/gateway
trace-scoring system (parity with Langfuse's default model-based evaluators), and enable
the 3 context-dependent templates by capturing retrieved RAG context at OTLP ingestion and
exposing it to judge prompts via a new `{{context}}` variable.

This is scoped to the **eval/gateway side** only (trace scoring). It does **not** touch the
agent-scenario eval system (`platform-ai-agent-eval`), and it does **not** unify the two
systems — see `project_ai_eval_vs_agent_eval_kept_separate` for that decision. Templates are
static built-ins (shipped as code), not a new DB entity.

## Background

`AiEvalExecutor` (in `automation-ai-gateway-service`) samples live observability traces,
renders a rule's `promptTemplate`, calls the configured model, and parses the response into an
`AiEvalScore` per the rule's `AiEvalScoreConfig`. Today `buildPrompt` substitutes only three
variables from the trace — `{{input}}`, `{{output}}`, `{{metadata}}` — and there is **no
representation of retrieved RAG context anywhere in the observability data model**:

- `AiObservabilitySpanType` = `GENERATION, SPAN, EVENT, TOOL_CALL` (no `RETRIEVAL`).
- `AiObservabilityOtlpIngestFacadeImpl.persistSpan` hardcodes every ingested span to
  `GENERATION`; no other span type is ever assigned.

Langfuse ships 8 default judge templates: Correctness, Conciseness, Helpfulness, Relevance,
Toxicity (input/output only) + Hallucination, Context-relevance, Context-correctness (require
retrieved context). The last three cannot be honestly expressed until retrieved context is
captured — hence the ingestion work below.

## Goals

1. Ship 8 built-in judge templates the UI can offer; picking one pre-fills a new editable
   `AiEvalRule` + `AiEvalScoreConfig`.
2. Capture retrieved RAG context at OTLP ingestion so context-dependent judges are valid.
3. Expose retrieved context to judge prompts via `{{context}}`.

## Non-Goals

- No unification of agent-eval and eval systems.
- No new template CRUD/entity (templates are static built-ins — representation "A").
- No change to the agent-scenario judge framework.
- No backfill of retrieval context for traces ingested before this change.

## Design

### Part 1 — Capture retrieval context at ingestion

**1a. Add `RETRIEVAL` span type.**
Append `RETRIEVAL` to `AiObservabilitySpanType` (after `TOOL_CALL`). The enum is persisted as
an INT ordinal; appending at the end preserves existing ordinals (per the append-only enum
rule). No migration needed for existing rows.

**1b. Detect retriever spans at ingestion.**
`OtelGenAiSpan` already carries an `OtelSpanAttributes attributes` map and a `readStringAttr`
helper. Add an accessor `spanKindAttr()` that reads the OpenInference semantic-convention
attribute `openinference.span.kind` (values include `RETRIEVER`, `LLM`, `TOOL`, `CHAIN`, …).
In `AiObservabilityOtlpIngestFacadeImpl.persistSpan`, replace the hardcoded
`AiObservabilitySpanType.GENERATION` with a mapping:

- `openinference.span.kind == "RETRIEVER"` → `RETRIEVAL`
- otherwise → `GENERATION` (unchanged default; keeps all current behavior identical)

Only `RETRIEVER` is mapped in this change — `TOOL`/`CHAIN`/etc. stay `GENERATION` to keep the
blast radius minimal and avoid re-typing existing generation spans.

**1c. Store the retrieved documents.**
For a `RETRIEVAL` span, populate the span's existing `output` field with the retrieved
documents (no new column). Source, in priority order:
1. OpenInference `retrieval.documents.{i}.document.content` attributes, joined into a single
   text block; else
2. the span's `outputBody` (fallback for exporters that put the documents in the span body).

This mirrors Langfuse's model (retriever span output == retrieved context) and reuses the
existing `output` column, so the only schema change in Part 1 is the enum append.

### Part 2 — Expose `{{context}}` to judge prompts

- Inject `AiObservabilitySpanService` into `AiEvalExecutor`.
- Change `buildPrompt(promptTemplate, trace)` → `buildPrompt(promptTemplate, trace, spans)`.
- Add substitution: `{{context}}` = the `output` of the trace's `RETRIEVAL` spans
  (`getSpansByTrace(traceId)` filtered to `type == RETRIEVAL`), concatenated with a separator.
  When there are no retrieval spans, `{{context}}` substitutes to an empty string — the
  context templates then evaluate against "no context provided" rather than fabricating one
  (no silent-lying).
- Existing `{{input}}`/`{{output}}`/`{{metadata}}` substitutions are unchanged.

### Part 3 — Built-in template catalog (representation A)

Define an `EvalTemplate` value type and a static catalog of 8 entries. Each entry:

```
EvalTemplate {
    String key;                 // stable id, e.g. "hallucination"
    String title;               // display name
    String description;         // one line
    String promptTemplate;      // uses {{input}}/{{output}}/{{context}}/{{metadata}}
    AiEvalScoreDataType dataType;
    BigDecimal minValue;        // NUMERIC only
    BigDecimal maxValue;        // NUMERIC only
    List<String> categories;    // CATEGORICAL only
}
```

**Instantiation:** a facade method `instantiateTemplate(templateKey, projectId, model,
samplingRate)` creates an `AiEvalScoreConfig` from the template's score shape and a new
`AiEvalRule` (referencing that score config, with the template's `promptTemplate`, the given
model/project/samplingRate, and `enabled=false` so it is reviewed before going live) and
returns the new rule id. Both entities are fully editable afterward. A `listTemplates()`
facade method returns the catalog for the UI. These live alongside the existing `AiEvalRuleFacade` in the
gateway; the `EvalTemplate` catalog itself is pure data in `platform-ai-eval-api`.

**The 8 templates and score shapes** (each prompt instructs the model to emit *only* the score
value in the exact format `buildScoreFromResponse` parses — strict NUMERIC/BOOLEAN parsing
fails loud):

| Template | Vars | dataType | Output format |
|---|---|---|---|
| Correctness | input, output | NUMERIC 0–1 | a decimal in [0,1] |
| Conciseness | input, output | NUMERIC 0–1 | a decimal in [0,1] |
| Helpfulness | input, output | NUMERIC 0–1 | a decimal in [0,1] |
| Relevance | input, output | NUMERIC 0–1 | a decimal in [0,1] |
| Toxicity | output | BOOLEAN | `true`/`false` |
| Hallucination | output, **context** | BOOLEAN | `true`=hallucinated |
| Context-relevance | input, **context** | NUMERIC 0–1 | a decimal in [0,1] |
| Context-correctness | output, **context** | NUMERIC 0–1 | a decimal in [0,1] |

### Data model / schema changes

- `AiObservabilitySpanType`: append `RETRIEVAL` (enum ordinal append; no migration).
- No new tables. No template table. No new span column (retrieved docs reuse `output`).

## Testing strategy (TDD)

**Part 1 (ingestion):**
- `openinference.span.kind == RETRIEVER` → span persisted as `RETRIEVAL`; absent/other → `GENERATION` (regression guard that default is unchanged).
- Retrieved documents from `retrieval.documents.*` attributes land in span `output`; fallback to `outputBody` when attributes absent.

**Part 2 (executor):**
- `buildPrompt` substitutes `{{context}}` from RETRIEVAL span outputs; empty string when none; `{{input}}/{{output}}/{{metadata}}` still substitute correctly.

**Part 3 (templates):**
- Catalog has 8 entries with unique keys.
- Each template renders (all its declared variables are substitutable) and its instructed output parses to a valid `AiEvalScore` under its declared `dataType` (NUMERIC/BOOLEAN).
- Contract test: every template's `dataType` is consistent with its prompt's declared output format.
- `instantiateTemplate` creates an `AiEvalScoreConfig` + `AiEvalRule` with the template's prompt and score shape.

All tests follow the existing module test conventions; integration-touching tests end in `IntTest`.

## Risks & honest scope notes

- **Detection depends on exporters emitting OpenInference `openinference.span.kind`.** OTel
  GenAI semantic conventions do not (yet) standardize a retriever span kind, so retrieval
  capture works for OpenInference-instrumented apps (LlamaIndex, LangChain via OpenInference,
  Arize) and any exporter that sets that attribute. Apps that don't tag retriever spans yield
  empty `{{context}}`, and the 3 context templates degrade to "no context provided" — visible
  and honest, not a silent wrong score.
- **No backfill:** only traces ingested after this change carry RETRIEVAL spans.
- **Strict parsing:** NUMERIC/BOOLEAN parsing fails loud (marks execution ERROR) on
  unexpected model output — template prompts must pin the output format tightly.

## Follow-ups (out of scope here)

- Map additional OpenInference kinds (`TOOL`, `CHAIN`) to richer span types.
- User-defined/custom templates (would promote representation A → a template entity).
- A RAGAS-style expansion of the judge catalog beyond the initial 8.
