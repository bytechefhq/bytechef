# models.dev Model Catalog

**Date:** 2026-08-10
**Status:** Approved

## Problem

Every `ai_gateway_model` row is typed in by hand. An admin registering a model supplies its name,
context window, capabilities, and both cost rates from memory or from a provider's pricing page.
Nothing seeds the table and nothing keeps it current, so the catalog drifts the moment a provider
reprices.

The cost of that drift is already visible in the code. `OtlpCostResolver` carries a nine-value
`Reason` enum, and two of its entries — `MODEL_NOT_REGISTERED` and `RATES_MISSING` — exist purely
because the catalog is hand-maintained. Both produce spans with no cost attribution, which means
gaps in spend dashboards and budget enforcement. `CostOptimizedRoutingStrategy`,
`IntelligentRoutingStrategy`, and `AiGatewayModelTier.classify` all route on those same rates, so
stale pricing silently misroutes traffic.

[models.dev](https://github.com/anomalyco/models.dev) publishes exactly this data as an open
database: 182 providers, community-maintained, with per-model pricing, limits, modalities, and
capability flags.

## Scope

The catalog is a **write-side source**. It populates `ai_gateway_model` rows; those rows remain the
authoritative read path for cost calculation and routing.

**In scope:**

- A standalone CE module exposing models.dev data as a typed Java API.
- A reconciler that inserts and updates `ai_gateway_model` rows from that catalog.
- A per-row pin so an admin edit to a catalog-owned field survives reconciliation.

**Explicitly out of scope:**

- `AiGatewayCostCalculatorImpl` and `OtlpCostResolver` are **not modified**. They keep reading
  `ai_gateway_model` columns. The catalog improves the data in those columns; it does not become a
  fallback lookup in the cost path.
- The LLM component model dropdowns (`ModelUtils.getEnumOptions` over vendor SDK enums) stay as
  they are. The module is placed in CE so this can be revisited without a move, but it is not part
  of this work.

## Architecture

### Module

`server/libs/platform/platform-ai/platform-ai-model-catalog`, Apache-2.0, split `-api` / `-service`
to match `platform-ai-gateway` and the rest of the `platform-ai` tree.

The module is deliberately ignorant of the AI gateway. Its vocabulary is models.dev's: providers are
identified by upstream string ids (`"anthropic"`, `"azure"`), never by `AiGatewayProviderType`. The
enum mapping lives on the EE side. This is what keeps the module standalone and lets a future
consumer — the component dropdowns, an embedding-model picker — adopt it without a relicense or a
move.

### `-api`

```java
public interface ModelCatalog {

    Optional<CatalogModel> fetchModel(String providerId, String modelId);

    List<CatalogModel> getModels(String providerId);

    List<CatalogProvider> getProviders();

    Instant getLoadedAt();
}
```

`getLoadedAt()` is provenance: it reports when the in-memory catalog was populated, so an operator
can tell a bundled snapshot from a successful refresh.

Records mirror the upstream shape:

```java
public record CatalogProvider(String id, String name, @Nullable String doc) {}

public record CatalogModel(
    String id,
    String name,
    @Nullable String description,
    @Nullable String family,
    boolean attachment,
    boolean reasoning,
    boolean toolCall,
    boolean structuredOutput,
    boolean temperature,
    boolean openWeights,
    @Nullable String knowledge,
    @Nullable LocalDate releaseDate,
    @Nullable LocalDate lastUpdated,
    Status status,
    Modalities modalities,
    Limit limit,
    @Nullable Cost cost) {

    public enum Status { ACTIVE, BETA, DEPRECATED }
}

public record Modalities(List<Modality> input, List<Modality> output) {

    public enum Modality { TEXT, IMAGE, AUDIO, VIDEO, PDF }
}

public record Limit(@Nullable Integer context, @Nullable Integer input, @Nullable Integer output) {}

public record Cost(
    @Nullable BigDecimal input,
    @Nullable BigDecimal output,
    @Nullable BigDecimal cacheRead,
    @Nullable BigDecimal cacheWrite,
    @Nullable BigDecimal reasoning,
    @Nullable BigDecimal inputAudio,
    @Nullable BigDecimal outputAudio,
    List<CostTier> tiers) {}

public record CostTier(
    @Nullable Integer contextSize,
    @Nullable BigDecimal input,
    @Nullable BigDecimal output,
    @Nullable BigDecimal cacheRead,
    @Nullable BigDecimal cacheWrite) {}
```

Design notes on the record shapes:

- **`cost` is nullable.** 21 of the 249 models across the eight mapped providers ship no `cost`
  block at all (`groq/whisper-large-v3`, the `cohere/c4ai-aya-*` family, Google's `veo-*` and
  `gemma-*` entries). A non-null `Cost` with null fields would blur "priced at zero" against "no
  pricing published"; the resolver already treats that distinction as load-bearing.
- **`BigDecimal`, not `double`.** These are money rates multiplied by token counts. The existing
  `AiGatewayModel` columns are `BigDecimal` and the whole cost path is `BigDecimal`; parsing to
  `double` and back would reintroduce the drift those types exist to prevent.
- **`tiers` is modeled but unused by the reconciler.** 23 of the 249 models price by context size
  (Claude Opus and GPT-5.x charge more above 200k/272k tokens). The gateway's two flat columns
  cannot express this, so the reconciler writes the base rate. Modeling tiers now means a future
  gateway change is a consumer change, not a re-parse.

  Upstream expresses tiering twice: a structured `cost.tiers` array and a flat
  `cost.context_over_200k` object, and every tiered model in the data carries both. The parser reads
  `cost.tiers` and **ignores `context_over_200k`** — the array is the general form (it carries its
  own `tier.size`), while the flat key hardcodes a threshold into its own name and cannot describe a
  model tiered at 272k, which several GPT-5.x entries are.
- **`status` defaults to `ACTIVE`** when upstream omits the field. Upstream uses it sparsely: across
  the mapped providers, 197 models have no status, 37 are `deprecated`, 15 are `beta`.

### `-service`

Three collaborators:

**`ModelsDevSnapshotLoader`** reads the bundled snapshot from the classpath and parses it into an
immutable `Map<String, CatalogProvider>` + nested model maps.

**`ModelCatalogImpl`** holds the current catalog in a single `volatile` reference and serves reads
from it. **Parsing is lazy** — it happens on first access, not in the constructor and not at
context refresh. This is deliberate: server-app startup is already sensitive to classpath-wide
scanning, and a 3.6 MB parse on every boot for a feature most deployments will not touch on the
first request is the wrong trade.

**`ModelsDevRefresher`** is `@Scheduled` and fetches `https://models.dev/api.json` over a
`RestClient` with connect and read timeouts, parses it through the same code path as the snapshot,
and atomically swaps `ModelCatalogImpl`'s reference on success.

Failure handling is **fail-soft in one direction only**: any failure — timeout, non-2xx, malformed
JSON, a body that parses to zero providers — logs at warn and leaves the existing catalog in place.
The refresher never installs an empty or partial catalog. An operator whose egress is blocked gets
the bundled snapshot forever and a warn line per interval; they never get a catalog that silently
stops answering.

Properties, all under `bytechef.ai.model-catalog`:

| Property | Default | Meaning |
|---|---|---|
| `refresh.enabled` | `true` | Whether the scheduled refresh runs at all |
| `refresh.url` | `https://models.dev/api.json` | Override for a mirror or an internal proxy |
| `refresh.interval` | `P1D` | Refresh period |

### The bundled snapshot

`src/main/resources/config/model-catalog/models-dev-api.json` — a **verbatim** copy of the
published `api.json`, 3.6 MB.

Two alternatives were measured and rejected. Pruning to the fields the records model saves only
3.6 MB → 2.7 MB, because the bulk is 182 providers × their model lists rather than the dropped
fields. Gzipping saves more but makes the file opaque. Verbatim is worth the bytes: a refresh commit
diffs cleanly against upstream, so a reviewer can confirm the snapshot is genuinely what models.dev
published rather than something a script mangled.

A Gradle task `refreshModelsDevSnapshot` on the module re-fetches the file for a deliberate commit.

## Reconciliation

`AiGatewayModelCatalogReconciler`, in `platform-ai-gateway-service` (EE), gated by the module's
existing `@ConditionalOnEEVersion` + `bytechef.ai.gateway.enabled` pair.

### Provider mapping

`AiGatewayProviderType` has eight values and every one has a models.dev counterpart, so the mapping
is an explicit total table, not a fuzzy match:

| `AiGatewayProviderType` | models.dev id | Models | Text-output |
|---|---|---|---|
| `ANTHROPIC` | `anthropic` | 13 | 13 |
| `AZURE_OPENAI` | `azure` | 82 | 80 |
| `COHERE` | `cohere` | 14 | 14 |
| `DEEPSEEK` | `deepseek` | 4 | 4 |
| `GOOGLE_GEMINI` | `google` | 41 | 34 |
| `GROQ` | `groq` | 15 | 13 |
| `MISTRAL` | `mistral` | 33 | 32 |
| `OPENAI` | `openai` | 47 | 45 |
| | **Total** | **249** | **235** |

The mapping is exhaustive over the enum. Adding an enum value without a mapping entry must fail a
test, not silently skip that provider at runtime.

### Triggers

Reconcile runs on application startup, on a daily schedule, and on demand via an admin mutation.

Startup reconcile is intentional: it means a fresh deployment that configures a provider has a
populated model list without waiting a day or finding a button. It runs after the context is ready
and off the startup thread, so it does not extend boot time.

### Rules

For each **enabled** `AiGatewayProvider`, resolve its models.dev id and then, per model:

| Row state | Action |
|---|---|
| No row exists for a catalog model | **INSERT** — `enabled = true`, all catalog-owned fields populated |
| Row exists, `catalogPinned = false`, catalog match | **UPDATE** catalog-owned fields |
| Row exists, `catalogPinned = true` | **skip** |
| Row exists, no catalog match | **skip, silently** |

**Catalog-owned fields** are exactly four: `inputCostPerMTokens`, `outputCostPerMTokens`,
`contextWindow`, `capabilities`. Everything else on the row — `alias`, `enabled`,
`defaultRoutingPolicyId` — is admin-owned and never written by reconcile, including on insert
beyond the `enabled = true` seed.

**Uncatalogued rows are skipped silently**, with no marker column. Azure deployment names,
fine-tunes like `ft:gpt-4o:acme:x`, and models newer than the snapshot all land here and keep
whatever rates were entered by hand.

> **Superseded:** this section originally also said "and no UI badge." A follow-up review found
> that the two-state `catalogPinned` badge rendered these rows as "Catalog," falsely claiming the
> reconciler maintains their pricing. The user decided to add a third UI state instead — see
> "Three-state catalog badge" under Pinning, below. No marker *column* was added; the badge's third
> state is computed on read, not stored.

### Insert filter

Insert is filtered; update is not.

A model is **inserted** only if `modalities.output` contains `text` **and** `status != DEPRECATED`.
A model that already has a row is **updated** whenever the catalog has a match, regardless of both
conditions.

The asymmetry is the point. Without it, enabling the Google provider inserts `veo-3.1-*` video
generation models the gateway cannot route, and enabling any provider inserts 37 deprecated models
across the eight. But a deprecated model that a deployment is *still routing to* costs real money
and must keep getting repriced — so an existing row is never abandoned just because upstream marked
it deprecated. The filter yields roughly 200 inserted rows across all eight providers.

### Base-tier pricing

The reconciler writes `cost.input` and `cost.output` — the base tier. For the 23 tiered models, that
is the rate below the context threshold. Above-threshold traffic is under-costed against the row's
rates. This is a known, accepted limitation of the gateway's two-column pricing model, not something
this work introduces: those columns could not express tiered pricing before either. It is recorded
here so the eventual fix has a starting point.

### `capabilities` encoding

`ai_gateway_model.capabilities` is a free-form `VARCHAR(256)` that nothing in the codebase parses.
This work defines it: a comma-separated token list derived from the catalog, produced by a single
`CapabilitiesEncoder` so the format has exactly one writer.

Tokens: `tool_call`, `structured_output`, `reasoning`, `attachment`, `temperature`, and `vision`
(emitted when `modalities.input` contains `image`). Alphabetically ordered for stable diffs and
idempotent updates. Worst case is well under 256 characters.

## Pinning

`ai_gateway_model` gains `catalog_pinned BOOLEAN NOT NULL DEFAULT FALSE`.

`platform-ai-gateway` has **zero files in the `v0.31.2` tag** — the module is unreleased, so per the
repository's Liquibase convention the column is added directly to
`00000000000001_ai_gateway_init.xml` rather than stacked as a new changeset. No customer database
has ever run this changelog.

The flag is set when `updateAiGatewayModel` writes a value to any of the four catalog-owned fields
that **differs from the current value**. Comparing rather than merely detecting presence matters:
GraphQL clients routinely round-trip a whole object, so a save that touches only `alias` would
otherwise pin the row as a side effect.

Once pinned, reconcile skips the row entirely — the admin's negotiated rate survives indefinitely.
Two surface changes support this:

- `catalogPinned: Boolean!` on the `AiGatewayModel` GraphQL type, so the UI can show which rows the
  catalog manages.
- An `unpinAiGatewayModel(id)` mutation that clears the flag, handing the row back to the catalog.
  The next reconcile overwrites it. A workspace-scoped equivalent,
  `unpinWorkspaceAiGatewayModel(workspaceId, modelId)`, was added on
  `WorkspaceAiGatewayModelFacade` with the same workspace-ownership guard as the facade's other
  mutating methods, since the workspace facade — not the platform/admin one — is what the UI
  actually calls.

This means "catalog always wins" holds for unpinned rows only. That is the intended semantic: the
catalog is the default authority, and an explicit admin edit is the documented way to opt out.

### Three-state catalog badge

`catalogPinned` alone only distinguishes two states, but a row can also be unpinned *and*
uncatalogued — an Azure deployment name, a fine-tune, a model newer than the bundled snapshot.
Rendering that row the same as a catalog-maintained one is a false maintenance claim, so the UI
needs a third state:

| State | Condition | Meaning |
|---|---|---|
| Overridden | `catalogPinned == true` | An admin edited a catalog-owned field; the reconciler skips this row. |
| Catalog | `catalogPinned == false` and the catalog has an entry | The reconciler maintains this row. |
| Unmanaged | `catalogPinned == false` and the catalog has no entry | The reconciler silently skips this row; its rates are exactly whatever a human typed. |

The client cannot derive "the catalog has an entry" from `catalogPinned` alone, so a second
computed field, `catalogManaged: Boolean!`, was added to the `AiGatewayModel` GraphQL type. It
reports whether the catalog currently has a match for the row's provider + model id, independent of
`catalogPinned` — the two are combined client-side into the three states above.

`catalogManaged` is resolved by `AiGatewayModelCatalogReconciler.catalogManagedModelIds`, batched
behind a single `@BatchMapping` GraphQL resolver so a models list of any size costs one provider
lookup, not one per row — the same N+1 discipline the reconciler already applies to its own sweep.
A row on a **disabled** provider is never reported managed, matching `reconcile()`'s
`getEnabledProviders()` sweep: a provider an admin has disabled will not be repriced again until
re-enabled, so labeling its rows "Catalog" would be the same false claim this field exists to
prevent.

## Error handling

| Condition | Behavior |
|---|---|
| Refresh fetch fails (timeout, non-2xx, malformed) | Warn, keep current catalog. Never install empty. |
| Bundled snapshot missing or unparseable | Fail fast at first access — a packaging error, not a runtime condition. |
| Unknown `status` or `modalities` value from upstream | Tolerate. An unrecognized modality is dropped from the list; an unrecognized status maps to `ACTIVE`. models.dev is community-maintained and adds vocabulary without warning — a strict parse would let one new value upstream blank the entire catalog on the next refresh. |
| A single model entry fails to parse | Skip that model, keep the rest of the provider. Logged at debug with the provider and model id. |
| Enum value with no provider mapping | Fails a test at build time; at runtime, warn and skip that provider. |
| Catalog model has no `cost` | Insert the row with null rates. Matches today's "admin has not filled it in" state; `OtlpCostResolver` already reports `RATES_MISSING`. |
| Reconcile fails mid-run for one provider | Log and continue to the next provider. One bad provider must not abort the sweep. |

## Testing

**CE, `platform-ai-model-catalog`:**

- Parsing over a small hand-written fixture: lookup by provider + model, absent model, unknown
  provider, a model with no `cost`, status defaulting to `ACTIVE`, tier parsing, modality mapping.
- `ModelsDevRefresher` against a stubbed `RestClient`: success swaps the catalog and advances
  `getLoadedAt()`; each failure mode (timeout, non-2xx, malformed body, zero-provider body)
  preserves the previous catalog.
- A test over the **bundled** snapshot asserting it parses and contains all eight mapped providers.
  This is the guard that makes a bad `refreshModelsDevSnapshot` commit fail CI rather than ship.

**EE, `platform-ai-gateway-service`:**

- `AiGatewayModelCatalogReconcilerTest` — one case per row of the rules table: insert, update,
  pinned-skip, uncatalogued-skip, disabled-provider-skip.
- The insert filter's asymmetry, explicitly: a deprecated model is not inserted, but a deprecated
  model with an existing row **is** updated. Likewise a non-text-output model.
- Provider-mapping exhaustiveness over `AiGatewayProviderType.values()`.
- Pinning: an update that changes a catalog-owned field pins; an update that changes only `alias`
  does not; `unpinAiGatewayModel` clears the flag and the next reconcile overwrites the row.
- Idempotence: two consecutive reconciles against an unchanged catalog produce no second write.

## Consequences

`OtlpCostResolver`'s `MODEL_NOT_REGISTERED` and `RATES_MISSING` counters should drop sharply once a
provider is enabled, since its models now exist with rates before any traffic arrives. Both counters
stay — they remain the correct signal for uncatalogued models and for the 21 catalog models with no
published pricing.

`AiGatewayModelTier.classify` and both routing strategies get accurate rates without any change to
their code, because they read the same columns the reconciler now maintains.
