# Embedding-Provider Status in the UI (AI Providers badge + Knowledge Base banner)

**Date:** 2026-06-27
**Status:** Design — pending review
**Edition scope:** Badge is EE (AI Providers settings page is EE); KB banner works in both editions.
**Builds on:** `2026-06-27-catalog-embedding-model-design.md` (backend boot + lazy embedding resolution).

## Problem

After the catalog-embedding-model backend change, the server boots even when no
embedding provider is configured, and the Knowledge Base only fails **reactively**:

- Uploading a document with no embedding provider active runs the ingestion job,
  which calls `CatalogEmbeddingModel.embed()` → throws the actionable
  `IllegalStateException` → the document lands in `KnowledgeBaseDocument.STATUS_ERROR`
  (shown as "Error" in the document list).
- Searching throws → error toast / 500.

There is **no proactive signal** anywhere in the UI that the Knowledge Base cannot
work because no embedding provider is active, and nothing tells the user *which*
provider to activate or *where*.

## Goals

1. **AI Providers settings page**: show an "Embeddings" badge on providers usable
   for embeddings (today: OpenAI only), so users know which provider to activate
   for the Knowledge Base.
2. **Knowledge Bases list page**: when no embedding-capable provider is active for
   the currently-selected environment, show a warning banner above the list.
3. The banner **links to the AI Providers settings page** so the user can activate
   one.

## Non-goals

- No per-document or per-single-KB banner (list page only, per decision).
- No new embedding providers — "embedding-capable" = exactly what
  `CatalogEmbeddingModelFactory.resolveFactory` supports today (OpenAI).
- No change to the embedding model name/dimensions (config-pinned).
- No banner dismissal/snooze state.

## Decisions (from brainstorming)

1. **Badge scope = currently-wired providers only** (OpenAI today). The badge must
   never advertise a provider the embedding path can't actually use.
2. **KB banner = list page only** (`KnowledgeBases.tsx`), keyed to the page's
   existing `currentEnvironmentId` (the list already renders `<EnvironmentSelect />`
   and is environment-scoped). No new environment selector.
3. **Two distinct backend signals, one per surface:**
   - **Badge** ← a new `supportsEmbeddings` flag on the AI Providers REST model
     (EE-only surface).
   - **Banner** ← a new edition-safe authoritative query
     `knowledgeBaseEmbeddingActive(environment)` (works in CE and EE).

   Rejected alternative: deriving the banner client-side from
   `getAiProviders(environment)` (checking `supportsEmbeddings && enabled`). It is
   EE-only (the AI Providers query/endpoint does not exist in CE) and would
   duplicate the authoritative predicate in the client, so the banner uses the
   dedicated query instead.

## Architecture

### Backend

#### `supportsEmbeddings` on the AI Providers REST model (EE)
- The AI Providers settings page calls `AiProviderApiController.getAiProviders(environment)`
  → `AiProviderFacadeImpl.getAiProviderCatalog(environment)`, returning a per-provider
  model that currently carries `{key, name, icon, enabled, supportsModelById, models}`.
- Add a boolean `supportsEmbeddings`. Source of truth: a single predicate that asks
  "does the embedding path support this provider?" — true for `Provider.OPEN_AI`,
  false otherwise. Implement as a small static capability check colocated with the
  embedding factory so the badge and the runtime factory cannot drift:
  - Add `boolean CatalogEmbeddingModelFactory.supports(Provider provider)` (returns
    `resolveFactory(provider) != null`), and have `AiProviderFacadeImpl` call it.
  - `AiProviderFacadeImpl` is EE and already depends on the provider enum; wire the
    factory (EE, same `platform-ai-agent` area) via constructor injection. If a
    direct module dependency is undesirable, inject an `ObjectProvider<CatalogEmbeddingModelFactory>`
    and default `supportsEmbeddings=false` when absent.
- Propagate the field through the REST DTO and the generated client model.

#### Edition-safe embedding-active check for the KB banner
- **EE**: the banner derives from `getAiProviders(environment)` (see UI). No new
  endpoint.
- **CE**: the AI Providers UI/endpoint does not exist. The banner must still work,
  so the Knowledge Base needs an edition-safe authoritative status. Add a minimal
  read-only query exposed by the Knowledge Base layer:
  `knowledgeBaseEmbeddingActive(environment: Int!): Boolean!`.
  - Backed by an SPI `EmbeddingProviderStatus` (interface in a CE platform module,
    e.g. `platform-ai` API): `boolean isActive(int environment)`.
  - EE impl (in `platform-ai-agent-service`): `ProviderApiKeyResolver.resolve(OPEN_AI, environment) != null`.
  - CE impl: static config key present (`ApplicationProperties …embedding`/`provider.openai.api-key`).
  - The KB list page calls this query (works in both editions). The EE-derived
    "providers" approach is an optimization the UI MAY use when the AI Providers
    query is already loaded, but the authoritative query is the edition-safe default.

