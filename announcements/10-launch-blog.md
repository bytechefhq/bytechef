# blog.bytechef.io/launch — Launch Blog Post Content

> This is the canonical launch blog post at blog.bytechef.io/launch. All announcements link here first.
> **6 sections total** — focused for launch-day conversion. Extended content (full integration list, deep-dive articles, founder narrative) lives on dedicated subpages.
> Reuse the dev.to images (7 assets) for visual continuity across channels.

---

## 1. HERO SECTION

### Eyebrow
**Now live — ByteChef v0.25**

### Headline
**Automation built for the whole team.**

### Subheadline
Open-source data orchestration platform. Visual canvas for non-engineers, polyglot code workflows (JS/Python/Ruby/Bash) for engineers — same workflow. Apache 2.0 core.

### Primary CTA
[Try ByteChef →]  (Docker quickstart in under 2 min)

### Secondary CTAs
[Star on GitHub](https://github.com/bytechefhq/bytechef) — [Read the docs](https://docs.bytechef.io)

### Hero visual
**Demo video (primary):** Embed the 30–60s demo video at the top of the hero, autoplay-muted-loop.
**Video URL:** `[DEMO_VIDEO_URL]` *(update with unlisted YouTube link before launch)*

**Fallback image (if video not ready):** Reuse dev.to cover image ("ByteChef platform overview").

Video content: canvas with Gmail trigger → Python code step → LLM component → Slack output → execution history with traceable data flow.

---

## 2. THE ZERO TOLERANCE CAROUSEL

### Headline
**At ByteChef, we have ZERO tolerance for:**

### Three ZERO value blocks (carousel or 3-column grid)

**🤖 Blind Trust in AI**
Bring your own LLM from 13+ providers. Every AI call traceable in execution history. Human-in-the-loop approvals shipping next, so you stay in control of what AI does, when, and with what data.

**🔓 Vendor Lock-in**
Open-core with a genuine Apache 2.0 core. Self-host anywhere. Export workflows anytime. No retroactive license switches. No "source-available" fine print on the platform.

**💸 Bill Shock**
Per-task billing at a consistent rate across packages — no pricing tiers that jump as you scale. Component testing doesn't count against your quota.

---

## 3. PRODUCT OVERVIEW

### Headline
**Everything you need to automate, orchestrate, and govern.**

### Three-column layout (each column is a short block)

**🎨 Workflows for both audiences**
Free-form visual canvas for non-technical users. Polyglot code workflows (JS, Python, Ruby, Bash via GraalVM) for engineers. Mix a no-code visual step with a Python data transformation in the same workflow — no context-switching, no separate "developer mode."

**Image 3:** *"Automate with drag-and-drop in ByteChef"* — show a workflow with a drag-and-drop step next to a code step in the same canvas.

**🤖 AI with oversight**
13+ LLM providers (OpenAI, Claude, Gemini, Mistral, Groq, DeepSeek, Bedrock, Ollama) as first-class workflow components. MCP support in both directions. Bring your own LLM. Every AI call traceable.

**Image 6:** *"AI models in ByteChef"*

**🔌 180+ integrations, built properly**
Generated from OpenAPI specs and built out by hand where it matters — full auth handling, error states, dynamic options, and actions that match real-world use, not 5-field wrappers. Salesforce, HubSpot, Slack, Jira, Google Workspace, Microsoft 365, PostgreSQL, Stripe, Shopify, and many more. Generate a new component from any OpenAPI spec.

[See all integrations →](/integrations)

### Below the three columns — one-line note
**Deploy anywhere:** Docker, Kubernetes, your choice of message broker (Redis / RabbitMQ / Kafka / SQS) and storage backend (filesystem / S3 / database). [Read the self-hosting guide →](/docs/self-hosting)

---

## 4. OPEN-CORE SECTION

### Headline
**Open-core — with a real Apache 2.0 core.**

### Body
ByteChef's platform core, workflow engine, component system, AI components, and all 180+ integrations are Apache 2.0. No "source-available" caveats. No restrictions on commercial use. No license switches planned.

Advanced features aimed at larger organizations (SSO/SAML, RBAC, multi-tenancy, audit logging, Embedded iPaaS) live in the Enterprise Edition under a commercial license. That's how we fund ongoing development while keeping the core genuinely open.

### CTAs
[View the source](https://github.com/bytechefhq/bytechef) · [Read the license](https://github.com/bytechefhq/bytechef/blob/main/LICENSE) · [Community vs Enterprise](https://blog.bytechef.io/pricing)

---

## 5. ROADMAP — What's in active development

### Headline
**We're just getting started.**

### Three-column phased layout

**Shipping next**
- AI Agents (tool use, memory, skills, AI guardrails)
- Streaming chat workflows
- Human-in-the-loop approvals
- Template library

**After that**
- AI Copilot
- Data Tables
- Knowledge Base (RAG, 12+ vector stores)
- MCP Server

**Later**
- AI Gateway with observability
- Embedded iPaaS (commercial EE, early alpha)
- n8n workflow converter
- More on our [public roadmap →](https://github.com/bytechefhq/bytechef/milestones)

---

## 6. FINAL CTA SECTION

### Headline
**Focus on what matters. Automate the rest.**

### Three CTAs side-by-side

**Try it now**
Docker quickstart in under 2 min.
[Quickstart →]

**Star on GitHub**
180+ integrations. Apache 2.0 core.
[github.com/bytechefhq/bytechef →]

**Join the community**
Discord, GitHub Discussions, and monthly newsletter.
[Join Discord →]

---

## FOOTER LINKS

- v0.25 release notes: `github.com/bytechefhq/bytechef/releases/tag/v0.25`
- Full story: [Read the launch blog post on dev.to](https://dev.to/bytechef/welcome-to-bytechef)
- Documentation: `docs.bytechef.io`
- Pricing: `blog.bytechef.io/pricing`
- Roadmap: `github.com/bytechefhq/bytechef/milestones`

---

## SEO METADATA

**Title tag** (50-60 chars)
`ByteChef — Open-Source Automation with Apache 2.0 Core`

**Meta description** (150-160 chars)
`Open-source data orchestration platform with 180+ integrations and polyglot code workflows. Self-hosted or cloud. Apache 2.0 core — no license surprises.`

**Canonical URL**
`https://blog.bytechef.io/launch`

**OG image**
ByteChef logo + "Automation you actually own" + visual of the canvas editor

---

## IMPLEMENTATION NOTES

1. **No CMS required** — this can be a static page, hand-coded in whatever stack powers blog.bytechef.io.
2. **Kept short on purpose.** The full narrative lives on the dev.to blog post; the full integration list lives on `/integrations`; the full self-hosting guide lives in the docs. This page exists to convert HN/Reddit/LinkedIn visitors — it should read in under a minute.
3. **dev.to canonical** — the dev.to post should add a canonical tag pointing to `blog.bytechef.io/launch` so SEO flows back to your domain. Same for Medium if you republish.
4. **Keep it permanent** — even after launch week, this URL should stay live. Redirect `/launch` → `/` after 3-6 months if you replace with evergreen messaging, but archive the content.
5. **Analytics** — tag inbound links with UTM parameters by channel (HN, Reddit, LinkedIn, Twitter, Discord, IH, dev.to) so you can measure conversion by source.
6. **What we cut from the previous draft:** the 400-word "Stepping outside the comfort zone" narrative (lives on dev.to now); the full 10-category integration list (moved to `/integrations`); the long self-hosting explainer (moved to `/docs/self-hosting`); the "Focus on what matters, Automate the rest" closing essay (compressed into the final CTA headline). Net: ~11 sections → 6 sections, ~13,000 chars → ~4,500 chars.

---

## VISUAL ASSETS CHECKLIST

The launch page reuses a subset of dev.to images. You don't need all 7; the tighter page uses 2–3.

| # | Alt text | Placement |
|---|----------|-----------|
| 1 | ByteChef platform overview | Hero cover |
| 3 | Automate with drag-and-drop in ByteChef | Product overview — "Workflows for both audiences" column |
| 6 | AI models in ByteChef | Product overview — "AI with oversight" column |

The other 4 dev.to images (2, 4, 5, 7) stay on dev.to and on subpages if relevant, but don't need to repeat on the launch page.

**Additional visuals to create:**
- ZERO campaign illustration for the carousel (5 icon/illustration blocks)
- Integration logo grid for a future `/integrations` subpage
- OG image specific to `/launch` URL
