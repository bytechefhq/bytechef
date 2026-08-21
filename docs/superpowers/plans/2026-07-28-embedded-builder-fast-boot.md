# Embedded Builder Fast Boot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cut the embedded workflow builder's first paint by giving it a dedicated entry point on a new URL and lazy-loading Monaco and assistant-ui out of the editor's initial chunk.

**Architecture:** A third Vite entry (`workflow-builder.html`) mounts `EmbeddedWorkflowBuilderApp` directly with only the providers it needs; `SpaWebFilter` forwards the new `/embedded/builder/**` path to it while the legacy path keeps forwarding to `index.html`. Heavy deps split via lazy shim modules at their existing file boundaries so the 16+ import sites need no edits.

**Tech Stack:** Vite 8 (rolldown), React 19 `lazy`/`Suspense`, Spring `OncePerRequestFilter`, `@bytechef/embedded` SDK.

**Spec:** `docs/superpowers/specs/2026-07-28-embedded-builder-fast-boot-design.md`

## Global Constraints

- Branch: all work lands on `0_732` as fresh commits (never amend — user commits in parallel).
- The legacy `/embedded/workflow-builder/**` URL must keep working permanently via `index.html`.
- `EMBED_READY`/`EMBED_INIT` postMessage protocol unchanged.
- `@xyflow/react` stays statically imported (canvas is first paint).
- Client checks: `cd client && npm run check` must pass before every client commit; ESLint sort-keys is manual-fix.
- Server: `./gradlew spotlessApply` then targeted test task; never judge a Gradle run through a pipe — redirect to a file and check `$?`.
- Commit convention: client commits prefixed `client - `.

---

### Task 1: Type-only import fixes (unblocks all chunk splits)

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/stores/useWorkflowTestChatStore.ts:6`
- Modify: `client/src/shared/hooks/useAnalytics.ts:3`
- Modify: `client/src/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet.ts:14,81`

**Interfaces:**
- Produces: no runtime imports of `@assistant-ui/react`, `posthog-js`, or `monaco-editor` from these three modules; behavior identical.

- [ ] **Step 1: Make the two type-position imports type-only**

In `useWorkflowTestChatStore.ts` replace line 6:

```ts
import type {ThreadMessageLike} from '@assistant-ui/react';
```

In `useAnalytics.ts` replace line 3:

```ts
import type {PostHog} from 'posthog-js';
```

- [ ] **Step 2: Replace the monaco VALUE import with a local constant**

In `useWorkflowCodeEditorSheet.ts` delete line 14 (`import {MarkerSeverity} from 'monaco-editor';`) and add below the remaining imports:

```ts
// monaco-editor's MarkerSeverity.Error — inlined so this hook does not pull monaco into the initial chunk
const MARKER_SEVERITY_ERROR = 8;
```

Replace line 81:

```ts
const hasErrors = markers.some((marker) => marker.severity === MARKER_SEVERITY_ERROR);
```

(The `import type {editor}` on line 22 stays — type-only is free.)

- [ ] **Step 3: Verify**

Run: `cd client && npm run check 2>&1 | tail -5`
Expected: lint, typecheck, tests all pass.

- [ ] **Step 4: Commit**

```bash
git add client/src/pages/platform/workflow-editor/stores/useWorkflowTestChatStore.ts client/src/shared/hooks/useAnalytics.ts client/src/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet.ts
git commit -m "client - Make assistant-ui, posthog and monaco imports type-only outside their lazy boundaries"
```

---

### Task 2: Lazy Monaco behind the existing wrapper

**Files:**
- Create: `client/src/shared/components/MonacoEditorWrapperImpl.tsx` (git mv of `MonacoEditorWrapper.tsx`)
- Create: `client/src/shared/components/MonacoEditorWrapper.tsx` (new lazy shim)

**Interfaces:**
- Consumes: existing `MonacoEditorWrapper` props (default export, `Editor` props pass-through).
- Produces: identical default export; all 16 existing import sites untouched. Monaco and its workers load only when a wrapper actually renders.

- [ ] **Step 1: Move the real implementation**

```bash
cd client && git mv src/shared/components/MonacoEditorWrapper.tsx src/shared/components/MonacoEditorWrapperImpl.tsx
```

- [ ] **Step 2: Write the lazy shim at the old path**

Create `client/src/shared/components/MonacoEditorWrapper.tsx`:

```tsx
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {Suspense, lazy} from 'react';

import type {ComponentProps} from 'react';

const MonacoEditorWrapperImpl = lazy(() => import('@/shared/components/MonacoEditorWrapperImpl'));

