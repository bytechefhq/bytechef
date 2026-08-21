# shadcn sidebar-07 App Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `DesktopSidebar` + `MobileSidebar` with one shadcn `Sidebar` (collapsible="icon"), defaulting to the icon rail, expandable to labels, unified onto an off-canvas Sheet on mobile.

**Architecture:** Install the shadcn `Sidebar` primitive via the CLI and reconcile it to ByteChef conventions (cn, theme tokens, no cookie persistence, default collapsed). Build a presentational `AppSidebar` (nav) + `AppSidebarFooter` (account/selectors, relocated from `DesktopSidebarBottomMenu`). Wire `App.tsx` with `SidebarProvider`/`SidebarInset` and a `SidebarTrigger` in the top bar.

**Tech Stack:** React 19, TypeScript, `@xyflow`-unrelated; shadcn/ui Sidebar, Radix, Tailwind v4 (hybrid `tailwind.config.js` + `@theme`), Zustand, lucide-react, Vitest + @testing-library/react.

---

## File Structure

| File | Responsibility | Change |
| --- | --- | --- |
| `client/components.json` | shadcn config | Fix `hooks` alias → `@/shared/hooks` |
| `client/src/components/ui/sidebar.tsx` | Sidebar primitive | Create via CLI, then reconcile |
| `client/src/shared/hooks/use-mobile.ts` | mobile breakpoint hook | Create via CLI, breakpoint=1024 |
| `client/src/styles/index.css` | `--sidebar-*` tokens | Add (mapped to existing tokens) |
| `client/tailwind.config.js` | `sidebar` colors | Add `sidebar` color group |
| `client/src/shared/layout/app-sidebar/AppSidebar.tsx` | nav presentation | Create |
| `client/src/shared/layout/app-sidebar/AppSidebarFooter.tsx` | account/selectors footer | Create (from DesktopSidebarBottomMenu) |
| `client/src/App.tsx` | layout | Wrap in SidebarProvider/Inset; trigger |
| `client/src/shared/layout/MobileTopNavigation.tsx` | top bar | Menu button → SidebarTrigger |
| deletions | — | DesktopSidebar.tsx, DesktopSidebar.css, MobileSidebar.tsx, DesktopSidebarBottomMenu.tsx |

Test command (single file): `cd client && npx vitest run <path>`. Lint: `cd client && npx eslint <path>`.

