# Reddit r/SaaS Post

**Title:** We just launched ByteChef — open-core automation platform built for teams that serve both technical and business users

---

**Post body:**

Hey r/SaaS,

We just launched ByteChef and figured this community might care for two reasons: automation tools with pricing tiers that explode as you scale are a real pain if you're running a growing SaaS, and we're early on an Embedded iPaaS story we'd love input on.

**Launch page:** https://blog.bytechef.io/launch
**GitHub:** https://github.com/bytechefhq/bytechef

### Quick context

I'm Ivica, one of three co-founders. ByteChef is an open-source data orchestration platform — think of it as an open-source alternative to Zapier and n8n, but built for teams that want to treat automation as infrastructure rather than a collection of app connections.

It's open-core: the platform core, workflow engine, and 180+ integrations are Apache 2.0. Advanced features for larger orgs (SSO/SAML, RBAC, multi-tenancy, audit logging) live in the commercial Enterprise Edition.

### Why SaaS founders might care

**1. Consistent pricing as you scale**

Most automation tools (Zapier, Make) jump pricing tiers as usage grows. A small spike in user activity can blow up your bill. We have per-task billing too, but the rate is consistent across packages — costs scale predictably. Component testing doesn't count against your quota. No hidden fees.

**2. Both technical and business users can work on it**

n8n is powerful but positioned at technical teams — your CS lead or your ops person can't just pick it up. Zapier is accessible but runs out of steam on anything non-trivial. ByteChef has a free-form visual canvas for non-technical users AND polyglot code workflows (JS, Python, Ruby, Bash via GraalVM) for engineers, mixable in the same workflow. If you're a small SaaS team where the same person wears 3 hats, this matters.

**3. Embedded iPaaS — alpha, looking for a few early design partners**

If you've ever had a customer say "I love your product, but does it integrate with [tool X, Y, Z]?" — you know the pain. Building and maintaining 50+ native integrations is a black hole. Buying closed-source embedded iPaaS (Workato Embedded, Tray Embedded, Paragon) gets expensive fast and locks you into someone else's roadmap.

**Our Embedded iPaaS is in alpha — not publicly announced, not broadly marketed.** We're looking for 3–5 SaaS companies to work with us as early design partners before we ship more widely. If that's interesting, DM me and we'll talk. Real access, real influence on the design — but expect rough edges.

### What's available right now

- Visual workflow editor with a free-form canvas + code workflows in JS/Python/Ruby/Bash
- 180+ pre-built integrations (Salesforce, HubSpot, Slack, Stripe, Shopify, Google Workspace, Microsoft 365, etc.)
- 13+ LLM providers (OpenAI, Claude, Gemini, Mistral, Groq, DeepSeek, Bedrock, Ollama) as workflow components
- MCP support — consume MCP tools and build workflows through MCP
- Self-host on Docker / Kubernetes, or run on our cloud
- Multiple message brokers (Redis, RabbitMQ, Kafka, SQS) and storage backends

### What's next

**Shipping next:**
- AI Agents (tool use, memory, skills, AI guardrails)
- Streaming chat workflows
- Human-in-the-loop approvals
- Template library

**After that:**
- AI Copilot (build workflows in natural language)
- Data Tables (built-in structured storage)
- Knowledge Base (RAG with 12+ vector stores)
- MCP Server (expose your workflows as MCP tools)

**Later:**
- AI Gateway with observability (LLM tracing, prompt management, evaluations, alerting, rate limiting, playground)
- Embedded iPaaS for SaaS (commercial EE, early alpha in exploration)
- n8n workflow converter
- More to come

### A few things I'd love feedback on

- If you've embedded integrations into your SaaS product, what made you build vs. buy? Where did your current solution disappoint you?
- For SaaS founders dealing with automation costs at scale — how do you handle pricing tier jumps in tools like Zapier?
- For anyone who's gone open-core: what worked, what didn't?

Genuinely interested in the discussion. Happy to answer questions about the architecture, our open-core decision, or anything else.

Built for teams that value ZERO black boxes, ZERO vendor lock-in, ZERO bill shock. That's the pitch.

Focus on what matters. Automate the rest.
