# Docs Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Take `docs/` from 528 broken internal links and a parallel marketing tree to zero broken links, one guidance-voiced tree, and machine-readable page status.

**Architecture:** Six phases, each independently landable. Durable links are repaired first (they need no decisions), then navigation structure is completed (missing index pages and `meta.json` files, which are *why* several links are unfixable), then the 49-page Enterprise tree is folded into the product guides it shadows, then links sweep to zero, then every hand-written page is checked against the code, then Fumadocs components and frontmatter status flags land.

**Tech Stack:** Fumadocs 16.14.4 (`fumadocs-mdx`, `fumadocs-ui`, `fumadocs-core`), Next.js, MDX, `next-validate-link`, `bun` (devDependency), Zod frontmatter schema.

**Spec:** `docs/superpowers/specs/2026-08-18-docs-remediation-design.md`

**Refinement vs. the spec:** the spec orders phases link-repair → fold → duplicates. This plan inserts navigation structure (`meta.json`, section-link resolution) *before* the fold, because three of the spec's "unambiguous" rewrites are false positives caused by directories that have no index page, and several of the 54 dead targets are links to section landings that do not exist.

**Section-link rule:** a link pointing at a directory resolves to the **first available page in that directory's declared nav order** — skipping `--- separator ---` entries, and recursing into a folder if the first entry is one. No new index pages are authored: 24 platform directories lack an index, and landing pages nobody asked for are content to write and then keep true.

## Global Constraints

- **Set `SCRATCH` once, in every shell you use.** Every task writes its intermediate artifacts there:

  ```bash
  export SCRATCH=/private/tmp/claude-502/-Volumes-Data-bytechef-bytechef/b63ed507-8ae0-49b5-92cb-9dc8689a42b6/scratchpad
  mkdir -p "$SCRATCH"
  ```

  Any session-scratch directory works; nothing under `$SCRATCH` is ever committed.
- **Working directory is `docs/`** for every command unless stated otherwise. The shell's cwd
  persists between commands — several tasks `cd` to the repo root to commit and back to `docs/` to
  validate, so state the directory explicitly rather than assuming.
- **`bun` is not on `PATH`.** Always invoke `./node_modules/.bin/bun`. Likewise `./node_modules/.bin/fumadocs-mdx`.
- **`npm run types:check` gate.** The docs `tsconfig.json` carries a pre-existing
  `TS5101: Option 'baseUrl' is deprecated` error, so the command exits 2 regardless of content.
  Task 5 adds `"ignoreDeprecations": "6.0"` to silence it, after which the gate means what it
  says. Until then, and if it ever reappears, the gate is **"no error other than that one
  TS5101 line"** — check the error list, not just the exit code.
- **`npm run lint` can die before the link validator ever runs.** The `fumadocs-mdx` step that
  precedes it fetches every *remote* image to measure it, and `content/docs/reference/components/`
  (generated, never edit) references an image on `raw.githubusercontent.com`. When GitHub answers
  `429`, the whole pipeline aborts with an `ImageSizeError` and the validator produces **no output
  at all** — so `grep -c not-found` returns `0`, which reads exactly like a clean run. Never accept
  a zero without confirming the validator actually ran: the real output contains a line per broken
  link plus a summary. On `ImageSizeError`/`429`, just re-run — it is transient.
- **Never flip a "Coming soon" marker to available.** These docs track the latest *released*
  version — **`v0.31.4`, released 2026-08-13** — not this branch and not `master`, both of which carry
  large amounts of unreleased work. Finding the implementing code proves nothing about release
  status. Phase F converts these markers to a `comingSoon` frontmatter flag: that is a **format**
  change and is in scope. Changing a page's *status* is not, and needs the user. Removing a
  marker is the one edit in this plan that can make the docs actively lie.
  **Release status is judged on the user-facing surface, not on backend presence.** A feature
  whose service or table exists at the release tag but which has no UI, no CLI and no API a
  user can reach there has **not** shipped, and its page is marked coming-soon in full. Check
  the client too: `git ls-tree -r --name-only v0.31.4 -- client/src | grep -i <feature>`.
  (Identifying the latest release: **`gh release list --repo bytechefhq/bytechef`** — releases live
  on the `upstream` remote, NOT `origin`, which is a personal fork and lags. A local `git tag`
  listing is not authoritative and was stale by a full release during this work. If you do read
  local tags, sort by `--sort=-creatordate`, never `-v:refname`: the higher-numbered `v1.1.x`
  tags are from 2025-12 and are ancestors of the v0.31.x line. Ignore `pre-rebase-*` /
  `backup-before-*` working tags.)
- **The link validator is the test.** `./node_modules/.bin/fumadocs-mdx && ./node_modules/.bin/bun ./scripts/lint.ts`. It must be re-run after every content change; a phase is not done until its expected instance count is met.
- **Never edit the GENERATED trees.** Precisely, these three and only these three:
  `content/docs/reference/components/**`, `content/docs/reference/flow-controls/**` (both written by
  `buildSrc/src/main/kotlin/com.bytechef.documentation-generator.gradle.kts:720-721`) and
  `content/docs/openapi/(generated)/**`. Read them freely as evidence; never write to them.
  **Their parent directories are NOT generated.** `content/docs/reference/{index.mdx,expressions.md,
  meta.json}` and `content/docs/openapi/{index.mdx,meta.json}` are hand-written and in scope —
  an earlier blanket `reference/**` / `openapi/**` prohibition wrongly locked those five files,
  including one Task 18 is assigned to edit. They are link *targets*, never edit targets.
- **Fragments must be preserved.** Every rewrite maps the path portion only and re-attaches the original `#fragment` verbatim. The validator reports `invalid-fragment` only for fragments that are present and wrong — never for fragments that were silently deleted.
- **`invalid-fragment` count must be 0 at every phase boundary.** It is 0 today.
- **Commit message convention:** `732 docs - <description>`. Never amend; always fresh commits (the user commits to `0_732` in parallel).
- **Stage only files this task changed.** Do not `git add -A`.
- **Do not document microservice / distributed deployments.** Per the user's standing
  instruction, the coordinator/worker microservice deployment model is suppressed **for now**.
  `use-bytechef/self-hosted/installation/distributed.mdx` carries a "Coming soon" marker and its
  content stays put; **no other page may describe, recommend, or cross-reference distributed
  deployment**, and no task may reintroduce it. This is a real trap for the Phase E accuracy pass:
  the distributed apps genuinely exist under `server/ee/apps/`, so a task verifying claims against
  the codebase will find supporting code and helpfully document the model back in. Finding the code
  is not permission to document it. Where an existing sentence needs a distributed caveat to stay
  true (a feature that behaves differently across deployments), drop the caveat and state the
  monolith behaviour plainly rather than naming the other model. The runtime job runner
  (`self-hosted/runtime-job.mdx`) is **not** covered by this: it is a single-shot, single-process
  binary — the opposite of a microservice — and only its cross-references to distributed mode are
  removed.

  When sweeping for this, search widely (`microservice`, `coordinator-app`, `worker-app`,
  `api-gateway`, `config-server`, `execution-app`, `webhook-app`, `scheduler-app`,
  `configuration-app`, `connection-app`, `coordinator/worker`, `service discovery`, `spring cloud`,
  `eureka`, `helm`, `distributed`) and then read every hit, because four of those terms produce
  **false positives that must be left alone**: "Spring Cloud AWS" is the S3 client library
  (`configuration/file-storage.md`); "ByteChef is distributed as a single container image" means
  *shipped*, not clustered (`installation/aws-ec2.md`); and the Helm chart deploys the **monolith**,
  so every `helm` hit across `installation/{kubernetes,azure,google-cloud}.md` is legitimate. A
  blanket removal on term match would delete correct installation instructions.
- **Voice:** guidance, not positioning. "How you use it and what it does", never "why it is compelling". No superlatives, no competitive framing.
- **Scratch files** go in the session scratchpad, never in the repo. Rewrite scripts are throwaway and are not committed.

---

## Phase A — Durable link repair

### Task 1: Establish the validator baseline and the reviewed rewrite map

**Files:**
- Create (scratch, not committed): `<scratchpad>/linkcheck-before.log`, `<scratchpad>/link-map.json`, `<scratchpad>/rewrite.mjs`

**Interfaces:**
- Consumes: nothing.
- Produces: `link-map.json` — a flat `{ "<old-path>": "<new-path>" }` object with 59 entries, consumed by Task 2's `rewrite.mjs`.

