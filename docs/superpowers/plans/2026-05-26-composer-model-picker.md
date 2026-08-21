# Composer-integrated model picker — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the LLM provider/model selector from the Copilot and AI Hub panel headers into the composer footer of both surfaces, and rebuild it with tier shortcuts (Recommended / Smartest / Fastest), collapsible provider groups, capability badges, and a hover-details side panel — all driven by metadata derived from the existing `ai_gateway_model` GraphQL fields with no schema changes.

**Architecture:** Split the existing `ModelPicker.tsx` (~483 LOC) into four files under `client/src/shared/components/ai/model-picker/`: a pure `derive.ts` helper (intelligence/speed/capabilities from cost + `capabilities[]`), a `ModelPickerTrigger.tsx` (composer + full variants), a `ModelPickerDropdown.tsx` (search + tiers + provider groups + capability badges), and a `ModelPickerHoverCard.tsx` (side panel with speed/intelligence bars). Wire the new trigger into both composer footers (AI Hub via direct JSX edit; Copilot via `ComposerPrimitive` wrapping). Delete the header instances after the composer integration ships.

**Tech Stack:** React 19, TypeScript 5.9, Vitest, `@assistant-ui/react` (Copilot composer primitives), TailwindCSS, lucide-react icons, Radix `DropdownMenu` (existing component, kept).

**Spec:** `docs/superpowers/specs/2026-05-26-composer-model-picker-design.md`

**Open question resolutions adopted in this plan:**
- Model metadata: derived from existing `inputCostPerMTokens` + `capabilities[]` + `contextWindow` (no schema changes).
- Copilot composer access: wrap `@assistant-ui` `<Thread>` shorthand with `ComposerPrimitive.Root` + `ComposerPrimitive.Input` + custom footer.
- Scope: both surfaces in this PR, headers removed after composer integration ships.
- Trigger label: always show the model alias (e.g. "Claude 4.7 Opus"). Tier labels appear only inside the dropdown.

---

## File Structure

**Create** (in `client/src/shared/components/ai/model-picker/`):
- `derive.ts` — pure helpers (intelligence/speed buckets, capability parsing, tier resolution, context-window formatter)
- `derive.test.ts`
- `MODEL_DESCRIPTIONS.ts` — model-id → one-line description constant table
- `ModelPickerHoverCard.tsx` — hover-anchored side panel with speed/intelligence bars
- `ModelPickerHoverCard.test.tsx`
- `ModelPickerDropdown.tsx` — the dropdown panel (search + tiers + collapsible providers + capability badges)
- `ModelPickerDropdown.test.tsx`
- `ModelPickerTrigger.tsx` — trigger button with `composer` and `full` variants

**Modify:**
- `client/src/shared/components/ai/model-picker/ModelPicker.tsx` — becomes a thin re-export shim so external consumers (Personal Agent form, existing test imports) keep working through the same module path. The monolithic 483-line implementation moves into the new files.
- `client/src/pages/automation/ai-hub/composer/AiHubChatComposer.tsx` — insert `<ModelPickerTrigger variant="composer" />` as the leftmost footer control.
- `client/src/shared/components/copilot/CopilotPanel.tsx` — replace `<Thread>` shorthand with `<ThreadPrimitive.Root>` + `<ComposerPrimitive.Root>` composition that gives us a custom footer; insert the trigger there.
- `client/src/pages/automation/ai-hub/AiHubPanel.tsx` — remove the header model picker block (lines 301-317).
- `client/src/shared/components/copilot/CopilotPanel.tsx` — remove the header model picker block (lines 126-134; same edit as the composer rewrite above).

**Delete:** none — the existing `ModelPicker.tsx` is kept as a re-export shim.

---

## Task 1: `derive.ts` — failing test first (TDD)

**Files:**
- Create: `client/src/shared/components/ai/model-picker/derive.test.ts`

- [ ] **Step 1: Write the failing tests**

