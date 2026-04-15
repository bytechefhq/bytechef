# Reddit r/opensource Post

**Title:** ByteChef — open-core alternative to Zapier and n8n with 180+ integrations (Apache 2.0 core)

---

**Post body:**

Hi r/opensource,

We're sharing ByteChef today — an open-source platform for workflow automation, API integration, and AI agent orchestration with an Apache 2.0 core. Three of us (Igor, Ivica, and Matija) have been building this, and we're excited to finally put it in front of the open-source community.

**GitHub:** https://github.com/bytechefhq/bytechef
**Launch page:** https://blog.bytechef.io/launch
**License:** Open-core — platform core and basic features under Apache 2.0, advanced Enterprise features under a commercial EE license.

### Why open-core with a real Apache 2.0 core

We picked Apache 2.0 for the core on purpose. We've seen what happened with tools like n8n (Sustainable Use License — not OSI-approved open source) and others that go "source-available" or change licenses once they get traction. We think the automation platform itself should be genuinely open — no restrictions on commercial use, no "fair use" limitations, no surprises down the road. Use it, modify it, embed it, ship it. Advanced features aimed at larger teams (SSO/SAML, RBAC, multi-tenancy, audit logging, etc.) live in the Enterprise Edition under a commercial license — but the core platform, the workflow engine, the component system, and the 180+ integrations are all Apache 2.0.

### What ByteChef does

It lets you automate repetitive tasks and orchestrate complex multi-step business processes. You can build workflows visually with a drag-and-drop editor, write them in code (JavaScript, Python, Ruby, Bash), or combine both approaches.

We ship 180+ integrations covering CRM, project management, communication, databases, e-commerce, dev tools, Google Workspace, Microsoft 365, and more. AI is built in as a first-class feature — 13+ LLM providers as workflow components, and MCP protocol support (consume MCP tools and build workflows through MCP).

### The stack

- **Backend:** Java 25, Spring Boot 4
- **Frontend:** React 19, TypeScript
- **Code execution:** GraalVM Polyglot (JS, Python, Ruby)
- **Database:** PostgreSQL 15+
- **Deployment:** Docker, Kubernetes (Helm charts)
- **Enterprise Edition (commercial):** SSO/SAML, multi-tenancy, RBAC, audit logging

### Contributing

We welcome contributions — whether that's new integrations, bug fixes, documentation, or feature requests. The component architecture makes it relatively easy to add new integrations. We also have a CLI that generates components from OpenAPI specs, which speeds things up a lot.

### What's next

**Shipping next:**
- **AI Agents** — autonomous task execution with tool use, memory, agent skills, evaluations, and AI guardrails.
- **Streaming chat workflows** — conversational workflows with streamed responses.
- **Human-in-the-loop approvals** — require sign-off before sensitive or AI-initiated actions run.
- **Template library** — share and import workflows built by the community.

**After that:**
- **AI Copilot** — natural language workflow building and code editor assist.
- **Data Tables** — built-in structured data storage.
- **Knowledge Base** — RAG with 12+ vector stores.
- **MCP Server** — expose workflows as MCP tools for other AI systems.

**Later:**
- **AI Gateway with observability** — LLM tracing, prompt management, evaluations, alerting, rate limiting, playground. Open-source LLM observability built into the platform.
- **Embedded iPaaS** — for SaaS companies to embed ByteChef's integrations into their product. Part of the commercial EE. Early alpha in exploration.
- **n8n workflow converter** — import your existing n8n workflows directly.
- More to come.

Would love your feedback, questions, or ideas. What would you use this for?
