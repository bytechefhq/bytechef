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

## Data table commands

Data tables, their columns and their rows are addressed by name and managed in any environment (`--environment`),
with the same profile and `X-Environment` handling as the other automation commands. Output is JSON by default.

```bash
bytechef automation data-table list --workspace-id 1 --tag hot
bytechef automation data-table get --name orders
bytechef automation data-table create --workspace-id 1 --name orders \
  --column total:NUMBER --column sku:STRING --tag hot
bytechef automation data-table update --name orders --description "New description" --tag hot --tag cold
bytechef automation data-table delete --name orders
bytechef automation data-table column add --name orders --column sku:STRING
bytechef automation data-table column remove --name orders --column sku
bytechef automation data-table column rename --name orders --column sku --new-name productSku

bytechef automation data-table row list --name orders --filter total:GTE:5 --filter sku:IN:a,b \
  --sort total:DESC --page-size 20
bytechef automation data-table row get --name orders --id 7
bytechef automation data-table row get --name orders --external-id ext-1
bytechef automation data-table row insert --name orders --values '{"total":5,"sku":"a"}' --external-id ext-2
bytechef automation data-table row update --name orders --id 7 --values '{"total":9}'
bytechef automation data-table row upsert --name orders --external-id ext-2 --values '{"total":9}'
bytechef automation data-table row delete --name orders --id 7
bytechef automation data-table row delete --name orders --ids 1,2,3
bytechef automation data-table row delete --name orders --external-id ext-2
bytechef automation data-table row batch --name orders --file ./rows.json --strategy upsert
bytechef automation data-table row clear --name orders --yes true
bytechef automation data-table row import --name orders --file ./rows.csv
bytechef automation data-table row export --name orders --file ./export.csv
```

- `--column` on `create` and `column add` is `name:TYPE` (`STRING`, `NUMBER`, `INTEGER`, `DATE`, `DATE_TIME`,
  `BOOLEAN`). A malformed value or an unknown type fails locally before any request is sent.
- `--filter` and `--sort` on `row list`, and `--column` and `--tag` on `create` and `update`, are repeatable. Filters
  and sorts are forwarded to the API as given: a filter is `column:OPERATOR:value` (`EQ`, `NEQ`, `IN`, `CONTAINS`,
  `STARTS_WITH`, `GT`, `GTE`, `LT`, `LTE`, `BETWEEN`) and a sort is `column:ASC` or `column:DESC`.
- `row delete` takes exactly one of `--id`, `--ids` (comma-separated) or `--external-id`.
- `row clear` runs only with `--yes true`; a bare `--yes` is not read as a flag on multi-word commands.
- `row update --external-id` can set an external id but not clear one, and `update --tag` can replace a table's tags
  but not clear them: the generated client is built with `openApiNullable=false`, so an omitted value means "leave it
  as it is" and an explicit null cannot be sent.
- `row export` writes the CSV to `--file`, or to stdout without it. It sends its request directly instead of through
  the generated `DataTableRowApi#exportRows`, which tries to read the `text/csv` reply as JSON.
- `cli/clients/automation-data-table/openapi.yaml` is a copy of
  `server/ee/libs/automation/automation-data-table/automation-data-table-public-rest/openapi.yaml`. When the server
  spec changes, copy it again and run `./gradlew :cli:clients:automation-data-table:generateClient`.

## App commands

For an app made from the `create-bytechef-app` template, usually run by a coding agent (the template's `AGENTS.md`
describes the whole loop). An app has instances, one per environment and possibly several; the key, the URL, the
bindings and the sign-in mode belong to an instance. The commands address the instance recorded in
`.bytechef/app.json`, or the one `--instance-id` names. `list`, `instance list` and `status` work in every
environment; `create`, `link`, `instance create`, `instance update`, `instance deployment-settings set|clear`,
`key rotate`, `bind` and
`unbind` refuse any environment but
DEVELOPMENT before sending a request, so use a profile whose token is an **automation API key created in DEVELOPMENT**
(an automation API key works only in the environment it was created in). The server refuses the same operations
outside DEVELOPMENT too. The version and deployment commands (`version create`, `version list`, `deployment deploy`,
`deployment rollback`, `deployment cancel`, `deployment record`, `deployment list`) and
`instance deployment-settings get` work in every environment: they are what a CI job runs to deploy the app or after
it deployed the app.

