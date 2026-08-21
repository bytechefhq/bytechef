# Agentic AI component (Embabel GOAP)

Opt-in GOAP planner component: profile gating, blackboard bindings, checkpoint resume.

Extracted from `CLAUDE.md` to keep that file within its size budget;
read this before working in the areas below.

### Agentic AI component (Embabel GOAP, opt-in)

- `server/libs/modules/components/ai/agentic-ai` wraps Embabel **1.0.0**'s GOAP planner
  (`EmbabelAgentRunner.kt`, the repo's only Kotlin production code). The component is OFF by
  default and opt-in via the `agentic` Spring profile — no provider API key needed:
  `AgentPlatformAutoConfiguration` sits in the default `spring.autoconfigure.exclude`
  (server-app, worker-app, and the liquibase profile) and `application-agentic.yml` re-enables it
  (the profile file mirrors the default exclude list minus the Embabel entry — profile property
  values replace wholesale, keep them in sync) plus sets `embabel.models.default-llm:
  bytechef-canvas`, the inert placeholder `SpringAiLlmService` registered by
  `AgenticAiPlatformConfiguration` purely so Embabel's `ConfigurableModelProvider` (which
  hard-fails with zero models) can boot. The handler stays `@ConditionalOnBean(AgentPlatform.class)`.
- **All LLM calls use the canvas-selected MODEL cluster element** (required, with its ByteChef
  connection): action prompts run via `ChatClient.create(chatModel)` with the step's Spring AI
  ToolCallbacks (client-side tool loop), and smart-goal evaluation is `CanvasSmartGoalCondition`
  (an Embabel `Condition` backed by the same ChatModel) instead of Embabel's `PromptCondition`.
  Embabel's model registry/LLM layer is never invoked, so its token/cost budget can't observe
  usage — the action-count budget is the effective planner cap. ByteChef cost tracking DOES see
  agentic runs: a thread-safe `TokenUsageAccumulator` aggregates ChatResponse usage across all
  calls (Embabel may execute actions off-thread) and flushes once into the thread-local
  `TokenUsageHolder` on the perform thread, even when the plan fails.
- **Mid-plan crash resume**: after every completed GOAP action, produced bindings are
  checkpointed (fingerprint-guarded, fail-open) to CURRENT_EXECUTION data storage
  (`agenticAiBlackboardCheckpoint`; untyped values as content strings, typed as tagged maps); a
  crash-resumed job reseeds them so the planner skips completed actions. Cleared on success;
  editor/job-less runs skip checkpointing.
- Blackboard carriers: untyped bindings use the `Binding(content)` data class; bindings whose
  producers declare an `outputSchema` on the ACTION cluster element become **typed** — an Embabel
  `DynamicType` named after the binding (PascalCase), carried as a `_typeName`-tagged map.
  Typed/untyped actions are built as `DynamicTransformationAction` (custom `AbstractAction` with
  string-typed `IoBinding`s, since Embabel ships no `DynamicType`-aware action factory);
  fully-untyped actions keep the stock `promptedTransformer<Binding, Binding>` path.
- Execution-time `getValue` type matching is STRICT (a tagged map only satisfies its `_typeName`,
  a `Binding` only the Binding class) even though the planner's world-state determiner
  short-circuits maps — so all producers of one binding name must agree on typed-ness and schema;
  the runner validates this up front (also: no `:` in binding names, no schema on `userGoal`).
  A typed goal binding makes the run action return the parsed object (tag keys stripped) instead
  of a string. Analysis + implementation status:
  `docs/superpowers/specs/2026-07-20-embabel-koog-goap-domain-model-analysis.md` §4.
