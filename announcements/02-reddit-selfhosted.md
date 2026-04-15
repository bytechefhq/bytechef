# Reddit r/selfhosted Post

**Title:** We built ByteChef — self-hosted-first workflow automation with 180+ integrations, polyglot code workflows, and no black boxes (Apache 2.0 core)

---

**Post body:**

Hey r/selfhosted,

We just launched ByteChef and figured this community would care about what we've built. It's an open-source workflow automation platform that's designed to be self-hosted from the ground up — not a cloud product with a self-hosted afterthought bolted on.

**GitHub:** https://github.com/bytechefhq/bytechef
**Launch page:** https://blog.bytechef.io/launch

### The backstory

We're three founders (Igor, Ivica, and Matija) who spent years dealing with tools like Zapier that got expensive fast, n8n's "source-available" license that limits what you can do with it, and platforms that struggled with anything beyond simple triggers. We built ByteChef for teams that value ZERO vendor lock-in, ZERO infrastructure limits, and ZERO license surprises — something you can run on your own machines, customize to your needs, and not worry about some vendor changing their pricing or license.

### What it does

You get a visual workflow editor — drag and drop on a free-form canvas — plus 180+ pre-built integrations (Slack, Jira, Salesforce, HubSpot, Google Workspace, PostgreSQL, Shopify, Stripe, and a lot more). If visual isn't your thing, you can write workflows in code (JavaScript, Python, Ruby, Bash) or mix both.

Flow control is real: conditions, branches, loops, parallel execution, error handling.

The AI stuff is baked in too: you can plug LLMs from 13+ providers directly into workflows. MCP protocol integration lets you consume MCP tools in your workflows and build workflows through MCP itself.

### The self-hosting bits

This is probably what you actually want to know:

- **Docker & Kubernetes** with Helm charts. Spin it up with docker compose and you're running.
- **Start monolith, go microservices** when you need to. No re-architecture required.
- **Pick your message broker** — Redis (default), RabbitMQ, Kafka, or AWS SQS.
- **Pick your storage** — Filesystem, S3, or database.
- **PostgreSQL 15+** for the main database.
- **Enterprise Edition** — SSO/SAML/OIDC, multi-tenancy, RBAC, API key management, audit logging, encryption at rest available under a commercial EE license for teams that need them. We're open-core: platform and basic features are Apache 2.0; advanced features are EE.

### Tech stack

Java 25 (Spring Boot 4) backend, React 19 + TypeScript frontend. GraalVM for polyglot code execution.

### What's coming

**Shipping next:**
- **AI Agents** — autonomous agents with tool use, chat memory, agent skills, evaluations, and AI guardrails.
- **Streaming chat workflows** — conversational workflows with streamed responses.
- **Human-in-the-loop approvals** — require sign-off before sensitive or AI-initiated actions run.
- **Template library** — share and import workflows built by the community.

**After that:**
- **AI Copilot** — natural language workflow building, code editor assist, execution sheet assist.
- **Data Tables** — built-in structured data storage with webhooks.
- **Knowledge Base** — RAG with 12+ vector stores.
- **MCP Server** — expose workflows as MCP tools so other AI systems can call them.

**Later:**
- **AI Gateway with observability** — LLM tracing, prompt management, evaluations, alerting, rate limiting, playground, data export. Built-in, self-hostable, no third-party dependency.
- **Embedded iPaaS** — for SaaS companies to embed ByteChef's integrations into their product (commercial EE). Early alpha in exploration.
- **n8n workflow template converter** — import your n8n workflows directly.
- More to come.

We'd love feedback from this community. What matters most to you when you self-host an automation platform? What integrations or features would you want to see?

Happy to answer any questions about deployment, resource requirements, or the architecture.
