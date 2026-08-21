# Dark mode — completing the client theme

**Date:** 2026-08-02
**Status:** Design approved, ready for planning
**Surface:** `client/` only. No server changes.

## Problem

Dark mode is half-built. The plumbing exists — a theme provider, an Appearance settings page, a
`.dark` token block, and `darkMode: ['class', 'class']` in the Tailwind config — but it has never
been finished or, judging by the token values, ever looked at.

Three things are wrong:

1. **The `.dark` palette is unvetted.** Eight tokens with live usage have no `.dark` override at all
   and silently inherit their light values; several more were copied verbatim from `:root`.
2. **The CSS `color-scheme` property is never declared**, so browser-native UI (scrollbars, form
   controls, `<select>` popups) renders light-on-dark, and Shiki's `light-dark()` code themes never
   resolve to dark.
3. **1215 hardcoded Tailwind palette classes** (`bg-white`, `text-gray-400`, …) bypass the token
   system and cannot respond to theme at all.

The feature ships hidden: the Appearance nav item is gated on `ff-445`, which is enabled only in
`application-local.yml`.

## Key measurement

The ratio that drives the whole design:

| | token-based | hardcoded palette |
|---|---|---|
| Repo-wide | 3445 (74%) | 1215 (26%) |
| The 215 in-scope files | 961 (56%) | 768 (44%) |

The app is already 74% token-based. **Fixing the palette is therefore centralised and
high-leverage** — one file corrects 3445 usages. The class migration is distributed work serving
the remaining 26%.

`src/components/ui` (shadcn) is the most token-based directory in the codebase at 377 token usages
to 11 hardcoded — a consequence of `cssVariables: true` in `components.json`.

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Scope | Core app; workflow editor and cluster-element editor deferred | Editors need node-type colour design decisions, not just a port |
| Persistence | localStorage only, no server change | Already built; per-browser is acceptable |
| Palette | Audit and fix, keep the existing token structure | Names and scheme are sound; the values are not |
| Migration | Migrate to semantic tokens, area by area | Fixes root cause; accepts deliberate light-mode drift |
| Categorical colours | `dark:` variants, documented exception | No semantic token models identity coding |
| shadcn | Included, 8 targeted fixes only | 97% already token-based |
| Hand-rolled dialogs | Convert to shadcn primitives, full adoption, **before** the colour migration | Deletes markup the migration would otherwise fix first |

## Scope

**In:** 215 files, 768 hardcoded occurrences. The 15 hand-rolled dialogs of Phase 2 are a subset
of those 215, not an addition to them — all 15 already carry palette classes and would have been
touched by Phase 3 regardless.

