# Composer-integrated model picker for Copilot and AI Hub

**Status:** Draft
**Date:** 2026-05-26
**Author:** Ivica Cardic

## Motivation

The LLM provider/model selector currently sits in the panel **header** for both surfaces:

- Copilot panel: `client/src/shared/components/copilot/CopilotPanel.tsx:126-134` — icon-only square button next to clean-messages + close.
- AI Hub panel: `client/src/pages/automation/ai-hub/AiHubPanel.tsx:301-317` — full trigger when right panel collapsed, icon-only when expanded.

The user's mockups (provided 2026-05-26) push the picker **down into the composer footer**, next to the existing controls (apps, paperclip, @, Skill, incognito, mic, send). Three reasons this is better UX:

1. **Per-turn intent.** Picking a model is part of composing the next message, not configuring the conversation. Putting it next to the send button makes the cost/capability trade-off explicit at send time.
2. **Surface consistency.** AI Hub already has a custom composer footer with chips and resource pickers; the model picker belongs in that family of controls, not in a separate header.
3. **Header visual weight.** Both panel headers are getting busy (mode toggle, clean messages, close, picker). Moving the picker frees that line for what's actually about-this-conversation, not about-this-message.

The mockups also call for **richer model picker UX** than today: tier shortcuts (Recommended / Smartest / Fastest), per-row context-window badge + capability badges (tool calling, vision), and a hover-details side panel with speed/intelligence bars and provider/context summary. Today's picker is a flat n8n-style cascade with no metadata.

## Non-goals

- **No schema changes.** No new fields on `ai_gateway_model` or `ai_gateway_provider`. All new UX is derived from the existing `capabilities[]`, `contextWindow`, `inputCostPerMTokens`, `outputCostPerMTokens` fields the picker already queries.
- **No backend tier configuration.** "Recommended / Smartest / Fastest" are computed client-side from the derived signals (see §"Tier computation").
- **No persistence across sessions.** Selection scoping stays where it is today: per-conversation for Copilot (`useCopilotStore.selectedLlm*`), per-task for AI Hub (`useAiHubTasksStore.taskLlmSelections`). Moving the picker UI doesn't change the state contract.
- **Personal Agent edit form is out of scope.** That surface uses `ModelPicker layout="full"` inside a form; the form is the right home for it. Only Copilot panel + AI Hub panel headers change.
- **No model-route preview / cost telemetry yet.** The hover panel shows static metadata only. Live token-usage / cost-this-turn display is a separate spec.

## Current state

### Component
- `client/src/shared/components/ai/model-picker/ModelPicker.tsx` — 483-line `DropdownMenu`-based picker with provider-cascade, per-conversation/per-task LLM override. `iconOnly` + `layout="compact"|"full"` props for the two existing trigger styles.

### Mounting
- Copilot: `CopilotPanel.tsx:126-134` — header, `iconOnly` square button.
- AI Hub: `AiHubPanel.tsx:301-317` — header, conditional `iconOnly` when right panel open.
- Personal Agent form: `AiHubPersonalAgentForm.tsx` — `layout="full"`. Untouched.

### State
- Copilot: `useCopilotStore.selectedLlmProvider/Model` + `setSelectedLlm`. Resets on `generateConversationId()`.
- AI Hub: `useAiHubTasksStore.taskLlmSelections[taskId] = {provider, model}` + `setTaskLlmSelection`. Resets on task switch.
- Consumption: both inject into agent state in `CopilotRuntimeProvider.tsx:81-83` / `AiHubRuntimeProvider.tsx:1096-1105`.

### Catalog
- GraphQL: `useWorkspaceAiGatewayModelsQuery({workspaceId})` returns `{id, name, alias, providerId, capabilities[], contextWindow, enabled, inputCostPerMTokens, outputCostPerMTokens}`.
- Providers: `useWorkspaceAiGatewayProvidersQuery({workspaceId})` returns `{id, name, type, enabled, ...}`.
- `enabled: false` models are filtered out by the existing picker; same here.

### Composers
- **AI Hub** has a custom composer at `client/src/pages/automation/ai-hub/composer/AiHubChatComposer.tsx:206-391`. The footer is hand-rolled (`<footer>` with left/right sections). Adding the picker is a JSX edit.
- **Copilot** uses `@assistant-ui/react`'s `Thread` component (`CopilotPanel.tsx` line ~150+). Custom footer injection requires wrapping the composer in a `ComposerPrimitive.Root` and rendering the `ComposerPrimitive.Input` + our footer controls inside it (the library exposes the primitives separately from the `Thread` shorthand — `AiHubChatComposer` already uses this pattern, so we mirror it).