```bash
bytechef configure --profile dev --host https://app.bytechef.io --token <automation-api-key> \
  --environment DEVELOPMENT --workspace-id 1

# In the app's directory
bytechef automation app create --name my-app --profile dev
# or, with the optional fields
bytechef automation app create --name my-app --description "Order tracker" --hosting-target SELF_HOSTED --profile dev
# an app whose users do not sign in
bytechef automation app create --name my-app --user-auth-mode NONE --profile dev
bytechef automation app link --app-id 42 --profile dev
bytechef automation app list --profile dev
bytechef automation app instance list --app-id 42 --profile dev
bytechef automation app instance create --app-id 42 --name Partner --env-file .env.partner --profile dev
# where the app runs; sign-in registers its callback from it, so set it before the first sign-in
bytechef automation app instance update --url http://localhost:3000 --profile dev
bytechef automation app instance update --app-id 42 --instance-id 8 --name Partner --profile dev
bytechef automation app status --profile dev --output table
bytechef automation connection list --component-name hubspot --profile dev
bytechef automation app bind connection --binding-key crm --component-name hubspot \
  --actions createContact,getContact --tools searchContacts --proxy GET --profile dev
bytechef automation app bind workflow --workflow-key onboard-customer --deployment-id 7 \
  --workflow-uuid 3f1c2a9e-0d4b-4c55-9a43-6a1f0e2b7c11 --profile dev
bytechef automation app bind api-collection --api-collection-id 9 --profile dev
bytechef automation app unbind connection --binding-key crm --profile dev
bytechef automation app unbind workflow --workflow-key onboard-customer --profile dev
bytechef automation app unbind api-collection --api-collection-id 9 --profile dev
bytechef automation app key rotate --profile dev
# an app with several instances in the profile's environment: name the instance
bytechef automation app status --app-id 42 --instance-id 8 --profile dev
bytechef automation app key rotate --app-id 42 --instance-id 8 --env-file .env.partner --profile dev
# the hosting provider connection and settings ByteChef deploys the instance with
bytechef automation app instance deployment-settings set --connection-id 12 --setting projectId=prj_1 \
  --setting repository=acme/my-app --setting teamId=team_1 --profile dev
bytechef automation app instance deployment-settings get --profile dev
bytechef automation app instance deployment-settings clear --profile dev

# In CI, ByteChef deploys the version through the instance's hosting provider and waits for the result
bytechef automation app deployment deploy --app-id 42 --instance-id 9 --version 1.4.0 --commit "$GIT_SHA" \
  --git-ref main --wait --timeout 900 --environment PRODUCTION
bytechef automation app deployment rollback --app-id 42 --instance-id 9 --version 1.3.2 --wait --environment PRODUCTION
bytechef automation app deployment cancel --app-id 42 --instance-id 9 --environment PRODUCTION

# In CI, after deploying the app, with a profile or --host/--token/--environment of the target environment
bytechef automation app version create --app-id 42 --name 1.4.0 --commit "$GIT_SHA" --git-ref main \
  --description "Spring release" --environment PRODUCTION
bytechef automation app version list --app-id 42 --environment PRODUCTION --limit 10
# the versions of an instance your workspace hosts of an app another workspace owns
bytechef automation app version list --app-id 42 --instance-id 9 --environment PRODUCTION
bytechef automation app deployment record --app-id 42 --instance-id 9 --version 1.4.0 \
  --url https://my-app.example.com --commit "$GIT_SHA" --environment PRODUCTION
bytechef automation app deployment list --app-id 42 --instance-id 9 --environment PRODUCTION --output table
```