- [ ] **Step 1: Capture the baseline**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
./node_modules/.bin/bun ./scripts/lint.ts > "$SCRATCH/linkcheck-before.log" 2>&1
grep -cE '^[^ ].*: not-found' "$SCRATCH/linkcheck-before.log"
grep -cE '^[^ ].*: invalid-fragment' "$SCRATCH/linkcheck-before.log" || true
```

Expected: `528` not-found, `0` invalid-fragment. If either differs, stop — the tree has changed since the spec was measured and the map below must be regenerated before proceeding.

- [ ] **Step 2: Write the reviewed rewrite map**

This map covers only the **durable** targets — neither the source file nor the destination page is deleted by the Enterprise fold. It is path-only; fragments are handled by the script, not the map.

Three entries were deliberately excluded because automated suffix-matching resolved them incorrectly, and Task 3 creates the pages they actually want:
`/automation/build/workflows` (matched the *embedded* page), `/embedded` (matched `/openapi/embedded`, the API reference), and any target resolving into `/platform/enterprise/*`.

Write to `$SCRATCH/link-map.json`:

```json
{
  "/automation/api-platform": "/platform/automation/deploy/api-platform",
  "/automation/asset-files": "/platform/automation/data/asset-files",
  "/automation/build/build-approaches": "/platform/automation/build/workflows/build-approaches",
  "/automation/build/data-pills": "/platform/automation/build/workflows/data-pills",
  "/automation/build/data-streams": "/platform/automation/build/workflows/data-streams",
  "/automation/build/human-in-the-loop": "/platform/automation/build/workflows/human-in-the-loop",
  "/automation/build/subflows": "/platform/automation/build/workflows/subflows",
  "/automation/build/universal-ai": "/platform/automation/build/workflows/ai/universal-ai",
  "/automation/connect-data/authentication": "/platform/automation/build/connections/authentication",
  "/automation/connect-data/troubleshooting": "/platform/automation/build/connections/troubleshooting",
  "/automation/deploy/projects": "/platform/automation/build/workflows/projects",
  "/automation/knowledge-base/add-documents": "/platform/automation/data/knowledge-base/add-documents",
  "/automation/knowledge-base/create-a-knowledge-base": "/platform/automation/data/knowledge-base/create-a-knowledge-base",
  "/automation/knowledge-base/search": "/platform/automation/data/knowledge-base/search",
  "/automation/knowledge-base/sources": "/platform/automation/data/knowledge-base/sources",
  "/automation/knowledge-base/use-in-workflows": "/platform/automation/data/knowledge-base/use-in-workflows",
  "/automation/quick-start/configure-workflow-trigger": "/platform/automation/get-started/quick-start/configure-workflow-trigger",
  "/automation/quick-start/star-repository-on-github": "/platform/automation/get-started/quick-start/star-repository-on-github",
  "/automation/templates": "/platform/automation/build/workflows/templates",
  "/automation/workflow-chats": "/platform/automation/build/with-ai/hub/workflow-chats",
  "/automation/workflow-chats/advanced": "/platform/automation/build/with-ai/hub/workflow-chats/advanced",
  "/automation/workflow-chats/enable-chat": "/platform/automation/build/with-ai/hub/workflow-chats/enable-chat",
  "/automation/workflow-chats/using-chats": "/platform/automation/build/with-ai/hub/workflow-chats/using-chats",
  "/deploy/self-hosted/configuration/environment-variables": "/platform/use-bytechef/self-hosted/configuration/environment-variables",
  "/deploy/self-hosted/deployment/aws-ec2": "/platform/use-bytechef/self-hosted/installation/aws-ec2",
  "/deploy/self-hosted/deployment/aws-ecs": "/platform/use-bytechef/self-hosted/installation/aws-ecs",
  "/deploy/self-hosted/deployment/azure": "/platform/use-bytechef/self-hosted/installation/azure",
  "/deploy/self-hosted/deployment/digitalocean": "/platform/use-bytechef/self-hosted/installation/digitalocean",
  "/deploy/self-hosted/deployment/google-cloud": "/platform/use-bytechef/self-hosted/installation/google-cloud",
  "/deploy/self-hosted/deployment/kubernetes": "/platform/use-bytechef/self-hosted/installation/kubernetes",
  "/deploy/self-hosted/deployment/local-docker": "/platform/use-bytechef/self-hosted/installation/local-docker",
  "/developer-guide/architecture": "/platform/use-bytechef/self-hosted/architecture",
  "/embedded/app-events": "/platform/embedded/build/app-events",
  "/embedded/automations": "/platform/embedded/build/automations",
  "/embedded/connected-users": "/platform/embedded/monitor/connected-users",
  "/embedded/field-mapping": "/platform/embedded/build/workflows/field-mapping",
  "/embedded/getting-started/adding-an-integration": "/platform/embedded/get-started/initial-setup/adding-an-integration",
  "/embedded/getting-started/displaying-the-connect-dialog": "/platform/embedded/get-started/initial-setup/displaying-the-connect-dialog",
  "/embedded/getting-started/installing-the-sdk": "/platform/embedded/get-started/initial-setup/installing-the-sdk",
  "/embedded/integrations": "/platform/embedded/build/integrations",
  "/embedded/permission-expressions": "/platform/embedded/build/permission-expressions",
  "/embedded/sample-app": "/platform/embedded/get-started/quick-start/sample-app",
  "/embedded/tenant-isolated-security": "/platform/embedded/get-started/tenant-isolated-security",
  "/embedded/white-label-execution": "/platform/embedded/get-started/white-label-execution",
  "/glossary": "/platform/glossary",
  "/platform/ai-gateway": "/platform/automation/deploy/ai-gateway",
  "/platform/ai-providers": "/platform/settings/ai-providers",
  "/platform/ai/agent": "/platform/automation/build/workflows/ai/agent",
  "/platform/ai/agent/agent-utils": "/platform/automation/build/workflows/ai/agent/agent-utils",
  "/platform/ai/agent/evals": "/platform/automation/build/workflows/ai/agent/evals",
  "/platform/ai/agent/guardrails": "/platform/automation/build/workflows/ai/agent/guardrails",
  "/platform/ai/agent/skills": "/platform/automation/ai/skills",
  "/platform/ai/agentic-patterns": "/platform/automation/build/workflows/ai/agentic-patterns",
  "/platform/automation/quick-start/build-first-workflow": "/platform/automation/get-started/quick-start/build-first-workflow",
  "/platform/copilot": "/platform/automation/build/with-ai/copilot",
  "/platform/deploy/cloud": "/platform/use-bytechef/cloud",
  "/platform/deploy/self-hosted": "/platform/use-bytechef/self-hosted",
  "/platform/deploy/self-hosted/configuration/environment-variables": "/platform/use-bytechef/self-hosted/configuration/environment-variables",
  "/platform/mcp-server": "/platform/settings/mcp-server"
}
```

- [ ] **Step 3: Verify the map has 59 entries and no doomed destinations**

```bash
node -e "
const m=require('$SCRATCH/link-map.json');
const k=Object.keys(m);
console.log('entries:', k.length);
const doomed=k.filter(x=>m[x].startsWith('/platform/enterprise'));
console.log('destinations inside platform/enterprise (must be 0):', doomed.length);
"
```

Expected: `entries: 59`, `destinations inside platform/enterprise (must be 0): 0`.

The spec says "87 distinct targets" for the same set. Both are right: 87 counts each distinct
`path#fragment` string, 59 counts distinct paths. The map is keyed by path because the script
re-attaches fragments itself — 59 is the number to expect here.

- [ ] **Step 4: Commit nothing**

Task 1 produces only scratch artifacts. There is nothing to commit. Proceed to Task 2.

---

### Task 2: Apply the durable rewrites with fragment preservation

**Files:**
- Modify: every `content/docs/**/*.md` and `*.mdx` outside `reference/` and `openapi/` that contains a mapped path
- Create (scratch): `<scratchpad>/rewrite.mjs`

**Interfaces:**
- Consumes: `link-map.json` from Task 1.
- Produces: a tree whose validator not-found count is 340.

- [ ] **Step 1: Write the rewrite script**

Two link forms carry absolute paths in this corpus: markdown `](/path)` (618 occurrences) and JSX `href="/path"` (22). Both must be handled. Relative links (`](./x)`, `](../x)`) are never broken and must not be touched.

The ordering of `sort((a,b)=>b.length-a.length)` is load-bearing: `/platform/ai/agent` is a prefix of `/platform/ai/agent/evals`, and rewriting the short key first would corrupt the long one.

```js
// $SCRATCH/rewrite.mjs
import fs from 'node:fs';
import path from 'node:path';

const map = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const root = 'content/docs';
// Longest key first so /platform/ai/agent never eats /platform/ai/agent/evals.
const keys = Object.keys(map).sort((a, b) => b.length - a.length);

let files = 0;
let edits = 0;

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'reference' || entry.name === 'openapi') continue;
      walk(full);
    } else if (/\.mdx?$/.test(entry.name)) {
      rewrite(full);
    }
  }
}

function rewrite(file) {
  const original = fs.readFileSync(file, 'utf8');
  let text = original;

  for (const oldPath of keys) {
    const newPath = map[oldPath];
    // Capture group 2 is the optional fragment/query tail; it is re-attached verbatim.
    // The trailing (?=[)"]) ensures we only match a complete link target, so
    // /embedded never matches inside /embedded/app-events.
    const markdown = new RegExp(`\\]\\(${escape(oldPath)}((?:#|\\?)[^)]*)?\\)`, 'g');
    const href = new RegExp(`href="${escape(oldPath)}((?:#|\\?)[^"]*)?"`, 'g');

    text = text.replace(markdown, (_m, tail = '') => `](${newPath}${tail || ''})`);
    text = text.replace(href, (_m, tail = '') => `href="${newPath}${tail || ''}"`);
  }

  if (text !== original) {
    fs.writeFileSync(file, text);
    files++;
    edits += countDiff(original, text);
  }
}

function countDiff(a, b) {
  // Rough instance count: number of replaced occurrences.
  let n = 0;
  for (const oldPath of keys) {
    const re = new RegExp(escape(oldPath) + '(?=[)"#?])', 'g');
    n += (a.match(re) || []).length - (b.match(re) || []).length;
  }
  return n;
}

function escape(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

walk(root);
console.log(`rewrote ${edits} link instances across ${files} files`);
```

- [ ] **Step 2: Dry-run against a single file first**

```bash
cd docs
cp content/docs/platform/settings/ai-providers.mdx "$SCRATCH/ai-providers.before"
grep -n 'deploy/self-hosted/configuration/environment-variables' content/docs/platform/settings/ai-providers.mdx
```

Expected: one hit, `.../environment-variables#ai-provider-api-keys`. This is the fragment-preservation canary — it must survive with its `#ai-provider-api-keys` intact.

- [ ] **Step 3: Run the rewrite**

```bash
cd docs
node "$SCRATCH/rewrite.mjs" "$SCRATCH/link-map.json"
```

Expected: roughly `rewrote 188 link instances across N files` (N is about 60-70).

The script's instance counter is approximate by construction — it re-counts occurrences rather than
tracking each substitution. Treat it as a smoke signal only. **The authoritative check is Step 5's
validator count of 340**; do not tune the script to make this number read exactly 188.

- [ ] **Step 4: Verify the fragment canary survived**

```bash
grep -n 'environment-variables#ai-provider-api-keys' content/docs/platform/settings/ai-providers.mdx
```

Expected: one hit, now reading `/platform/use-bytechef/self-hosted/configuration/environment-variables#ai-provider-api-keys`. If the `#ai-provider-api-keys` is gone, the script's capture group is broken — fix it and `git checkout content/docs` before retrying.

- [ ] **Step 5: Re-run the validator**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
./node_modules/.bin/bun ./scripts/lint.ts > "$SCRATCH/linkcheck-after-a.log" 2>&1
grep -cE '^[^ ].*: not-found' "$SCRATCH/linkcheck-after-a.log"
grep -cE '^[^ ].*: invalid-fragment' "$SCRATCH/linkcheck-after-a.log" || echo 0
```

Expected: `0` invalid-fragment (hard requirement), and not-found down from `528` to roughly
`320`. The plan originally predicted exactly 340. That figure counted only instances whose
*source file* survives the Enterprise fold; the rewrite script has no such exclusion and also
repairs links inside `platform/enterprise/`, which Phase C deletes anyway. The real landing
point is ~321. Treat anything in the 315-325 band as correct and check `invalid-fragment`
instead — that is the number that must be exact.

If `invalid-fragment` is non-zero, a fragment was re-attached to a page that lacks that heading. The most likely offender is `/automation/deploy/projects#deploy-project` → `projects.mdx`, which may not have a `## Deploy project` heading. Fix by finding the correct heading on the destination page and updating the link by hand, or dropping the fragment if no equivalent exists. Do not suppress the check.

- [ ] **Step 6: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs
git commit -m "732 docs - Repair the durable internal links broken by the reorganization

Rewrites the 188 link instances whose source file and destination page both
survive the pending Enterprise fold, preserving #fragments. The remaining 340
are entangled with platform/enterprise and resolve as part of that work."
```

---

## Phase B — Navigation structure

### Task 3: Resolve section links to the first available page

**Files:**
- Modify: the pages containing the section links listed below

**Interfaces:**
- Consumes: `rewrite.mjs` from Task 2.
- Produces: no new URLs. Every section-targeting link resolves to a real page.

**Why:** three of Task 1's excluded targets, and several of the 54 dead targets, point at a
*directory* rather than a page. 24 platform directories have no index page, so those links cannot
resolve. Rather than authoring landing pages, each such link is pointed at the first page in that
directory's declared nav order.

- [ ] **Step 1: Derive the first available page for each linked-to directory**

Read the governing `meta.json` and take the first entry that is not a `--- separator ---`. If that
entry is a folder, recurse into its own `meta.json`; if a directory has no `meta.json`, Fumadocs
orders alphabetically and the first `.md`/`.mdx` file wins.

```bash
cd docs/content/docs/platform
for d in embedded automation automation/build/workflows automation/deploy settings use-bytechef; do
  printf '%-38s ' "$d"
  if [ -f "$d/meta.json" ]; then
    node -e "
      const m = require('./$d/meta.json');
      const first = (m.pages || []).find((p) => !/^-{2,}/.test(String(p).trim()));
      console.log('first page:', first);
    "
  else
    echo "(no meta.json - alphabetical)"
  fi
done
```

Expected, which the mapping in Step 2 encodes:

| Directory | First non-separator entry | Resolves to |
|---|---|---|
| `platform/embedded` | `get-started/index` | `/platform/embedded/get-started` |
| `platform/automation` | `get-started/index` | `/platform/automation/get-started` |
| `platform/automation/build/workflows` | `projects` | `/platform/automation/build/workflows/projects` |
| `platform/automation/deploy` | `deploy-workflows` | `/platform/automation/deploy/deploy-workflows` |
| `platform/automation/monitor` | `workflow-executions` | `/platform/automation/monitor/workflow-executions` |
| `platform/settings` | `workspaces` | `/platform/settings/workspaces` |
| `platform/use-bytechef` | `cloud` | `/platform/use-bytechef/cloud` |

Both product areas already have a `get-started/index.mdx`, so the two most-linked section targets
land on a real introduction rather than an arbitrary interior page.

- [ ] **Step 2: Apply the section mapping**

```bash
cd docs
cat > "$SCRATCH/section-map.json" <<'JSON'
{
  "/automation/build/workflows": "/platform/automation/build/workflows/projects",
  "/automation/build": "/platform/automation/build/workflows/projects",
  "/automation/deploy": "/platform/automation/deploy/deploy-workflows",
  "/automation/monitor": "/platform/automation/monitor/workflow-executions",
  "/automation/overview": "/platform/automation/get-started",
  "/automation/connect-data/overview": "/platform/automation/build/connections",
  "/embedded": "/platform/embedded/get-started",
  "/deploy": "/platform/use-bytechef/self-hosted"
}
JSON
node "$SCRATCH/rewrite.mjs" "$SCRATCH/section-map.json"
```

Two entries deviate from the mechanical rule, deliberately:

- **`/automation/build`** would resolve to `/platform/automation/build/with-ai/copilot`, because the
  parent nav lists `build/with-ai` first and that folder has no `meta.json` of its own. Landing a
  "building workflows" link on the Copilot page is wrong, so it points at `workflows/projects`.
- **`/deploy`** would resolve to `/platform/use-bytechef/cloud`, but every occurrence means
  self-hosting. `use-bytechef/self-hosted/index.mdx` already exists, so it points there.

Record both deviations in the commit body — a later reader will otherwise assume the rule was
applied uniformly.

- [ ] **Step 3: Confirm every destination is a real page**

```bash
cd docs
node -e '
const fs = require("fs"), path = require("path");
const map = JSON.parse(fs.readFileSync(process.env.SCRATCH + "/section-map.json", "utf8"));
const urls = new Set();
(function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full);
    else if (/\.mdx?$/.test(entry.name)) {
      let url = full.slice("content/docs".length).replace(/\.mdx?$/, "");
      url = url.split("/").filter((s) => !/^\(.*\)$/.test(s)).join("/");
      if (url.endsWith("/index")) url = url.slice(0, -6) || "/";
      urls.add(url || "/");
    }
  }
})("content/docs");
for (const [from, to] of Object.entries(map)) {
  console.log((urls.has(to) ? "ok       " : "MISSING  ") + from + " -> " + to);
}
'
```

Expected: every line starts `ok`. A `MISSING` line means the destination does not exist — fix the
map before running the validator.

- [ ] **Step 4: Run the validator**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
./node_modules/.bin/bun ./scripts/lint.ts > "$SCRATCH/linkcheck-after-b3.log" 2>&1
grep -cE '^[^ ].*: not-found' "$SCRATCH/linkcheck-after-b3.log"
grep -cE '^[^ ].*: invalid-fragment' "$SCRATCH/linkcheck-after-b3.log" || echo 0
```