**Deferred to a follow-up:** 66 files under `pages/platform/workflow-editor` and
`pages/platform/cluster-element-editor`; the node-type stroke colours (`--stroke-{branch,loop,map,
each,parallel,subflow,condition,fork-join}-*`, all currently collapsed to `0 0% 100%` in dark,
erasing the editor's colour coding); `workflow-builder.tsx`, which stays `defaultTheme="light"`;
and server-side theme persistence.

## Phase ordering

Three phases. The ordering is load-bearing:

1. **Foundation** — 4 files. Independent of everything and delivers dark mode for 3445 usages.
   Goes first so that every subsequent phase can be reviewed in dark mode as it lands.
2. **Dialog conversion** — 15 files. Runs before the colour migration because it *deletes* markup
   the migration would otherwise have to fix. Delete-then-edit is cheap; edit-then-delete is waste.
3. **Colour migration** — 215 files, minus whatever Phase 2 removed.

Phase 3 does not hard-depend on Phase 2. If Phase 2 is deprioritised, Phase 3 proceeds and simply
does slightly more work across those 15 files. Phase 2 is written to be separable.

---

## Phase 1 — Foundation

Four files. Delivers dark mode for 3445 usages on its own.

### 1.1 `client/src/styles/index.css` — palette audit

**Add missing `.dark` overrides.** Each is defined in `:root`, has live usage, and has no dark
counterpart, so it renders its light value in dark mode:

| Token | Usages |
|---|---|
| `--stroke-neutral-secondary` | 74 |
| `--stroke-neutral-tertiary` | 20 |
| `--content-destructive` | 14 |
| `--stroke-neutral-primary` | 9 |
| `--skeleton` | 8 |
| `--surface-brand-secondary-hover` | 4 |
| `--content-success` | 2 |
| `--content-warning` | 2 |
| `--stroke-warning-secondary` | 2 |

Counts are exact-match. A word-boundary grep over-reports these families, because
`text-content-destructive-primary` contains `text-content-destructive`.

Derived by parsing `index.css` and diffing the `:root` and `.dark` token sets — 151 vs 130, a
21-token gap. Of those 21: **9** are the real gaps above; **2** are dead
(`--stroke-brand-primary-pressed`, `--stroke-neutral-primary-hover`, 0 usages each); and **10** are
legitimately exempt — `--radius` and `--workflow-nodes-popover-component-menu-width` are not
colours, and the 8 `--sidebar*` tokens are `var(--muted)`-style aliases declared on the same
element, so they resolve against the dark values automatically and must NOT be given overrides.

Task 1 of the plan encodes this diff as a permanent regression test.

`--stroke-neutral-secondary` is the most damaging: 74 borders across the app currently render at
`214 32% 91%` — near-white — against dark surfaces.

**Re-derive values copied from light.** These are light surfaces sitting in a dark UI:

- `--surface-destructive-secondary` and its `-hover` / `-active`. In dark these resolve to
  `0 93% 94%` (very light pink) and pair at usage sites with `text-content-destructive-primary`
  (`0 84% 60%`) for roughly 2.5:1 — below AA.
- `--surface-warning-secondary` and its `-hover` / `-active`.

**Delete dead tokens** — zero usages, verified by exact-match grep across `*.tsx`, `*.ts`, `*.css`:
`--content-destructive-secondary`, `--surface-overlay-primary`, `--stroke-neutral-primary-hover`,
`--stroke-brand-primary-pressed`, `--info`, `--info-foreground`. They are traps: `--info` reads as
an available variant and would hand the next caller a light-blue surface in dark mode.

**Add `color-scheme`:**

```css
:root { color-scheme: light; }
.dark { color-scheme: dark; }
```

The highest-leverage line in the design. It governs browser-native UI — scrollbars, form controls,
`<select>` dropdowns, date pickers — and it is what Shiki's `defaultColor="light-dark()"` in
`components/assistant-ui/shiki-highlighter.tsx` resolves against. That component was written
correctly against a contract the stylesheet never fulfilled.

**Leave alone:** the node-type `--stroke-*` families. They belong to the deferred editor work.

### 1.2 `client/src/shared/providers/theme-provider.tsx` — two bugs

- The `useEffect` reads `window.matchMedia` once and never subscribes. On the `system` setting,
  changing the OS theme does nothing until reload. Add a `change` listener, removed on cleanup.
- `initialState.setTheme` is a no-op and the context defaults to a valid object, so the
  `context === undefined` guard in `useTheme` can never fire. Either default the context to
  `undefined` so the guard works, or drop the guard.

### 1.3 `client/index.html` — flash prevention

The existing inline script is `type="module"`, which is implicitly deferred and runs after the
document is parsed — far too late to prevent a flash. Add a **blocking, non-module** `<script>` in
`<head>` that reads `bytechef.ui-theme` from localStorage, resolves `system` via `matchMedia`, and
stamps the class on `documentElement` before first paint.

Make `<meta name="theme-color">` (hardcoded `#111827`) follow the resolved theme.

### 1.4 `client/src/main.tsx` — default

`defaultTheme="light"` means `system` never applies for a user who has not chosen. Change to
`system`. `workflow-builder.tsx` keeps `light` — deferred editor surface.

---

## Phase 2 — Dialog conversion

### 2.1 Why

15 dialogs hand-roll the modal shell instead of using `components/ui/dialog.tsx`. They import
**zero** shadcn primitives — they sit entirely outside the design system.

Locations: 14 under `pages/automation/ai/gateway/`, 1 at
`pages/settings/platform/workflow-alerts/components/WorkflowAlertRuleDialog.tsx`.

Accessibility audit — all fifteen fail every check:

| Feature | Present |
|---|---|
| `Escape` to close | 0 / 15 |
| `role="dialog"` | 0 / 15 |
| `aria-modal` | 0 / 15 |
| Portal | 0 / 15 |
| Focus trap / restore | 0 / 15 |
| Body scroll lock | 0 / 15 |

A keyboard user cannot close any of these dialogs. This is an accessibility defect independent of
theming; it is sequenced here because it also removes work from Phase 3.

### 2.2 What

Full primitive adoption. Shell plus form controls:

| From | To | Count |
|---|---|---|
| `<div className="fixed inset-0 … bg-black/50">` | `Dialog` / `DialogContent` / `DialogHeader` / `DialogTitle` / `DialogFooter` | 15 |
| `<label>` | `Label` | 74 |
| `<input>` | `Input` | 53 |
| `<button>` | `Button` | 26 |
| `<select>` | `Select` | 13 |
| `<textarea>` | `Textarea` | 4 |

**`<select>` → `Select` is a real API change** — `onChange(event)` becomes
`onValueChange(value)`, and `<option>` becomes `SelectItem`. All 13 need form-behaviour testing.
The other four swaps are mechanical.

### 2.3 Effect on Phase 3

- Deletes 15 of the 19 overlay scrims. Exception class B shrinks from 19 to 4.
- Removes most of the 36 palette classes in those files, since the replaced markup carried them.

Phase 3 counts below are stated **pre-Phase-2** and will shrink accordingly.

### 2.4 Verification

`npm run check`, plus manual interaction testing per dialog: open, close via Escape, close via
overlay click, tab-cycle within the dialog, focus restoration on close, and submit for every
converted `<select>`.

---

## Phase 3 — Colour migration

### 3.1 Mapping table

Derived by converting each Tailwind colour to HSL and comparing against the token value.

**Near-exact — 214 occurrences, no visible light-mode change:**

| From | To | Delta |
|---|---|---|
| `bg-white` (40) | `bg-surface-neutral-primary` | exact |
| `text-gray-400` (56) | `text-content-neutral-tertiary` | 218 11% 65% → 215 20% 65% |
| `text-gray-300` (40) | `text-content-neutral-tertiary` | close |
| `bg-gray-50` (34) | `bg-surface-main` | 210 20% 98% → 210 40% 98% |
| `bg-gray-100` (33) | `bg-surface-neutral-secondary` | 220 14% 96% → 210 40% 96% |
| `border-gray-200` (11) | `border-stroke-neutral-secondary` | 220 13% 91% → 214 32% 91% |

**Drifting — 83 occurrences, light mode will visibly change:**

| From | To | Delta |
|---|---|---|
| `text-gray-800` (23) | `text-content-neutral-primary` | 215 28% 17% → 229 84% 5%, much darker |
| `text-gray-700` (20) | `text-content-neutral-secondary` | 217 19% 27% → 215 19% 35%, lighter |
| `text-gray-600` (16) | `text-content-neutral-secondary` | near-exact |
| `text-gray-500` (14) | `text-content-neutral-secondary` | 220 9% 46% → 215 19% 35%, darker |
| `text-gray-900` (10) | `text-content-neutral-primary` | 221 39% 11% → 229 84% 5% |

This table is the authoritative record of expected light-mode change. A light-mode difference not
explained by a row here is a regression.

**Semantic hues — mechanical, by property:**

| Hue | Token family |
|---|---|
| blue | `brand` |
| red | `destructive` |
| green, emerald | `success` |
| yellow, amber, orange | `warning` |

applied as `bg-surface-*`, `text-content-*`, `border-stroke-*`.

### 3.2 Exceptions

Hardcoded is not the same as wrong. Four distinct classes:

| Class | Count | Treatment |
|---|---|---|
| A — Categorical / identity colours | 21 | Keep the raw class, add a `dark:` counterpart |
| B — Overlay scrims (`bg-black/50`, `bg-black/80`) | 19 → 4 after Phase 2 | Do not touch |
| C — Deliberate dark surface | 1 | Do not touch |
| D — `text-white` on a coloured fill | 12 | Migrate only when its fill migrates |

**A — Categorical.** These encode identity, not severity, and no semantic token models them:

```
shared/components/workflow-executions/WorkflowExecutionLogsContent.tsx:21  bg-purple-100 text-purple-600
pages/automation/connections/components/ConnectionScopeBadge.tsx:18        text-purple-500
pages/automation/ai-hub/messages/AiHubToolCallRenderer.tsx:352,556         bg-purple-100 text-purple-700
pages/automation/ai-hub/composer/AiHubChatComposer.tsx:44,47,48            bg-{pink,purple,cyan}-100 + text-*-700
pages/automation/ai/skills/utils/getSkillColor.ts:1                        SKILL_COLORS x6
pages/automation/ai/gateway/.../AiObservabilityTraceDetail.tsx:39          bg-purple-100 text-purple-800
pages/automation/ai/gateway/.../SpanWaterfall.tsx:20                       bg-purple-400
```

`pages/automation/project/components/WorkflowShareDialog.tsx:225` is inside a `{/* … */}` block.
Dead code — delete rather than convert.

*Constraint:* these class names must remain complete literal strings. Tailwind's content scanner
only sees literals; refactoring `SKILL_COLORS` to `` `bg-${hue}-500` `` would silently drop them
from the build.

**B — Overlay scrims.** Translucent black is correct in both themes. After Phase 2 the remaining
four are `components/ui/{dialog,sheet,alert-dialog}.tsx` — which *are* the primitive — and
`shared/components/DialogLoader.tsx`. Converting any of them would break every modal overlay.

**C — Deliberate dark surface.** `shared/lib/voice/VoiceModeLayout.tsx:114` — `bg-black text-white`,
a full-screen voice mode that is dark by design in both themes.

**D — `text-white` on a coloured fill.** Needs a rule, not a blanket exemption:

- *Leave* where the fill is already a token — `bg-destructive text-white` in `button.tsx` and
  `badge.tsx`, which additionally already handle dark via `dark:bg-destructive/60`.
- *Convert* where the fill is itself being migrated — `bg-emerald-600 text-white` in
  `VoiceModeLayout` becomes `bg-surface-success-primary text-content-onsurface-primary`.

Pairs must move together. Migrating a fill while leaving its foreground looks correct until a token
value changes, at which point the coupling fails invisibly.

### 3.3 shadcn — 8 targeted fixes

`components/ui` is 97% token-based. Touch only:

| File | Fix |
|---|---|
| `alert.tsx:17` | amber warning variant ×5 → `warning` tokens |
| `button.tsx:14` | `text-white` → foreground token |
| `badge.tsx:16` | `text-white` → foreground token |
| `slider.tsx:54` | `bg-white` thumb → surface token |

`alert.tsx` matters most — a shared primitive, so leaving it hardcoded breaks the warning alert
everywhere it appears.

### 3.4 Sequencing

Smallest blast radius first, so shared primitives settle before their consumers:

| Step | Area | Files |
|---|---|---|
| 1 | `components/ui` (8 fixes) + `components/*` primitives | 17 |
| 2 | `shared/` — layout, sidebar, ai-chat, error, connection | 18 |
| 3 | `pages/account/settings` + `pages/settings/platform` | 14 |
| 4 | `pages/*` remainder — automation non-AI, platform | 48 |
| 5 | `pages/automation/ai` + `ai-hub` | 50 |
| 6 | `ee/*` — settings, embedded, automation, shared | 68 |
| | **Total** | **215** |

Each step is independently reviewable in both themes and independently revertable.

---

## Verification and exit

### Third-party surfaces

| Surface | Status | Action |
|---|---|---|
| Sonner toasts | Already wired — `useTheme` plus `hsl(var(--popover))` CSS vars | none |
| react-data-grid | `--rdg-color-scheme` set in both blocks | verify visually |
| Shiki code blocks | `light-dark()` never resolves to dark | fixed by §1.1 `color-scheme` |
| Native controls, scrollbars, `<select>` popups | Render light-on-dark | fixed by §1.1 `color-scheme` |

### Per-step verification

- `npm run check` — lint, typecheck, tests.
- Grep assertion: the step's directory contains no palette classes outside the documented
  exceptions in §3.2.
- Manual review in **both** themes. Light mode needs review too — 83 occurrences drift by design,
  and §3.1 is the reference for what is expected.
- `Button.test.tsx`, `Badge.test.tsx`, and `Switch.test.tsx` assert on class strings and must be
  updated in lockstep. They serve as a tripwire.

### Exit criteria — flipping `ff-445`

1. All three phases complete.
2. Grep assertion clean across the in-scope set.
3. Both themes reviewed across all six Phase 3 steps.
4. `ff-445` enabled beyond `application-local.yml`.

### Risks

| Risk | Mitigation |
|---|---|
| Light-mode regressions from the 83 drifting mappings | Per-step light review against the §3.1 table |
| `<select>` → `Select` API change breaks form submission | Per-dialog interaction testing, §2.4 |
| `important: true` makes third-party overrides brittle | Third-party surfaces confined to the four above, all verified |
| Mechanical sweep converts an intentionally hardcoded value | Explicit exception classes, §3.2 |
| Runtime-composed class names dropped by Tailwind's scanner | Literal-string constraint, §3.2 A |
| A missed area ships broken behind an enabled flag | Grep assertion is mechanical, not judgment-based |

## Follow-up work

1. **Editor dark mode** — 66 files across `workflow-editor` and `cluster-element-editor`.
2. **Identity-colour system** — node-type stroke colours plus the 21 categorical sites from
   §3.2 A, solved together.
3. **Server-side theme persistence** — if theme should follow a user across browsers.
