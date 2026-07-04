# ByteChef MCP App — Workflow Viewer

Read-only workflow canvas that renders inside MCP Apps hosts (Claude.ai, Claude Desktop, …)
while the ByteChef MCP server builds workflows. The widget is a pure projection of tool
results: the host pushes each workflow tool result to the iframe (`app.ontoolresult`), the
widget reads `structuredContent.definition` (the nested workflow definition) and re-renders.
It makes no tool calls and no fetches other than component-icon `<img>` loads.

Ships as ONE self-contained HTML file (`vite-plugin-singlefile`) served by the automation
MCP server as the `ui://bytechef/workflow-editor` resource.

## Commands

```bash
npm install
npm run dev      # dev server; opening the page directly renders the bundled fixture
npm run build    # typecheck + emit dist/index.html (single file)
```

Dev harness: with the fixture rendered, simulate a host-pushed update from the console:

```js
window.__pushToolResult({structuredContent: {definition: {/* nested workflow definition */}}});
```

Icon base URL: `VITE_ICON_BASE_URL` at build time (defaults to the local
bytechef-github-proxy, `http://localhost:6123/integration/component-icons`). The chosen host
must appear in the `img-src` of the CSP declared in the workflow tools' `_meta.ui`.

## Provenance — this is a PORT (third copy). Keep it in sync.

`src/workflow-graph/` is copied from the **bytechef-website** repo,
`app/(marketing)/workflow-templates/[templateSlug]/workflow-graph/`, which is itself a
store-free/session-free port of the ByteChef client's read-only workflow editor
(`client/src/pages/platform/workflow-editor/`). The website folder's README carries the
file-by-file provenance table back to the client sources — that table applies to this copy
verbatim. When dispatcher rendering changes in the client editor, mirror the change in the
website port and here.

Adaptations applied on top of the website port:

- `'use client'` directives removed (no Next.js).
- `WorkflowDefinitionType` inlined to `src/types.ts` (was `../../types` in the website app).
- `ComponentImage` inlined to `src/components/ComponentImage.tsx` (was `template-card.tsx`);
  same loading-spinner / generic-glyph-fallback semantics.
- `buildIconUrl` inlined to `src/iconUrl.ts` with a build-time injectable base URL
  (was `utils.ts` with `NEXT_PUBLIC_BYTECHEF_PROXY_API_URL`).
- Tailwind theme reduced to the five design tokens the graph uses (`tailwind.config.ts` +
  variables in `src/app.css`, ByteChef light theme only).

Carried-over v1 limitation: AI-agent cluster canvases render as plain read-only nodes.
