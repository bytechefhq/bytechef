---
title: AI Gateway
description: Route, observe, and govern LLM traffic — tracing, rate limiting, and scoring for every model call.
---

# AI Gateway

<EEBadge />

> **Coming soon.** The AI Gateway is on the upcoming release track and is not yet available in the latest released version of ByteChef.

The **AI Gateway** is an Enterprise Edition service that sits between your applications and LLM providers, giving platform teams one place to observe and govern model traffic:

- **Tracing** — LLM calls are captured as spans (OTLP ingestion supported), so you can follow a request across prompts, tool calls, and retrievals.
- **Rate limiting** — cap traffic per tenant or client before it reaches a provider.
- **Scoring** — attach quality scores to traces (including batched external scores) to evaluate model behavior over time.

The gateway surface lives under **AI → Gateway** in the automation workspace. It is enabled and tuned with the [`BYTECHEF_AI_GATEWAY_*`](/self-hosting/configuration/environment-variables#ai-gateway-configuration) environment variables.
