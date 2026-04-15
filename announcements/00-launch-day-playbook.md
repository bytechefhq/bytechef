# ByteChef Launch Day Playbook — Tuesday, April 21, 2026

> **Target launch time:** 6:30am PT (Pacific Time) = 3:30pm CEST (Central European Summer Time)
> **Rationale:** HN peak traffic window is 6:30–8:30am PT when the US East Coast wakes up and the West Coast is starting the day.
> All times below are shown in **PT / CEST** so founders in both regions can follow along.

---

## WEEKEND (Friday April 17 – Sunday April 19) — Dress rehearsal

### Friday evening (Apr 17)
- Publish dev.to post as **scheduled draft** for Tuesday morning (or keep as draft to publish manually on launch day)
- Publish launch page at blog.bytechef.io/launch — UNLISTED or noindex'd if possible
- Share privately with advisors/friends; collect feedback

### Saturday (Apr 18)
- Apply feedback from advisors
- Ask ~10 friendly beta users to try the Docker quickstart cold and report broken things
- Record a 30–60 second demo GIF or video showing: drag-drop on canvas → LLM component → execution history
- Polish repo README — add hero image/GIF at the top, clear quickstart, CONTRIBUTING.md visible

### Sunday (Apr 19)
- Fix whatever beta users found
- Rewrite any copy that didn't land
- Prepare the HN pinned FAQ comment (draft it now, post it as soon as HN thread goes live)
- Prep email draft to personal network: brief, honest, "we're launching Tuesday"

---

## MONDAY (Apr 20) — Pre-launch day

### Morning (all timezones)
- **Final QA on blog.bytechef.io/launch** — test on mobile, test all CTAs, check analytics with UTM tags
- **Final QA on docs** — make sure Quickstart works end-to-end on a fresh machine
- **Brief the team**: who's monitoring HN comments, who's on Reddit, who's handling Discord influx
- **Warm up the repo**: make sure issues are triaged, CONTRIBUTING visible, Discord invite in README

### Afternoon
- **Pre-schedule social content where possible:**
  - LinkedIn post scheduled for Tuesday 9am CEST (or draft ready to publish)
  - Twitter thread in Typefully/Buffer, scheduled or draft
  - Reddit posts drafted with UTM links, stored for quick copy-paste
- **Send "we're launching tomorrow" note** to 20–50 close network contacts (investors, advisors, past customers) — no ask, just a heads-up so they can upvote/engage organically when they see it
- **DO NOT** ask anyone to upvote at a specific time — HN penalizes vote rings aggressively

#### Demo video placement checklist
Before anything else on Monday, confirm the demo video is uploaded and the URL is substituted everywhere. Search for `[DEMO_VIDEO_URL]` across the announcements folder — each occurrence needs to be replaced with the actual link.

- [ ] Demo video recorded, edited, uploaded to YouTube as **unlisted** (30–60s, muted-loop, shows visual step + Python code step + LLM component + execution history with traceable data)
- [ ] MP4 file exported for native upload to Twitter and LinkedIn
- [ ] Animated GIF exported for GitHub README (keep under 10MB)
- [ ] `[DEMO_VIDEO_URL]` replaced in `01-hackernews.md` (HN post body)
- [ ] `[DEMO_VIDEO_URL]` replaced in `05-discord.md` (Discord announcement)
- [ ] `[DEMO_VIDEO_URL]` replaced in `06-linkedin.md` (LinkedIn post — plus attach MP4 natively)
- [ ] `[DEMO_VIDEO_URL]` replaced in `07-twitter.md` (Twitter main tweet — plus attach MP4 natively)
- [ ] `[DEMO_VIDEO_URL]` replaced in `10-launch-blog.md` (launch blog hero — embed as autoplay-muted-loop)
- [ ] Video embedded/uploaded to dev.to post above the existing 7 images
- [ ] Animated GIF added to top of GitHub README
- [ ] Video tested on mobile viewport (launch blog hero is the critical check)

### Evening
- **Early night.** Launch day is a marathon.
- Phone on silent overnight, but charged and ready

---

## TUESDAY (Apr 21) — LAUNCH DAY

### T-90 minutes (5:00am PT / 2:00pm CEST) — Pre-flight check
- Coffee ☕
- **Check status of:** bytechef.io, GitHub repo, docs site, Discord, Cloud signup flow, Docker quickstart
- Verify the launch page is live and not noindex'd
- Verify dev.to post is ready to publish
- Open HN submission form in one tab
- Open the HN pinned comment draft in another tab