## Proposed design

### Component split

Split the existing `ModelPicker.tsx` into three files for readability and reuse:

- `ModelPickerTrigger.tsx` — the button users click. Two variants:
  - `composer` (new): icon + model alias (e.g. "Smartest" in the mockup), used in both composer footers.
  - `full` (existing): wider, with provider+model labels — used by Personal Agent form. Unchanged.
- `ModelPickerDropdown.tsx` — the dropdown panel itself. Contains the search input, the three quick-pick rows (Recommended/Smartest/Fastest), the alphabetized provider list with collapsible model groups, and the hover-details side panel.
- `ModelPickerHoverCard.tsx` — the side panel rendered when a model row is hovered. Pulls speed/intelligence/capabilities from the derivation helpers (see below).

All three live under `client/src/shared/components/ai/model-picker/`. The old monolithic `ModelPicker.tsx` becomes a thin re-export so external consumers (Personal Agent form, existing imports) keep working through the same module path.

### Derived metadata

A single pure helper module — `client/src/shared/components/ai/model-picker/derive.ts` — turns the GraphQL row into the display metadata the mockups need. **Pure and unit-tested**; the picker is the only consumer.

```typescript
type ProviderType = 'ANTHROPIC' | 'OPENAI' | 'GOOGLE' | 'QWEN' | 'MOONSHOT' | string;

interface DerivedModel {
  /** 0-5 bar rating, derived from inputCostPerMTokens. Higher cost → higher intelligence. */
  intelligence: 0 | 1 | 2 | 3 | 4 | 5;
  /** 0-5 bar rating, inverse of intelligence (cheap/light models are faster). */
  speed: 0 | 1 | 2 | 3 | 4 | 5;
  /** Parsed from capabilities[]. Looks for any of: "tool_calling", "function_calling", "tools". */
  toolCalling: boolean;
  /** Parsed from capabilities[]. Looks for any of: "vision", "image_input", "multimodal". */
  vision: boolean;
  /** Human-readable context window — "1M tokens | 750K words" form (words ≈ tokens × 0.75). */
  contextLabel: string;
  /** One-line description, hardcoded per provider+model-family. Empty string if unknown. */
  description: string;
}
```

**Cost → intelligence bucketing** (input cost per million tokens):

| Bucket | Range (USD/Mtok) | Intelligence | Speed |
| --- | --- | --- | --- |
| Light | < $1 | 1 | 5 |
| Standard | $1 - $5 | 2 | 4 |
| Capable | $5 - $15 | 3 | 3 |
| Frontier | $15 - $50 | 4 | 2 |
| Top-tier | ≥ $50 | 5 | 1 |

Models with no `inputCostPerMTokens` (e.g. self-hosted Ollama) default to `intelligence: 3, speed: 3`. Cost as a proxy for capability is intentionally imperfect; the spec calls it out so downstream contributors don't reverse-engineer it as a law.

**Description map** — a small constant table `MODEL_DESCRIPTIONS` keyed by model `name` (with provider as fallback key for unknowns). Sourced from the mockups + provider docs. Curated set:

```typescript
{
  'claude-opus-4.7':   'Most capable model for complex reasoning and agentic coding',
  'claude-sonnet-4.6': 'Best balance of capability and cost for most coding tasks',
  'claude-haiku-4.5':  'Fast and inexpensive for high-volume tasks',
  'gpt-4o':            'OpenAI flagship multimodal model',
  'gemini-3.5-flash':  'Best balance of speed, quality, and cost',
  // …
}
```

When a model isn't in the map, the dropdown row shows alias + context badge only (no description); the hover panel shows "—" for description.

### Tier computation

Three derived tiers, shown as the top-three rows in the dropdown above the alphabetized provider list:

- **Recommended** — the model with the highest `intelligence × speed` product that supports tool calling. Falls back to highest-intelligence tool-capable model. Falls back to first available model.
- **Smartest** — the model with the highest `intelligence` rating, breaking ties by largest `contextWindow`.
- **Fastest** — the model with the highest `speed` rating, breaking ties by smallest `inputCostPerMTokens`.

All three are computed from the workspace's enabled-and-loaded model list each time the dropdown opens — they're not stored. If two tiers resolve to the same model (e.g. only one model is enabled), the lower-priority slot collapses (Recommended is shown alone with a sub-label "and also smartest, fastest").

