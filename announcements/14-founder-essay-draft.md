# Why we chose open-core (and why we won't switch licenses)

> **Draft of the Option A founder follow-up essay.**
> Publish target: Apr 28–29, 2026 (7–10 days post-launch).
> Primary venue: `blog.bytechef.io/open-core` (new post).
> Republish: dev.to and Medium with canonical tags pointing back to the primary URL.
> Author: Ivica (or Igor/Matija — whoever owns the voice). Co-founder byline works either way.
> Length: ~1,950 words.

---

**Subtitle:** The open-source infrastructure business model is under pressure. Here's how we're trying to make open-core work without pulling a license switch.

---

Every open-source infrastructure company we grew up admiring has now changed its license.

Elastic moved to ELv2 in 2021. HashiCorp to BUSL in 2023. Redis to RSALv2 in 2024. MongoDB, CockroachDB, Confluent, Grafana — all of them. The automation space we operate in wasn't spared: n8n shipped under a Sustainable Use License from the start, and Airbyte moved to ELv2 in 2023. The list is long enough that "open-source infra company" and "eventual relicense" have become something close to a joke in engineering circles.

We started building ByteChef in 2022. We watched this pattern unfold in real time. When we picked Apache 2.0 for our core — a genuine OSI-approved license, with no restrictions on commercial use — we knew people would ask the obvious question: *when are you going to relicense?*

This essay is our answer. Not a press release. Not a promise written in press-release English that we'll quietly walk back in three years. An honest attempt to say what we're doing, why we think it works, and what we'd do instead if the business model ever needed to change.

---

## Why companies relicense (the honest version)

It's tempting to pretend the companies that relicensed did something wrong. They didn't, mostly. The pressure they faced is real.

Here's the actual dynamic. You spend five years building a piece of infrastructure software, open-source, under a permissive license. You grow a community. You make the product great. And then a large cloud vendor — AWS, typically, but not only — takes your open-source code, wraps it in a managed service, charges enterprise customers for it, and returns nothing to the project that made it possible. Not code. Not funding. Not even a clear public acknowledgement.

This is legal. It is, under the license you chose, exactly what you said anyone could do. But it also means that the company that's funding the ongoing development of the software — paying the salaries of the engineers who still respond to GitHub issues at midnight — is competing with a trillion-dollar cloud vendor that has massively better distribution and no R&D cost to amortize.

The business case for relicensing is easy to make in a board meeting. The business case for not relicensing is harder. You have to believe that long-term trust with your users is worth more than short-term protection against hyperscaler resale. You have to have a different monetization model that isn't undermined by the cloud taking your code. And you have to be willing to hold that line when the revenue pressure gets real.

We don't fault the companies that relicensed. We just wanted to find a way to avoid being forced into the same corner.

---

## Open-core — but with a real Apache 2.0 core

Our answer is open-core. Which is not a new model. What's new — or at least uncommon — is the specific way we drew the line.

Here's the split:

**Under Apache 2.0 (the open core):**
- The platform core — the workflow engine, scheduler, execution engine
- The component system — the framework that defines how integrations plug in
- All 180+ integrations — every Salesforce, HubSpot, Slack, Postgres, Stripe component
- The AI components — every LLM provider integration, MCP support, code execution
- The visual editor, the code editor, polyglot code workflows
- Every tool needed to build, run, and deploy real automations, at real scale, in production, for real commercial use

**Under a commercial Enterprise Edition license:**
- SSO / SAML / OIDC
- Role-based access control (RBAC)
- Multi-tenancy with workspace isolation
- Audit logging
- Encryption at rest with customer-managed keys
- Embedded iPaaS (white-label integrations for SaaS products resold to their customers)

The point of this split isn't to carve out the minimum viable open-source product and call the rest premium. It's the opposite. The Apache 2.0 core is designed to be **enough to get real value from, on its own, for the vast majority of users**. Solo developers, indie hackers, small agencies, mid-sized teams — the Apache 2.0 version is the whole product for you. You don't hit a feature wall. You don't get crippled by missing functionality. You can embed it in your commercial product, fork it, modify it, resell it, host it for your customers.

The Enterprise Edition contains the features that primarily matter to organizations with 50+ employees and specific compliance, identity, or multi-tenancy requirements. Those organizations have budgets for enterprise software. They pay for SSO everywhere — including for products they'd otherwise use for free. That's the monetization path that works without us having to worry about AWS wrapping the engine and selling it underneath us.

