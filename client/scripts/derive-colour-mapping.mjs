#!/usr/bin/env node

/**
 * Derives the Phase 3 colour mapping table: hardcoded Tailwind palette classes -> semantic design tokens.
 *
 * The script is pure analysis. It reads `src/styles/index.css` and the in-scope source files and writes
 * nothing back into `src/`. Its output is markdown on stdout, which is committed as
 * `docs/superpowers/notes/2026-08-02-colour-mapping-table.md`.
 *
 * Two things this script deliberately does NOT shortcut:
 *
 * 1. Tailwind 4 ships its palette as `oklch()` strings, not hex. Every palette value is converted through
 *    the real chain (OKLCH -> OKLab -> LMS -> linear sRGB -> gamma-encoded sRGB -> HSL). A Tailwind v3 hex
 *    lookup table produces greys that land close enough to look correct and chromatic colours that do not.
 *
 * 2. Token families are named after CSS properties (`--surface-*`, `--content-*`, `--stroke-*`) but usages
 *    are organised by role. A `bg-` class on a decorative dot is foreground; a `text-` class on a `size-24`
 *    empty-state icon is a decorative rule, not body text. The script cannot tell a decorative icon from
 *    body text, so it does not pretend to: it emits EVERY eligible family with that family's best match and
 *    leaves the choice to the human resolutions in MANUAL_RESOLUTIONS below.
 *
 * 3. Lightness is not the whole of a colour, and it is not the whole of a usage either. Every row carries an
 *    OKLab ΔE alongside ΔL, because a class can sit 0 lightness points from a token while shifting hue
 *    perceptibly. Rows are keyed by the class AS WRITTEN including its variant prefix, because
 *    `hover:bg-gray-50` and `bg-gray-50` want different tokens. And three token groups — canvas/app-shell,
 *    disabled, and the workflow node types — are excluded from automatic candidacy, because being the
 *    nearest token to something is not the same as being the right one.
 *
 * Usage:
 *   node scripts/derive-colour-mapping.mjs                 # print the markdown mapping table
 *   node scripts/derive-colour-mapping.mjs --check TARGET  # list remaining set A / set C classes under a
 *                                                          # directory or file; exits 1 when any remain
 */