### Dropdown structure (UX-pinned)

```
┌─────────────────────────────────────────────┐
│  🔍 Search models...                        │  ← textfield, controlled, filters
├─────────────────────────────────────────────┤
│  ★ Recommended    Gemini 3.5 Flash          │  ← three tier shortcuts
│    Best balance of speed, quality, and cost │
│                                             │
│  ★ Smartest       Claude 4.7 Opus           │
│    Maximum intelligence for complex tasks   │
│                                             │
│  ★ Fastest        Gemini 3.5 Flash          │
│    Optimized for speed and low latency      │
├─────────────────────────────────────────────┤
│  ▼ Anthropic                                │  ← collapsible per provider
│      Claude 4.7 Opus   1M   ⚡ 👁           │
│      Claude 4.6 Sonnet 1M   ⚡ 👁           │
│      …                                      │
│  ▶ OpenAI                                   │  ← collapsed by default below selected provider
│  ▶ Google                                   │
│  ▶ Qwen                                     │
│  ▶ Moonshot                                 │
└─────────────────────────────────────────────┘
```

Behavior pins:

- The **provider section that owns the currently-selected model** is expanded by default; all others are collapsed.
- **Search** filters both tiers and provider sections by alias / canonical name / provider name (case-insensitive). When search matches inside a collapsed provider, that provider auto-expands.
- **Capability badges** on the right of each model row: `⚡` icon when `toolCalling`, `👁` icon when `vision`. Coloured lucide icons; tooltip on each ("Supports tool calling" / "Supports vision input"). Context-window badge ("1M" / "200K" / "32K") is rendered as a chip before the icons.
- **Keyboard nav** matches today's behaviour: arrow keys move between rows, Enter selects, Esc closes. Letter keys reach the search input (Radix's letter-jump handler is disabled inside the search field).

### Hover details panel

When the user hovers a model row for >200ms, a side panel appears anchored to the dropdown's right edge with the following content:

```
┌────────────────────────────────────────┐
│  ★ Claude 4.7 Opus                     │
│  Most capable model for complex        │
│  reasoning and agentic coding.         │
│                                        │
│  Speed         ━━─────                 │  ← 5-segment bar, filled per `speed` rating
│  Intelligence  ━━━━━━                  │
│  Provider                    Anthropic │
│  Context           1M tokens | 750K w  │
│  ─────────────                         │
│  Tool Calling                       ✓  │
│  Vision                             ✓  │
└────────────────────────────────────────┘
```

Implementation: a new `<ModelPickerHoverCard model={derived} provider={name} />` rendered absolutely-positioned to the right of the dropdown, only when `hoveredModelId !== null`. Not using shadcn `HoverCard` because we want it anchored to the dropdown (not the trigger element), and we want to keep it open while the cursor moves between rows. A simple controlled `useState` + `onMouseEnter` / `onMouseLeave` per row is enough; tested manually.

### Composer integration — AI Hub

`AiHubChatComposer.tsx:206-391` already has a footer with left/right control groups. Insert the model picker as the **leftmost control** in the left group, before the existing apps/resource picker:

```tsx
<footer>
  <left>
    <ModelPickerTrigger variant="composer" /* … */ />   // ← new
    <AiHubComposer />                                    // existing resource picker
    <PaperclipIcon />                                    // existing attach
    {/* @ button, Skill button — net new in mockup but out of scope here */}
  </left>
  <right>
    <Incognito />   // already exists in the panel but lives in the header — see open question
    <MicButton />
    <SendButton />
  </right>
</footer>
```