> Design note: to avoid two code paths in the client, the **simplest consistent
> option is to use the `knowledgeBaseEmbeddingActive(environment)` query for the
> banner in both editions**, and use `supportsEmbeddings` only for the AI Providers
> badge. This is the recommended approach; the "derive from getAiProviders" path is
> documented as a rejected alternative because it would be EE-only and duplicate the
> authoritative predicate in the client.

### Frontend

#### AI Providers badge (`client/src/ee/pages/settings/platform/ai-providers/components/AiProviderList.tsx`)
- Next to the provider name (around line 87), render an "Embeddings" `Badge`
  (`@/components/Badge/Badge`, `styleType="secondary-outline"` or `success-outline`)
  when `aiProvider.supportsEmbeddings` is true.
- Mirrors existing conventions: lucide icons with `Icon` suffix, `twMerge`, `Props`
  interface naming.

#### KB list banner (`client/src/pages/automation/knowledge-bases/KnowledgeBases.tsx`)
- Read `currentEnvironmentId` from `useEnvironmentStore` (already used by the page's
  `<EnvironmentSelect />`).
- Query `knowledgeBaseEmbeddingActive(currentEnvironmentId)`.
- When `false`, render an `Alert` (`@/components/ui/alert`, `variant="destructive"`
  or a warning variant) above the list with:
  - Title: "No embedding model is active"
  - Description: "Knowledge Base documents can't be processed until an
    embedding-capable AI provider is activated for this environment."
  - A link to the AI Providers settings page (button/anchor → the AI Providers route).
- Banner placement: above the list/`EmptyList`, below the header.

## Data flow

```
KnowledgeBases.tsx
  env = useEnvironmentStore(currentEnvironmentId)
  active = useKnowledgeBaseEmbeddingActive(env)          // GraphQL: knowledgeBaseEmbeddingActive(env)
  if (!active) -> <Alert warning> ... <Link to AI Providers/>

AiProviderList.tsx
  providers = useGetAiProvidersQuery(env)                // REST, now includes supportsEmbeddings
  per provider: if (provider.supportsEmbeddings) -> <Badge>Embeddings</Badge>
```

## Error handling / edge cases

- If `knowledgeBaseEmbeddingActive` query fails (network), do **not** show a false
  banner — treat unknown as "active" (fail-open on the warning) to avoid noise; the
  reactive `STATUS_ERROR` still catches real failures.
- Switching the environment selector re-runs the query (banner is per-environment).
- CE deployments with a static OpenAI key → query returns true → no banner, no badge
  page (AI Providers UI is EE-only). The link target should degrade gracefully if the
  AI Providers route is absent in CE (show the banner text without the link, or omit
  the banner in CE since the user can't act on it — decide in planning).

## Testing

- **Backend**:
  - `CatalogEmbeddingModelFactory.supports(OPEN_AI)` true; others false.
  - `AiProviderFacadeImpl` sets `supportsEmbeddings=true` only for OpenAI.
  - `knowledgeBaseEmbeddingActive(environment)`: EE impl true when resolver returns a
    key, false otherwise; CE impl true when static key present.
- **Frontend** (vitest):
  - `AiProviderList`: badge renders only for providers with `supportsEmbeddings`.
  - `KnowledgeBases`: banner renders when query returns false; hidden when true;
    re-queries on environment change; link points at the AI Providers route.

## Open questions for planning

- Exact home module for the `EmbeddingProviderStatus` SPI + its CE/EE impls, and the
  GraphQL controller that exposes `knowledgeBaseEmbeddingActive` (KB graphql is CE;
  the EE impl lives in `platform-ai-agent-service`).
- CE behavior of the banner's link (AI Providers route is EE-only) — show text-only,
  or suppress the banner in CE.
- Badge copy/style ("Embeddings" vs "Embedding"; outline vs filled).

## References

- Backend embedding resolution: `2026-06-27-catalog-embedding-model-design.md`.
- `AiProviderFacadeImpl.getAiProviderCatalog` / `AiProviderApiController`
  (`server/ee/libs/platform/platform-configuration/...`).
- `CatalogEmbeddingModelFactory.resolveFactory`, `ProviderApiKeyResolver`
  (`server/ee/libs/platform/platform-ai/platform-ai-agent/platform-ai-agent-service`).
- `AiProviderList.tsx`, `KnowledgeBases.tsx`, `@/components/Badge/Badge`,
  `@/components/ui/alert`, `useEnvironmentStore`.