Expected: not-found drops from `321` (the actual post-Task-2 figure) to roughly `300`;
`invalid-fragment` stays `0`. Record the figure — Task 4 uses it as its baseline.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs
git commit -m "732 docs - Point section links at the first page in each section

24 platform directories have no index page, so links targeting a section could
not resolve. Each now points at the first page in that section's declared nav
order. Two deviate on purpose: /automation/build would have landed on Copilot,
and /deploy would have landed on Cloud rather than Self-Hosted."
```

---

### Task 4: Add the four missing `meta.json` files

**Files:**
- Create: `content/docs/platform/automation/data/meta.json`
- Create: `content/docs/platform/automation/deploy/meta.json`
- Create: `content/docs/platform/embedded/build/meta.json`
- Create: `content/docs/platform/embedded/configure/meta.json`

**Interfaces:**
- Consumes: the section mapping from Task 3.
- Produces: declared page order in four directories; `embedded/configure/meta.json` is what stops `mcp-servers2` auto-appearing in the sidebar, which Task 5 then deletes.

- [ ] **Step 1: Confirm the four are still missing**

```bash
cd docs
for d in platform/automation/data platform/automation/deploy platform/embedded/build platform/embedded/configure; do
  [ -f "content/docs/$d/meta.json" ] && echo "EXISTS: $d" || echo "missing: $d"
done
```

Expected: all four `missing`.

- [ ] **Step 2: Write the four files**

`platform/automation/data/meta.json`:

```json
{
  "$schema": "../../../../.source/json-schema/docs.meta.json",
  "title": "Data",
  "description": "Data tables, the knowledge base, and asset files.",
  "pages": ["data-tables", "knowledge-base", "asset-files", "context-store"]
}
```

`platform/automation/deploy/meta.json`:

```json
{
  "$schema": "../../../../.source/json-schema/docs.meta.json",
  "title": "Deploy",
  "description": "Deploy workflows and expose them to callers and agents.",
  "pages": ["deploy-workflows", "api-platform", "mcp-servers", "a2a-servers", "ai-gateway"]
}
```

`platform/embedded/build/meta.json`:

```json
{
  "$schema": "../../../../.source/json-schema/docs.meta.json",
  "title": "Build",
  "description": "Integrations, workflows, connections, and the component kit.",
  "pages": [
    "integrations",
    "workflows",
    "automations",
    "connections",
    "app-events",
    "component-kit",
    "unified-api",
    "permission-expressions"
  ]
}
```

`platform/embedded/configure/meta.json`:

```json
{
  "$schema": "../../../../.source/json-schema/docs.meta.json",
  "title": "Configure",
  "description": "Per-customer configuration and MCP exposure.",
  "pages": ["instance-configurations", "mcp-servers"]
}
```

Note the deliberate omission: `embedded/configure/meta.json` lists only two pages. `mcp-servers2` is excluded so it stops rendering in the nav immediately; Task 5 deletes the file.

- [ ] **Step 3: Verify the `$schema` depth is right**

The relative path must resolve to `docs/.source/json-schema/docs.meta.json`. From `content/docs/platform/automation/data/` that is four levels up. Check against an existing sibling:

```bash
cd docs
grep '"\$schema"' content/docs/platform/automation/build/workflows/meta.json
```

Expected: `"../../../../../.source/json-schema/docs.meta.json"` — five levels, because that directory is one deeper. Confirm each new file's depth matches its own nesting; a wrong depth is an editor-only annoyance, not a build failure, but fix it now.

- [ ] **Step 4: Build to confirm the nav still assembles**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
npm run types:check
```

Expected: exit 0. A `meta.json` naming a page that does not exist is a type error here.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform
git commit -m "732 docs - Declare page order in the four directories missing meta.json

automation/data, automation/deploy, embedded/build and embedded/configure were
auto-listed alphabetically, which is why mcp-servers2 appeared in the sidebar."
```

---

### Task 5: Resolve the two stray duplicate pages

**Files:**
- Modify: `content/docs/platform/embedded/configure/mcp-servers.md`
- Delete: `content/docs/platform/embedded/configure/mcp-servers2.mdx`
- Modify: `content/docs/platform/automation/build/workflows/human-in-the-loop.mdx`
- Delete: `content/docs/platform/automation/build/workflows/human-in-the-loop2.mdx`
- Modify: `content/docs/platform/automation/build/workflows/meta.json:10-12`

**Interfaces:**
- Consumes: `embedded/configure/meta.json` from Task 4.
- Produces: two fewer pages; no new URLs.

- [ ] **Step 1: Read both pairs in full before merging**

```bash
cd docs
wc -w content/docs/platform/embedded/configure/mcp-servers.md \
      content/docs/platform/embedded/configure/mcp-servers2.mdx \
      content/docs/platform/automation/build/workflows/human-in-the-loop.mdx \
      content/docs/platform/automation/build/workflows/human-in-the-loop2.mdx
