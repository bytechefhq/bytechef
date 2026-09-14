# docs.bytechef.io agent readiness: design

Status as of September 14, 2026. Is Agentic score 59/100. Scope: the five fixes the scan lists, done inside Fumadocs' own conventions so nothing the framework owns is touched.

## What the scan actually hit (verified against the live site)

| Check | Live behavior today | Cause in the code |
|---|---|---|
| Agent-friendly 404s | HTML request for `/platform/nope` returns 200 with the "Not Found" page. `Accept: text/markdown` for the same path returns 404 with an empty body. `/api/nope` returns 200 with the HTML app shell. | `app/(docs)/[...slug]/page.tsx` renders `<NotFound>` inline instead of calling `notFound()`, so Next never sets the status. `app/llms.mdx/[...slug]/route.ts` calls `notFound()`, which gives the status but no body. No route claims unknown `/api/*` paths. |
| Content without JavaScript | `/` is a 307 to `/platform`. On `/platform`, text is 2,076 chars of 50,406 script-free HTML, 4.1%. The article itself is 9.5%; the sidebar (17.7 KB) and inline SVG icons (20 KB) pull the page down. One H1, headings sequential. | The Quick Start page is mostly cards. Sidebar and icons are Fumadocs layout and stay. |
| OpenAPI spec published | Nothing at `/openapi.json` or `/api/openapi.yaml`. | The five public specs are read from `server/ee/libs` by `lib/openapi/index.ts` for the generated reference pages only. |
| JSON error responses | `/api/nope` answers with HTML. `/api/chat` GET answers 405 with an empty body. | No API 404 handler. |
| Markdown negotiation | Negotiation works: `proxy.ts` rewrites to `/llms.mdx/*` via `isMarkdownPreferred`. The markdown response carries only Next's RSC Vary list, no `Accept`. | The llms.mdx route sets `Content-Type` only. |

## Changes, in the scan's priority order

Each item names the file, the Fumadocs API it relies on, and what is deliberately not touched.

### 1. Real 404s with a markdown body

- `app/(docs)/[...slug]/page.tsx`: replace the inline `<NotFound>` return with `notFound()` from `next/navigation`. This is what Fumadocs' own `[...slug]/page.tsx` template does. `generateMetadata` already handles the missing page; it keeps returning the "Not Found" title.
- New `app/(docs)/not-found.tsx`: renders the existing `components/not-found.tsx` inside the docs layout, so the sidebar, header and theme stay exactly as on the current 404 page. Next serves it with HTTP 404. The commented-out Orama `getSuggestions` stays commented out; the suggestion box keeps its current "No Alternative Found" state.
- `components/not-found.tsx`: under the existing box, add a short list of recovery links: Quick Start (`/platform`), Reference (`/reference`), `/llms.txt`, `/llms-full.txt`, `/sitemap.xml`, `/openapi.json`, and www.bytechef.io. Same button and card styles from `fumadocs-ui`. No other visual change.
- `app/llms.mdx/[...slug]/route.ts`: instead of `notFound()`, return a `text/markdown` 404 whose body is `# 404: page not found`, the requested path, and the same recovery links as absolute URLs. Keep `generateStaticParams` and `revalidate = false` as they are.
- New `app/api/[...path]/route.ts`: every method answers JSON 404 for API paths no other route claims. Declared routes (`/api/search`, `/api/chat`, `/api/proxy`) are unaffected because Next matches them first.

Not touched: `lib/source.tsx`, `source.config.ts`, `proxy.ts` matching, the search and proxy routes.

### 2. Content without JavaScript on the landing page

- `content/docs/platform/index.mdx` (Quick Start, where `/` redirects): add two paragraphs of real content under the existing "Introduction" H2, roughly 700 characters: what ByteChef is (workflow automation and AI agents, Apache 2.0 core, self-host or cloud, embeddable), and how these docs are organized (Platform for using it, Reference for every component and the expression syntax, Developer Guide for extending it, API reference for the public REST APIs), each with a link. The cards stay. Measured target: above 5% on script-free HTML, which needs about 600 more characters of text with light markup. If the first pass lands under 5%, add a third paragraph on editions rather than removing sidebar or icon markup, which is Fumadocs layout.
- Nothing else. No change to the `/` redirect; that is a product decision, see below.