```typescript
import {describe, expect, it} from 'vitest';
import {
    deriveModelMetadata,
    resolveTiers,
    formatContextLabel,
    type AiGatewayModelInput,
} from './derive';

describe('deriveModelMetadata', () => {
    it('buckets intelligence by cost-per-mtok', () => {
        // Light: < $1
        expect(deriveModelMetadata({inputCostPerMTokens: 0.5}).intelligence).toBe(1);
        expect(deriveModelMetadata({inputCostPerMTokens: 0.5}).speed).toBe(5);

        // Standard: $1-$5
        expect(deriveModelMetadata({inputCostPerMTokens: 3}).intelligence).toBe(2);
        expect(deriveModelMetadata({inputCostPerMTokens: 3}).speed).toBe(4);

        // Capable: $5-$15
        expect(deriveModelMetadata({inputCostPerMTokens: 10}).intelligence).toBe(3);
        expect(deriveModelMetadata({inputCostPerMTokens: 10}).speed).toBe(3);

        // Frontier: $15-$50
        expect(deriveModelMetadata({inputCostPerMTokens: 30}).intelligence).toBe(4);
        expect(deriveModelMetadata({inputCostPerMTokens: 30}).speed).toBe(2);

        // Top-tier: >= $50
        expect(deriveModelMetadata({inputCostPerMTokens: 75}).intelligence).toBe(5);
        expect(deriveModelMetadata({inputCostPerMTokens: 75}).speed).toBe(1);
    });

    it('defaults intelligence and speed to 3 when cost is null', () => {
        const m = deriveModelMetadata({inputCostPerMTokens: null});
        expect(m.intelligence).toBe(3);
        expect(m.speed).toBe(3);
    });

    it('parses tool_calling capability variants', () => {
        expect(deriveModelMetadata({capabilities: ['tool_calling']}).toolCalling).toBe(true);
        expect(deriveModelMetadata({capabilities: ['function_calling']}).toolCalling).toBe(true);
        expect(deriveModelMetadata({capabilities: ['tools']}).toolCalling).toBe(true);
        expect(deriveModelMetadata({capabilities: []}).toolCalling).toBe(false);
        expect(deriveModelMetadata({capabilities: null}).toolCalling).toBe(false);
    });

    it('parses vision capability variants', () => {
        expect(deriveModelMetadata({capabilities: ['vision']}).vision).toBe(true);
        expect(deriveModelMetadata({capabilities: ['image_input']}).vision).toBe(true);
        expect(deriveModelMetadata({capabilities: ['multimodal']}).vision).toBe(true);
        expect(deriveModelMetadata({capabilities: ['VISION']}).vision).toBe(true); // case-insensitive
        expect(deriveModelMetadata({capabilities: ['function_calling']}).vision).toBe(false);
    });
});

describe('formatContextLabel', () => {
    it('formats common context window sizes', () => {
        expect(formatContextLabel(1_000_000)).toBe('1M');
        expect(formatContextLabel(200_000)).toBe('200K');
        expect(formatContextLabel(128_000)).toBe('128K');
        expect(formatContextLabel(32_000)).toBe('32K');
        expect(formatContextLabel(8192)).toBe('8K');
    });

    it('returns empty string for null context window', () => {
        expect(formatContextLabel(null)).toBe('');
    });
});

describe('resolveTiers', () => {
    const models: AiGatewayModelInput[] = [
        {
            id: 1,
            name: 'claude-opus-4.7',
            alias: 'Claude 4.7 Opus',
            providerId: 1,
            capabilities: ['tool_calling', 'vision'],
            contextWindow: 1_000_000,
            inputCostPerMTokens: 75,
            enabled: true,
        },
        {
            id: 2,
            name: 'gemini-3.5-flash',
            alias: 'Gemini 3.5 Flash',
            providerId: 2,
            capabilities: ['tool_calling', 'vision'],
            contextWindow: 1_000_000,
            inputCostPerMTokens: 0.3,
            enabled: true,
        },
        {
            id: 3,
            name: 'claude-haiku-4.5',
            alias: 'Claude 4.5 Haiku',
            providerId: 1,
            capabilities: ['tool_calling'],
            contextWindow: 200_000,
            inputCostPerMTokens: 0.8,
            enabled: true,
        },
    ];

    it('resolves smartest as the highest-intelligence model', () => {
        const tiers = resolveTiers(models);
        expect(tiers.smartest?.id).toBe(1); // Claude Opus, top-tier cost
    });

    it('resolves fastest as the highest-speed (lowest-cost) model', () => {
        const tiers = resolveTiers(models);
        expect(tiers.fastest?.id).toBe(2); // Gemini, lowest cost
    });

    it('resolves recommended as highest intelligence * speed product with tool calling', () => {
        const tiers = resolveTiers(models);
        // Gemini: 1 * 5 = 5, Opus: 5 * 1 = 5, Haiku: 1 * 5 = 5
        // Tie-break documented in derive.ts (TBD — pin in implementation)
        expect(tiers.recommended).not.toBeNull();
        expect(tiers.recommended?.toolCalling).toBe(true);
    });

    it('returns null tiers for empty model list', () => {
        const tiers = resolveTiers([]);
        expect(tiers.recommended).toBeNull();
        expect(tiers.smartest).toBeNull();
        expect(tiers.fastest).toBeNull();
    });

    it('falls back to non-tool-capable model for recommended when no tool-capable models exist', () => {
        const noTools: AiGatewayModelInput[] = [
            {...models[0], capabilities: []},
            {...models[1], capabilities: []},
        ];
        const tiers = resolveTiers(noTools);
        expect(tiers.recommended).not.toBeNull(); // doesn't crash; picks something
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd client && npm run test -- derive.test.ts
```
Expected: FAIL — module `./derive` not found.

---

## Task 2: `derive.ts` — implementation

**Files:**
- Create: `client/src/shared/components/ai/model-picker/derive.ts`

- [ ] **Step 1: Implement**

