# Standalone AI Guardrails Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the AI Gateway's content guardrails into `platform-ai-guardrails` and enforce them, per workspace, on the canvas AI Agent and all AI Hub LLM turns, configured under a new Workspace Settings → AI → Guardrails page.

**Architecture:** The engine moves verbatim to an EE `platform-ai-guardrails` module; the gateway keeps its exact contract through a thin delegating adapter that layers its project overlay on top. Agents integrate through a Spring AI advisor: AI Hub adds it at the `AiHubSpringAIAgent` request spec (workspace from verified state), the CE canvas component through an optional-bean SPI whose EE impl resolves the workspace from the project deployment. Config is a property-backed workspace record (the `AiGatewayWorkspaceSettings` storage pattern) with a new `blockingMode`.

**Tech Stack:** Spring AI advisors (`CallAdvisor`/`StreamAdvisor`), Spring `PropertyService` (Scope.WORKSPACE), GraphQL settings pattern, React settings pages.

**Spec:** `docs/superpowers/specs/2026-07-31-ai-guardrails-standalone-design.md`

## Global Constraints

- All work lands on `0_732` as fresh commits (never amend; user commits in parallel).
- EE files: ByteChef Enterprise license header + `@version ee` javadoc tag (spotless keys off the tag CONTENT).
- **Plan-level resolution of a spec assumption:** `AiGatewayWorkspaceSettings` is a property-backed record (`PropertyService`, `Property.Scope.WORKSPACE`, `PROPERTY_KEY`), NOT a table. The new settings follow the same pattern — no liquibase work anywhere in this plan. The spec's "init changelogs edited in place" clause is therefore moot; note this in the spec's decisions log in the final task.
- Enum storage: `blockingMode` serialized in the property map as the enum NAME string (property maps are JSON; ordinal rule applies to DB INT columns, which we don't have here) — but keep the enum append-only anyway and pin it with an ordinal-stability test per convention.
- Gateway contract frozen: HTTP 422, request/response DTO methods, per-project overlay, existing tests pass UNMODIFIED (extraction proof).
- Effective policy additive: global `ApplicationProperties` ∪ workspace settings; project overlay gateway-only.
- Violations never echo offending content; exception/notice text names only the guardrail category.
- Gradle: never judge a piped run — redirect to a file, check `$?` on its own line, grep `^> Task .* FAILED`. `test` excludes `*IntTest*`; use `testIntegration` where IntTests exist. `./gradlew spotlessApply` before each server commit.
- Client: `cd client && npm run check` before each client commit; sort-keys is manual-fix; GraphQL enum values SCREAMING_SNAKE_CASE; codegen via `npx graphql-codegen`, operations and generated file committed separately.
- New GraphQL schema files must be added to `client/codegen.ts` schema array.

---

### Task 1: `platform-ai-guardrails` module + settings record + service

**Files:**
- Create: `server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-api/build.gradle.kts`
- Create: `.../platform-ai-guardrails-api/src/main/java/com/bytechef/ee/platform/ai/guardrails/domain/AiGuardrailsWorkspaceSettings.java`
- Create: `.../platform-ai-guardrails-api/src/main/java/com/bytechef/ee/platform/ai/guardrails/service/AiGuardrailsWorkspaceSettingsService.java`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-guardrails/platform-ai-guardrails-service/build.gradle.kts`
- Create: `.../platform-ai-guardrails-service/src/main/java/com/bytechef/ee/platform/ai/guardrails/service/AiGuardrailsWorkspaceSettingsServiceImpl.java`
- Modify: `settings.gradle.kts` (two includes, alphabetical near the platform-ai block)
- Test: `.../platform-ai-guardrails-service/src/test/java/com/bytechef/ee/platform/ai/guardrails/service/AiGuardrailsWorkspaceSettingsServiceTest.java`
- Test: `.../platform-ai-guardrails-api/src/test/java/com/bytechef/ee/platform/ai/guardrails/domain/BlockingModeStabilityTest.java`

**Interfaces:**
- Produces (later tasks consume verbatim):

```java
public record AiGuardrailsWorkspaceSettings(
    Long workspaceId,            // null = tenant default
    Boolean redactPii,
    Boolean redactSecrets,
    String blockedTerms,         // comma-separated, same format as the gateway field
    Boolean moderationEnabled,
    Boolean injectionDetectionEnabled,
    Boolean scanResponses,
    BlockingMode blockingMode) {

    public static final String PROPERTY_KEY = "ai_guardrails_workspace_settings";

    public enum BlockingMode {
        BLOCK,                   // ordinal 0, default
        REDACT_AND_CONTINUE      // ordinal 1 — append-only
    }
}
```

```java
public interface AiGuardrailsWorkspaceSettingsService {
    Optional<AiGuardrailsWorkspaceSettings> fetchSettings(@Nullable Long workspaceId);
    AiGuardrailsWorkspaceSettings saveSettings(AiGuardrailsWorkspaceSettings settings);
}
```

- [ ] **Step 1: Scaffold the two modules.** Copy the build.gradle.kts shape from `platform-ai-gateway-api`/`-service` (same parent dir); `-api` deps: `platform-configuration-api` is NOT needed in api (keep api dependency-light: only annotations); `-service` deps: `project(":server:libs:platform:platform-configuration:platform-configuration-api")` for `PropertyService`, plus the `-api` project. EE license header + `@version ee` on every class. Add both includes to `settings.gradle.kts` (pattern: `include("server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-api")`).

- [ ] **Step 2: Write the failing service test** (Mockito over `PropertyService`, mirroring `AiGatewayWorkspaceSettingsServiceImpl`'s storage):

```java
@Test
void testFetchSettingsReadsWorkspaceScopedProperty() {
    when(propertyService.fetchProperty(
        AiGuardrailsWorkspaceSettings.PROPERTY_KEY, Property.Scope.WORKSPACE, 7L))
            .thenReturn(Optional.of(property(Map.of("redactPii", true, "blockingMode", "REDACT_AND_CONTINUE"))));

    AiGuardrailsWorkspaceSettings settings = service.fetchSettings(7L).orElseThrow();

    assertThat(settings.redactPii()).isTrue();
    assertThat(settings.blockingMode()).isEqualTo(BlockingMode.REDACT_AND_CONTINUE);
}

@Test
void testFetchSettingsWithNullWorkspaceReadsTenantDefault() {
    // null workspaceId = tenant default row; the service must use a stable sentinel scope id
    // — mirror EXACTLY how AiGatewayWorkspaceSettingsServiceImpl handles scope ids and reuse it.
}

@Test
void testBlockingModeDefaultsToBlockWhenAbsent() { /* map without the key -> BlockingMode.BLOCK */ }
```

Read `AiGatewayWorkspaceSettingsServiceImpl` first and copy its map<->record conversion style (per-field KEY_ constants). If the gateway impl has no null-workspace handling, add it here as `Property.Scope.PLATFORM`-...no: use the same Scope.WORKSPACE with the convention the gateway uses; if the gateway simply never passes null, define: null workspaceId → `fetchProperty(..., Scope.WORKSPACE, 0L)` is WRONG — instead store the tenant default under `Property.Scope.PLATFORM` with scopeId 0. Decide by reading `Property.Scope` values and document the choice in the impl javadoc.

- [ ] **Step 3: Run to fail, implement, run to pass**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:test > /tmp/t1.log 2>&1; echo "exit=$?"
grep -cE "^> Task .* FAILED" /tmp/t1.log
```

- [ ] **Step 4: BlockingMode stability test** (pins append-only ordinals):

```java
@Test
void testBlockingModeOrdinalsArePinned() {
    assertThat(BlockingMode.BLOCK.ordinal()).isEqualTo(0);
    assertThat(BlockingMode.REDACT_AND_CONTINUE.ordinal()).isEqualTo(1);
    assertThat(BlockingMode.values()).hasSize(2);
}
```

- [ ] **Step 5: spotlessApply + commit**

```bash
./gradlew spotlessApply > /tmp/sp.log 2>&1; echo "exit=$?"
git add server/ee/libs/platform/platform-ai/platform-ai-guardrails settings.gradle.kts
git commit -m "Add the platform-ai-guardrails module with workspace settings and blocking mode"
```

---

### Task 2: Move the engine

**Files:**
- Create (moves, `git mv`, then rename classes): from `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/guardrail/`:
  - `AiGatewayGuardrails.java` → `.../platform-ai-guardrails-service/src/main/java/com/bytechef/ee/platform/ai/guardrails/AiGuardrails.java`
  - `PromptBasedInjectionClassifier.java`, `PromptBasedModerationClassifier.java`, `StreamingResponseRedactor.java` → same package
  - `AiGatewayGuardrailMetrics.java` → `AiGuardrailMetrics.java`
- Move engine tests from the gateway module's `guardrail/` test package alongside.
- Modify: `platform-ai-guardrails-service/build.gradle.kts` (add whatever deps the moved classes need — read their imports: `ApplicationProperties` (platform-configuration), the classifier interfaces + exception from `platform-ai-gateway-api`, Micrometer via `ObjectProvider<MeterRegistry>`).

**Interfaces:**
- Produces: `AiGuardrails` with the engine methods, gateway-DTO-free:

```java
public List<String> applyToInputs(List<String> inputs, @Nullable Long workspaceId);
public String scanResponseText(String text, @Nullable Long workspaceId);      // extracted from redactResponse's core
public StreamingResponseRedactor newStreamingResponseRedactor(@Nullable Long workspaceId);
public AiGuardrailsWorkspaceSettings.BlockingMode resolveBlockingMode(@Nullable Long workspaceId);
```

The gateway-DTO methods (`apply(AiGatewayChatCompletionRequest, ...)`, `redactResponse(AiGatewayChatCompletionResponse, ...)`) and every `projectId` parameter/overlay lookup DO NOT move — they stay for Task 3's adapter. Split the class: text-level core here; DTO+project layer stays behind.

- [ ] **Step 1:** `git mv` the five files + their tests; fix packages; rename `AiGatewayGuardrails`→`AiGuardrails`, `AiGatewayGuardrailMetrics`→`AiGuardrailMetrics` (metric name becomes `bytechef_ai_guardrail` with tags `event`,`surface`; constructor takes the surface string). Constructor swaps `AiGatewayWorkspaceSettingsService` → Task 1's `AiGuardrailsWorkspaceSettingsService`; DROP the `AiGatewayProjectSettingsService` field and all `projectId` overloads (they move to the adapter in Task 3). Keep reading the SAME global `ApplicationProperties` keys the gateway used (`bytechef.ai.gateway.guardrails.*` — do not rename properties; note a `// property names kept for compatibility` comment).
- [ ] **Step 2:** Engine tests: update construction; workspace settings mocked via the new service; assert behavior unchanged (the moved tests are the spec). Add one new test: `resolveBlockingMode` returns `BLOCK` with no settings row.
- [ ] **Step 3:**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:test > /tmp/t2.log 2>&1; echo "exit=$?"
grep -cE "^> Task .* FAILED" /tmp/t2.log
```

(The gateway module will NOT compile at this point — that's Task 3; do not commit a broken repo: Tasks 2+3 commit TOGETHER at the end of Task 3.)

---

### Task 3: Gateway delegation adapter (contract frozen)

**Files:**
- Recreate: `automation-ai-gateway-service/.../guardrail/AiGatewayGuardrails.java` as a thin adapter (same class name, same public surface as before the move).
- Modify: `platform-ai-gateway-api/.../domain/AiGatewayWorkspaceSettings.java` — REMOVE the five guardrail fields (`redactPii`, `blockedTerms`, `moderationEnabled`, `redactSecrets`, `injectionDetectionEnabled`, `scanResponses`) from the record; update its compact-constructor/validation accordingly.
- Modify: `AiGatewayWorkspaceSettingsServiceImpl` — drop the guardrail KEY_ constants and map fields.
- Modify: gateway GraphQL schema + controller for workspace settings — guardrail fields removed (Task 8 adds the replacement API).
- Test: existing gateway guardrail/facade tests — they now construct the adapter; update ONLY construction wiring, never assertions.

**Interfaces:**
- Consumes: Task 2's `AiGuardrails`.
- Produces: `AiGatewayGuardrails` (adapter) with the exact pre-move public methods incl. `projectId` overloads and DTO methods; internally: project overlay resolved via `AiGatewayProjectSettingsService`, then delegates text work to `AiGuardrails`, unioning the project overlay's additions (the overlay can only ADD blocked terms / enable guardrails — preserve the existing union logic by moving it into the adapter).

- [ ] **Step 1:** Write the adapter: fields `AiGuardrails aiGuardrails`, `@Nullable AiGatewayProjectSettingsService`; each public method resolves the project overlay (if projectId non-null) into an "extra settings" view and calls the engine with the union. The DTO methods (`apply`, `redactResponse`) keep their existing message-walking logic but call `aiGuardrails.applyToInputs`/`scanResponseText` for the text work. Gateway does NOT consult `blockingMode` — blocks remain `AiGatewayGuardrailException` → 422 (pin with a comment).
- [ ] **Step 2:** Strip guardrail fields from `AiGatewayWorkspaceSettings` + its service impl + the gateway GraphQL type/mutation inputs + the client-side gateway settings form fields (client compile fix only here; the relocated UI is Task 9 — for THIS task just delete the moved fields from the gateway page so the client compiles).
- [ ] **Step 3: Extraction proof**

```bash
./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test > /tmp/t3.log 2>&1; echo "exit=$?"
grep -cE "^> Task .* FAILED" /tmp/t3.log
cd client && npm run check 2>&1 | tail -3
```

Expected: gateway tests pass with assertions untouched.
- [ ] **Step 4: spotlessApply, then ONE commit for Tasks 2+3**

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-guardrails server/ee/libs/automation/automation-ai/automation-ai-gateway server/ee/libs/platform/platform-ai/platform-ai-gateway client/src
git commit -m "Extract the guardrail engine into platform-ai-guardrails behind a gateway adapter"
```

---

### Task 4: `AiGuardrailsAdvisor`

**Files:**
- Create: `platform-ai-guardrails-service/.../advisor/AiGuardrailsAdvisor.java`
- Create: `platform-ai-guardrails-api/.../exception/AiGuardrailViolationException.java`
- Test: `.../advisor/AiGuardrailsAdvisorTest.java`

**Interfaces:**
- Produces:

```java
// implements org.springframework.ai.chat.client.advisor.api.CallAdvisor, StreamAdvisor
public final class AiGuardrailsAdvisor implements CallAdvisor, StreamAdvisor {
    public AiGuardrailsAdvisor(AiGuardrails aiGuardrails, @Nullable Long workspaceId, String surface) {...}
    @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; } // workspace floor runs first
}
public class AiGuardrailViolationException extends RuntimeException {
    public AiGuardrailViolationException(String category) { super("Blocked by AI guardrail: " + category); }
    public String getCategory() {...}
}
```

Behavior: before the model call, run `applyToInputs` over user+system message texts; redactions rewrite the messages in the forwarded request. A BLOCKING violation: if `resolveBlockingMode(workspaceId) == BLOCK` → throw `AiGuardrailViolationException(category)` (message = category only, NEVER content); if `REDACT_AND_CONTINUE` → mask the offending span, emit `blocking_downgraded` metric event, continue. After the call, `scanResponseText` on the completion (when `scanResponses` enabled); stream variant pipes chunks through `newStreamingResponseRedactor` (one `response_redacted` per stream at flush — engine already does this).

- [ ] **Step 1: Failing tests** — with a stubbed `AiGuardrails`:

```java
@Test void testBlockModeThrowsCategoryOnlyException() {
    // engine reports blocked term; mode BLOCK
    assertThatThrownBy(() -> advisor.adviseCall(request, chain))
        .isInstanceOf(AiGuardrailViolationException.class)
        .hasMessageNotContaining("the secret text")
        .hasMessageContaining("blocked_term");
}
@Test void testRedactAndContinueMasksAndProceeds() { /* chain invoked, forwarded text masked, downgrade metric */ }
@Test void testStreamRedactsAcrossChunkBoundary() { /* "sk-12" + "345..." chunks -> masked in output flux */ }
@Test void testAdvisorOrderIsHighestPrecedence() { assertThat(advisor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE); }
```

- [ ] **Step 2: Implement; run; pass.** Check Spring AI's advisor API names against the version in `gradle/libs.versions.toml` (RC naming — memory: use framework defaults for ordering except this deliberate HIGHEST_PRECEDENCE).
- [ ] **Step 3: spotless + commit** `"Add the AiGuardrailsAdvisor with block and redact-and-continue modes"`

---

### Task 5: CE SPI + EE workspace resolution

**Files:**
- Create (CE): `server/libs/platform/platform-ai/platform-ai-api/src/main/java/com/bytechef/platform/ai/guardrails/AiGuardrailsAdvisorProvider.java`
- Create (EE): `platform-ai-guardrails-service/.../AiGuardrailsAdvisorProviderImpl.java`
- Test (EE): `.../AiGuardrailsAdvisorProviderImplTest.java`

**Interfaces:**
- Produces (CE, no Spring AI dependency issues — `platform-ai-api` already depends on spring-ai; verify, else return `Object`-typed advisor is NOT acceptable: add the spring-ai-client-chat dep to platform-ai-api):

```java
public interface AiGuardrailsAdvisorProvider {
    /** Advisor bound to the run's workspace, or empty when guardrails are not applicable. */
    Optional<Advisor> getAdvisor(PlatformType platformType, @Nullable Long jobPrincipalId, String surface);
}
```

- EE impl: for `PlatformType.AUTOMATION` + non-null jobPrincipalId → resolve `ProjectDeployment` → `projectId` → project's `workspaceId` (find the existing service chain: `ProjectDeploymentService.getProjectDeployment(id).getProjectId()` → `WorkspaceService`/`ProjectService` — read `automation-configuration-api` for the exact accessor; the plan-limits EE spend provider does a similar resolution, copy its chain). Embedded / null / resolution failure → advisor bound to `workspaceId = null` (tenant default) — guardrails still apply, never silently skipped. Cache resolution per (platformType, jobPrincipalId) in a Caffeine cache (5 min), since this runs per model call.

- [ ] **Step 1: failing EE tests:** automation id resolves workspace; embedded → null-workspace advisor; resolution exception → null-workspace advisor (fail-open on scope, NOT on guardrails).
- [ ] **Step 2: implement, pass, spotless, commit** `"Add the AI guardrails advisor SPI with EE workspace resolution"`

---

### Task 6: Canvas AI Agent wiring

**Files:**
- Modify: `server/libs/modules/components/ai/agent/src/main/java/com/bytechef/component/ai/agent/action/AbstractAiAgentChatAction.java` (~line 238, `getChatClientRequestSpec`)
- Modify: the agent's Spring config class that constructs these actions (find where `AbstractAiAgentChatAction` subclasses get their collaborators — the `@Component("...ComponentHandler")` wiring) to inject `ObjectProvider<AiGuardrailsAdvisorProvider>`.
- Test: `.../action/AbstractAiAgentChatActionGuardrailsTest.java`

**Interfaces:**
- Consumes: Task 5's SPI. Surface string: `"ai_agent"`.

- [ ] **Step 1: failing tests** (pattern-match the existing `AbstractAiAgentChatActionResumeGateTest` fixture style):

```java
@Test void testGuardrailsAdvisorRegisteredWhenProviderPresent() { /* provider returns advisor -> request spec advisors contain it FIRST */ }
@Test void testNoProviderMeansNoAdvisor() { /* empty ObjectProvider -> spec built exactly as before */ }
@Test void testBlockingViolationFailsTheStep() {
    // advisor throws AiGuardrailViolationException -> perform() propagates -> task fails;
    // assert exception message names category only
}
```

- [ ] **Step 2: implement.** In `getChatClientRequestSpec`, before `.advisors(getAdvisors(...))`:

```java
List<Advisor> advisors = new ArrayList<>();

aiGuardrailsAdvisorProvider.ifAvailable(provider -> provider
    .getAdvisor(platformType, jobPrincipalId, "ai_agent")
    .ifPresent(advisors::add));
```

and register `advisors` ahead of the existing chain (its HIGHEST_PRECEDENCE order makes Spring AI sort it first regardless — registering it explicitly first documents intent). `platformType`/`jobPrincipalId` come from the action's execution context — locate how the action already reads its job context (the checkpoint code around line 550 reads job scope; reuse that access path).
- [ ] **Step 3: pass, spotless, commit** `"Enforce workspace AI guardrails on the canvas AI Agent"`

---

### Task 7: AI Hub wiring (all LLM turns)

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/AiHubSpringAIAgent.java` — the request-spec build (near the `.stream()` call, ~line 302) and the advisor params block (~line 200 where `ChatMemory.CONVERSATION_ID` is set).
- Modify: `AiHubConfiguration` (or the agent's constructor wiring) to inject `AiGuardrails` + `AiGuardrailsWorkspaceSettingsService`-backed advisor factory (`ObjectProvider<AiGuardrails>` — ai-hub-service may depend on platform-ai-guardrails directly, both EE).
- Test: `.../agent/AiHubSpringAIAgentGuardrailsTest.java`

**Interfaces:**
- Consumes: `AiGuardrailsAdvisor` constructed per turn: `new AiGuardrailsAdvisor(aiGuardrails, workspaceId, "ai_hub")`, `workspaceId` from the verified `WORKSPACE_ID` state key (the same accessor `buildInvocationContext` uses, ~line 324).
- Coverage rationale to encode as a comment: the advisor on the MAIN request spec covers every conversation kind (COPILOT, WORKFLOW_CHAT via its own path is exempt — it doesn't run the LLM; PERSONAL_AGENT incl. model-override clients, since overrides go through this same run method). Subagent one-shot calls are transitively covered: their input already passed the parent turn's input guardrails and their output returns into the parent turn, whose response scan runs on the final completion.

- [ ] **Step 1: failing tests:** advisor present on the request spec for a plain copilot turn AND for a personal-agent turn with an override ChatClient; BLOCK-mode violation produces a blocked-turn response whose text names the category and not the content; REDACT mode proceeds.
- [ ] **Step 2: implement, pass, spotless, commit** `"Enforce workspace AI guardrails on all AI Hub LLM turns"`

---

### Task 8: GraphQL settings API

**Files:**
- Create: `platform-ai-guardrails-graphql` module (mirror a small settings GraphQL module — copy the shape of the gateway's workspace-settings GraphQL controller): `AiGuardrailsWorkspaceSettingsGraphQlController.java` + `src/main/resources/graphql/ai-guardrails-workspace-settings.graphqls`
- Modify: `settings.gradle.kts`, server-app `build.gradle.kts` (runtime dep).
- Test: controller test pinning admin gating + null-workspace default handling.

**Interfaces:**
- Schema:

```graphql
type AiGuardrailsWorkspaceSettings {
    workspaceId: ID
    redactPii: Boolean
    redactSecrets: Boolean
    blockedTerms: String
    moderationEnabled: Boolean
    injectionDetectionEnabled: Boolean
    scanResponses: Boolean
    blockingMode: AiGuardrailsBlockingMode
}
enum AiGuardrailsBlockingMode { BLOCK, REDACT_AND_CONTINUE }
extend type Query { aiGuardrailsWorkspaceSettings(workspaceId: ID): AiGuardrailsWorkspaceSettings }
extend type Mutation { updateAiGuardrailsWorkspaceSettings(input: AiGuardrailsWorkspaceSettingsInput!): AiGuardrailsWorkspaceSettings }
input AiGuardrailsWorkspaceSettingsInput { workspaceId: ID, redactPii: Boolean, redactSecrets: Boolean, blockedTerms: String, moderationEnabled: Boolean, injectionDetectionEnabled: Boolean, scanResponses: Boolean, blockingMode: AiGuardrailsBlockingMode }
```

- Mutation `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`, query `isAuthenticated()` (the A2A GraphQL precedent).
- [ ] Steps: failing controller test (admin gate: non-admin mutation rejected; save/fetch round-trip through mocked service) → implement → pass → spotless → commit `"Expose AI guardrails workspace settings over GraphQL"`.

---

### Task 9: Client — Workspace Settings → AI → Guardrails

**Files:**
- Create: `client/src/graphql/platform/ai-guardrails/aiGuardrailsWorkspaceSettings.graphql` (query + mutation ops)
- Modify: `client/codegen.ts` (add the new .graphqls schema path)
- Create: `client/src/ee/pages/settings/automation/ai/guardrails/AiGuardrails.tsx` (+ `components/` as needed)
- Modify: the automation settings navigation (find where settings sidebar items for automation are declared — grep `Alerts` settings page registration from the workflow-alerts feature and mirror it) — new nav GROUP "AI" containing "Guardrails".
- Modify: `client/src/routes.tsx` (route for the page).
- Modify: the AI Gateway settings page — remove the relocated toggles (done server-side in Task 3; delete any leftover UI), add a link "Workspace-level guardrails moved to Settings → AI → Guardrails".
- Test: `AiGuardrails.test.tsx` — form renders all six toggles + blocked terms + mode radio from query data; save calls the mutation with changed values; interface names end `I`/`Props`; hook ordering per convention.

- [ ] Steps: ops + `npx graphql-codegen` (commit generated file separately) → page + nav + tests → `npm run check` → two commits: `"client - Add AI guardrails GraphQL operations and regenerate client"`, `"client - Add the Workspace Settings AI Guardrails page"`.

---

### Task 10: Docs, CLAUDE.md, final gates

**Files:**
- Modify: `.agents/ai-gateway-guardrails.md` → generalize into the standalone story (or add `.agents/ai-guardrails.md` and slim the gateway doc to the overlay+422 specifics; pick based on the file's current structure).
- Modify: `docs/content/docs/platform/ai-gateway.md` guardrails section — point workspace-level config at the new settings page.
- Modify: CLAUDE.md "AI Gateway content guardrails (EE)" section → rewrite for the extraction (engine location, advisor, blockingMode, surfaces, gateway adapter).
- Modify: the spec's decisions log — add the property-storage resolution from Global Constraints.

- [ ] **Step 1:** Docs edits; `cd docs && npm run types:check` → exit 0.
- [ ] **Step 2: full gates**

```bash
./gradlew spotlessApply > /tmp/g1.log 2>&1; echo "exit=$?"
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-api:check :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:check :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:check :server:ee:libs:ai:ai-hub:ai-hub-service:check :server:libs:modules:components:ai:agent:check --continue > /tmp/g2.log 2>&1; echo "exit=$?"
grep -cE "^> Task .* FAILED" /tmp/g2.log
./gradlew compileJava compileTestJava --continue > /tmp/g3.log 2>&1; echo "exit=$?"   # repo-wide compile (the Task-9 lesson from error-workflow)
grep -cE "^> Task .* FAILED" /tmp/g3.log
cd client && npm run check 2>&1 | tail -3
```

- [ ] **Step 3: commit** `"Document standalone AI guardrails and update the spec decisions log"`
