# AI Hub voice — cost + compliance design

**Status:** Design | **Owner:** Ivica | **Created:** 2026-05-13 | **Last updated:** 2026-05-13

## Why

ByteChef has no visibility into per-workspace voice spend. Deepgram bills the customer's account directly; we proxy the audio but don't meter it. As voice usage grows this creates two problems:

1. **No cost-attribution** for multi-tenant deployments — customers can't see which user/task drives spend.
2. **No proactive guardrails** — workspaces can rack up bills until the provider account hits its hard limit.

Plus several compliance gaps around data residency, audio retention, and user consent that block voice for regulated industries.

This is **GA-readiness** scope, not pre-GA. Ship it once voice has real usage.

## Item 1 — Per-workspace usage metering

### What's missing

The v1.1 metrics (`bytechef_voice_session_total`, `bytechef_voice_session_duration_seconds`) are global. They don't break down by workspace. Adding a `workspace` tag to those counters would explode cardinality in multi-tenant deployments (one of the original v1 design decisions to avoid this).

### Change

Two new persistence-backed tables (not metrics — actual rows we can join with billing):

```sql
CREATE TABLE ai_hub_voice_session_log (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    ai_hub_task_id BIGINT,                -- null for Twilio synthetic tasks
    user_id BIGINT,                        -- null for Twilio inbound
    provider VARCHAR(32) NOT NULL,
    mode INT NOT NULL,                     -- VoiceProviderMode ordinal
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    duration_seconds INT,
    inbound_audio_bytes BIGINT,            -- approximate, for cost-modeling
    outbound_audio_bytes BIGINT,
    transcript_chars INT,                  -- for STT minute estimation
    assistant_text_chars INT,              -- for TTS character billing
    close_reason VARCHAR(64),              -- 'normal' | 'max_duration' | 'error' | 'provider_stall' | ...
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP NOT NULL
);

CREATE INDEX idx_ai_hub_voice_session_log_workspace_date
    ON ai_hub_voice_session_log(workspace_id, started_at);
```

Written by `AiHubVoiceWebSocketHandler` at session open (with `ended_at = null`) and updated at session close. Reads are by `(workspace_id, date_range)` for the admin UI.

### Admin UI

New view in Workspace Settings → AI Hub Voice → Usage:
- Daily/weekly/monthly minute totals.
- Top users by minutes consumed.
- Top tasks by minutes consumed.
- Mode breakdown (passthrough vs native LLM).
- Provider breakdown (when we ship OpenAI / ElevenLabs).
- Approximate cost (using provider price-per-minute as a workspace setting).

### Effort

~3 days (schema + persistence + admin UI).

## Item 2 — Per-workspace cost caps

### What's missing

Combined with metering, the natural next step is hard caps: "this workspace is capped at 1000 minutes/month of voice." Currently nothing enforces this beyond Deepgram's own account-level limit (which applies to all workspaces using the same connection, so it doesn't help multi-tenant deployments).

### Change

New workspace settings:

- `voice_monthly_minute_quota INT NULL` — null = no cap.
- `voice_monthly_minute_alert_threshold INT NULL` — fire a metric/event when usage hits this (e.g. 80% of quota).

Enforcement:

1. At session open, sum `duration_seconds` from `ai_hub_voice_session_log` for the current month + estimate the current session's duration (worst case = max-session-duration). If sum > quota, reject the session with `SERVICE_OVERLOAD` and a clear error.
2. Periodically (every 5 min) check the alert threshold and emit `bytechef_voice_workspace_quota_threshold_total{workspace_class}` so dashboards can fire alerts.

**Subtlety.** The "sum at session open" SQL is fine for moderate usage but becomes a hot query under high concurrency. Cache the per-workspace monthly total in-memory (TTL 1 min) and only re-query if a session close updates the cache.

### Effort

~2 days.

## Item 3 — Provider quota detection

### What's missing

If Deepgram returns 429 (rate-limited) or 402 (billing-exhausted), we close the WS with a generic SERVER_ERROR. The customer gets no signal that their PROVIDER account (not their ByteChef workspace) is at fault.

### Change

Parse provider-specific error responses:

- Deepgram returns `{"type":"Error","description":"...","reason":"INSUFFICIENT_FUNDS"|"RATE_LIMITED"|...}` on the WS during/after open.
- Map to specific `AiHubVoiceProviderException` subclasses: `ProviderRateLimitedException`, `ProviderBillingExhaustedException`.
- The WS handler closes with a distinct close-status reason and emits a distinct browser error so the UI can show "Deepgram account is rate-limited — check your provider dashboard" instead of "internal server error."