```

Expected: 569, 907, 1506, 1645 words respectively.

- [ ] **Step 2: Fold `mcp-servers2.mdx` into `mcp-servers.md`**

`mcp-servers.md` is a UI walkthrough (creating a server, adding components and workflows, filtering). `mcp-servers2.mdx` is positioning prose wrapped around four pieces of real reference content. Carry over **only** these four, rewritten as guidance, and drop everything else:

1. **Accepted credentials** — the endpoint `/api/embedded/<secret-key>/mcp` accepts a ByteChef-signed JWT (minted with the tenant signing key; `kid` identifies the tenant, `sub` carries the external user id) or JWTs from the tenant's configured external identity provider via OAuth2 federation.
2. **Environment selection** — the `X-Environment` request header.
3. **Optional authentication** — the per-server **Require authentication** toggle: on (default for new servers) requires a credential and resolves a ConnectedUser; off serves on the URL secret alone, with no ConnectedUser, so tools needing a connection return their setup URL instead of executing. Servers created before the setting existed default to off.
4. **Tenant scoping** — a session exposes only the tenant's own workflows, connections, and execution history.

Delete outright: the "What this enables" narrative, "The shape of an Embedded MCP session" ASCII diagram, "Why this is the right architecture for agentic products", and the two `{/* TODO screenshot */}` blocks with their non-existent `/enterprise/embedded-mcp/*.png` images.

Do **not** carry over the "Per-tenant tool exposure controls" (Off / On / Approval-gated) or "Per-tool permissions" sections without verifying them against the code first — they describe a three-state per-workflow setting that Phase E must confirm exists. If unverified at this point, omit them and let Task 16 add them back with evidence.

Rename the merged file to `.mdx` only if it gains a component; otherwise leave it `.md`.

- [ ] **Step 3: Delete `mcp-servers2.mdx`**

```bash
cd /Volumes/Data/bytechef/bytechef
git rm docs/content/docs/platform/embedded/configure/mcp-servers2.mdx
```

- [ ] **Step 4: Fold `human-in-the-loop2.mdx` into `human-in-the-loop.mdx`**

`human-in-the-loop.mdx` documents the shipped Approval component. `human-in-the-loop2.mdx` documents the unshipped advanced surface (external delivery channels, in-place messenger approvals, the pending-approvals inbox, expiry/reminder/escalation, MCP/A2A elicitation) behind a "Coming soon" callout.

Merge the unshipped material into `human-in-the-loop.mdx` as a clearly-delimited section at the end, retaining exactly one "Coming soon" callout for the whole section rather than one per feature. Phase F converts that callout to `comingSoon` frontmatter — but this page is only *partly* unshipped, so it keeps the inline callout and does **not** get `comingSoon: true`.

Cross-reference `.agents/hitl-approvals.md` while merging: it is the internal source of truth for the approval/AskUserQuestion split and the delivery fan-out, and the merged page must not contradict it.

- [ ] **Step 5: Delete `human-in-the-loop2.mdx` and drop its nav entry**

```bash
cd /Volumes/Data/bytechef/bytechef
git rm docs/content/docs/platform/automation/build/workflows/human-in-the-loop2.mdx
```

Then remove the `"human-in-the-loop2",` line from
`docs/content/docs/platform/automation/build/workflows/meta.json`.

- [ ] **Step 6: Validate**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
npm run types:check
./node_modules/.bin/bun ./scripts/lint.ts > "$SCRATCH/linkcheck-after-b5.log" 2>&1
grep -cE '^[^ ].*: not-found' "$SCRATCH/linkcheck-after-b5.log"
```

Expected: `types:check` exits 0 (it fails if `meta.json` still names the deleted page). not-found drops by roughly 10 — both deleted pages contained broken links of their own.

- [ ] **Step 7: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform
git commit -m "732 docs - Merge the mcp-servers2 and human-in-the-loop2 duplicates

Carries the reference content from each positioning-voiced duplicate into its
guide twin and deletes the duplicate."
```

---

## Phase C — The Enterprise fold

Every task in this phase follows the same shape, so it is stated once here rather than repeated per task.

**Per-page procedure:**

1. Read the source page in `content/docs/platform/enterprise/` in full.
2. Read the destination page in full.
3. Rewrite the source's substance into the destination in **guidance voice**: drop superlatives, competitive framing, "why this matters" framing, and any second-person sales address. Keep procedures, constraints, property names, defaults, and limits.
4. Mark EE-only sections with `<EEBadge />` immediately under the heading. If the *entire* destination page is EE-only, it will additionally get `ee: true` frontmatter in Phase F — do not add that field yet, the schema does not accept it until Task 19.
5. If the source carried a "Coming soon" marker, preserve it as a single `<Callout type="warn" title="Coming soon">` for now; Phase F converts it.
6. `git rm` the source page.
7. Record the source URL → destination URL pair in `$SCRATCH/enterprise-redirects.json` for Task 10.

**Do not** run the link validator expecting zero mid-phase; deleting enterprise pages breaks links from other enterprise pages that have not been deleted yet. Task 10 is where it converges.

### Task 6: Fold governance-security and extensibility into the settings stubs

**Files:**
- Modify (all currently frontmatter-only): `content/docs/platform/settings/users.mdx`, `oauth2-clients.mdx`, `identity-providers.mdx`, `audit-events.mdx`, `license.mdx`, `connections.mdx`, `admin-api-keys.mdx`, `components/custom-components.mdx`, `components/component-visibility.mdx`, `components/api-connectors.mdx`
- Modify: `content/docs/platform/automation/settings/api-keys.mdx`
- Delete: 11 pages under `content/docs/platform/enterprise/governance-security/` and `extensibility/`

**Interfaces:**
- Consumes: the per-page procedure above.
- Produces: 10 filled guide pages from 11 source pages (`rbac.mdx` shares `settings/users.mdx` with `users.mdx`); 11 entries in `enterprise-redirects.json`.

| Source (`platform/enterprise/`) | Words | Destination |
|---|---:|---|
| `governance-security/users.mdx` | 802 | `settings/users.mdx` |
| `governance-security/rbac.mdx` | 550 | `settings/users.mdx` — roles section of the same page |
| `governance-security/oauth2-clients.mdx` | 652 | `settings/oauth2-clients.mdx` |
| `governance-security/sso.mdx` | 922 | `settings/identity-providers.mdx` |
| `governance-security/audit-log.mdx` | 921 | `settings/audit-events.mdx` |
| `governance-security/license-gated-distribution.mdx` | 730 | `settings/license.mdx` |
| `governance-security/connection-visibility.mdx` | 834 | `settings/connections.mdx` |
| `governance-security/component-policies.mdx` | 615 | `settings/components/component-visibility.mdx` |
| `governance-security/api-keys.mdx` | 1069 | `settings/admin-api-keys.mdx` + `automation/settings/api-keys.mdx` |
| `extensibility/custom-components.mdx` | 3226 | `settings/components/custom-components.mdx` |
| `extensibility/api-connectors.mdx` | 1565 | `settings/components/api-connectors.mdx` |

- [ ] **Step 1: Fix the `settings/connections.mdx` title bug**

The file currently reads `title: Users` — a copy-paste error. Set it to:

```yaml
---
title: Connections
description: Organization-level view of shared connections and who can reach them.
---
```

- [ ] **Step 2: Fold each row in the table above**

Apply the per-page procedure to each of the 11 source pages. Two rows need specific care:

- **`rbac.mdx` → `settings/users.mdx`** shares a destination with `users.mdx`. Fold `users.mdx` first (members, invitations, seat behavior), then append roles and permission scopes from `rbac.mdx` as a `## Roles and permissions` section. Verify against `CLAUDE.md`'s note that RBAC collapsed to workspace-only scoping — project roles were removed in 2026-07, so any project-role prose in the source is stale and must not be carried over.
- **`api-keys.mdx` → two destinations.** `settings/admin-api-keys.mdx` covers organization-level admin keys; `automation/settings/api-keys.mdx` covers keys bound to a workspace and environment for the automation public API. Split by that boundary rather than duplicating the page.

- [ ] **Step 3: Verify each destination is no longer frontmatter-only**

```bash
cd docs
for f in settings/users settings/oauth2-clients settings/identity-providers settings/audit-events \
         settings/license settings/connections settings/admin-api-keys \
         settings/components/custom-components settings/components/component-visibility \
         settings/components/api-connectors; do
  w=$(wc -w < "content/docs/platform/$f.mdx" | tr -d ' ')
  [ "$w" -gt 100 ] && echo "ok   $f ($w words)" || echo "STUB $f ($w words)"
done
```

Expected: all ten report `ok`.

- [ ] **Step 4: Delete the sources and record redirects**

```bash
cd /Volumes/Data/bytechef/bytechef/docs/content/docs/platform/enterprise
git rm governance-security/users.mdx governance-security/rbac.mdx \
       governance-security/oauth2-clients.mdx governance-security/sso.mdx \
       governance-security/audit-log.mdx governance-security/license-gated-distribution.mdx \
       governance-security/connection-visibility.mdx governance-security/component-policies.mdx \
       governance-security/api-keys.mdx \
       extensibility/custom-components.mdx extensibility/api-connectors.mdx
```

Append to `$SCRATCH/enterprise-redirects.json`:

```json
{
  "/platform/enterprise/governance-security/users": "/platform/settings/users",
  "/platform/enterprise/governance-security/rbac": "/platform/settings/users",
  "/platform/enterprise/governance-security/oauth2-clients": "/platform/settings/oauth2-clients",
  "/platform/enterprise/governance-security/sso": "/platform/settings/identity-providers",
  "/platform/enterprise/governance-security/audit-log": "/platform/settings/audit-events",
  "/platform/enterprise/governance-security/license-gated-distribution": "/platform/settings/license",
  "/platform/enterprise/governance-security/connection-visibility": "/platform/settings/connections",
  "/platform/enterprise/governance-security/component-policies": "/platform/settings/components/component-visibility",
  "/platform/enterprise/governance-security/api-keys": "/platform/settings/admin-api-keys",
  "/platform/enterprise/extensibility/custom-components": "/platform/settings/components/custom-components",
  "/platform/enterprise/extensibility/api-connectors": "/platform/settings/components/api-connectors"
}
```

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform
git commit -m "732 docs - Fill the settings guides from the enterprise governance pages

The platform/settings pages were frontmatter-only stubs; the enterprise tree
held their bodies. Rewritten from positioning voice into guidance."
```

---

### Task 7: Fold the automation-facing enterprise pages

**Files:**
- Modify: `content/docs/platform/automation/data/data-tables.mdx`, `data/knowledge-base/index.mdx`, `monitor/workflow-executions.mdx`, `settings/git-configuration.mdx`, `settings/ai-agents/guardrails.mdx`, `build/workflows/projects.mdx`, `deploy/deploy-workflows.mdx`, `deploy/mcp-servers.mdx`
- Modify: `content/docs/platform/settings/ai-providers.mdx`, `settings/workspaces.mdx`, `settings/mcp-server.md`
- Create: `content/docs/platform/automation/deploy/environments.mdx`
- Delete: 11 pages under `content/docs/platform/enterprise/`

**Interfaces:**
- Consumes: the per-page procedure.
- Produces: 11 more entries in `enterprise-redirects.json`; a new `/platform/automation/deploy/environments` URL that Task 4's `automation/deploy/meta.json` must then list.

| Source | Words | Destination |
|---|---:|---|
| `data-knowledge/data-tables.mdx` | 726 | `automation/data/data-tables.mdx` |
| `data-knowledge/knowledge-base.mdx` | 753 | `automation/data/knowledge-base/index.mdx` |
| `data-knowledge/embedding-models.mdx` | 557 | `settings/ai-providers.mdx`, alongside its `## Default models` section |
| `governance-security/ai-guardrails.mdx` | 654 | `automation/settings/ai-agents/guardrails.mdx` |
| `collaboration-devops/workflow-executions.mdx` | 690 | `automation/monitor/workflow-executions.mdx` |
| `collaboration-devops/workspaces-projects.mdx` | 533 | `settings/workspaces.mdx` + `automation/build/workflows/projects.mdx` |
| `collaboration-devops/git-backed-change-tracking.mdx` | 1386 | `automation/settings/git-configuration.mdx` |
| `collaboration-devops/workflow-versioning.mdx` | 535 | `automation/deploy/deploy-workflows.mdx` |
| `collaboration-devops/build-once-deploy-many.mdx` | 569 | `automation/deploy/deploy-workflows.mdx` |
| `collaboration-devops/environments.mdx` | 642 | `automation/deploy/environments.mdx` (new) |
| `extensibility/mcp-integration.mdx` | 523 | `automation/deploy/mcp-servers.mdx` + `settings/mcp-server.md` |

- [ ] **Step 1: Fold each row**

Apply the per-page procedure. Row-specific notes:

- **`embedding-models.mdx` → `settings/ai-providers.mdx`.** That page is EE-gated and carries a CE `<Callout>` explaining that CE configures providers through `BYTECHEF_AI_PROVIDER_*` environment variables. The folded embedding content must preserve that edition split — CE users set embedding models by property, not in the UI. Do not drop the caveat.
- **`ai-guardrails.mdx` → `automation/settings/ai-agents/guardrails.mdx`.** Cross-check `.agents/ai-guardrails.md`. Per `CLAUDE.md`, guardrails were extracted into a standalone module and are no longer gateway-only, and the settings UI lives at `/automation/settings/ai/agents/guardrails`. Any "applies to AI Gateway traffic only" prose in the source is stale.
- **`workspaces-projects.mdx` → two destinations.** Workspace administration goes to `settings/workspaces.mdx`; the project-as-deployable-unit material goes to `build/workflows/projects.mdx`.
- **`mcp-integration.mdx` → two destinations.** Automation-side MCP servers go to `automation/deploy/mcp-servers.mdx`; the ByteChef-management MCP server goes to `settings/mcp-server.md`.

- [ ] **Step 2: Create `automation/deploy/environments.mdx`**

Rewrite `collaboration-devops/environments.mdx` into it. Frontmatter:

```yaml
---
title: Environments
description: Development, Staging, and Production — how workflows, connections, and configuration are scoped per environment.
---
```

- [ ] **Step 3: Add the new page to the deploy nav**

Edit `content/docs/platform/automation/deploy/meta.json` (created in Task 4) so `pages` reads:

```json
"pages": ["deploy-workflows", "environments", "api-platform", "mcp-servers", "a2a-servers", "ai-gateway"]
```

- [ ] **Step 4: Delete the sources and record redirects**

```bash
cd /Volumes/Data/bytechef/bytechef/docs/content/docs/platform/enterprise
git rm data-knowledge/data-tables.mdx data-knowledge/knowledge-base.mdx \
       data-knowledge/embedding-models.mdx governance-security/ai-guardrails.mdx \
       collaboration-devops/workflow-executions.mdx collaboration-devops/workspaces-projects.mdx \
       collaboration-devops/git-backed-change-tracking.mdx collaboration-devops/workflow-versioning.mdx \
       collaboration-devops/build-once-deploy-many.mdx collaboration-devops/environments.mdx \
       extensibility/mcp-integration.mdx
```

Append the corresponding 11 pairs to `$SCRATCH/enterprise-redirects.json`, each mapping
`/platform/enterprise/<source-without-extension>` to the destination URL from the table.

- [ ] **Step 5: Verify the build**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
npm run types:check
```

Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform
git commit -m "732 docs - Fold the automation-facing enterprise pages into the guides

Data tables, knowledge base, embedding models, guardrails, executions,
workspaces, git configuration, versioning, environments and MCP."
```

---

### Task 8: Fold the infrastructure pages into self-hosted

**Files:**
- Modify: `content/docs/platform/use-bytechef/self-hosted/index.mdx`, `architecture.mdx`, `installation/distributed.mdx`, `management/observability.mdx`, `management/upgrades.mdx`
- Create: `content/docs/platform/use-bytechef/self-hosted/configuration/message-brokers.md`, `configuration/file-storage.md`, `configuration/plan-limits.md`, `management/crash-recovery.mdx`, `runtime-job.mdx`
- Modify: `content/docs/platform/use-bytechef/self-hosted/configuration/index.md` (nav/index links to the three new configuration pages)
- Delete: 14 pages under `content/docs/platform/enterprise/`

**Interfaces:**
- Consumes: the per-page procedure.
- Produces: 5 new URLs under `/platform/use-bytechef/self-hosted/`; 14 more redirect entries.

| Source | Words | Destination |
|---|---:|---|
| `scale-reliability/horizontal-scaling.mdx` | 669 | `self-hosted/installation/distributed.mdx` |
| `scale-reliability/distributed-scheduler.mdx` | 548 | `self-hosted/installation/distributed.mdx` |
| `scale-reliability/message-brokers.mdx` | 528 | `self-hosted/configuration/message-brokers.md` (new) |
| `scale-reliability/cloud-native-storage.mdx` | 568 | `self-hosted/configuration/file-storage.md` (new) |
| `scale-reliability/multi-tenant-isolation.mdx` | 627 | `self-hosted/architecture.mdx` |
| `scale-reliability/crash-recovery.mdx` | 373 | `self-hosted/management/crash-recovery.mdx` (new) |
| `scale-reliability/plan-limits.mdx` | 418 | `self-hosted/configuration/plan-limits.md` (new) |
| `scale-reliability/runtime-job.mdx` | 1371 | `self-hosted/runtime-job.mdx` (new) |
| `runtime-job-runner/index.mdx` | 778 | `self-hosted/runtime-job.mdx` — same page, these two duplicate each other |
| `governance-security/flexible-deployment.mdx` | 1033 | `self-hosted/index.mdx` + `architecture.mdx` |
| `governance-security/encrypted-credentials.mdx` | 934 | `self-hosted/configuration/` — encryption key section |
| `governance-security/data-retention.mdx` | 451 | `self-hosted/configuration/` — retention section |
| `governance-security/observability.mdx` | 687 | `self-hosted/management/observability.mdx` |
| `support-trust/production-migrations.mdx` | 667 | `self-hosted/management/upgrades.mdx` |

- [ ] **Step 1: Merge the two runtime-job pages first**

`scale-reliability/runtime-job.mdx` (1371w) and `runtime-job-runner/index.mdx` (778w) both document the
runtime job runner, but they are **complementary, not duplicates** — they share **zero headings**:

| `scale-reliability/runtime-job.mdx` | `runtime-job-runner/index.mdx` |
|---|---|
| No database, no broker, no state | What it is |
| Exit codes | Command-line interface |
| Arguments | Usage — Plain JAR / Gradle / Docker |
| Workflow sources | Connection mapping |
| Connections | When to reach for the runtime job runner |
| Configuration via environment variables | What's missing vs. the full platform |
| Building the image | Error handling |
| Kubernetes example / Docker example | Debug logging |
| What's inside — and what isn't | |
| When to use which | |

Neither page is redundant. Produce one `self-hosted/runtime-job.mdx` carrying **both** sets of material,
merging only the genuinely overlapping topics (connections/connection mapping, Docker, when-to-use,
what's-missing) and reconciling conflicts against the code rather than picking a side. Discarding either
would lose real content — the exit codes and argument reference exist only in the first, the CLI and
error-handling reference only in the second.

The runtime-job image is **not published**: no publish or docker-push step for it exists anywhere under
`.github/workflows/`. Do not state or imply a prebuilt image is available; the page must describe building
it yourself.

- [ ] **Step 2: Fold the remaining rows**

Apply the per-page procedure. Row-specific notes:

- **`crash-recovery.mdx`** must match the properties in `CLAUDE.md`: `bytechef.workflow.execution.recovery.enabled`, `.staleness-threshold` (default `PT5M`), `.auto-resume` (default `false`), `.max-auto-resume-attempts` (default 3). Also note that orphan detection is monolith-only today.
- **`plan-limits.mdx`** must state that null means unlimited, never zero, and that `SELF_HOSTED` is the default tier with all limits null.
- **`observability.mdx`** merges into a destination that is already 1450 words — read it fully first and merge, do not append a second overlapping treatment.

- [ ] **Step 3: Delete the sources and record redirects**

```bash
cd /Volumes/Data/bytechef/bytechef/docs/content/docs/platform/enterprise
git rm scale-reliability/horizontal-scaling.mdx scale-reliability/distributed-scheduler.mdx \
       scale-reliability/message-brokers.mdx scale-reliability/cloud-native-storage.mdx \
       scale-reliability/multi-tenant-isolation.mdx scale-reliability/crash-recovery.mdx \
       scale-reliability/plan-limits.mdx scale-reliability/runtime-job.mdx \
       runtime-job-runner/index.mdx governance-security/flexible-deployment.mdx \
       governance-security/encrypted-credentials.mdx governance-security/data-retention.mdx \
       governance-security/observability.mdx support-trust/production-migrations.mdx
```

Append the corresponding pairs to `$SCRATCH/enterprise-redirects.json`.

- [ ] **Step 4: Verify**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
npm run types:check
```

Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform
git commit -m "732 docs - Fold the enterprise infrastructure pages into self-hosted

Scaling, brokers, storage, isolation, crash recovery, plan limits, the runtime
job runner (deduplicated), deployment topology, credential encryption, data
retention, observability and migrations."
```

---

### Task 9: Create the three homeless guide pages and fold embedded-ipaas

**Files:**
- Create: `content/docs/platform/automation/build/workflows/code-workflows.mdx`
- Create: `content/docs/platform/automation/build/workflows/components.mdx`
- Modify: `content/docs/platform/automation/build/workflows/meta.json`
- Modify: `content/docs/platform/embedded/get-started/index.mdx`
- Delete: 4 pages under `content/docs/platform/enterprise/`

**Interfaces:**
- Consumes: the per-page procedure.
- Produces: 2 new URLs under `/platform/automation/build/workflows/`; 4 more redirect entries.

**Why these are different:** `code-workflows.mdx` (2516w), `polyglot-scripting.mdx` (540w) and `built-in-components.mdx` (538w) have **no counterpart in the product guides**. They are not restatements — deleting them without a destination would lose 3,594 words of unique documentation.

| Source | Words | Destination |
|---|---:|---|
| `extensibility/code-workflows.mdx` | 2516 | `automation/build/workflows/code-workflows.mdx` (new) |
| `extensibility/polyglot-scripting.mdx` | 540 | same page — a `## Scripting languages` section |
| `extensibility/built-in-components.mdx` | 538 | `automation/build/workflows/components.mdx` (new), fronting `/reference/components` |
| `embedded-ipaas/index.mdx` | 162 | `embedded/get-started/index.mdx` |

- [ ] **Step 1: Create `code-workflows.mdx`**

Frontmatter:

```yaml
---
title: Code Workflows
description: Define workflows as code — the SDK contract, declared connections, the perform context, and the draft/publish model.
---
```

**This page is marked coming-soon in full** — decided by the user on 2026-08-18. At `v0.31.4` the
storage layer ships (`CodeWorkflowContainer`, `CodeWorkflowContainerFacade`) but there is **no
user-facing surface whatsoever**: `CodeWorkflowTaskContext`, `CodeWorkflowHostBridge` and
`PolyglotSandbox` are all absent, and `client/src` contains **zero** code-workflow files against 25 on
this branch. Carry one `<Callout type="warn" title="Coming soon">` at the top now; Phase F converts it
to `comingSoon: true` frontmatter. The content still lands in full — all 2,516 words plus the scripting
material — it is simply labelled unreleased.

Fold `code-workflows.mdx` and `polyglot-scripting.mdx` into one page. Verify against `CLAUDE.md` and the two specs it names (`2026-08-05-code-perform-context-design.md`, `2026-08-06-code-artifact-connections-design.md`) that the page states: `perform(context)` with `context.component.<name>.<action>(input, connectionName)` and `context.log`; connections declared via `WorkflowDsl.task(...).connections(...)` or a `connections` list/map in the script contract; and that editor saves reconcile a draft in place while publish happens only through the project header.

- [ ] **Step 2: Create `components.mdx`**

Frontmatter:

```yaml
---
title: Components
description: The built-in component library — what a component is, how actions and triggers work, and where to find the full reference.
---
```

This page is a short orientation that hands off to the generated reference; it must not restate individual component documentation. Do not assert a component count without checking — `CLAUDE.md` says 190+, the enterprise source says 200+, and the project memory records that the count was "docs-normalized to 200+". Resolve by counting the directories under `server/libs/modules/components/` and state the rounded-down figure.

- [ ] **Step 3: Register both pages in the workflows nav**

Edit `content/docs/platform/automation/build/workflows/meta.json` so `pages` includes `"components"` after `"workflows"` and `"code-workflows"` after `"build-approaches"`. The `"human-in-the-loop2"` entry was already removed in Task 5.

- [ ] **Step 4: Fold `embedded-ipaas/index.mdx` into the embedded introduction**

162 words, mostly a pointer. Carry over only what the embedded introduction does not already say, then delete.

- [ ] **Step 5: Delete the sources and record redirects**

```bash
cd /Volumes/Data/bytechef/bytechef/docs/content/docs/platform/enterprise
git rm extensibility/code-workflows.mdx extensibility/polyglot-scripting.mdx \
       extensibility/built-in-components.mdx embedded-ipaas/index.mdx
```

Append to `$SCRATCH/enterprise-redirects.json`:

```json
{
  "/platform/enterprise/extensibility/code-workflows": "/platform/automation/build/workflows/code-workflows",
  "/platform/enterprise/extensibility/polyglot-scripting": "/platform/automation/build/workflows/code-workflows",
  "/platform/enterprise/extensibility/built-in-components": "/platform/automation/build/workflows/components",
  "/platform/enterprise/embedded-ipaas": "/platform/embedded/get-started"
}
```

- [ ] **Step 6: Verify and commit**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
npm run types:check
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform
git commit -m "732 docs - Give code workflows and the component library real guide pages

These three enterprise pages had no counterpart in the guides; folding without
a destination would have lost 3,594 words of unique documentation."
```

---

### Task 10: Replace the Enterprise tree with a single index and wire the redirects

**Files:**
- Rewrite: `content/docs/platform/enterprise/index.mdx`
- Delete: `content/docs/platform/enterprise/meta.json` section entries; the six section `index.mdx` files; the two sales-copy pages
- Modify: `next.config.ts` — `redirects()`
- Modify: `content/docs/platform/meta.json` (unchanged list, but confirm `enterprise` still resolves)

**Interfaces:**
- Consumes: `enterprise-redirects.json` accumulated across Tasks 6–9.
- Produces: `/platform/enterprise` as the sole surviving enterprise URL; permanent redirects for the other 48.

- [ ] **Step 1: Delete the six section indexes and the two sales-copy pages**

```bash
cd /Volumes/Data/bytechef/bytechef/docs/content/docs/platform/enterprise
git rm collaboration-devops/index.mdx data-knowledge/index.mdx extensibility/index.mdx \
       governance-security/index.mdx scale-reliability/index.mdx support-trust/index.mdx \
       support-trust/support-slas.mdx support-trust/source-available-code.mdx
git rm collaboration-devops/meta.json data-knowledge/meta.json extensibility/meta.json \
       governance-security/meta.json scale-reliability/meta.json support-trust/meta.json \
       embedded-ipaas/meta.json
```

**Seven** section `meta.json` files go, not six: `embedded-ipaas/` no longer has an `index.mdx` at
all — Task 9 folded it — so its `meta.json` is already sitting alone in an otherwise empty
directory, which is the end state the other six reach only after Step 1's first command.
By this point every one of them lists pages that
Tasks 6-8 already deleted (`governance-security/meta.json` still names `ai-guardrails`;
`collaboration-devops/meta.json` names all six of its removed pages; `data-knowledge/meta.json`
names all three of its own). Fumadocs drops unresolvable `pages` entries silently rather than
failing, which is exactly why these survived unnoticed — deleting the directories' only remaining
file is what actually removes them.

- [ ] **Step 2: Confirm only `index.mdx` remains**

```bash
cd docs
find content/docs/platform/enterprise -type f | sort
```

Expected: exactly two lines — `content/docs/platform/enterprise/index.mdx` and
`content/docs/platform/enterprise/meta.json`. Note this greps **every file**, not just `*.md*`:
a stale `meta.json` is invisible to the narrower pattern and is the one thing most likely to be
left behind. If a page remains, it was missed by Tasks 6-9 — fold it before continuing rather
than deleting it.

- [ ] **Step 3: Rewrite `enterprise/index.mdx` as a pure capability index**

Replace the file entirely. Every entry links to the guide page that now documents the capability; no prose is duplicated.

```mdx
---
title: Enterprise
description: The capabilities available in ByteChef Enterprise Edition, and where each one is documented.
---

# Enterprise

ByteChef Enterprise Edition adds governance, scale, and extensibility on top of
the Community Edition. Every capability below is documented in the guide for the
area it belongs to — this page is the index.

Pages marked <EEBadge /> require an Enterprise license.

## Governance and security

<Cards>
  <Card title="Users and roles" href="/platform/settings/users" description="Members, invitations, workspace roles, and permission scopes." />
  <Card title="Single sign-on" href="/platform/settings/identity-providers" description="Authenticate through your own identity provider." />
  <Card title="Audit events" href="/platform/settings/audit-events" description="A record of who changed what." />
  <Card title="Connection visibility" href="/platform/settings/connections" description="Private, workspace-shared, or granted to named people." />
  <Card title="Component visibility" href="/platform/settings/components/component-visibility" description="Restrict which components workflows may use." />
  <Card title="OAuth2 clients" href="/platform/settings/oauth2-clients" description="Clients registered against the authorization server." />
  <Card title="AI guardrails" href="/platform/automation/settings/ai-agents/guardrails" description="Redaction, blocked terms, moderation, and injection detection." />
  <Card title="License" href="/platform/settings/license" description="Activate and inspect your Enterprise license." />
</Cards>

## Scale and reliability

<Cards>
  <Card title="Distributed deployment" href="/platform/use-bytechef/self-hosted/installation/distributed" description="Coordinator and worker topology, horizontal scaling, and the distributed scheduler." />
  <Card title="Architecture" href="/platform/use-bytechef/self-hosted/architecture" description="How the services fit together, including multi-tenant isolation." />
  <Card title="Message brokers" href="/platform/use-bytechef/self-hosted/configuration/message-brokers" description="Redis, RabbitMQ, Kafka, JMS, AMQP, and AWS SQS." />
  <Card title="File storage" href="/platform/use-bytechef/self-hosted/configuration/file-storage" description="Filesystem and cloud-native object storage." />
  <Card title="Crash recovery" href="/platform/use-bytechef/self-hosted/management/crash-recovery" description="Orphaned-job detection, timeouts, and auto-resume." />
  <Card title="Plan limits" href="/platform/use-bytechef/self-hosted/configuration/plan-limits" description="Quotas, rate limits, and cost controls." />
  <Card title="Runtime job runner" href="/platform/use-bytechef/self-hosted/runtime-job" description="Ephemeral, database-free workflow execution." />
  <Card title="Observability" href="/platform/use-bytechef/self-hosted/management/observability" description="Metrics, traces, and log streaming." />
</Cards>

## Data and knowledge

<Cards>
  <Card title="Data tables" href="/platform/automation/data/data-tables" description="Structured storage your workflows can read and write." />
  <Card title="Knowledge base" href="/platform/automation/data/knowledge-base" description="Postgres-backed retrieval for agents and workflows." />
  <Card title="AI providers" href="/platform/settings/ai-providers" description="Activate chat and embedding models once, per environment." />
</Cards>

## Extensibility

<Cards>
  <Card title="Custom components" href="/platform/settings/components/custom-components" description="Author and publish your own components." />
  <Card title="API connectors" href="/platform/settings/components/api-connectors" description="Turn an OpenAPI specification into a component." />
  <Card title="Code workflows" href="/platform/automation/build/workflows/code-workflows" description="Define workflows as code, in Java, JavaScript, Python, or Ruby." />
  <Card title="MCP servers" href="/platform/automation/deploy/mcp-servers" description="Expose workflows as tools for AI agents." />
  <Card title="Management MCP server" href="/platform/settings/mcp-server" description="Let an agent administer ByteChef itself." />
</Cards>

## Collaboration and DevOps

<Cards>
  <Card title="Workspaces" href="/platform/settings/workspaces" description="Group projects and control who reaches them." />
  <Card title="Environments" href="/platform/automation/deploy/environments" description="Development, Staging, and Production." />
  <Card title="Deploy workflows" href="/platform/automation/deploy/deploy-workflows" description="Versioning, publishing, and deploying to an environment." />
  <Card title="Git configuration" href="/platform/automation/settings/git-configuration" description="Back workflow changes with a Git repository." />
  <Card title="Workflow executions" href="/platform/automation/monitor/workflow-executions" description="Run history and execution detail." />
</Cards>

## Embedded iPaaS

<Cards>
  <Card title="Embedded" href="/platform/embedded/get-started" description="Ship a white-labeled workflow builder inside your product." />
  <Card title="Tenant-isolated security" href="/platform/embedded/get-started/tenant-isolated-security" description="How one customer's data stays separate from another's." />
  <Card title="Embedded MCP" href="/platform/embedded/configure/mcp-servers" description="Expose your customers' workflows to their agents." />
</Cards>
```

- [ ] **Step 4: Replace `enterprise/meta.json`**

The current file lists seven deleted sections. Replace it with:

```json
{
  "$schema": "../../../.source/json-schema/docs.meta.json",
  "title": "Enterprise",
  "description": "Enterprise Edition capabilities and where each is documented",
  "icon": "Building2",
  "pages": ["index"]
}
```

- [ ] **Step 5: Add the 48 permanent redirects**

Generate the entries from the accumulated map, then paste them into `next.config.ts`'s `redirects()` array:

```bash
node -e "
const m=require('$SCRATCH/enterprise-redirects.json');
console.log(Object.entries(m).map(([s,d])=>
  '      { source: \'' + s + '\', destination: \'' + d + '\', permanent: true },'
).join('\n'));
"
```

Insert the generated lines into the existing `redirects()` return array in `next.config.ts`, after the three entries already there. Keep `permanent: true` — these URLs are gone for good, unlike the three existing `permanent: false` entries.

- [ ] **Step 6: Verify every deleted page has a redirect**

```bash
cd /Volumes/Data/bytechef/bytechef
node -e "
const m=require('$SCRATCH/enterprise-redirects.json');
console.log('redirect entries:', Object.keys(m).length);
" 
git log --diff-filter=D --name-only --format= -- docs/content/docs/platform/enterprise \
  | grep -E '\.mdx?$' | sort -u | wc -l
```

Expected: both report `48`. A mismatch means a page was deleted without a redirect — add it before committing.

- [ ] **Step 7: Verify the build and the link count**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
npm run types:check
npm run build
```

Expected: all exit 0. `npm run build` is required here — it is the only check that exercises `redirects()`.

- [ ] **Step 8: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform docs/next.config.ts
git commit -m "732 docs - Replace the enterprise tree with a single capability index

48 pages folded into the product guides across the preceding commits; this
collapses what remains to one index page and adds permanent redirects for
every deleted URL."
```

---

## Phase D — Final link sweep

### Task 11: Drive the validator to zero

**Files:**
- Modify: whichever pages the validator still reports

**Interfaces:**
- Consumes: the tree after Phase C.
- Produces: a clean validator run.

- [ ] **Step 1: Re-run the validator and classify what remains**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
./node_modules/.bin/bun ./scripts/lint.ts > "$SCRATCH/linkcheck-after-c.log" 2>&1
grep -cE '^[^ ].*: not-found' "$SCRATCH/linkcheck-after-c.log"
grep -cE '^[^ ].*: invalid-fragment' "$SCRATCH/linkcheck-after-c.log" || echo 0
grep -oE '^[^ ]+: not-found' "$SCRATCH/linkcheck-after-c.log" | sed 's/: not-found//' | sort | uniq -c | sort -rn
```

The remaining set should be dominated by the 25 dead targets from the spec's §3.1 plus any `/enterprise/*` links inside pages that survived the fold.

- [ ] **Step 2: Resolve the known dead targets**

These have no page and never will; repoint each to the nearest real page:

| Dead target | Repoint to |
|---|---|
| `/automation/ai-hub` | `/platform/automation/build/with-ai/hub` |
| `/embedded/embedded-mcp` | `/platform/embedded/configure/mcp-servers` |
| `/embedded/configurations` | `/platform/embedded/configure/instance-configurations` |
| `/embedded/quickstart` | `/platform/embedded/get-started/quick-start` |
| `/platform/embedded/quickstart` | `/platform/embedded/get-started/quick-start` |
| `/embedded/executions` | `/platform/embedded/monitor/workflow-executions` |
| `/embedded/settings` | `/platform/embedded/administration/signing-keys` |
| `/quickstart` | `/platform/index` |
| `/introduction` | `/platform` |
| `/automation/overview` | `/platform/automation` |
| `/automation/connect-data/overview` | `/platform/automation/build/connections` |
| `/deploy` | `/platform/use-bytechef/self-hosted` |
| `/reference/components/redis_v1` | `/reference/components/redisVectorStore_v1` |

The last one is a component rename, not a reorg casualty — commit `ac292a3d5a5` renamed the Redis vector store component.

- [ ] **Step 3: Delete links to pages that were removed on purpose**

`/embedded/workflow-builder-tools`, `/embedded/configuration-api`, `/embedded/settings`,
`/deploy/self-hosted/configuration/manage-instance`, and
`/deploy/self-hosted/configuration/configure-instance` were deleted in commit `f85587b669e`.
Where the link is inside a sentence, rewrite the sentence so it no longer promises a page.
Where it is a bare "see also" list item, remove the item. Do not point them at an unrelated page.

- [ ] **Step 4: Fix any `invalid-fragment` results**

A non-zero count means a fragment was carried onto a page that lacks that heading. For each, open the destination, find the equivalent heading, and update the fragment — or drop the fragment if the destination genuinely has no equivalent section.

- [ ] **Step 5: Confirm zero**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
./node_modules/.bin/bun ./scripts/lint.ts
```

Expected: the command prints the collected-URL line and **no** `Invalid URLs in ...` blocks, and exits 0.

- [ ] **Step 6: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs
git commit -m "732 docs - Drive the internal link validator to zero

Repoints the targets that no longer exist and removes references to pages
deleted on purpose."
```

---

## Phase E — Accuracy pass

Seven batches, one task each, in descending order of drift risk. Every batch follows the same shape.

**Per-batch procedure:**

1. List the batch's pages.
2. For each page, extract its factual claims: property names, defaults, endpoint paths, class and table names, enum values, UI labels, feature availability.
3. Verify each against the code on branch `0_732`. `CLAUDE.md` is a strong index of where things live but is **not** the source of truth — check the code.
4. Fix clear-cut errors directly.
5. Where a page describes behavior that exists in code but has not shipped in a release tag, mark it "Coming soon" rather than deleting it.
6. Record every altered claim in the commit body.
7. Run the validator — accuracy edits often touch links.

**Never** change `content/docs/reference/**` or `content/docs/openapi/**`.

### Task 12: Batch 1 — AI Hub, agents, guardrails, evals

**Files:** `content/docs/platform/automation/build/with-ai/**` (7 pages), `content/docs/platform/automation/build/workflows/ai/**` (19 pages), `content/docs/platform/automation/ai/**` (2 pages)

**Interfaces:** Consumes the per-batch procedure. Produces a corrected batch plus a changelog in the commit body.

- [ ] **Step 1: Verify the AI Hub chat model**

Check `AiHubChatKind` in the EE `com.bytechef.ee.ai.hub.chat` package. The docs must describe three kinds — `STANDARD`, `WORKFLOW_CHAT`, `AGENT_CHAT` — and must not mention a `TASK` kind, which was removed. Confirm the transcript lives in the Spring AI session store keyed by `ai_hub_chat.thread_id`, not in `SPRING_AI_CHAT_MEMORY`.

```bash
cd /Volumes/Data/bytechef/bytechef
grep -rn "enum AiHubChatKind" --include='*.java' server/ee | head
grep -rn "isWebhookBridged" --include='*.java' server/ee | head
```

- [ ] **Step 2: Verify agent scheduling**

Per `CLAUDE.md` there is no AI Hub task entity; a recurring run is an AI Agent with a `schedule` channel (`ai_agent_channel` row, 5-field cron in `expression`). Any page describing a "Scheduled task" entity is stale.

```bash
cd /Volumes/Data/bytechef/bytechef
grep -rn "ChannelDefinitions" --include='*.java' server/libs server/ee | head -5
```

- [ ] **Step 3: Verify guardrails placement**

Guardrails are a standalone EE module (`platform-ai-guardrails`), registered unconditionally and no longer gated on `bytechef.ai.gateway.enabled`. The settings route is `/automation/settings/ai/agents/guardrails`. Remove any "AI Gateway traffic only" claim.

- [ ] **Step 4: Verify the agent built-in tools**

`AiAgent.settings.builtInTools`: `askUserQuestion`, `autoMemory`, `skillManagement` default **on**; `webSearch` defaults **off** and needs a `webSearchConnectionId` to publish. There is no `skills` key — attaching a `SKILL` row is itself the opt-in.

- [ ] **Step 5: Fix every discrepancy found and re-run the validator**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
./node_modules/.bin/bun ./scripts/lint.ts
```

Expected: clean.

- [ ] **Step 6: Commit with a changelog**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform/automation
git commit -m "732 docs - Correct the AI Hub, agent and guardrails pages against the code

<one line per altered claim>"
```

### Task 13: Batch 2 — Deploy surfaces (MCP, A2A, AI Gateway, API platform)

**Files:** `content/docs/platform/automation/deploy/**` (6 pages incl. the new `environments.mdx`), `content/docs/platform/settings/mcp-server.md`

**Interfaces:** Consumes the per-batch procedure.

- [ ] **Step 1: Verify the MCP tool-mapping model**

A workflow is MCP-exposable only with a `workflow/newWorkflowCall` trigger. The mapping (`toolName`, `toolDescription`, `fromAi(...)` input values) lives on `McpProjectWorkflow.parameters`, **not** in the workflow definition. `createMcpProject` attaches workflows with empty parameters, so a setup is not servable until the mapping is completed.

- [ ] **Step 2: Verify the A2A endpoints**

`GET /api/automation/a2a/{secretKey}/.well-known/agent-card.json` and `POST /api/automation/a2a/{secretKey}`. The card advertises `streaming=false`; `message/stream` is event-level, not token-level. Tasks live in a bounded in-handler LRU, not durable storage.

- [ ] **Step 3: Verify the AI model catalog split**

Per `CLAUDE.md`, `ai_model` moved into a CE/EE `platform-ai-model-catalog` twin; the reconciler stayed gateway-side; `aiProviderCatalog` labels come from the CE catalog. Names that deliberately keep the `AiGatewayModel` prefix must not be "corrected".

- [ ] **Step 4: Fix, validate, commit**

```bash
cd docs && ./node_modules/.bin/fumadocs-mdx && ./node_modules/.bin/bun ./scripts/lint.ts
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform
git commit -m "732 docs - Correct the deploy-surface pages against the code

<one line per altered claim>"
```

### Task 14: Batch 3 — Embedded

**Files:** `content/docs/platform/embedded/**` (27 pages including the new `index.mdx`)

- [ ] **Step 1: Verify the embedded automation bridge**

`POST /api/embedded/internal/automation/projects/deploy`, the `__EMBEDDED_AUTOMATION__` marker, and the `COPY` vs `REFERENCE` project kinds. Per-user connection wiring for a reference lives in `connected_user_project_workflow_connection`, not `WorkflowTestConfiguration`.

- [ ] **Step 2: Verify the MCP credential story on the merged `mcp-servers.md`**

Confirm the two Bearer credential types, the `X-Environment` header, and the **Require authentication** toggle semantics carried over in Task 5. Then decide the "Per-tenant tool exposure controls" question Task 5 deferred: check whether a three-state Off / On / Approval-gated per-workflow setting actually exists. If it does not, do not add it.

- [ ] **Step 3: Fix, validate, commit**

```bash
cd docs && ./node_modules/.bin/fumadocs-mdx && ./node_modules/.bin/bun ./scripts/lint.ts
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform/embedded
git commit -m "732 docs - Correct the embedded pages against the code

<one line per altered claim>"
```

### Task 15: Batch 4 — Settings and administration

**Files:** `content/docs/platform/settings/**` **excluding `settings/mcp-server.md`** (14 pages), `content/docs/platform/automation/settings/**` (6 pages), `content/docs/platform/your-account/**` (3 pages)

> `settings/mcp-server.md` is named explicitly by **Task 13**, which reviews the MCP surface as a
> whole; an explicit assignment beats this task's wildcard. Leave it alone.

Most of these were written from scratch in Phase C, so this batch verifies new prose rather than
aged prose — except the seven stubs in Step 3, which no enterprise page fed and which this task
writes.

- [ ] **Step 1: Verify the visibility model**

`ResourceVisibility` is `PRIVATE < WORKSPACE < ORGANIZATION`. Everything is created **WORKSPACE**-visible. CE force-writes `WORKSPACE` with no picker; embedded force-writes `PRIVATE`. "Specific people" is `PRIVATE` plus `resource_grant` rows, not a fourth stored value. `WORKSPACE` grants *use plus existence*, not read plus write — authorization parameters are obfuscated and never mutated after creation.

- [ ] **Step 2: Verify RBAC scoping**

Per the project memory, RBAC collapsed to workspace-only scoping (ticket 1051) and project roles were removed in 2026-07. `settings/users.mdx` must not describe project roles.

- [ ] **Step 3: Write the seven stubs no enterprise page fed**

Phase C filled every stub that had a counterpart in the enterprise tree. These seven had none, so
they are still frontmatter-only and this task owns them. Each is a UI surface that exists in the
product — write it from the client code, not from imagination.

| Stub | Client source to read |
|---|---|
| `settings/notifications.mdx` | The notification settings page; `Notification.Type` is `EMAIL`, `WEBHOOK`, `SLACK` with settings keys `email` / `webhook` + `webhookSecret` / `slackWebhookUrl` |
| `automation/settings/users.mdx` | Workspace-level member list — distinct from organization-level `settings/users.mdx`; say how they differ rather than repeating |
| `automation/settings/ai-hub-connectors.mdx` | The AI Hub composer's Resources menu connector toggles, scoped per chat |
| `automation/settings/ai-agents/system-prompt.mdx` | The workspace system prompt tab, sibling of the guardrails tab |
| `your-account/profile.mdx` | Profile fields the user can edit |
| `your-account/appearance.mdx` | Theme selection, including the three-state light/dark/system behavior |
| `your-account/active-sessions.mdx` | Session list and revocation |

Each needs at least a paragraph of orientation plus what the page lets you change. If a surface
turns out not to exist in the client, delete the stub and its `meta.json` entry rather than writing
a page for it — and say so in the commit body.

- [ ] **Step 4: Fix, validate, commit**

```bash
cd docs && ./node_modules/.bin/fumadocs-mdx && ./node_modules/.bin/bun ./scripts/lint.ts
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform
git commit -m "732 docs - Verify the settings pages written during the enterprise fold

<one line per altered claim>"
```

### Task 16: Batch 5 — Self-hosted

**Files:** `content/docs/platform/use-bytechef/**` (21 pages after Phase C)

- [ ] **Step 1: Verify every property name in `environment-variables.md`**

That page is 4,174 words of property reference and is the highest-value target in the corpus. Check each `BYTECHEF_*` name against `ApplicationProperties` — per `CLAUDE.md`, strict binding means every property must be a field there, so a name absent from that class is wrong.

```bash
cd /Volumes/Data/bytechef/bytechef
find server -name 'ApplicationProperties.java' | head
```

- [ ] **Step 2: Verify the default ports**

Server 9555 in `dev`, 8080 in `prod` — one app, not two services. Client 5173. PostgreSQL 5432. Redis 6379. Mailpit 1025 (the compose service is `mailpit`, not `mailhog`).

- [ ] **Step 3: Verify the crash-recovery and plan-limit pages written in Task 8**

- [ ] **Step 4: Fix, validate, commit**

```bash
cd docs && ./node_modules/.bin/fumadocs-mdx && ./node_modules/.bin/bun ./scripts/lint.ts
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform/use-bytechef
git commit -m "732 docs - Correct the self-hosted pages against the code

<one line per altered claim>"
```

### Task 17: Batch 6 — Automation data and workflows

**Files:** `content/docs/platform/automation/data/**` (9), `content/docs/platform/automation/build/workflows/**` **excluding `build/workflows/ai/**`** (12), `content/docs/platform/automation/build/connections/**` (7), `content/docs/platform/automation/get-started/**` (4), `content/docs/platform/automation/monitor/**` (1)

> **Scope boundary — enforce it mechanically.** `build/workflows/**` textually subsumes
> `build/workflows/ai/**`, which is **Task 12's**. Nineteen pages sit in that overlap. Enumerate your
> file list with the exclusion applied before you start:
> `find content/docs/platform/automation/build/workflows -name '*.md*' | grep -v '/workflows/ai/'`
> `content/docs/reference/flow-controls/` is **generated** — read it as evidence for the task-dispatcher
> pages, never edit it.

- [ ] **Step 1: Verify the flow-control set against the generated reference**

`content/docs/reference/flow-controls/` lists the dispatchers that actually exist: branch, condition, each, fork-join, graph, loop, loop-break, map, on-error, parallel, subflow, terminate, wait-for-approval. `flow-controls.mdx` must not describe one that is absent, nor omit one that is present.

```bash
cd docs
ls content/docs/reference/flow-controls/
```

- [ ] **Step 2: Verify the error-workflow model**

Configured via `project.error_project_workflow_id` with a per-workflow override and a separate `error_workflow_disabled` flag. The handler must live in the same project and carry a `workflow/newWorkflowError` trigger. Recursion is capped at depth 1. **Monolith only** — distributed EE cannot resolve the handler.

- [ ] **Step 3: Fix, validate, commit**

```bash
cd docs && ./node_modules/.bin/fumadocs-mdx && ./node_modules/.bin/bun ./scripts/lint.ts
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs/platform/automation
git commit -m "732 docs - Correct the automation data and workflow pages against the code

<one line per altered claim>"
```

### Task 18: Batch 7 — Developer guide and remaining root pages

**Files:** `content/docs/developer-guide/**`, `content/docs/platform/what-is-bytechef.mdx`, `content/docs/platform/glossary.mdx`, `content/docs/platform/index.mdx`, `content/docs/openapi/index.mdx`, `content/docs/openapi/meta.json`, `content/docs/reference/index.mdx`, `content/docs/reference/expressions.md`, `content/docs/reference/meta.json`, `content/docs/platform/enterprise/index.mdx`

> **This task is the catch-all**, so it owns the pages a coverage audit found orphaned by every other
> batch: the four hand-written files inside the otherwise-generated `reference/` and `openapi/`
> directories (see the Global Constraint — those two directories are only *partly* generated), and the
> capability index Task 10 writes at `platform/enterprise/index.mdx`, whose links all point into pages
> other tasks rewrote. Verify each of that index's links resolves to a page that still exists.

- [ ] **Step 1: Verify the CLI commands**

`bytechef component init --name … --open-api-path … --output-path …`; build with `./gradlew :cli:cli-app:installDist`. Note the `run` task's working directory is `cli/cli-app`, so it needs absolute paths.

- [ ] **Step 2: Verify the component-authoring contract**

`@AutoService(ComponentHandler.class)` for ServiceLoader discovery (no Spring DI), `@Component("name_v1_ComponentHandler")` when constructor injection is needed.

- [ ] **Step 3: Fix, validate, commit**

```bash
cd docs && ./node_modules/.bin/fumadocs-mdx && ./node_modules/.bin/bun ./scripts/lint.ts
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs
git commit -m "732 docs - Correct the developer guide and root pages against the code

<one line per altered claim>"
```

---

## Phase F — Frontmatter schema and components

### Task 19: Add the `ee` and `comingSoon` frontmatter fields

**Files:**
- Modify: `docs/source.config.ts:15-24` — the `frontmatterSchema.extend({...})` block
- Modify: `docs/mdx-components.tsx`

**Interfaces:**
- Consumes: nothing.
- Produces: two boolean frontmatter fields readable as `page.data.ee` and `page.data.comingSoon`; two newly-registered MDX components.

> **Only two components actually need registering — measured, not assumed.** `TypeTable` and
> `Banner` are ALREADY registered, in `app/(docs)/[...slug]/page.tsx` rather than in
> `mdx-components.tsx`, which is why a grep of the latter suggests otherwise. So are `Callout`,
> `Card`/`Cards`, `Accordion`/`Accordions`, and — via the `...TabsComponents` and
> `...FilesComponents` **spreads**, which a whole-word grep does not match — `Tabs`/`Tab` and
> `Files`/`File`/`Folder`. Re-registering any of them is harmless but pointless; treat the two
> registration sites as one surface when checking.
>
> Genuinely missing: **`Steps`/`Step`** and **`ImageZoom`**. Both ship in `fumadocs-ui`
> (`dist/components/steps.js`, `dist/components/image-zoom.js`) and neither is in
> `defaultMdxComponents`. `ImageZoom` also has its own stylesheet,
> `fumadocs-ui/components/image-zoom2.css` — check whether it needs importing in `app/global.css`
> alongside the other presets, or the component renders unstyled.
>
> Note `remarkSteps` is already active in `source.config.ts`, so the markdown-shorthand form of
> steps may work without the component; verify which form you actually want before converting
> content in Task 21.
>
> **Current usage of all five is zero** across the hand-written tree, so Task 21 is applying
> components that are available rather than introducing new capability.

- [ ] **Step 1: Extend the frontmatter schema**

In `source.config.ts`, the `docs.schema` currently reads:

```ts
schema: frontmatterSchema.extend({
  preview: z.string().optional(),
  index: z.boolean().default(false),
  /**
   * API routes only
   */
  method: z.string().optional(),
}),
```

Add two fields:

```ts
schema: frontmatterSchema.extend({
  preview: z.string().optional(),
  index: z.boolean().default(false),
  /**
   * API routes only
   */
  method: z.string().optional(),
  /**
   * The page documents an Enterprise Edition capability in full. Sections that are
   * only partly EE keep the inline <EEBadge /> instead.
   */
  ee: z.boolean().default(false),
  /**
   * The capability is implemented but not in the latest released version. Read by the
   * marketing site as the authoritative feature-status gate, which is why it is a
   * structured field rather than prose.
   */
  comingSoon: z.boolean().default(false),
}),
```

- [ ] **Step 2: Register the four missing components**

In `mdx-components.tsx`, add the imports and spread them into the returned object alongside the existing `TabsComponents` and `FilesComponents`:

```tsx
import * as StepsComponents from 'fumadocs-ui/components/steps';
import { TypeTable } from 'fumadocs-ui/components/type-table';
import { ImageZoom } from 'fumadocs-ui/components/image-zoom';
import { Banner } from 'fumadocs-ui/components/banner';
```

and inside `getMDXComponents`:

```tsx
    ...StepsComponents,
    TypeTable,
    ImageZoom,
    Banner,