### T-0 (6:30am PT / 3:30pm CEST) — LAUNCH 🚀
- **Publish dev.to post** first (so HN visitors clicking through see fresh content)
- **Submit HN "Launch HN" post** — title: `Launch HN: ByteChef – Open-source alternative to Zapier/n8n with Apache 2.0 core`
  - Link: `https://blog.bytechef.io/launch?utm_source=hn&utm_medium=launch&utm_campaign=v025`
- **Within 60 seconds:** post the pinned FAQ comment on your HN thread covering: license, business model (open-core), comparison to n8n, why Java, self-host quickstart

### T+30 min (7:00am PT / 4:00pm CEST) — Initial engagement
- **Monitor HN thread** — respond to every question within 10 minutes
- Tone: warm, technical, honest. If something is coming soon, say so. If it's EE, say so.
- **DO NOT** upvote from multiple accounts, don't ask friends to upvote specific threads
- Share launch with the team privately so they can engage organically if they want

### T+2 hrs (8:30am PT / 5:30pm CEST) — Fan out to Reddit
Assuming HN is going well (on front page of "new" or climbing toward main page):

- **Post to r/selfhosted** — use `02-reddit-selfhosted.md`. Link with UTM `?utm_source=reddit&utm_medium=selfhosted`
- **Post to r/SaaS** — use `12-reddit-saas.md`. UTM `?utm_source=reddit&utm_medium=saas`
- Wait 2 hours between Reddit posts to avoid cross-subreddit spam flags
- Monitor and respond to comments on each

### T+4 hrs (10:30am PT / 7:30pm CEST) — LinkedIn + Twitter
- **Publish LinkedIn post** — use `06-linkedin.md`. Ivica posts from his personal account; Igor and Matija share/comment
- **Post main Twitter tweet** — use `07-twitter.md` Tweet 1. Schedule follow-up tweets every 30–60 minutes
- Ask the team to like/share organically, not in coordinated bursts

### T+6 hrs (12:30pm PT / 9:30pm CEST) — r/opensource + IndieHackers
- **Post to r/opensource** — use `03-reddit-opensource.md`. UTM `?utm_source=reddit&utm_medium=opensource`
- **Post to IndieHackers** — use `04-indiehackers.md`. UTM `?utm_source=ih`
- Start feeling tired. Power through — the first 8 hours matter most.

### T+8 hrs (2:30pm PT / 11:30pm CEST) — Discord + status check
- **Post Discord announcement** — use `05-discord.md`. This is for your existing community, lower stakes.
- **Evening status check:**
  - Current HN rank?
  - Any Reddit posts on top of their subreddits?
  - Total launch page visits (check analytics)
  - GitHub stars added today
  - Discord new members
  - Sign-ups on cloud (if tracking)

### T+12 hrs (6:30pm PT / 3:30am CEST Wednesday)
- **If you're in Europe: sleep.** Set an alarm for 8am CEST.
- Responses to HN/Reddit comments will resume in the morning
- Leave a brief "heading to bed, back in 6 hours for more questions" comment on HN if your thread is still active

---

## WEDNESDAY (Apr 22) — Launch day +1

### Morning (Europe waking up)
- Catch up on overnight HN comments, Reddit threads, Twitter replies
- Respond to everyone who asked a substantive question in the last 12 hours
- Check if any comments went viral — if so, engage thoughtfully

### Midday
- **Republish dev.to post on Medium** using Medium's Import Story feature — this auto-adds canonical tag back to dev.to. Zero extra writing.
- Post a short LinkedIn follow-up: "Launch update — thanks to everyone who engaged, here's what surprised us in the first 24 hours"

### Afternoon
- Analyze: which channel drove the most traffic? Signups? Stars?
- Share the early learnings in Discord to keep the community engaged

---

## THURSDAY–FRIDAY (Apr 23–24) — Sustain

- **Thursday:** Follow up on any HN threads still getting comments. Respond to issues opened in GitHub.
- **Friday:** Ship a small visible improvement (bug fix, docs update, community-requested feature). Post about it — "you asked, we shipped" signals active development.
- **By end of week:** Compile a launch retrospective document for internal use: what worked, what didn't, conversion by channel, signups/stars/Discord joins.

---

## WEEK 2+ (Apr 28 onwards) — Follow-up momentum