- `create` writes `BYTECHEF_URL` and `BYTECHEF_APP_KEY` into `--env-file` (default `.env.local`) and
  `.bytechef/app.json` (`id`, `uuid`, `instanceId`, `workspaceId`, `environment`, `host`; no secret, commit it) in the current
  directory. It never prints the key. Only the two `BYTECHEF_*` lines change; every other line stays. The file is
  written to an owner-only (`600`) temporary file and renamed into place, and the CLI warns when git does not ignore
  it. If a write fails after the app was created, the error names the app id: `automation app link --app-id <id>`
  restores `.bytechef/app.json` and `automation app key rotate` writes a new key.
- `--hosting-target` is `VERCEL` (the default) or `SELF_HOSTED`; `--description` is optional.
- `--user-auth-mode` is `NONE`, `OPTIONAL` or `REQUIRED` on `create` and `instance create`. Without it the CLI sends
  no mode and the server picks one: `REQUIRED` when the OAuth2 authorization server is on, `NONE` when it is off; the
  output's `userAuthMode` names the mode the instance got. When the server picked `NONE`, a warning on stderr says the
  instance has no end-user sign-in and that anyone with the app's URL can open it. An explicit `OPTIONAL` or `REQUIRED`
  with the server off answers 409 `APP_USER_AUTH_UNAVAILABLE` and the CLI suggests `--user-auth-mode NONE`.
- `instance create` adds an instance to an existing app (`--app-id`, or the linked directory) in DEVELOPMENT.
  `--workspace-id` is the workspace of the new instance and defaults to the profile's workspace; it may differ from the
  app's, and the server decides whether you may. `--env-file` is required: the new instance's key is written there
  and never printed, so it cannot overwrite the linked instance's key unless you name that file. `instance create`
  never writes or changes `.bytechef/app.json`; run `link --app-id <id> --instance-id <id>` in another directory to
  link the new instance. `instance list` shows each instance's id, name, environment, workspace, status, sign-in mode
  and URL in the profile's environment.
- `instance update` sets the URL (`--url`), the name (`--name`) or both of the linked instance, or of the one
  `--app-id` and `--instance-id` name, in DEVELOPMENT; an option left out keeps its stored value. `create` and
  `instance create` take no URL, so set it with `instance update` before the first sign-in: an instance that requires
  sign-in has no sign-in client until its URL is set, and the app's `/auth/login` answers 503 with a message saying so.
- `--instance-id` on `link`, `status`, `instance update`, `key rotate`, `bind` and `unbind` addresses one instance of the app named by
  `--app-id` (or by the linked directory); it must be an instance in the profile's environment. Without it the linked
  directory's instance is used, or the app's only instance in the profile's environment and workspace; an app with
  several needs `--instance-id`. In a linked directory, `key rotate --instance-id` for another instance than the
  linked one also needs an explicit `--env-file`. `--app-id` with the linked app's id also works when the profile's
  environment is not the link's, which is how to read an app from STAGING or PRODUCTION inside its DEVELOPMENT directory.
- The other commands find the app through `.bytechef/app.json`, walking up from the current directory like git, or
  take `--app-id`. `--env-file` is relative to the directory holding `.bytechef/` when the app is found through it, and
  to the current directory otherwise. A `.bytechef/app.json` written for another host, environment or workspace than
  the resolved profile is an error. `create` and `link` refuse to run where one already exists.
- An app named by `--app-id` rather than by a matching `.bytechef/app.json` is fetched first: `status`, `key rotate`, `bind` and
  `unbind` need one instance in the profile's environment and workspace, or `--instance-id`. Inside a directory linked to another
  app, `key rotate --app-id` also needs an explicit `--env-file`, so it cannot overwrite the linked app's key.
- `bind connection` makes the binding exactly what it names: an omitted `--actions`, `--tools` or `--proxy` means
  none. Without `--connection-id` the CLI uses the only usable connection of `--component-name` and otherwise lists
  the candidates. Unknown action or tool names fail before anything is written, listing the allowed ones.
