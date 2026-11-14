---
title: Connections
description: Manage authentication credentials and connection configurations for third-party services.
---

![Connections overview](connections/connections-overview.png)

A connection stores the credentials a component needs to act in a third-party service, such as an API key or an OAuth2 sign-in. The **Connections** page lists the connections tenant admins create for embedded workflows. A connection marked shared is available to every connected user in the environment. Connected users' own connections are managed through the embedded API, described in [Connections for connected users](#connections-for-connected-users).

---

## Key Features

| Feature | Description |
|---|---|
| Component filtering | Filter connections by component (third-party service) using the left sidebar. |
| Tag filtering | Organize and filter connections by assigned tags. |
| Environment scoping | Connections are scoped to the current environment (Development, Staging, Production). |
| Status indicator | Each connection shows **Active** / **Not Active** when its credential is valid, or the credential status itself (for example `INVALID`) when it is not. |
| Shared connections | Mark a connection shared so every connected user in the environment can use it. |
| Creation date | See when each connection was created. |

### Connection Details

Each connection in the list displays:

- **Component icon** - the icon of the third-party service.
- **Component name** - the name of the connected service (for example Gmail, Slack or AWS S3).
- **Status** - **Active** when the credential is valid and something references the connection, **Not Active** when the credential is valid but nothing uses it yet, or the credential status (for example `INVALID`) when the credential itself is the problem.
- **Shared** - a badge shown on connections marked shared.
- **Tags** - assigned tags for organization.
- **Creation date** - when the connection was created.

---

## How to Use

### Creating a Connection