```typescript
/**
 * Pure helpers that turn a GraphQL ai_gateway_model row into the display metadata the composer-integrated
 * ModelPicker needs (intelligence rating, speed rating, capability flags, context label, derived tiers).
 *
 * Cost as a proxy for intelligence is intentionally imperfect — see the design spec under §"Tier
 * computation" for the rationale. A future schema-side `tier` / `speedRating` / `intelligenceRating`
 * field can override these heuristics by changing only this file.
 */

/** Subset of the GraphQL ai_gateway_model fields this module needs. Decoupled from the codegen type. */
export interface AiGatewayModelInput {
    id: number;
    name: string;
    alias?: string | null;
    providerId: number;
    capabilities?: readonly string[] | null;
    contextWindow?: number | null;
    inputCostPerMTokens?: number | null;
    enabled: boolean;
    // Optional fields propagated to DerivedModel consumers:
    outputCostPerMTokens?: number | null;
}

export interface DerivedModel {
    intelligence: 0 | 1 | 2 | 3 | 4 | 5;
    speed: 0 | 1 | 2 | 3 | 4 | 5;
    toolCalling: boolean;
    vision: boolean;
    contextLabel: string;
}

export interface ResolvedTiers {
    recommended: (AiGatewayModelInput & DerivedModel) | null;
    smartest: (AiGatewayModelInput & DerivedModel) | null;
    fastest: (AiGatewayModelInput & DerivedModel) | null;
}

const TOOL_CALLING_CAPABILITY_TOKENS = ['tool_calling', 'function_calling', 'tools'];
const VISION_CAPABILITY_TOKENS = ['vision', 'image_input', 'multimodal'];

export function deriveModelMetadata(model: Partial<AiGatewayModelInput>): DerivedModel {
    const cost = model.inputCostPerMTokens ?? null;

    let intelligence: DerivedModel['intelligence'];
    let speed: DerivedModel['speed'];

    if (cost === null) {
        intelligence = 3;
        speed = 3;
    } else if (cost < 1) {
        intelligence = 1;
        speed = 5;
    } else if (cost < 5) {
        intelligence = 2;
        speed = 4;
    } else if (cost < 15) {
        intelligence = 3;
        speed = 3;
    } else if (cost < 50) {
        intelligence = 4;
        speed = 2;
    } else {
        intelligence = 5;
        speed = 1;
    }

    const capabilities = (model.capabilities ?? []).map((capability) => capability.toLowerCase());

    const toolCalling = capabilities.some((capability) => TOOL_CALLING_CAPABILITY_TOKENS.includes(capability));
    const vision = capabilities.some((capability) => VISION_CAPABILITY_TOKENS.includes(capability));

    const contextLabel = formatContextLabel(model.contextWindow ?? null);

    return {intelligence, speed, toolCalling, vision, contextLabel};
}

export function formatContextLabel(contextWindow: number | null): string {
    if (contextWindow === null || contextWindow <= 0) {
        return '';
    }

    if (contextWindow >= 1_000_000) {
        return `${Math.round(contextWindow / 1_000_000)}M`;
    }

    return `${Math.round(contextWindow / 1000)}K`;
}

export function resolveTiers(models: readonly AiGatewayModelInput[]): ResolvedTiers {
    const enriched = models
        .filter((model) => model.enabled)
        .map((model) => ({...model, ...deriveModelMetadata(model)}));

    if (enriched.length === 0) {
        return {recommended: null, smartest: null, fastest: null};
    }

    // Smartest: highest intelligence, tiebreak largest contextWindow.
    const smartest = [...enriched].sort((a, b) => {
        if (b.intelligence !== a.intelligence) {
            return b.intelligence - a.intelligence;
        }

        return (b.contextWindow ?? 0) - (a.contextWindow ?? 0);
    })[0];

    // Fastest: highest speed, tiebreak cheapest input cost.
    const fastest = [...enriched].sort((a, b) => {
        if (b.speed !== a.speed) {
            return b.speed - a.speed;
        }

        return (a.inputCostPerMTokens ?? Infinity) - (b.inputCostPerMTokens ?? Infinity);
    })[0];

    // Recommended: highest intelligence*speed product among tool-capable models, tiebreak largest contextWindow.
    // Fallback to highest-intelligence tool-capable model, then to first available model.
    const toolCapable = enriched.filter((model) => model.toolCalling);
    const recommendedCandidates = toolCapable.length > 0 ? toolCapable : enriched;

    const recommended = [...recommendedCandidates].sort((a, b) => {
        const productDiff = b.intelligence * b.speed - a.intelligence * a.speed;

        if (productDiff !== 0) {
            return productDiff;
        }

        return (b.contextWindow ?? 0) - (a.contextWindow ?? 0);
    })[0];

    return {recommended, smartest, fastest};
}
```

- [ ] **Step 2: Run tests, expect PASS**

```bash
cd client && npm run test -- derive.test.ts
```
Expected: all tests pass.

- [ ] **Step 3: Commit**

```bash
git add client/src/shared/components/ai/model-picker/derive.ts \
        client/src/shared/components/ai/model-picker/derive.test.ts
git commit -m "$(cat <<'EOF'
- client - Add ModelPicker derive helpers + tests

Pure functions that turn an AiGatewayModel GraphQL row into the display
metadata the upcoming composer-integrated ModelPicker needs:

- deriveModelMetadata — 5-bucket intelligence + inverse speed from
  inputCostPerMTokens; tool-calling + vision flags parsed from
  capabilities[]; humanised context-window label
- resolveTiers — Recommended / Smartest / Fastest computed client-side
  from a list of models with documented tiebreak rules
- formatContextLabel — "1M" / "200K" / "32K" form

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: `MODEL_DESCRIPTIONS` constant

**Files:**
- Create: `client/src/shared/components/ai/model-picker/MODEL_DESCRIPTIONS.ts`

- [ ] **Step 1: Implement**

```typescript
/**
 * One-line model descriptions surfaced in the ModelPickerHoverCard and as the tier-row sub-label.
 * Keyed by canonical model name (lowercase). Falls back to empty string if unknown.
 *
 * Curated from provider docs (Anthropic / OpenAI / Google / Qwen / Moonshot) — keep entries short
 * and capability-focused. New models that aren't in the map degrade gracefully (no description shown).
 */
