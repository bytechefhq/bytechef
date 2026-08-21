# Catalog-resolved Embedding Model for the Knowledge Base

**Date:** 2026-06-27
**Status:** Design — approved, pending spec review
**Edition scope:** EE (Enterprise) for dynamic resolution; CE behavior unchanged

## Problem

The server fails to start when the Knowledge Base is enabled but no embedding
provider is configured at boot:

```
Parameter 1 of method knowledgeBasePgVectorStore in
MultiTenantKnowledgeBasePgVectorConfiguration required a bean of type
'org.springframework.ai.embedding.EmbeddingModel' that could not be found.
```

Today the KB / Copilot / ContextStore pgvector stores receive a **singleton
`EmbeddingModel` bean wired once at boot**, and the only producer of that bean is
`AiModelConfiguration.openAiEmbeddingModel`, gated on
`bytechef.ai.provider.openai.api-key`. Spring AI's own embedding autoconfig is
disabled (`spring.ai.model.embedding: none`). So when an EE deployment enables
the Knowledge Base but provides the OpenAI key **through the UI** rather than
through static config, no `EmbeddingModel` bean exists and the context aborts.

A short-term fail-fast guard already exists (replaces the cryptic Spring error
with an actionable `IllegalStateException` in the KB and Copilot vector-store
configs). This spec describes the real fix: **the app should always boot, and the
Knowledge Base should start working the moment an embedding-capable provider is
activated in the UI — no restart.**

## Background: chat already does this, embeddings do not

Chat models are resolved **at call time** from whichever provider is activated in
the UI:

- "Activated in the UI" = a row in `PropertyService`, `Scope.PLATFORM`,
  environment-scoped, keyed by provider (`ai.provider.openAi`), carrying an
  `apiKey` and an `isEnabled()` flag.
- `CatalogChatClientResolverImpl.resolve(environment, providerKey, model)` reads
  that key (falling back to static config via `configApiKey`) and builds a
  `ChatModel` through `CatalogChatModelFactory`, which reuses each LLM
  component's `CHAT_MODEL` lambda.
- Boot never depends on any chat provider being configured.

There is **no embedding equivalent** — no `CatalogEmbeddingModelFactory`, no
resolver. Embeddings come only from the static boot-time bean or the EE AI
gateway's per-provider factory.

## Decisions (from brainstorming)

1. **Provider selection — reuse the activated provider.** The embedding **model
   name** stays in `application.yml`
   (`bytechef.ai.provider.embedding.openai.options.model: text-embedding-3-small`).
   Only the **API key** is dynamic and is sourced from the activated provider row
   (same source the chat side reads), falling back to static config. The
   embedding provider set is whatever `ApplicationProperties.Ai.Provider.Embedding`
   configures — **today OpenAI only**.

2. **Dimension constraint — restrict to dimension-compatible models.** Satisfied
   for free: the embedding model is config-pinned (`text-embedding-3-small`,
   1536 dims) and already matches the pgvector table's configured dimension
   (`spring.ai.vectorstore.pgvector.dimensions: 1536`). The model never varies at
   runtime, so no re-index path is needed. (If a future change introduces a
   second embedding provider/model, its dimension MUST match the table or the
   change must include a re-index migration — out of scope here.)

3. **Environment propagation — ThreadLocal `EnvironmentContext`.** AI keys are
   environment-scoped, but `VectorStore.add()/similaritySearch()` call
   `embeddingModel.embed()` without an environment argument. A small ThreadLocal
   (parallel to `TenantContext`) carries the current environment; KB / Copilot /
   ContextStore facades set it around vector-store operations.

## Goals

- App **always boots** regardless of whether an embedding provider is configured
  at startup (EE).
- KB embedding (ingestion + similarity search) works as soon as the OpenAI
  provider is activated in the UI for the relevant environment — no restart.
- Reuse the chat-side key-resolution logic so chat and embeddings cannot diverge.
- KB/Copilot/ContextStore vector-store configurations remain unchanged.

## Non-goals

- Making embeddings dynamic in **CE** (no UI provider catalog in CE; CE keeps the
  static-config requirement plus the fail-fast guard already shipped).
- Supporting embedding providers other than what `embedding.*` config names
  (today OpenAI). Activating only a non-embedding provider (e.g. Anthropic) does
  not enable KB.
