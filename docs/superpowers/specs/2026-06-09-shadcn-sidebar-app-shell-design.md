# Replace DesktopSidebar/MobileSidebar with the shadcn sidebar-07 app shell

Date: 2026-06-09

## Summary

Replace the two divergent app-shell sidebars — the fixed 56px icon rail
(`DesktopSidebar`) and the separate mobile `Dialog` (`MobileSidebar`) — with a single
sidebar built on shadcn/ui's `Sidebar` primitive in `collapsible="icon"` mode (the
"sidebar-07" block). The new sidebar defaults to the icon rail, can be toggled open to
reveal labels, group headers, and a richer footer, and on mobile renders as shadcn's
off-canvas `Sheet` driven by a single `SidebarTrigger`.

## Goals

- One sidebar component (`AppSidebar`) for desktop rail, desktop-expanded, and mobile.
- Preserve all current functionality: navigation items (icon + tooltip), the user menu,
  and the EE-only workspace / environment / platform-type selectors, settings, account,
  docs, and logout.
- Match the existing ByteChef theme (no shadcn default-gray sidebar).
- Install the primitive via the shadcn CLI, then reconcile generated files to repo
  conventions.

## Non-goals (YAGNI)

- Cross-reload persistence of the expand/collapse state (session-only; resets to
  collapsed on each load).
- Changing the navigation data model, feature-flag filtering, or routes (filtering stays
  in `App.tsx`; the sidebar stays presentational).
- Touching the Copilot right-panel, the embedded SDK shell, or page-level sub-sidebars
  (e.g. Projects/DataTables/AI-Hub rails).
- Restyling the top bar beyond swapping its menu button for `SidebarTrigger`.

## Decisions

| Decision | Choice |
| --- | --- |
| Class merging | Use `cn` (`@/shared/util/cn-utils`), matching every `components/ui/*` |
| Default state | Collapsed to the icon rail on every load |
| Persistence | None — toggle is session-only, resets to collapsed on reload |
| Mobile | Unified onto shadcn off-canvas `Sheet` via `SidebarTrigger` |
| Footer when collapsed | Only the user avatar; workspace/env/platform fold into its dropdown |
| Footer when expanded | User avatar + workspace/env/platform selectors + settings/account/docs/logout |
| Install method | `npx shadcn@latest add sidebar`, then adapt generated files |

## Context (current state)

- `DesktopSidebar.tsx` (+ `DesktopSidebar.css`): fixed icon rail, `hidden lg:flex`, no
  collapse; renders logo, nav icons w/ `sr-only` labels + tooltips, and
  `DesktopSidebarBottomMenu`.
- `DesktopSidebarBottomMenu.tsx` (~323 lines): user avatar dropdown, EE-only platform
  type / workspace / environment selectors, settings/account/docs links, logout.
- `MobileSidebar.tsx`: full-screen `Dialog`, `lg:hidden`, opened from a hamburger in
  `MobileTopNavigation`.
- Rendered in `App.tsx` (authenticated layout ~lines 350-381) as a `flex h-full` row:
  `<MobileSidebar/> <DesktopSidebar/>` then a content column containing
  `<MobileTopNavigation/>`, `<Outlet/>`, and the Copilot `<aside>`.
- Navigation arrays are built and feature-flag/edition/environment-filtered in `App.tsx`
  and passed to the sidebars as a `navigation` prop.
- `components.json` exists: `style: new-york`, `cssVariables: true`,
  `aliases.utils = @/shared/util/cn-utils`, `aliases.ui = @/components/ui`,
  `aliases.hooks = @/hooks` (**mismatch** — real hooks live in `@/shared/hooks`).
- Tailwind **v4** (hybrid: `tailwind.config.js` registers `hsl(var(--x))` colors AND
  `styles/index.css` uses `@theme`). `cn` exists at `@/shared/util/cn-utils`
  (`twMerge(clsx(...))`). `components/ui/` already has button, tooltip, sheet, skeleton,
  separator, input. No `sidebar.tsx`, no `useIsMobile`, no `--sidebar-*` tokens.

## Design

### 1. Foundation (install + reconcile)

1. **Fix the hooks alias first:** set `components.json` `aliases.hooks` to
   `@/shared/hooks` so the CLI writes `use-mobile.ts` into the repo's real hooks dir.
2. **Run `npx shadcn@latest add sidebar`.** This generates `components/ui/sidebar.tsx`
   (importing `cn` from `@/shared/util/cn-utils` per the utils alias) and
   `shared/hooks/use-mobile.ts`, and may add `--sidebar-*` tokens to `styles/index.css`
   and/or `tailwind.config.js`.
3. **Reconcile generated output:**
   - **Theme tokens:** the CLI emits gray (baseColor) `--sidebar`, `--sidebar-foreground`,
     `--sidebar-primary(-foreground)`, `--sidebar-accent(-foreground)`,
     `--sidebar-border`, `--sidebar-ring`. Remap these to existing ByteChef tokens
     (surface/content/stroke/ring families) so the sidebar matches the current theme in
     both light and dark. Ensure the `sidebar` color is registered (via
     `tailwind.config.js` `extend.colors` using `hsl(var(--sidebar...))`, matching the
     file's existing pattern) so `bg-sidebar`, `text-sidebar-foreground`,
     `bg-sidebar-accent`, `border-sidebar-border`, `ring-sidebar-ring` resolve.
   - **`useIsMobile` breakpoint:** set it to the `lg` breakpoint (1024px) to match the
     current `hidden lg:flex` / `lg:hidden` switch, so the desktop/mobile boundary does
     not move.
   - **Default collapsed + no persistence:** in the generated `SidebarProvider`, remove
     the `document.cookie` read/write so state is in-memory only, and consume it with
     `defaultOpen={false}` from `App.tsx`.
   - Keep shadcn's Ctrl/Cmd+B toggle shortcut (harmless, useful).
   - Run `npm run check`-level lint on the generated `sidebar.tsx`/`use-mobile.ts` and fix
     import ordering / naming to satisfy the repo's ESLint (the file is large; expect a
     few `bytechef/*` rule fixes).