And the lines between the two are explicit and public. We're not going to quietly move features from Apache to EE over time. That's how you break trust. We're putting them in one bucket or the other, now, and leaving them there.

---

## Why this specific split

We spent more time on the question of *where* to draw the line than on whether to go open-core in the first place. Two principles guided us:

**Principle 1: If it's core to the work, it's Apache.** Building workflows, running them, integrating with third-party APIs, using AI components, deploying it yourself — all of that is the actual work of automation. It stays in the core. Forever. Even if we see a plausible path to monetize one of those capabilities, we're not going to close it. If we couldn't build a business on top of that core without clawing back something fundamental, then we'd have the wrong business model.

**Principle 2: If it only matters to large organizations, it's EE.** SSO isn't useful to a solo developer — their "org" is just them. RBAC with 30 roles isn't useful if your team is three people and you all trust each other. Audit logging is useful to everyone, but *compliance-grade* audit logging that an enterprise IT auditor signs off on is meaningfully different from "you can see what happened." The EE features are the ones where the incremental engineering cost of enterprise-grade is high, and the value accrues almost entirely to enterprise-sized customers. That's where we can charge without punishing the people who can't pay.

These principles aren't foolproof. There are edge cases. We'll probably get some of them wrong. But they're the principles, and they're written down.

---

## What we're committing to

Public commitments are easy to write and easy to regret. But trust requires specifics, so here are ours:

**We won't retroactively move features from Apache 2.0 to EE.** Everything in the core today, stays in the core. No "we realized SSO should be EE-only" quiet migrations. No "we're introducing a commercial-use restriction starting in version 2.0" pivots. If something is Apache 2.0 today, it is Apache 2.0 forever.

**We won't add restrictions to the Apache 2.0 license.** No "Sustainable Use" amendments. No "Business Source License" transitions. No field-of-use carveouts. Apache 2.0 is Apache 2.0. If we ever felt we needed to change the license of the core, we would fork the project and leave the Apache 2.0 version intact — not pull a relicense out from under existing users.

**We won't require a CLA that allows us to relicense.** We use the [Developer Certificate of Origin (DCO)](https://developercertificate.org/) for contributions. Contributors retain their copyright. That structurally prevents us (or any future acquirer) from relicensing the existing codebase to something more restrictive. This is intentional.

**We won't gate bug fixes, security patches, or performance improvements behind EE.** The Apache 2.0 core gets the same code quality and the same security patches as the EE. If there's a critical security fix, it goes to the Apache 2.0 branch. Full stop.

These aren't hypothetical rules we can quietly break later. They're the architecture of how we decided to build the business.

---

## What we'd do instead if the model needed to change

Let's be direct: this kind of commitment is only as good as the commercial model that backs it. If the EE doesn't pay the bills, we'd be forced to reconsider. We've thought about what "reconsider" would look like — and what it wouldn't.

**What we'd try first:**

- **A more aggressive hosted offering.** Managed ByteChef, run by us, with uptime SLAs. Many open-source companies make the majority of their revenue from hosting their own OSS project for customers who don't want to self-host. We have a hosted offering today and expect it to grow.
- **Premium support contracts.** Large organizations pay for 24/7 support, dedicated Slack channels, escalation paths, named engineers. The Apache 2.0 core stays open; the SLA is what they're buying.
- **Paid professional services.** Implementation, migration from other platforms, custom integration work, training. This doesn't touch the license at all.
- **More EE features.** If we need more commercial revenue, we'd add more EE-only features — as long as they pass Principle 2 above. More enterprise-grade governance, compliance certifications, specialized reporting.

**What we wouldn't do:**

- **Retroactively move something from Apache to EE.** Ever.
- **Add a "Sustainable Use" restriction to the core.** That's a license switch. It's the thing we're committing not to do.
- **Introduce an SLA that only applies if you're not self-hosting.** The point of self-hosting is that you're responsible for your own uptime — we're not going to punish the community version by making it feel second-class.

If we got to a point where none of the "what we'd try" options worked, the honest answer is: we'd have to consider whether to keep the company going, or whether to hand the core to a foundation and wind down the commercial entity. Leaving the core Apache 2.0 and foundation-stewarded is strictly better than relicensing. That's a hard thing to commit to publicly — but it's the commitment that makes the rest of it real.

---

## Why this matters for you