/**
 * Lazy boundary for Monaco. The implementation (and monaco-editor + its workers)
 * loads only when an editor actually renders, keeping monaco out of every
 * initial chunk. All import sites keep using this path unchanged.
 */
const MonacoEditorWrapper = (props: ComponentProps<typeof MonacoEditorWrapperImpl>) => (
    <Suspense fallback={<MonacoEditorLoader />}>
        <MonacoEditorWrapperImpl {...props} />
    </Suspense>
);

export default MonacoEditorWrapper;
```

- [ ] **Step 3: Verify no import site broke and monaco left the graph**

Run: `cd client && npm run check 2>&1 | tail -5` — expected PASS.
Run: `npm run build 2>&1 | tail -3` then
`grep -L "monaco" dist/assets/main-*.js >/dev/null && echo "monaco not in main chunk"`
Expected: build succeeds; the main entry chunk contains no monaco module (monaco appears only in its own async chunk).

- [ ] **Step 4: Commit**

```bash
git add -A client/src/shared/components/
git commit -m "client - Lazy-load Monaco behind MonacoEditorWrapper"
```

---

### Task 3: Lazy copilot and test-chat panels (assistant-ui)

**Files:**
- Create: `client/src/shared/components/copilot/CopilotPanelImpl.tsx` (git mv of `CopilotPanel.tsx`)
- Create: `client/src/shared/components/copilot/CopilotPanel.tsx` (lazy shim)
- Modify: `client/src/pages/platform/workflow-editor/WorkflowEditorLayout.tsx:7,327`

**Interfaces:**
- Consumes: `CopilotPanel` default export props; `useWorkflowTestChatStore` state `workflowTestChatPanelOpen`.
- Produces: identical `CopilotPanel` default export; `WorkflowTestChatPanel` mounts only when its store says open. `@assistant-ui/react` loads on first panel open.

- [ ] **Step 1: Move CopilotPanel and write the shim**

```bash
cd client && git mv src/shared/components/copilot/CopilotPanel.tsx src/shared/components/copilot/CopilotPanelImpl.tsx
```

Create `client/src/shared/components/copilot/CopilotPanel.tsx`:

```tsx
import LoadingDots from '@/components/LoadingDots';
import {Suspense, lazy} from 'react';

import type {ComponentProps} from 'react';

const CopilotPanelImpl = lazy(() => import('@/shared/components/copilot/CopilotPanelImpl'));

/**
 * Lazy boundary for the copilot panel: @assistant-ui/react and the AG-UI client
 * load on first open instead of riding in the editor's initial chunk. Mount
 * sites already render this conditionally, so the chunk fetch happens on open.
 */
const CopilotPanel = (props: ComponentProps<typeof CopilotPanelImpl>) => (
    <Suspense
        fallback={
            <div className="flex size-full items-center justify-center p-4">
                <LoadingDots />
            </div>
        }
    >
        <CopilotPanelImpl {...props} />
    </Suspense>
);

export default CopilotPanel;
```

Note: `CopilotPanelBoundary` (React-19 unmount-throw catch) lives around mount sites already — the shim sits inside it, do not remove it.

- [ ] **Step 2: Gate WorkflowTestChatPanel on its open state**

In `WorkflowEditorLayout.tsx` replace line 7's static import:

```tsx
import {Suspense, lazy} from 'react';

const WorkflowTestChatPanel = lazy(
    () => import('@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel')
);
```

(merge the `lazy`/`Suspense` names into the existing `react` import line rather than adding a duplicate import), add alongside the other store reads (respecting hook ordering — store hooks before derived values):

```tsx
const workflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.workflowTestChatPanelOpen);
```

and replace line 327:

```tsx
{workflow.id && workflowTestChatPanelOpen && (
    <Suspense fallback={null}>
        <WorkflowTestChatPanel />
    </Suspense>
)}
```

The open-state check must sit OUTSIDE the lazy component — inside it, the chunk downloads on mount anyway.

- [ ] **Step 3: Verify**

Run: `cd client && npm run check 2>&1 | tail -5` — expected PASS (tests mocking `@/shared/components/copilot/CopilotPanel` keep working — the path is unchanged).
Run: `npm run build 2>&1 | tail -3` and confirm `@assistant-ui` appears only in async chunks (inspect `dist/.vite/manifest.json` or grep the entry chunk).

- [ ] **Step 4: Commit**

```bash
git add -A client/src/shared/components/copilot/ client/src/pages/platform/workflow-editor/WorkflowEditorLayout.tsx
git commit -m "client - Lazy-load the copilot and test-chat panels behind their open actions"
```

---

### Task 4: Dedicated workflow-builder entry point

**Files:**
- Create: `client/workflow-builder.html`
- Create: `client/src/workflow-builder.tsx`
- Modify: `client/vite.config.mts:22-25` (add input)
- Modify: `client/src/main.tsx:17,45-47` (dynamic main router)

**Interfaces:**
- Consumes: `EmbeddedWorkflowBuilderApp`, `WorkflowBuilder` (existing), providers from `main.tsx`.
- Produces: `/workflow-builder.html` entry that mounts the builder with no main-router graph, no devtools, no blocking `getApplicationInfo()`.

- [ ] **Step 1: Create the html entry**

`client/workflow-builder.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Workflow Builder - ByteChef</title>
</head>
<body>
    <div id="root"></div>
    <script type="module" src="/src/workflow-builder.tsx"></script>
