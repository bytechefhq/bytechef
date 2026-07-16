---
title: Integration Configurations
description: Deploy and manage integration instances assigned to specific environments.
---

![Integration Configurations overview](configurations-overview.png)

---

## Key Features

| Feature | Description |
|---|---|
| Environment scoping | Each configuration is tied to an environment (Development, Staging, Production). Switch environments from the selector in the left sidebar. |
| Integration filtering | Filter configurations by integration using the left sidebar. |
| Tag filtering | Filter configurations by tag for quick access. |
| Unified API filtering *(feature-flagged)* | When the Unified API feature flag is enabled in your deployment, filter by Unified API category (Accounting, Commerce, CRM). |
| Enable/Disable toggle | Activate or deactivate a configuration without deleting it. |
| Version selection | Choose which published version of an integration to deploy. |

### Configuration Details

Each configuration in the list displays:

- **Integration name** -- the name and icon of the underlying integration.
- **Workflow count** -- number of workflows included in this configuration.
- **Version** -- the published integration version deployed by this configuration.
- **Tags** -- assigned tags for organization and filtering.
- **Enabled/Disabled status** -- whether the configuration is currently active.

---

## How to Use

### Creating a Configuration

1. Click the **New Instance Configuration** button in the top-right corner.
2. Select the integration you want to deploy.
3. Choose the published version to use.
4. Configure connection credentials and workflow-specific settings.
5. Assign tags if desired.
6. Click **Save** to create the configuration.

### Internal-only workflow inputs

A workflow input can be marked **Internal only** in the workflow editor (the checkbox in the input's edit dialog). This routes where the input is set:

- **Internal only checked** — the input is configured here, in the instance configuration, by you. It never appears in the end-user connect dialog. Use this for values you set once per deployment (an API key, an account ID, a default that should not be end-user editable).
- **Internal only unchecked** *(default)* — the input is collected from the connected user in the [connect dialog](/embedded/quickstart#7-render-the-connect-dialog), not here.

The split is strict: an input renders in exactly one place. Existing inputs are treated as not internal-only (they render in the connect dialog) until you mark them.

### Managing Configurations

- **Enable/Disable** -- toggle a configuration on or off to control whether its workflows execute.
- **Edit** -- update the version, connections, or workflow settings.
- **Delete** -- remove the configuration entirely.

### Filtering Configurations

Use the left sidebar to narrow the list:

- **Integrations** -- select a specific integration to show only its configurations, or choose "All Integrations" to see everything.
- **Tags** -- click a tag to filter by that tag.
- **Unified API** -- when the Unified API feature flag is enabled, filter by Accounting, Commerce, or CRM category. Hidden otherwise.

### Environment Selection

Configurations are scoped to environments. Use the environment selector in the left sidebar (next to the user menu) to switch between Development, Staging, and Production. Each environment maintains its own set of configurations independently.