- Switching embedding models/dimensions at runtime or re-indexing the KB.
- Routing embeddings through the EE AI **gateway** (`AiGatewayProvider`); this
  design aligns with the **catalog/PropertyService** activation that chat uses,
  per "reuse the activated provider."

## Architecture

### New / changed components

#### `CatalogEmbeddingModel` (new — EE)
- Location: `server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service`
  (beside `CatalogChatModelFactory` / `CatalogChatClientResolverImpl`).
- `@Component @Primary @ConditionalOnEEVersion`, implements
  `org.springframework.ai.embedding.EmbeddingModel`.
- Always present → every vector-store config finds an `EmbeddingModel` → context
  boots.
- `embed(...)` flow:
  1. Read current environment from `EnvironmentContext` (default `PRODUCTION`,
     ordinal 2, with a logged debug message if unset).
  2. Resolve the configured embedding provider from
     `ApplicationProperties.Ai.Provider.Embedding` (today OpenAI) and its model
     name.
  3. Resolve the provider's API key via `ProviderApiKeyResolver` (activated
     property → static config fallback).
  4. If no key → throw `IllegalStateException` with an actionable message.
  5. Otherwise build + **cache** an `OpenAiEmbeddingModel` keyed by
     `(environment, apiKey)` and delegate all `EmbeddingModel` methods to it.
- Caching mirrors `AiGatewayEmbeddingModelFactoryImpl` (avoid building an HTTP
  client per call). Cache is invalidated when a provider's key changes — see
  "Cache invalidation."

#### `ProviderApiKeyResolver` (extracted — EE)
- Extract the private `resolveApiKey(provider, environment)` + `configApiKey`
  logic out of `CatalogChatClientResolverImpl` into a shared `@Component`.
- `CatalogChatClientResolverImpl` and `CatalogEmbeddingModel` both delegate to
  it. No behavior change for chat (verified by existing chat resolver tests).
- Signature (sketch): `@Nullable String resolve(Provider provider, int environment)`.

#### `EnvironmentContext` (new — small ThreadLocal)
- Placed in a CE-visible module alongside `Environment`
  (`com.bytechef.platform.configuration`), so KB facades (CE) can set it and
  `CatalogEmbeddingModel` (EE) can read it.
- API: `set(Environment) / getCurrentEnvironment() / clear()`, used in
  `try { set(...); vectorStore.add(...); } finally { clear(); }`.
- Default when unset: `PRODUCTION` (ordinal 2), logged at DEBUG.

#### Facade changes (set the ThreadLocal)
- `KnowledgeBaseDocumentFacadeImpl` (ingestion) and
  `KnowledgeBaseDocumentChunkFacadeImpl` (search): wrap `vectorStore.add()` /
  `similaritySearch()` with `EnvironmentContext.set(knowledgeBase.getEnvironment())`.
- Copilot / ContextStore facades: same wrap where they call their vector stores
  (their environment source TBD during implementation — Copilot/ContextStore are
  not multi-environment in the same way; if no per-entity environment exists they
  use the default).

### Bean wiring matrix

| Edition | EmbeddingModel bean | Boots without provider config? | No provider activated |
| --- | --- | --- | --- |
| EE | `CatalogEmbeddingModel` (`@Primary`) | **Yes** | `embed()` throws actionable error at request time |
| CE | static `openAiEmbeddingModel` (if key) | No (needs static key) | fail-fast guard at boot (already shipped) |

The previously-shipped config guards remain: in EE they become inert (an
`EmbeddingModel` is always present); in CE they still produce the actionable
boot-time message. The "no provider" actionable message now lives in two places
by design — config guard (CE boot) and `CatalogEmbeddingModel.embed()` (EE
request time).

## Data flow

**Ingestion**
```
KnowledgeBaseDocumentFacadeImpl.ingest(doc)
  EnvironmentContext.set(knowledgeBase.getEnvironment())
  vectorStore.add(documents)
    -> CatalogEmbeddingModel.embed(documents)
         env = EnvironmentContext.getCurrentEnvironment()
         apiKey = ProviderApiKeyResolver.resolve(OPEN_AI, env)
         delegate = cache[(env, apiKey)] ?: new OpenAiEmbeddingModel(apiKey, model)
         return delegate.embed(documents)
  EnvironmentContext.clear()
```

**Search** — identical wrap around `similaritySearch()`.

