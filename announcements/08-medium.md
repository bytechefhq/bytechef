# Medium — Republish strategy

> Medium is not an original-content surface for this launch. It's a **republish of the dev.to "Welcome to ByteChef" post** using Medium's Import Story feature, with a canonical tag pointing back to dev.to.

---

## Why republish instead of write a separate Medium post

1. **Writing two long-form pieces saying the same thing dilutes both.** The dev.to post already covers the full "Welcome to ByteChef" narrative well. A separate Medium piece would either duplicate it or create messaging drift.
2. **Medium's audience is drive-by readers, not subscribers.** Unlike a tech blog where people follow authors, Medium visitors usually land via search or curated feeds. A canonical republish captures that audience without requiring original content.
3. **Canonical tags consolidate SEO credit.** Medium's Import Story feature preserves the canonical link back to the original URL. You get Medium's reach without losing search credit to medium.com.
4. **You don't have bandwidth for two versions.** The founder team has limited time in launch week. Focus that time on live engagement (HN comments, Reddit threads, Discord) rather than rewriting content.

---

## When to republish

**Target:** 3–5 days AFTER the dev.to post goes live on launch day (April 21).
- **Launch day (Apr 21):** dev.to post goes live. HN/Reddit/Twitter/LinkedIn all link to it and to `blog.bytechef.io/launch`.
- **Apr 24–25 (Thursday–Friday):** Publish the Medium republish. Waiting 3–5 days lets Google index the dev.to URL as canonical first, so the Medium version is correctly attributed as a secondary copy.

Do NOT publish Medium on launch day. If Google indexes both simultaneously without canonical signals settled, you risk the Medium version competing with dev.to in search results.

---

## How to republish (step-by-step)

Medium has a built-in "Import Story" feature that handles everything — canonical tag, images, formatting — automatically.

1. Go to **https://medium.com/p/import**
2. Paste the dev.to post URL: `https://dev.to/bytechef/welcome-to-bytechef`
3. Medium imports the content, preserves the canonical tag, and creates a draft
4. **Review the import:**
   - Confirm all 7 images came through correctly
   - Check that formatting (headings, code blocks, bold) survived
   - Fix any markdown → Medium conversion artifacts (rare but worth checking)
5. **Verify the canonical tag is set correctly.** In the Medium story settings, confirm:
   - Canonical URL → `https://dev.to/bytechef/welcome-to-bytechef`
6. **Publish.** Tags: `automation`, `open-source`, `ai`, `workflow`, `startup`
7. **Submit to Medium publications** if relevant: [Better Programming](https://betterprogramming.pub/), [Level Up Coding](https://levelup.gitconnected.com/), or [The Startup](https://medium.com/swlh) — each has a submission process and can expand reach.

---

## What to write if you DO want original Medium content later

If the Medium republish does well, consider writing the follow-up founder essay (`14-founder-essay-draft.md`) as the *primary* on Medium instead of `blog.bytechef.io/open-core`. That gives Medium a surface where ByteChef has original content, not just republishes.

But do that for the follow-up — not for the launch.

---

## Tracking

- Use a different UTM parameter for the Medium version so you can measure the incremental traffic separately from dev.to:
  - dev.to canonical URL receives `?utm_source=devto`
  - Medium republish links add `?utm_source=medium` in the article CTAs
  - Both flow to `blog.bytechef.io/launch` for signups

---

## Implementation checklist

- [ ] dev.to post is live and indexed (check via `site:dev.to bytechef` on Google 2–3 days post-launch)
- [ ] Medium account exists and is linked to ByteChef brand
- [ ] Import Story tested on a draft first (use a test post to confirm the flow works)
- [ ] Canonical tag verified on the imported draft
- [ ] All 7 images present and rendering correctly
- [ ] UTM parameters added to CTAs
- [ ] Publication submission prepared (Better Programming or similar)
- [ ] Announced once on Twitter/LinkedIn ("For the Medium readers among you: [link]") — don't over-promote, it's a republish
