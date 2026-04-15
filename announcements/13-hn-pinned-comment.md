# HN Pinned FAQ Comment — Launch Day

> **When to post:** Within 60 seconds of submitting the Launch HN thread.
> **Why:** The first comment on your own Launch HN thread sets the tone for the whole conversation. It pre-answers the most predictable questions, shows the founders are present and engaged, and gives readers a place to land before scrolling comments.
> **Length target:** 400–600 words. HN readers skim — don't make them work.

---

## THE COMMENT

Thanks for checking out ByteChef! Igor, Matija, and I (Ivica) are here all day to answer questions. Between us we've spent 30+ years building integration and workflow infrastructure — ByteChef is our third-generation take on it. A few things we figured would come up first:

**On the license and business model**

ByteChef is open-core. The platform core, the workflow engine, the component system, AI components, and all 180+ integrations are Apache 2.0 — no "source-available" fine print. Commercial use is fine. Forks are fine. Embedding it in your product is fine.

Advanced features aimed at larger organizations live in a commercial Enterprise Edition: SSO/SAML/OIDC, RBAC, multi-tenancy, API key management, audit logging, encryption at rest, and Embedded iPaaS (for SaaS companies that want to resell integrations to their customers). That's how we fund ongoing development.

We know the open-core → relicense pattern is a real concern (HashiCorp, Elastic, Redis, n8n all did it). Our split is designed to avoid that path: what's Apache 2.0 today is what a solo dev or small team needs to get real value, and we're not planning to move those features into EE. If our business model ever needed to change, we'd look at other options (hosted offering, support contracts, priced EE tiers) before touching the core license.

**What's ready today vs. coming soon**

Today: visual workflow editor, 180+ integrations, polyglot code workflows (JS/Python/Ruby/Bash on GraalVM), 13+ LLM providers as workflow components, MCP consumption and workflow-building-via-MCP, self-host on Docker/K8s, multiple message brokers and storage backends, multi-environment deployment.

In active development, ordered by shipping sequence:

**Shipping next:** AI Agents (tool use, memory, skills, AI guardrails), streaming chat workflows, human-in-the-loop approvals, Template library.

**After that:** AI Copilot, Data Tables, Knowledge Base (RAG with 12+ vector stores), MCP Server (expose workflows as MCP tools).

**Later:** AI Gateway with observability (LLM tracing, prompt management, evaluations, alerting, rate limiting, playground), Embedded iPaaS for SaaS (commercial EE, early alpha in exploration), n8n workflow converter, and more.

**Pricing**

We have per-task billing, but the rate is consistent across packages — so costs scale predictably instead of jumping tiers. Component testing doesn't count against your quota. Full pricing is on the site.

**Tech stack questions we expect**

- **Why Java?** Mature concurrency primitives, strong ecosystem for enterprise integration (connection pooling, message brokers, transaction management), GraalVM gives us polyglot code execution for free. We're aware Java isn't popular on HN; we made the choice consciously.
- **How does self-host work?** `docker compose up` gets you running in ~2 min. K8s via Helm is documented. You can start as a monolith and split into microservices when you actually need to.
- **Where's data stored?** Wherever you point it — PostgreSQL + your choice of filesystem / S3 / database for files. Nothing phones home.
- **How are credentials handled?** Encrypted at rest, per-workspace keys, no plaintext in logs.

**How to try it**

Quickstart: [link to docs]
Repo: https://github.com/bytechefhq/bytechef
Launch page with the full story: https://blog.bytechef.io/launch

**What we'd love feedback on**

- Which integrations are missing for your use case?
- If you've used n8n, Zapier, or Make — what frustrated you that we should avoid?
- For those doing embedded iPaaS today — what do you wish you could change?

Fire away with questions. We'll be here.

---

## NOTES FOR IVICA (DON'T POST THESE)

### Adjust before posting
- Replace `[link to docs]` with the actual docs URL (docs.bytechef.io/quickstart or similar)
- If any "coming soon" features have shipped by launch day, move them to "Today"
- If pricing page URL differs from bytechef.io/pricing, update

### If you need to cut
The comment is ~500 words. If you want to trim to ~350:
- Cut the "Tech stack questions we expect" section (Java / self-host / data / credentials)
- Those questions will come up in comments anyway, and answering them inline as they arrive is more engaging than pre-empting them

### Tone check
- First person plural ("we"), first name upfront (Ivica, Igor, Matija) — humanizes it
- Explicit honesty: "not vaporware, but also not shipped yet" signals credibility
- Acknowledges the elephant in the room (open-core → relicense anxiety) without being defensive
- "We're aware Java isn't popular on HN" — preempts a predictable pile-on by naming it yourself
- No emojis, no marketing language, no exclamation points beyond the opener

### Common HN follow-ups to prepare for
Have mental (or written) answers ready for these — they WILL come up:

1. **"What stops you from relicensing the core once you get traction?"**
   → Answer honestly. Something like: "Nothing legally stops any Apache 2.0 project from forking. What stops us practically is that our business thesis is explicitly that the core stays open — EE features are the monetization path, not a rug pull. If we ever needed to change the business model, we'd look at hosted offerings, support contracts, or priced EE tiers before touching core licensing."

2. **"How is this different from n8n?"**
   → We respect what n8n has built — they're a great team. The real differences are:
   (1) **Target audience** — n8n is designed for technical teams. ByteChef is designed for both technical and business users, without forcing the technical side to give up flexibility. A non-engineer can ship a real workflow; an engineer can drop into JS, Python, Ruby, or Bash when they need to.
   (2) **License** — ByteChef's core is Apache 2.0 (OSI-approved). n8n uses a Sustainable Use License, which isn't OSI-approved and restricts certain commercial and hosted use cases. Objective, verifiable.
   (3) **Polyglot code workflows** — JavaScript, Python, Ruby, and Bash via GraalVM, all mixable in the same workflow. n8n supports JS and limited Python.
   (4) **Architectural flexibility** — monolith-to-microservices split, choice of message broker (Redis, RabbitMQ, Kafka, SQS), choice of storage backend. n8n is more opinionated.
   (5) **Embedded iPaaS** (upcoming, EE) — for SaaS companies that want to offer integrations to their customers under their own brand.
   (6) **MCP support in both directions** — consume MCP tools and build workflows through MCP today; expose workflows as MCP tools coming soon.
   Both platforms have visual editors, both have LLM integrations, both support self-host. The AI capabilities comparison is converging and I'd be skeptical of anyone claiming otherwise — including us.

3. **"How is this different from Activepieces?"**
   → Similar open-source automation space. Different stack (Java vs TypeScript), different feature emphasis (ours leans into polyglot code workflows plus an explicit governance layer for AI), different license approach (both Apache 2.0, but our EE/OSS split is explicit).

4. **"Why not use Temporal / Airflow / Prefect?"**
   → Those are developer-first workflow orchestrators. ByteChef is built for both technical and business users — the visual editor, 180+ pre-built integrations, and no-code path are table stakes for the market we're targeting. Code workflows are there when you need them, but they're not the only way in.

5. **"Have you raised funding?"**
   → Be honest about whatever the truth is. If bootstrapped, say so — bootstrapped open-source projects get HN sympathy. If funded, share the round and lead investor.

6. **"What about [specific competitor]?"**
   → Acknowledge fairly, differentiate clearly, don't trash-talk. HN readers notice and punish FUD.

### Engagement pacing
- First 2 hours: respond within 10 minutes to every comment
- Hours 2–4: respond within 20 minutes
- Hours 4–8: respond within an hour
- After hour 8: respond when you can, but don't stay up past midnight unless the thread is actively climbing
