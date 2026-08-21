# Slack in-place approval interactivity — design

Status: IMPLEMENTED (phase 1 + phase 2 — in-place buttons, signature-verified endpoint, message
rewrite, and the discard-with-comment modal). The connection-schema decision was made: the Slack
connection carries an optional `signingSecret`. Implemented pieces: `SlackConstants.SIGNING_SECRET`
on the connection, `SlackApprovalChannel` in-place `block_actions` buttons
(`approval_approve`/`approval_discard`, value = tokenized resume id) when the secret is set, and
`SlackInteractivityController`/`SlackInteractivityHandler` in platform-webhook-rest-impl
(anonymous path `/slack/interactivity`, permit-listed in
`WebhookAuthorizeHttpRequestContributor`) verifying the signature per tenant (anchored by the
resume id) against all Slack connections carrying a secret, resolving through `JobResumeFacade`,
and rewriting the message via `response_url`. Phase 2: a Discard click opens a comment modal via
`views.open` (using the verifying connection's bot token; requires the `views:write` scope),
carrying the resume id + `response_url` in the view's `private_metadata`; the `view_submission`
callback resolves with `{approved: false, comment}`. When no modal can be opened (no bot token,
missing scope, transport error) the discard falls back to immediate resolution so it is never
lost. The reviewer's Slack username — verified by the same signature check — is passed through as
the resolver identity. The Discord/WhatsApp variants remain open — see Later.

## Goal

Resolve an approval directly inside Slack: the Approve/Discard buttons act in place (no browser
tab), and the message rewrites itself to "Approved by @name at 12:04" so the channel shows the
outcome and the buttons stop being actionable. Today the Slack channel sends URL buttons that
open the hosted form with the decision pre-selected — one confirm click, scanner-safe, but it
leaves Slack.

## Current state

- `SlackApprovalChannel` posts a `section` block plus an `actions` block with two URL buttons
  (`?approved=true|false` on the hosted form). URL buttons never call back into Slack, so no
  endpoint exists and none is needed.
- Resolution goes through `POST /job/resume/{id}` (tokenized, anonymous-capable, first response
  wins, expiry-checked in `JobResumeFacadeImpl`).

## Constraint that blocks a drop-in implementation

In-place buttons are `block_actions` interactivity: Slack POSTs a signed payload to a single
**Request URL configured on the Slack app**, and the receiver MUST verify the
`X-Slack-Signature` HMAC using the app's **signing secret**.

- The ByteChef Slack connection stores only a bot token. There is nowhere to put the signing
  secret today, so an interactivity endpoint could not verify payloads — an unverified public
  endpoint that resolves approvals is not acceptable.
- The Request URL is per Slack app. Users on self-hosted deployments must configure it manually
  in their app settings; there is no API to set it.

Hence the product decision: extend the Slack connection definition with an optional
`signingSecret` property (breaking nothing — absent secret simply keeps URL-button behavior).

## Proposed design

1. **Connection**: add optional `signingSecret` to the Slack connection definition. Persisted
   encrypted like all connection parameters.
2. **Channel**: `SlackApprovalChannel` switches to in-place buttons ONLY when the connection has
   a signing secret; otherwise it keeps today's URL buttons. In-place buttons carry
   `action_id: approval_resolve`, `value: {"resumeId": "<tokenized id>", "approved": true|false}`
   (the same tokenized resume id the form URL carries — no new secret material in Slack).
3. **Endpoint**: `POST /api/slack/interactivity` (platform-webhook-rest, anonymous like the
   resume endpoint). Steps:
   - Parse `payload` (form-encoded JSON), extract team/app identifiers.
   - Resolve candidate signing secrets: look up Slack connections carrying a signing secret
     (worst case a handful per tenant); verify `X-Slack-Signature` =
     `v0=HMAC-SHA256(secret, "v0:{timestamp}:{rawBody}")`, reject if older than 5 minutes
     (replay window). Constant-time compare.
   - On `block_actions` with `action_id: approval_resolve`: call
     `JobResumeFacade.resumeJob(resumeId, {approved, comment: null})`. Map outcomes: resolved →
     rewrite; GONE/expired/already-resolved → rewrite with the terminal state.
   - Respond via the payload's `response_url` (`replace_original: true`) with a plain section:
     "✅ Approved by <@user>" / "🚫 Discarded by <@user>" / "⌛ This approval already expired."
     `response_url` needs no token and is valid 30 minutes — no bot-token lookup required.
4. **Comment support**: optional round 2 — a `Discard with comment` overflow action opening a
   Slack modal (`views.open` needs the bot token; available via the connection that delivered
   the message — carry the connection id inside the button `value`).
5. **Audit/metrics**: count `bytechef_approval_resolution{approved}` as today (the resume facade
   already does); add `source=slack_interactivity` tag if per-surface attribution is wanted.

## Failure modes

- Signing secret misconfigured → verification fails → 401, Slack shows a warning to the
  clicking user; URL-button fallback remains available by clearing the secret.
- Multi-workspace: signature verification identifies the app, not the tenant; the tokenized
  resumeId inside the payload is the tenant anchor (same model as the MCP secret-key URLs).
- First-response-wins: a second click after resolution gets the terminal-state rewrite, mirroring
  the hosted form's "no longer available".

## Later

Discord and WhatsApp in-place resolution are **explicitly deferred** (2026-07-22 product
decision) because both require new infrastructure we chose not to take on:

- **Discord** — an interactions endpoint with Ed25519 public-key verification is straightforward,
  but Discord caps a button's `custom_id` at 100 characters while the default (signed) resume
  token is ~108. Carrying it would need either a short-id → token store or threading the raw
  (unsigned) inner token into the channel-delivery path — infra changes out of scope for now.
- **WhatsApp** — Meta allows only one webhook URL per app, so interactive-button taps arrive on the
  existing WhatsApp *trigger* webhook rather than a dedicated resolution endpoint; resolving
  approvals from there means entangling with trigger routing.

Until that infra exists, Discord and WhatsApp keep their current behavior: Approve/Discard buttons
(or links) that open the hosted approval form, which resolves the same approval. No in-place
resolution for these channels.
