# Dark Mode Phase 3 — Colour Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the hardcoded Tailwind palette classes that remain in the client to semantic design tokens, so the last quarter of the app responds to theme.

**Architecture:** A measured migration, not a sweep. Every candidate class was converted from Tailwind 4's OKLCH palette to HSL and matched against the nearest token in an appropriate family, producing four disjoint sets: clean matches that migrate mechanically, matches that land on a state variant and need per-site judgement, occurrences that drift visibly in light mode, and documented exceptions that must not be touched. Only the first set is unconditionally safe.

**Tech Stack:** React 19.2, TypeScript 5.9, Vite 8, TailwindCSS 4.3 (OKLCH palette), Vitest 4, Testing Library.

## Global Constraints

- **Working directory is `client/`.** All paths relative to `/Volumes/Data/bytechef/bytechef/client`.
- **Tailwind 4 uses OKLCH, not hex.** Any colour comparison must convert from `oklch()` — the v3 hex values are wrong for this project. Phase 1's mapping table was derived from v3 hex and had to be redone.
- **Deleting a token is a two-file change** — `src/styles/index.css` and `tailwind.config.js`. Three separate tasks in Phase 1 shipped a dangling config mapping. `src/styles/tests/configTokenResolution.test.ts` now guards this; keep it green.
- **Token parity is guarded** by `src/styles/tests/tokenParity.test.ts` (`:root` ↔ `.dark`). Keep it green.
- ESLint `sort-keys`: object keys alphabetical. `--fix` does NOT fix this.
- Named imports alphabetical within `{}`; `type` imports sort by name.
- Interface names end in `I` or `Props`.
- Lucide icons imported with the `Icon` suffix.
- `twMerge` from `tailwind-merge`; never `cn()`.
- No short or cryptic variable names.
- Test files `.test.tsx` in a `tests/` subdirectory; natural-language `it()` descriptions.
- Import `render` from `@/shared/util/test-utils`.
- Canonical Button is `@/components/Button/Button`; Select from `@/components/Select/Select`.
- Commit format: `0 client - <description>`.
- **Verification is `npm run check`** — prettier runs first, and a formatting drift slipped past a gate in Phase 2 because only the individual commands were run.

---

## Scope

**In:** 204 files, 759 occurrences. Excludes `workflow-editor` and `cluster-element-editor` (66 files) and test/story files.

**Excluded and tracked separately:** the editors, whose node-type stroke colours collapse to white in dark and need a design decision first.

## The measurement

Every in-scope occurrence was converted from Tailwind 4 OKLCH to HSL and matched to the nearest token by lightness within an appropriate family. Reproduce with the script in Task 1.

| Set | Occurrences | Disposition |
|---|---|---|
| A — clean match to a base token | ~260 | Task 2, mechanical |
| B — clean match only to a `-hover`/`-active` variant | 162 | Task 3, per-site judgement |
| C — cross-family match (role ≠ CSS property) | ~66 | Task 2, once Task 1's rule lands |
| D — visible light-mode drift | ~248 | **Task 4, blocked on a product decision** |
| E — documented exceptions | 23 | never migrate |

### The central finding

**The token system is organised by CSS property — `surface` / `content` / `stroke` — while the usages are organised by role.** Three examples, all verified:

- `bg-gray-400` ×14 is every loading dot in `LoadingDots.tsx`, `DialogLoader.tsx`, `LazyLoadWrapper.tsx`. It writes `bg-` but is decorative foreground. Against `--content-neutral-tertiary` it is ΔL **+1**.
- `text-gray-300` ×40 is `<KeyIcon className="size-24 text-gray-300" />` in empty states — large decorative icons, not text. Against `--stroke-neutral-tertiary` (`213 27% 84%`) it is ΔL **0**.
- `bg-slate-400` ×12 is the Dark theme preview swatch in `Appearance.tsx`, which must stay literal so it does not follow the active theme.

A rule forbidding cross-family mapping manufactures a coverage gap that does not exist. Task 1 settles that rule.

### Set E — the exceptions, never migrate

| Exception | Count | Why |
|---|---|---|
| Overlay scrim `bg-black/50`, `/80` | 1 (`DialogLoader.tsx`) | Correct in both themes |
| Deliberate dark surface | 1 (`VoiceModeLayout.tsx:114`) | Dark by design in both themes |
| Theme preview swatches | 12 (`Appearance.tsx`) | Must not follow the active theme |
| Categorical identity colours | 22 | Purple/cyan/pink encode identity, not severity; no token models them |

Categorical sites keep their raw class and gain a `dark:` counterpart. Class names must remain complete literal strings — Tailwind's scanner only sees literals, so `` `bg-${hue}-500` `` would drop them from the build.

---

