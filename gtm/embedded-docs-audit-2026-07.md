# Embedded Getting-Started / Quickstart Audit

**Date:** July 28, 2026 · **Scope:** the embedded quickstart + getting-started docs as the week-1–2 asset of the GTM embedded sprint (strategy §7.1). Findings verified against the code on branch `0_732`.

## Headline status

**The embedded quickstart already exists and is good — it's just not published.** The live docs site (docs.bytechef.io/embedded/quickstart, built from `master`) serves a stub: *"This is currently in development, contact support@bytechef.io."* Branch `0_732` contains the real thing: `docs/content/docs/embedded/quickstart.mdx` (214 lines, 9 steps, 5 screenshots) plus ~29 embedded doc files `master` doesn't have — the getting-started trilogy (`installing-the-sdk`, `adding-an-integration`, `displaying-the-connect-dialog`), the sample-app walkthrough, settings/signing-keys pages, field mapping, and the "Enterprise iPaaS" concept pages.

**Sprint task therefore changes from "write the quickstart" to "finish + publish":**
- If `0_732` merges to `master` soon, publishing solves itself.
- If not, cherry-pick the embedded docs to `master` as a docs-only change — they document the product, not this branch's error-handling feature work, and shouldn't wait on a 1,100+-commit branch.

## Verified accurate (checked against server/ee/libs/embedded + the SDK)

The documented lifecycle matches the code end to end:

| Step | Verified detail |
|---|---|
| Signing keys | RSA-2048; private key shown exactly once; `kid` = TenantKey (doubles as tenant anchor) |
| JWT contract | RS256, `kid` header, `sub` = your user id, short TTL; verified against `EmbeddedApiKeyAuthenticationConverter` |
| Connected users | **Auto-created on first authenticated request** — there is no create endpoint. Under-documented; add an explicit callout (every integrating engineer will go looking for the API) |
| Connect dialog | `useConnectDialog` renders a React portal (not an iframe), drives OAuth via popup, calls the public API directly with the JWT |
| Instances / workflows | `POST /api/embedded/v1/integrations/{id}/instances` is the "connect" call; enable/inputs/options endpoints all real |
| Triggering | `POST /api/embedded/v1/app-events` (async fan-out, no request body — payload delivery genuinely "coming soon") and `POST /api/embedded/v1/workflows/{workflowUuid}` (sync) |
| Workflow builder | `EmbeddedWorkflowBuilder` IS an iframe (`/embedded/workflow-builder/{uuid}`), EMBED_READY/EMBED_INIT handshake |

## Ship-blockers (fix before the page goes live / before any design-partner sees it)

1. **`@bytechef/embedded` is not on public npm** — registry returns 404; the SDK (v0.1.0) publishes only to a local Verdaccio registry. Step 6 (`npm install @bytechef/embedded`) fails for every external reader. → Publish to npm. Also fix on the way: `library/package.json` has `"main": "eslint.config.js"` (copy-paste bug) and the README references npm scripts (`yalc:*`) that don't exist.
2. ~~**Quickstart step 2 is wrong about API keys.**~~ **FIXED on this branch (2026-07-28).** It said to use the API key on `/api/embedded/internal` endpoints; the security code makes that impossible (internal paths never match the API-key converter's `/v{n}/{externalUserId}/` pattern, and even on success the principal has zero authorities). Corrected in `quickstart.mdx` step 2 and in `settings/index.md`'s "Using the key" example — both now point at the public `/api/embedded/v1/{externalUserId}/…` routes and state the internal-endpoint restriction. The auto-provisioning callout for Connected Users was added to quickstart step 5 in the same pass.
3. ~~**Branding "next step" points at nothing.**~~ **FIXED on this branch (2026-07-28).** The quickstart's branding next-step now says branding customization is on the roadmap, and `white-label-execution.mdx` + `configuration-api.mdx` each carry a "Roadmap notice" callout separating shipped mechanics (connect dialog, workflow builder, public API, core resources) from planned conveniences (theme tokens, emails/webhooks from your domain, idempotency keys, cursor pagination). Decide separately whether to keep them in the nav for design partners.

## Strategic decision — RESOLVED (2026-08-06)

**Embedded stays EE.** Confirmed: every embedded module lives under `server/ee/` and there are none outside it, so the CE build genuinely does not contain the embedded iPaaS. The decision is to keep it that way and fix the *copy*, not the packaging boundary.

This collided with the Track B pillar "open source — validate everything before paying," which appeared in five places (strategy §3.2 pillar 1, §3.3 Track B statement, and Appendix A's hero, social-proof strip, and pillar 1). All five are now reworded: they lead with the four claims that remain true under EE — read the engine, run it yourself, validate before paying, no vendor-death risk — instead of leaning on the "open source" label for Track B. "Open source" is still used for Track A, where it is literally true.

"Validate before paying" is kept honest by a **free evaluation licence**, which needs no new code: `LicenceFileParser` already parses an `expiry` instant and a `features` list from the `.lic` file, so a time-limited full-featured licence is issuable today.

**Still open (pricing call, not a copy one):** the evaluation term — how long, and full-featured or capped. Appendix A's hero and its licensing FAQ both need it in one sentence; the placeholder is marked `[DECIDE: evaluation term …]` in the strategy doc.

## Engineering side-findings (file as tickets, not sprint work)

- The hand-written `/external/{externalUserId}/…` MCP instance routes bypass `FRONTEND_RESERVED_PATH_SEGMENTS`: a non-JWT bearer token mints a phantom ConnectedUser named `external` before authorization rejects the call (`McpIntegrationInstanceToolApiController`, `McpIntegrationInstanceWorkflowApiController`).
- Spec inconsistency: `POST .../workflows/{uuid}/enable` declares `bearerAuth` while its sibling `DELETE` declares `jwtBearerAuth` (embedded-configuration-public openapi.yaml) — one is wrong.
- Dead Swagger group `embedded-frontend` (`/api/embedded/frontend/v1/**` — no controller mounts there).
- `docs/content/docs/openapi/(generated)/embedded*` folders contain only `meta.json` — the public API specs aren't actually rendered into the docs site.
- Repo-internal demo host app `sdks/frontend/embedded/test-apps/` is unreferenced by any docs page — mention it or supersede it with the external sample app.

## Week-1–2 checklist (assets, in order)

1. Publish `@bytechef/embedded` to npm (+ package.json/README fixes).
2. Fix quickstart step 2 (API-key scope) and the branding next-step line; add the connected-user auto-provisioning callout.
3. Decide the publish path for the embedded docs (merge vs docs-only cherry-pick to `master`).
4. Mark/pull the aspirational pages (`white-label-execution`, `configuration-api`, `embedded-mcp`, `workflow-builder-tools` already say "coming soon" — keep that honesty).
5. Close remaining `TODO screenshot` placeholders in the deeper pages.
6. Then: landing page + outreach begin (strategy §7.1 steps 2–4).
