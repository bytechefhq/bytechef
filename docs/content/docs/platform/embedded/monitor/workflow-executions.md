---
title: Workflow Executions
description: View and filter the execution history of your connected users' integration and automation workflows.
---

![Workflow Executions overview](executions/executions-overview.png)

The **Workflow Executions** page lists every run of an integration workflow or an automation workflow that belongs to your connected users. Use it to check whether a workflow ran, how long it took, and why it failed. Open it under **Embedded → Workflow Executions**. Executions are scoped to the environment selected in the application sidebar.

## Viewing executions

1. Go to **Embedded → Workflow Executions**.
2. Pick a **Source** and any filters in the left sidebar.
3. Click a row, or choose **View** in its **Actions** menu, to open the execution detail.

Click the refresh button in the page header to reload the list. To stop a running execution, choose **Stop** in its **Actions** menu. **Stop** is available only while the execution is in the `STARTED` state.

## Sources

The **Source** select at the top of the left sidebar picks which executions the page lists:

| Source | Lists |
|---|---|
| **Any** | Integration and automation executions in one list, newest first. This is the default. |
| **Integrations** | Executions of integration instances your connected users enabled. |
| **Automations** | Executions of the workflows your connected users built in the [Automation Hub](/platform/embedded/build/automations/automation-hub). |

## Table columns

Every source shows **Status**, **Workflow**, **Duration**, **Execution date** and **Actions**. The other columns depend on the source:

| Column | Source | Description |
|---|---|---|
| **Source** | Any | An icon for where the run came from (a square for an integration, a workflow icon for an automation), followed by the integration's component or the automation's project. The icon's tooltip names the kind of source. |
| **Integration** | Integrations | The integration. |
| **Instance** | Integrations | The integration instance configuration. |
| **Project** | Automations | The automation project. |
| **Version** | All | The integration or project version that ran. |
| **Connected user** | All | The integration instance's connected user, or the automation's deployment. |

## Filters

| Filter | Source | Description |
|---|---|---|
| **Source** | All | **Any** (default), **Integrations** or **Automations**. |
| **Status** | All | `STARTED`, `COMPLETED`, `CREATED`, `STOPPED` or `FAILED`. **Any** shows every status. |
| **Start date** | All | Executions that started on or after this date. |
| **End date** | All | Executions that started on or before this date. |
| **Integration** | Integrations | One integration. |
| **Instance Configuration** | Integrations | One integration instance configuration. |
| **Workflow** | Integrations, Automations | One workflow. |
| **Connected User** | Any, Integrations | One connected user, listed as `User <externalId>`. |
| **Connected Users** | Automations | The automations built by one connected user. |

## Execution detail

Clicking a row opens the detail view that matches where the run came from. It shows:

- The overall execution status and timing.
- Each workflow step with its input, output and status.
- Error messages and stack traces for failed steps.

For both kinds of execution, the detail colors the run on its read-only canvas and lists each AI Agent tool call as a row under the agent's task, the same way the [automation execution detail](/platform/automation/monitor/workflow-executions#what-the-detail-view-shows) does. See [Seeing which tools ran](/platform/automation/build/workflows/ai/agent#seeing-which-tools-ran).

<!-- TODO screenshot: Workflow execution detail sheet open, showing the workflow panel with a selected step and its Input / Output / Error tabs -->

### AI Copilot for automation executions

> **Coming soon.** Analyzing an automation execution with the AI Copilot from the execution detail is not generally available yet.

## Related

- [External Invocations](/platform/embedded/monitor/external-invocations)
- [Failures and Retries](/platform/embedded/monitor/failures-and-retries)
- [Connected Users](/platform/embedded/monitor/connected-users)
- [Troubleshooting](/platform/embedded/monitor/troubleshooting)
