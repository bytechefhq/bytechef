# HackerNews Post

**Title:** Launch HN: ByteChef – Open-source alternative to Zapier/n8n with Apache 2.0 core

---

**Post body:**

Hi HN, we're Igor, Ivica, and Matija — three engineers with collectively 30+ years building integration and workflow infrastructure. ByteChef is our third-generation take on the problem: an open-source data orchestration platform with an Apache 2.0 core.

Repo: https://github.com/bytechefhq/bytechef
Launch page: https://blog.bytechef.io/launch
45-second demo: `[DEMO_VIDEO_URL]`

**Why now:** The open-source automation space has gotten worse for end users in the last 18 months. n8n relicensed to a non-OSI license, Zapier kept hiking per-task pricing, and AI features got bolted onto platforms that weren't built for them. We wanted an open-core alternative that's built right from the start.

**The product:** A visual workflow editor on a free-form canvas, plus code workflows in JavaScript, Python, Ruby, or Bash (GraalVM) — mix visual and code steps in the same workflow, so engineers and non-engineers can work on the same automation without context-switching. 180+ integrations out of the box, and a CLI to generate a new component from any OpenAPI spec.

Three things we think set us apart:

**AI with oversight, not AI as a black box.** LLMs from 13+ providers (OpenAI, Claude, Gemini, Mistral, Groq, DeepSeek, Bedrock, Ollama) drop in as workflow components. Bring your own LLM. Every AI call is traceable in execution history. MCP support works both ways — consume MCP tools, and build workflows through MCP. Human-in-the-loop approvals are shipping next.

**Operational clarity.** Every workflow execution is fully traceable — what data flowed where, when, and why. Audit logs, triggers, observability hooks. When something breaks at 2am, you know where to look.

**Consistent pricing, no bill shock.** Per-task billing at a consistent rate across packages — costs scale predictably. Component testing doesn't count against your quota.

**Tech stack:** Java 25 (Spring Boot 4), React 19 + TypeScript, GraalVM, PostgreSQL. Deploy monolith or microservices. Supports Redis, RabbitMQ, Kafka, SQS.

**Open-core:** Platform core, workflow engine, and all 180+ integrations are Apache 2.0 — no source-available games, no license switches planned. Advanced Enterprise features (SSO/SAML, RBAC, multi-tenancy, audit logging, Embedded iPaaS) are under a commercial EE license.

**What's coming:**

- *Shipping next:* AI Agents (tool use, memory, skills, guardrails), streaming chat workflows, human-in-the-loop approvals, Template library.
- *After that:* AI Copilot, Data Tables, Knowledge Base (RAG, 12+ vector stores), MCP Server.
- *Later:* AI Gateway with observability, Embedded iPaaS for SaaS (commercial EE, early alpha), n8n workflow converter, and more.

Would love to hear what you think — what's missing, what use cases you'd throw at it, or any questions about how it works under the hood.