**Import-order note (verified against this repo's linter):** `bytechef/group-imports` puts `@/...` absolute imports FIRST (alphabetical), then external packages, then a blank line, then relative `../`/`./` imports. Always confirm with `npx eslint`.

---

## Task 1: Install the Sidebar primitive and reconcile to ByteChef

Foundation task (CLI-generated code — not red/green TDD). Verification is lint + typecheck + token presence.

**Files:** `components.json`, `src/components/ui/sidebar.tsx` (created), `src/shared/hooks/use-mobile.ts` (created), `src/styles/index.css`, `tailwind.config.js`.

- [ ] **Step 1: Point the hooks alias at the real hooks dir**

In `client/components.json`, change:
```json
    "hooks": "@/hooks"
```
to:
```json
    "hooks": "@/shared/hooks"
```

- [ ] **Step 2: Run the shadcn CLI**

Run: `cd client && npx shadcn@latest add sidebar`
- If it prompts to overwrite existing primitives (button/input/separator/sheet/skeleton/tooltip), choose **No / skip** for those — keep the repo's versions. Allow it to create `sidebar.tsx` and `use-mobile.ts`.
Expected: creates `src/components/ui/sidebar.tsx` and `src/shared/hooks/use-mobile.ts`. It imports `cn` from `@/shared/util/cn-utils` (per the utils alias).

- [ ] **Step 3: Review the CLI diff; revert unwanted changes**

Run: `git -C /Volumes/Data/bytechef/bytechef/.claude/worktrees/sleepy-pasteur-9eb8fc status` and `git diff`.
- Keep only: `sidebar.tsx`, `use-mobile.ts`, `components.json`, and (if the CLI added them) the `--sidebar-*` tokens. `git checkout` any unintended edits to other `components/ui/*` files.

- [ ] **Step 4: Set the mobile breakpoint to `lg` (1024)**

In `src/shared/hooks/use-mobile.ts`, set the breakpoint constant the CLI generated (commonly `MOBILE_BREAKPOINT = 768`) to `1024`, so the desktop/mobile boundary matches today's `hidden lg:flex` / `lg:hidden`:
```ts
const MOBILE_BREAKPOINT = 1024;
```

- [ ] **Step 5: Remove cookie persistence and confirm in-memory state**

In `src/components/ui/sidebar.tsx`, inside `SidebarProvider`, delete the line that writes the cookie on toggle (the `document.cookie = \`${SIDEBAR_COOKIE_NAME}=...\`` statement) and any `SIDEBAR_COOKIE_*` reads. The provider must derive its initial open state ONLY from the `defaultOpen` prop (no cookie). Leave the Ctrl/Cmd+B keyboard shortcut intact.

- [ ] **Step 6: Map sidebar theme tokens (replace shadcn gray)**

In `src/styles/index.css`, inside the `:root` (light) block, add these token aliases (they reference existing tokens, so the dark block inherits automatically through the indirection — do NOT duplicate in dark):
```css
        --sidebar: var(--muted);
        --sidebar-foreground: var(--foreground);
        --sidebar-primary: var(--primary);
        --sidebar-primary-foreground: var(--primary-foreground);
        --sidebar-accent: var(--accent);
        --sidebar-accent-foreground: var(--accent-foreground);
        --sidebar-border: var(--border);
        --sidebar-ring: var(--ring);
```
If the CLI inserted gray triplet values for any of these names, replace them with the `var(--...)` aliases above and delete any dark-block duplicates it added.

- [ ] **Step 7: Register the `sidebar` Tailwind colors**

In `client/tailwind.config.js`, inside `theme.extend.colors`, add (matching the file's existing `hsl(var(--x))` pattern):
```js
                sidebar: {
                    DEFAULT: 'hsl(var(--sidebar))',
                    accent: 'hsl(var(--sidebar-accent))',
                    'accent-foreground': 'hsl(var(--sidebar-accent-foreground))',
                    border: 'hsl(var(--sidebar-border))',
                    foreground: 'hsl(var(--sidebar-foreground))',
                    primary: 'hsl(var(--sidebar-primary))',
                    'primary-foreground': 'hsl(var(--sidebar-primary-foreground))',
                    ring: 'hsl(var(--sidebar-ring))',
                },
```

- [ ] **Step 8: Verify lint + typecheck + token wiring**

Run:
```bash
cd client
npx eslint src/components/ui/sidebar.tsx src/shared/hooks/use-mobile.ts
npm run typecheck
grep -n "sidebar" src/styles/index.css
grep -n "sidebar" tailwind.config.js
```
Expected: eslint 0 errors (fix any import-order/naming the linter reports), typecheck clean, and the `--sidebar-*` + `sidebar` color entries present.

- [ ] **Step 9: Commit**

```bash
git add client/components.json client/src/components/ui/sidebar.tsx client/src/shared/hooks/use-mobile.ts client/src/styles/index.css client/tailwind.config.js
git commit -m "client - Add shadcn sidebar primitive, mobile hook, and theme tokens"
```

---

## Task 2: Build `AppSidebar` (navigation presentation)

**Files:**
- Create: `client/src/shared/layout/app-sidebar/AppSidebar.tsx`
- Test: `client/src/shared/layout/app-sidebar/AppSidebar.test.tsx`

- [ ] **Step 1: Write the failing test**

Create `AppSidebar.test.tsx`:
```tsx
import {render, screen} from '@/shared/util/test-utils';
import {FolderIcon, MessagesSquareIcon} from 'lucide-react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it} from 'vitest';

import {SidebarProvider} from '@/components/ui/sidebar';
import {AppSidebar} from './AppSidebar';

const navigation = [
    {href: '/automation/ai-hub', icon: MessagesSquareIcon, name: 'AI Hub'},
    {href: '/automation/projects', icon: FolderIcon, name: 'Projects'},
];

const renderSidebar = (open = true) =>
    render(
        <MemoryRouter initialEntries={['/automation/projects']}>
            <SidebarProvider defaultOpen={open}>
                <AppSidebar navigation={navigation} />
            </SidebarProvider>
        </MemoryRouter>
    );

describe('AppSidebar', () => {
    it('renders a menu item for each navigation entry', () => {
        renderSidebar(true);

        expect(screen.getByRole('link', {name: 'AI Hub'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Projects'})).toBeInTheDocument();
    });

    it('links each item to its href', () => {
        renderSidebar(true);

        expect(screen.getByRole('link', {name: 'AI Hub'})).toHaveAttribute('href', '/automation/ai-hub');
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/shared/layout/app-sidebar/AppSidebar.test.tsx`
Expected: FAIL — cannot resolve `./AppSidebar`.

- [ ] **Step 3: Implement `AppSidebar`**

Create `AppSidebar.tsx`:
```tsx
import reactLogo from '@/assets/logo.svg';
import {
    Sidebar,
    SidebarContent,
    SidebarFooter,
    SidebarGroup,
    SidebarGroupContent,
    SidebarHeader,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
} from '@/components/ui/sidebar';
import {ForwardRefExoticComponent, SVGProps} from 'react';
import {Link, useLocation} from 'react-router-dom';

import {AppSidebarFooter} from './AppSidebarFooter';

export interface AppSidebarNavItemI {
    name: string;
    href: string;
    icon: ForwardRefExoticComponent<Omit<SVGProps<SVGSVGElement>, 'ref'>>;
}

interface AppSidebarPropsI {
    navigation: AppSidebarNavItemI[];
}

export function AppSidebar({navigation}: AppSidebarPropsI) {
    const {pathname} = useLocation();

    const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

    return (
        <Sidebar collapsible="icon">
            <SidebarHeader>
                <Link className="flex items-center justify-center py-2" to="/">
                    <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                </Link>
            </SidebarHeader>

            <SidebarContent>
                <SidebarGroup>
                    <SidebarGroupContent>
                        <SidebarMenu>
                            {navigation.map((item) => (
                                <SidebarMenuItem key={item.name}>
                                    <SidebarMenuButton asChild isActive={isActive(item.href)} tooltip={item.name}>
                                        <Link to={item.href}>
                                            <item.icon aria-hidden="true" />

                                            <span>{item.name}</span>
                                        </Link>
                                    </SidebarMenuButton>
                                </SidebarMenuItem>
                            ))}
                        </SidebarMenu>
                    </SidebarGroupContent>
                </SidebarGroup>
            </SidebarContent>

            <SidebarFooter>
                <AppSidebarFooter />
            </SidebarFooter>
        </Sidebar>
    );
}
```

NOTE: This imports `AppSidebarFooter` (Task 3). To make THIS task's test pass independently, create a minimal placeholder footer first IF Task 3 is not yet done:
```tsx
// temporary minimal AppSidebarFooter.tsx — replaced in Task 3
export function AppSidebarFooter() {
    return null;
}
```
If Task 3 is already implemented, skip the placeholder.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd client && npx vitest run src/shared/layout/app-sidebar/AppSidebar.test.tsx`
Expected: PASS (2 passing). Then `npx eslint src/shared/layout/app-sidebar/AppSidebar.tsx src/shared/layout/app-sidebar/AppSidebar.test.tsx` → 0 errors; fix import order if flagged.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/layout/app-sidebar/AppSidebar.tsx client/src/shared/layout/app-sidebar/AppSidebar.test.tsx
git commit -m "client - Add AppSidebar navigation component"
```

---

## Task 3: Build `AppSidebarFooter` (relocate DesktopSidebarBottomMenu)

The existing `DesktopSidebarBottomMenu` is already an avatar-trigger dropdown containing the platform/workspace/environment selectors + settings/account/docs/logout — exactly the collapsed "avatar-only" footer required. This task moves it into the app-sidebar folder unchanged in behavior, wrapped so it sits in `SidebarFooter`.

**Files:**
- Create: `client/src/shared/layout/app-sidebar/AppSidebarFooter.tsx` (content copied from `DesktopSidebarBottomMenu.tsx`)
- Test: `client/src/shared/layout/app-sidebar/AppSidebarFooter.test.tsx`

- [ ] **Step 1: Write the failing test**

Create `AppSidebarFooter.test.tsx`. Mock the data hooks so it renders without network. Verify the avatar trigger renders and opening it shows the account email + Log Out:
```tsx
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {AppSidebarFooter} from './AppSidebarFooter';

vi.mock('@/shared/middleware/graphql', () => ({
    useEnvironmentsQuery: () => ({data: {environments: []}}),
}));
vi.mock('@/shared/queries/automation/workspaces.queries', () => ({
    useGetUserWorkspacesQuery: () => ({data: []}),
}));

describe('AppSidebarFooter', () => {
    beforeEach(() => {
        useAuthenticationStore.setState({account: {email: 'user@localhost.com'}} as never);
        useApplicationInfoStore.setState({application: {edition: 'CE'}} as never);
    });

    it('renders the user avatar trigger', () => {
        render(
            <MemoryRouter>
                <AppSidebarFooter />
            </MemoryRouter>
        );

        // Avatar trigger button is present (User2 icon button)
        expect(screen.getByRole('button')).toBeInTheDocument();
    });

    it('shows the signed-in email and Log Out when opened', async () => {
        const user = userEvent.setup();

        render(
            <MemoryRouter>
                <AppSidebarFooter />
            </MemoryRouter>
        );

        await user.click(screen.getByRole('button'));

        expect(screen.getByText('user@localhost.com')).toBeInTheDocument();
        expect(screen.getByText('Log Out')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/shared/layout/app-sidebar/AppSidebarFooter.test.tsx`
Expected: FAIL — cannot resolve `./AppSidebarFooter`.

- [ ] **Step 3: Create `AppSidebarFooter` from `DesktopSidebarBottomMenu`**

Copy the entire contents of `client/src/shared/layout/desktop-sidebar/DesktopSidebarBottomMenu.tsx` into the new `client/src/shared/layout/app-sidebar/AppSidebarFooter.tsx`, with these changes:
- Rename the component `DesktopSidebarBottomMenu` → `AppSidebarFooter` and change `export default` to a **named export**: `export function AppSidebarFooter() { ... }`.
- Keep ALL existing logic (the stores, queries, effects, handlers, and the full `DropdownMenu` with platform/workspace/environment subs + settings/account/docs/logout) byte-for-byte.
- Do NOT change relative import depths incorrectly: the new file is at `shared/layout/app-sidebar/`, the same depth as `shared/layout/desktop-sidebar/`, so all `@/...` absolute imports are unchanged. (The original uses only `@/...` imports, so nothing relative needs fixing — verify.)

(The component already renders as `<DropdownMenu><DropdownMenuTrigger asChild><Avatar>…</Avatar></DropdownMenuTrigger>…`, which works in both collapsed and expanded sidebar footers.)

- [ ] **Step 4: Run test to verify it passes**

Run: `cd client && npx vitest run src/shared/layout/app-sidebar/AppSidebarFooter.test.tsx`
Expected: PASS (2 passing). Then `npx eslint` on both new files → 0 errors (fix import order if flagged).

- [ ] **Step 5: Replace the placeholder footer import in AppSidebar (if a placeholder was used in Task 2)**

If Task 2 created a placeholder `AppSidebarFooter`, ensure `AppSidebar.tsx` now imports the real one from `./AppSidebarFooter`. Re-run `AppSidebar.test.tsx` to confirm still green.

- [ ] **Step 6: Commit**

```bash
git add client/src/shared/layout/app-sidebar/AppSidebarFooter.tsx client/src/shared/layout/app-sidebar/AppSidebarFooter.test.tsx client/src/shared/layout/app-sidebar/AppSidebar.tsx
git commit -m "client - Add AppSidebarFooter (relocated from DesktopSidebarBottomMenu)"
```

---

## Task 4: Wire `App.tsx` layout and the SidebarTrigger

**Files:**
- Modify: `client/src/App.tsx` (authenticated layout, ~lines 350-381 + imports)
- Modify: `client/src/shared/layout/MobileTopNavigation.tsx`

- [ ] **Step 1: Update MobileTopNavigation to use SidebarTrigger**

Replace the menu `<Button>` in `MobileTopNavigation.tsx` with the shadcn `SidebarTrigger`, and drop the `setMobileMenuOpen` prop. New file:
```tsx
import {SidebarTrigger} from '@/components/ui/sidebar';

import reactLogo from '../../assets/logo.svg';

export function MobileTopNavigation() {
    return (
        <div className="lg:hidden">
            <div className="flex items-center justify-between bg-white px-4 py-2">
                <div>
                    <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                </div>

                <SidebarTrigger aria-label="Open sidebar" />
            </div>
        </div>
    );
}
```

- [ ] **Step 2: Update App.tsx imports**

In `client/src/App.tsx`:
- Remove the imports of `DesktopSidebar` and `MobileSidebar`.
- Add: `import {SidebarInset, SidebarProvider} from '@/components/ui/sidebar';` and `import {AppSidebar} from '@/shared/layout/app-sidebar/AppSidebar';`
- Keep the `MobileTopNavigation` import (its props changed).
- Remove any now-unused `mobileMenuOpen` / `setMobileMenuOpen` state if it is no longer referenced anywhere else in `App.tsx` (verify with a search before deleting).

- [ ] **Step 3: Replace the authenticated layout JSX**

Replace the authenticated `return` block (currently lines ~350-381) with:
```tsx
    return authenticated ? (
        <SidebarProvider defaultOpen={false}>
            <AppSidebar navigation={navigation} />

            <SidebarInset className="flex h-full min-w-0 flex-1 flex-col">
                <MobileTopNavigation />

                <div className="flex size-full">
                    <div className="flex h-full min-w-0 flex-1">
                        <Outlet />
                    </div>

                    {ai.copilot.enabled && (
                        <aside className="h-full shrink-0">
                            <Suspense fallback={null}>
                                <CopilotPanel open={copilotPanelOpen} />
                            </Suspense>
                        </aside>
                    )}
                </div>
            </SidebarInset>

            <Toaster />

            {ff_2396 && <GlobalSearchDialog onOpenChange={setSearchOpen} open={searchOpen} />}
        </SidebarProvider>
    ) : (
        <Outlet />
    );
```

- [ ] **Step 4: Typecheck + lint**

Run:
```bash
cd client
npm run typecheck
npx eslint src/App.tsx src/shared/layout/MobileTopNavigation.tsx
```
Expected: clean. Fix any unused-var (e.g. leftover `mobileMenuOpen`) or import-order issues.

- [ ] **Step 5: Commit**

```bash
git add client/src/App.tsx client/src/shared/layout/MobileTopNavigation.tsx
git commit -m "client - Wire AppSidebar into the app shell with SidebarProvider and trigger"
```

---

## Task 5: Delete the old sidebar files

**Files (delete):**
- `client/src/shared/layout/desktop-sidebar/DesktopSidebar.tsx`
- `client/src/shared/layout/desktop-sidebar/DesktopSidebar.css`
- `client/src/shared/layout/desktop-sidebar/DesktopSidebarBottomMenu.tsx`
- `client/src/shared/layout/MobileSidebar.tsx`

- [ ] **Step 1: Confirm no remaining references**

Run:
```bash
cd client
grep -rn "DesktopSidebar\|MobileSidebar\|DesktopSidebarBottomMenu" src --include=*.ts --include=*.tsx
```
Expected: no references except possibly the files themselves. If anything else imports them, stop and report (NEEDS_CONTEXT).

- [ ] **Step 2: Delete the files**

```bash
git rm client/src/shared/layout/desktop-sidebar/DesktopSidebar.tsx \
       client/src/shared/layout/desktop-sidebar/DesktopSidebar.css \
       client/src/shared/layout/desktop-sidebar/DesktopSidebarBottomMenu.tsx \
       client/src/shared/layout/MobileSidebar.tsx
```
(If the `desktop-sidebar/` directory is now empty, that's fine.)

- [ ] **Step 3: Typecheck**

Run: `cd client && npm run typecheck`
Expected: clean (no dangling imports).

- [ ] **Step 4: Commit**

```bash
git commit -m "client - Remove legacy DesktopSidebar and MobileSidebar"
```

---

## Task 6: Full check and manual verification

**Files:** none (verification only).

- [ ] **Step 1: Run the full client check**

Run: `cd client && npm run check`
Expected: PASS (lint + typecheck + all tests). Fix any fallout (import order, sort-keys, naming).

- [ ] **Step 2: Manual QA (running app — requires server + client)**

Start the app and verify:
- Authenticated app loads with the sidebar **collapsed to the icon rail** by default; icons show tooltips on hover.
- Clicking the `SidebarTrigger` (top bar) **expands** the sidebar to show labels + group; clicking again collapses it. Reloading returns to **collapsed** (no persistence).
- The footer **avatar** opens the dropdown with workspace / environment / platform-type selectors (EE) and settings / account / docs / logout; all navigate correctly.
- Theme matches (no shadcn default-gray panel); dark mode looks correct.
- On a narrow viewport (<1024px), the trigger opens the **off-canvas Sheet**; nav + footer work there; the old mobile dialog is gone.
- Ctrl/Cmd+B toggles the sidebar.

- [ ] **Step 3: Final commit (if Step 1 required fixes)**

```bash
git add -A
git commit -m "client - Lint/format fixes for shadcn sidebar app shell"
```

---

## Self-Review Notes

- **Spec coverage:** install via CLI + reconcile (Task 1: hooks alias, cookie removal, breakpoint, tokens, colors); `AppSidebar` nav with collapse tooltips (Task 2); footer = avatar-only dropdown reused from DesktopSidebarBottomMenu (Task 3); layout + unified mobile via SidebarTrigger/SidebarInset, default collapsed, no persistence (Task 4); deletions (Task 5); tests throughout + manual QA (Task 6). All spec sections covered.
- **Naming consistency:** `AppSidebar`, `AppSidebarFooter`, `AppSidebarNavItemI`, `SidebarProvider`/`SidebarInset`/`SidebarTrigger` used consistently across tasks.
- **Known irreducible-manual parts:** the CLI generates `sidebar.tsx` (can't be pasted) and Tailwind v4 token resolution / visual fidelity need the running app — captured as Task 1 verification + Task 6 manual QA, not as unit tests.
- **Footer reuse vs. expanded-label divergence:** the spec described expanded-mode selectors as separate buttons; this plan reuses the existing avatar-dropdown for BOTH modes (it already satisfies the user's "collapsed = only avatar" requirement and is far lower risk). If expanded-mode inline selectors are later desired, that's a follow-up.