</body>
</html>
```

- [ ] **Step 2: Create the entry module**

`client/src/workflow-builder.tsx`:

```tsx
import {createRoot} from 'react-dom/client';

import './styles/index.css';

import EmbeddedWorkflowBuilderApp from '@/EmbeddedWorkflowBuilderApp';
import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowBuilder from '@/ee/pages/embedded/workflow-builder/WorkflowBuilder';
import I18n from '@/i18n';
import {ThemeProvider} from '@/shared/providers/theme-provider';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {StrictMode} from 'react';
import {RouterProvider, createBrowserRouter} from 'react-router-dom';

const container = document.getElementById('root') as HTMLDivElement;
const root = createRoot(container);
const queryClient = new QueryClient();

// Deliberately NOT awaited: the builder renders its skeleton immediately and
// application info resolves in the background. This is the main first-paint win
// over the legacy index.html boot.
applicationInfoStore.getState().getApplicationInfo();

const router = createBrowserRouter([
    {
        children: [
            {
                element: <WorkflowBuilder />,
                path: 'builder/:workflowUuid',
            },
        ],
        element: <EmbeddedWorkflowBuilderApp />,
        path: '/embedded',
    },
]);

root.render(
    <StrictMode>
        <I18n>
            <ThemeProvider defaultTheme="light">
                <QueryClientProvider client={queryClient}>
                    <TooltipProvider>
                        <RouterProvider router={router} />
                    </TooltipProvider>
                </QueryClientProvider>
            </ThemeProvider>
        </I18n>
    </StrictMode>
);
```

Absent by design: `src/routes.tsx`, `ReactQueryDevtools`, `ConditionalPostHogProvider`, commandbar, userGuiding, account fetch, `/login` redirect.

- [ ] **Step 3: Register the input**

In `vite.config.mts` add to `rollupOptions.input` (alphabetical, sort-keys):

```ts
input: {
    connect: resolve(__dirname, 'connect.html'),
    main: resolve(__dirname, 'index.html'),
    workflowBuilder: resolve(__dirname, 'workflow-builder.html'),
},
```

- [ ] **Step 4: Make the main router import dynamic in main.tsx**

Delete line 17 (`import {getRouter as getMainRouter} from './routes';`) and change lines 45–47 to:

```ts
const router = isEmbeddedWorkflowBuilder
    ? (await import('@/embeddedWorkflowBuilderRoutes')).getRouter()
    : (await import('./routes')).getRouter(queryClient);
```

- [ ] **Step 5: Verify**

Run: `cd client && npm run check 2>&1 | tail -5` — PASS.
Run: `npm run build 2>&1 | tail -5` — three entries emitted. Then assert the new entry graph is clean:

```bash
node -e "
const m=require('./dist/.vite/manifest.json');
const seen=new Set();
(function walk(k){ if(seen.has(k))return; seen.add(k);
  (m[k]?.imports||[]).forEach(walk); })('workflow-builder.html');
const files=[...seen].map(k=>m[k].file);
const bad=files.filter(f=>/monaco|assistant-ui|posthog/.test(f));
console.log(bad.length? 'FAIL: '+bad.join(',') : 'workflow-builder entry clean ('+files.length+' chunks)');
process.exit(bad.length?1:0);"
```

Expected: `workflow-builder entry clean`. Dev-server spot check: `npm run dev`, open `https://localhost:3000/workflow-builder.html` — builder skeleton renders.

- [ ] **Step 6: Commit**

```bash
git add client/workflow-builder.html client/src/workflow-builder.tsx client/vite.config.mts client/src/main.tsx
git commit -m "client - Add a dedicated workflow-builder entry point and unhook the main router from the embedded boot"
```

---

### Task 5: SpaWebFilter forward for /embedded/builder

**Files:**
- Modify: `server/libs/config/security-config/src/main/java/com/bytechef/security/web/filter/SpaWebFilter.java:70-90`
- Test: `server/libs/config/security-config/src/test/java/com/bytechef/security/web/rest/filter/SpaWebFilterIntTest.java`