- `status` lists every instance of the app with its sign-in mode, then the bindings of the one instance it addresses.
- `deployment deploy` deploys `--version` of the app to an instance through the instance's hosting provider, with
  the instance's deployment settings. A version name the app does not have yet is created only when `--commit` or
  `--git-ref` says which code it is; without them the command fails (exit code 3) before deploying. An existing name
  with another `--commit` is refused before anything is sent. `deployment rollback` deploys an existing version again
  and never creates one. Both print the deployment. With `--wait` they first write the deployment id and status to
  stderr, then poll it until it is `READY`, `FAILED` or `CANCELED`, for at most `--timeout` seconds (default 600).
  A poll that fails with a transport error, HTTP 408, 429 or 5xx is retried until the timeout; 401, 403 and 404 stop the wait at
  once. The exit code is 1 when the deployment ends `FAILED` or `CANCELED`, when the provider could not start it, and
  when `--wait` times out or is interrupted, and 2 or 3 when a poll is refused or not found. Whenever the wait stops
  early, the CLI prints the last known deployment and its id; the deployment keeps running on the server, and the
  error names `deployment cancel --deployment-id <id>`. The server answers 409 when a deployment of the instance is already in progress
  (`APP_DEPLOYMENT_IN_PROGRESS`), when no hosting provider serves the instance's hosting target, and when the instance
  has no deployment connection; 400 when the deployment settings are not valid for the provider.
- `deployment cancel` cancels `--deployment-id`, or without it the instance's one deployment in progress (exit code 3
  when there is none). A canceled deployment stays in the history as `CANCELED`.
- `instance deployment-settings set` stores the hosting provider connection (`--connection-id`, required) and the
  provider settings the instance is deployed with; repeat `--setting key=value` for each, the value being everything
  after the first `=`. The settings replace the stored ones. `clear` removes both. Both are DEVELOPMENT only, like the
  other instance changes; `get` shows them in every environment.
- `deployment record` records that `--version` of the app was deployed to an instance by someone other than ByteChef,
  and makes it the instance's current deployment. A version name the app does not have yet is created with
  `--commit`; an existing name with another commit is refused (409 `APP_VERSION_COMMIT_MISMATCH`), so pick a new name.
  `--url` must be an absolute `http` or `https` URL. An empty `--commit` or `--url` (an unset CI variable) counts as
  absent. When two jobs record the same new version name at once, the loser gets 409
  `APP_VERSION_NAME_ALREADY_EXISTS`; `deployment record` retries once on exactly that error.
- The deployment commands address the instance like `status`: the linked directory's instance,
  `--instance-id`, or the app's only instance in the profile's environment and workspace. In CI there is usually no
  `.bytechef/app.json`: `--app-id` with `--instance-id` is enough. `version create` and `version list` need only the
  app. `deployment deploy` and `deployment rollback` find `--version` through the instance
  (`GET /app-instances/{id}/versions?name=`), and `version list` reads through the instance when `--instance-id` is
  given or the directory is linked to one, so a workspace that hosts an instance of an app owned by another workspace
  can deploy it and list its versions: it needs `APP_VIEW` on the instance, not on the app. When the app itself may not
  be read, `--instance-id` is checked against the instance instead. Creating a version (`version create`, or `deployment
  deploy` with a new name) still needs `APP_DEPLOY` on the app. None of the version and deployment commands writes a file, and none needs DEVELOPMENT; they need a role that
  may deploy the app (`APP_DEPLOY`: deploy, rollback, cancel, record, create) or view it (`APP_VIEW`: list) in the
  profile's environment, and their 401 and 403 messages name that environment and permission.
- `deployment list` and `version list` print the newest 50, newest first; `--limit` asks for 1 to 200.
- `status` shows each connection binding's component version as `none` (`null` in JSON output) when it cannot be resolved (the connection
  can no longer be bound, or you are not a workspace admin).
- `cli/clients/automation-app/openapi.yaml` is a copy of
  `server/ee/libs/automation/automation-app/automation-app-public-rest/openapi-management.yaml` with the
  `Environment` schema inlined (the server file's relative `$ref` does not resolve from the client). When the server
  spec changes, copy it again, inline `Environment`, and run `./gradlew :cli:clients:automation-app:generateClient`.

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