```

- [ ] **Step 3: Verify the schema and registrations compile**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
npm run types:check
```

Expected: exit 0. If `fumadocs-ui/components/banner` fails to resolve, confirm the export path:

```bash
ls node_modules/fumadocs-ui/dist/components/ | grep -E 'steps|type-table|image-zoom|banner'
```

Expected: all four present.

- [ ] **Step 4: Prove the flags render by converting one page**

Pick `content/docs/platform/embedded/configure/mcp-servers.md` — it currently carries a
`> **Coming soon.**` blockquote. Rename it to `.mdx`, remove the blockquote, and add
`comingSoon: true` to its frontmatter. Then confirm the page still builds and the flag is readable:

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
node -e "
const {docs}=require('./.source/index.js');
" 2>/dev/null || npm run types:check
```

Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/source.config.ts docs/mdx-components.tsx docs/content/docs/platform/embedded
git commit -m "732 docs - Add ee and comingSoon frontmatter flags and register four components

Page status was expressed four incompatible ways across 67 pages and is
scraped as prose by the marketing site. Registers Steps, TypeTable, ImageZoom
and Banner, which were available but never wired up."
```

---

### Task 20: Convert the page-level "Coming soon" markers to the frontmatter flag

**Files:**
- Modify: the 67 hand-written pages carrying a "Coming soon" marker

