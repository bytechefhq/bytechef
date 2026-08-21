# AI Gateway Intelligent Routing — Cost-Tier Mapping

**Date:** 2026-06-09
**Status:** Design
**Author:** Ivica Cardic

## Summary

Bring the AI Gateway's `INTELLIGENT_COST` / `INTELLIGENT_BALANCED` / `INTELLIGENT_QUALITY`
routing strategies in line with [Merge Gateway's intelligent routing](https://docs.merge.dev/merge-gateway/features/routing-policies/intelligent):
classify candidate models into **five cost tiers** and map a 0–1 prompt-complexity score
**across** those tiers per strategy. Today the strategy collapses to a binary pick (cheapest
vs. most-expensive deployment), which is the main behavioral gap versus the page.

The prompt-complexity score stays **deterministic and model-free** (no LLM / agent-judge in the
routing hot path), but is moved behind a swappable interface so an embedding-based scorer can be
introduced later without touching the strategy.

## Non-Goals

- No LLM-as-judge / agent-judge in the routing decision. The existing post-response
  `AiEvalExecutor` (LLM judge over completed traces) is unrelated and unchanged.
- No embedding-based complexity scorer in this iteration (Merge embeds the prompt; we ship the
  deterministic heuristic behind an interface and defer the embedding scorer).
- No API, DTO, GraphQL, or database/schema changes. Routing policies already carry deployments
  plus a `strategy` enum; tier classification is derived from existing cost data.
- No new user configuration. (Per-policy tier overrides are explicitly deferred — YAGNI.)

## Current State

- `AiGatewayRouterImpl` switches `AiGatewayRoutingStrategyType` to a strategy instance. The three
  `INTELLIGENT_*` values share one class, `IntelligentRoutingStrategy`, differing only by threshold.
- `IntelligentRoutingStrategy.selectDeployment` sorts deployments by `outputCostPerMTokens` and
  returns `getFirst()` (cheapest) when `promptComplexityScore < threshold`, else `getLast()`
  (most expensive). Thresholds: `INTELLIGENT_COST` 0.7, `INTELLIGENT_BALANCED` 0.5,
  `INTELLIGENT_QUALITY` 0.3. **Only two tiers are ever reachable.**
- `promptComplexityScore` is produced by `AiGatewayFacadeImpl.estimatePromptComplexity(request)`:
  `totalChars / 4` (token estimate) bucketed into `{0.2, 0.5, 0.8}` — a char-length heuristic, no
  prompt-content analysis.
- `AiGatewayRoutingContext` already carries `promptComplexityScore`, `modelMap`,
  `averageLatencyByModelId`, `providerTypeByModelId`, `tags`.

## Design

### 1. Cost tiers

New enum `AiGatewayModelTier` in `platform-ai-gateway` classifying a model by its
`outputCostPerMTokens` (USD per 1M output tokens), thresholds verbatim from the Merge page:

| Tier      | Output $/1M  |
|-----------|--------------|
| FRONTIER  | ≥ 5.00       |
| ADVANCED  | 2.00 – < 5.00|
| STANDARD  | 1.50 – < 2.00|
| EFFICIENT | 0.10 – < 1.50|
| BASIC     | < 0.10       |

Tier ordering (BASIC → FRONTIER) is the capability axis. A classifier maps a model to a tier;
a model with null/missing pricing is treated as the **most capable** tier so it is never silently
chosen as the "cheap" option (consistent with today's `Long.MAX_VALUE` deprioritization on the
cost axis).

### 2. Score → tier mapping per strategy

`IntelligentRoutingStrategy.selectDeployment`:

1. Group enabled deployments by tier; build the sorted list of **tiers actually present** in the
   policy, cheap → capable.
2. Map `promptComplexityScore ∈ [0,1]` to an index into that present-tiers list, per strategy:
   - **INTELLIGENT_COST** — convex skew toward cheap tiers; only scores ≳0.7 reach the top tiers
     (Merge: ~70% traffic to cheaper models).
   - **INTELLIGENT_BALANCED** — linear map of score across present tiers (Merge: ~50/50).
   - **INTELLIGENT_QUALITY** — only scores ≲0.3 drop to cheap tiers; otherwise capable
     (Merge: only clearly-simple prompts go cheap).
3. If multiple deployments share the selected tier, pick deterministically (lowest cost within the
   tier, then stable order) so the choice is reproducible in tests.

The mapping curves are expressed as named constants/helpers on the strategy so they are tunable
without changing control flow. The mapping operates on **present** tiers, so a policy with only two
tiers still behaves sensibly (degenerates toward today's behavior).

### 3. Complexity scorer

Extract scoring into a `PromptComplexityScorer` interface in `platform-ai-gateway`:

```
double score(AiGatewayChatCompletionRequest request)  // [0.0, 1.0]
```

Ship `DeterministicPromptComplexityScorer` (model-free) as the implementation. It produces a
**continuous** 0–1 score (replacing the `{0.2, 0.5, 0.8}` buckets so the per-strategy mapping has
real resolution) from signals already on the request:

| Signal             | Source                                   | Normalization              | Weight |
|--------------------|------------------------------------------|----------------------------|--------|
| Prompt size        | Σ message content chars ÷ 4 (token est.) | `min(tokens / 2000, 1)`    | 0.40   |
| Tool count         | `request.tools().size()`                 | `min(tools / 8, 1)`        | 0.25   |
| Structured content | code fences / high brace-bracket density | present → 1, else 0        | 0.15   |
| Conversation turns | `request.messages().size()`              | `min((n − 1) / 19, 1)`     | 0.10   |
| Requested output   | `request.maxTokens()`                    | `min(maxTokens / 4000, 1)` | 0.10   |

`score = clamp(Σ weightᵢ · signalᵢ, 0, 1)`. Null/missing signal contributes 0. Weights and
ceilings are named constants. (Prompt-size / structured weights were tuned to 0.40 / 0.15 — from an
initial 0.35 / 0.20, sum unchanged at 1.0 — so a large multi-tool request without structured
content still crosses the "complex" routing threshold.)

`AiGatewayFacadeImpl` calls the injected `PromptComplexityScorer` instead of the inline
`estimatePromptComplexity`, which is deleted. The interface is the seam for a future
embedding-based scorer.

### 4. Fallback

If the scorer throws or is unavailable, route to the **most capable** present deployment (highest
tier), matching the page's "falls back to the most capable model in your policy." The
facade/strategy wraps scoring so a scorer failure degrades to max-capability rather than erroring
the request.

## Components

| Unit | Module | Responsibility |
|------|--------|----------------|
| `AiGatewayModelTier` (enum) | `platform-ai-gateway-api` | The five cost tiers + ordering |
| Tier classifier | `platform-ai-gateway-service` | `AiGatewayModel` → tier by `outputCostPerMTokens` |
| `PromptComplexityScorer` (interface) | `platform-ai-gateway-api` | `request` → 0–1 score |
| `DeterministicPromptComplexityScorer` | `platform-ai-gateway-service` | Model-free weighted heuristic |
| `IntelligentRoutingStrategy` (modified) | `platform-ai-gateway-service` | Score → present-tier mapping per axis |
| `AiGatewayFacadeImpl` (modified) | `automation-ai-gateway-service` | Inject scorer; remove inline method |

## Data Flow

`chatCompletionWithRouting` → `PromptComplexityScorer.score(request)` → build
`AiGatewayRoutingContext(..., score, ...)` → `AiGatewayRouter.route(strategy, deployments, ctx)`
→ `IntelligentRoutingStrategy`: classify deployments into present tiers → map score to a tier per
strategy → pick deployment in that tier. Scorer failure → most-capable deployment.

## Testing

- **Tier classifier** — boundary costs (4.99 vs 5.00; 2.00; 1.50; 0.10; 0.099; null → most capable).
- **Mapping per strategy** — simple prompt (low score) → cheap present tier; complex prompt
  (high score) → Frontier; `INTELLIGENT_BALANCED` linear midpoint case; policies with 2 / 3 / 5
  present tiers; missing-tier fallthrough to nearest present tier.
- **`DeterministicPromptComplexityScorer`** — monotonic per signal; code-bearing prompt scores
  above equal-length prose; clamp bounds; null fields contribute 0.
- **Fallback** — scorer throwing → most-capable deployment selected.

All units are pure / context-driven; no Spring context required for these tests.

## Risks / Open Questions

- Tier thresholds are absolute USD and may drift as model pricing moves; they live as named
  constants for easy revision. (Merge owns the same risk.)
- The deterministic scorer is a proxy for difficulty, not a trained complexity model; the
  `PromptComplexityScorer` seam is the intended upgrade path to an embedding-based scorer.
