# AI Model Catalog Extraction

**Status:** Design
**Date:** 2026-08-12
**Scope:** EE

Rename `ai_gateway_model` to `ai_model` and move the catalog out of the AI Gateway into a standalone
`platform-ai-model-catalog` module, then use it to give model pickers readable labels.

| Phase  | Change                                                                              |
| ------ | ----------------------------------------------------------------------------------- |
| **P1** | Establish whether the table is released; pick the migration strategy from that      |
| **P2** | New `platform-ai-model-catalog` module (`-api` / `-service`), catalog moved into it |
| **P3** | Rename table and types; repoint every consumer                                      |
| **P4** | Populate model labels in `aiProviderCatalog`                                        |

## Problem

### P0 — the catalog is named and housed as if only the gateway used it

`ai_gateway_model` is the models.dev-backed catalog landed on 2026-08-10. Its name and module say it
belongs to the AI Gateway, but it does not:

| Module                                      | Files referencing `AiGatewayModel` / `ai_gateway_model` |
| ------------------------------------------- | ------------------------------------------------------- |
| `platform-ai-gateway-service`               | 33                                                      |
| `platform-ai-gateway-api`                   | 14                                                      |
| `automation-ai-gateway-service`             | 10                                                      |
| `automation-ai-gateway-graphql`             | 8                                                       |
| `platform-ai-guardrails-service`            | 4                                                       |
| `automation-ai-gateway-public-rest`         | 2                                                       |
| `automation-ai-gateway-api`                 | 2                                                       |
| `automation-ai-eval-experiment-public-rest` | 1                                                       |
| `ai-hub-service`                            | 1                                                       |
| **Total (server)**                          | **75**                                                  |
| Client, excluding generated                 | 7                                                       |

The four hits in `platform-ai-guardrails-service` are the load-bearing ones. Guardrails is a
standalone module deliberately decoupled from `bytechef.ai.gateway.enabled` — it is registered
unconditionally so the agent surfaces work with the gateway switched off. It reaching into a
gateway-named catalog is the concrete evidence that the catalog is platform-level, not gateway-level.

### P4 — model pickers show raw ids

`ModelPicker` renders `claude-opus-4-1-20250805` rather than `Claude Opus 4.1`.

The client is **already correct**: it renders `{model.label || model.name}` at
`ModelPicker.tsx:356`, and the same fallback appears at three more sites for the trigger label. The
labels are empty because the models come from `aiProviderCatalog` — resolved by
`AiProviderCatalogFacade` in `platform-configuration` — which does not populate them, not from
`aiGatewayModels`.

This is why P4 belongs to this spec rather than standing alone: the fix edits the facade that joins
against the very catalog P2/P3 move and rename. Done separately, the two changes conflict on the
same seam.

## Decisions

### D1 — Settle released-or-not before writing any migration

`ai_gateway_model` landed 2026-08-10, which is recent enough that it may not have shipped. The answer
selects the strategy outright, so it is step one, not a detail:

```
git ls-tree -r --name-only <latest-tag> | grep ai_gateway_model
git merge-base --is-ancestor <introducing-commit> upstream/master
```

- **Unreleased** — edit the init changelog in place. Cheapest, and the convention CLAUDE.md already
  sanctions once absence from every release tag is proven.
- **Released** — a new changeset renaming the table. Never rewrite what customers have run.

In-place init edits drift local dev databases two ways (schema and stale md5sums);
`scripts/dev/sync-local-schema-after-collapse.sh` patches both, idempotently.

### D2 — The gateway depends on the catalog, never the reverse

`platform-ai-model-catalog` must not know the gateway exists. Guardrails already consumes the catalog
without wanting the gateway, and the gateway's own toggle (`bytechef.ai.gateway.enabled`, default
false) must not gate catalog availability — the same split already drawn for
`platform-ai-guardrails`, which registers unconditionally and stays inert.

### D3 — Module shape follows the existing platform convention

`-api` (domain, service interfaces, SPI) and `-service` (impls, Liquibase, autoconfiguration), per
`platform-ai-guardrails`. That means: `settings.gradle.kts` entries, an `@AutoConfiguration` class
with `@EnableJdbcRepositories` and `@ConditionalOnBean(AbstractJdbcConfiguration.class)`, and
registration in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
The changelog moves with the service module; delete stale copies from `build/resources/` afterwards,
or Liquibase sees both the old and new paths on the classpath.

### D4 — Labels fall back to the id, and the id stays visible

An uncataloged model must still render. `label` falls back to the model id, and where space allows
the id remains as secondary text: it is what API configurations reference, so hiding it entirely
would make a picker selection hard to reconcile with a config file.

## Design

### P2/P3 — Server

- New `platform-ai-model-catalog-api` / `-service` under `server/ee/libs/platform/platform-ai/`.
- `AiGatewayModel` → `AiModel`; `ai_gateway_model` → `ai_model`; repository, service, facade and
  DTOs renamed to match.
- GraphQL: schema field and operation renames in `automation-ai-gateway-graphql`, then
  `cd client && npx graphql-codegen`. Commit the operations and the regenerated file separately.
- The gateway keeps its own model-routing concerns; only the catalog moves.

### P4 — Labels

`AiProviderCatalogFacade` joins its items against the catalog by model id and fills `label` from the
models.dev display name. No client change is required.

