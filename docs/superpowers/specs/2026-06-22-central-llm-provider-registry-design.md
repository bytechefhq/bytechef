# Central LLM Provider Registry for Universal Actions — Design

Date: 2026-06-22
Status: Design (awaiting review)

## Problem

The universal AI components (`ai/universal/universal-text`, `ai/universal/universal-image`)
expose capability-oriented actions (Text Generation, Classify, Summarize, …, Generate Image)
that are provider-agnostic: the LLM provider is chosen at runtime via a `PROVIDER` property.
At execution time each universal action definition dispatches to the chosen provider's shared
model constant (`OpenAiChatAction.CHAT_MODEL`, `AnthropicChatAction.CHAT_MODEL`, …).

That dispatch is currently a hand-maintained `switch` plus one helper method per provider, in
**two** places:

- `AiTextActionDefinition.getChatModel()` — a 9-arm `switch` + nine `getXxxChatModel()` helpers.
- `AiImageActionDefinition.getImageModel()` — a 3-arm `switch` + three helpers.

Consequences:

- Adding a provider means editing the enum, **both** definition switches, and adding boilerplate
  helpers — three Java touch-points per provider per modality.
- The helpers are near-identical: resolve the API key, fall back to static config, return the
  model constant.
- The duplication has already produced a latent bug: `getGroqChatModel()` returns
  `PerplexityChatAction.CHAT_MODEL` instead of `GroqChatAction.CHAT_MODEL`.

## Goal

Make provider wiring central so that adding (or removing) a provider requires **zero edits** to
either universal action definition. A new provider should declare its own capabilities in its own
module; the universal layer discovers it automatically.

Non-goals: changing the user-facing action set, the `PROVIDER`/`MODEL` property UX, the primary
DB-backed token lookup, or the per-provider chat/image model implementations themselves.

## Key constraints discovered

1. **`Provider` enum lives in the base `ai:llm` module**, while each `CHAT_MODEL` / `IMAGE_MODEL`
   constant lives in a provider sub-module that *depends on* `ai:llm`. The enum therefore cannot
   statically reference the model constants (circular dependency). Wiring must live either in the
   universal layer (which already depends on every provider module) or be inverted via an SPI.

2. **Base `ai:llm` does NOT depend on `app-config`** and never imports `ApplicationProperties`.
   Any SPI defined in `ai:llm` must not reference `ApplicationProperties`, or it would drag the
   config dependency into `ai:llm` and every provider sub-module.

3. **Token resolution already has two tiers, and the primary tier is already generic:**
   - **Primary — DB `property` table via `PropertyService`**, keyed by `Provider.getKey()`
     (e.g. `"ai.provider.openAi"`), `Scope.PLATFORM`, per-environment. No per-provider code.
   - **Fallback — static `ApplicationProperties` config**, the *only* per-provider branch
     (`aiProvider.getOpenAi().getApiKey()`).

   So the per-provider `switch` for tokens exists solely for the static-config fallback.

## Design

Two independent centralizing pieces, kept separate to respect constraint (2).

### Piece 1 — `LLMModelProvider` SPI (model wiring)

New interface in base `ai:llm`:

```java
package com.bytechef.component.ai.llm;

public interface LLMModelProvider {
    Provider getProvider();

    @Nullable default ChatModel getChatModel()  { return null; } // text capability, null if none
    @Nullable default ImageModel getImageModel() { return null; } // image capability, null if none
}
```

Each provider sub-module ships exactly one `@AutoService(LLMModelProvider.class)` contributor —
matching the repo's existing `@AutoService(ComponentHandler.class)` ServiceLoader idiom. Examples:

```java
// open-ai (both modalities)
@AutoService(LLMModelProvider.class)
public class OpenAiModelProvider implements LLMModelProvider {
    public Provider getProvider()    { return Provider.OPEN_AI; }
    public ChatModel getChatModel()  { return OpenAiChatAction.CHAT_MODEL; }
    public ImageModel getImageModel(){ return OpenAiCreateImageAction.IMAGE_MODEL; }
}

// anthropic (text only)
@AutoService(LLMModelProvider.class)
public class AnthropicModelProvider implements LLMModelProvider {
    public Provider getProvider()   { return Provider.ANTHROPIC; }
    public ChatModel getChatModel() { return AnthropicChatAction.CHAT_MODEL; }
}

// stability (image only)
@AutoService(LLMModelProvider.class)
public class StabilityModelProvider implements LLMModelProvider {
    public Provider getProvider()    { return Provider.STABILITY; }
    public ImageModel getImageModel(){ return StabilityCreateImageAction.IMAGE_MODEL; }
}
```

