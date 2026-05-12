<div align="center">

[![License: Apache 2.0 + EE](https://img.shields.io/static/v1?label=license&message=Apache%202.0%20%2B%20EE&color=brightgreen)](https://github.com/bytechefhq/bytechef/blob/master/LICENSE)
[![Docker Pulls](https://img.shields.io/docker/pulls/bytechef/bytechef)](https://hub.docker.com/r/bytechef/bytechef)
[![Build Status](https://github.com/bytechefhq/bytechef/actions/workflows/build_push.yml/badge.svg)](https://github.com/bytechefhq/bytechef/actions/workflows/build_push.yml)
[![Discord](https://img.shields.io/badge/Discord-Join%20Us-7389D8?logo=discord&logoColor=white)](https://discord.gg/VKvNxHjpYx)

[![ByteChef](https://raw.githubusercontent.com/bytechefhq/bytechef/master/static/bytechef_logo.png)](https://www.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme)

# **The open-source platform that unifies AI agent orchestration and workflow automation**
### Autonomy and precision — in one platform.

[Documentation](https://docs.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme) · [Live Demo](https://www.youtube.com/watch?v=vFpobtcdWSc) · [Discord](https://discord.gg/VKvNxHjpYx) · [Connect on X](https://x.com/bytechefhq) · [Roadmap](https://github.com/orgs/bytechefhq/projects/3)

</div>

> <!-- VISUAL 1 — Workflow editor. Capture: full canvas with multiple steps, branches, and a trigger. Save as static/workflow-editor.png -->
> ![ByteChef workflow editor](static/workflow-editor.png)

[//]: # (---)

[//]: # (## About ByteChef)

[//]: # ()
[//]: # (ByteChef is an open-source platform for AI agents, workflow automation, and API integration.)

[//]: # ()
[//]: # (Instead of using separate tools for AI agents and automated workflows, ByteChef brings both into a single platform with one orchestration layer, centralized management, and enterprise-grade security.)

[//]: # ()
[//]: # (Built for modern enterprises, ByteChef can run in regulated environments and be embedded into products that deliver AI capabilities to end users.)

[//]: # ()
[//]: # (---)

### AI Agents — built in, not bolted on

> <!-- VISUAL 2 — Agent editor (cluster element editor). Capture: the AI agent component opened with its cluster of sub-elements visible — model, tools list, memory, guardrails, knowledge base. Save as static/agent-editor.png -->
> ![ByteChef agent editor — cluster element view](static/agent-editor.png)

A drag-and-drop **AI Agent** component runs the full agent loop — model → tool selection → execution → observation → next step — with streaming and structured output.

[//]: # (| Capability | What ships |)

[//]: # (| --- | --- |)

[//]: # (| **LLM providers** &#40;14&#41; | OpenAI · Anthropic · Azure OpenAI · Bedrock · Vertex Gemini · Mistral · Groq · DeepSeek · Hugging Face · Nvidia · Perplexity · Stability · Ollama · OpenRouter |)

[//]: # (| **Tools** | Every component is a tool. Mark properties with `fromAi&#40;"…", "STRING", { required: true }&#41;` and the agent fills them at runtime. Sub-workflows are tools too. |)

[//]: # (| **Memory** &#40;8 backends&#41; | JDBC · Redis · MongoDB · Cassandra · Cosmos DB · Neo4j · vector-store-backed · in-memory |)

[//]: # (| **Guardrails** &#40;12&#41; | PII · LLM-PII · jailbreak · NSFW · topical alignment · keywords · secret keys · URLs · sanitize · custom regex · custom rules · violation aggregator |)

[//]: # (| **Knowledge bases & RAG** | Native ingestion + chunking. 15+ vector stores: pgvector · Pinecone · Qdrant · Weaviate · Milvus · Couchbase · Neo4j · Redis · Typesense · MariaDB · Oracle · S3 · built-in. Two RAG patterns: `rag-modular`, `rag-questionanswer`. |)

[//]: # (| **Agent Skills** 🚧 | _In development._ Versioned, downloadable bundles of prompt + tools + memory + guardrails + knowledge bindings. [Track on roadmap]&#40;https://github.com/orgs/bytechefhq/projects/3&#41;. |)

[//]: # (| **MCP** &#40;in & out&#41; | Consume any MCP server as a tool source. Expose any workflow as an MCP tool to Claude Desktop, Cursor, Windsurf — with API-key auth. |)

[//]: # (| **Evaluations** 🚧 | _In development._ Scenarios, runs, judges &#40;StringEquals, Regex, Contains, JsonSchema, ResponseLength, Similarity, LlmRule, ToolUsage&#41;, tool simulation, user simulator. [Track on roadmap]&#40;https://github.com/orgs/bytechefhq/projects/3&#41;. |)

### Build Workflows with Ease using Copilot

> <!-- COPILOT SCREENSHOT — capture: Copilot side panel with a natural-language prompt on the left, generating a workflow with a configured AI agent component on the canvas. Save as static/copilot.png -->
> ![ByteChef AI Copilot generating an agent workflow](static/copilot.png)

Build AI agents and workflows by talking to ByteChef. The Copilot generates workflows from a sentence, drops in configured agent steps, explains failed runs and suggests fixes.

---

## Quick Start

### Docker Compose (Fastest Setup)

**Requirement:** [Docker Desktop](https://www.docker.com/products/docker-desktop/)

This is the fastest way to start ByteChef. Download the `docker-compose.yml` file from the repository:
```bash
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/docker-compose.yml
docker compose -f docker-compose.yml up
```

Both PostgreSQL database and ByteChef containers will start automatically.

Open <http://localhost:8080/login> → **Create Account** → sign in.

### Docker (Manual Setup)

If Docker Compose isn't supported in your environment, follow these steps:

#### 1. Create Docker Network
```bash
docker network create -d bridge bytechef_network
```

#### 2. Start PostgreSQL Container
```bash
docker run --name postgres -d -p 5432:5432 \
    --env POSTGRES_USER=postgres \
    --env POSTGRES_PASSWORD=postgres \
    --hostname postgres \
    --network bytechef_network \
    -v /opt/postgre/data:/var/lib/postgresql/data \
    postgres:15-alpine
```

#### 3. Start ByteChef Container
```bash
docker run --name bytechef -it -p 8080:8080 \
    --env BYTECHEF_DATASOURCE_URL=jdbc:postgresql://postgres:5432/bytechef \
    --env BYTECHEF_DATASOURCE_USERNAME=postgres \
    --env BYTECHEF_DATASOURCE_PASSWORD=postgres \
    --env BYTECHEF_SECURITY_REMEMBER_ME_KEY=e48612ba1fd46fa7089fe9f5085d8d164b53ffb2 \
    --network bytechef_network \
    docker.bytechef.io/bytechef/bytechef:latest
```

**Note:** Use `-d` flag instead of `-it` to run in detached mode.

Open <http://localhost:8080/login> → **Create Account** → sign in.

### Build your first agent in 60 seconds

1. **New Project → New Workflow**,
2. Add a trigger
3. Add the **AI Agent** component
4. Pick a **model**, attach **tools** from 200+ connectors, optionally add a **knowledge base** and **guardrails**
5. Fill the necessary credentials
6. Configure each component's parameters in the properties panel
7. Test your workflow
8. Deploy

---

## Workflow Automation

- **Visual editor** with JSON underneath, Git-friendly
- **Flow controls** — `condition` · `switch` · `loop` · `each` · `parallel` · `branch` · sub-workflows
- **Triggers** — webhook · schedule · polling · app-event · manual · form
- **Polyglot code** — Java · JavaScript · Python · Ruby on GraalVM
- **Durable execution** on the Atlas runtime, Postgres-backed, queue-mode for horizontal scale (memory · Redis · RabbitMQ · Kafka · JMS · AMQP · SQS)
- **Workflows-as-APIs** — workflows can be an authenticated HTTP endpoint
- **Git-native** — push from the UI, environments backed by branches

---

## The Unification

- **Agents inside workflows** — an agent is a step; downstream branches react to its decisions
- **Workflows as agent tools** — a "refund order" workflow with retries and approvals becomes one tool
- **Sub-agents** — coordinator agents call specialist agents
- **Human-in-the-loop** — pause on approval, route to Slack/email, resume on response
- **One audit log** — agent decisions, tool calls, workflow runs, human approvals, all in one trail

---

## 180+ connectors

CRM · marketing · communication · e-commerce · cloud storage · databases · AI/ML · helpdesk · finance. Every connector is **also an agent tool, also an MCP tool**. Browse the [full catalog](https://docs.bytechef.io/reference/components).

---

## Open core — Apache 2.0 + EE

| Capability                                                                | CE (Apache 2.0) | EE |
|---------------------------------------------------------------------------| --- | --- |
| Visual editor, AI agents, workflows, 200+ connectors                      | ✅ | ✅ |
| Polyglot code (Java/JS/Python/Ruby)                                       | ✅ | ✅ |
| Knowledge bases, vector stores, guardrails, MCP server                    | ✅ | ✅ |
| Agent skills, agent evaluations                                           | 🚧 in development | 🚧 in development |
| Self-host (Docker / Kubernetes / Helm)                                    | ✅ | ✅ |
| **Workflows-as-APIs**                                                     | — | ✅ |
| **Git-native**                                                            | — | ✅ |
| **Microservices deployment**                                              | — |  🚧 in development |
| **AI Copilot**                                                            | — | ✅ |
| **SSO / SAML / OIDC**, SCIM, advanced RBAC                                | — |  🚧 in development |
| **Connection scope sharing** (Workspace / Project / Organization)         | — |  🚧 in development  |
| Multi-environment promotion, audit log with correlation IDs               | — | ✅ |
| **AI Gateway** - model routing, quotas, cost controls                     | — |  🚧 in development  |
| Embedded iPaaS - ship integrations and AI agents inside your SaaS product | — | ✅ |

---

## Contributing

If you would like to contribute to the software, read the [contributing guide](https://github.com/bytechefhq/bytechef/blob/master/CONTRIBUTING.md) to get started.

---

## License

This project is licenced under **Apache 2.0** for the core (everything outside `/ee/`) and the **ByteChef Enterprise License** for code under `/ee/` (microservices, embedded, AI Copilot, SSO/SCIM, advanced RBAC)

---

## Contributors

[![Contributors](https://contrib.rocks/image?repo=bytechefhq/bytechef)](https://github.com/bytechefhq/bytechef/graphs/contributors)

---

## Credits

ByteChef started as a fork of [Piper](https://github.com/runabol/piper).
## FAQ

### General

**What is ByteChef?**

ByteChef is an open-source platform that unifies AI agent orchestration and workflow automation in one place. Instead of using separate tools for AI agents and automated workflows, ByteChef brings both into a single platform with one orchestration layer, centralized management, and enterprise-grade security.

**How does ByteChef compare to n8n, Zapier, or Temporal?**

- **n8n/Zapier**: Traditional workflow automation tools focused on connecting apps and APIs. ByteChef extends this with native AI agent capabilities, knowledge bases, guardrails, and agentic workflow patterns.
- **Temporal**: A durable execution engine. ByteChef uses its own Atlas runtime (Postgres-backed) and adds visual workflow editing, AI agent components, and 200+ pre-built connectors.

**Is ByteChef suitable for production use?**

Yes. ByteChef is built for enterprise deployments with Docker/Kubernetes support, durable execution, Git-native version control, and enterprise-grade security features.

### Getting Started

**What are the system requirements?**

- Docker Desktop (for Docker Compose setup)
- PostgreSQL database (included in Docker Compose)
- 2GB+ RAM recommended for local development

**How do I install ByteChef?**

The fastest way is Docker Compose:
```bash
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/docker-compose.yml
docker compose -f docker-compose.yml up
```

Then open http://localhost:8080/login and create an account.

### AI Agents

**What LLM providers are supported?**

ByteChef supports 14+ LLM providers including OpenAI, Anthropic, Azure OpenAI, AWS Bedrock, Google Vertex Gemini, Mistral, Groq, DeepSeek, Hugging Face, Nvidia, Perplexity, Stability, Ollama, and OpenRouter.

**What is the AI Agent component?**

The AI Agent component is a drag-and-drop element that runs the full agent loop: model → tool selection → execution → observation → next step. It supports streaming output, structured responses, and integrates with tools, memory, guardrails, and knowledge bases.

**How do tools work in ByteChef?**

Every component in ByteChef can be used as a tool for AI agents. Mark properties with `fromAi("…", "STRING", { required: true })` and the agent fills them at runtime. Sub-workflows can also be exposed as tools.

### Memory and Knowledge

**What memory backends are available?**

ByteChef supports 8 memory backends: JDBC, Redis, MongoDB, Cassandra, Cosmos DB, Neo4j, vector-store-backed, and in-memory.

**What vector stores are supported for RAG?**

15+ vector stores including pgvector, Pinecone, Qdrant, Weaviate, Milvus, Couchbase, Neo4j, Redis, Typesense, MariaDB, Oracle, S3, and a built-in option.

**What RAG patterns are available?**

Two patterns: `rag-modular` for flexible retrieval and `rag-questionanswer` for Q&A workflows.

### Guardrails

**What guardrails are available?**

ByteChef provides 12 guardrails: PII detection, LLM-PII, jailbreak prevention, NSFW filter, topical alignment, keywords filter, secret keys detection, URL filtering, sanitize, custom regex, custom rules, and violation aggregator.

### MCP Integration

**Does ByteChef support MCP?**

Yes, ByteChef supports MCP both as a consumer (use any MCP server as a tool source) and as a provider (expose workflows as MCP tools to Claude Desktop, Cursor, Windsurf).

### Workflow Automation

**What flow controls are available?**

ByteChef provides condition, switch, loop, each, parallel, branch, and sub-workflow controls.

**What triggers are supported?**

Webhook, schedule, polling, app-event, manual, and form triggers.

**What programming languages can I use in workflows?**

Polyglot support: Java, JavaScript, Python, Ruby via GraalVM.

### Deployment

**How do I deploy ByteChef?**

- **Docker Compose**: Local development
- **Docker**: Manual setup with PostgreSQL
- **Kubernetes**: Production deployments (see documentation)
- **Workflows-as-APIs**: Expose workflows as authenticated HTTP endpoints

**What is Git-native versioning?**

ByteChef workflows are Git-friendly with JSON underneath. Push from the UI, environments backed by branches.

### Enterprise Features

**What enterprise features are available?**

Enterprise features include SSO/SCIM integration, advanced RBAC, microservices deployment, embedded mode, AI Copilot, and audit logging.

### Help & Resources

- **Documentation**: https://docs.bytechef.io
- **Discord**: https://discord.gg/VKvNxHjpYx
- **GitHub Issues**: Report bugs and request features
- **Roadmap**: https://github.com/orgs/bytechefhq/projects/3