- **Tuesday, Apr 28 — 6:30am PT / 3:30pm CEST:** Publish the founder follow-up essay `14-founder-essay-draft.md` (Option A: "Why we chose open-core"). Same day-of-week as the launch so you catch the same HN/Reddit audience pattern.
  - **Monday Apr 27 evening:** Final essay polish, DCO/CLA verification, legal review check — per the notes at the bottom of `14-founder-essay-draft.md`.
  - **Primary URL:** `blog.bytechef.io/open-core`
  - **T+15 min:** Submit to HN as a regular blog post (NOT "Show HN" or "Launch HN" — those tags don't fit an essay).
  - **T+2 hours:** Post to r/opensource and r/programming.
  - **T+4 hours:** LinkedIn post from Ivica/Igor/Matija personal accounts.
  - **T+6 hours:** Twitter thread pulling the strongest quotes ("We won't retroactively move features from Apache 2.0 to EE").
  - **Friday May 1 or later:** Republish on dev.to and Medium with canonical tags pointing back to `blog.bytechef.io/open-core`.
- **Early May:** First "community showcase" — feature a workflow or integration built by a community member. Post to Discord, tweet, include in a brief newsletter.
- **Mid-May:** Second blog post. Keep the content rhythm going. A dead blog after launch kills momentum.

---

## ROLES FOR LAUNCH DAY

If all three founders are on deck:

**Ivica** — primary HN responder, LinkedIn poster, media-facing voice
**Igor** — technical questions (architecture, self-hosting, broker choices, stack)
**Matija** — community and Reddit moderator interactions, Discord monitoring

If only one person is on for a stretch, that person rotates between HN + Reddit + Discord every 20 minutes. Batch reading and responses to avoid burnout.

---

## WHAT TO DO IF IT DOESN'T TAKE OFF

Real talk: most launches don't hit the HN front page. If by T+4 hours you're still stuck on page 3 of HN "new":

1. **Don't panic. Don't resubmit.** Moderators will ban you for that.
2. **Keep engaging on the existing thread** — good comments sometimes resurrect a post
3. **Reddit may still work** — the feeds are more forgiving
4. **Plan a "Show HN" retry in 2-3 weeks** with a different angle:
   - "Show HN: We added AI Agents to our open-source automation platform"
   - "Show HN: ByteChef's n8n workflow converter — migrate in one click"
   - "Show HN: Why we built an embedded iPaaS on top of our open-source automation platform"
5. **LinkedIn and Twitter don't depend on HN** — those will drive their own traffic regardless

The goal of launch day isn't to win the internet. It's to establish the story, seed the repo with initial stars, and set up the next 90 days of content to compound.

---

## CRITICAL DO-NOTS

- Do not ask anyone to upvote on HN. Don't coordinate upvotes. HN will penalize you.
- Do not post the same content verbatim across multiple subreddits. Reddit auto-flags cross-posts.
- Do not use multiple accounts to comment on your own posts. It's obvious and kills trust.
- Do not respond to trolls or bad-faith comments. Ignore, move on, let the community handle them.
- Do not delete negative comments or questions. It's Streisand effect.
- Do not make the launch page a sign-up wall. Docker quickstart should work without an account.
- Do not announce a cloud waitlist without a clear use case — self-hosted is the hero on launch day.

---

## QUICK TIME ZONE REFERENCE

| PT | CEST | US ET | London |
|----|------|-------|--------|
| 6:30am | 3:30pm | 9:30am | 2:30pm |
| 8:00am | 5:00pm | 11:00am | 4:00pm |
| 10:00am | 7:00pm | 1:00pm | 6:00pm |
| 12:30pm | 9:30pm | 3:30pm | 8:30pm |
| 2:30pm | 11:30pm | 5:30pm | 10:30pm |

---

## FINAL CHECKLIST (Print this for launch morning)

### 30 minutes before launch
- [ ] blog.bytechef.io/launch returns 200 and loads fast
- [ ] Launch page is indexable (no noindex tag)
- [ ] GitHub repo: README looks good, CONTRIBUTING.md visible, recent activity
- [ ] Docker quickstart runs on fresh machine
- [ ] Discord invite link works
- [ ] Cloud signup works (if applicable)
- [ ] All three founders are awake and at their laptops
- [ ] HN post draft is ready to submit
- [ ] HN pinned FAQ comment is ready to post
- [ ] Reddit drafts are ready
- [ ] LinkedIn draft is ready
- [ ] Twitter thread is ready
- [ ] Analytics dashboard is open in a tab
- [ ] Coffee ☕

### Launch moment
- [ ] Publish dev.to post
- [ ] Submit HN "Launch HN" post
- [ ] Post HN pinned FAQ comment

### First hour
- [ ] Respond to every HN comment within 10 minutes
- [ ] Monitor for broken links or bugs reported
- [ ] Stay on one channel at a time — don't context-switch faster than every 20 min

---

Good luck. You only launch once.