**The mockup also shows `@`, `Skill`, and `Incognito` controls in the composer footer.** These are out of scope for this spec — the `@` resource picker already lives in the composer via `AiHubComposer` (just visually different from the mockup's `@` button); a `Skill` quick-link doesn't exist today and would need its own design; the `Incognito` toggle lives in the header today and moving it is parallel work. Filing those as follow-ups, not in this PR.

### Composer integration — Copilot

The Copilot panel uses `@assistant-ui/react`'s `<Thread>` shorthand which renders an opinionated thread + composer combo with no footer slot. Mirror the AI Hub approach: wrap with `ComposerPrimitive.Root` + `ComposerPrimitive.Input` directly and render our footer controls below.

Concretely, refactor `CopilotPanel.tsx` lines ~150+ to:

```tsx
<ThreadPrimitive.Root>
  <ThreadPrimitive.Viewport /* messages */>...</ThreadPrimitive.Viewport>
  <ComposerPrimitive.Root>
    <ComposerPrimitive.Input />
    <footer className="composer-footer">
      <left>
        <ModelPickerTrigger variant="composer" /* … */ />
      </left>
      <right>
        <MicButton />        {/* if Copilot has mic; otherwise skip */}
        <ComposerPrimitive.Send />
      </right>
    </footer>
  </ComposerPrimitive.Root>
</ThreadPrimitive.Root>
```

The `ComposerPrimitive.Send` / `ComposerPrimitive.Cancel` primitives handle the streaming send/stop UX the `<Thread>` shorthand gives us today. Verifying via `@assistant-ui/react` docs that this primitives-based composition is supported (the library splits `Thread`, `Composer`, `Message` into discrete primitives precisely for this kind of customization). If a primitives API gap blocks this, fall back to a fully-custom Copilot composer.

### Header removal

Once composer integration is in place on both surfaces:

- **Copilot:** remove the icon-only `<ModelPicker iconOnly />` from `CopilotPanel.tsx:126-134`. The header keeps clean-messages and close only.
- **AI Hub:** remove the conditional `<ModelPicker>` block at `AiHubPanel.tsx:301-317`. The header keeps right-panel toggle and close only.

### Trigger label

The composer trigger always shows the **model alias** (e.g. "Claude 4.7 Opus", "Gemini 3.5 Flash") — never a tier name. Tier labels (Recommended / Smartest / Fastest) only appear inside the dropdown as quick-pick row labels; once a model is selected, the selection identity is the model itself.

The trigger shows a small leading icon — the **provider's icon**, sourced from `client/public/icons/<provider-name>.svg` (already used by the existing model picker's full-layout variant). When no model is selected (null state — falls back to workspace default), the trigger shows the workspace default's provider icon and model alias.

## Out of scope (deferred)

- **`@`, `Skill` toggles in the composer.** Mockup shows these; they're independent features.
- **`Incognito` toggle relocation.** Lives in the header today; moving it is a separate small change.
- **Cost-per-turn live preview.** No live $ counter for the chosen model.
- **Workspace-admin tier configuration.** Tiers stay client-derived; no admin UI.
- **`16 apps` chip from the mockup.** That's the tool-attach state surface — separate from model picking.
- **Recommended model boot-up call.** Picker doesn't pre-select Recommended for users who haven't picked yet — null selection still means "use workspace default" (server-side resolved).

## Risks and mitigations

- **Hover-details panel layout collisions** with the dropdown when the picker is near the right edge of the viewport. Mitigation: flip the panel to the left of the dropdown when there isn't 360px of right-edge space; default right-positioned otherwise. Standard floating-ui rule.
- **`@assistant-ui` primitives gap.** If `ComposerPrimitive` doesn't expose what we need for Copilot, fall back option is documented above. Low risk — AI Hub's composer already uses these primitives so we have a working reference.
- **Tier computation feels wrong for some workspaces.** Cost-as-intelligence is a proxy; an enterprise that ships their own cheap-but-capable fine-tune could see it bucketed as "Light → Fastest" when the user thinks of it as Smartest. Acceptable for v1 — the user can still pick the model directly from the provider list. A future schema-based tier field can override this if it becomes a real problem.

## Spec → plan handoff

The implementation plan (`docs/superpowers/plans/2026-05-26-composer-model-picker.md`) decomposes this into ~15 tasks:

1. `derive.ts` helper + unit tests (intelligence/speed/capabilities/context label).
2. `MODEL_DESCRIPTIONS` constant + lookup helper.
3. `ModelPickerHoverCard.tsx` component + tests.
4. `ModelPickerDropdown.tsx` rewrite (search + tiers + collapsible providers + capability badges + hover state).
5. `ModelPickerTrigger.tsx` (composer variant + full variant compat).
6. `ModelPicker.tsx` becomes a thin re-export shim.
7. Existing component tests updated for the split.
8. AI Hub composer integration — wire trigger into footer.
9. AI Hub header removal.
10. Copilot composer integration — wrap `Thread` with `ComposerPrimitive` and inject footer.
11. Copilot header removal.
12. End-to-end browser smoke (open both surfaces, pick a model, send a turn, verify it lands in agent state).
13. Storybook stories for the three components (if Storybook is set up; otherwise skip).
14. Update CLAUDE.md "Client testing" + "Composer" sections to reference the new layout.
15. PR with screenshot reproductions of the three mockups.
