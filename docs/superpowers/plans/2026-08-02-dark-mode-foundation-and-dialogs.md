# Dark Mode — Foundation and Dialog Conversion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make dark mode render correctly across the 74% of the client that already uses semantic tokens, and bring 15 hand-rolled dialogs into the design system.

**Architecture:** Three changes in sequence. First, correct the `.dark` token block and declare `color-scheme` so the browser themes its own widgets — one file that fixes 3445 usages. Second, fix two live bugs in the theme provider and add a pre-paint script so dark-mode users stop seeing a white flash. Third, replace 15 hand-rolled modal shells with the shadcn `Dialog` primitive and their 170 raw form elements with `Input` / `Select` / `Textarea` / `Label`, which fixes six accessibility defects and deletes 15 overlay scrims.

**Tech Stack:** React 19.2, TypeScript 5.9, Vite 8, TailwindCSS 4.3 (Vite plugin, `@config` pointing at `tailwind.config.js`), Radix UI via shadcn, Vitest 4, Testing Library.

## Global Constraints

Copied verbatim from the repo's client conventions. Every task's requirements implicitly include this section.

- **Working directory is `client/`.** All paths below are relative to `/Volumes/Data/bytechef/bytechef/client`.
- **ESLint `sort-keys`:** object keys in natural ascending (alphabetical) order. `--fix` does NOT auto-fix this; fix manually.
- **Import destructure sort:** named imports sorted alphabetically within `{}`. `type` imports sort by name, not grouped separately.
- **Interface naming:** interface names must end with `I` or `Props`.
- **Lucide icons:** always import with the `Icon` suffix — `XIcon`, not `X`.
- **Class merging:** use `twMerge` from `tailwind-merge`. Do **not** use `cn()`.
- **Refs:** `useRef` variables must end with `Ref`.
- **Hook ordering:** `useState` → `useRef` → custom store hooks → other custom hooks → derived values / `useMemo` / `useCallback` → `useEffect` → `return`. All `useEffect` calls go last, immediately before `return`.
- **Semantic form grouping:** use `fieldset` with `border-0`, not `div`.
- **Variable naming:** no short or cryptic names. `users.find((user) => ...)`, never `(u) =>`.
- **Test naming:** unit test files end `.test.tsx`. Test method names are camelCase without underscores.
- **The canonical Button is `@/components/Button/Button`** (423 importers), NOT `@/components/ui/button` (5 importers). Its API is `label`, `variant`, `size`, `disabled`, `onClick` — it renders a label string, it does not take children for the label case.
- **Commit message format:** client-side changes use `<ticket_number> client - <description>`. Use `0` as the ticket number if none is assigned.
- **Verification command:** `npm run check` runs prettier, eslint (`--max-warnings=0`), tsc, and vitest with coverage. Individual tests: `npx vitest run <path>`.

---

## Scope

This plan covers **Phase 1 (Foundation)** and **Phase 2 (Dialog conversion)** from `docs/superpowers/specs/2026-08-02-dark-mode-design.md` — 19 files.

**Phase 3 (the 215-file colour migration) is deliberately excluded and gets its own plan.** Two reasons: it is 215 files of repetitive mechanical work that needs a different task shape, and its inputs are not yet known — Phase 2 deletes 15 overlay scrims and most of the 36 palette classes inside those dialogs, so any Phase 3 file counts written today would be stale by the time Phase 3 starts. Write that plan after Phase 2 lands and the numbers can be re-measured.

Phases 1 and 2 each produce working, independently shippable software: Phase 1 ships dark mode for 3445 usages, Phase 2 ships an accessibility fix.

---

## File Structure

**Phase 1 — created:**

| File | Responsibility |
|---|---|
| `src/styles/tests/tokenParity.test.ts` | Parses `index.css`, asserts every themeable `:root` token has a `.dark` counterpart. Permanent regression guard against the exact class of bug this project fixes. |
| `src/shared/providers/tests/theme-provider.test.tsx` | Covers the two provider bugs — OS-theme subscription while on `system`, and the `useTheme`-outside-provider guard. |

**Phase 1 — modified:**

| File | Change |
|---|---|
| `src/styles/index.css` | 9 missing `.dark` overrides, 6 dead tokens deleted, 6 copied-from-light values re-derived, `color-scheme` declared |
| `src/shared/providers/theme-provider.tsx` | `matchMedia` subscription; context defaults to `undefined` so the guard fires |
| `index.html` | Blocking pre-paint theme script; `theme-color` follows theme |
| `src/main.tsx` | `defaultTheme="light"` → `"system"` |

**Phase 2 — modified (15 dialogs) and created (15 test files):** listed per task.

---

# Phase 1 — Foundation

### Task 1: Token parity regression test

Writes the test that encodes the `:root` ↔ `.dark` contract. It fails on the current file, listing every gap — that failure list is the specification for Task 2.

**Files:**
- Create: `src/styles/tests/tokenParity.test.ts`

**Interfaces:**
- Consumes: nothing.
- Produces: nothing importable. Later tasks rely on this test staying green.

- [ ] **Step 1: Write the failing test**

Create `src/styles/tests/tokenParity.test.ts`:

```ts
import {readFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {describe, expect, it} from 'vitest';

const currentDirectory = dirname(fileURLToPath(import.meta.url));

const stylesheet = readFileSync(resolve(currentDirectory, '../index.css'), 'utf8');

/**
 * Tokens that legitimately have no `.dark` override.
 *
 * `--radius` and the popover width are not colours. The `--sidebar*` tokens are
 * `var(--muted)`-style aliases declared on the same element as the `.dark` class, so they
 * already resolve against the dark values — giving them explicit overrides would break that
 * indirection.
 */
const THEME_EXEMPT_TOKENS = new Set([
    '--radius',
    '--sidebar',
    '--sidebar-accent',
    '--sidebar-accent-foreground',
    '--sidebar-border',
    '--sidebar-foreground',
    '--sidebar-primary',
    '--sidebar-primary-foreground',
    '--sidebar-ring',
    '--workflow-nodes-popover-component-menu-width',
]);

const extractBlockBody = (source: string, selector: string): string => {
    const selectorIndex = source.indexOf(`${selector} {`);

    if (selectorIndex === -1) {
        throw new Error(`Could not find the "${selector}" block in index.css`);
    }

    const bodyStart = source.indexOf('{', selectorIndex) + 1;

    let depth = 0;

    for (let index = bodyStart - 1; index < source.length; index += 1) {
        if (source[index] === '{') {
            depth += 1;
        } else if (source[index] === '}') {
            depth -= 1;

            if (depth === 0) {
                return source.slice(bodyStart, index);
            }
        }
    }

    throw new Error(`The "${selector}" block in index.css is not terminated`);
};

const extractTokenNames = (blockBody: string): Set<string> =>
    new Set([...blockBody.matchAll(/^\s*(--[a-z0-9-_]+)\s*:/gim)].map((match) => match[1]));

describe('index.css theme token parity', () => {
    const rootTokens = extractTokenNames(extractBlockBody(stylesheet, ':root'));
    const darkTokens = extractTokenNames(extractBlockBody(stylesheet, '.dark'));

    it('defines a dark override for every themeable root token', () => {
        const missing = [...rootTokens]
            .filter((token) => !THEME_EXEMPT_TOKENS.has(token))
            .filter((token) => !darkTokens.has(token))
            .sort();

        expect(missing).toEqual([]);
    });

    it('does not define dark tokens that no longer exist in root', () => {
        const orphaned = [...darkTokens].filter((token) => !rootTokens.has(token)).sort();

        expect(orphaned).toEqual([]);
    });

    it('declares color-scheme in both themes so browser-native UI follows', () => {
        expect(extractBlockBody(stylesheet, ':root')).toMatch(/color-scheme:\s*light/);
        expect(extractBlockBody(stylesheet, '.dark')).toMatch(/color-scheme:\s*dark/);
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/styles/tests/tokenParity.test.ts`

Expected: FAIL. Two of the three cases fail.

The parity case reports 11 missing tokens:
```
--content-destructive          --stroke-neutral-primary-hover
--content-success              --stroke-neutral-secondary
--content-warning              --stroke-neutral-tertiary
--skeleton                     --stroke-warning-secondary
--stroke-brand-primary-pressed --surface-brand-secondary-hover
--stroke-neutral-primary
```

The `color-scheme` case fails because neither block declares it. The orphan case passes.

- [ ] **Step 3: Exclude `.ts` test files from coverage instrumentation**

`vitest.config.ts:12` excludes `**/*.test.tsx` from coverage but not `**/*.test.ts`, so this new file would be measured as if it were source. Extend the list:

```ts
exclude: ['.vitest/', 'node_modules/', 'src/middleware', '**/*.test.ts', '**/*.test.tsx'],
```

- [ ] **Step 4: Commit the failing test**

```bash
git add src/styles/tests/tokenParity.test.ts vitest.config.ts
git commit -m "0 client - Add index.css theme token parity regression test"
```

---

### Task 2: Close the token gaps and delete dead tokens

Turns Task 1's parity case green. Nine tokens get real dark values; two are dead and get deleted instead.

**Files:**
- Modify: `src/styles/index.css` — the `:root` block (lines ~50–204) and the `.dark` block (lines ~206–337)
- Test: `src/styles/tests/tokenParity.test.ts` (from Task 1, unchanged)

**Interfaces:**
- Consumes: the parity test from Task 1.
- Produces: nine `.dark` custom properties consumed by existing Tailwind utilities — `border-stroke-neutral-secondary`, `text-content-destructive`, and so on. No new class names.

- [ ] **Step 1: Delete the two dead tokens from `:root`**

Both have zero usages across `*.tsx`, `*.ts`, `*.css`. Remove these two lines from the `:root` block:

```css
--stroke-brand-primary-pressed: 213 87% 34%;
--stroke-neutral-primary-hover: 215 20% 65%;
```

Verify before deleting:

```bash
grep -rn "stroke-brand-primary-pressed\|stroke-neutral-primary-hover" src --include="*.tsx" --include="*.ts" | grep -v index.css
```
Expected: no output.

- [ ] **Step 2: Add the nine missing `.dark` overrides**

Insert into the `.dark` block, keeping the block's existing alphabetical ordering:

```css
--content-destructive: 0 84% 60%;
--content-success: 142 71% 45%;
--content-warning: 45 93% 47%;
--skeleton: 215 28% 17%;
--stroke-neutral-primary: 217 33% 17%;
--stroke-neutral-secondary: 215 25% 27%;
--stroke-neutral-tertiary: 215 19% 35%;
--stroke-warning-secondary: 45 80% 30%;
--surface-brand-secondary-hover: 217 91% 28%;
```

Rationale for the values, so a reviewer can judge them:

| Token | Light | Dark | Why |
|---|---|---|---|
| `--content-destructive` | `0 74% 42%` | `0 84% 60%` | Matches the existing dark `--content-destructive-primary`; the two are used interchangeably |
| `--content-success` | `142 72% 29%` | `142 71% 45%` | Matches the existing dark `--content-success-primary` |
| `--content-warning` | `45 93% 47%` | `45 93% 47%` | Matches the existing dark `--content-warning-primary`; saturated yellow already reads on dark |
| `--skeleton` | `220 9% 93%` | `215 28% 17%` | Matches dark `--muted`; skeletons should sit just above the base surface |
| `--stroke-neutral-primary` | `210 40% 96%` | `217 33% 17%` | Light ramp runs subtle→visible (96%→91%→84%); dark inverts it (17%→27%→35%) |
| `--stroke-neutral-secondary` | `214 32% 91%` | `215 25% 27%` | Same ramp. This is the 74-usage token, the single most visible fix |
| `--stroke-neutral-tertiary` | `213 27% 84%` | `215 19% 35%` | Same ramp |
| `--stroke-warning-secondary` | `44 70% 82%` | `45 80% 30%` | Muted warning border readable against dark surfaces |
| `--surface-brand-secondary-hover` | `212 80% 90%` | `217 91% 28%` | One step lighter than dark `--surface-brand-secondary` (`217 91% 22%`) |

These are starting values, chosen for ramp consistency. They are subject to the visual review in Task 8.

- [ ] **Step 3: Run the parity test to verify it passes**

Run: `npx vitest run src/styles/tests/tokenParity.test.ts -t 'defines a dark override'`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/styles/index.css
git commit -m "0 client - Add missing dark theme token overrides and drop two dead tokens"
```

---

### Task 3: Re-derive values copied from light, delete remaining dead tokens

Six `.dark` values are light surfaces sitting in a dark UI. Four more tokens are dead in both blocks.

**Files:**
- Modify: `src/styles/index.css`

**Interfaces:**
- Consumes: nothing new.
- Produces: nothing new. Changes existing values only.

- [ ] **Step 1: Verify the four remaining dead tokens really are dead**

```bash
for token in content-destructive-secondary surface-overlay-primary info info-foreground; do
  echo -n "$token: "
  grep -rhoE "\b(bg|text|border|fill|stroke|ring|divide|from|to|via)-${token}[a-z-]*" src --include='*.tsx' --include='*.ts' --include='*.css' \
    | sed -E 's/^(bg|text|border|fill|stroke|ring|divide|from|to|via)-//' | grep -cx -- "$token"
done
```
Expected: `0` for all four.

- [ ] **Step 2: Delete the four dead tokens from BOTH `:root` and `.dark`**

Remove `--content-destructive-secondary`, `--surface-overlay-primary`, `--info`, and `--info-foreground` from both blocks. Also delete the now-unused `info` entry from `tailwind.config.js` (`colors.info`, the `DEFAULT` / `foreground` pair).

`--info` is the important one: it reads as an available variant, so the next person styling a banner reaches for it and gets a light-blue surface in dark mode.

- [ ] **Step 3: Re-derive the six copied-from-light dark values**

Replace these in the `.dark` block:

```css
/* was 0 93% 94% / 0 93% 94% / 0 93% 94% — light pink surfaces in a dark UI */
--surface-destructive-secondary: 0 63% 18%;
--surface-destructive-secondary-hover: 0 63% 24%;
--surface-destructive-secondary-active: 0 63% 30%;

/* was 45 93% 47% / 45 93% 47% / 45 93% 47% — full-brightness yellow used as a surface */
--surface-warning-secondary: 45 80% 16%;
--surface-warning-secondary-hover: 45 80% 22%;
--surface-warning-secondary-active: 45 80% 28%;
```

Why this matters concretely: `bg-surface-destructive-secondary` pairs at its usage sites with `text-content-destructive-primary`. In dark that was `0 84% 60%` on `0 93% 94%` — roughly 2.5:1, below AA. Against `0 63% 18%` it clears AA comfortably.

Usage sites to eyeball in Task 8: `Badge.tsx:113`, `Button.tsx:70`, `WorkflowNodeDetailsPanel.tsx:216`, `WorkflowCodeEditorSheet.tsx:230`.

- [ ] **Step 4: Run the full parity test**

Run: `npx vitest run src/styles/tests/tokenParity.test.ts`

Expected: the parity and orphan cases PASS; the `color-scheme` case still FAILS (Task 4 fixes it).

- [ ] **Step 5: Commit**

```bash
git add src/styles/index.css tailwind.config.js
git commit -m "0 client - Re-derive dark surface values and remove dead theme tokens"
```

---

### Task 4: Declare `color-scheme`

The highest-leverage two lines in the project. Governs scrollbars, native form controls, `<select>` popups, and date pickers, and is what Shiki's `defaultColor="light-dark()"` resolves against.

**Files:**
- Modify: `src/styles/index.css`

**Interfaces:**
- Consumes: nothing.
- Produces: nothing importable.

- [ ] **Step 1: Add the declarations**

In the `:root` block, alongside the existing `@apply h-full;`:

```css
color-scheme: light;
```

In the `.dark` block:

```css
color-scheme: dark;
```

- [ ] **Step 2: Run the test to verify it passes**

Run: `npx vitest run src/styles/tests/tokenParity.test.ts`

Expected: all three cases PASS.

- [ ] **Step 3: Commit**

```bash
git add src/styles/index.css
git commit -m "0 client - Declare color-scheme so browser-native UI follows the theme"
```

---

### Task 5: Make `system` follow the OS live

The provider reads `matchMedia` once at mount and never subscribes, so changing the OS theme does nothing until reload.

**Files:**
- Create: `src/shared/providers/tests/theme-provider.test.tsx`
- Modify: `src/shared/providers/theme-provider.tsx:33-47`

**Interfaces:**
- Consumes: nothing.
- Produces: `ThemeProvider` and `useTheme` keep their existing signatures — `useTheme(): {theme: ThemeType; setTheme: (theme: ThemeType) => void}`, `ThemeType = 'dark' | 'light' | 'system'`.

- [ ] **Step 1: Write the failing test**

Create `src/shared/providers/tests/theme-provider.test.tsx`:

```tsx
import {ThemeProvider} from '@/shared/providers/theme-provider';
import {act, render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const changeListeners = new Set<(event: MediaQueryListEvent) => void>();

let systemPrefersDark = false;

const setSystemPrefersDark = (prefersDark: boolean) => {
    systemPrefersDark = prefersDark;

    changeListeners.forEach((listener) => listener({matches: prefersDark} as MediaQueryListEvent));
};

beforeEach(() => {
    changeListeners.clear();

    systemPrefersDark = false;

    localStorage.clear();

    document.documentElement.className = '';

    vi.stubGlobal('matchMedia', (query: string) => ({
        addEventListener: (_event: string, listener: (event: MediaQueryListEvent) => void) => {
            changeListeners.add(listener);
        },
        get matches() {
            return systemPrefersDark;
        },
        media: query,
        removeEventListener: (_event: string, listener: (event: MediaQueryListEvent) => void) => {
            changeListeners.delete(listener);
        },
    }));
});

describe('ThemeProvider', () => {
    it('applies the resolved system theme on mount', () => {
        render(
            <ThemeProvider defaultTheme="system">
                <div />
            </ThemeProvider>
        );

        expect(document.documentElement.classList.contains('light')).toBe(true);
    });

    it('follows the OS theme while the setting is system', () => {
        render(
            <ThemeProvider defaultTheme="system">
                <div />
            </ThemeProvider>
        );

        act(() => setSystemPrefersDark(true));

        expect(document.documentElement.classList.contains('dark')).toBe(true);
        expect(document.documentElement.classList.contains('light')).toBe(false);
    });

    it('ignores OS changes while the setting is an explicit theme', () => {
        render(
            <ThemeProvider defaultTheme="light">
                <div />
            </ThemeProvider>
        );

        act(() => setSystemPrefersDark(true));

        expect(document.documentElement.classList.contains('light')).toBe(true);
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/shared/providers/tests/theme-provider.test.tsx`

Expected: FAIL on "follows the OS theme while the setting is system" — the class stays `light` because nothing subscribes. The other two cases pass.

- [ ] **Step 3: Implement the subscription**

Replace the `useEffect` in `src/shared/providers/theme-provider.tsx` (currently lines 33–47) with:

```tsx
useEffect(() => {
    const root = window.document.documentElement;
    const darkModeQuery = window.matchMedia('(prefers-color-scheme: dark)');

    const applyTheme = () => {
        root.classList.remove('light', 'dark');

        if (theme === 'system') {
            root.classList.add(darkModeQuery.matches ? 'dark' : 'light');
        } else {
            root.classList.add(theme);
        }
    };

    applyTheme();

    if (theme !== 'system') {
        return;
    }

    darkModeQuery.addEventListener('change', applyTheme);

    return () => darkModeQuery.removeEventListener('change', applyTheme);
}, [theme]);
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `npx vitest run src/shared/providers/tests/theme-provider.test.tsx`

Expected: all three cases PASS.

- [ ] **Step 5: Commit**

```bash
git add src/shared/providers/theme-provider.tsx src/shared/providers/tests/theme-provider.test.tsx
git commit -m "0 client - Subscribe to OS theme changes while the theme setting is system"
```

---

### Task 6: Make the `useTheme` guard reachable

`initialState.setTheme` is a no-op and the context defaults to a valid object, so `context === undefined` can never be true — a component calling `useTheme` outside a provider silently gets a broken setter instead of an error.

**Files:**
- Modify: `src/shared/providers/theme-provider.tsx:16-21,64-70`
- Test: `src/shared/providers/tests/theme-provider.test.tsx` (from Task 5)

**Interfaces:**
- Consumes: `ThemeProviderStateI` from Task 5's file, unchanged.
- Produces: `useTheme` now throws `Error('useTheme must be used within a ThemeProvider')` when called outside a provider. Its return type is unchanged for all in-provider callers.

- [ ] **Step 1: Add the failing test**

Append to the `describe('ThemeProvider', ...)` block in `src/shared/providers/tests/theme-provider.test.tsx`:

```tsx
    it('throws when useTheme is called outside a ThemeProvider', () => {
        const ThemeConsumer = () => {
            useTheme();

            return null;
        };

        expect(() => render(<ThemeConsumer />)).toThrow('useTheme must be used within a ThemeProvider');
    });
```

Update the import at the top of the file to pull in `useTheme`:

```tsx
import {ThemeProvider, useTheme} from '@/shared/providers/theme-provider';
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/shared/providers/tests/theme-provider.test.tsx -t 'throws when useTheme'`

Expected: FAIL — nothing is thrown, because the context resolves to `initialState`.

- [ ] **Step 3: Make the context default undefined**

In `src/shared/providers/theme-provider.tsx`, delete the `initialState` constant entirely and change the context creation:

```tsx
const ThemeProviderContext = createContext<ThemeProviderStateI | undefined>(undefined);
```

The `useTheme` guard at the bottom of the file already reads correctly and needs no change:

```tsx
export const useTheme = () => {
    const context = useContext(ThemeProviderContext);

    if (context === undefined) throw new Error('useTheme must be used within a ThemeProvider');

    return context;
};
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `npx vitest run src/shared/providers/tests/theme-provider.test.tsx`

Expected: all four cases PASS.

- [ ] **Step 5: Check nothing depended on the silent fallback**

Run: `npx vitest run && npx tsc --project tsconfig.json --noEmit`

Expected: PASS. `src/components/ui/sonner.tsx` calls `useTheme` — confirm `Toaster` is rendered inside `ThemeProvider` in both `src/main.tsx` and `src/workflow-builder.tsx`. If any test renders `Toaster` bare, wrap it in `ThemeProvider`.

- [ ] **Step 6: Commit**

```bash
git add src/shared/providers/theme-provider.tsx src/shared/providers/tests/theme-provider.test.tsx
git commit -m "0 client - Make the useTheme outside-provider guard reachable"
```

---

### Task 7: Prevent the theme flash, and default to `system`

The existing inline script in `index.html` is `type="module"`, which is implicitly deferred and runs after parsing — too late to stop a white flash. And `defaultTheme="light"` in `main.tsx` means `system` never applies for a user who has not chosen.

**Files:**
- Modify: `index.html:3-17`
- Modify: `src/main.tsx:78`

**Interfaces:**
- Consumes: the localStorage key `bytechef.ui-theme`, written by `ThemeProvider` (Task 5). The key string must match `storageKey`'s default in `theme-provider.tsx` exactly.
- Produces: `documentElement` already carries `light` or `dark` before React mounts, so `ThemeProvider`'s first effect is a no-op rather than a visible swap.

- [ ] **Step 1: Add the blocking pre-paint script**

In `index.html`, add this as the **last element inside `<head>`**, after the existing `<meta name="theme-color">`. It must be a plain `<script>` — **not** `type="module"`, which would defer it and defeat the purpose.

```html
<script>
    (function () {
        try {
            var stored = localStorage.getItem('bytechef.ui-theme') || 'system';
            var resolved =
                stored === 'system'
                    ? window.matchMedia('(prefers-color-scheme: dark)').matches
                        ? 'dark'
                        : 'light'
                    : stored;

            document.documentElement.classList.add(resolved);

            var themeColorMeta = document.querySelector('meta[name="theme-color"]');

            if (themeColorMeta) {
                themeColorMeta.setAttribute('content', resolved === 'dark' ? '#030712' : '#ffffff');
            }
        } catch (error) {
            document.documentElement.classList.add('light');
        }
    })();
</script>
```

The `try`/`catch` matters: `localStorage` throws in some privacy modes and in sandboxed iframes, and an uncaught throw here would block the whole page. `#030712` is the dark `--background` (`224 71.4% 4.1%`); `#ffffff` is the light one.

Leave the existing `type="module"` script in `<body>` — it handles the embedded `bg-transparent` case and is unrelated.

- [ ] **Step 2: Change the default theme**

In `src/main.tsx` line 78:

```tsx
<ThemeProvider defaultTheme="system">
```

Leave `src/workflow-builder.tsx:40` as `defaultTheme="light"` — that is the deferred editor surface.

- [ ] **Step 3: Verify manually**

```bash
npm run dev
```

1. Open the app, set the OS to dark, and confirm no white flash on hard reload (`Cmd+Shift+R`).
2. In DevTools, run `localStorage.setItem('bytechef.ui-theme', 'dark')` and reload — no flash.
3. Confirm `<html>` carries exactly one of `light` / `dark` and never both.
4. Confirm scrollbars and any native `<select>` render dark — this verifies Task 4's `color-scheme`.

- [ ] **Step 4: Commit**

```bash
git add index.html src/main.tsx
git commit -m "0 client - Prevent theme flash on load and default to the system theme"
```

---

### Task 8: Phase 1 visual review

The token values in Tasks 2 and 3 were chosen for ramp consistency, not by eye. This is where they get judged.

**Files:** none modified unless the review finds problems.

- [ ] **Step 1: Run the full check**

Run: `npm run check`

Expected: PASS.

- [ ] **Step 2: Review in dark mode**

Run `npm run dev`, enable `ff-445` (already on in `application-local.yml`), and visit Account → Appearance to switch to dark. Then walk:

| Surface | Watch for |
|---|---|
| App sidebar and headers | `--stroke-neutral-secondary` borders — the 74-usage fix, should read as subtle, not invisible or glaring |
| Any list or table page | Row dividers, skeleton loaders |
| A destructive confirm dialog | `bg-surface-destructive-secondary` + `text-content-destructive-primary` — this is the AA fix from Task 3 |
| A warning alert | `--surface-warning-secondary`, `--stroke-warning-secondary` |
| Scrollbars, any `<select>` | Native widgets follow the theme — verifies Task 4 |
| A code block (AI Hub thread) | Shiki resolves its dark theme — also Task 4 |

- [ ] **Step 3: Review in light mode**

Switch back to light and confirm nothing changed. Phase 1 touches only `.dark` values, dead tokens, and `color-scheme: light` — light mode should be pixel-identical. Any visible light-mode difference is a regression, most likely a dead-token deletion that was not actually dead.

- [ ] **Step 4: Adjust and commit if needed**

If a value reads wrong, adjust it in `index.css`, keeping the ramp logic from Task 2's table intact.

```bash
git add src/styles/index.css
git commit -m "0 client - Adjust dark theme token values after visual review"
```

**Phase 1 is now shippable on its own.** Dark mode renders correctly for the 3445 token-based usages — roughly three-quarters of the client.

---

# Phase 2 — Dialog conversion

15 dialogs hand-roll their modal shell. All 15 fail every accessibility check: no `Escape`, no `role="dialog"`, no `aria-modal`, no portal, no focus trap, no scroll lock. A keyboard user cannot close any of them.

## Conversion recipe

**This recipe is the shared content for Tasks 9–23. Every task in Phase 2 applies it — read it here rather than looking at a neighbouring task.**

### Imports

Remove `XIcon` from the `lucide-react` import if it was only used for the hand-rolled close button — `DialogContent` renders its own. Add, respecting alphabetical destructure order:

```tsx
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Textarea} from '@/components/ui/textarea';
```

Import only what the file actually uses — ESLint runs with `--max-warnings=0`.

**Select comes from `@/components/Select/Select`, not `@/components/ui/select`.** `eslint-restricted-imports.mjs:29` bans the `ui` path, and the reason is behavioural rather than stylistic: the wrapper defaults `SelectContent` to `position="popper"` so the dropdown renders below the trigger at the trigger's width. Export names are identical, so it is a drop-in swap. Repo usage confirms the convention — 60 files import the wrapper, 3 the raw primitive. `ui/button`, `ui/badge`, and `ui/switch` are restricted the same way; `ui/dialog`, `ui/input`, `ui/textarea`, and `ui/label` are not, and are correct as written above.

### Shell

These dialogs are conditionally rendered by their parent and have no `open` prop. Preserve that: hard-code `open` and route dismissal through the existing `onClose`.

```tsx
// Before
<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
    <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
        <div className="mb-4 flex items-center justify-between">
            <h3 className="text-lg font-medium">Title</h3>

            <button onClick={onClose}>
                <XIcon className="size-4" />
            </button>
        </div>

        {/* body */}

        <div className="mt-6 flex justify-end gap-2">{/* actions */}</div>
    </div>
</div>

// After
<Dialog
    onOpenChange={(open) => {
        if (!open) {
            onClose();
        }
    }}
    open
>
    <DialogContent className="max-w-md">
        <DialogHeader>
            <DialogTitle>Title</DialogTitle>
        </DialogHeader>

        {/* body */}

        <DialogFooter>{/* actions */}</DialogFooter>
    </DialogContent>
</Dialog>
```

The scrim, the panel wrapper, and the `mb-4`/`mt-6` spacing divs all disappear — `DialogContent`, `DialogHeader`, and `DialogFooter` supply them.

**Two corrections to an earlier version of this recipe, both found by the Task 9 pilot review:**

**The close button is NOT automatic.** `dialog.tsx:63` defaults `showCloseButton = false` in this repo. Deleting the hand-rolled `<button><XIcon/></button>` therefore removes a real affordance unless something replaces it. The rule:

- Dialog has a footer with a `Cancel` / dismiss button → deleting the X is fine, leave `showCloseButton` unset.
- Dialog has **no** footer dismiss button → pass `showCloseButton` explicitly, or the only remaining dismissals are Escape and overlay-click. Repo precedent: `AiHubAddMcpServerDialog.tsx:65`, `AiHubConnectConnectionDialog.tsx:108`.

`AiObservabilityWebhookDeliveriesDialog` (Task 10) is exactly this case — one button, the close X, no footer.

**Describe the dialog, or opt out explicitly.** Radix sets `aria-describedby={descriptionId}` on `DialogContent` unconditionally. With no `DialogDescription` rendered, that is a dangling idref pointing at an element that does not exist, and this Radix version emits no warning about it — the warning components were removed, so their silence proves nothing.

These dialogs have no existing subtitle text, and inventing description copy for fifteen of them is content authorship outside this plan's scope. So opt out explicitly:

```tsx
<DialogContent aria-describedby={undefined} className="max-w-md">
```

Where a dialog already has descriptive text in its body, prefer rendering it as `DialogDescription` instead. Writing real descriptions for the rest is tracked as follow-up work.

### Form controls

```tsx
// label -> Label. Always pair htmlFor with the control's id; that pairing is
// what makes getByLabelText work in the tests below.
<label className="mb-1 block text-sm font-medium">Name</label>
<Label className="mb-1 block" htmlFor="unique-field-id">Name</Label>

// input -> Input. Drop the hand-rolled className; Input carries its own token-based styling.
<input className="w-full rounded-md border px-3 py-2 text-sm" onChange={...} value={...} />
<Input id="unique-field-id" onChange={...} value={...} />

// textarea -> Textarea
<textarea className="w-full rounded-md border px-3 py-2 text-sm" rows={3} onChange={...} value={...} />
<Textarea id="unique-field-id" onChange={...} rows={3} value={...} />

// button -> Button, from @/components/Button/Button (NOT @/components/ui/button)
<button onClick={handleSave}>Save</button>
<Button label="Save" onClick={handleSave} />
```

Keep `fieldset className="border-0"` wrappers — they are the house convention for form grouping.

**Group captions become `<legend>`, not `Label`.** Some `<label>` elements caption a *group* of controls (a checkbox list, a set of derived values) rather than one control. A `Label` with no `htmlFor` carries label semantics with nothing to label, and forces a `getByText` assertion that passes against the old markup too. Use the native element instead:

```tsx
<fieldset className="border-0">
    <legend className="mb-1 block text-sm font-medium">Events</legend>
    ...checkboxes...
</fieldset>
```

`fieldset` has the implicit ARIA role `group` and `legend` supplies its accessible name, so the test both discriminates and asserts something real:

```tsx
expect(screen.getByRole('group', {name: 'Events'})).toBeInTheDocument();
```

A bare `<label>` never names a group, so this fails against the unconverted markup.

**Test render helper: import `render` from `@/shared/util/test-utils`,** not `@testing-library/react`. The repo's `customRender` already wraps `QueryClientProvider` and `ThemeProvider` (the latter added in Task 6), so tests need no manual provider scaffolding. It is also the repo-wide majority — 170 files against 109.

Tasks 9-13 predate this decision and wrap `QueryClientProvider` by hand. They pass and are left alone; retrofitting nine working test files for style would be scope creep. The inconsistency is recorded for final-review triage.

### `<select>` — the one real API change

`onChange(event)` becomes `onValueChange(value)`, and `<option>` becomes `SelectItem`. There is no `event.target`.

```tsx
// Before
<select
    className="w-full rounded-md border px-3 py-2 text-sm"
    onChange={(event) => setProviderId(event.target.value)}
    value={providerId}
>
    <option value="">Select a provider</option>

    {providers.map((provider) => (
        <option key={provider.id} value={provider.id}>
            {provider.name}
        </option>
    ))}
</select>

// After
<Select onValueChange={setProviderId} value={providerId}>
    <SelectTrigger id="unique-field-id">
        <SelectValue placeholder="Select a provider" />
    </SelectTrigger>

    <SelectContent>
        {providers.map((provider) => (
            <SelectItem key={provider.id} value={provider.id}>
                {provider.name}
            </SelectItem>
        ))}
    </SelectContent>
</Select>
```

Two traps:
- **Radix `SelectItem` forbids `value=""`.** A placeholder `<option value="">` becomes `SelectValue`'s `placeholder` prop, not an item. Rendering an empty-valued item throws at runtime.
- **`disabled` moves to `Select`**, not `SelectTrigger`.

### Per-file test

Every converted dialog gets a test file at `<its directory>/tests/<Name>.test.tsx`. The `role="dialog"` and Escape cases are the ones that fail before conversion.

```tsx
import {fireEvent, render, screen} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {describe, expect, it, vi} from 'vitest';

import ExampleDialog from '../ExampleDialog';

// Mock every GraphQL hook the component imports. Match the real names exactly.
vi.mock('@/shared/middleware/graphql', () => ({
    useCreateExampleMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateExampleMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const renderDialog = (onClose = vi.fn()) => {
    render(
        <QueryClientProvider client={new QueryClient()}>
            <ExampleDialog onClose={onClose} workspaceId="1" />
        </QueryClientProvider>
    );

    return onClose;
};

describe('ExampleDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    // Asserts the title is the DIALOG's accessible name, not merely that a heading exists.
    // `getByRole('heading', ...)` would pass against the old hand-rolled `<h3>` too, so it
    // proves nothing about the conversion — the Task 9 pilot review caught exactly that.
    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'Create Example'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
    });
});
```

For dialogs with a `<select>`, assert the control is a **Radix** trigger, not merely a combobox:

```tsx
    it('renders the scope control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Scope')).toHaveAttribute('data-slot', 'select-trigger');
    });
```

**Do NOT use `getByRole('combobox')` here.** A native `<select>` already has the implicit ARIA role `combobox` (aria-query maps any `<select>` without `multiple` or `size > 1` to it), so that assertion passes identically against the unconverted markup and proves nothing. `getByText('Traces')` is equally useless — Testing Library matches `<option>` text whether or not the dropdown is open.

`data-slot="select-trigger"` is set only by the Radix trigger (`components/ui/select.tsx:35`) and can never appear on a native `<select>`, so it discriminates. Routing the query through `getByLabelText` also pins the `htmlFor`/`id` wiring in the same assertion.

The Task 10-13 batch review caught this; the original recipe used the combobox form.

Radix `Select` needs `ResizeObserver` and `matchMedia`, both already polyfilled in `.vitest/setup.ts`.

### Per-task step sequence

Tasks 9–23 all follow the same five steps. Substitute the file and its control counts:

1. Write the test file (recipe above), run it, watch `role="dialog"` and Escape fail
2. Apply the shell conversion
3. Convert the form controls
4. Run the test — all cases pass — then `npx tsc --project tsconfig.json --noEmit`
5. Commit

---

### Task 9: Convert AiPromptDialog (pilot)

The smallest dialog with a representative control mix. **Do this one first and review it before starting Tasks 10–23** — it establishes the pattern every other task copies.

**Files:**
- Modify: `src/pages/automation/ai/gateway/components/prompts/AiPromptDialog.tsx` (109 lines)
- Create: `src/pages/automation/ai/gateway/components/prompts/tests/AiPromptDialog.test.tsx`

Controls: 1 `<input>`, 1 `<textarea>`, 1 raw `<button>` (the close X — deleted, not converted), 2 `<label>`. No `<select>`.

**Interfaces:**
- Consumes: `Dialog`, `DialogContent`, `DialogFooter`, `DialogHeader`, `DialogTitle` from `@/components/ui/dialog`; `Input` from `@/components/ui/input`; `Label` from `@/components/ui/label`; `Textarea` from `@/components/ui/textarea`; `Button` from `@/components/Button/Button`.
- Produces: `AiPromptDialogProps` is unchanged — `{onClose: () => void; prompt?: AiPromptType; workspaceId: string}`. The parent's conditional rendering keeps working untouched.

- [ ] **Step 1: Write the failing test**

Create `src/pages/automation/ai/gateway/components/prompts/tests/AiPromptDialog.test.tsx`:

```tsx
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import AiPromptDialog from '../AiPromptDialog';

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateAiPromptMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAiPromptMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const renderDialog = (onClose = vi.fn()) => {
    render(
        <QueryClientProvider client={new QueryClient()}>
            <AiPromptDialog onClose={onClose} workspaceId="1" />
        </QueryClientProvider>
    );

    return onClose;
};

describe('AiPromptDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('renders an accessible title', () => {
        renderDialog();

        expect(screen.getByRole('heading', {name: 'Create Prompt'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Description (optional)')).toBeInTheDocument();
    });

    it('disables the submit button until a name is entered', () => {
        renderDialog();

        expect(screen.getByRole('button', {name: 'Create'})).toBeDisabled();

        fireEvent.change(screen.getByLabelText('Name'), {target: {value: 'My Prompt'}});

        expect(screen.getByRole('button', {name: 'Create'})).toBeEnabled();
    });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/pages/automation/ai/gateway/components/prompts/tests/AiPromptDialog.test.tsx`

Expected: FAIL on "renders with the dialog role", "renders an accessible title", "closes on Escape", and "associates every label with its control". The disabled-button case passes — that behaviour already works.

- [ ] **Step 3: Rewrite the component**

Replace the whole of `src/pages/automation/ai/gateway/components/prompts/AiPromptDialog.tsx` with:

```tsx
import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useCreateAiPromptMutation, useUpdateAiPromptMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useState} from 'react';

import {AiPromptType} from '../../types';

interface AiPromptDialogProps {
    onClose: () => void;
    prompt?: AiPromptType;
    workspaceId: string;
}

const AiPromptDialog = ({onClose, prompt, workspaceId}: AiPromptDialogProps) => {
    const [description, setDescription] = useState(prompt?.description ?? '');
    const [name, setName] = useState(prompt?.name ?? '');

    const queryClient = useQueryClient();

    const isEditMode = !!prompt;

    const createMutation = useCreateAiPromptMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiPrompts']});

            onClose();
        },
    });

    const updateMutation = useUpdateAiPromptMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiPrompts']});

            onClose();
        },
    });

    const handleSubmit = useCallback(() => {
        if (isEditMode) {
            updateMutation.mutate({
                id: prompt.id,
                input: {
                    description: description || undefined,
                    name,
                },
            });
        } else {
            createMutation.mutate({
                input: {
                    description: description || undefined,
                    name,
                    workspaceId,
                },
            });
        }
    }, [createMutation, description, isEditMode, name, prompt, updateMutation, workspaceId]);

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle>{isEditMode ? 'Edit Prompt' : 'Create Prompt'}</DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-prompt-name">
                            Name
                        </Label>

                        <Input
                            id="ai-prompt-name"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="My Prompt Template"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-prompt-description">
                            Description (optional)
                        </Label>

                        <Textarea
                            id="ai-prompt-description"
                            onChange={(event) => setDescription(event.target.value)}
                            placeholder="Describe what this prompt does..."
                            rows={3}
                            value={description}
                        />
                    </fieldset>
                </div>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!name || createMutation.isPending || updateMutation.isPending}
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiPromptDialog;
```

- [ ] **Step 4: Run the test and typecheck**

Run: `npx vitest run src/pages/automation/ai/gateway/components/prompts/tests/AiPromptDialog.test.tsx`

Expected: all five cases PASS.

Run: `npx tsc --project tsconfig.json --noEmit`

Expected: PASS.

- [ ] **Step 5: Verify manually, then commit**

`npm run dev`, open the AI Gateway prompts page, create a prompt. Check: Escape closes; clicking the overlay closes; Tab cycles inside the dialog and does not escape to the page behind; focus returns to the triggering button on close; the page behind does not scroll.

```bash
git add src/pages/automation/ai/gateway/components/prompts/AiPromptDialog.tsx \
        src/pages/automation/ai/gateway/components/prompts/tests/AiPromptDialog.test.tsx
git commit -m "0 client - Convert AiPromptDialog to the shadcn Dialog primitive"
```

---

### Tasks 10–23: Convert the remaining 14 dialogs

Each follows the conversion recipe and the five-step sequence above. Ordered smallest-first so the pattern is well-worn before the large ones.

| Task | File (relative to `src/`) | select | input | button | textarea | label |
|---|---|---|---|---|---|---|
| 10 | `pages/automation/ai/gateway/components/exports/AiObservabilityWebhookDeliveriesDialog.tsx` | 0 | 0 | 1 | 0 | 0 |
| 11 | `pages/automation/ai/gateway/components/prompts/AiPromptDetail.tsx` | 0 | 0 | 4 | 0 | 0 |
| 12 | `pages/automation/ai/gateway/components/scores/AiEvalRules.tsx` | 0 | 2 | 2 | 0 | 2 |
| 13 | `pages/automation/ai/gateway/components/exports/AiObservabilityExportJobDialog.tsx` | 2 | 0 | 3 | 0 | 2 |
| 14 | `pages/automation/ai/gateway/components/routing/AiGatewayRoutingPolicyDialog.tsx` | 1 | 2 | 1 | 0 | 3 |
| 15 | `pages/automation/ai/gateway/components/providers/AiGatewayProviderDialog.tsx` | 1 | 3 | 1 | 0 | 4 |
| 16 | `pages/automation/ai/gateway/components/prompts/AiPromptVersionDialog.tsx` | 2 | 2 | 1 | 1 | 6 |
| 17 | `pages/automation/ai/gateway/components/exports/AiObservabilityWebhookSubscriptionDialog.tsx` | 0 | 5 | 3 | 0 | 6 |
| 18 | `pages/automation/ai/gateway/components/scores/AiEvalScoreConfigDialog.tsx` | 1 | 4 | 3 | 1 | 6 |
| 19 | `pages/automation/ai/gateway/components/scores/AiEvalRuleDialog.tsx` | 1 | 5 | 2 | 1 | 7 |
| 20 | `pages/automation/ai/gateway/components/models/AiGatewayModelDialog.tsx` | 2 | 6 | 1 | 0 | 8 |
| 21 | `pages/automation/ai/gateway/components/alerts/AiObservabilityAlertRuleDialog.tsx` | 2 | 6 | 1 | 0 | 9 |
| 22 | `pages/automation/ai/gateway/components/projects/AiGatewayProjectDialog.tsx` | 0 | 10 | 1 | 0 | 10 |
| 23 | `pages/settings/platform/workflow-alerts/components/WorkflowAlertRuleDialog.tsx` | 1 | 7 | 1 | 0 | 9 |

For each task N:

- [ ] **Step 1:** Create `<directory>/tests/<Name>.test.tsx` using the per-file test template from the recipe. Mock every hook the component imports from `@/shared/middleware/graphql` — read the component's imports first and match the names exactly. Include the label case for each `<label>` in the table above, and the combobox case if `select > 0`.
- [ ] **Step 2:** Run the test. Expect FAIL on the dialog-role, title, Escape, and label cases.
- [ ] **Step 3:** Apply the shell conversion from the recipe.
- [ ] **Step 4:** Convert the form controls per the counts in the table. For any file with `select > 0`, re-read the `<select>` section of the recipe — the `value=""` placeholder trap is the most likely source of a runtime error.
- [ ] **Step 5:** Run `npx vitest run <test path>` then `npx tsc --project tsconfig.json --noEmit`. Both must pass.
- [ ] **Step 6:** Commit with `git commit -m "0 client - Convert <Name> to the shadcn Dialog primitive"`.

**Two files need extra care:**

- **Task 11, `AiPromptDetail.tsx`** — this is a detail *page* containing an inline dialog, not a dialog component. Convert only the modal region (the `fixed inset-0 … bg-black/50` block and its contents); leave the page around it alone.
- **Task 12, `AiEvalRules.tsx`** — likewise a list component with an inline dialog. Same rule.

---

### Task 24: Phase 2 verification

**Files:** none modified unless problems are found.

- [ ] **Step 1: Verify every hand-rolled scrim is gone**

```bash
grep -rn "fixed inset-0.*bg-black/" src --include="*.tsx" | grep -v "components/ui/"
```

Expected: exactly one line — `src/shared/components/DialogLoader.tsx`. That one is a loading overlay, not a dialog, and stays as it is.

- [ ] **Step 2: Verify the accessibility defects are closed**

```bash
grep -rln "role=\"dialog\"\|DialogContent" $(grep -rl "onClose" src/pages/automation/ai/gateway/components --include="*Dialog*.tsx") | wc -l
```

Expected: every gateway dialog file is listed.

- [ ] **Step 3: Run the full check**

Run: `npm run check`

Expected: PASS. `vitest.config.ts` declares no coverage thresholds, so coverage is reported but cannot fail the run — any failure here is a real lint, type, or test failure.

- [ ] **Step 4: Manual keyboard pass**

Open each of the 15 dialogs and confirm, using only the keyboard: `Escape` closes; `Tab` cycles within the dialog and never reaches the page behind; focus returns to the trigger on close; the background does not scroll. For each converted `<select>`, submit the form and confirm the value reaches the mutation — this is the one behavioural change in Phase 2.

- [ ] **Step 5: Re-measure Phase 3's inputs**

Phase 2 changed the numbers Phase 3 was scoped against. Capture the new ones so the Phase 3 plan is written against reality:

```bash
PAL='\b(bg|text|border|fill|stroke|ring|divide|from|to|via)-(white|black|gray-[0-9]+|slate-[0-9]+|zinc-[0-9]+|neutral-[0-9]+|stone-[0-9]+|blue-[0-9]+|red-[0-9]+|green-[0-9]+|yellow-[0-9]+|orange-[0-9]+|purple-[0-9]+|indigo-[0-9]+|emerald-[0-9]+|amber-[0-9]+|sky-[0-9]+|teal-[0-9]+|violet-[0-9]+|pink-[0-9]+|rose-[0-9]+|cyan-[0-9]+|lime-[0-9]+|fuchsia-[0-9]+)\b'
echo "files remaining : $(grep -rlE "$PAL" src --include='*.tsx' --include='*.ts' --include='*.css' | grep -v 'workflow-editor\|cluster-element-editor' | grep -v '\.test\.\|\.stories\.' | wc -l)"
echo "occurrences     : $(grep -rhoE "$PAL" src --include='*.tsx' --include='*.ts' --include='*.css' | wc -l)"
```

Record both numbers in the Phase 3 plan when it is written.

- [ ] **Step 6: Commit any fixes**

```bash
git add -A
git commit -m "0 client - Fix issues found in dialog conversion verification"
```

---

## Self-Review

**Spec coverage.** Every Phase 1 and Phase 2 requirement in `2026-08-02-dark-mode-design.md` maps to a task:

| Spec section | Task |
|---|---|
| §1.1 missing `.dark` overrides (9) | 1, 2 |
| §1.1 dead tokens (6) | 2, 3 |
| §1.1 re-derived copied-from-light values (6) | 3 |
| §1.1 `color-scheme` | 4 |
| §1.2 `matchMedia` subscription bug | 5 |
| §1.2 unreachable context guard | 6 |
| §1.3 FOUC script, `theme-color` | 7 |
| §1.4 `defaultTheme="system"` | 7 |
| §2.1 accessibility defects | 9–23 |
| §2.2 shell + 170 control swaps | 9–23 |
| §2.3 effect on Phase 3 | 24 step 5 |
| §2.4 verification | 24 |

Phase 3 sections (§3.1–3.4) are deliberately unmapped — see Scope.

**Placeholder scan.** No "TBD", no "add appropriate error handling", no "similar to Task N". The Phase 2 recipe is a self-contained shared section rather than a cross-reference, so a worker starting at Task 17 has the full pattern without reading Task 9.

**Type consistency.** `ThemeType`, `ThemeProviderStateI`, `useTheme`'s return shape, and `AiPromptDialogProps` are used identically across Tasks 5, 6, 7, and 9. The `bytechef.ui-theme` storage key is the same string in Task 7's inline script and `theme-provider.tsx`'s `storageKey` default. `Button` resolves to `@/components/Button/Button` everywhere, per Global Constraints.

**One known gap, deliberate.** Task 2's nine token values and Task 3's six re-derived values are engineering judgements from ramp consistency, not design decisions. Task 8 is the gate where they get looked at and adjusted. If a designer is available, route Task 8 through them.
