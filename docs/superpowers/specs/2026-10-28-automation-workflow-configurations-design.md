# Automation Workflow Configurations

Issue: [#6756](https://github.com/bytechefhq/bytechef/issues/6756)

## Intent

Today, publishing an embedded automation workflow project is the same as releasing it everywhere:

- The Automation Hub (`GET /automation-workflow-projects/frontend`) and the provisioning API (`GET /automation-workflow-projects`) both call `AutomationWorkflowProjectFacade.getPublishedProjects(externalUserId, environment)`, which builds templates from the project's **last published version**, in every environment.
- On `AutomationWorkflowProjectPublishedEvent`, `ConnectedUserReferenceRolloutManager.rollOut` moves every connected user's reference deployment to `getLastPublishedVersion`.
- Copies (**Edit workflow** in the hub) are made from the last published version.
- **Show in Automation Hub** (`automation_workflow_project.automation_hub_visible`) is only filtered by the hub client (`automationHub.queries.ts`); the API serves hidden projects.

An admin cannot test a version in Development before Production picks it up, cannot turn a project off, and cannot hold back one workflow (short of an always-false permission expression).

Integrations already solve the same problem with per-environment **Configurations** (`IntegrationInstanceConfiguration`: environment, pinned `integrationVersion`, `enabled`, per-workflow `IntegrationInstanceConfigurationWorkflow.enabled`). Automation workflow projects get the same concept, holding release control only.

## Goals

- A publish creates a version; it reaches an environment only when that environment's configuration is pointed at it.
- An admin can switch a project off per environment, and switch individual workflows off per environment.
- Whether a project is listed in the Automation Hub is decided per environment.
- The hub, the API, references, and copies all read the same per-environment source of truth.
- Executions are viewed where they belong: per environment, from the configuration's workflow rows.

## Non-goals

- Per-environment connections or credentials. Connected users bring their own connections and inputs; a configuration stores none.
- Changing permission expressions. Project and workflow permission expressions stay on the project and its templates; they decide *who* sees a template, independent of environment, and are evaluated after the configuration filter.
- Changing how copies behave after they are made. A copy stays the user's own workflow and does not follow later versions.
- Multiple configurations per project per environment.

## Anti-goals

- **No implicit exposure.** Nothing reaches the hub or the API until an admin has an enabled configuration in that environment. Publishing never creates or moves a configuration on its own.
- **No second release mechanism.** Once configurations exist, nothing else may expose a version: `getLastPublishedVersion` stops being a release decision anywhere in the hub, API, rollout, copy, or attention paths.
- **No hub-only gating.** "Hidden from hub" stays a listing choice; "off" must be enforced on the server for the API too.

## Constraints

- The feature has not shipped in a release, so there is **no data migration and no compatibility default**. Existing rows in development databases may be dropped or left unconfigured (and therefore unexposed).
- EE code: Enterprise license header and `@version ee` on every class; the admin surface is GraphQL (`embedded-configuration-graphql`), the hub/API surface is the public REST API (`embedded-configuration-public-rest`).
- Liquibase changelog in its own commit, before code; one changelog for this unreleased feature.
- Follow the repo's facade/service split: facades use services, never repositories; `@PreAuthorize` on facades.

## Model

New aggregate, one row per automation workflow project per environment:

**`automation_workflow_project_configuration`**
- `id`
- `automation_workflow_project_id` → `automation_workflow_project.id` (cascade delete with the project)
- `environment_id`
- `project_version` (int, not null): the pinned published version
- `enabled` (boolean, default false)
- `automation_hub_visible` (boolean, default true)
- audit columns + `version`
- unique (`automation_workflow_project_id`, `environment_id`)

**`automation_workflow_project_configuration_workflow`**
- `id`
- `automation_workflow_project_configuration_id` (cascade delete)
- `workflow_uuid`: the template's stable project workflow uuid
- `enabled` (boolean, default true)
- unique (`automation_workflow_project_configuration_id`, `workflow_uuid`)

`automation_workflow_project.automation_hub_visible` is no longer read or written (the flag moved to the configuration); the column itself is dropped in a follow-up PR after the next release, not here.

A workflow without a row in the configuration-workflow table is treated as **enabled** (my call: new templates in a newly pinned version should not silently disappear; an admin opts a workflow out, not in). Because rows are keyed by the template's stable uuid, a workflow switched off stays off across version changes; a template removed in a later version leaves a harmless row that the builder may clean up on version change.

No column is added to `connected_user_project_workflow`. A reference's administrative state is derived (see References below), not stored.

## Behavior

**Exposure (hub + API).** `getPublishedProjects(externalUserId, environment)` returns only projects with an **enabled** configuration in `environment`, builds templates from the configuration's `project_version` (not the last published version), and drops templates whose configuration-workflow row is disabled. Permission expressions are then applied as today. The DTO carries the configuration's `automationHubVisible`. Both the frontend endpoint (`GET /automation-workflow-projects/frontend`) and the provisioning endpoint (`GET /automation-workflow-projects`) return hidden projects with `automationHubVisible: false`, because custom UIs list templates through the frontend endpoint too; the hub filters hidden projects out on the client. The model's published-version fields reflect the configuration's pinned version, not the project's last published one.

**Activation and provisioning.** Activating or provisioning a reference, or making a copy, for a project or template that is not exposed in the caller's environment fails with a clear error (not a silent no-op).

**References.** A reference's environment is the connected user's (`connected_user.environment_id`), never the caller's `X-ENVIRONMENT` header; `enableProjectWorkflow` defaults to Production when none is given, so the builder must resolve it from the user.

`rollOut` targets the configuration's `project_version` for that environment instead of `getLastPublishedVersion`. It runs when a configuration's version changes (new event, e.g. `AutomationWorkflowProjectConfigurationUpdatedEvent`), not on publish. `ConnectedUserReferenceAttentionResolver` compares against the configuration's version too.

**Administrative state is derived, not stored.** `connected_user_project_workflow.enabled` keeps its current meaning: the user's own switch. A reference's effective state is

    user enabled AND configuration enabled AND configuration-workflow enabled

resolved from the user's environment. The attention reason ("turned off by the administrator", wording left to the builder) comes from `ConnectedUserReferenceAttentionResolver` at read time, like the existing reasons. Turning a configuration or a workflow off or on, deleting a configuration, or recreating one, re-applies the derived value to the reference deployments' `ProjectDeploymentWorkflow.enabled` (the switch that actually starts and stops triggers) for every affected reference; it never writes to the reference row. Consequences:
- Re-enabling restores exactly what each user had chosen; nothing is "re-enabled automatically" or "left for the user" because the user's choice was never touched.
- Configuration **deleted** (decided: reversible) behaves exactly like disabled. Deleting must **not** use the one-way project-delete path (`ConnectedUserReferenceRolloutManager.deleteReferenceDeployments`, which marks references dangling and deletes the reference `ProjectDeployment` holding each user's connections and inputs). The reference deployments are kept. Recreating a configuration for that environment brings references back with their connections and inputs, pinned to the recreated configuration's version via the normal rollout.
- `dangling` keeps its current one-way meaning (template or project gone).

**Copies.** New copies are made from the configuration's `project_version` for the caller's environment, and only while the project and the template are on there. **Existing copies keep working** (decided) when the project or workflow is later turned off or its configuration deleted: a copy is the user's own workflow, so it can still be listed, enabled, edited and run. "Off" gates new activations and new copies, and switches references off; it does not reach into copies.

**Publishing.** Unchanged apart from no longer triggering rollout. The publish dialog may offer an optional "Release to Development" step that creates or updates the Development configuration to the new version, enabled; it is never automatic.

**Code-workflow deploy API** (decided). `POST /automation-project-code-workflows/deploy` gains an optional release flag (e.g. `release: true`). When set, after publishing it points the configuration for the request's environment (`X-ENVIRONMENT`) at the new version, which rolls existing references forward. If no configuration exists for that environment it creates one, enabled. It **never flips an existing configuration from disabled to enabled**: an environment an admin turned off stays off after a redeploy. Without the flag, a deploy only publishes. This is an explicit opt-in per call, so it does not break the "no implicit exposure" anti-goal.

**Version changes.** Changing a configuration's `project_version` may only point at a published version. Moving to an older version is allowed (rollback); references roll to it the same way.

## Admin UI (embedded Automation Workflows page)

- The project row's expanded area gets a **Workflows | Configurations** tab pair, mirroring embedded Integrations (`IntegrationListItemTabs`).
- **Workflows tab**: unchanged template authoring. Clicking a template opens the editor; the template ⋮ menu keeps **Edit** and **Delete**; the **Executions** item is removed.
- **Configurations tab**: one row per environment that has a configuration, with the environment, pinned version badge, a "newer version available" hint when the project's last published version is higher than the pinned one, enabled switch, and a ⋮ menu (**Edit**, **Delete**). Expanding a row lists that version's workflows, each with its own enabled switch. **Clicking a workflow opens its executions** for that environment (reuse `AutomationWorkflowExecutionsSheet` with an environment and the configuration's version). A **New Configuration** button sits right of the tab pair, disabled with a tooltip until the project has a published version.
- **Configuration dialog**: environment (only environments without a configuration), published version, enabled, **Show in Automation Hub**.
- The project dialog loses **Show in Automation Hub**. The "Hidden from hub" badge moves to configuration rows.
- Nested configuration rows follow the conventions already used for integration configurations under an integration: no repeated project icon, no Tags.

**Executions on a configuration workflow row** (my call): every execution of that template in the configuration's environment, including connected users' copies and runs from earlier pinned versions, with each run's version shown. This keeps history across version changes and rollbacks and reuses the existing embedded executions query (with the copy-inclusion from 6730); the sheet takes the environment from the configuration instead of the global environment store.

## Embedded sample app

`bytechef-samples/bytechef-embedded-sample-app` must be updated alongside the server change:

- **Automation Code Playground** (`front-end/src/app/automation-code-playground`) deploys with `POST /api/embedded/v1/automation-project-code-workflows/deploy`, then finds the project through `GET /api/embedded/v1/automation/projects` and relies on a redeploy rolling out to existing references. With configurations, a deploy alone exposes nothing, so the playground's deploy route (`/api/code-playground/automation/deploy`) must pass the new release flag. Its explanatory copy ("Deploying publishes it to the embedded catalog straight away; ... ByteChef rolls the new version out to existing references") changes to say the deploy also releases it to the app's environment.
- **Automation Hub** and **templates** pages (`app/automation-hub`, `app/automations/templates`) keep calling the same endpoints; they need no code change, but their behavior changes (only enabled configurations in `BYTECHEF_ENVIRONMENT` appear), so any copy or README that says "publish to make it appear" must say the project also needs an enabled configuration.
- The sample app's README and setup steps must include creating a configuration for the environment the app runs against.

## Docs

Update `embedded/build/automations/index.md` (configurations tab, executions moved, publishing no longer releases), `embedded/build/automation-hub.mdx` (what the hub shows per environment; references follow the environment's configuration), and the project dialog section (Show in Automation Hub moved).

## Left to the builder

- Module placement (new domain, repository, service in `embedded-configuration-*`, mirroring `IntegrationInstanceConfiguration`) and GraphQL schema shape.
- The exact name and shape of the deploy API's release flag.
- The exact attention reason strings.
- Whether the "Release to Development" publish shortcut ships in the first iteration.