### Effort

~1 day.

## Item 4 — Data residency

### What's missing

Deepgram processes audio in their US/EU regions. Customers in regulated industries (healthcare, EU privacy) need to:
- Pin the provider region (route to Deepgram EU only).
- Know which region was used (audit trail).

### Change

1. **Region setting** on connection or workspace settings: `voice_provider_region` (`US|EU|APAC`).
2. **Region-aware URI selection** in each provider session:
   - Deepgram US: `wss://api.deepgram.com` (current).
   - Deepgram EU: `wss://api.deepgram.eu`.
   - OpenAI/ElevenLabs similarly when added.
3. **Audit log entry** at session open records the resolved region.

### Effort

~1 day (per provider).

## Item 5 — Audio + transcript retention

### What's missing

- Audio bytes are forwarded provider-side and not stored by ByteChef. Good.
- Transcripts persist to `SPRING_AI_CHAT_MEMORY` indefinitely. Same retention as text chat — but text chat doesn't have the same regulatory implications (HIPAA, GDPR voice recordings).

### Change

New workspace settings:

- `voice_transcript_retention_days INT NULL` — null = same as text chat retention; otherwise voice-specific TTL.
- `voice_transcript_pii_redaction BOOLEAN NULL` — null = off; when on, pass transcripts through a PII redaction pipeline (regex-based for v1; LLM-based for v2) before persisting.

Scheduled cleanup task (existing `ai_hub_task_cleanup_job` or similar) deletes voice-tagged messages older than the TTL.

### Effort

~2 days (TTL is straightforward; PII redaction is a rabbit hole — defer the redaction piece to a separate spec).

## Item 6 — Consent prompt

### What's missing

Some jurisdictions (especially CA for two-party consent calls) require an explicit "this conversation is being processed by [AI]" disclosure when voice AI is engaged. Today there's no surface for that.

### Change

New workspace settings:

- `voice_consent_disclosure_text VARCHAR(1000) NULL` — when set, the WS handler emits this as the first assistant utterance before any user interaction. The text is workspace-configurable so customers can match their local legal requirements (Spanish/French/etc).
- `voice_consent_required BOOLEAN NULL` — when true, the session won't accept user audio until a separate `{"action":"consent_acknowledged"}` text frame is received from the browser. The widget/composer UI shows a "I understand" button.

For Twilio inbound calls, the disclosure is spoken via TTS at call start. The acknowledgement is implicit (the user keeps talking) or explicit (DTMF "1 to continue").

### Effort

~1.5 days.

## Cross-cutting

### Liquibase migration

Items 1, 2, 4, 5, 6 all add columns. Group them:

```sql
ALTER TABLE ai_hub_workspace_settings
    ADD COLUMN voice_monthly_minute_quota INT NULL,
    ADD COLUMN voice_monthly_minute_alert_threshold INT NULL,
    ADD COLUMN voice_provider_region VARCHAR(8) NULL,
    ADD COLUMN voice_transcript_retention_days INT NULL,
    ADD COLUMN voice_transcript_pii_redaction BOOLEAN NULL,
    ADD COLUMN voice_consent_disclosure_text VARCHAR(1000) NULL,
    ADD COLUMN voice_consent_required BOOLEAN NULL;

-- New table for Item 1, separate migration.
```

### Metrics

- `bytechef_voice_workspace_quota_exhausted_total{workspace_class}` — Item 2.
- `bytechef_voice_provider_quota_rejection_total{provider,reason}` — Item 3.
- `bytechef_voice_consent_required_blocked_total{workspace_class}` — Item 6 (when audio is rejected pre-consent).

### Sequencing

Roughly in priority order for B2B GA:

1. **Item 1 (metering)** — without it, all the others are blind. ~3 days.
2. **Item 3 (provider quota detection)** — quick win, helps the UX immediately. ~1 day.
3. **Item 2 (cost caps)** — needs Item 1 first. ~2 days.
4. **Item 6 (consent)** — needed for regulated industries. ~1.5 days.
5. **Item 4 (residency)** — needed for EU customers. ~1 day per provider.
6. **Item 5 (retention)** — defer redaction; TTL is small. ~1 day.

Total: ~10–11 days for the full spec. Items 1 + 3 alone (~4 days) are the bare minimum to move from "we have no idea what voice costs" to "customers can see their usage and provider failures are intelligible."
