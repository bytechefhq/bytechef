# IndieHackers Post

**Title:** We built an open-source automation platform for teams who don't want to choose between power and accessibility — here's ByteChef

---

**Post body:**

Hey IH 👋

We're Igor, Ivica, and Matija — three founders who just launched ByteChef, an open-source platform for AI agent orchestration, workflow automation, and API integration.

**Launch page:** https://blog.bytechef.io/launch
**GitHub:** https://github.com/bytechefhq/bytechef

### Why we built this

The automation space has a weird split. Zapier's per-task pricing jumps between tiers as you grow — a small success can blow up your bill. n8n is powerful but positioned squarely at technical teams — great for engineers, but non-technical users hit a wall fast. Make.com is decent but still cloud-only. We wanted something that serves both technical and business users without forcing the technical users to give up flexibility — and that's genuinely open-source.

We wanted to build something different. Built for teams that value ZERO: ZERO blind trust in AI, ZERO vendor lock-in, ZERO bill shock. You own the core. AI is built in from day one — with bring-your-own LLMs and full execution history on every AI call. The core license is genuinely open (Apache 2.0). We're open-core — platform core and basic features are Apache 2.0, with advanced Enterprise features under a commercial EE license. Per-task billing at a consistent rate across packages, with component testing at no additional charge.

### What we've built so far

- **Visual workflow editor** — free-form canvas, drag-and-drop, the whole thing. But also supports writing workflows in code if you prefer.
- **180+ integrations** — all the usual suspects: Salesforce, HubSpot, Slack, Jira, Google Workspace, Stripe, Shopify, PostgreSQL, and many more.
- **AI as a workflow component** — plug in LLMs from 13+ providers (OpenAI, Claude, Gemini, Mistral, DeepSeek, Ollama, etc.). Their outputs flow through your workflow like any other data. MCP support lets you consume AI tools and build workflows through MCP.
- **Self-hosted or cloud** — Docker, Kubernetes with Helm charts, start simple and scale up. Advanced Enterprise features (SSO, RBAC, multi-tenancy, audit logs) are available under a commercial EE license.

### The numbers

- 180+ integrations
- 3 co-founders, all technical
- Open-core (Apache 2.0 core + commercial EE)

### What's next

**Shipping next:**
- **AI Agents** — autonomous task execution with tool use, memory, agent skills, evaluations, and AI guardrails.
- **Streaming chat workflows** — conversational workflows with streamed responses.
- **Human-in-the-loop approvals** — require sign-off before sensitive or AI-initiated actions run.
- **Template library** — share and import workflows built by the community.

**After that:**
- **AI Copilot** — natural language workflow building.
- **Data Tables** — built-in structured data storage.
- **Knowledge Base** — RAG with 12+ vector stores.
- **MCP Server** — expose workflows as MCP tools for other AI systems.

**Later:**
- **AI Gateway with observability** — LLM tracing, prompt management, evaluations, alerting, rate limiting, and playground built into the platform.
- **Embedded iPaaS** — for SaaS companies to embed ByteChef's integrations directly into their product (white-label, per-customer config, unified API). Part of the commercial Enterprise Edition. Early alpha in exploration.
- **n8n workflow converter** — import your existing n8n workflows directly into ByteChef.
- More to come.

### Our ask

We'd love feedback from the IH community. If you've used Zapier, n8n, Make, or similar tools — what frustrated you? What would make you switch to something open-source? And if you're building a SaaS product — have you considered embedding integrations directly into it?

Check it out and let us know what you think: https://blog.bytechef.io/launch
