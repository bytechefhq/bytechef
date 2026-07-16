---
title: Connected Users
description: View and manage end users who have connected integrations through your embedded iPaaS.
---

![Connected Users overview](connected-users-overview.png)

---

## Key Features

| Feature | Description |
|---|---|
| User table | A paginated table of all connected users with key details. |
| Search | Search by name, email, or **External Id** — the unique identifier your application assigns to each user (also referred to as "User ERC" in the search placeholder). |
| Status filtering | Filter by connection status (Valid or Invalid). |
| Integration filtering | Filter by specific integration to find users of a particular service. |
| Date range filtering | Filter by the date range when users were created. |
| Pagination | Navigate through large user lists with page controls. |

### Table Columns

| Column | Description |
|---|---|
| Status | Connection credential status indicator -- green for Valid, gray for Invalid. |
| External Id | The unique identifier your application assigns to the user (this is the `sub` claim of the JWT you sign with your Signing Key). |
| Name | The user's display name. |
| Email | The user's email address. |
| Integrations | Icons representing which integrations the user has connected. |
| Created Date | When the connected user record was created. |

---

## How to Use

### Viewing Connected Users

1. Navigate to the **Connected Users** page from the Embedded sidebar.
2. The table displays all connected users for the current environment.
3. Click on a user row to open a detail sheet with full information about their connections and integrations.

### Filtering Users

Use the left sidebar filters to narrow the user list:

- **Search** -- enter a name, email address, or External Id (User ERC) to find specific users.
- **Connection Status** -- select "Valid" or "Invalid" to filter by credential status.
- **Integration** -- select an integration to show only users who have connected that service.
- **Created Date** -- pick a date range to filter users by when they were created.

Click **Filter** to apply the selected filters.

### User Details

Click on a connected user row to open a side sheet with a **Profile** header card followed by three tabs:

1. **Profile** -- a header card with the user's name, email, external ID, and account metadata.
2. **Integrations** tab (default) -- every integration the user has connected, with the integration's status and the workflows enabled for each integration instance. Use this tab to inspect and manage the connect-flow side of the relationship.
3. **MCP Servers** tab -- the MCP servers this user has access to. Each entry shows the server and lets you enable or disable it for the user, so you can control which component tool sets are exposed to that user's AI agents.
4. **Automation Workflows** tab -- the automation workflows associated with this user across all projects they have access to, listed flat with the workflow label, the version currently in use for this user, and the last execution date (or "No executions" if it hasn't run yet). The tab shows "No automation workflows." when nothing is associated.

The sheet stays open across outside clicks (only the close button or the **Escape** key dismisses it), so you can keep it open while navigating other UI.

### Environment Selection

Connected users are scoped to the current environment. Use the environment selector in the left sidebar (next to the user menu) to switch between Development, Staging, and Production to see users in each environment.
