# Design: Merge platform-ai-hub + automation-ai-hub into ee/libs/ai/ai-hub

- **Date:** 2026-06-06
- **Branch:** `0_732`
- **Status:** Approved (design accepted in session)
- **Author:** Ivica Cardic (with Claude)

## 1. Problem / Motivation

AI Hub is currently split across two module trees under two different roots:

- `server/ee/libs/platform/platform-ai-hub/` → `platform-ai-hub-api`, `platform-ai-hub-service`
- `server/ee/libs/automation/automation-ai-hub/` → `automation-ai-hub-api`, `automation-ai-hub-service`,
  `automation-ai-hub-graphql`, `automation-ai-hub-rest`

This split (platform = lower-level primitives, automation = wiring + automation-specific tool
callbacks) forced cross-module bridges (`AiHubTaskArtifactRecorder` interface in platform / impl in
automation; the now-removed `AiHubAuditEmitter` followed the same shape). The sibling AI feature
`ai-copilot` already lives at `server/ee/libs/ai/ai-copilot/` as a clean 4-module unit
(`api/graphql/rest/service`, package `com.bytechef.ee.ai.copilot.*`).

**Goal:** consolidate AI Hub into the same shape — one module tree at `server/ee/libs/ai/ai-hub/`
with 4 sub-modules and a unified package `com.bytechef.ee.ai.hub.*` — dissolving the
platform/automation split.

## 2. Goals / Non-goals

**Goals**
- Single module tree `server/ee/libs/ai/ai-hub/` with 4 sub-modules mirroring `ai-copilot`.
- Unified package root `com.bytechef.ee.ai.hub.*` (from both `com.bytechef.ee.platform.aihub.*` and
  `com.bytechef.ee.automation.aihub.*`).
- Build stays green at each phase; behaviour is byte-for-byte unchanged (pure structural move).

**Non-goals**
- No behavioural changes, no API changes, no test-logic changes.
- No collapsing of the `AiHubTaskArtifactRecorder` interface+impl into one class (they may now sit in
  one module, but merging them is out of scope — YAGNI).
- No renaming of class names (only package segments change: `…platform.aihub` / `…automation.aihub` →
  `…ai.hub`; `AiHubTask` etc. keep their names).

## 3. Target structure

| New module (`server:ee:libs:ai:ai-hub:…`) | Merged from |
|---|---|
| `ai-hub-api` | `platform-ai-hub-api` + `automation-ai-hub-api` |
| `ai-hub-service` | `platform-ai-hub-service` + `automation-ai-hub-service` |
| `ai-hub-graphql` | `automation-ai-hub-graphql` |
| `ai-hub-rest` | `automation-ai-hub-rest` |

Package: everything under `com.bytechef.ee.ai.hub.*`. The two old roots have **no fully-qualified-name
collisions** after the rename (verified by scan over both api pairs and both service pairs), so the
merge is safe. Split packages exist transiently between phases but resolve when modules merge.

**Consumers** (only these reference the modules externally): `server-app`, `ai-copilot-app`, and a
test-only dependency in `mcp-tool-automation`. All get repointed to the new project paths.

## 4. Migration strategy (two phases, each build-verifiable)

### Phase A — package rename, in place
- Repo-wide string replace in `*.java`: `com.bytechef.ee.platform.aihub` → `com.bytechef.ee.ai.hub`
  and `com.bytechef.ee.automation.aihub` → `com.bytechef.ee.ai.hub` (covers `package` decls, imports,
  fully-qualified refs, and string literals such as `@EnableJdbcRepositories(basePackages=…)`).
- Same replace in the one source resource file:
  `platform-ai-hub-service/…/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  (and any other non-`build/` resource the rename scan finds).
- `git mv` each module's physical package dir `…/src/{main,test}/java/com/bytechef/ee/{platform,automation}/aihub/`
  → `…/com/bytechef/ee/ai/hub/` so directories match the new package.
- Modules stay in place; `settings.gradle.kts` untouched.
- **Verify:** `./gradlew compileJava compileTestJava` for the 6 modules + `server-app`/`ai-copilot-app`.

### Phase B — relocate + collapse to 4 modules
- `git mv` the trees under `server/ee/libs/ai/ai-hub/`:
  - `automation-ai-hub-api` → `ai-hub/ai-hub-api`; move `platform-ai-hub-api`'s sources into it.
  - `automation-ai-hub-service` → `ai-hub/ai-hub-service`; move `platform-ai-hub-service`'s sources +
    its `META-INF` resource into it.
  - `automation-ai-hub-graphql` → `ai-hub/ai-hub-graphql`; `automation-ai-hub-rest` → `ai-hub/ai-hub-rest`.
- Merge the folded modules' `build.gradle.kts` dependency lists (union; drop the now-internal
  `platform-ai-hub-*` ↔ `automation-ai-hub-*` project deps).
- Rewrite the 6 `settings.gradle.kts` includes as 4 (`server:ee:libs:ai:ai-hub:ai-hub-{api,graphql,rest,service}`).
- Repoint dependents (`server-app`, `ai-copilot-app`, `mcp-tool-automation`, internal cross-refs) to
  the 4 new project paths.
- Delete the now-empty `platform-ai-hub/` and `automation-ai-hub/` directories.
- **Verify:** `./gradlew :…:ai-hub-api:check :…:ai-hub-service:check :…:ai-hub-graphql:check
  :…:ai-hub-rest:check` plus compile of `server-app` + `ai-copilot-app`.

## 5. Risks

- **Scale:** ~320 source files repackaged, ~620 files repo-wide with imports updated. Mitigated by
  scripting the rename (`find … | xargs sed`) and using the build as the verification oracle.
- **Stale `build/` outputs:** delete `build/` dirs of the affected modules before re-verifying so old
  `.class`/resource copies don't mask issues.
- **Spring autoconfig discovery:** the merged `ai-hub-service` must carry BOTH old modules' `META-INF`
  autoconfig entries (only the platform one references a renamed class; automation's, if any, is
  carried along). Confirm the merged `AutoConfiguration.imports` lists every config class.
- **Concurrent actor:** earlier this session an external process reverted the working tree. Commit
  each phase immediately after it goes green.
