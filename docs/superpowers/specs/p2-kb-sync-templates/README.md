# P2 Knowledge Base auto-sync — workflow templates

Six starter workflow templates implementing the rescoped P2 (see [the parity
spec](../2026-04-29-ai-hub-mothership-parity-design.md)). Each is a
schedule-driven workflow that lists items from a source, iterates them, and
loads each into a Knowledge Base via the existing
`ai_vectorstore_knowledgebase/v1/load` action.

## Why these live here, not in the runtime

The pre-built template gallery loads from
[`bytechefhq/bytechef-workflows`](https://github.com/bytechefhq/bytechef-workflows)
(see `application.yml`'s `templates.owner` / `templates.repo`). This directory
is a **design artifact** — a reviewable bundle to upload to that repo.
Nothing in this directory is loaded at runtime by the application.

## Upload path

Each subdirectory contains the two files the runtime expects in the template
ZIP archive:

- `template.json` — the metadata the gallery indexes (author, categories,
  description, last-modified).
- `workflow-<slug>.json` — the workflow definition the user clones.

To publish:

```sh
cd notion-to-kb
zip -r ../notion-to-kb.zip template.json workflow-notion-to-kb.json
# upload notion-to-kb.zip into bytechefhq/bytechef-workflows under workflows/
```

The runtime reads the ZIP and discovers the two files by name; the ZIP
filename becomes the template id.

## What's deliberately stubbed

The `ai_vectorstore_knowledgebase/v1/load` action requires a
`DOCUMENT_READER` cluster element (see `KnowledgeBaseLoadAction` and the
`actionClusterElementTypes` block on the component definition). The template
JSONs leave the cluster-element wiring as a TODO note in the load step's
`metadata.ui.note` — the user finalizes which reader to use (Tika for
text/binary, a source-specific reader where the source already returns rich
text) inside the visual workflow editor when they fork the template. Hard-coding
the reader here would require six different choices and a lot of brittle
schema, when one editor click handles it after the fork.

The category each template advertises is `Knowledge Base` so the gallery's
existing category filter surfaces the bundle as a coherent set.

## Source map

| Template | Trigger | Source action(s) | Notes |
|---|---|---|---|
| `notion-to-kb` | `schedule/v1/cron` (hourly) | `notion/v1/listDatabaseItems` | Page content read via the `documentReader` cluster element |
| `gdrive-to-kb` | `schedule/v1/cron` (hourly) | `googleDrive/v1/listFiles` → per-item `googleDrive/v1/downloadFile` | Folder-scoped sync |
| `github-to-kb` | `schedule/v1/cron` (daily) | `github/v1/getRepositoryContent` | Repo + path glob; works for docs/ and README.md style trees |
| `confluence-to-kb` | `schedule/v1/cron` (hourly) | placeholder for a Confluence component | Marked TODO in the workflow — Confluence component is not in the in-tree component set yet |
| `slack-to-kb` | `schedule/v1/cron` (daily) | placeholder for a Slack channel-history action | Slack component currently exposes `sendChannelMessage`/`addReaction`; the channel-history action would need to land first |
| `webhook-to-kb` | `webhook/v1/webhook` | none — payload IS the document | Push-mode for sources with no list API |

## Open follow-ups

- **Slack channel-history** and **Confluence** components: once those actions
  land in `server/libs/modules/components/`, the placeholder workflow tasks in
  the corresponding templates can be filled in. The trigger + each-loop +
  KB-load skeleton stays the same.
- **Delete propagation**: not in any of the six templates. When it matters,
  the workflow author adds a "list current KB docs by tag → diff against the
  source listing → call `knowledgeBase` delete action" tail. The platform
  already has the moving parts; deferring until a user actually asks.
- **Modified-since incremental sync**: most sources expose a "modified since"
  filter in their list action; using it cuts re-embedding cost dramatically.
  Each template should be revised to wire its source's modified-since
  parameter against the workflow's `${trigger.lastFiredAt}` value once
  upstream confirms that variable is available.
