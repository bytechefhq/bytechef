# ByteChef MCP App — Custom Component Viewer

Read-only viewer that renders inside MCP Apps hosts (Claude.ai, Claude Desktop, …) while the
ByteChef **management** MCP server serves resources. The widget is a pure projection of a tool
result: the host pushes each matching tool result to the iframe (`app.ontoolresult`), the widget
reads `structuredContent.{name?, language?, source}` (language defaults to `java`). Backed by the `getCustomComponentSource` tool; renders source with highlight.js. It makes no tool calls and no network fetches.

Ships as ONE self-contained HTML file (`vite-plugin-singlefile`) served by the CE `McpAppViewer`
helper as the `ui://bytechef/custom-component-viewer` resource. The backing tool opts in via
`_meta.ui.resourceUri`; the server shapes the tool result into the `structuredContent` above
(see `ViewerToolMcpContributorConfiguration` / `CodeCustomComponentViewerMcpContributorConfiguration`).

## Commands

```bash
npm install
npm run dev      # dev server; opening the page directly renders the bundled fixture
npm run build    # typecheck + emit dist/index.html (single file)
```

Dev harness: with the fixture rendered, simulate a host-pushed update from the console:

```js
window.__pushToolResult({structuredContent: {/* the shape above */}});
```

The build is Node-gated and NOT part of the regular Gradle build; run the `buildCustomComponentViewer`
task (`npm run build`) then rebuild `platform-mcp-server-support`, whose `processResources` bundles
`dist/index.html` onto the classpath as `mcp-apps/custom-component-viewer.html`. When the bundle is absent the server
starts normally and simply does not serve this resource.