1. Click **New Connection** in the top-right corner. On an empty page, click **Create Connection**.
2. Select the component (third-party service) you want to connect to.
3. Provide the required authentication credentials (API key, OAuth, etc.).
4. Optionally turn on **Shared Connection** (see [Shared connections](#shared-connections)).
5. Assign tags if desired.
6. Click **Save** to create the connection.

A newly created connection shows **Not Active** until a workflow, integration instance or test configuration references it. The badge reports usage, not reachability.

### Managing Connections

- **Edit** - change the connection's name, tags or **Shared Connection** setting, or replace its credentials.
- **Delete** - remove a connection after confirming in an alert dialog. Deletion fails while a workflow still uses the connection.
- **Tag** - add or remove tags to organize connections.

Only the user who created a connection, or an admin, can edit, tag, or delete it. Anyone else gets an access-denied error.

This page lists only your own connections, including the ones you shared with connected users. Connections a connected user owns, whether they created them or they belong to the user's integration instances, are not shown to admins, and opening, changing, tagging or deleting one answers "not found". To manage the credentials of an integration you provisioned for a user, see [Credentials of provisioned integrations](/platform/embedded/monitor/connected-users#credentials-of-provisioned-integrations).

To replace the credentials of one of your connections, choose **Edit** and click **Update credentials** in the **Edit Connection** dialog. This is refused for a revoked connection and for one waiting to be reassigned to a new owner; the **Not Active** usage badge does not matter. If someone else changed the connection in the meantime, the save is refused and you reload before trying again. Each replacement is recorded as a `CONNECTION_CREDENTIALS_UPDATED` audit event that holds the connection id only, never the credential values.

### Shared connections

Turn on **Shared Connection** in the create or edit dialog to make a connection available to every connected user in the current environment. Shared connections carry a **Shared** badge in the list.

A connected user can select a shared connection in their own workflows, but cannot reconnect or delete it. Only its creator or an admin can change it, and a change applies to every connected user at once. Connections that connected users create themselves are never shared.

### Filtering Connections

Use the left sidebar to filter the connection list:

- **Components** - the sidebar lists all components that have at least one connection. Click a component name to show only its connections, or select "All Components" to view everything.
- **Tags** - click a tag to filter by that tag.

### Environment Selection

Connections are scoped to environments. Use the environment selector in the app sidebar to switch between Development, Staging and Production. Each environment has its own set of connections, so you can use different credentials for testing and production. When more than one environment is available, the page header notes which environment you are viewing.

### Connection Status

| Status | Description |
|---|---|
| Active | The credential is valid and at least one workflow, integration instance, or test configuration uses this connection. |
| Not Active | The credential is valid but nothing references the connection yet. |
| `INVALID` (or another credential status) | The stored credential is missing, expired, or was rejected. The connected user who owns the connection can reconnect it (see below). |

---

## Connections for connected users

### Which connections a connected user sees

A connected user can use the connections they own plus the shared connections in their environment. They own the connections they created and the connections of their integration instances.

The connection list endpoints return only these connections. They take an optional `connectionIds` query parameter that narrows the result to those ids; ids the user is not entitled to are left out.

A connection created from the Connect dialog, including from the [activation dialog](/platform/embedded/build/automations/activation-dialog), belongs to an integration instance. It is available to both the user's integrations and automations. When a connection backed by an integration instance is reconnected, the authorization parameters of the integration's configuration, such as an OAuth2 app's client id and secret, are restored unless the form supplies them.

Assigning a connection the user is not entitled to (for example when a catalog workflow is set up for the user) fails with `400 Bad Request`.

Each returned connection carries two flags:

- **`shared`** - an admin marked the connection shared.
- **`editable`** - the user owns the connection and can reconnect or delete it.

### Frontend endpoints

With a [Signing Key JWT](/openapi/frontend/embedded-configuration-connection), a connected user manages their own connections:

| Method and path | Description |
|---|---|
| `GET /connections` | List every connection the user can use. |
| `GET /components/{componentName}/connections` | List the user's connections for one component. Accepts `connectionIds`. |
| `POST /components/{componentName}/connections` | Create a connection. Returns the new connection id. |
| `DELETE /connections/{id}` | Delete a connection the user owns. |
| `POST /connections/{id}/reauthorize` | Replace the credentials of a connection the user owns, keeping its id. |

Paths are relative to `/api/embedded/v1`.

- **Delete** returns `404` when the connection does not exist or the user does not own it, and `409` with `{"reason": "CONNECTION_IS_USED"}` while a workflow still uses it.
- **Reauthorize** replaces the authorization parameters as a whole: a parameter you leave out is cleared, so send the complete set. Connection-level properties stay unchanged. A successful call marks the credentials valid again. It returns `404` when the user does not own the connection.

For example, to create an Airtable connection, which uses a bearer token, send the component's connection version, its authorization type and the parameters that authorization asks for. The response is the new connection id:

```bash
curl -X POST "https://your-bytechef-host.example.com/api/embedded/v1/components/airtable/connections" \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -H "X-Environment: PRODUCTION" \
  -d '{
    "name": "My Airtable",
    "connectionVersion": 1,
    "authorizationType": "BEARER_TOKEN",
    "parameters": {"token": "<airtable-personal-access-token>"}
  }'
```

`authorizationType` is the authorization's constant name, such as `API_KEY`, `BEARER_TOKEN` or `OAUTH2_AUTHORIZATION_CODE`. Leave it out for a component whose connection has no authorization.

To replace the token later, send the complete parameter set to `reauthorize`. The response is `204` with no body:

```bash
curl -X POST "https://your-bytechef-host.example.com/api/embedded/v1/connections/1051/reauthorize" \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -H "X-Environment: PRODUCTION" \
  -d '{"parameters": {"token": "<new-airtable-personal-access-token>"}}'
```

The list endpoints return the connections with their flags:

```json
[
  {
    "id": 1051,
    "name": "My Airtable",
    "componentName": "airtable",
    "connectionVersion": 1,
    "authorizationType": "BEARER_TOKEN",
    "environment": "PRODUCTION",
    "credentialStatus": "VALID",
    "createdDate": "2026-05-18T10:00:00Z",
    "shared": false,
    "editable": true
  }
]
```

The [Automation Hub](/platform/embedded/build/automations/automation-hub#connections) uses these endpoints: its **Reconnect** action opens a **Reconnect &lt;component&gt;** dialog that asks only for the credentials and saves them with **Update credentials**.