**Interfaces:**
- Produces: GET `/embedded/builder/{uuid}` forwards to `/workflow-builder.html`; every other SPA path keeps forwarding to `/index.html`.

- [ ] **Step 1: Write the failing tests**

Add to `SpaWebFilterIntTest`:

```java
@Test
void testFilterForwardsEmbeddedBuilderToWorkflowBuilderHtml() throws Exception {
    mockMvc.perform(get("/embedded/builder/0199f000-aaaa-bbbb-cccc-000000000001"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/workflow-builder.html"));
}

@Test
void testFilterStillForwardsLegacyWorkflowBuilderToIndex() throws Exception {
    mockMvc.perform(get("/embedded/workflow-builder/0199f000-aaaa-bbbb-cccc-000000000001"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
}
```

- [ ] **Step 2: Run to verify the first fails**

```bash
./gradlew :server:libs:config:security-config:test --tests '*SpaWebFilterIntTest*' > /tmp/spa-test.log 2>&1; echo "exit=$?"; grep -E "FAILED|passed" /tmp/spa-test.log | tail -3
```

Expected: exit non-zero; the new builder test fails (forwarded to `/index.html`).

- [ ] **Step 3: Implement the forward**

In `SpaWebFilter.doFilterInternal`, insert before the existing index.html branch:

```java
if (path.startsWith("/embedded/builder/") && !path.contains(".")) {
    RequestDispatcher requestDispatcher = request.getRequestDispatcher("/workflow-builder.html");

    requestDispatcher.forward(request, response);

    return;
}
```

(One blank line before the `if` per the control-statement convention.)

- [ ] **Step 4: Verify**

```bash
./gradlew spotlessApply :server:libs:config:security-config:test --tests '*SpaWebFilterIntTest*' > /tmp/spa-test2.log 2>&1; echo "exit=$?"; grep -cE "FAILED" /tmp/spa-test2.log
```

Expected: exit=0, zero FAILED.

- [ ] **Step 5: Commit**

```bash
git add server/libs/config/security-config/
git commit -m "Forward /embedded/builder to the dedicated workflow-builder entry"
```

---

### Task 6: SDK points at the fast path

**Files:**
- Modify: `sdks/frontend/embedded/library/src/components/embedded-workflow-builder/EmbeddedWorkflowBuilder.tsx:117`
- Modify: `sdks/frontend/embedded/library/package.json:3` (version)

**Interfaces:**
- Consumes: the new `/embedded/builder/{workflowUuid}` path from Task 5.
- Produces: SDK `0.2.0` whose iframe boots the fast entry; postMessage protocol byte-identical.

- [ ] **Step 1: Change the iframe src**

Line 117:

```tsx
src={`${baseUrl}/embedded/builder/${workflowUuid}`}
```

Update the component JSDoc's URL mention accordingly, and bump `"version": "0.2.0"` in `library/package.json`.

- [ ] **Step 2: Verify**

```bash
cd sdks/frontend/embedded/library && npm run lint 2>&1 | tail -2 && npm run test 2>&1 | tail -3 && npm run build 2>&1 | tail -2
```

Expected: all pass. Manual smoke via the workspace test-app (`npm run dev` at the workspace root): builder loads through the iframe on the new URL, `EMBED_INIT` still delivers the JWT.

- [ ] **Step 3: Commit**

```bash
git add sdks/frontend/embedded/library/
git commit -m "client - Point the embedded SDK iframe at the fast /embedded/builder entry"
```

---

### Task 7: End-to-end verification and docs touch-up

**Files:**
- Modify: `docs/superpowers/specs/2026-07-28-embedded-builder-fast-boot-design.md` (status: Implemented)

**Interfaces:**
- Consumes: everything above.

- [ ] **Step 1: Full client + server gates**

```bash
cd client && npm run check 2>&1 | tail -3
cd .. && ./gradlew spotlessApply check > /tmp/gradle-check.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/gradle-check.log | head
```

Expected: both clean.

- [ ] **Step 2: Boot comparison (manual, recorded in the spec)**

With the dev server and backend running: open `/embedded/workflow-builder/{uuid}` (legacy) and `/embedded/builder/{uuid}` (fast) with devtools network open. Record for each: transferred bytes to first canvas paint, and confirm monaco/assistant-ui chunks are absent until the code-editor sheet / copilot opens. Append the two numbers to the spec's Problem section as the measured result.

- [ ] **Step 3: Mark spec implemented and commit**

```bash
git add docs/superpowers/specs/2026-07-28-embedded-builder-fast-boot-design.md
git commit -m "Mark the embedded builder fast-boot spec implemented with measured results"
```