import {readFileSync, readdirSync, statSync} from 'node:fs';
import {dirname, join, relative, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import colors from 'tailwindcss/colors';

const CLIENT_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const TOKEN_STYLESHEET_PATH = join(CLIENT_ROOT, 'src/styles/index.css');
const SOURCE_ROOT = join(CLIENT_ROOT, 'src');

/** Directories excluded from Phase 3: the canvas surfaces migrate on their own schedule. */
const EXCLUDED_DIRECTORY_NAMES = ['cluster-element-editor', 'node_modules', 'workflow-editor'];

/**
 * Directories excluded from Phase 3 by full path (relative to the client root), not by bare directory name.
 *
 * This rule covers vendored component sets generally, not just shadcn: any directory whose contents are
 * generated or copied from a package rather than authored in this codebase. This codebase never restyles a
 * vendored component directly — it wraps it in a composition component and does all styling there. A
 * hardcoded palette class inside a vendored directory is therefore never in scope for this migration: it
 * lives on an unreachable variant the wrappers never select, or, if reachable, it belongs to the upstream
 * package, not to this codebase's token system, and must be reverted rather than migrated.
 *
 * `src/components/ui` is the vendored shadcn primitives directory. This codebase wraps each primitive in a
 * composition component (`src/components/Button/Button.tsx`, `Badge/Badge.tsx`, `Select/Select.tsx`,
 * `Switch/Switch.tsx`, ...) and does all styling there, a rule `eslint-restricted-imports.mjs` enforces by
 * blocking direct `@/components/ui/*` imports outside the wrappers.
 *
 * `src/components/assistant-ui` is the vendored `@assistant-ui/react` component set, generated the same way
 * (see its files' `"use client"` headers and upstream formatting style, which differ from this repo's own
 * Prettier config). A step 2a migration commit restyled `attachment.tsx` directly instead of excluding the
 * directory, which silently turned a deliberate hover-suppression idiom (`hover:bg-white!`, same colour as
 * resting, `!important`) into a real hover-colour change (`hover:bg-surface-neutral-primary-hover!`) that
 * never existed in the original. That commit was reverted and the directory added here.
 *
 * A bare-name exclusion (adding `'ui'` to EXCLUDED_DIRECTORY_NAMES) was rejected even though only one
 * directory in `src` is currently named `ui`: `ui` is a generic, widely-reused folder name (feature-local
 * `ui/` subfolders are a common React convention), so excluding it by name would silently exempt any FUTURE
 * non-shadcn `ui/` directory anywhere in `src` from migration too, not just this one. A full-path exclusion
 * names exactly the directory the rule is about.
 */
const EXCLUDED_DIRECTORY_PATHS = ['src/components/ui', 'src/components/assistant-ui'];
const SOURCE_FILE_PATTERN = /\.(css|jsx?|mjs|tsx?)$/;
const EXCLUDED_FILE_PATTERN = /\.(stories|test)\./;

/** Only these three families are semantic theme tokens; the rest of `:root` is shadcn/legacy plumbing. */
const TOKEN_FAMILIES = ['content', 'stroke', 'surface'];
/**
 * A token whose name ends in one of these is a state variant, not a base token. Drives set B.
 *
 * `-focus` is deliberately absent: `--stroke-brand-focus` is the focus-ring colour in its own right, not a
 * focused derivative of another stroke token, and a focus ring is a legitimate migration target.
 */
const TOKEN_VARIANT_SUFFIXES = ['active', 'disabled', 'hover'];

/**
 * The subset of `TOKEN_VARIANT_SUFFIXES` that names an INTERACTION state rather than a disabled state.
 * Drives `findIdenticalContentStateTokens` below. `-disabled` is deliberately excluded: a disabled token
 * legitimately holding the same value across themes (or the same value as its resting token) is not the
 * defect this guard exists for — a disabled control genuinely can look the same regardless of theme.
 */
const STATE_VARIANT_SUFFIXES = ['active', 'hover'];

/**
 * Which token state a usage wants, keyed by the state carried in the class's own Tailwind variant prefix.
 *
 * `null` means "a resting token". `focus` maps to `hover` because there is no `-focus` state in the surface
 * or content families, and a focus-visible fill is the keyboard analogue of a hover fill — the one call site
 * that has both writes them on the same element (`hover:bg-gray-50 focus-visible:bg-gray-50`).
 */
const USAGE_STATE_TOKEN_SUFFIXES = {
    active: 'active',
    focus: 'hover',
    hover: 'hover',
};

/**
 * Which token families a class may legitimately target.
 *
 * `primary` is the family implied by the CSS property the class writes. `alternate` are the families the
 * class can still legitimately target when the ROLE differs from the property — a `bg-` on a decorative
 * shape is foreground (`content`), a `text-` on a large decorative icon is a rule (`stroke`). A match found
 * only in an alternate family is a cross-family match: set C.
 */
const CLASS_PREFIX_FAMILIES = {
    accent: {alternate: [], primary: 'surface'},
    bg: {alternate: ['content'], primary: 'surface'},
    border: {alternate: [], primary: 'stroke'},
    caret: {alternate: [], primary: 'content'},
    decoration: {alternate: [], primary: 'content'},
    divide: {alternate: [], primary: 'stroke'},
    fill: {alternate: ['stroke'], primary: 'content'},
    from: {alternate: ['content'], primary: 'surface'},
    outline: {alternate: [], primary: 'stroke'},
    placeholder: {alternate: [], primary: 'content'},
    ring: {alternate: [], primary: 'stroke'},
    shadow: {alternate: [], primary: 'stroke'},
    stroke: {alternate: ['content'], primary: 'stroke'},
    text: {alternate: ['stroke'], primary: 'content'},
    to: {alternate: ['content'], primary: 'surface'},
    via: {alternate: ['content'], primary: 'surface'},
};

/** Lightness deltas, in HSL lightness points, that separate an exact match from drift. */
const EXACT_LIGHTNESS_DELTA = 2;
const NEAR_LIGHTNESS_DELTA = 5;

/**
 * Lightness is not the whole of a colour. `exact` and `near` above are LIGHTNESS claims only, and a class can
 * sit 0 lightness points from its token while shifting hue by 18 degrees — `text-orange-800` does exactly
 * that. Every row therefore also carries an OKLab ΔE, which is a perceptual distance in all three dimensions.
 *
 * ~0.02 is the commonly cited just-noticeable difference in OKLab. Anything past twice that is a visible
 * light-mode change, which set A explicitly promises not to be, so those rows become **set F** rather than
 * staying in A/B/C behind a warning. A set a worker cannot accidentally scope into is stronger than a flag
 * they have to notice.
 */
const OKLAB_JND = 0.02;
const VISIBLE_SHIFT_DELTA_E = OKLAB_JND * 2;

/**
 * Sets a perceptually-visible row is promoted OUT of. D and E keep their letters:
 *
 * - D is blocked on "may we change light-mode lightness", F on "may we change light-mode hue". Separate
 *   questions that may get separate answers, so they stay separate sets rather than folding together.
 * - E is never migrated at all, so a perceptual distance to a token it will never take is meaningless.
 */
const PERCEPTUALLY_PROMOTABLE_SETS = ['A', 'B', 'C'];

/**
 * A nearest-by-lightness search across the whole token set would happily map `bg-green-100` onto a neutral
 * token that sits at the same lightness. Candidates are therefore gated on SEMANTIC GROUP first, and ranked
 * by lightness only within that gate.
 *
 * The gate is semantic rather than colorimetric on purpose. Hue/chroma gating breaks at both ends of the
 * lightness range: `--content-neutral-primary: 229 84% 5%` reads as 84% saturated but is visually near-black,
 * and `bg-blue-50` is a vivid hue at 1.4% OKLCh chroma. Both would be misfiled by any threshold that works
 * for the mid-range. The token names already carry the design intent, so the intent is what is matched.
 *
 * Each entry maps the token-name segment after the family prefix to its semantic group. Longest sensible
 * prefix wins; `loop` also covers `loop-break`, both being canvas node-type colours.
 */
const TOKEN_SEMANTIC_GROUPS = [
    {group: 'brand', prefix: 'brand'},
    {group: 'canvas', prefix: 'branch'},
    {group: 'canvas', prefix: 'canvas_dot'},
    {group: 'canvas', prefix: 'condition'},
    {group: 'canvas', prefix: 'each'},
    {group: 'canvas', prefix: 'fork-join'},
    {group: 'canvas', prefix: 'loop'},
    {group: 'canvas', prefix: 'map'},
    {group: 'canvas', prefix: 'parallel'},
    {group: 'canvas', prefix: 'popover-canvas'},
    {group: 'canvas', prefix: 'subflow'},
    {group: 'canvas', prefix: 'main'},
    {group: 'destructive', prefix: 'destructive'},
    {group: 'disabled', prefix: 'disabled'},
    {group: 'neutral', prefix: 'neutral'},
    {group: 'neutral', prefix: 'tooltip'},
    {group: 'onsurface', prefix: 'onsurface'},
    {group: 'success', prefix: 'success'},
    {group: 'warning', prefix: 'onwarning'},
    {group: 'warning', prefix: 'warning'},
];

/**
 * Groups that are never an automatic target. All three exclusions exist because lightness proximity to these
 * tokens is a trap, not a signal.
 *
 * `canvas` covers two things. The workflow node-type tokens (`--stroke-branch-*`, `--stroke-loop-*`, and
 * siblings) encode which kind of node an edge belongs to, not a UI chrome role. It also covers the
 * APP-SHELL backdrops, `--surface-main` and `--surface-popover-canvas`: in light mode `--surface-main` is
 * `210 40% 98%`, the nearest token to `gray-50` by 0.1 lightness points, but **in dark mode it is
 * `229 84% 5%`, byte-identical to `--surface-neutral-primary`, the card colour**. Anything that is not a page
 * background — a `hover:` fill on a list row, an inset tile inside a card — renders the same colour as its
 * own container once the theme flips, and hover feedback and panel separation disappear. A genuine page
 * background needs a manual resolution naming `--surface-main` explicitly.
 *
 * `disabled` tokens are STATE tokens: `--content-disabled: 0 0% 64%` sits 0.3 lightness points from
 * `gray-400`, closer than the correct `--content-neutral-tertiary`, so a pure nearest-by-lightness search
 * maps every muted grey onto "this element is disabled". It is only a correct target when the call site
 * really is a disabled state, which is a manual call.
 */
const NON_CANDIDATE_TOKEN_GROUPS = ['canvas', 'disabled', 'other'];

/** Which semantic group each Tailwind palette belongs to. `null` means the palette has no token counterpart. */
const PALETTE_SEMANTIC_GROUPS = {
    amber: 'warning',
    black: 'neutral',
    blue: 'brand',
    cyan: null,
    emerald: 'success',
    fuchsia: null,
    gray: 'neutral',
    green: 'success',
    indigo: 'brand',
    lime: 'success',
    mauve: null,
    mist: null,
    neutral: 'neutral',
    olive: null,
    orange: 'warning',
    pink: null,
    purple: null,
    red: 'destructive',
    rose: 'destructive',
    sky: 'brand',
    slate: 'neutral',
    stone: 'neutral',
    taupe: null,
    teal: 'success',
    violet: null,
    white: 'neutral',
    yellow: 'warning',
    zinc: 'neutral',
};

/** Token groups a palette group may target. Neutrals also reach the on-surface foregrounds. */
const CANDIDATE_TOKEN_GROUPS = {
    brand: ['brand'],
    destructive: ['destructive'],
    neutral: ['neutral', 'onsurface'],
    success: ['success'],
    warning: ['warning'],
};

/**
 * Hand resolutions, read off the actual call sites. Everything here overrides the derived set.
 *
 * `set` E means never migrate. `family` forces which family the class resolves against, which is how the
 * cross-family cases are settled. `reason` is mandatory: a later reader must be able to tell why
 * `bg-gray-400` lands on a content token without re-deriving it.
 *
 * `crossFamilyNote` records the opposite decision — a class whose alternate family matches cleanly but whose
 * ROLE does not justify crossing. Every class with a clean alternate needs one of the two, or it shows up as
 * UNRESOLVED in the generated table.
 *
 * `token` names a target outright, overriding the ranking. Needed when correctness depends on what the
 * element sits ON, which no colour metric can see: an inset tile must be a DIFFERENT surface from its
 * container, so the nearest token by lightness is exactly the wrong answer when the nearest token is the
 * container's own.
 *
 * `bg-gray-50` and `text-gray-300` are gone as of step 2f: their last in-scope occurrences (the ee
 * inset tiles / large empty-state icons under `src/ee/**`) were migrated to `--surface-neutral-secondary`
 * and `--stroke-neutral-tertiary` respectively, and every other occurrence of either class lives inside
 * `workflow-editor` or `cluster-element-editor`, both excluded from `collectPaletteClasses`. With no
 * remaining row for either written class, the two entries would trip `findStaleResolutions` on every
 * future invocation, so they are removed rather than left to redirect nothing.
 *
 * `bg-black/50` is gone as of the shadcn-revert task: its only three occurrences were the overlay scrim in
 * `components/ui/alert-dialog.tsx` / `dialog.tsx` / `sheet.tsx`, and that directory is now excluded wholesale
 * (see `EXCLUDED_DIRECTORY_PATHS`) rather than migrated site by site. With no remaining row for the written
 * class, the entry would trip `findStaleResolutions` on every future invocation, so it is removed rather
 * than left to redirect nothing. `bg-black` and `bg-black/80` are unaffected — their sole occurrences
 * (`VoiceModeLayout.tsx`, `DialogLoader.tsx`) sit outside `components/ui`.
 *
 * `active:text-blue-700`, `hover:text-blue-700`, `dark:text-blue-300`, `text-green-900` and `text-red-900`
 * are gone as of Task 4 (the 86 coupled occurrences): their only in-scope call sites
 * (`AiSkillDetail.tsx:69`'s hover/active tab treatment, `AiHubPanel.tsx`'s workflow-chat badge,
 * `format.ts`'s experiment status map, and `AiPromptDetail.tsx`'s diff-view added/removed rows) were
 * migrated because each sat under an already-migrated `bg-surface-*` token. With no remaining row for
 * any of the five written classes, the entries would trip `findStaleResolutions` on every future
 * invocation, so they are removed rather than left to redirect nothing.
 *
 * `[&_svg]:text-black` is gone as of the assistant-ui-revert task: its sole occurrence was
 * `components/assistant-ui/attachment.tsx`, and that directory is now excluded wholesale (see
 * `EXCLUDED_DIRECTORY_PATHS`) for the same vendored-component reason `components/ui` already was. With no
 * remaining row for the written class, the entry would trip `findStaleResolutions` on every future
 * invocation, so it is removed rather than left to redirect nothing.
 *
 * `hover:text-blue-600`, `text-blue-500`, `text-blue-600` and `text-emerald-700` are gone as of the
 * forced-text-literal migration (dark-mode Phase 3): their only in-scope call sites (`RightSidebar.tsx`'s
 * nav hover, the `ClockIcon` status glyphs in the approval-task views, the link/icon text in
 * `ConnectionDialog.tsx`/`RightSidebar.tsx`/`ApprovalTaskFilters.tsx`/`ApprovalTaskCreateDialog.tsx`/
 * `ContextStoreSourceDetailDialog.tsx`/`KnowledgeBaseSearchInterface.tsx`/`AiSkillUploadDialog.tsx`/
 * `AiEvalRules.tsx`/`AiPromptDetail.tsx`, and `AiDatasets.tsx`'s inline "open" label) were migrated because
 * each was bare text or a bare icon glyph sitting on a container that had already migrated to a
 * `--surface-*` token. With no remaining row for any of the four written classes, the entries would trip
 * `findStaleResolutions` on every future invocation, so they are removed rather than left to redirect
 * nothing.
 */
const MANUAL_RESOLUTIONS = {
    'bg-black': {
        reason: '`VoiceModeLayout.tsx:114` — `bg-black text-white` is a deliberately dark stage for voice mode. It is dark in both themes by design, so it must not follow the active theme.',
        set: 'E',
    },
    'bg-black/80': {
        reason: '`DialogLoader.tsx` overlay scrim. Same reasoning as `bg-black/50`.',
        set: 'E',
    },
    'bg-blue-500': {
        reason: 'Both sites are identity colours: one entry of the `SKILL_COLORS` avatar palette in `getSkillColor.ts`, and the `Generation` span type in `SpanWaterfall.tsx`. Sibling entries are purple/pink/teal, which have no tokens at all — theming one member of a categorical palette would break the set.',
        set: 'E',
    },
    'bg-emerald-400': {
        reason: '`VoiceModeLayout.tsx` voice-orb ring on the deliberately dark stage. Part of the same un-themed surface as `bg-black`.',
        set: 'E',
    },
    'bg-emerald-600': {
        reason: '`VoiceModeLayout.tsx` voice-orb ring and connect button on the deliberately dark stage.',
        set: 'E',
    },
    'bg-gray-400': {
        crossFamilyNote:
            'ACCEPTED — see set C. A `size-4 rounded-full` loading dot is a foreground mark that happens to be painted with background-color.',
        family: 'content',
        reason: '12 of 14 are `size-4 rounded-full` loading dots (`LoadingDots.tsx`, `DialogLoader.tsx`, `LazyLoadWrapper.tsx`). The class writes background-color, but a loading dot IS the foreground mark, so its role is content and it takes `--content-neutral-tertiary` — an exact match a same-family rule would have reported as 20 points of drift. SITE EXCEPTION: the 2 occurrences in `SpanWaterfall.tsx` are the `Span` entry of SPAN_TYPE_COLORS, a categorical identity colour; leave those literal.',
        set: 'C',
    },
    'bg-gray-600': {
        reason: '`VoiceModeLayout.tsx` idle voice-orb ring on the deliberately dark stage.',
        set: 'E',
    },
    'bg-orange-500': {
        reason: 'Identity colours only: a `SKILL_COLORS` avatar entry and the `ToolCall` span type in `SpanWaterfall.tsx`.',
        set: 'E',
    },
    'bg-slate-100': {
        reason: '`AiHubChatComposer.tsx` — the `task` kind badge. One member of a categorical badge palette whose siblings have no token counterpart.',
        set: 'E',
    },
    'bg-slate-200': {
        reason: '`Appearance.tsx` theme preview swatch (the Light card). Preview swatches must stay literal, otherwise every card renders in the active theme and the picker shows the same thing three times.',
        set: 'E',
    },
    'bg-slate-400': {
        reason: '`Appearance.tsx` theme preview swatch — all 12 are the skeleton bars and dots inside the Dark card. Must stay literal so the Dark preview stays dark while the app is in light mode.',
        set: 'E',
    },
    'bg-slate-800': {
        reason: '`Appearance.tsx` theme preview swatch (the Dark card panels). Must stay literal.',
        set: 'E',
    },
    'bg-slate-950': {
        reason: '`Appearance.tsx` theme preview swatch (the Dark card backdrop). Must stay literal.',
        set: 'E',
    },
    'bg-teal-500': {
        reason: 'A `SKILL_COLORS` avatar entry in `getSkillColor.ts`. Categorical identity colour.',
        set: 'E',
    },
    'bg-yellow-500': {
        crossFamilyNote:
            'Rejected. `AiGatewayDashboard.tsx` budget progress-bar fill — a filled region, which is exactly what the surface family is for. The content match is a lightness coincidence.',
    },
    'border-emerald-500/40': {
        reason: '`VoiceModeLayout.tsx` status-chip border on the deliberately dark stage.',
        set: 'E',
    },
    'dark:text-black': {
        reason: 'A `@apply dark:text-black` rule in the vendored `CreatableSelect.css`. Already a hand-rolled dark override on third-party markup; it belongs to whoever owns that vendored widget.',
        set: 'E',
    },
    'hover:bg-emerald-500': {
        reason: '`VoiceModeLayout.tsx` connect-button hover on the deliberately dark stage.',
        set: 'E',
    },
    'text-blue-700': {
        crossFamilyNote:
            'Rejected. Text inside `bg-blue-50` alerts and the selected CadencePicker card. Body text on a filled chip.',
    },
    'text-emerald-400': {
        reason: '`VoiceModeLayout.tsx` status-chip label on the deliberately dark stage.',
        set: 'E',
    },
    'text-emerald-900': {
        crossFamilyNote: 'Rejected. Badge text on a `bg-emerald-100` chip in the gateway experiment/dataset views.',
    },
    'text-green-800': {
        crossFamilyNote:
            'Rejected. Status and HTTP-method badge text on a `bg-green-100` chip (`SyncSourceStatusBadge.tsx`, `httpMethod-utils.ts`, `endpointEditor-utils.ts`). Badge text is text. The only clean stroke match is `--stroke-success-primary-active`, a pressed-border state, so crossing here would be wrong on both axes.',
    },
    'text-orange-700': {
        crossFamilyNote:
            'Rejected. HTTP-method labels in the API-connector and API-collection endpoint lists. Text, and the clean stroke match is a hover state.',
    },
    'text-red-800': {
        crossFamilyNote: 'Rejected. Status and method badge text on a `bg-red-100` chip. Same shape as text-green-800.',
    },
    'text-slate-700': {
        reason: '`AiHubChatComposer.tsx` — the `task` kind badge label, paired with `bg-slate-100`.',
        set: 'E',
    },
    'text-white': {
        family: 'content',
        reason: 'Foreground on a filled surface; `--content-onsurface-primary` is the token minted for exactly this. SITE EXCEPTION: the 3 occurrences in `VoiceModeLayout.tsx` sit on the deliberately dark stage and stay literal with the rest of that file.',
        set: 'A',
    },
    'text-yellow-600': {
        crossFamilyNote:
            'Rejected. Log-level badge text on a `bg-yellow-100` chip in `WorkflowExecutionLogsContent.tsx`.',
    },
};

/**
 * Files whose ENTIRE colour scheme is a deliberate exception, whatever the individual classes derive to.
 *
 * These are why a row's set is not the last word: `bg-white` is a clean set A everywhere except inside the
 * Appearance theme previews, where it is one of the literal swatches. `--check` skips these files so a later
 * task's grep assertion does not demand a migration that must never happen.
 */
const EXCEPTION_FILES = [
    {
        path: 'src/pages/account/settings/Appearance.tsx',
        reason: 'Theme preview swatches. Every colour in the Light/Dark/System cards must stay literal, or all three cards render in the active theme and the picker shows the same thing three times.',
    },
    {
        path: 'src/pages/automation/ai/gateway/components/traces/SpanWaterfall.tsx',
        reason: 'SPAN_TYPE_COLORS and ERROR_COLOR — categorical identity colours for span types.',
    },
    {
        path: 'src/pages/automation/ai/skills/utils/getSkillColor.ts',
        reason: 'SKILL_COLORS — the categorical avatar palette. Its members are blue/green/purple/orange/pink/teal; theming the ones that happen to have tokens would break the set.',
    },
    {
        path: 'src/shared/lib/voice/VoiceModeLayout.tsx',
        reason: 'A deliberately dark voice-mode stage (`bg-black text-white`) that is dark in both themes by design.',
    },
];

/**
 * Occurrences of a class that resolve differently from the rest of their row, pinned by call site.
 *
 * This is the mirror image of keying rows by the written class. That split is right — `hover:bg-gray-50` and
 * `bg-gray-50` usually want different tokens — but it assumes a `hover:` class expresses a hover CHANGE.
 * Sometimes it expresses hover SUPPRESSION: a zebra-striped row writes `bg-gray-50 hover:bg-gray-50` over a
 * base `hover:bg-transparent` precisely so the stripe does NOT move on hover. Send that pair to two tokens
 * and the striped rows start changing on hover in dark mode — and changing darker, while unstriped rows go
 * lighter. So here two differently-written classes must land on the same token.
 *
 * Each site carries an `expect` string that must appear on the named line, so a later edit that shifts the
 * file fails the run instead of silently pinning the wrong occurrence.
 *
 * Empty as of step 2c: its only two sites — `OrganizationConnectionsTable.tsx:36` and
 * `NotificationsTable.tsx:34` — were migrated to `bg-surface-neutral-secondary` /
 * `hover:bg-surface-neutral-secondary` (the same token on both, preserving the hover-suppression this
 * resolution existed to pin), so there is no longer a raw `bg-gray-50`/`hover:bg-gray-50` pair at either
 * literal site for this entry to redirect. Leaving the old `expect` strings in place would permanently trip
 * `findDriftedLineContracts` for every future invocation, in every directory, since that check runs
 * unconditionally ahead of `--check`.
 */
const SITE_RESOLUTIONS = [];

/**
 * Line ranges whose colours are a deliberate exception inside a file that is otherwise ordinary.
 *
 * The hazard these catch is the one `bg-gray-400` made visible: a categorical palette declared as an object
 * literal, where most members happen to have semantic-token counterparts and one or two do not. Theming the
 * members that match and leaving the rest literal breaks the set — which is the very reason the unmatched
 * member was filed as an exception. A whole-file exception would be too blunt here; both files carry ordinary
 * chrome outside these ranges.
 *
 * Absolute line numbers are a contract with a file that later tasks will edit, so every range carries
 * `expect` strings that must all appear inside it. An insertion above a range would otherwise shift it
 * silently — un-excepting a categorical map, or excepting ordinary chrome — with nothing to notice. Ranges
 * deliberately reach up to include the `const` declaration, which is the most stable anchor available.
 */
const EXCEPTION_SITES = [
    {
        path: 'src/pages/automation/ai-hub/composer/AiHubChatComposer.tsx',
        ranges: [
            {
                end: 51,
                expect: [
                    'const KIND_BADGE_CLASSES',
                    "apiCollection: 'bg-pink-100",
                    "workflowExecution: 'bg-orange-100",
                ],
                start: 43,
            },
        ],
        reason: 'KIND_BADGE_CLASSES — the resource-kind badge palette. Its 8 members are pink/green/blue/purple/cyan/slate/amber/orange; `bg-slate-100 text-slate-700` and the pink/purple/cyan members have no token counterpart, so theming the other four would leave a half-themed set. The ordinary `bg-red-100` and `bg-blue-50` badges elsewhere in this file are NOT covered.',
    },
    {
        path: 'src/pages/automation/ai/gateway/components/traces/AiObservabilityTraceDetail.tsx',
        ranges: [
            {
                end: 49,
                expect: ['const SPAN_TYPE_CLASSES', 'const TRACE_STATUS_CLASSES', "ERROR: 'bg-red-100 text-red-800'"],
                start: 38,
            },
            {end: 120, expect: ['SPAN_TYPE_CLASSES[span.type]'], start: 120},
            {end: 142, expect: ["span.status === 'COMPLETED'", "'bg-blue-100 text-blue-800'"], start: 138},
            {end: 505, expect: ['TRACE_STATUS_CLASSES[trace.status]'], start: 505},
        ],
        reason: "SPAN_TYPE_CLASSES and TRACE_STATUS_CLASSES, plus the inline `|| 'bg-gray-100 text-gray-800'` fallbacks and the status ternary that repeat the same strings. This is the same span-type semantic that makes `SpanWaterfall.tsx` a whole-file exception. The `hover:bg-green-50` / `hover:bg-red-50` buttons elsewhere in this file are NOT covered.",
    },
    {
        path: 'src/ee/pages/settings/platform/api-connectors/utils/httpMethod-utils.ts',
        ranges: [
            {
                end: 22,
                expect: [
                    'getHttpMethodBadgeColor',
                    'case HttpMethod.Patch:',
                    "return 'text-orange-700 dark:text-orange-400';",
                ],
                start: 7,
            },
        ],
        reason: "getHttpMethodBadgeColor's HTTP-method identity map. GET/POST/PUT/DELETE already read semantic tokens; PATCH's `text-orange-700` is the one member with no clean warning-family slot distinct from PUT's `--content-warning-primary` — migrating it collapsed PUT and PATCH onto the same colour, a distinction the badges draw today. Left literal so the five methods stay five colours, but `dark:text-orange-400` was added so PATCH isn't a half-themed member of an otherwise dark-aware set (Task 5's KIND_BADGE_CLASSES/SPAN_TYPE_CLASSES precedent). The sibling `getHttpMethodPillColor` (PATCH already `bg-surface-warning-secondary text-content-warning-primary`, a filled pill rather than a bare identity colour) is NOT covered.",
    },
    {
        path: 'src/ee/pages/settings/platform/api-connectors/hooks/useApiConnectorEndpointListItem.ts',
        ranges: [
            {
                end: 63,
                expect: [
                    'const httpMethodStyles',
                    "case 'PATCH':",
                    "textColor: 'text-orange-700 dark:text-orange-400',",
                ],
                start: 29,
            },
        ],
        reason: "Same HTTP-method identity map as httpMethod-utils.ts's getHttpMethodBadgeColor (this hook is its per-endpoint-list-item counterpart), same PUT/PATCH collision if PATCH migrated; same `dark:text-orange-400` completion applied here too.",
    },
    {
        path: 'src/ee/pages/automation/api-platform/api-collections/components/ApiCollectionEndpointListItem.tsx',
        ranges: [
            {
                end: 140,
                expect: [
                    'const httpMethodStyles',
                    "case 'PATCH':",
                    "textColor: 'text-orange-700 dark:text-orange-400',",
                ],
                start: 106,
            },
        ],
        reason: 'Same HTTP-method identity map, third copy (API Collections endpoint list). Same PUT/PATCH collision if PATCH migrated; same `dark:text-orange-400` completion applied here too.',
    },
    {
        path: 'src/pages/automation/connections/components/ConnectionScopeBadge.tsx',
        ranges: [
            {
                end: 35,
                expect: [
                    "const VISIBILITY_CONFIG",
                    "className: 'text-purple-500 dark:text-purple-400'",
                    "className: 'text-gray-500 dark:text-gray-400'",
                    "className: 'text-green-500 dark:text-green-400'",
                ],
                start: 13,
            },
        ],
        reason: "VISIBILITY_CONFIG's three-member connection-scope identity map. ORGANIZATION's `text-purple-500` has no token counterpart (set E) and must stay literal; migrating PRIVATE/WORKSPACE alone would leave the set half-themed — in light mode the un-migrated purple sits roughly 30 lightness points brighter than its now-tokenised siblings, which is the same half-themed-set hazard KIND_BADGE_CLASSES and SPAN_TYPE_CLASSES exist to prevent. All three members left literal.",
    },
];

/**
 * Palettes with no semantic-token counterpart (purple, cyan, pink, ...) are identity colours: resource
 * badges, skill avatars, span types. The hue is the meaning, so there is nothing to migrate them to.
 */
const CATEGORICAL_IDENTITY_REASON =
    'Categorical identity colour (resource badge / skill avatar / span type). The hue carries the meaning and has no semantic-token counterpart, so it is not a themeable neutral.';

function toTitleCase(text) {
    return text.charAt(0).toUpperCase() + text.slice(1);
}

/** Converts a single gamma-encoded sRGB triple in 0..1 to HSL with hue in degrees and s/l in percent. */
function srgbToHsl(red, green, blue) {
    const maximum = Math.max(red, green, blue);
    const minimum = Math.min(red, green, blue);
    const lightness = (maximum + minimum) / 2;

    if (maximum === minimum) {
        return [0, 0, lightness * 100];
    }

    const chroma = maximum - minimum;
    const saturation = lightness > 0.5 ? chroma / (2 - maximum - minimum) : chroma / (maximum + minimum);

    let hue;

    if (maximum === red) {
        hue = (green - blue) / chroma + (green < blue ? 6 : 0);
    } else if (maximum === green) {
        hue = (blue - red) / chroma + 2;
    } else {
        hue = (red - green) / chroma + 4;
    }

    return [hue * 60, saturation * 100, lightness * 100];
}

function gammaEncode(linearChannel) {
    const clamped = Math.min(1, Math.max(0, linearChannel));

    return clamped <= 0.0031308 ? 12.92 * clamped : 1.055 * clamped ** (1 / 2.4) - 0.055;
}

/**
 * OKLCH -> OKLab -> LMS -> linear sRGB -> gamma-encoded sRGB -> HSL.
 *
 * Matrices are Bjorn Ottosson's published OKLab constants. The LMS step is where the cube lives: the OKLab
 * -> LMS matrix produces cube roots, which are cubed back to cone responses before the sRGB matrix.
 */
function oklchToHsl(oklchText) {
    const match = /^oklch\(\s*([\d.]+)(%?)\s+([\d.]+)\s+([\d.]+)\s*\)$/.exec(oklchText.trim());

    if (!match) {
        return null;
    }

    const [, lightnessText, percentSign, chromaText, hueText] = match;
    const lightness = Number.parseFloat(lightnessText) / (percentSign === '%' ? 100 : 1);
    const chroma = Number.parseFloat(chromaText);
    const hueRadians = (Number.parseFloat(hueText) * Math.PI) / 180;

    const aAxis = chroma * Math.cos(hueRadians);
    const bAxis = chroma * Math.sin(hueRadians);

    const longCubeRoot = lightness + 0.3963377774 * aAxis + 0.2158037573 * bAxis;
    const mediumCubeRoot = lightness - 0.1055613458 * aAxis - 0.0638541728 * bAxis;
    const shortCubeRoot = lightness - 0.0894841775 * aAxis - 1.291485548 * bAxis;

    const long = longCubeRoot ** 3;
    const medium = mediumCubeRoot ** 3;
    const short = shortCubeRoot ** 3;

    const linearRed = 4.0767416621 * long - 3.3077115913 * medium + 0.2309699292 * short;
    const linearGreen = -1.2684380046 * long + 2.6097574011 * medium - 0.3413193965 * short;
    const linearBlue = -0.0041960863 * long - 0.7034186147 * medium + 1.707614701 * short;

    return srgbToHsl(gammaEncode(linearRed), gammaEncode(linearGreen), gammaEncode(linearBlue));
}

function hslToSrgb(hsl) {
    const [hue, saturationPercent, lightnessPercent] = hsl;
    const saturation = saturationPercent / 100;
    const lightness = lightnessPercent / 100;
    const chroma = (1 - Math.abs(2 * lightness - 1)) * saturation;
    const huePrime = (((hue % 360) + 360) % 360) / 60;
    const secondComponent = chroma * (1 - Math.abs((huePrime % 2) - 1));
    const offset = lightness - chroma / 2;
    const sector = Math.floor(huePrime) % 6;
    const sectorChannels = [
        [chroma, secondComponent, 0],
        [secondComponent, chroma, 0],
        [0, chroma, secondComponent],
        [0, secondComponent, chroma],
        [secondComponent, 0, chroma],
        [chroma, 0, secondComponent],
    ][sector];

    return sectorChannels.map((channel) => channel + offset);
}

/** Gamma-encoded sRGB (0..1) to OKLab. The forward direction of the same chain `oklchToHsl` runs backwards. */
function srgbToOklab([red, green, blue]) {
    const linearize = (channel) => (channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4);
    const linearRed = linearize(red);
    const linearGreen = linearize(green);
    const linearBlue = linearize(blue);

    const longCubeRoot = Math.cbrt(0.4122214708 * linearRed + 0.5363325363 * linearGreen + 0.0514459929 * linearBlue);
    const mediumCubeRoot = Math.cbrt(0.2119034982 * linearRed + 0.6806995451 * linearGreen + 0.1073969566 * linearBlue);
    const shortCubeRoot = Math.cbrt(0.0883024619 * linearRed + 0.2817188376 * linearGreen + 0.6299787005 * linearBlue);

    return [
        0.2104542553 * longCubeRoot + 0.793617785 * mediumCubeRoot - 0.0040720468 * shortCubeRoot,
        1.9779984951 * longCubeRoot - 2.428592205 * mediumCubeRoot + 0.4505937099 * shortCubeRoot,
        0.0259040371 * longCubeRoot + 0.7827717662 * mediumCubeRoot - 0.808675766 * shortCubeRoot,
    ];
}

/** Perceptual distance between two HSL triples, in OKLab. Compare against OKLAB_JND. */
function oklabDistance(firstHsl, secondHsl) {
    const first = srgbToOklab(hslToSrgb(firstHsl));
    const second = srgbToOklab(hslToSrgb(secondHsl));

    return Math.hypot(first[0] - second[0], first[1] - second[1], first[2] - second[2]);
}

/**
 * Splits an OKLab difference into its lightness, chroma and hue components, so a row can say WHICH axis
 * moves rather than only how far.
 *
 * This is not cosmetic. HSL lightness understates perceptual lightness at high chroma: `text-green-600` sits
 * only 3.6 HSL lightness points from `--content-success-primary` and has identical HSL hue, which makes it
 * look like a saturation-only change — but its OKLab difference is 0.108 lightness against 0.050 chroma. The
 * dominant axis is perceptual lightness. Reading the axis off the HSL columns gets this row wrong.
 */
function decomposeOklabDifference(firstHsl, secondHsl) {
    const first = srgbToOklab(hslToSrgb(firstHsl));
    const second = srgbToOklab(hslToSrgb(secondHsl));
    const firstChroma = Math.hypot(first[1], first[2]);
    const secondChroma = Math.hypot(second[1], second[2]);
    const chromaDifference = Math.abs(firstChroma - secondChroma);
    const planeDistance = Math.hypot(first[1] - second[1], first[2] - second[2]);

    return {
        chroma: chromaDifference,
        hue: Math.sqrt(Math.max(0, planeDistance ** 2 - chromaDifference ** 2)),
        lightness: Math.abs(first[0] - second[0]),
    };
}

function dominantAxisOf(firstHsl, secondHsl) {
    const components = decomposeOklabDifference(firstHsl, secondHsl);

    return Object.keys(components).reduce((strongest, axis) =>
        components[axis] > components[strongest] ? axis : strongest
    );
}

function hexToHsl(hexText) {
    const match = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i.exec(hexText.trim());

    if (!match) {
        return null;
    }

    const digits = match[1].length === 3 ? [...match[1]].map((digit) => digit + digit).join('') : match[1];
    const red = Number.parseInt(digits.slice(0, 2), 16) / 255;
    const green = Number.parseInt(digits.slice(2, 4), 16) / 255;
    const blue = Number.parseInt(digits.slice(4, 6), 16) / 255;

    return srgbToHsl(red, green, blue);
}

function paletteValueToHsl(paletteValue) {
    if (typeof paletteValue !== 'string') {
        return null;
    }

    return paletteValue.startsWith('oklch(') ? oklchToHsl(paletteValue) : hexToHsl(paletteValue);
}

/**
 * Extracts a top-level rule's declaration block by brace matching, so a nested rule inside `@layer base`
 * does not truncate it at the first closing brace.
 *
 * Used for both `:root` and `.dark`: `readTokens` needs the dark block alongside the root block to tell
 * whether a token's dark value is byte-identical to its light one (see `findIdenticalContentStateTokens`).
 */
function extractStylesheetBlock(stylesheetText, selector) {
    const startIndex = stylesheetText.indexOf(selector);

    if (startIndex === -1) {
        throw new Error(`No "${selector}" block found in ${TOKEN_STYLESHEET_PATH}`);
    }

    const openIndex = stylesheetText.indexOf('{', startIndex);
    let depth = 0;

    for (let index = openIndex; index < stylesheetText.length; index += 1) {
        if (stylesheetText[index] === '{') {
            depth += 1;
        } else if (stylesheetText[index] === '}') {
            depth -= 1;

            if (depth === 0) {
                return stylesheetText.slice(openIndex + 1, index);
            }
        }
    }

    throw new Error(`Unterminated "${selector}" block in ${TOKEN_STYLESHEET_PATH}`);
}

/**
 * Parses `H S% L%` and `H, S%, L%` — both forms exist in index.css — and takes the first three components
 * of an alpha-carrying value such as `0 74% 42% / 50%`.
 */
function parseTokenHsl(declarationValue) {
    const [colourPart, alphaPart] = declarationValue.split('/');
    const components = colourPart
        .trim()
        .split(/[,\s]+/)
        .filter((component) => component.length > 0);

    if (components.length < 3) {
        return null;
    }

    const numbers = components.slice(0, 3).map((component) => Number.parseFloat(component));

    if (numbers.some((number) => Number.isNaN(number))) {
        return null;
    }

    return {alpha: alphaPart === undefined ? null : alphaPart.trim(), hsl: numbers};
}

/** Every declaration in a block, keyed by full `--token-name`, parsed the same way `readTokens` parses `:root`. */
function readBlockDeclarations(blockText) {
    const declarationsByName = new Map();

    for (const match of blockText.matchAll(/--([a-z0-9_-]+)\s*:\s*([^;]+);/gi)) {
        const [, tokenName, declarationValue] = match;
        const parsed = parseTokenHsl(declarationValue);

        if (parsed !== null) {
            declarationsByName.set(`--${tokenName}`, parsed);
        }
    }

    return declarationsByName;
}

/** `true` when a token's `.dark` declaration is byte-identical to its `:root` one — see `STATE_VARIANT_SUFFIXES`. */
function isIdenticalAcrossThemes(rootDeclaration, darkDeclaration) {
    return (
        darkDeclaration !== undefined &&
        darkDeclaration.alpha === rootDeclaration.alpha &&
        darkDeclaration.hsl.every((component, index) => component === rootDeclaration.hsl[index])
    );
}

function readTokens() {
    const stylesheetText = readFileSync(TOKEN_STYLESHEET_PATH, 'utf8');
    const rootBlock = extractStylesheetBlock(stylesheetText, ':root');
    const darkDeclarationsByName = readBlockDeclarations(extractStylesheetBlock(stylesheetText, '.dark'));
    const tokens = [];

    for (const match of rootBlock.matchAll(/--([a-z0-9_-]+)\s*:\s*([^;]+);/gi)) {
        const [, tokenName, declarationValue] = match;
        const family = tokenName.split('-')[0];

        if (!TOKEN_FAMILIES.includes(family)) {
            continue;
        }

        const parsed = parseTokenHsl(declarationValue);

        if (parsed === null) {
            continue;
        }

        if (parsed.alpha === '0%') {
            continue;
        }

        const lastSegment = tokenName.split('-').pop();
        const remainder = tokenName.slice(family.length + 1);
        const semanticGroup = TOKEN_SEMANTIC_GROUPS.find((entry) => remainder.startsWith(entry.prefix));
        const name = `--${tokenName}`;
        const isStateVariant = family === 'content' && STATE_VARIANT_SUFFIXES.includes(lastSegment);

        tokens.push({
            alpha: parsed.alpha,
            family,
            group: semanticGroup === undefined ? 'other' : semanticGroup.group,
            hsl: parsed.hsl,
            isIdenticalInDarkMode: isStateVariant && isIdenticalAcrossThemes(parsed, darkDeclarationsByName.get(name)),
            isVariant: TOKEN_VARIANT_SUFFIXES.includes(lastSegment),
            name,
        });
    }

    return tokens;
}

/**
 * A content-family `-hover`/`-active` token whose `:root` and `.dark` declarations are byte-identical
 * cannot express its state in dark mode at all — hovering or pressing renders exactly like resting. This
 * is the defect `--content-brand-primary-hover` and `--content-brand-primary-active` shipped with: it
 * silently reproduced the same dark-mode contrast failure (1.62:1 against `--surface-brand-secondary`) at
 * two separate call sites (`format.ts`, then `KnowledgeBaseDocumentChunkListSelectionBar.tsx`) before
 * either was traced back to the token itself rather than fixed call site by call site. `bestTokenInFamily`
 * and `namedTokenCandidate` both refuse these tokens as migration targets; this is the loud, unconditional
 * form of that refusal, so the defect fails the whole run instead of only disqualifying a candidate a
 * human might not notice went missing.
 */
function findIdenticalContentStateTokens(tokens) {
    return tokens.filter((token) => token.isIdenticalInDarkMode);
}

function listSourceFiles(directoryPath) {
    const filePaths = [];

    for (const entryName of readdirSync(directoryPath)) {
        const entryPath = join(directoryPath, entryName);

        if (statSync(entryPath).isDirectory()) {
            const isExcludedByPath = EXCLUDED_DIRECTORY_PATHS.includes(relative(CLIENT_ROOT, entryPath));

            if (!EXCLUDED_DIRECTORY_NAMES.includes(entryName) && !isExcludedByPath) {
                filePaths.push(...listSourceFiles(entryPath));
            }
        } else if (SOURCE_FILE_PATTERN.test(entryName) && !EXCLUDED_FILE_PATTERN.test(entryName)) {
            filePaths.push(entryPath);
        }
    }

    return filePaths;
}

/**
 * Matches a palette utility together with any Tailwind variant prefix it carries, so `hover:bg-gray-50` and
 * `bg-gray-50` are distinguishable. They are two different usages of one colour and they want different
 * tokens; collapsing them hides that a resting token is standing in for a hover fill.
 *
 * The leading lookbehind is what forces the whole `hover:` to be consumed. Without it the scan also matches
 * from `over:bg-gray-50`, since `over` is a syntactically valid variant name.
 */
function buildPaletteClassPattern() {
    const paletteNames = Object.keys(colors).filter((colorName) => typeof colors[colorName] === 'object');
    const prefixes = Object.keys(CLASS_PREFIX_FAMILIES);
    const shadedForm = `(?:${paletteNames.join('|')})-(?:50|\\d{3})`;
    const variantForm = '(?:(?:[a-z0-9-]+(?:\\[[^\\]\\s]+\\])?|\\[[^\\]\\s]+\\]):)*';

    return new RegExp(
        `(?<![\\w:.-])(${variantForm})(${prefixes.join('|')})-(${shadedForm}|black|white)(?:\\/(\\d{1,3}))?\\b`,
        'g'
    );
}

/**
 * The interaction state a usage is written for, read off its variant prefix.
 *
 * `dark:` is tracked separately because it is not a state at all — a `dark:` palette literal is a hand-rolled
 * theme override, and migrating its base class means DELETING it, not remapping it.
 */
function usageStateOf(variantPrefix) {
    const segments = variantPrefix.split(':').filter((segment) => segment.length > 0);
    const hasSegmentEndingIn = (suffix) => segments.some((segment) => segment.endsWith(suffix));

    let state = null;

    if (hasSegmentEndingIn('hover')) {
        state = 'hover';
    } else if (hasSegmentEndingIn('active')) {
        state = 'active';
    } else if (segments.some((segment) => segment.startsWith('focus'))) {
        state = 'focus';
    }

    return {isDarkVariant: segments.includes('dark'), state};
}

/** The token-name suffix a usage's state wants. `null` means a resting token. */
function desiredTokenSuffixFor(usageState) {
    return usageState.state === null ? null : (USAGE_STATE_TOKEN_SUFFIXES[usageState.state] ?? null);
}

function tokenMatchesUsageState(token, desiredTokenSuffix) {
    return desiredTokenSuffix === null ? !token.isVariant : token.name.endsWith(`-${desiredTokenSuffix}`);
}

function paletteColourOf(paletteReference) {
    if (paletteReference === 'black') {
        return colors.black;
    }

    if (paletteReference === 'white') {
        return colors.white;
    }

    const separatorIndex = paletteReference.lastIndexOf('-');
    const paletteName = paletteReference.slice(0, separatorIndex);
    const shade = paletteReference.slice(separatorIndex + 1);
    const palette = colors[paletteName];

    return palette === undefined ? undefined : palette[shade];
}

/**
 * Scans the in-scope files and returns one record per distinct WRITTEN class, with counts and call sites.
 *
 * "Written class" includes the variant prefix: `hover:bg-gray-50` and `bg-gray-50` are separate records.
 */
function collectPaletteClasses(rootDirectory) {
    const pattern = buildPaletteClassPattern();
    const byWrittenClass = new Map();

    for (const filePath of listSourceFiles(rootDirectory)) {
        const lines = readFileSync(filePath, 'utf8').split('\n');

        lines.forEach((lineText, lineIndex) => {
            const lineNumber = lineIndex + 1;

            for (const match of lineText.matchAll(pattern)) {
                const [writtenClass, variantPrefix, prefix, paletteReference, alphaText] = match;
                const siteResolution = findSiteResolution(filePath, lineNumber, writtenClass);
                const recordKey = siteResolution === null ? writtenClass : `${writtenClass}#${siteResolution.label}`;
                const existing = byWrittenClass.get(recordKey);

                if (existing === undefined) {
                    byWrittenClass.set(recordKey, {
                        alpha: alphaText === undefined ? null : `${alphaText}%`,
                        className: writtenClass.slice(variantPrefix.length),
                        count: 1,
                        paletteReference,
                        prefix,
                        siteResolution,
                        sites: [{filePath, lineNumber}],
                        usageState: usageStateOf(variantPrefix),
                        variantPrefix,
                        writtenClass,
                    });
                } else {
                    existing.count += 1;
                    existing.sites.push({filePath, lineNumber});
                }
            }
        });
    }

    return [...byWrittenClass.values()].sort((first, second) => second.count - first.count);
}

function paletteNameOf(paletteReference) {
    if (paletteReference === 'black' || paletteReference === 'white') {
        return paletteReference;
    }

    return paletteReference.slice(0, paletteReference.lastIndexOf('-'));
}

/** The token groups a class may target, derived from the palette it names. Empty means no counterpart exists. */
function candidateTokenGroupsFor(paletteReference) {
    const paletteGroup = PALETTE_SEMANTIC_GROUPS[paletteNameOf(paletteReference)] ?? null;

    return paletteGroup === null ? [] : CANDIDATE_TOKEN_GROUPS[paletteGroup];
}

function classifyDelta(lightnessDelta) {
    const magnitude = Math.abs(lightnessDelta);

    if (magnitude <= EXACT_LIGHTNESS_DELTA) {
        return 'exact';
    }

    return magnitude <= NEAR_LIGHTNESS_DELTA ? 'near' : 'drift';
}

/**
 * Best token in one family: semantic-group compatible first, then ranked within that gate.
 *
 * The ranking is NOT plain nearest-by-lightness, and each departure earns its place:
 *
 * - A token that is inside the clean band always beats one outside it, whatever the raw distance.
 * - Inside the clean band, a token whose STATE matches the usage wins. A resting usage should take a resting
 *   token when one is close enough, and a `hover:` usage should take a `-hover` token. Without this,
 *   `bg-green-100` was filed as needing `--surface-success-secondary-hover` (0.5 points away) when the
 *   resting `--surface-success-secondary` was a perfectly clean 4.5 points away, and `hover:bg-gray-200`
 *   took a resting token that only happens to hold the same value as the hover token today.
 * - Remaining ties go to the earlier semantic group, then to CSS order. `--content-neutral-tertiary` and
 *   `--content-onsurface-secondary` are both `215 20% 65%`, and a muted grey belongs to the neutral ramp.
 */
function bestTokenInFamily(classHsl, tokens, family, allowedGroups, desiredTokenSuffix) {
    let best = null;

    tokens.forEach((token, tokenIndex) => {
        if (
            token.family !== family ||
            NON_CANDIDATE_TOKEN_GROUPS.includes(token.group) ||
            token.isIdenticalInDarkMode
        ) {
            return;
        }

        if (!allowedGroups.includes(token.group)) {
            return;
        }

        const lightnessDelta = token.hsl[2] - classHsl[2];
        const match = classifyDelta(lightnessDelta);
        const candidate = {
            deltaE: oklabDistance(classHsl, token.hsl),
            groupRank: allowedGroups.indexOf(token.group),
            isClean: match !== 'drift',
            lightnessDelta,
            match,
            stateMatches: tokenMatchesUsageState(token, desiredTokenSuffix),
            token,
            tokenIndex,
        };

        if (best === null || isBetterCandidate(candidate, best)) {
            best = candidate;
        }
    });

    return best;
}

/** Builds the same candidate record as the ranking would, for a token a hand resolution names outright. */
function namedTokenCandidate(classHsl, tokens, tokenName, desiredTokenSuffix) {
    const tokenIndex = tokens.findIndex((token) => token.name === tokenName);

    if (tokenIndex === -1) {
        throw new Error(`MANUAL_RESOLUTIONS names ${tokenName}, which is not a token in ${TOKEN_STYLESHEET_PATH}`);
    }

    const token = tokens[tokenIndex];

    if (token.isIdenticalInDarkMode) {
        throw new Error(
            `MANUAL_RESOLUTIONS names ${tokenName}, whose :root and .dark values are byte-identical — it cannot ` +
                'express a hover/active state in dark mode and must not be named as a migration target until fixed'
        );
    }

    const lightnessDelta = token.hsl[2] - classHsl[2];
    const match = classifyDelta(lightnessDelta);

    return {
        deltaE: oklabDistance(classHsl, token.hsl),
        groupRank: 0,
        isClean: match !== 'drift',
        lightnessDelta,
        match,
        stateMatches: tokenMatchesUsageState(token, desiredTokenSuffix),
        token,
        tokenIndex,
    };
}

function isBetterCandidate(candidate, incumbent) {
    if (candidate.isClean !== incumbent.isClean) {
        return candidate.isClean;
    }

    if (candidate.isClean && candidate.stateMatches !== incumbent.stateMatches) {
        return candidate.stateMatches;
    }

    const candidateDistance = Math.abs(candidate.lightnessDelta);
    const incumbentDistance = Math.abs(incumbent.lightnessDelta);

    if (candidateDistance !== incumbentDistance) {
        return candidateDistance < incumbentDistance;
    }

    if (candidate.stateMatches !== incumbent.stateMatches) {
        return candidate.stateMatches;
    }

    if (candidate.groupRank !== incumbent.groupRank) {
        return candidate.groupRank < incumbent.groupRank;
    }

    return candidate.tokenIndex < incumbent.tokenIndex;
}

/**
 * Derives the set for one usage from the family its CSS property implies, before manual resolution.
 *
 * A - clean match to a token in the right STATE for the usage.
 * B - clean match only to a token in the wrong state. Both directions count: a resting usage landing on a
 *     `-hover`/`-active` token, and a `hover:` usage landing on a resting token. Either is a state mismatch
 *     that happens to be invisible today only because the two tokens hold equal values.
 * D - nothing within NEAR_LIGHTNESS_DELTA.
 *
 * Set C is deliberately NOT derived. A clean match in an alternate family is evidence that a cross-family
 * mapping MIGHT be right, not that it is: `text-green-800` on a status badge also matches a stroke token
 * exactly, and it is still body text. Only a human who has read the call site can promote a class to C, so
 * C comes exclusively from MANUAL_RESOLUTIONS. The alternate candidates are still reported, as the raw
 * material for that judgement.
 */
function deriveSet(candidates) {
    const primary = candidates.find((candidate) => candidate.isPrimaryFamily)?.best ?? null;

    if (primary === null || primary.match === 'drift') {
        return 'D';
    }

    return primary.stateMatches ? 'A' : 'B';
}

function formatHsl(hsl, alpha) {
    const formatted = `${hsl[0].toFixed(0)} ${hsl[1].toFixed(0)}% ${hsl[2].toFixed(1)}%`;

    return alpha === null || alpha === undefined ? formatted : `${formatted} / ${alpha}`;
}

function formatDelta(lightnessDelta) {
    const rounded = Math.round(lightnessDelta);

    return rounded > 0 ? `+${rounded}` : `${rounded}`;
}

/** Joins the analysis: palette classes x tokens -> one resolved row per distinct class. */
function buildRows(paletteClasses, tokens) {
    return paletteClasses.map((paletteClass) => {
        const paletteValue = paletteColourOf(paletteClass.paletteReference);
        const classHsl = paletteValueToHsl(paletteValue);
        const families = CLASS_PREFIX_FAMILIES[paletteClass.prefix];
        const eligibleFamilies = [
            {family: families.primary, isPrimaryFamily: true},
            ...families.alternate.map((family) => ({family, isPrimaryFamily: false})),
        ];
        const allowedGroups = candidateTokenGroupsFor(paletteClass.paletteReference);
        const desiredTokenSuffix = desiredTokenSuffixFor(paletteClass.usageState);
        const candidates = eligibleFamilies.map((eligible) => ({
            ...eligible,
            best:
                classHsl === null
                    ? null
                    : bestTokenInFamily(classHsl, tokens, eligible.family, allowedGroups, desiredTokenSuffix),
        }));

        const paletteName = paletteNameOf(paletteClass.paletteReference);
        const siteResolution = paletteClass.siteResolution;
        const manual = siteResolution ?? MANUAL_RESOLUTIONS[paletteClass.writtenClass];
        const derivedSet = deriveSet(candidates);

        let set = manual?.set ?? derivedSet;
        let reason = manual?.reason ?? null;

        if (manual?.set === undefined && PALETTE_SEMANTIC_GROUPS[paletteName] === null) {
            set = 'E';
            reason = CATEGORICAL_IDENTITY_REASON;
        }

        const chosenFamily = manual?.family ?? families.primary;
        const derivedChosen = candidates.find((candidate) => candidate.family === chosenFamily)?.best ?? null;
        const chosen =
            manual?.token === undefined
                ? derivedChosen
                : namedTokenCandidate(classHsl, tokens, manual.token, desiredTokenSuffix);
        const primaryCandidate = candidates.find((candidate) => candidate.isPrimaryFamily);
        const primaryDrifts = primaryCandidate.best === null || primaryCandidate.best.match === 'drift';
        const hasCleanAlternate =
            primaryDrifts &&
            candidates.some(
                (candidate) => !candidate.isPrimaryFamily && candidate.best !== null && candidate.best.match !== 'drift'
            );

        const resolvedChosen = set === 'E' ? null : chosen;
        const hasVisibleShift =
            PERCEPTUALLY_PROMOTABLE_SETS.includes(set) &&
            resolvedChosen !== null &&
            resolvedChosen.deltaE > VISIBLE_SHIFT_DELTA_E;

        return {
            candidates,
            chosen: resolvedChosen,
            classHsl,
            crossFamilyNote: manual?.crossFamilyNote ?? null,
            derivedSet,
            hasCleanAlternate,
            paletteClass,
            paletteValue,
            // Which set the row would have been in but for its perceptual distance. Set F is drawn from
            // A, B and C uniformly, and knowing the origin tells a reader what is left to do once the hue
            // question is answered.
            perceptualOriginSet: hasVisibleShift ? set : null,
            reason,
            set: hasVisibleShift ? 'F' : set,
        };
    });
}

function formatDeltaE(deltaE) {
    return deltaE.toFixed(3);
}

/**
 * Renders the class cell, backticks included.
 *
 * A site-resolved row shares its written class with the rest of that class's occurrences, so the label is
 * the only thing telling the two rows apart in the table.
 */
function displayClassOf(paletteClass) {
    const code = `\`${paletteClass.writtenClass}\``;

    return paletteClass.siteResolution === null ? code : `${code} — ${paletteClass.siteResolution.label}`;
}

/** Signed circular hue difference in degrees, token minus class. */
function formatHueDelta(classHsl, tokenHsl) {
    const rawDifference = (((tokenHsl[0] - classHsl[0]) % 360) + 540) % 360;
    const signedDifference = Math.round(rawDifference - 180);

    return signedDifference > 0 ? `+${signedDifference}` : `${signedDifference}`;
}

function renderTable(rows, paletteClasses, tokens, fileCount) {
    const totalOccurrences = paletteClasses.reduce(
        (runningTotal, paletteClass) => runningTotal + paletteClass.count,
        0
    );
    const setTotals = {A: 0, B: 0, C: 0, D: 0, E: 0, F: 0};

    for (const row of rows) {
        setTotals[row.set] += row.paletteClass.count;
    }

    const lines = [];

    lines.push('# Phase 3 colour mapping table');
    lines.push('');
    lines.push(
        'Generated by `client/scripts/derive-colour-mapping.mjs`. Do not hand-edit the derived columns — change the script and regenerate. The `Set`, `Target token` and `Why` columns for cross-family, exception, and hand-pinned rows come from `MANUAL_RESOLUTIONS` in that script, which is authored against the call sites; a resolution that stops matching anything in scope makes the script exit non-zero rather than silently reverting to the derived answer.'
    );
    lines.push('');
    lines.push(
        '**Beyond a row\'s set letter, three sections override it**: "Exception files" and "Exception sites" (call sites where the set letter does not apply), and "Dark-variant literals" (`dark:` classes get DELETED, not remapped).'
    );
    lines.push('');
    lines.push('## Method');
    lines.push('');
    lines.push(
        `- Token values are parsed from the \`:root\` block of \`client/src/styles/index.css\` (${tokens.length} tokens in the \`--surface-*\` / \`--content-*\` / \`--stroke-*\` families). Both the space-separated \`229 84% 5%\` and comma-separated \`213, 27%, 84%\` forms are handled, and an alpha suffix such as \`0 74% 42% / 50%\` parses its first three components.`
    );
    lines.push(
        '- Palette values come from `tailwindcss/colors` at the version installed in this repo (Tailwind 4), which ships `oklch()` strings. Each is converted OKLCH -> OKLab -> LMS -> linear sRGB -> gamma-encoded sRGB -> HSL. **A Tailwind v3 hex table is not equivalent**: it happens to land close on the greys and diverges on saturation for everything chromatic (v3 `blue-500` is `217 91% 60%`, Tailwind 4 `blue-500` is `216 100% 59%`).'
    );
    lines.push(
        `- Scope: all of \`client/src\`, excluding \`workflow-editor\`, \`cluster-element-editor\`, \`*.test.*\` and \`*.stories.*\`. ${fileCount} files scanned, **${totalOccurrences} occurrences** across **${paletteClasses.length} distinct classes**.`
    );
    lines.push(
        '- A candidate token must be in a compatible SEMANTIC GROUP before lightness is compared: `gray`/`slate`/`zinc`/`stone`/`black`/`white` -> `neutral` + `onsurface`, `blue`/`indigo`/`sky` -> `brand`, `red`/`rose` -> `destructive`, `green`/`emerald`/`teal`/`lime` -> `success`, `amber`/`yellow`/`orange` -> `warning`. Without that gate a nearest-by-lightness search maps `bg-green-100` onto a neutral token at the same lightness. The gate is semantic rather than colorimetric because hue/chroma thresholds misfile both ends of the lightness range — `--content-neutral-primary: 229 84% 5%` reads as 84% saturated but is visually near-black, and `bg-blue-50` is a vivid hue at 1.4% OKLCh chroma.'
    );
    lines.push(
        '- Three token groups are excluded from automatic candidacy, because lightness proximity to them is a trap. The **workflow-canvas node-type** tokens (`--stroke-branch-*`, `--stroke-loop-*`, and siblings) encode which kind of node an edge belongs to. The **disabled** tokens are states: `--content-disabled: 0 0% 64%` is 0.3 points from `gray-400`, closer than the correct `--content-neutral-tertiary`, so every muted grey would come out meaning "disabled". And the **app-shell backdrops**, `--surface-main` and `--surface-popover-canvas`: `--surface-main` is the nearest token to `bg-gray-50` in light mode by 0.1 points, but in dark mode it is `229 84% 5%`, byte-identical to `--surface-neutral-primary`, the card colour — so a hover fill or an inset tile that took it would render as its own container once the theme flipped. Each is reachable through a manual resolution when a call site genuinely wants it.'
    );
    lines.push(
        `- Match quality: \`exact\` = ΔL <= ${EXACT_LIGHTNESS_DELTA}, \`near\` = ΔL <= ${NEAR_LIGHTNESS_DELTA}, \`drift\` = ΔL > ${NEAR_LIGHTNESS_DELTA}. ΔL is *token lightness minus class lightness*, in HSL lightness points, shown rounded. **\`exact\` and \`near\` are LIGHTNESS claims and nothing more** — see the next bullet.`
    );
    lines.push(
        `- Every row also carries **ΔE**, a perceptual distance in OKLab across all three dimensions. Lightness alone hides hue: \`text-orange-800\` sits 2 lightness points from \`--content-warning-primary\` and is labelled \`exact\`, while shifting hue from 17° to 35° — a ΔE of 0.113, nearly 6x the ~${OKLAB_JND} just-noticeable difference. **Any row past ${VISIBLE_SHIFT_DELTA_E} (2x JND) becomes set F**, wherever it would otherwise have landed. It is not a flag on an A row; it is a different set, so scoping off the letter cannot pull it in by accident.`
    );
    lines.push(
        '- Classes are keyed by the class **as written, including its Tailwind variant prefix**, so `hover:bg-gray-50` and `bg-gray-50` are separate rows. They are two usages of one colour that want different tokens, and collapsing them hid a resting token standing in for a hover fill.'
    );
    lines.push('');
    lines.push('## The cross-family rule');
    lines.push('');
    lines.push(
        'Token families are named after CSS properties. Usages are organised by role. Where the two disagree, **role wins** — a rule forbidding cross-family mapping turns exact matches into phantom drift.'
    );
    lines.push('');
    lines.push('| Usage shape | Eligible families |');
    lines.push('|---|---|');
    lines.push('| `bg-` on a filled region | `surface` |');
    lines.push('| `bg-` on a decorative shape (`rounded-full` dot, small indicator) | `content` |');
    lines.push('| `text-` on text | `content` |');
    lines.push(
        '| `text-` on a large decorative icon (the `icon` prop of an empty state; observed `size-12` and up) | `stroke` and `content` both eligible |'
    );
    lines.push('| `border-`, `divide-`, `ring-` | `stroke` |');
    lines.push('');
    lines.push('## Sets');
    lines.push('');
    lines.push('| Set | Meaning | Disposition | Distinct classes | Occurrences |');
    lines.push('|---|---|---|---|---|');

    const setNames = ['A', 'B', 'C', 'D', 'E', 'F'];
    const setMeanings = {
        A: 'Clean match to a token in the right state for the usage, **and** a sub-threshold perceptual distance',
        B: 'Clean match only to a token in the WRONG state — either direction',
        C: 'Cross-family match — the role differs from the CSS property',
        D: 'Drifts more than 5 points of lightness from every eligible token',
        E: 'Exception — never migrate',
        F: `Perceptual review required — ΔE above ${VISIBLE_SHIFT_DELTA_E} (2x JND)`,
    };
    const setDispositions = {
        A: 'Migrate mechanically',
        B: 'Migrate after confirming the state substitution',
        C: 'Migrate mechanically; the family choice is already resolved',
        D: 'BLOCKED — needs a design decision on whether the lightness change is acceptable',
        E: 'Leave alone, permanently',
        F: 'BLOCKED — needs a design decision on whether a visible light-mode change is acceptable',
    };

    for (const setName of setNames) {
        const setRows = rows.filter((row) => row.set === setName);

        lines.push(
            `| ${setName} | ${setMeanings[setName]} | ${setDispositions[setName]} | ${setRows.length} | ${setTotals[setName]} |`
        );
    }

    lines.push(`| **Total** | | | **${rows.length}** | **${totalOccurrences}** |`);
    lines.push('');
    lines.push(
        'Sets D and F are both blocked, on **different questions**. D asks "may this element change its light-mode lightness by more than 5 HSL points"; F asks "may this element change in a way the eye can see", on whichever axis the row\'s numbers show — most F rows are perceptual-lightness moves, some are chroma, some are hue. The two sets can get different answers, so they stay separate.'
    );
    lines.push('');

    const rowColumns = '| Class | Count | Usage | Actual HSL | Target token | Target HSL | ΔL | Match | ΔE | Why |';
    const rowDivider = '|---|---|---|---|---|---|---|---|---|---|';
    const renderRow = (row) => {
        const {chosen, paletteClass} = row;
        const actual = row.classHsl === null ? '?' : formatHsl(row.classHsl, paletteClass.alpha);
        const usage = paletteClass.usageState.state ?? (paletteClass.usageState.isDarkVariant ? 'dark' : 'resting');

        return (
            `| ${displayClassOf(paletteClass)} | ${paletteClass.count} | ${usage} | ${actual} ` +
            `| ${chosen === null ? '—' : `\`${chosen.token.name}\``} ` +
            `| ${chosen === null ? '—' : formatHsl(chosen.token.hsl, chosen.token.alpha)} ` +
            `| ${chosen === null ? '—' : formatDelta(chosen.lightnessDelta)} ` +
            `| ${chosen === null ? '—' : chosen.match} ` +
            `| ${chosen === null ? '—' : formatDeltaE(chosen.deltaE)} ` +
            `| ${row.reason ?? describeCandidates(row)} |`
        );
    };

    for (const setName of setNames) {
        const setRows = rows.filter((row) => row.set === setName);

        lines.push(`## Set ${setName} — ${setMeanings[setName]}`);
        lines.push('');
        lines.push(`_${setDispositions[setName]}._`);
        lines.push('');

        if (setRows.length === 0) {
            lines.push('_None._');
            lines.push('');

            continue;
        }

        if (setName === 'F') {
            lines.push(
                'Each of these matches its token on lightness — several are labelled `exact` — and still moves perceptibly. The `Would be` column is the set the row returns to once the question is answered.'
            );
            lines.push('');
            lines.push(
                '**Read the `Axis` column, not the HSL deltas, to see what moves.** `Axis` is the dominant component of the OKLab difference; `ΔH` and `ΔS` are HSL degrees and saturation points, token minus class. The two disagree, and OKLab is the one that matches the eye: `text-green-600` has ΔH 0 and looks like a saturation-only change, but its OKLab difference is 0.108 lightness against 0.050 chroma — HSL lightness understates perceptual lightness at high chroma. So this set is not "the hue questions"; it is the perceptually visible ones, on whichever axis each row shows.'
            );
            lines.push('');
            lines.push(
                '| Class | Count | Would be | Target token | Actual HSL | Target HSL | ΔL | ΔH | ΔS | ΔE | x JND | Axis |'
            );
            lines.push('|---|---|---|---|---|---|---|---|---|---|---|---|');

            for (const row of setRows) {
                const {chosen, paletteClass} = row;

                lines.push(
                    `| ${displayClassOf(paletteClass)} | ${paletteClass.count} | ${row.perceptualOriginSet} ` +
                        `| \`${chosen.token.name}\` | ${formatHsl(row.classHsl, paletteClass.alpha)} ` +
                        `| ${formatHsl(chosen.token.hsl, chosen.token.alpha)} ` +
                        `| ${formatDelta(chosen.lightnessDelta)} ` +
                        `| ${formatHueDelta(row.classHsl, chosen.token.hsl)} ` +
                        `| ${formatDelta(chosen.token.hsl[1] - row.classHsl[1])} ` +
                        `| ${formatDeltaE(chosen.deltaE)} | ${(chosen.deltaE / OKLAB_JND).toFixed(1)}x ` +
                        `| ${dominantAxisOf(row.classHsl, chosen.token.hsl)} |`
                );
            }

            lines.push('');

            continue;
        }

        lines.push(rowColumns);
        lines.push(rowDivider);
        setRows.forEach((row) => lines.push(renderRow(row)));
        lines.push('');
    }

    const darkVariantRows = rows.filter((row) => row.paletteClass.usageState.isDarkVariant);

    lines.push('## Dark-variant literals');
    lines.push('');
    lines.push(
        'These carry a `dark:` prefix, which means they are hand-rolled theme overrides — the very thing a token replaces. When the base class beside them migrates, the `dark:` literal must be **DELETED**, not remapped: the token already carries its own dark value. Their derived set describes the colour, not the action to take.'
    );
    lines.push('');

    if (darkVariantRows.length === 0) {
        lines.push('_None._');
    } else {
        lines.push('| Class | Count | Set | Nearest token | ΔL | ΔE |');
        lines.push('|---|---|---|---|---|---|');

        for (const row of darkVariantRows) {
            const nearest = row.chosen;

            lines.push(
                `| ${displayClassOf(row.paletteClass)} | ${row.paletteClass.count} | ${row.set} ` +
                    `| ${nearest === null ? '—' : `\`${nearest.token.name}\``} ` +
                    `| ${nearest === null ? '—' : formatDelta(nearest.lightnessDelta)} ` +
                    `| ${nearest === null ? '—' : formatDeltaE(nearest.deltaE)} |`
            );
        }
    }

    lines.push('');

    lines.push('## Exception files');
    lines.push('');
    lines.push(
        "A row's set is not the last word for these files: their entire colour scheme is deliberate. `bg-white` is a clean set A everywhere except inside the Appearance theme previews, where it is one of the literal swatches. `--check` skips them."
    );
    lines.push('');
    lines.push('| File | Why |');
    lines.push('|---|---|');

    for (const exceptionFile of EXCEPTION_FILES) {
        lines.push(`| \`client/${exceptionFile.path}\` | ${exceptionFile.reason} |`);
    }

    lines.push('');
    lines.push('## Exception sites');
    lines.push('');
    lines.push(
        'Line ranges inside otherwise-ordinary files. Each is a categorical palette declared as an object literal where some members have semantic-token counterparts and some do not — theming the ones that match would leave a half-themed set, which is the same reason the unmatched member was filed as an exception in the first place. `--check` skips these lines.'
    );
    lines.push('');
    lines.push('| File | Lines | Why |');
    lines.push('|---|---|---|');

    for (const exceptionSite of EXCEPTION_SITES) {
        const ranges = exceptionSite.ranges
            .map((range) => (range.start === range.end ? `${range.start}` : `${range.start}-${range.end}`))
            .join(', ');

        lines.push(`| \`client/${exceptionSite.path}\` | ${ranges} | ${exceptionSite.reason} |`);
    }

    lines.push('');
    lines.push('## Site resolutions');
    lines.push('');
    lines.push(
        'Occurrences that resolve differently from the rest of their class. The mirror image of keying rows by the written class: that split assumes a `hover:` class expresses a hover CHANGE, and sometimes it expresses hover SUPPRESSION, where two differently-written classes must land on the SAME token. These appear as their own labelled rows in the set tables above.'
    );
    lines.push('');
    lines.push('| Class | Label | Sites | Pinned to | Why |');
    lines.push('|---|---|---|---|---|');

    for (const siteResolution of SITE_RESOLUTIONS) {
        const sites = siteResolution.sites.map((site) => `\`${site.path}:${site.line}\``).join(', ');

        lines.push(
            `| \`${siteResolution.writtenClass}\` | ${siteResolution.label} | ${sites} | \`${siteResolution.token}\` | ${siteResolution.reason} |`
        );
    }

    lines.push('');
    lines.push('## Cross-family decision log');
    lines.push('');
    lines.push(
        'Every class where the family the CSS property implies does NOT match cleanly but an alternate family does. A clean alternate is evidence that a cross-family mapping might be right, never proof that it is — each of these was settled by reading the call sites. Anything marked UNRESOLVED needs a human before the class is migrated.'
    );
    lines.push('');
    lines.push('| Class | Count | Property family | Alternate family | Decision |');
    lines.push('|---|---|---|---|---|');

    const crossFamilyRows = rows.filter((row) => row.hasCleanAlternate && row.set !== 'E');

    for (const row of crossFamilyRows) {
        const primary = row.candidates.find((candidate) => candidate.isPrimaryFamily);
        const alternate = row.candidates.find(
            (candidate) => !candidate.isPrimaryFamily && candidate.best !== null && candidate.best.match !== 'drift'
        );
        const primaryText =
            primary.best === null
                ? `${primary.family}: no compatible token`
                : `${primary.family}: \`${primary.best.token.name}\` ${formatDelta(primary.best.lightnessDelta)} (${primary.best.match})`;
        const alternateText = `${alternate.family}: \`${alternate.best.token.name}\` ${formatDelta(alternate.best.lightnessDelta)} (${alternate.best.match})`;
        const decision = row.crossFamilyNote ?? '**UNRESOLVED** — no call-site judgement recorded.';

        lines.push(
            `| ${displayClassOf(row.paletteClass)} | ${row.paletteClass.count} | ${primaryText} | ${alternateText} | ${decision} |`
        );
    }

    lines.push('');
    lines.push('## All eligible candidates per class');
    lines.push('');
    lines.push(
        'The script emits every eligible family with its best match rather than a single answer — it cannot tell a decorative icon from body text, so it does not pretend to. This is the raw material a human resolution is made from.'
    );
    lines.push('');
    lines.push('| Class | Family | Role | Best token | Target HSL | ΔL | Match |');
    lines.push('|---|---|---|---|---|---|---|');

    for (const row of rows) {
        for (const candidate of row.candidates) {
            const role = candidate.isPrimaryFamily ? 'property' : 'alternate';
            const best = candidate.best;
            const tokenName = best === null ? '_no compatible token_' : `\`${best.token.name}\``;
            const targetHsl = best === null ? '—' : formatHsl(best.token.hsl, best.token.alpha);
            const delta = best === null ? '—' : formatDelta(best.lightnessDelta);
            const match = best === null ? 'none' : best.match;

            lines.push(
                `| ${displayClassOf(row.paletteClass)} | ${candidate.family} | ${role} | ${tokenName} | ${targetHsl} | ${delta} | ${match} |`
            );
        }
    }

    lines.push('');
    lines.push('## Call sites for sets C and E');
    lines.push('');
    lines.push('| Class | Set | Files |');
    lines.push('|---|---|---|');

    for (const row of rows.filter((candidateRow) => candidateRow.set === 'C' || candidateRow.set === 'E')) {
        const files = [...new Set(row.paletteClass.sites.map((site) => relative(CLIENT_ROOT, site.filePath)))];
        const shown = files
            .slice(0, 6)
            .map((file) => `\`${file}\``)
            .join(', ');
        const suffix = files.length > 6 ? `, +${files.length - 6} more` : '';

        lines.push(`| ${displayClassOf(row.paletteClass)} | ${row.set} | ${shown}${suffix} |`);
    }

    lines.push('');

    return lines.join('\n');
}

function describeCandidates(row) {
    return row.candidates
        .map((candidate) => {
            const best = candidate.best;
            const label = `${toTitleCase(candidate.family)}${candidate.isPrimaryFamily ? '' : ' (alternate)'}`;

            return best === null
                ? `${label}: no compatible token`
                : `${label}: ${best.token.name} ${formatDelta(best.lightnessDelta)} (${best.match})`;
        })
        .join('; ');
}

function isExceptionFile(filePath) {
    const relativePath = relative(CLIENT_ROOT, filePath);

    return EXCEPTION_FILES.some((exceptionFile) => relativePath === exceptionFile.path);
}

function isExceptionSite(filePath, lineNumber) {
    const relativePath = relative(CLIENT_ROOT, filePath);

    return EXCEPTION_SITES.some(
        (exceptionSite) =>
            relativePath === exceptionSite.path &&
            exceptionSite.ranges.some((range) => lineNumber >= range.start && lineNumber <= range.end)
    );
}

/**
 * Fails loudly when a line-number contract has drifted.
 *
 * Every EXCEPTION_SITES range and every SITE_RESOLUTIONS site pins absolute line numbers in files that
 * tasks 2-6 will edit. An insertion above any of them shifts the target silently, and the failure mode is
 * invisible in both directions: a categorical map quietly stops being excepted, or ordinary chrome quietly
 * starts being. Anchoring on text that must be inside the range turns that into a non-zero exit.
 */
function findDriftedLineContracts() {
    const problems = [];
    const readLines = (relativePath) => {
        try {
            return readFileSync(join(CLIENT_ROOT, relativePath), 'utf8').split('\n');
        } catch {
            return null;
        }
    };

    for (const exceptionSite of EXCEPTION_SITES) {
        const lines = readLines(exceptionSite.path);

        if (lines === null) {
            problems.push(`EXCEPTION_SITES: ${exceptionSite.path} no longer exists`);

            continue;
        }

        for (const range of exceptionSite.ranges) {
            const rangeText = lines.slice(range.start - 1, range.end).join('\n');
            const missing = range.expect.filter((expected) => !rangeText.includes(expected));

            if (missing.length > 0) {
                problems.push(
                    `EXCEPTION_SITES: ${exceptionSite.path}:${range.start}-${range.end} no longer contains ${missing.map((expected) => JSON.stringify(expected)).join(', ')}`
                );
            }
        }
    }

    for (const siteResolution of SITE_RESOLUTIONS) {
        for (const site of siteResolution.sites) {
            const lines = readLines(site.path);
            const lineText = lines === null ? null : lines[site.line - 1];

            if (lineText === null || lineText === undefined || !lineText.includes(site.expect)) {
                problems.push(
                    `SITE_RESOLUTIONS: ${site.path}:${site.line} no longer contains ${JSON.stringify(site.expect)}`
                );
            }
        }
    }

    return problems;
}

/** The site resolution, if any, that claims a specific occurrence of a class. */
function findSiteResolution(filePath, lineNumber, writtenClass) {
    const relativePath = relative(CLIENT_ROOT, filePath);

    return (
        SITE_RESOLUTIONS.find(
            (siteResolution) =>
                siteResolution.writtenClass === writtenClass &&
                siteResolution.sites.some((site) => site.path === relativePath && site.line === lineNumber)
        ) ?? null
    );
}

/**
 * Fails loudly when a hand resolution no longer matches anything.
 *
 * Every entry in MANUAL_RESOLUTIONS is a judgement made against a call site. If the class is renamed, moved
 * out of scope, or has its variant prefix changed, the judgement silently stops applying and the class
 * quietly reverts to whatever the script derives. That is exactly the kind of regression this table exists
 * to prevent, so it is an error rather than a warning.
 */
function findStaleResolutions(rows) {
    const seenClasses = new Set(rows.map((row) => row.paletteClass.writtenClass));
    const seenLabels = new Set(
        rows
            .filter((row) => row.paletteClass.siteResolution !== null)
            .map((row) => row.paletteClass.siteResolution.label)
    );

    return [
        ...Object.keys(MANUAL_RESOLUTIONS).filter((writtenClass) => !seenClasses.has(writtenClass)),
        ...SITE_RESOLUTIONS.filter((siteResolution) => !seenLabels.has(siteResolution.label)).map(
            (siteResolution) => `${siteResolution.writtenClass} (${siteResolution.label})`
        ),
    ];
}

/** `--check TARGET`: reports any remaining set A or set C class under TARGET, for later tasks' grep assertions. */
function runCheck(checkTarget, rows) {
    const migratableClassNames = new Set(
        rows.filter((row) => row.set === 'A' || row.set === 'C').map((row) => row.paletteClass.writtenClass)
    );
    const pattern = buildPaletteClassPattern();
    const findings = [];
    const targetFiles = statSync(checkTarget).isDirectory() ? listSourceFiles(checkTarget) : [checkTarget];

    for (const filePath of targetFiles) {
        if (isExceptionFile(filePath)) {
            continue;
        }

        const lines = readFileSync(filePath, 'utf8').split('\n');

        lines.forEach((lineText, lineIndex) => {
            const lineNumber = lineIndex + 1;

            if (isExceptionSite(filePath, lineNumber)) {
                return;
            }

            for (const match of lineText.matchAll(pattern)) {
                if (migratableClassNames.has(match[0])) {
                    findings.push(`${relative(CLIENT_ROOT, filePath)}:${lineNumber}: ${match[0]}`);
                }
            }
        });
    }

    if (findings.length === 0) {
        process.stdout.write(`No remaining set A or set C classes under ${relative(CLIENT_ROOT, checkTarget)}\n`);

        return 0;
    }

    process.stdout.write(findings.join('\n'));
    process.stdout.write(
        `\n${findings.length} remaining set A/C occurrence(s) under ${relative(CLIENT_ROOT, checkTarget)}\n`
    );

    return 1;
}

function main() {
    const driftedContracts = findDriftedLineContracts();

    if (driftedContracts.length > 0) {
        process.stderr.write(
            `Line-number contracts have drifted:\n${driftedContracts.map((problem) => `  ${problem}`).join('\n')}\n` +
                'Re-read the call sites and update the line numbers; do not just widen the ranges.\n'
        );

        return 4;
    }

    const tokens = readTokens();
    const brokenContentStateTokens = findIdenticalContentStateTokens(tokens);

    if (brokenContentStateTokens.length > 0) {
        process.stderr.write(
            'Content-family hover/active tokens with byte-identical :root and .dark values (cannot express ' +
                'their state in dark mode):\n' +
                `${brokenContentStateTokens.map((token) => `  ${token.name}`).join('\n')}\n` +
                'Give each a real dark value before it can be a migration target.\n'
        );

        return 5;
    }

    const paletteClasses = collectPaletteClasses(SOURCE_ROOT);
    const rows = buildRows(paletteClasses, tokens);
    const staleResolutions = findStaleResolutions(rows);

    if (staleResolutions.length > 0) {
        process.stderr.write(
            `MANUAL_RESOLUTIONS entries match nothing in scope: ${staleResolutions.join(', ')}\n` +
                'Each is a call-site judgement that has silently stopped applying. Re-check the call site.\n'
        );

        return 3;
    }

    const checkIndex = process.argv.indexOf('--check');

    if (checkIndex !== -1) {
        const checkTarget = process.argv[checkIndex + 1];

        if (checkTarget === undefined) {
            process.stderr.write('--check requires a directory\n');

            return 2;
        }

        return runCheck(resolve(CLIENT_ROOT, checkTarget), rows);
    }

    const fileCount = new Set(paletteClasses.flatMap((paletteClass) => paletteClass.sites.map((site) => site.filePath)))
        .size;

    process.stdout.write(renderTable(rows, paletteClasses, tokens, fileCount));
    process.stdout.write('\n');

    return 0;
}

process.exitCode = main();