**Interfaces:**
- Consumes: the `comingSoon` field from Task 19.
- Produces: one consistent status mechanism.

> **The classification is already done — do not redo it by hand.**
> `$SCRATCH/coming-soon-classified.json` holds the measured split of the 65 pages that carry a
> marker, produced by locating each marker relative to the page's first `##` heading:
>
> - **`page` (32)** — every marker sits above the first heading, so it governs the whole page.
>   These convert to `comingSoon: true` cleanly.
> - **`section` (29)** — every marker sits under a heading, so it governs one section of an
>   otherwise-shipped page. **These keep their prose callouts.** A page-level flag here would be
>   the inverse of the error this plan keeps finding: it would mark shipped content unavailable.
>   `environment-variables.md` alone holds 16 of them, and flagging that page would mark the entire
>   environment-variable reference as unreleased.
> - **`both` (4)** — a page-level marker plus section markers. Convert the page-level one to the
>   flag and leave the section callouts in place.
>
> The two mechanisms are complementary, not alternatives: the flag states "this page documents
> something you cannot use yet", the callout states "this part of an otherwise-usable page".

- [ ] **Step 1: Enumerate the pages and their marker form**

```bash
cd docs
grep -rilE 'coming soon' content/docs --include='*.mdx' --include='*.md' \
  | grep -vE '/(reference|openapi)/' | sort > "$SCRATCH/coming-soon-pages.txt"
wc -l < "$SCRATCH/coming-soon-pages.txt"
```

