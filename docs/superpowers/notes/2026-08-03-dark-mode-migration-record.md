# Dark mode migration — durable record

**Completed 2026-08-03.** Distilled from the working notes of a three-phase migration before that
scratch directory was deleted. Everything here is evidence or reasoning that exists nowhere else.

## Where the rest lives

| | |
|---|---|
| Per-class mapping — target token, ΔL, ΔE, set letter, cross-family adjudications | [`2026-08-02-colour-mapping-table.md`](./2026-08-02-colour-mapping-table.md) |
| Plan, including its in-flight corrections | `docs/superpowers/plans/2026-08-02-dark-mode-phase-3-colour-migration.md` |
| Original design and its decisions | `docs/superpowers/specs/2026-08-02-dark-mode-design.md` |
| Follow-up work, with evidence | `.superpowers/sdd/2026-08-02-dark-mode-phase-3-colour-migration/` — **not yet filed as issues**, see below |
| Every code change and why | git history |

The eight follow-ups were written up but never reached the tracker; their only copies are the
`fix-*.md` and `task-*-report.md` files in the SDD workspace named above. That directory must not be
deleted until they are filed or folded in here.

This file covers only what those do not.

---

## Rules that govern this area

Discovered during the work, mostly by breaking them first.

**Vendored component directories are never styled directly.** `src/components/ui` (shadcn) and
`src/components/assistant-ui` are generated from packages. They are styled through composition
wrappers — `components/Button/Button.tsx`, `components/Select/Select.tsx`, `components/Badge/Badge.tsx`,
`components/Switch/Switch.tsx` — and `eslint-restricted-imports.mjs` enforces importing the wrapper.
Nothing enforced *not editing what they wrap*, and the migration edited three shadcn files before this
was caught. Both directories are now excluded in `scripts/derive-colour-mapping.mjs` via
`EXCLUDED_DIRECTORY_PATHS`.

Two of those three edits changed unreachable code: `Badge` and `Button` wrappers do
`Omit<ShadcnProps, 'variant'>`, so shadcn's own variants never render.

**Deleting a design token is a two-file change.** A token lives in `src/styles/index.css`, in
`tailwind.config.js`, and in the utility classes components use. Removing only the declaration leaves
the config generating a live class name that resolves to an undefined variable — broken colour, no
build error, no test failure. Three separate tasks shipped this before it was made a constraint.
`src/styles/tests/configTokenResolution.test.ts` now catches it.

**Content-family `-hover`/`-active` tokens must not be dark-identical.** A token named `-hover` whose
`:root` and `.dark` values match is a lie the mapping table will believe. `--content-brand-primary-hover`
was `217 91% 36%` in both blocks, giving **1.62:1** against `--surface-brand-secondary` in dark. That
shipped twice — `format.ts:59` and `KnowledgeBaseDocumentChunkListSelectionBar.tsx:15` — before the
generator was taught to refuse such targets.

**Tests that guard this area, and what they do not catch.**

| test | catches |
|---|---|
| `src/styles/tests/tokenParity.test.ts` | a `:root` token with no `.dark` counterpart |
| `src/styles/tests/configTokenResolution.test.ts` | a `tailwind.config.js` `var(--x)` with no declaration |
| `scripts/derive-colour-mapping.mjs --check` | unmigrated set A/C occurrences; dark-identical state tokens |

None of them catch contrast. Nothing catches a class name that produces no CSS — that is how
`text-content-error-primary`, a token that never existed, survived at three call sites in
`AiHubToolCallRenderer.tsx` and `AiHubRetryBanner.tsx`, with a test asserting on the dead class.

---

## Decisions that look arbitrary without their reasoning

**Cross-family mappings are deliberate.** The token families are named after CSS properties —
`surface`, `content`, `stroke` — while usages are organised by role. `bg-gray-400` is every loading
dot in the app: it writes `bg-` but is decorative foreground, and maps to `--content-neutral-tertiary`
at ΔL +1. `text-gray-300` is `size-24` empty-state icons: it writes `text-` but wants a border-weight
tone, and maps to `--stroke-neutral-tertiary` at ΔL 0. Forbidding cross-family mapping manufactures a
coverage gap that does not exist. Do not "correct" these toward their CSS property.

**Rows are keyed by written class, not by apparent intent.** `text-gray-400` and `text-gray-300` are
both used for `EmptyList` icons, and they map to different families. Four `text-gray-400` icons at
`size-12` look identical in role to the `text-gray-300` ones. Harmonising them would be a defect —
the table is per written class, and that discipline is what kept 369 substitutions mechanical.

**Some classes are migrated at one site and left literal at another.** The forced set was defined by
whether an occurrence renders dark-on-dark, not by its class name. `text-gray-800` appears on the
migrated list 20 times and stays raw elsewhere. Working from the class name rather than the site list
over-migrates and changes light mode where the owner decided it must not.