## Cache invalidation

When an activated provider's API key is updated or disabled in the UI, the cached
`OpenAiEmbeddingModel` for that `(environment, apiKey)` must not be served stale.
Two acceptable strategies (decide during implementation):
- Key the cache by `(environment, apiKey)` so a changed key naturally produces a
  cache miss (a disabled provider yields no key → error). **Preferred** — simplest
  and self-correcting.
- Explicit eviction hook on provider update (mirrors
  `AiGatewayProviderServiceImpl.update()` calling `evict`).

The `(environment, apiKey)` keying is preferred because it requires no coupling
to the provider-update path; a rotated or removed key simply stops matching.

## Error handling

- No provider activated / no key for the environment → `IllegalStateException`:
  > "No embedding provider is activated for environment `<ENV>`. Activate the
  > OpenAI provider in the UI (or set `bytechef.ai.provider.openai.api-key`) so
  > the Knowledge Base can embed documents."
- The error surfaces as a failed KB operation (ingestion job / search request),
  not a boot failure. The app stays up.
- Activated provider lacks embeddings (only a non-OpenAI provider is active) →
  same message; OpenAI is specifically required because the embedding config
  names it.

## Testing

- **`CatalogEmbeddingModel`** (unit): key activated → delegates to a built model;
  no key → throws the actionable message; reads environment from
  `EnvironmentContext`; caches per `(environment, apiKey)` (second call with same
  key does not rebuild).
- **`ProviderApiKeyResolver`** (unit): enabled property wins; falls back to static
  config when no enabled property; null when neither. Existing
  `CatalogChatClientResolver` tests must stay green (parity after extraction).
- **EE KB config** (`ApplicationContextRunner`): context boots **without** a
  static `bytechef.ai.provider.openai.api-key` when a `CatalogEmbeddingModel`
  bean is present (the regression this whole effort fixes).
- **`EnvironmentContext`** (unit): set / get / clear; default `PRODUCTION` when
  unset.
- **Facades**: environment is set around vector-store calls and cleared in a
  `finally` (no leakage across requests/jobs).

## Files (anticipated)

New:
- `.../platform-ai-agent-service/.../catalog/CatalogEmbeddingModel.java`
- `.../platform-ai-agent-service/.../catalog/ProviderApiKeyResolver.java`
- `.../platform-configuration/.../EnvironmentContext.java` (exact module TBD)
- Tests for each.

Changed:
- `CatalogChatClientResolverImpl` — delegate key resolution to
  `ProviderApiKeyResolver`.
- `KnowledgeBaseDocumentFacadeImpl`, `KnowledgeBaseDocumentChunkFacadeImpl`
  (and Copilot / ContextStore facades) — set/clear `EnvironmentContext`.

Unchanged:
- `MultiTenantKnowledgeBasePgVectorConfiguration`,
  `KnowledgeBasePgVectorConfiguration`, `CopilotPgVectorConfiguration`,
  `ContextStorePgVectorConfiguration` — they keep injecting `EmbeddingModel`.

## Open questions for implementation

- Exact home module for `EnvironmentContext` (must be visible to both CE KB
  facades and EE `CatalogEmbeddingModel`).
- Copilot / ContextStore environment source — whether those entities carry an
  environment or always use the default.
- Whether `CatalogEmbeddingModel` builds `OpenAiEmbeddingModel` directly (as
  `AiModelConfiguration` does) or reuses the `OpenAiEmbedding` component cluster
  element (as `CatalogChatModelFactory` reuses `CHAT_MODEL`). Direct construction
  is simpler and is the default unless reuse proves cleaner.

## References

- Chat analog: `CatalogChatClientResolverImpl`, `CatalogChatModelFactory`
  (`server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service`).
- Static embedding bean: `AiModelConfiguration.openAiEmbeddingModel`
  (`server/libs/config/ai-model-config`).
- Gateway embedding factory (per-provider caching pattern):
  `AiGatewayEmbeddingModelFactoryImpl`
  (`server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service`).
- Vector-store configs:
  `MultiTenantKnowledgeBasePgVectorConfiguration`,
  `KnowledgeBasePgVectorConfiguration`, `CopilotPgVectorConfiguration`,
  `ContextStorePgVectorConfiguration`.
- Fail-fast guard (shipped precursor): commits `1fb754c5c0f`, `118b4e85691`.