Expected: 67 (fewer if Phase C deleted some).

- [ ] **Step 2: Classify each page as whole-page or partial**

A page is **whole-page** if the marker applies to everything it documents — replace the marker with `comingSoon: true` frontmatter and rename `.md` → `.mdx` if it is not already MDX.

A page is **partial** if only a section is unshipped — keep an inline `<Callout type="warn" title="Coming soon">` scoped to that section and do **not** set the frontmatter flag. `human-in-the-loop.mdx` from Task 5 is the canonical partial case.

The inline parenthetical form `(coming soon)` inside a heading is always partial; convert it to a `<Callout>` under the heading and remove it from the heading text — heading text changes break `#fragment` links, so run the validator after.

- [ ] **Step 3: Fix the fragments this breaks**

Removing `(coming soon)` from a heading changes its slug. Known affected fragments:
`#code-workflows-coming-soon`, `#microservices-topology-coming-soon`,
`#auto-generated-api-connectors-coming-soon`, `#external-secret-managers-coming-soon`.

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
./node_modules/.bin/bun ./scripts/lint.ts > "$SCRATCH/linkcheck-after-f.log" 2>&1
grep -E 'invalid-fragment' "$SCRATCH/linkcheck-after-f.log" || echo "no fragment breakage"
```

Repoint every reported fragment to the new slug.

- [ ] **Step 4: Confirm zero broken links**

```bash
cd docs
./node_modules/.bin/bun ./scripts/lint.ts
```

Expected: no `Invalid URLs in ...` blocks.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs
git commit -m "732 docs - Replace the four Coming soon prose forms with the comingSoon flag

Whole-page markers become frontmatter; section-scoped markers become a single
scoped Callout. Repoints the fragments that heading-text changes invalidated."
```