**Categorical identity colours keep raw classes plus `dark:` counterparts.** Purple for knowledge
bases, cyan for MCP servers, the skill avatar palette, span types, HTTP methods. They encode *which
thing*, not *how severe*; no semantic token models identity, and forcing one collapses the
distinction. Members of one set must be treated together — theming a hue-filtered subset leaves
near-black chips beside pastel ones, an OKLab ΔE of 0.63–0.70 inside a single badge row.
`EXCEPTION_SITES` in the mapping script pins the line ranges of these maps.

**Hover suppression is not a hover style.** `bg-gray-50 hover:bg-gray-50` on a zebra-striped row exists
so the stripe does *not* change on hover. A row-key mechanism that correctly learns "the written class
carries usage intent" breaks the one case where two different written classes must resolve to the same
token. Same shape at `attachment.tsx`'s `hover:bg-white!`.

**`bg-black/50` scrims are correct hardcoded.** A translucent black overlay should darken whatever is
behind it in both themes. Set E exists so a migration cannot "improve" that.

**Owner decisions, not derived:** migrate only where forced and leave the rest raw; fold the AI Gateway
guardrails into one Save; skip all visual gates.

---

## Measurement — the corrections that cost defects

Each of these was learned by shipping something.

**Tailwind 4 is OKLCH, not hex.** The first mapping table was derived from v3 hex values. The greys
landed close by coincidence, which made a spot check look like validation. Any comparison must convert
from `oklch()` — the conversion chain is OKLCH → OKLab → LMS → linear sRGB → gamma-encoded sRGB, and
`scripts/derive-colour-mapping.mjs` implements it.

**A lightness delta is not a contrast ratio.** Two same-hue dark blues 14 points apart are perceptually
one colour. A "≥10 lightness points" floor passed a pair at **1.62:1**. Use WCAG contrast: 4.5:1 for
text, 3:1 for decorative marks.

**ΔL alone hides hue shifts.** 70 set A occurrences exceeded 2× JND in OKLab ΔE while reading as
"exact" on lightness; `text-orange-800` shifted hue 17°→35°. Set F exists for exactly this, and the
mapping table carries a per-row dominant-axis column so no row can be misattributed by eyeballing HSL.

**Contrast is against what is behind an element, not what shares its class string.** Matching a surface
token and a text literal on the same element found 86 coupled sites. The real figure was larger — bare
text inherits a container that migrated, and 107 further sites rendered dark-on-dark through a parent.

**An interactive element has several states, and rest is rarely the one that matters.** A Delete menu
item measured 4.23:1 at rest and **2.28:1 on focus** — the state a user is in at the instant they
commit. Every contrast check in this project measured rest until that was found.

**Verify a class produces CSS, not that its name looks right.** `dark:` utilities compiled to
`:is(class *)` — a type selector matching a `<class>` element that does not exist — because
`tailwind.config.js` read `darkMode: ['class', 'class']`. All 36 were dead, and had always been.
Dark mode worked solely through the `.dark` class redefining variables. Compile and read the emitted
selector.

---

## Contrast evidence

Measured at completion. A change to any of these tokens should re-check the pairs below.

| pair | dark | note |
|---|---|---|
| `--content-destructive` on `--surface-destructive-secondary` | 4.56:1 | tightest passing pair, 14 sites |
| destructive menu items, all four states | 5.07:1 worst | was 2.28:1 on focus |
| `--content-destructive` on `--muted` | 3.88:1 | icon only, clears the 3:1 floor |
| categorical badge sets, member vs member | ΔE 0.081 min | ~4× JND, sets stay distinguishable |
| skill avatar fills, 6 members | ΔE 0.186 min | `-600` chosen because `teal-500` was 2.31:1 under its icon |

Full per-class figures are in the mapping table.

---

## Deliberately unfinished

**The editors** — `workflow-editor` and `cluster-element-editor`, ~66 files and ~196 occurrences,
excluded in code via `EXCLUDED_DIRECTORY_NAMES`. Blocked on a design decision: every node-type stroke
token (`--stroke-branch-*`, `--stroke-loop-*`, `--stroke-map-*`, and the rest) collapses to
`0 0% 100%` in dark, erasing the colour coding that distinguishes node types in light. Resolving that
is a design decision, not a mapping one, and it gates the whole ~196-occurrence migration.

**~196 raw classes remain in scope, by decision.** Self-contained light islands — a raw text colour on
a raw background — render consistently in both themes. They are unthemed, not broken.

**Residuals**, since fixed but never tracked: ~45 `<Button>` call sites without a dark hover
counterpart (fixed at the wrapper, commit `4f0283d9884`), the guardrails save surface (folded into
one Save), and the dialog test-mock hazard.

**Nothing was ever looked at.** Every figure in this document is computed. Visual gates were out of
scope by the owner's decision, so no one has opened the application in dark mode. `/account/appearance`
is the highest-value first look — it is where a review caught a wrong token value by arithmetic alone.
