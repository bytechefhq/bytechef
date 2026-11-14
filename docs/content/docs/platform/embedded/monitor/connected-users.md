---
title: Connected Users
description: View and manage end users who have connected integrations through your embedded iPaaS.
---

A connected user is an end user of your product who uses integrations or automations through ByteChef Embedded. ByteChef creates the record the first time a request authenticates for a new external id, or you create it here. The **Connected Users** page lists the connected users of the current environment and lets you create, import, edit, disable and delete them, provision integrations, workflows and MCP servers for them, and move them between configurations.

This page covers the ByteChef console view. For how a connected user record comes into existence and how your backend attaches a name, email or its own metadata to it, see [Syncing Connected Users](/platform/embedded/get-started/initial-setup/syncing-connected-users-programmatically).

![Connected Users overview](connected-users/connected-users-overview.png)

## Connected user list

Open **Embedded → Connected Users**. The table lists the connected users of the environment selected in the application sidebar, a page at a time.

| Column | Description |
|---|---|
| Checkbox | Selects the user for a [batch upgrade](#upgrading-several-users-at-once). |
| **Status** | Credential status: green when all connections the user's integrations use are valid, red when some are invalid, gray when the user has no integrations yet. Hover for the explanation. |
| **External Id** | The identifier your application assigns to the user. It is the `sub` claim of the JWT you sign with your Signing Key. |
| **Name** | The user's display name. |
| **Email** | The user's email address. |
| **Integrations** | Icons of the integrations the user has connected. |
| **Created Date** | When the connected user record was created. |
| ⋮ menu | **Details** (opens the user sheet), **Enable** or **Disable**, and **Delete**. |

A disabled user cannot authenticate, so their requests are rejected and they cannot start executions. **Delete** asks for confirmation, then removes the user record and their integration instances, so the workflows they had enabled stop running.

### Filtering users

Use the left sidebar filters to narrow the list:

| Filter | Description |
|---|---|
| **Search** | A name, email address or external id. The placeholder calls the external id "User ERC". |
| **Connection Status** | **Valid** or **Invalid** credential status. |
| **Integration** | Users who have connected that integration. |
| **Created Date** | A date range for when users were created. |

## Creating a Connected User

Click **New connected user** in the page header (**Add Connected User** when the list is empty). The **New connected user** dialog asks for:

- **External ID** *(required)* - the identifier your application uses for this user, the same value your backend puts in the JWT `sub` claim. It must be unique in the environment, is case-sensitive, and must not start or end with whitespace.
- **Name** and **Email** *(optional)*.
- **Metadata** *(optional)* - key/value rows; click **Add metadata** for each one.

Click **Create**. The user is created enabled, in the environment selected in the sidebar. When that user later authenticates through the SDK with the same external ID, ByteChef reuses this record instead of creating a new one.

Name, email, metadata keys, and metadata values are limited to 256 characters each.

## Importing Connected Users from CSV

Click **Import** in the page header. In the **Import connected users** dialog, choose a **CSV file** and click **Import**. The file needs a header row:

| Column | Meaning |
|---|---|
| `externalId` | Required. The user's external ID, with the same rules as above. |
| `name` | Optional. |
| `email` | Optional. |
| any other column | Stored as a metadata key, named exactly as in the header. |

The `externalId`, `name`, and `email` headers are matched case-insensitively. An empty cell means "no value": no metadata key is created for it.

```csv
externalId,name,email,plan,region
user-1001,Ada Lovelace,ada@example.com,enterprise,eu
user-1002,Alan Turing,,free,
```

A file can hold at most 1,000 data rows and 1 MB. A larger file, or a metadata header longer than 256 characters, refuses the whole import. Otherwise each row is created on its own, so one bad row does not stop the others. When the import finishes, the dialog shows how many users were created and lists:

- **Skipped (duplicates)** - external IDs that already exist in the environment, or repeat an earlier row of the same file. Existing users are never overwritten.
- **Rejected** - rows that could not be created, with the spreadsheet row number (the header is row 1) and the reason.

## User Details

Click a connected user row to open a side sheet. At the top is a **Profile** header card with the user's ID, name, email, external ID and metadata, followed by six tabs:

1. **Integrations** (default) - every integration the user has connected, with the integration's status and the workflows enabled for each integration instance. A workflow's switch is grayed out when its instance configuration does not enable it. A workflow that an embedded MCP server exposes as a tool is listed here only when it also has other triggers, such as a webhook or a schedule. It is marked **MCP tool**, and its switch controls those other triggers. A workflow whose only trigger is the tool trigger is managed on the **MCP Servers** tab. Click a workflow's name to open its executions for this user. Click **Provision Integration** to add an instance for the user (see [Provisioning an Integration](#provisioning-an-integration)).
2. **Automations** - the automation workflows this user has, in two kinds of groups:
   - A group named after an [automation project](/platform/embedded/build/automations), marked **Reference**, holds the workflows provisioned from that project. They follow the project version of the user's configuration. The group's card shows the configuration's name, the number of workflows and the project version, and its ⋮ menu offers **Change configuration…**.
   - **Created by this user**, marked **Copy**, holds the workflows the user owns and edits in the workflow editor: copies of a template and workflows built from scratch. Each one shows its own version, or **V1 DRAFT** until the user publishes it, and it cannot be enabled before it is published.

   Expand a group to see its workflows. Each row shows the workflow label, its static webhook URL when it has one, its trigger and components, the last execution date (or **No executions**) and a switch that enables or disables the workflow for this user. Click a workflow's name to open its executions for this user. A row's ⋮ menu offers **Delete**, and for a workflow in a project group also **Edit** (see [Editing a Workflow's Connections and Inputs](#editing-a-workflows-connections-and-inputs)). Click **Provision Workflow** to add workflows (see [Provisioning a Workflow](#provisioning-a-workflow)). The tab shows **No automations yet** when the user has none.
3. **MCP Servers** - the MCP servers this user has access to, so you can control which tools are exposed to that user's AI agents. Each server card shows how many component tools and workflow tools it exposes to the user, with a switch that turns all of them on or off at once. Expand a card to see its **Component Tools** and **Workflow Tools**, grouped by component, each with its own switch. A workflow tool's switch only decides whether the user's AI agents see that tool. It does not change the workflow's switch on the **Integrations** tab. When you add tools or workflows to the server, users who have it get them automatically. **Delete** removes the server and all of its tools for this user. The user's integrations and workflows are not affected, and the server can be provisioned for the user again. To give the user a server, click **Provision MCP Server**, pick a server in the **New MCP Server** dialog and click **Save**. Only servers that expose tools for integrations the user already has can be picked.
4. **Connections** - the user's connections: connections the user created (**Self-created**), connections an admin provisioned (**Provisioned**) and shared admin connections (**Internal**). Each row shows its credential status and whether an integration or an automation uses it. See [Credentials of provisioned connections](#credentials-of-provisioned-connections).
5. **Workflow Executions** - the user's integration and automation runs, a page at a time, with the same columns as [Workflow Executions](/platform/embedded/monitor/workflow-executions). Click a run to open its detail.
6. **External Invocations** - the user's most recent tool and action calls made through embedded MCP servers and the Component Kit. See [External Invocations](/platform/embedded/monitor/external-invocations).

The executions view opens on top of the user sheet and lists only this user's runs of that workflow, with a **Workflow** tab that shows the workflow's definition. Click an execution to see its task-by-task details.

The sheet header has a ⋮ menu with actions for the user:

- **Edit** - opens the **Edit connected user** dialog (see [Editing a Connected User](#editing-a-connected-user)).
- **Enable** or **Disable** - toggles the user's active state. A disabled user is marked **Disabled** in the sheet title.
- **Delete** - asks for confirmation, then removes the connected user and their integration instances.

The sheet stays open when you click outside it. Close it with the close button or the **Escape** key.

<!-- TODO screenshot: Connected user detail side sheet open, showing the Profile header card and the Integrations / MCP Servers / Automations tabs, plus the Enable/Disable and Delete actions in the sheet header -->

## Editing a Connected User

Choose **Edit** in the sheet's ⋮ menu. You can change the **Name**, **Email** and **Metadata**. The **External ID** is read-only. Name and email can be cleared.

Saving **replaces** the user's metadata with what the form shows, so a key you remove in the form is removed from the user. This is different from the SDK's `PATCH` call, which only adds or overwrites keys (see [Syncing Connected Users](/platform/embedded/get-started/initial-setup/syncing-connected-users-programmatically#what-happens-to-each-key)). If the user changed after you opened the dialog, for example because your backend updated its metadata, the save is refused and the dialog asks you to **Reload** the latest values before editing again.

## Provisioning an Integration

Provisioning sets an integration up for a connected user from the console, so the user does not have to go through the Connect dialog. It is also how you keep a customer on a specific configuration or integration version while new users get another.

1. Open the connected user's sheet and click **Provision Integration** on the **Integrations** tab.
2. In the **Provision integration** dialog, choose the **Integration**. Only integrations the user is allowed to use (by their [permission expressions](/platform/embedded/build/permission-expressions)) are listed.
3. Choose the **Configuration**. Each enabled [instance configuration](/platform/embedded/configure/instance-configurations) of the integration is listed with its version. The configuration users get when they connect on their own is preselected.
4. Fill in the **connection** fields. Fields the configuration already sets (such as a shared OAuth2 client ID and secret) are hidden. For an OAuth2 configuration, click **Connect** at the end of the configuration's row and sign in through the popup with the customer's account; the button turns into a **Connected** badge.
5. Select the **workflows** to enable and fill in their inputs. The dialog lists the workflows the configuration enables. A workflow an embedded MCP server exposes is listed only when it also has other triggers. It is marked **MCP tool**, and enabling it turns on those other triggers. Whether the user's AI agents see the tool is set on the **MCP Servers** tab. Required inputs of a selected workflow must be filled before you can provision.
6. Click **Provision**.

ByteChef creates the connection, the integration instance on the chosen configuration, and the selected workflows, and the new instance appears on the **Integrations** tab. If any step fails, nothing is kept: the workflows already enabled are disabled and the instance and connection are deleted.

The instance is created in the connected user's own environment. Provisioning always adds a new instance, even when the user already has one for the same integration (for example, two accounts of the same service). The user's own embedded UI then shows that instance's configuration and workflows, not the self-serve one.

## Provisioning a Workflow

Provisioning gives a connected user workflows from one of your automation projects, without the user activating them in the [Automation Hub](/platform/embedded/build/automations/automation-hub).

1. Open the connected user's sheet and click **Provision Workflow** on the **Automations** tab. The button is disabled when no project has a configuration released to the user's environment.
2. In the **New Workflow** dialog, choose the **Project**. Each project is listed with its version.
3. Choose the **Configuration**. The self-serve configuration, or the only one, is preselected. If the user is already assigned to a configuration of the project, it is shown read-only; use **Change configuration…** on the project's card to move the user.
4. Select the workflows to provision. Workflows the user already has are checked and marked **Provisioned**.
5. For each selected workflow, pick a **connection** for every component that needs one. The list holds the user's own connections for that component, and the **+** button creates a new one. A workflow provisioned without a connection it needs is provisioned disabled.
6. Fill in the workflow's **inputs**. Required inputs are marked with an asterisk and must be filled before you can save.
7. Click **Save**.

Connections and inputs that the configuration provides are not listed, because the configuration's values always apply.

## Editing a Workflow's Connections and Inputs

You can change the connections and inputs a provisioned workflow runs with. This applies to workflows in a project group. Workflows under **Created by this user** are edited by the user in the workflow editor.

1. On the **Automations** tab, expand the project group, open the workflow's ⋮ menu and choose **Edit**.
2. On the **Connections** tab of the dialog, pick another of the user's connections for a component, or create a new one with the **+** button. The workflow's current connection is preselected.
3. On the **Inputs** tab, change the input values. The current values are prefilled.
4. Click **Save**.

The workflow keeps its enabled state. As when provisioning, connections and inputs the configuration provides are not listed. **Save** is unavailable when the configuration provides all of them, when the workflow's configuration is not released to the user's environment, or when the workflow needs attention, for example because its project was deleted (see [Dangling references](/platform/embedded/build/automations/automation-code-workflows#dangling-references)).

## Credentials of provisioned integrations

On the **Integrations** tab, a row an admin provisioned shows a key icon with a **Credentials** action. Integrations provisioned before this release, and integrations the connected user activated themselves, have no **Credentials** action.

- **View** - the dialog shows the connection's credentials obfuscated and read-only. Viewing is not audited.
- **Replace** - click **Update credentials** and enter the new values. ByteChef reuses the integration configuration's OAuth2 app and shared values, so you do not retype the client id and secret. If the integration's configuration can no longer be provisioned for this user, the dialog is view-only and explains that the credentials cannot be replaced.
- **After the user reauthorizes** - if the connected user reauthorizes the connection, you can still view and replace it.

Each replacement is recorded as an `INTEGRATION_INSTANCE_CREDENTIALS_UPDATED` audit event with the integration instance, connection and connected user ids, never the credential values.

## Credentials of provisioned connections

On the **Connections** tab, every connection marked **Provisioned** shows a key icon with a **Credentials** action. A connection that belongs to a provisioned integration opens the dialog described in [Credentials of provisioned integrations](#credentials-of-provisioned-integrations). A connection you provisioned for the user's automations opens the **Edit Connection** dialog on its credentials: enter the new values and click **Update credentials**. The connection keeps its id, so the automations that use it get the new credentials on their next run.

Connections the user created themselves have no **Credentials** action. A connection marked **Internal** is one of your shared connections: its key icon is greyed out, and you replace its credentials on the [Connections](/platform/embedded/build/connections) page.

Each replacement of an automation connection is recorded as a `CONNECTION_CREDENTIALS_UPDATED` audit event with the connection and connected user ids, never the credential values.

In the embedded workflow builder, an admin still sees a connected user's own connections so they can pick one, but listed by name without their credentials. Connections an admin entered keep their obfuscated credentials.

## Upgrading Several Users at Once

Use a batch upgrade to move many connected users to a newer configuration at once, without opening each one.

1. Tick the checkboxes of the users you want in the table, or the checkbox in the header to select every user on the page. A bar appears above the table with the count, for example "2 selected", and the buttons **Upgrade integration**, **Upgrade automation**, and **Clear**. **Clear** drops the selection. The selection is also cleared when you change a filter, the page or the environment.

   ![Connected users selected for a batch upgrade](connected-users/connected-users-batch-selection.png)

2. For an integration, click **Upgrade integration**, then pick one of the integrations the selected users have. Next pick the **Configuration** to move them to. Each configuration of the current environment is listed as its name and version, for example "Gmail · V10". Configurations with a version higher than the lowest one the selected users are on are marked **Newer version**. If none of the selected users has an integration in this environment, the dialog says so instead of listing any.
3. For an automation, click **Upgrade automation**, then pick a project that has configurations in the current environment. Next pick the **Configuration**. When the configurations have different versions, the one with the highest version is marked **Latest version**. If no published project has configurations in this environment, the dialog says so.
4. Read the impact preview. It shows what changes for the workflows involved, such as new required inputs or connections, and lists the users that would be skipped, each with the reason under their name. When MCP servers use these configurations, a note says how many, because their tools follow the configuration each user lands on.

   ![Upgrade dialog with the impact preview](connected-users/connected-users-upgrade-integration.png)

5. Click **Upgrade**. While the upgrade runs, the dialog stays open and cannot be closed.

**Upgrade** stays unavailable until you pick a configuration and the preview has loaded. It also stays unavailable when none of the selected users would move, because each of them is already on that configuration or doesn't have that integration or project. A warning under the **Configuration** field says so. Pick another configuration or change the selection.

![Warning when none of the selected users would move](connected-users/connected-users-upgrade-nobody-moves.png)

When it finishes, the dialog shows how many users were moved and how many need attention, and lists each skipped user with a reason. Closing the dialog refreshes the list and clears the selection. The reasons you may see are:

- **Already on this configuration**: nothing to do for that user.
- **Doesn't have this integration** or **Doesn't have this project**: the user has no configuration of what you picked in this environment.
- **The target configuration doesn't have these workflows:** followed by the workflow names: the target lacks a workflow the user relies on.
- **These workflows aren't enabled on the target configuration:** followed by the workflow names: those workflows exist on the target but are not enabled.
- **The target configuration uses a different authorization type**: the user's connection cannot be carried over.
- **The move failed. Try again.**: the move did not complete for that user.

Skipped users are left exactly as they were.

A few things to know:

- **Versions belong to configurations, not to users.** A user has no version of their own. Upgrading a user means moving them onto another configuration, and that configuration has the newer version.
- **Only the current environment.** Users and target configurations must be in the environment selected in the sidebar.
- **Upgrading a whole configuration is separate.** **Change Integration Version** and **Change Project Version** on a configuration still upgrade everyone on it. Use the batch upgrade when you only want to move some users. For moving everyone off one configuration, see [Moving Users Between Configurations](/platform/embedded/configure/instance-configurations#moving-users-between-configurations).

## Related

- [Syncing Connected Users](/platform/embedded/get-started/initial-setup/syncing-connected-users-programmatically)
- [Instance Configurations](/platform/embedded/configure/instance-configurations)
- [Workflow Executions](/platform/embedded/monitor/workflow-executions)
- [External Invocations](/platform/embedded/monitor/external-invocations)
