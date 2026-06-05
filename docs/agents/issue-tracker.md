# Issue tracker: GitHub

Issues and PRDs for this repo live as GitHub issues on **`bytechefhq/bytechef`**. Use the `gh`
CLI for all operations, and **always pass `--repo bytechefhq/bytechef`** — this clone has multiple
GitHub remotes (`origin` → your fork `ivicac/bytechef`, `upstream` → `bytechefhq/bytechef`), so
relying on `gh`'s auto-detection can file issues against the wrong repo.

## Conventions

- **Create an issue**: `gh issue create --repo bytechefhq/bytechef --title "..." --body "..."`. Use a heredoc for multi-line bodies.
- **Read an issue**: `gh issue view <number> --repo bytechefhq/bytechef --comments`, filtering comments by `jq` and also fetching labels.
- **List issues**: `gh issue list --repo bytechefhq/bytechef --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` with appropriate `--label` and `--state` filters.
- **Comment on an issue**: `gh issue comment <number> --repo bytechefhq/bytechef --body "..."`
- **Apply / remove labels**: `gh issue edit <number> --repo bytechefhq/bytechef --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --repo bytechefhq/bytechef --comment "..."`

## When a skill says "publish to the issue tracker"

Create a GitHub issue on `bytechefhq/bytechef`.

## When a skill says "fetch the relevant ticket"

Run `gh issue view <number> --repo bytechefhq/bytechef --comments`.