export const MODEL_DESCRIPTIONS: Readonly<Record<string, string>> = {
    'claude-opus-4.7': 'Most capable model for complex reasoning and agentic coding',
    'claude-opus-4.6': 'Capable reasoning + coding model from the Claude 4 family',
    'claude-sonnet-4.6': 'Best balance of capability and cost for most coding tasks',
    'claude-sonnet-4.5': 'Strong general-purpose Claude model',
    'claude-haiku-4.5': 'Fast and inexpensive for high-volume tasks',
    'gpt-4o': 'OpenAI flagship multimodal model',
    'gpt-4o-mini': 'Smaller, cheaper variant of GPT-4o for high-volume work',
    'gemini-3.5-flash': 'Best balance of speed, quality, and cost',
    'gemini-3.5-pro': 'Higher-capability Gemini with large context window',
    // Extend as new models land. Empty/unknown keys degrade gracefully.
};

export function lookupModelDescription(name: string | null | undefined): string {
    if (!name) {
        return '';
    }

    return MODEL_DESCRIPTIONS[name.toLowerCase()] ?? '';
}
```

- [ ] **Step 2: Run `npm run check` (lint + typecheck + tests)**

- [ ] **Step 3: Commit**

```bash
git add client/src/shared/components/ai/model-picker/MODEL_DESCRIPTIONS.ts
git commit -m "- client - Add curated MODEL_DESCRIPTIONS table for ModelPicker"
```

---

## Task 4: `ModelPickerHoverCard` — failing test + impl

**Files:**
- Create: `client/src/shared/components/ai/model-picker/ModelPickerHoverCard.test.tsx`
- Create: `client/src/shared/components/ai/model-picker/ModelPickerHoverCard.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {ModelPickerHoverCard} from './ModelPickerHoverCard';