### 2. `AppSidebar` component (replaces both old sidebars)

New `shared/layout/app-sidebar/AppSidebar.tsx`, built from the primitive with
`collapsible="icon"`. Props: `{navigation: NavItem[]}` (same shape as today:
`{name, href, icon}`), keeping it presentational. Composition:

- **`SidebarHeader`** — ByteChef logo. Collapsed → mark only; expanded → full logo.
- **`SidebarContent`** — a `SidebarGroup` → `SidebarMenu`; one `SidebarMenuItem` +
  `SidebarMenuButton` (as a react-router `Link`) per nav item: icon + label, active state
  from the current route. `SidebarMenuButton`'s built-in `tooltip` prop supplies the
  hover tooltip when collapsed (preserving today's icon+tooltip rail).
- **`SidebarFooter`** — extracted into `AppSidebarFooter.tsx`, reusing the existing data
  hooks/handlers from `DesktopSidebarBottomMenu`:
  - **Expanded:** user avatar + email, then the EE-only platform-type / workspace /
    environment selectors, then settings / account / docs / logout — each a
    `SidebarMenuButton` (selectors keep their `DropdownMenu`).
  - **Collapsed:** render **only the user avatar** as a single `SidebarMenuButton` whose
    `DropdownMenu` contains everything (platform/workspace/environment selectors + the
    settings/account/docs/logout actions). This keeps the rail clean.
  - Implemented by branching footer content on the provider's collapsed state (`useSidebar()`).

### 3. Layout & mobile (`App.tsx`)

- Wrap the authenticated layout in `<SidebarProvider defaultOpen={false}>`.
- Render `<AppSidebar navigation={filtered}/>` followed by `<SidebarInset>` containing the
  top bar, `<Outlet/>`, and the existing Copilot `<aside>` (unchanged).
- Put a `<SidebarTrigger/>` in the top bar. On desktop it toggles rail↔expanded; on mobile
  (`useIsMobile`) it opens the off-canvas `Sheet`. Remove `<MobileSidebar/>` and the
  bespoke hamburger; keep `MobileTopNavigation` but swap its menu button for
  `SidebarTrigger`.

### 4. Deletions

`DesktopSidebar.tsx`, `DesktopSidebar.css`, `MobileSidebar.tsx`,
`DesktopSidebarBottomMenu.tsx` (logic folded into `AppSidebarFooter`). Remove their
imports/usages from `App.tsx`.

### 5. Component boundaries

- `AppSidebar` (nav presentation) and `AppSidebarFooter` (account/selectors) are separate
  files with single responsibilities; both consume data via props/existing hooks, so each
  is independently testable.
- The `components/ui/sidebar.tsx` primitive is the only piece holding open/collapse +
  mobile state; `AppSidebar` and `App.tsx` read it via `useSidebar()` / `SidebarProvider`.

## Testing

- **`AppSidebar`:** renders every passed nav item; expanded shows text labels; collapsed
  hides labels and exposes tooltips; active route marked; `SidebarTrigger` toggles
  `SidebarProvider` state; mobile (`useIsMobile` mocked true) renders the `Sheet` variant.
- **`AppSidebarFooter`:** expanded shows the EE-gated selectors + actions; collapsed shows
  only the avatar whose dropdown exposes selectors + settings/account/docs/logout; EE
  gating respected (non-EE hides selectors).
- Tests use `@/shared/util/test-utils` (`render`/`screen`/`userEvent`), wrap in required
  providers (`TooltipProvider`, `SidebarProvider`, router), reset Zustand stores in
  `beforeEach`.
- Run `cd client && npm run check` (lint + typecheck + tests).

## Files touched

- Add: `client/src/components/ui/sidebar.tsx` (via CLI), `client/src/shared/hooks/use-mobile.ts` (via CLI)
- Add: `client/src/shared/layout/app-sidebar/AppSidebar.tsx`, `.../AppSidebarFooter.tsx` (+ tests)
- Modify: `client/components.json` (hooks alias), `client/src/styles/index.css` (+ `tailwind.config.js`) for `--sidebar-*` tokens, `client/src/App.tsx` (layout), `MobileTopNavigation` (trigger)
- Delete: `DesktopSidebar.tsx`, `DesktopSidebar.css`, `MobileSidebar.tsx`, `DesktopSidebarBottomMenu.tsx`

## Risks / open items

- **Hybrid Tailwind v4 + `tailwind.config.js`:** the shadcn CLI's token output may not
  land cleanly; the reconcile step must verify `bg-sidebar` et al. actually resolve before
  building `AppSidebar`.
- **Footer logic reuse:** `DesktopSidebarBottomMenu` is large and EE-gated; folding it into
  `AppSidebarFooter` while supporting a collapsed avatar-only dropdown is the most complex
  task and should be its own plan step with its own test.
- **CLI may modify shared primitives** (button/input) or add deps — review the CLI diff and
  keep only the sidebar-related changes.