### Task 1: Establish and encode the cross-family mapping rule

Settles the question the measurement raised, and converts ~66 occurrences from "drift" to "exact" before any file is touched.

**Files:**
- Create: `scripts/derive-colour-mapping.mjs`
- Create: `docs/superpowers/notes/2026-08-02-colour-mapping-table.md`

**Interfaces:**
- Consumes: `src/styles/index.css` (token values), `tailwindcss/colors` (OKLCH palette).
- Produces: the authoritative mapping table every later task reads. No runtime code.

- [ ] **Step 1: Write the derivation script**

Create `scripts/derive-colour-mapping.mjs`. It must:

1. Parse the `:root` block of `src/styles/index.css` into `{name, [h, s, l]}` records, tolerating both space-separated (`229 84% 5%`) and comma-separated (`213, 27%, 84%`) forms — both exist in the file.
2. Import `tailwindcss/colors` and convert each referenced value from `oklch()` to sRGB to HSL. The conversion is OKLCH → OKLab → LMS → linear sRGB → gamma-encoded sRGB; do not approximate it with a v3 hex lookup.
3. For each distinct palette class in the in-scope file set, propose the nearest token by lightness.
4. Classify: `exact` (ΔL ≤ 2), `near` (ΔL ≤ 5), `drift` (ΔL > 5), `none`.

**Candidate families must follow role, not CSS property.** This is the rule this task exists to encode:

| Usage shape | Candidate family |
|---|---|
| `bg-` on a filled region | `surface` |
| `bg-` on a decorative shape (`rounded-full` dot, small indicator) | `content` |
| `text-` on text | `content` |
| `text-` on a large decorative icon (`size-16` and up) | `stroke` and `content` both eligible |
| `border-`, `divide-`, `ring-` | `stroke` |

The script cannot infer "decorative" reliably, so it must emit *all* eligible families per class with their best match rather than picking one. A human picks in step 2.

Script must be pure analysis: no writes to `src/`.

- [ ] **Step 2: Generate and commit the mapping table**

Run the script and write its output to `docs/superpowers/notes/2026-08-02-colour-mapping-table.md`, one row per distinct class: count, actual HSL, chosen token, target HSL, ΔL, set (A–E).

Resolve each cross-family case by hand against the usage, using the three verified examples above as precedent. Record the reasoning in the row — a later reader must be able to tell why `bg-gray-400` maps to a `content` token.

- [ ] **Step 3: Verify the three known cases**

The table must contain:

| Class | Target | ΔL |
|---|---|---|
| `bg-gray-400` (loading dots) | `--content-neutral-tertiary` | +1 |
| `text-gray-300` (empty-state icons) | `--stroke-neutral-tertiary` | 0 |
| `bg-slate-400` (`Appearance.tsx`) | set E, no target | — |

If any disagrees, the script's family rule is wrong — fix the script, not the table.

- [ ] **Step 4: Commit**

```bash
git add scripts/derive-colour-mapping.mjs docs/superpowers/notes/2026-08-02-colour-mapping-table.md
git commit -m "0 client - Derive the Phase 3 colour mapping table from Tailwind 4 OKLCH values"
```

---

### Task 2: Migrate set A and set C — the clean matches

~326 occurrences that map to a base token with ΔL ≤ 5. Zero intended visual change in either theme.

**Files:** the in-scope files containing set A/C classes, in six steps (below).

**Interfaces:**
- Consumes: the mapping table from Task 1. It is authoritative — do not re-derive per file.
- Produces: no new exports. Class substitutions only.

**Sequencing** — smallest blast radius first, so shared primitives settle before consumers:

| Step | Area | Files |
|---|---|---|
| 2a | `components/*` | 17 |
| 2b | `shared/*` | 18 |
| 2c | `pages/account` + `pages/settings` | 13 |
| 2d | `pages/*` remainder | 48 |
| 2e | `pages/automation/ai` + `ai-hub` | 40 |
| 2f | `ee/*` | 68 |

For each step:

- [ ] **Step 1: Substitute set A and C classes only**

Use the mapping table. Leave every set B, D and E occurrence untouched — a file will usually contain a mix, and finishing a file is not the goal.

- [ ] **Step 2: Assert no visual change is intended**

For this task the token value equals the palette value within ΔL ≤ 5 by construction, so any *visible* difference means the wrong row was applied. There is no test that can catch this; the reviewer's job is to check each substitution against the table.

- [ ] **Step 3: Run the focused suite and the reconciliation check**

**Do not expect the table to regenerate byte-identically.** The script scans live call sites, so a migration necessarily shrinks its counts — an earlier version of this plan asked for byte-identity here, which is unsatisfiable by construction and was flagged by the step 2a implementer.