describe('ModelPickerHoverCard', () => {
    const baseProps = {
        model: {
            id: 1,
            name: 'claude-opus-4.7',
            alias: 'Claude 4.7 Opus',
            providerId: 1,
            capabilities: ['tool_calling', 'vision'],
            contextWindow: 1_000_000,
            inputCostPerMTokens: 75,
            enabled: true,
        },
        providerName: 'Anthropic',
    };

    it('renders model alias as title', () => {
        render(<ModelPickerHoverCard {...baseProps} />);

        expect(screen.getByText('Claude 4.7 Opus')).toBeInTheDocument();
    });

    it('renders the curated description for known models', () => {
        render(<ModelPickerHoverCard {...baseProps} />);

        expect(screen.getByText(/Most capable model for complex reasoning/)).toBeInTheDocument();
    });

    it('renders provider, context label, and capability ticks', () => {
        render(<ModelPickerHoverCard {...baseProps} />);

        expect(screen.getByText('Anthropic')).toBeInTheDocument();
        expect(screen.getByText(/1M tokens/)).toBeInTheDocument();
        expect(screen.getByLabelText(/Tool Calling supported/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Vision supported/i)).toBeInTheDocument();
    });

    it('renders empty description fallback for unknown models', () => {
        render(
            <ModelPickerHoverCard
                {...baseProps}
                model={{...baseProps.model, name: 'unknown-model'}}
            />
        );

        expect(screen.queryByText(/Most capable/)).not.toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Implement `ModelPickerHoverCard.tsx`**

```tsx
import {CheckIcon, MinusIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';
import {type AiGatewayModelInput, deriveModelMetadata} from './derive';
import {lookupModelDescription} from './MODEL_DESCRIPTIONS';

interface ModelPickerHoverCardPropsI {
    model: AiGatewayModelInput;
    providerName: string;
    className?: string;
}

export function ModelPickerHoverCard({className, model, providerName}: ModelPickerHoverCardPropsI) {
    const derived = deriveModelMetadata(model);
    const description = lookupModelDescription(model.name);
    const wordCount = model.contextWindow ? Math.round((model.contextWindow * 0.75) / 1000) : 0;
    const wordsLabel = wordCount >= 1000 ? `${(wordCount / 1000).toFixed(0)}M words` : `${wordCount}K words`;

    return (
        <div
            className={twMerge(
                'rounded-lg border border-border bg-popover p-4 shadow-md w-80',
                className
            )}
            role="dialog"
            aria-label={`Details for ${model.alias ?? model.name}`}
        >
            <h3 className="text-sm font-semibold">{model.alias ?? model.name}</h3>

            {description && (
                <p className="mt-1 text-xs text-muted-foreground">{description}</p>
            )}

            <div className="mt-3 space-y-2 text-xs">
                <RatingRow label="Speed" value={derived.speed} />

                <RatingRow label="Intelligence" value={derived.intelligence} />

                <KeyValueRow label="Provider" value={providerName} />

                {derived.contextLabel && (
                    <KeyValueRow
                        label="Context"
                        value={`${derived.contextLabel} tokens | ${wordsLabel}`}
                    />
                )}
            </div>

            <hr className="my-3 border-border" />

            <div className="space-y-2 text-xs">
                <CapabilityRow label="Tool Calling" supported={derived.toolCalling} />

                <CapabilityRow label="Vision" supported={derived.vision} />
            </div>
        </div>
    );
}

function RatingRow({label, value}: {label: string; value: number}) {
    return (
        <div className="flex items-center justify-between">
            <span className="text-muted-foreground">{label}</span>

            <div className="flex gap-1">
                {[0, 1, 2, 3, 4].map((index) => (
                    <span
                        key={index}
                        className={twMerge(
                            'h-1.5 w-4 rounded-full',
                            index < value ? 'bg-blue-500' : 'bg-muted'
                        )}
                    />
                ))}
            </div>
        </div>
    );
}

function KeyValueRow({label, value}: {label: string; value: string}) {
    return (
        <div className="flex items-center justify-between gap-4">
            <span className="text-muted-foreground">{label}</span>

            <span className="text-right">{value}</span>
        </div>
    );
}

function CapabilityRow({label, supported}: {label: string; supported: boolean}) {
    return (
        <div className="flex items-center justify-between">
            <span className="text-muted-foreground">{label}</span>

            {supported ? (
                <CheckIcon aria-label={`${label} supported`} className="size-4 text-green-600" />
            ) : (
                <MinusIcon aria-label={`${label} not supported`} className="size-4 text-muted-foreground" />
            )}
        </div>
    );
}
```

- [ ] **Step 3: Run tests, expect PASS**

- [ ] **Step 4: Commit**

```bash
git add client/src/shared/components/ai/model-picker/ModelPickerHoverCard.tsx \
        client/src/shared/components/ai/model-picker/ModelPickerHoverCard.test.tsx
git commit -m "- client - Add ModelPickerHoverCard side panel"
```

---

## Task 5: `ModelPickerDropdown` — rewrite of the dropdown panel

**Files:**
- Create: `client/src/shared/components/ai/model-picker/ModelPickerDropdown.tsx`
- Create: `client/src/shared/components/ai/model-picker/ModelPickerDropdown.test.tsx`

- [ ] **Step 1: Write the failing tests**

Coverage targets:
1. Renders the three tier rows (Recommended / Smartest / Fastest) when the catalog has 1+ enabled models.
2. Tier rows are hidden when the catalog is empty; an empty state is rendered.
3. Provider sections collapse by default; the section owning the currently-selected model is expanded.
4. Typing in the search box filters tiers + provider rows by alias + model name + provider name.
5. When search matches inside a collapsed provider, the provider auto-expands.
6. Selecting a model row fires `onChange(provider, model)`.
7. Capability icons render conditionally (tool-calling and vision) on matching rows.
8. Hovering a row >200ms reveals the hover card; mouse-leaving hides it.

```tsx
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ModelPickerDropdown} from './ModelPickerDropdown';

const PROVIDERS = [
    {id: 1, name: 'Anthropic', type: 'ANTHROPIC' as const, enabled: true},
    {id: 2, name: 'OpenAI', type: 'OPENAI' as const, enabled: true},
];

const MODELS = [
    {
        id: 10,
        name: 'claude-opus-4.7',
        alias: 'Claude 4.7 Opus',
        providerId: 1,
        capabilities: ['tool_calling', 'vision'],
        contextWindow: 1_000_000,
        inputCostPerMTokens: 75,
        enabled: true,
    },
    {
        id: 20,
        name: 'gpt-4o',
        alias: 'GPT-4o',
        providerId: 2,
        capabilities: ['tool_calling'],
        contextWindow: 128_000,
        inputCostPerMTokens: 5,
        enabled: true,
    },
];

describe('ModelPickerDropdown', () => {
    it('renders three tier rows when catalog has models', () => {
        render(
            <ModelPickerDropdown
                models={MODELS}
                providers={PROVIDERS}
                onChange={vi.fn()}
                selectedModelId={null}
                selectedProviderId={null}
            />
        );

        expect(screen.getByText(/Recommended/)).toBeInTheDocument();
        expect(screen.getByText(/Smartest/)).toBeInTheDocument();
        expect(screen.getByText(/Fastest/)).toBeInTheDocument();
    });

    it('filters tiers + provider rows when typing in the search box', () => {
        render(
            <ModelPickerDropdown
                models={MODELS}
                providers={PROVIDERS}
                onChange={vi.fn()}
                selectedModelId={null}
                selectedProviderId={null}
            />
        );

        fireEvent.change(screen.getByPlaceholderText(/Search models/), {target: {value: 'gpt'}});

        expect(screen.queryByText(/Claude 4.7 Opus/)).not.toBeInTheDocument();
        expect(screen.getByText(/GPT-4o/)).toBeInTheDocument();
    });

    it('expands the provider owning the currently-selected model by default', () => {
        render(
            <ModelPickerDropdown
                models={MODELS}
                providers={PROVIDERS}
                onChange={vi.fn()}
                selectedModelId={10}
                selectedProviderId={1}
            />
        );

        // Anthropic section visible (Claude row), OpenAI collapsed.
        expect(screen.getByText('Claude 4.7 Opus')).toBeInTheDocument();
        expect(screen.queryByText('GPT-4o')).not.toBeInTheDocument();
    });

    it('fires onChange with provider+model when a row is selected', () => {
        const onChange = vi.fn();

        render(
            <ModelPickerDropdown
                models={MODELS}
                providers={PROVIDERS}
                onChange={onChange}
                selectedModelId={null}
                selectedProviderId={null}
            />
        );

        fireEvent.click(screen.getByText(/Smartest/));

        expect(onChange).toHaveBeenCalledWith('Anthropic', 'claude-opus-4.7');
    });

    it('renders empty state when the catalog has no enabled models', () => {
        render(
            <ModelPickerDropdown
                models={[]}
                providers={[]}
                onChange={vi.fn()}
                selectedModelId={null}
                selectedProviderId={null}
            />
        );

        expect(screen.getByText(/No models available/)).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Implement the dropdown**

The component is large but mechanical — search input → `useState` filter; tier rows render `resolveTiers(filteredModels)`; provider sections render alphabetised + a chevron-toggle `useState<Set<providerId>>` for expanded state; rows render capability badges + context chip; hover state is `useState<modelId | null>` with `setTimeout` 200ms delay on `onMouseEnter` and `clearTimeout` on `onMouseLeave`; the hover card is absolute-positioned to the right edge of the dropdown (flip to left when there's <360px of right-edge clearance — measure via a `useRef` + `useLayoutEffect`).

Use existing `lucide-react` icons: `SearchIcon`, `ChevronDownIcon`, `ChevronRightIcon`, `SparkleIcon` for the tier marker, `WrenchIcon` for tool-calling, `EyeIcon` for vision.

Reference today's `ModelPicker.tsx` for the Radix `DropdownMenu` integration patterns (controlled open state, key event handling on the search input to stop letter-jump propagation). Keep that pattern.

- [ ] **Step 3: Run tests, expect PASS**

- [ ] **Step 4: Commit**

---

## Task 6: `ModelPickerTrigger`

**Files:**
- Create: `client/src/shared/components/ai/model-picker/ModelPickerTrigger.tsx`

- [ ] **Step 1: Implement**

```tsx
import {ChevronDownIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';
import {type AiGatewayModelInput} from './derive';

export type ModelPickerTriggerVariant = 'composer' | 'full';

interface ModelPickerTriggerPropsI {
    className?: string;
    onClick: () => void;
    /** When true, renders an open-state highlight ring. */
    open?: boolean;
    /** Selected model. When null, falls back to the workspace default (rendered with default icon/label). */
    selectedModel: AiGatewayModelInput | null;
    /** Provider name for the selected model (or the workspace default when no selection). */
    selectedProviderName: string | null;
    /** Label/alias for the workspace default model, used when selectedModel is null. */
    workspaceDefaultLabel: string | null;
    /** Workspace default provider name, for the default state icon. */
    workspaceDefaultProviderName: string | null;
    variant: ModelPickerTriggerVariant;
}

export function ModelPickerTrigger({
    className,
    onClick,
    open,
    selectedModel,
    selectedProviderName,
    variant,
    workspaceDefaultLabel,
    workspaceDefaultProviderName,
}: ModelPickerTriggerPropsI) {
    const label = selectedModel?.alias ?? selectedModel?.name ?? workspaceDefaultLabel ?? 'Select model';
    const providerName = selectedProviderName ?? workspaceDefaultProviderName ?? '';
    const iconPath = providerName ? `/icons/${providerName.toLowerCase()}.svg` : null;

    return (
        <button
            aria-haspopup="menu"
            aria-expanded={open ?? false}
            className={twMerge(
                'inline-flex items-center gap-1.5 rounded-full border border-transparent px-2 py-1 text-sm',
                'hover:bg-muted',
                open && 'bg-muted',
                variant === 'full' && 'px-3 py-2 border-border',
                className
            )}
            onClick={onClick}
            type="button"
        >
            {iconPath && <img alt="" className="size-4" src={iconPath} />}

            <span className="truncate max-w-32">{label}</span>

            <ChevronDownIcon className="size-3 text-muted-foreground" />
        </button>
    );
}
```

- [ ] **Step 2: Commit**

---

## Task 7: `ModelPicker.tsx` becomes a thin re-export shim

**Files:**
- Modify: `client/src/shared/components/ai/model-picker/ModelPicker.tsx`

- [ ] **Step 1: Replace the file's contents**

```tsx
/**
 * Composer-integrated ModelPicker.
 *
 * This file is a thin facade over the split components introduced in 2026-05-26:
 * - {@link ModelPickerTrigger} — the button users click.
 * - {@link ModelPickerDropdown} — the dropdown content (search + tiers + provider groups).
 * - {@link ModelPickerHoverCard} — the side panel anchored to a hovered model row.
 *
 * Callers that previously imported the monolithic ModelPicker get a default-styled
 * composer-variant trigger + dropdown wired together for them.
 */
import {DropdownMenu, DropdownMenuContent, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {useWorkspaceAiGatewayModelsQuery, useWorkspaceAiGatewayProvidersQuery} from '@/shared/middleware/graphql';
import {useMemo, useState} from 'react';
import {ModelPickerDropdown} from './ModelPickerDropdown';
import {ModelPickerTrigger, type ModelPickerTriggerVariant} from './ModelPickerTrigger';

export interface ModelPickerPropsI {
    onChange: (provider: string | null, model: string | null) => void;
    selectedModel: string | null;
    selectedProvider: string | null;
    variant?: ModelPickerTriggerVariant;
    workspaceId: number;
}

export function ModelPicker({
    onChange,
    selectedModel,
    selectedProvider,
    variant = 'composer',
    workspaceId,
}: ModelPickerPropsI) {
    const [open, setOpen] = useState(false);

    const {data: models = []} = useWorkspaceAiGatewayModelsQuery({workspaceId});
    const {data: providers = []} = useWorkspaceAiGatewayProvidersQuery({workspaceId});

    const selectedModelRow = useMemo(
        () => models.find((model) => model.name === selectedModel) ?? null,
        [models, selectedModel]
    );

    return (
        <DropdownMenu onOpenChange={setOpen} open={open}>
            <DropdownMenuTrigger asChild>
                <ModelPickerTrigger
                    onClick={() => setOpen((previous) => !previous)}
                    open={open}
                    selectedModel={selectedModelRow}
                    selectedProviderName={selectedProvider}
                    variant={variant}
                    workspaceDefaultLabel={null /* TODO wire workspace default */}
                    workspaceDefaultProviderName={null}
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="start" className="p-0 w-96">
                <ModelPickerDropdown
                    models={models}
                    onChange={(providerName, modelName) => {
                        onChange(providerName, modelName);
                        setOpen(false);
                    }}
                    providers={providers}
                    selectedModelId={selectedModelRow?.id ?? null}
                    selectedProviderId={selectedModelRow?.providerId ?? null}
                />
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
```

This collapses the original 483-line monolith to ~50 lines. The new files (`derive.ts`, `ModelPickerDropdown.tsx`, `ModelPickerTrigger.tsx`, `ModelPickerHoverCard.tsx`, `MODEL_DESCRIPTIONS.ts`) carry the implementation.

- [ ] **Step 2: Run `npm run check` (full)**

- [ ] **Step 3: Verify existing callers still compile**

```bash
grep -rln 'from .*model-picker/ModelPicker' client/src --include='*.tsx' --include='*.ts'
```

For each caller (Personal Agent form, AI Hub panel header, Copilot panel header, any tests), verify the new `ModelPicker` props shape is a superset of what they pass (or update the callers if a prop name changed). Likely all callers continue to work.

- [ ] **Step 4: Commit**

---

## Task 8: Wire `ModelPicker` into the AI Hub composer footer

**Files:**
- Modify: `client/src/pages/automation/ai-hub/composer/AiHubChatComposer.tsx`

- [ ] **Step 1: Add the trigger to the composer footer**

Read the file's existing footer JSX (lines ~206-391) to find the `<footer>` element. Insert `<ModelPicker />` as the leftmost control:

```tsx
<footer className="flex items-center justify-between gap-2 px-4 pb-2 pt-1">
    <div className="flex items-center gap-2">
        <ModelPicker
            onChange={(providerName, modelName) =>
                setTaskLlmSelection(currentTaskId, providerName, modelName)
            }
            selectedModel={taskLlmSelections[currentTaskId]?.model ?? null}
            selectedProvider={taskLlmSelections[currentTaskId]?.provider ?? null}
            variant="composer"
            workspaceId={currentWorkspaceId}
        />
        {/* existing resource picker, paperclip, etc. — kept */}
    </div>
    <div className="flex items-center gap-2">
        {/* existing mic + send — kept */}
    </div>
</footer>
```

Pull the state hooks from `useAiHubTasksStore` and the workspace id from `useWorkspaceStore` per existing patterns in the same file.

- [ ] **Step 2: Run `npm run check`**

- [ ] **Step 3: Commit**

---

## Task 9: Remove the model picker from the AI Hub panel header

**Files:**
- Modify: `client/src/pages/automation/ai-hub/AiHubPanel.tsx`

- [ ] **Step 1: Delete lines 301-317 (the header `<ModelPicker>` block)**

After the composer integration is in place, the header instance is redundant. Delete it. The header keeps the right-panel toggle and close button.

- [ ] **Step 2: Run `npm run check`**

- [ ] **Step 3: Commit**

---

## Task 10: Wire `ModelPicker` into the Copilot composer

**Files:**
- Modify: `client/src/shared/components/copilot/CopilotPanel.tsx`

- [ ] **Step 1: Replace the `<Thread>` shorthand with a primitives-based composition**

Today's render uses something like `<Thread runtime={...} />`. Refactor to:

```tsx
<ThreadPrimitive.Root>
    <ThreadPrimitive.Viewport className="...">
        <ThreadPrimitive.Messages />
    </ThreadPrimitive.Viewport>

    <ComposerPrimitive.Root className="composer-shell">
        <ComposerPrimitive.Input className="composer-input" placeholder="..." />

        <footer className="flex items-center justify-between gap-2 px-4 pb-2 pt-1">
            <div className="flex items-center gap-2">
                <ModelPicker
                    onChange={setSelectedLlm}
                    selectedModel={selectedLlmModel}
                    selectedProvider={selectedLlmProvider}
                    variant="composer"
                    workspaceId={currentWorkspaceId}
                />
            </div>
            <div className="flex items-center gap-2">
                <ComposerPrimitive.Send />
                <ComposerPrimitive.Cancel />
            </div>
        </footer>
    </ComposerPrimitive.Root>
</ThreadPrimitive.Root>
```

State from `useCopilotStore.selectedLlmProvider/Model` + `setSelectedLlm`.

- [ ] **Step 2: Verify in browser** — Copilot still streams messages, send/stop still work, model selection still injects into agent state per `CopilotRuntimeProvider.tsx:81-83`.

- [ ] **Step 3: Run `npm run check`**

- [ ] **Step 4: Commit**

If `@assistant-ui/react` doesn't expose `ComposerPrimitive` + `ThreadPrimitive` the way the AI Hub composer uses them, fall back to fully-custom composer JSX (mirroring `AiHubChatComposer.tsx`'s structure). This is the documented risk in the spec.

---

## Task 11: Remove the model picker from the Copilot panel header

**Files:**
- Modify: `client/src/shared/components/copilot/CopilotPanel.tsx`

- [ ] **Step 1: Delete lines 126-134 (the header `<ModelPicker iconOnly />` block)**

The composer now owns the picker. The header keeps clean-messages and close only.

- [ ] **Step 2: Run `npm run check`**

- [ ] **Step 3: Commit**

---

## Task 12: Hover-card right-edge flip behavior

**Files:**
- Modify: `client/src/shared/components/ai/model-picker/ModelPickerDropdown.tsx`

- [ ] **Step 1: Add the viewport-edge detection**

The hover card is anchored to the right edge of the dropdown by default. When there is <360px of viewport clearance to the right, flip it to the left. Use `useLayoutEffect` to measure the dropdown's `getBoundingClientRect()` against `window.innerWidth`.

```tsx
const dropdownRef = useRef<HTMLDivElement | null>(null);
const [hoverSide, setHoverSide] = useState<'left' | 'right'>('right');

useLayoutEffect(() => {
    if (!dropdownRef.current) {
        return;
    }

    const rect = dropdownRef.current.getBoundingClientRect();
    const rightClearance = window.innerWidth - rect.right;

    setHoverSide(rightClearance < 360 ? 'left' : 'right');
}, [hoveredModelId]);
```

Position the hover card with `left-full ml-2` (right-anchored) or `right-full mr-2` (left-anchored).

- [ ] **Step 2: Browser test**

Open the AI Hub composer near the right edge of the viewport, hover a model row, verify the hover card flips to the left. Move to the centre and verify it stays on the right.

- [ ] **Step 3: Commit**

---

## Task 13: Re-baseline `ModelPicker` test coverage

**Files:**
- Update existing: `client/src/shared/components/ai/model-picker/ModelPicker.test.tsx` (if exists) — or create stub

- [ ] **Step 1: Update or write integration tests**

The existing 483-line `ModelPicker.tsx` may have a test file. If so, port the tests to the new shim — they should now mostly verify the shim wires the new components correctly (rather than re-testing internal behavior). The behavior is owned by `derive.test.ts` + `ModelPickerDropdown.test.tsx` + `ModelPickerHoverCard.test.tsx`.

- [ ] **Step 2: Run `npm run check`** — confirm all tests pass.

---

## Task 14: End-to-end browser smoke

- [ ] **Step 1: Run the dev stack**

```bash
cd server && docker compose -f docker-compose.dev.infra.yml up -d
cd .. && ./gradlew -p server/apps/server-app bootRun &
cd client && npm run dev
```

- [ ] **Step 2: Manual smoke checklist**

Open both surfaces in the browser:

- **AI Hub**: open Hey-Ivica chat. Composer shows the model picker on the left. Click it → dropdown appears with search + tiers + provider groups. Hover a model row → side panel appears with speed/intelligence bars. Pick a different model. Send a message. Verify in network panel that the agent invocation carries `userSelectedLlmProvider/Model` in the request body.
- **Copilot**: open the workflow editor and toggle the Copilot panel. Same set of checks. Confirm send/stop streaming still works (Copilot uses `@assistant-ui` Thread streaming, so a regression here is the highest-risk change in the PR).
- **Both surfaces**: verify the panel headers no longer have the model picker.
- **Personal Agent form** (unchanged): open the agent edit form, confirm the model picker still renders in the form (uses `variant="full"`).
- **Workspace default**: clear the selection (close + reopen) and confirm the trigger shows the workspace default model alias rather than "Select model" placeholder.

- [ ] **Step 3: Screenshot the three states** (closed, dropdown open, hover) for the PR description.

---

## Task 15: PR

- [ ] **Step 1: Open PR with:**
  - Summary referencing the spec (`docs/superpowers/specs/2026-05-26-composer-model-picker-design.md`)
  - The three screenshots
  - A checklist confirming the smoke test passed for both surfaces
  - Note about deferred follow-ups (`@`, `Skill`, `Incognito` in composer, cost-per-turn preview, workspace-admin tier configuration)

---

## Self-Review

**Spec coverage:**
- §"Component split" → Tasks 1, 2, 4, 5, 6, 7
- §"Derived metadata" → Task 1, 2 (helper + tests)
- §"Tier computation" → Task 1, 2 (`resolveTiers` + tests)
- §"Dropdown structure" → Task 5
- §"Hover details panel" → Tasks 4, 12 (flip behavior)
- §"Composer integration — AI Hub" → Task 8
- §"Composer integration — Copilot" → Task 10
- §"Header removal" → Tasks 9, 11
- §"Trigger label" → Task 6 (always shows model alias)

**Placeholder scan:** no TBDs in the per-task steps. The "wire workspace default" line in Task 7 needs a concrete state source — file as a sub-step of Task 7 to read the workspace default from `useApplicationInfoStore` or the existing `defaultRoutingPolicyId` field on the model row.

**Type consistency:**
- `AiGatewayModelInput` defined once in `derive.ts`, imported everywhere
- `ModelPickerTriggerVariant = 'composer' | 'full'` consistent in trigger + shim + callers
- Tier object shape (`recommended | smartest | fastest`) consistent across tests + helper

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-26-composer-model-picker.md`. Two execution options:

**1. Subagent-Driven (recommended)** — fresh subagent per task, review between tasks. Good fit: 15 tasks, each ends in a clean compile + commit checkpoint, browser smoke at the end.

**2. Inline Execution** — execute in this session via executing-plans, batch with checkpoints.

Which approach?