---

### Task 21: Apply Steps, Tabs, TypeTable and ImageZoom where the content already has the shape

**Files:**
- Modify: the pages listed per component below
- Rename: `.md` → `.mdx` only for pages that gain a component

**Interfaces:**
- Consumes: the registrations from Task 19.
- Produces: no new URLs — the extension does not affect the route.

**Rule:** apply a component only where the content is *already* in that shape. Do not restructure prose to justify a component.

- [ ] **Step 1: Apply `Steps` to numbered procedures**

Targets: `platform/embedded/get-started/quick-start/index.mdx`,
`platform/embedded/get-started/initial-setup/*.mdx` (3),
`platform/automation/get-started/quick-start/*.mdx` (3),
`platform/automation/data/knowledge-base/create-a-knowledge-base.mdx`,
`platform/automation/data/knowledge-base/add-documents.mdx`,
`platform/use-bytechef/self-hosted/installation/*.md` (8).

Wrap the existing ordered list in `<Steps>` and promote each item to a `###` heading inside a `<Step>`. Rename the eight installation pages to `.mdx`.

- [ ] **Step 2: Apply `Tabs` to per-platform and per-language variants**

Targets: `platform/use-bytechef/self-hosted/installation/*.mdx` — where a page shows the same step for Docker vs Kubernetes vs a cloud provider; `platform/automation/build/connections/authentication/*.mdx` (4) — where the same concept is shown per auth type; `platform/embedded/get-started/initial-setup/installing-the-sdk.mdx` — npm/yarn/pnpm and React/vanilla variants.

Note `remarkNpmOptions` already sets `persist: { id: 'package-manager' }`, so package-manager tabs share selection across pages for free.

- [ ] **Step 3: Apply `TypeTable` to property references**

Primary target: `platform/use-bytechef/self-hosted/configuration/environment-variables.md` (4,174 words). Convert its property tables to `TypeTable` so each entry carries a type and a default. Rename to `.mdx`.

Secondary: the three configuration pages created in Task 8.

- [ ] **Step 4: Apply `ImageZoom` to screenshot-heavy walkthroughs**

Targets: any page with three or more screenshots — the embedded configure and monitor pages, the automation build pages, `platform/automation/build/workflows/ai/agent/evals.md`.

- [ ] **Step 5: Verify nothing broke**

```bash
cd docs
./node_modules/.bin/fumadocs-mdx
npm run types:check
./node_modules/.bin/bun ./scripts/lint.ts
npm run build
```

Expected: all exit 0, validator clean. `npm run build` is required — a malformed `<Steps>` nesting is a render error the type check does not catch.

- [ ] **Step 6: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add docs/content/docs
git commit -m "732 docs - Apply Steps, Tabs, TypeTable and ImageZoom to the pages already shaped for them

Renames the affected .md pages to .mdx; routes are unchanged by the extension."
```

---

## Final verification

- [ ] **Full clean run**

```bash
cd docs
rm -rf .next .source
./node_modules/.bin/fumadocs-mdx
npm run types:check
./node_modules/.bin/bun ./scripts/lint.ts
npm run build
npm run lint
```

Expected: every command exits 0, and the validator reports no `Invalid URLs in ...` blocks.

- [ ] **Confirm the enterprise tree is one page**

```bash
cd docs
find content/docs/platform/enterprise -name '*.md*'
```

Expected: exactly `content/docs/platform/enterprise/index.mdx`.

- [ ] **Confirm no stub pages remain in settings**

```bash
cd docs
for f in $(find content/docs/platform/settings content/docs/platform/automation/settings \
                content/docs/platform/your-account -name '*.mdx'); do
  w=$(wc -w < "$f" | tr -d ' ')
  [ "$w" -lt 100 ] && echo "STILL A STUB: $f ($w words)"
done
```

Expected: no output. Tasks 6 and 7 fill the stubs that had an enterprise source; Task 15 Step 3
writes the seven that did not.

- [ ] **Confirm redirect coverage**

```bash
cd /Volumes/Data/bytechef/bytechef
grep -c "platform/enterprise/" docs/next.config.ts
```

Expected: 48.