The correct check is **reconciliation**: regenerate the table **to a temporary file**, diff it against the committed copy, and confirm every delta is accounted for by exactly the occurrences this step migrated. Step 2a's arithmetic is the model — 13 migrations produced 758→745 occurrences, 144→143 classes, 204→197 files, with each per-row change traceable to one migration. Any delta you cannot attribute means something was changed that should not have been.

**The committed table stays frozen at the Task 1 derivation. Never commit a regenerated copy.** It is a contract, not a status report: a migrated row disappears from a live scan, so committing the regeneration would progressively erase the very mappings the later steps still need. Every step reads the same frozen table, and the diff against it grows monotonically as the migration proceeds — that growing diff *is* the progress record.

Reconcile by running the script and diffing. Do not hand-enumerate which files reached zero — that method produced a wrong attribution at 9 files in step 2a, and step 2f is 68.

```bash
npx vitest run <the step's directory>
```

Then confirm the step's directory contains no remaining set A or C classes:

```bash
node scripts/derive-colour-mapping.mjs --check <directory>
```

Extend the script with a `--check` mode in Task 1 if you have not already.

- [ ] **Step 4: Commit the step**

```bash
git commit -m "0 client - Migrate <area> to semantic colour tokens"
```

---

### Task 3: Adjudicate set B — the state-variant matches

162 occurrences whose only close match is a `-hover` or `-active` token. `bg-gray-100` → `--surface-neutral-primary-hover` is ΔL 0 and semantically wrong: a state variant standing in for a resting value.

**Files:** determined by the set B rows in Task 1's table.

- [ ] **Step 1: Classify each site**

Per occurrence, decide which applies:

1. **Genuinely a hover/active state** — the class sits inside a `hover:` or `active:` variant, or a `data-[state=]` selector. Migrate to the state token.
2. **A resting value that happens to match a state token's lightness** — migrate to the nearest *base* token and accept the ΔL, or leave it for Task 4 if the ΔL exceeds 5.
3. **The base token should have this value** — the state token is right and the base is wrong. Report it; do not change the token unilaterally.

- [ ] **Step 2: Record the classification before changing code**

Append the per-site decision to Task 1's mapping table. A reviewer must be able to check the decision without re-deriving it.

- [ ] **Step 3: Apply, verify, commit**

Same per-step verification as Task 2. Commit per area, message `0 client - Resolve state-variant colour matches in <area>`.

---

### Task 4: The 86 coupled occurrences

**Owner decision, recorded 2026-08-03.** The blocked sets were split into two populations with different logic, and answered separately:

| population | n | decision |
|---|---|---|
| **Coupled** — a migrated surface token sits under a raw text literal | **86** | **Migrate the text partners.** Not a preference: these render dark-on-dark until the pair moves together. |
| **Standalone** — D/F literals with no migrated surface partner | 201 | **Leave raw.** Light mode stays byte-identical; these sites remain theme-blind, exactly as today. |

So Task 4 is no longer "resolve set D". It is: migrate the 86 text literals that are coupled to an already-migrated surface, drawn from sets D and F, and leave every other D and F occurrence alone.

**Worklist:** `.superpowers/sdd/2026-08-02-dark-mode-phase-3-colour-migration/task-4-worklist.md` — all 86 sites with their surface/text pair.

The 17 distinct text literals span both sets. `dark:text-blue-300` is written with a `dark:` prefix, so it is governed by the dark-literal rule rather than by a set row.

**Consequences accepted:** light mode visibly changes at those 86 elements — `text-gray-800` moves from 17% to 5% lightness, `text-green-800` shifts 9 points. That drift is confined to the coupled elements; the other 201 sites are untouched, so light mode elsewhere is unchanged.

**This closes the hard gate at Task 6 Step 3b.** Once these 86 land, no migrated surface sits under a literal text colour, and the invisible-badge defect is resolved.

### Task 5: Categorical colours and the exception sweep

23 occurrences in set E.

> **BLOCKED on the `darkMode` config fix in Task 6 Step 3.** `tailwind.config.js:5` is `darkMode: ['class', 'class']`. The second array element is the custom selector, and the literal `class` is parsed as a *type* selector, so every `dark:` utility compiles to `:is(class *)` and matches nothing. Verified by compiling with the repo toolchain: `.dark\:border-amber-500:is(class *)` today versus `:is(.dark *)` once fixed. **All 36 `dark:` utilities in the codebase are dead CSS and always have been** — dark mode works solely through the `.dark` class redefining CSS variables in `index.css`.
>
> Adding `dark:` variants before that fix lands would produce dead CSS. Run Task 6 Step 3 first, or run this task after Task 6.

- [ ] **Step 1: Add `dark:` variants to the 22 categorical sites**