## Testing

- Existing catalog tests move with the module and are renamed; the role-gated CRUD coverage from the
  2026-08-10 work must survive the move intact, not be rewritten.
- An `*IntTest` proving the renamed table is created from scratch — Testcontainers builds the schema
  fresh, which is stronger evidence than any changelog reading. The `liquibase` Spring profile does
  **not** apply migrations via `bootRun`; it exits 0 having created nothing.
- `AiProviderCatalogGraphQlControllerTest` gains a case for a model with a catalog entry (labelled)
  and one without (falls back to the id).
- Verify with `./gradlew check` — not `:module:test`, which skips static analysis and integration
  tests — plus `cd client && npm run check`.

## Notes

`0_732` is being rewritten by concurrent sessions; rebase with
`git rebase --onto <new-base> <old-fork-point> <branch>` rather than a plain `git rebase 0_732`,
which replays hundreds of unrelated commits.

Commit prefixes track the feature area, not the branch. This work has no ticket, so `-`.

## As built

Recorded after implementation. Where the design assumed something that turned out not to exist, the
assumption is corrected here rather than quietly dropped.

- **A CE `platform-ai-model-catalog` module already existed** (`server/libs/platform/platform-ai/`,
  packages `com.bytechef.platform.ai.model.catalog`): the in-memory models.dev catalog (`ModelCatalog`,
  `CatalogModel`), landed with the same 2026-08-10 work. `ai_gateway_model` was the *persisted* catalog
  the gateway's reconciler populates **from** it. The new EE module is therefore its persisted EE twin —
  same-name CE/EE twins per the `platform-configuration` precedent — not the first module of that name.
- **P1 verdict: unreleased.** Absent from the latest release tag `v0.31.3` (= `upstream/stable`) and
  from `master`; the table exists only on the 0_732 line. Init changelogs were edited in place, and
  `scripts/dev/sync-local-schema-after-collapse.sh` gained an `ai_gateway_model → ai_model` rename block.
- **The reconciler stayed in the gateway** (renamed `AiModelCatalogReconciler*`), along with
  `AiModelsDevProviderIds` and `CapabilitiesEncoder`. It iterates gateway provider registrations and
  switches on `AiGatewayProviderType`; moving it would have forced a provider SPI the design never asked
  for, while D2 only forbids the catalog→gateway direction. What moved is the data layer: `AiModel`,
  `AiModelRepository`, `AiModelService(+Impl)`, the `ai_model` changeset, plus `AiModelPinningTest`.
- **The moved service dropped the `bytechef.ai.gateway.enabled` gate.** Before the extraction,
  `AiGatewayModelServiceImpl` was `@ConditionalOnProperty` on the gateway toggle — guardrails could not
  actually resolve models with the gateway off, contrary to this spec's premise. `AiModelServiceImpl`
  registers under `@ConditionalOnEEVersion` alone; the module's `AiModelServiceIntTest` runs without the
  toggle to pin that.
- **Delete cascade inverted through a new `AiModelDeleteListener` SPI** (catalog-api). The old service
  called `AiGatewayModelDeploymentService.deleteByModelId` directly — a catalog→gateway dependency. The
  gateway now contributes `AiGatewayModelDeploymentCleanupListener`.
- **FK ownership**: `ai_model.provider_id` is a bare NOT NULL column in the catalog changelog; the
  gateway's changelog adds `fk_ai_model_provider` and re-points `fk_ai_llm_gw_deploy_model` at `ai_model`,
  both behind `tableExists ai_model` preconditions (the notification-FK precedent), since the gateway may
  know the catalog but never the reverse.
- **P4 joins the CE `ModelCatalog`, not the moved table.** `AiModel` has no display-name field (the
  reconciler never wrote one), and its rows exist only where gateway providers are registered — a
  deployment that never enabled the gateway would get no labels. `AiProviderFacadeImpl` resolves
  `ObjectProvider<ModelCatalog>` per request and maps `Provider` → models.dev id via an exhaustive
  switch (ids verified against the bundled snapshot; OLLAMA/HUGGING_FACE/STABILITY are uncataloged).
  Label precedence: models.dev display name → component option label → model id, never blank.
- **Names deliberately kept**: `AiGatewayModelDeployment*`, `AiGatewayModelTier*` (model-routing
  concerns), `AiGatewayModelApiController` (the OpenAI-compatible public REST surface), the
  `ai_gateway_model_deployment` table, and the client page components `AiGatewayModels`/
  `AiGatewayModelDialog` (they name the AI Gateway page's Models tab, whose GraphQL operations and types
  did rename).
- **The substantive label tests live in `AiProviderFacadeCatalogTest`** — the label fill happens in
  `AiProviderFacadeImpl`, and `AiProviderCatalogGraphQlControllerTest` is a pure delegation test; it
  gained the labelled/fallback fixture pair the Testing section asked for, as a passthrough assertion.
- **D4 won over "No client change is required"**: with labels now populated, the picker would have hidden
  the raw model id entirely, so the dropdown item renders it as secondary muted text (trigger labels stay
  label-only — no space). One new `ModelPicker` test pins it.
- The spec's `ai-hub-service: 1` reference was commented-out code in `PersonalAgentSaveValidator`; the
  comments were renamed with everything else.