If you're a reader thinking about using ByteChef in production — or considering contributing to it — here's what the open-core-with-a-real-Apache-core approach actually enables:

**You can commercially embed ByteChef in your own product.** No restrictions. You can resell workflows built on ByteChef. You can white-label it internally. You can offer it as part of your own SaaS. Apache 2.0 is permissive on purpose.

**You can fork it.** If you disagree with a product decision we make, you can take the Apache 2.0 code, fork it, and build what you need. This is a real backstop, not just a theoretical right. It's the thing that makes "we won't relicense" mean something — if we ever did, the community could fork the last Apache 2.0 version and keep it alive.

**You can contribute without fear.** When you submit a PR to ByteChef, you retain your copyright. We don't require a CLA that lets us relicense your contribution. The code you wrote stays under the license you submitted it under.

**You can self-host without worrying about future surprises.** No "starting in v3.0, self-hosting requires a commercial license." No "the free tier now caps at 1,000 monthly runs." Self-hosting the Apache 2.0 core is, and will remain, unlimited and unrestricted.

**You can trust that the platform will be there in five years — with the same rules.** Not because we promise so (though we do), but because the legal structure — DCO-based contributions, Apache 2.0, explicit public commitments — makes it architecturally hard to change.

---

## Closing

We don't know if this model will work long-term. Nobody does. The companies that relicensed had the same intentions we do; the pressure that pushed them to change was real. We might face it too. We're hoping our commercial model, the way we've drawn the line between Apache and EE, and the explicit commitments we've made will let us thread the needle that others couldn't.

But if we fail at this, we'd rather fail honestly than succeed by quietly pulling a license switch five years from now. That's the version of the company we want to build.

If you've read this far — thank you for caring enough to ask the question. Licensing is not an exciting topic, and most users will never read a post like this. But for the ones who do: we see you. We know this matters. And we're taking it seriously.

ByteChef is [on GitHub](https://github.com/bytechefhq/bytechef) under Apache 2.0. If you want to fork it, embed it, ship it, change it — go for it. That's the deal.

— Ivica (with Igor and Matija)

---

## NOTES FOR PUBLISHING (don't publish these)

### Distribution plan
- **Primary:** `blog.bytechef.io/open-core` — publish Apr 28 or 29, Tuesday/Wednesday morning PT
- **HN submission:** "Why we chose open-core (and why we won't switch licenses)" — *not* a "Show HN" or "Launch HN"; submit as a blog post. HN tends to engage heavily with license posts.
- **Reddit:** r/opensource, r/programming (careful — programming subreddit enforces "no self-promotion" rules; frame as "we wrote up our thinking on licensing" not "check out our product")
- **LinkedIn:** Ivica/Igor/Matija personal posts linking to the essay with a 2-3 sentence summary
- **Twitter/X:** Thread of 5-7 tweets pulling the strongest quotes ("We won't retroactively move features from Apache 2.0 to EE" is a retweet-worthy line)
- **dev.to and Medium republish:** Use canonical tags pointing to `blog.bytechef.io/open-core`. Wait 3-5 days after primary publish so canonical signals settle.

### Edit pass before publishing
- Get one of Igor or Matija to read for tone. First-person plural should feel like we, not I.
- Check that no unshipped features are mentioned as shipped. The essay is about licensing, not product.
- Verify the DCO claim is accurate. If ByteChef uses a CLA today that *does* allow relicensing, this paragraph needs to change before publish. (**Action item for the author: confirm contribution structure before publishing.**)
- Fact-check the relicense dates (Elastic 2021, HashiCorp 2023, Redis 2024, MongoDB 2018, Airbyte 2023).
- Legal review: run past your counsel to make sure the "we won't relicense" commitment doesn't create liability issues for a future acquirer. Public commitments can be legally enforceable depending on jurisdiction.

### If this essay does well
- Follow up 4-6 weeks later with Option B ("What we got wrong building AI-native") or a technical essay on why GraalVM polyglot changed our architecture.
- Don't let it be a one-off. A second essay within 6-8 weeks signals active publishing; a dead blog after launch signals "we shipped a launch, now quiet again."

### If this essay doesn't do well
- That's fine. Not every essay lands. The value of writing and publishing this is less about launch traffic and more about establishing the public commitment. When a prospective enterprise customer asks "are you going to relicense?", you can send them the link. That's worth more than HN upvotes.