They encode identity — knowledge base, MCP server, span type, skill avatar — not severity, so no semantic token fits. Keep the raw class, add a dark counterpart.

Class names must stay complete literal strings. `getSkillColor.ts` builds from an array of literals; refactoring it to `` `bg-${hue}-500` `` would drop every one from the Tailwind build.

- [ ] **Step 2: Confirm the do-not-touch set is untouched**

```bash
grep -rn "fixed inset-0.*bg-black/" src --include="*.tsx" | grep -v "components/ui/"
```
Expected: only `shared/components/DialogLoader.tsx`.

`VoiceModeLayout.tsx:114` must still read `bg-black text-white`, and `Appearance.tsx`'s Dark preview card must still use literal `bg-slate-*`.

- [ ] **Step 3: Commit**

```bash
git commit -m "0 client - Add dark variants to categorical colour sites"
```

---

### Task 6: Exit

- [ ] **Step 1: Full check**

`npm run check` must exit 0.

- [ ] **Step 2: Re-measure**

Run the Task 1 script over the whole in-scope set. Every remaining occurrence must be in set D (if option 1 was chosen) or set E. Anything else is a missed site.

- [ ] **Step 3: Fix the `darkMode` selector — do this BEFORE the two steps below**

`client/tailwind.config.js:5`:

```js
darkMode: ['class', 'class'],     // every dark: utility compiles to :is(class *) — matches nothing
darkMode: ['class', '.dark'],     // correct
```

This is a real, pre-existing bug found during step 2d and verified by compiling with the repo's own toolchain. All 36 `dark:` utilities in the codebase are currently dead, including shadcn's `dark:bg-destructive/60` on `button.tsx` and `badge.tsx`.

**Fixing it wakes 36 dormant utilities at once**, so dark rendering changes in places nobody has reviewed. That is why it is sequenced here rather than earlier: it lands together with the default flip and the flag, as one reviewable change at the moment dark mode is actually looked at.

Task 5 depends on this step — adding `dark:` variants to the categorical sites before it would produce dead CSS.

- [ ] **Step 3b: HARD GATE — set D must be resolved before the flip**

Discovered by the step 2f review, and it changes what "blocked" means for set D.

Migrating set A while set D stays literal leaves roughly **40 badge sites where a migrated surface meets an unmigrated D-set text colour**. The worst pair, in `ee/pages/settings/platform/api-connectors/utils/endpointEditor-utils.ts:41` and `httpMethod-utils.ts:41`:

```
bg-surface-neutral-secondary   dark: 217 33% 17%
text-gray-800                  dark: 215 31% 16.9%
                                     ^^^^ 0.1 lightness points apart — invisible
```

Today those badges read `bg-gray-100 text-gray-800` and are legible in both themes. The same shape exists at `SyncSourceStatusBadge.tsx:9`, `approval-task-utils.tsx:119`, and across the `pages/automation/ai/gateway` badge family.

So set D is **not** merely blocked on a preference about light-mode drift. It is a prerequisite for switching dark mode on at all: flipping the default with D unresolved ships ~40 badges with invisible text.

This does not make the migration wrong — it is the expected intermediate state of migrating A while D waits — but it must be closed before Step 4, by resolving set D one way or another.

- [ ] **Step 4: Flip the theme default**

Only once Tasks 2–5 are complete and set D is resolved:

- `src/main.tsx` — `defaultTheme="light"` → `"system"`
- `index.html` — the absent-key case → `'system'`

The two must continue to agree on every stored value, including absent and corrupt.

- [ ] **Step 4: Enable the feature flag**

Add `ff-445` beyond `application-local.yml` so the Appearance page is reachable. This is the moment dark mode ships.

---

## Risks

| Risk | Mitigation |
|---|---|
| A v3 hex value is used for a comparison | The script converts from OKLCH; Phase 1's table was wrong for exactly this reason |
| Nearest-by-lightness proposes a semantically wrong token | It proposes, a human decides — that is Task 3's entire purpose |
| A migrated class was an intentional exception | Set E is enumerated with a per-item reason, and Task 5 Step 2 asserts it |
| Deleting or repointing a token leaves a dangling config mapping | `configTokenResolution.test.ts` fails the build |
| Light-mode regression | Task 4 is blocked pending an explicit decision, and no other task changes light mode by more than ΔL 5 |

## Follow-up

- **Editors** — 66 files, blocked on a decision about node-type colours in dark.
- **Identity-colour system** — the 22 categorical sites and the editor node colours are the same problem; solve them together.
- **`vi.mock` factories returning a fresh object per render** — present in 14 of 15 Phase 2 dialog test files, and demonstrated to cause an infinite render and OOM. Fix with `vi.hoisted`.
