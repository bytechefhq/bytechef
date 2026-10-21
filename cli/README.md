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

## API collection commands

API collections and their endpoints are managed in any environment (`--environment`), with the same profile and
`X-Environment` handling as the other automation commands. Output is JSON by default; `--output table` is also accepted.

```bash
bytechef automation api-collection list --workspace-id 1 --project-id 3
bytechef automation api-collection create --project-id 3 --project-version 2 --name orders --context-path orders \
  --collection-version 1 --description "Order API"
bytechef automation api-collection get --id 5
bytechef automation api-collection update --id 5 --name renamed --description "New description"
bytechef automation api-collection delete --id 5
bytechef automation api-collection update-version --id 5 --project-version 3 --remove-missing-endpoints
bytechef automation api-collection endpoint list --collection-id 5
bytechef automation api-collection endpoint create --collection-id 5 --name create-order --method POST \
  --path /orders --workflow-uuid <uuid> --connection http_1:connection=7 --inputs-json '{"orderId":"abc"}'
bytechef automation api-collection endpoint update --id 9 --path /orders/new
bytechef automation api-collection endpoint delete --id 9
bytechef automation api-collection endpoint enable --id 9
bytechef automation api-collection endpoint disable --id 9
```

- `--connection node:key=connectionId` binds a connection to a connection slot of the endpoint's workflow and is
  repeatable; a malformed value fails locally before any request is sent. `--inputs-json` takes a JSON object.
- `create` puts the API collection in the workspace the project belongs to.
- `endpoint update` keeps the stored connections and inputs unless `--connection` or `--inputs-json` is given, in
  which case they replace the stored ones as a whole. It cannot clear the connections or remove a single one: pass
  every connection the workflow should keep as a `--connection` instead.
- Endpoints that serve the same workflow share its connections and inputs, so setting them for one endpoint sets them
  for the others too.
- `update-version` prints the endpoints it removed, the connections and inputs the new version still needs, and the
  endpoints it disabled because of them.
- `cli/clients/automation-api-platform/openapi.yaml` is a copy of
  `server/ee/libs/automation/automation-api-platform/automation-api-platform-configuration/automation-api-platform-configuration-rest/openapi-public.yaml`
  with the `Environment` schema inlined. When the server spec changes, copy it again, inline `Environment`, and run
  `./gradlew :cli:clients:automation-api-platform:generateClient`.


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