Modality is **intrinsic**: there is no provider list anywhere. The text registry is "contributors
with a non-null `getChatModel()`"; the image registry is "contributors with a non-null
`getImageModel()`".

Contributors are added for every text provider currently in the switch
(Anthropic, Azure OpenAI, DeepSeek, Groq, Mistral, NVIDIA, OpenAI, Perplexity, Vertex Gemini) and
every image provider (Azure OpenAI, OpenAI, Stability). The aggregator router modules
(`router/open-router`, `router/nano-gpt`) are out of scope — they are not enum `Provider`s and are
not part of the universal switch today.

### Piece 2 — Registry holder (universal layer)

A small holder in the universal layer builds two maps once from
`ServiceLoader.load(LLMModelProvider.class)`:

```java
Map<Provider, ChatModel>  CHAT_MODELS;   // contributors with non-null chatModel
Map<Provider, ImageModel> IMAGE_MODELS;  // contributors with non-null imageModel
```

Placement: a shared utility reachable by both universal modules. Default plan: a class in the base
`ai:llm` module (e.g. `LLMModelRegistry`) so both `universal-text` and `universal-image` reuse one
implementation. (`ai:llm` is already a dependency of both, and `ServiceLoader.load` resolves
against the runtime classpath of the universal module that triggers it, so the provider
contributors are discovered correctly.)

`AiTextActionDefinition.getChatModel()` collapses to a map lookup; the `switch` and all nine
`getXxxChatModel()` helpers are deleted. `AiImageActionDefinition.getImageModel()` collapses the
same way. The `PropertyService` primary-token lookup and the `PROVIDER`/`MODEL` property selection
are untouched.

### Piece 3 — Generic API-key fallback accessor (config layer)

Add one method to `ApplicationProperties.Ai.Provider`:

```java
@Nullable String getProviderApiKey(String providerKey);
```

Implemented as a single `switch` over provider keys mapping to each nested config's `getApiKey()`
(e.g. `case "ai.provider.openAi" -> openAi.getApiKey()`). This mirrors the already-generic primary
table lookup — both tiers now resolve by the same `Provider.getKey()` string.

The universal layer's token resolution becomes:

```
token = primary lookup from PropertyService (unchanged)
if (token == null) token = aiProvider.getProviderApiKey(provider.getKey());
```

This replaces every `aiProvider.getOpenAi().getApiKey()` per-provider branch. Rejected
alternative: a shared `ProviderConfig { getApiKey(); }` interface implemented by every nested
config class — fully switch-free but touches every config class, too invasive for a fallback path
(YAGNI).

## Behavior change: Groq fix

The current `AiTextActionDefinition.getGroqChatModel()` returns `PerplexityChatAction.CHAT_MODEL`
— a copy-paste bug. Under the SPI, Groq's contributor returns `GroqChatAction.CHAT_MODEL` (which
already exists). This is an intentional, called-out behavior change, not a pure refactor.

## Migration order

1. Add `LLMModelProvider` SPI + `LLMModelRegistry` holder in `ai:llm`.
2. Add `@AutoService` contributors to each text provider sub-module (Groq returns its own model).
3. Add `@AutoService` contributors to each image provider sub-module.
4. Add `getProviderApiKey(String)` to `ApplicationProperties.Ai.Provider`.
5. Rewrite `AiTextActionDefinition` to use the registry + generic fallback; delete switch + helpers.
6. Rewrite `AiImageActionDefinition` likewise.
7. Confirm gradle deps: every contributing provider module is already an `implementation`
   dependency of the universal modules (verify and add any missing).

Each step compiles independently; the universal definitions are rewritten last so the registry and
contributors exist before they are consumed.

## Testing

- **Registry unit test** (`LLMModelRegistryTest`): ServiceLoader discovers all contributors; each
  `Provider` constant in the text switch maps to a non-null `ChatModel`; each image provider maps
  to a non-null `ImageModel`; Groq maps to `GroqChatAction.CHAT_MODEL` (regression guard for the
  fix).
- **Config accessor unit test**: `getProviderApiKey(key)` returns the right nested config's key for
  every provider key and `null` for an unknown key.
- **Existing snapshot tests** (`AiTextComponentHandlerTest`, `AiImageComponentHandlerTest`): remain
  green — action definitions are unchanged, only internal dispatch is rewired. Per repo conventions,
  if any definition JSON regenerates, delete stale copies under `src/test/resources/definition/` and
  `build/resources/test/definition/` before rerunning.

## Net effect

After this change, adding a provider requires: create the provider sub-module + its `@AutoService`
contributor, add the enum constant, add one `getProviderApiKey` switch arm, and add one gradle dep.
**Zero edits** to `AiTextActionDefinition` or `AiImageActionDefinition`.
