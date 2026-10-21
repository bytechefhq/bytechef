# ByteChef CLI

A command-line interface for ByteChef. It scaffolds custom components locally and calls the
ByteChef **public REST API** for automation resources.

## Configure a profile

```bash
bytechef configure --host https://app.bytechef.io --token <public-api-token> \
  --environment PRODUCTION --workspace-id 1
```

Credentials are stored in `~/.bytechef/config` (INI format, file mode `600`), one section per named
profile. Select a profile with `--profile <name>` (defaults to `default`).

Every value can be overridden per command, in this precedence:

1. per-command flag (`--host`, `--token`, `--environment`, `--workspace-id`)
2. environment variable (`BYTECHEF_HOST`, `BYTECHEF_TOKEN`, `BYTECHEF_ENVIRONMENT`,
   `BYTECHEF_WORKSPACE_ID`)
3. the selected profile in `~/.bytechef/config`

## Automation commands

```bash
# Deploy a code-based project archive
bytechef automation project deploy --workspace-id 1 --project-file ./project.zip

# Pull a project from its git repository
bytechef automation project pull --id 5
```

Requests are sent to `<host>/api/automation/v1` with `Authorization: Bearer <token>` and
`X-Environment: <environment>` headers.

## Embedded commands

Embedded commands act on behalf of a connected user, identified by `--external-user-id`, and hit
`<host>/api/embedded/v1`.

```bash
# Integrations and integration instances
bytechef embedded integration list --external-user-id user-42
bytechef embedded integration get --external-user-id user-42 --id 3
bytechef embedded integration-instance create --external-user-id user-42 --id 3 --data @instance.json
bytechef embedded integration-instance delete --external-user-id user-42 --id 9
bytechef embedded integration-instance workflow-enable|workflow-disable --external-user-id user-42 --id 9 --workflow-uuid wf-1
bytechef embedded integration-instance workflow-update --external-user-id user-42 --id 9 --workflow-uuid wf-1 --data @cfg.json
bytechef embedded integration-instance input-options --external-user-id user-42 --id 9 --data @req.json

# Projects and project workflows
bytechef embedded project list --external-user-id user-42
bytechef embedded workflow list --external-user-id user-42
bytechef embedded workflow get|enable|disable|delete --external-user-id user-42 --workflow-uuid wf-1
bytechef embedded workflow publish --external-user-id user-42 --workflow-uuid wf-1 --description "v2"
bytechef embedded workflow create --external-user-id user-42 --data @workflow.json
bytechef embedded workflow update --external-user-id user-42 --workflow-uuid wf-1 --data @workflow.json
bytechef embedded workflow generate --external-user-id user-42 --data '{"prompt":"a workflow that..."}'
bytechef embedded workflow update-from-prompt --external-user-id user-42 --workflow-uuid wf-1 --data '{"prompt":"..."}'
bytechef embedded workflow copy-template --external-user-id user-42 --workflow-uuid tmpl-1
bytechef embedded workflow set-connection --external-user-id user-42 --workflow-uuid wf-1 --node-name n1 --connection-key c1 --data @conn.json

# Tools and actions
bytechef embedded tool list --external-user-id user-42
bytechef embedded tool execute --external-user-id user-42 --data '{"name":"my_tool"}'
bytechef embedded action execute --external-user-id user-42 --component-name slack \
  --component-version 1 --action-name sendMessage --data @input.json

# Connected user and connections
bytechef embedded user update --external-user-id user-42 --data '{"name":"New Name"}'
bytechef embedded connection list --external-user-id user-42 --component-name slack
```

Commands that take a request body accept `--data` as either literal JSON or `@path/to/file.json`.
List commands accept `--output table` for a compact summary (JSON is the default), plus the API's
filter flags: `tool list` takes `--categories/--components/--tools`, and `connection list` takes
`--connection-ids` (comma-separated).

The **frontend** variants of the embedded API (connected-user session token, for the browser SDK) and
the AI Gateway and webhook endpoints are intentionally not exposed as CLI commands — their auth or
inbound-call model doesn't fit a CLI.

## Component scaffolding

```bash
bytechef component init --name my-component --open-api-path ./openapi.yaml --output-path .
```

`component init` is local-only and contacts no instance.

## Exit codes

| Code | Meaning |
|------|---------|
| 0 | success |
| 1 | generic error (bad request, missing file, unexpected failure) |
| 2 | authentication failure (HTTP 401/403) |
| 3 | not found (HTTP 404) |
| 4 | missing or invalid configuration |
