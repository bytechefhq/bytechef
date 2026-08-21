# Embabel vs Koog: dynamic domain models and GOAP inside ByteChef

Date: 2026-07-20. Status: analysis (no build). **Revised same day for Embabel 1.0.0 — see §4;
it partially supersedes §1's "compile-time types only" conclusion.** Questions answered:

1. Can we — via some Kotlin feature — **dynamically** express the typed domain input/output
   models Embabel requires (Embabel does not support Maps)?
2. How does JetBrains **Koog** (https://github.com/JetBrains/koog) behave regarding domain
   models?
3. Is it easier to implement **GOAP inside ByteChef** than to keep going through Embabel?

## 0. Where we are today (recon of our own code)

The `agenticAi` component (`server/libs/modules/components/ai/agentic-ai/`, commit
`e52563d005`) already wraps **Embabel 0.3.5** with real GOAP: `EmbabelAgentRunner.kt` (the
only Kotlin production code in the server) maps canvas-defined `ActionStep`s to Embabel
`promptedTransformer` actions, seeds a `userGoal` binding, and lets Embabel's planner
backward-chain over **named blackboard bindings**. STRUCTURAL + experimental SMART goal
modes, cost-weighted path selection, budgets, fail-fast validation. It is currently **dark**:
`AgentPlatformAutoConfiguration` is excluded in every app YAML, so the
`@ConditionalOnBean(AgentPlatform.class)` handler never registers.

Crucially, our integration **did not adopt Embabel's typed domain model**. Everything flows
through one carrier type — `Binding(content: String)` — discriminated by binding *name*.
That is precisely the "magic map with one key" Embabel's design philosophy rejects; we pay
Embabel's integration cost without getting its typed-blackboard benefit.

## 1. Can Kotlin dynamically express Embabel's domain models?

Embabel's contract: actions are methods whose **parameters and return types are JVM classes**
(records/data classes), optionally annotated with Jackson `@JsonClassDescription` /
`@JsonPropertyDescription` so the LLM understands them. The GOAP planner chains actions by
**type** (+ name) on the blackboard. "Everything is strongly typed … no more magic maps" is a
design pillar; there is no documented runtime-dynamic type facility.

ByteChef's problem: our domain models are **runtime data** — a workflow author defines
shapes in the editor (JSON-schema properties, sample outputs), per workflow, editable at any
time. There is no compile-time class to hand Embabel. The options for conjuring one:

| Option | How | Verdict |
|---|---|---|
| **Kotlin scripting / embedded K2 compiler** | Generate `data class` source from the editor schema, compile at runtime (JSR-223 / `kotlin-scripting-jvm`), load per workflow version | Works, but heavy: the compiler adds ~50 MB + seconds of latency per (re)compile, needs a classloader per workflow revision, and cache invalidation on every canvas edit. GraalVM-unfriendly. |
| **ByteBuddy / ASM bytecode generation** | Emit a JavaBean with Jackson annotations directly from the JSON schema; no compiler | Lighter and feasible — this is the realistic path if we must feed Embabel real types. Still costs: per-revision classloaders (type identity breaks across edits), `equals`/`hashCode` discipline for planner state, debugging opacity, and Embabel schema derivation must accept the generated metadata. |
| **Kotlin language feature (delegates, `dynamic`, etc.)** | — | **No.** Kotlin/JVM has no runtime class synthesis in the language; `dynamic` exists only on Kotlin/JS. Delegated properties, reflection, and DSLs all still require compile-time classes. The DSL *calls* can be made at runtime (our runner already builds actions dynamically) — the **types** cannot. |
| **Ask Embabel for schema-based bindings** | Upstream feature request: bind blackboard entries by (name, JSON schema) instead of JVM type | Cleanest long-term, but out of our hands and against the framework's stated philosophy. |

**Answer: not with a language feature.** Runtime bytecode generation (ByteBuddy) can
manufacture the classes, but it is a fight against Embabel's grain — the framework's core
bet (compile-time typed domain models with behavior) is structurally opposed to ByteChef's
core bet (author-defined models at runtime).

## 2. How Koog handles domain models

Koog 1.0 (JetBrains, Apache 2.0, shipped at KotlinConf 2026, one-year API-stability
guarantee, first-class **Java API** and Spring Boot integration) splits the concern:

- **Structured LLM output** is compile-time typed like Embabel: `@Serializable` Kotlin data
  classes + `@LLMDescription` annotations; schema generators exist but still require an
  underlying class. Maps are allowed as *field types*, not as the schema itself.
- **Strategy graphs** are typed FSMs (`strategy<Input, Output>`), nodes/edges with generic
  types; edges can transform between node types.
- **The GOAP planner is different — and this is the important part.** In
  `agents/agents-planner` (`ai.koog.agents.planner.goap`):

  ```kotlin
  public typealias Condition<State> = (State) -> Boolean
  public typealias Belief<State>    = (State) -> State
  public typealias Cost<State>      = (State) -> Double
  public typealias Execute<State>   = suspend (AIAgentPlannerContext, State) -> State
  ```

  `Action`, `Goal`, and `GOAPPlanner<Input, Output, State>` are **generic over an arbitrary
  `State` type**; the only requirement is extending `GoapAgentState<Input, Output>` (two
  members) and value-equality (the A* search keys score maps by `State`). Preconditions,
  effects ("beliefs"), and costs are plain lambdas. Builders are `@JvmStatic` — callable from
  Java. The planner is a straight **A\*** over goal costs with a replan / execute-one-step /
  check-completion loop, and plans serialize as `List<String>` action names ("suitable for
  persistence" per the source docs).

**Consequence:** a ByteChef state class like
`data class BindingsState(val bindings: Map<String, Any?>)` satisfies Koog's GOAP contract
directly. Canvas-defined actions become `Action.builder<BindingsState>()` calls with
lambda preconditions (`state.bindings.containsKey("research")`) — **no bytecode generation,
no compile-time domain classes, no annotations**. Koog explicitly supports the dynamic
shape Embabel forbids, while still offering typed structured output where we *do* have
fixed shapes.

Also noteworthy for the checkpoint-resume workstream: Koog ships **agent persistence**
(`agents-features-snapshot`, `agents-features-persistence-jdbc`) — per-node checkpoints
(opt-in continuous mode), pluggable storage providers, rollback strategies including
tool-side-effect rollback via a `RollbackToolRegistry`, requiring unique node names.

## 3. Is implementing GOAP inside ByteChef easier than Embabel?

The honest comparison, given that our Embabel integration already reduced the domain model
to named string bindings:

**What GOAP actually needs** (all three implementations agree): a world-state value with
equality; actions with `precondition(state)`, `effect(state) -> state'`, `cost`; goals with
`condition(state)`; A* (or backward chaining) over that graph; a replan-after-each-step
loop. Over *named bindings* — which is what our canvas gives us — this is **~200-300 lines
of dependency-free Java**. Our `EmbabelAgentRunner` already contains most of the hard parts
that are NOT the planner: prompt assembly, tool wiring (`FromAiInputSchemaUtils` JSON-schema
tool inputs), budgets, validation, SMART-mode goal judging.

| | Keep Embabel | Adopt Koog GOAP | Native GOAP in ByteChef |
|---|---|---|---|
| Domain model fit | Poor — typed classes required; we bypass with string bindings (or ByteBuddy codegen) | Good — `State` is generic; bindings map works as-is | Perfect — we define the state model |
| Dependency weight | Heavy: Embabel platform + Spring auto-config we must exclude everywhere today; Kotlin toolchain in one module | Medium: `koog-agents` Apache-2.0 JARs, Java API (Kotlin optional!), Spring Boot starter | Zero new deps |
| Planner quality | Mature GOAP incl. OODA replanning | A* GOAP + LLM-planner variants, replanning loop, serializable plans | We write and own it (small, testable — pin with the same validation tests we already have) |
| Extras we'd inherit | Budgets (we use) | Checkpoint/persistence, OTel, Spring AI interop | — |
| Runtime enablement | Currently dark (auto-config excluded) | Normal Spring bean wiring | Trivial |
| Model-call layer | Embabel's own LLM layer (separate config from our AI providers) | Koog's executors — or keep ours and use ONLY `agents-planner` for the algorithm | Our existing Spring AI / AI-gateway path unchanged |

**Recommendation:** yes — implementing GOAP inside ByteChef is easier than making Embabel
fit, *because our binding-based state model is already Map-shaped*. Two credible routes:

1. **Native planner (preferred):** extract `EmbabelAgentRunner`'s action/goal/validation
   model, replace Embabel's planner with a small in-house A* over `Map<String, Binding>`
   states, keep our Spring AI execution path. Deletes the Embabel dependency, the Kotlin
   toolchain requirement, and the excluded-auto-config wart; the component's canvas contract
   (`ActionStep`, bindings, goal modes, budgets) is unchanged.
2. **Koog `agents-planner` as the algorithm library:** same shape, but reuse Koog's tested
   `GOAPPlanner`/A* with a `BindingsState`, via the Java API. Buys serializable plans and a
   maintained planner for one small Apache-2.0 dependency — and opens the door to Koog's
   checkpoint machinery later. Costs a new framework dependency whose agent runtime we would
   deliberately NOT use (planner module only) — keep that boundary explicit if chosen.

Either way, Embabel remains the reference for semantics (we keep the behavioral tests), but
the typed-domain-model impedance mismatch — the reason the integration is dark and
string-typed — goes away.

## 4. Update: Embabel 1.0.0 (source-verified 2026-07-20)

Embabel shipped **1.0.0 GA** (release train 0.4.0 "Curdimurka" → 0.5.0 "Darwin" → 1.0.0-RC1
"Euroa" → 1.0.0; our module pins 0.3.5). Verified against the 1.0 source tree, not release
notes. This changes parts of §1 and §3.

### 4.1 The "no magic maps" wall now has a door: `DynamicType`

`com.embabel.agent.core.DynamicType` is a first-class `DomainType` ("enables interop with
non-JVM types"): `name` + `description` + `ownProperties: List<PropertyDefinition>` +
`parents`, identity purely by name, Jackson-serializable. On the blackboard, a dynamic-typed
value is a **plain `Map<String, Any?>` tagged with `_typeName`** (optionally `_typeLabels`)
or a `DomainInstance` (`domainType` + `properties` map) — `Blackboard.satisfiesType` matches
all three shapes. `IoBinding` remains a `"name:type"` **string**, and the GOAP `WorldState`
is a map keyed by those strings — the planner never distinguished JVM from dynamic types.

**So §1's "only via bytecode generation" is wrong for 1.0**: a ByteChef workflow's
editor-defined shapes can be expressed as `DynamicType`s with property lists, and values as
tagged maps, with the planner chaining on them natively. No ByteBuddy, no Kotlin compiler.

### 4.2 The door is real but half-finished (all source-verified)

- **No shipped action factory accepts a `DynamicType`** — `promptedTransformer` /
  `TransformationAction` still require reified JVM classes. Dynamic-typed actions mean
  implementing Embabel's `Action` interface ourselves and emitting
  `IoBinding("x", "OurTypeName")` by hand (supported, just not sugared).
- **LLM structured output still needs a JVM carrier**: `PromptRunner.createObject` has no
  `DomainType` overload. `PropertyDefinition → JSON schema` generation exists
  (`DomainTypeInputSchema`) but is wired only for **tool input schemas**, not for model
  output. Our runner would keep generating output instructions itself (as it already does).
- **No validation of Map values against `PropertyDefinition`** — and
  `BlackboardWorldStateDeterminer` short-circuits: any `Map` value satisfies any type
  condition (`// TODO may want to add type checking here`). Type discrimination for tagged
  maps rests on the `_typeName` tag during binding resolution, not during planning.
- **The "agent-spec" from the release notes is not an agent-from-YAML format**: the only
  data-driven definition shipped is agentskills.io `SKILL.md` parsing
  (`embabel-agent-skills`); `Agent`/`AgentProcess` are deliberately non-JSON-serializable
  (`ComputerSaysNoSerializer`). Playbooks are progressive tool-unlocking, not specs.

### 4.3 Upgrade path for our runner (0.3.5 → 1.0.0)

All eight `com.embabel.*` imports in `EmbabelAgentRunner.kt` survive with identical packages
(`Budget` and `LlmCall` moved files, not packages; `PromptCondition` remains
`experimental.primitive`). One real change: `promptedTransformer` now takes Embabel's own
`Tool` instead of Spring AI `ToolCallback` — the shipped bidirectional adapter
(`ToolCallback.toEmbabelTool()` in `spi/support/springai/SpringToolCallbackAdapter.kt`)
covers our wrapping. Also relevant given the checkpoint-resume workstream: 1.0's
`AgentProcessRepository` persistence SPI ships **in-memory only** — Embabel adds no durable
agent-process checkpointing either.

### 4.4 Revised verdict

The 0.3.5-era argument "Embabel structurally can't hold our runtime domain models" no longer
holds. Three options, re-ranked:

1. **Upgrade to Embabel 1.0 + `DynamicType`/tagged maps** — upgrade our string
   `Binding(content)` carrier to `_typeName`-tagged maps with per-workflow `DynamicType`
   property schemas. The planner gains real type discrimination; the glue we must write
   (custom `Action` impl, output-schema prompting) is comparable to what the runner already
   contains. Keeps the mature planner + budgets; the auto-config exclusion question remains.
2. **Native GOAP** — still the smallest dependency footprint and now the *only* remaining
   argument is dependency weight + the dark-wiring wart, not capability.
3. **Koog `agents-planner`** — unchanged: generic-state A*, Java-callable, serializable
   plans; superior if we also want its checkpoint machinery.

Decision input, not made here: if the `agenticAi` component is to be lit up soon, option 1
is now the least-work path to a *typed* GOAP canvas; if the Embabel dependency was the
reason it stayed dark, option 2 remains the recommendation from §3.

> **Status 2026-07-21: option 1 is IMPLEMENTED.** The module is on Embabel 1.0.0 (zero source
> changes needed for the bump itself) and the runner now supports typed bindings end to end:
> the ACTION cluster element gained an optional `outputSchema` (name/type/description entries);
> a binding whose producers declare a schema becomes a `DynamicType` named after the binding,
> carried as a `_typeName`-tagged map. Actions touching a typed binding are built as
> `DynamicTransformationAction` (custom `AbstractAction` subclass with string-typed `IoBinding`s
> — the "no shipped factory accepts DynamicType" gap from §4.2, closed by hand as predicted);
> the model gets a property-list instruction block and its JSON reply is parsed, tagged, and
> bound. A typed goal binding makes the run action return the structured object (tag keys
> stripped) instead of a string. Because execution-time `getValue` matching is strict, the
> runner validates producer/consumer schema agreement per binding name up front (mixed
> typed/untyped producers, conflicting schemas, schemas on `userGoal`, and `:` in binding names
> are all rejected with actionable messages). The auto-config exclusion question is resolved as an
> **opt-in `agentic` Spring profile** with **no provider key required**: the platform's
> zero-models boot failure is satisfied by an inert `bytechef-canvas` placeholder `LlmService`
> (`AgenticAiPlatformConfiguration`) that the profile points `embabel.models.default-llm` at,
> and all real LLM calls go through the canvas-selected MODEL cluster element — action prompts
> via `ChatClient.create(chatModel)` with the step's Spring AI ToolCallbacks, smart-goal
> evaluation via `CanvasSmartGoalCondition` (an Embabel `Condition` over the same ChatModel,
> replacing `PromptCondition`). This closes the §4.2 "LLM structured output still needs Embabel's
> model layer" concern by bypassing that layer entirely; the trade-off is that Embabel's
> token/cost budget cannot observe the direct calls, leaving the action-count budget as the
> effective cap.

## Sources

- Repo recon: `server/libs/modules/components/ai/agentic-ai/` (see §0 for files).
- https://github.com/embabel/embabel-agent — programming model, GOAP, "no more magic maps";
  1.0.0 source tree read directly (`core/DynamicType.kt`, `core/IoBinding.kt`,
  `core/Blackboard.kt`, `core/support/BlackboardWorldStateDeterminer.kt`,
  `api/dsl/AgentBuilder.kt`, `api/tool/Tool.kt` + `SpringToolCallbackAdapter.kt`,
  `core/ProcessOptions.kt`, `core/AgentProcessRepository.kt`,
  `embabel-agent-skills/.../spec/SkillDefinition.kt`).
- https://github.com/JetBrains/koog — `agents/agents-planner` sources (`Entities.kt`,
  `GOAPPlanner.kt`, `GoapAgentState.kt`, `GOAPPlannerAgentTest.kt`), `docs/docs/structured-output.md`.
- Koog agent persistence: https://docs.koog.ai/agent-persistence/ (checkpoints, storage
  providers, rollback), Koog 1.0 / Java API announcements (JetBrains blog, 2026-03/04).