### 3. OpenAPI specs published

- New `lib/agent/openapi-specs.ts`: reads each YAML from the `SPECS` map already exported by `lib/openapi/index.ts`, parses it with `yaml` (add it as an explicit dependency; today it is only transitive), and prepends `https://app.bytechef.io` to `servers` the way the website does. The `embedded` spec is the default, matching www.bytechef.io.
- New `app/openapi.json/route.ts` and `app/openapi/[spec].json/route.ts` with `revalidate = false` and `generateStaticParams`, so the specs are baked at build time from the checked-out server tree, the same way the reference pages are. `Content-Type: application/openapi+json`.
- New `app/.well-known/api-catalog/route.ts`: RFC 9727 linkset listing all five specs.
- `next.config.ts` `headers()`: `Link` header on `/platform` (and `/`) with `rel="service-desc"` to `/openapi.json`, `rel="api-catalog"`, and `rel="llms-txt"`.

Not touched: `scripts/generate-openapi.mts`, `content/docs/openapi/*`, the fumadocs-openapi proxy.

### 4. JSON error responses

- Covered by the `app/api/[...path]/route.ts` handler in item 1. Body shape, the same as the website: `{"error": {"code": "not_found", "message": "...", "hint": "...", "documentation": "https://docs.bytechef.io/openapi.json"}}`.
- Not changed: `/api/chat` 405 on GET. It is a Next default for a POST-only handler; a JSON 405 can be added if the rescan flags it.

### 5. Vary: Accept on the markdown variant

- `app/llms.mdx/[...slug]/route.ts`: add `Vary: Accept, Accept-Encoding` next to `Content-Type`. This is the response the check inspects.
- `proxy.ts`: also set the header on both the rewrite and the pass-through response, so the intent is declared once at the edge. Known limit, identical to www.bytechef.io: Next replaces Vary on HTML page responses with its RSC list and drops `Accept`, so only the markdown variant will show it. The check reads the markdown response, so this is enough for the score.
- Keep `isMarkdownPreferred` and `rewritePath` from `fumadocs-core/negotiation`; no custom Accept parsing.

### Tests

- Add vitest as a devDependency with `npm test`, environment node, tests under `lib/agent/__tests__/`. They import only the new pure modules: the API error body, the OpenAPI transform (servers prefixed, every spec in the catalog), the 404 markdown body, and the Vary merge. Nothing imports `@/lib/source` or `.source/`, so the tests do not depend on `fumadocs-mdx` output and do not touch the `bun`-based lint script.

### Verification after implementation

Production build with `npm run build`, then `next start`, then:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000/platform/nope
curl -s -H "Accept: text/markdown" -D - http://localhost:3000/platform/nope
curl -s -H "Accept: text/markdown" -D - -o /dev/null http://localhost:3000/platform
curl -s -w "\n%{http_code}\n" http://localhost:3000/api/nope
curl -s http://localhost:3000/openapi.json | head -c 300
curl -s http://localhost:3000/.well-known/api-catalog
```

Plus the script-free HTML content ratio on `/platform`, and a check that the existing `/llms.txt`, `/llms-full.txt`, `/sitemap.xml`, `/static.json` and `/api/search?query=x` responses are byte-for-byte what they were.

## Decisions for Ivica

- `/` redirects to `/platform`. Scanners and agents follow it, so the fix targets `/platform`. Serving the Quick Start at `/` itself would remove the hop but changes the site's URL structure; not proposed here.
- Default spec at `/openapi.json`: `embedded`, to match www.bytechef.io. The catalog lists all five.
- The MCP check is out of scope for the docs site.
- Deploy is from the bytechef repo, so this ships with the next docs deploy of the branch it lands on.
