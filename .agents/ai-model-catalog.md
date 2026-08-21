# AI model catalog

CE models.dev snapshot vs the EE persisted twin, and the one-way gateway dependency.

Extracted from `CLAUDE.md` to keep that file within its size budget;
read this before working in the areas below.

### AI model catalog (CE snapshot + EE persisted twin, 2026-08-12)

Two same-name modules, deliberately (the `platform-configuration` CE/EE twin precedent):

- **CE `platform-ai-model-catalog`** (`server/libs/platform/platform-ai/`) — the in-memory models.dev
  snapshot: `ModelCatalog` SPI, `CatalogModel(id, name, …)` where `name` is the human display label.
- **EE `platform-ai-model-catalog`** (`server/ee/libs/platform/platform-ai/`) — the persisted catalog,
  extracted from the AI Gateway: `AiModel` (`ai_model` table, formerly `ai_gateway_model`),
  `AiModelRepository`, `AiModelService(+Impl)`. Registers under `@ConditionalOnEEVersion` alone — the
  gateway's `bytechef.ai.gateway.enabled` toggle must never gate catalog availability.

Dependency direction is one-way: **the gateway knows the catalog, never the reverse.** Consequences:
the reconciler (`AiModelCatalogReconciler*`, populates `ai_model` from models.dev per enabled gateway
provider registration) stays IN the gateway; `ai_model.provider_id` is a bare NOT NULL column in the
catalog changelog (`platform/ai/model_catalog`, loaded before `platform/ai/gateway` in master.xml)
while the GATEWAY changelog adds `fk_ai_model_provider` and the deployment FK behind `tableExists
ai_model` preconditions; and delete cascade inverts through the `AiModelDeleteListener` SPI
(catalog-api) that the gateway implements with `AiGatewayModelDeploymentCleanupListener`. Rows exist
only where gateway providers are registered — an empty table on gateway-less deployments is by design.

Names that deliberately keep the `AiGatewayModel` prefix: `AiGatewayModelDeployment*`,
`AiGatewayModelTier*` (model-routing concerns), `AiGatewayModelApiController` (OpenAI-compatible
public REST), and the client page components `AiGatewayModels`/`AiGatewayModelDialog` (their GraphQL
operations did rename: `aiModels`, `workspaceAiModels`, `AiModel`, `reconcileAiModelCatalog`, …).

`aiProviderCatalog` model labels (`ModelPicker`) come from the **CE** catalog: `AiProviderFacadeImpl`
resolves `ObjectProvider<ModelCatalog>` per request and maps `Provider` → models.dev id via an
exhaustive no-default switch (`AiModel` has no display-name field, so the EE table is not a label
source). Label precedence: models.dev display name → component option label → model id, never blank
(`AiProviderModel.label` is non-null in the schema). Spec:
`docs/superpowers/specs/2026-08-12-ai-model-catalog-extraction-design.md` (see its As-built section).
