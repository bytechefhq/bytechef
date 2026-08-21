# Composer default model + AI Hub visibility — design

**Date:** 2026-06-05
**Branch:** `0_732`
**Status:** Implemented (Option B)

> **Revision note.** An earlier draft of this spec proposed surface-aware defaults driven by
> `bytechef.ai.copilot.provider` / a new `bytechef.ai.hub.provider` (a shared `ChatProvider` enum).
> That was abandoned: investigation showed those `provider:` knobs are **inert at runtime** — the only
> code that read `copilot.provider`, `AiEnvironmentPostProcessor`, was never registered (no
> `spring.factories` / `EnvironmentPostProcessor.imports`), so it never ran. The runtime default chat
> model is purely the `@Primary` `ChatModel` bean (`AiModelConfiguration`): **anthropic when its
> config api-key is set, otherwise openai**. Rather than wire the knobs to runtime (Option A), we made
> the *display* mirror the real runtime selection and **removed** the knobs + the dead post-processor
> (Option B). The sections below describe the as-built Option B.

## Problem

1. **No default model shown in the composer.** `ModelPicker` shows the placeholder "Select model" even
   though the server already runs a configured default when the picker sends `null`. The user can't see
   which model will run.
2. **AI Hub visibility gated on the wrong flag.** `App.tsx` showed the AI Hub nav item when
   `ai.copilot.enabled`, not `ai.hub.enabled`.

## Goals

- Show the deployment default model in the composer trigger (e.g. "Claude Sonnet 4.6"), **display-only**
  (the picker still sends `null`; the server resolves the model). If no default is configured, keep
  "Select model".
- The displayed default must **mirror the runtime `@Primary` selection** so the label never lies.
- Gate the AI Hub UI on `bytechef.ai.hub.enabled` (already exposed via actuator `info.ai.hub.enabled`).

## Non-goals

- Changing runtime model selection / the `@Primary` wiring.
- Per-surface or per-workspace default providers (removed — `provider:` knobs are gone).
- Exposing temperature / reasoning-effort to the picker.

## Part 1 — Remove the inert `provider:` knobs

- `ApplicationProperties.java`: remove `Copilot.provider`, `Hub.provider`, and the `ChatProvider` enum;
  `Hub` and `Copilot` keep only `enabled`.
- `application-bytechef.yml`: remove `ai.copilot.provider` and `ai.hub.provider`.
- Delete the dead `AiEnvironmentPostProcessor` (`server/libs/config/ai-model-config/.../boot/`).

## Part 2 — Display-only default that mirrors `@Primary`

The default surfaced to both composers is the same the runtime uses with no override:

1. Provider = **anthropic** if `bytechef.ai.provider.anthropic.api-key` is set, else **openai** if
   `bytechef.ai.provider.openai.api-key` is set, else none. (Mirrors `AiModelConfiguration`:
   `anthropicChatModel` is `@Primary` + `@ConditionalOnProperty(...anthropic.api-key)`; `openAiChatModel`
   is the keyed fallback. Selection is config-api-key driven, **not** the AI Providers admin DB property.)
2. Model = `bytechef.ai.provider.chat.<provider>.options.model` — the same source the `@Primary` bean reads.
3. Return `{provider, model}` only if a provider resolved **and** its model is non-blank; else `null` →
   "Select model".

### Display-only semantics

| User state | Trigger shows | Sent to server |
| --- | --- | --- |
| No explicit pick, default configured | default model label | `null` (server resolves the default) |
| No explicit pick, no default | "Select model" | `null` |
| User picks a model | the picked model | that provider+model override; persisted as per-workspace last-used |
| User chooses "Use default" | default label (or "Select model") | `null` |

Reuses the existing display-only `agentDefault*` precedence pattern in `ModelPicker`.

### Backend

- DTO `AiDefaultModelDTO(String provider, String model)` (`platform-configuration-api`).
- `AiProviderFacade.getAiDefaultModel()` → `@Nullable AiDefaultModelDTO`; `AiProviderFacadeImpl`
  resolves per the rule above, reusing the private `hasConfigApiKey(Provider)`. No `environment`/`surface`
  parameter — the default is deployment-global and surface-independent.
- GraphQL (`automation-ai-gateway-graphql`, `ai-provider-catalog.graphqls`):

  ```graphql
  extend type Query { aiDefaultModel: AiDefaultModel }
  type AiDefaultModel { provider: String!  model: String! }
  ```

  Resolver on `AiProviderCatalogGraphQlController`, USER authority. `aiProviderCatalog` unchanged.

### Frontend

- `ModelPicker` gains display-only `defaultProvider`/`defaultModel` props. Trigger precedence:
  **explicit pick → agent default → configured default → "Select model"**.
- GraphQL op `aiDefaultModel` (no variables); regenerate `graphql.ts`/`graphql-types.ts`.
- The three composers (`AiHubHomePanel`, `AiHubPanel`, `CopilotPanel`) call `useAiDefaultModelQuery()`
  and pass the two props (`AiHubPanel`'s personal-agent `agentDefault*` still wins over the deployment
  default).

## Part 3 — AI Hub visibility on `ai.hub.enabled` (client-only)

`info.ai.hub.enabled` is already published by actuator (`application.yml`), but the client didn't read it.

- `useApplicationInfoStore.tsx`: add `hub: { enabled }` to the `ai` shape; parse `json.ai.hub?.enabled`.
- `App.tsx`: gate the AI Hub nav item on `edition === EE && ai.hub.enabled` (was `ai.copilot.enabled`).

## Testing

- `AiProviderFacadeDefaultModelTest`: anthropic-keyed → anthropic; only openai-keyed → openai; no key →
  null; resolved provider with blank model → null.
- `AiProviderCatalogGraphQlControllerTest`: `aiDefaultModel()` delegates to the facade.
- `ModelPicker.test.tsx`: configured default shows in the trigger; falls back to "Select model".
- `useApplicationInfoStore.test.ts`: initial `ai.hub.enabled` shape.

## Risks / notes

- The display now derives from the **same** signal as runtime (`@Primary` + config api-key), so they
  can't drift. The trade-off vs. Option A: there is no per-surface provider override — both composers and
  the runtime share one deployment default. That matched the requirement (one consistent default, less
  config) and removed the dead/misleading knobs.
- EE-only: facade + query are `@ConditionalOnEEVersion`; CE returns null like the catalog query.
